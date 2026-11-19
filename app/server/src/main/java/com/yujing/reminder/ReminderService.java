package com.yujing.reminder;

import com.yujing.common.error.BizException;
import com.yujing.common.error.ErrorCode;
import com.yujing.common.time.ClientZone;
import com.yujing.reminder.dto.CreateReminderRequest;
import com.yujing.reminder.dto.ReminderReceiptRequest;
import com.yujing.reminder.dto.ReminderView;
import com.yujing.reminder.dto.UpdateReminderRequest;
import com.yujing.reminder.mapper.ReminderDeliveryMapper;
import com.yujing.reminder.mapper.ReminderMapper;
import com.yujing.reminder.model.ReminderDeliveryRow;
import com.yujing.reminder.model.ReminderRow;
import com.yujing.settings.mapper.UserSettingsMapper;
import com.yujing.settings.model.UserSettingsRow;
import com.yujing.sync.TombstoneService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * B5 reminder aggregation (plan 3.6, 4.2). Source modules only call
 * {@link #syncGenerated}; everything else is user-facing list operations.
 */
@Service
public class ReminderService {

    public static final String SOURCE_COURSE = "course";
    public static final String SOURCE_BIRTHDAY = "birthday";
    public static final String SOURCE_TASK = "task";
    public static final String SOURCE_MANUAL = "manual";

    private static final Set<String> RESULTS = Set.of("sent", "failed");
    private static final int MAX_RETRY_ATTEMPTS = 2;

    private final ReminderMapper reminderMapper;
    private final ReminderDeliveryMapper deliveryMapper;
    private final UserSettingsMapper settingsMapper;
    private final TombstoneService tombstoneService;
    private final Clock clock;

    public ReminderService(ReminderMapper reminderMapper,
                           ReminderDeliveryMapper deliveryMapper,
                           UserSettingsMapper settingsMapper,
                           TombstoneService tombstoneService,
                           Clock clock) {
        this.reminderMapper = reminderMapper;
        this.deliveryMapper = deliveryMapper;
        this.settingsMapper = settingsMapper;
        this.tombstoneService = tombstoneService;
        this.clock = clock;
    }

    /**
     * Makes the generated set match {@code specs}: new occurrences are inserted,
     * pending ones are refreshed (keeping the user's on/off switch), sent/failed
     * rows are left untouched, and pending rows that disappeared are removed.
     * Triggers already in the past are skipped (plan 4.2).
     */
    @Transactional
    public GeneratedSync syncGenerated(Long ownerId, String source, Long sourceId,
                                       List<ReminderSpec> specs, ClientZone zone, LocalDateTime now) {
        rememberZone(ownerId, zone, now);

        Map<String, ReminderSpec> desired = new LinkedHashMap<>();
        for (ReminderSpec spec : specs) {
            desired.put(spec.dedupeKey(), spec);
        }
        Map<String, ReminderRow> existing = new LinkedHashMap<>();
        for (ReminderRow row : reminderMapper.findBySource(ownerId, source, sourceId)) {
            existing.put(row.getDedupeKey(), row);
        }

        int created = 0;
        int updated = 0;
        int skippedPast = 0;
        for (ReminderSpec spec : desired.values()) {
            LocalDateTime triggerUtc = spec.triggerAt()
                    .withOffsetSameInstant(ZoneOffset.UTC)
                    .toLocalDateTime();
            if (!triggerUtc.isAfter(now)) {
                skippedPast++;
                continue;
            }
            ReminderRow row = existing.get(spec.dedupeKey());
            if (row == null) {
                reminderMapper.insert(ownerId, source, sourceId, spec.title(), triggerUtc,
                        spec.advanceMinutes(), true, "pending", spec.dedupeKey(), now);
                created++;
            } else if ("pending".equals(row.getDeliveryStatus())) {
                reminderMapper.updateGenerated(ownerId, row.getId(), spec.title(), triggerUtc,
                        spec.advanceMinutes(), now);
                updated++;
            }
        }

        int removed = 0;
        for (ReminderRow row : existing.values()) {
            if ("pending".equals(row.getDeliveryStatus()) && !desired.containsKey(row.getDedupeKey())) {
                reminderMapper.deleteByIdInternal(ownerId, row.getId());
                removed++;
            }
        }
        return new GeneratedSync(created, updated, removed, skippedPast);
    }

    /** Removes generated reminders when the source is disabled or deleted. */
    @Transactional
    public void removeGenerated(Long ownerId, String source, Long sourceId, boolean includeFailed) {
        if (includeFailed) {
            reminderMapper.deleteUnsentBySource(ownerId, source, sourceId);
        } else {
            reminderMapper.deletePendingBySource(ownerId, source, sourceId);
        }
    }

    public void rememberZone(Long ownerId, ClientZone zone) {
        rememberZone(ownerId, zone, LocalDateTime.now(clock));
    }

    private void rememberZone(Long ownerId, ClientZone zone, LocalDateTime now) {
        if (zone == null) {
            return;
        }
        settingsMapper.insertDefault(ownerId, now);
        settingsMapper.updateTimeZone(ownerId, zone.offsetMinutes(), now);
    }

    public List<ReminderView> list(Long ownerId) {
        return reminderMapper.findByOwner(ownerId).stream().map(ReminderService::toView).toList();
    }

    @Transactional
    public ReminderView createManual(Long ownerId, CreateReminderRequest request, ClientZone zone) {
        LocalDateTime now = LocalDateTime.now(clock);
        rememberZone(ownerId, zone, now);
        String title = request.title() == null ? "" : request.title().strip();
        if (title.isEmpty() || title.codePointCount(0, title.length()) > 40) {
            throw new BizException(ErrorCode.VALIDATION_FAILED,
                    Map.of("fields", Map.of("title", "标题需为 1–40 字")));
        }
        LocalDateTime triggerUtc = request.triggerAt()
                .withOffsetSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
        String dedupeKey = "manual:" + UUID.randomUUID();
        reminderMapper.insert(ownerId, SOURCE_MANUAL, null, title, triggerUtc, 0, true, "pending",
                dedupeKey, now);
        ReminderRow row = reminderMapper.findByDedupe(ownerId, dedupeKey);
        if (row == null) {
            throw new BizException(ErrorCode.INTERNAL_ERROR);
        }
        return toView(row);
    }

    @Transactional
    public ReminderView setEnabled(Long ownerId, Long id, UpdateReminderRequest request) {
        ReminderRow row = requireRow(ownerId, id);
        if (row.getVersion() != request.version()) {
            throw new BizException(ErrorCode.VERSION_CONFLICT);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (reminderMapper.updateEnabled(ownerId, id, request.enabled(), request.version(), now) == 0) {
            throw new BizException(ErrorCode.VERSION_CONFLICT);
        }
        return toView(requireRow(ownerId, id));
    }

    @Transactional
    public void delete(Long ownerId, Long id, long version) {
        ReminderRow row = requireRow(ownerId, id);
        if (row.getVersion() != version) {
            throw new BizException(ErrorCode.VERSION_CONFLICT);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (reminderMapper.deleteById(ownerId, id, version) == 0) {
            throw new BizException(ErrorCode.VERSION_CONFLICT);
        }
        tombstoneService.record(ownerId, TombstoneService.REMINDER, id, version, now);
    }

    /** Due reminders for one device, honouring the global switch (plan 4.5). */
    public List<ReminderView> due(Long ownerId, String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            throw new BizException(ErrorCode.VALIDATION_FAILED,
                    Map.of("fields", Map.of("deviceId", "设备编号不能为空")));
        }
        UserSettingsRow settings = settingsMapper.findByOwner(ownerId);
        if (settings != null && !settings.isRemindersEnabled()) {
            return List.of();
        }
        Set<Long> sent = new HashSet<>(deliveryMapper.findSentReminderIds(ownerId, deviceId));
        LocalDateTime now = LocalDateTime.now(clock);
        return reminderMapper.findDue(ownerId, now).stream()
                .filter(row -> !sent.contains(row.getId()))
                .map(ReminderService::toView)
                .toList();
    }

    /**
     * Records one delivery attempt. A second failure marks the reminder failed;
     * any success wins. The unique key (reminder, device) is the dedupe rule.
     */
    @Transactional
    public ReminderView receipt(Long ownerId, Long id, ReminderReceiptRequest request, String headerDeviceId) {
        String deviceId = request.deviceId() == null || request.deviceId().isBlank()
                ? headerDeviceId : request.deviceId().strip();
        if (deviceId == null || deviceId.isBlank()) {
            throw new BizException(ErrorCode.VALIDATION_FAILED,
                    Map.of("fields", Map.of("deviceId", "设备编号不能为空")));
        }
        String result = request.result() == null ? null : request.result().strip();
        if (result == null || !RESULTS.contains(result)) {
            throw new BizException(ErrorCode.VALIDATION_FAILED,
                    Map.of("fields", Map.of("result", "结果只能是 sent 或 failed")));
        }
        requireRow(ownerId, id);

        LocalDateTime now = LocalDateTime.now(clock);
        deliveryMapper.upsert(ownerId, id, deviceId, result, now);
        ReminderDeliveryRow delivery = deliveryMapper.find(ownerId, id, deviceId);
        if ("sent".equals(result)) {
            reminderMapper.markSent(ownerId, id, now);
        } else if (delivery != null && delivery.getAttemptCount() >= MAX_RETRY_ATTEMPTS) {
            reminderMapper.markFailed(ownerId, id, now);
        }
        return toView(requireRow(ownerId, id));
    }

    private ReminderRow requireRow(Long ownerId, Long id) {
        ReminderRow row = reminderMapper.findById(ownerId, id);
        if (row == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return row;
    }

    public static ReminderView toView(ReminderRow row) {
        return new ReminderView(
                row.getId(),
                row.getSource(),
                row.getSourceId(),
                row.getTitle(),
                utc(row.getTriggerAt()),
                row.getAdvanceMinutes(),
                row.isEnabled(),
                row.getDeliveryStatus(),
                utc(row.getSentAt()),
                utc(row.getFailedAt()),
                row.getVersion(),
                utc(row.getUpdatedAt()));
    }

    private static OffsetDateTime utc(LocalDateTime value) {
        return value == null ? null : OffsetDateTime.of(value, ZoneOffset.UTC);
    }

    public record GeneratedSync(int created, int updated, int removed, int skippedPast) {

        public String notice() {
            return skippedPast > 0 ? "部分提醒时间已过去，未生成对应提醒" : null;
        }
    }
}
