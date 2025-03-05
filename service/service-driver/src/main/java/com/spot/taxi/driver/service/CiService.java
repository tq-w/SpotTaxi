package com.spot.taxi.driver.service;

import com.spot.taxi.model.vo.order.TextAuditingVo;

public interface CiService {
    public Boolean imageAuditing(String path);

    TextAuditingVo textAuditing(String content);
}
