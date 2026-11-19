package com.yujing.sync;

import com.yujing.sync.dto.TombstoneView;
import com.yujing.sync.mapper.SyncTombstoneMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Records a 30-day delete marker after the business row is physically removed
 * (plan 5.4, 6.3). Callers pass the version the row had when it was deleted.
 */
@Service
public class TombstoneService {

    public static final String BIRTHDAY = "birthday";
    public static final String COURSE = "course";
    public static final String TASK = "task";
    public static final String NOTE = "note";
    public static final String REMINDER = "reminder";

    private final SyncTombstoneMapper mapper;

    public TombstoneService(SyncTombstoneMapper mapper) {
        this.mapper = mapper;
    }

    public void record(Long ownerId, String entityType, Long entityId, long entityVersion, LocalDateTime now) {
        mapper.upsert(ownerId, entityType, entityId, entityVersion, now);
    }

    public List<TombstoneView> changes(Long ownerId, String entityType, LocalDateTime since) {
        return mapper.findSinceType(ownerId, entityType, since).stream()
                .map(row -> new TombstoneView(row.getEntityType(), row.getEntityId(),
                        row.getEntityVersion(),
                        OffsetDateTime.of(row.getDeletedAt(), ZoneOffset.UTC)))
                .toList();
    }

    public List<TombstoneView> changes(Long ownerId, LocalDateTime since) {
        return mapper.findSince(ownerId, since).stream()
                .map(row -> new TombstoneView(row.getEntityType(), row.getEntityId(),
                        row.getEntityVersion(),
                        OffsetDateTime.of(row.getDeletedAt(), ZoneOffset.UTC)))
                .toList();
    }
}
