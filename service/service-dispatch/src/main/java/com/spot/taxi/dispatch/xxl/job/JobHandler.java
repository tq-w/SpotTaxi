package com.spot.taxi.dispatch.xxl.job;

import com.spot.taxi.dispatch.mapper.XxlJobLogMapper;
import com.spot.taxi.dispatch.service.NewOrderService;
import com.spot.taxi.model.entity.dispatch.XxlJobLog;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JobHandler {
    
    private final NewOrderService newOrderService;

    
    private final XxlJobLogMapper xxlJobLogMapper;


    @XxlJob("newOrderTaskHandler")
    public void newOrderTaskHandler() {
        System.out.println("执行任务newOrderTaskHandler");
        XxlJobLog xxlJobLog = new XxlJobLog();
        xxlJobLog.setJobId(XxlJobHelper.getJobId());
        long startTime = System.currentTimeMillis();

        try {
            newOrderService.executeTask(XxlJobHelper.getJobId());
            xxlJobLog.setStatus(1);
        } catch (Exception e) {
            xxlJobLog.setStatus(0);
            xxlJobLog.setError(e.getMessage());
            e.printStackTrace();
        } finally {
            long endTime = System.currentTimeMillis();
            xxlJobLog.setTimes(endTime - startTime);
            XxlJobHelper.log("任务执行完成");
            xxlJobLogMapper.insert(xxlJobLog);
        }
    }
}
