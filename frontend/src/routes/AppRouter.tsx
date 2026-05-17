import { lazy } from 'react';
import { createBrowserRouter, Navigate } from 'react-router-dom';

import { LoginPage } from '@/features/auth/LoginPage';
import { AppLayout } from '@/components/layout/AppLayout';
import { ProtectedRoute } from './ProtectedRoute';
import { RoleGuard } from './RoleGuard';
import { DashboardPage } from '@/features/dashboard/DashboardPage';

// Rotas pesadas (Recharts, react-hook-form, formulários grandes) ficam fora
// do bundle principal. Login + Dashboard ficam eager por serem a 1ª tela após
// abrir o app e estarem no caminho crítico de TTI (Time To Interactive).
const FiliaisPage = lazy(() => import('@/features/cadastros/filiais/FiliaisPage').then((m) => ({ default: m.FiliaisPage })));
const CargaInicialPage = lazy(() => import('@/features/cadastros/filiais/CargaInicialPage').then((m) => ({ default: m.CargaInicialPage })));
const InsumosPage = lazy(() => import('@/features/cadastros/insumos/InsumosPage').then((m) => ({ default: m.InsumosPage })));
const FornecedoresPage = lazy(() => import('@/features/cadastros/fornecedores/FornecedoresPage').then((m) => ({ default: m.FornecedoresPage })));
const ProdutosPage = lazy(() => import('@/features/cadastros/produtos/ProdutosPage').then((m) => ({ default: m.ProdutosPage })));
const FichasTecnicasPage = lazy(() => import('@/features/receitas/FichasTecnicasPage').then((m) => ({ default: m.FichasTecnicasPage })));
const EstoquePage = lazy(() => import('@/features/operacoes/EstoquePage').then((m) => ({ default: m.EstoquePage })));
const MovimentacoesPage = lazy(() => import('@/features/operacoes/MovimentacoesPage').then((m) => ({ default: m.MovimentacoesPage })));
const TransferenciasPage = lazy(() => import('@/features/operacoes/TransferenciasPage').then((m) => ({ default: m.TransferenciasPage })));
const AlertasPage = lazy(() => import('@/features/alertas/AlertasPage').then((m) => ({ default: m.AlertasPage })));
const RelatoriosPage = lazy(() => import('@/features/relatorios/RelatoriosPage').then((m) => ({ default: m.RelatoriosPage })));
const NotificacoesPage = lazy(() => import('@/features/notificacoes/NotificacoesPage').then((m) => ({ default: m.NotificacoesPage })));
const CategoriasPage = lazy(() => import('@/features/admin/categorias/CategoriasPage').then((m) => ({ default: m.CategoriasPage })));
const NotasFiscaisPage = lazy(() => import('@/features/operacoes/notas-fiscais/NotasFiscaisPage').then((m) => ({ default: m.NotasFiscaisPage })));
const LancarNotaFiscalPage = lazy(() => import('@/features/operacoes/notas-fiscais/LancarNotaFiscalPage').then((m) => ({ default: m.LancarNotaFiscalPage })));
const UnidadesPage = lazy(() => import('@/features/admin/unidades/UnidadesPage').then((m) => ({ default: m.UnidadesPage })));
const EmpresasPage = lazy(() => import('@/features/admin/empresas/EmpresasPage').then((m) => ({ default: m.EmpresasPage })));
const UsuariosPage = lazy(() => import('@/features/admin/usuarios/UsuariosPage').then((m) => ({ default: m.UsuariosPage })));
const VendasPage = lazy(() => import('@/features/vendas/VendasPage').then((m) => ({ default: m.VendasPage })));
const CredenciaisPage = lazy(() => import('@/features/canais/CredenciaisPage').then((m) => ({ default: m.CredenciaisPage })));
const DeparaPage = lazy(() => import('@/features/canais/DeparaPage').then((m) => ({ default: m.DeparaPage })));
const PedidosCanaisPage = lazy(() => import('@/features/canais/PedidosCanaisPage').then((m) => ({ default: m.PedidosCanaisPage })));

// Visibilidade espelhando o Sidebar (T-RBAC-01):
//   ADMIN     → tudo
//   GERENTE   → Operacional + Cadastros
//   OPERADOR  → só Operacional (sem Cadastros, sem Administração)
//   CONSULTA  → só Dashboard + Relatórios
const OPERACIONAL = ['ADMIN', 'GERENTE', 'OPERADOR'];
const CADASTROS = ['ADMIN', 'GERENTE'];

function guard(allow: string[], element: React.ReactNode) {
  return <RoleGuard allow={allow}>{element}</RoleGuard>;
}

export const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  {
    path: '/',
    element: (
      <ProtectedRoute>
        <AppLayout />
      </ProtectedRoute>
    ),
    children: [
      { index: true, element: <Navigate to="/dashboard" replace /> },
      // Operacional — visíveis para CONSULTA apenas: Dashboard + Relatórios
      { path: 'dashboard', element: <DashboardPage /> },
      { path: 'relatorios', element: <RelatoriosPage /> },
      { path: 'notas-fiscais', element: guard(OPERACIONAL, <NotasFiscaisPage />) },
      { path: 'notas-fiscais/lancar', element: guard(OPERACIONAL, <LancarNotaFiscalPage />) },
      { path: 'estoque', element: guard(OPERACIONAL, <EstoquePage />) },
      { path: 'vendas', element: guard(OPERACIONAL, <VendasPage />) },
      { path: 'canais/pedidos', element: guard(OPERACIONAL, <PedidosCanaisPage />) },
      { path: 'alertas', element: guard(OPERACIONAL, <AlertasPage />) },
      // Cadastros — bloqueado para OPERADOR e CONSULTA
      { path: 'insumos', element: guard(CADASTROS, <InsumosPage />) },
      { path: 'fichas-tecnicas', element: guard(CADASTROS, <FichasTecnicasPage />) },
      { path: 'produtos', element: guard(CADASTROS, <ProdutosPage />) },
      { path: 'transferencias', element: guard(CADASTROS, <TransferenciasPage />) },
      { path: 'movimentacoes', element: guard(CADASTROS, <MovimentacoesPage />) },
      // Administração — só ADMIN (com algumas exceções p/ GERENTE)
      { path: 'filiais', element: guard(['ADMIN'], <FiliaisPage />) },
      { path: 'filiais/:id/carga-inicial', element: guard(['ADMIN'], <CargaInicialPage />) },
      { path: 'fornecedores', element: guard(CADASTROS, <FornecedoresPage />) },
      { path: 'admin/categorias', element: guard(CADASTROS, <CategoriasPage />) },
      { path: 'admin/unidades', element: guard(CADASTROS, <UnidadesPage />) },
      { path: 'admin/empresas', element: guard(['ADMIN'], <EmpresasPage />) },
      { path: 'admin/usuarios', element: guard(CADASTROS, <UsuariosPage />) },
      { path: 'admin/canais/credenciais', element: guard(['ADMIN'], <CredenciaisPage />) },
      { path: 'admin/canais/depara', element: guard(['ADMIN'], <DeparaPage />) },
      // Notificações: acessível a todos autenticados (sem gating)
      { path: 'notificacoes', element: <NotificacoesPage /> },
    ],
  },
  { path: '*', element: <Navigate to="/dashboard" replace /> },
]);
