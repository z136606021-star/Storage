package com.storage.warehouse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.storage.warehouse.entity.MaterialLedger;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface MaterialLedgerMapper extends BaseMapper<MaterialLedger> {

    @Select("SELECT * FROM material_ledger WHERE id = #{id} FOR UPDATE")
    MaterialLedger selectByIdForUpdate(@Param("id") Long id);
}
