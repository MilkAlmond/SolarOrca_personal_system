package com.yujing.version.mapper;

import com.yujing.version.model.AppVersionRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AppVersionMapper {

    @Select("SELECT * FROM app_versions WHERE platform = #{platform}")
    AppVersionRow findByPlatform(@Param("platform") String platform);

    @Select("SELECT * FROM app_versions ORDER BY platform")
    List<AppVersionRow> findAll();

    @Update("""
            UPDATE app_versions
               SET latest_version = #{latestVersion},
                   min_supported_version = #{minSupportedVersion},
                   download_url = #{downloadUrl,jdbcType=VARCHAR},
                   notes = #{notes,jdbcType=VARCHAR},
                   updated_at = #{now}
             WHERE platform = #{platform}
            """)
    int update(@Param("platform") String platform,
               @Param("latestVersion") String latestVersion,
               @Param("minSupportedVersion") String minSupportedVersion,
               @Param("downloadUrl") String downloadUrl,
               @Param("notes") String notes,
               @Param("now") LocalDateTime now);
}
