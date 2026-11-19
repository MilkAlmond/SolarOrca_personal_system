package com.yujing.task.mapper;

import com.yujing.security.OwnerScoped;
import com.yujing.task.model.TaskCheckinRow;
import com.yujing.task.model.TaskMilestoneRow;
import com.yujing.task.model.TaskRow;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
@OwnerScoped
public interface TaskMapper {

    @Select("SELECT * FROM tasks WHERE owner_id = #{ownerId} ORDER BY id")
    List<TaskRow> findAll(@Param("ownerId") Long ownerId);

    @Select("""
            SELECT * FROM tasks
             WHERE owner_id = #{ownerId}
               AND updated_at > #{since}
             ORDER BY updated_at, id
            """)
    List<TaskRow> findChanged(@Param("ownerId") Long ownerId,
                              @Param("since") java.time.LocalDateTime since);

    @Update("""
            UPDATE tasks
               SET updated_at = #{now}
             WHERE owner_id = #{ownerId}
               AND id = #{id}
            """)
    int touch(@Param("ownerId") Long ownerId, @Param("id") Long id,
              @Param("now") java.time.LocalDateTime now);

    @Select("SELECT * FROM tasks WHERE owner_id = #{ownerId} AND id = #{id}")
    TaskRow findById(@Param("ownerId") Long ownerId, @Param("id") Long id);

    @Insert("""
            INSERT INTO tasks (owner_id, name, period, weekly_target, start_date, remind_time, enabled,
                               version, created_at, updated_at)
            VALUES (#{ownerId}, #{name}, #{period}, #{weeklyTarget,jdbcType=TINYINT}, #{startDate},
                    #{remindTime,jdbcType=TIME}, #{enabled}, 0, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TaskRow row);

    @Update("""
            UPDATE tasks
               SET name = #{name},
                   period = #{period},
                   weekly_target = #{weeklyTarget,jdbcType=TINYINT},
                   start_date = #{startDate},
                   remind_time = #{remindTime,jdbcType=TIME},
                   enabled = #{enabled},
                   version = version + 1,
                   updated_at = #{updatedAt}
             WHERE owner_id = #{ownerId}
               AND id = #{id}
               AND version = #{version}
            """)
    int update(TaskRow row);

    @Delete("""
            DELETE FROM tasks
             WHERE owner_id = #{ownerId}
               AND id = #{id}
               AND version = #{version}
            """)
    int delete(@Param("ownerId") Long ownerId,
               @Param("id") Long id,
               @Param("version") long version);

    @Select("""
            SELECT task_id, checkin_date FROM task_checkins
             WHERE owner_id = #{ownerId}
             ORDER BY task_id, checkin_date
            """)
    List<TaskCheckinRow> findCheckins(@Param("ownerId") Long ownerId);

    @Select("""
            SELECT checkin_date FROM task_checkins
             WHERE owner_id = #{ownerId}
               AND task_id = #{taskId}
             ORDER BY checkin_date
            """)
    List<LocalDate> findTaskCheckinDates(@Param("ownerId") Long ownerId,
                                         @Param("taskId") Long taskId);

    @Select("""
            SELECT COUNT(*) FROM task_checkins
             WHERE owner_id = #{ownerId}
               AND task_id = #{taskId}
               AND checkin_date = #{date}
            """)
    int countCheckin(@Param("ownerId") Long ownerId,
                     @Param("taskId") Long taskId,
                     @Param("date") LocalDate date);

    @Insert("""
            INSERT INTO task_checkins (owner_id, task_id, checkin_date, version, created_at, updated_at)
            VALUES (#{ownerId}, #{taskId}, #{date}, 0, #{now}, #{now})
            """)
    int insertCheckin(@Param("ownerId") Long ownerId,
                      @Param("taskId") Long taskId,
                      @Param("date") LocalDate date,
                      @Param("now") LocalDateTime now);

    @Delete("""
            DELETE FROM task_checkins
             WHERE owner_id = #{ownerId}
               AND task_id = #{taskId}
               AND checkin_date = #{date}
            """)
    int deleteCheckin(@Param("ownerId") Long ownerId,
                      @Param("taskId") Long taskId,
                      @Param("date") LocalDate date);

    @Select("""
            SELECT task_id, milestone_days, reached_on FROM task_milestones
             WHERE owner_id = #{ownerId}
             ORDER BY task_id, milestone_days
            """)
    List<TaskMilestoneRow> findMilestones(@Param("ownerId") Long ownerId);

    @Select("""
            SELECT COUNT(*) FROM task_milestones
             WHERE owner_id = #{ownerId}
               AND task_id = #{taskId}
               AND milestone_days = #{days}
            """)
    int countMilestone(@Param("ownerId") Long ownerId,
                       @Param("taskId") Long taskId,
                       @Param("days") int days);

    @Insert("""
            INSERT INTO task_milestones (owner_id, task_id, milestone_days, reached_on, version,
                                         created_at, updated_at)
            VALUES (#{ownerId}, #{taskId}, #{days}, #{reachedOn}, 0, #{now}, #{now})
            """)
    int insertMilestone(@Param("ownerId") Long ownerId,
                        @Param("taskId") Long taskId,
                        @Param("days") int days,
                        @Param("reachedOn") LocalDate reachedOn,
                        @Param("now") LocalDateTime now);
}
