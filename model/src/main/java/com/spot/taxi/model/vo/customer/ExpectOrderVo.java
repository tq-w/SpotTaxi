package com.spot.taxi.model.vo.customer;

import com.spot.taxi.model.vo.map.DrivingLineVo;
import com.spot.taxi.model.vo.rules.FeeRuleResponseVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "预估订单实体")
@Data
public class ExpectOrderVo {

	@Schema(description = "驾车路线")
	private DrivingLineVo drivingLineVo;

	@Schema(description = "订单费用")
	private FeeRuleResponseVo feeRuleResponseVo;

}