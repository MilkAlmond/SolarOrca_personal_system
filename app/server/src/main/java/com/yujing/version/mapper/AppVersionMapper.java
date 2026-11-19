package com.yujing.version.mapper;

import com.yujing.version.model.AppVersionRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AppVersionMapper {

    @Select("SELECT * FROM app_versions WHERE platform = #{platform}")
    AppVersionRow findByPlatform(@Param("platform") String platform);
}
