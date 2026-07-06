package com.storage.system.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.storage.common.dto.PageResult;
import com.storage.common.exception.BusinessException;
import com.storage.system.auth.service.AuthService;
import com.storage.system.menu.mapper.SysMenuMapper;
import com.storage.system.menu.service.SysMenuService;
import com.storage.system.role.entity.SysRole;
import com.storage.system.role.mapper.SysRoleMapper;
import com.storage.system.user.dto.ResetPasswordDTO;
import com.storage.system.user.dto.SysUserQueryDTO;
import com.storage.system.user.dto.SysUserSaveDTO;
import com.storage.system.user.dto.SysUserVO;
import com.storage.system.user.dto.UserPermissionsVO;
import com.storage.system.user.dto.UserStatusDTO;
import com.storage.system.user.entity.SysUser;
import com.storage.system.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

    private static final String ADMIN_ROLE_CODE = "ADMIN";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysMenuMapper sysMenuMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final SysMenuService sysMenuService;
    private final SysUserExportService sysUserExportService;

    @Value("${storage.auth.default-password-suffix:@123}")
    private String defaultPasswordSuffix;

    @Override
    public PageResult<SysUserVO> page(SysUserQueryDTO query) {
        int page = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 10 : query.getPageSize();

        Page<SysUser> result = sysUserMapper.selectPage(new Page<>(page, pageSize), buildQueryWrapper(query));
        List<SysUserVO> records = result.getRecords().stream().map(this::toVo).toList();
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public List<SysUserVO> listByQuery(SysUserQueryDTO query) {
        return sysUserMapper.selectList(buildQueryWrapper(query)).stream().map(this::toVo).toList();
    }

    @Override
    public SysUserVO getById(Long id) {
        return toVo(requireUser(id));
    }

    @Override
    public UserPermissionsVO getPermissions(Long userId) {
        requireUser(userId);
        return UserPermissionsVO.builder()
                .menuTree(sysMenuService.listTree())
                .checkedMenuIds(sysMenuMapper.selectMenuIdsByUserId(userId))
                .build();
    }

    @Override
    @Transactional
    public SysUserVO create(SysUserSaveDTO dto) {
        validateSaveDto(dto);
        if (sysUserMapper.selectByUsername(dto.getUsername()) != null) {
            throw new BusinessException("NTID 已存在");
        }
        validateRoleIds(dto.getRoleIds());

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername().trim());
        user.setDisplayName(dto.getDisplayName().trim());
        user.setEmail(trimToNull(dto.getEmail()));
        user.setPhone(trimToNull(dto.getPhone()));
        user.setPasswordHash(passwordEncoder.encode(resolvePassword(dto)));
        user.setStatus(dto.getStatus());
        sysUserMapper.insert(user);
        replaceUserRoles(user.getId(), dto.getRoleIds());
        return toVo(requireUser(user.getId()));
    }

    @Override
    @Transactional
    public SysUserVO update(Long id, SysUserSaveDTO dto) {
        validateSaveDto(dto);
        SysUser user = requireUser(id);
        SysUser existing = sysUserMapper.selectByUsername(dto.getUsername());
        if (existing != null && !existing.getId().equals(id)) {
            throw new BusinessException("NTID 已存在");
        }
        validateRoleIds(dto.getRoleIds());
        assertCanModifyUser(id, dto.getRoleIds(), dto.getStatus());

        user.setUsername(dto.getUsername().trim());
        user.setDisplayName(dto.getDisplayName().trim());
        user.setEmail(trimToNull(dto.getEmail()));
        user.setPhone(trimToNull(dto.getPhone()));
        user.setStatus(dto.getStatus());
        if (StringUtils.hasText(dto.getPassword())) {
            user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }
        sysUserMapper.updateById(user);
        replaceUserRoles(id, dto.getRoleIds());
        return toVo(requireUser(id));
    }

    @Override
    @Transactional
    public void resetPassword(Long id, ResetPasswordDTO dto) {
        SysUser user = requireUser(id);
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        sysUserMapper.updateById(user);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, UserStatusDTO dto) {
        SysUser user = requireUser(id);
        List<Long> roleIds = sysMenuMapper.selectRoleIdsByUserId(id);
        assertCanModifyUser(id, roleIds, dto.getStatus());
        user.setStatus(dto.getStatus());
        sysUserMapper.updateById(user);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireUser(id);
        SysUser current = authService.currentUser();
        if (current != null && current.getId().equals(id)) {
            throw new BusinessException("不能删除当前登录账号");
        }
        List<Long> roleIds = sysMenuMapper.selectRoleIdsByUserId(id);
        assertCanModifyUser(id, roleIds, 0);
        sysUserMapper.deleteById(id);
    }

    @Override
    public byte[] export(SysUserQueryDTO query) throws IOException {
        return sysUserExportService.export(listByQuery(query));
    }

    @Override
    public byte[] exportTemplate() throws IOException {
        return sysUserExportService.exportTemplate();
    }

    private LambdaQueryWrapper<SysUser> buildQueryWrapper(SysUserQueryDTO query) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getUsername())) {
            wrapper.like(SysUser::getUsername, query.getUsername().trim());
        }
        if (StringUtils.hasText(query.getDisplayName())) {
            wrapper.like(SysUser::getDisplayName, query.getDisplayName().trim());
        }
        if (StringUtils.hasText(query.getEmail())) {
            wrapper.like(SysUser::getEmail, query.getEmail().trim());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(SysUser::getUsername, query.getKeyword())
                    .or()
                    .like(SysUser::getDisplayName, query.getKeyword()));
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysUser::getStatus, query.getStatus());
        }
        if (query.getRoleId() != null) {
            wrapper.apply(
                    "EXISTS (SELECT 1 FROM sys_user_role ur WHERE ur.user_id = sys_user.id AND ur.role_id = {0})",
                    query.getRoleId());
        }
        wrapper.orderByDesc(SysUser::getId);
        return wrapper;
    }

    private void validateSaveDto(SysUserSaveDTO dto) {
        if (StringUtils.hasText(dto.getEmail()) && !EMAIL_PATTERN.matcher(dto.getEmail().trim()).matches()) {
            throw new BusinessException("邮箱格式不正确");
        }
    }

    private String resolvePassword(SysUserSaveDTO dto) {
        if (StringUtils.hasText(dto.getPassword())) {
            return dto.getPassword();
        }
        return dto.getUsername().trim() + defaultPasswordSuffix;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private SysUser requireUser(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    private void validateRoleIds(List<Long> roleIds) {
        for (Long roleId : roleIds) {
            SysRole role = sysRoleMapper.selectById(roleId);
            if (role == null) {
                throw new BusinessException("角色不存在");
            }
            if (role.getStatus() != null && role.getStatus() != 1) {
                throw new BusinessException("角色「" + role.getName() + "」已停用，无法分配");
            }
        }
    }

    private void replaceUserRoles(Long userId, List<Long> roleIds) {
        sysMenuMapper.deleteUserRolesByUserId(userId);
        for (Long roleId : roleIds) {
            sysMenuMapper.insertUserRole(userId, roleId);
        }
    }

    private void assertCanModifyUser(Long targetUserId, List<Long> roleIds, Integer newStatus) {
        SysUser current = authService.currentUser();
        if (current != null && current.getId().equals(targetUserId) && newStatus != null && newStatus != 1) {
            throw new BusinessException("不能禁用当前登录账号");
        }
        if (isOnlyActiveAdmin(targetUserId, roleIds, newStatus)) {
            throw new BusinessException("不能移除或禁用最后一个管理员");
        }
    }

    private boolean isOnlyActiveAdmin(Long userId, List<Long> roleIds, Integer newStatus) {
        List<String> codes = roleIds.stream()
                .map(sysRoleMapper::selectById)
                .filter(role -> role != null)
                .map(SysRole::getCode)
                .toList();
        boolean targetIsAdmin = codes.contains(ADMIN_ROLE_CODE);
        boolean disabling = newStatus != null && newStatus != 1;
        boolean removingAdmin = !targetIsAdmin;
        if (!targetIsAdmin && !disabling) {
            List<String> existingCodes = sysUserMapper.selectRoleCodesByUserId(userId);
            if (!existingCodes.contains(ADMIN_ROLE_CODE)) {
                return false;
            }
            removingAdmin = true;
        }
        if (!removingAdmin && !disabling) {
            return false;
        }
        return sysUserMapper.countActiveAdmins() <= 1
                && sysUserMapper.selectRoleCodesByUserId(userId).contains(ADMIN_ROLE_CODE);
    }

    private SysUserVO toVo(SysUser user) {
        List<Long> roleIds = sysMenuMapper.selectRoleIdsByUserId(user.getId());
        List<String> roleCodes = sysUserMapper.selectRoleCodesByUserId(user.getId());
        List<String> roleNames = sysUserMapper.selectRoleNamesByUserId(user.getId());
        return SysUserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus())
                .roleIds(roleIds)
                .roleCodes(roleCodes)
                .roleNames(roleNames)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
