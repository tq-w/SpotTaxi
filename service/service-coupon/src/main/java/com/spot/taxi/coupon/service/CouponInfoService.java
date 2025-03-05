package com.spot.taxi.coupon.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.spot.taxi.model.entity.coupon.CouponInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.spot.taxi.model.form.coupon.UseCouponForm;
import com.spot.taxi.model.vo.base.PageVo;
import com.spot.taxi.model.vo.coupon.AvailableCouponVo;
import com.spot.taxi.model.vo.coupon.NoReceiveCouponVo;
import com.spot.taxi.model.vo.coupon.NoUseCouponVo;
import com.spot.taxi.model.vo.coupon.UsedCouponVo;

import java.math.BigDecimal;
import java.util.List;

public interface CouponInfoService extends IService<CouponInfo> {


    PageVo<NoReceiveCouponVo> findNoReceivePage(Page<CouponInfo> pageParam, Long customerId);

    PageVo<NoUseCouponVo> findNoUsePage(Page<CouponInfo> pageParam, Long customerId);

    PageVo<UsedCouponVo> findUsedPage(Page<CouponInfo> pageParam, Long customerId);

    Boolean receive(Long customerId, Long couponId);

    List<AvailableCouponVo> findAvailableCoupon(Long customerId, BigDecimal orderAmount);

    BigDecimal useCoupon(UseCouponForm useCouponForm);
}
