package com.nonnas.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface SpringDataAuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {
}
