import { useEffect } from 'react';
import { Navigate } from 'react-router-dom';
import { toast } from 'sonner';

import { useAuthStore } from '@/features/auth/store';

interface Props {
  allow: string[];
  children: React.ReactNode;
}

export function hasAnyRole(perfil: string | null | undefined, allow: string[]): boolean {
  if (!perfil) return false;
  return allow.includes(perfil);
}

/**
 * Bloqueia acesso à rota se o perfil do usuário autenticado não estiver em
 * `allow`. Redireciona para /dashboard com toast informativo. Use em conjunto
 * com `ProtectedRoute` (que cobre o caso de não-autenticado).
 */
export function RoleGuard({ allow, children }: Props) {
  const user = useAuthStore((s) => s.user);
  const permitted = hasAnyRole(user?.perfil, allow);

  useEffect(() => {
    if (user && !permitted) {
      toast.error('Você não tem permissão para acessar esta área.');
    }
  }, [user, permitted]);

  if (user && !permitted) {
    return <Navigate to="/dashboard" replace />;
  }
  return <>{children}</>;
}
