package com.yujing.course;

import com.yujing.common.error.BizException;
import com.yujing.common.error.ErrorCode;
import com.yujing.common.time.ClientZone;
import com.yujing.course.dto.CourseRequest;
import com.yujing.course.dto.CourseSaveResult;
import com.yujing.course.dto.CourseView;
import com.yujing.course.mapper.CourseMapper;
import com.yujing.course.model.CourseRow;
import com.yujing.reminder.ReminderKeys;
import com.yujing.reminder.ReminderService;
import com.yujing.reminder.ReminderSpec;
import com.yujing.sync.TombstoneService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class CourseService {

    private static final Set<String> REPEAT_TYPES = Set.of("once", "weekly");
    private static final Set<Integer> REMIND_OPTIONS = Set.of(0, 5, 10, 15, 30);
    private static final int DEFAULT_REMIND_MINUTES = 10;
    private static final int HORIZON_DAYS = 35;

    private final CourseMapper mapper;
    private final ReminderService reminderService;
    private final TombstoneService tombstoneService;
    private final Clock clock;

    public CourseService(CourseMapper mapper, ReminderService reminderService,
                         TombstoneService tombstoneService, Clock clock) {
        this.mapper = mapper;
        this.reminderService = reminderService;
        this.tombstoneService = tombstoneService;
        this.clock = clock;
    }

    public List<CourseView> list(Long ownerId) {
        return mapper.findAll(ownerId).stream().map(CourseService::toView).toList();
    }

    @Transactional
    public CourseSaveResult create(Long ownerId, CourseRequest request, ClientZone zone) {
        CourseRow row = new CourseRow();
        row.setOwnerId(ownerId);
        apply(request, row);
        LocalDateTime now = LocalDateTime.now(clock);
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        mapper.insert(row);

        List<CourseView> conflicts = findConflicts(ownerId, row);
        ReminderService.GeneratedSync sync = sync(ownerId, row, zone, now);
        return new CourseSaveResult(toView(row), conflicts, sync.notice());
    }

    @Transactional
    public CourseSaveResult update(Long ownerId, Long id, CourseRequest request, ClientZone zone) {
        CourseRow existing = require(ownerId, id);
        long expected = request.version() == null ? -1 : request.version();
        if (expected != existing.getVersion()) {
            throw new BizException(ErrorCode.VERSION_CONFLICT);
        }
        CourseRow row = new CourseRow();
        row.setId(id);
        row.setOwnerId(ownerId);
        row.setVersion(existing.getVersion());
        apply(request, row);
        row.setUpdatedAt(LocalDateTime.now(clock));
        if (mapper.update(row) == 0) {
            throw new BizException(ErrorCode.VERSION_CONFLICT);
        }

        CourseRow saved = require(ownerId, id);
        List<CourseView> conflicts = findConflicts(ownerId, saved);
        ReminderService.GeneratedSync sync = sync(ownerId, saved, zone, LocalDateTime.now(clock));
        return new CourseSaveResult(toView(saved), conflicts, sync.notice());
    }

    @Transactional
    public void delete(Long ownerId, Long id, long version) {
        CourseRow row = require(ownerId, id);
        if (row.getVersion() != version) {
            throw new BizException(ErrorCode.VERSION_CONFLICT);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (mapper.delete(ownerId, id, version) == 0) {
            throw new BizException(ErrorCode.VERSION_CONFLICT);
        }
        tombstoneService.record(ownerId, TombstoneService.COURSE, id, version, now);
        reminderService.removeGenerated(ownerId, ReminderService.SOURCE_COURSE, id, true);
    }

    /** Used by the scheduled maintenance job to keep future reminders materialised. */
    @Transactional
    public void regenerateForOwner(Long ownerId, ClientZone zone) {
        LocalDateTime now = LocalDateTime.now(clock);
        for (CourseRow row : mapper.findAll(ownerId)) {
            sync(ownerId, row, zone, now);
        }
    }

    private ReminderService.GeneratedSync sync(Long ownerId, CourseRow row, ClientZone zone,
                                               LocalDateTime now) {
        List<ReminderSpec> specs = new ArrayList<>();
        if (row.getRemindMinutes() > 0) {
            LocalDate today = zone.today(clock);
            String title = "上课提醒：" + row.getName();
            for (int i = 0; i <= HORIZON_DAYS; i++) {
                LocalDate date = today.plusDays(i);
                if (!occursOn(row, date)) {
                    continue;
                }
                LocalDateTime triggerLocal = LocalDateTime.of(date, row.getStartTime())
                        .minusMinutes(row.getRemindMinutes());
                specs.add(new ReminderSpec(
                        ReminderKeys.course(row.getId(), date, row.getStartTime()),
                        title,
                        OffsetDateTime.of(triggerLocal, zone.zone()),
                        row.getRemindMinutes()));
            }
        }
        return reminderService.syncGenerated(ownerId, ReminderService.SOURCE_COURSE, row.getId(),
                specs, zone, now);
    }

    private static boolean occursOn(CourseRow row, LocalDate date) {
        if ("once".equals(row.getRepeatType())) {
            return date.equals(row.getCourseDate());
        }
        if (date.getDayOfWeek().getValue() != row.getWeekday()) {
            return false;
        }
        if (row.getEffectiveFrom() != null && date.isBefore(row.getEffectiveFrom())) {
            return false;
        }
        return row.getEffectiveTo() == null || !date.isAfter(row.getEffectiveTo());
    }

    private List<CourseView> findConflicts(Long ownerId, CourseRow candidate) {
        List<CourseView> conflicts = new ArrayList<>();
        for (CourseRow other : mapper.findAll(ownerId)) {
            if (candidate.getId() != null && candidate.getId().equals(other.getId())) {
                continue;
            }
            if (conflicts(candidate, other)) {
                conflicts.add(toView(other));
            }
        }
        return conflicts;
    }

    private static boolean conflicts(CourseRow a, CourseRow b) {
        if (a.getWeekday() != b.getWeekday()) {
            return false;
        }
        boolean overlaps = a.getStartTime().isBefore(b.getEndTime())
                && b.getStartTime().isBefore(a.getEndTime());
        if (!overlaps) {
            return false;
        }
        boolean aWeekly = "weekly".equals(a.getRepeatType());
        boolean bWeekly = "weekly".equals(b.getRepeatType());
        if (aWeekly && bWeekly) {
            return rangesIntersect(a.getEffectiveFrom(), a.getEffectiveTo(),
                    b.getEffectiveFrom(), b.getEffectiveTo());
        }
        if (aWeekly) {
            return withinRange(a.getEffectiveFrom(), a.getEffectiveTo(), b.getCourseDate());
        }
        if (bWeekly) {
            return withinRange(b.getEffectiveFrom(), b.getEffectiveTo(), a.getCourseDate());
        }
        return a.getCourseDate() != null && Objects.equals(a.getCourseDate(), b.getCourseDate());
    }

    private static boolean rangesIntersect(LocalDate from1, LocalDate to1,
                                           LocalDate from2, LocalDate to2) {
        if (to1 != null && from2 != null && from2.isAfter(to1)) {
            return false;
        }
        return to2 == null || from1 == null || !from1.isAfter(to2);
    }

    private static boolean withinRange(LocalDate from, LocalDate to, LocalDate date) {
        if (date == null) {
            return false;
        }
        if (from != null && date.isBefore(from)) {
            return false;
        }
        return to == null || !date.isAfter(to);
    }

    private CourseRow require(Long ownerId, Long id) {
        CourseRow row = mapper.findById(ownerId, id);
        if (row == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return row;
    }

    private void apply(CourseRequest request, CourseRow row) {
        Map<String, String> errors = new LinkedHashMap<>();

        String name = trim(request.name());
        if (name == null || name.isEmpty()) {
            errors.put("name", "名称不能为空");
        } else if (codePoints(name) > 40) {
            errors.put("name", "名称不能超过 40 字");
        }

        String teacher = trim(request.teacher());
        if (teacher != null && codePoints(teacher) > 20) {
            errors.put("teacher", "老师不能超过 20 字");
        }

        String location = trim(request.location());
        if (location != null && codePoints(location) > 40) {
            errors.put("location", "地点不能超过 40 字");
        }

        Integer weekday = request.weekday();
        if (weekday == null || weekday < 1 || weekday > 7) {
            errors.put("weekday", "星期需在 1–7 之间");
        }

        if (request.startTime() == null) {
            errors.put("startTime", "请选择开始时间");
        }
        if (request.endTime() == null) {
            errors.put("endTime", "请选择结束时间");
        } else if (request.startTime() != null && !request.endTime().isAfter(request.startTime())) {
            errors.put("endTime", "结束时间必须晚于开始时间");
        }

        String repeatType = trim(request.repeatType());
        if (repeatType == null || !REPEAT_TYPES.contains(repeatType)) {
            errors.put("repeatType", "重复方式只能是单次或每周");
        }

        LocalDate courseDate = null;
        LocalDate effectiveFrom = null;
        LocalDate effectiveTo = null;
        if ("once".equals(repeatType)) {
            if (request.courseDate() == null) {
                errors.put("courseDate", "请选择上课日期");
            } else {
                courseDate = request.courseDate();
                if (weekday != null && weekday >= 1 && weekday <= 7
                        && courseDate.getDayOfWeek().getValue() != weekday) {
                    errors.put("weekday", "上课日期与星期不一致");
                }
            }
        } else if ("weekly".equals(repeatType)) {
            effectiveFrom = request.effectiveFrom();
            effectiveTo = request.effectiveTo();
            if (effectiveFrom != null && effectiveTo != null && effectiveFrom.isAfter(effectiveTo)) {
                errors.put("effectiveTo", "生效结束日期不能早于开始日期");
            }
        }

        int remindMinutes = request.remindMinutes() == null
                ? DEFAULT_REMIND_MINUTES : request.remindMinutes();
        if (!REMIND_OPTIONS.contains(remindMinutes)) {
            errors.put("remindMinutes", "提醒时间只能是 0、5、10、15、30 分钟");
        }

        if (!errors.isEmpty()) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, Map.of("fields", errors));
        }

        row.setName(name);
        row.setTeacher(teacher);
        row.setLocation(location);
        row.setWeekday(weekday);
        row.setStartTime(request.startTime());
        row.setEndTime(request.endTime());
        row.setRepeatType(repeatType);
        row.setCourseDate(courseDate);
        row.setEffectiveFrom(effectiveFrom);
        row.setEffectiveTo(effectiveTo);
        row.setRemindMinutes(remindMinutes);
    }

    private static CourseView toView(CourseRow row) {
        return new CourseView(
                row.getId(),
                row.getName(),
                row.getTeacher(),
                row.getLocation(),
                row.getWeekday(),
                row.getStartTime(),
                row.getEndTime(),
                row.getRepeatType(),
                row.getCourseDate(),
                row.getEffectiveFrom(),
                row.getEffectiveTo(),
                row.getRemindMinutes(),
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
