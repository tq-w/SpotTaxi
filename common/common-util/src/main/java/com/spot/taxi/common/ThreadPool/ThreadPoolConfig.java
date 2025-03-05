package com.spot.taxi.common.ThreadPool;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;


@Configuration
public class ThreadPoolConfig {

    @Bean("endOrderThreadPool")
    public ThreadPoolExecutor orderThreadPool() {

        //动态获取服务器核数
        int processors = Runtime.getRuntime().availableProcessors();
        // 核心线程个数 io:2n ,cpu: n+1  n:内核数据
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                processors * 2,
                processors * 2 + 10,
                30, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100),
                new CustomThreadFactory("endOrder-pool-"),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        threadPoolExecutor.allowCoreThreadTimeOut(true);
        return threadPoolExecutor;
    }

    @Bean("logThreadPool")
    public ThreadPoolExecutor logThreadPool() {

        //动态获取服务器核数
        int processors = Runtime.getRuntime().availableProcessors();
        // 核心线程个数 io:2n ,cpu: n+1  n:内核数据
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                processors * 2,
                processors * 2 + 10,
                30, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100),
                new CustomThreadFactory("log-pool-"),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        threadPoolExecutor.allowCoreThreadTimeOut(true);
        return threadPoolExecutor;
    }
}