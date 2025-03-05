package com.spot.taxi.order.service;

import com.spot.taxi.model.entity.order.OrderMonitor;
import com.spot.taxi.model.entity.order.OrderMonitorRecord;
import com.baomidou.mybatisplus.extension.service.IService;

public interface OrderMonitorService extends IService<OrderMonitor> {

    Boolean saveOrderMonitorRecord(OrderMonitorRecord orderMonitorRecord);
}
