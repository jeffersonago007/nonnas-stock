import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Eye, PlayCircle, RefreshCw, RotateCw, Wand2 } from 'lucide-react';
import { toast } from 'sonner';

import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { PageHeader } from '@/components/PageHeader';
import { DataTable, type ColumnDef } from '@/components/data-table/DataTable';
import { toastError } from '@/lib/toastError';
import { useFilialFiltroStore } from '@/features/filtroGlobal/store';
import { useAuthStore } from '@/features/auth/store';
import { hasAnyRole } from '@/routes/RoleGuard';
import { cn } from '@/lib/utils';

import {
  CANAIS,
  CANAL_LABEL,
  STATUS_LABEL,
  type CanalTipo,
  type PedidoCanal,
  type StatusPedidoCanal,
  listarPedidos,
  pollNow,
  processarPendentes,
  reprocessarPedido,
} from './api';
import { DetalhesPedidoDialog } from './DetalhesPedidoDialog';
import { SimularPedidoDevDialog } from './SimularPedidoDevDialog';

const STATUS_TODOS = '__todos__';

// Ordem para sort: ações prioritárias primeiro.
const STATUS_ORDER: Record<StatusPedidoCanal, number> = {
  FALHA: 0,
  RECEBIDO: 1,
  EM_PROCESSAMENTO: 2,
  CONFIRMADO_ESTOQUE: 3,
  CONCLUIDO: 4,
  CANCELADO: 5,
};

const STATUS_TONE: Record<StatusPedidoCanal, string> = {
  FALHA: 'bg-destructive/15 text-destructive',
  RECEBIDO: 'bg-amber-100 text-amber-900',
  EM_PROCESSAMENTO: 'bg-blue-100 text-blue-900',
  CONFIRMADO_ESTOQUE: 'bg-emerald-100 text-emerald-900',
  CONCLUIDO: 'bg-muted text-muted-foreground',
  CANCELADO: 'bg-muted text-muted-foreground line-through',
};

function fmtMoeda(valor: number, moeda: string) {
  try {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: moeda || 'BRL',
    }).format(valor);
  } catch {
    return `${moeda} ${valor.toFixed(2)}`;
  }
}

function fmtDate(iso: string | null): string {
  if (!iso) return '—';
  try {
    return new Date(iso).toLocaleString('pt-BR');
  } catch {
    return iso;
  }
}

export function PedidosCanaisPage() {
  const queryClient = useQueryClient();
  const filialId = useFilialFiltroStore((s) => s.filialId);
  const perfil = useAuthStore((s) => s.user?.perfil);
  const isAdmin = hasAnyRole(perfil, ['ADMIN']);
  const podeReprocessar = hasAnyRole(perfil, ['ADMIN', 'GERENTE']);

  const [statusFiltro, setStatusFiltro] = useState<string>(STATUS_TODOS);
  const [canalPoll, setCanalPoll] = useState<CanalTipo>('IFOOD');
  const [verPedido, setVerPedido] = useState<PedidoCanal | null>(null);
  const [simularOpen, setSimularOpen] = useState(false);

  const pedidosQuery = useQuery({
    queryKey: ['canais-pedidos', filialId, statusFiltro],
    queryFn: () =>
      listarPedidos(
        filialId!,
        statusFiltro !== STATUS_TODOS ? (statusFiltro as StatusPedidoCanal) : undefined,
      ),
    enabled: filialId !== null,
    // Scheduler backend processa eventos; UI refetch a cada 60s para refletir.
    refetchInterval: filialId !== null ? 60_000 : false,
  });

  const ordenados = useMemo(() => {
    const base = pedidosQuery.data ?? [];
    return [...base].sort((a, b) => {
      const so = STATUS_ORDER[a.status] - STATUS_ORDER[b.status];
      if (so !== 0) return so;
      return b.recebidoEm.localeCompare(a.recebidoEm);
    });
  }, [pedidosQuery.data]);

  const pendentesCount = useMemo(
    () => ordenados.filter((p) => p.status === 'RECEBIDO' || p.status === 'FALHA').length,
    [ordenados],
  );

  const reprocessarMut = useMutation({
    mutationFn: reprocessarPedido,
    onSuccess: (p) => {
      toast.success(`Pedido reprocessado: ${STATUS_LABEL[p.status]}`);
      queryClient.invalidateQueries({ queryKey: ['canais-pedidos'] });
    },
    onError: (e) => toastError('Não foi possível reprocessar', e),
  });

  const processarPendentesMut = useMutation({
    mutationFn: processarPendentes,
    onSuccess: (r) => {
      toast.success(
        `Processados: ${r.processadosSucesso} sucesso, ${r.processadosFalha} falha, ${r.totalPendentes} pendentes total`,
      );
      queryClient.invalidateQueries({ queryKey: ['canais-pedidos'] });
    },
    onError: (e) => toastError('Não foi possível processar pendentes', e),
  });

  const pollNowMut = useMutation({
    mutationFn: pollNow,
    onSuccess: (r) => {
      toast.success(`${CANAL_LABEL[r.canal]}: ${r.eventosNovos} eventos novos`);
      queryClient.invalidateQueries({ queryKey: ['canais-pedidos'] });
    },
    onError: (e) => toastError('Não foi possível buscar agora', e),
  });

  if (filialId === null) {
    return (
      <div className="space-y-6">
        <PageHeader
          title="Pedidos de canal"
          description="Pedidos recebidos via iFood, 99Food, Keeta e outros canais Open Delivery."
        />
        <div className="rounded-md border bg-card p-8 text-center text-muted-foreground">
          Selecione uma filial no header para listar os pedidos de canal.
        </div>
      </div>
    );
  }

  const columns: ColumnDef<PedidoCanal>[] = [
    {
      key: 'pedido',
      header: 'Pedido',
      cell: (p) => (
        <div className="flex flex-col">
          <span className="font-medium">{p.displayId ?? p.pedidoExternoId}</span>
          <span className="text-xs text-muted-foreground">
            {p.clienteNome ?? '—'} {p.clienteTelefone ? `· ${p.clienteTelefone}` : ''}
          </span>
        </div>
      ),
    },
    {
      key: 'canal',
      header: 'Canal',
      cell: (p) => CANAL_LABEL[p.canalTipo],
      className: 'w-[170px]',
    },
    {
      key: 'status',
      header: 'Status',
      cell: (p) => (
        <div className="flex flex-col gap-1">
          <span
            className={cn(
              'inline-flex w-fit rounded px-2 py-0.5 text-xs font-medium',
              STATUS_TONE[p.status],
            )}
          >
            {STATUS_LABEL[p.status]}
          </span>
          {p.erroProcessamento && (
            <span className="text-xs text-destructive">{p.erroProcessamento}</span>
          )}
        </div>
      ),
      className: 'w-[200px]',
    },
    {
      key: 'total',
      header: 'Total',
      cell: (p) => fmtMoeda(p.valorTotal, p.moeda),
      className: 'w-[120px] text-right',
    },
    {
      key: 'recebidoEm',
      header: 'Recebido em',
      cell: (p) => <span className="text-xs">{fmtDate(p.recebidoEm)}</span>,
      className: 'w-[170px]',
    },
    {
      key: 'actions',
      header: <span className="sr-only">Ações</span>,
      className: 'text-right w-[220px]',
      cell: (p) => (
        <div className="flex justify-end gap-2">
          <Button variant="ghost" size="sm" onClick={() => setVerPedido(p)}>
            <Eye className="h-4 w-4" /> Detalhes
          </Button>
          {podeReprocessar && (p.status === 'RECEBIDO' || p.status === 'FALHA') && (
            <Button
              variant="ghost"
              size="sm"
              disabled={reprocessarMut.isPending && reprocessarMut.variables === p.id}
              onClick={() => reprocessarMut.mutate(p.id)}
            >
              <RotateCw className="h-4 w-4" /> Reprocessar
            </Button>
          )}
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Pedidos de canal"
        description={`Pedidos recebidos via iFood, 99Food, Keeta e outros canais. ${pendentesCount > 0 ? `${pendentesCount} pendentes de processamento.` : ''}`}
        actions={
          <div className="flex gap-2">
            {isAdmin && (
              <Button variant="outline" onClick={() => setSimularOpen(true)}>
                <Wand2 className="h-4 w-4" /> Simular pedido (dev)
              </Button>
            )}
            {podeReprocessar && (
              <Button
                variant="outline"
                disabled={processarPendentesMut.isPending}
                onClick={() => processarPendentesMut.mutate()}
              >
                <PlayCircle className="h-4 w-4" />{' '}
                {processarPendentesMut.isPending ? 'Processando…' : 'Processar pendentes'}
              </Button>
            )}
          </div>
        }
      />

      <div className="rounded-md border bg-card p-4">
        <div className="grid items-end gap-3 md:grid-cols-4">
          <div className="space-y-1.5">
            <Label htmlFor="status">Status</Label>
            <Select value={statusFiltro} onValueChange={setStatusFiltro}>
              <SelectTrigger id="status">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={STATUS_TODOS}>Todos</SelectItem>
                {(Object.keys(STATUS_LABEL) as StatusPedidoCanal[]).map((s) => (
                  <SelectItem key={s} value={s}>
                    {STATUS_LABEL[s]}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          {isAdmin && (
            <>
              <div className="space-y-1.5">
                <Label htmlFor="canalPoll">Canal para buscar</Label>
                <Select value={canalPoll} onValueChange={(v) => setCanalPoll(v as CanalTipo)}>
                  <SelectTrigger id="canalPoll">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {CANAIS.map((c) => (
                      <SelectItem key={c} value={c}>
                        {CANAL_LABEL[c]}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div>
                <Button
                  variant="outline"
                  disabled={pollNowMut.isPending}
                  onClick={() => pollNowMut.mutate(canalPoll)}
                >
                  <RefreshCw className="h-4 w-4" />{' '}
                  {pollNowMut.isPending ? 'Buscando…' : 'Buscar pedidos agora'}
                </Button>
              </div>
            </>
          )}
        </div>
      </div>

      <DataTable
        data={ordenados}
        columns={columns}
        isLoading={pedidosQuery.isLoading}
        isError={pedidosQuery.isError}
        rowKey={(p) => p.id}
        emptyState={
          <p className="text-muted-foreground">
            Nenhum pedido nessa combinação de filtros.
          </p>
        }
      />

      <DetalhesPedidoDialog
        pedido={verPedido}
        onClose={() => setVerPedido(null)}
      />

      <SimularPedidoDevDialog
        open={simularOpen}
        onOpenChange={setSimularOpen}
        filialId={filialId}
      />
    </div>
  );
}
