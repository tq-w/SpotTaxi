package com.spot.taxi.driver.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.spot.taxi.common.fallbackFactory.SentinelFallback;
import com.spot.taxi.common.result.Result;
import com.spot.taxi.driver.service.CosService;
import com.spot.taxi.driver.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "上传管理接口")
@RestController
@RequestMapping("file")
@SentinelResource(value = "FileController", blockHandlerClass = SentinelFallback.class, blockHandler = "defaultBlockHandler")
public class FileController {
    @Autowired
    private CosService cosService;
    @Autowired
    private FileService fileService;

//    //文件上传接口
//    @Operation(summary = "上传")
////    @CheckLoginStatus
//    @PostMapping("/upload")
//    public Result<String> upload(@RequestPart("file") MultipartFile file, @RequestParam(name = "path",defaultValue = "auth") String path) {
//        CosUploadVo cosUploadVo = cosService.uploadFile(file,path);
//        String showUrl = cosUploadVo.getShowUrl();
//        return Result.ok(showUrl);

    @Operation(summary = "上传")
    @PostMapping("/upload")
    public Result<String> upload(@RequestPart("file") MultipartFile file) {
        String url = fileService.upload(file);
        return Result.ok(url);
    }
}
