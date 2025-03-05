package com.spot.taxi.driver.client;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.spot.taxi.common.fallbackFactory.SentinelFallback;
import com.spot.taxi.common.result.Result;
import com.spot.taxi.model.vo.order.TextAuditingVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@SentinelResource(value = "CiFeignClient", blockHandlerClass = SentinelFallback.class, blockHandler = "defaultBlockHandler")
@FeignClient(value = "service-driver")
public interface CiFeignClient {
    @PostMapping("/ci/textAuditing")
    Result<TextAuditingVo> textAuditing(@RequestBody String content);
}