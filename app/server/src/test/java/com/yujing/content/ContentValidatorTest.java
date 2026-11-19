package com.yujing.content;

import com.yujing.common.error.BizException;
import com.yujing.content.dto.UpdateContactsRequest.ContactInput;
import com.yujing.content.dto.UpdateHonorsRequest.HonorInput;
import com.yujing.content.dto.UpdateInfoLinesRequest.LineInput;
import com.yujing.content.dto.UpdateProfileRequest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class ContentValidatorTest {

    @Test
    void ignoresBlankRowsAndKeepsOrder() {
        List<ContentValidator.NormalizedLine> lines = ContentValidator.validateLines(List.of(
                new LineInput("城市", "上海"),
                new LineInput("空行", "   "),
                new LineInput(null, "自由文本")));

        assertThat(lines).hasSize(2);
        assertThat(lines.get(0).label()).isEqualTo("城市");
        assertThat(lines.get(1).value()).isEqualTo("自由文本");
    }

    @Test
    void rejectsLabelsContainingColon() {
        BizException ex = catchThrowableOfType(
                () -> ContentValidator.validateLines(List.of(new LineInput("邮箱: 工作", "a@b.com"))),
                BizException.class);

        assertThat(fields(ex)).containsKey("lines[0].label");
    }

    @Test
    void limitsInfoLinesToTwentyAfterBlankRowsAreDropped() {
        List<LineInput> inputs = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            inputs.add(new LineInput("L" + i, "V" + i));
        }
        BizException ex = catchThrowableOfType(
                () -> ContentValidator.validateLines(inputs), BizException.class);
        assertThat(fields(ex)).containsKey("lines");
    }

    @Test
    void profileCountsCharactersWithoutNewlines() {
        String exactly2000 = "a\n".repeat(1000) + "a".repeat(1000);
        UpdateProfileRequest ok = new UpdateProfileRequest(null, "标题", null, exactly2000, List.of(), 0L);
        assertThat(ContentValidator.validateProfile(ok).bio()).hasSize(exactly2000.length());

        String tooLong = exactly2000 + "b";
        UpdateProfileRequest bad = new UpdateProfileRequest(null, "标题", null, tooLong, List.of(), 0L);
        BizException ex = catchThrowableOfType(
                () -> ContentValidator.validateProfile(bad), BizException.class);
        assertThat(fields(ex)).containsKey("bio");
    }

    @Test
    void rejectsMoreThanSixOrDuplicateImages() {
        List<Long> seven = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L);
        BizException tooMany = catchThrowableOfType(
                () -> ContentValidator.validateProfile(new UpdateProfileRequest(null, null, null, null, seven, 0L)),
                BizException.class);
        assertThat(fields(tooMany)).containsKey("imageFileIds");

        BizException duplicate = catchThrowableOfType(
                () -> ContentValidator.validateProfile(
                        new UpdateProfileRequest(null, null, null, null, List.of(1L, 1L), 0L)),
                BizException.class);
        assertThat(fields(duplicate)).containsKey("imageFileIds");
    }

    @Test
    void parsesHonorDatesAndRejectsMalformedOnes() {
        List<ContentValidator.NormalizedHonor> honors = ContentValidator.validateHonors(List.of(
                new HonorInput("一等奖", "2024-05-01")));
        assertThat(honors.get(0).awardedOn()).isEqualTo(LocalDate.of(2024, 5, 1));

        BizException ex = catchThrowableOfType(
                () -> ContentValidator.validateHonors(List.of(new HonorInput("一等奖", "2024/05/01"))),
                BizException.class);
        assertThat(fields(ex)).containsKey("items[0].awardedOn");
    }

    @Test
    void validatesContactTargetsByType() {
        BizException email = catchThrowableOfType(
                () -> ContentValidator.validateContacts(List.of(
                        new ContactInput("email", "工作邮箱", "not-an-email", true))),
                BizException.class);
        assertThat(fields(email)).containsKey("items[0].target");

        BizException link = catchThrowableOfType(
                () -> ContentValidator.validateContacts(List.of(
                        new ContactInput("link", "主页", "ftp://example.com", true))),
                BizException.class);
        assertThat(fields(link)).containsKey("items[0].target");

        List<ContentValidator.NormalizedContact> social = ContentValidator.validateContacts(List.of(
                new ContactInput("social", "微信", null, null)));
        assertThat(social.get(0).publicVisible()).isTrue();
        assertThat(social.get(0).target()).isNull();
    }

    @Test
    void rejectsUnknownContactType() {
        BizException ex = catchThrowableOfType(
                () -> ContentValidator.validateContacts(List.of(
                        new ContactInput("phone", "电话", "123", null))),
                BizException.class);
        assertThat(fields(ex)).containsKey("items[0].type");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> fields(BizException ex) {
        return (Map<String, String>) ((Map<String, Object>) ex.data()).get("fields");
    }
}
