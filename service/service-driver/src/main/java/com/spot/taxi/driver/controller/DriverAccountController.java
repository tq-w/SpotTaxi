package com.spot.taxi.driver.controller;

import com.spot.taxi.driver.service.DriverAccountService;
import com.spot.taxi.model.form.driver.TransferForm;
import com.spot.taxi.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "司机账户API接口管理")
@RestController
@RequestMapping(value="/driver/account")
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "rawtypes"})
public class DriverAccountController {
    
    private final DriverAccountService driverAccountService;

    @Operation(summary = "转账")
    @PostMapping("/transfer")
    public Result<Boolean> transfer(@RequestBody TransferForm transferForm) {
        return Result.ok(driverAccountService.transfer(transferForm));
    }

}

