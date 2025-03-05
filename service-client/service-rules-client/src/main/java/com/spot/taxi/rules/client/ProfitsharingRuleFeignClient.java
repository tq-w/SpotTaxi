package com.spot.taxi.rules.client;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.spot.taxi.common.fallbackFactory.SentinelFallback;
import com.spot.taxi.common.result.Result;
import com.spot.taxi.model.form.rules.ProfitsharingRuleRequestForm;
import com.spot.taxi.model.vo.rules.ProfitsharingRuleResponseVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@SentinelResource(value = "ProfitsharingRuleFeignClient", blockHandlerClass = SentinelFallback.class, blockHandler = "defaultBlockHandler")
@FeignClient(value = "service-rules")
public interface ProfitsharingRuleFeignClient {
    @PostMapping("/rules/profitsharing/calculateOrderProfitsharingFee")
    Result<ProfitsharingRuleResponseVo> calculateOrderProfitsharingFee(@RequestBody ProfitsharingRuleRequestForm profitsharingRuleRequestForm);
}