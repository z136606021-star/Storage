package com.storage.system.role.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.storage.common.exception.BusinessException;
import com.storage.system.menu.entity.SysMenu;
import com.storage.system.menu.mapper.SysMenuMapper;
import com.storage.system.role.dto.SysRoleSaveDTO;
import com.storage.system.role.dto.SysRoleVO;
import com.storage.system.role.entity.SysRole;
import com.storage.system.role.mapper.SysRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl implements SysRoleService {

    private static final Set<String> BUILTIN_ROLE_CODES = Set.of("ADMIN", "USER");

    private final SysRoleMapper sysRoleMapper;
    private final SysMenuMapper sysMenuMapper;
    private final SysRoleExportService sysRoleExportService;

    @Override
    public List<SysRoleVO> listAll() {
        return sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId))
                .stream()
                .map(this::toVo)
                .toList();
    }

    @Override
    public List<SysRoleVO> listEnabled() {
        return sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getStatus, 1)
                        .orderByAsc(SysRole::getId))
                .stream()
                .map(this::toVo)
                .toList();
    }

    @Override
    public SysRoleVO getById(Long id) {
        return toVo(requireRole(id));
    }

    @Override
    @Transactional
    public SysRoleVO create(SysRoleSaveDTO dto) {
        if (sysRoleMapper.selectByCode(dto.getCode()) != null) {
            throw new BusinessException("角色编码已存在");
        }
        validateMenuIds(dto.getMenuIds());

        SysRole role = new SysRole();
        role.setCode(dto.getCode().trim());
        role.setName(dto.getName().trim());
        role.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        sysRoleMapper.insert(role);
        replaceRoleMenus(role.getId(), dto.getMenuIds());
        return toVo(requireRole(role.getId()));
    }

    @Override
    @Transactional
    public SysRoleVO update(Long id, SysRoleSaveDTO dto) {
        SysRole role = requireRole(id);
        SysRole existing = sysRoleMapper.selectByCode(dto.getCode());
        if (existing != null && !existing.getId().equals(id)) {
            throw new BusinessException("角色编码已存在");
        }
        validateMenuIds(dto.getMenuIds());

        role.setCode(dto.getCode().trim());
        role.setName(dto.getName().trim());
        role.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        sysRoleMapper.updateById(role);
        replaceRoleMenus(id, dto.getMenuIds());
        return toVo(requireRole(id));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        SysRole role = requireRole(id);
        if (BUILTIN_ROLE_CODES.contains(role.getCode())) {
            throw new BusinessException("内置角色不可删除");
        }
        sysMenuMapper.deleteRoleMenusByRoleId(id);
        sysRoleMapper.deleteById(id);
    }

    @Override
    public byte[] export() throws IOException {
        return sysRoleExportService.export(listAll());
    }

    @Override
    public byte[] exportTemplate() throws IOException {
        return sysRoleExportService.exportTemplate();
    }

    private SysRole requireRole(Long id) {
        SysRole role = sysRoleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        return role;
    }

    private void validateMenuIds(List<Long> menuIds) {
        for (Long menuId : menuIds) {
            if (sysMenuMapper.selectById(menuId) == null) {
                throw new BusinessException("菜单不存在");
            }
        }
    }

    private void replaceRoleMenus(Long roleId, List<Long> menuIds) {
        sysMenuMapper.deleteRoleMenusByRoleId(roleId);
        for (Long menuId : menuIds) {
            sysMenuMapper.insertRoleMenu(roleId, menuId);
        }
    }

    private SysRoleVO toVo(SysRole role) {
        List<Long> menuIds = sysRoleMapper.selectMenuIdsByRoleId(role.getId());
        List<String> permissions = new ArrayList<>();
        for (Long menuId : menuIds) {
            SysMenu menu = sysMenuMapper.selectById(menuId);
            if (menu != null && StringUtils.hasText(menu.getPermission())) {
                permissions.add(menu.getPermission());
            }
        }
        return SysRoleVO.builder()
                .id(role.getId())
                .code(role.getCode())
                .name(role.getName())
                .status(role.getStatus() == null ? 1 : role.getStatus())
                .menuIds(menuIds)
                .permissions(permissions)
                .createdAt(role.getCreatedAt())
                .build();
    }
}
