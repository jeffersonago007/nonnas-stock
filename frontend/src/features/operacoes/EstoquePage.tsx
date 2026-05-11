import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { AlertTriangle, CalendarClock, Search, X } from 'lucide-react';

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
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { PageHeader } from '@/components/PageHeader';
import { DataTable, type ColumnDef } from '@/components/data-table/DataTable';
import { useFilialFiltroStore } from '@/features/filtroGlobal/store';
import { listarCategorias, listarInsumos, listarUnidades } from '@/features/cadastros/insumos/api';

import {
  type PosicaoEstoque,
  listarPosicao,
  listarRuptura,
  listarVencimentos,
} from './api';

const CATEGORIA_TODAS = '__todas__';

/**
 * Saldo amigável: remove zeros à direita ("1,000" → "1") e justapõe a sigla
 * da unidade-base ("1 UN", "12,5 KG"). Evita confusão com a notação anglo
 * (onde "1,000" = mil).
 */
function fmtSaldo(n: number, unidade?: string) {
  const num = n.toLocaleString('pt-BR', { minimumFractionDigits: 0, maximumFractionDigits: 3 });
  return unidade ? `${num} ${unidade}` : num;
}
function fmtMoney(n: number) {
  return n.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

export function EstoquePage() {
  const filialId = useFilialFiltroStore((s) => s.filialId);

  // Inputs (não disparam query/filtragem).
  const [buscaInput, setBuscaInput] = useState('');
  const [categoriaInput, setCategoriaInput] = useState(CATEGORIA_TODAS);

  // Filtros aplicados (atualizam só ao clicar Pesquisar — alinha com Cardápio).
  const [filtros, setFiltros] = useState({ q: '', categoriaId: CATEGORIA_TODAS });

  const posicaoQuery = useQuery({
    queryKey: ['posicao', { filialId, categoriaId: filtros.categoriaId }],
    queryFn: () => listarPosicao(filialId, filtros.categoriaId === CATEGORIA_TODAS ? undefined : filtros.categoriaId),
  });

  const rupturaQuery = useQuery({
    queryKey: ['ruptura', filialId],
    queryFn: () => listarRuptura(filialId),
  });

  const vencimentoQuery = useQuery({
    queryKey: ['vencimento', filialId],
    queryFn: () => listarVencimentos(filialId, 30),
  });

  const categoriasQuery = useQuery({
    queryKey: ['categorias-insumo'],
    queryFn: listarCategorias,
  });

  const insumosQuery = useQuery({
    queryKey: ['insumos', { ativo: undefined }],
    queryFn: () => listarInsumos({}),
  });
  const unidadesQuery = useQuery({
    queryKey: ['unidades-medida'],
    queryFn: listarUnidades,
  });

  const unidadePorInsumo = useMemo(() => {
    const unidades: Record<string, string> = {};
    (unidadesQuery.data ?? []).forEach((u) => {
      unidades[u.id] = u.codigo;
    });
    const map: Record<string, string> = {};
    (insumosQuery.data ?? []).forEach((i) => {
      map[i.id] = unidades[i.unidadeBaseId] ?? '';
    });
    return map;
  }, [insumosQuery.data, unidadesQuery.data]);

  const rupturaSet = useMemo(() => {
    return new Set(rupturaQuery.data?.map((r) => `${r.filialId}|${r.insumoId}`));
  }, [rupturaQuery.data]);

  const proximoVencimentoPorInsumo = useMemo(() => {
    const m = new Map<string, number>();
    vencimentoQuery.data?.forEach((v) => {
      const key = `${v.filialId}|${v.insumoId}`;
      const atual = m.get(key);
      if (atual === undefined || v.diasParaVencer < atual) m.set(key, v.diasParaVencer);
    });
    return m;
  }, [vencimentoQuery.data]);

  const filtradas = useMemo(() => {
    const termo = filtros.q.trim().toLowerCase();
    return (posicaoQuery.data ?? []).filter((p) => {
      if (!termo) return true;
      return p.nome.toLowerCase().includes(termo) || p.codigo.toLowerCase().includes(termo);
    });
  }, [posicaoQuery.data, filtros.q]);

  function aplicarFiltros() {
    setFiltros({ q: buscaInput, categoriaId: categoriaInput });
  }

  function limparFiltros() {
    setBuscaInput('');
    setCategoriaInput(CATEGORIA_TODAS);
    setFiltros({ q: '', categoriaId: CATEGORIA_TODAS });
  }

  const totalValor = useMemo(
    () => filtradas.reduce((acc, p) => acc + (p.valorEstoque || 0), 0),
    [filtradas],
  );

  const columns: ColumnDef<PosicaoEstoque>[] = [
    { key: 'codigo', header: 'Código', cell: (p) => <code className="text-xs">{p.codigo}</code>, className: 'w-[120px]' },
    { key: 'nome', header: 'Insumo', cell: (p) => <span className="font-medium">{p.nome}</span> },
    {
      key: 'saldo',
      header: 'Saldo total',
      cell: (p) => <span className="font-mono">{fmtSaldo(p.saldoTotal, unidadePorInsumo[p.insumoId])}</span>,
      className: 'text-right w-[160px]',
    },
    {
      key: 'lotes',
      header: 'Lotes',
      cell: (p) => <span className="font-mono text-muted-foreground">{p.quantidadeLotes}</span>,
      className: 'text-right w-[80px]',
    },
    {
      key: 'valor',
      header: 'Valor estoque',
      cell: (p) => <span className="font-mono">{fmtMoney(p.valorEstoque)}</span>,
      className: 'text-right w-[160px]',
    },
    {
      key: 'flags',
      header: 'Indicadores',
      cell: (p) => {
        const key = `${p.filialId}|${p.insumoId}`;
        const ruptura = rupturaSet.has(key);
        const dias = proximoVencimentoPorInsumo.get(key);
        return (
          <div className="flex flex-wrap gap-1">
            {ruptura && (
              <span className="inline-flex items-center gap-1 rounded-full bg-destructive/10 px-2 py-0.5 text-xs font-medium text-destructive">
                <AlertTriangle className="h-3 w-3" /> Ruptura
              </span>
            )}
            {dias !== undefined && dias <= 30 && (
              <span className="inline-flex items-center gap-1 rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-900">
                <CalendarClock className="h-3 w-3" /> Vence em {dias}d
              </span>
            )}
          </div>
        );
      },
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Estoque"
        description="Saldo materializado por filial e insumo. Indicadores de ruptura e vencimento próximos."
      />

      <div className="grid gap-4 md:grid-cols-3">
        <SummaryCard
          label="Itens com saldo"
          value={posicaoQuery.data?.length ?? 0}
          description="Insumos com saldo registrado"
        />
        <SummaryCard
          label="Em ruptura"
          value={rupturaQuery.data?.length ?? 0}
          description="Saldo abaixo do mínimo"
          tone="danger"
        />
        <SummaryCard
          label="Vencendo em 30d"
          value={vencimentoQuery.data?.length ?? 0}
          description="Lotes próximos da data de validade"
          tone="warning"
        />
      </div>

      <form
        className="space-y-3 rounded-md border bg-card p-4"
        onSubmit={(e) => {
          e.preventDefault();
          aplicarFiltros();
        }}
      >
        <div className="grid gap-3 md:grid-cols-3">
          <div className="space-y-1.5 md:col-span-2">
            <Label htmlFor="filtro-busca">Buscar insumo</Label>
            <div className="relative">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                id="filtro-busca"
                placeholder="Nome ou código"
                className="pl-9"
                value={buscaInput}
                onChange={(e) => setBuscaInput(e.target.value)}
              />
            </div>
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
        isLoading={posicaoQuery.isLoading}
        isError={posicaoQuery.isError}
        rowKey={(p) => `${p.filialId}|${p.insumoId}`}
        emptyState={
          <p>
            Nenhuma posição de estoque encontrada para os filtros atuais.{' '}
            {!filialId && 'Considere selecionar uma filial específica no header.'}
          </p>
        }
      />

      {filtradas.length > 0 && (
        <p className="text-right text-sm text-muted-foreground">
          Total estoque exibido: <strong>{fmtMoney(totalValor)}</strong>
        </p>
      )}
    </div>
  );
}

interface SummaryCardProps {
  label: string;
  value: number;
  description: string;
  tone?: 'normal' | 'danger' | 'warning';
}

function SummaryCard({ label, value, description, tone = 'normal' }: SummaryCardProps) {
  const valueClass =
    tone === 'danger'
      ? 'text-destructive'
      : tone === 'warning'
        ? 'text-amber-600'
        : 'text-foreground';
  return (
    <Card>
      <CardHeader className="pb-2">
        <CardDescription>{label}</CardDescription>
        <CardTitle className={`text-3xl ${valueClass}`}>{value}</CardTitle>
      </CardHeader>
      <CardContent>
        <p className="text-xs text-muted-foreground">{description}</p>
      </CardContent>
    </Card>
  );
}
