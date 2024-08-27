package com.hao.springbootinit.service;

import com.hao.springbootinit.model.entity.Chart;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 *
 */
public interface ChartService extends IService<Chart> {
    /**
     * 动态创建chart_id
     * @param tableName
     */
    void createTable(String tableName);
}
