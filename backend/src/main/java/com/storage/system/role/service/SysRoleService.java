package com.storage.system.role.service;

import com.storage.system.role.dto.SysRoleSaveDTO;
import com.storage.system.role.dto.SysRoleVO;

import java.io.IOException;
import java.util.List;

public interface SysRoleService {

    List<SysRoleVO> listAll();

    List<SysRoleVO> listEnabled();

    SysRoleVO getById(Long id);

    SysRoleVO create(SysRoleSaveDTO dto);

    SysRoleVO update(Long id, SysRoleSaveDTO dto);

    void delete(Long id);

    byte[] export() throws IOException;

    byte[] exportTemplate() throws IOException;
}
