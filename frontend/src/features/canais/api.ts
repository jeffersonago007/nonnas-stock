import { api } from '@/lib/api';

export type CanalTipo = 'IFOOD' | 'NOVENTANOVE_FOOD' | 'KEETA' | 'OPEN_DELIVERY_GENERICO';

export const CANAL_LABEL: Record<CanalTipo, string> = {
  IFOOD: 'iFood',
  NOVENTANOVE_FOOD: '99Food',
  KEETA: 'Keeta',
  OPEN_DELIVERY_GENERICO: 'Open Delivery (genérico)',
};

export const CANAIS: CanalTipo[] = ['IFOOD', 'NOVENTANOVE_FOOD', 'KEETA', 'OPEN_DELIVERY_GENERICO'];

export type StatusPedidoCanal =
  | 'RECEBIDO'
  | 'EM_PROCESSAMENTO'
  | 'CONFIRMADO_ESTOQUE'
  | 'CONCLUIDO'
  | 'CANCELADO'
  | 'FALHA';

export const STATUS_LABEL: Record<StatusPedidoCanal, string> = {
  RECEBIDO: 'Recebido',
  EM_PROCESSAMENTO: 'Em processamento',
  CONFIRMADO_ESTOQUE: 'Estoque baixado',
  CONCLUIDO: 'Concluído',
  CANCELADO: 'Cancelado',
  FALHA: 'Falha',
};

// Credenciais ---------------------------------------------------------------

export interface CredencialCanal {
  id: string;
  canalTipo: CanalTipo;
  filialId: string;
  merchantExternoId: string;
  clientId: string;
  baseUrl: string | null;
  ativa: boolean;
  observacao: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CredencialCreateRequest {
  canalTipo: CanalTipo;
  filialId: string;
  merchantExternoId: string;
  clientId: string;
  clientSecret: string;
  baseUrl?: string | null;
  observacao?: string | null;
}

export interface CredencialUpdateRequest {
  baseUrl?: string | null;
  observacao?: string | null;
  /** null/vazio = não rotaciona; preenchido = rotaciona o segredo. */
  clientSecret?: string | null;
}

export async function listarCredenciais(canal?: CanalTipo): Promise<CredencialCanal[]> {
  const { data } = await api.get<CredencialCanal[]>('/canais/credenciais', {
    params: canal ? { canal } : undefined,
  });
  return data;
}

export async function criarCredencial(payload: CredencialCreateRequest): Promise<CredencialCanal> {
  const { data } = await api.post<CredencialCanal>('/canais/credenciais', payload);
  return data;
}

export async function atualizarCredencial(
  id: string,
  payload: CredencialUpdateRequest,
): Promise<CredencialCanal> {
  const { data } = await api.put<CredencialCanal>(`/canais/credenciais/${id}`, payload);
  return data;
}

export async function ativarCredencial(id: string): Promise<CredencialCanal> {
  const { data } = await api.patch<CredencialCanal>(`/canais/credenciais/${id}/ativar`);
  return data;
}

export async function desativarCredencial(id: string): Promise<CredencialCanal> {
  const { data } = await api.patch<CredencialCanal>(`/canais/credenciais/${id}/desativar`);
  return data;
}

// De-para -------------------------------------------------------------------

export interface CanalProdutoDePara {
  id: string;
  canalTipo: CanalTipo;
  externalCode: string;
  filialId: string | null;
  produtoVendavelId: string;
  observacao: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface DeparaCreateRequest {
  canalTipo: CanalTipo;
  externalCode: string;
  filialId?: string | null;
  produtoVendavelId: string;
  observacao?: string | null;
}

export interface DeparaUpdateRequest {
  produtoVendavelId: string;
  observacao?: string | null;
}

export async function listarDepara(canal: CanalTipo): Promise<CanalProdutoDePara[]> {
  const { data } = await api.get<CanalProdutoDePara[]>('/canais/depara-produtos', {
    params: { canal },
  });
  return data;
}

export async function criarDepara(payload: DeparaCreateRequest): Promise<CanalProdutoDePara> {
  const { data } = await api.post<CanalProdutoDePara>('/canais/depara-produtos', payload);
  return data;
}

export async function atualizarDepara(
  id: string,
  payload: DeparaUpdateRequest,
): Promise<CanalProdutoDePara> {
  const { data } = await api.put<CanalProdutoDePara>(`/canais/depara-produtos/${id}`, payload);
  return data;
}

export async function deletarDepara(id: string): Promise<void> {
  await api.delete(`/canais/depara-produtos/${id}`);
}

// Pedidos -------------------------------------------------------------------

export interface ItemPedidoCanal {
  sequencia: number;
  externalCode: string | null;
  nome: string;
  quantidade: number;
  unidade: string;
  precoUnitario: number;
  precoTotal: number;
  observacao: string | null;
  produtoVendavelId: string | null;
}

export interface PedidoCanal {
  id: string;
  canalTipo: CanalTipo;
  pedidoExternoId: string;
  displayId: string | null;
  filialId: string;
  credencialId: string;
  status: StatusPedidoCanal;
  valorTotal: number;
  taxaEntrega: number;
  taxaServico: number;
  valorLiquido: number;
  moeda: string;
  clienteNome: string | null;
  clienteTelefone: string | null;
  itens: ItemPedidoCanal[];
  movimentacaoId: string | null;
  erroProcessamento: string | null;
  recebidoEm: string;
  processadoEm: string | null;
  concluidoEm: string | null;
  canceladoEm: string | null;
}

export interface ProcessarPendentesResponse {
  processadosSucesso: number;
  processadosFalha: number;
  totalPendentes: number;
}

export interface PollNowResponse {
  canal: CanalTipo;
  eventosNovos: number;
}

export async function listarPedidos(
  filialId: string,
  status?: StatusPedidoCanal,
): Promise<PedidoCanal[]> {
  const { data } = await api.get<PedidoCanal[]>('/canais/pedidos', {
    params: { filialId, status },
  });
  return data;
}

export async function buscarPedido(id: string): Promise<PedidoCanal> {
  const { data } = await api.get<PedidoCanal>(`/canais/pedidos/${id}`);
  return data;
}

export async function reprocessarPedido(id: string): Promise<PedidoCanal> {
  const { data } = await api.post<PedidoCanal>(`/canais/pedidos/${id}/reprocessar`);
  return data;
}

export async function processarPendentes(): Promise<ProcessarPendentesResponse> {
  const { data } = await api.post<ProcessarPendentesResponse>('/canais/processar-pendentes');
  return data;
}

export async function pollNow(canal: CanalTipo): Promise<PollNowResponse> {
  const { data } = await api.post<PollNowResponse>(`/canais/${canal}/poll-now`);
  return data;
}

// Dev simulator (backend só responde com perfil dev ativo) -----------------

export interface SimularItem {
  externalCode: string;
  nome: string;
  quantidade: number;
  unidade: string;
  precoUnitario: number;
  observacao?: string | null;
}

export interface SimularPedidoRequest {
  canal: CanalTipo;
  filialId: string;
  displayId?: string | null;
  clienteNome?: string | null;
  clienteTelefone?: string | null;
  taxaEntrega?: number | null;
  taxaServico?: number | null;
  itens: SimularItem[];
}

export async function simularPedidoDev(req: SimularPedidoRequest): Promise<PedidoCanal> {
  const { data } = await api.post<PedidoCanal>('/canais/dev/simular-pedido', req);
  return data;
}
