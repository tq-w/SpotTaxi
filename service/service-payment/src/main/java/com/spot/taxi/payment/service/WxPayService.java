package com.spot.taxi.payment.service;

import com.spot.taxi.model.form.payment.PaymentInfoForm;
import com.spot.taxi.model.vo.payment.WxPrepayVo;
import jakarta.servlet.http.HttpServletRequest;

public interface WxPayService {


    WxPrepayVo createWxPayment(PaymentInfoForm paymentInfoForm);

    Object queryPayStatus(String orderNo);

    void wxnotify(HttpServletRequest request);

    void handleOrder(String orderNo);
}
