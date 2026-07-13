package com.storage.system.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.storage.system.auth.entity.RegistrationVerificationCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RegistrationVerificationCodeMapper extends BaseMapper<RegistrationVerificationCode> {

    @Select("""
            SELECT * FROM registration_verification_code
            WHERE email = #{email}
            ORDER BY created_at DESC
            LIMIT 1
            """)
    RegistrationVerificationCode selectLatestByEmail(@Param("email") String email);

    @Select("""
            SELECT * FROM registration_verification_code
            WHERE email = #{email}
              AND code_hash = #{codeHash}
              AND used_at IS NULL
              AND expires_at > CURRENT_TIMESTAMP
            LIMIT 1
            FOR UPDATE
            """)
    RegistrationVerificationCode selectValidForUpdate(
            @Param("email") String email,
            @Param("codeHash") String codeHash
    );
}
