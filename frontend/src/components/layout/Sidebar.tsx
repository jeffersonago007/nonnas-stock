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
  Smartphone,
  KeyRound,
  Link as LinkIcon,
} from 'lucide-react';

import { cn } from '@/lib/utils';
import { useAuthStore } from '@/features/auth/store';
import { hasAnyRole } from '@/routes/RoleGuard';

interface NavItem {
  to: string;
  label: string;
  icon: typeof LayoutDashboard;
  /** Roles permitidos para ver o item. Vazio/undefined = qualquer autenticado. */
  allow?: string[];
}

// Mapa de visibilidade por perfil (T-RBAC-01):
//   ADMIN     → vê tudo (Operacional + Cadastros + Administração)
//   GERENTE   → vê Operacional + Cadastros
//   OPERADOR  → só Operacional (sem Cadastros, sem Administração)
//   CONSULTA  → só Dashboard + Relatórios
//
// Ordem do bloco Operacional reflete frequência de uso do operador no dia-a-dia.
const navItemsOperacional: NavItem[] = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/notas-fiscais', label: 'Notas fiscais', icon: FileText, allow: ['ADMIN', 'GERENTE', 'OPERADOR'] },
  { to: '/estoque', label: 'Estoque', icon: Boxes, allow: ['ADMIN', 'GERENTE', 'OPERADOR'] },
  { to: '/vendas', label: 'Saídas', icon: Receipt, allow: ['ADMIN', 'GERENTE', 'OPERADOR'] },
  { to: '/canais/pedidos', label: 'Deliveries', icon: Smartphone, allow: ['ADMIN', 'GERENTE', 'OPERADOR'] },
  { to: '/relatorios', label: 'Relatórios', icon: BarChart3 },
  { to: '/alertas', label: 'Alertas', icon: Bell, allow: ['ADMIN', 'GERENTE', 'OPERADOR'] },
];

const navItemsCadastros: NavItem[] = [
  { to: '/insumos', label: 'Produtos', icon: ShoppingBasket, allow: ['ADMIN', 'GERENTE'] },
  { to: '/fichas-tecnicas', label: 'Fichas técnicas', icon: ClipboardList, allow: ['ADMIN', 'GERENTE'] },
  { to: '/produtos', label: 'Cardápio', icon: BookOpen, allow: ['ADMIN', 'GERENTE'] },
  { to: '/transferencias', label: 'Transferências', icon: ArrowLeftRight, allow: ['ADMIN', 'GERENTE'] },
  { to: '/movimentacoes', label: 'Movimentações', icon: History, allow: ['ADMIN', 'GERENTE'] },
];

const adminItems: NavItem[] = [
  { to: '/filiais', label: 'Filiais', icon: Building2, allow: ['ADMIN'] },
  { to: '/fornecedores', label: 'Fornecedores', icon: Truck, allow: ['ADMIN', 'GERENTE'] },
  { to: '/admin/categorias', label: 'Categorias', icon: Tag, allow: ['ADMIN', 'GERENTE'] },
  { to: '/admin/unidades', label: 'Un. medida', icon: Ruler, allow: ['ADMIN', 'GERENTE'] },
  { to: '/admin/empresas', label: 'Empresas', icon: Building, allow: ['ADMIN'] },
  { to: '/admin/usuarios', label: 'Usuários', icon: Users, allow: ['ADMIN', 'GERENTE'] },
  { to: '/admin/canais/credenciais', label: 'Canais — credenciais', icon: KeyRound, allow: ['ADMIN'] },
  { to: '/admin/canais/depara', label: 'Canais — de-para', icon: LinkIcon, allow: ['ADMIN'] },
];

function visibleFor(perfil: string | null | undefined, items: NavItem[]): NavItem[] {
  return items.filter((item) => !item.allow || hasAnyRole(perfil, item.allow));
}

export function Sidebar() {
  const perfil = useAuthStore((s) => s.user?.perfil);
  const operacionalVisible = visibleFor(perfil, navItemsOperacional);
  const cadastrosVisible = visibleFor(perfil, navItemsCadastros);
  const adminVisible = visibleFor(perfil, adminItems);

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
        {operacionalVisible.map(({ to, label, icon: Icon }) => (
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

        {cadastrosVisible.length > 0 && (
          <>
            <div className="my-2 border-t border-border/60" />
            {cadastrosVisible.map(({ to, label, icon: Icon }) => (
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
