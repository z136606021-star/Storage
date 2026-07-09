package com.storage.experience.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("experience_project_link")
public class ExperienceProjectLink {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long recordId;

    private String projectName;

    private Integer sortOrder;
}
