package com.hao.springbootinit.controller;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hao.springbootinit.annotation.AuthCheck;
import com.hao.springbootinit.bizmq.BiMessageProducer;
import com.hao.springbootinit.bizmq.BiMqConstant;
import com.hao.springbootinit.common.BaseResponse;
import com.hao.springbootinit.common.DeleteRequest;
import com.hao.springbootinit.common.ErrorCode;
import com.hao.springbootinit.common.ResultUtils;
import com.hao.springbootinit.exception.ThrowUtils;
import com.hao.springbootinit.manager.AiManager;
import com.hao.springbootinit.manager.RedisLimiterManager;
import com.hao.springbootinit.model.dto.chart.*;
import com.hao.springbootinit.model.entity.Chart;
import com.hao.springbootinit.model.entity.User;
import com.hao.springbootinit.model.vo.BiResponse;
import com.hao.springbootinit.service.ChartService;
import com.hao.springbootinit.service.UserService;
import com.hao.springbootinit.utils.ExcelUtils;
import com.hao.springbootinit.utils.SqlUtils;
import com.hao.springbootinit.constant.CommonConstant;
import com.hao.springbootinit.constant.UserConstant;
import com.hao.springbootinit.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 图表接口
 *
 *
 */
@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;
    @Resource
    private AiManager aiManager;
    @Resource
    private RedisLimiterManager redisLimiterManager;
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;
    @Resource
    BiMessageProducer biMessageProducer;


    /**
     * 智能分析（异步）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */

    @PostMapping("/gen/async/mq")
    public BaseResponse<BiResponse> genChartByAiAsyncMq(@RequestPart("file") MultipartFile multipartFile,
                                                        GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();

        // 校验请求参数
        // 如果分析目标为空，就抛出请求参数错误异常，并给出提示
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        // 如果名称不为空，并且名称长度大于100，就抛出异常，并给出提示
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");

        // 校验文件
        // 获取文件大小
        long size = multipartFile.getSize();
        // 获取文件名
        String originalFilename = multipartFile.getOriginalFilename();

        // 校验文件大小
        final long ONE_MB = 1024 * 1024L;
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件超过1M");
        // TODO 更高级别的校验文件后缀
        // 校验文件后缀
        // 获取文件后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        // 定义后缀白名单
        List<String> validFileSuffixList = Arrays.asList("png", "jpg", "svg", "webp", "jpeg", "xlsx");
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀非法");

        // 通过request的Session中获取
        User loginUser = userService.getLoginUser(request);

        // 限流判断，每个用户一个限流器
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());

//         分析需求：
//         分析网站用户的增长情况
//         原始数据：
//         日期,用户数
//         1号,10
//         2号,20
//         3号,30

        // 读取用户上传的excel文件，进行处理
        // 用户输入
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
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        userInput.append("原始数据：").append(csvData).append("\n");

        // 在调用AI服务之前，就保存Chart到数据库中
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        // 设置状态为排队
        chart.setStatus("wait");
        chart.setUserId(loginUser.getId());
        boolean saveResult = chartService.save(chart);
        // 校验图表是否保存成功
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");

        // 通过线程池异步调用AI服务改为将数据库中新增的chart的id发给mq，让消费者异步处理
        // 获取新增图表后的id
        Long chartId = chart.getId();
        biMessageProducer.sendMessage(BiMqConstant.BI_EXCHANGE_NAME, BiMqConstant.BI_ROUTING_KEY, chartId.toString());


        /*// 将调用AiManner接口获取Ai生成的结果设置为一个任务并交给线程池执行
        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                Chart updateChart = new Chart();
                updateChart.setId(chart.getId());
                // 先将任务状态改为执行中。（为了防止重负执行）如果调用AI执行成功，任务状态改为已成功；否者，任务状态改失败。
                updateChart.setStatus("running");
                boolean b = chartService.updateById(updateChart);
                if (!b) {
                    handleChartUpdateError(chart.getId(), "更新图表执行中状态失败");
                    return;
                }
                // TODO 设置调用AI接口的超时时间
                String result = aiManager.doChat(biModelId, userInput.toString());
                // 对AI返回的result进行处理，将生成的图表数据和图表结论分别存储
                String[] splits = result.split("【【【【【");
                if (splits.length < 3) {
                    handleChartUpdateError(chart.getId(), "AI 生成错误");
                    return;
                }
                String genChart = splits[1].trim();
                String genResult = splits[2].trim();
                // 任务状态改为已成功
                Chart updateChartResult = new Chart();
                updateChartResult.setId(chart.getId());
                updateChartResult.setStatus("succeed");
                updateChartResult.setGenChart(genChart);
                updateChartResult.setGenResult(genResult);
                boolean updateResult = chartService.updateById(updateChartResult);
                if (!updateResult) {
                    handleChartUpdateError(chart.getId(), "更新图表成功状态失败");
                }
                // TODO 提示用户图表生成成功或失败
                // TODO 设置定时任务将失败状态的图表再放到队列中
            }
        }, threadPoolExecutor);*/

        // 返回前端BiResponse
        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chartId);

        return ResultUtils.success(biResponse);
    }

    /**
     * 智能分析（异步）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */

    @PostMapping("/gen/async")
    public BaseResponse<BiResponse> genChartByAiAsync(@RequestPart("file") MultipartFile multipartFile,
                                                      GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();

        // 校验请求参数
        // 如果分析目标为空，就抛出请求参数错误异常，并给出提示
        ThrowUtils.throwIf(StringUtils.isBlank(name), ErrorCode.PARAMS_ERROR, "目标为空");
        // 如果名称不为空，并且名称长度大于100，就抛出异常，并给出提示
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");

        // 校验文件
        // 获取文件大小
        long size = multipartFile.getSize();
        // 获取文件名
        String originalFilename = multipartFile.getOriginalFilename();

        // 校验文件大小
        final long ONE_MB = 1024 * 1024L;
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件超过1M");
        // TODO 更高级别的校验文件后缀
        // 校验文件后缀
        // 获取文件后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        // 定义后缀白名单
        List<String> validFileSuffixList = Arrays.asList("png", "jpg", "svg", "webp", "jpeg", "xlsx");
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀非法");

        // 通过request的Session中获取
        User loginUser = userService.getLoginUser(request);

        // 限流判断，每个用户一个限流器
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());

        // 自己选定的AI模型
        long biModelId = 1810651263133483010L;
//         分析需求：
//         分析网站用户的增长情况
//         原始数据：
//         日期,用户数
//         1号,10
//         2号,20
//         3号,30

        // 读取用户上传的excel文件，进行处理
        // 用户输入
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
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        userInput.append("原始数据：").append(csvData).append("\n");

        // 在调用AI服务之前，就保存Chart到数据库中
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        // 设置状态为排队
        chart.setStatus("wait");
        chart.setUserId(loginUser.getId());
        boolean saveResult = chartService.save(chart);
        // 校验图表是否保存成功
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");

        // 将调用AiManner接口获取Ai生成的结果设置为一个任务并交给线程池执行
        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                Chart updateChart = new Chart();
                updateChart.setId(chart.getId());
                // 先将任务状态改为执行中。（为了防止重负执行）如果调用AI执行成功，任务状态改为已成功；否者，任务状态改失败。
                updateChart.setStatus("running");
                boolean b = chartService.updateById(updateChart);
                if (!b) {
                    handleChartUpdateError(chart.getId(), "更新图表执行中状态失败");
                    return;
                }
                // TODO 设置调用AI接口的超时时间
                String result = aiManager.doChat(biModelId, userInput.toString());
                // 对AI返回的result进行处理，将生成的图表数据和图表结论分别存储
                String[] splits = result.split("【【【【【");
                if (splits.length < 3) {
                    handleChartUpdateError(chart.getId(), "AI 生成错误");
                    return;
                }
                String genChart = splits[1].trim();
                String genResult = splits[2].trim();
                // 任务状态改为已成功
                Chart updateChartResult = new Chart();
                updateChartResult.setId(chart.getId());
                updateChartResult.setStatus("succeed");
                updateChartResult.setGenChart(genChart);
                updateChartResult.setGenResult(genResult);
                boolean updateResult = chartService.updateById(updateChartResult);
                if (!updateResult) {
                    handleChartUpdateError(chart.getId(), "更新图表成功状态失败");
                }
                // TODO 提示用户图表生成成功或失败
                // TODO 设置定时任务将失败状态的图表再放到队列中
            }
        }, threadPoolExecutor);
        // 获取新增图表后的id
        Long chartId = chart.getId();
        // 返回前端BiResponse
        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chartId);

        return ResultUtils.success(biResponse);
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

    /**
     * 智能分析（同步）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen/sync")
    public BaseResponse<BiResponse> genChartByAi(@RequestPart("file") MultipartFile multipartFile,
                                                 GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();

        // 校验请求参数
        // 如果分析目标为空，就抛出请求参数错误异常，并给出提示
        ThrowUtils.throwIf(StringUtils.isBlank(name), ErrorCode.PARAMS_ERROR, "目标为空");
        // 如果名称不为空，并且名称长度大于100，就抛出异常，并给出提示
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");

        // 校验文件
        // 获取文件大小
        long size = multipartFile.getSize();
        // 获取文件名
        String originalFilename = multipartFile.getOriginalFilename();

        // 校验文件大小
        final long ONE_MB = 1024 * 1024L;
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件超过1M");
        // TODO 更高级别的校验文件后缀
        // 校验文件后缀
        // 获取文件后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        // 定义后缀白名单
        List<String> validFileSuffixList = Arrays.asList("png", "jpg", "svg", "webp", "jpeg", "xlsx");
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀非法");


        // 通过request的Session中获取
        User loginUser = userService.getLoginUser(request);

        // 限流判断，每个用户一个限流器
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());

        // 自己选定的AI模型
        long biModelId = 1810651263133483010L;
//         分析需求：
//         分析网站用户的增长情况
//         原始数据：
//         日期,用户数
//         1号,10
//         2号,20
//         3号,30

        // 读取用户上传的excel文件，进行处理
        // 用户输入
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
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        userInput.append("原始数据：").append(csvData).append("\n");


        // 调用AiManner接口获取Ai生成的结果
        String result = aiManager.doChat(biModelId, userInput.toString());
        // 对AI返回的result进行处理，将生成的图表数据和图表结论分别存储
        String[] splits = result.split("【【【【【");
        if (splits.length < 3) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 生成错误");
        }
        String genChart = splits[1].trim();
        String genResult = splits[2].trim();
        // 保存Chart到数据库
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setGenChart(genChart);
        chart.setGenResult(genResult);
        chart.setUserId(loginUser.getId());
        boolean saveResult = chartService.save(chart);
        // 校验图表是否保存成功
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");
        // 获取新增图表后的id
        Long chartId = chart.getId();
        // 创建ChartData字段的表
//        chartService.createTable(String.format("chart_%s", chartId));
        // 向chart_chartId表中存入csvData数据

        // 返回前端BiResponse
        BiResponse biResponse = new BiResponse();
        biResponse.setGenChart(genChart);
        biResponse.setGenResult(genResult);
        biResponse.setChartId(chartId);

        return ResultUtils.success(biResponse);


    }

    // region 增删改查

    /**
     * 创建
     *
     * @param chartAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
        if (chartAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest, chart);
        User loginUser = userService.getLoginUser(request);
        chart.setUserId(loginUser.getId());
        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newChartId = chart.getId();
        return ResultUtils.success(newChartId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param chartUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartUpdateRequest, chart);
        long id = chartUpdateRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Chart> getChartById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(chart);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                     HttpServletRequest request) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<Chart>> listMyChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                       HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        long pages = chartPage.getPages();
        List<Chart> records = chartPage.getRecords();
        long total = chartPage.getTotal();

        return ResultUtils.success(chartPage);
    }

    // endregion

    /**
     * 编辑（用户）
     *
     * @param chartEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editChart(@RequestBody ChartEditRequest chartEditRequest, HttpServletRequest request) {
        if (chartEditRequest == null || chartEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartEditRequest, chart);
        User loginUser = userService.getLoginUser(request);
        long id = chartEditRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldChart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 获取查询包装类
     *
     * @param chartQueryRequest
     * @return
     */
    private QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chartQueryRequest.getId();
        String goal = chartQueryRequest.getGoal();
        String chartType = chartQueryRequest.getChartType();
        Long userId = chartQueryRequest.getUserId();
        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();

        queryWrapper.eq(id != null && id > 0, "id", id);
        queryWrapper.eq(StringUtils.isNotBlank(goal), "goal", goal);
        queryWrapper.eq(StringUtils.isNotBlank(chartType), "chartType", chartType);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }


}
