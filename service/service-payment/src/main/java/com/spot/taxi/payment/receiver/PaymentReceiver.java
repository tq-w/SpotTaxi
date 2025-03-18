package com.spot.taxi.payment.receiver;

import com.alibaba.fastjson2.JSONObject;
import com.spot.taxi.model.form.payment.ProfitsharingForm;
import com.spot.taxi.payment.service.WxPayService;
import com.spot.taxi.payment.service.WxProfitsharingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import org.springframework.stereotype.Component;
import com.spot.taxi.common.constant.MqConst;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;

import java.io.IOException;


@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentReceiver {
    
    private final WxPayService wxPayService;

    private final WxProfitsharingService wxProfitsharingService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_PAY_SUCCESS,durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_ORDER),
            key = {MqConst.ROUTING_PAY_SUCCESS}
    ))
    public void paySuccess(String orderNo, Message message, Channel channel) {
        wxPayService.handleOrder(orderNo);
    }

    // 分账消息
    @RabbitListener(queues = MqConst.QUEUE_PROFITSHARING)
    public void profitsharingMessage(String param, Message message, Channel channel) throws IOException {
        try {
            ProfitsharingForm profitsharingForm = JSONObject.parseObject(param, ProfitsharingForm.class);
            log.info("分账：{}", param);
            wxProfitsharingService.profitsharing(profitsharingForm);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.info("分账调用失败：{}", e.getMessage());
            //任务执行失败，就退回队列继续执行，优化：设置退回次数，超过次数记录日志
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }
}
