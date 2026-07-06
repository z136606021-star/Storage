package com.storage.system.customer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_customer")
public class SysCustomer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String customerCode;

    private String name;

    private String contactName;

    private String phone;

    private String email;

    private String address;

    private String remark;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
