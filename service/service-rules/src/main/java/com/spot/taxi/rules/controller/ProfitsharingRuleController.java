package com.spot.taxi.rules.controller;

import com.spot.taxi.common.result.Result;
import com.spot.taxi.model.form.rules.ProfitsharingRuleRequestForm;
import com.spot.taxi.model.vo.rules.ProfitsharingRuleResponseVo;
import com.spot.taxi.rules.service.ProfitsharingRuleService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/rules/profitsharing")
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "rawtypes"})
public class ProfitsharingRuleController {
    
    private final ProfitsharingRuleService profitsharingRuleService;

    @Operation(summary = "计算系统分账费用")
    @PostMapping("/calculateOrderProfitsharingFee")
    public Result<ProfitsharingRuleResponseVo> calculateOrderProfitsharingFee(@RequestBody ProfitsharingRuleRequestForm profitsharingRuleRequestForm) {
        return Result.ok(profitsharingRuleService.calculateOrderProfitsharingFee(profitsharingRuleRequestForm));
    }

}

