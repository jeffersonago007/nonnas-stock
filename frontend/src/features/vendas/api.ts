import { api } from '@/lib/api';

export interface VendaItemResposta {
  id: string;
  insumoId: string;
  loteId: string;
  quantidadeBase: number;
  valorUnitario: number;
}

export interface VendaResposta {
  movimentacaoId: string;
  filialId: string;
  tipo: string;
  dataMovimentacao: string;
  gerouNegativo: boolean;
  itens: VendaItemResposta[];
}

export interface RegistrarVendaPayload {
  produtoVendavelId: string;
  filialId: string;
  usuarioId: string;
  quantidadeVendida: number;
  observacao?: string;
}

export async function registrarVenda(payload: RegistrarVendaPayload): Promise<VendaResposta> {
  const { data } = await api.post<VendaResposta>('/vendas-simuladas', payload);
  return data;
}

export interface LoteConsumidoPreview {
  loteId: string;
  numero: string | null;
  validade: string | null;
  quantidade: number;
}

export interface ItemBaixaPreview {
  insumoId: string;
  insumoNome: string;
  quantidadeBase: number;
  unidadeBase: string;
  controlaValidade: boolean;
  lotes: LoteConsumidoPreview[];
  saldoRestanteAposBaixa: number;
}

export interface PreviewVendaResposta {
  itens: ItemBaixaPreview[];
  gerouNegativo: boolean;
}

export interface PreviewVendaPayload {
  produtoVendavelId: string;
  filialId: string;
  quantidadeVendida: number;
}

export async function obterPreviewVenda(payload: PreviewVendaPayload): Promise<PreviewVendaResposta> {
  const { data } = await api.post<PreviewVendaResposta>('/vendas-simuladas/preview', payload);
  return data;
}

export type OrigemItemCardapio = 'PRODUTO_FABRICADO' | 'PRODUTO_REVENDA' | 'INSUMO_ORFAO';

export interface ItemCardapio {
  origem: OrigemItemCardapio;
  id: string;                       // produtoVendavelId OU insumoId conforme origem
  codigo: string;
  nome: string;
  categoria: string | null;
  unidadeBaseCodigo: string | null;
  saldoNaFilial: number | null;
  vendasUltimos30Dias: number;      // 0 = sem venda recente
}

export async function listarCardapio(filialId: string): Promise<ItemCardapio[]> {
  const { data } = await api.get<{ itens: ItemCardapio[] }>('/cardapio', { params: { filialId } });
  return data.itens;
}

export interface VenderInsumoPayload {
  insumoId: string;
  filialId: string;
  usuarioId: string;
  quantidadeVendida: number;
  observacao?: string;
}

export interface VenderInsumoResposta {
  produtoVendavelCriadoId: string;
  movimentacaoId: string;
  gerouNegativo: boolean;
}

export async function venderInsumoOrfao(payload: VenderInsumoPayload): Promise<VenderInsumoResposta> {
  const { data } = await api.post<VenderInsumoResposta>('/cardapio/vender-insumo', payload);
  return data;
}
