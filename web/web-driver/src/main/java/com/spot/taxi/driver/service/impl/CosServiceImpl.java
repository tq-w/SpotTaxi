package com.spot.taxi.driver.service.impl;

import com.spot.taxi.common.result.Result;
import com.spot.taxi.driver.client.CosFeignClient;
import com.spot.taxi.driver.service.CosService;
import com.spot.taxi.model.vo.driver.CosUploadVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "rawtypes"})
public class CosServiceImpl implements CosService {

    private final CosFeignClient cosFeignClient;

    @Override
    public CosUploadVo uploadFile(MultipartFile file, String path) {
        Result<CosUploadVo> cosUploadVoResult = cosFeignClient.upload(file, path);
        return cosUploadVoResult.getData();
    }
}
