package com.storage.system.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.storage.system.user.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("SELECT * FROM sys_user WHERE username = #{username} LIMIT 1")
    SysUser selectByUsername(@Param("username") String username);

    @Select("SELECT * FROM sys_user WHERE email = #{email} LIMIT 1")
    SysUser selectByEmail(@Param("email") String email);

    @Select("""
            SELECT * FROM sys_user
            WHERE username = #{identity}
               OR email = #{identity}
            LIMIT 1
            """)
    SysUser selectByUsernameOrEmail(@Param("identity") String identity);

    @Select("""
            SELECT r.code FROM sys_role r
            INNER JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId}
            """)
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT r.name FROM sys_role r
            INNER JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId}
            """)
    List<String> selectRoleNamesByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT DISTINCT m.permission FROM sys_menu m
            INNER JOIN sys_role_menu rm ON rm.menu_id = m.id
            INNER JOIN sys_user_role ur ON ur.role_id = rm.role_id
            WHERE ur.user_id = #{userId} AND m.permission IS NOT NULL
            """)
    List<String> selectPermissionsByUserId(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM sys_user u INNER JOIN sys_user_role ur ON ur.user_id = u.id INNER JOIN sys_role r ON r.id = ur.role_id WHERE r.code = 'ADMIN' AND u.status = 1")
    long countActiveAdmins();
}
