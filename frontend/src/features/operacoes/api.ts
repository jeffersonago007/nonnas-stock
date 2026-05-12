import { api } from '@/lib/api';

// ─────────────────── Reports re-exports (single source of truth) ─────────
//
// Tipos e listagens de relatórios moraram historicamente aqui, mas a tela
// /relatorios precisava deles centralizados. Mantemos os re-exports para
// não quebrar imports antigos de `@/features/operacoes/api` (EstoquePage,
// DashboardPage, MovimentacoesPage etc.).
export {
  listarPosicao,
  listarRuptura,
  listarVencimentos,
  listarMovimentacoesPorPeriodo,
} from '@/features/relatorios/api';
export type {
  PosicaoEstoque,
  RupturaItem,
  SituacaoRuptura,
  VencimentoItem,
  MovimentacaoResumo,
  TipoMovimentacao,
} from '@/features/relatorios/api';

import type { TipoMovimentacao } from '@/features/relatorios/api';

// ─────────────────────────── Movimentações ───────────────────────────────

export const TIPOS_ENTRADA_MANUAL: TipoMovimentacao[] = [
  'ENTRADA_NF',
  'ENTRADA_AJUSTE',
  'ENTRADA_DEVOLUCAO_CLIENTE',
];

export const TIPOS_SAIDA_MANUAL: TipoMovimentacao[] = [
  'SAIDA_AJUSTE',
  'SAIDA_PERDA',
  'SAIDA_QUEBRA',
  'SAIDA_VENCIMENTO',
];

export interface EntradaManualRequest {
  filialId: string;
  usuarioId: string;
  insumoId: string;
  fornecedorId?: string | null;
  numeroLote?: string | null;
  dataFabricacao?: string | null;
  dataValidade?: string | null;
  valorUnitario: number;
  unidadeLancamentoId: string;
  quantidadeLancada: number;
  quantidadeBase: number;
  tipo: TipoMovimentacao;
  observacao?: string;
}

export interface SaidaManualRequest {
  filialId: string;
  usuarioId: string;
  insumoId: string;
  unidadeLancamentoId: string;
  quantidadeBase: number;
  tipo: TipoMovimentacao;
  observacao?: string;
}

export async function lancarEntradaManual(payload: EntradaManualRequest) {
  const { data } = await api.post('/movimentacoes/entrada-manual', payload);
  return data;
}

export async function lancarSaidaManual(payload: SaidaManualRequest) {
  const { data } = await api.post('/movimentacoes/saida-manual', payload);
  return data;
}

// ─────────────────────────── Transferências ──────────────────────────────

export type StatusTransferencia =
  | 'SOLICITADA'
  | 'APROVADA'
  | 'EM_TRANSITO'
  | 'RECEBIDA'
  | 'CANCELADA';

export interface TransferenciaItem {
  id: string;
  insumoId: string;
  unidadeId: string;
  quantidadeSolicitada: number;
  quantidadeRecebida?: number | null;
}

export interface Transferencia {
  id: string;
  filialOrigemId: string;
  filialDestinoId: string;
  status: StatusTransferencia;
  solicitadoPor: string;
  aprovadoPor?: string | null;
  enviadoPor?: string | null;
  recebidoPor?: string | null;
  canceladoPor?: string | null;
  motivoCancelamento?: string | null;
  observacao?: string | null;
  dataSolicitacao: string;
  dataAprovacao?: string | null;
  dataEnvio?: string | null;
  dataRecebimento?: string | null;
  dataCancelamento?: string | null;
  itens: TransferenciaItem[];
}

export interface SolicitarTransferenciaRequest {
  filialOrigemId: string;
  filialDestinoId: string;
  solicitadoPor: string;
  observacao?: string;
  itens: Array<{ insumoId: string; unidadeId: string; quantidade: number }>;
}

export async function solicitarTransferencia(payload: SolicitarTransferenciaRequest): Promise<Transferencia> {
  const { data } = await api.post<Transferencia>('/transferencias', payload);
  return data;
}

export async function listarTransferencias(params: {
  filialId?: string | null;
  status?: StatusTransferencia | null;
}): Promise<Transferencia[]> {
  const q: Record<string, string> = {};
  if (params.filialId) q.filialId = params.filialId;
  if (params.status) q.status = params.status;
  const { data } = await api.get<Transferencia[]>('/transferencias', { params: q });
  return data;
}

export async function buscarTransferencia(id: string): Promise<Transferencia> {
  const { data } = await api.get<Transferencia>(`/transferencias/${id}`);
  return data;
}

export async function aprovarTransferencia(id: string, usuarioId: string): Promise<Transferencia> {
  const { data } = await api.post<Transferencia>(`/transferencias/${id}/aprovar`, { usuarioId });
  return data;
}

export async function enviarTransferencia(id: string, usuarioId: string): Promise<Transferencia> {
  const { data } = await api.post<Transferencia>(`/transferencias/${id}/enviar`, { usuarioId });
  return data;
}

export async function receberTransferencia(
  id: string,
  recebidoPor: string,
  itens: Array<{ itemId: string; quantidadeRecebida: number }>,
): Promise<Transferencia> {
  const { data } = await api.post<Transferencia>(`/transferencias/${id}/receber`, {
    recebidoPor,
    itens,
  });
  return data;
}

export async function cancelarTransferencia(id: string, motivo: string): Promise<Transferencia> {
  const { data } = await api.post<Transferencia>(`/transferencias/${id}/cancelar`, { motivo });
  return data;
}

export interface EmTransitoItem {
  insumoId: string;
  quantidadeEmTransito: number;
}

export async function listarEmTransito(filialDestinoId?: string | null): Promise<EmTransitoItem[]> {
  const params: Record<string, string> = {};
  if (filialDestinoId) params.filialDestinoId = filialDestinoId;
  const { data } = await api.get<EmTransitoItem[]>('/transferencias/em-transito', { params });
  return data;
}
