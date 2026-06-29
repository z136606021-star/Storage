package com.storage.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.storage.dto.SysCustomerQueryDTO;
import com.storage.entity.SysCustomer;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

public final class SysCustomerQueryBuilder {

    private SysCustomerQueryBuilder() {
    }

    public static LambdaQueryWrapper<SysCustomer> build(SysCustomerQueryDTO query) {
        LambdaQueryWrapper<SysCustomer> wrapper = Wrappers.lambdaQuery();

        if (!CollectionUtils.isEmpty(query.getIds())) {
            wrapper.in(SysCustomer::getId, query.getIds());
        }

        if (StringUtils.hasText(query.getCustomerCode())) {
            wrapper.like(SysCustomer::getCustomerCode, query.getCustomerCode().trim());
        }

        if (StringUtils.hasText(query.getName())) {
            wrapper.like(SysCustomer::getName, query.getName().trim());
        }

        if (StringUtils.hasText(query.getContactName())) {
            wrapper.like(SysCustomer::getContactName, query.getContactName().trim());
        }

        wrapper.orderByAsc(SysCustomer::getCustomerCode);
        return wrapper;
    }
}
