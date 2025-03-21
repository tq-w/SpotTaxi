package com.spot.taxi.order.mapper;

import com.spot.taxi.model.entity.order.OrderBill;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;

@Mapper
public interface OrderBillMapper extends BaseMapper<OrderBill> {

    int updateCouponAmount(Long orderId, BigDecimal couponAmount);
}
