package com.yujing.content;

import com.yujing.common.error.BizException;
import com.yujing.common.error.ErrorCode;
import com.yujing.content.dto.UpdateContactsRequest.ContactInput;
import com.yujing.content.dto.UpdateHonorsRequest.HonorInput;
import com.yujing.content.dto.UpdateInfoLinesRequest.LineInput;
import com.yujing.content.dto.UpdateProfileRequest;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Field rules for the four public blocks (plan 2.2-2.6). Messages point at the
 * offending row so the admin UI can highlight it.
 */
public final class ContentValidator {

    private static final Pattern DATE = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");
    private static final Set<String> CONTACT_TYPES = Set.of("email", "social", "link");

    private ContentValidator() {
    }

    public static NormalizedProfile validateProfile(UpdateProfileRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        String title = trimToNull(request.title());
        String subtitle = trimToNull(request.subtitle());
        String bio = request.bio() == null ? null : request.bio().strip();
        if (bio != null && bio.isEmpty()) {
            bio = null;
        }
        if (title != null && codePoints(title) > 40) {
            errors.put("title", "标题不能超过 40 字");
        }
        if (subtitle != null && codePoints(subtitle) > 60) {
            errors.put("subtitle", "副标题不能超过 60 字");
        }
        if (bio != null && visibleChars(bio) > 2000) {
            errors.put("bio", "正文不能超过 2000 字（不含换行）");
        }

        List<Long> images = request.imageFileIds() == null ? List.of() : request.imageFileIds();
        if (images.size() > 6) {
            errors.put("imageFileIds", "配图最多 6 张");
        }
        if (new HashSet<>(images).size() != images.size()) {
            errors.put("imageFileIds", "配图不能重复选择");
        }
        failIfAny(errors);
        return new NormalizedProfile(request.avatarFileId(), title, subtitle, bio, List.copyOf(images));
    }

    public static List<NormalizedLine> validateLines(List<LineInput> inputs) {
        Map<String, String> errors = new LinkedHashMap<>();
        List<NormalizedLine> lines = new ArrayList<>();
        if (inputs != null) {
            for (int i = 0; i < inputs.size(); i++) {
                LineInput input = inputs.get(i);
                if (input == null) {
                    continue;
                }
                String value = trimToNull(input.value());
                if (value == null) {
                    continue; // blank rows are ignored on save (plan 2.3)
                }
                String label = trimToNull(input.label());
                if (label != null) {
                    if (codePoints(label) > 40) {
                        errors.put("lines[" + i + "].label", "标签不能超过 40 字");
                    }
                    if (label.indexOf(':') >= 0 || label.indexOf('：') >= 0) {
                        errors.put("lines[" + i + "].label", "标签不能包含冒号");
                    }
                }
                if (codePoints(value) > 200) {
                    errors.put("lines[" + i + "].value", "取值不能超过 200 字");
                }
                lines.add(new NormalizedLine(label, value));
            }
        }
        if (lines.size() > 20) {
            errors.put("lines", "个人信息最多 20 行");
        }
        failIfAny(errors);
        return lines;
    }

    public static List<NormalizedHonor> validateHonors(List<HonorInput> inputs) {
        Map<String, String> errors = new LinkedHashMap<>();
        List<NormalizedHonor> honors = new ArrayList<>();
        if (inputs != null) {
            for (int i = 0; i < inputs.size(); i++) {
                HonorInput input = inputs.get(i);
                if (input == null) {
                    continue;
                }
                String name = trimToNull(input.name());
                if (name == null) {
                    errors.put("items[" + i + "].name", "名称不能为空");
                } else if (codePoints(name) > 60) {
                    errors.put("items[" + i + "].name", "名称不能超过 60 字");
                }

                String rawDate = trimToNull(input.awardedOn());
                LocalDate date = null;
                if (rawDate == null) {
                    errors.put("items[" + i + "].awardedOn", "时间不能为空");
                } else if (!DATE.matcher(rawDate).matches()) {
                    errors.put("items[" + i + "].awardedOn", "时间格式需为 yyyy-MM-dd");
                } else {
                    try {
                        date = LocalDate.parse(rawDate);
                    } catch (DateTimeParseException ex) {
                        errors.put("items[" + i + "].awardedOn", "时间不是有效日期");
                    }
                }
                if (name != null && date != null) {
                    honors.add(new NormalizedHonor(name, date));
                }
            }
        }
        failIfAny(errors);
        return honors;
    }

    public static List<NormalizedContact> validateContacts(List<ContactInput> inputs) {
        Map<String, String> errors = new LinkedHashMap<>();
        List<NormalizedContact> contacts = new ArrayList<>();
        if (inputs != null) {
            for (int i = 0; i < inputs.size(); i++) {
                ContactInput input = inputs.get(i);
                if (input == null) {
                    continue;
                }
                String type = trimToNull(input.type());
                String displayText = trimToNull(input.displayText());
                String target = trimToNull(input.target());

                if (type == null || !CONTACT_TYPES.contains(type)) {
                    errors.put("items[" + i + "].type", "类型只能是 email、social 或 link");
                }
                if (displayText == null) {
                    errors.put("items[" + i + "].displayText", "显示文字不能为空");
                } else if (codePoints(displayText) > 60) {
                    errors.put("items[" + i + "].displayText", "显示文字不能超过 60 字");
                }

                if ("email".equals(type)) {
                    if (target == null) {
                        errors.put("items[" + i + "].target", "邮箱地址不能为空");
                    } else if (codePoints(target) > 300 || !EMAIL.matcher(target).matches()) {
                        errors.put("items[" + i + "].target", "邮箱格式不正确");
                    }
                } else if ("link".equals(type)) {
                    if (target == null) {
                        errors.put("items[" + i + "].target", "链接不能为空");
                    } else if (codePoints(target) > 300 || !isHttpUrl(target)) {
                        errors.put("items[" + i + "].target", "链接需为 http 或 https 地址");
                    }
                } else if (target != null && codePoints(target) > 300) {
                    errors.put("items[" + i + "].target", "跳转地址不能超过 300 字");
                }

                boolean publicVisible = input.publicVisible() == null || input.publicVisible();
                if (type != null && displayText != null) {
                    contacts.add(new NormalizedContact(type, displayText, target, publicVisible));
                }
            }
        }
        failIfAny(errors);
        return contacts;
    }

    private static boolean isHttpUrl(String value) {
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            return uri.isAbsolute()
                    && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && uri.getHost() != null;
        } catch (URISyntaxException ex) {
            return false;
        }
    }

    private static void failIfAny(Map<String, String> errors) {
        if (!errors.isEmpty()) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, Map.of("fields", errors));
        }
    }

    static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }

    static int codePoints(String value) {
        return value.codePointCount(0, value.length());
    }

    static int visibleChars(String value) {
        int count = 0;
        for (int i = 0; i < value.length(); ) {
            int codePoint = value.codePointAt(i);
            if (codePoint != '\n' && codePoint != '\r') {
                count++;
            }
            i += Character.charCount(codePoint);
        }
        return count;
    }

    public record NormalizedProfile(Long avatarFileId, String title, String subtitle, String bio,
                                    List<Long> imageFileIds) {
    }

    public record NormalizedLine(String label, String value) {
    }

    public record NormalizedHonor(String name, LocalDate awardedOn) {
    }

    public record NormalizedContact(String type, String displayText, String target, boolean publicVisible) {
    }
}
