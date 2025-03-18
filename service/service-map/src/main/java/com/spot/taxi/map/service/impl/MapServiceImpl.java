package com.spot.taxi.map.service.impl;


import com.alibaba.fastjson2.JSONObject;
import com.spot.taxi.common.execption.CustomException;
import com.spot.taxi.common.result.ResultCodeEnum;
import com.spot.taxi.map.service.MapService;
import com.spot.taxi.model.form.map.CalculateDrivingLineForm;
import com.spot.taxi.model.vo.map.DrivingLineVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "rawtypes"})
public class MapServiceImpl implements MapService {

    private final RestTemplate restTemplate;

    @Value("${tencent.map.key}")
    private String key;

    @Override
    public DrivingLineVo calculateDrivingLine(CalculateDrivingLineForm calculateDrivingLineForm) {
        String url = "https://apis.map.qq.com/ws/direction/v1/driving/?from={from}&to={to}&output=json&key={key}";


        Map<String, String> map = new HashMap();
        map.put("from", calculateDrivingLineForm.getStartPointLatitude() + "," + calculateDrivingLineForm.getStartPointLongitude());
        map.put("to", calculateDrivingLineForm.getEndPointLatitude() + "," + calculateDrivingLineForm.getEndPointLongitude());
        map.put("key", key);

        JSONObject result = restTemplate.getForObject(url, JSONObject.class, map);

        if (result == null) {
            log.error("调用腾讯地图接口失败");
            throw new CustomException(ResultCodeEnum.MAP_FAIL);
        }

        int status = result.getIntValue("status");
        if (status != 0) {
            log.error("调用腾讯地图接口失败, status:{}", status);
            throw new CustomException(ResultCodeEnum.MAP_FAIL);
        }

        // todo多条路线如何实现？
        JSONObject route = result.getJSONObject("result").getJSONArray("routes").getJSONObject(0);
        DrivingLineVo drivingLineVo = new DrivingLineVo();
        drivingLineVo.setDistance(route.getBigDecimal("distance").divide(new BigDecimal(1000), 2, RoundingMode.HALF_UP));
        drivingLineVo.setDuration(route.getBigDecimal("duration"));
        drivingLineVo.setPolyline(route.getJSONArray("polyline"));
        return drivingLineVo;
    }
}
