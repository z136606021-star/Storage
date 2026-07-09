package com.storage.experience.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("experience_attachment")
public class ExperienceAttachment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long recordId;

    private Long fileId;

    private String objectKey;

    private String originalName;

    private String contentType;

    private Long sizeBytes;

    private Integer sortOrder;
}
