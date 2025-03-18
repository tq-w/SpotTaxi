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
import com.wechat.pay.java.service.profitsharing.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Primary
@Service
@Slf4j
@RequiredArgsConstructor
public class WxProfitsharingServiceImplMock implements WxProfitsharingService {
    
    private final PaymentInfoMapper paymentInfoMapper;
    
    private final ProfitsharingInfoMapper profitsharingInfoMapper;
    
    private final WxPayV3Properties wxPayV3Properties;
    
    private final RSAAutoCertificateConfig rsaAutoCertificateConfig;
    
    private final RabbitService rabbitService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void profitsharing(ProfitsharingForm profitsharingForm) {
        // 1. 保留防重逻辑
        long count = profitsharingInfoMapper.selectCount(
                new LambdaQueryWrapper<ProfitsharingInfo>()
                        .eq(ProfitsharingInfo::getOrderNo, profitsharingForm.getOrderNo()));
        if (count > 0) return;

        // 2. 获取支付信息（保持原有逻辑）
        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(
                new LambdaQueryWrapper<PaymentInfo>()
                        .eq(PaymentInfo::getOrderNo, profitsharingForm.getOrderNo()));

        // 3. 模拟分账流程（替换微信SDK调用）
        try {
            // ========== 模拟添加接收方 ==========
            log.info("[模拟] 添加分账接收方成功 - openid: {}", paymentInfo.getDriverOpenId());

            // ========== 模拟分账请求 ==========
            OrdersEntity mockOrder = new OrdersEntity();
            String outOrderNo = profitsharingForm.getOrderNo() + "_" + new Random().nextInt(10);

            // 模拟分账结果（可配置化）
            String mockState = determineMockState(profitsharingForm.getOrderNo());
            mockOrder.setState(OrderStatus.valueOf(mockState));
            mockOrder.setOutOrderNo(outOrderNo);

            // ========== 处理分账结果 ==========
            if ("FINISHED".equals(mockState)) {
                ProfitsharingInfo info = new ProfitsharingInfo();
                info.setOrderNo(paymentInfo.getOrderNo());
                info.setTransactionId(paymentInfo.getTransactionId());
                info.setOutTradeNo(outOrderNo);
                info.setAmount(String.valueOf(profitsharingForm.getAmount()));
                info.setState(mockState);
                info.setResponeContent("{\"mock\": true}");
                profitsharingInfoMapper.insert(info);

                // 发送成功消息
                rabbitService.sendMessage(MqConst.EXCHANGE_ORDER,
                        MqConst.ROUTING_PROFITSHARING_SUCCESS,
                        paymentInfo.getOrderNo());

            } else if ("PROCESSING".equals(mockState)) {
                // 延迟重试逻辑
                rabbitService.sendDelayMessage(
                        MqConst.EXCHANGE_PROFITSHARING,
                        MqConst.ROUTING_PROFITSHARING,
                        JSON.toJSONString(profitsharingForm),
                        SystemConstant.PROFITSHARING_DELAY_TIME);

            } else {
                log.error("[模拟] 分账失败 - 订单号: {}", profitsharingForm.getOrderNo());
                throw new CustomException(ResultCodeEnum.PROFITSHARING_FAIL);
            }

        } catch (Exception e) {
            log.error("[模拟] 分账异常", e);
            throw new CustomException(ResultCodeEnum.PROFITSHARING_FAIL);
        }
    }
    private String determineMockState(String orderNo) {
//        // 示例1：通过订单号特征判断
//        if (orderNo.endsWith("_SUCCESS")) return "FINISHED";
//        if (orderNo.endsWith("_RETRY")) return "PROCESSING";
//        if (orderNo.endsWith("_FAIL")) return "FAILED";
//
//        // 示例2：随机成功率（50%成功，30%处理中，20%失败）
//        int random = new Random().nextInt(100);
//        if (random < 50) return "FINISHED";
//        if (random < 80) return "PROCESSING";
        return "FINISHED";
    }
}
