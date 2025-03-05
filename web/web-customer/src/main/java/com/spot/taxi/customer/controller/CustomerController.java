package com.spot.taxi.customer.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.spot.taxi.common.fallbackFactory.SentinelFallback;
import com.spot.taxi.common.login.CheckLoginStatus;
import com.spot.taxi.common.result.Result;
import com.spot.taxi.common.util.AuthContextHolder;
import com.spot.taxi.customer.service.CustomerService;
import com.spot.taxi.model.form.customer.UpdateWxPhoneForm;
import com.spot.taxi.model.vo.customer.CustomerLoginVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "客户API接口管理")
@RestController
@RequestMapping("/customer")
@SentinelResource(value = "CustomerController", blockHandlerClass = SentinelFallback.class, blockHandler = "defaultBlockHandler")
@SuppressWarnings({"unchecked", "rawtypes"})
public class CustomerController {
    @Autowired
    private CustomerService customerService;

    @Operation(summary = "小程序授权登录")
    @GetMapping("/login/{code}")
    public Result<String> wxLogin(@PathVariable String code) {
        return Result.ok(customerService.wxLogin(code));
    }

    @Operation(summary = "获取客户登录信息")
//    @GetMapping("/getCustomerLoginInfo")
    public Result<CustomerLoginVo> getCustomerLoginInfo(@RequestHeader(value = "token") String token) {
        return Result.ok(customerService.getCustomerLoginInfo(token));
    }

    @Operation(summary = "获取客户登录信息")
    @CheckLoginStatus
    @GetMapping("/getCustomerLoginInfo")
    public Result<CustomerLoginVo> getCustomerLoginInfov2() {
        Long userId = AuthContextHolder.getUserId();
        return Result.ok(customerService.getCustomerInfo(userId));
    }

    @Operation(summary = "更新用户微信手机号")
    @CheckLoginStatus
    @PostMapping("/updateWxPhone")
    public Result updateWxPhone(@RequestBody UpdateWxPhoneForm updateWxPhoneForm) {
        System.out.println(updateWxPhoneForm);
        updateWxPhoneForm.setCustomerId(AuthContextHolder.getUserId());
        return Result.ok(customerService.updateWxPhoneNumber(updateWxPhoneForm));
    }
}

