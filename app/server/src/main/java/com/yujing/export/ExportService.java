package com.yujing.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yujing.birthday.mapper.BirthdayMapper;
import com.yujing.common.error.BizException;
import com.yujing.common.error.ErrorCode;
import com.yujing.course.mapper.CourseMapper;
import com.yujing.export.dto.ExportJobView;
import com.yujing.export.dto.TaskExport;
import com.yujing.export.mapper.ExportJobMapper;
import com.yujing.export.model.ExportJobRow;
import com.yujing.file.FileStorage;
import com.yujing.file.mapper.FileMapper;
import com.yujing.file.model.FileRow;
import com.yujing.note.mapper.NoteMapper;
import com.yujing.reminder.mapper.ReminderMapper;
import com.yujing.task.mapper.TaskMapper;
import com.yujing.task.model.TaskCheckinRow;
import com.yujing.task.model.TaskMilestoneRow;
import com.yujing.task.model.TaskRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Asynchronous export of all personal data (plan 9.7): five JSON files plus
 * every PNG. One export per 24 hours, download valid for 7 days.
 */
@Service
public class ExportService {

    private static final Logger log = LoggerFactory.getLogger(ExportService.class);

    private static final Duration COOLDOWN = Duration.ofHours(24);
    private static final Duration TTL = Duration.ofDays(7);

    private final ExportJobMapper jobMapper;
    private final FileMapper fileMapper;
    private final BirthdayMapper birthdayMapper;
    private final CourseMapper courseMapper;
    private final TaskMapper taskMapper;
    private final NoteMapper noteMapper;
    private final ReminderMapper reminderMapper;
    private final FileStorage storage;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public ExportService(ExportJobMapper jobMapper,
                         FileMapper fileMapper,
                         BirthdayMapper birthdayMapper,
                         CourseMapper courseMapper,
                         TaskMapper taskMapper,
                         NoteMapper noteMapper,
                         ReminderMapper reminderMapper,
                         FileStorage storage,
                         ObjectMapper objectMapper,
                         Clock clock) {
        this.jobMapper = jobMapper;
        this.fileMapper = fileMapper;
        this.birthdayMapper = birthdayMapper;
        this.courseMapper = courseMapper;
        this.taskMapper = taskMapper;
        this.noteMapper = noteMapper;
        this.reminderMapper = reminderMapper;
        this.storage = storage;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public ExportJobView create(Long ownerId) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime last = jobMapper.findLastCreatedAt(ownerId);
        if (last != null && last.isAfter(now.minus(COOLDOWN))) {
            throw new BizException(ErrorCode.EXPORT_RATE_LIMITED);
        }
        ExportJobRow row = new ExportJobRow();
        row.setOwnerId(ownerId);
        row.setStatus("pending");
        row.setCreatedAt(now);
        jobMapper.insert(row);
        return toView(row);
    }

    public ExportJobView latest(Long ownerId) {
        ExportJobRow row = jobMapper.findLatest(ownerId);
        return row == null ? null : toView(row);
    }

    public ExportJobView get(Long ownerId, Long id) {
        return toView(require(ownerId, id));
    }

    public ExportDownload download(Long ownerId, Long id) {
        ExportJobRow job = require(ownerId, id);
        LocalDateTime now = LocalDateTime.now(clock);
        if (!"ready".equals(job.getStatus()) || job.getFileId() == null
                || job.getExpiresAt() == null || !job.getExpiresAt().isAfter(now)) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        FileRow file = fileMapper.findActive(job.getFileId());
        if (file == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        LocalDate date = job.getCompletedAt() == null ? now.toLocalDate() : job.getCompletedAt().toLocalDate();
        return new ExportDownload(storage.read(file.getStoragePath()),
                "yujing-export-" + date + ".zip");
    }

    /** Called by the worker; failures are recorded on the job, never thrown. */
    public void processPending(int limit) {
        for (Long id : jobMapper.findPendingIds(limit)) {
            ExportJobRow job = jobMapper.findByIdInternal(id);
            if (job == null || !"pending".equals(job.getStatus())) {
                continue;
            }
            try {
                build(job);
            } catch (RuntimeException ex) {
                log.warn("Export job {} failed", id, ex);
                jobMapper.markFailed(job.getOwnerId(), job.getId(), LocalDateTime.now(clock));
            }
        }
    }

    public void expireOld(int limit) {
        LocalDateTime now = LocalDateTime.now(clock);
        for (Long id : jobMapper.findExpiredIds(now, limit)) {
            ExportJobRow job = jobMapper.findByIdInternal(id);
            if (job == null || jobMapper.markExpired(id) == 0) {
                continue;
            }
            if (job.getFileId() == null) {
                continue;
            }
            FileRow file = fileMapper.findActive(job.getFileId());
            if (file != null) {
                storage.deleteQuietly(file.getStoragePath());
                fileMapper.markDeleted(file.getId(), now);
            }
        }
    }

    private void build(ExportJobRow job) {
        byte[] archive = buildArchive(job.getOwnerId());
        String relativePath = "exports/" + UUID.randomUUID().toString().replace("-", "") + ".zip";
        String sha256 = storage.write(relativePath, archive);

        LocalDateTime now = LocalDateTime.now(clock);
        FileRow file = new FileRow();
        file.setOwnerId(job.getOwnerId());
        file.setPurpose("export");
        file.setStoragePath(relativePath);
        file.setContentType("application/zip");
        file.setSizeBytes((long) archive.length);
        file.setSha256(sha256);
        file.setCreatedAt(now);
        try {
            fileMapper.insert(file);
        } catch (RuntimeException ex) {
            storage.deleteQuietly(relativePath);
            throw ex;
        }

        jobMapper.markReady(job.getOwnerId(), job.getId(), file.getId(),
                now.plus(TTL), now);
    }

    private byte[] buildArchive(Long ownerId) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(buffer, StandardCharsets.UTF_8)) {
            addJson(zip, "birthdays.json", birthdayMapper.findAll(ownerId));
            addJson(zip, "courses.json", courseMapper.findAll(ownerId));
            addJson(zip, "tasks.json", tasks(ownerId));
            addJson(zip, "notes.json", noteMapper.findAll(ownerId));
            addJson(zip, "reminders.json", reminderMapper.findByOwner(ownerId));
            for (FileRow file : fileMapper.listActiveByOwner(ownerId)) {
                try {
                    addBytes(zip, "images/" + file.getId() + ".png",
                            storage.read(file.getStoragePath()));
                } catch (BizException ex) {
                    // A row without its physical file is an orphan; skip it so one
                    // corrupt image cannot block the whole export.
                    log.warn("Skipping missing export image {}", file.getId());
                }
            }
        } catch (IOException ex) {
            throw new BizException(ErrorCode.SERVICE_UNAVAILABLE);
        }
        return buffer.toByteArray();
    }

    private List<TaskExport> tasks(Long ownerId) {
        Map<Long, List<LocalDate>> checkins = new HashMap<>();
        for (TaskCheckinRow row : taskMapper.findCheckins(ownerId)) {
            checkins.computeIfAbsent(row.getTaskId(), key -> new ArrayList<>())
                    .add(row.getCheckinDate());
        }
        Map<Long, List<TaskExport.MilestoneExport>> milestones = new HashMap<>();
        for (TaskMilestoneRow row : taskMapper.findMilestones(ownerId)) {
            milestones.computeIfAbsent(row.getTaskId(), key -> new ArrayList<>())
                    .add(new TaskExport.MilestoneExport(row.getMilestoneDays(), row.getReachedOn()));
        }
        List<TaskExport> result = new ArrayList<>();
        for (TaskRow row : taskMapper.findAll(ownerId)) {
            result.add(new TaskExport(row.getId(), row.getOwnerId(), row.getName(), row.getPeriod(),
                    row.getWeeklyTarget(), row.getStartDate(), row.getRemindTime(), row.isEnabled(),
                    row.getVersion(), row.getCreatedAt(), row.getUpdatedAt(),
                    checkins.getOrDefault(row.getId(), List.of()),
                    milestones.getOrDefault(row.getId(), List.of())));
        }
        return result;
    }

    private void addJson(ZipOutputStream zip, String name, Object value) throws IOException {
        addBytes(zip, name, objectMapper.writeValueAsBytes(value));
    }

    private static void addBytes(ZipOutputStream zip, String name, byte[] bytes) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(bytes);
        zip.closeEntry();
    }

    private ExportJobRow require(Long ownerId, Long id) {
        ExportJobRow row = jobMapper.findById(ownerId, id);
        if (row == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return row;
    }

    private ExportJobView toView(ExportJobRow row) {
        LocalDateTime now = LocalDateTime.now(clock);
        boolean ready = "ready".equals(row.getStatus())
                && row.getExpiresAt() != null && row.getExpiresAt().isAfter(now);
        return new ExportJobView(row.getId(), row.getStatus(), utc(row.getCreatedAt()),
                utc(row.getCompletedAt()), utc(row.getExpiresAt()),
                ready ? "/api/v1/export/" + row.getId() + "/download" : null);
    }

    private static OffsetDateTime utc(LocalDateTime value) {
        return value == null ? null : OffsetDateTime.of(value, ZoneOffset.UTC);
    }

    public record ExportDownload(byte[] content, String filename) {
    }
}
