package com.storage.system.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.storage.system.auth.entity.PasswordResetToken;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface PasswordResetTokenMapper extends BaseMapper<PasswordResetToken> {

    @Select("SELECT * FROM password_reset_token WHERE token_hash = #{tokenHash} LIMIT 1 FOR UPDATE")
    PasswordResetToken selectByTokenHashForUpdate(@Param("tokenHash") String tokenHash);
}
