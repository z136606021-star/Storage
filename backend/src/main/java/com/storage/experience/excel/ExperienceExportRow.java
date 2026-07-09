package com.storage.experience.excel;

import lombok.Data;
import org.jeecgframework.poi.excel.annotation.Excel;

@Data
public class ExperienceExportRow {

    @Excel(name = "序号", orderNum = "0")
    private Integer index;

    @Excel(name = "类型", orderNum = "1")
    private String typeName;

    @Excel(name = "描述", orderNum = "2", width = 40)
    private String description;

    @Excel(name = "影响", orderNum = "3", width = 40)
    private String impact;

    @Excel(name = "建议", orderNum = "4", width = 40)
    private String suggestion;

    @Excel(name = "行动方案", orderNum = "5", width = 40)
    private String actionPlan;

    @Excel(name = "关联项目", orderNum = "6", width = 30)
    private String projects;

    @Excel(name = "记录人", orderNum = "7")
    private String recorderName;

    @Excel(name = "记录时间", orderNum = "8")
    private String recordedAt;
}
