package com.spot.taxi.rules.service.impl;

import com.spot.taxi.model.form.rules.RewardRuleRequest;
import com.spot.taxi.model.form.rules.RewardRuleRequestForm;
import com.spot.taxi.model.vo.rules.RewardRuleResponse;
import com.spot.taxi.model.vo.rules.RewardRuleResponseVo;
import com.spot.taxi.rules.service.RewardRuleService;
import com.spot.taxi.rules.utils.DroolsHelper;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;


@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class RewardRuleServiceImpl implements RewardRuleService {

    public static final String RULES_REWARD_RULES_DRL = "rules/RewardRule.drl";

    @Override
    public RewardRuleResponseVo calculateOrderRewardFee(RewardRuleRequestForm rewardRuleRequestForm) {
        RewardRuleRequest rewardRuleRequest = new RewardRuleRequest();
        rewardRuleRequest.setOrderNum(rewardRuleRequestForm.getOrderNum());
        // 将Date转换为Instant
        Instant instant = rewardRuleRequestForm.getStartTime().toInstant();
        // 将Instant转换为LocalTime，使用系统默认时区
        LocalTime localTime = instant.atZone(ZoneId.systemDefault()).toLocalTime();
        // 创建一个指定格式的DateTimeFormatter
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        // 格式化LocalTime为字符串
        String formattedTime = localTime.format(formatter);
        rewardRuleRequest.setStartTime(formattedTime);

        KieSession kieSession = DroolsHelper.loadForRule(RULES_REWARD_RULES_DRL);

        RewardRuleResponse rewardRuleResponse = new RewardRuleResponse();
        kieSession.setGlobal("rewardRuleResponse", rewardRuleResponse);

        kieSession.insert(rewardRuleRequest);
        kieSession.fireAllRules();

        kieSession.dispose();

        RewardRuleResponseVo rewardRuleResponseVo = new RewardRuleResponseVo();
        rewardRuleResponseVo.setRewardRuleId(0L);  // todo这里本来是从一个数据库中获取的
        rewardRuleResponseVo.setRewardAmount(rewardRuleResponse.getRewardAmount());
        return rewardRuleResponseVo;
    }
}
