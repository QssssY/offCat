package com.airesume.server.mapper;

import com.airesume.server.entity.CommunityComment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 社区评论Mapper接口
 */
@Mapper
public interface CommunityCommentMapper extends BaseMapper<CommunityComment> {

    @Select("SELECT c.post_id FROM community_comment c " +
            "INNER JOIN community_post p ON p.id = c.post_id AND p.is_deleted = 0 " +
            "WHERE c.user_id = #{userId} AND c.is_deleted = 0 " +
            "GROUP BY c.post_id ORDER BY MAX(c.create_time) DESC")
    IPage<Long> selectDistinctPostIdsByUserId(Page<?> page, @Param("userId") Long userId);

    @Select("SELECT IFNULL(SUM(cnt), 0) FROM (" +
            "SELECT COUNT(*) AS cnt FROM community_post_like " +
            "WHERE post_id IN (SELECT id FROM community_post WHERE user_id = #{userId} AND is_deleted = 0) " +
            "AND user_id != #{userId} AND create_time > #{since} " +
            "UNION ALL " +
            "SELECT COUNT(*) AS cnt FROM community_comment " +
            "WHERE post_id IN (SELECT id FROM community_post WHERE user_id = #{userId} AND is_deleted = 0) " +
            "AND user_id != #{userId} AND parent_comment_id IS NULL " +
            "AND create_time > #{since} AND is_deleted = 0 " +
            "UNION ALL " +
            "SELECT COUNT(*) AS cnt FROM community_comment " +
            "WHERE parent_comment_id IN (SELECT id FROM community_comment WHERE user_id = #{userId} AND is_deleted = 0) " +
            "AND user_id != #{userId} AND create_time > #{since} AND is_deleted = 0 " +
            "UNION ALL " +
            "SELECT COUNT(*) AS cnt FROM community_post_favorite " +
            "WHERE post_id IN (SELECT id FROM community_post WHERE user_id = #{userId} AND is_deleted = 0) " +
            "AND user_id != #{userId} AND create_time > #{since}" +
            ") t")
    int countUnreadInteractions(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Select("<script>" +
            "SELECT parent_comment_id, COUNT(*) AS cnt FROM community_comment " +
            "WHERE parent_comment_id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            " AND is_deleted = 0 " +
            "GROUP BY parent_comment_id" +
            "</script>")
    List<Map<String, Object>> countByParentIds(@Param("ids") java.util.Collection<Long> ids);
}
