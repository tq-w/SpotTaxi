package com.spot.taxi.driver.service.impl;

import com.spot.taxi.driver.client.CiFeignClient;
import com.spot.taxi.driver.service.FileService;
import com.spot.taxi.driver.service.MonitorService;
import com.spot.taxi.model.entity.order.OrderMonitorRecord;
import com.spot.taxi.model.form.order.OrderMonitorForm;
import com.spot.taxi.model.vo.order.TextAuditingVo;
import com.spot.taxi.order.client.OrderMonitorFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class MonitorServiceImpl implements MonitorService {
    @Autowired
    private FileService fileService;
    @Autowired
    private OrderMonitorFeignClient orderMonitorFeignClient;
    @Autowired
    private CiFeignClient ciFeignClient;

    @Override
    public Boolean upload(MultipartFile file, OrderMonitorForm orderMonitorForm) {
        String url = fileService.upload(file);
        // 审核
        TextAuditingVo textAuditingVo = ciFeignClient.textAuditing(orderMonitorForm.getContent()).getData();
        // 封装
        OrderMonitorRecord orderMonitorRecord = new OrderMonitorRecord();
        orderMonitorRecord.setOrderId(orderMonitorForm.getOrderId());
        orderMonitorRecord.setFileUrl(url);
        orderMonitorRecord.setContent(orderMonitorForm.getContent());
        orderMonitorRecord.setResult(textAuditingVo.getResult());
        orderMonitorRecord.setKeywords(textAuditingVo.getKeywords());
        // 存储
        orderMonitorFeignClient.saveMonitorRecord(orderMonitorRecord);
        return true;
    }
}
