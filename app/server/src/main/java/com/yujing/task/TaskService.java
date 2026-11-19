package com.yujing.task;

import com.yujing.common.error.BizException;
import com.yujing.common.error.ErrorCode;
import com.yujing.common.time.ClientZone;
import com.yujing.reminder.ReminderKeys;
import com.yujing.reminder.ReminderService;
import com.yujing.reminder.ReminderSpec;
import com.yujing.sync.TombstoneService;
import com.yujing.task.dto.TaskCheckinResult;
import com.yujing.task.dto.TaskRequest;
import com.yujing.task.dto.TaskView;
import com.yujing.task.mapper.TaskMapper;
import com.yujing.task.model.TaskCheckinRow;
import com.yujing.task.model.TaskMilestoneRow;
import com.yujing.task.model.TaskRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TaskService {

    private static final Set<String> PERIODS = Set.of("daily", "weekly");
    private static final int HORIZON_DAYS = 35;
    private static final int[] MILESTONE_DAYS = {7, 30, 100};

    private final TaskMapper mapper;
    private final ReminderService reminderService;
    private final TombstoneService tombstoneService;
    private final Clock clock;

    public TaskService(TaskMapper mapper, ReminderService reminderService,
                       TombstoneService tombstoneService, Clock clock) {
        this.mapper = mapper;
        this.reminderService = reminderService;
        this.tombstoneService = tombstoneService;
        this.clock = clock;
    }

    public List<TaskView> list(Long ownerId, ClientZone zone) {
        LocalDate today = zone.today(clock);
        Map<Long, List<LocalDate>> checkins = groupCheckins(ownerId);
        Map<Long, List<Integer>> milestones = groupMilestones(ownerId);
        return mapper.findAll(ownerId).stream()
                .map(row -> toView(row, today, checkins.get(row.getId()), milestones.get(row.getId())))
                .toList();
    }

    public List<TaskView> today(Long ownerId, LocalDate date, ClientZone zone) {
        LocalDate target = date == null ? zone.today(clock) : date;
        Map<Long, List<LocalDate>> checkins = groupCheckins(ownerId);
        Map<Long, List<Integer>> milestones = groupMilestones(ownerId);
        return mapper.findAll(ownerId).stream()
                .filter(row -> row.isEnabled() && !row.getStartDate().isAfter(target))
                .map(row -> toView(row, target, checkins.get(row.getId()), milestones.get(row.getId())))
                .sorted(Comparator.comparing((TaskView view) -> view.checkedToday() ? 1 : 0)
                        .thenComparing(TaskView::id))
                .toList();
    }

    @Transactional
    public TaskView create(Long ownerId, TaskRequest request, ClientZone zone) {
        TaskRow row = new TaskRow();
        row.setOwnerId(ownerId);
        apply(request, row);
        LocalDateTime now = LocalDateTime.now(clock);
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        mapper.insert(row);

        sync(ownerId, row, zone, now);
        return toView(row, zone.today(clock), List.of(), List.of());
    }

    @Transactional
    public TaskView update(Long ownerId, Long id, TaskRequest request, ClientZone zone) {
        TaskRow existing = require(ownerId, id);
        long expected = request.version() == null ? -1 : request.version();
        if (expected != existing.getVersion()) {
            throw new BizException(ErrorCode.VERSION_CONFLICT);
        }
        TaskRow row = new TaskRow();
        row.setId(id);
        row.setOwnerId(ownerId);
        row.setVersion(existing.getVersion());
        apply(request, row);
        row.setUpdatedAt(LocalDateTime.now(clock));
        if (mapper.update(row) == 0) {
            throw new BizException(ErrorCode.VERSION_CONFLICT);
        }

        TaskRow saved = require(ownerId, id);
        sync(ownerId, saved, zone, LocalDateTime.now(clock));
        return toView(saved, zone.today(clock),
                mapper.findTaskCheckinDates(ownerId, id), milestonesFor(ownerId, id));
    }

    @Transactional
    public void delete(Long ownerId, Long id, long version) {
        TaskRow row = require(ownerId, id);
        if (row.getVersion() != version) {
            throw new BizException(ErrorCode.VERSION_CONFLICT);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (mapper.delete(ownerId, id, version) == 0) {
            throw new BizException(ErrorCode.VERSION_CONFLICT);
        }
        tombstoneService.record(ownerId, TombstoneService.TASK, id, version, now);
        reminderService.removeGenerated(ownerId, ReminderService.SOURCE_TASK, id, true);
    }

    @Transactional
    public TaskCheckinResult checkin(Long ownerId, Long id, LocalDate date) {
        if (date == null) {
            throw new BizException(ErrorCode.VALIDATION_FAILED,
                    Map.of("fields", Map.of("date", "日期不能为空")));
        }
        TaskRow row = require(ownerId, id);
        LocalDateTime now = LocalDateTime.now(clock);

        Integer newMilestone = null;
        if (mapper.countCheckin(ownerId, id, date) == 0) {
            try {
                mapper.insertCheckin(ownerId, id, date, now);
                if ("daily".equals(row.getPeriod())) {
                    newMilestone = recordMilestoneIfReached(ownerId, id, date, now);
                }
            } catch (org.springframework.dao.DuplicateKeyException ignored) {
                // Another device checked in first; idempotent by design.
            }
        }

        TaskView view = toView(row, date,
                mapper.findTaskCheckinDates(ownerId, id), milestonesFor(ownerId, id));
        return new TaskCheckinResult(view, newMilestone);
    }

    @Transactional
    public TaskView undoCheckin(Long ownerId, Long id, LocalDate date) {
        TaskRow row = require(ownerId, id);
        mapper.deleteCheckin(ownerId, id, date);
        return toView(row, date,
                mapper.findTaskCheckinDates(ownerId, id), milestonesFor(ownerId, id));
    }

    /** Used by the scheduled maintenance job to keep future reminders materialised. */
    @Transactional
    public void regenerateForOwner(Long ownerId, ClientZone zone) {
        LocalDateTime now = LocalDateTime.now(clock);
        for (TaskRow row : mapper.findAll(ownerId)) {
            sync(ownerId, row, zone, now);
        }
    }

    private Integer recordMilestoneIfReached(Long ownerId, Long taskId, LocalDate date,
                                             LocalDateTime now) {
        Set<LocalDate> dates = new HashSet<>(mapper.findTaskCheckinDates(ownerId, taskId));
        int streak = TaskStats.streak(dates, date);
        for (int milestone : MILESTONE_DAYS) {
            if (streak == milestone && mapper.countMilestone(ownerId, taskId, milestone) == 0) {
                mapper.insertMilestone(ownerId, taskId, milestone, date, now);
                return milestone;
            }
        }
        return null;
    }

    private ReminderService.GeneratedSync sync(Long ownerId, TaskRow row, ClientZone zone,
                                               LocalDateTime now) {
        List<ReminderSpec> specs = new ArrayList<>();
        if (row.isEnabled() && row.getRemindTime() != null) {
            LocalDate today = zone.today(clock);
            LocalDate start = row.getStartDate().isAfter(today) ? row.getStartDate() : today;
            LocalDate end = today.plusDays(HORIZON_DAYS);
            String title = "任务提醒：" + row.getName();
            for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                specs.add(new ReminderSpec(ReminderKeys.task(row.getId(), date), title,
                        OffsetDateTime.of(LocalDateTime.of(date, row.getRemindTime()), zone.zone()), 0));
            }
        }
        return reminderService.syncGenerated(ownerId, ReminderService.SOURCE_TASK, row.getId(),
                specs, zone, now);
    }

    private Map<Long, List<LocalDate>> groupCheckins(Long ownerId) {
        Map<Long, List<LocalDate>> grouped = new HashMap<>();
        for (TaskCheckinRow row : mapper.findCheckins(ownerId)) {
            grouped.computeIfAbsent(row.getTaskId(), key -> new ArrayList<>())
                    .add(row.getCheckinDate());
        }
        return grouped;
    }

    private Map<Long, List<Integer>> groupMilestones(Long ownerId) {
        Map<Long, List<Integer>> grouped = new HashMap<>();
        for (TaskMilestoneRow row : mapper.findMilestones(ownerId)) {
            grouped.computeIfAbsent(row.getTaskId(), key -> new ArrayList<>())
                    .add(row.getMilestoneDays());
        }
        return grouped;
    }

    private List<Integer> milestonesFor(Long ownerId, Long taskId) {
        List<Integer> result = new ArrayList<>();
        for (TaskMilestoneRow row : mapper.findMilestones(ownerId)) {
            if (row.getTaskId().equals(taskId)) {
                result.add(row.getMilestoneDays());
            }
        }
        return result;
    }

    private TaskRow require(Long ownerId, Long id) {
        TaskRow row = mapper.findById(ownerId, id);
        if (row == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return row;
    }

    private void apply(TaskRequest request, TaskRow row) {
        Map<String, String> errors = new LinkedHashMap<>();

        String name = trim(request.name());
        if (name == null || name.isEmpty()) {
            errors.put("name", "名称不能为空");
        } else if (codePoints(name) > 40) {
            errors.put("name", "名称不能超过 40 字");
        }

        String period = trim(request.period());
        if (period == null || !PERIODS.contains(period)) {
            errors.put("period", "周期只能是每日或每周");
        }

        Integer weeklyTarget = null;
        if ("weekly".equals(period)) {
            weeklyTarget = request.weeklyTarget();
            if (weeklyTarget == null || weeklyTarget < 1 || weeklyTarget > 7) {
                errors.put("weeklyTarget", "每周目标需在 1–7 之间");
            }
        }

        LocalDate startDate = request.startDate();
        if (startDate == null) {
            errors.put("startDate", "请选择开始日期");
        }

        if (!errors.isEmpty()) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, Map.of("fields", errors));
        }

        row.setName(name);
        row.setPeriod(period);
        row.setWeeklyTarget(weeklyTarget);
        row.setStartDate(startDate);
        row.setRemindTime(request.remindTime());
        row.setEnabled(request.enabled() == null || request.enabled());
    }

    private static TaskView toView(TaskRow row, LocalDate date, List<LocalDate> checkins,
                                   List<Integer> milestones) {
        Set<LocalDate> dates = checkins == null ? Set.of() : new HashSet<>(checkins);
        boolean weekly = "weekly".equals(row.getPeriod());
        boolean checked = dates.contains(date);
        int streak = weekly ? 0 : TaskStats.streak(dates, date);
        int weekProgress = TaskStats.weekProgress(dates, date);
        boolean weekCompleted = weekly && row.getWeeklyTarget() != null
                && weekProgress >= row.getWeeklyTarget();
        return new TaskView(
                row.getId(),
                row.getName(),
                row.getPeriod(),
                row.getWeeklyTarget(),
                row.getStartDate(),
                row.getRemindTime(),
                row.isEnabled(),
                streak,
                checked,
                weekProgress,
                weekCompleted,
                milestones == null ? List.of() : List.copyOf(milestones),
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
