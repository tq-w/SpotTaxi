package com.spot.taxi.common.service;


import com.alibaba.fastjson2.JSON;
import com.spot.taxi.common.entity.RebbitMqCorrelationData;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RabbitService {
    private final RabbitTemplate rabbitTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 发送消息
     *
     * @param exchange   交换机
     * @param routingKey 路由键
     * @param message    消息
     */
    public boolean sendMessage(String exchange, String routingKey, Object message) {
        //1.创建自定义相关消息对象-包含业务数据本身，交换器名称，路由键，队列类型，延迟时间,重试次数
        RebbitMqCorrelationData correlationData = new RebbitMqCorrelationData();
        String uuid = "mq:" + UUID.randomUUID().toString().replaceAll("-", "");
        correlationData.setId(uuid);
        correlationData.setMessage(message);
        correlationData.setExchange(exchange);
        correlationData.setRoutingKey(routingKey);
        //2.将相关消息封装到发送消息方法中
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
        //3.将相关消息存入Redis  Key：UUID  相关消息对象  10 分钟
        // todo看看这咋回事
        redisTemplate.opsForValue().set(uuid, JSON.toJSONString(correlationData), 10, TimeUnit.MINUTES);

        //log.info("生产者发送消息成功：{}，{}，{}", exchange, routingKey, message);
        return true;
    }

    public boolean sendDelayMessage(String exchange, String routingKey, Object message, int delayTime) {
        //1.创建自定义相关消息对象-包含业务数据本身，交换器名称，路由键，队列类型，延迟时间,重试次数
        RebbitMqCorrelationData correlationData = new RebbitMqCorrelationData();
        String uuid = "mq:" + UUID.randomUUID().toString().replaceAll("-", "");
        correlationData.setId(uuid);
        correlationData.setMessage(message);
        correlationData.setExchange(exchange);
        correlationData.setRoutingKey(routingKey);
        correlationData.setDelay(true);
        correlationData.setDelayTime(delayTime);

        //2.将相关消息封装到发送消息方法中
        rabbitTemplate.convertAndSend(
                exchange,
                routingKey,
                message,
                msg -> {
                    // 使用 x-delay 头设置延迟时间（单位：毫秒）
                    msg.getMessageProperties().setHeader("x-delay", delayTime * 1000);
                    return msg;
                },
                correlationData
        );

        //3.将相关消息存入Redis  Key：UUID  相关消息对象  10 分钟
        redisTemplate.opsForValue().set(uuid, JSON.toJSONString(correlationData), 10, TimeUnit.MINUTES);
        return true;
    }

//    public void sendDelayMessage(String exchangeProfitsharing, String routingProfitsharing, String jsonString, int profitsharingDelayTime) {
//    }
}
