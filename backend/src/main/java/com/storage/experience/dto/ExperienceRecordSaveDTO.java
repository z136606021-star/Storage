package com.storage.experience.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ExperienceRecordSaveDTO {

    @NotNull(message = "类型不能为空")
    private Long typeId;

    @NotBlank(message = "描述不能为空")
    @Size(max = 2000, message = "描述不能超过2000个字符")
    private String description;

    @Size(max = 2000, message = "影响不能超过2000个字符")
    private String impact;

    @Size(max = 2000, message = "建议不能超过2000个字符")
    private String suggestion;

    @Size(max = 2000, message = "行动方案不能超过2000个字符")
    private String actionPlan;

    private LocalDateTime recordedAt;

    private List<@Size(max = 128, message = "关联项目不能超过128个字符") String> projectNames;

    private List<@Size(max = 255, message = "附件标识不能超过255个字符") String> attachmentObjectKeys;
}
