package com.nonnas.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
public class AuditLogEntity {

    @Id
    private UUID id;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_email")
    private String actorEmail;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "target_kind", length = 64)
    private String targetKind;

    @Column(name = "target_id")
    private UUID targetId;

    @Column(name = "request_ip", length = 64)
    private String requestIp;

    @Column(name = "request_ua", length = 512)
    private String requestUa;

    @Column(name = "metadata", columnDefinition = "text")
    private String metadata;

    public AuditLogEntity() {}

    public AuditLogEntity(UUID id, Instant occurredAt, UUID actorId, String actorEmail,
                          String eventType, String targetKind, UUID targetId,
                          String requestIp, String requestUa, String metadata) {
        this.id = id;
        this.occurredAt = occurredAt;
        this.actorId = actorId;
        this.actorEmail = actorEmail;
        this.eventType = eventType;
        this.targetKind = targetKind;
        this.targetId = targetId;
        this.requestIp = requestIp;
        this.requestUa = requestUa;
        this.metadata = metadata;
    }
}
