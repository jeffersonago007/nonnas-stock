import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  AlertTriangle,
  Boxes,
  CalendarClock,
  Download,
  LineChart,
  RefreshCw,
  Scale,
  Search,
  TrendingUp,
  X,
} from 'lucide-react';
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
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { PageHeader } from '@/components/PageHeader';
import { DataTable, type ColumnDef } from '@/components/data-table/DataTable';
import { downloadCsv, type CsvColumn } from '@/lib/csv';
import { toastError } from '@/lib/toastError';

import { useFilialFiltroStore } from '@/features/filtroGlobal/store';
import { useAuthStore } from '@/features/auth/store';
import { hasAnyRole } from '@/routes/RoleGuard';
import { listarCategorias } from '@/features/cadastros/insumos/api';

import {
  type ClasseABC,
  type CurvaABCItem,
  type DivergenciaItem,
  type MovimentacaoResumo,
  type PosicaoEstoque,
  type RupturaItem,
  type SituacaoRuptura,
  type TipoMovimentacao,
  type VencimentoItem,
  listarCurvaABC,
  listarDivergencia,
  listarMovimentacoesPorPeriodo,
  listarPosicao,
  listarRuptura,
  listarVencimentos,
  refreshRelatorios,
} from './api';

const CATEGORIA_TODAS = '__todas__';
const SITUACAO_TODAS = '__todas__';
const TIPO_TODOS = '__todos__';

const TIPOS_MOVIMENTACAO: TipoMovimentacao[] = [
  'ENTRADA_NF',
  'ENTRADA_AJUSTE',
  'ENTRADA_TRANSFERENCIA',
  'ENTRADA_DEVOLUCAO_CLIENTE',
  'ENTRADA_CARGA_INICIAL',
  'SAIDA_VENDA',
  'SAIDA_AJUSTE',
  'SAIDA_TRANSFERENCIA',
  'SAIDA_PERDA',
  'SAIDA_QUEBRA',
  'SAIDA_VENCIMENTO',
];

const SITUACAO_LABELS: Record<SituacaoRuptura, string> = {
  RUPTURA_TOTAL: 'Ruptura total',
  ABAIXO_PONTO_PEDIDO: 'Abaixo do ponto de pedido',
  ABAIXO_MINIMO: 'Abaixo do mínimo',
};

function fmtNumero(n: number | null | undefined): string {
  if (n === null || n === undefined) return '0';
  return n.toLocaleString('pt-BR', { minimumFractionDigits: 0, maximumFractionDigits: 3 });
}

function fmtMoney(n: number | null | undefined): string {
  if (n === null || n === undefined) return 'R$ 0,00';
  return n.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function fmtPercent(n: number | null | undefined): string {
  if (n === null || n === undefined) return '0%';
  return `${n.toLocaleString('pt-BR', { minimumFractionDigits: 0, maximumFractionDigits: 2 })}%`;
}

function fmtData(iso: string | null | undefined): string {
  if (!iso) return '';
  // ISO `yyyy-MM-dd` ou ISO completo — pt-BR sem hora.
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleDateString('pt-BR');
}

function formatTipoLabel(t: string): string {
  return t.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase());
}

function hojeIso(): string {
  return new Date().toISOString().slice(0, 10);
}

function diasAtrasIso(dias: number): string {
  return new Date(Date.now() - dias * 24 * 3600 * 1000).toISOString().slice(0, 10);
}

// ─────────────────────────────── Página ──────────────────────────────────

export function RelatoriosPage() {
  const queryClient = useQueryClient();
  const user = useAuthStore((s) => s.user);
  const podeRefresh = hasAnyRole(user?.perfil, ['ADMIN', 'GERENTE']);

  const refreshMutation = useMutation({
    mutationFn: refreshRelatorios,
    onSuccess: () => {
      toast.success('Views de relatório atualizadas', {
        description: 'Curva ABC e ruptura agora refletem as movimentações mais recentes.',
      });
      // Invalida as queries que dependem das MVs.
      queryClient.invalidateQueries({ queryKey: ['rel-curva-abc'] });
      queryClient.invalidateQueries({ queryKey: ['rel-ruptura'] });
      queryClient.invalidateQueries({ queryKey: ['rel-posicao'] });
      queryClient.invalidateQueries({ queryKey: ['ruptura'] });
      queryClient.invalidateQueries({ queryKey: ['posicao'] });
    },
    onError: (error) => toastError('Não foi possível atualizar as views', error),
  });

  return (
    <div className="space-y-6">
      <PageHeader
        title="Relatórios"
        description="Posição, curva ABC, ruptura, vencimento, movimentações e divergência de inventário."
        actions={
          podeRefresh && (
            <Button
              variant="outline"
              onClick={() => refreshMutation.mutate()}
              disabled={refreshMutation.isPending}
            >
              <RefreshCw className={`h-4 w-4 ${refreshMutation.isPending ? 'animate-spin' : ''}`} />
              {refreshMutation.isPending ? 'Atualizando…' : 'Atualizar dados'}
            </Button>
          )
        }
      />

      <Tabs defaultValue="posicao" className="space-y-4">
        <TabsList className="flex flex-wrap h-auto">
          <TabsTrigger value="posicao">
            <Boxes className="mr-2 h-4 w-4" /> Posição
          </TabsTrigger>
          <TabsTrigger value="curva-abc">
            <TrendingUp className="mr-2 h-4 w-4" /> Curva ABC
          </TabsTrigger>
          <TabsTrigger value="ruptura">
            <AlertTriangle className="mr-2 h-4 w-4" /> Ruptura
          </TabsTrigger>
          <TabsTrigger value="vencimento">
            <CalendarClock className="mr-2 h-4 w-4" /> Vencimento
          </TabsTrigger>
          <TabsTrigger value="movimentacoes">
            <LineChart className="mr-2 h-4 w-4" /> Movimentações
          </TabsTrigger>
          <TabsTrigger value="divergencia">
            <Scale className="mr-2 h-4 w-4" /> Divergência
          </TabsTrigger>
        </TabsList>

        <TabsContent value="posicao">
          <PosicaoTab />
        </TabsContent>
        <TabsContent value="curva-abc">
          <CurvaAbcTab />
        </TabsContent>
        <TabsContent value="ruptura">
          <RupturaTab />
        </TabsContent>
        <TabsContent value="vencimento">
          <VencimentoTab />
        </TabsContent>
        <TabsContent value="movimentacoes">
          <MovimentacoesTab />
        </TabsContent>
        <TabsContent value="divergencia">
          <DivergenciaTab />
        </TabsContent>
      </Tabs>
    </div>
  );
}

// ─────────────────────────────── Posição ─────────────────────────────────

function PosicaoTab() {
  const filialId = useFilialFiltroStore((s) => s.filialId);
  const [categoriaInput, setCategoriaInput] = useState(CATEGORIA_TODAS);
  const [filtros, setFiltros] = useState({ categoriaId: CATEGORIA_TODAS });

  const categoriasQuery = useQuery({
    queryKey: ['categorias-insumo'],
    queryFn: listarCategorias,
  });

  const query = useQuery({
    queryKey: ['rel-posicao', { filialId, categoriaId: filtros.categoriaId }],
    queryFn: () =>
      listarPosicao(
        filialId,
        filtros.categoriaId === CATEGORIA_TODAS ? undefined : filtros.categoriaId,
      ),
  });

  function aplicar() {
    setFiltros({ categoriaId: categoriaInput });
  }
  function limpar() {
    setCategoriaInput(CATEGORIA_TODAS);
    setFiltros({ categoriaId: CATEGORIA_TODAS });
  }

  const columns: ColumnDef<PosicaoEstoque>[] = [
    { key: 'codigo', header: 'Código', cell: (r) => <code className="text-xs">{r.codigo}</code>, className: 'w-[120px]' },
    { key: 'nome', header: 'Insumo', cell: (r) => <span className="font-medium">{r.nome}</span> },
    {
      key: 'saldo',
      header: 'Saldo total',
      cell: (r) => <span className="font-mono">{fmtNumero(r.saldoTotal)}</span>,
      className: 'text-right w-[140px]',
    },
    {
      key: 'lotes',
      header: 'Lotes',
      cell: (r) => <span className="font-mono text-muted-foreground">{r.quantidadeLotes}</span>,
      className: 'text-right w-[80px]',
    },
    {
      key: 'valor',
      header: 'Valor estoque',
      cell: (r) => <span className="font-mono">{fmtMoney(r.valorEstoque)}</span>,
      className: 'text-right w-[160px]',
    },
  ];

  const csvCols: CsvColumn<PosicaoEstoque>[] = [
    { header: 'Filial', value: (r) => r.filialId },
    { header: 'Código', value: (r) => r.codigo },
    { header: 'Insumo', value: (r) => r.nome },
    { header: 'Saldo total', value: (r) => fmtNumero(r.saldoTotal) },
    { header: 'Lotes', value: (r) => r.quantidadeLotes },
    { header: 'Valor estoque', value: (r) => fmtMoney(r.valorEstoque) },
  ];

  return (
    <div className="space-y-4">
      <form
        className="space-y-3 rounded-md border bg-card p-4"
        onSubmit={(e) => {
          e.preventDefault();
          aplicar();
        }}
      >
        <div className="grid gap-3 md:grid-cols-3">
          <div className="space-y-1.5 md:col-span-2">
            <Label htmlFor="pos-cat">Categoria</Label>
            <Select value={categoriaInput} onValueChange={setCategoriaInput}>
              <SelectTrigger id="pos-cat">
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
          <div className="flex items-end gap-2 justify-end">
            <Button type="button" variant="outline" onClick={limpar}>
              <X className="h-4 w-4" /> Limpar
            </Button>
            <Button type="submit">
              <Search className="h-4 w-4" /> Pesquisar
            </Button>
          </div>
        </div>
      </form>

      <ExportToolbar
        disabled={!query.data?.length}
        onExport={() => downloadCsv('relatorio-posicao', csvCols, query.data ?? [])}
        total={query.data?.length ?? 0}
      />

      <DataTable
        data={query.data}
        columns={columns}
        isLoading={query.isLoading}
        isError={query.isError}
        rowKey={(r) => `${r.filialId}|${r.insumoId}`}
        emptyState={<p>Nenhuma posição de estoque para os filtros atuais.</p>}
      />
    </div>
  );
}

// ────────────────────────────── Curva ABC ────────────────────────────────

function CurvaAbcTab() {
  const filialId = useFilialFiltroStore((s) => s.filialId);
  // Backend não recebe `dias` ainda — a Curva ABC consome a MV. Mantemos o
  // input como informativo para a UX (e potencial param futuro), mas hoje
  // ele só etiqueta o relatório.
  const [diasInput, setDiasInput] = useState('90');
  const [diasFiltro, setDiasFiltro] = useState('90');

  const query = useQuery({
    queryKey: ['rel-curva-abc', { filialId }],
    queryFn: () => listarCurvaABC(filialId),
  });

  function aplicar() {
    setDiasFiltro(diasInput);
  }
  function limpar() {
    setDiasInput('90');
    setDiasFiltro('90');
  }

  const columns: ColumnDef<CurvaABCItem>[] = [
    {
      key: 'classe',
      header: 'Classe',
      cell: (r) => <ClasseABCBadge classe={r.classe} />,
      className: 'w-[90px]',
    },
    { key: 'codigo', header: 'Código', cell: (r) => <code className="text-xs">{r.codigo}</code>, className: 'w-[120px]' },
    { key: 'nome', header: 'Insumo', cell: (r) => <span className="font-medium">{r.nome}</span> },
    {
      key: 'qtd',
      header: 'Quantidade',
      cell: (r) => <span className="font-mono">{fmtNumero(r.quantidadeTotal)}</span>,
      className: 'text-right w-[140px]',
    },
    {
      key: 'valor',
      header: 'Valor total',
      cell: (r) => <span className="font-mono">{fmtMoney(r.valorTotal)}</span>,
      className: 'text-right w-[160px]',
    },
    {
      key: 'acumulado',
      header: '% acumulado',
      cell: (r) => <span className="font-mono text-muted-foreground">{fmtPercent(r.percentualAcumulado)}</span>,
      className: 'text-right w-[120px]',
    },
  ];

  const csvCols: CsvColumn<CurvaABCItem>[] = [
    { header: 'Classe', value: (r) => r.classe },
    { header: 'Código', value: (r) => r.codigo },
    { header: 'Insumo', value: (r) => r.nome },
    { header: 'Quantidade', value: (r) => fmtNumero(r.quantidadeTotal) },
    { header: 'Valor total', value: (r) => fmtMoney(r.valorTotal) },
    { header: '% acumulado', value: (r) => fmtPercent(r.percentualAcumulado) },
  ];

  return (
    <div className="space-y-4">
      <form
        className="space-y-3 rounded-md border bg-card p-4"
        onSubmit={(e) => {
          e.preventDefault();
          aplicar();
        }}
      >
        <div className="grid gap-3 md:grid-cols-3">
          <div className="space-y-1.5">
            <Label htmlFor="abc-dias">Período (dias)</Label>
            <Input
              id="abc-dias"
              type="number"
              step={1}
              min={1}
              value={diasInput}
              onChange={(e) => setDiasInput(e.target.value)}
            />
            <p className="text-xs text-muted-foreground">
              Etiqueta do relatório — janela atual: {diasFiltro} d.
            </p>
          </div>
          <div className="md:col-span-2 flex items-end gap-2 justify-end">
            <Button type="button" variant="outline" onClick={limpar}>
              <X className="h-4 w-4" /> Limpar
            </Button>
            <Button type="submit">
              <Search className="h-4 w-4" /> Pesquisar
            </Button>
          </div>
        </div>
      </form>

      <ExportToolbar
        disabled={!query.data?.length}
        onExport={() => downloadCsv('relatorio-curva-abc', csvCols, query.data ?? [])}
        total={query.data?.length ?? 0}
      />

      <DataTable
        data={query.data}
        columns={columns}
        isLoading={query.isLoading}
        isError={query.isError}
        rowKey={(r) => `${r.filialId}|${r.insumoId}`}
        emptyState={
          <p>
            Nenhum item classificado. Use o botão "Atualizar dados" no topo se a janela
            estiver desatualizada.
          </p>
        }
      />
    </div>
  );
}

function ClasseABCBadge({ classe }: { classe: ClasseABC }) {
  const styles: Record<ClasseABC, string> = {
    A: 'bg-emerald-100 text-emerald-900',
    B: 'bg-amber-100 text-amber-900',
    C: 'bg-muted text-muted-foreground',
  };
  return (
    <span className={`inline-flex items-center justify-center rounded-full px-2 py-0.5 text-xs font-semibold ${styles[classe]}`}>
      {classe}
    </span>
  );
}

// ─────────────────────────────── Ruptura ─────────────────────────────────

function RupturaTab() {
  const filialId = useFilialFiltroStore((s) => s.filialId);
  const [situacaoInput, setSituacaoInput] = useState<string>(SITUACAO_TODAS);
  const [situacaoFiltro, setSituacaoFiltro] = useState<string>(SITUACAO_TODAS);

  const query = useQuery({
    queryKey: ['rel-ruptura', { filialId }],
    queryFn: () => listarRuptura(filialId),
  });

  const filtradas = useMemo(() => {
    if (situacaoFiltro === SITUACAO_TODAS) return query.data;
    return query.data?.filter((r) => r.situacao === situacaoFiltro);
  }, [query.data, situacaoFiltro]);

  function aplicar() {
    setSituacaoFiltro(situacaoInput);
  }
  function limpar() {
    setSituacaoInput(SITUACAO_TODAS);
    setSituacaoFiltro(SITUACAO_TODAS);
  }

  const columns: ColumnDef<RupturaItem>[] = [
    { key: 'codigo', header: 'Código', cell: (r) => <code className="text-xs">{r.codigo}</code>, className: 'w-[120px]' },
    { key: 'nome', header: 'Insumo', cell: (r) => <span className="font-medium">{r.nome}</span> },
    {
      key: 'saldo',
      header: 'Saldo',
      cell: (r) => <span className="font-mono">{fmtNumero(r.saldoTotal)}</span>,
      className: 'text-right w-[120px]',
    },
    {
      key: 'minimo',
      header: 'Mínimo',
      cell: (r) => <span className="font-mono text-muted-foreground">{fmtNumero(r.estoqueMinimo)}</span>,
      className: 'text-right w-[120px]',
    },
    {
      key: 'ponto',
      header: 'Ponto pedido',
      cell: (r) => <span className="font-mono text-muted-foreground">{fmtNumero(r.pontoPedido)}</span>,
      className: 'text-right w-[120px]',
    },
    {
      key: 'situacao',
      header: 'Situação',
      cell: (r) => <SituacaoBadge situacao={r.situacao} />,
    },
  ];

  const csvCols: CsvColumn<RupturaItem>[] = [
    { header: 'Código', value: (r) => r.codigo },
    { header: 'Insumo', value: (r) => r.nome },
    { header: 'Saldo', value: (r) => fmtNumero(r.saldoTotal) },
    { header: 'Mínimo', value: (r) => fmtNumero(r.estoqueMinimo) },
    { header: 'Ponto pedido', value: (r) => fmtNumero(r.pontoPedido) },
    { header: 'Situação', value: (r) => SITUACAO_LABELS[r.situacao] },
  ];

  return (
    <div className="space-y-4">
      <form
        className="space-y-3 rounded-md border bg-card p-4"
        onSubmit={(e) => {
          e.preventDefault();
          aplicar();
        }}
      >
        <div className="grid gap-3 md:grid-cols-3">
          <div className="space-y-1.5 md:col-span-2">
            <Label htmlFor="rup-sit">Situação</Label>
            <Select value={situacaoInput} onValueChange={setSituacaoInput}>
              <SelectTrigger id="rup-sit">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={SITUACAO_TODAS}>Todas</SelectItem>
                <SelectItem value="RUPTURA_TOTAL">Ruptura total</SelectItem>
                <SelectItem value="ABAIXO_PONTO_PEDIDO">Abaixo do ponto de pedido</SelectItem>
                <SelectItem value="ABAIXO_MINIMO">Abaixo do mínimo</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div className="flex items-end gap-2 justify-end">
            <Button type="button" variant="outline" onClick={limpar}>
              <X className="h-4 w-4" /> Limpar
            </Button>
            <Button type="submit">
              <Search className="h-4 w-4" /> Pesquisar
            </Button>
          </div>
        </div>
      </form>

      <ExportToolbar
        disabled={!filtradas?.length}
        onExport={() => downloadCsv('relatorio-ruptura', csvCols, filtradas ?? [])}
        total={filtradas?.length ?? 0}
      />

      <DataTable
        data={filtradas}
        columns={columns}
        isLoading={query.isLoading}
        isError={query.isError}
        rowKey={(r) => `${r.filialId}|${r.insumoId}`}
        emptyState={
          <p>Tudo dentro dos mínimos configurados — nenhum item em ruptura agora.</p>
        }
      />
    </div>
  );
}

function SituacaoBadge({ situacao }: { situacao: SituacaoRuptura }) {
  const styles: Record<SituacaoRuptura, string> = {
    RUPTURA_TOTAL: 'bg-destructive/10 text-destructive',
    ABAIXO_PONTO_PEDIDO: 'bg-amber-100 text-amber-900',
    ABAIXO_MINIMO: 'bg-orange-100 text-orange-900',
  };
  return (
    <span className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium ${styles[situacao]}`}>
      {SITUACAO_LABELS[situacao]}
    </span>
  );
}

// ────────────────────────────── Vencimento ───────────────────────────────

function VencimentoTab() {
  const filialId = useFilialFiltroStore((s) => s.filialId);
  const [diasInput, setDiasInput] = useState('30');
  const [diasFiltro, setDiasFiltro] = useState(30);

  const query = useQuery({
    queryKey: ['rel-vencimento', { filialId, diasFiltro }],
    queryFn: () => listarVencimentos(filialId, diasFiltro),
  });

  function aplicar() {
    const n = Number(diasInput);
    if (!Number.isFinite(n) || n <= 0) {
      toast.error('Informe um número positivo de dias');
      return;
    }
    setDiasFiltro(n);
  }
  function limpar() {
    setDiasInput('30');
    setDiasFiltro(30);
  }

  const columns: ColumnDef<VencimentoItem>[] = [
    { key: 'codigo', header: 'Código', cell: (r) => <code className="text-xs">{r.codigo}</code>, className: 'w-[120px]' },
    { key: 'nome', header: 'Insumo', cell: (r) => <span className="font-medium">{r.nome}</span> },
    {
      key: 'lote',
      header: 'Lote',
      cell: (r) => <span className="font-mono text-xs">{r.numeroLote || '—'}</span>,
      className: 'w-[140px]',
    },
    {
      key: 'validade',
      header: 'Validade',
      cell: (r) => <span>{fmtData(r.dataValidade)}</span>,
      className: 'w-[110px]',
    },
    {
      key: 'dias',
      header: 'Dias p/ vencer',
      cell: (r) => <DiasVencerBadge dias={r.diasParaVencer} />,
      className: 'text-right w-[130px]',
    },
    {
      key: 'saldo',
      header: 'Saldo',
      cell: (r) => <span className="font-mono">{fmtNumero(r.saldo)}</span>,
      className: 'text-right w-[120px]',
    },
    {
      key: 'valor',
      header: 'Valor unit.',
      cell: (r) => <span className="font-mono text-muted-foreground">{fmtMoney(r.valorUnitario)}</span>,
      className: 'text-right w-[140px]',
    },
  ];

  const csvCols: CsvColumn<VencimentoItem>[] = [
    { header: 'Código', value: (r) => r.codigo },
    { header: 'Insumo', value: (r) => r.nome },
    { header: 'Lote', value: (r) => r.numeroLote || '' },
    { header: 'Validade', value: (r) => fmtData(r.dataValidade) },
    { header: 'Dias p/ vencer', value: (r) => r.diasParaVencer },
    { header: 'Saldo', value: (r) => fmtNumero(r.saldo) },
    { header: 'Valor unitário', value: (r) => fmtMoney(r.valorUnitario) },
  ];

  return (
    <div className="space-y-4">
      <form
        className="space-y-3 rounded-md border bg-card p-4"
        onSubmit={(e) => {
          e.preventDefault();
          aplicar();
        }}
      >
        <div className="grid gap-3 md:grid-cols-3">
          <div className="space-y-1.5">
            <Label htmlFor="venc-dias">Dias-limite</Label>
            <Input
              id="venc-dias"
              type="number"
              step={1}
              min={1}
              value={diasInput}
              onChange={(e) => setDiasInput(e.target.value)}
            />
          </div>
          <div className="md:col-span-2 flex items-end gap-2 justify-end">
            <Button type="button" variant="outline" onClick={limpar}>
              <X className="h-4 w-4" /> Limpar
            </Button>
            <Button type="submit">
              <Search className="h-4 w-4" /> Pesquisar
            </Button>
          </div>
        </div>
      </form>

      <ExportToolbar
        disabled={!query.data?.length}
        onExport={() => downloadCsv('relatorio-vencimento', csvCols, query.data ?? [])}
        total={query.data?.length ?? 0}
      />

      <DataTable
        data={query.data}
        columns={columns}
        isLoading={query.isLoading}
        isError={query.isError}
        rowKey={(r) => r.loteId}
        emptyState={<p>Nenhum lote vence em até {diasFiltro} dias.</p>}
      />
    </div>
  );
}

function DiasVencerBadge({ dias }: { dias: number }) {
  let tone: string;
  if (dias <= 0) tone = 'bg-destructive/10 text-destructive';
  else if (dias <= 7) tone = 'bg-orange-100 text-orange-900';
  else if (dias <= 30) tone = 'bg-amber-100 text-amber-900';
  else tone = 'bg-muted text-muted-foreground';
  return (
    <span className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 font-mono text-xs font-medium ${tone}`}>
      {dias}d
    </span>
  );
}

// ──────────────────────────── Movimentações ──────────────────────────────

function MovimentacoesTab() {
  const filialId = useFilialFiltroStore((s) => s.filialId);

  const [inicioInput, setInicioInput] = useState(diasAtrasIso(30));
  const [fimInput, setFimInput] = useState(hojeIso());
  const [tipoInput, setTipoInput] = useState<string>(TIPO_TODOS);
  const [filtros, setFiltros] = useState({
    inicio: diasAtrasIso(30),
    fim: hojeIso(),
    tipo: TIPO_TODOS,
  });

  const inicioIso = useMemo(() => `${filtros.inicio}T00:00:00Z`, [filtros.inicio]);
  const fimIso = useMemo(() => `${filtros.fim}T23:59:59Z`, [filtros.fim]);

  const query = useQuery({
    queryKey: ['rel-movimentacoes', { filialId, inicioIso, fimIso, tipo: filtros.tipo }],
    queryFn: () =>
      listarMovimentacoesPorPeriodo({
        filialId,
        inicio: inicioIso,
        fim: fimIso,
        tipo: filtros.tipo === TIPO_TODOS ? undefined : filtros.tipo,
      }),
    enabled: Boolean(filtros.inicio && filtros.fim),
  });

  function aplicar() {
    setFiltros({ inicio: inicioInput, fim: fimInput, tipo: tipoInput });
  }
  function limpar() {
    const i = diasAtrasIso(30);
    const f = hojeIso();
    setInicioInput(i);
    setFimInput(f);
    setTipoInput(TIPO_TODOS);
    setFiltros({ inicio: i, fim: f, tipo: TIPO_TODOS });
  }

  const columns: ColumnDef<MovimentacaoResumo>[] = [
    { key: 'codigo', header: 'Código', cell: (r) => <code className="text-xs">{r.codigo}</code>, className: 'w-[120px]' },
    { key: 'nome', header: 'Insumo', cell: (r) => <span className="font-medium">{r.nome}</span> },
    {
      key: 'tipo',
      header: 'Tipo',
      cell: (r) => <span className="text-sm">{formatTipoLabel(r.tipoMovimentacao)}</span>,
      className: 'w-[200px]',
    },
    {
      key: 'qtdMovs',
      header: 'Movs',
      cell: (r) => <span className="font-mono text-muted-foreground">{r.quantidadeMovimentacoes}</span>,
      className: 'text-right w-[80px]',
    },
    {
      key: 'qtdTotal',
      header: 'Qtd total',
      cell: (r) => <span className="font-mono">{fmtNumero(r.quantidadeTotal)}</span>,
      className: 'text-right w-[140px]',
    },
    {
      key: 'valor',
      header: 'Valor total',
      cell: (r) => <span className="font-mono">{fmtMoney(r.valorTotal)}</span>,
      className: 'text-right w-[160px]',
    },
  ];

  const csvCols: CsvColumn<MovimentacaoResumo>[] = [
    { header: 'Código', value: (r) => r.codigo },
    { header: 'Insumo', value: (r) => r.nome },
    { header: 'Tipo', value: (r) => formatTipoLabel(r.tipoMovimentacao) },
    { header: 'Movimentações', value: (r) => r.quantidadeMovimentacoes },
    { header: 'Quantidade total', value: (r) => fmtNumero(r.quantidadeTotal) },
    { header: 'Valor total', value: (r) => fmtMoney(r.valorTotal) },
  ];

  return (
    <div className="space-y-4">
      <form
        className="space-y-3 rounded-md border bg-card p-4"
        onSubmit={(e) => {
          e.preventDefault();
          aplicar();
        }}
      >
        <div className="grid gap-3 md:grid-cols-3">
          <div className="space-y-1.5">
            <Label htmlFor="mov-inicio">Início</Label>
            <Input
              id="mov-inicio"
              type="date"
              value={inicioInput}
              onChange={(e) => setInicioInput(e.target.value)}
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="mov-fim">Fim</Label>
            <Input
              id="mov-fim"
              type="date"
              value={fimInput}
              onChange={(e) => setFimInput(e.target.value)}
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="mov-tipo">Tipo</Label>
            <Select value={tipoInput} onValueChange={setTipoInput}>
              <SelectTrigger id="mov-tipo">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={TIPO_TODOS}>Todos</SelectItem>
                {TIPOS_MOVIMENTACAO.map((t) => (
                  <SelectItem key={t} value={t}>
                    {formatTipoLabel(t)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </div>
        <div className="flex justify-end gap-2 pt-1">
          <Button type="button" variant="outline" onClick={limpar}>
            <X className="h-4 w-4" /> Limpar
          </Button>
          <Button type="submit">
            <Search className="h-4 w-4" /> Pesquisar
          </Button>
        </div>
      </form>

      <ExportToolbar
        disabled={!query.data?.length}
        onExport={() => downloadCsv('relatorio-movimentacoes', csvCols, query.data ?? [])}
        total={query.data?.length ?? 0}
      />

      <DataTable
        data={query.data}
        columns={columns}
        isLoading={query.isLoading}
        isError={query.isError}
        rowKey={(r) => `${r.filialId}|${r.insumoId}|${r.tipoMovimentacao}`}
        emptyState={<p>Nenhuma movimentação no período e tipo selecionados.</p>}
      />
    </div>
  );
}

// ────────────────────────────── Divergência ──────────────────────────────

function DivergenciaTab() {
  const filialId = useFilialFiltroStore((s) => s.filialId);

  // Divergência usa janela de aprovação dos ajustes — default 90 dias.
  const [inicioInput, setInicioInput] = useState(diasAtrasIso(90));
  const [fimInput, setFimInput] = useState(hojeIso());
  const [filtros, setFiltros] = useState({ inicio: diasAtrasIso(90), fim: hojeIso() });

  const inicioIso = useMemo(() => `${filtros.inicio}T00:00:00Z`, [filtros.inicio]);
  const fimIso = useMemo(() => `${filtros.fim}T23:59:59Z`, [filtros.fim]);

  const query = useQuery({
    queryKey: ['rel-divergencia', { filialId, inicioIso, fimIso }],
    queryFn: () => listarDivergencia({ filialId, inicio: inicioIso, fim: fimIso }),
    enabled: Boolean(filtros.inicio && filtros.fim),
  });

  function aplicar() {
    setFiltros({ inicio: inicioInput, fim: fimInput });
  }
  function limpar() {
    const i = diasAtrasIso(90);
    const f = hojeIso();
    setInicioInput(i);
    setFimInput(f);
    setFiltros({ inicio: i, fim: f });
  }

  const columns: ColumnDef<DivergenciaItem>[] = [
    { key: 'codigo', header: 'Código', cell: (r) => <code className="text-xs">{r.codigo}</code>, className: 'w-[120px]' },
    { key: 'nome', header: 'Insumo', cell: (r) => <span className="font-medium">{r.nome}</span> },
    {
      key: 'ajustes',
      header: 'Ajustes',
      cell: (r) => <span className="font-mono text-muted-foreground">{r.quantidadeAjustes}</span>,
      className: 'text-right w-[100px]',
    },
    {
      key: 'positiva',
      header: 'Diff positiva',
      cell: (r) => <span className="font-mono text-emerald-700">+{fmtNumero(r.quantidadeDiffPositiva)}</span>,
      className: 'text-right w-[140px]',
    },
    {
      key: 'negativa',
      header: 'Diff negativa',
      cell: (r) => <span className="font-mono text-destructive">-{fmtNumero(r.quantidadeDiffNegativa)}</span>,
      className: 'text-right w-[140px]',
    },
    {
      key: 'liquida',
      header: 'Diff líquida',
      cell: (r) => {
        const v = r.quantidadeDiffLiquida;
        const tone = v > 0 ? 'text-emerald-700' : v < 0 ? 'text-destructive' : 'text-muted-foreground';
        const sign = v > 0 ? '+' : '';
        return <span className={`font-mono font-semibold ${tone}`}>{sign}{fmtNumero(v)}</span>;
      },
      className: 'text-right w-[140px]',
    },
  ];

  const csvCols: CsvColumn<DivergenciaItem>[] = [
    { header: 'Código', value: (r) => r.codigo },
    { header: 'Insumo', value: (r) => r.nome },
    { header: 'Ajustes', value: (r) => r.quantidadeAjustes },
    { header: 'Diff positiva', value: (r) => fmtNumero(r.quantidadeDiffPositiva) },
    { header: 'Diff negativa', value: (r) => fmtNumero(r.quantidadeDiffNegativa) },
    { header: 'Diff líquida', value: (r) => fmtNumero(r.quantidadeDiffLiquida) },
  ];

  return (
    <div className="space-y-4">
      <form
        className="space-y-3 rounded-md border bg-card p-4"
        onSubmit={(e) => {
          e.preventDefault();
          aplicar();
        }}
      >
        <div className="grid gap-3 md:grid-cols-3">
          <div className="space-y-1.5">
            <Label htmlFor="div-inicio">Início</Label>
            <Input
              id="div-inicio"
              type="date"
              value={inicioInput}
              onChange={(e) => setInicioInput(e.target.value)}
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="div-fim">Fim</Label>
            <Input
              id="div-fim"
              type="date"
              value={fimInput}
              onChange={(e) => setFimInput(e.target.value)}
            />
          </div>
          <div className="flex items-end gap-2 justify-end">
            <Button type="button" variant="outline" onClick={limpar}>
              <X className="h-4 w-4" /> Limpar
            </Button>
            <Button type="submit">
              <Search className="h-4 w-4" /> Pesquisar
            </Button>
          </div>
        </div>
      </form>

      <ExportToolbar
        disabled={!query.data?.length}
        onExport={() => downloadCsv('relatorio-divergencia', csvCols, query.data ?? [])}
        total={query.data?.length ?? 0}
      />

      <DataTable
        data={query.data}
        columns={columns}
        isLoading={query.isLoading}
        isError={query.isError}
        rowKey={(r) => `${r.filialId}|${r.insumoId}`}
        emptyState={<p>Nenhuma divergência aprovada no período.</p>}
      />
    </div>
  );
}

// ─────────────────────────────── Helpers ─────────────────────────────────

interface ExportToolbarProps {
  disabled: boolean;
  onExport: () => void;
  total: number;
}

function ExportToolbar({ disabled, onExport, total }: ExportToolbarProps) {
  return (
    <div className="flex items-center justify-between">
      <span className="text-sm text-muted-foreground">
        {total > 0 ? `${total} registro${total === 1 ? '' : 's'}` : 'Sem registros'}
      </span>
      <Button type="button" variant="outline" size="sm" disabled={disabled} onClick={onExport}>
        <Download className="h-4 w-4" /> Exportar CSV
      </Button>
    </div>
  );
}
