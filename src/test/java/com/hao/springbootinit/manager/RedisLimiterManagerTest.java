package com.hao.springbootinit.manager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RedisLimiterManagerTest {
    @Autowired
    private RedisLimiterManager redisLimiterManager;
    @Test
    void doRateLimit() throws InterruptedException {
        String userId = "1";
        // 一秒内执行两次
        for (int i = 0; i < 2; i++) {
            redisLimiterManager.doRateLimit(userId);
            System.out.println("成功");
        }
        Thread.sleep(1000);
        for (int i = 0; i < 20; i++) {
            redisLimiterManager.doRateLimit(userId);
            System.out.println("成功");
        }
    }
}