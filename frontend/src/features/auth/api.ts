import { api } from '@/lib/api';
import type { AuthUser } from './store';

export interface LoginRequest {
  email: string;
  senha: string;
}

export interface LoginResponse {
  accessToken: string;
  accessExpiresAt: string;
  refreshToken: string;
  refreshExpiresAt: string;
  tokenType: string;
  usuario: AuthUser;
}

export async function login(payload: LoginRequest): Promise<LoginResponse> {
  const { data } = await api.post<LoginResponse>('/auth/login', payload);
  return data;
}

/**
 * Encerra a sessão no backend (revoga o access token via blacklist e a
 * família de refresh tokens). Best-effort: erros aqui não devem bloquear o
 * logout client-side — o store sempre limpa o token local.
 */
export async function logoutBackend(refreshToken?: string | null): Promise<void> {
  try {
    await api.post('/auth/logout', refreshToken ? { refreshToken } : {});
  } catch {
    // Acesso já pode estar expirado/revogado; ignoramos para não impedir
    // o logout local.
  }
}
