package com.yujing.reminder.mapper;

import com.yujing.reminder.model.ReminderRow;
import com.yujing.security.OwnerScoped;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
@OwnerScoped
public interface ReminderMapper {

    @Select("SELECT * FROM reminders WHERE owner_id = #{ownerId} ORDER BY trigger_at, id")
    List<ReminderRow> findByOwner(@Param("ownerId") Long ownerId);

    @Select("SELECT * FROM reminders WHERE owner_id = #{ownerId} AND id = #{id}")
    ReminderRow findById(@Param("ownerId") Long ownerId, @Param("id") Long id);

    @Select("""
            SELECT * FROM reminders
             WHERE owner_id = #{ownerId}
               AND source = #{source}
               AND source_id = #{sourceId}
             ORDER BY trigger_at, id
            """)
    List<ReminderRow> findBySource(@Param("ownerId") Long ownerId,
                                   @Param("source") String source,
                                   @Param("sourceId") Long sourceId);

    @Select("SELECT * FROM reminders WHERE owner_id = #{ownerId} AND dedupe_key = #{dedupeKey}")
    ReminderRow findByDedupe(@Param("ownerId") Long ownerId, @Param("dedupeKey") String dedupeKey);

    @Select("""
            SELECT * FROM reminders
             WHERE owner_id = #{ownerId}
               AND enabled = TRUE
               AND delivery_status = 'pending'
               AND trigger_at <= #{now}
             ORDER BY trigger_at, id
            """)
    List<ReminderRow> findDue(@Param("ownerId") Long ownerId, @Param("now") LocalDateTime now);

    @Insert("""
            INSERT IGNORE INTO reminders (owner_id, source, source_id, title, trigger_at, advance_minutes,
                                   enabled, delivery_status, dedupe_key, version, created_at, updated_at)
            VALUES (#{ownerId}, #{source}, #{sourceId,jdbcType=BIGINT}, #{title}, #{triggerAt},
                    #{advanceMinutes}, #{enabled}, #{deliveryStatus}, #{dedupeKey}, 0, #{now}, #{now})
            """)
    int insert(@Param("ownerId") Long ownerId,
               @Param("source") String source,
               @Param("sourceId") Long sourceId,
               @Param("title") String title,
               @Param("triggerAt") LocalDateTime triggerAt,
               @Param("advanceMinutes") int advanceMinutes,
               @Param("enabled") boolean enabled,
               @Param("deliveryStatus") String deliveryStatus,
               @Param("dedupeKey") String dedupeKey,
               @Param("now") LocalDateTime now);

    @Update("""
            UPDATE reminders
               SET title = #{title},
                   trigger_at = #{triggerAt},
                   advance_minutes = #{advanceMinutes},
                   version = version + 1,
                   updated_at = #{now}
             WHERE owner_id = #{ownerId}
               AND id = #{id}
               AND delivery_status = 'pending'
               AND (title <> #{title}
                    OR trigger_at <> #{triggerAt}
                    OR advance_minutes <> #{advanceMinutes})
            """)
    int updateGenerated(@Param("ownerId") Long ownerId,
                        @Param("id") Long id,
                        @Param("title") String title,
                        @Param("triggerAt") LocalDateTime triggerAt,
                        @Param("advanceMinutes") int advanceMinutes,
                        @Param("now") LocalDateTime now);

    @Update("""
            UPDATE reminders
               SET enabled = #{enabled},
                   version = version + 1,
                   updated_at = #{now}
             WHERE owner_id = #{ownerId}
               AND id = #{id}
               AND version = #{expectedVersion}
            """)
    int updateEnabled(@Param("ownerId") Long ownerId,
                      @Param("id") Long id,
                      @Param("enabled") boolean enabled,
                      @Param("expectedVersion") long expectedVersion,
                      @Param("now") LocalDateTime now);

    @Update("""
            UPDATE reminders
               SET delivery_status = 'sent',
                   sent_at = #{now},
                   version = version + 1,
                   updated_at = #{now}
             WHERE owner_id = #{ownerId}
               AND id = #{id}
               AND delivery_status <> 'sent'
            """)
    int markSent(@Param("ownerId") Long ownerId, @Param("id") Long id, @Param("now") LocalDateTime now);

    @Update("""
            UPDATE reminders
               SET delivery_status = 'failed',
                   failed_at = #{now},
                   version = version + 1,
                   updated_at = #{now}
             WHERE owner_id = #{ownerId}
               AND id = #{id}
               AND delivery_status <> 'sent'
            """)
    int markFailed(@Param("ownerId") Long ownerId, @Param("id") Long id, @Param("now") LocalDateTime now);

    @Delete("""
            DELETE FROM reminders
             WHERE owner_id = #{ownerId}
               AND id = #{id}
               AND version = #{expectedVersion}
            """)
    int deleteById(@Param("ownerId") Long ownerId,
                   @Param("id") Long id,
                   @Param("expectedVersion") long expectedVersion);

    @Delete("DELETE FROM reminders WHERE owner_id = #{ownerId} AND id = #{id}")
    int deleteByIdInternal(@Param("ownerId") Long ownerId, @Param("id") Long id);

    @Delete("""
            DELETE FROM reminders
             WHERE owner_id = #{ownerId}
               AND source = #{source}
               AND source_id = #{sourceId}
               AND delivery_status = 'pending'
            """)
    int deletePendingBySource(@Param("ownerId") Long ownerId,
                              @Param("source") String source,
                              @Param("sourceId") Long sourceId);

    @Delete("""
            DELETE FROM reminders
             WHERE owner_id = #{ownerId}
               AND source = #{source}
               AND source_id = #{sourceId}
               AND delivery_status <> 'sent'
            """)
    int deleteUnsentBySource(@Param("ownerId") Long ownerId,
                             @Param("source") String source,
                             @Param("sourceId") Long sourceId);

}
