package com.spot.taxi.order.controller;

import com.spot.taxi.common.result.Result;
import com.spot.taxi.model.entity.order.OrderMonitorRecord;
import com.spot.taxi.order.service.OrderMonitorService;
import io.swagger.v3.oas.annotations.Operation;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/order/monitor")
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderMonitorController {
    
    private final OrderMonitorService orderMonitorService;

    @Operation(summary = "保存订单监控记录数据")
    @PostMapping("/saveOrderMonitorRecord")
    public Result<Boolean> saveMonitorRecord(@RequestBody OrderMonitorRecord orderMonitorRecord) {
        return Result.ok(orderMonitorService.saveOrderMonitorRecord(orderMonitorRecord));
    }

}

