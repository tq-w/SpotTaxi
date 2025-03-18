package com.spot.taxi.driver.service.impl;

import com.spot.taxi.common.execption.CustomException;
import com.spot.taxi.common.result.ResultCodeEnum;
import com.spot.taxi.driver.config.TencentCloudProperties;
import com.spot.taxi.driver.service.CiService;
import com.spot.taxi.driver.service.CosService;
import com.spot.taxi.model.vo.driver.CosUploadVo;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.*;
import com.qcloud.cos.region.Region;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "rawtypes"})
public class CosServiceImpl implements CosService {

    private final TencentCloudProperties tencentCloudProperties;
    
    private final CiService ciService;

    private COSClient getCosClient() {
        // 1 初始化用户身份信息（secretId, secretKey）。
        String secretId = tencentCloudProperties.getSecretId();
        String secretKey = tencentCloudProperties.getSecretKey();
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        // 2 设置 bucket 的地域, COS 地域
        Region region = new Region(tencentCloudProperties.getRegion());
        ClientConfig clientConfig = new ClientConfig(region);
        // 这里建议设置使用 https 协议
        clientConfig.setHttpProtocol(HttpProtocol.https);
        // 3 生成 cos 客户端。
        return new COSClient(cred, clientConfig);
    }

    @Override
    public CosUploadVo uploadFile(MultipartFile file, String path) {
        COSClient cosClient = getCosClient();

        //文件上传
        //元数据信息
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentLength(file.getSize());
        meta.setContentEncoding("UTF-8");
        meta.setContentType(file.getContentType());

        //向存储桶中保存文件
        String fileType = Objects.requireNonNull(file.getOriginalFilename()).substring(file.getOriginalFilename().lastIndexOf(".")); //文件后缀名
        String uploadPath = "/driver/" + path + "/" + UUID.randomUUID().toString().replaceAll("-", "") + fileType;
        // 01.jpg
        // /driver/auth/0o98754.jpg
        PutObjectRequest putObjectRequest = null;
        try {
            //1 bucket名称
            //2
            putObjectRequest = new PutObjectRequest(tencentCloudProperties.getBucketPrivate(),
                    uploadPath,
                    file.getInputStream(),
                    meta);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        putObjectRequest.setStorageClass(StorageClass.Standard);
        PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest); //上传文件
        cosClient.shutdown();

        //审核图片
        Boolean isAuditing = ciService.imageAuditing(uploadPath);
        if(!isAuditing) {
            //删除违规图片
            cosClient.deleteObject(tencentCloudProperties.getBucketPrivate(), uploadPath);
            // todo这里是应该抛出异常还是返回错误信息呢
            throw new CustomException(ResultCodeEnum.IMAGE_AUDITION_FAIL);
        }

        //返回vo对象
        CosUploadVo cosUploadVo = new CosUploadVo();
        cosUploadVo.setUrl(uploadPath);
        // todo这里用不用检查一下是否成功呢
        cosUploadVo.setShowUrl(this.getImageUrl(uploadPath));
        return cosUploadVo;
    }

    @Override
    public String getImageUrl(String path) {
        if (!StringUtils.hasText(path)) return "";
        COSClient cosClient = this.getCosClient();
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(tencentCloudProperties.getBucketPrivate(), path, HttpMethodName.GET);

        request.setExpiration(new DateTime().plusMinutes(15).toDate());

        URL url = cosClient.generatePresignedUrl(request);
        cosClient.shutdown();
        return url.toString();
    }
}
