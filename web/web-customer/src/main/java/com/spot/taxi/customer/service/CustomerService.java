package com.spot.taxi.customer.service;

import com.spot.taxi.model.form.customer.UpdateWxPhoneForm;
import com.spot.taxi.model.vo.customer.CustomerLoginVo;

public interface CustomerService {
    String wxLogin(String accessCode);
    CustomerLoginVo getCustomerLoginInfo(String token);
    CustomerLoginVo getCustomerInfo(Long customerId);
    Boolean updateWxPhoneNumber(UpdateWxPhoneForm updateWxPhoneForm);
}
