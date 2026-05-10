package com.nonnas.identity.application.auth;

import com.nonnas.identity.domain.Usuario;

/**
 * Resultado de {@link AutenticarUseCase#execute}: par de tokens emitidos +
 * o usuário autenticado, para que o controller possa devolver dados básicos
 * de identidade no mesmo response do login (frontend usa para popular sidebar
 * com role-gating, header com nome, etc.).
 */
public record LoginResult(TokenPair tokens, Usuario usuario) {}
