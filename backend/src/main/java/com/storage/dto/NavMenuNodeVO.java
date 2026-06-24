package com.storage.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class NavMenuNodeVO {

    private String key;

    private String label;

    private String path;

    private String icon;

    private List<NavMenuNodeVO> children;
}
