package com.storage.system.menu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_menu")
public class SysMenu {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;

    private String menuType;

    private String name;

    private String permission;

    private String path;

    private String componentKey;

    private String icon;

    private Integer visible;

    private Integer sortOrder;

    private LocalDateTime createdAt;
}
