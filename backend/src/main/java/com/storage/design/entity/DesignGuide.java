package com.storage.design.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("design_guide")
public class DesignGuide {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productTypeId;

    private String productTypeCode;

    private String productTypeName;

    private Long stageId;

    private String stageName;

    private String scope;

    private String checkItem;

    private String remark;

    private Long recorderUserId;

    private String recorderName;

    private LocalDateTime recordedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
