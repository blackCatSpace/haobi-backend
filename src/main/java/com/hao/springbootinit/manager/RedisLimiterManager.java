package com.hao.springbootinit.manager;

import com.hao.springbootinit.common.ErrorCode;
import com.hao.springbootinit.exception.BusinessException;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RedisLimiterManager {
    @Autowired
    private RedissonClient redissonClient;

    public void doRateLimit(String key) {
        // 创建限流器
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        // 限流速率，令牌桶算法
        rateLimiter.trySetRate(RateType.OVERALL, 2, 1, RateIntervalUnit.SECONDS);
        // 每一个操作进来获取一次令牌
        boolean canOp = rateLimiter.tryAcquire(1);
        // 如果不能拿到令牌，则抛出异常
        if (!canOp) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST);
        }
    }
}
