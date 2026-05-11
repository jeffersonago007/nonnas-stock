import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  Building2,
  Truck,
  ShoppingBasket,
  ClipboardList,
  Boxes,
  ArrowLeftRight,
  History,
  Bell,
  BarChart3,
  Tag,
  Ruler,
  Building,
  Users,
  FileText,
  BookOpen,
  Receipt,
} from 'lucide-react';

import { cn } from '@/lib/utils';
import { useAuthStore } from '@/features/auth/store';
import { hasAnyRole } from '@/routes/RoleGuard';

interface NavItem {
  to: string;
  label: string;
  icon: typeof LayoutDashboard;
  /** Roles permitidos. Vazio/undefined = qualquer autenticado. */
  allow?: string[];
}

// "Insumos" renomeado para "Produtos" pra alinhar com a linguagem do operador
// (no domínio interno, /insumos é matéria-prima — mas pro restaurante "produto"
// é tudo que entra/sai do estoque). A página /produtos antiga (produto vendável
// + ficha técnica) fica oculta nesta onda — quando reabrir, vai precisar de um
// nome distinto pra não conflitar (ex.: "Cardápio").
const navItems: NavItem[] = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/filiais', label: 'Filiais', icon: Building2 },
  { to: '/insumos', label: 'Produtos', icon: ShoppingBasket },
  { to: '/fornecedores', label: 'Fornecedores', icon: Truck },
  { to: '/produtos', label: 'Cardápio', icon: BookOpen },
  { to: '/fichas-tecnicas', label: 'Fichas técnicas', icon: ClipboardList },
  { to: '/estoque', label: 'Estoque', icon: Boxes },
  { to: '/vendas', label: 'Vendas', icon: Receipt },
  { to: '/movimentacoes', label: 'Movimentações', icon: History },
  { to: '/notas-fiscais', label: 'Notas fiscais', icon: FileText },
  { to: '/transferencias', label: 'Transferências', icon: ArrowLeftRight },
  { to: '/alertas', label: 'Alertas', icon: Bell },
  { to: '/relatorios', label: 'Relatórios', icon: BarChart3 },
];

const adminItems: NavItem[] = [
  { to: '/admin/categorias', label: 'Categorias', icon: Tag, allow: ['ADMIN', 'GERENTE'] },
  { to: '/admin/unidades', label: 'Un. medida', icon: Ruler, allow: ['ADMIN', 'GERENTE'] },
  { to: '/admin/empresas', label: 'Empresas', icon: Building, allow: ['ADMIN'] },
  { to: '/admin/usuarios', label: 'Usuários', icon: Users, allow: ['ADMIN', 'GERENTE'] },
];

export function Sidebar() {
  const perfil = useAuthStore((s) => s.user?.perfil);
  const adminVisible = adminItems.filter((item) => !item.allow || hasAnyRole(perfil, item.allow));

  return (
    <aside className="hidden w-60 flex-col border-r border-border bg-neutral-surface md:flex">
      <div className="flex h-36 items-center justify-center border-b border-border px-2">
        <img
          src="/logo-nonnas.png"
          alt="Nonnas Paola — Churrascaria & Pizzaria"
          className="h-32 w-auto object-contain mix-blend-multiply"
        />
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

        {adminVisible.length > 0 && (
          <>
            <div className="px-3 pt-4 pb-1 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              Administração
            </div>
            {adminVisible.map(({ to, label, icon: Icon }) => (
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
          </>
        )}
      </nav>
    </aside>
  );
}
