package com.spot.taxi.customer.service;

import com.spot.taxi.model.form.customer.ExpectOrderForm;
import com.spot.taxi.model.form.customer.SubmitOrderForm;
import com.spot.taxi.model.form.map.CalculateDrivingLineForm;
import com.spot.taxi.model.form.payment.CreateWxPaymentForm;
import com.spot.taxi.model.vo.base.PageVo;
import com.spot.taxi.model.vo.customer.ExpectOrderVo;
import com.spot.taxi.model.vo.driver.DriverInfoVo;
import com.spot.taxi.model.vo.map.DrivingLineVo;
import com.spot.taxi.model.vo.map.OrderLocationVo;
import com.spot.taxi.model.vo.map.OrderServiceLastLocationVo;
import com.spot.taxi.model.vo.order.CurrentOrderInfoVo;
import com.spot.taxi.model.vo.order.OrderInfoVo;
import com.spot.taxi.model.vo.payment.WxPrepayVo;

public interface OrderService {

    ExpectOrderVo expectOrder(ExpectOrderForm expectOrderForm);

    Long submitOrder(SubmitOrderForm submitOrderForm);

    Integer getOrderStatus(Long orderId);

    CurrentOrderInfoVo searchCustomerCurrentOrder(Long customerId);

    OrderInfoVo getOrderInfo(Long orderId, Long customerId);

    DriverInfoVo getDriverInfo(Long orderId, Long customerId);

    DrivingLineVo calculateDrivingLine(CalculateDrivingLineForm calculateDrivingLineForm);

    OrderLocationVo getCacheOrderLocation(Long orderId);

    OrderServiceLastLocationVo getOrderServiceLastLocation(Long orderId);

    PageVo findCustomerOrderPage(Long customerId, Long page, Long limit);

    WxPrepayVo createWxPayment(CreateWxPaymentForm createWxPaymentForm);

    Boolean queryPayStatus(String orderNo);
}
