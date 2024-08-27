package com.hao.springbootinit.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class MultiConsumer {

    private static final String TASK_QUEUE_NAME = "multi_queue";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        final Connection connection = factory.newConnection();
        for (int i = 0; i < 2; i++) {
            final Channel channel = connection.createChannel();

            channel.queueDeclare(TASK_QUEUE_NAME, true, false, false, null);
            System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

            // 每个消费者最多同时处理 1 个任务
            // 设置预取计数为 1，这样RabbitMQ就会在给消费者新消息之前等待先前的消息被确认
            channel.basicQos(1);

            int finalI = i;
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");

                try {
                    System.out.println(" [x] Received '" + "编号:" + finalI + ":" + message + "'");
                    // 发送消息确认
                    channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
                    // 处理工作,模拟处理消息所花费的时间,机器处理能力有限(接收一条消息,20秒后再接收下一条消息)
                    Thread.sleep(20000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    System.out.println(" [x] Done");
                    // 手动发送应答,告诉RabbitMQ消息已经被处理
//                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                }
            };
            // 开始消费消息,传入队列名称,是否自动确认,投递回调和消费者取消回调
            channel.basicConsume(TASK_QUEUE_NAME, false, deliverCallback, consumerTag -> {
            });
        }
    }

}