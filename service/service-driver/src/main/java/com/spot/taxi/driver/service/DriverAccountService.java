package com.spot.taxi.driver.service;

import com.spot.taxi.model.entity.driver.DriverAccount;
import com.baomidou.mybatisplus.extension.service.IService;
import com.spot.taxi.model.form.driver.TransferForm;

public interface DriverAccountService extends IService<DriverAccount> {


    Boolean transfer(TransferForm transferForm);
}
