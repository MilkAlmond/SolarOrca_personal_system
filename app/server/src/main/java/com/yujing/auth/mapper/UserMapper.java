package com.yujing.auth.mapper;

import com.yujing.auth.model.UserRow;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM users WHERE username = #{username} LIMIT 1")
    UserRow findByUsername(@Param("username") String username);

    @Select("SELECT * FROM users WHERE username = #{username} LIMIT 1 FOR UPDATE")
    UserRow findByUsernameForUpdate(@Param("username") String username);

    @Select("SELECT * FROM users WHERE id = #{id}")
    UserRow findById(@Param("id") Long id);

    @Insert("""
            INSERT INTO users (username, password_hash, display_name, role, status,
                               failed_login_count, created_at, updated_at)
            VALUES (#{username}, #{passwordHash}, #{displayName}, #{role}, #{status},
                    0, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserRow row);

    @Update("""
            UPDATE users
               SET failed_login_count = 0,
                   locked_until = NULL,
                   last_login_at = #{now},
                   version = version + 1,
                   updated_at = #{now}
             WHERE id = #{id}
            """)
    int markLoginSuccess(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Update("""
            UPDATE users
               SET failed_login_count = #{count},
                   locked_until = #{lockedUntil},
                   version = version + 1,
                   updated_at = #{now}
             WHERE id = #{id}
            """)
    int markLoginFailure(@Param("id") Long id,
                         @Param("count") int count,
                         @Param("lockedUntil") LocalDateTime lockedUntil,
                         @Param("now") LocalDateTime now);

    @Update("""
            UPDATE users
               SET failed_login_count = 0,
                   locked_until = NULL,
                   updated_at = #{now}
             WHERE id = #{id}
            """)
    int resetLock(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Update("""
            UPDATE users
               SET password_hash = #{passwordHash},
                   version = version + 1,
                   updated_at = #{now}
             WHERE id = #{id}
            """)
    int updatePassword(@Param("id") Long id,
                       @Param("passwordHash") String passwordHash,
                       @Param("now") LocalDateTime now);

    @Update("""
            UPDATE users
               SET display_name = #{displayName},
                   version = version + 1,
                   updated_at = #{now}
             WHERE id = #{id}
            """)
    int updateDisplayName(@Param("id") Long id,
                          @Param("displayName") String displayName,
                          @Param("now") LocalDateTime now);

    @Update("""
            UPDATE users
               SET deletion_requested_at = #{requestedAt},
                   deletion_scheduled_at = #{scheduledAt},
                   version = version + 1,
                   updated_at = #{now}
             WHERE id = #{id}
            """)
    int updateDeletion(@Param("id") Long id,
                       @Param("requestedAt") LocalDateTime requestedAt,
                       @Param("scheduledAt") LocalDateTime scheduledAt,
                       @Param("now") LocalDateTime now);

    @Update("""
            UPDATE users
               SET deletion_requested_at = NULL,
                   deletion_scheduled_at = NULL,
                   version = version + 1,
                   updated_at = #{now}
             WHERE id = #{id}
            """)
    int clearDeletion(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Select("""
            SELECT * FROM users
             WHERE deletion_scheduled_at IS NOT NULL
               AND deletion_scheduled_at <= #{now}
             ORDER BY deletion_scheduled_at
             LIMIT #{limit}
            """)
    List<UserRow> findDeletionDue(@Param("now") LocalDateTime now, @Param("limit") int limit);

    @Delete("DELETE FROM users WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
