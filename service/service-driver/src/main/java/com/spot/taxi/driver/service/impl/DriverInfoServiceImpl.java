package com.spot.taxi.driver.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.spot.taxi.common.constant.SystemConstant;
import com.spot.taxi.common.execption.CustomException;
import com.spot.taxi.common.result.ResultCodeEnum;
import com.spot.taxi.driver.config.TencentCloudProperties;
import com.spot.taxi.driver.mapper.*;
import com.spot.taxi.driver.service.CosService;
import com.spot.taxi.driver.service.DriverInfoService;
import com.spot.taxi.model.entity.driver.*;
import com.spot.taxi.model.form.driver.DriverFaceModelForm;
import com.spot.taxi.model.form.driver.UpdateDriverAuthInfoForm;
import com.spot.taxi.model.vo.driver.DriverAuthInfoVo;
import com.spot.taxi.model.vo.driver.DriverInfoVo;
import com.spot.taxi.model.vo.driver.DriverLoginVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tencentcloudapi.common.AbstractModel;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.iai.v20200303.IaiClient;
import com.tencentcloudapi.iai.v20200303.models.*;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Date;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class DriverInfoServiceImpl extends ServiceImpl<DriverInfoMapper, DriverInfo> implements DriverInfoService {
    @Autowired
    private WxMaService wxMaService;
    @Autowired
    private DriverInfoMapper driverInfoMapper;
    @Autowired
    private DriverSetMapper driverSetMapper;
    @Autowired
    private DriverAccountMapper driverAccountMapper;
    @Autowired
    private DriverLoginLogMapper driverLoginLogMapper;
    @Autowired
    private CosService cosService;
    @Autowired
    private TencentCloudProperties tencentCloudProperties;
    @Autowired
    private DriverFaceRecognitionMapper driverFaceRecognitionMapper;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long login(String accessCode) {
        String openid;
        try {
            WxMaJscode2SessionResult sessionInfo = wxMaService.getUserService().getSessionInfo(accessCode);
            openid = sessionInfo.getOpenid();
        } catch (WxErrorException e) {
            log.error("从服务器获取openid失败", e);
            throw new CustomException(ResultCodeEnum.DATA_ERROR);
        }
        LambdaQueryWrapper<DriverInfo> queryWrapper = new LambdaQueryWrapper();
        queryWrapper.eq(DriverInfo::getWxOpenId, openid);
        DriverInfo driverInfo = driverInfoMapper.selectOne(queryWrapper);

        if (driverInfo == null) {
            //添加司机基本信息
            driverInfo = new DriverInfo();
            driverInfo.setWxOpenId(openid);
            driverInfo.setNickname("新司机" + System.currentTimeMillis());
            driverInfo.setAvatarUrl("https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");
            driverInfoMapper.insert(driverInfo);

            //初始化司机设置
            DriverSet driverSet = new DriverSet();
            driverSet.setDriverId(driverInfo.getId());
            driverSet.setDriverId(driverInfo.getId());
            driverSet.setOrderMileageLimit(new BigDecimal(0));//0：无限制
            driverSet.setDriverPassengerDistanceLimit(new BigDecimal(SystemConstant.ACCEPT_DISTANCE));//默认接单范围：5公里
            driverSet.setIsAutoAccept(0);//0：否 1：是
            driverSetMapper.insert(driverSet);

            //初始化司机账户信息
            DriverAccount driverAccount = new DriverAccount();
            driverAccount.setDriverId(driverInfo.getId());
            driverAccountMapper.insert(driverAccount);
        }

        //记录司机登录信息
        DriverLoginLog driverLoginLog = new DriverLoginLog();
        driverLoginLog.setDriverId(driverInfo.getId());
        driverLoginLog.setMsg("小程序登录");
        driverLoginLogMapper.insert(driverLoginLog);

        return driverInfo.getId();
    }

    @Override
    public DriverLoginVo getDriverInfo(Long driverId) {
        DriverInfo driverInfo = driverInfoMapper.selectById(driverId);

        DriverLoginVo driverLoginVo = new DriverLoginVo();
        BeanUtils.copyProperties(driverInfo, driverLoginVo);

        boolean isArchiveFace = StringUtils.hasText(driverInfo.getFaceModelId());
        driverLoginVo.setIsArchiveFace(isArchiveFace);
        return driverLoginVo;
    }

    @Override
    public DriverAuthInfoVo getDriverAuthInfo(Long driverId) {
        DriverInfo driverInfo = driverInfoMapper.selectById(driverId);
        DriverAuthInfoVo driverAuthInfoVo = new DriverAuthInfoVo();
        // todo 这里好像有点问题，driverInfo中的数据是哪里来的呢
        BeanUtils.copyProperties(driverInfo, driverAuthInfoVo);

        driverAuthInfoVo.setIdcardBackShowUrl(cosService.getImageUrl(driverAuthInfoVo.getIdcardBackUrl()));
        driverAuthInfoVo.setIdcardFrontShowUrl(cosService.getImageUrl(driverAuthInfoVo.getIdcardFrontUrl()));
        driverAuthInfoVo.setIdcardHandShowUrl(cosService.getImageUrl(driverAuthInfoVo.getIdcardHandUrl()));
        driverAuthInfoVo.setDriverLicenseFrontShowUrl(cosService.getImageUrl(driverAuthInfoVo.getDriverLicenseFrontUrl()));
        driverAuthInfoVo.setDriverLicenseBackShowUrl(cosService.getImageUrl(driverAuthInfoVo.getDriverLicenseBackUrl()));
        driverAuthInfoVo.setDriverLicenseHandShowUrl(cosService.getImageUrl(driverAuthInfoVo.getDriverLicenseHandUrl()));
        return driverAuthInfoVo;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm updateDriverAuthInfoForm) {
        Long driverId = updateDriverAuthInfoForm.getDriverId();

        DriverInfo driverInfo = new DriverInfo();
        driverInfo.setId(driverId);
        BeanUtils.copyProperties(updateDriverAuthInfoForm, driverInfo);
        // 进入待审核状态
        driverInfo.setAuthStatus(1);
        // todo完成管理端后取消这行
        driverInfo.setAuthStatus(2);
        return this.updateById(driverInfo);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean creatDriverFaceModel(DriverFaceModelForm driverFaceModelForm) {
        DriverInfo driverInfo = driverInfoMapper.selectById(driverFaceModelForm.getDriverId());
        try {
            Credential cred = new Credential(tencentCloudProperties.getSecretId(),
                    tencentCloudProperties.getSecretKey());
            // 实例化一个http选项，可选的，没有特殊需求可以跳过
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("iai.tencentcloudapi.com");
            // 实例化一个client选项，可选的，没有特殊需求可以跳过
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            // 实例化要请求产品的client对象,clientProfile是可选的
            IaiClient client = new IaiClient(cred, tencentCloudProperties.getRegion(),
                    clientProfile);
            // 实例化一个请求对象,每个接口都会对应一个request对象
            CreatePersonRequest req = new CreatePersonRequest();
            //设置相关值
            req.setGroupId(tencentCloudProperties.getPersonGroupId());
            //基本信息
            req.setPersonId(String.valueOf(driverInfo.getId()));
            req.setGender(Long.parseLong(driverInfo.getGender()));
            req.setQualityControl(4L);
            req.setUniquePersonControl(4L);
            req.setPersonName(driverInfo.getName());
            req.setImage(driverFaceModelForm.getImageBase64());

            // 返回的resp是一个CreatePersonResponse的实例，与请求对象对应
            CreatePersonResponse resp = client.CreatePerson(req);
            // 输出json格式的字符串回包
            System.out.println(AbstractModel.toJsonString(resp));
            String faceId = resp.getFaceId();
            if (StringUtils.hasText(faceId)) {
                driverInfo.setFaceModelId(faceId);
                driverInfoMapper.updateById(driverInfo);
            }
        } catch (TencentCloudSDKException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public DriverSet getDriverSetting(Long driverId) {
        LambdaQueryWrapper<DriverSet> queryWrapper = new LambdaQueryWrapper();
        queryWrapper.eq(DriverSet::getDriverId, driverId);
        return driverSetMapper.selectOne(queryWrapper);
    }

    @Override
    public Boolean isFaceRecognition(Long driverId) {
        LambdaQueryWrapper<DriverFaceRecognition> queryWrapper = new LambdaQueryWrapper();
        queryWrapper.eq(DriverFaceRecognition::getDriverId, driverId);
        queryWrapper.eq(DriverFaceRecognition::getFaceDate, new DateTime().toString("yyyy-MM-dd"));
        Long count = driverFaceRecognitionMapper.selectCount(queryWrapper);
        return count > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean verifyDriverFace(DriverFaceModelForm driverFaceModelForm) {
        try {
            // 实例化一个认证对象，入参需要传入腾讯云账户 SecretId 和 SecretKey，此处还需注意密钥对的保密
            // 代码泄露可能会导致 SecretId 和 SecretKey 泄露，并威胁账号下所有资源的安全性。以下代码示例仅供参考，建议采用更安全的方式来使用密钥，请参见：https://cloud.tencent.com/document/product/1278/85305
            // 密钥可前往官网控制台 https://console.cloud.tencent.com/cam/capi 进行获取
            Credential cred = new Credential(tencentCloudProperties.getSecretId(), tencentCloudProperties.getSecretKey());
            // 实例化一个http选项，可选的，没有特殊需求可以跳过
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("iai.tencentcloudapi.com");
            // 实例化一个client选项，可选的，没有特殊需求可以跳过
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            // 实例化要请求产品的client对象,clientProfile是可选的
            IaiClient client = new IaiClient(cred, tencentCloudProperties.getRegion(), clientProfile);
            // 实例化一个请求对象,每个接口都会对应一个request对象
            VerifyFaceRequest req = new VerifyFaceRequest();
            req.setImage(driverFaceModelForm.getImageBase64());
            req.setPersonId(String.valueOf(driverFaceModelForm.getDriverId()));

            // 返回的resp是一个VerifyFaceResponse的实例，与请求对象对应
            VerifyFaceResponse resp = client.VerifyFace(req);
            // 输出json格式的字符串回包
            System.out.println(AbstractModel.toJsonString(resp));
            if (resp.getIsMatch()) {
                Boolean isSuccess = this.detectLiveFace(driverFaceModelForm.getImageBase64());
                if (isSuccess) {
                    DriverFaceRecognition driverFaceRecognition = new DriverFaceRecognition();
                    driverFaceRecognition.setDriverId(driverFaceModelForm.getDriverId());
                    // 只会保存日期部分"yyyy-MM-dd"
                    driverFaceRecognition.setFaceDate(new Date());
                    driverFaceRecognitionMapper.insert(driverFaceRecognition);
                    return true;
                }
            }

        } catch (TencentCloudSDKException e) {
            System.out.println(e.toString());
            throw new CustomException(ResultCodeEnum.DATA_ERROR);
        }
        log.info("人脸认证未通过");
        return false;
    }

    // 更新接单状态
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean updateServiceStatus(Long driverId, Integer status) {
        LambdaQueryWrapper<DriverSet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DriverSet::getDriverId, driverId);
        DriverSet driverSet = new DriverSet();
        driverSet.setServiceStatus(status);
        driverSetMapper.update(driverSet, wrapper);
        return true;
    }

    //获取司机基本信息，用于实时订单页面显示
    @Override
    public DriverInfoVo getDriverInfoOrder(Long driverId) {
        DriverInfo driverInfo = driverInfoMapper.selectById(driverId);

        DriverInfoVo driverInfoVo = new DriverInfoVo();
        BeanUtils.copyProperties(driverInfo, driverInfoVo);

        int currentYear = new DateTime().getYear();
        int thatYear = new DateTime(driverInfo.getDriverLicenseIssueDate()).getYear();
        int drivingYear = currentYear - thatYear;
        driverInfoVo.setDriverLicenseAge(drivingYear);
        return driverInfoVo;
    }

    @Override
    public String getDriverOpenId(Long driverId) {
        DriverInfo driverInfo = this.getOne(new LambdaQueryWrapper<DriverInfo>().eq(DriverInfo::getId, driverId).select(DriverInfo::getWxOpenId));
        return driverInfo.getWxOpenId();
    }

    private Boolean detectLiveFace(String imageBase64) {
        try {
            // 实例化一个认证对象，入参需要传入腾讯云账户 SecretId 和 SecretKey，此处还需注意密钥对的保密
            // 代码泄露可能会导致 SecretId 和 SecretKey 泄露，并威胁账号下所有资源的安全性。以下代码示例仅供参考，建议采用更安全的方式来使用密钥，请参见：https://cloud.tencent.com/document/product/1278/85305
            // 密钥可前往官网控制台 https://console.cloud.tencent.com/cam/capi 进行获取
            Credential cred = new Credential(tencentCloudProperties.getSecretId(), tencentCloudProperties.getSecretKey());
            // 实例化一个http选项，可选的，没有特殊需求可以跳过
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("iai.tencentcloudapi.com");
            // 实例化一个client选项，可选的，没有特殊需求可以跳过
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            // 实例化要请求产品的client对象,clientProfile是可选的
            IaiClient client = new IaiClient(cred, tencentCloudProperties.getRegion(), clientProfile);
            // 实例化一个请求对象,每个接口都会对应一个request对象
            DetectLiveFaceRequest req = new DetectLiveFaceRequest();
            req.setImage(imageBase64);
            // 返回的resp是一个DetectLiveFaceResponse的实例，与请求对象对应
            DetectLiveFaceResponse resp = client.DetectLiveFace(req);

            // 输出json格式的字符串回包
            System.out.println(AbstractModel.toJsonString(resp));
            if (resp.getIsLiveness()) {
                return true;
            }
        } catch (TencentCloudSDKException e) {
            System.out.println(e.toString());
            throw new CustomException(ResultCodeEnum.DATA_ERROR);
        }
        log.info("人脸认证未通过活体检测");
        return false;
    }
}