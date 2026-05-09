package com.nonnas.identity.application.ports;

import com.nonnas.identity.application.audit.AuditEvent;

public interface AuditLogPort {
    void record(AuditEvent event);
}
