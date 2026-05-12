import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { AlertTriangle, ArrowRight, Bell, Building2 } from 'lucide-react';

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { PageHeader } from '@/components/PageHeader';
import { listarFiliais } from '@/features/cadastros/filiais/api';
import { useFilialFiltroStore } from '@/features/filtroGlobal/store';
import { listarDisparados } from '@/features/alertas/api';
import {
  listarEmTransito,
  listarRuptura,
  listarTransferencias,
} from '@/features/operacoes/api';

export function DashboardPage() {
  const filialId = useFilialFiltroStore((s) => s.filialId);

  const filiaisQuery = useQuery({
    queryKey: ['filiais'],
    queryFn: () => listarFiliais(),
  });
  const alertasQuery = useQuery({
    queryKey: ['alertas-disparados', { filialId, status: 'ATIVO' }],
    queryFn: () => listarDisparados({ status: 'ATIVO', filialId }),
  });
  const emTransitoQuery = useQuery({
    queryKey: ['em-transito', filialId],
    queryFn: () => listarEmTransito(filialId),
  });
  const transferenciasAbertasQuery = useQuery({
    queryKey: ['transferencias', { filialId, status: 'EM_TRANSITO' }],
    queryFn: () => listarTransferencias({ filialId, status: 'EM_TRANSITO' }),
  });
  const rupturaQuery = useQuery({
    queryKey: ['ruptura', filialId],
    queryFn: () => listarRuptura(filialId),
  });

  const totalFiliaisAtivas = filiaisQuery.data?.filter((f) => f.ativa).length ?? 0;
  const totalAlertasAtivos = alertasQuery.data?.length ?? 0;
  const totalTransferenciasEmTransito = transferenciasAbertasQuery.data?.length ?? 0;
  const totalRuptura = rupturaQuery.data?.length ?? 0;

  const dadosGrafico = useMemo(() => {
    return (rupturaQuery.data ?? [])
      .slice(0, 10)
      .map((r) => ({
        nome: r.nome.length > 15 ? r.nome.slice(0, 14) + '…' : r.nome,
        saldo: r.saldoTotal,
        minimo: r.estoqueMinimo,
        situacao: r.situacao,
      }));
  }, [rupturaQuery.data]);

  return (
    <div className="space-y-6">
      <PageHeader
        title="Dashboard"
        description={
          filialId
            ? 'Operação resumida da filial selecionada.'
            : 'Operação resumida da rede toda. Selecione uma filial no header pra focar.'
        }
      />

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <SummaryCard
          icon={<Building2 className="h-5 w-5 text-muted-foreground" />}
          label="Filiais ativas"
          value={totalFiliaisAtivas}
          description="Filiais habilitadas para operação"
        />
        <SummaryCard
          icon={<Bell className="h-5 w-5 text-destructive" />}
          label="Alertas ativos"
          value={totalAlertasAtivos}
          description="Disparos não-resolvidos"
          tone="danger"
        />
        <SummaryCard
          icon={<ArrowRight className="h-5 w-5 text-purple-600" />}
          label="Transferências em trânsito"
          value={totalTransferenciasEmTransito}
          description="Aguardando recebimento"
          tone="info"
        />
        <SummaryCard
          icon={<AlertTriangle className="h-5 w-5 text-amber-600" />}
          label="Itens em ruptura"
          value={totalRuptura}
          description="Saldo abaixo do mínimo"
          tone="warning"
        />
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Top 10 itens em ruptura</CardTitle>
          <CardDescription>
            Saldo atual vs estoque mínimo. Foco visual nos casos mais críticos.
          </CardDescription>
        </CardHeader>
        <CardContent>
          {rupturaQuery.isLoading && (
            <p className="text-sm text-muted-foreground">Carregando…</p>
          )}
          {!rupturaQuery.isLoading && dadosGrafico.length === 0 && (
            <p className="text-sm text-muted-foreground">
              Nenhum item em ruptura — tudo dentro do mínimo configurado.
            </p>
          )}
          {dadosGrafico.length > 0 && (
            <div className="h-72">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={dadosGrafico} margin={{ top: 16, right: 16, left: 0, bottom: 16 }}>
                  <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
                  <XAxis dataKey="nome" tick={{ fontSize: 12 }} interval={0} angle={-15} textAnchor="end" height={60} />
                  <YAxis tick={{ fontSize: 12 }} />
                  <Tooltip
                    formatter={(value: number, name: string) => [
                      value.toLocaleString('pt-BR'),
                      name === 'saldo' ? 'Saldo atual' : 'Estoque mínimo',
                    ]}
                  />
                  <Bar dataKey="saldo">
                    {dadosGrafico.map((entry, idx) => (
                      <Cell
                        key={`cell-${idx}`}
                        fill={entry.situacao === 'RUPTURA_TOTAL' ? '#D62828' : '#D97706'}
                      />
                    ))}
                  </Bar>
                  <Bar dataKey="minimo" fill="#6B7280" />
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </CardContent>
      </Card>

      {emTransitoQuery.data && emTransitoQuery.data.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Em trânsito (agregado por insumo)</CardTitle>
            <CardDescription>
              Quantidades já despachadas mas ainda não recebidas.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <ul className="grid gap-2 md:grid-cols-2 lg:grid-cols-3">
              {emTransitoQuery.data.slice(0, 12).map((item) => (
                <li
                  key={item.insumoId}
                  className="flex items-center justify-between rounded-md border p-2 text-sm"
                >
                  <span className="font-mono text-xs text-muted-foreground">
                    {item.insumoId.slice(0, 8)}…
                  </span>
                  <span className="font-mono">{item.quantidadeEmTransito.toLocaleString('pt-BR')}</span>
                </li>
              ))}
            </ul>
          </CardContent>
        </Card>
      )}
    </div>
  );
}

interface SummaryCardProps {
  icon: React.ReactNode;
  label: string;
  value: number;
  description: string;
  tone?: 'normal' | 'danger' | 'warning' | 'info';
}

function SummaryCard({ icon, label, value, description, tone = 'normal' }: SummaryCardProps) {
  const valueClass =
    tone === 'danger'
      ? 'text-destructive'
      : tone === 'warning'
        ? 'text-amber-600'
        : tone === 'info'
          ? 'text-purple-700'
          : 'text-foreground';
  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
        <CardDescription>{label}</CardDescription>
        {icon}
      </CardHeader>
      <CardContent>
        <div className={`text-3xl font-semibold ${valueClass}`}>{value}</div>
        <p className="mt-1 text-xs text-muted-foreground">{description}</p>
      </CardContent>
    </Card>
  );
}
