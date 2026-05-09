package com.nonnas.identity.infrastructure.persistence;

import com.nonnas.identity.application.audit.AuditEvent;
import com.nonnas.identity.application.ports.AuditLogPort;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
class AuditLogPortImpl implements AuditLogPort {

    private final SpringDataAuditLogRepository jpa;

    AuditLogPortImpl(SpringDataAuditLogRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void record(AuditEvent event) {
        jpa.save(new AuditLogEntity(
                UUID.randomUUID(),
                event.occurredAt(),
                event.actorId(),
                event.actorEmail(),
                event.eventType(),
                event.targetKind(),
                event.targetId(),
                event.requestIp(),
                event.requestUserAgent(),
                event.metadataJson()));
    }
}
