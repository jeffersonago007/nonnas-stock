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

export function extractProblem(error: unknown): ProblemDetail | null {
  if (axios.isAxiosError(error) && error.response?.data) {
    return error.response.data as ProblemDetail;
  }
  return null;
}
