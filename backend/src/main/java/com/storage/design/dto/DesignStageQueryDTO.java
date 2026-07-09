package com.storage.design.dto;

import lombok.Data;

import java.util.List;

@Data
public class DesignStageQueryDTO {

    private String stageName;

    private Integer enabled;

    private List<Long> ids;

    private Integer page = 1;

    private Integer pageSize = 10;
}
