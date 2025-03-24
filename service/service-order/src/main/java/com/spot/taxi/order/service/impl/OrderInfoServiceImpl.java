package com.spot.taxi.order.service.impl;

import com.spot.taxi.common.constant.MqConst;
import com.spot.taxi.common.constant.RedisConstant;
import com.spot.taxi.common.constant.SystemConstant;
import com.spot.taxi.common.execption.CustomException;
import com.spot.taxi.common.result.ResultCodeEnum;
import com.spot.taxi.common.service.RabbitService;
import com.spot.taxi.model.entity.order.OrderBill;
import com.spot.taxi.model.entity.order.OrderInfo;
import com.spot.taxi.model.entity.order.OrderProfitsharing;
import com.spot.taxi.model.entity.order.OrderStatusLog;
import com.spot.taxi.model.enums.OrderStatus;
import com.spot.taxi.model.form.order.OrderInfoForm;
import com.spot.taxi.model.form.order.StartDriveForm;
import com.spot.taxi.model.form.order.UpdateOrderBillForm;
import com.spot.taxi.model.form.order.UpdateOrderCarForm;
import com.spot.taxi.model.vo.base.PageVo;
import com.spot.taxi.model.vo.order.*;
import com.spot.taxi.order.mapper.OrderBillMapper;
import com.spot.taxi.order.mapper.OrderInfoMapper;
import com.spot.taxi.order.mapper.OrderProfitsharingMapper;
import com.spot.taxi.order.mapper.OrderStatusLogMapper;
import com.spot.taxi.order.service.OrderInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.spot.taxi.common.constant.RedisConstant.*;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {
    // 随机数生成器
    private static final Random RANDAM = new Random();
    private final OrderInfoMapper orderInfoMapper;
    private final OrderStatusLogMapper orderStatusLogMapper;
    private final RedisTemplate<Object, Object> redisTemplate;
    private final RedissonClient redissonClient;
    private final OrderBillMapper orderBillMapper;
    private final OrderProfitsharingMapper orderProfitsharingMapper;
    private final RabbitService rabbitService;
    private static final String DECREMENT_SCRIPT =
            "local key = KEYS[1]\n" +
                    "local current = tonumber(redis.call('get', key) or 0)\n" +
                    "if current > 0 then\n" +
                    "    redis.call('decr', key)\n" +
                    "    return 0\n" +  // 扣减成功，返回 0（后续需明确处理）
                    "else\n" +
                    "    return -1\n" + // 库存不足
                    "end";

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long saveOrderInfo(OrderInfoForm orderInfoForm) {
        OrderInfo orderInfo = new OrderInfo();
        BeanUtils.copyProperties(orderInfoForm, orderInfo);

        String orderNo = System.currentTimeMillis() + getRandomPart();
        orderInfo.setOrderNo(orderNo);
        orderInfo.setStatus(OrderStatus.WAITING_ACCEPT.getStatus());
        orderInfoMapper.insert(orderInfo);
        this.log(orderInfo.getId(), orderInfo.getStatus());
        //接单标识，标识不存在了说明不在等待接单状态了
        redisTemplate.opsForValue().set(RedisConstant.ORDER_ACCEPT_MARK + orderInfo.getId(), 1, RedisConstant.ORDER_ACCEPT_MARK_EXPIRES_TIME, TimeUnit.MINUTES);
        //发送延迟消息，取消订单
        rabbitService.sendDelayMessage(MqConst.EXCHANGE_CANCEL_ORDER, MqConst.ROUTING_CANCEL_ORDER, String.valueOf(orderInfo.getId()), SystemConstant.CANCEL_ORDER_DELAY_TIME);
        return orderInfo.getId();
    }

    @Override
    public Integer getOrderStatus(Long orderId) {
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getId, orderId);
        queryWrapper.select(OrderInfo::getStatus);
        OrderInfo orderInfo = orderInfoMapper.selectOne(queryWrapper);
        if (orderInfo == null) {
            return OrderStatus.NULL_ORDER.getStatus();
        }
        return orderInfo.getStatus();
    }

    // 随机决定是否执行
    private boolean shouldCompensate() {
        Random random = new Random();
        // 这里设置随机概率为50%，可以根据需要调整
        return random.nextBoolean();
    }

    // 用于压测单用原子扣减库存能否实现防止超卖
//    @Override
//    public Boolean takeNewOrder(Long driverId, Long orderId) {
//        String redisKey = RedisConstant.ORDER_ACCEPT_MARK + orderId;
//        // 1. 使用 Lua 脚本原子扣减库存（拦截 99% 无效请求）
//        Long scriptResult = redisTemplate.execute(
//                new DefaultRedisScript<>(DECREMENT_SCRIPT, Long.class),
//                Collections.singletonList(redisKey)
//        );
//        // 库存不足或脚本执行失败
//        if (scriptResult == -1) {
//            return false;
//        }
//        if (shouldCompensate()) {
//            redisTemplate.opsForValue().increment(redisKey);
//            return false;
//        }
//        log.info("抢单成功，订单ID：{}", orderId);
//        return true;
//    }

    // 1.3
    @Override
    public Boolean takeNewOrder(Long driverId, Long orderId) {
        String redisKey = RedisConstant.ORDER_ACCEPT_MARK + orderId;
        RLock lock = null;
        boolean locked = false;

        try {
            // 1. 使用 Lua 脚本原子扣减库存（拦截 99% 无效请求）
            Long scriptResult = redisTemplate.execute(
                    new DefaultRedisScript<>(DECREMENT_SCRIPT, Long.class),
                    Collections.singletonList(redisKey)
            );

            // 库存不足或脚本执行失败
            if (scriptResult == -1) {
                return false;
            }

            // 3. 查询订单状态（幂等性检查）
            OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
            if (orderInfo == null ||
                    !OrderStatus.WAITING_ACCEPT.getStatus().equals(orderInfo.getStatus())) {
                // 订单已被处理，回滚库存
                redisTemplate.opsForValue().increment(redisKey);
                return false;
            }

            // 4. 更新订单状态
            orderInfo.setDriverId(driverId);
            orderInfo.setStatus(OrderStatus.ACCEPTED.getStatus());
            orderInfo.setAcceptTime(new Date());

            if (orderInfoMapper.updateById(orderInfo) != 1) {
                // 数据库更新失败，回滚库存
                redisTemplate.opsForValue().increment(redisKey);
                return false;
            }
            return true;
        } catch (Exception e) {
            // 补偿库存
            redisTemplate.opsForValue().increment(redisKey);
            throw new CustomException(ResultCodeEnum.TAKE_NEW_ORDER_FAIL);
        }
    }

    // 1.2
//    @Override
//    public Boolean takeNewOrder(Long driverId, Long orderId) {
//        String redisKey = RedisConstant.ORDER_ACCEPT_MARK + orderId;
//        RLock lock = null;
//        boolean locked = false;
//
//        try {
//            // 1. 使用 Lua 脚本原子扣减库存（拦截 99% 无效请求）
//            Long scriptResult = redisTemplate.execute(
//                    new DefaultRedisScript<>(DECREMENT_SCRIPT, Long.class),
//                    Collections.singletonList(redisKey)
//            );
//
//            // 库存不足或脚本执行失败
//            if (scriptResult == -1) {
//                return false;
//            }
//
//            // 2. 获取分布式锁（只有库存扣减成功的请求进入）
//            lock = redissonClient.getLock(TAKE_NEW_ORDER_LOCK + orderId);
//            locked = lock.tryLock(TAKE_NEW_ORDER_LOCK_WAIT_TIME, TAKE_NEW_ORDER_LOCK_LEASE_TIME, TimeUnit.SECONDS);
//
//            if (!locked) {
//                // 补偿库存（异步优化点：可放入队列延迟补偿）
//                redisTemplate.opsForValue().increment(redisKey);
//                return false;
//            }
//
//            // 3. 查询订单状态（幂等性检查）
//            OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
//            if (orderInfo == null ||
//                    !OrderStatus.WAITING_ACCEPT.getStatus().equals(orderInfo.getStatus())) {
//                // 订单已被处理，回滚库存
//                redisTemplate.opsForValue().increment(redisKey);
//                return false;
//            }
//
//            // 4. 更新订单状态
//            orderInfo.setDriverId(driverId);
//            orderInfo.setStatus(OrderStatus.ACCEPTED.getStatus());
//            orderInfo.setAcceptTime(new Date());
//
//            if (orderInfoMapper.updateById(orderInfo) != 1) {
//                // 数据库更新失败，回滚库存
//                redisTemplate.opsForValue().increment(redisKey);
//                return false;
//            }
//
//            return true;
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//            // 补偿库存
//            redisTemplate.opsForValue().increment(redisKey);
//            throw new CustomException(ResultCodeEnum.TAKE_NEW_ORDER_FAIL);
//        } catch (Exception e) {
//            // 补偿库存
//            redisTemplate.opsForValue().increment(redisKey);
//            throw new CustomException(ResultCodeEnum.TAKE_NEW_ORDER_FAIL);
//        } finally {
//            if (lock != null && locked && lock.isHeldByCurrentThread()) {
//                lock.unlock();
//            }
//        }
//    }

// 1.1
//    @Override
//    public Boolean takeNewOrder(Long driverId, Long orderId) {
//        // 1. 原子扣减库存（订单库存初始为1，扣减后为0）todo这里可能会出现两个线程都没通过的情况
//        Long stock = redisTemplate.opsForValue().decrement(RedisConstant.ORDER_ACCEPT_MARK + orderId);
//        if (stock == null || stock < 0) {
//            System.out.println("订单已被抢走，" + driverId + "抢单" + orderId + "失败");
//            return false;
//        }
//
//        // 2. 获取分布式锁（防止极端情况下重复操作）
//        RLock lock = redissonClient.getLock(TAKE_NEW_ORDER_LOCK + orderId);
//        try {
//            boolean flag = lock.tryLock(TAKE_NEW_ORDER_LOCK_WAIT_TIME, TAKE_NEW_ORDER_LOCK_LEASE_TIME, TimeUnit.SECONDS);
//            if (!flag) {
//                // 补偿：回滚库存（因已扣减成功但未抢到锁）
//                redisTemplate.opsForValue().increment(RedisConstant.ORDER_ACCEPT_MARK + orderId);
//                return false;
//            }
//
//            // 3. 检查订单状态是否已被修改（防止重复处理）
//            OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
//            if (orderInfo != null && orderInfo.getStatus().equals(OrderStatus.WAITING_ACCEPT.getStatus())) {
//                orderInfo.setDriverId(driverId);
//                orderInfo.setStatus(OrderStatus.ACCEPTED.getStatus());
//                orderInfo.setAcceptTime(new Date());
//                if (orderInfoMapper.updateById(orderInfo) == 1) {
//                    return true;
//                }
//            }
//
//            // 4. 数据库更新失败时回滚库存
//            redisTemplate.opsForValue().increment(RedisConstant.ORDER_ACCEPT_MARK + orderId);
//            return false;
//        } catch (Exception e) {
//            // 异常时回滚库存
//            redisTemplate.opsForValue().increment(RedisConstant.ORDER_ACCEPT_MARK + orderId);
//            throw new CustomException(ResultCodeEnum.TAKE_NEW_ORDER_FAIL);
//        } finally {
//            if (lock.isHeldByCurrentThread()) {
//                lock.unlock();
//            }
//        }
//    }

// 1.0
//    @Override
//    public Boolean takeNewOrder(Long driverId, Long orderId) {
//        if (!redisTemplate.hasKey(RedisConstant.ORDER_ACCEPT_MARK + orderId)) {
//            System.out.println("订单已被抢走，" + driverId + "抢单" + orderId + "失败");
//            return false;
//        }
//        RLock lock = redissonClient.getLock(RedisConstant.TAKE_NEW_ORDER_LOCK + orderId);
//        try {
//            boolean flag = lock.tryLock(RedisConstant.TAKE_NEW_ORDER_LOCK_WAIT_TIME, RedisConstant.TAKE_NEW_ORDER_LOCK_LEASE_TIME, TimeUnit.SECONDS);
//            if (!flag) {
//                // 表示在指定的等待时间内未能成功获取锁，可能是因为锁已经被其他线程持有。
//                System.out.println("抢单失败，" + driverId + "抢单" + orderId + "失败");
//                return false;
//            }
//            // 成功获取到了锁，程序可以在接下来的时间内执行对应的逻辑。
//            LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
//            queryWrapper.eq(OrderInfo::getId, orderId);
//            OrderInfo orderInfo = orderInfoMapper.selectOne(queryWrapper);
//            if (orderInfo == null) {
//                System.out.println("订单不存在，" + driverId + "抢单" + orderId + "失败");
//                return false;
//            }
//            orderInfo.setDriverId(driverId);
//            orderInfo.setStatus(OrderStatus.ACCEPTED.getStatus());
//            orderInfo.setAcceptTime(new Date());
//            int rows = orderInfoMapper.updateById(orderInfo);
//            if (rows != 1) {
//                System.out.println("更新订单状态失败，" + driverId + "抢单" + orderId + "失败");
//                throw new CustomException(ResultCodeEnum.DATA_ERROR);
//            }
//            redisTemplate.delete(RedisConstant.ORDER_ACCEPT_MARK + orderId);
//            return true;
//        } catch (Exception e) {
//            throw new CustomException(ResultCodeEnum.TAKE_NEW_ORDER_FAIL);
//        } finally {
//            if (lock.isHeldByCurrentThread()) {
//                lock.unlock();
//            }
//        }
//    }

    //乘客端查找当前订单
    @Override
    public CurrentOrderInfoVo searchCustomerCurrentOrder(Long customerId) {
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getCustomerId, customerId);
        List<Integer> statusList = Arrays.asList(
                OrderStatus.ACCEPTED.getStatus(),
                OrderStatus.DRIVER_ARRIVED.getStatus(),
                OrderStatus.UPDATE_CAR_INFO.getStatus(),
                OrderStatus.START_SERVICE.getStatus(),
                OrderStatus.END_SERVICE.getStatus(),
                OrderStatus.UNPAID.getStatus()
        );
        queryWrapper.in(OrderInfo::getStatus, statusList);
        queryWrapper.orderByDesc(OrderInfo::getId);
        queryWrapper.last("limit 1");

        OrderInfo orderInfo = orderInfoMapper.selectOne(queryWrapper);
        CurrentOrderInfoVo currentOrderInfoVo = new CurrentOrderInfoVo();
        if (orderInfo != null) {
            currentOrderInfoVo.setOrderId(orderInfo.getId());
            currentOrderInfoVo.setStatus(orderInfo.getStatus());
            currentOrderInfoVo.setIsHasCurrentOrder(true);
        } else {
            currentOrderInfoVo.setIsHasCurrentOrder(false);
        }
        return currentOrderInfoVo;
    }

    @Override
    public CurrentOrderInfoVo searchDriverCurrentOrder(Long driverId) {
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getDriverId, driverId);
        List<Integer> statusList = Arrays.asList(
                OrderStatus.ACCEPTED.getStatus(),
                OrderStatus.DRIVER_ARRIVED.getStatus(),
                OrderStatus.UPDATE_CAR_INFO.getStatus(),
                OrderStatus.START_SERVICE.getStatus(),
                OrderStatus.END_SERVICE.getStatus()
        );
        queryWrapper.in(OrderInfo::getStatus, statusList);
        queryWrapper.orderByDesc(OrderInfo::getId);
        queryWrapper.last("limit 1");
        OrderInfo orderInfo = orderInfoMapper.selectOne(queryWrapper);
        //封装到vo
        CurrentOrderInfoVo currentOrderInfoVo = new CurrentOrderInfoVo();
        if (null != orderInfo) {
            currentOrderInfoVo.setStatus(orderInfo.getStatus());
            currentOrderInfoVo.setOrderId(orderInfo.getId());
            currentOrderInfoVo.setIsHasCurrentOrder(true);
        } else {
            currentOrderInfoVo.setIsHasCurrentOrder(false);
        }
        return currentOrderInfoVo;
    }

    // 司机到达起始点
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean driverArriveStartLocation(Long orderId, Long driverId) {
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getId, orderId);
        queryWrapper.eq(OrderInfo::getDriverId, driverId);

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setStatus(OrderStatus.DRIVER_ARRIVED.getStatus());
        orderInfo.setArriveTime(new Date());

        int rows = orderInfoMapper.update(orderInfo, queryWrapper);
        if (rows == 1) {
            return true;
        } else {
            throw new CustomException(ResultCodeEnum.UPDATE_ERROR);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean updateOrderCart(UpdateOrderCarForm updateOrderCarForm) {
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getId, updateOrderCarForm.getOrderId());
        queryWrapper.eq(OrderInfo::getDriverId, updateOrderCarForm.getDriverId());

        OrderInfo orderInfo = new OrderInfo();
        BeanUtils.copyProperties(updateOrderCarForm, orderInfo);
        orderInfo.setStatus(OrderStatus.UPDATE_CAR_INFO.getStatus());

        int rows = orderInfoMapper.update(orderInfo, queryWrapper);
        if (rows == 1) {
            return true;
        } else {
            throw new CustomException(ResultCodeEnum.UPDATE_ERROR);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean startDriving(StartDriveForm startDriveForm) {
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getId, startDriveForm.getOrderId());
        queryWrapper.eq(OrderInfo::getDriverId, startDriveForm.getDriverId());

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setStatus(OrderStatus.START_SERVICE.getStatus());
        orderInfo.setStartServiceTime(new Date());
        int rows = orderInfoMapper.update(orderInfo, queryWrapper);
        if (rows == 1) {
            return true;
        } else {
            throw new CustomException(ResultCodeEnum.UPDATE_ERROR);
        }
    }

    @Override
    public Long getOrderNumByTime(String startTime, String endTime) {
        // todo这里不需要按照司机id查询吗
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ge(OrderInfo::getStartServiceTime, startTime);
        queryWrapper.lt(OrderInfo::getStartServiceTime, endTime);
        return orderInfoMapper.selectCount(queryWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean endDrive(UpdateOrderBillForm updateOrderBillForm) {
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getId, updateOrderBillForm.getOrderId());
        queryWrapper.eq(OrderInfo::getDriverId, updateOrderBillForm.getDriverId());

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setStatus(OrderStatus.END_SERVICE.getStatus());
        orderInfo.setRealAmount(updateOrderBillForm.getTotalAmount());
        orderInfo.setFavourFee(updateOrderBillForm.getFavourFee());
        orderInfo.setRealDistance(updateOrderBillForm.getRealDistance());
        orderInfo.setEndServiceTime(new Date());

        int rows = orderInfoMapper.update(orderInfo, queryWrapper);

        if (rows == 1) {
            OrderBill orderBill = new OrderBill();
            // todo这里应该是没封装完全
            BeanUtils.copyProperties(updateOrderBillForm, orderBill);
            orderBill.setPayAmount(updateOrderBillForm.getTotalAmount());
            orderBillMapper.insert(orderBill);

            OrderProfitsharing orderProfitsharing = new OrderProfitsharing();
            BeanUtils.copyProperties(updateOrderBillForm, orderProfitsharing);
            orderProfitsharing.setRuleId(updateOrderBillForm.getFeeRuleId());
            // todo弄个枚举
            orderProfitsharing.setStatus(1);
            System.out.println(orderProfitsharing);
            orderProfitsharingMapper.insert(orderProfitsharing);
        } else {
            // todo这里是要抛异常吗
            throw new CustomException(ResultCodeEnum.UPDATE_ERROR);
        }
        return true;
    }

    @Override
    public PageVo findCustomerOrderPage(Page<OrderInfo> pageParam, Long customerId) {
        IPage<OrderListVo> pageInfo = orderInfoMapper.selectCustomerOrderPage(pageParam, customerId);
        return new PageVo<>(pageInfo.getRecords(), pageInfo.getPages(), pageInfo.getTotal());
    }

    @Override
    public PageVo findDriverOrderPage(Page<OrderInfo> pageParam, Long driverId) {
        IPage<OrderListVo> pageInfo = orderInfoMapper.selectDriverOrderPage(pageParam, driverId);
        return new PageVo<>(pageInfo.getRecords(), pageInfo.getPages(), pageInfo.getTotal());
    }

    @Override
    public OrderBillVo getOrderBillInfo(Long orderId) {
        LambdaQueryWrapper<OrderBill> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderBill::getOrderId, orderId);
        OrderBill orderBill = orderBillMapper.selectOne(queryWrapper);

        OrderBillVo orderBillVo = new OrderBillVo();
        BeanUtils.copyProperties(orderBill, orderBillVo);
        return orderBillVo;
    }

    @Override
    public OrderProfitsharingVo getOrderProfitsharing(Long orderId) {
        LambdaQueryWrapper<OrderProfitsharing> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderProfitsharing::getOrderId, orderId);
        OrderProfitsharing orderProfitsharing = orderProfitsharingMapper.selectOne(wrapper);

        OrderProfitsharingVo orderProfitsharingVo = new OrderProfitsharingVo();
        BeanUtils.copyProperties(orderProfitsharing, orderProfitsharingVo);
        return orderProfitsharingVo;
    }

    // 发送账单就是更新订单状态，未支付
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean sendOrderBillInfo(Long orderId, Long driverId) {
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getId, orderId);
        queryWrapper.eq(OrderInfo::getDriverId, driverId);

        OrderInfo updateOrderInfo = new OrderInfo();
        updateOrderInfo.setStatus(OrderStatus.UNPAID.getStatus());

        int rows = orderInfoMapper.update(updateOrderInfo, queryWrapper);
        if (rows == 1) {
            return true;
        } else {
            throw new CustomException(ResultCodeEnum.UPDATE_ERROR);
        }
    }

    @Override
    public OrderPayVo getOrderPayVo(String orderNo, Long customerId) {
        OrderPayVo orderPayVo = orderInfoMapper.selectOrderPayVo(orderNo, customerId);
        if (null != orderPayVo) {
            String content = orderPayVo.getStartLocation() + " 到 " + orderPayVo.getEndLocation();
            orderPayVo.setContent(content);
        }
        return orderPayVo;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void systemCancelOrder(long orderId) {
        Integer orderStatus = this.getOrderStatus(orderId);
        if (null != orderStatus && orderStatus.intValue() == OrderStatus.WAITING_ACCEPT.getStatus().intValue()) {
            //取消订单
            OrderInfo orderInfo = new OrderInfo();
            orderInfo.setId(orderId);
            orderInfo.setStatus(OrderStatus.CANCEL_ORDER.getStatus());
            int row = orderInfoMapper.updateById(orderInfo);
            if (row == 1) {
                //记录日志
                this.log(orderInfo.getId(), orderInfo.getStatus());

                //删除redis订单标识
                redisTemplate.delete(RedisConstant.ORDER_ACCEPT_MARK + orderInfo.getId());
            } else {
                throw new CustomException(ResultCodeEnum.UPDATE_ERROR);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean updateOrderPayStatus(String orderNo) {
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getOrderNo, orderNo);
        OrderInfo orderInfo = orderInfoMapper.selectOne(queryWrapper);
        if (orderInfo == null || Objects.equals(orderInfo.getStatus(), OrderStatus.PAID.getStatus())) {
            return false;
        }
        LambdaQueryWrapper<OrderInfo> updateWrapper = new LambdaQueryWrapper<>();
        updateWrapper.eq(OrderInfo::getOrderNo, orderNo);
        OrderInfo updateOrderInfo = new OrderInfo();
        updateOrderInfo.setStatus(OrderStatus.PAID.getStatus());
        updateOrderInfo.setPayTime(new Date());

        int rows = orderInfoMapper.update(updateOrderInfo, updateWrapper);
        if (rows == 1) {
            return true;
        } else {
            throw new CustomException(ResultCodeEnum.UPDATE_ERROR);
        }
    }

    @Override
    public OrderRewardVo getOrderRewardFee(String orderNo) {
        OrderInfo orderInfo = orderInfoMapper.selectOne(
                new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, orderNo).select(OrderInfo::getId, OrderInfo::getDriverId));
        OrderBill orderBill = orderBillMapper.selectOne(
                new LambdaQueryWrapper<OrderBill>().eq(OrderBill::getOrderId, orderInfo.getId()).select(OrderBill::getRewardFee));
        OrderRewardVo orderRewardVo = new OrderRewardVo();
        orderRewardVo.setOrderId(orderInfo.getId());
        orderRewardVo.setDriverId(orderInfo.getDriverId());
        orderRewardVo.setRewardFee(orderBill.getRewardFee());
        return orderRewardVo;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean updateCouponAmount(Long orderId, BigDecimal couponAmount) {
        int row = orderBillMapper.updateCouponAmount(orderId, couponAmount);
        if (row != 1) {
            throw new CustomException(ResultCodeEnum.UPDATE_ERROR);
        }
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateProfitsharingStatus(String orderNo) {
        //查询订单
        OrderInfo orderInfo = orderInfoMapper.selectOne(new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, orderNo).select(OrderInfo::getId));

        //更新状态条件
        LambdaQueryWrapper<OrderProfitsharing> updateQueryWrapper = new LambdaQueryWrapper<>();
        updateQueryWrapper.eq(OrderProfitsharing::getOrderId, orderInfo.getId());
        //更新字段
        OrderProfitsharing updateOrderProfitsharing = new OrderProfitsharing();
        updateOrderProfitsharing.setStatus(2);
        orderProfitsharingMapper.update(updateOrderProfitsharing, updateQueryWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    @Async("logThreadPool")
    public void log(Long orderId, Integer status) {
        OrderStatusLog orderStatusLog = new OrderStatusLog();
        orderStatusLog.setOrderId(orderId);
        orderStatusLog.setOrderStatus(status);
        orderStatusLog.setOperateTime(new Date());
        orderStatusLogMapper.insert(orderStatusLog);
    }

    private static String getRandomPart() {
        int randomNumber = RANDAM.nextInt(10000);  // 生成一个0-999之间的随机数
        return String.format("%03d", randomNumber);  // 保证随机数是3位，不足补零
    }
}
