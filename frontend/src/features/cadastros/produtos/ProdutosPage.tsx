import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ClipboardList, Pencil, Plus, Power } from 'lucide-react';
import { Link } from 'react-router-dom';
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
  type Produto,
  ativarProduto,
  desativarProduto,
  listarProdutos,
} from './api';
import { ProdutoFormDialog } from './ProdutoFormDialog';

const ATIVO_TODOS = '__todos__';

export function ProdutosPage() {
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<Produto | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [busca, setBusca] = useState('');
  const [categoriaFiltro, setCategoriaFiltro] = useState('');
  const [ativoFiltro, setAtivoFiltro] = useState(ATIVO_TODOS);

  const filtros = useMemo(
    () => ({
      categoria: categoriaFiltro.trim() || undefined,
      ativo: ativoFiltro === ATIVO_TODOS ? undefined : ativoFiltro === 'true',
      q: busca.trim() || undefined,
    }),
    [categoriaFiltro, ativoFiltro, busca],
  );

  const produtosQuery = useQuery({
    queryKey: ['produtos', filtros],
    queryFn: () => listarProdutos(filtros),
  });

  const desativarMutation = useMutation({
    mutationFn: desativarProduto,
    onSuccess: () => {
      toast.success('Produto desativado');
      queryClient.invalidateQueries({ queryKey: ['produtos'] });
    },
    onError: (error) => toastError('Não foi possível desativar', error),
  });
  const ativarMutation = useMutation({
    mutationFn: ativarProduto,
    onSuccess: () => {
      toast.success('Produto reativado');
      queryClient.invalidateQueries({ queryKey: ['produtos'] });
    },
    onError: (error) => toastError('Não foi possível ativar', error),
  });
  const togglingId = desativarMutation.variables ?? ativarMutation.variables;

  const columns: ColumnDef<Produto>[] = [
    { key: 'codigo', header: 'Código', cell: (p) => <code className="text-xs">{p.codigo}</code>, className: 'w-[120px]' },
    { key: 'nome', header: 'Nome', cell: (p) => <span className="font-medium">{p.nome}</span> },
    { key: 'categoria', header: 'Categoria', cell: (p) => p.categoria },
    { key: 'status', header: 'Status', cell: (p) => <StatusBadge active={p.ativo} /> },
    {
      key: 'actions',
      header: <span className="sr-only">Ações</span>,
      className: 'text-right w-[280px]',
      cell: (p) => (
        <div className="flex justify-end gap-2">
          <Button asChild variant="ghost" size="sm">
            <Link to={`/fichas-tecnicas?produtoId=${p.id}`}>
              <ClipboardList className="h-4 w-4" /> Ficha técnica
            </Link>
          </Button>
          <Button variant="ghost" size="sm" onClick={() => { setEditing(p); setDialogOpen(true); }}>
            <Pencil className="h-4 w-4" /> Editar
          </Button>
          <Button
            variant="ghost"
            size="sm"
            disabled={togglingId === p.id}
            onClick={() => (p.ativo ? desativarMutation.mutate(p.id) : ativarMutation.mutate(p.id))}
          >
            <Power className="h-4 w-4" /> {p.ativo ? 'Desativar' : 'Ativar'}
          </Button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Produtos"
        description="Produtos vendáveis pela rede. Cada produto pode ter uma ficha técnica vigente."
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
          <Input
            id="filtro-categoria"
            placeholder="Ex.: Pizzas"
            value={categoriaFiltro}
            onChange={(e) => setCategoriaFiltro(e.target.value)}
          />
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
        data={produtosQuery.data}
        columns={columns}
        isLoading={produtosQuery.isLoading}
        isError={produtosQuery.isError}
        rowKey={(p) => p.id}
        rowClassName={(p) => (!p.ativo ? 'opacity-60' : '')}
        emptyState={
          <div className="space-y-3">
            <p>Nenhum produto encontrado.</p>
            <Button variant="outline" onClick={() => { setEditing(null); setDialogOpen(true); }}>
              <Plus className="h-4 w-4" /> Cadastrar produto
            </Button>
          </div>
        }
      />

      <ProdutoFormDialog open={dialogOpen} onOpenChange={setDialogOpen} produto={editing} />
    </div>
  );
}
