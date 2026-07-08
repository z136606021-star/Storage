package com.storage.warehouse.mapper;

import com.storage.warehouse.dto.MaterialIoAggregateVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface WarehouseStatsMapper {

    @Select("SELECT COALESCE(SUM(stock_quantity), 0) FROM material_ledger")
    Long sumStockQuantity();

    @Select("""
            SELECT COUNT(*)
            FROM material_ledger ml
            INNER JOIN safety_stock ss ON ss.material_ledger_id = ml.id
            WHERE ss.warning_enabled = 1 AND ml.stock_quantity < ss.safety_quantity
            """)
    Long countWarningMaterials();

    @Select("""
            SELECT io_type AS ioType, COUNT(*) AS recordCount, COALESCE(SUM(quantity), 0) AS quantitySum
            FROM material_io_record
            WHERE operated_at >= #{since}
            GROUP BY io_type
            """)
    List<MaterialIoAggregateVO> summarizeIoSince(@Param("since") LocalDateTime since);
}
