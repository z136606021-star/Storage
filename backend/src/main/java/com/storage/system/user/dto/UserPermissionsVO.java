package com.storage.system.user.dto;

import com.storage.system.menu.dto.SysMenuVO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserPermissionsVO {

    private List<SysMenuVO> menuTree;

    private List<Long> checkedMenuIds;
}
