package com.spot.taxi.customer.service.impl;

import com.spot.taxi.common.constant.RedisConstant;
import com.spot.taxi.common.execption.CustomException;
import com.spot.taxi.common.result.Result;
import com.spot.taxi.common.result.ResultCodeEnum;
import com.spot.taxi.customer.client.CustomerInfoFeignClient;
import com.spot.taxi.customer.service.CustomerService;
import com.spot.taxi.model.form.customer.UpdateWxPhoneForm;
import com.spot.taxi.model.vo.customer.CustomerLoginVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class CustomerServiceImpl implements CustomerService {
    @Autowired
    private CustomerInfoFeignClient customerInfoFeignClient;
    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;



    @Override
    public String wxLogin(String accessCode) {
        Result<Long> loginResult = customerInfoFeignClient.login(accessCode);

        Long customerId = loginResult.getData();

        // todo 换一种token生成方式，比如jwt
        String token = UUID.randomUUID().toString().replaceAll("-", "");

        redisTemplate.opsForValue().set(RedisConstant.USER_LOGIN_KEY_PREFIX + token,
                customerId.toString(),
                RedisConstant.USER_LOGIN_KEY_TIMEOUT,
                TimeUnit.SECONDS);

        return token;
    }

    @Override
    public CustomerLoginVo getCustomerLoginInfo(String token) {
        String customerId = (String) redisTemplate.opsForValue().get(RedisConstant.USER_LOGIN_KEY_PREFIX + token);
        // todo 有人说要在网关来判断 p32
        if (!StringUtils.hasText(customerId)) {
            log.error("token无效");
            throw new CustomException(ResultCodeEnum.DATA_ERROR);
        }
        Result<CustomerLoginVo> customerLoginVoResult = customerInfoFeignClient.getCustomerLoginInfo(Long.valueOf(customerId));

        return customerLoginVoResult.getData();

    }

    @Override
    public CustomerLoginVo getCustomerInfo(Long customerId) {
        System.out.println("customerId = " + customerId);
        Result<CustomerLoginVo> customerLoginVoResult = customerInfoFeignClient.getCustomerLoginInfo(customerId);

        return customerLoginVoResult.getData();
    }

    @Override
    public Boolean updateWxPhoneNumber(UpdateWxPhoneForm updateWxPhoneForm) {
        Result<Boolean> updateWxPhoneFormResult = customerInfoFeignClient.updateWxPhoneNumber(updateWxPhoneForm);
        // todo这里原来是true，之后检查一下
        return updateWxPhoneFormResult.getData();
    }


}
