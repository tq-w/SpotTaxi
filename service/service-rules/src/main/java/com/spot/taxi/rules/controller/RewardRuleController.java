package com.spot.taxi.rules.controller;

import com.spot.taxi.common.result.Result;
import com.spot.taxi.model.form.rules.RewardRuleRequestForm;
import com.spot.taxi.model.vo.rules.RewardRuleResponseVo;
import com.spot.taxi.rules.service.RewardRuleService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/rules/reward")
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "rawtypes"})
public class RewardRuleController {
    
    private final RewardRuleService rewardRuleService;

    @Operation(summary = "计算订单奖励费用")
    @PostMapping("/calculateOrderRewardFee")
    public Result<RewardRuleResponseVo>
    calculateOrderRewardFee(@RequestBody RewardRuleRequestForm rewardRuleRequestForm) {
        return Result.ok(rewardRuleService.calculateOrderRewardFee(rewardRuleRequestForm));
    }

}

