package com.yujing.birthday.mapper;

import com.yujing.birthday.model.BirthdayRow;
import com.yujing.security.OwnerScoped;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
@OwnerScoped
public interface BirthdayMapper {

    @Select("SELECT * FROM birthdays WHERE owner_id = #{ownerId} ORDER BY id")
    List<BirthdayRow> findAll(@Param("ownerId") Long ownerId);

    @Select("SELECT * FROM birthdays WHERE owner_id = #{ownerId} AND id = #{id}")
    BirthdayRow findById(@Param("ownerId") Long ownerId, @Param("id") Long id);

    @Insert("""
            INSERT INTO birthdays (owner_id, name, category, calendar_type, solar_date, lunar_month,
                                   lunar_day, lunar_leap, relation_tag, advance_days, note, version,
                                   created_at, updated_at)
            VALUES (#{ownerId}, #{name}, #{category}, #{calendarType}, #{solarDate,jdbcType=DATE},
                    #{lunarMonth,jdbcType=TINYINT}, #{lunarDay,jdbcType=TINYINT}, #{lunarLeap},
                    #{relationTag,jdbcType=VARCHAR}, #{advanceDays}, #{note,jdbcType=VARCHAR},
                    0, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BirthdayRow row);

    @Update("""
            UPDATE birthdays
               SET name = #{name},
                   category = #{category},
                   calendar_type = #{calendarType},
                   solar_date = #{solarDate,jdbcType=DATE},
                   lunar_month = #{lunarMonth,jdbcType=TINYINT},
                   lunar_day = #{lunarDay,jdbcType=TINYINT},
                   lunar_leap = #{lunarLeap},
                   relation_tag = #{relationTag,jdbcType=VARCHAR},
                   advance_days = #{advanceDays},
                   note = #{note,jdbcType=VARCHAR},
                   version = version + 1,
                   updated_at = #{updatedAt}
             WHERE owner_id = #{ownerId}
               AND id = #{id}
               AND version = #{version}
            """)
    int update(BirthdayRow row);

    @Delete("""
            DELETE FROM birthdays
             WHERE owner_id = #{ownerId}
               AND id = #{id}
               AND version = #{version}
            """)
    int delete(@Param("ownerId") Long ownerId,
               @Param("id") Long id,
               @Param("version") long version);
}
