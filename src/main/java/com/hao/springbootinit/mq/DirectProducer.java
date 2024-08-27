package com.hao.springbootinit.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.util.Scanner;

public class DirectProducer {
  // 定义交换机名称
  private static final String EXCHANGE_NAME = "direct_exchange";

  public static void main(String[] argv) throws Exception {
    // 创建连接工厂
    ConnectionFactory factory = new ConnectionFactory();
    // 设置连接工厂的主机地址为本地主机
    factory.setHost("localhost");
    // 建立连接并创建通道
    try (Connection connection = factory.newConnection();
         Channel channel = connection.createChannel()) {
        // 使用通道声明交换机，类型为direct
        channel.exchangeDeclare(EXCHANGE_NAME, "direct");
    	// 获取严重程度（路由键）和消息内容
        Scanner sc = new Scanner(System.in);
        while (sc.hasNext()) {
            // 读取用户输入内容，以空格分隔
            String userInput = sc.nextLine();
            String[] strings = userInput.split(" ");
            if (strings.length < 1) {
                continue;
            }
            // 分别获取消息和路由键
            String message = strings[0];
            String routingKey = strings[1];
            // 发布消息到交换机
            channel.basicPublish(EXCHANGE_NAME, routingKey, null, message.getBytes("UTF-8"));
            System.out.println(" [x] Sent '" + routingKey + "':'" + message + "'");
        }

    }
  }
}