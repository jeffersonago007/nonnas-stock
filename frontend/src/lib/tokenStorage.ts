/*
 * Wrapper sobre localStorage para JWT.
 *
 * NOTA SEGURANÇA — TODO T16: localStorage é vulnerável a XSS. O master doc
 * (seção 13) indica migração para httpOnly cookies + refresh rotation no
 * hardening de segurança (T16). Manter este wrapper como única superfície
 * de acesso ao token facilita essa troca: basta reescrever as 3 funções
 * abaixo para chamar /api/v1/auth/me ou ler de cookie sem mexer em
 * api.ts ou no store Zustand.
 */
const TOKEN_KEY = 'nonnas.token';

export const tokenStorage = {
  get(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  },
  set(token: string): void {
    localStorage.setItem(TOKEN_KEY, token);
  },
  clear(): void {
    localStorage.removeItem(TOKEN_KEY);
  },
};
