package com.yujing.reminder;

import com.yujing.birthday.BirthdayService;
import com.yujing.common.time.ClientZone;
import com.yujing.course.CourseService;
import com.yujing.reminder.mapper.ReminderSchedulerMapper;
import com.yujing.reminder.model.OwnerZoneRow;
import com.yujing.task.TaskService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Keeps the 35-day reminder horizon rolling for offline users and retires
 * reminders nobody delivered within a day (plan 4.2, 4.5).
 */
@Component
public class ReminderMaintenanceJob {

    private static final long MISSED_AFTER_HOURS = 24;
    private static final long TOMBSTONE_RETENTION_DAYS = 30;

    private final ReminderSchedulerMapper schedulerMapper;
    private final BirthdayService birthdayService;
    private final CourseService courseService;
    private final TaskService taskService;
    private final Clock clock;

    public ReminderMaintenanceJob(ReminderSchedulerMapper schedulerMapper,
                                  BirthdayService birthdayService,
                                  CourseService courseService,
                                  TaskService taskService,
                                  Clock clock) {
        this.schedulerMapper = schedulerMapper;
        this.birthdayService = birthdayService;
        this.courseService = courseService;
        this.taskService = taskService;
        this.clock = clock;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        regenerateAll();
    }

    @Scheduled(cron = "0 5 * * * *")
    public void regenerateAll() {
        for (OwnerZoneRow owner : schedulerMapper.listOwnerZones()) {
            ClientZone zone = ClientZone.ofMinutes(owner.getTimezoneOffsetMinutes());
            birthdayService.regenerateForOwner(owner.getOwnerId(), zone);
            courseService.regenerateForOwner(owner.getOwnerId(), zone);
            taskService.regenerateForOwner(owner.getOwnerId(), zone);
        }
    }

    @Scheduled(cron = "0 20 3 * * *")
    public void markMissedAndPurge() {
        LocalDateTime now = LocalDateTime.now(clock);
        for (Long id : schedulerMapper.findMissedIds(now.minusHours(MISSED_AFTER_HOURS))) {
            schedulerMapper.markMissed(id, now);
        }
        schedulerMapper.deleteExpiredTombstones(now.minusDays(TOMBSTONE_RETENTION_DAYS));
    }
}
