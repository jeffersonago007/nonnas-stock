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
  type Categoria,
  ativarCategoria,
  desativarCategoria,
  listarCategorias,
} from './api';
import { CategoriaFormDialog } from './CategoriaFormDialog';

const ATIVA_TODOS = '__todos__';

export function CategoriasPage() {
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<Categoria | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [busca, setBusca] = useState('');
  const [ativaFiltro, setAtivaFiltro] = useState(ATIVA_TODOS);

  const categoriasQuery = useQuery({
    queryKey: ['admin-categorias'],
    queryFn: listarCategorias,
  });

  const filtradas = useMemo(() => {
    const todas = categoriasQuery.data ?? [];
    const termo = busca.trim().toLowerCase();
    return todas.filter((c) => {
      if (ativaFiltro === 'true' && !c.ativa) return false;
      if (ativaFiltro === 'false' && c.ativa) return false;
      if (termo && !c.nome.toLowerCase().includes(termo)) return false;
      return true;
    });
  }, [categoriasQuery.data, busca, ativaFiltro]);

  const desativarMutation = useMutation({
    mutationFn: desativarCategoria,
    onSuccess: () => {
      toast.success('Categoria desativada');
      queryClient.invalidateQueries({ queryKey: ['admin-categorias'] });
      queryClient.invalidateQueries({ queryKey: ['categorias-insumo'] });
    },
    onError: (error) => toastError('Não foi possível desativar', error),
  });
  const ativarMutation = useMutation({
    mutationFn: ativarCategoria,
    onSuccess: () => {
      toast.success('Categoria reativada');
      queryClient.invalidateQueries({ queryKey: ['admin-categorias'] });
      queryClient.invalidateQueries({ queryKey: ['categorias-insumo'] });
    },
    onError: (error) => toastError('Não foi possível ativar', error),
  });
  const togglingId = desativarMutation.variables ?? ativarMutation.variables;

  const columns: ColumnDef<Categoria>[] = [
    { key: 'nome', header: 'Nome', cell: (c) => <span className="font-medium">{c.nome}</span> },
    { key: 'status', header: 'Status', cell: (c) => <StatusBadge active={c.ativa} /> },
    {
      key: 'actions',
      header: <span className="sr-only">Ações</span>,
      className: 'text-right w-[200px]',
      cell: (c) => (
        <div className="flex justify-end gap-2">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => {
              setEditing(c);
              setDialogOpen(true);
            }}
          >
            <Pencil className="h-4 w-4" /> Editar
          </Button>
          <Button
            variant="ghost"
            size="sm"
            disabled={togglingId === c.id}
            onClick={() =>
              c.ativa ? desativarMutation.mutate(c.id) : ativarMutation.mutate(c.id)
            }
          >
            <Power className="h-4 w-4" /> {c.ativa ? 'Desativar' : 'Ativar'}
          </Button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Categorias de insumo"
        description="Agrupamento usado em insumos, alertas e relatórios."
        actions={
          <Button
            onClick={() => {
              setEditing(null);
              setDialogOpen(true);
            }}
          >
            <Plus className="h-4 w-4" /> Nova categoria
          </Button>
        }
      />

      <div className="grid gap-3 rounded-md border bg-card p-4 md:grid-cols-3">
        <div className="space-y-1.5 md:col-span-2">
          <Label htmlFor="filtro-busca">Buscar</Label>
          <Input
            id="filtro-busca"
            placeholder="Nome da categoria"
            value={busca}
            onChange={(e) => setBusca(e.target.value)}
          />
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
        isLoading={categoriasQuery.isLoading}
        isError={categoriasQuery.isError}
        rowKey={(c) => c.id}
        rowClassName={(c) => (!c.ativa ? 'opacity-60' : '')}
        emptyState={
          <div className="space-y-3">
            <p>Nenhuma categoria encontrada.</p>
            <Button
              variant="outline"
              onClick={() => {
                setEditing(null);
                setDialogOpen(true);
              }}
            >
              <Plus className="h-4 w-4" /> Cadastrar categoria
            </Button>
          </div>
        }
      />

      <CategoriaFormDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        categoria={editing}
      />
    </div>
  );
}
