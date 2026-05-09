package com.nonnas.alerts.domain;

public enum StatusAlerta {
    ATIVO,
    RESOLVIDO_AUTO,
    RESOLVIDO_MANUAL;

    public boolean isAtivo()     { return this == ATIVO; }
    public boolean isResolvido() { return this == RESOLVIDO_AUTO || this == RESOLVIDO_MANUAL; }
}
