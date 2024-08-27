package com.hao.springbootinit.bizmq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class MyMessageProducer {
    @Resource
    private RabbitTemplate rabbitTemplate;
    void sendMessage(String exchange, String routingKey, String message) {
        // 使用rabbitTemplate的convertAndSend方法将消息发送到指定的交换机和路由键
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }
}
