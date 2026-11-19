package com.yujing.audit;

import com.yujing.audit.mapper.AuditLogMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
public class AuditWriter {

    private final AuditLogMapper mapper;
    private final Clock clock;

    public AuditWriter(AuditLogMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(Long actorUserId, String action, String target, String result, String ip, String detail) {
        mapper.insert(actorUserId, action, target, result, ip, detail, LocalDateTime.now(clock));
    }
}
