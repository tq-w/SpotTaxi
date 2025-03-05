package com.spot.taxi.coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.spot.taxi.common.constant.RedisConstant;
import com.spot.taxi.common.execption.CustomException;
import com.spot.taxi.common.result.ResultCodeEnum;
import com.spot.taxi.coupon.mapper.CouponInfoMapper;
import com.spot.taxi.coupon.mapper.CustomerCouponMapper;
import com.spot.taxi.coupon.service.CouponInfoService;
import com.spot.taxi.model.entity.coupon.CouponInfo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spot.taxi.model.entity.coupon.CustomerCoupon;
import com.spot.taxi.model.form.coupon.UseCouponForm;
import com.spot.taxi.model.vo.base.PageVo;
import com.spot.taxi.model.vo.coupon.AvailableCouponVo;
import com.spot.taxi.model.vo.coupon.NoReceiveCouponVo;
import com.spot.taxi.model.vo.coupon.NoUseCouponVo;
import com.spot.taxi.model.vo.coupon.UsedCouponVo;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class CouponInfoServiceImpl extends ServiceImpl<CouponInfoMapper, CouponInfo> implements CouponInfoService {
    @Autowired
    private CustomerCouponMapper customerCouponMapper;
    @Autowired
    private CouponInfoMapper couponInfoMapper;
    @Autowired
    private RedissonClient redissonClient;

    public CouponInfoServiceImpl(CustomerCouponMapper customerCouponMapper) {
        this.customerCouponMapper = customerCouponMapper;
    }

    @Override
    public PageVo<NoReceiveCouponVo> findNoReceivePage(Page<CouponInfo> pageParam, Long customerId) {
        IPage<NoReceiveCouponVo> pageInfo = couponInfoMapper.findNoReceivePage(pageParam, customerId);
        return new PageVo<>(pageInfo.getRecords(), pageInfo.getPages(), pageInfo.getTotal());
    }

    @Override
    public PageVo<NoUseCouponVo> findNoUsePage(Page<CouponInfo> pageParam, Long customerId) {
        IPage<NoUseCouponVo> pageInfo = couponInfoMapper.findNoUsePage(pageParam, customerId);
        return new PageVo<>(pageInfo.getRecords(), pageInfo.getPages(), pageInfo.getTotal());
    }

    @Override
    public PageVo<UsedCouponVo> findUsedPage(Page<CouponInfo> pageParam, Long customerId) {
        IPage<UsedCouponVo> pageInfo = couponInfoMapper.findUsedPage(pageParam, customerId);
        return new PageVo<>(pageInfo.getRecords(), pageInfo.getPages(), pageInfo.getTotal());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean receive(Long customerId, Long couponId) {
        CouponInfo couponInfo = this.getById(couponId);
        if (couponInfo == null) {
            log.error("领取优惠券失败，优惠券信息不存在，couponId：" + couponId);
            throw new CustomException(ResultCodeEnum.DATA_ERROR);
        }
        if (couponInfo.getExpireTime().before(new Date())) {
            log.info("优惠券已过期，couponId: {}", couponId);
            return false;
        }
        if (couponInfo.getPublishCount() != 0 && couponInfo.getReceiveCount() >= couponInfo.getPublishCount()) {
            log.info("优惠券已领完，couponId: {}", couponId);
            return false;
        }
        RLock lock = null;
        try {
            lock = redissonClient.getLock(RedisConstant.COUPON_LOCK + customerId);
            boolean flag = lock.tryLock(RedisConstant.COUPON_LOCK_WAIT_TIME, RedisConstant.COUPON_LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if (flag) {
                if (couponInfo.getPerLimit() > 0) {
                    Long count = customerCouponMapper.selectCount(new LambdaQueryWrapper<CustomerCoupon>().
                            eq(CustomerCoupon::getCouponId, couponId).eq(CustomerCoupon::getCustomerId, customerId));
                    if (count >= couponInfo.getPerLimit()) {
                        log.info("超过优惠券领取限制，couponId: {}", couponId);
                        return false;
                    }
                }
                int rows = 0;
                if (couponInfo.getPublishCount() == 0) {
                    // 没有限制
                    rows = couponInfoMapper.updateReceiveCount(couponId);
                } else {
                    // 有限制
                    rows = couponInfoMapper.updateReceiveCountByLimit(couponId);
                }
                if (rows == 0) {
                    log.info("优惠券已领完，couponId: {}", couponId);
                    return false;
                } else if (rows != 1) {
                    log.error("领取优惠券失败，couponId：{}", couponId);
                    throw new CustomException(ResultCodeEnum.DATA_ERROR);
                }
                //6、保存领取记录
                this.saveCustomerCoupon(customerId, couponId, couponInfo.getExpireTime());
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new CustomException(ResultCodeEnum.DATA_ERROR);
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
        log.info("领取优惠券失败，couponId：{}", couponId);
        return false;
    }

    @Override
    public List<AvailableCouponVo> findAvailableCoupon(Long customerId, BigDecimal orderAmount) {
        List<NoUseCouponVo> list = couponInfoMapper.findNoUseList(customerId);
        List<NoUseCouponVo> couponList1 = list.stream().filter(item -> item.getCouponType() == 1).toList();
        List<AvailableCouponVo> list1 = couponList1.stream().filter(item -> {
            BigDecimal reduceAmount = item.getAmount();
            if (item.getConditionAmount().compareTo(BigDecimal.ZERO) == 0) {
                return orderAmount.compareTo(reduceAmount) > 0;
            } else {
                return orderAmount.compareTo(item.getConditionAmount()) > 0;
            }
        }).map(item -> this.buildBestNoUseCouponVo(item, item.getAmount())).toList();


        List<NoUseCouponVo> couponList2 = list.stream().filter(item -> item.getCouponType() == 2).toList();
        List<AvailableCouponVo> list2 = couponList2.stream().filter(item -> {
            BigDecimal finalAmount = orderAmount.multiply(item.getDiscount()).divide(new BigDecimal("10"), RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP);
            if (item.getConditionAmount().compareTo(BigDecimal.ZERO) == 0) {
                return true;
            } else {
                return finalAmount.compareTo(item.getConditionAmount()) > 0;
            }
        }).map(item -> {
            BigDecimal finalAmount = orderAmount.multiply(item.getDiscount()).divide(new BigDecimal("10"), RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP);
            BigDecimal reduceAmount = orderAmount.subtract(finalAmount);
            return this.buildBestNoUseCouponVo(item, reduceAmount);
        }).toList();

        List<AvailableCouponVo> availableCouponVoList = new ArrayList<>(Stream.concat(list1.stream(), list2.stream()).toList());
        if (!CollectionUtils.isEmpty(availableCouponVoList)) {
            // todo默认是升序
            availableCouponVoList.sort(Comparator.comparing(AvailableCouponVo::getReduceAmount));
        }
        return availableCouponVoList;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public BigDecimal useCoupon(UseCouponForm useCouponForm) {
        CustomerCoupon customerCoupon = customerCouponMapper.selectById(useCouponForm.getCustomerCouponId());
        if (customerCoupon == null || !customerCoupon.getCustomerId().equals(useCouponForm.getCustomerId())) {
            log.error("优惠券信息不存在或不匹配，customerCouponId：{}，customerCouponId：{}", useCouponForm.getCustomerCouponId(), useCouponForm.getCustomerCouponId());
            throw new CustomException(ResultCodeEnum.DATA_ERROR);
        }
        CouponInfo couponInfo = couponInfoMapper.selectById(customerCoupon.getCouponId());
        if (couponInfo == null) {
            log.error("使用优惠券失败，优惠券信息不存在，couponId：{}", customerCoupon.getCouponId());
            throw new CustomException(ResultCodeEnum.DATA_ERROR);
        }
        //获取优惠券减免金额
        BigDecimal reduceAmount = null;
        if (couponInfo.getCouponType() == 1) {
            //使用门槛判断
            //2.1.1.没门槛，订单金额必须大于优惠券减免金额
            if (couponInfo.getConditionAmount().compareTo(BigDecimal.ZERO) == 0 && useCouponForm.getOrderAmount().compareTo(couponInfo.getAmount()) > 0) {
                reduceAmount = couponInfo.getAmount();
            }
            //2.1.2.有门槛，订单金额大于优惠券门槛金额
            if (couponInfo.getConditionAmount().compareTo(BigDecimal.ZERO) > 0 && useCouponForm.getOrderAmount().compareTo(couponInfo.getConditionAmount()) > 0) {
                reduceAmount = couponInfo.getAmount();
            }
        } else {
            //使用门槛判断
            //订单折扣后金额
            BigDecimal discountOrderAmount = useCouponForm.getOrderAmount().multiply(couponInfo.getDiscount()).divide(new BigDecimal("10"), RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP);
            //订单优惠金额
            //2.2.1.没门槛
            if (couponInfo.getConditionAmount().compareTo(BigDecimal.ZERO) == 0) {
                //减免金额
                reduceAmount = useCouponForm.getOrderAmount().subtract(discountOrderAmount);
            }
            //2.2.2.有门槛，订单折扣后金额大于优惠券门槛金额
            if (couponInfo.getConditionAmount().compareTo(BigDecimal.ZERO) > 0 && discountOrderAmount.compareTo(couponInfo.getConditionAmount()) > 0) {
                //减免金额
                reduceAmount = useCouponForm.getOrderAmount().subtract(discountOrderAmount);
            }
        }
        assert reduceAmount != null;
        if (reduceAmount.compareTo(BigDecimal.ZERO) > 0) {
            Integer useCount_old = couponInfo.getUseCount();
            couponInfo.setUseCount(useCount_old + 1);
            couponInfoMapper.updateById(couponInfo);
            //更新customer_coupon
            // todo这里会不会出现高并发的问题呢，我觉得不会
            CustomerCoupon updateCustomerCoupon = new CustomerCoupon();
            updateCustomerCoupon.setId(customerCoupon.getId());
            updateCustomerCoupon.setUsedTime(new Date());
            updateCustomerCoupon.setOrderId(useCouponForm.getOrderId());
            customerCouponMapper.updateById(updateCustomerCoupon);
            return reduceAmount;
        }
        return null;
    }

    private AvailableCouponVo buildBestNoUseCouponVo(NoUseCouponVo noUseCouponVo, BigDecimal reduceAmount) {
        AvailableCouponVo bestNoUseCouponVo = new AvailableCouponVo();
        BeanUtils.copyProperties(noUseCouponVo, bestNoUseCouponVo);
        bestNoUseCouponVo.setCouponId(noUseCouponVo.getId());
        bestNoUseCouponVo.setReduceAmount(reduceAmount);
        return bestNoUseCouponVo;
    }

    private void saveCustomerCoupon(Long customerId, Long couponId, Date expireTime) {
        CustomerCoupon customerCoupon = new CustomerCoupon();
        customerCoupon.setCustomerId(customerId);
        customerCoupon.setCouponId(couponId);
        customerCoupon.setStatus(1);
        customerCoupon.setReceiveTime(new Date());
        customerCoupon.setExpireTime(expireTime);
        customerCouponMapper.insert(customerCoupon);
    }
}
