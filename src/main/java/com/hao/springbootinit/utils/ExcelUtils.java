package com.hao.springbootinit.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class ExcelUtils {
    public static String excelToCsv(MultipartFile multipartFile) {
        List<Map<Integer, String>> list = null;
        try {
//            file = ResourceUtils.getFile("classpath:test_excel.xlsx");
            list = EasyExcel.read(multipartFile.getInputStream())
                    .excelType(ExcelTypeEnum.XLSX)
                    .sheet()
                    .headRowNumber(0)
                    .doReadSync();
        } catch (IOException e) {
            log.error("表格处理错误", e);
        }

        if (list == null || list.isEmpty()) {
            return "";
        }
        // 转换为 csv
        StringBuilder stringBuilder = new StringBuilder();

        // 读取表头信息
        LinkedHashMap<Integer, String> headerMap = (LinkedHashMap<Integer, String>) list.get(0);
        // Map把value取出来过滤掉空字符串后再存入List<String>
        List<String> headerList = headerMap.values().stream().filter(header -> ObjectUtils.isNotEmpty(header)).collect(Collectors.toList());
        // 把List<String>数据取出来用逗号分隔开
        stringBuilder.append(StringUtils.join(headerList,",")).append("\n");

        // 读取数据信息
        for (int i = 1; i < list.size(); i++) {
            // 数据处理同上
            LinkedHashMap<Integer, String> dataMap = (LinkedHashMap) list.get(i);
            List<String> dataList = dataMap.values().stream().filter(ObjectUtils::isNotEmpty).collect(Collectors.toList());
            stringBuilder.append(StringUtils.join(dataList, ",")).append("\n");
        }
        return stringBuilder.toString();
    }

}
