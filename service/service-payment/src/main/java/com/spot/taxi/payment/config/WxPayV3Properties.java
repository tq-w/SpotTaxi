package com.spot.taxi.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.Data;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;

@Configuration
@ConfigurationProperties(prefix="wx.v3pay") //读取节点
@Data
public class WxPayV3Properties {

    private String appid;
    /** 商户号 */
    public String merchantId;
    /** 商户API私钥路径 */
    public String privateKeyPath;
    /** 商户证书序列号 */
    public String merchantSerialNumber;
    /** 商户APIV3密钥 */
    public String apiV3key;
    /** 回调地址 */
    private String notifyUrl;

    @Bean
    public RSAAutoCertificateConfig getConfig(){
        return new RSAAutoCertificateConfig.Builder()
                .merchantId(this.getMerchantId())
                .privateKeyFromPath(this.getPrivateKeyPath())
                .merchantSerialNumber(this.getMerchantSerialNumber())
                .apiV3Key(this.getApiV3key())
                .build();

    }
}