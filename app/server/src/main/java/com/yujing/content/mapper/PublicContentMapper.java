package com.yujing.content.mapper;

import com.yujing.content.model.ContactRow;
import com.yujing.content.model.HonorRow;
import com.yujing.content.model.ProfileRow;
import com.yujing.content.model.PublicProfileRow;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Public content is a global singleton maintained only by the admin, so its
 * statements are deliberately not owner-scoped (plan 5.3).
 */
@Mapper
public interface PublicContentMapper {

    @Select("SELECT * FROM public_profile WHERE id = 1")
    PublicProfileRow findProfile();

    @Insert("""
            INSERT INTO public_profile (id, avatar_file_id, title, subtitle, bio, version, updated_by,
                                        created_at, updated_at)
            VALUES (1, #{avatarFileId,jdbcType=BIGINT}, #{title,jdbcType=VARCHAR}, #{subtitle,jdbcType=VARCHAR},
                    #{bio,jdbcType=VARCHAR}, 0, #{updatedBy}, #{now}, #{now})
            """)
    int insertProfile(@Param("avatarFileId") Long avatarFileId,
                      @Param("title") String title,
                      @Param("subtitle") String subtitle,
                      @Param("bio") String bio,
                      @Param("updatedBy") Long updatedBy,
                      @Param("now") LocalDateTime now);

    @Update("""
            UPDATE public_profile
               SET avatar_file_id = #{avatarFileId,jdbcType=BIGINT},
                   title = #{title,jdbcType=VARCHAR},
                   subtitle = #{subtitle,jdbcType=VARCHAR},
                   bio = #{bio,jdbcType=VARCHAR},
                   updated_by = #{updatedBy},
                   version = version + 1,
                   updated_at = #{now}
             WHERE id = 1
               AND version = #{expectedVersion}
            """)
    int updateProfile(@Param("avatarFileId") Long avatarFileId,
                      @Param("title") String title,
                      @Param("subtitle") String subtitle,
                      @Param("bio") String bio,
                      @Param("updatedBy") Long updatedBy,
                      @Param("expectedVersion") long expectedVersion,
                      @Param("now") LocalDateTime now);

    @Select("SELECT file_id FROM profile_images ORDER BY sort_order, id")
    List<Long> listImageFileIds();

    @Delete("DELETE FROM profile_images")
    int deleteAllImages();

    @Insert("""
            INSERT INTO profile_images (file_id, sort_order, created_at)
            VALUES (#{fileId}, #{sortOrder}, #{now})
            """)
    int insertImage(@Param("fileId") Long fileId,
                    @Param("sortOrder") int sortOrder,
                    @Param("now") LocalDateTime now);

    @Select("SELECT * FROM profile_rows ORDER BY sort_order, id")
    List<ProfileRow> listRows();

    @Delete("DELETE FROM profile_rows")
    int deleteAllRows();

    @Insert("""
            INSERT INTO profile_rows (label, value, sort_order, created_at, updated_at)
            VALUES (#{label,jdbcType=VARCHAR}, #{value}, #{sortOrder}, #{now}, #{now})
            """)
    int insertRow(@Param("label") String label,
                  @Param("value") String value,
                  @Param("sortOrder") int sortOrder,
                  @Param("now") LocalDateTime now);

    @Select("SELECT * FROM honors ORDER BY sort_order, id")
    List<HonorRow> listHonors();

    @Delete("DELETE FROM honors")
    int deleteAllHonors();

    @Insert("""
            INSERT INTO honors (name, awarded_on, sort_order, created_at, updated_at)
            VALUES (#{name}, #{awardedOn}, #{sortOrder}, #{now}, #{now})
            """)
    int insertHonor(@Param("name") String name,
                    @Param("awardedOn") LocalDate awardedOn,
                    @Param("sortOrder") int sortOrder,
                    @Param("now") LocalDateTime now);

    @Select("""
            SELECT id, type, display_text, target, is_public AS public_visible, sort_order,
                   created_at, updated_at
              FROM contacts
             ORDER BY sort_order, id
            """)
    List<ContactRow> listContacts();

    @Delete("DELETE FROM contacts")
    int deleteAllContacts();

    @Insert("""
            INSERT INTO contacts (type, display_text, target, is_public, sort_order, created_at, updated_at)
            VALUES (#{type}, #{displayText}, #{target,jdbcType=VARCHAR}, #{publicVisible}, #{sortOrder},
                    #{now}, #{now})
            """)
    int insertContact(@Param("type") String type,
                      @Param("displayText") String displayText,
                      @Param("target") String target,
                      @Param("publicVisible") boolean publicVisible,
                      @Param("sortOrder") int sortOrder,
                      @Param("now") LocalDateTime now);

    @Select("""
            SELECT (SELECT COUNT(*) FROM public_profile)
                 + (SELECT COUNT(*) FROM profile_rows)
                 + (SELECT COUNT(*) FROM honors)
                 + (SELECT COUNT(*) FROM contacts)
                 + (SELECT COUNT(*) FROM profile_images)
            """)
    long countContent();

    @Select("SELECT COUNT(*) FROM public_profile WHERE avatar_file_id = #{fileId}")
    int countAvatarRef(@Param("fileId") Long fileId);

    @Select("SELECT COUNT(*) FROM profile_images WHERE file_id = #{fileId}")
    int countImageRef(@Param("fileId") Long fileId);
}
