package com.yujing.settings.mapper;

import com.yujing.settings.model.UserSettingsRow;
import com.yujing.security.OwnerScoped;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
@OwnerScoped
public interface UserSettingsMapper {

    @Select("SELECT * FROM user_settings WHERE owner_id = #{ownerId} LIMIT 1")
    UserSettingsRow findByOwner(@Param("ownerId") Long ownerId);

    @Insert("""
            INSERT IGNORE INTO user_settings (owner_id, reminders_enabled, version, created_at, updated_at)
            VALUES (#{ownerId}, TRUE, 0, #{now}, #{now})
            """)
    int insertDefault(@Param("ownerId") Long ownerId, @Param("now") LocalDateTime now);

    @Update("""
            UPDATE user_settings
               SET reminders_enabled = #{enabled},
                   version = version + 1,
                   updated_at = #{now}
             WHERE owner_id = #{ownerId}
            """)
    int updateEnabled(@Param("ownerId") Long ownerId,
                      @Param("enabled") boolean enabled,
                      @Param("now") LocalDateTime now);

    @Update("""
            UPDATE user_settings
               SET timezone_offset_minutes = #{minutes},
                   updated_at = #{now}
             WHERE owner_id = #{ownerId}
            """)
    int updateTimeZone(@Param("ownerId") Long ownerId,
                       @Param("minutes") int minutes,
                       @Param("now") LocalDateTime now);
}
