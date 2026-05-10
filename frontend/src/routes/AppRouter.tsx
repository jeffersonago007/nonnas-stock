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
      { path: 'dashboard', element: <DashboardPage /> },
      { path: 'filiais', element: <FiliaisPage /> },
      { path: 'filiais/:id/carga-inicial', element: <CargaInicialPage /> },
      { path: 'insumos', element: <InsumosPage /> },
      { path: 'fornecedores', element: <FornecedoresPage /> },
      { path: 'produtos', element: <ProdutosPage /> },
      { path: 'fichas-tecnicas', element: <FichasTecnicasPage /> },
      { path: 'estoque', element: <EstoquePage /> },
      { path: 'movimentacoes', element: <MovimentacoesPage /> },
      { path: 'notas-fiscais', element: <NotasFiscaisPage /> },
      { path: 'notas-fiscais/lancar', element: <LancarNotaFiscalPage /> },
      { path: 'transferencias', element: <TransferenciasPage /> },
      { path: 'alertas', element: <AlertasPage /> },
      { path: 'relatorios', element: <RelatoriosPage /> },
      { path: 'notificacoes', element: <NotificacoesPage /> },
      {
        path: 'admin/categorias',
        element: (
          <RoleGuard allow={['ADMIN', 'GERENTE']}>
            <CategoriasPage />
          </RoleGuard>
        ),
      },
      {
        path: 'admin/unidades',
        element: (
          <RoleGuard allow={['ADMIN', 'GERENTE']}>
            <UnidadesPage />
          </RoleGuard>
        ),
      },
      {
        path: 'admin/empresas',
        element: (
          <RoleGuard allow={['ADMIN']}>
            <EmpresasPage />
          </RoleGuard>
        ),
      },
      {
        path: 'admin/usuarios',
        element: (
          <RoleGuard allow={['ADMIN', 'GERENTE']}>
            <UsuariosPage />
          </RoleGuard>
        ),
      },
    ],
  },
  { path: '*', element: <Navigate to="/dashboard" replace /> },
]);
