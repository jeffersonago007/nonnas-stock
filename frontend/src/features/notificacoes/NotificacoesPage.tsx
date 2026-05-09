import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Archive, CheckCheck, ExternalLink, Eye } from 'lucide-react';
import { Link } from 'react-router-dom';
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
import { Card, CardContent } from '@/components/ui/card';
import { PageHeader } from '@/components/PageHeader';
import { DataTable, type ColumnDef } from '@/components/data-table/DataTable';
import { toastError } from '@/lib/toastError';

import {
  type Notificacao,
  type Prioridade,
  arquivar,
  listarNotificacoes,
  marcarLida,
  marcarTodasLidas,
} from './api';

const TIPO_TODOS = '__todos__';

const TIPO_LABELS: Record<string, string> = {
  ALERTA_DISPARADO: 'Alerta disparado',
  TRANSFERENCIA_APROVADA: 'Transferência aprovada',
  TRANSFERENCIA_RECEBIDA: 'Transferência recebida',
  DIVERGENCIA_INVENTARIO: 'Divergência inventário',
  LOGIN_NOVO_IP: 'Login de novo IP',
  LGPD_DIREITO_EXERCIDO: 'Direito LGPD exercido',
};

const PRIORIDADE_TONES: Record<Prioridade, string> = {
  INFO: 'bg-blue-100 text-blue-900',
  AVISO: 'bg-amber-100 text-amber-900',
  CRITICA: 'bg-destructive/10 text-destructive',
};

export function NotificacoesPage() {
  const queryClient = useQueryClient();
  const [tipoFiltro, setTipoFiltro] = useState(TIPO_TODOS);
  const [somenteNaoLidas, setSomenteNaoLidas] = useState(false);

  const filtros = useMemo(
    () => ({
      tipo: tipoFiltro === TIPO_TODOS ? undefined : tipoFiltro,
      somenteNaoLidas,
    }),
    [tipoFiltro, somenteNaoLidas],
  );

  const query = useQuery({
    queryKey: ['notificacoes', filtros],
    queryFn: () => listarNotificacoes(filtros),
  });

  const lidaMut = useMutation({
    mutationFn: marcarLida,
    onSuccess: () => invalidateAll(),
    onError: (e) => toastError('Falha ao marcar como lida', e),
  });
  const arquivarMut = useMutation({
    mutationFn: arquivar,
    onSuccess: () => {
      toast.success('Notificação arquivada');
      invalidateAll();
    },
    onError: (e) => toastError('Falha ao arquivar', e),
  });
  const todasLidasMut = useMutation({
    mutationFn: marcarTodasLidas,
    onSuccess: (n) => {
      toast.success(`${n} notificação${n === 1 ? '' : 'es'} marcada${n === 1 ? '' : 's'} como lida`);
      invalidateAll();
    },
    onError: (e) => toastError('Falha ao marcar todas', e),
  });

  function invalidateAll() {
    queryClient.invalidateQueries({ queryKey: ['notificacoes'] });
    queryClient.invalidateQueries({ queryKey: ['notificacoes-contagem'] });
    queryClient.invalidateQueries({ queryKey: ['notificacoes-criticas-recentes'] });
  }

  const columns: ColumnDef<Notificacao>[] = [
    {
      key: 'criada',
      header: 'Recebida em',
      cell: (n) => (
        <span className={n.lidaEm ? 'text-sm text-muted-foreground' : 'text-sm font-medium'}>
          {new Date(n.criadaEm).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })}
        </span>
      ),
      className: 'w-[160px]',
    },
    {
      key: 'tipo',
      header: 'Tipo',
      cell: (n) => TIPO_LABELS[n.tipo] ?? n.tipo,
      className: 'w-[200px]',
    },
    {
      key: 'titulo',
      header: 'Notificação',
      cell: (n) => (
        <div>
          <p className={n.lidaEm ? 'text-foreground' : 'font-medium'}>{n.titulo}</p>
          <p className="text-xs text-muted-foreground">{n.mensagem}</p>
        </div>
      ),
    },
    {
      key: 'prioridade',
      header: 'Prioridade',
      cell: (n) => (
        <span className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium ${PRIORIDADE_TONES[n.prioridade]}`}>
          {n.prioridade}
        </span>
      ),
      className: 'w-[110px]',
    },
    {
      key: 'actions',
      header: <span className="sr-only">Ações</span>,
      className: 'text-right w-[280px]',
      cell: (n) => (
        <div className="flex justify-end gap-2">
          {n.linkAcao && (
            <Button asChild variant="ghost" size="sm">
              <Link to={n.linkAcao}>
                <ExternalLink className="h-4 w-4" /> Abrir
              </Link>
            </Button>
          )}
          {!n.lidaEm && (
            <Button variant="ghost" size="sm" onClick={() => lidaMut.mutate(n.id)}>
              <Eye className="h-4 w-4" /> Marcar lida
            </Button>
          )}
          <Button variant="ghost" size="sm" onClick={() => arquivarMut.mutate(n.id)}>
            <Archive className="h-4 w-4" /> Arquivar
          </Button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Notificações"
        description="Alertas, transferências e eventos LGPD direcionados a você."
        actions={
          <Button
            variant="outline"
            onClick={() => todasLidasMut.mutate()}
            disabled={todasLidasMut.isPending}
          >
            <CheckCheck className="h-4 w-4" /> Marcar todas como lidas
          </Button>
        }
      />

      <Card>
        <CardContent className="grid gap-3 p-4 md:grid-cols-3">
          <div className="space-y-1.5">
            <Label htmlFor="filtro-tipo">Tipo</Label>
            <Select value={tipoFiltro} onValueChange={setTipoFiltro}>
              <SelectTrigger id="filtro-tipo">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={TIPO_TODOS}>Todos</SelectItem>
                {Object.entries(TIPO_LABELS).map(([value, label]) => (
                  <SelectItem key={value} value={value}>
                    {label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="filtro-status">Status</Label>
            <Select
              value={somenteNaoLidas ? 'nao-lidas' : 'todas'}
              onValueChange={(v) => setSomenteNaoLidas(v === 'nao-lidas')}
            >
              <SelectTrigger id="filtro-status">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="todas">Todas</SelectItem>
                <SelectItem value="nao-lidas">Apenas não-lidas</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </CardContent>
      </Card>

      <DataTable
        data={query.data}
        columns={columns}
        isLoading={query.isLoading}
        isError={query.isError}
        rowKey={(n) => n.id}
        rowClassName={(n) => (n.lidaEm ? 'opacity-70' : '')}
        emptyState={
          <p>
            {somenteNaoLidas
              ? 'Sem notificações novas.'
              : 'Você ainda não recebeu nenhuma notificação.'}
          </p>
        }
      />
    </div>
  );
}
