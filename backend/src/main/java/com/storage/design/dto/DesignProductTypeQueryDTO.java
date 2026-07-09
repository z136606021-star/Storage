package com.storage.design.dto;

import lombok.Data;

import java.util.List;

@Data
public class DesignProductTypeQueryDTO {

    private String typeCode;

    private String typeName;

    private Integer enabled;

    private List<Long> ids;

    private Integer page = 1;

    private Integer pageSize = 10;
}
