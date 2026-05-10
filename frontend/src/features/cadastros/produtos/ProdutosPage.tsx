import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ClipboardList, Pencil, Plus, Power, Search, X } from 'lucide-react';
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
  listarCategoriasProduto,
  listarProdutos,
} from './api';
import { listarCategorias as listarCategoriasInsumo } from '@/features/cadastros/insumos/api';
import { ProdutoFormDialog } from './ProdutoFormDialog';

const ATIVO_TODOS = '__todos__';
const CATEGORIA_TODAS = '__todas__';

interface FiltrosAplicados {
  q?: string;
  categoria?: string;
  ativo?: boolean;
}

export function ProdutosPage() {
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<Produto | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);

  // Estado dos campos do formulário (não dispara query)
  const [buscaInput, setBuscaInput] = useState('');
  const [categoriaInput, setCategoriaInput] = useState(CATEGORIA_TODAS);
  const [ativoInput, setAtivoInput] = useState(ATIVO_TODOS);

  // Filtros efetivamente aplicados (só atualiza quando o usuário clica Pesquisar)
  const [filtros, setFiltros] = useState<FiltrosAplicados>({});

  // Combo unificado: une categorias já em uso por produtos + categorias
  // do admin (catalog/CategoriaInsumo). Decisão consciente do MVP — pro
  // operador do restaurante "categoria" é uma só, embora o domínio separe
  // matéria-prima (insumo) de item vendido (produto).
  const categoriasProdutoQuery = useQuery({
    queryKey: ['produtos-categorias'],
    queryFn: listarCategoriasProduto,
  });
  const categoriasAdminQuery = useQuery({
    queryKey: ['categorias-insumo'],
    queryFn: listarCategoriasInsumo,
  });
  const categoriasUnificadas = useMemo(() => {
    const set = new Set<string>();
    (categoriasAdminQuery.data ?? []).filter((c) => c.ativa).forEach((c) => set.add(c.nome));
    (categoriasProdutoQuery.data ?? []).forEach((c) => set.add(c));
    return Array.from(set).sort((a, b) => a.localeCompare(b, 'pt-BR'));
  }, [categoriasAdminQuery.data, categoriasProdutoQuery.data]);

  const produtosQuery = useQuery({
    queryKey: ['produtos', filtros],
    queryFn: () => listarProdutos(filtros),
  });

  function aplicarFiltros() {
    setFiltros({
      q: buscaInput.trim() || undefined,
      categoria: categoriaInput === CATEGORIA_TODAS ? undefined : categoriaInput,
      ativo: ativoInput === ATIVO_TODOS ? undefined : ativoInput === 'true',
    });
  }

  function limparFiltros() {
    setBuscaInput('');
    setCategoriaInput(CATEGORIA_TODAS);
    setAtivoInput(ATIVO_TODOS);
    setFiltros({});
  }

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

      <form
        className="space-y-3 rounded-md border bg-card p-4"
        onSubmit={(e) => {
          e.preventDefault();
          aplicarFiltros();
        }}
      >
        <div className="grid gap-3 md:grid-cols-3">
          <div className="space-y-1.5">
            <Label htmlFor="filtro-busca">Buscar</Label>
            <Input
              id="filtro-busca"
              placeholder="Nome ou código"
              value={buscaInput}
              onChange={(e) => setBuscaInput(e.target.value)}
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="filtro-categoria">Categoria</Label>
            <Select value={categoriaInput} onValueChange={setCategoriaInput}>
              <SelectTrigger id="filtro-categoria">
                <SelectValue
                  placeholder={categoriasAdminQuery.isLoading || categoriasProdutoQuery.isLoading
                    ? 'Carregando…'
                    : 'Todas'}
                />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={CATEGORIA_TODAS}>Todas</SelectItem>
                {categoriasUnificadas.map((c) => (
                  <SelectItem key={c} value={c}>
                    {c}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="filtro-ativo">Status</Label>
            <Select value={ativoInput} onValueChange={setAtivoInput}>
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
