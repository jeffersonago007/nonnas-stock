import { create } from 'zustand';

import { tokenStorage } from '@/lib/tokenStorage';
import { userStorage } from '@/lib/userStorage';
import { logoutBackend } from './api';

export interface AuthUser {
  id: string;
  nome: string;
  email: string;
  perfil: string;
  filialId?: string | null;
}

interface AuthState {
  token: string | null;
  user: AuthUser | null;
  setSession: (token: string, user: AuthUser) => void;
  logout: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set) => ({
  token: tokenStorage.get(),
  user: userStorage.get(),
  setSession: (token, user) => {
    tokenStorage.set(token);
    userStorage.set(user);
    set({ token, user });
  },
  logout: async () => {
    // Revoga no backend antes de limpar local — o token precisa estar válido
    // nesse momento. Falhas no backend são silenciosas (ver logoutBackend).
    await logoutBackend();
    tokenStorage.clear();
    userStorage.clear();
    set({ token: null, user: null });
  },
}));

// Hooks utilitários para gating por perfil/filial (T-RBAC-01).
export const useIsAdmin = (): boolean =>
  useAuthStore((s) => s.user?.perfil === 'ADMIN');

export const useCurrentFilialId = (): string | null =>
  useAuthStore((s) => s.user?.filialId ?? null);
