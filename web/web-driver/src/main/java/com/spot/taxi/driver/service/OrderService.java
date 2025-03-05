package com.spot.taxi.driver.service;

import com.spot.taxi.model.form.map.CalculateDrivingLineForm;
import com.spot.taxi.model.form.order.OrderFeeForm;
import com.spot.taxi.model.form.order.StartDriveForm;
import com.spot.taxi.model.form.order.UpdateOrderCarForm;
import com.spot.taxi.model.vo.base.PageVo;
import com.spot.taxi.model.vo.map.DrivingLineVo;
import com.spot.taxi.model.vo.order.CurrentOrderInfoVo;
import com.spot.taxi.model.vo.order.NewOrderDataVo;
import com.spot.taxi.model.vo.order.OrderInfoVo;

import java.util.List;

public interface OrderService {


    Integer getOrderStatus(Long orderId);

    List<NewOrderDataVo> findNewOrderQueueData(Long driverId);

    Boolean takeNewOrder(Long driverId, Long orderId);

    CurrentOrderInfoVo searchDriverCurrentOrder(Long driverId);

    OrderInfoVo getOrderInfo(Long orderId, Long driverId);

    DrivingLineVo calculateDrivingLine(CalculateDrivingLineForm calculateDrivingLineForm);

    Boolean driverArriveStartLocation(Long orderId, Long driverId);

    Boolean updateOrdeCart(UpdateOrderCarForm updateOrderCartForm);

    Boolean startDriving(StartDriveForm startDriveForm);

    Boolean endDrive(OrderFeeForm orderFeeForm);

    PageVo findDriverOrderPage(Long driverId, Long page, Long limit);

    Boolean sendOrderBillInfo(Long orderId, Long driverId);
}
