package com.storage.experience.dto;

import com.storage.experience.entity.ExperienceType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExperienceFilterOptionsVO {

    private List<ExperienceType> types;

    private List<String> recorderNames;
}
