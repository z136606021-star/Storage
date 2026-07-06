package com.storage.system.menu.service;

import com.storage.system.menu.dto.NavMenuNodeVO;
import com.storage.system.menu.dto.SysMenuSaveDTO;
import com.storage.system.menu.dto.SysMenuVO;

import java.util.List;

public interface SysMenuService {

    List<SysMenuVO> listTree();

    List<NavMenuNodeVO> navTreeForCurrentUser();

    SysMenuVO create(SysMenuSaveDTO dto);

    SysMenuVO update(Long id, SysMenuSaveDTO dto);

    void delete(Long id);
}
