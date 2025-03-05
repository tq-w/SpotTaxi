package com.spot.taxi.common.config;

import com.alibaba.fastjson2.JSON;
import com.spot.taxi.common.entity.RebbitMqCorrelationData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitInitConfigApplicationListener implements ApplicationListener<ApplicationReadyEvent> {
    private final RabbitTemplate rabbitTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        this.setupCallbacks();
    }

    private void setupCallbacks() {
        /**
         * 只确认消息是否正确到达 Exchange 中,成功与否都会回调
         *
         * @param correlation 相关数据  非消息本身业务数据
         * @param ack             应答结果
         * @param reason           如果发送消息到交换器失败，错误原因
         */
        this.rabbitTemplate.setConfirmCallback((correlation, ack, reason) -> {
            if (ack) {
                log.info("消息发送到Exchange成功：{}", correlation);
            } else {
                log.error("消息发送到Exchange失败：{}", reason);
            }
        });


        //消息没有正确到达队列时触发回调，如果正确到达队列不执行
        this.rabbitTemplate.setReturnsCallback((returned) -> {
            log.error("Returned: " + returned.getMessage() + "\nreplyCode: " + returned.getReplyCode()
                    + "\nreplyText: " + returned.getReplyText() + "\nexchange/rk: "
                    + returned.getExchange() + "/" + returned.getRoutingKey());
            //当路由队列失败 也需要重发
            //1.构建相关数据对象
            String redisKey = returned.getMessage().getMessageProperties().getHeader("spring_returned_message_correlation");
            String correlationDataStr = redisTemplate.opsForValue().get(redisKey);
            RebbitMqCorrelationData rebbitMqCorrelationData = JSON.parseObject(correlationDataStr, RebbitMqCorrelationData.class);
            //方式一:如果不考虑延迟消息重发 直接返回
            assert rebbitMqCorrelationData != null;
            if (rebbitMqCorrelationData.isDelay()) {
                // 避免立即重发，延迟消息的重发逻辑委托给 retrySendMsg 方法，该方法能够正确处理延迟消息的延迟属性
                return;
            }
            //2.调用消息重发方法
            this.retrySendMsg(rebbitMqCorrelationData);
        });
    }

    /**
     * 消息重新发送
     *
     * @param correlationData
     */
    private void retrySendMsg(CorrelationData correlationData) {
        //获取相关数据
        RebbitMqCorrelationData rebbitMqCorrelationData = (RebbitMqCorrelationData) correlationData;

        //获取redis中存放重试次数
        //先重发，在写会到redis中次数
        int retryCount = rebbitMqCorrelationData.getRetryCount();
        if (retryCount >= 3) {
            //超过最大重试次数
            log.error("生产者超过最大重试次数，将失败的消息存入数据库用人工处理；给管理员发送邮件；给管理员发送短信；");
            return;
        }
        //重发次数+1
        retryCount += 1;
        rebbitMqCorrelationData.setRetryCount(retryCount);
        redisTemplate.opsForValue().set(rebbitMqCorrelationData.getId(), JSON.toJSONString(rebbitMqCorrelationData), 10, TimeUnit.MINUTES);
        log.info("进行消息重发！");
        //重发消息
        //方式二：如果是延迟消息，依然需要设置消息延迟时间
        if (rebbitMqCorrelationData.isDelay()) {
            //延迟消息
            rabbitTemplate.convertAndSend(rebbitMqCorrelationData.getExchange(), rebbitMqCorrelationData.getRoutingKey(), rebbitMqCorrelationData.getMessage(), message -> {
                message.getMessageProperties().setHeader("x-delay", rebbitMqCorrelationData.getDelayTime() * 1000);
                return message;
            }, rebbitMqCorrelationData);
        } else {
            //普通消息
            rabbitTemplate.convertAndSend(rebbitMqCorrelationData.getExchange(), rebbitMqCorrelationData.getRoutingKey(), rebbitMqCorrelationData.getMessage(), rebbitMqCorrelationData);
        }
    }

}
