package com.yujing.note;

import com.yujing.common.api.PageData;
import com.yujing.common.error.BizException;
import com.yujing.common.error.ErrorCode;
import com.yujing.note.dto.NoteRequest;
import com.yujing.note.dto.NoteSummary;
import com.yujing.note.dto.NoteView;
import com.yujing.note.mapper.NoteMapper;
import com.yujing.note.model.NoteRow;
import com.yujing.sync.TombstoneService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class NoteService {

    private static final int MAX_TITLE = 60;
    private static final int MAX_BODY = 10000;
    private static final int MAX_CATEGORY = 20;

    private final NoteMapper noteMapper;
    private final TombstoneService tombstoneService;
    private final Clock clock;

    public NoteService(NoteMapper noteMapper, TombstoneService tombstoneService, Clock clock) {
        this.noteMapper = noteMapper;
        this.tombstoneService = tombstoneService;
        this.clock = clock;
    }

    public PageData<NoteSummary> list(Long ownerId, String keyword, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String safeKeyword = trim(keyword);
        List<NoteSummary> items = noteMapper
                .findPage(ownerId, safeKeyword, safeSize, (long) (safePage - 1) * safeSize)
                .stream()
                .map(NoteService::toSummary)
                .toList();
        return PageData.of(items, noteMapper.count(ownerId, safeKeyword), safePage, safeSize);
    }

    public NoteView get(Long ownerId, Long id) {
        return toView(require(ownerId, id));
    }

    @Transactional
    public NoteView create(Long ownerId, NoteRequest request) {
        NoteRow row = new NoteRow();
        row.setOwnerId(ownerId);
        apply(request, row);
        LocalDateTime now = LocalDateTime.now(clock);
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        noteMapper.insert(row);
        return toView(row);
    }

    @Transactional
    public NoteView update(Long ownerId, Long id, NoteRequest request) {
        NoteRow existing = require(ownerId, id);
        long expected = request.version() == null ? -1 : request.version();
        if (expected != existing.getVersion()) {
            throw new BizException(ErrorCode.VERSION_CONFLICT);
        }
        NoteRow row = new NoteRow();
        row.setId(id);
        row.setOwnerId(ownerId);
        row.setVersion(existing.getVersion());
        apply(request, row);
        row.setUpdatedAt(LocalDateTime.now(clock));
        if (noteMapper.update(row) == 0) {
            throw new BizException(ErrorCode.VERSION_CONFLICT);
        }
        return toView(require(ownerId, id));
    }

    @Transactional
    public void delete(Long ownerId, Long id, long version) {
        NoteRow row = require(ownerId, id);
        if (row.getVersion() != version) {
            throw new BizException(ErrorCode.VERSION_CONFLICT);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (noteMapper.delete(ownerId, id, version) == 0) {
            throw new BizException(ErrorCode.VERSION_CONFLICT);
        }
        tombstoneService.record(ownerId, TombstoneService.NOTE, id, version, now);
    }

    private NoteRow require(Long ownerId, Long id) {
        NoteRow row = noteMapper.findById(ownerId, id);
        if (row == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return row;
    }

    private void apply(NoteRequest request, NoteRow row) {
        Map<String, String> errors = new LinkedHashMap<>();

        String title = trim(request.title());
        if (title == null) {
            errors.put("title", "标题不能为空");
        } else if (codePoints(title) > MAX_TITLE) {
            errors.put("title", "标题不能超过 60 字");
        }

        String body = request.body();
        if (body != null && codePoints(body) > MAX_BODY) {
            errors.put("body", "正文不能超过 10000 字");
        }

        String category = trim(request.category());
        if (category != null && codePoints(category) > MAX_CATEGORY) {
            errors.put("category", "分类不能超过 20 字");
        }

        if (!errors.isEmpty()) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, Map.of("fields", errors));
        }

        row.setTitle(title);
        row.setBody(body == null || body.isBlank() ? null : body);
        row.setCategory(category);
    }

    private static NoteSummary toSummary(NoteRow row) {
        return new NoteSummary(
                row.getId(),
                row.getTitle(),
                row.getCategory(),
                row.getVersion(),
                OffsetDateTime.of(row.getCreatedAt(), ZoneOffset.UTC),
                OffsetDateTime.of(row.getUpdatedAt(), ZoneOffset.UTC));
    }

    private static NoteView toView(NoteRow row) {
        return new NoteView(
                row.getId(),
                row.getTitle(),
                row.getBody(),
                row.getCategory(),
                row.getVersion(),
                OffsetDateTime.of(row.getCreatedAt(), ZoneOffset.UTC),
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
