package com.storage.system.menu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.storage.system.menu.entity.SysMenu;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    @Select("SELECT COUNT(*) FROM sys_menu WHERE parent_id = #{parentId}")
    long countByParentId(@Param("parentId") Long parentId);

    @Select("SELECT COUNT(*) FROM sys_menu WHERE permission = #{permission} AND id <> #{excludeId}")
    long countByPermissionExcludeId(@Param("permission") String permission, @Param("excludeId") Long excludeId);

    @Select("SELECT COUNT(*) FROM sys_menu WHERE permission = #{permission}")
    long countByPermission(@Param("permission") String permission);

    @Select("SELECT id FROM sys_menu WHERE permission = #{permission} LIMIT 1")
    Long selectIdByPermission(@Param("permission") String permission);

    @Select("""
            SELECT DISTINCT m.* FROM sys_menu m
            INNER JOIN sys_role_menu rm ON rm.menu_id = m.id
            INNER JOIN sys_user_role ur ON ur.role_id = rm.role_id
            WHERE ur.user_id = #{userId}
            ORDER BY m.sort_order ASC, m.id ASC
            """)
    List<SysMenu> selectMenusByUserId(@Param("userId") Long userId);

    @Insert("""
            INSERT INTO sys_role_menu (role_id, menu_id) VALUES (#{roleId}, #{menuId})
            """)
    void insertRoleMenu(@Param("roleId") Long roleId, @Param("menuId") Long menuId);

    @Delete("DELETE FROM sys_role_menu WHERE role_id = #{roleId}")
    void deleteRoleMenusByRoleId(@Param("roleId") Long roleId);

    @Insert("""
            INSERT INTO sys_user_role (user_id, role_id) VALUES (#{userId}, #{roleId})
            """)
    void insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    void deleteUserRolesByUserId(@Param("userId") Long userId);

    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId}")
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT DISTINCT rm.menu_id FROM sys_role_menu rm
            INNER JOIN sys_user_role ur ON ur.role_id = rm.role_id
            INNER JOIN sys_role r ON r.id = ur.role_id
            WHERE ur.user_id = #{userId} AND r.status = 1
            """)
    List<Long> selectMenuIdsByUserId(@Param("userId") Long userId);
}
