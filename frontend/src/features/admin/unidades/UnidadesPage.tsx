import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Pencil, Plus, Power, Search, X } from 'lucide-react';
import { toast } from 'sonner';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { PageHeader } from '@/components/PageHeader';
import { StatusBadge } from '@/components/StatusBadge';
import { DataTable, type ColumnDef } from '@/components/data-table/DataTable';
import { toastError } from '@/lib/toastError';

import {
  type Unidade,
  type UnidadeTipo,
  ativarUnidade,
  desativarUnidade,
  listarUnidades,
} from './api';
import { UnidadeFormDialog } from './UnidadeFormDialog';

const ATIVA_TODOS = '__todos__';
const TIPO_TODOS = '__todos__';

const tipoLabel: Record<UnidadeTipo, string> = {
  PESO: 'Peso',
  VOLUME: 'Volume',
  UNIDADE: 'Unidade',
};

export function UnidadesPage() {
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<Unidade | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);

  // Inputs (não disparam filtragem).
  const [buscaInput, setBuscaInput] = useState('');
  const [ativaInput, setAtivaInput] = useState(ATIVA_TODOS);
  const [tipoInput, setTipoInput] = useState(TIPO_TODOS);

  // Filtros aplicados (só mudam ao clicar Pesquisar).
  const [filtros, setFiltros] = useState({
    q: '',
    ativa: ATIVA_TODOS,
    tipo: TIPO_TODOS,
  });

  const unidadesQuery = useQuery({
    queryKey: ['admin-unidades'],
    queryFn: listarUnidades,
  });

  const filtradas = useMemo(() => {
    const todas = unidadesQuery.data ?? [];
    const termo = filtros.q.trim().toLowerCase();
    return todas
      .filter((u) => {
        if (filtros.ativa === 'true' && !u.ativa) return false;
        if (filtros.ativa === 'false' && u.ativa) return false;
        if (filtros.tipo !== TIPO_TODOS && u.tipo !== filtros.tipo) return false;
        if (
          termo &&
          !u.nome.toLowerCase().includes(termo) &&
          !u.codigo.toLowerCase().includes(termo)
        ) {
          return false;
        }
        return true;
      })
      // Convenção UX: inativos ao fim; alfabético por nome (pt-BR).
      .sort((a, b) => {
        if (a.ativa !== b.ativa) return a.ativa ? -1 : 1;
        return a.nome.localeCompare(b.nome, 'pt-BR', { sensitivity: 'base' });
      });
  }, [unidadesQuery.data, filtros]);

  function aplicarFiltros() {
    setFiltros({ q: buscaInput, ativa: ativaInput, tipo: tipoInput });
  }

  function limparFiltros() {
    setBuscaInput('');
    setAtivaInput(ATIVA_TODOS);
    setTipoInput(TIPO_TODOS);
    setFiltros({ q: '', ativa: ATIVA_TODOS, tipo: TIPO_TODOS });
  }

  const desativarMutation = useMutation({
    mutationFn: desativarUnidade,
    onSuccess: () => {
      toast.success('Unidade desativada');
      queryClient.invalidateQueries({ queryKey: ['admin-unidades'] });
      queryClient.invalidateQueries({ queryKey: ['unidades-medida'] });
    },
    onError: (error) => toastError('Não foi possível desativar', error),
  });
  const ativarMutation = useMutation({
    mutationFn: ativarUnidade,
    onSuccess: () => {
      toast.success('Unidade reativada');
      queryClient.invalidateQueries({ queryKey: ['admin-unidades'] });
      queryClient.invalidateQueries({ queryKey: ['unidades-medida'] });
    },
    onError: (error) => toastError('Não foi possível ativar', error),
  });
  const togglingId = desativarMutation.isPending
    ? desativarMutation.variables
    : ativarMutation.isPending
      ? ativarMutation.variables
      : undefined;

  const columns: ColumnDef<Unidade>[] = [
    {
      key: 'codigo',
      header: 'Código',
      cell: (u) => <span className="font-mono font-medium">{u.codigo}</span>,
      className: 'w-[120px]',
    },
    { key: 'nome', header: 'Nome', cell: (u) => u.nome },
    { key: 'tipo', header: 'Tipo', cell: (u) => tipoLabel[u.tipo], className: 'w-[120px]' },
    { key: 'status', header: 'Status', cell: (u) => <StatusBadge active={u.ativa} /> },
    {
      key: 'actions',
      header: <span className="sr-only">Ações</span>,
      className: 'text-right w-[200px]',
      cell: (u) => (
        <div className="flex justify-end gap-2">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => {
              setEditing(u);
              setDialogOpen(true);
            }}
          >
            <Pencil className="h-4 w-4" /> Editar
          </Button>
          <Button
            variant="ghost"
            size="sm"
            disabled={togglingId === u.id}
            onClick={() =>
              u.ativa ? desativarMutation.mutate(u.id) : ativarMutation.mutate(u.id)
            }
          >
            <Power className="h-4 w-4" /> {u.ativa ? 'Desativar' : 'Ativar'}
          </Button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Unidades de medida"
        description="Catálogo de unidades — usadas na unidade-base de cada insumo e em conversões."
        actions={
          <Button
            onClick={() => {
              setEditing(null);
              setDialogOpen(true);
            }}
          >
            <Plus className="h-4 w-4" /> Nova unidade
          </Button>
        }
      />

      <form
        className="space-y-3 rounded-md border bg-card p-4"
        onSubmit={(e) => {
          e.preventDefault();
          aplicarFiltros();
        }}
      >
        <div className="grid gap-3 md:grid-cols-4">
          <div className="space-y-1.5 md:col-span-2">
            <Label htmlFor="filtro-busca">Buscar</Label>
            <Input
              id="filtro-busca"
              placeholder="Nome ou código"
              value={buscaInput}
              onChange={(e) => setBuscaInput(e.target.value)}
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="filtro-tipo">Tipo</Label>
            <Select value={tipoInput} onValueChange={setTipoInput}>
              <SelectTrigger id="filtro-tipo">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={TIPO_TODOS}>Todos</SelectItem>
                <SelectItem value="PESO">Peso</SelectItem>
                <SelectItem value="VOLUME">Volume</SelectItem>
                <SelectItem value="UNIDADE">Unidade</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="filtro-ativa">Status</Label>
            <Select value={ativaInput} onValueChange={setAtivaInput}>
              <SelectTrigger id="filtro-ativa">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ATIVA_TODOS}>Todas</SelectItem>
                <SelectItem value="true">Ativas</SelectItem>
                <SelectItem value="false">Inativas</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>
        <div className="flex justify-end gap-2 pt-1">
          <Button type="button" variant="outline" onClick={limparFiltros}>
            <X className="h-4 w-4" /> Limpar
          </Button>
          <Button type="submit">
            <Search className="h-4 w-4" /> Pesquisar
          </Button>
        </div>
      </form>

      <DataTable
        data={filtradas}
        columns={columns}
        isLoading={unidadesQuery.isLoading}
        isError={unidadesQuery.isError}
        rowKey={(u) => u.id}
        rowClassName={(u) => (!u.ativa ? 'opacity-60' : '')}
        emptyState={
          <div className="space-y-3">
            <p>Nenhuma unidade encontrada.</p>
            <Button
              variant="outline"
              onClick={() => {
                setEditing(null);
                setDialogOpen(true);
              }}
            >
              <Plus className="h-4 w-4" /> Cadastrar unidade
            </Button>
          </div>
        }
      />

      <UnidadeFormDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        unidade={editing}
      />
    </div>
  );
}
