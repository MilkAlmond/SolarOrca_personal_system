package com.yujing.account;

import com.yujing.audit.AuditService;
import com.yujing.auth.mapper.UserMapper;
import com.yujing.auth.model.UserRow;
import com.yujing.file.FileStorage;
import com.yujing.file.mapper.FileMapper;
import com.yujing.file.model.FileRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Physically removes accounts whose 30-day freeze has elapsed. The users row is
 * deleted first (FKs cascade business rows, sessions, files rows and export
 * jobs); stored artefacts are removed afterwards, so a failure only leaves
 * orphans for the periodic cleanup.
 */
@Component
public class AccountPurgeJob {

    private static final Logger log = LoggerFactory.getLogger(AccountPurgeJob.class);

    private final UserMapper userMapper;
    private final FileMapper fileMapper;
    private final FileStorage storage;
    private final AuditService auditService;
    private final Clock clock;

    public AccountPurgeJob(UserMapper userMapper,
                           FileMapper fileMapper,
                           FileStorage storage,
                           AuditService auditService,
                           Clock clock) {
        this.userMapper = userMapper;
        this.fileMapper = fileMapper;
        this.storage = storage;
        this.auditService = auditService;
        this.clock = clock;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        purgeDueAccounts();
    }

    @Scheduled(cron = "0 40 3 * * *")
    public void scheduled() {
        purgeDueAccounts();
    }

    public void purgeDueAccounts() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<UserRow> due;
        try {
            due = userMapper.findDeletionDue(now, 20);
        } catch (RuntimeException ex) {
            log.warn("Could not scan due account deletions", ex);
            return;
        }
        for (UserRow user : due) {
            List<FileRow> files = fileMapper.listAllByOwner(user.getId());
            userMapper.deleteById(user.getId());
            for (FileRow file : files) {
                storage.deleteQuietly(file.getStoragePath());
            }
            auditService.record(null, "ACCOUNT_PURGE", "user:" + user.getId(), "success", null,
                    user.getUsername());
        }
    }
}
