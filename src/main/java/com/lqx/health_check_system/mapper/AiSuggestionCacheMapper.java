package com.lqx.health_check_system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lqx.health_check_system.entity.AiSuggestionCache;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AiSuggestionCacheMapper extends BaseMapper<AiSuggestionCache> {

    @Select("SELECT * FROM ai_suggestion_cache WHERE exam_id = #{examId} AND data_hash = #{dataHash} AND expires_at > NOW() LIMIT 1")
    AiSuggestionCache selectValidCache(@Param("examId") Long examId, @Param("dataHash") String dataHash);

    @Select("DELETE FROM ai_suggestion_cache WHERE expires_at < NOW()")
    void cleanExpiredCache();
}