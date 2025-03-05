package com.spot.taxi.driver.service;

import com.spot.taxi.model.form.driver.DriverFaceModelForm;
import com.spot.taxi.model.form.driver.UpdateDriverAuthInfoForm;
import com.spot.taxi.model.vo.driver.DriverAuthInfoVo;
import com.spot.taxi.model.vo.driver.DriverLoginVo;

public interface DriverService {
    String login(String accessCode);

    DriverLoginVo getDriverInfo(Long driverId);

    DriverAuthInfoVo getDriverAuthInfo(Long driverId);

    Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm updateDriverAuthInfoForm);

    Boolean creatDriverFaceModel(DriverFaceModelForm driverFaceModelForm);

    Boolean isFaceRecognition(Long driverId);

    Boolean verifyDriverFace(DriverFaceModelForm driverFaceModelForm);

    Boolean startService(Long driverId);

    Boolean stopService(Long driverId);
}
