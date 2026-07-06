package com.storage.system.customer.dto;

import lombok.Data;

import java.util.List;

@Data
public class SysCustomerQueryDTO {

    private String customerCode;

    private String name;

    private String contactName;

    private List<Long> ids;

    private Integer page = 1;

    private Integer pageSize = 10;
}
