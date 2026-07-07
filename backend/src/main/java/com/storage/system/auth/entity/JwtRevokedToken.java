package com.storage.system.auth.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("jwt_revoked_token")
public class JwtRevokedToken {

    @TableId
    private String jti;

    private LocalDateTime expiresAt;

    private LocalDateTime revokedAt;
}
