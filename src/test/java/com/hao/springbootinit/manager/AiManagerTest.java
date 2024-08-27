package com.hao.springbootinit.manager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AiManagerTest {
//    AiManager aiManager = new AiManager();
    @Autowired
    private AiManager aiManager;
    @Test
    void doChat() {
        System.out.println(aiManager.doChat(1810651263133483010L,"分析需求：\n" +
                "         分析网站用户的增长情况\n" +
                "         原始数据：\n" +
                "         日期,用户数\n" +
                "         1号,10\n" +
                "         2号,20\n" +
                "         3号,30"));
    }
}