package com.storage.warehouse.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.storage.warehouse.dto.MaterialQueryDTO;
import com.storage.warehouse.entity.MaterialLedger;
import com.storage.warehouse.dto.FilterLinkageQueryDTO;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

public final class MaterialLedgerQueryBuilder {

  public static final String STOCK_STATUS_IN_STOCK = "IN_STOCK";

  public static final String STOCK_STATUS_ZERO_STOCK = "ZERO_STOCK";

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

    applyStockStatus(wrapper, query.getStockStatus());

    wrapper.orderByAsc(MaterialLedger::getId);
    return wrapper;
  }

  public static void applyStockStatus(LambdaQueryWrapper<MaterialLedger> wrapper, String stockStatus) {
    if (!StringUtils.hasText(stockStatus)) {
      return;
    }
    String normalized = stockStatus.trim().toUpperCase();
    if (STOCK_STATUS_IN_STOCK.equals(normalized)) {
      wrapper.gt(MaterialLedger::getStockQuantity, 0);
      return;
    }
    if (STOCK_STATUS_ZERO_STOCK.equals(normalized)) {
      wrapper.le(MaterialLedger::getStockQuantity, 0);
      return;
    }
    throw new IllegalArgumentException(
        "库存状态无效，仅支持 IN_STOCK、ZERO_STOCK"
    );
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
    return StringUtils.hasText(value);
  }

  public static LambdaQueryWrapper<MaterialLedger> byNaturalKey(
      String category,
      String genericName,
      String brand,
      String name,
      String model,
      String binLocation
  ) {
    LambdaQueryWrapper<MaterialLedger> wrapper = Wrappers.lambdaQuery();
    wrapper.eq(MaterialLedger::getCategory, category.trim())
        .eq(MaterialLedger::getGenericName, genericName.trim())
        .eq(MaterialLedger::getName, name.trim())
        .eq(MaterialLedger::getModel, model.trim())
        .eq(MaterialLedger::getBinLocation, binLocation.trim());
    if (StringUtils.hasText(brand)) {
      wrapper.eq(MaterialLedger::getBrand, brand.trim());
    } else {
      wrapper.and(w -> w.isNull(MaterialLedger::getBrand).or().eq(MaterialLedger::getBrand, ""));
    }
    return wrapper;
  }
}
