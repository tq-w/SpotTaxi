package com.spot.taxi.rules.service;

import com.spot.taxi.model.form.rules.ProfitsharingRuleRequestForm;
import com.spot.taxi.model.vo.rules.ProfitsharingRuleResponseVo;

public interface ProfitsharingRuleService {

    ProfitsharingRuleResponseVo calculateOrderProfitsharingFee(ProfitsharingRuleRequestForm profitsharingRuleRequestForm);
}
