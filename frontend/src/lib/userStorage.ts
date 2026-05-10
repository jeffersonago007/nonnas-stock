import type { AuthUser } from '@/features/auth/store';

/*
 * Wrapper sobre localStorage para os dados do usuário autenticado (id, nome,
 * e-mail, perfil, filial). Espelha o `tokenStorage` para permitir reidratação
 * do `useAuthStore` após F5/recarregamento, evitando que o sidebar perca
 * gating de role enquanto o token ainda é válido.
 *
 * NOTA SEGURANÇA — TODO T16: junto com o token, este storage migra para
 * httpOnly cookies + endpoint `/api/v1/auth/me` quando o hardening estiver
 * concluído. Manter o wrapper como única superfície facilita essa troca.
 */
const USER_KEY = 'nonnas.user';

export const userStorage = {
  get(): AuthUser | null {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as AuthUser;
    } catch {
      localStorage.removeItem(USER_KEY);
      return null;
    }
  },
  set(user: AuthUser): void {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  },
  clear(): void {
    localStorage.removeItem(USER_KEY);
  },
};
