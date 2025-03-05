package com.spot.taxi.order.service.impl;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.spot.taxi.order.service.TestService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class TestServiceImpl implements TestService {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public void testLock() {
        RLock lock1 = redissonClient.getLock("lock1");
        //2 尝试获取锁
        //(1) 阻塞一直等待直到获取到，获取锁之后，默认过期时间30s
        lock1.lock();

        //(2) 获取到锁，锁过期时间10s
        // lock.lock(10,TimeUnit.SECONDS);

        //(3) 第一个参数获取锁等待时间
        //    第二个参数获取到锁，锁过期时间
        //        try {
        //            // true
        //            boolean tryLock = lock.tryLock(30, 10, TimeUnit.SECONDS);
        //        } catch (InterruptedException e) {
        //            throw new RuntimeException(e);
        //        }
        //3 编写业务代码
        //1.先从redis中通过key num获取值  key提前手动设置 num 初始值：0
        String value = redisTemplate.opsForValue().get("num");
        //2.如果值为空则非法直接返回即可
        if (StringUtils.isBlank(value)) {
            return;
        }
        //3.对num值进行自增加一
        int num = Integer.parseInt(value);
        redisTemplate.opsForValue().set("num", String.valueOf(++num));

        //4 释放锁
        lock1.unlock();
    }
//    @Override
//    public synchronized void testLock() {
//        //从redis里面获取数据
//        String value = redisTemplate.opsForValue().get("num");
//
//        if(StringUtils.isBlank(value)) {
//            return;
//        }
//        //把从redis获取数据+1
//        int num = Integer.parseInt(value);
//
//        //数据+1之后放回到redis里面
//        redisTemplate.opsForValue().set("num",String.valueOf(++num));
//    }

}
