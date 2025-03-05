package com.spot.taxi.map.client;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.spot.taxi.common.fallbackFactory.SentinelFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.spot.taxi.common.result.Result;
import com.spot.taxi.model.form.payment.PaymentInfoForm;
import com.spot.taxi.model.vo.payment.WxPrepayVo;

@SentinelResource(value = "WxPayFeignClient", blockHandlerClass = SentinelFallback.class, blockHandler = "defaultBlockHandler")
@FeignClient(value = "service-payment")
public interface WxPayFeignClient {
    @PostMapping("/payment/wxPay/createWxPayment")
    Result<WxPrepayVo> createWxPayment(@RequestBody PaymentInfoForm paymentInfoForm);

    @GetMapping("/payment/wxPay/queryPayStatus/{orderNo}")
    Result<Boolean> queryPayStatus(@PathVariable("orderNo") String orderNo);
}