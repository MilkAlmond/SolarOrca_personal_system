package com.yujing.audit.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AuditLogMapper {

    @Insert("""
            INSERT INTO audit_logs (actor_user_id, action, target, result, ip, detail, created_at)
            VALUES (#{actorUserId}, #{action}, #{target}, #{result}, #{ip}, #{detail}, #{createdAt})
            """)
    int insert(@Param("actorUserId") Long actorUserId,
               @Param("action") String action,
               @Param("target") String target,
               @Param("result") String result,
               @Param("ip") String ip,
               @Param("detail") String detail,
               @Param("createdAt") LocalDateTime createdAt);

    @Select("""
            SELECT * FROM audit_logs
             WHERE (#{action,jdbcType=VARCHAR} IS NULL OR action = #{action,jdbcType=VARCHAR})
               AND (#{result,jdbcType=VARCHAR} IS NULL OR result = #{result,jdbcType=VARCHAR})
               AND (#{keyword,jdbcType=VARCHAR} IS NULL
                    OR target LIKE CONCAT('%', #{keyword,jdbcType=VARCHAR}, '%')
                    OR detail LIKE CONCAT('%', #{keyword,jdbcType=VARCHAR}, '%'))
             ORDER BY id DESC
             LIMIT #{limit} OFFSET #{offset}
            """)
    List<com.yujing.audit.model.AuditLogRow> findPage(@Param("action") String action,
                                                      @Param("result") String result,
                                                      @Param("keyword") String keyword,
                                                      @Param("limit") int limit,
                                                      @Param("offset") long offset);

    @Select("""
            SELECT COUNT(*) FROM audit_logs
             WHERE (#{action,jdbcType=VARCHAR} IS NULL OR action = #{action,jdbcType=VARCHAR})
               AND (#{result,jdbcType=VARCHAR} IS NULL OR result = #{result,jdbcType=VARCHAR})
               AND (#{keyword,jdbcType=VARCHAR} IS NULL
                    OR target LIKE CONCAT('%', #{keyword,jdbcType=VARCHAR}, '%')
                    OR detail LIKE CONCAT('%', #{keyword,jdbcType=VARCHAR}, '%'))
            """)
    long count(@Param("action") String action,
               @Param("result") String result,
               @Param("keyword") String keyword);
}
