package com.storage.experience.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ExperienceRecordQueryDTO {

    private Integer page;

    private Integer pageSize;

    private Long typeId;

    private String recorderName;

    private String keyword;

    private LocalDate recordedStart;

    private LocalDate recordedEnd;

    private List<Long> ids;
}
