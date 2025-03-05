package com.spot.taxi.map.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class MapConfig {
    // todo这个是有必要的吗
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
