package com.spot.taxi.customer.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.spot.taxi.common.fallbackFactory.SentinelFallback;
import com.spot.taxi.common.login.CheckLoginStatus;
import com.spot.taxi.common.result.Result;
import com.spot.taxi.common.util.AuthContextHolder;
import com.spot.taxi.customer.service.OrderService;
import com.spot.taxi.model.form.customer.ExpectOrderForm;
import com.spot.taxi.model.form.customer.SubmitOrderForm;
import com.spot.taxi.model.form.map.CalculateDrivingLineForm;
import com.spot.taxi.model.form.payment.CreateWxPaymentForm;
import com.spot.taxi.model.vo.base.PageVo;
import com.spot.taxi.model.vo.customer.ExpectOrderVo;
import com.spot.taxi.model.vo.driver.DriverInfoVo;
import com.spot.taxi.model.vo.map.DrivingLineVo;
import com.spot.taxi.model.vo.map.OrderLocationVo;
import com.spot.taxi.model.vo.map.OrderServiceLastLocationVo;
import com.spot.taxi.model.vo.order.CurrentOrderInfoVo;
import com.spot.taxi.model.vo.order.OrderInfoVo;
import com.spot.taxi.model.vo.payment.WxPrepayVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "订单API接口管理")
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
@SentinelResource(value = "OrderController", blockHandlerClass = SentinelFallback.class, blockHandler = "defaultBlockHandler")
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "乘客端查找当前订单")
    @CheckLoginStatus
    @GetMapping("/searchCustomerCurrentOrder")
    public Result<CurrentOrderInfoVo> searchCustomerCurrentOrder() {
        Long customerId = AuthContextHolder.getUserId();
        return Result.ok(orderService.searchCustomerCurrentOrder(customerId));
    }

    @Operation(summary = "预估订单数据")
    @CheckLoginStatus
    @PostMapping("/expectOrder")
    public Result<ExpectOrderVo> expectOrder(@RequestBody ExpectOrderForm expectOrderForm) {
        return Result.ok(orderService.expectOrder(expectOrderForm));
    }

    @Operation(summary = "乘客下单")
    @CheckLoginStatus
    @PostMapping("/submitOrder")
    public Result<Long> submitOrder(@RequestBody SubmitOrderForm submitOrderForm) {
        submitOrderForm.setCustomerId(AuthContextHolder.getUserId());
        return Result.ok(orderService.submitOrder(submitOrderForm));
    }

    @Operation(summary = "查询订单状态")
    @CheckLoginStatus
    @GetMapping("/getOrderStatus/{orderId}")
    public Result<Integer> getOrderStatus(@PathVariable Long orderId) {
        return Result.ok(orderService.getOrderStatus(orderId));
    }

    @Operation(summary = "获取订单信息")
    @CheckLoginStatus
    @GetMapping("/getOrderInfo/{orderId}")
    public Result<OrderInfoVo> getOrderInfo(@PathVariable Long orderId) {
        Long customerId = AuthContextHolder.getUserId();
        return Result.ok(orderService.getOrderInfo(orderId, customerId));
    }

    @Operation(summary = "根据订单id获取司机基本信息")
    @CheckLoginStatus
    @GetMapping("/getDriverInfo/{orderId}")
    public Result<DriverInfoVo> getDriverInfo(@PathVariable Long orderId) {
        Long customerId = AuthContextHolder.getUserId();
        return Result.ok(orderService.getDriverInfo(orderId, customerId));
    }

    // todo乘客这边也需要计算吗，是不是可以一边计算就行了，然后存在redis里面，调取
    @Operation(summary = "计算最佳驾驶线路")
    @CheckLoginStatus
    @PostMapping("/calculateDrivingLine")
    public Result<DrivingLineVo> calculateDrivingLine(@RequestBody CalculateDrivingLineForm calculateDrivingLineForm) {
        return Result.ok(orderService.calculateDrivingLine(calculateDrivingLineForm));
    }

    @Operation(summary = "司机赶往代驾起始点：获取订单经纬度位置")
    @CheckLoginStatus
    @GetMapping("/getCacheOrderLocation/{orderId}")
    public Result<OrderLocationVo> getOrderLocation(@PathVariable Long orderId) {
        return Result.ok(orderService.getCacheOrderLocation(orderId));
    }

    @Operation(summary = "代驾服务：获取订单服务最后一个位置信息")
    @CheckLoginStatus
    @GetMapping("/getOrderServiceLastLocation/{orderId}")
    public Result<OrderServiceLastLocationVo> getOrderServiceLastLocation(@PathVariable Long orderId) {
        return Result.ok(orderService.getOrderServiceLastLocation(orderId));
    }

    @Operation(summary = "获取乘客订单分页列表")
    @CheckLoginStatus
    @GetMapping("findCustomerOrderPage/{page}/{limit}")
    public Result<PageVo> findCustomerOrderPage(
            @Parameter(name = "page", description = "当前页码", required = true) @PathVariable Long page,
            @Parameter(name = "limit", description = "每页记录数", required = true) @PathVariable Long limit) {
        Long customerId = AuthContextHolder.getUserId();
        PageVo pageVo = orderService.findCustomerOrderPage(customerId, page, limit);
        return Result.ok(pageVo);
    }

    @Operation(summary = "创建微信支付")
    @CheckLoginStatus
    @PostMapping("/createWxPayment")
    public Result<WxPrepayVo> createWxPayment(@RequestBody CreateWxPaymentForm createWxPaymentForm) {
        Long customerId = AuthContextHolder.getUserId();
        createWxPaymentForm.setCustomerId(customerId);
        return Result.ok(orderService.createWxPayment(createWxPaymentForm));
    }

    @Operation(summary = "支付状态查询")
    @CheckLoginStatus
    @GetMapping("/queryPayStatus/{orderNo}")
    public Result<Boolean> queryPayStatus(@PathVariable String orderNo) {
        return Result.ok(orderService.queryPayStatus(orderNo));
    }


}

