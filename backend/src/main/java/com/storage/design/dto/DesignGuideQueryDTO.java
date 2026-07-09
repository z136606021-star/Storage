package com.storage.design.dto;

import lombok.Data;

import java.util.List;

@Data
public class DesignGuideQueryDTO {

    private Long productTypeId;

    private Long stageId;

    private String scope;

    private String checkItem;

    private List<Long> ids;

    private Integer page = 1;

    private Integer pageSize = 10;
}
