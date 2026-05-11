import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { AlertCircle, ArrowDownToLine, Pencil, Plus, Power, Search, X } from 'lucide-react';
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
import { Switch } from '@/components/ui/switch';
import { PageHeader } from '@/components/PageHeader';
import { StatusBadge } from '@/components/StatusBadge';
import { DataTable, type ColumnDef } from '@/components/data-table/DataTable';
import { toastError } from '@/lib/toastError';
import { useFilialFiltroStore } from '@/features/filtroGlobal/store';
import { listarPosicao } from '@/features/operacoes/api';

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
  const filialId = useFilialFiltroStore((s) => s.filialId);
  const [editing, setEditing] = useState<Insumo | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);

  // Inputs (não disparam query).
  const [buscaInput, setBuscaInput] = useState('');
  const [categoriaInput, setCategoriaInput] = useState(CATEGORIA_TODAS);
  const [ativoInput, setAtivoInput] = useState(ATIVO_TODOS);
  const [soSemEstoqueInput, setSoSemEstoqueInput] = useState(false);

  // Filtros aplicados (só mudam ao clicar Pesquisar).
  const [filtrosAplicados, setFiltrosAplicados] = useState({
    q: '',
    categoria: CATEGORIA_TODAS,
    ativo: ATIVO_TODOS,
    soSemEstoque: false,
  });

  const filtros = useMemo(
    () => ({
      categoriaId: filtrosAplicados.categoria === CATEGORIA_TODAS ? undefined : filtrosAplicados.categoria,
      ativo: filtrosAplicados.ativo === ATIVO_TODOS ? undefined : filtrosAplicados.ativo === 'true',
      q: filtrosAplicados.q.trim() || undefined,
    }),
    [filtrosAplicados],
  );

  function aplicarFiltros() {
    setFiltrosAplicados({
      q: buscaInput,
      categoria: categoriaInput,
      ativo: ativoInput,
      soSemEstoque: soSemEstoqueInput,
    });
  }

  function limparFiltros() {
    setBuscaInput('');
    setCategoriaInput(CATEGORIA_TODAS);
    setAtivoInput(ATIVO_TODOS);
    setSoSemEstoqueInput(false);
    setFiltrosAplicados({
      q: '',
      categoria: CATEGORIA_TODAS,
      ativo: ATIVO_TODOS,
      soSemEstoque: false,
    });
  }

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

  // Saldo agregado por insumo na filial selecionada — alimenta a coluna
  // "Estoque" e o badge "Sem estoque". Sem filial selecionada não busca.
  const posicaoQuery = useQuery({
    queryKey: ['posicao', { filialId, all: true }],
    queryFn: () => listarPosicao(filialId),
    enabled: filialId != null,
  });
  const saldoPorInsumo = useMemo(() => {
    const m = new Map<string, number>();
    (posicaoQuery.data ?? []).forEach((p) => {
      m.set(p.insumoId, (m.get(p.insumoId) ?? 0) + p.saldoTotal);
    });
    return m;
  }, [posicaoQuery.data]);

  // Lista filtrada client-side: apenas para o toggle "só sem estoque"
  // (saldo vive no frontend depois do join com posição). Filtros de
  // categoria/ativo/q continuam server-side via insumosQuery acima.
  const insumosFiltrados = useMemo(() => {
    const base = insumosQuery.data ?? [];
    if (!filtrosAplicados.soSemEstoque || filialId == null) return base;
    return base.filter((i) => (saldoPorInsumo.get(i.id) ?? 0) <= 0);
  }, [insumosQuery.data, filtrosAplicados.soSemEstoque, filialId, saldoPorInsumo]);

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
      key: 'estoque',
      header: 'Estoque',
      className: 'w-[160px]',
      cell: (i) => {
        if (filialId == null) {
          return <span className="text-xs text-muted-foreground">—</span>;
        }
        if (posicaoQuery.isLoading) {
          return <span className="text-xs text-muted-foreground">…</span>;
        }
        const saldo = saldoPorInsumo.get(i.id) ?? 0;
        const unidade = unidadeMap.get(i.unidadeBaseId) ?? '';
        if (saldo <= 0) {
          return (
            <span className="inline-flex items-center gap-1 rounded-md bg-destructive/10 px-2 py-0.5 text-xs font-medium text-destructive">
              <AlertCircle className="h-3 w-3" />
              Sem estoque
            </span>
          );
        }
        const fmt = saldo.toLocaleString('pt-BR', { minimumFractionDigits: 0, maximumFractionDigits: 3 });
        return <span className="text-sm font-medium">{fmt} {unidade}</span>;
      },
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
      className: 'text-right w-[340px]',
      cell: (i) => {
        const semSaldo = filialId != null && (saldoPorInsumo.get(i.id) ?? 0) <= 0;
        return (
          <div className="flex justify-end gap-2">
            {i.ativo && semSaldo && (
              <Button asChild variant="ghost" size="sm" className="text-primary hover:text-primary">
                <Link to={`/movimentacoes?tab=entrada&insumoId=${i.id}`}>
                  <ArrowDownToLine className="h-4 w-4" /> Lançar entrada
                </Link>
              </Button>
            )}
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
        );
      },
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
        <div className="flex items-center gap-2 pt-1">
          <Switch
            id="filtro-sem-estoque"
            checked={soSemEstoqueInput}
            onCheckedChange={setSoSemEstoqueInput}
            disabled={filialId == null}
          />
          <Label htmlFor="filtro-sem-estoque" className="cursor-pointer text-sm">
            Só sem estoque
          </Label>
          {filialId == null && (
            <span className="text-xs text-muted-foreground">
              (selecione uma filial no topo pra usar)
            </span>
          )}
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
        data={insumosFiltrados}
        columns={columns}
        isLoading={insumosQuery.isLoading}
        isError={insumosQuery.isError}
        rowKey={(i) => i.id}
        rowClassName={(i) => {
          if (!i.ativo) return 'opacity-60';
          // Linha levemente avermelhada pra produto ativo sem saldo na filial.
          const saldo = filialId ? saldoPorInsumo.get(i.id) ?? 0 : null;
          return saldo != null && saldo <= 0 ? 'bg-destructive/[0.03]' : '';
        }}
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
