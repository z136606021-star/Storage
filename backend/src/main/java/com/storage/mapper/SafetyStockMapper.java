package com.storage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.storage.dto.SafetyStockQueryDTO;
import com.storage.dto.SafetyStockRecordVO;
import com.storage.entity.SafetyStock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;

import java.util.List;

@Mapper
public interface SafetyStockMapper extends BaseMapper<SafetyStock> {

    @SelectProvider(type = SafetyStockSqlProvider.class, method = "selectJoinedPage")
    IPage<SafetyStockRecordVO> selectJoinedPage(
            Page<SafetyStockRecordVO> page,
            @Param("query") SafetyStockQueryDTO query
    );

    @SelectProvider(type = SafetyStockSqlProvider.class, method = "selectJoinedList")
    List<SafetyStockRecordVO> selectJoinedList(@Param("query") SafetyStockQueryDTO query);
}
