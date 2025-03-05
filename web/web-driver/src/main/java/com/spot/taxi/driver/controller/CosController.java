package com.spot.taxi.driver.controller;

import com.spot.taxi.common.result.Result;
import com.spot.taxi.driver.service.CosService;
import com.spot.taxi.model.vo.driver.CosUploadVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Tag(name = "腾讯云cos上传接口管理")
@RestController
@RequestMapping(value = "/cos")
@SuppressWarnings({"unchecked", "rawtypes"})
public class CosController {
    @Autowired
    private CosService cosService;

    @Operation(summary = "上传")
//    @CheckLoginStatus todo暂时注释掉
    @PostMapping("/upload")
    public Result<CosUploadVo> upload(@RequestPart("file") MultipartFile file, @RequestParam(value = "path", defaultValue = "auth") String path) {
        CosUploadVo cosUploadVo = cosService.uploadFile(file, path);
        return Result.ok(cosUploadVo);
    }


}

