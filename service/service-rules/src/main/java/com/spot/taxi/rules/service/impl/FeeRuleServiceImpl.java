package com.spot.taxi.rules.service.impl;

import com.spot.taxi.model.form.rules.FeeRuleRequest;
import com.spot.taxi.model.form.rules.FeeRuleRequestForm;
import com.spot.taxi.model.vo.rules.FeeRuleResponse;
import com.spot.taxi.model.vo.rules.FeeRuleResponseVo;
import com.spot.taxi.rules.service.FeeRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.BeanUtils;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "rawtypes"})
public class FeeRuleServiceImpl implements FeeRuleService {

    private final KieContainer kieContainer;

    @Override
    public FeeRuleResponseVo calculateOrderFee(FeeRuleRequestForm feeRuleRequestForm) {
        FeeRuleRequest feeRuleRequest = new FeeRuleRequest();

//        feeRuleRequest.setDistance(feeRuleRequestForm.getDistance());
        // todo为了测试，因为没法抱着电脑跑一圈
        feeRuleRequest.setDistance(new BigDecimal(2));
        feeRuleRequest.setStartTime(new DateTime(feeRuleRequestForm.getStartTime()).toString("HH:mm:ss"));
        feeRuleRequest.setWaitMinute(feeRuleRequestForm.getWaitMinute());
        System.out.println(feeRuleRequest);

        //Drools使用
        KieSession kieSession = kieContainer.newKieSession();

        //封装返回对象
        FeeRuleResponse feeRuleResponse = new FeeRuleResponse();
        kieSession.setGlobal("feeRuleResponse",feeRuleResponse);

        kieSession.insert(feeRuleRequest);
        kieSession.fireAllRules();
        kieSession.dispose();
        System.out.println(feeRuleResponse);

        FeeRuleResponseVo feeRuleResponseVo = new FeeRuleResponseVo();
        feeRuleResponseVo.setFeeRuleId(0L); //todo这里本来应该是访问数据库查询
        BeanUtils.copyProperties(feeRuleResponse,feeRuleResponseVo);
        return feeRuleResponseVo;
    }
}
