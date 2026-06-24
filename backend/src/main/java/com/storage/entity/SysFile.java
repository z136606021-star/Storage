package com.storage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_file")
public class SysFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String objectKey;

    private String originalName;

    private String contentType;

    private Long sizeBytes;

    private Long uploaderId;

    private LocalDateTime createdAt;
}
