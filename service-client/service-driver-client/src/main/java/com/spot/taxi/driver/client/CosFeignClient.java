package com.spot.taxi.driver.client;

import com.spot.taxi.common.result.Result;
import com.spot.taxi.model.vo.driver.CosUploadVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(value = "service-driver")
public interface CosFeignClient {
    @PostMapping(path = "/driver/cos/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<CosUploadVo> upload(@RequestPart("file") MultipartFile file, @RequestParam("path") String path);
}