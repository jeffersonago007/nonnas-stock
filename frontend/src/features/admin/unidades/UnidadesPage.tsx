import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Pencil, Plus, Power } from 'lucide-react';
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
  const [busca, setBusca] = useState('');
  const [ativaFiltro, setAtivaFiltro] = useState(ATIVA_TODOS);
  const [tipoFiltro, setTipoFiltro] = useState(TIPO_TODOS);

  const unidadesQuery = useQuery({
    queryKey: ['admin-unidades'],
    queryFn: listarUnidades,
  });

  const filtradas = useMemo(() => {
    const todas = unidadesQuery.data ?? [];
    const termo = busca.trim().toLowerCase();
    return todas.filter((u) => {
      if (ativaFiltro === 'true' && !u.ativa) return false;
      if (ativaFiltro === 'false' && u.ativa) return false;
      if (tipoFiltro !== TIPO_TODOS && u.tipo !== tipoFiltro) return false;
      if (
        termo &&
        !u.nome.toLowerCase().includes(termo) &&
        !u.codigo.toLowerCase().includes(termo)
      ) {
        return false;
      }
      return true;
    });
  }, [unidadesQuery.data, busca, ativaFiltro, tipoFiltro]);

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
  const togglingId = desativarMutation.variables ?? ativarMutation.variables;

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

      <div className="grid gap-3 rounded-md border bg-card p-4 md:grid-cols-4">
        <div className="space-y-1.5 md:col-span-2">
          <Label htmlFor="filtro-busca">Buscar</Label>
          <Input
            id="filtro-busca"
            placeholder="Nome ou código"
            value={busca}
            onChange={(e) => setBusca(e.target.value)}
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="filtro-tipo">Tipo</Label>
          <Select value={tipoFiltro} onValueChange={setTipoFiltro}>
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
          <Select value={ativaFiltro} onValueChange={setAtivaFiltro}>
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
