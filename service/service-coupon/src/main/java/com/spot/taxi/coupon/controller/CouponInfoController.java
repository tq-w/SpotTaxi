package com.spot.taxi.coupon.controller;

import com.spot.taxi.common.result.Result;
import com.spot.taxi.coupon.service.CouponInfoService;
import com.spot.taxi.model.entity.coupon.CouponInfo;
import com.spot.taxi.model.form.coupon.UseCouponForm;
import com.spot.taxi.model.vo.base.PageVo;
import com.spot.taxi.model.vo.coupon.AvailableCouponVo;
import com.spot.taxi.model.vo.coupon.NoReceiveCouponVo;
import com.spot.taxi.model.vo.coupon.NoUseCouponVo;
import com.spot.taxi.model.vo.coupon.UsedCouponVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.math.BigDecimal;
import java.util.List;


@Tag(name = "优惠券活动接口管理")
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/coupon/info")
@SuppressWarnings({"unchecked", "rawtypes"})
public class CouponInfoController {
    
    private final CouponInfoService couponInfoService;

    @Operation(summary = "查询未领取优惠券分页列表")
    @GetMapping("findNoReceivePage/{customerId}/{page}/{limit}")
    public Result<PageVo<NoReceiveCouponVo>> findNoReceivePage(@PathVariable("customerId") Long customerId, @PathVariable("page") Long page, @PathVariable("limit") Long limit) {
        Page<CouponInfo> pageParam = new Page<>(page, limit);
        PageVo<NoReceiveCouponVo> pageVo = couponInfoService.findNoReceivePage(pageParam, customerId);
        pageVo.setPage(page);
        pageVo.setLimit(limit);
        return Result.ok(pageVo);
    }

    @Operation(summary = "查询未使用优惠券分页列表")
    @GetMapping("findNoUsePage/{customerId}/{page}/{limit}")
    public Result<PageVo<NoUseCouponVo>> findNoUsePage(@PathVariable("customerId") Long customerId, @PathVariable("page") Long page, @PathVariable("limit") Long limit) {
        Page<CouponInfo> pageParam = new Page<>(page, limit);
        PageVo<NoUseCouponVo> pageVo = couponInfoService.findNoUsePage(pageParam, customerId);
        pageVo.setPage(page);
        pageVo.setLimit(limit);
        return Result.ok(pageVo);
    }

    @Operation(summary = "查询已使用优惠券分页列表")
    @GetMapping("findUsedPage/{customerId}/{page}/{limit}")
    public Result<PageVo<UsedCouponVo>> findUsedPage(@PathVariable("customerId") Long customerId, @PathVariable("page") Long page, @PathVariable("limit") Long limit) {
        Page<CouponInfo> pageParam = new Page<>(page, limit);
        PageVo<UsedCouponVo> pageVo = couponInfoService.findUsedPage(pageParam, customerId);
        pageVo.setPage(page);
        pageVo.setLimit(limit);
        return Result.ok(pageVo);
    }

    @Operation(summary = "领取优惠券")
    @GetMapping("/receive/{customerId}/{couponId}")
    public Result<Boolean> receive(@PathVariable Long customerId, @PathVariable Long couponId) {
        return Result.ok(couponInfoService.receive(customerId, couponId));
    }

    @Operation(summary = "获取未使用的最佳优惠券信息")
    @GetMapping("/findAvailableCoupon/{customerId}/{orderAmount}")
    public Result<List<AvailableCouponVo>> findAvailableCoupon(@PathVariable Long customerId, @PathVariable BigDecimal orderAmount) {
        return Result.ok(couponInfoService.findAvailableCoupon(customerId, orderAmount));
    }

    @Operation(summary = "使用优惠券")
    @PostMapping("/useCoupon")
    public Result<BigDecimal> useCoupon(@RequestBody UseCouponForm useCouponForm) {
        return Result.ok(couponInfoService.useCoupon(useCouponForm));
    }

}

