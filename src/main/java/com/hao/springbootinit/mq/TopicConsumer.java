package com.hao.springbootinit.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class TopicConsumer {

    private static final String EXCHANGE_NAME = "topic_exchange";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        // 创建前端队列
        channel.exchangeDeclare(EXCHANGE_NAME, "topic");
        String queueName = "frontend_queue";
        channel.queueDeclare(queueName, true, false, false, null);
        channel.queueBind(queueName, EXCHANGE_NAME, "#.前端.#");

        // 创建后端队列
        String queueName2 = "backend_queue";
        channel.queueDeclare(queueName2, true, false, false, null);
        channel.queueBind(queueName2, EXCHANGE_NAME, "#.后端.#");

        // 创建产品队列
        String queueName3 = "product_queue";
        channel.queueDeclare(queueName3, true, false, false, null);
        channel.queueBind(queueName3, EXCHANGE_NAME, "#.产品.#");

        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        // 创建消息处理的回调函数（消费者：员工小a接收）
        DeliverCallback xiaoaDeliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [员工小a] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };

        // 创建消息处理的回调函数（消费者：员工小b接收）
        DeliverCallback xiaobDeliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [员工小b] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };

        // 创建消息处理的回调函数（消费者：员工小c接收）
        DeliverCallback xiaocDeliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [员工小c] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };
        // 启动消费者并绑定消息处理的回调函数到各个队列上
        // 员工小a处理前端队列接收到的消息
        channel.basicConsume(queueName, true, xiaoaDeliverCallback, consumerTag -> {
        });

        // 员工小b处理后端队列接收到的消息
        channel.basicConsume(queueName2, true, xiaobDeliverCallback, consumerTag -> {
        });

        // 员工小b处理后端队列接收到的消息
        channel.basicConsume(queueName3, true, xiaocDeliverCallback, consumerTag -> {
        });
    }
}