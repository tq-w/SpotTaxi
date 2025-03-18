package com.spot.taxi.driver.service.impl;

import com.spot.taxi.common.result.Result;
import com.spot.taxi.driver.client.OcrFeignClient;
import com.spot.taxi.driver.service.OcrService;
import com.spot.taxi.model.vo.driver.DriverLicenseOcrVo;
import com.spot.taxi.model.vo.driver.IdCardOcrVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "rawtypes"})
public class OcrServiceImpl implements OcrService {

    private final OcrFeignClient ocrFeignClient;

    @Override
    public IdCardOcrVo idCardOcr(MultipartFile file) {
        Result<IdCardOcrVo> idCardOcrVoResult = ocrFeignClient.idCardOcr(file);
        return idCardOcrVoResult.getData();
    }

    @Override
    public DriverLicenseOcrVo driverLicenseOcr(MultipartFile file) {
        Result<DriverLicenseOcrVo> driverLicenseOcrVoResult = ocrFeignClient.driverLicenseOcr(file);
        return driverLicenseOcrVoResult.getData();
    }
}
