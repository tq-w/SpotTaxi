package com.spot.taxi.driver.service.impl;

import com.spot.taxi.common.constant.RedisConstant;
import com.spot.taxi.common.execption.CustomException;
import com.spot.taxi.common.result.Result;
import com.spot.taxi.common.result.ResultCodeEnum;
import com.spot.taxi.dispatch.client.NewOrderFeignClient;
import com.spot.taxi.driver.client.DriverInfoFeignClient;
import com.spot.taxi.driver.service.DriverService;
import com.spot.taxi.map.client.LocationFeignClient;
import com.spot.taxi.model.form.driver.DriverFaceModelForm;
import com.spot.taxi.model.form.driver.UpdateDriverAuthInfoForm;
import com.spot.taxi.model.vo.driver.DriverAuthInfoVo;
import com.spot.taxi.model.vo.driver.DriverLoginVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class DriverServiceImpl implements DriverService {
    @Autowired
    private DriverInfoFeignClient driverInfoFeignClient;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private LocationFeignClient locationFeignClient;
    @Autowired
    private NewOrderFeignClient newOrderFeignClient;

    @Override
    public String login(String accessCode) {
        Result<Long> loginResult = driverInfoFeignClient.login(accessCode);
        Long driverId = loginResult.getData();
        String token = UUID.randomUUID().toString().replaceAll("-", "");

        redisTemplate.opsForValue().set(RedisConstant.USER_LOGIN_KEY_PREFIX + token,
                driverId.toString(),
                RedisConstant.USER_LOGIN_KEY_TIMEOUT, TimeUnit.SECONDS);
        return token;
    }

    @Override
    public DriverLoginVo getDriverInfo(Long driverId) {
        Result<DriverLoginVo> driverLoginInfoVoResult = driverInfoFeignClient.getDriverLoginInfo(driverId);
        return driverLoginInfoVoResult.getData();
    }

    @Override
    public DriverAuthInfoVo getDriverAuthInfo(Long driverId) {
        Result<DriverAuthInfoVo> driverAuthInfoVoResult = driverInfoFeignClient.getDriverAuthInfo(driverId);
        return driverAuthInfoVoResult.getData();
    }

    @Override
    public Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm updateDriverAuthInfoForm) {
        Result<Boolean> booleanResult = driverInfoFeignClient.updateDriverAuthInfo(updateDriverAuthInfoForm);
        return booleanResult.getData();
    }

    @Override
    public Boolean creatDriverFaceModel(DriverFaceModelForm driverFaceModelForm) {
        Result<Boolean> booleanResult = driverInfoFeignClient.creatDriverFaceModel(driverFaceModelForm);
        return booleanResult.getData();
    }

    @Override
    public Boolean isFaceRecognition(Long driverId) {
        Result<Boolean> faceRecognitionResult = driverInfoFeignClient.isFaceRecognition(driverId);
        return faceRecognitionResult.getData();
    }

    @Override
    public Boolean verifyDriverFace(DriverFaceModelForm driverFaceModelForm) {
        Result<Boolean> booleanResult = driverInfoFeignClient.verifyDriverFace(driverFaceModelForm);
        return booleanResult.getData();
    }

    @Override
    public Boolean startService(Long driverId) {
        //1 判断完成认证
        Result<DriverLoginVo> driverLoginInfoResult = driverInfoFeignClient.getDriverLoginInfo(driverId);
        DriverLoginVo driverLoginVo = driverLoginInfoResult.getData();
        // todo 弄一个枚举
        if (driverLoginVo.getAuthStatus() != 2) {
            throw new CustomException(ResultCodeEnum.AUTH_ERROR);
        }
        //2 判断当日是否人脸识别
        Result<Boolean> faceRecognitionResult = driverInfoFeignClient.isFaceRecognition(driverId);
        Boolean isFaceRecognition = faceRecognitionResult.getData();
        if (!isFaceRecognition) {
            // todo是应该抛出异常还是返回false
            throw new CustomException(ResultCodeEnum.FACE_ERROR);
        }

        locationFeignClient.removeDriverLocation(driverId);
        newOrderFeignClient.clearNewOrderQueueData(driverId);
        driverInfoFeignClient.updateServiceStatus(driverId, 1);
        return true;
    }

    @Override
    public Boolean stopService(Long driverId) {
        //更新司机的接单状态 0
        driverInfoFeignClient.updateServiceStatus(driverId,0);

        //删除司机位置信息
        locationFeignClient.removeDriverLocation(driverId);

        //清空司机临时队列
        newOrderFeignClient.clearNewOrderQueueData(driverId);
        return true;
    }
}
