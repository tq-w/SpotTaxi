package com.spot.taxi.driver.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.spot.taxi.common.fallbackFactory.SentinelFallback;
import com.spot.taxi.common.login.CheckLoginStatus;
import com.spot.taxi.common.result.Result;
import com.spot.taxi.common.util.AuthContextHolder;
import com.spot.taxi.driver.service.DriverService;
import com.spot.taxi.model.form.driver.DriverFaceModelForm;
import com.spot.taxi.model.form.driver.UpdateDriverAuthInfoForm;
import com.spot.taxi.model.vo.driver.DriverAuthInfoVo;
import com.spot.taxi.model.vo.driver.DriverLoginVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "司机API接口管理")
@RestController
@RequestMapping(value = "/driver")
@RequiredArgsConstructor
@SentinelResource(value = "DriverController", blockHandlerClass = SentinelFallback.class, blockHandler = "defaultBlockHandler")
@SuppressWarnings({"unchecked", "rawtypes"})
public class DriverController {

    private final DriverService driverService;

    @Operation(summary = "小程序授权登录")
    @GetMapping("/login/{code}")
    public Result<String> login(@PathVariable String code) {
        return Result.ok(driverService.login(code));
    }

    @Operation(summary = "获取司机登录信息")
    @CheckLoginStatus
    @GetMapping("/getDriverLoginInfo")
    public Result<DriverLoginVo> getDriverLoginInfo() {
        Long driverId = AuthContextHolder.getUserId();
        return Result.ok(driverService.getDriverInfo(driverId));
    }

    @Operation(summary = "获取司机认证信息")
    @CheckLoginStatus
    @GetMapping("/getDriverAuthInfo")
    public Result<DriverAuthInfoVo> getDriverAuthInfo() {
        Long driverId = AuthContextHolder.getUserId();
        return Result.ok(driverService.getDriverAuthInfo(driverId));
    }

    @Operation(summary = "更新司机认证信息")
    @CheckLoginStatus
    @PostMapping("/updateDriverAuthInfo")
    public Result<Boolean> updateDriverAuthInfo(@RequestBody UpdateDriverAuthInfoForm updateDriverAuthInfoForm) {
        updateDriverAuthInfoForm.setDriverId(AuthContextHolder.getUserId());
        Boolean isSuccess = driverService.updateDriverAuthInfo(updateDriverAuthInfoForm);
        return Result.ok(isSuccess);
    }

    @Operation(summary = "创建司机人脸模型")
    @CheckLoginStatus
    @PostMapping("/creatDriverFaceModel")
    public Result<Boolean> creatDriverFaceModel(@RequestBody DriverFaceModelForm driverFaceModelForm) {
        driverFaceModelForm.setDriverId(AuthContextHolder.getUserId());
        Boolean isSuccess = driverService.creatDriverFaceModel(driverFaceModelForm);
        return Result.ok(isSuccess);
    }

    @Operation(summary = "判断司机当日是否进行过人脸识别")
    @CheckLoginStatus
    @GetMapping("/isFaceRecognition")
    Result<Boolean> isFaceRecognition() {
        Long driverId = AuthContextHolder.getUserId();
        return Result.ok(driverService.isFaceRecognition(driverId));
    }

    @Operation(summary = "验证司机人脸")
    @CheckLoginStatus
    @PostMapping("/verifyDriverFace")
    public Result<Boolean> verifyDriverFace(@RequestBody DriverFaceModelForm driverFaceModelForm) {
        driverFaceModelForm.setDriverId(AuthContextHolder.getUserId());
        return Result.ok(driverService.verifyDriverFace(driverFaceModelForm));
    }

    @Operation(summary = "开始接单服务")
    @CheckLoginStatus
    @GetMapping("/startService")
    public Result<Boolean> startService() {
        Long driverId = AuthContextHolder.getUserId();
        return Result.ok(driverService.startService(driverId));
    }

    @Operation(summary = "停止接单服务")
    @CheckLoginStatus
    @GetMapping("/stopService")
    public Result<Boolean> stopService() {
        Long driverId = AuthContextHolder.getUserId();
        return Result.ok(driverService.stopService(driverId));
    }

}

