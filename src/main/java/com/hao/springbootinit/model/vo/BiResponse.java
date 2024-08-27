package com.hao.springbootinit.model.vo;

import lombok.Data;

@Data
public class BiResponse {
    private String genChart;
    private String genResult;
    // 新生成的图表id
    private Long chartId;
}
