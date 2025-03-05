package com.spot.taxi.driver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.spot.taxi.driver.mapper.DriverAccountDetailMapper;
import com.spot.taxi.driver.mapper.DriverAccountMapper;
import com.spot.taxi.driver.service.DriverAccountService;
import com.spot.taxi.model.entity.driver.DriverAccount;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spot.taxi.model.entity.driver.DriverAccountDetail;
import com.spot.taxi.model.form.driver.TransferForm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class DriverAccountServiceImpl extends ServiceImpl<DriverAccountMapper, DriverAccount> implements DriverAccountService {

    @Autowired
    private DriverAccountDetailMapper driverAccountDetailMapper;
    @Autowired
    private DriverAccountMapper driverAccountMapper;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean transfer(TransferForm transferForm) {
        LambdaQueryWrapper<DriverAccountDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DriverAccountDetail::getTradeNo, transferForm.getTradeNo());
        Long count = driverAccountDetailMapper.selectCount(queryWrapper);
        if (count > 0) {
            log.info("交易号已存在，tradeNo={}", transferForm.getTradeNo());
            return false;
        }
        driverAccountMapper.add(transferForm.getDriverId(), transferForm.getAmount());

        DriverAccountDetail driverAccountDetail = new DriverAccountDetail();
        BeanUtils.copyProperties(transferForm, driverAccountDetail);
        driverAccountDetailMapper.insert(driverAccountDetail);
        return true;
    }
}
