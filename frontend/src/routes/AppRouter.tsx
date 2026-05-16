import { createBrowserRouter, Navigate } from 'react-router-dom';

import { LoginPage } from '@/features/auth/LoginPage';
import { AppLayout } from '@/components/layout/AppLayout';
import { ProtectedRoute } from './ProtectedRoute';
import { RoleGuard } from './RoleGuard';
import { DashboardPage } from '@/features/dashboard/DashboardPage';
import { FiliaisPage } from '@/features/cadastros/filiais/FiliaisPage';
import { CargaInicialPage } from '@/features/cadastros/filiais/CargaInicialPage';
import { InsumosPage } from '@/features/cadastros/insumos/InsumosPage';
import { FornecedoresPage } from '@/features/cadastros/fornecedores/FornecedoresPage';
import { ProdutosPage } from '@/features/cadastros/produtos/ProdutosPage';
import { FichasTecnicasPage } from '@/features/receitas/FichasTecnicasPage';
import { EstoquePage } from '@/features/operacoes/EstoquePage';
import { MovimentacoesPage } from '@/features/operacoes/MovimentacoesPage';
import { TransferenciasPage } from '@/features/operacoes/TransferenciasPage';
import { AlertasPage } from '@/features/alertas/AlertasPage';
import { RelatoriosPage } from '@/features/relatorios/RelatoriosPage';
import { NotificacoesPage } from '@/features/notificacoes/NotificacoesPage';
import { CategoriasPage } from '@/features/admin/categorias/CategoriasPage';
import { NotasFiscaisPage } from '@/features/operacoes/notas-fiscais/NotasFiscaisPage';
import { LancarNotaFiscalPage } from '@/features/operacoes/notas-fiscais/LancarNotaFiscalPage';
import { UnidadesPage } from '@/features/admin/unidades/UnidadesPage';
import { EmpresasPage } from '@/features/admin/empresas/EmpresasPage';
import { UsuariosPage } from '@/features/admin/usuarios/UsuariosPage';
import { VendasPage } from '@/features/vendas/VendasPage';
import { CredenciaisPage } from '@/features/canais/CredenciaisPage';
import { DeparaPage } from '@/features/canais/DeparaPage';
import { PedidosCanaisPage } from '@/features/canais/PedidosCanaisPage';

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
