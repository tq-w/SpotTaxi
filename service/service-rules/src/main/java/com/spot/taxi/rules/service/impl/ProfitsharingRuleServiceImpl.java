package com.spot.taxi.rules.service.impl;

import com.alibaba.fastjson2.JSON;
import com.spot.taxi.model.form.rules.ProfitsharingRuleRequest;
import com.spot.taxi.model.form.rules.ProfitsharingRuleRequestForm;
import com.spot.taxi.model.vo.rules.ProfitsharingRuleResponse;
import com.spot.taxi.model.vo.rules.ProfitsharingRuleResponseVo;
import com.spot.taxi.rules.mapper.ProfitsharingRuleMapper;
import com.spot.taxi.rules.service.ProfitsharingRuleService;
import com.spot.taxi.rules.utils.DroolsHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.BeanUtils;

import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "rawtypes"})
public class ProfitsharingRuleServiceImpl implements ProfitsharingRuleService {
    private static final String RULES_PROFITSHARING_RULES_DRL = "rules/ProfitsharingRule.drl";

    private final ProfitsharingRuleMapper rewardRuleMapper;

    @Override
    public ProfitsharingRuleResponseVo calculateOrderProfitsharingFee(ProfitsharingRuleRequestForm profitsharingRuleRequestForm) {
        ProfitsharingRuleRequest profitsharingRuleRequest = new ProfitsharingRuleRequest();
        BeanUtils.copyProperties(profitsharingRuleRequestForm, profitsharingRuleRequest);
        //创建kieSession
        KieSession kieSession = DroolsHelper.loadForRule(RULES_PROFITSHARING_RULES_DRL);
        //封装返回对象
        ProfitsharingRuleResponse profitsharingRuleResponse = new ProfitsharingRuleResponse();
        kieSession.setGlobal("profitsharingRuleResponse", profitsharingRuleResponse);
        //触发规则，返回vo对象
        kieSession.insert(profitsharingRuleRequest);
        kieSession.fireAllRules();
        kieSession.dispose();
        log.info("计算结果：{}", JSON.toJSONString(profitsharingRuleResponse));

        ProfitsharingRuleResponseVo profitsharingRuleResponseVo = new ProfitsharingRuleResponseVo();
        profitsharingRuleResponseVo.setProfitsharingRuleId(0L); // todo这里本来是从一个数据库中获取的
        BeanUtils.copyProperties(profitsharingRuleResponse, profitsharingRuleResponseVo);
        return profitsharingRuleResponseVo;
    }
}
