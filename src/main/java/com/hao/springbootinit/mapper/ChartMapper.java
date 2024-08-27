package com.hao.springbootinit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hao.springbootinit.model.entity.Chart;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * @Entity com.yupi.springbootinit.model.entity.Chart
 */
@Mapper
public interface ChartMapper extends BaseMapper<Chart> {
    /**
     * 动态创建chart_id
     * @param tableName
     */
    void createTable(String tableName);

    /**
     * 动态查询chart_id表中的数据
     * @param querySql
     * @return
     */
    List<Map<String, Object>> queryChartData(String querySql);
}




