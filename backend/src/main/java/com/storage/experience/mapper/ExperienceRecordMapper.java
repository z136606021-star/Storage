package com.storage.experience.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.storage.experience.entity.ExperienceRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ExperienceRecordMapper extends BaseMapper<ExperienceRecord> {

    @Select("""
            SELECT DISTINCT recorder_name
            FROM experience_record
            WHERE recorder_name IS NOT NULL AND recorder_name <> ''
            ORDER BY recorder_name ASC
            """)
    List<String> selectDistinctRecorderNames();
}
