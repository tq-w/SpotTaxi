package com.spot.taxi.payment.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.spot.taxi.common.constant.MqConst;
import com.spot.taxi.common.constant.SystemConstant;
import com.spot.taxi.common.execption.CustomException;
import com.spot.taxi.common.result.ResultCodeEnum;
import com.spot.taxi.common.service.RabbitService;
import com.spot.taxi.model.entity.payment.PaymentInfo;
import com.spot.taxi.model.entity.payment.ProfitsharingInfo;
import com.spot.taxi.model.form.payment.ProfitsharingForm;
import com.spot.taxi.payment.config.WxPayV3Properties;
import com.spot.taxi.payment.mapper.PaymentInfoMapper;
import com.spot.taxi.payment.mapper.ProfitsharingInfoMapper;
import com.spot.taxi.payment.service.WxProfitsharingService;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.service.profitsharing.ProfitsharingService;
import com.wechat.pay.java.service.profitsharing.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@Slf4j
@RequiredArgsConstructor
public class WxProfitsharingServiceImpl implements WxProfitsharingService {
    private final PaymentInfoMapper paymentInfoMapper;
    private final ProfitsharingInfoMapper profitsharingInfoMapper;
    private final WxPayV3Properties wxPayV3Properties;
    private final RSAAutoCertificateConfig rsaAutoCertificateConfig;
    private final RabbitService rabbitService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void profitsharing(ProfitsharingForm profitsharingForm) {
        //分账成功才记录分账消息，查询是否已经分账
        long count = profitsharingInfoMapper.selectCount(new LambdaQueryWrapper<ProfitsharingInfo>().eq(ProfitsharingInfo::getOrderNo, profitsharingForm.getOrderNo()));
        if (count > 0) return;

        //根据订单号获取微信支付信息
        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(new LambdaQueryWrapper<PaymentInfo>().eq(PaymentInfo::getOrderNo, profitsharingForm.getOrderNo()));

        // 构建分账service
        ProfitsharingService service = new ProfitsharingService.Builder().config(rsaAutoCertificateConfig).build();

        //添加分账接收方
        //API地址：https://pay.weixin.qq.com/wiki/doc/apiv3/apis/chapter8_1_8.shtml
        //构建分账接收方参数
        AddReceiverRequest addReceiverRequest = new AddReceiverRequest();
        addReceiverRequest.setAppid(wxPayV3Properties.getAppid());
        addReceiverRequest.setType(ReceiverType.PERSONAL_OPENID);
        addReceiverRequest.setAccount(paymentInfo.getDriverOpenId());
        addReceiverRequest.setRelationType(ReceiverRelationType.PARTNER);
        AddReceiverResponse addReceiverResponse = service.addReceiver(addReceiverRequest);
        log.info("添加分账接收方：{}", JSON.toJSONString(addReceiverResponse));

        //构建分账参数
        CreateOrderRequest request = new CreateOrderRequest();
        request.setAppid(wxPayV3Properties.getAppid());
        request.setTransactionId(paymentInfo.getTransactionId());
        //商户分账单号
        String outOrderNo = profitsharingForm.getOrderNo() + "_" + new Random().nextInt(10);
        request.setOutOrderNo(outOrderNo);

        //分账接收方列表
        List<CreateOrderReceiver> receivers = new ArrayList<>();
        CreateOrderReceiver orderReceiver = new CreateOrderReceiver();
        orderReceiver.setType("PERSONAL_OPENID");
        orderReceiver.setAccount(paymentInfo.getDriverOpenId());
        //分账金额从元转换成分
        Long amount = profitsharingForm.getAmount().multiply(new BigDecimal("100")).longValue();
        orderReceiver.setAmount(amount);
        orderReceiver.setDescription("司机代驾分账");
        receivers.add(orderReceiver);
        //分账接收方列表

        request.setReceivers(receivers);
        request.setUnfreezeUnsplit(true);
        //执行分账，返回结果
        OrdersEntity ordersEntity = service.createOrder(request);
        //分账成功
        if (ordersEntity.getState().name().equals("FINISHED")) {
            ProfitsharingInfo profitsharingInfo = new ProfitsharingInfo();
            profitsharingInfo.setOrderNo(paymentInfo.getOrderNo());
            profitsharingInfo.setTransactionId(paymentInfo.getTransactionId());
            profitsharingInfo.setOutTradeNo(outOrderNo);
            profitsharingInfo.setAmount(profitsharingInfo.getAmount());
            profitsharingInfo.setState(ordersEntity.getState().name());
            profitsharingInfo.setResponeContent(JSON.toJSONString(ordersEntity));
            profitsharingInfoMapper.insert(profitsharingInfo);

            //分账成功，发送消息
            rabbitService.sendMessage(MqConst.EXCHANGE_ORDER, MqConst.ROUTING_PROFITSHARING_SUCCESS, paymentInfo.getOrderNo());
        } else if (ordersEntity.getState().name().equals("PROCESSING")) {
            //如果状态是分账中，等待2分钟再执行分账
            rabbitService.sendDelayMessage(MqConst.EXCHANGE_PROFITSHARING, MqConst.ROUTING_PROFITSHARING, JSON.toJSONString(profitsharingForm), SystemConstant.PROFITSHARING_DELAY_TIME);
        } else {
            log.error("执行分账失败");
            throw new CustomException(ResultCodeEnum.PROFITSHARING_FAIL);
        }
    }
}
