package com.spot.taxi.dispatch.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.spot.taxi.common.constant.RedisConstant;
import com.spot.taxi.common.execption.CustomException;
import com.spot.taxi.common.result.Result;
import com.spot.taxi.common.result.ResultCodeEnum;
import com.spot.taxi.dispatch.client.XxlJobClient;
import com.spot.taxi.dispatch.mapper.OrderJobMapper;
import com.spot.taxi.dispatch.service.NewOrderService;
import com.spot.taxi.map.client.LocationFeignClient;
import com.spot.taxi.model.entity.dispatch.OrderJob;
import com.spot.taxi.model.enums.OrderStatus;
import com.spot.taxi.model.form.map.SearchNearbyDriverForm;
import com.spot.taxi.model.vo.dispatch.NewOrderTaskVo;
import com.spot.taxi.model.vo.map.NearByDriverVo;
import com.spot.taxi.model.vo.order.NewOrderDataVo;
import com.spot.taxi.order.client.OrderInfoFeignClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "rawtypes"})
public class NewOrderServiceImpl implements NewOrderService {
    
    private final OrderJobMapper orderJobMapper;
    
    private final XxlJobClient xxlJobClient;
    
    private final OrderInfoFeignClient orderInfoFeignClient;
    
    private final LocationFeignClient locationFeignClient;
    
    private final RedisTemplate<Object, Object> redisTemplate;

    //创建并启动任务调度方法
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long addAndStartTask(NewOrderTaskVo newOrderTaskVo) {
        /*
        根据传来的新订单请求，判断是否已经启动任务调度，如果没有启动则创建并启动任务调度，并且存到数据库中，返回任务id
         */
        LambdaQueryWrapper<OrderJob> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderJob::getOrderId, newOrderTaskVo.getOrderId());
        OrderJob orderJob = orderJobMapper.selectOne(queryWrapper);
        if (orderJob == null) {
            Long jobId = xxlJobClient.addAndStart("newOrderTaskHandler", "", "0 0/1 * * * ?", "新创建订单任务调度：" + newOrderTaskVo.getOrderId());
            orderJob = new OrderJob();
            orderJob.setOrderId(newOrderTaskVo.getOrderId());
            orderJob.setJobId(jobId);
            orderJob.setParameter(JSONObject.toJSONString(newOrderTaskVo));
            orderJobMapper.insert(orderJob);
        }
        return orderJob.getJobId();
    }

    @Override
    public void executeTask(long jobId) {
        /*
        执行任务调度，其中包括搜寻附近司机
         */
        // 1.看任务是否存在
        System.out.println("执行任务调度！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！");
        LambdaQueryWrapper<OrderJob> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderJob::getJobId, jobId);
        OrderJob orderJob = orderJobMapper.selectOne(queryWrapper);
        if (orderJob == null) {
            log.error("任务调度不存在，jobId:{}", jobId);
            return;
        }
        // 2.看任务对应的订单的状态
        Long orderId = orderJob.getOrderId();
        Result<Integer> orderStatusResult = orderInfoFeignClient.getOrderStatus(orderId);
        if (orderStatusResult.getCode() != 200) {
            log.error("获取订单状态失败，orderId:{}", orderId);
            throw new CustomException(ResultCodeEnum.FEIGN_FAIL);
        }
        Integer orderStatus = orderStatusResult.getData();
        if (!orderStatus.equals(OrderStatus.WAITING_ACCEPT.getStatus())) {
            System.out.println("订单状态不是待接单");
            xxlJobClient.startJob(jobId);
            return;
        }
        String newOrderTaskVoJson = orderJob.getParameter();
        NewOrderTaskVo newOrderTaskVo = JSONObject.parseObject(newOrderTaskVoJson, NewOrderTaskVo.class);
        System.out.println(newOrderTaskVo);
        // 3.找司机
        SearchNearbyDriverForm searchNearbyDriverForm = new SearchNearbyDriverForm();
        searchNearbyDriverForm.setLongitude(newOrderTaskVo.getStartPointLongitude());
        searchNearbyDriverForm.setLatitude(newOrderTaskVo.getStartPointLatitude());
        searchNearbyDriverForm.setMileageDistance(newOrderTaskVo.getExpectDistance());
        Result<List<NearByDriverVo>> listResult = locationFeignClient.searchNearbyDriver(searchNearbyDriverForm);
        if (listResult.getCode() != 200) {
            log.error("搜索附近司机失败，orderId:{}", orderId);
            throw new CustomException(ResultCodeEnum.FEIGN_FAIL);
        }
        System.out.println("附近司机数量:" + listResult.getData().size());
        // 4.订单推送给司机
        listResult.getData().forEach(nearByDriverVo -> {
            String repeatKey = RedisConstant.DRIVER_ORDER_REPEAT_LIST + newOrderTaskVo.getOrderId();
            Boolean isMember = redisTemplate.opsForSet().isMember(repeatKey, nearByDriverVo.getDriverId());
            if (Boolean.TRUE.equals(isMember)) {
                return;
            }
            redisTemplate.opsForSet().add(repeatKey, nearByDriverVo.getDriverId());
            redisTemplate.expire(repeatKey, RedisConstant.DRIVER_ORDER_REPEAT_LIST_EXPIRES_TIME, TimeUnit.MINUTES);
            NewOrderDataVo newOrderDataVo = new NewOrderDataVo();
            BeanUtils.copyProperties(newOrderTaskVo, newOrderDataVo);
            newOrderDataVo.setDistance(nearByDriverVo.getDistance());
            System.out.println(newOrderDataVo);
            String driverOrderListKey = RedisConstant.DRIVER_ORDER_TEMP_LIST + nearByDriverVo.getDriverId();
            redisTemplate.opsForList().leftPush(driverOrderListKey, JSONObject.toJSONString(newOrderDataVo));
            redisTemplate.expire(driverOrderListKey, RedisConstant.DRIVER_ORDER_TEMP_LIST_EXPIRES_TIME, TimeUnit.MINUTES);
        });
    }

    // 从推送给司机的可接单订单队列中获得这些订单的列表
    @Override
    public List<NewOrderDataVo> findNewOrderQueueData(Long driverId) {
        ArrayList<NewOrderDataVo> list = new ArrayList<>();
        String driverOrderListKey = RedisConstant.DRIVER_ORDER_TEMP_LIST + driverId;
        Long size = redisTemplate.opsForList().size(driverOrderListKey);
        if ((size != null && size > 0)) {
            for (int i = 0; i < size; i++) {
                // todo是不是也可以不转为json，这里测试会报错，为什么呢
//                NewOrderDataVo newOrderDataVo = (NewOrderDataVo) redisTemplate.opsForList().leftPop(driverOrderListKey);
                String NewOrderDataVoJson = (String) redisTemplate.opsForList().leftPop(driverOrderListKey);
                NewOrderDataVo newOrderDataVo = JSONObject.parseObject(NewOrderDataVoJson, NewOrderDataVo.class);
                list.add(newOrderDataVo);
            }
        }
        return list;
    }

    @Override
    public Boolean clearNewOrderQueueData(Long driverId) {
        String driverOrderListKey = RedisConstant.DRIVER_ORDER_TEMP_LIST + driverId;
        redisTemplate.delete(driverOrderListKey);
        return true;
    }
}
