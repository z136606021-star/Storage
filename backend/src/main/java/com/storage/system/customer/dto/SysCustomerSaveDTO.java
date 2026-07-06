package com.storage.system.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SysCustomerSaveDTO {

    @NotBlank(message = "客户编号不能为空")
    private String customerCode;

    @NotBlank(message = "客户名称不能为空")
    private String name;

    private String contactName;

    private String phone;

    private String email;

    private String address;

    private String remark;

    @NotNull(message = "状态不能为空")
    private Integer status;
}
