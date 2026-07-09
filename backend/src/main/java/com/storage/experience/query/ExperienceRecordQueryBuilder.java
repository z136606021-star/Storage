package com.storage.experience.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.storage.experience.dto.ExperienceRecordQueryDTO;
import com.storage.experience.entity.ExperienceRecord;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

public final class ExperienceRecordQueryBuilder {

    private ExperienceRecordQueryBuilder() {
    }

    public static LambdaQueryWrapper<ExperienceRecord> build(ExperienceRecordQueryDTO query) {
        LambdaQueryWrapper<ExperienceRecord> wrapper = Wrappers.lambdaQuery();
        if (query == null) {
            return wrapper.orderByDesc(ExperienceRecord::getRecordedAt);
        }
        if (!CollectionUtils.isEmpty(query.getIds())) {
            wrapper.in(ExperienceRecord::getId, query.getIds());
        }
        if (query.getTypeId() != null) {
            wrapper.eq(ExperienceRecord::getTypeId, query.getTypeId());
        }
        if (StringUtils.hasText(query.getRecorderName())) {
            wrapper.eq(ExperienceRecord::getRecorderName, query.getRecorderName().trim());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(nested -> nested
                    .like(ExperienceRecord::getDescription, keyword)
                    .or()
                    .like(ExperienceRecord::getImpact, keyword)
                    .or()
                    .like(ExperienceRecord::getSuggestion, keyword)
                    .or()
                    .like(ExperienceRecord::getActionPlan, keyword));
        }
        if (query.getRecordedStart() != null) {
            wrapper.ge(ExperienceRecord::getRecordedAt, query.getRecordedStart().atStartOfDay());
        }
        if (query.getRecordedEnd() != null) {
            wrapper.lt(ExperienceRecord::getRecordedAt, query.getRecordedEnd().plusDays(1).atStartOfDay());
        }
        return wrapper.orderByDesc(ExperienceRecord::getRecordedAt)
                .orderByDesc(ExperienceRecord::getId);
    }
}
