package com.yujing.auth.mapper;

import com.yujing.auth.model.SessionRow;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface SessionMapper {

    @Insert("""
            INSERT INTO sessions (user_id, token_hash, device_id, platform, app_version,
                                  created_at, last_seen_at)
            VALUES (#{userId}, #{tokenHash}, #{deviceId}, #{platform}, #{appVersion},
                    #{createdAt}, #{lastSeenAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SessionRow row);

    @Select("SELECT * FROM sessions WHERE token_hash = #{tokenHash} AND revoked_at IS NULL LIMIT 1")
    SessionRow findActiveByTokenHash(@Param("tokenHash") String tokenHash);

    @Update("""
            UPDATE sessions
               SET revoked_at = #{now},
                   revoke_reason = #{reason}
             WHERE id = #{id}
               AND revoked_at IS NULL
            """)
    int revokeById(@Param("id") Long id,
                   @Param("now") LocalDateTime now,
                   @Param("reason") String reason);

    @Update("""
            UPDATE sessions
               SET revoked_at = #{now},
                   revoke_reason = #{reason}
             WHERE user_id = #{userId}
               AND id <> #{keepSessionId}
               AND revoked_at IS NULL
            """)
    int revokeOthers(@Param("userId") Long userId,
                     @Param("keepSessionId") Long keepSessionId,
                     @Param("now") LocalDateTime now,
                     @Param("reason") String reason);

    @Update("""
            UPDATE sessions
               SET revoked_at = #{now},
                   revoke_reason = #{reason}
             WHERE user_id = #{userId}
               AND revoked_at IS NULL
            """)
    int revokeAll(@Param("userId") Long userId,
                  @Param("now") LocalDateTime now,
                  @Param("reason") String reason);
}
