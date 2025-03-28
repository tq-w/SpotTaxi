package com.spot.taxi.order.service.impl;

import com.spot.taxi.model.entity.order.OrderMonitor;
import com.spot.taxi.model.entity.order.OrderMonitorRecord;
import com.spot.taxi.order.mapper.OrderMonitorMapper;
import com.spot.taxi.order.repository.OrderMonitorRecordRepository;
import com.spot.taxi.order.service.OrderMonitorService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderMonitorServiceImpl extends ServiceImpl<OrderMonitorMapper, OrderMonitor> implements OrderMonitorService {
    
    private final OrderMonitorRecordRepository orderMonitorRecordRepository;

    @Override
    public Boolean saveOrderMonitorRecord(OrderMonitorRecord orderMonitorRecord) {
        orderMonitorRecordRepository.save(orderMonitorRecord);
        return true;
    }
}
