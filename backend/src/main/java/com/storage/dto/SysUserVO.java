package com.storage.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SysUserVO {

    private Long id;

    private String username;

    private String displayName;

    private String email;

    private String phone;

    private Integer status;

    private List<Long> roleIds;

    private List<String> roleCodes;

    private List<String> roleNames;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
