package com.spot.taxi.dispatch.service;

import com.spot.taxi.model.vo.dispatch.NewOrderTaskVo;
import com.spot.taxi.model.vo.order.NewOrderDataVo;

import java.util.List;

public interface NewOrderService {

    Long addAndStartTask(NewOrderTaskVo newOrderTaskVo);

    void executeTask(long jobId);

    List<NewOrderDataVo> findNewOrderQueueData(Long driverId);

    Boolean clearNewOrderQueueData(Long driverId);
}
