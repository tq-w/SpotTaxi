package com.spot.taxi.map.service;

import com.spot.taxi.model.form.map.CalculateDrivingLineForm;
import com.spot.taxi.model.vo.map.DrivingLineVo;

public interface MapService {

    DrivingLineVo calculateDrivingLine(CalculateDrivingLineForm calculateDrivingLineForm);
}
