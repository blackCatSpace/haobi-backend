package com.hao.springbootinit.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hao.springbootinit.model.entity.Chart;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

@SpringBootTest
class ChartMapperTest {
    @Autowired
    private ChartMapper chartMapper;

    @Test
    void createTable() {
        String chartId = "1811036232512307201";
        String tableName = String.format("chart_%s", chartId);
        chartMapper.createTable(tableName);
    }

    @Test
    void queryChartData() {
        String chartId = "1811036232512307201";
        String querySql = String.format("select * from chart_%s", chartId);
        List<Map<String, Object>> resultData = chartMapper.queryChartData(querySql);
        System.out.println(resultData);
    }

    @Test
    void testQuery() {
        Wrapper<Chart> wrapper = new QueryWrapper<Chart>().like("chartType", "饼");
        List<Chart> charts = chartMapper.selectList(wrapper);
        System.out.println(charts);
    }
}