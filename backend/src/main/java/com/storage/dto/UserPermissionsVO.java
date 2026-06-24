package com.storage.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserPermissionsVO {

    private List<SysMenuVO> menuTree;

    private List<Long> checkedMenuIds;
}
