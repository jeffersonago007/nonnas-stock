import { create } from 'zustand';

import { tokenStorage } from '@/lib/tokenStorage';

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
  logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  token: tokenStorage.get(),
  user: null,
  setSession: (token, user) => {
    tokenStorage.set(token);
    set({ token, user });
  },
  logout: () => {
    tokenStorage.clear();
    set({ token: null, user: null });
  },
}));
