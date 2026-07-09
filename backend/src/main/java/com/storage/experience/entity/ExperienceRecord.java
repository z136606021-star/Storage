package com.storage.experience.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("experience_record")
public class ExperienceRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long typeId;

    private String description;

    private String impact;

    private String suggestion;

    private String actionPlan;

    private Long recorderUserId;

    private String recorderName;

    private LocalDateTime recordedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
