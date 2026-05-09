import { createBrowserRouter, Navigate } from 'react-router-dom';

import { LoginPage } from '@/features/auth/LoginPage';
import { AppLayout } from '@/components/layout/AppLayout';
import { ProtectedRoute } from './ProtectedRoute';
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
      { path: 'transferencias', element: <TransferenciasPage /> },
      { path: 'alertas', element: <AlertasPage /> },
      { path: 'relatorios', element: <RelatoriosPage /> },
      { path: 'notificacoes', element: <NotificacoesPage /> },
    ],
  },
  { path: '*', element: <Navigate to="/dashboard" replace /> },
]);
