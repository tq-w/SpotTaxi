package com.spot.taxi.order.client;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.spot.taxi.common.fallbackFactory.SentinelFallback;
import com.spot.taxi.common.result.Result;
import com.spot.taxi.model.entity.order.OrderInfo;
import com.spot.taxi.model.form.order.OrderInfoForm;
import com.spot.taxi.model.form.order.StartDriveForm;
import com.spot.taxi.model.form.order.UpdateOrderBillForm;
import com.spot.taxi.model.form.order.UpdateOrderCarForm;
import com.spot.taxi.model.vo.order.*;
import com.spot.taxi.model.vo.base.PageVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;

@SentinelResource(value = "OrderInfoFeignClient", blockHandlerClass = SentinelFallback.class, blockHandler = "defaultBlockHandler")
@FeignClient(value = "service-order")
public interface OrderInfoFeignClient {
    @PostMapping("/order/info/saveOrderInfo")
    Result<Long> saveOrderInfo(@RequestBody OrderInfoForm orderInfoForm);

    @GetMapping("/order/info/getOrderStatus/{orderId}")
    Result<Integer> getOrderStatus(@PathVariable("orderId") Long orderId);

    @GetMapping("/order/info/takeNewOrder/{driverId}/{orderId}")
    Result<Boolean> takeNewOrder(@PathVariable("driverId") Long driverId, @PathVariable("orderId") Long orderId);

    @GetMapping("/order/info/searchCustomerCurrentOrder/{customerId}")
    Result<CurrentOrderInfoVo> searchCustomerCurrentOrder(@PathVariable("customerId") Long customerId);

    @GetMapping("/order/info/searchDriverCurrentOrder/{driverId}")
    Result<CurrentOrderInfoVo> searchDriverCurrentOrder(@PathVariable("driverId") Long driverId);

    @GetMapping("/order/info/getOrderInfo/{orderId}")
    Result<OrderInfo> getOrderInfo(@PathVariable("orderId") Long orderId);

    @GetMapping("/order/info/driverArriveStartLocation/{orderId}/{driverId}")
    Result<Boolean> driverArriveStartLocation(@PathVariable("orderId") Long orderId, @PathVariable("driverId") Long driverId);

    @PostMapping("/order/info/updateOrderCar")
    Result<Boolean> updateOrderCar(@RequestBody UpdateOrderCarForm updateOrderCarForm);

    @PostMapping("/order/info/startDriving")
    Result<Boolean> startDriving(@RequestBody StartDriveForm startDriveForm);

    @PostMapping("/order/info/endDrive")
    Result<Boolean> endDrive(@RequestBody UpdateOrderBillForm updateOrderBillForm);

    @GetMapping("/order/info/getOrderNumByTime/{startTime}/{endTime}")
    Result<Long> getOrderNumByTime(@PathVariable("startTime") String startTime, @PathVariable("endTime") String endTime);

    @GetMapping("/order/info/findCustomerOrderPage/{customerId}/{page}/{limit}")
    Result<PageVo> findCustomerOrderPage(@PathVariable("customerId") Long customerId, @PathVariable("page") Long page, @PathVariable("limit") Long limit);

    @GetMapping("/order/info/findDriverOrderPage/{driverId}/{page}/{limit}")
    Result<PageVo> findDriverOrderPage(@PathVariable("driverId") Long driverId, @PathVariable("page") Long page, @PathVariable("limit") Long limit);

    @GetMapping("/order/info/getOrderBillInfo/{orderId}")
    Result<OrderBillVo> getOrderBillInfo(@PathVariable("orderId") Long orderId);

    @GetMapping("/order/info/getOrderProfitsharing/{orderId}")
    Result<OrderProfitsharingVo> getOrderProfitsharing(@PathVariable("orderId") Long orderId);

    @GetMapping("/order/info/sendOrderBillInfo/{orderId}/{driverId}")
    Result<Boolean> sendOrderBillInfo(@PathVariable("orderId") Long orderId, @PathVariable("driverId") Long driverId);

    @GetMapping("/order/info/getOrderPayVo/{orderNo}/{customerId}")
    Result<OrderPayVo> getOrderPayVo(@PathVariable("orderNo") String orderNo, @PathVariable("customerId") Long customerId);

    @GetMapping("/order/info/updateOrderPayStatus/{orderNo}")
    Result<Boolean> updateOrderPayStatus(@PathVariable("orderNo") String orderNo);

    @GetMapping("/order/info/getOrderRewardFee/{orderNo}")
    Result<OrderRewardVo> getOrderRewardFee(@PathVariable("orderNo") String orderNo);

    @GetMapping("/order/info/updateCouponAmount/{orderId}/{couponAmount}")
    Result<Boolean> updateCouponAmount(@PathVariable Long orderId, @PathVariable BigDecimal couponAmount);

}