package com.spot.taxi.order.client;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.spot.taxi.common.fallbackFactory.SentinelFallback;
import com.spot.taxi.common.result.Result;
import com.spot.taxi.model.entity.order.OrderMonitorRecord;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@SentinelResource(value = "OrderMonitorFeignClient", blockHandlerClass = SentinelFallback.class, blockHandler = "defaultBlockHandler")
@FeignClient(value = "service-order")
public interface OrderMonitorFeignClient {
    @PostMapping("/order/monitor/saveOrderMonitorRecord")
    Result<Boolean> saveMonitorRecord(@RequestBody OrderMonitorRecord orderMonitorRecord);

}