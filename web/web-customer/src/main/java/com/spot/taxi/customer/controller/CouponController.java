package com.spot.taxi.customer.controller;

import com.spot.taxi.common.login.CheckLoginStatus;
import com.spot.taxi.common.result.Result;
import com.spot.taxi.common.util.AuthContextHolder;
import com.spot.taxi.customer.service.CouponService;
import com.spot.taxi.model.vo.base.PageVo;
import com.spot.taxi.model.vo.coupon.AvailableCouponVo;
import com.spot.taxi.model.vo.coupon.NoReceiveCouponVo;
import com.spot.taxi.model.vo.coupon.NoUseCouponVo;
import com.spot.taxi.model.vo.coupon.UsedCouponVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;


@Tag(name = "优惠券活动接口管理")
@RestController
@RequestMapping(value = "/coupon")
@SuppressWarnings({"unchecked", "rawtypes"})
public class CouponController {
    @Autowired
    private CouponService couponService;

    @Operation(summary = "查询未领取优惠券分页列表")
    @CheckLoginStatus
    @GetMapping("findNoReceivePage/{page}/{limit}")
    public Result<PageVo<NoReceiveCouponVo>> findNoReceivePage(@PathVariable("page") Long page, @PathVariable("limit") Long limit) {
        Long customerId = AuthContextHolder.getUserId();
        PageVo<NoReceiveCouponVo> pageVo = couponService.findNoReceivePage(customerId, page, limit);
        return Result.ok(pageVo);
    }

    @Operation(summary = "查询未使用优惠券分页列表")
    @CheckLoginStatus
    @GetMapping("findNoUsePage/{page}/{limit}")
    public Result<PageVo<NoUseCouponVo>> findNoUsePage(@PathVariable("page") Long page, @PathVariable("limit") Long limit) {
        Long customerId = AuthContextHolder.getUserId();
        PageVo<NoUseCouponVo> pageVo = couponService.findNoUsePage(customerId, page, limit);
        return Result.ok(pageVo);
    }

    @Operation(summary = "查询已使用优惠券分页列表")
    @CheckLoginStatus
    @GetMapping("findUsedPage/{page}/{limit}")
    public Result<PageVo<UsedCouponVo>> findUsedPage(@PathVariable("page") Long page, @PathVariable("limit") Long limit) {
        Long customerId = AuthContextHolder.getUserId();
        PageVo<UsedCouponVo> pageVo = couponService.findUsedPage(customerId, page, limit);
        return Result.ok(pageVo);
    }

    @Operation(summary = "领取优惠券")
    @CheckLoginStatus
    @GetMapping("/receive/{couponId}")
    public Result<Boolean> receive(@PathVariable Long couponId) {
        Long customerId = AuthContextHolder.getUserId();
        return Result.ok(couponService.receive(customerId, couponId));
    }

    @Operation(summary = "获取未使用的最佳优惠券信息")
    @CheckLoginStatus
    @GetMapping("/findAvailableCoupon/{orderId}")
    public Result<List<AvailableCouponVo>> findAvailableCoupon(@PathVariable Long orderId) {
        Long customerId = AuthContextHolder.getUserId();
        return Result.ok(couponService.findAvailableCoupon(customerId, orderId));
    }




}

