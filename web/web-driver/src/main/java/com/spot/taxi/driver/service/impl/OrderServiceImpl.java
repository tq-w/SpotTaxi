package com.spot.taxi.driver.service.impl;

import com.spot.taxi.common.execption.CustomException;
import com.spot.taxi.common.result.Result;
import com.spot.taxi.common.result.ResultCodeEnum;
import com.spot.taxi.dispatch.client.NewOrderFeignClient;
import com.spot.taxi.driver.service.OrderService;
import com.spot.taxi.map.client.LocationFeignClient;
import com.spot.taxi.map.client.MapFeignClient;
import com.spot.taxi.model.entity.order.OrderInfo;
import com.spot.taxi.model.enums.OrderStatus;
import com.spot.taxi.model.form.map.CalculateDrivingLineForm;
import com.spot.taxi.model.form.order.OrderFeeForm;
import com.spot.taxi.model.form.order.StartDriveForm;
import com.spot.taxi.model.form.order.UpdateOrderBillForm;
import com.spot.taxi.model.form.order.UpdateOrderCarForm;
import com.spot.taxi.model.form.rules.FeeRuleRequestForm;
import com.spot.taxi.model.form.rules.ProfitsharingRuleRequestForm;
import com.spot.taxi.model.form.rules.RewardRuleRequestForm;
import com.spot.taxi.model.vo.base.PageVo;
import com.spot.taxi.model.vo.map.DrivingLineVo;
import com.spot.taxi.model.vo.order.*;
import com.spot.taxi.model.vo.rules.FeeRuleResponseVo;
import com.spot.taxi.model.vo.rules.ProfitsharingRuleResponseVo;
import com.spot.taxi.model.vo.rules.RewardRuleResponseVo;
import com.spot.taxi.order.client.OrderInfoFeignClient;
import com.spot.taxi.rules.client.FeeRuleFeignClient;
import com.spot.taxi.rules.client.ProfitsharingRuleFeignClient;
import com.spot.taxi.rules.client.RewardRuleFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderServiceImpl implements OrderService {

    private final OrderInfoFeignClient orderInfoFeignClient;

    private final NewOrderFeignClient newOrderFeignClient;

    private final MapFeignClient mapFeignClient;

    private final LocationFeignClient locationFeignClient;

    private final FeeRuleFeignClient feeRuleFeignClient;

    private final RewardRuleFeignClient rewardRuleFeignClient;

    private final ProfitsharingRuleFeignClient profitsharingRuleFeignClient;

    @Qualifier("endOrderThreadPool")
    private ThreadPoolExecutor orderThreadPool;

    @Override
    public Integer getOrderStatus(Long orderId) {
        Result<Integer> orderStatusResult = orderInfoFeignClient.getOrderStatus(orderId);
        return orderStatusResult.getData();
    }

    @Override
    public List<NewOrderDataVo> findNewOrderQueueData(Long driverId) {
        Result<List<NewOrderDataVo>> newOrderQueueDataResult = newOrderFeignClient.findNewOrderQueueData(driverId);
        return newOrderQueueDataResult.getData();
    }

    @Override
    public Boolean takeNewOrder(Long driverId, Long orderId) {
        Result<Boolean> booleanResult = orderInfoFeignClient.takeNewOrder(driverId, orderId);
        return booleanResult.getData();
    }

    @Override
    public CurrentOrderInfoVo searchDriverCurrentOrder(Long driverId) {
        Result<CurrentOrderInfoVo> currentOrderInfoVoResult = orderInfoFeignClient.searchDriverCurrentOrder(driverId);
        return currentOrderInfoVoResult.getData();
    }

    @Override
    public OrderInfoVo getOrderInfo(Long orderId, Long driverId) {
        OrderInfo orderInfo = orderInfoFeignClient.getOrderInfo(orderId).getData();
        if (!driverId.equals(orderInfo.getDriverId())) {
            log.error("订单信息不匹配，driverId:{},orderId:{}", driverId, orderId);
            throw new CustomException(ResultCodeEnum.ILLEGAL_REQUEST);
        }
        OrderBillVo orderBillVo = null;
        OrderProfitsharingVo orderProfitsharingVo = null;
        //判断
        if(orderInfo.getStatus() >= OrderStatus.END_SERVICE.getStatus()) {
            //账单信息
            orderBillVo = orderInfoFeignClient.getOrderBillInfo(orderId).getData();
            //分账信息
            orderProfitsharingVo = orderInfoFeignClient.getOrderProfitsharing(orderId).getData();
        }
        OrderInfoVo orderInfoVo = new OrderInfoVo();
        orderInfoVo.setOrderId(orderId);
        BeanUtils.copyProperties(orderInfo, orderInfoVo);
        orderInfoVo.setOrderBillVo(orderBillVo);
        orderInfoVo.setOrderProfitsharingVo(orderProfitsharingVo);
        return orderInfoVo;
    }

    @Override
    public DrivingLineVo calculateDrivingLine(CalculateDrivingLineForm calculateDrivingLineForm) {
        Result<DrivingLineVo> drivingLineVoResult = mapFeignClient.calculateDrivingLine(calculateDrivingLineForm);
        return drivingLineVoResult.getData();
    }

    @Override
    public Boolean driverArriveStartLocation(Long orderId, Long driverId) {
        Result<Boolean> booleanResult = orderInfoFeignClient.driverArriveStartLocation(orderId, driverId);
        return booleanResult.getData();
    }

    @Override
    public Boolean updateOrdeCart(UpdateOrderCarForm updateOrderCartForm) {
        Result<Boolean> booleanResult = orderInfoFeignClient.updateOrderCar(updateOrderCartForm);
        return booleanResult.getData();
    }

    @Override
    public Boolean startDriving(StartDriveForm startDriveForm) {
        return orderInfoFeignClient.startDriving(startDriveForm).getData();
    }

    @Override
    @SneakyThrows
    public Boolean endDrive(OrderFeeForm orderFeeForm) {
        //1 根据orderId获取订单信息，判断当前订单是否司机接单
        CompletableFuture<OrderInfo> orderInfoCf = CompletableFuture.supplyAsync(() -> {
            OrderInfo orderInfo = orderInfoFeignClient.getOrderInfo(orderFeeForm.getOrderId()).getData();
            if (!Objects.equals(orderInfo.getDriverId(), orderFeeForm.getDriverId())) {
                throw new CustomException(ResultCodeEnum.ILLEGAL_REQUEST);
            }
            return orderInfo;
        }, orderThreadPool);

        //2 计算订单实际里程
        CompletableFuture<BigDecimal> realDistanceCf = CompletableFuture.supplyAsync(() ->
                locationFeignClient.calculateOrderRealDistance(orderFeeForm.getOrderId()).getData(), orderThreadPool);

        //3 计算代驾实际费用
        CompletableFuture<FeeRuleResponseVo> feeRuleResponseVoCf = orderInfoCf.thenCombine(realDistanceCf, (orderInfo, realDistance) -> {
            FeeRuleRequestForm feeRuleRequestForm = new FeeRuleRequestForm();
            feeRuleRequestForm.setDistance(realDistance);
            feeRuleRequestForm.setStartTime(orderInfo.getStartServiceTime());

            int waitMinute = Math.abs((int) ((orderInfo.getArriveTime().getTime() - orderInfo.getStartServiceTime().getTime()) / (1000 * 60)));

            feeRuleRequestForm.setWaitMinute(waitMinute);
            FeeRuleResponseVo feeRuleResponseVo = feeRuleFeignClient.calculateOrderFee(feeRuleRequestForm).getData();
            BigDecimal totalAmount = feeRuleResponseVo.getTotalAmount().add(orderFeeForm.getOtherFee()).add(orderFeeForm.getTollFee()
                    .add(orderFeeForm.getParkingFee()).add(orderInfo.getFavourFee()));
            feeRuleResponseVo.setTotalAmount(totalAmount);
            return feeRuleResponseVo;
        });

        //4 计算系统奖励
        CompletableFuture<Long> orderNumCf = orderInfoCf.thenApplyAsync((orderInfo) -> {
            // todo这不对呀，为啥要按照订单开始结束时间来找订单数
//        String startTime = new DateTime(orderInfo.getStartServiceTime()).toString("yyyy-MM-dd HH:mm:ss");
            String startTime = new DateTime().withTimeAtStartOfDay().toString("yyyy-MM-dd HH:mm:ss");
            String endTime = new DateTime(orderInfo.getEndServiceTime()).toString("yyyy-MM-dd HH:mm:ss");
            return orderInfoFeignClient.getOrderNumByTime(startTime, endTime).getData();
        }, orderThreadPool);

        CompletableFuture<RewardRuleResponseVo> rewardRuleResponseVoCf = orderNumCf.thenCombine(orderInfoCf, (orderNum, orderInfo) -> {
            RewardRuleRequestForm rewardRuleRequestForm = new RewardRuleRequestForm();
            rewardRuleRequestForm.setOrderNum(orderNum);
            rewardRuleRequestForm.setStartTime(orderInfo.getStartServiceTime());
            return rewardRuleFeignClient.calculateOrderRewardFee(rewardRuleRequestForm).getData();
        });


        //5 计算分账信息
        CompletableFuture<ProfitsharingRuleResponseVo> profitsharingRuleResponseVoCf = feeRuleResponseVoCf.thenCombine(orderNumCf, (feeRuleResponseVo, orderNum) -> {
            ProfitsharingRuleRequestForm profitsharingRuleRequestForm = new ProfitsharingRuleRequestForm();
            profitsharingRuleRequestForm.setOrderAmount(feeRuleResponseVo.getTotalAmount());
            profitsharingRuleRequestForm.setOrderNum(orderNum);
            return profitsharingRuleFeignClient.calculateOrderProfitsharingFee(profitsharingRuleRequestForm).getData();
        });

        CompletableFuture<Void> allOfCf = CompletableFuture.allOf(orderInfoCf, realDistanceCf, rewardRuleResponseVoCf, feeRuleResponseVoCf, profitsharingRuleResponseVoCf);

        allOfCf.thenRun(() -> {
            OrderInfo orderInfo = orderInfoCf.join();
            BigDecimal realDistance = realDistanceCf.join();
            FeeRuleResponseVo feeRuleResponseVo = feeRuleResponseVoCf.join();
            RewardRuleResponseVo rewardRuleResponseVo = rewardRuleResponseVoCf.join();
            ProfitsharingRuleResponseVo profitsharingRuleResponseVo = profitsharingRuleResponseVoCf.join();
            //6 封装实体类，结束代驾更新订单，添加账单和分账信息
            UpdateOrderBillForm updateOrderBillForm = new UpdateOrderBillForm();
            BeanUtils.copyProperties(orderFeeForm, updateOrderBillForm);
            updateOrderBillForm.setFavourFee(orderInfo.getFavourFee());
            updateOrderBillForm.setRealDistance(realDistance);
            BeanUtils.copyProperties(rewardRuleResponseVo, updateOrderBillForm);
            BeanUtils.copyProperties(feeRuleResponseVo, updateOrderBillForm);
            BeanUtils.copyProperties(profitsharingRuleResponseVo, updateOrderBillForm);
            orderInfoFeignClient.endDrive(updateOrderBillForm);
        }).join();
        return true;
    }

    @Override
    public PageVo findDriverOrderPage(Long driverId, Long page, Long limit) {
        return orderInfoFeignClient.findDriverOrderPage(driverId, page, limit).getData();
    }

    @Override
    public Boolean sendOrderBillInfo(Long orderId, Long driverId) {
        return orderInfoFeignClient.sendOrderBillInfo(orderId, driverId).getData();
    }
}
