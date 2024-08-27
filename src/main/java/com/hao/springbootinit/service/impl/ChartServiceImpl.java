package com.hao.springbootinit.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hao.springbootinit.mapper.ChartMapper;
import com.hao.springbootinit.model.entity.Chart;
import com.hao.springbootinit.service.ChartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 */
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
    implements ChartService {
    @Autowired
    private ChartMapper chartMapper;
    /**
     * 动态创建chart_id
     * @param tableName
     */
    @Override
    public void createTable(String tableName) {
        chartMapper.createTable(tableName);
    }
}




