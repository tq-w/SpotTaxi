package com.spot.taxi.rules.service;

import com.spot.taxi.model.form.rules.FeeRuleRequestForm;
import com.spot.taxi.model.vo.rules.FeeRuleResponseVo;

public interface FeeRuleService {

    FeeRuleResponseVo calculateOrderFee(FeeRuleRequestForm feeRuleRequestForm);
}
