package com.storage.system.user.dto;

import lombok.Data;

@Data
public class SysUserQueryDTO {

    private Integer page = 1;

    private Integer pageSize = 10;

    /** @deprecated 兼容旧参数，优先使用分字段筛选 */
    private String keyword;

    private String username;

    private String displayName;

    private String email;

    private Long roleId;

    private Integer status;
}
