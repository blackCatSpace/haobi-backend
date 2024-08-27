package com.hao.springbootinit.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
public class ChartData implements Serializable {
    /**
     * id
     */

    private Long id;
    /**
     * 图表名称
     */
    private Integer 日期;

    /**
     * 分析目标
     */
    private Integer 用户数;
}
