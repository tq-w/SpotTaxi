package com.spot.taxi.payment.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.spot.taxi.common.constant.MqConst;
import com.spot.taxi.common.constant.RedisConstant;
import com.spot.taxi.common.constant.SystemConstant;
import com.spot.taxi.common.service.RabbitService;
import com.spot.taxi.driver.client.DriverAccountFeignClient;
import com.spot.taxi.model.entity.payment.PaymentInfo;
import com.spot.taxi.model.enums.TradeType;
import com.spot.taxi.model.form.driver.TransferForm;
import com.spot.taxi.model.form.payment.PaymentInfoForm;
import com.spot.taxi.model.form.payment.ProfitsharingForm;
import com.spot.taxi.model.vo.order.OrderProfitsharingVo;
import com.spot.taxi.model.vo.order.OrderRewardVo;
import com.spot.taxi.model.vo.payment.WxPrepayVo;
import com.spot.taxi.order.client.OrderInfoFeignClient;
import com.spot.taxi.payment.config.WxPayV3Properties;
import com.spot.taxi.payment.mapper.PaymentInfoMapper;
import com.spot.taxi.payment.service.WxPayService;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.beans.BeanUtils;

import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Primary
@Slf4j
@Service
@RequiredArgsConstructor
public class WxPayServiceImplMock implements WxPayService {
    
    private final PaymentInfoMapper paymentInfoMapper;
    
    private final RSAAutoCertificateConfig rsaAutoCertificateConfig;
    
    private final WxPayV3Properties wxPayV3Properties;
    
    private final RabbitService rabbitService;
    
    private final OrderInfoFeignClient orderInfoFeignClient;
    
    private final DriverAccountFeignClient driverAccountFeignClient;
    
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public WxPrepayVo createWxPayment(PaymentInfoForm paymentInfoForm) {
        // 1. 保持原有的支付记录创建逻辑
        LambdaQueryWrapper<PaymentInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentInfo::getOrderNo, paymentInfoForm.getOrderNo());
        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(wrapper);
        if (paymentInfo == null) {
            paymentInfo = new PaymentInfo();
            BeanUtils.copyProperties(paymentInfoForm, paymentInfo);
            paymentInfo.setPaymentStatus(0); // 0表示未支付状态
            paymentInfoMapper.insert(paymentInfo);
        }

        // 2. 生成模拟的微信预支付响应
        WxPrepayVo wxPrepayVo = new WxPrepayVo();
        // 从配置中获取APPID（保持与原有逻辑一致）
        wxPrepayVo.setAppId(wxPayV3Properties.getAppid());
        // 生成时间戳（秒级）
        wxPrepayVo.setTimeStamp(String.valueOf(System.currentTimeMillis() / 1000));
        // 生成随机字符串（模拟微信格式）
        wxPrepayVo.setNonceStr(UUID.randomUUID().toString().replace("-", ""));
        // 构建模拟的预支付交易标识（格式保持与微信一致）
        wxPrepayVo.setPackageVal("prepay_id=MOCK_" + paymentInfoForm.getOrderNo());
        // 签名类型（根据项目实际情况设置）
        wxPrepayVo.setSignType("RSA");
        // 生成模拟签名（实际项目可以用固定测试值）
        wxPrepayVo.setPaySign("MOCK_SIGN_" + new Random().nextInt(10000));
        redisTemplate.opsForValue().set(RedisConstant.PAYMENT_FAKE_MARK + paymentInfoForm.getOrderNo(), "1");
        redisTemplate.expire(RedisConstant.PAYMENT_FAKE_MARK + paymentInfoForm.getOrderNo(), 60, TimeUnit.SECONDS);
        log.info("假装支付成功");
        return wxPrepayVo;
    }

    @Override
    public Object queryPayStatus(String orderNo) {
        String isSuccess = redisTemplate.opsForValue().get(RedisConstant.PAYMENT_FAKE_MARK + orderNo);
        if (Objects.equals(isSuccess, "1")) {
            this.handlePayment(orderNo);
            return true;
        }
        return false;
    }

    @Override
    public void wxnotify(HttpServletRequest request) {
        log.info("模拟微信支付回调wxnotify");
    }

    //支付成功后续处理
    @GlobalTransactional
    @Override
    public void handleOrder(String orderNo) {
        //1 远程调用：更新订单状态：已经支付
        orderInfoFeignClient.updateOrderPayStatus(orderNo);

        //2 远程调用：获取系统奖励，打入到司机账户
        OrderRewardVo orderRewardVo = orderInfoFeignClient.getOrderRewardFee(orderNo).getData();
        if (orderRewardVo != null && orderRewardVo.getRewardFee().doubleValue() > 0) {
            TransferForm transferForm = new TransferForm();
            transferForm.setTradeNo(orderNo);
            transferForm.setTradeType(TradeType.REWARD.getType());
            transferForm.setContent(TradeType.REWARD.getContent());
            transferForm.setAmount(orderRewardVo.getRewardFee());
            transferForm.setDriverId(orderRewardVo.getDriverId());
            driverAccountFeignClient.transfer(transferForm);
        }

        //3 TODO 微信支付应该无法调用，改成本地操作
        //分账处理
        assert orderRewardVo != null;
        OrderProfitsharingVo orderProfitsharingVo = orderInfoFeignClient.getOrderProfitsharing(orderRewardVo.getOrderId()).getData();
        //封装分账参数对象
        ProfitsharingForm profitsharingForm = new ProfitsharingForm();
        profitsharingForm.setOrderNo(orderNo);
        profitsharingForm.setAmount(orderProfitsharingVo.getDriverIncome());
        profitsharingForm.setDriverId(orderRewardVo.getDriverId());
        //分账有延迟，支付成功后最少2分钟执行分账申请
        rabbitService.sendDelayMessage(MqConst.EXCHANGE_PROFITSHARING, MqConst.ROUTING_PROFITSHARING, JSON.toJSONString(profitsharingForm), SystemConstant.PROFITSHARING_DELAY_TIME);
    }

    private void handlePayment(String orderNo) {
        LambdaQueryWrapper<PaymentInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentInfo::getOrderNo, orderNo);
        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(wrapper);
        if (paymentInfo.getPaymentStatus() == 1) {
            return;
        }
        paymentInfo.setPaymentStatus(1);
        paymentInfo.setOrderNo(orderNo);
        paymentInfo.setTransactionId("MOCK_" + orderNo);
        paymentInfo.setCallbackTime(new Date());
        paymentInfo.setCallbackContent(JSON.toJSONString("MOCK_DATA"));
        paymentInfoMapper.updateById(paymentInfo);

        //2 发送端：发送mq消息，传递 订单编号
        //  接收端：获取订单编号，完成后续处理
        rabbitService.sendMessage(MqConst.EXCHANGE_ORDER, MqConst.ROUTING_PAY_SUCCESS, orderNo);
    }
}
