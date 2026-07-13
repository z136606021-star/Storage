package com.storage.system.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.storage.system.auth.entity.EmailVerificationCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EmailVerificationCodeMapper extends BaseMapper<EmailVerificationCode> {

    @Select("""
            SELECT * FROM email_verification_code
            WHERE user_id = #{userId} AND purpose = #{purpose}
            ORDER BY created_at DESC
            LIMIT 1
            """)
    EmailVerificationCode selectLatestByUserAndPurpose(@Param("userId") Long userId, @Param("purpose") String purpose);

    @Select("""
            SELECT * FROM email_verification_code
            WHERE user_id = #{userId}
              AND purpose = #{purpose}
              AND code_hash = #{codeHash}
              AND used_at IS NULL
              AND expires_at > CURRENT_TIMESTAMP
            LIMIT 1
            FOR UPDATE
            """)
    EmailVerificationCode selectValidForUpdate(
            @Param("userId") Long userId,
            @Param("purpose") String purpose,
            @Param("codeHash") String codeHash
    );
}
