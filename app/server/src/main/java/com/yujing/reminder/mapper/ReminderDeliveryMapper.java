package com.yujing.reminder.mapper;

import com.yujing.reminder.model.ReminderDeliveryRow;
import com.yujing.security.OwnerScoped;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
@OwnerScoped
public interface ReminderDeliveryMapper {

    @Insert("""
            INSERT INTO reminder_deliveries (owner_id, reminder_id, device_id, attempted_at, result,
                                             attempt_count, version, created_at, updated_at)
            VALUES (#{ownerId}, #{reminderId}, #{deviceId}, #{now}, #{result}, 1, 0, #{now}, #{now})
            ON DUPLICATE KEY UPDATE result = VALUES(result),
                                    attempted_at = VALUES(attempted_at),
                                    attempt_count = attempt_count + 1,
                                    version = version + 1,
                                    updated_at = VALUES(updated_at)
            """)
    int upsert(@Param("ownerId") Long ownerId,
               @Param("reminderId") Long reminderId,
               @Param("deviceId") String deviceId,
               @Param("result") String result,
               @Param("now") LocalDateTime now);

    @Select("""
            SELECT * FROM reminder_deliveries
             WHERE owner_id = #{ownerId}
               AND reminder_id = #{reminderId}
               AND device_id = #{deviceId}
            """)
    ReminderDeliveryRow find(@Param("ownerId") Long ownerId,
                             @Param("reminderId") Long reminderId,
                             @Param("deviceId") String deviceId);

    @Select("""
            SELECT reminder_id FROM reminder_deliveries
             WHERE owner_id = #{ownerId}
               AND device_id = #{deviceId}
               AND result = 'sent'
            """)
    List<Long> findSentReminderIds(@Param("ownerId") Long ownerId,
                                   @Param("deviceId") String deviceId);
}
