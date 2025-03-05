package com.spot.taxi.order.mapper;

import com.spot.taxi.model.entity.order.OrderInfo;
import com.spot.taxi.model.vo.order.OrderListVo;
import com.spot.taxi.model.vo.order.OrderPayVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {

    IPage<OrderListVo> selectCustomerOrderPage(Page<OrderInfo> pageParam, Long customerId);

    IPage<OrderListVo> selectDriverOrderPage(Page<OrderInfo> pageParam, Long driverId);

    OrderPayVo selectOrderPayVo(String orderNo, Long customerId);
}
