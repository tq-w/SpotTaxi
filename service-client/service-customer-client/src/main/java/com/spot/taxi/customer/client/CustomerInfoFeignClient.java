package com.spot.taxi.customer.client;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.spot.taxi.common.result.Result;
import com.spot.taxi.model.form.customer.UpdateWxPhoneForm;
import com.spot.taxi.model.vo.customer.CustomerLoginVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@SentinelResource(value = "CustomerInfoFeignClient", blockHandler = "CustomerInfoFeignClientFallbackFactory.class")
@FeignClient(value = "service-customer")
public interface CustomerInfoFeignClient {
    // 不要忘记加上前缀/customer/info
    @GetMapping("/customer/info/login/{code}")
    Result<Long> login(@PathVariable String code);

    @GetMapping("/customer/info/getCustomerLoginInfo/{customerId}")
    Result<CustomerLoginVo> getCustomerLoginInfo(@PathVariable Long customerId);

    @PostMapping("/customer/info/updateWxPhoneNumber")
    Result<Boolean> updateWxPhoneNumber(@RequestBody UpdateWxPhoneForm updateWxPhoneForm);

    @GetMapping("/customer/info/getCustomerOpenId/{customerId}")
    Result<String> getCustomerOpenId(@PathVariable("customerId") Long customerId);
}


