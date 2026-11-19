package com.yujing.admin.mapper;

import com.yujing.auth.model.UserRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AdminUserMapper {

    @Select("""
            SELECT id, username, display_name, role, status, last_login_at, created_at
              FROM users
             WHERE (#{pattern,jdbcType=VARCHAR} IS NULL OR username LIKE #{pattern,jdbcType=VARCHAR})
             ORDER BY id DESC
             LIMIT #{limit} OFFSET #{offset}
            """)
    List<UserRow> findPage(@Param("pattern") String pattern,
                           @Param("limit") int limit,
                           @Param("offset") long offset);

    @Select("""
            SELECT COUNT(*)
              FROM users
             WHERE (#{pattern,jdbcType=VARCHAR} IS NULL OR username LIKE #{pattern,jdbcType=VARCHAR})
            """)
    long count(@Param("pattern") String pattern);

    @Update("""
            UPDATE users
               SET status = #{status},
                   version = version + 1,
                   updated_at = #{now}
             WHERE id = #{id}
            """)
    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("now") LocalDateTime now);
}
