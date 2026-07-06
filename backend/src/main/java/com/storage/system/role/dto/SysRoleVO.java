package com.storage.system.role.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SysRoleVO {

    private Long id;

    private String code;

    private String name;

    private Integer status;

    private List<Long> menuIds;

    private List<String> permissions;

    private LocalDateTime createdAt;
}
