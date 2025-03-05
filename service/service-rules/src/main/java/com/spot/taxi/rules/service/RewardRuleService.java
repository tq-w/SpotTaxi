package com.spot.taxi.rules.service;

import com.spot.taxi.model.form.rules.RewardRuleRequestForm;
import com.spot.taxi.model.vo.rules.RewardRuleResponseVo;

public interface RewardRuleService {

    RewardRuleResponseVo calculateOrderRewardFee(RewardRuleRequestForm rewardRuleRequestForm);
}
