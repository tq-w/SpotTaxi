package com.spot.taxi.driver.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.spot.taxi.common.fallbackFactory.SentinelFallback;
import com.spot.taxi.common.login.CheckLoginStatus;
import com.spot.taxi.common.result.Result;
import com.spot.taxi.common.util.AuthContextHolder;
import com.spot.taxi.driver.service.OrderService;
import com.spot.taxi.model.form.map.CalculateDrivingLineForm;
import com.spot.taxi.model.form.order.OrderFeeForm;
import com.spot.taxi.model.form.order.StartDriveForm;
import com.spot.taxi.model.form.order.UpdateOrderCarForm;
import com.spot.taxi.model.vo.base.PageVo;
import com.spot.taxi.model.vo.map.DrivingLineVo;
import com.spot.taxi.model.vo.order.CurrentOrderInfoVo;
import com.spot.taxi.model.vo.order.NewOrderDataVo;
import com.spot.taxi.model.vo.order.OrderInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "订单API接口管理")
@RestController
@RequestMapping("/order")
@SentinelResource(value = "OrderController", blockHandlerClass = SentinelFallback.class, blockHandler = "defaultBlockHandler")
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Operation(summary = "查询订单状态")
    @CheckLoginStatus
    @GetMapping("/getOrderStatus/{orderId}")
    public Result<Integer> getOrderStatus(@PathVariable Long orderId) {
        return Result.ok(orderService.getOrderStatus(orderId));
    }

    @Operation(summary = "司机端查找当前订单")
    @CheckLoginStatus
    @GetMapping("/searchDriverCurrentOrder")
    public Result<CurrentOrderInfoVo> searchDriverCurrentOrder() {
        Long driverId = AuthContextHolder.getUserId();
        return Result.ok(orderService.searchDriverCurrentOrder(driverId));
    }

    @Operation(summary = "查询司机新订单数据")
    @CheckLoginStatus
    @GetMapping("/findNewOrderQueueData")
    public Result<List<NewOrderDataVo>> findNewOrderQueueData() {
        Long driverId = AuthContextHolder.getUserId();
        return Result.ok(orderService.findNewOrderQueueData(driverId));
    }

    @Operation(summary = "司机抢单")
    @CheckLoginStatus
    @GetMapping("/robNewOrder/{orderId}")
    public Result<Boolean> robNewOrder(@PathVariable Long orderId) {
        Long driverId = AuthContextHolder.getUserId();
        return Result.ok(orderService.takeNewOrder(driverId, orderId));
    }

    @Operation(summary = "获取订单账单详细信息")
    @CheckLoginStatus
    @GetMapping("/getOrderInfo/{orderId}")
    public Result<OrderInfoVo> getOrderInfo(@PathVariable Long orderId) {
        Long driverId = AuthContextHolder.getUserId();
        return Result.ok(orderService.getOrderInfo(orderId, driverId));
    }


    @Operation(summary = "计算最佳驾驶线路")
    @CheckLoginStatus
    @PostMapping("/calculateDrivingLine")
    public Result<DrivingLineVo> calculateDrivingLine(@RequestBody CalculateDrivingLineForm calculateDrivingLineForm) {
        return Result.ok(orderService.calculateDrivingLine(calculateDrivingLineForm));
    }

    @Operation(summary = "司机到达代驾起始地点")
    @CheckLoginStatus
    @GetMapping("/driverArriveStartLocation/{orderId}")
    public Result<Boolean> driverArriveStartLocation(@PathVariable Long orderId) {
        Long driverId = AuthContextHolder.getUserId();
        return Result.ok(orderService.driverArriveStartLocation(orderId, driverId));
    }

    @Operation(summary = "更新代驾车辆信息")
    @CheckLoginStatus
    @PostMapping("/updateOrderCart")
    public Result<Boolean> updateOrderCart(@RequestBody UpdateOrderCarForm updateOrderCartForm) {
        Long driverId = AuthContextHolder.getUserId();
        updateOrderCartForm.setDriverId(driverId);
        return Result.ok(orderService.updateOrdeCart(updateOrderCartForm));
    }

    @Operation(summary = "开始代驾服务")
    @CheckLoginStatus
    @PostMapping("/startDrive")
    public Result<Boolean> startDriving(@RequestBody StartDriveForm startDriveForm) {
        Long driverId = AuthContextHolder.getUserId();
        startDriveForm.setDriverId(driverId);
        return Result.ok(orderService.startDriving(startDriveForm));
    }

    @Operation(summary = "结束代驾服务更新订单账单")
    @CheckLoginStatus
    @PostMapping("/endDrive")
    public Result<Boolean> endDrive(@RequestBody OrderFeeForm orderFeeForm) {
        Long driverId = AuthContextHolder.getUserId();
        orderFeeForm.setDriverId(driverId);
        return Result.ok(orderService.endDrive(orderFeeForm));
    }

    @Operation(summary = "获取司机订单分页列表")
    @CheckLoginStatus
    @GetMapping("findDriverOrderPage/{page}/{limit}")
    public Result<PageVo> findDriverOrderPage(
            @Parameter(name = "page", description = "当前页码", required = true)
            @PathVariable Long page,

            @Parameter(name = "limit", description = "每页记录数", required = true)
            @PathVariable Long limit) {
        Long driverId = AuthContextHolder.getUserId();
        PageVo pageVo = orderService.findDriverOrderPage(driverId, page, limit);
        return Result.ok(pageVo);
    }

    @Operation(summary = "司机发送账单信息")
    @CheckLoginStatus
    @GetMapping("/sendOrderBillInfo/{orderId}")
    public Result<Boolean> sendOrderBillInfo(@PathVariable Long orderId) {
        Long driverId = AuthContextHolder.getUserId();
        return Result.ok(orderService.sendOrderBillInfo(orderId, driverId));
    }
}

