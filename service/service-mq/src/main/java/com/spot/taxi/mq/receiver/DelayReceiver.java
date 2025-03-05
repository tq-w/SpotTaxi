package com.spot.taxi.mq.receiver;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.spot.taxi.mq.config.DelayedMqConfig;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class DelayReceiver {
    private final RedisTemplate<String, String> redisTemplate;
    /**
     * 监听到延迟消息
     *
     * @param msg
     * @param message
     * @param channel
     */
    @RabbitListener(queues = DelayedMqConfig.queue_delay_1)
    public void getDelayMsg(String msg, Message message, Channel channel) {
        System.out.println("msg = " + msg);
        String key = "mq:" + msg;
        try {
            //如果业务保证幂等性，基于redis setnx保证
            Boolean flag = redisTemplate.opsForValue().setIfAbsent(key, "", 10, TimeUnit.SECONDS);
            if (Boolean.FALSE.equals(flag)) {
                //说明该业务数据以及被执行
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }
            if (StringUtils.isNotBlank(msg)) {
                log.info("延迟插件监听消息：{}", msg);
            }
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            e.printStackTrace();
            log.error("异常：{}", e);
            redisTemplate.delete(key);
        }
    }
}
