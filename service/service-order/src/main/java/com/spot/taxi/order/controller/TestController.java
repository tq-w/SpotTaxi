package com.spot.taxi.order.controller;

import com.spot.taxi.common.result.Result;
import com.spot.taxi.order.service.TestService;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "测试接口")
@RestController
@RequiredArgsConstructor
@RequestMapping("/order/test")
public class TestController {
    
    private final TestService testService;

    @GetMapping("testLock")
    public Result testLock() {
        testService.testLock();
        return Result.ok();
    }
}