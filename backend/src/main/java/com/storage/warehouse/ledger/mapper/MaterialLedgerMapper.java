package com.storage.warehouse.ledger.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.storage.warehouse.ledger.entity.MaterialLedger;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface MaterialLedgerMapper extends BaseMapper<MaterialLedger> {

    @Select("SELECT * FROM material_ledger WHERE id = #{id} FOR UPDATE")
    MaterialLedger selectByIdForUpdate(@Param("id") Long id);

    @Select("SELECT DISTINCT category FROM material_ledger WHERE category IS NOT NULL AND category <> '' ORDER BY category")
    List<String> selectDistinctCategories();

    @Select("SELECT DISTINCT generic_name FROM material_ledger WHERE generic_name IS NOT NULL AND generic_name <> '' ORDER BY generic_name")
    List<String> selectDistinctGenericNames();

    @Select("SELECT DISTINCT brand FROM material_ledger WHERE brand IS NOT NULL AND brand <> '' ORDER BY brand")
    List<String> selectDistinctBrands();

    @Select("SELECT DISTINCT model FROM material_ledger WHERE model IS NOT NULL AND model <> '' ORDER BY model")
    List<String> selectDistinctModels();

    @Select("SELECT DISTINCT bin_location FROM material_ledger WHERE bin_location IS NOT NULL AND bin_location <> '' ORDER BY bin_location")
    List<String> selectDistinctBinLocations();
}
