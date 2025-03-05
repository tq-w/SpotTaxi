package com.spot.taxi.map.service;

import com.spot.taxi.model.form.map.OrderServiceLocationForm;
import com.spot.taxi.model.form.map.SearchNearbyDriverForm;
import com.spot.taxi.model.form.map.UpdateDriverLocationForm;
import com.spot.taxi.model.form.map.UpdateOrderLocationForm;
import com.spot.taxi.model.vo.map.NearByDriverVo;
import com.spot.taxi.model.vo.map.OrderLocationVo;
import com.spot.taxi.model.vo.map.OrderServiceLastLocationVo;

import java.math.BigDecimal;
import java.util.List;

public interface LocationService {

    Boolean updateDriverLocation(UpdateDriverLocationForm updateDriverLocationForm);

    Boolean removeDriverLocation(Long driverId);

    List<NearByDriverVo> searchNearbyDriver(SearchNearbyDriverForm searchNearbyDriverForm);

    Boolean updateOrderLocationToCache(UpdateOrderLocationForm updateOrderLocationForm);

    OrderLocationVo getCacheOrderLocation(Long orderId);

    Boolean saveOrderServiceLocation(List<OrderServiceLocationForm> orderLocationServiceFormList);

    OrderServiceLastLocationVo getOrderServiceLastLocation(Long orderId);

    BigDecimal calculateOrderRealDistance(Long orderId);
}
