package com.hao.springbootinit.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
@Configuration
public class ThreadPoolExecutorConfig {
    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {
        // 创建一个线程工厂
        ThreadFactory threadFactory = new ThreadFactory() {
            // 初始化线程数为1
            private int count = 1;
            @Override
            public Thread newThread(@NotNull Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("线程" + count);
                count++;
                return thread;
            }
        };
        // 创建线程池，线程池核心大小为2，最大线程大小为4，
        // 非核心线程空闲时间为100秒，任务队列为阻塞队列，长度为4，使用自定义线程工厂创建线程

        return new ThreadPoolExecutor(2, 4, 100, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(4), threadFactory);

    }
}
