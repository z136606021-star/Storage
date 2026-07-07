package com.storage.system.menu.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class NavMenuNodeVO {

    private String key;

    private String label;

    private String path;

    private String permission;

    private String componentKey;

    private String icon;

    private Integer visible;

    private List<NavMenuNodeVO> children;
}
