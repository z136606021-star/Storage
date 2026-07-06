package com.storage.infrastructure.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.storage.infrastructure.file.entity.SysFile;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysFileMapper extends BaseMapper<SysFile> {
}
