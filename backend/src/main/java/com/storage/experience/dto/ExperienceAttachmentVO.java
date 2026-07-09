package com.storage.experience.dto;

import lombok.Data;

@Data
public class ExperienceAttachmentVO {

    private Long id;

    private String objectKey;

    private String originalName;

    private String contentType;

    private Long sizeBytes;

    private String url;

    private Boolean previewable;
}
