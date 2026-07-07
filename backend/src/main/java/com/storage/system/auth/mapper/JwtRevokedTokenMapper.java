package com.storage.system.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.storage.system.auth.entity.JwtRevokedToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface JwtRevokedTokenMapper extends BaseMapper<JwtRevokedToken> {

    @Select("SELECT COUNT(1) FROM jwt_revoked_token WHERE jti = #{jti}")
    long countByJti(@Param("jti") String jti);
}
