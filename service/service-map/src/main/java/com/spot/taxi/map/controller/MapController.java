package com.spot.taxi.map.controller;

import com.spot.taxi.common.result.Result;
import com.spot.taxi.map.service.MapService;
import com.spot.taxi.model.form.map.CalculateDrivingLineForm;
import com.spot.taxi.model.vo.map.DrivingLineVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "地图API接口管理")
@RestController
@RequestMapping("/map")
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "rawtypes"})
public class MapController {

    private final MapService mapService;

    @Operation(summary = "计算驾驶线路")
    @PostMapping("/calculateDrivingLine")
    public Result<DrivingLineVo> calculateDrivingLine(@RequestBody CalculateDrivingLineForm calculateDrivingLineForm) {
        return Result.ok(mapService.calculateDrivingLine(calculateDrivingLineForm));
    }


}

