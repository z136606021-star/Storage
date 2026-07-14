package com.storage.experience.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.storage.common.dto.BatchDeleteDTO;
import com.storage.common.dto.PageResult;
import com.storage.common.exception.BusinessException;
import com.storage.common.query.PageSupport;
import com.storage.experience.converter.ExperienceRecordConverter;
import com.storage.experience.dto.ExperienceAttachmentVO;
import com.storage.experience.dto.ExperienceFilterOptionsVO;
import com.storage.experience.dto.ExperienceRecordDetailVO;
import com.storage.experience.dto.ExperienceRecordQueryDTO;
import com.storage.experience.dto.ExperienceRecordSaveDTO;
import com.storage.experience.dto.ExperienceRecordVO;
import com.storage.experience.entity.ExperienceAttachment;
import com.storage.experience.entity.ExperienceProjectLink;
import com.storage.experience.entity.ExperienceRecord;
import com.storage.experience.entity.ExperienceType;
import com.storage.experience.exception.ExperienceRecordNotFoundException;
import com.storage.experience.exception.ExperienceTypeNotFoundException;
import com.storage.experience.mapper.ExperienceAttachmentMapper;
import com.storage.experience.mapper.ExperienceProjectLinkMapper;
import com.storage.experience.mapper.ExperienceRecordMapper;
import com.storage.experience.mapper.ExperienceTypeMapper;
import com.storage.experience.query.ExperienceRecordQueryBuilder;
import com.storage.infrastructure.file.entity.SysFile;
import com.storage.infrastructure.file.mapper.SysFileMapper;
import com.storage.infrastructure.file.service.FileStorageService;
import com.storage.system.user.contract.OperatorInfo;
import com.storage.system.user.contract.OperatorResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExperienceRecordServiceImpl implements ExperienceRecordService {

    private final ExperienceRecordMapper experienceRecordMapper;
    private final ExperienceTypeMapper experienceTypeMapper;
    private final ExperienceProjectLinkMapper experienceProjectLinkMapper;
    private final ExperienceAttachmentMapper experienceAttachmentMapper;
    private final SysFileMapper sysFileMapper;
    private final ExperienceRecordConverter experienceRecordConverter;
    private final ExperienceRecordExportService experienceRecordExportService;
    private final FileStorageService fileStorageService;
    private final OperatorResolver operatorResolver;

    @Override
    public PageResult<ExperienceRecordVO> page(ExperienceRecordQueryDTO query) {
        var result = experienceRecordMapper.selectPage(
                PageSupport.page(query.getPage(), query.getPageSize()),
                ExperienceRecordQueryBuilder.build(query)
        );
        return PageSupport.result(result, toListVO(result.getRecords()));
    }

    @Override
    public ExperienceRecordDetailVO getById(Long id) {
        ExperienceRecord record = getRequired(id);
        return toDetailVO(record);
    }

    @Override
    @Transactional
    public ExperienceRecordDetailVO create(ExperienceRecordSaveDTO dto) {
        OperatorInfo operator = operatorResolver.requireCurrentOperator();
        return createWithRecorder(dto, operator.getId(), operator.getUsername());
    }

    @Override
    @Transactional
    public ExperienceRecordDetailVO createImported(ExperienceRecordSaveDTO dto, String recorderName) {
        String normalizedRecorder = StringUtils.hasText(recorderName) ? recorderName.trim() : null;
        if (!StringUtils.hasText(normalizedRecorder)) {
            OperatorInfo operator = operatorResolver.requireCurrentOperator();
            return createWithRecorder(dto, operator.getId(), operator.getUsername());
        }
        return createWithRecorder(dto, null, normalizedRecorder);
    }

    @Override
    @Transactional
    public ExperienceRecordDetailVO update(Long id, ExperienceRecordSaveDTO dto) {
        ExperienceRecord existing = getRequired(id);
        validateType(dto.getTypeId());
        normalizeSaveDto(dto);
        LocalDateTime existingRecordedAt = existing.getRecordedAt();
        experienceRecordConverter.applySaveDto(existing, dto);
        if (existing.getRecordedAt() == null) {
            existing.setRecordedAt(existingRecordedAt == null ? LocalDateTime.now() : existingRecordedAt);
        }
        experienceRecordMapper.updateById(existing);
        replaceProjectLinks(existing.getId(), dto.getProjectNames());
        replaceAttachments(existing.getId(), dto.getAttachmentObjectKeys());
        return getById(existing.getId());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        getRequired(id);
        experienceProjectLinkMapper.delete(Wrappers.<ExperienceProjectLink>lambdaQuery()
                .eq(ExperienceProjectLink::getRecordId, id));
        experienceAttachmentMapper.delete(Wrappers.<ExperienceAttachment>lambdaQuery()
                .eq(ExperienceAttachment::getRecordId, id));
        experienceRecordMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void batchDelete(BatchDeleteDTO dto) {
        for (Long id : dto.getIds()) {
            delete(id);
        }
    }

    @Override
    public List<ExperienceRecordDetailVO> listDetailsByQuery(ExperienceRecordQueryDTO query) {
        return experienceRecordMapper.selectList(ExperienceRecordQueryBuilder.build(query)).stream()
                .map(this::toDetailVO)
                .toList();
    }

    @Override
    public ExperienceFilterOptionsVO filterOptions() {
        List<ExperienceType> types = experienceTypeMapper.selectList(Wrappers.<ExperienceType>lambdaQuery()
                .eq(ExperienceType::getStatus, 1)
                .orderByAsc(ExperienceType::getSortOrder)
                .orderByAsc(ExperienceType::getId));
        return new ExperienceFilterOptionsVO(types, experienceRecordMapper.selectDistinctRecorderNames());
    }

    @Override
    public byte[] export(ExperienceRecordQueryDTO query) throws IOException {
        return experienceRecordExportService.export(listDetailsByQuery(query));
    }

    @Override
    public byte[] exportTemplate() throws IOException {
        return experienceRecordExportService.exportTemplate();
    }

    private ExperienceRecordDetailVO createWithRecorder(ExperienceRecordSaveDTO dto, Long recorderUserId, String recorderName) {
        validateType(dto.getTypeId());
        normalizeSaveDto(dto);
        ExperienceRecord entity = experienceRecordConverter.toNewEntity(dto);
        entity.setRecorderUserId(recorderUserId);
        entity.setRecorderName(StringUtils.hasText(recorderName) ? recorderName.trim() : "未知用户");
        if (entity.getRecordedAt() == null) {
            entity.setRecordedAt(LocalDateTime.now());
        }
        experienceRecordMapper.insert(entity);
        replaceProjectLinks(entity.getId(), dto.getProjectNames());
        replaceAttachments(entity.getId(), dto.getAttachmentObjectKeys());
        return getById(entity.getId());
    }

    private ExperienceRecord getRequired(Long id) {
        ExperienceRecord record = experienceRecordMapper.selectById(id);
        if (record == null) {
            throw new ExperienceRecordNotFoundException(id);
        }
        return record;
    }

    private ExperienceType validateType(Long typeId) {
        ExperienceType type = experienceTypeMapper.selectById(typeId);
        if (type == null) {
            throw new ExperienceTypeNotFoundException(typeId);
        }
        if (type.getStatus() == null || type.getStatus() != 1) {
            throw new BusinessException("经验类型已停用");
        }
        return type;
    }

    private void normalizeSaveDto(ExperienceRecordSaveDTO dto) {
        if (!StringUtils.hasText(dto.getDescription())) {
            throw new BusinessException("描述不能为空");
        }
        dto.setDescription(dto.getDescription().trim());
        dto.setImpact(trimToNull(dto.getImpact()));
        dto.setSuggestion(trimToNull(dto.getSuggestion()));
        dto.setActionPlan(trimToNull(dto.getActionPlan()));
        dto.setProjectNames(normalizeTextList(dto.getProjectNames()));
        dto.setAttachmentObjectKeys(normalizeTextList(dto.getAttachmentObjectKeys()));
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private List<String> normalizeTextList(List<String> values) {
        if (CollectionUtils.isEmpty(values)) {
            return List.of();
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                unique.add(value.trim());
            }
        }
        return List.copyOf(unique);
    }

    private void replaceProjectLinks(Long recordId, List<String> projectNames) {
        experienceProjectLinkMapper.delete(Wrappers.<ExperienceProjectLink>lambdaQuery()
                .eq(ExperienceProjectLink::getRecordId, recordId));
        int index = 0;
        for (String projectName : projectNames) {
            ExperienceProjectLink link = new ExperienceProjectLink();
            link.setRecordId(recordId);
            link.setProjectName(projectName);
            link.setSortOrder(index++);
            experienceProjectLinkMapper.insert(link);
        }
    }

    private void replaceAttachments(Long recordId, List<String> objectKeys) {
        experienceAttachmentMapper.delete(Wrappers.<ExperienceAttachment>lambdaQuery()
                .eq(ExperienceAttachment::getRecordId, recordId));
        int index = 0;
        for (String objectKey : objectKeys) {
            fileStorageService.assertAllowedFile(objectKey);
            SysFile file = sysFileMapper.selectOne(Wrappers.<SysFile>lambdaQuery()
                    .eq(SysFile::getObjectKey, objectKey)
                    .last("LIMIT 1"));
            if (file == null) {
                throw new BusinessException("附件不存在: " + objectKey);
            }
            ExperienceAttachment attachment = new ExperienceAttachment();
            attachment.setRecordId(recordId);
            attachment.setFileId(file.getId());
            attachment.setObjectKey(file.getObjectKey());
            attachment.setOriginalName(file.getOriginalName());
            attachment.setContentType(file.getContentType());
            attachment.setSizeBytes(file.getSizeBytes());
            attachment.setSortOrder(index++);
            experienceAttachmentMapper.insert(attachment);
        }
    }

    private List<ExperienceRecordVO> toListVO(List<ExperienceRecord> records) {
        if (CollectionUtils.isEmpty(records)) {
            return List.of();
        }
        List<Long> recordIds = records.stream().map(ExperienceRecord::getId).toList();
        Map<Long, ExperienceType> typeMap = loadTypeMap(records);
        Map<Long, List<String>> projectMap = loadProjectMap(recordIds);
        Map<Long, Long> attachmentCountMap = experienceAttachmentMapper.selectList(Wrappers.<ExperienceAttachment>lambdaQuery()
                        .in(ExperienceAttachment::getRecordId, recordIds))
                .stream()
                .collect(Collectors.groupingBy(ExperienceAttachment::getRecordId, Collectors.counting()));

        return records.stream()
                .map(record -> toVO(record, typeMap.get(record.getTypeId()),
                        projectMap.getOrDefault(record.getId(), List.of()),
                        attachmentCountMap.getOrDefault(record.getId(), 0L).intValue()))
                .toList();
    }

    private ExperienceRecordDetailVO toDetailVO(ExperienceRecord record) {
        ExperienceRecordVO base = toVO(
                record,
                experienceTypeMapper.selectById(record.getTypeId()),
                loadProjectMap(List.of(record.getId())).getOrDefault(record.getId(), List.of()),
                null
        );
        List<ExperienceAttachmentVO> attachments = loadAttachments(record.getId());
        ExperienceRecordDetailVO detail = new ExperienceRecordDetailVO();
        detail.setId(base.getId());
        detail.setTypeId(base.getTypeId());
        detail.setTypeName(base.getTypeName());
        detail.setDescription(base.getDescription());
        detail.setImpact(base.getImpact());
        detail.setSuggestion(base.getSuggestion());
        detail.setActionPlan(base.getActionPlan());
        detail.setRecorderUserId(base.getRecorderUserId());
        detail.setRecorderName(base.getRecorderName());
        detail.setRecordedAt(base.getRecordedAt());
        detail.setCreatedAt(base.getCreatedAt());
        detail.setUpdatedAt(base.getUpdatedAt());
        detail.setProjectNames(base.getProjectNames());
        detail.setAttachmentCount(attachments.size());
        detail.setAttachments(attachments);
        return detail;
    }

    private ExperienceRecordVO toVO(
            ExperienceRecord record,
            ExperienceType type,
            List<String> projectNames,
            Integer attachmentCount
    ) {
        ExperienceRecordVO vo = new ExperienceRecordVO();
        vo.setId(record.getId());
        vo.setTypeId(record.getTypeId());
        vo.setTypeName(type == null ? null : type.getName());
        vo.setDescription(record.getDescription());
        vo.setImpact(record.getImpact());
        vo.setSuggestion(record.getSuggestion());
        vo.setActionPlan(record.getActionPlan());
        vo.setRecorderUserId(record.getRecorderUserId());
        vo.setRecorderName(record.getRecorderName());
        vo.setRecordedAt(record.getRecordedAt());
        vo.setCreatedAt(record.getCreatedAt());
        vo.setUpdatedAt(record.getUpdatedAt());
        vo.setProjectNames(projectNames);
        vo.setAttachmentCount(attachmentCount == null ? 0 : attachmentCount);
        return vo;
    }

    private Map<Long, ExperienceType> loadTypeMap(List<ExperienceRecord> records) {
        List<Long> typeIds = records.stream()
                .map(ExperienceRecord::getTypeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (typeIds.isEmpty()) {
            return Map.of();
        }
        return experienceTypeMapper.selectBatchIds(typeIds).stream()
                .collect(Collectors.toMap(ExperienceType::getId, Function.identity()));
    }

    private Map<Long, List<String>> loadProjectMap(List<Long> recordIds) {
        if (CollectionUtils.isEmpty(recordIds)) {
            return Map.of();
        }
        return experienceProjectLinkMapper.selectList(Wrappers.<ExperienceProjectLink>lambdaQuery()
                        .in(ExperienceProjectLink::getRecordId, recordIds)
                        .orderByAsc(ExperienceProjectLink::getSortOrder)
                        .orderByAsc(ExperienceProjectLink::getId))
                .stream()
                .collect(Collectors.groupingBy(
                        ExperienceProjectLink::getRecordId,
                        Collectors.mapping(ExperienceProjectLink::getProjectName, Collectors.toList())
                ));
    }

    private List<ExperienceAttachmentVO> loadAttachments(Long recordId) {
        return experienceAttachmentMapper.selectList(Wrappers.<ExperienceAttachment>lambdaQuery()
                        .eq(ExperienceAttachment::getRecordId, recordId)
                        .orderByAsc(ExperienceAttachment::getSortOrder)
                        .orderByAsc(ExperienceAttachment::getId))
                .stream()
                .map(this::toAttachmentVO)
                .toList();
    }

    private ExperienceAttachmentVO toAttachmentVO(ExperienceAttachment attachment) {
        boolean previewable = StringUtils.hasText(attachment.getContentType())
                && attachment.getContentType().toLowerCase(Locale.ROOT).startsWith("image/");
        ExperienceAttachmentVO vo = new ExperienceAttachmentVO();
        vo.setId(attachment.getId());
        vo.setObjectKey(attachment.getObjectKey());
        vo.setOriginalName(attachment.getOriginalName());
        vo.setContentType(attachment.getContentType());
        vo.setSizeBytes(attachment.getSizeBytes());
        vo.setPreviewable(previewable);
        vo.setUrl(previewable
                ? fileStorageService.resolveAccessUrl(attachment.getObjectKey())
                : "/api/files/download?objectKey=" + URLEncoder.encode(attachment.getObjectKey(), StandardCharsets.UTF_8));
        return vo;
    }
}
