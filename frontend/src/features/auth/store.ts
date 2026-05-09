import { create } from 'zustand';

import { tokenStorage } from '@/lib/tokenStorage';
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
  user: null,
  setSession: (token, user) => {
    tokenStorage.set(token);
    set({ token, user });
  },
  logout: async () => {
    // Revoga no backend antes de limpar local — o token precisa estar válido
    // nesse momento. Falhas no backend são silenciosas (ver logoutBackend).
    await logoutBackend();
    tokenStorage.clear();
    set({ token: null, user: null });
  },
}));
