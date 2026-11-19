package com.yujing.note.mapper;

import com.yujing.note.model.NoteRow;
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
public interface NoteMapper {

    @Select("""
            SELECT id, title, category, version, created_at, updated_at
              FROM notes
             WHERE owner_id = #{ownerId}
               AND (#{keyword,jdbcType=VARCHAR} IS NULL
                    OR title LIKE CONCAT('%', #{keyword,jdbcType=VARCHAR}, '%'))
             ORDER BY updated_at DESC, id DESC
             LIMIT #{limit} OFFSET #{offset}
            """)
    List<NoteRow> findPage(@Param("ownerId") Long ownerId,
                           @Param("keyword") String keyword,
                           @Param("limit") int limit,
                           @Param("offset") long offset);

    @Select("""
            SELECT COUNT(*)
              FROM notes
             WHERE owner_id = #{ownerId}
               AND (#{keyword,jdbcType=VARCHAR} IS NULL
                    OR title LIKE CONCAT('%', #{keyword,jdbcType=VARCHAR}, '%'))
            """)
    long count(@Param("ownerId") Long ownerId, @Param("keyword") String keyword);

    @Select("SELECT * FROM notes WHERE owner_id = #{ownerId} AND id = #{id}")
    NoteRow findById(@Param("ownerId") Long ownerId, @Param("id") Long id);

    @Insert("""
            INSERT INTO notes (owner_id, title, body, category, version, created_at, updated_at)
            VALUES (#{ownerId}, #{title}, #{body,jdbcType=LONGVARCHAR}, #{category,jdbcType=VARCHAR},
                    0, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(NoteRow row);

    @Update("""
            UPDATE notes
               SET title = #{title},
                   body = #{body,jdbcType=LONGVARCHAR},
                   category = #{category,jdbcType=VARCHAR},
                   version = version + 1,
                   updated_at = #{updatedAt}
             WHERE owner_id = #{ownerId}
               AND id = #{id}
               AND version = #{version}
            """)
    int update(NoteRow row);

    @Delete("""
            DELETE FROM notes
             WHERE owner_id = #{ownerId}
               AND id = #{id}
               AND version = #{version}
            """)
    int delete(@Param("ownerId") Long ownerId,
               @Param("id") Long id,
               @Param("version") long version);
}
