package com.storage.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.storage.dto.FilterLinkageQueryDTO;
import com.storage.dto.MaterialQueryDTO;
import com.storage.entity.MaterialLedger;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

public final class MaterialLedgerQueryBuilder {

  private static final String ALL = "全部";

  private MaterialLedgerQueryBuilder() {
  }

  public static LambdaQueryWrapper<MaterialLedger> build(MaterialQueryDTO query) {
    LambdaQueryWrapper<MaterialLedger> wrapper = Wrappers.lambdaQuery();

    if (!CollectionUtils.isEmpty(query.getIds())) {
      wrapper.in(MaterialLedger::getId, query.getIds());
    }

    if (isFilterValue(query.getCategory())) {
      wrapper.eq(MaterialLedger::getCategory, query.getCategory());
    }
    if (isFilterValue(query.getGenericName())) {
      wrapper.eq(MaterialLedger::getGenericName, query.getGenericName());
    }
    if (isFilterValue(query.getBrand())) {
      wrapper.eq(MaterialLedger::getBrand, query.getBrand());
    }
    if (StringUtils.hasText(query.getName())) {
      wrapper.like(MaterialLedger::getName, query.getName().trim());
    }
    if (isFilterValue(query.getModel())) {
      wrapper.eq(MaterialLedger::getModel, query.getModel());
    }
    if (isFilterValue(query.getBinLocation())) {
      wrapper.eq(MaterialLedger::getBinLocation, query.getBinLocation());
    }

    wrapper.orderByAsc(MaterialLedger::getId);
    return wrapper;
  }

  public static LambdaQueryWrapper<MaterialLedger> withCategory(FilterLinkageQueryDTO query) {
    LambdaQueryWrapper<MaterialLedger> wrapper = Wrappers.lambdaQuery();
    if (isFilterValue(query.getCategory())) {
      wrapper.eq(MaterialLedger::getCategory, query.getCategory());
    }
    return wrapper;
  }

  public static LambdaQueryWrapper<MaterialLedger> withCategoryAndGeneric(FilterLinkageQueryDTO query) {
    LambdaQueryWrapper<MaterialLedger> wrapper = withCategory(query);
    if (isFilterValue(query.getGenericName())) {
      wrapper.eq(MaterialLedger::getGenericName, query.getGenericName());
    }
    return wrapper;
  }

  public static LambdaQueryWrapper<MaterialLedger> withCategoryGenericAndBrand(FilterLinkageQueryDTO query) {
    LambdaQueryWrapper<MaterialLedger> wrapper = withCategoryAndGeneric(query);
    if (isFilterValue(query.getBrand())) {
      wrapper.eq(MaterialLedger::getBrand, query.getBrand());
    }
    return wrapper;
  }

  public static boolean isFilterValue(String value) {
    return StringUtils.hasText(value) && !ALL.equals(value);
  }
}
