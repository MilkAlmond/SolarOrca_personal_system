package com.yujing.sync.mapper;

import com.yujing.security.OwnerScoped;
import com.yujing.sync.model.SyncTombstoneRow;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
@OwnerScoped
public interface SyncTombstoneMapper {

    @Insert("""
            INSERT INTO sync_tombstones (owner_id, entity_type, entity_id, entity_version, deleted_at,
                                         version, created_at, updated_at)
            VALUES (#{ownerId}, #{entityType}, #{entityId}, #{entityVersion}, #{now}, 0, #{now}, #{now})
            ON DUPLICATE KEY UPDATE entity_version = VALUES(entity_version),
                                    deleted_at = VALUES(deleted_at),
                                    version = version + 1,
                                    updated_at = VALUES(updated_at)
            """)
    int upsert(@Param("ownerId") Long ownerId,
               @Param("entityType") String entityType,
               @Param("entityId") Long entityId,
               @Param("entityVersion") long entityVersion,
               @Param("now") LocalDateTime now);

    @Select("""
            SELECT * FROM sync_tombstones
             WHERE owner_id = #{ownerId}
               AND deleted_at > #{since}
             ORDER BY deleted_at, id
            """)
    List<SyncTombstoneRow> findSince(@Param("ownerId") Long ownerId,
                                     @Param("since") LocalDateTime since);

    @Select("""
            SELECT * FROM sync_tombstones
             WHERE owner_id = #{ownerId}
             ORDER BY deleted_at, id
            """)
    List<SyncTombstoneRow> findAll(@Param("ownerId") Long ownerId);
}
