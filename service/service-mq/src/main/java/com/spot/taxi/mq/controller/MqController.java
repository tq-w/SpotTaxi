package com.spot.taxi.mq.controller;

import com.spot.taxi.common.result.Result;
import com.spot.taxi.common.service.RabbitService;
import com.spot.taxi.mq.config.DeadLetterMqConfig;
import com.spot.taxi.mq.config.DelayedMqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/mq")
@RequiredArgsConstructor
public class MqController {
    private final RabbitService rabbitService;

    @GetMapping("sendConfirm")
    public Result sendConfirm() {
        rabbitService.sendMessage("exchange.confirm", "routing.confirm", "来人了，开始接客吧！");
        return Result.ok();
    }

    @GetMapping("/sendDeadLetterMsg")
    public Result sendDeadLetterMsg() {
        rabbitService.sendMessage(DeadLetterMqConfig.exchange_dead, DeadLetterMqConfig.routing_dead_1, "我是延迟消息");
        log.info("基于死信发送延迟消息成功");
        return Result.ok();
    }

    /**
     * 消息发送延迟消息：基于延迟插件使用
     */
    @GetMapping("/sendDelayMsg")
    public Result sendDelayMsg() {
        //调用工具方法发送延迟消息
        int delayTime = 10;
        rabbitService.sendDelayMessage(DelayedMqConfig.exchange_delay, DelayedMqConfig.routing_delay, "我是延迟消息", delayTime);
        log.info("基于延迟插件-发送延迟消息成功");
        return Result.ok();
    }

}