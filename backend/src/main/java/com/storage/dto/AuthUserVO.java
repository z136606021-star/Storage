package com.storage.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AuthUserVO {

    private Long id;

    private String username;

    private String displayName;
}
