import { LogOut } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { useAuthStore } from '@/features/auth/store';
import { FilialFiltro } from './FilialFiltro';
import { NotificacoesBadge } from './NotificacoesBadge';

export function Header() {
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);

  return (
    <header className="flex h-16 items-center justify-between gap-4 border-b border-border bg-neutral-surface px-6">
      <FilialFiltro />
      <div className="flex items-center gap-3">
        <NotificacoesBadge />
        <span className="text-sm text-muted-foreground">
          {user ? `Olá, ${user.nome}` : 'Sessão ativa'}
        </span>
        <Button variant="ghost" size="sm" onClick={logout}>
          <LogOut className="h-4 w-4" />
          Sair
        </Button>
      </div>
    </header>
  );
}
