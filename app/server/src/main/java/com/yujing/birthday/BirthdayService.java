package com.yujing.birthday;

import com.yujing.birthday.BirthdaySchedule.Occurrence;
import com.yujing.birthday.dto.BirthdayRequest;
import com.yujing.birthday.dto.BirthdaySaveResult;
import com.yujing.birthday.dto.BirthdayView;
import com.yujing.birthday.mapper.BirthdayMapper;
import com.yujing.birthday.model.BirthdayRow;
import com.yujing.common.error.BizException;
import com.yujing.common.error.ErrorCode;
import com.yujing.common.time.ClientZone;
import com.yujing.reminder.ReminderKeys;
import com.yujing.reminder.ReminderService;
import com.yujing.reminder.ReminderSpec;
import com.yujing.sync.TombstoneService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class BirthdayService {

    private static final Set<String> CATEGORIES = Set.of("birthday", "anniversary");
    private static final Set<String> CALENDARS = Set.of("solar", "lunar");
    private static final LocalTime DEFAULT_HOUR = LocalTime.of(9, 0);

    private final BirthdayMapper mapper;
    private final ReminderService reminderService;
    private final TombstoneService tombstoneService;
    private final Clock clock;

    public BirthdayService(BirthdayMapper mapper, ReminderService reminderService,
                           TombstoneService tombstoneService, Clock clock) {
        this.mapper = mapper;
        this.reminderService = reminderService;
        this.tombstoneService = tombstoneService;
        this.clock = clock;
    }

    public List<BirthdayView> list(Long ownerId, ClientZone zone) {
        LocalDate today = zone.today(clock);
        return mapper.findAll(ownerId).stream()
                .map(row -> toView(row, today))
                .sorted(Comparator.comparing(BirthdayView::nextDate,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(BirthdayView::id))
                .toList();
    }

    @Transactional
    public BirthdaySaveResult create(Long ownerId, BirthdayRequest request, ClientZone zone) {
        BirthdayRow row = new BirthdayRow();
        row.setOwnerId(ownerId);
        apply(request, row);
        LocalDateTime now = LocalDateTime.now(clock);
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        mapper.insert(row);

        ReminderService.GeneratedSync sync = sync(ownerId, row, zone, now);
        return new BirthdaySaveResult(toView(row, zone.today(clock)), sync.notice());
    }

    @Transactional
    public BirthdaySaveResult update(Long ownerId, Long id, BirthdayRequest request, ClientZone zone) {
        BirthdayRow existing = require(ownerId, id);
        long expected = request.version() == null ? -1 : request.version();
        if (expected != existing.getVersion()) {
            throw new BizException(ErrorCode.VERSION_CONFLICT);
        }
        BirthdayRow row = new BirthdayRow();
        row.setId(id);
        row.setOwnerId(ownerId);
        row.setVersion(existing.getVersion());
        apply(request, row);
        row.setUpdatedAt(LocalDateTime.now(clock));
        if (mapper.update(row) == 0) {
            throw new BizException(ErrorCode.VERSION_CONFLICT);
        }

        BirthdayRow saved = require(ownerId, id);
        ReminderService.GeneratedSync sync = sync(ownerId, saved, zone, LocalDateTime.now(clock));
        return new BirthdaySaveResult(toView(saved, zone.today(clock)), sync.notice());
    }

    @Transactional
    public void delete(Long ownerId, Long id, long version) {
        BirthdayRow row = require(ownerId, id);
        if (row.getVersion() != version) {
            throw new BizException(ErrorCode.VERSION_CONFLICT);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (mapper.delete(ownerId, id, version) == 0) {
            throw new BizException(ErrorCode.VERSION_CONFLICT);
        }
        tombstoneService.record(ownerId, TombstoneService.BIRTHDAY, id, version, now);
        reminderService.removeGenerated(ownerId, ReminderService.SOURCE_BIRTHDAY, id, true);
    }

    /** Used by the scheduled maintenance job to keep future reminders materialised. */
    @Transactional
    public void regenerateForOwner(Long ownerId, ClientZone zone) {
        LocalDateTime now = LocalDateTime.now(clock);
        for (BirthdayRow row : mapper.findAll(ownerId)) {
            sync(ownerId, row, zone, now);
        }
    }

    private ReminderService.GeneratedSync sync(Long ownerId, BirthdayRow row, ClientZone zone,
                                               LocalDateTime now) {
        Occurrence occurrence = BirthdaySchedule.next(row.getSolarDate(), row.getLunarMonth(),
                row.getLunarDay(), row.isLunarLeap(), zone.today(clock));
        List<ReminderSpec> specs = new ArrayList<>();
        if (occurrence != null) {
            String prefix = "birthday".equals(row.getCategory()) ? "生日：" : "纪念日：";
            String title = prefix + row.getName();
            OffsetDateTime dayAt = atNine(occurrence.date(), zone);
            specs.add(new ReminderSpec(ReminderKeys.birthday(row.getId(), occurrence.date(), false),
                    title, dayAt, 0));
            if (row.getAdvanceDays() > 0) {
                LocalDate advanceDate = occurrence.date().minusDays(row.getAdvanceDays());
                specs.add(new ReminderSpec(ReminderKeys.birthday(row.getId(), occurrence.date(), true),
                        title, atNine(advanceDate, zone), row.getAdvanceDays() * 1440));
            }
        }
        return reminderService.syncGenerated(ownerId, ReminderService.SOURCE_BIRTHDAY, row.getId(),
                specs, zone, now);
    }

    private static OffsetDateTime atNine(LocalDate date, ClientZone zone) {
        return OffsetDateTime.of(LocalDateTime.of(date, DEFAULT_HOUR), zone.zone());
    }

    private BirthdayRow require(Long ownerId, Long id) {
        BirthdayRow row = mapper.findById(ownerId, id);
        if (row == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return row;
    }

    private void apply(BirthdayRequest request, BirthdayRow row) {
        Map<String, String> errors = new LinkedHashMap<>();

        String name = trim(request.name());
        if (name == null || name.isEmpty()) {
            errors.put("name", "名称不能为空");
        } else if (codePoints(name) > 40) {
            errors.put("name", "名称不能超过 40 字");
        }

        String category = trim(request.category());
        if (category == null || !CATEGORIES.contains(category)) {
            errors.put("category", "类别只能是生日或纪念日");
        }

        String calendarType = trim(request.calendarType());
        if (calendarType == null || !CALENDARS.contains(calendarType)) {
            errors.put("calendarType", "历法只能是公历或农历");
        }

        LocalDate solarDate = null;
        Integer lunarMonth = null;
        Integer lunarDay = null;
        boolean lunarLeap = false;
        if ("solar".equals(calendarType)) {
            if (request.solarDate() == null) {
                errors.put("solarDate", "请选择公历日期");
            } else {
                solarDate = request.solarDate();
            }
        } else if ("lunar".equals(calendarType)) {
            lunarMonth = request.lunarMonth();
            lunarDay = request.lunarDay();
            lunarLeap = Boolean.TRUE.equals(request.lunarLeap());
            if (lunarMonth == null || lunarMonth < 1 || lunarMonth > 12) {
                errors.put("lunarMonth", "农历月份需在 1–12 之间");
            }
            if (lunarDay == null || lunarDay < 1 || lunarDay > 30) {
                errors.put("lunarDay", "农历日期需在 1–30 之间");
            }
        }

        int advanceDays = request.advanceDays() == null ? 0 : request.advanceDays();
        if (advanceDays < 0 || advanceDays > 30) {
            errors.put("advanceDays", "提前提醒需在 0–30 天之间");
        }

        String relationTag = trim(request.relationTag());
        if (relationTag != null && codePoints(relationTag) > 20) {
            errors.put("relationTag", "关系标签不能超过 20 字");
        }

        String note = trim(request.note());
        if (note != null && codePoints(note) > 200) {
            errors.put("note", "备注不能超过 200 字");
        }

        if (!errors.isEmpty()) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, Map.of("fields", errors));
        }

        row.setName(name);
        row.setCategory(category);
        row.setCalendarType(calendarType);
        row.setSolarDate(solarDate);
        row.setLunarMonth(lunarMonth);
        row.setLunarDay(lunarDay);
        row.setLunarLeap(lunarLeap);
        row.setRelationTag(relationTag);
        row.setAdvanceDays(advanceDays);
        row.setNote(note);
    }

    private BirthdayView toView(BirthdayRow row, LocalDate today) {
        Occurrence occurrence = BirthdaySchedule.next(row.getSolarDate(), row.getLunarMonth(),
                row.getLunarDay(), row.isLunarLeap(), today);
        LocalDate nextDate = occurrence == null ? null : occurrence.date();
        long daysUntil = nextDate == null ? 0 : BirthdaySchedule.daysUntil(nextDate, today);
        return new BirthdayView(
                row.getId(),
                row.getName(),
                row.getCategory(),
                row.getCalendarType(),
                row.getSolarDate(),
                row.getLunarMonth(),
                row.getLunarDay(),
                row.isLunarLeap(),
                row.getRelationTag(),
                row.getAdvanceDays(),
                row.getNote(),
                nextDate,
                daysUntil,
                occurrence != null && occurrence.leapAdjusted(),
                row.getVersion(),
                OffsetDateTime.of(row.getUpdatedAt(), ZoneOffset.UTC));
    }

    private static String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static int codePoints(String value) {
        return value.codePointCount(0, value.length());
    }
}
