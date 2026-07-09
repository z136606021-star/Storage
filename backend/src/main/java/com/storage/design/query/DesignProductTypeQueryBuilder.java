package com.storage.design.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.storage.design.dto.DesignProductTypeQueryDTO;
import com.storage.design.entity.DesignProductType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

public final class DesignProductTypeQueryBuilder {

    private DesignProductTypeQueryBuilder() {
    }

    public static LambdaQueryWrapper<DesignProductType> build(DesignProductTypeQueryDTO query) {
        LambdaQueryWrapper<DesignProductType> wrapper = Wrappers.lambdaQuery();
        if (!CollectionUtils.isEmpty(query.getIds())) {
            wrapper.in(DesignProductType::getId, query.getIds());
        }
        if (StringUtils.hasText(query.getTypeCode())) {
            wrapper.like(DesignProductType::getTypeCode, query.getTypeCode().trim());
        }
        if (StringUtils.hasText(query.getTypeName())) {
            wrapper.like(DesignProductType::getTypeName, query.getTypeName().trim());
        }
        if (query.getEnabled() != null) {
            wrapper.eq(DesignProductType::getEnabled, query.getEnabled());
        }
        wrapper.orderByAsc(DesignProductType::getTypeCode).orderByAsc(DesignProductType::getId);
        return wrapper;
    }
}
