package com.spot.taxi.driver.controller;

import com.spot.taxi.common.result.Result;
import com.spot.taxi.driver.service.OcrService;
import com.spot.taxi.model.vo.driver.DriverLicenseOcrVo;
import com.spot.taxi.model.vo.driver.IdCardOcrVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Tag(name = "腾讯云识别接口管理")
@RestController
@RequestMapping(value="/ocr")
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "rawtypes"})
public class OcrController {
    
    private final OcrService ocrService;

    @Operation(summary = "身份证识别")
    @PostMapping("/idCardOcr")
    public Result<IdCardOcrVo> idCardOcr(@RequestPart("file") MultipartFile file) {
        return Result.ok(ocrService.idCardOcr(file));
    }

    @Operation(summary = "驾驶证识别")
    @PostMapping("/driverLicenseOcr")
    public Result<DriverLicenseOcrVo> driverLicenseOcr(@RequestPart("file") MultipartFile file) {
        return Result.ok(ocrService.driverLicenseOcr(file));
    }

}

