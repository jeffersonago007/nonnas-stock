import { LogOut } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { useAuthStore } from '@/features/auth/store';

export function Header() {
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);

  return (
    <header className="flex h-16 items-center justify-between border-b border-border bg-neutral-surface px-6">
      <div className="text-sm text-muted-foreground">
        {user ? `Olá, ${user.nome}` : 'Sessão ativa'}
      </div>
      <Button variant="ghost" size="sm" onClick={logout}>
        <LogOut className="h-4 w-4" />
        Sair
      </Button>
    </header>
  );
}
