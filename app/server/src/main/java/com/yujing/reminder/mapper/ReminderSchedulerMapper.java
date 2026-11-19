package com.yujing.reminder.mapper;

import com.yujing.reminder.model.OwnerZoneRow;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Cross-owner statements for the scheduled maintenance job. These are not
 * owner-scoped because the job deliberately walks every account; the API never
 * calls them.
 */
@Mapper
public interface ReminderSchedulerMapper {

    @Select("SELECT owner_id, timezone_offset_minutes FROM user_settings")
    List<OwnerZoneRow> listOwnerZones();

    @Select("""
            SELECT id FROM reminders
             WHERE delivery_status = 'pending'
               AND trigger_at < #{threshold}
            """)
    List<Long> findMissedIds(@Param("threshold") LocalDateTime threshold);

    @Update("""
            UPDATE reminders
               SET delivery_status = 'failed',
                   failed_at = #{now},
                   version = version + 1,
                   updated_at = #{now}
             WHERE id = #{id}
               AND delivery_status = 'pending'
            """)
    int markMissed(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Delete("DELETE FROM sync_tombstones WHERE deleted_at < #{cutoff}")
    int deleteExpiredTombstones(@Param("cutoff") LocalDateTime cutoff);
}
