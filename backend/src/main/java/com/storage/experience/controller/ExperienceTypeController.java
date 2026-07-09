package com.storage.experience.controller;

import com.storage.experience.dto.ExperienceTypeSaveDTO;
import com.storage.experience.entity.ExperienceType;
import com.storage.experience.service.ExperienceTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/experience/types")
@RequiredArgsConstructor
public class ExperienceTypeController {

    private final ExperienceTypeService experienceTypeService;

    @GetMapping
    @RequiresPermissions("platform:experience:read")
    public List<ExperienceType> list() {
        return experienceTypeService.listAll();
    }

    @PostMapping
    @RequiresPermissions("platform:experience:write")
    public ExperienceType create(@Valid @RequestBody ExperienceTypeSaveDTO dto) {
        return experienceTypeService.create(dto);
    }

    @PutMapping("/{id}")
    @RequiresPermissions("platform:experience:write")
    public ExperienceType update(@PathVariable Long id, @Valid @RequestBody ExperienceTypeSaveDTO dto) {
        return experienceTypeService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @RequiresPermissions("platform:experience:write")
    public void delete(@PathVariable Long id) {
        experienceTypeService.delete(id);
    }
}
