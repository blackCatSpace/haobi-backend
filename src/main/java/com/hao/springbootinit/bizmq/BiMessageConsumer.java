package com.hao.springbootinit.bizmq;

import com.hao.springbootinit.common.ErrorCode;
import com.hao.springbootinit.manager.AiManager;
import com.hao.springbootinit.model.entity.Chart;
import com.hao.springbootinit.service.ChartService;
import com.rabbitmq.client.Channel;
import com.hao.springbootinit.constant.CommonConstant;
import com.hao.springbootinit.exception.BusinessException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
public class BiMessageConsumer {
    @Resource
    private ChartService chartService;
    @Resource
    private AiManager aiManager;
    @SneakyThrows
    @RabbitListener(queues = {BiMqConstant.BI_QUEUE_NAME}, ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        // 使用日志记录器打印接收到的消息内容
        log.info("receiveMessage message = {}", message);
        // 消息传递的是数据库chart的id
        long chartId = Long.parseLong(message);
        Chart chart = chartService.getById(chartId);

        // 校验数据库中是成功取到数据
        if (chart == null) {
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图表为空");
        }

        String goal = chart.getGoal();
        String chartType = chart.getChartType();

        // 用户输入从数据里取
        StringBuilder userInput = new StringBuilder();
        // 如果传入图表类型，则分析需求拼接上图表类型
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal = userGoal + ", 请使用" + chartType;
        }
        userInput.append("分析需求：").append(userGoal).append("\n");

        // 压缩后的数据
//        日期,用户数
//        1号,10
//        2号,20
//        3号,40
        String csvData = chart.getChartData();
        userInput.append("原始数据：").append(csvData).append("\n");

        Chart updateChart = new Chart();
        updateChart.setId(chart.getId());
        // 先将任务状态改为执行中。（为了防止重负执行）如果调用AI执行成功，任务状态改为已成功；否者，任务状态改失败。
        updateChart.setStatus("running");
        boolean b = chartService.updateById(updateChart);
        if (!b) {
            channel.basicNack(deliveryTag, false, false);
            handleChartUpdateError(chart.getId(), "更新图表执行中状态失败");
            return;
        }
        // TODO 设置调用AI接口的超时时间
        String result = aiManager.doChat(CommonConstant.BI_MODEL_ID, userInput.toString());
        // 对AI返回的result进行处理，将生成的图表数据和图表结论分别存储
        String[] splits = result.split("【【【【【");
        if (splits.length < 3) {
            channel.basicNack(deliveryTag, false, false);
            handleChartUpdateError(chart.getId(), "AI 生成错误");
            return;
        }
        String genChart = splits[1].trim();
        String genResult = splits[2].trim();
        // 任务状态改为已成功
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chart.getId());
        // TODO 状态的值定义成枚举类
        updateChartResult.setStatus("succeed");
        updateChartResult.setGenChart(genChart);
        updateChartResult.setGenResult(genResult);
        boolean updateResult = chartService.updateById(updateChartResult);
        if (!updateResult) {
            channel.basicNack(deliveryTag, false, false);
            handleChartUpdateError(chart.getId(), "更新图表成功状态失败");
        }

        // 投递标签是一个数字标识,它在消息消费者接收到消息后用于向RabbitMQ确认消息的处理状态。通过将投递标签传递给channel.basicAck(deliveryTag, false)方法,可以告知RabbitMQ该消息已经成功处理,可以进行确认和从队列中删除。
        // 手动确认消息的接收，向RabbitMQ发送确认消息
        channel.basicAck(deliveryTag, false);
    }

    private void handleChartUpdateError(Long chartId, String execMessage) {
        Chart updateChartFail = new Chart();
        updateChartFail.setId(chartId);
        updateChartFail.setStatus(execMessage);
        boolean b = chartService.updateById(updateChartFail);
        if (!b) {
            log.info("更新图表失败状态失败" + chartId + "," + execMessage);
        }
    }
}
