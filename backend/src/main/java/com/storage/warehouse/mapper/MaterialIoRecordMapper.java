package com.storage.warehouse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.storage.warehouse.entity.MaterialIoRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface MaterialIoRecordMapper extends BaseMapper<MaterialIoRecord> {

    @Select("SELECT * FROM material_io_record WHERE id = #{id} FOR UPDATE")
    MaterialIoRecord selectByIdForUpdate(@Param("id") Long id);

    @Select("SELECT * FROM material_io_record ORDER BY id FOR UPDATE")
    List<MaterialIoRecord> selectAllForUpdate();
}
