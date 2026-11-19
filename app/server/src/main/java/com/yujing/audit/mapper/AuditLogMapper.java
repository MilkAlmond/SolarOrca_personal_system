package com.yujing.audit.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

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
}
