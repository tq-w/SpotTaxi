package com.spot.taxi.common.fallbackFactory;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.spot.taxi.common.result.Result;


public class SentinelFallback {
    public static Result<String> defaultBlockHandler(BlockException ex) {
        return Result.fail("系统限流");
    }
}
