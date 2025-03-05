package com.spot.taxi.common.constant;

public class MqConst {


    public static final String EXCHANGE_ORDER = "taxi.order";
    public static final String ROUTING_PAY_SUCCESS = "taxi.pay.success";
    public static final String ROUTING_PROFITSHARING_SUCCESS = "taxi.profitsharing.success";
    public static final String QUEUE_PAY_SUCCESS = "taxi.pay.success";
    public static final String QUEUE_PROFITSHARING_SUCCESS = "taxi.profitsharing.success";


    //取消订单延迟消息
    public static final String EXCHANGE_CANCEL_ORDER = "taxi.cancel.order";
    public static final String ROUTING_CANCEL_ORDER = "taxi.cancel.order";
    public static final String QUEUE_CANCEL_ORDER = "taxi.cancel.order";

    //分账延迟消息
    public static final String EXCHANGE_PROFITSHARING = "taxi.profitsharing";
    public static final String ROUTING_PROFITSHARING = "taxi.profitsharing";
    public static final String QUEUE_PROFITSHARING  = "taxi.profitsharing";

}
