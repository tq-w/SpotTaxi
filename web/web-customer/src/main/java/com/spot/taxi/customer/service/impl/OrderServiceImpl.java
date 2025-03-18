package com.spot.taxi.customer.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.spot.taxi.common.execption.CustomException;
import com.spot.taxi.common.result.Result;
import com.spot.taxi.common.result.ResultCodeEnum;
import com.spot.taxi.coupon.client.CouponFeignClient;
import com.spot.taxi.customer.client.CustomerInfoFeignClient;
import com.spot.taxi.customer.service.OrderService;
import com.spot.taxi.dispatch.client.NewOrderFeignClient;
import com.spot.taxi.driver.client.DriverInfoFeignClient;
import com.spot.taxi.map.client.LocationFeignClient;
import com.spot.taxi.map.client.MapFeignClient;
import com.spot.taxi.map.client.WxPayFeignClient;
import com.spot.taxi.model.entity.order.OrderInfo;
import com.spot.taxi.model.enums.OrderStatus;
import com.spot.taxi.model.form.coupon.UseCouponForm;
import com.spot.taxi.model.form.customer.ExpectOrderForm;
import com.spot.taxi.model.form.customer.SubmitOrderForm;
import com.spot.taxi.model.form.map.CalculateDrivingLineForm;
import com.spot.taxi.model.form.order.OrderInfoForm;
import com.spot.taxi.model.form.payment.CreateWxPaymentForm;
import com.spot.taxi.model.form.payment.PaymentInfoForm;
import com.spot.taxi.model.form.rules.FeeRuleRequestForm;
import com.spot.taxi.model.vo.base.PageVo;
import com.spot.taxi.model.vo.customer.ExpectOrderVo;
import com.spot.taxi.model.vo.dispatch.NewOrderTaskVo;
import com.spot.taxi.model.vo.driver.DriverInfoVo;
import com.spot.taxi.model.vo.map.DrivingLineVo;
import com.spot.taxi.model.vo.map.OrderLocationVo;
import com.spot.taxi.model.vo.map.OrderServiceLastLocationVo;
import com.spot.taxi.model.vo.order.CurrentOrderInfoVo;
import com.spot.taxi.model.vo.order.OrderBillVo;
import com.spot.taxi.model.vo.order.OrderInfoVo;
import com.spot.taxi.model.vo.order.OrderPayVo;
import com.spot.taxi.model.vo.payment.WxPrepayVo;
import com.spot.taxi.model.vo.rules.FeeRuleResponseVo;
import com.spot.taxi.order.client.OrderInfoFeignClient;
import com.spot.taxi.rules.client.FeeRuleFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderServiceImpl implements OrderService {
    
    private final MapFeignClient mapFeignClient;
    
    private final FeeRuleFeignClient feeRuleFeignClient;
    
    private final OrderInfoFeignClient orderInfoFeignClient;
    
    private final NewOrderFeignClient newOrderFeignClient;
    
    private final DriverInfoFeignClient driverInfoFeignClient;
    
    private final LocationFeignClient locationFeignClient;
    
    private final CustomerInfoFeignClient customerInfoFeignClient;
    
    private final WxPayFeignClient wxPayFeignClient;
    
    private final CouponFeignClient couponFeignClient;

    @Override
    public ExpectOrderVo expectOrder(ExpectOrderForm expectOrderForm) {
        // 获取驾驶线路和订单费用
        Result<DrivingLineVo> drivingLineVoResult = getDrivingLine(expectOrderForm);
        DrivingLineVo drivingLineVo = drivingLineVoResult.getData();

        Result<FeeRuleResponseVo> feeRuleResponseVoResult = getOrderFee(drivingLineVo.getDistance());
        FeeRuleResponseVo feeRuleResponseVo = feeRuleResponseVoResult.getData();

        //封装ExpectOrderVo
        ExpectOrderVo expectOrderVo = new ExpectOrderVo();
        expectOrderVo.setDrivingLineVo(drivingLineVo);
        expectOrderVo.setFeeRuleResponseVo(feeRuleResponseVo);
        return expectOrderVo;
    }

    @Override
    public Long submitOrder(SubmitOrderForm submitOrderForm) {
        // 重新计算驾驶线路和订单费用
        Result<DrivingLineVo> drivingLineVoResult = getDrivingLine(submitOrderForm);
        DrivingLineVo drivingLineVo = drivingLineVoResult.getData();
        Result<FeeRuleResponseVo> feeRuleResponseVoResult = getOrderFee(drivingLineVo.getDistance());
        FeeRuleResponseVo feeRuleResponseVo = feeRuleResponseVoResult.getData();

        //封装数据
        OrderInfoForm orderInfoForm = new OrderInfoForm();
        BeanUtils.copyProperties(submitOrderForm, orderInfoForm);
        orderInfoForm.setExpectDistance(drivingLineVo.getDistance());
        orderInfoForm.setExpectAmount(feeRuleResponseVo.getTotalAmount());
        Result<Long> orderInfoResult = orderInfoFeignClient.saveOrderInfo(orderInfoForm);
        Long orderId = orderInfoResult.getData();

        //查询附近可以接单司机
        NewOrderTaskVo newOrderTaskVo = new NewOrderTaskVo();
        BeanUtils.copyProperties(orderInfoForm, newOrderTaskVo);
        newOrderTaskVo.setOrderId(orderId);
        newOrderTaskVo.setExpectTime(drivingLineVo.getDuration());
        newOrderTaskVo.setCreateTime(new Date());

        Result<Long> jobIdResult = newOrderFeignClient.addAndStartTask(newOrderTaskVo);
        return orderId;
    }

    //查询订单状态
    @Override
    public Integer getOrderStatus(Long orderId) {
        Result<Integer> integerResult = orderInfoFeignClient.getOrderStatus(orderId);
        return integerResult.getData();
    }

    @Override
    public CurrentOrderInfoVo searchCustomerCurrentOrder(Long customerId) {
        Result<CurrentOrderInfoVo> currentOrderInfoVoResult = orderInfoFeignClient.searchCustomerCurrentOrder(customerId);
        return currentOrderInfoVoResult.getData();
    }

    @Override
    public OrderInfoVo getOrderInfo(Long orderId, Long customerId) {
        OrderInfo orderInfo = orderInfoFeignClient.getOrderInfo(orderId).getData();
        if (!Objects.equals(customerId, orderInfo.getCustomerId())) {
            log.error("订单信息不匹配");
            throw new CustomException(ResultCodeEnum.ILLEGAL_REQUEST);
        }

        DriverInfoVo driverInfoVo = null;
        Long driverId = orderInfo.getDriverId();
        if (driverId != null) {
            driverInfoVo = driverInfoFeignClient.getDriverInfoInOrder(driverId).getData();
        }

        OrderBillVo orderBillVo = null;
        if (orderInfo.getStatus() >= OrderStatus.UNPAID.getStatus()) {
            orderBillVo = orderInfoFeignClient.getOrderBillInfo(orderId).getData();
        }

        OrderInfoVo orderInfoVo = new OrderInfoVo();
        orderInfoVo.setOrderId(orderId);
        BeanUtils.copyProperties(orderInfo, orderInfoVo);
        orderInfoVo.setDriverInfoVo(driverInfoVo);
        orderInfoVo.setOrderBillVo(orderBillVo);

        return orderInfoVo;
    }

    @Override
    public DriverInfoVo getDriverInfo(Long orderId, Long customerId) {
        Result<OrderInfo> orderInfoResult = orderInfoFeignClient.getOrderInfo(orderId);
        OrderInfo orderInfo = orderInfoResult.getData();
        if (!customerId.equals(orderInfo.getCustomerId())) {
            log.error("订单信息不匹配");
            throw new CustomException(ResultCodeEnum.ILLEGAL_REQUEST);
        }
        Result<DriverInfoVo> driverInfoInOrderResult = driverInfoFeignClient.getDriverInfoInOrder(orderInfo.getDriverId());
        return driverInfoInOrderResult.getData();
    }

    @Override
    public DrivingLineVo calculateDrivingLine(CalculateDrivingLineForm calculateDrivingLineForm) {
        if (!(calculateDrivingLineForm.getStartPointLatitude() == null) && !(calculateDrivingLineForm.getStartPointLongitude() == null) &&
                !(calculateDrivingLineForm.getEndPointLatitude() == null) && !(calculateDrivingLineForm.getEndPointLongitude() == null)) {
            return mapFeignClient.calculateDrivingLine(calculateDrivingLineForm).getData();
        }
        DrivingLineVo drivingLineVo = new DrivingLineVo();
        drivingLineVo.setDistance(new BigDecimal(0));
        drivingLineVo.setDuration(new BigDecimal(0));
        drivingLineVo.setPolyline(JSONArray.of(""));
        return drivingLineVo;
    }

    @Override
    public OrderLocationVo getCacheOrderLocation(Long orderId) {
        Result<OrderLocationVo> cacheOrderLocationResult = locationFeignClient.getCacheOrderLocation(orderId);
        return cacheOrderLocationResult.getData();
    }

    @Override
    public OrderServiceLastLocationVo getOrderServiceLastLocation(Long orderId) {
        return locationFeignClient.getOrderServiceLastLocation(orderId).getData();
    }

    @Override
    public PageVo findCustomerOrderPage(Long customerId, Long page, Long limit) {
        return orderInfoFeignClient.findCustomerOrderPage(customerId, page, limit).getData();
    }

    @Override
    public WxPrepayVo createWxPayment(CreateWxPaymentForm createWxPaymentForm) {
        //获取订单支付信息
        OrderPayVo orderPayVo = orderInfoFeignClient.getOrderPayVo(createWxPaymentForm.getOrderNo(),
                createWxPaymentForm.getCustomerId()).getData();
        //判断
        if (!Objects.equals(orderPayVo.getStatus(), OrderStatus.UNPAID.getStatus())) {
            throw new CustomException(ResultCodeEnum.ILLEGAL_REQUEST);
        }

        //获取乘客和司机openid
        String customerOpenId = customerInfoFeignClient.getCustomerOpenId(orderPayVo.getCustomerId()).getData();

        String driverOpenId = driverInfoFeignClient.getDriverOpenId(orderPayVo.getDriverId()).getData();

        BigDecimal couponAmount = null;
        if (orderPayVo.getCouponAmount() == null && createWxPaymentForm.getCustomerCouponId() != null && createWxPaymentForm.getCustomerCouponId() != 0) {
            UseCouponForm useCouponForm = new UseCouponForm();
            useCouponForm.setOrderId(orderPayVo.getOrderId());
            useCouponForm.setCustomerCouponId(createWxPaymentForm.getCustomerCouponId());
            useCouponForm.setOrderAmount(orderPayVo.getPayAmount());
            useCouponForm.setCustomerId(createWxPaymentForm.getCustomerId());
            couponAmount = couponFeignClient.useCoupon(useCouponForm).getData();
        }
        BigDecimal payAmount = orderPayVo.getPayAmount();
        if (couponAmount != null) {
            Boolean isUpdate = orderInfoFeignClient.updateCouponAmount(orderPayVo.getOrderId(), couponAmount).getData();
            if (!isUpdate) {
                throw new CustomException(ResultCodeEnum.UPDATE_ERROR);
            }
            payAmount = payAmount.subtract(couponAmount);
        }

        //封装需要数据到实体类，远程调用发起微信支付
        PaymentInfoForm paymentInfoForm = new PaymentInfoForm();
        paymentInfoForm.setCustomerOpenId(customerOpenId);
        paymentInfoForm.setDriverOpenId(driverOpenId);
        paymentInfoForm.setOrderNo(orderPayVo.getOrderNo());
        paymentInfoForm.setAmount(payAmount);
        paymentInfoForm.setContent(orderPayVo.getContent());
        paymentInfoForm.setPayWay(1);

        return wxPayFeignClient.createWxPayment(paymentInfoForm).getData();
    }

    @Override
    public Boolean queryPayStatus(String orderNo) {
        return wxPayFeignClient.queryPayStatus(orderNo).getData();
    }

    // 提取的共用方法：获取驾驶线路
    private <T> Result<DrivingLineVo> getDrivingLine(T form) {
        CalculateDrivingLineForm calculateDrivingLineForm = new CalculateDrivingLineForm();
        BeanUtils.copyProperties(form, calculateDrivingLineForm);
        if (!(calculateDrivingLineForm.getStartPointLatitude().compareTo(BigDecimal.ZERO) == 0) || !(calculateDrivingLineForm.getStartPointLongitude().compareTo(BigDecimal.ZERO) == 0) ||
                !(calculateDrivingLineForm.getEndPointLatitude().compareTo(BigDecimal.ZERO) == 0) || !(calculateDrivingLineForm.getEndPointLongitude().compareTo(BigDecimal.ZERO) == 0)) {
            return mapFeignClient.calculateDrivingLine(calculateDrivingLineForm);
        }
        DrivingLineVo drivingLineVo = new DrivingLineVo();
        drivingLineVo.setDistance(new BigDecimal(0));
        drivingLineVo.setDuration(new BigDecimal(0));
        drivingLineVo.setPolyline(JSONArray.of(""));
        return Result.fail(drivingLineVo);
    }

    // 提取的共用方法：获取订单费用
    private Result<FeeRuleResponseVo> getOrderFee(BigDecimal distance) {
        FeeRuleRequestForm calculateOrderFeeForm = new FeeRuleRequestForm();
        calculateOrderFeeForm.setDistance(distance);
        calculateOrderFeeForm.setStartTime(new Date());
        calculateOrderFeeForm.setWaitMinute(0);
        // 直接返回 Result<FeeRuleResponseVo>，不再取 .getData()
        return feeRuleFeignClient.calculateOrderFee(calculateOrderFeeForm);
    }

}
