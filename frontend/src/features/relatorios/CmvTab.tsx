import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Banknote, Package, ShoppingBasket } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { DataTable, type ColumnDef } from '@/components/data-table/DataTable';

import { useFilialFiltroStore } from '@/features/filtroGlobal/store';
import {
  listarCmvPorCanal,
  listarCmvPorInsumo,
  listarCmvPorProduto,
  type CmvPorCanal,
  type CmvPorInsumo,
  type CmvPorProduto,
} from './api';

function fmtMoeda(v: number | null | undefined): string {
  if (v === null || v === undefined || Number.isNaN(v)) return '—';
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(v);
}

function fmtQtd(v: number | null | undefined): string {
  if (v === null || v === undefined || Number.isNaN(v)) return '—';
  return new Intl.NumberFormat('pt-BR', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 4,
  }).format(v);
}

const CANAL_LABEL: Record<string, string> = {
  IFOOD: 'iFood',
  NOVENTANOVE_FOOD: '99Food',
  KEETA: 'Keeta',
  OPEN_DELIVERY_GENERICO: 'Open Delivery',
};

/** ISO completo (00:00:00Z início; 23:59:59Z fim do dia) a partir do input date. */
function toInstantStart(dateInput: string): string {
  return new Date(`${dateInput}T00:00:00Z`).toISOString();
}
function toInstantEnd(dateInput: string): string {
  // CmvQueries usa < :ate (exclusivo), então a margem termina no dia seguinte 00:00Z.
  const d = new Date(`${dateInput}T00:00:00Z`);
  d.setUTCDate(d.getUTCDate() + 1);
  return d.toISOString();
}

function diasAtrasDefault(dias: number): { de: string; ate: string } {
  const hoje = new Date();
  const ate = hoje.toISOString().slice(0, 10);
  const inicio = new Date(hoje);
  inicio.setDate(inicio.getDate() - dias);
  const de = inicio.toISOString().slice(0, 10);
  return { de, ate };
}

export function CmvTab() {
  const filialId = useFilialFiltroStore((s) => s.filialId);
  const [periodo, setPeriodo] = useState(() => diasAtrasDefault(30));

  const de = useMemo(() => toInstantStart(periodo.de), [periodo.de]);
  const ate = useMemo(() => toInstantEnd(periodo.ate), [periodo.ate]);

  const queryParams = { de, ate, filialId };

  const insumoQuery = useQuery({
    queryKey: ['cmv-insumo', de, ate, filialId],
    queryFn: () => listarCmvPorInsumo(queryParams),
  });
  const produtoQuery = useQuery({
    queryKey: ['cmv-produto', de, ate, filialId],
    queryFn: () => listarCmvPorProduto(queryParams),
  });
  const canalQuery = useQuery({
    queryKey: ['cmv-canal', de, ate, filialId],
    queryFn: () => listarCmvPorCanal(queryParams),
  });

  const colsInsumo: ColumnDef<CmvPorInsumo>[] = [
    { key: 'codigo', header: 'Código', cell: (i) => <span className="font-mono text-xs">{i.codigo}</span> },
    { key: 'nome', header: 'Insumo', cell: (i) => i.nome },
    { key: 'qtd', header: 'Qtd vendida', cell: (i) => fmtQtd(i.quantidadeVendidaBase), className: 'text-right' },
    { key: 'custoMedio', header: 'Custo médio', cell: (i) => fmtMoeda(i.custoMedioPeriodo), className: 'text-right' },
    { key: 'cmv', header: 'CMV total', cell: (i) => <span className="font-semibold">{fmtMoeda(i.cmvTotal)}</span>, className: 'text-right' },
    { key: 'movs', header: 'Movs', cell: (i) => <>{i.quantidadeMovimentacoes}</>, className: 'text-right text-muted-foreground' },
  ];

  const colsProduto: ColumnDef<CmvPorProduto>[] = [
    { key: 'codigo', header: 'Código', cell: (i) => <span className="font-mono text-xs">{i.codigo}</span> },
    { key: 'nome', header: 'Produto', cell: (i) => i.nome },
    { key: 'qtd', header: 'Qtd vendida', cell: (i) => fmtQtd(i.quantidadeVendida), className: 'text-right' },
    { key: 'cmv', header: 'CMV total', cell: (i) => <span className="font-semibold">{fmtMoeda(i.cmvTotal)}</span>, className: 'text-right' },
    { key: 'movs', header: 'Movs', cell: (i) => <>{i.quantidadeMovimentacoes}</>, className: 'text-right text-muted-foreground' },
  ];

  const colsCanal: ColumnDef<CmvPorCanal>[] = [
    { key: 'canal', header: 'Canal', cell: (i) => <>{CANAL_LABEL[i.canal] ?? i.canal}</> },
    { key: 'pedidos', header: 'Pedidos', cell: (i) => <>{i.quantidadePedidos}</>, className: 'text-right' },
    { key: 'receita', header: 'Receita líquida', cell: (i) => fmtMoeda(i.receitaLiquidaTotal), className: 'text-right' },
    { key: 'cmv', header: 'CMV', cell: (i) => fmtMoeda(i.cmvTotal), className: 'text-right' },
    {
      key: 'margem',
      header: 'Margem bruta',
      cell: (i) => (
        <span className={i.margemBruta != null && i.margemBruta < 0 ? 'font-semibold text-destructive' : 'font-semibold text-emerald-700'}>
          {fmtMoeda(i.margemBruta)}
        </span>
      ),
      className: 'text-right',
    },
  ];

  const totalCmvInsumo = (insumoQuery.data ?? []).reduce((acc, i) => acc + (i.cmvTotal ?? 0), 0);
  const totalCmvProduto = (produtoQuery.data ?? []).reduce((acc, i) => acc + (i.cmvTotal ?? 0), 0);
  const totalCmvCanal = (canalQuery.data ?? []).reduce((acc, i) => acc + (i.cmvTotal ?? 0), 0);
  const totalReceitaCanal = (canalQuery.data ?? []).reduce((acc, i) => acc + (i.receitaLiquidaTotal ?? 0), 0);
  const totalMargemCanal = totalReceitaCanal - totalCmvCanal;

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-end gap-3 rounded-md border bg-muted/30 p-3">
        <div className="space-y-1">
          <Label className="text-xs" htmlFor="cmv-de">De</Label>
          <Input
            id="cmv-de"
            type="date"
            value={periodo.de}
            onChange={(e) => setPeriodo((p) => ({ ...p, de: e.target.value }))}
            className="w-[160px]"
          />
        </div>
        <div className="space-y-1">
          <Label className="text-xs" htmlFor="cmv-ate">Até</Label>
          <Input
            id="cmv-ate"
            type="date"
            value={periodo.ate}
            onChange={(e) => setPeriodo((p) => ({ ...p, ate: e.target.value }))}
            className="w-[160px]"
          />
        </div>
        <div className="ml-auto flex gap-2">
          <Button variant="outline" size="sm" onClick={() => setPeriodo(diasAtrasDefault(7))}>
            7 dias
          </Button>
          <Button variant="outline" size="sm" onClick={() => setPeriodo(diasAtrasDefault(30))}>
            30 dias
          </Button>
          <Button variant="outline" size="sm" onClick={() => setPeriodo(diasAtrasDefault(90))}>
            90 dias
          </Button>
        </div>
      </div>

      <Tabs defaultValue="insumo" className="space-y-3">
        <TabsList>
          <TabsTrigger value="insumo">
            <ShoppingBasket className="mr-2 h-4 w-4" /> Por insumo
          </TabsTrigger>
          <TabsTrigger value="produto">
            <Package className="mr-2 h-4 w-4" /> Por produto
          </TabsTrigger>
          <TabsTrigger value="canal">
            <Banknote className="mr-2 h-4 w-4" /> Por canal
          </TabsTrigger>
        </TabsList>

        <TabsContent value="insumo">
          <div className="mb-2 flex items-baseline justify-between">
            <p className="text-sm text-muted-foreground">
              {(insumoQuery.data ?? []).length} insumos com saída no período.
            </p>
            <p className="text-sm">
              CMV total: <span className="font-semibold">{fmtMoeda(totalCmvInsumo)}</span>
            </p>
          </div>
          <DataTable
            data={insumoQuery.data}
            columns={colsInsumo}
            isLoading={insumoQuery.isLoading}
            isError={insumoQuery.isError}
            rowKey={(i) => i.insumoId}
            emptyState="Nenhuma saída de venda no período."
          />
        </TabsContent>

        <TabsContent value="produto">
          <div className="mb-2 flex items-baseline justify-between">
            <p className="text-sm text-muted-foreground">
              {(produtoQuery.data ?? []).length} produtos vendidos no período.
            </p>
            <p className="text-sm">
              CMV total: <span className="font-semibold">{fmtMoeda(totalCmvProduto)}</span>
            </p>
          </div>
          <DataTable
            data={produtoQuery.data}
            columns={colsProduto}
            isLoading={produtoQuery.isLoading}
            isError={produtoQuery.isError}
            rowKey={(i) => i.produtoVendavelId}
            emptyState="Nenhuma venda no período."
          />
        </TabsContent>

        <TabsContent value="canal">
          <div className="mb-2 flex flex-wrap items-baseline justify-between gap-2">
            <p className="text-sm text-muted-foreground">
              {(canalQuery.data ?? []).length} canais com vendas no período.
            </p>
            <div className="text-sm">
              Receita: <span className="font-semibold">{fmtMoeda(totalReceitaCanal)}</span>
              {' · '}
              CMV: <span className="font-semibold">{fmtMoeda(totalCmvCanal)}</span>
              {' · '}
              Margem:{' '}
              <span className={totalMargemCanal < 0 ? 'font-semibold text-destructive' : 'font-semibold text-emerald-700'}>
                {fmtMoeda(totalMargemCanal)}
              </span>
            </div>
          </div>
          <DataTable
            data={canalQuery.data}
            columns={colsCanal}
            isLoading={canalQuery.isLoading}
            isError={canalQuery.isError}
            rowKey={(i) => i.canal}
            emptyState="Nenhuma venda de canal no período."
          />
        </TabsContent>
      </Tabs>
    </div>
  );
}
