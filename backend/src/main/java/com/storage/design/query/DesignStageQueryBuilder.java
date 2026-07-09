package com.storage.design.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.storage.design.dto.DesignStageQueryDTO;
import com.storage.design.entity.DesignStage;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

public final class DesignStageQueryBuilder {

    private DesignStageQueryBuilder() {
    }

    public static LambdaQueryWrapper<DesignStage> build(DesignStageQueryDTO query) {
        LambdaQueryWrapper<DesignStage> wrapper = Wrappers.lambdaQuery();
        if (!CollectionUtils.isEmpty(query.getIds())) {
            wrapper.in(DesignStage::getId, query.getIds());
        }
        if (StringUtils.hasText(query.getStageName())) {
            wrapper.like(DesignStage::getStageName, query.getStageName().trim());
        }
        if (query.getEnabled() != null) {
            wrapper.eq(DesignStage::getEnabled, query.getEnabled());
        }
        wrapper.orderByAsc(DesignStage::getSortOrder).orderByAsc(DesignStage::getId);
        return wrapper;
    }
}
