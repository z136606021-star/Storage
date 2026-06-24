package com.storage.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SysMenuVO {

    private Long id;

    private Long parentId;

    private String menuType;

    private String name;

    private String permission;

    private String path;

    private String icon;

    private Integer visible;

    private Integer sortOrder;

    private List<SysMenuVO> children;
}
