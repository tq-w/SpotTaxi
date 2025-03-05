package com.spot.taxi.customer.service;

import com.spot.taxi.model.entity.customer.CustomerInfo;
import com.spot.taxi.model.form.customer.UpdateWxPhoneForm;
import com.spot.taxi.model.vo.customer.CustomerLoginVo;
import com.baomidou.mybatisplus.extension.service.IService;

public interface CustomerInfoService extends IService<CustomerInfo> {
    Long login(String accessCode);
    CustomerLoginVo getCustomerInfo(Long customerId);
    Boolean updateWxPhoneNumber(UpdateWxPhoneForm updateWxPhoneForm);

    String getCustomerOpenId(Long customerId);
}
