package com.spot.taxi.driver.service.impl;

import com.spot.taxi.common.execption.CustomException;
import com.spot.taxi.common.result.Result;
import com.spot.taxi.common.result.ResultCodeEnum;
import com.spot.taxi.driver.client.DriverInfoFeignClient;
import com.spot.taxi.driver.service.LocationService;
import com.spot.taxi.map.client.LocationFeignClient;
import com.spot.taxi.model.entity.driver.DriverSet;
import com.spot.taxi.model.form.map.OrderServiceLocationForm;
import com.spot.taxi.model.form.map.UpdateDriverLocationForm;
import com.spot.taxi.model.form.map.UpdateOrderLocationForm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class LocationServiceImpl implements LocationService {
    @Autowired
    private LocationFeignClient locationFeignClient;
    @Autowired
    private DriverInfoFeignClient driverInfoFeignClient;

    @Override
    public Boolean updateDriverLocation(UpdateDriverLocationForm updateDriverLocationForm) {
        Long driverId = updateDriverLocationForm.getDriverId();
        Result<DriverSet> driverSettingResult = driverInfoFeignClient.getDriverSetting(driverId);
        DriverSet driverSet = driverSettingResult.getData();
        if (driverSet.getServiceStatus() == 1) {
            Result<Boolean> booleanResult = locationFeignClient.updateDriverLocation(updateDriverLocationForm);
            return booleanResult.getData();
        } else {
            //没有接单
            // todo这里为什么是抛出异常呢
            throw new CustomException(ResultCodeEnum.NO_START_SERVICE);
        }
    }

    @Override
    public Object updateOrderLocationToCache(UpdateOrderLocationForm updateOrderLocationForm) {
        Result<Boolean> booleanResult = locationFeignClient.updateOrderLocationToCache(updateOrderLocationForm);
        return booleanResult.getData();
    }

    @Override
    public Boolean saveOrderServiceLocation(List<OrderServiceLocationForm> orderLocationServiceFormList) {
        return locationFeignClient.saveOrderServiceLocation(orderLocationServiceFormList).getData();
    }

    // TODO: 这个貌似不属于这里
//    @Override
//    public OrderLocationVo getCacheOrderLocation(Long orderId) {
//        Result<OrderLocationVo> cacheOrderLocationResult = locationFeignClient.getCacheOrderLocation(orderId);
//        if (cacheOrderLocationResult.getCode() != 200) {
//            log.error("获取订单位置信息远程调用失败，orderId:{}", orderId);
//            throw new CustomException(ResultCodeEnum.FEIGN_FAIL);
//        }
//        return cacheOrderLocationResult.getData();
//    }
}
