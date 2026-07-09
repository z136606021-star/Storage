package com.storage.experience.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ExperienceRecordVO {

    private Long id;

    private Long typeId;

    private String typeName;

    private String description;

    private String impact;

    private String suggestion;

    private String actionPlan;

    private Long recorderUserId;

    private String recorderName;

    private LocalDateTime recordedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<String> projectNames;

    private Integer attachmentCount;
}
