import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';

import { tokenStorage } from './tokenStorage';

export const api = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
  timeout: 15_000,
});

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = tokenStorage.get();
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`);
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      tokenStorage.clear();
      // Force a hard navigation: simpler than wiring the router into the
      // axios layer, and guarantees the in-memory Zustand state resets.
      if (window.location.pathname !== '/login') {
        window.location.assign('/login');
      }
    }
    return Promise.reject(error);
  },
);

export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  errors?: Record<string, string>;
}

/**
 * Extrai mensagem de erro do response. Cobre 3 formatos:
 * 1. RFC 7807 Problem Details (nosso GlobalExceptionHandler) — usa direto.
 * 2. Default error attributes do Spring `{timestamp, status, error, path}`
 *    — vem quando o handler não interceptou (ex.: erro raw 500). Mapeia
 *    `error` do Spring → `title` do nosso shape, e infere `detail`.
 * 3. Erro de rede/timeout sem response — devolve fallback genérico com `title`.
 *
 * Permissivo de propósito: pequenas mudanças de versão do axios não devem
 * cortar a mensagem para o usuário.
 */
export function extractProblem(error: unknown): ProblemDetail | null {
  if (!error || typeof error !== 'object') return null;

  // axios error com response — caminho mais comum
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const e = error as any;
  const data = e.response?.data;
  const status: number | undefined = e.response?.status;

  if (data && typeof data === 'object') {
    // RFC 7807 (campos title/detail/type)
    if ('detail' in data || 'title' in data || 'type' in data) {
      return data as ProblemDetail;
    }
    // Spring default `{timestamp, status, error, path, message?}`
    if ('error' in data || 'timestamp' in data) {
      return {
        title: data.error ?? `Erro HTTP ${status ?? '?'}`,
        detail: data.message ?? data.path ?? `Status ${status ?? '?'} sem detalhe do servidor`,
        status: data.status ?? status,
      };
    }
  }

  // Sem response (network/timeout)
  if (e.message && !e.response) {
    return {
      title: 'Sem conexão com o servidor',
      detail: e.message,
    };
  }

  // Última tentativa: pelo menos o status
  if (status != null) {
    return { title: `Erro HTTP ${status}`, status };
  }

  return null;
}
