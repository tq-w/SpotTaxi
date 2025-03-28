package com.spot.taxi.driver.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.spot.taxi.common.fallbackFactory.SentinelFallback;
import com.spot.taxi.driver.service.MonitorService;
import com.spot.taxi.model.form.order.OrderMonitorForm;
import com.spot.taxi.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Tag(name = "监控接口管理")
@RestController
@RequestMapping(value="/monitor")
@RequiredArgsConstructor
@SentinelResource(value = "MonitorController", blockHandlerClass = SentinelFallback.class, blockHandler = "defaultBlockHandler")
@SuppressWarnings({"unchecked", "rawtypes"})
public class MonitorController {

    private final MonitorService monitorService;

    @Operation(summary = "上传录音")
    @PostMapping("/upload")
    public Result<Boolean> upload(@RequestParam("file") MultipartFile file,
                                  OrderMonitorForm orderMonitorForm) {

        return Result.ok(monitorService.upload(file, orderMonitorForm));
    }

}

