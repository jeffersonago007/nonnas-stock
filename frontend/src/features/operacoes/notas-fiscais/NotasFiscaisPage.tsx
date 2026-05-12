import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Plus, FileText, Search, X } from 'lucide-react';

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
import { DataTable, type ColumnDef } from '@/components/data-table/DataTable';
import { useFilialFiltroStore } from '@/features/filtroGlobal/store';
import { listarFornecedores } from '@/features/cadastros/fornecedores/api';

import { type NotaFiscal, type NotasFiscaisFiltro, listarNotasFiscais } from './api';
import { NotaFiscalDetailDialog } from './NotaFiscalDetailDialog';

function formatBRL(valor: number): string {
  return valor.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}
function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('pt-BR');
}

type CampoData = 'emissao' | 'lancamento';
type Periodo = 'semana' | 'mes' | 'ano';

interface InputState {
  fornecedorId: string;
  numero: string;
  chaveNfe: string;
  campoData: CampoData;
  dataDe: string;        // 'yyyy-mm-dd' do input date
  dataAte: string;
}

const FORNECEDOR_TODOS = '__todos__';

function inputVazio(): InputState {
  return {
    fornecedorId: FORNECEDOR_TODOS,
    numero: '',
    chaveNfe: '',
    campoData: 'emissao',
    dataDe: '',
    dataAte: '',
  };
}

function toISOStart(date: string): string | undefined {
  return date ? new Date(date + 'T00:00:00').toISOString() : undefined;
}
function toISOEnd(date: string): string | undefined {
  return date ? new Date(date + 'T23:59:59.999').toISOString() : undefined;
}

function rangePeriodo(p: Periodo): { de: string; ate: string } {
  const hoje = new Date();
  const ate = hoje.toISOString().slice(0, 10);
  const de = new Date(hoje);
  if (p === 'semana') de.setDate(hoje.getDate() - 7);
  else if (p === 'mes') de.setMonth(hoje.getMonth() - 1);
  else de.setFullYear(hoje.getFullYear() - 1);
  return { de: de.toISOString().slice(0, 10), ate };
}

export function NotasFiscaisPage() {
  const navigate = useNavigate();
  const filialId = useFilialFiltroStore((s) => s.filialId);
  const [detalheId, setDetalheId] = useState<string | null>(null);

  const [input, setInput] = useState<InputState>(inputVazio());
  const [aplicados, setAplicados] = useState<InputState>(inputVazio());

  const fornecedoresQuery = useQuery({
    queryKey: ['fornecedores', { ativo: true }],
    queryFn: () => listarFornecedores({ ativo: true }),
  });

  const filtroAPI: NotasFiscaisFiltro = useMemo(() => {
    const out: NotasFiscaisFiltro = {};
    if (filialId) out.filialId = filialId;
    if (aplicados.fornecedorId && aplicados.fornecedorId !== FORNECEDOR_TODOS) {
      out.fornecedorId = aplicados.fornecedorId;
    }
    if (aplicados.numero.trim()) out.numero = aplicados.numero.trim();
    if (aplicados.chaveNfe.trim()) out.chaveNfe = aplicados.chaveNfe.trim();
    if (aplicados.campoData === 'emissao') {
      out.emissaoDe = toISOStart(aplicados.dataDe);
      out.emissaoAte = toISOEnd(aplicados.dataAte);
    } else {
      out.lancamentoDe = toISOStart(aplicados.dataDe);
      out.lancamentoAte = toISOEnd(aplicados.dataAte);
    }
    return out;
  }, [filialId, aplicados]);

  const notasQuery = useQuery({
    queryKey: ['notas-fiscais', filtroAPI],
    queryFn: () => listarNotasFiscais(filtroAPI),
  });

  function aplicarFiltros() {
    setAplicados(input);
  }
  function limparFiltros() {
    setInput(inputVazio());
    setAplicados(inputVazio());
  }
  function aplicarPeriodo(p: Periodo) {
    const { de, ate } = rangePeriodo(p);
    const next: InputState = { ...input, dataDe: de, dataAte: ate, campoData: 'emissao' };
    setInput(next);
    setAplicados(next);
  }

  const columns: ColumnDef<NotaFiscal>[] = [
    {
      key: 'numero',
      header: 'Nº / Série',
      cell: (n) => <span className="font-medium">{n.numero}/{n.serie}</span>,
      className: 'w-[120px]',
    },
    { key: 'data', header: 'Emissão', cell: (n) => formatDate(n.dataEmissao), className: 'w-[120px]' },
    { key: 'lanc', header: 'Lançamento', cell: (n) => formatDate(n.dataLancamento), className: 'w-[120px]' },
    {
      key: 'chave',
      header: 'Chave NF-e',
      cell: (n) => (
        <span className="font-mono text-xs">
          {n.chaveNfe ? n.chaveNfe : <em className="text-muted-foreground">manual</em>}
        </span>
      ),
    },
    { key: 'itens', header: 'Itens', cell: (n) => n.itens.length, className: 'w-[80px] text-right' },
    {
      key: 'valor',
      header: 'Valor total',
      cell: (n) => formatBRL(n.valorTotal),
      className: 'w-[140px] text-right',
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Notas fiscais"
        description="Histórico de notas fiscais lançadas (entrada de mercadoria por NF-e)."
        actions={
          <Button onClick={() => navigate('/notas-fiscais/lancar')}>
            <Plus className="h-4 w-4" /> Lançar nota
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
        {/* Chips de período rápido — aplica direto pelo campo emissão. */}
        <div className="flex flex-wrap gap-2">
          <span className="text-xs text-muted-foreground self-center mr-1">Período rápido:</span>
          <Button type="button" variant="outline" size="sm" onClick={() => aplicarPeriodo('semana')}>
            Última semana
          </Button>
          <Button type="button" variant="outline" size="sm" onClick={() => aplicarPeriodo('mes')}>
            Último mês
          </Button>
          <Button type="button" variant="outline" size="sm" onClick={() => aplicarPeriodo('ano')}>
            Último ano
          </Button>
        </div>

        <div className="grid gap-3 md:grid-cols-3">
          <div className="space-y-1.5">
            <Label htmlFor="filtro-fornecedor">Fornecedor</Label>
            <Select
              value={input.fornecedorId}
              onValueChange={(v) => setInput({ ...input, fornecedorId: v })}
            >
              <SelectTrigger id="filtro-fornecedor">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={FORNECEDOR_TODOS}>Todos</SelectItem>
                {(fornecedoresQuery.data ?? []).map((f) => (
                  <SelectItem key={f.id} value={f.id}>
                    {f.razaoSocial}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="filtro-numero">Número</Label>
            <Input
              id="filtro-numero"
              value={input.numero}
              onChange={(e) => setInput({ ...input, numero: e.target.value })}
              placeholder="parcial ou completo"
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="filtro-chave">Chave NF-e</Label>
            <Input
              id="filtro-chave"
              value={input.chaveNfe}
              onChange={(e) => setInput({ ...input, chaveNfe: e.target.value })}
              placeholder="parcial ou completa"
              className="font-mono text-xs"
            />
          </div>
        </div>

        <div className="grid gap-3 md:grid-cols-3">
          <div className="space-y-1.5">
            <Label htmlFor="filtro-campo-data">Filtrar data por</Label>
            <Select
              value={input.campoData}
              onValueChange={(v) => setInput({ ...input, campoData: v as CampoData })}
            >
              <SelectTrigger id="filtro-campo-data">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="emissao">Data de emissão</SelectItem>
                <SelectItem value="lancamento">Data de lançamento</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="filtro-data-de">De</Label>
            <Input
              id="filtro-data-de"
              type="date"
              value={input.dataDe}
              onChange={(e) => setInput({ ...input, dataDe: e.target.value })}
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="filtro-data-ate">Até</Label>
            <Input
              id="filtro-data-ate"
              type="date"
              value={input.dataAte}
              onChange={(e) => setInput({ ...input, dataAte: e.target.value })}
            />
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
        data={notasQuery.data}
        columns={columns}
        isLoading={notasQuery.isLoading}
        isError={notasQuery.isError}
        rowKey={(n) => n.id}
        onRowClick={(n) => setDetalheId(n.id)}
        emptyState={
          <div className="space-y-3">
            <FileText className="mx-auto h-10 w-10 text-muted-foreground" />
            <p>Nenhuma nota fiscal nos filtros atuais.</p>
            <Button variant="outline" onClick={() => navigate('/notas-fiscais/lancar')}>
              <Plus className="h-4 w-4" /> Lançar nota
            </Button>
          </div>
        }
      />

      <NotaFiscalDetailDialog
        notaId={detalheId}
        onOpenChange={(open) => {
          if (!open) setDetalheId(null);
        }}
      />
    </div>
  );
}
