package com.spot.taxi.map.client;

import com.spot.taxi.common.result.Result;
import com.spot.taxi.model.form.map.OrderServiceLocationForm;
import com.spot.taxi.model.form.map.SearchNearbyDriverForm;
import com.spot.taxi.model.form.map.UpdateDriverLocationForm;
import com.spot.taxi.model.form.map.UpdateOrderLocationForm;
import com.spot.taxi.model.vo.map.NearByDriverVo;
import com.spot.taxi.model.vo.map.OrderLocationVo;
import com.spot.taxi.model.vo.map.OrderServiceLastLocationVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@FeignClient(value = "service-map")
public interface LocationFeignClient {
    @PostMapping("/map/location/updateDriverLocation")
    Result<Boolean> updateDriverLocation(@RequestBody UpdateDriverLocationForm updateDriverLocationForm);

    @DeleteMapping("/map/location/removeDriverLocation/{driverId}")
    Result<Boolean> removeDriverLocation(@PathVariable("driverId") Long driverId);

    @PostMapping("/map/location/searchNearByDriver")
    Result<List<NearByDriverVo>> searchNearbyDriver(@RequestBody SearchNearbyDriverForm searchNearbyDriverForm);

    // 司机赶往代驾起始点：更新订单地址到缓存
    @PostMapping("/map/location/updateOrderLocationToCache")
    Result<Boolean> updateOrderLocationToCache(@RequestBody UpdateOrderLocationForm updateOrderLocationForm);

    @GetMapping("/map/location/getCacheOrderLocation/{orderId}")
    Result<OrderLocationVo> getCacheOrderLocation(@PathVariable("orderId") Long orderId);

    @PostMapping("/map/location/saveOrderServiceLocation")
    Result<Boolean> saveOrderServiceLocation(@RequestBody List<OrderServiceLocationForm> orderLocationServiceFormList);

    @GetMapping("/map/location/getOrderServiceLastLocation/{orderId}")
    Result<OrderServiceLastLocationVo> getOrderServiceLastLocation(@PathVariable Long orderId);

    @GetMapping("/map/location/calculateOrderRealDistance/{orderId}")
    Result<BigDecimal> calculateOrderRealDistance(@PathVariable Long orderId);

}