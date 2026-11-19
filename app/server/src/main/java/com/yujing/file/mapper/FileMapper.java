package com.yujing.file.mapper;

import com.yujing.file.model.FileRow;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface FileMapper {

    @Insert("""
            INSERT INTO files (owner_id, purpose, storage_path, content_type, size_bytes, sha256, created_at)
            VALUES (#{ownerId}, #{purpose}, #{storagePath}, #{contentType}, #{sizeBytes}, #{sha256}, #{createdAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FileRow row);

    @Select("SELECT * FROM files WHERE id = #{id} AND deleted_at IS NULL")
    FileRow findActive(@Param("id") Long id);

    @Select("""
            SELECT * FROM files
             WHERE owner_id = #{ownerId}
               AND deleted_at IS NULL
               AND purpose IN ('avatar', 'bio')
             ORDER BY id
            """)
    List<FileRow> listActiveByOwner(@Param("ownerId") Long ownerId);

    @Select("SELECT * FROM files WHERE owner_id = #{ownerId} ORDER BY id")
    List<FileRow> listAllByOwner(@Param("ownerId") Long ownerId);

    @Update("UPDATE files SET deleted_at = #{now} WHERE id = #{id} AND deleted_at IS NULL")
    int markDeleted(@Param("id") Long id, @Param("now") LocalDateTime now);
}
