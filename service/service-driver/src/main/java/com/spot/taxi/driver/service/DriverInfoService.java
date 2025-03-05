package com.spot.taxi.driver.service;

import com.spot.taxi.model.entity.driver.DriverInfo;
import com.spot.taxi.model.entity.driver.DriverSet;
import com.spot.taxi.model.form.driver.DriverFaceModelForm;
import com.spot.taxi.model.form.driver.UpdateDriverAuthInfoForm;
import com.spot.taxi.model.vo.driver.DriverAuthInfoVo;
import com.spot.taxi.model.vo.driver.DriverInfoVo;
import com.spot.taxi.model.vo.driver.DriverLoginVo;
import com.baomidou.mybatisplus.extension.service.IService;

public interface DriverInfoService extends IService<DriverInfo> {
    Long login(String accessCode);

    DriverLoginVo getDriverInfo(Long driverId);

    DriverAuthInfoVo getDriverAuthInfo(Long driverId);

    Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm updateDriverAuthInfoForm);

    Boolean creatDriverFaceModel(DriverFaceModelForm driverFaceModelForm);

    DriverSet getDriverSetting(Long driverId);

    Boolean isFaceRecognition(Long driverId);

    Boolean verifyDriverFace(DriverFaceModelForm driverFaceModelForm);

    Boolean updateServiceStatus(Long driverId, Integer status);

    DriverInfoVo getDriverInfoOrder(Long driverId);

    String getDriverOpenId(Long driverId);
}
