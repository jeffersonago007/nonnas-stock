import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
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
  FilePlus2,
  BookOpen,
  Receipt,
  KeyRound,
  Link as LinkIcon,
  LogOut,
} from 'lucide-react';

import {
  CommandDialog,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
  CommandShortcut,
} from '@/components/ui/command';
import { Moto } from '@/components/icons/Moto';
import { useAuthStore } from '@/features/auth/store';
import { hasAnyRole } from '@/routes/RoleGuard';

type Entry = {
  label: string;
  to?: string;
  action?: () => void;
  icon: typeof LayoutDashboard;
  group: 'Navegação' | 'Atalhos';
  allow?: string[];
  keywords?: string[];
};

export function CommandPalette() {
  const [open, setOpen] = useState(false);
  const navigate = useNavigate();
  const perfil = useAuthStore((s) => s.user?.perfil);
  const logout = useAuthStore((s) => s.logout);

  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if ((e.key === 'k' || e.key === 'K') && (e.metaKey || e.ctrlKey)) {
        e.preventDefault();
        setOpen((o) => !o);
      }
    }
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, []);

  const entries: Entry[] = [
    // Navegação — espelha o Sidebar
    { label: 'Dashboard', to: '/dashboard', icon: LayoutDashboard, group: 'Navegação' },
    { label: 'Estoque', to: '/estoque', icon: Boxes, group: 'Navegação', allow: ['ADMIN', 'GERENTE', 'OPERADOR'], keywords: ['saldo', 'posição'] },
    { label: 'Notas fiscais', to: '/notas-fiscais', icon: FileText, group: 'Navegação', allow: ['ADMIN', 'GERENTE', 'OPERADOR'], keywords: ['nfe', 'nota'] },
    { label: 'Saídas / Vendas', to: '/vendas', icon: Receipt, group: 'Navegação', allow: ['ADMIN', 'GERENTE', 'OPERADOR'], keywords: ['pdv', 'pos', 'caixa'] },
    { label: 'Deliveries', to: '/canais/pedidos', icon: Moto, group: 'Navegação', allow: ['ADMIN', 'GERENTE', 'OPERADOR'], keywords: ['ifood', 'canal', '99food', 'keeta'] },
    { label: 'Relatórios', to: '/relatorios', icon: BarChart3, group: 'Navegação', keywords: ['curva abc', 'ruptura', 'vencimento'] },
    { label: 'Alertas', to: '/alertas', icon: Bell, group: 'Navegação', allow: ['ADMIN', 'GERENTE', 'OPERADOR'] },
    { label: 'Notificações', to: '/notificacoes', icon: Bell, group: 'Navegação' },
    { label: 'Produtos / Insumos', to: '/insumos', icon: ShoppingBasket, group: 'Navegação', allow: ['ADMIN', 'GERENTE'] },
    { label: 'Fichas técnicas', to: '/fichas-tecnicas', icon: ClipboardList, group: 'Navegação', allow: ['ADMIN', 'GERENTE'], keywords: ['receita'] },
    { label: 'Cardápio', to: '/produtos', icon: BookOpen, group: 'Navegação', allow: ['ADMIN', 'GERENTE'] },
    { label: 'Transferências', to: '/transferencias', icon: ArrowLeftRight, group: 'Navegação', allow: ['ADMIN', 'GERENTE'] },
    { label: 'Movimentações', to: '/movimentacoes', icon: History, group: 'Navegação', allow: ['ADMIN', 'GERENTE'] },
    { label: 'Filiais', to: '/filiais', icon: Building2, group: 'Navegação', allow: ['ADMIN'] },
    { label: 'Fornecedores', to: '/fornecedores', icon: Truck, group: 'Navegação', allow: ['ADMIN', 'GERENTE'] },
    { label: 'Categorias', to: '/admin/categorias', icon: Tag, group: 'Navegação', allow: ['ADMIN', 'GERENTE'] },
    { label: 'Unidades de medida', to: '/admin/unidades', icon: Ruler, group: 'Navegação', allow: ['ADMIN', 'GERENTE'] },
    { label: 'Empresas', to: '/admin/empresas', icon: Building, group: 'Navegação', allow: ['ADMIN'] },
    { label: 'Usuários', to: '/admin/usuarios', icon: Users, group: 'Navegação', allow: ['ADMIN', 'GERENTE'] },
    { label: 'Canais — credenciais', to: '/admin/canais/credenciais', icon: KeyRound, group: 'Navegação', allow: ['ADMIN'] },
    { label: 'Canais — de-para', to: '/admin/canais/depara', icon: LinkIcon, group: 'Navegação', allow: ['ADMIN'] },
    // Atalhos
    { label: 'Lançar nota fiscal', to: '/notas-fiscais/lancar', icon: FilePlus2, group: 'Atalhos', allow: ['ADMIN', 'GERENTE', 'OPERADOR'], keywords: ['nfe', 'criar', 'nova'] },
    { label: 'Sair (logout)', action: () => void logout(), icon: LogOut, group: 'Atalhos' },
  ];

  const visiveis = entries.filter((e) => !e.allow || hasAnyRole(perfil, e.allow));
  const grupos = ['Navegação', 'Atalhos'] as const;

  function run(entry: Entry) {
    setOpen(false);
    if (entry.to) navigate(entry.to);
    else entry.action?.();
  }

  return (
    <CommandDialog open={open} onOpenChange={setOpen}>
      <CommandInput placeholder="Pesquise páginas, atalhos…  (Ctrl+K)" />
      <CommandList>
        <CommandEmpty>Nenhum resultado.</CommandEmpty>
        {grupos.map((g) => {
          const items = visiveis.filter((e) => e.group === g);
          if (items.length === 0) return null;
          return (
            <CommandGroup key={g} heading={g}>
              {items.map((e) => {
                const Icon = e.icon;
                const value = `${e.label} ${(e.keywords ?? []).join(' ')}`.toLowerCase();
                return (
                  <CommandItem key={e.label} value={value} onSelect={() => run(e)}>
                    <Icon className="text-muted-foreground" />
                    <span>{e.label}</span>
                    {e.to && <CommandShortcut>{e.to}</CommandShortcut>}
                  </CommandItem>
                );
              })}
            </CommandGroup>
          );
        })}
      </CommandList>
    </CommandDialog>
  );
}
