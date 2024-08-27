package com.hao.springbootinit.controller;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@RestController
@RequestMapping("/queue")
@Slf4j
@Profile({"dev","local"})
public class QueueController {
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    //接收一个参数name，然后将任务添加到线程池中
    @GetMapping("/add")
    public void add(String name) {
        // 使用CompletableFuture运行一个异步任务
        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                log.info("任务执行中：" + name + ", 执行人：" + Thread.currentThread().getName());
                try {
                    Thread.sleep(600000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }, threadPoolExecutor); // 将任务交给线程池执行
    }
    // 改方法返回线程池状态信息
    @GetMapping("/get")
    public String get() {
        // 创建一个Map存储线程池状态信息
        Map<String, Object> map = new HashMap<>();
        // 获取线程池队列长度
        int size = threadPoolExecutor.getQueue().size();
        map.put("队列长度", size);
        // 获取线程池已接受任务总数
        long taskCount = threadPoolExecutor.getTaskCount();
        map.put("任务总数", taskCount);
        // 获取线程池已完成任务总数
        long completedTaskCount = threadPoolExecutor.getCompletedTaskCount();
        map.put("已完成的任务数", completedTaskCount);
        // 获取线程池正在工作的线程数
        int activeCount = threadPoolExecutor.getActiveCount();
        map.put("正在工作的线程数", activeCount);
        return JSONUtil.toJsonStr(map);
    }
}
