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

  // Inputs (não disparam filtragem).
  const [buscaInput, setBuscaInput] = useState('');
  const [ativaInput, setAtivaInput] = useState(ATIVA_TODOS);

  // Filtros aplicados (só mudam ao clicar Pesquisar).
  const [filtros, setFiltros] = useState({ q: '', ativa: ATIVA_TODOS });

  const categoriasQuery = useQuery({
    queryKey: ['admin-categorias'],
    queryFn: listarCategorias,
  });

  const filtradas = useMemo(() => {
    const todas = categoriasQuery.data ?? [];
    const termo = filtros.q.trim().toLowerCase();
    return todas
      .filter((c) => {
        if (filtros.ativa === 'true' && !c.ativa) return false;
        if (filtros.ativa === 'false' && c.ativa) return false;
        if (termo && !c.nome.toLowerCase().includes(termo)) return false;
        return true;
      })
      // Convenção UX: inativas ao fim; alfabético por nome (pt-BR).
      .sort((a, b) => {
        if (a.ativa !== b.ativa) return a.ativa ? -1 : 1;
        return a.nome.localeCompare(b.nome, 'pt-BR', { sensitivity: 'base' });
      });
  }, [categoriasQuery.data, filtros]);

  function aplicarFiltros() {
    setFiltros({ q: buscaInput, ativa: ativaInput });
  }

  function limparFiltros() {
    setBuscaInput('');
    setAtivaInput(ATIVA_TODOS);
    setFiltros({ q: '', ativa: ATIVA_TODOS });
  }

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
  // Só desabilita enquanto a mutation está em curso; após success, libera.
  const togglingId = desativarMutation.isPending
    ? desativarMutation.variables
    : ativarMutation.isPending
      ? ativarMutation.variables
      : undefined;

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

      <form
        className="space-y-3 rounded-md border bg-card p-4"
        onSubmit={(e) => {
          e.preventDefault();
          aplicarFiltros();
        }}
      >
        <div className="grid gap-3 md:grid-cols-3">
          <div className="space-y-1.5 md:col-span-2">
            <Label htmlFor="filtro-busca">Buscar</Label>
            <Input
              id="filtro-busca"
              placeholder="Nome da categoria"
              value={buscaInput}
              onChange={(e) => setBuscaInput(e.target.value)}
            />
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
