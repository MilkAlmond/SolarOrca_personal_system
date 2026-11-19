package com.yujing.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Audit rows are written after the business transaction commits, so an audit
 * row never references an uncommitted user. Failures are logged but never block
 * the business operation. Passwords and note bodies are never passed here.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditWriter writer;

    public AuditService(AuditWriter writer) {
        this.writer = writer;
    }

    public void record(Long actorUserId, String action, String target, String result, String ip, String detail) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    safeWrite(actorUserId, action, target, result, ip, detail);
                }
            });
        } else {
            safeWrite(actorUserId, action, target, result, ip, detail);
        }
    }

    private void safeWrite(Long actorUserId, String action, String target, String result, String ip, String detail) {
        try {
            writer.write(actorUserId, action, target, result, ip, detail);
        } catch (Exception ex) {
            log.warn("Audit write failed for action {}", action, ex);
        }
    }
}
