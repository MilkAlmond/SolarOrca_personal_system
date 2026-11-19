package com.yujing.export.mapper;

import com.yujing.export.model.ExportJobRow;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Export jobs are owner-scoped for user-facing calls. The worker needs
 * cross-owner scans, so the interface itself is not {@code @OwnerScoped}; every
 * owner-facing statement still filters on {@code owner_id}.
 */
@Mapper
public interface ExportJobMapper {

    @Insert("""
            INSERT INTO export_jobs (owner_id, status, created_at)
            VALUES (#{ownerId}, #{status}, #{createdAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ExportJobRow row);

    @Select("SELECT * FROM export_jobs WHERE owner_id = #{ownerId} AND id = #{id}")
    ExportJobRow findById(@Param("ownerId") Long ownerId, @Param("id") Long id);

    @Select("SELECT * FROM export_jobs WHERE id = #{id}")
    ExportJobRow findByIdInternal(@Param("id") Long id);

    @Select("SELECT * FROM export_jobs WHERE owner_id = #{ownerId} ORDER BY id DESC LIMIT 1")
    ExportJobRow findLatest(@Param("ownerId") Long ownerId);

    @Select("""
            SELECT MAX(created_at) FROM export_jobs
             WHERE owner_id = #{ownerId} AND status <> 'failed'
            """)
    LocalDateTime findLastCreatedAt(@Param("ownerId") Long ownerId);

    @Update("""
            UPDATE export_jobs
               SET status = 'ready',
                   file_id = #{fileId},
                   expires_at = #{expiresAt},
                   completed_at = #{completedAt}
             WHERE owner_id = #{ownerId}
               AND id = #{id}
            """)
    int markReady(@Param("ownerId") Long ownerId,
                  @Param("id") Long id,
                  @Param("fileId") Long fileId,
                  @Param("expiresAt") LocalDateTime expiresAt,
                  @Param("completedAt") LocalDateTime completedAt);

    @Update("""
            UPDATE export_jobs
               SET status = 'failed',
                   completed_at = #{now}
             WHERE owner_id = #{ownerId}
               AND id = #{id}
            """)
    int markFailed(@Param("ownerId") Long ownerId, @Param("id") Long id,
                   @Param("now") LocalDateTime now);

    @Select("SELECT id FROM export_jobs WHERE status = 'pending' ORDER BY id LIMIT #{limit}")
    List<Long> findPendingIds(@Param("limit") int limit);

    @Select("""
            SELECT id FROM export_jobs
             WHERE status = 'ready'
               AND expires_at IS NOT NULL
               AND expires_at <= #{now}
             ORDER BY id
             LIMIT #{limit}
            """)
    List<Long> findExpiredIds(@Param("now") LocalDateTime now, @Param("limit") int limit);

    @Update("UPDATE export_jobs SET status = 'expired' WHERE id = #{id} AND status = 'ready'")
    int markExpired(@Param("id") Long id);
}
