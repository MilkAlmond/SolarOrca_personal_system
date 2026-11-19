package com.yujing.device.mapper;

import com.yujing.device.model.DeviceRow;
import com.yujing.security.OwnerScoped;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
@OwnerScoped
public interface DeviceMapper {

    @Select("SELECT * FROM devices WHERE owner_id = #{ownerId} ORDER BY last_seen_at DESC, id")
    List<DeviceRow> findAll(@Param("ownerId") Long ownerId);

    @Select("SELECT * FROM devices WHERE owner_id = #{ownerId} AND device_id = #{deviceId}")
    DeviceRow findByDeviceId(@Param("ownerId") Long ownerId, @Param("deviceId") String deviceId);

    /**
     * Idempotent registration keyed by (owner, device). The notification toggle
     * is only set on first registration so an app launch never re-enables it.
     */
    @Insert("""
            INSERT INTO devices (owner_id, device_id, platform, app_version, notifications_enabled,
                                 last_seen_at, version, created_at, updated_at)
            VALUES (#{ownerId}, #{deviceId}, #{platform}, #{appVersion}, #{notificationsEnabled},
                    #{now}, 0, #{now}, #{now})
            ON DUPLICATE KEY UPDATE
                platform = VALUES(platform),
                app_version = VALUES(app_version),
                last_seen_at = VALUES(last_seen_at),
                updated_at = VALUES(updated_at),
                version = version + 1
            """)
    int upsert(@Param("ownerId") Long ownerId,
               @Param("deviceId") String deviceId,
               @Param("platform") String platform,
               @Param("appVersion") String appVersion,
               @Param("notificationsEnabled") boolean notificationsEnabled,
               @Param("now") LocalDateTime now);

    @Update("""
            UPDATE devices
               SET notifications_enabled = #{enabled},
                   version = version + 1,
                   updated_at = #{now}
             WHERE owner_id = #{ownerId}
               AND device_id = #{deviceId}
               AND version = #{expectedVersion}
            """)
    int updateNotifications(@Param("ownerId") Long ownerId,
                            @Param("deviceId") String deviceId,
                            @Param("enabled") boolean enabled,
                            @Param("expectedVersion") long expectedVersion,
                            @Param("now") LocalDateTime now);
}
