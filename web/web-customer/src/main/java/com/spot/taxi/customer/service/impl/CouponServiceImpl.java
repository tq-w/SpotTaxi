package com.spot.taxi.customer.service.impl;

import com.spot.taxi.coupon.client.CouponFeignClient;
import com.spot.taxi.customer.service.CouponService;
import com.spot.taxi.model.vo.base.PageVo;
import com.spot.taxi.model.vo.coupon.AvailableCouponVo;
import com.spot.taxi.model.vo.coupon.NoReceiveCouponVo;
import com.spot.taxi.model.vo.coupon.NoUseCouponVo;
import com.spot.taxi.model.vo.coupon.UsedCouponVo;
import com.spot.taxi.model.vo.order.OrderBillVo;
import com.spot.taxi.order.client.OrderInfoFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class CouponServiceImpl implements CouponService {
    @Autowired
    private CouponFeignClient couponFeignClient;
    @Autowired
    private OrderInfoFeignClient orderInfoFeignClient;

    @Override
    public PageVo<NoReceiveCouponVo> findNoReceivePage(Long customerId, Long page, Long limit) {
        return couponFeignClient.findNoReceivePage(customerId, page, limit).getData();
    }

    @Override
    public PageVo<NoUseCouponVo> findNoUsePage(Long customerId, Long page, Long limit) {
        return couponFeignClient.findNoUsePage(customerId, page, limit).getData();
    }

    @Override
    public PageVo<UsedCouponVo> findUsedPage(Long customerId, Long page, Long limit) {
        return couponFeignClient.findUsedPage(customerId, page, limit).getData();
    }

    @Override
    public Boolean receive(Long customerId, Long couponId) {
        return couponFeignClient.receive(customerId, couponId).getData();
    }

    @Override
    public List<AvailableCouponVo> findAvailableCoupon(Long customerId, Long orderId) {
        OrderBillVo orderBillVo = orderInfoFeignClient.getOrderBillInfo(orderId).getData();
        return couponFeignClient.findAvailableCoupon(customerId, orderBillVo.getPayAmount()).getData();
    }
}
