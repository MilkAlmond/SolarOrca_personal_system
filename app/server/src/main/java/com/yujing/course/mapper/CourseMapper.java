package com.yujing.course.mapper;

import com.yujing.course.model.CourseRow;
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
public interface CourseMapper {

    @Select("""
            SELECT * FROM courses
             WHERE owner_id = #{ownerId}
             ORDER BY weekday, start_time, id
            """)
    List<CourseRow> findAll(@Param("ownerId") Long ownerId);

    @Select("""
            SELECT * FROM courses
             WHERE owner_id = #{ownerId}
               AND updated_at > #{since}
             ORDER BY updated_at, id
            """)
    List<CourseRow> findChanged(@Param("ownerId") Long ownerId,
                                @Param("since") java.time.LocalDateTime since);

    @Select("SELECT * FROM courses WHERE owner_id = #{ownerId} AND id = #{id}")
    CourseRow findById(@Param("ownerId") Long ownerId, @Param("id") Long id);

    @Insert("""
            INSERT INTO courses (owner_id, name, teacher, location, weekday, start_time, end_time,
                                 repeat_type, course_date, effective_from, effective_to, remind_minutes,
                                 version, created_at, updated_at)
            VALUES (#{ownerId}, #{name}, #{teacher,jdbcType=VARCHAR}, #{location,jdbcType=VARCHAR},
                    #{weekday}, #{startTime}, #{endTime}, #{repeatType},
                    #{courseDate,jdbcType=DATE}, #{effectiveFrom,jdbcType=DATE},
                    #{effectiveTo,jdbcType=DATE}, #{remindMinutes},
                    0, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CourseRow row);

    @Update("""
            UPDATE courses
               SET name = #{name},
                   teacher = #{teacher,jdbcType=VARCHAR},
                   location = #{location,jdbcType=VARCHAR},
                   weekday = #{weekday},
                   start_time = #{startTime},
                   end_time = #{endTime},
                   repeat_type = #{repeatType},
                   course_date = #{courseDate,jdbcType=DATE},
                   effective_from = #{effectiveFrom,jdbcType=DATE},
                   effective_to = #{effectiveTo,jdbcType=DATE},
                   remind_minutes = #{remindMinutes},
                   version = version + 1,
                   updated_at = #{updatedAt}
             WHERE owner_id = #{ownerId}
               AND id = #{id}
               AND version = #{version}
            """)
    int update(CourseRow row);

    @Delete("""
            DELETE FROM courses
             WHERE owner_id = #{ownerId}
               AND id = #{id}
               AND version = #{version}
            """)
    int delete(@Param("ownerId") Long ownerId,
               @Param("id") Long id,
               @Param("version") long version);
}
