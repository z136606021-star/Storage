package com.storage.system.user.service;

import com.storage.common.dto.PageResult;
import com.storage.system.user.dto.ResetPasswordDTO;
import com.storage.system.user.dto.SysUserQueryDTO;
import com.storage.system.user.dto.SysUserSaveDTO;
import com.storage.system.user.dto.SysUserVO;
import com.storage.system.user.dto.UserPermissionsVO;
import com.storage.system.user.dto.UserStatusDTO;

import java.io.IOException;
import java.util.List;

public interface SysUserService {

    PageResult<SysUserVO> page(SysUserQueryDTO query);

    List<SysUserVO> listByQuery(SysUserQueryDTO query);

    SysUserVO getById(Long id);

    UserPermissionsVO getPermissions(Long userId);

    SysUserVO create(SysUserSaveDTO dto);

    SysUserVO update(Long id, SysUserSaveDTO dto);

    void resetPassword(Long id, ResetPasswordDTO dto);

    void updateStatus(Long id, UserStatusDTO dto);

    void delete(Long id);

    byte[] export(SysUserQueryDTO query) throws IOException;

    byte[] exportTemplate() throws IOException;
}
