package com.storage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.storage.entity.MaterialIoRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface MaterialIoRecordMapper extends BaseMapper<MaterialIoRecord> {

    @Select("SELECT * FROM material_io_record WHERE id = #{id} FOR UPDATE")
    MaterialIoRecord selectByIdForUpdate(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM material_io_record WHERE material_ledger_id = #{ledgerId}")
    long countByMaterialLedgerId(@Param("ledgerId") Long ledgerId);
}
