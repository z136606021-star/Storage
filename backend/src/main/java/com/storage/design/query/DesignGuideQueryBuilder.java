package com.storage.design.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.storage.design.dto.DesignGuideQueryDTO;
import com.storage.design.entity.DesignGuide;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

public final class DesignGuideQueryBuilder {

    private DesignGuideQueryBuilder() {
    }

    public static LambdaQueryWrapper<DesignGuide> build(DesignGuideQueryDTO query) {
        LambdaQueryWrapper<DesignGuide> wrapper = Wrappers.lambdaQuery();
        if (!CollectionUtils.isEmpty(query.getIds())) {
            wrapper.in(DesignGuide::getId, query.getIds());
        }
        if (query.getProductTypeId() != null) {
            wrapper.eq(DesignGuide::getProductTypeId, query.getProductTypeId());
        }
        if (query.getStageId() != null) {
            wrapper.eq(DesignGuide::getStageId, query.getStageId());
        }
        if (StringUtils.hasText(query.getScope())) {
            wrapper.eq(DesignGuide::getScope, query.getScope().trim());
        }
        if (StringUtils.hasText(query.getCheckItem())) {
            wrapper.like(DesignGuide::getCheckItem, query.getCheckItem().trim());
        }
        wrapper.orderByDesc(DesignGuide::getRecordedAt).orderByDesc(DesignGuide::getId);
        return wrapper;
    }
}
