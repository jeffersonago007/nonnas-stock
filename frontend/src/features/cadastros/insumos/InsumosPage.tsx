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
  type Insumo,
  ativarInsumo,
  desativarInsumo,
  listarCategorias,
  listarInsumos,
  listarUnidades,
} from './api';
import { InsumoFormDialog } from './InsumoFormDialog';

const ATIVO_TODOS = '__todos__';
const CATEGORIA_TODAS = '__todas__';

export function InsumosPage() {
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<Insumo | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [busca, setBusca] = useState('');
  const [categoriaFiltro, setCategoriaFiltro] = useState(CATEGORIA_TODAS);
  const [ativoFiltro, setAtivoFiltro] = useState(ATIVO_TODOS);

  const filtros = useMemo(
    () => ({
      categoriaId: categoriaFiltro === CATEGORIA_TODAS ? undefined : categoriaFiltro,
      ativo: ativoFiltro === ATIVO_TODOS ? undefined : ativoFiltro === 'true',
      q: busca.trim() || undefined,
    }),
    [categoriaFiltro, ativoFiltro, busca],
  );

  const insumosQuery = useQuery({
    queryKey: ['insumos', filtros],
    queryFn: () => listarInsumos(filtros),
  });

  const categoriasQuery = useQuery({
    queryKey: ['categorias-insumo'],
    queryFn: listarCategorias,
  });
  const unidadesQuery = useQuery({
    queryKey: ['unidades-medida'],
    queryFn: listarUnidades,
  });

  const unidadeMap = useMemo(() => {
    const m = new Map<string, string>();
    unidadesQuery.data?.forEach((u) => m.set(u.id, u.codigo));
    return m;
  }, [unidadesQuery.data]);

  const categoriaMap = useMemo(() => {
    const m = new Map<string, string>();
    categoriasQuery.data?.forEach((c) => m.set(c.id, c.nome));
    return m;
  }, [categoriasQuery.data]);

  const desativarMutation = useMutation({
    mutationFn: desativarInsumo,
    onSuccess: () => {
      toast.success('Produto desativado');
      queryClient.invalidateQueries({ queryKey: ['insumos'] });
    },
    onError: (error) => toastError('Não foi possível desativar', error),
  });
  const ativarMutation = useMutation({
    mutationFn: ativarInsumo,
    onSuccess: () => {
      toast.success('Produto reativado');
      queryClient.invalidateQueries({ queryKey: ['insumos'] });
    },
    onError: (error) => toastError('Não foi possível ativar', error),
  });
  const togglingId = desativarMutation.variables ?? ativarMutation.variables;

  const columns: ColumnDef<Insumo>[] = [
    { key: 'codigo', header: 'Código', cell: (i) => <code className="text-xs">{i.codigo}</code>, className: 'w-[120px]' },
    { key: 'nome', header: 'Nome', cell: (i) => <span className="font-medium">{i.nome}</span> },
    {
      key: 'categoria',
      header: 'Categoria',
      cell: (i) => categoriaMap.get(i.categoriaId) ?? i.categoriaId.slice(0, 8),
    },
    {
      key: 'unidade',
      header: 'Unidade base',
      cell: (i) => unidadeMap.get(i.unidadeBaseId) ?? i.unidadeBaseId.slice(0, 8),
      className: 'w-[120px]',
    },
    {
      key: 'controles',
      header: 'Controles',
      cell: (i) => (
        <div className="flex flex-wrap gap-1 text-xs">
          {i.controlaLote && <span className="rounded bg-muted px-2 py-0.5">Lote</span>}
          {i.controlaValidade && <span className="rounded bg-muted px-2 py-0.5">Validade</span>}
        </div>
      ),
    },
    { key: 'status', header: 'Status', cell: (i) => <StatusBadge active={i.ativo} /> },
    {
      key: 'actions',
      header: <span className="sr-only">Ações</span>,
      className: 'text-right w-[200px]',
      cell: (i) => (
        <div className="flex justify-end gap-2">
          <Button variant="ghost" size="sm" onClick={() => { setEditing(i); setDialogOpen(true); }}>
            <Pencil className="h-4 w-4" /> Editar
          </Button>
          <Button
            variant="ghost"
            size="sm"
            disabled={togglingId === i.id}
            onClick={() => (i.ativo ? desativarMutation.mutate(i.id) : ativarMutation.mutate(i.id))}
          >
            <Power className="h-4 w-4" /> {i.ativo ? 'Desativar' : 'Ativar'}
          </Button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Produtos"
        description="Catálogo central de produtos com unidades-base, lote e validade."
        actions={
          <Button onClick={() => { setEditing(null); setDialogOpen(true); }}>
            <Plus className="h-4 w-4" /> Novo produto
          </Button>
        }
      />

      <div className="grid gap-3 rounded-md border bg-card p-4 md:grid-cols-3">
        <div className="space-y-1.5">
          <Label htmlFor="filtro-busca">Buscar</Label>
          <Input
            id="filtro-busca"
            placeholder="Nome ou código"
            value={busca}
            onChange={(e) => setBusca(e.target.value)}
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="filtro-categoria">Categoria</Label>
          <Select value={categoriaFiltro} onValueChange={setCategoriaFiltro}>
            <SelectTrigger id="filtro-categoria">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={CATEGORIA_TODAS}>Todas</SelectItem>
              {categoriasQuery.data?.map((c) => (
                <SelectItem key={c.id} value={c.id}>
                  {c.nome}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="filtro-ativo">Status</Label>
          <Select value={ativoFiltro} onValueChange={setAtivoFiltro}>
            <SelectTrigger id="filtro-ativo">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ATIVO_TODOS}>Todos</SelectItem>
              <SelectItem value="true">Ativos</SelectItem>
              <SelectItem value="false">Inativos</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      <DataTable
        data={insumosQuery.data}
        columns={columns}
        isLoading={insumosQuery.isLoading}
        isError={insumosQuery.isError}
        rowKey={(i) => i.id}
        rowClassName={(i) => (!i.ativo ? 'opacity-60' : '')}
        emptyState={
          <div className="space-y-3">
            <p>Nenhum produto encontrado para os filtros atuais.</p>
            <Button variant="outline" onClick={() => { setEditing(null); setDialogOpen(true); }}>
              <Plus className="h-4 w-4" /> Cadastrar produto
            </Button>
          </div>
        }
      />

      <InsumoFormDialog open={dialogOpen} onOpenChange={setDialogOpen} insumo={editing} />
    </div>
  );
}
