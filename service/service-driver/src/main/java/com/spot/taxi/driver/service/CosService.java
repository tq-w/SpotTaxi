package com.spot.taxi.driver.service;

import com.spot.taxi.model.vo.driver.CosUploadVo;
import org.springframework.web.multipart.MultipartFile;

public interface CosService {
    CosUploadVo uploadFile(MultipartFile file, String path);
    String getImageUrl(String path);
}
