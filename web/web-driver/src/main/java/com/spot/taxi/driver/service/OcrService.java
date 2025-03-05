package com.spot.taxi.driver.service;

import com.spot.taxi.model.vo.driver.DriverLicenseOcrVo;
import com.spot.taxi.model.vo.driver.IdCardOcrVo;
import org.springframework.web.multipart.MultipartFile;

public interface OcrService {

    IdCardOcrVo idCardOcr(MultipartFile file);

    DriverLicenseOcrVo driverLicenseOcr(MultipartFile file);
}
