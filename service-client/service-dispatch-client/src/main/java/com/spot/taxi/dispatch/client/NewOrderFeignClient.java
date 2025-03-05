package com.spot.taxi.dispatch.client;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.spot.taxi.common.fallbackFactory.SentinelFallback;
import com.spot.taxi.common.result.Result;
import com.spot.taxi.model.vo.dispatch.NewOrderTaskVo;
import com.spot.taxi.model.vo.order.NewOrderDataVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@SentinelResource(value = "NewOrderFeignClient", blockHandlerClass = SentinelFallback.class, blockHandler = "defaultBlockHandler")
@FeignClient(value = "service-dispatch")
public interface NewOrderFeignClient {
    @PostMapping("/dispatch/newOrder/addAndStartTask")
    Result<Long> addAndStartTask(@RequestBody NewOrderTaskVo newOrderDispatchVo);

    @GetMapping("/dispatch/newOrder/findNewOrderQueueData/{driverId}")
    Result<List<NewOrderDataVo>> findNewOrderQueueData(@PathVariable("driverId") Long driverId);

    @GetMapping("/dispatch/newOrder/clearNewOrderQueueData/{driverId}")
    Result<Boolean> clearNewOrderQueueData(@PathVariable("driverId") Long driverId);
}