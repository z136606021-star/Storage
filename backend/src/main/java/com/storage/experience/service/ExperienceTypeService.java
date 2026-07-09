package com.storage.experience.service;

import com.storage.experience.dto.ExperienceTypeSaveDTO;
import com.storage.experience.entity.ExperienceType;

import java.util.List;

public interface ExperienceTypeService {

    List<ExperienceType> listAll();

    List<ExperienceType> listEnabled();

    ExperienceType create(ExperienceTypeSaveDTO dto);

    ExperienceType update(Long id, ExperienceTypeSaveDTO dto);

    void delete(Long id);
}
