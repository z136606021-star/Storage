package com.storage.experience.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ExperienceRecordDetailVO extends ExperienceRecordVO {

    private List<ExperienceAttachmentVO> attachments;
}
