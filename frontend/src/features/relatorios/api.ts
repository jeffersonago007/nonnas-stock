import { api } from '@/lib/api';

// ─────────────────────── Posição (mv_curva_abc-like) ──────────────────────

export interface PosicaoEstoque {
  filialId: string;
  insumoId: string;
  codigo: string;
  nome: string;
  saldoTotal: number;
  valorEstoque: number;
  quantidadeLotes: number;
}

export async function listarPosicao(
  filialId?: string | null,
  categoriaId?: string,
): Promise<PosicaoEstoque[]> {
  const params: Record<string, string> = {};
  if (filialId) params.filialId = filialId;
  if (categoriaId) params.categoriaId = categoriaId;
  const { data } = await api.get<PosicaoEstoque[]>('/relatorios/posicao', { params });
  return data;
}

// ─────────────────────────────── Curva ABC ────────────────────────────────

export type ClasseABC = 'A' | 'B' | 'C';

export interface CurvaABCItem {
  filialId: string;
  insumoId: string;
  codigo: string;
  nome: string;
  quantidadeTotal: number;
  valorTotal: number;
  percentualAcumulado: number;
  classe: ClasseABC;
}

export async function listarCurvaABC(filialId?: string | null): Promise<CurvaABCItem[]> {
  const params: Record<string, string> = {};
  if (filialId) params.filialId = filialId;
  const { data } = await api.get<CurvaABCItem[]>('/relatorios/curva-abc', { params });
  return data;
}

// ────────────────────────────── Ruptura ──────────────────────────────────

/**
 * Enum vindo do backend (SituacaoRuptura). A view só devolve linhas
 * com situação diferente de NORMAL, então as 3 abaixo cobrem 100% dos casos.
 */
export type SituacaoRuptura = 'RUPTURA_TOTAL' | 'ABAIXO_PONTO_PEDIDO' | 'ABAIXO_MINIMO';

export interface RupturaItem {
  filialId: string;
  insumoId: string;
  codigo: string;
  nome: string;
  saldoTotal: number;
  estoqueMinimo: number;
  pontoPedido: number;
  situacao: SituacaoRuptura;
}

export async function listarRuptura(filialId?: string | null): Promise<RupturaItem[]> {
  const params: Record<string, string> = {};
  if (filialId) params.filialId = filialId;
  const { data } = await api.get<RupturaItem[]>('/relatorios/ruptura', { params });
  return data;
}

// ───────────────────────────── Vencimento ────────────────────────────────

export interface VencimentoItem {
  filialId: string;
  insumoId: string;
  loteId: string;
  codigo: string;
  nome: string;
  numeroLote: string;
  dataValidade: string;
  diasParaVencer: number;
  saldo: number;
  valorUnitario: number;
}

export async function listarVencimentos(
  filialId?: string | null,
  diasJanela = 30,
): Promise<VencimentoItem[]> {
  const params: Record<string, string | number> = { diasJanela };
  if (filialId) params.filialId = filialId;
  const { data } = await api.get<VencimentoItem[]>('/relatorios/vencimento', { params });
  return data;
}

// ───────────────────────────── Movimentações ─────────────────────────────

export type TipoMovimentacao =
  | 'ENTRADA_NF'
  | 'ENTRADA_AJUSTE'
  | 'ENTRADA_TRANSFERENCIA'
  | 'ENTRADA_DEVOLUCAO_CLIENTE'
  | 'ENTRADA_CARGA_INICIAL'
  | 'SAIDA_VENDA'
  | 'SAIDA_AJUSTE'
  | 'SAIDA_TRANSFERENCIA'
  | 'SAIDA_PERDA'
  | 'SAIDA_QUEBRA'
  | 'SAIDA_VENCIMENTO';

export interface MovimentacaoResumo {
  filialId: string;
  insumoId: string;
  codigo: string;
  nome: string;
  tipoMovimentacao: string;
  quantidadeMovimentacoes: number;
  quantidadeTotal: number;
  valorTotal: number;
}

export async function listarMovimentacoesPorPeriodo(params: {
  filialId?: string | null;
  inicio: string;
  fim: string;
  tipo?: string;
}): Promise<MovimentacaoResumo[]> {
  const q: Record<string, string> = { inicio: params.inicio, fim: params.fim };
  if (params.filialId) q.filialId = params.filialId;
  if (params.tipo) q.tipo = params.tipo;
  const { data } = await api.get<MovimentacaoResumo[]>('/relatorios/movimentacoes', { params: q });
  return data;
}

// ───────────────────────────── Divergência ───────────────────────────────

export interface DivergenciaItem {
  filialId: string;
  insumoId: string;
  codigo: string;
  nome: string;
  quantidadeAjustes: number;
  quantidadeDiffPositiva: number;
  quantidadeDiffNegativa: number;
  quantidadeDiffLiquida: number;
}

export async function listarDivergencia(params: {
  filialId?: string | null;
  inicio: string;
  fim: string;
}): Promise<DivergenciaItem[]> {
  const q: Record<string, string> = { inicio: params.inicio, fim: params.fim };
  if (params.filialId) q.filialId = params.filialId;
  const { data } = await api.get<DivergenciaItem[]>('/relatorios/divergencia', { params: q });
  return data;
}

// ───────────────────────────── Refresh views ─────────────────────────────

export async function refreshRelatorios(): Promise<void> {
  await api.post('/relatorios/refresh');
}
