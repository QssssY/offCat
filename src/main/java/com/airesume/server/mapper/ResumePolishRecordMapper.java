package com.airesume.server.mapper;

import com.airesume.server.entity.ResumePolishRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * AI 简历润色记录 Mapper。
 */
@Mapper
public interface ResumePolishRecordMapper extends BaseMapper<ResumePolishRecord> {

    /**
     * 逻辑删除当前用户的 AI 简历润色记录。
     */
    @Update("""
            UPDATE resume_polish_record
            SET is_deleted = 1,
                update_time = NOW()
            WHERE user_id = #{userId}
              AND is_deleted = 0
            """)
    int logicalDeleteByUserId(@Param("userId") Long userId);
}
