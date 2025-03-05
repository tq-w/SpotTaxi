package com.spot.taxi.dispatch.xxl.feign;


import com.alibaba.fastjson2.JSONObject;
import com.spot.taxi.model.entity.dispatch.XxlJobInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "xxlJobClient", url = "${xxl.job.admin.addresses}")
public interface XxlJobFeignClient {
    @PostMapping("/jobinfo/addJob")
    ResponseEntity<JSONObject> addJob(@RequestBody XxlJobInfo xxlJobInfo);

    @PostMapping("/jobinfo/startJob")
    ResponseEntity<JSONObject> startJob(@RequestBody XxlJobInfo xxlJobInfo);

    @PostMapping("/jobinfo/stopJob")
    ResponseEntity<JSONObject> stopJob(@RequestBody XxlJobInfo xxlJobInfo);

    @PostMapping("/jobinfo/removeJob")
    ResponseEntity<JSONObject> removeJob(XxlJobInfo xxlJobInfo);

    @PostMapping("/jobinfo/addAndStartJob")
    ResponseEntity<JSONObject> addAndStart(XxlJobInfo xxlJobInfo);
}
