import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  Building2,
  Package,
  Truck,
  ShoppingBasket,
  ClipboardList,
  Boxes,
  ArrowLeftRight,
  History,
  Bell,
  BarChart3,
} from 'lucide-react';

import { cn } from '@/lib/utils';

interface NavItem {
  to: string;
  label: string;
  icon: typeof LayoutDashboard;
}

const navItems: NavItem[] = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/filiais', label: 'Filiais', icon: Building2 },
  { to: '/insumos', label: 'Insumos', icon: Package },
  { to: '/fornecedores', label: 'Fornecedores', icon: Truck },
  { to: '/produtos', label: 'Produtos', icon: ShoppingBasket },
  { to: '/fichas-tecnicas', label: 'Fichas técnicas', icon: ClipboardList },
  { to: '/estoque', label: 'Estoque', icon: Boxes },
  { to: '/movimentacoes', label: 'Movimentações', icon: History },
  { to: '/transferencias', label: 'Transferências', icon: ArrowLeftRight },
  { to: '/alertas', label: 'Alertas', icon: Bell },
  { to: '/relatorios', label: 'Relatórios', icon: BarChart3 },
];

export function Sidebar() {
  return (
    <aside className="hidden w-60 flex-col border-r border-border bg-neutral-surface md:flex">
      <div className="flex h-16 items-center border-b border-border px-6">
        <span className="font-display text-xl text-primary">Nonnas</span>
      </div>
      <nav className="flex-1 space-y-1 overflow-y-auto p-3">
        {navItems.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              cn(
                'flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors',
                isActive
                  ? 'bg-primary text-primary-foreground'
                  : 'text-foreground hover:bg-muted',
              )
            }
          >
            <Icon className="h-4 w-4" />
            {label}
          </NavLink>
        ))}
      </nav>
    </aside>
  );
}
