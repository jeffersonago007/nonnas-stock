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
