package com.spot.taxi.customer.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.binarywang.wx.miniapp.bean.WxMaPhoneNumberInfo;
import com.spot.taxi.common.execption.CustomException;
import com.spot.taxi.common.result.ResultCodeEnum;
import com.spot.taxi.customer.mapper.CustomerInfoMapper;
import com.spot.taxi.customer.mapper.CustomerLoginLogMapper;
import com.spot.taxi.customer.service.CustomerInfoService;
import com.spot.taxi.model.entity.customer.CustomerInfo;
import com.spot.taxi.model.entity.customer.CustomerLoginLog;
import com.spot.taxi.model.form.customer.UpdateWxPhoneForm;
import com.spot.taxi.model.vo.customer.CustomerLoginVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class CustomerInfoServiceImpl extends ServiceImpl<CustomerInfoMapper, CustomerInfo> implements CustomerInfoService {

    @Autowired
    private WxMaService wxMaService;

    @Autowired
    private CustomerInfoMapper customerInfoMapper;

    @Autowired
    private CustomerLoginLogMapper customerLoginLogMapper;

    @Autowired
    @Qualifier("logThreadPool")
    private ThreadPoolExecutor logThreadPool;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long login(String accessCode) {
        // 获取code值，通过code值获取openid，此处的code是小程序临时登录凭证
        String openid;
        try {
            WxMaJscode2SessionResult sessionInfo = wxMaService.getUserService().getSessionInfo(accessCode);
            openid = sessionInfo.getOpenid();
            // todo openid没有判空，也有人说它不会为空
        } catch (WxErrorException e) {
            throw new RuntimeException(e);
        }

        // 根据openid查询表，看是否存在该用户
        LambdaQueryWrapper<CustomerInfo> queryWrapper = new LambdaQueryWrapper();
        queryWrapper.eq(CustomerInfo::getWxOpenId, openid);
        CustomerInfo customerInfo = customerInfoMapper.selectOne(queryWrapper);
        if (customerInfo == null) {
            // 如果不存在，插入一条新的记录
            customerInfo = new CustomerInfo();
            customerInfo.setWxOpenId(openid);
            customerInfo.setNickname("新用户" + System.currentTimeMillis());
            customerInfo.setAvatarUrl("https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");
            customerInfoMapper.insert(customerInfo);
        }

        // 使用异步方式进行日志记录
        CustomerInfo finalCustomerInfo = customerInfo;
        CompletableFuture.runAsync(() -> {
            CustomerLoginLog customerLoginLog = new CustomerLoginLog();
            customerLoginLog.setCustomerId(finalCustomerInfo.getId());
            customerLoginLog.setMsg("小程序登录");
            customerLoginLogMapper.insert(customerLoginLog);
        }, logThreadPool);

        return customerInfo.getId();
    }

    @Override
    public CustomerLoginVo getCustomerInfo(Long customerId) {
        CustomerLoginVo customerLoginVo = new CustomerLoginVo();
        CustomerInfo customerInfo = customerInfoMapper.selectById(customerId);
        BeanUtils.copyProperties(customerInfo, customerLoginVo);

        boolean isBindPhone = StringUtils.hasText(customerInfo.getPhone());
        customerLoginVo.setIsBindPhone(isBindPhone);
        return customerLoginVo;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean updateWxPhoneNumber(UpdateWxPhoneForm updateWxPhoneForm) {
        System.out.println(updateWxPhoneForm);
        try {
//            WxMaPhoneNumberInfo phoneNoInfo = wxMaService.getUserService().getPhoneNoInfo(updateWxPhoneForm.getAccessCode());
//            String phone = phoneNoInfo.getPhoneNumber();
            // todo 个人开发者无获取用户号码权限，模拟获取手机号码
            String phone = "12345678901";
            Long customerId = updateWxPhoneForm.getCustomerId();
            CustomerInfo customerInfo = customerInfoMapper.selectById(customerId);
            customerInfo.setPhone(phone);
            customerInfoMapper.updateById(customerInfo);
            return true;
        } catch (Exception e) {
            log.error("获取手机号码失败", e);
            throw new CustomException(ResultCodeEnum.LOGIN_AUTH);
        }
    }

    @Override
    public String getCustomerOpenId(Long customerId) {
        LambdaQueryWrapper<CustomerInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerInfo::getId, customerId);
        CustomerInfo customerInfo = customerInfoMapper.selectOne(wrapper);
        return customerInfo.getWxOpenId();
    }
}
