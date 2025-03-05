package com.spot.taxi.map.client;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.spot.taxi.common.fallbackFactory.SentinelFallback;
import com.spot.taxi.common.result.Result;
import com.spot.taxi.model.form.map.CalculateDrivingLineForm;
import com.spot.taxi.model.vo.map.DrivingLineVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@SentinelResource(value = "MapFeignClient", blockHandlerClass = SentinelFallback.class, blockHandler = "defaultBlockHandler")
@FeignClient(value = "service-map")
public interface MapFeignClient {
    @PostMapping("/map/calculateDrivingLine")
    Result<DrivingLineVo> calculateDrivingLine(@RequestBody CalculateDrivingLineForm calculateDrivingLineForm);


}