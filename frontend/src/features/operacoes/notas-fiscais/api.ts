import { api } from '@/lib/api';

export interface EmitenteResumo {
  cnpj: string;
  razaoSocial: string;
  nomeFantasia: string | null;
  inscricaoEstadual: string | null;
}

export interface ItemPreview {
  numero: number;
  codigoFornecedor: string;
  descricao: string;
  ncm: string | null;
  unidadeComercial: string;
  quantidade: number;
  valorUnitario: number;
  valorTotal: number;
}

export interface PreviewResponse {
  chaveAcesso: string;
  numero: string;
  serie: string;
  dataEmissao: string;
  valorTotal: number;
  emitente: EmitenteResumo;
  itens: ItemPreview[];
}

export interface FornecedorRef {
  id?: string | null;
  cnpj?: string | null;
  razaoSocial?: string | null;
}

export interface InsumoRef {
  id?: string | null;
  codigo?: string | null;
  nome?: string | null;
  unidadeBaseId?: string | null;
}

export interface ItemRequest {
  insumo: InsumoRef;
  codigoFornecedor?: string | null;
  descricaoOrigem: string;
  quantidade: number;
  unidadeMedidaId: string;
  valorUnitario: number;
  valorTotal?: number | null;
  lote?: string | null;
  dataValidade?: string | null;
}

export interface LancarRequest {
  filialId: string;
  usuarioId: string;
  fornecedor: FornecedorRef;
  numero: string;
  serie: string;
  chaveNfe?: string | null;
  dataEmissao: string;
  valorTotal: number;
  observacao?: string | null;
  itens: ItemRequest[];
}

export interface ItemResponse {
  id: string;
  insumoId: string;
  codigoFornecedor: string | null;
  descricaoOrigem: string;
  quantidade: number;
  unidadeMedidaId: string;
  valorUnitario: number;
  valorTotal: number;
  lote: string | null;
  dataValidade: string | null;
}

export interface NotaFiscal {
  id: string;
  fornecedorId: string;
  filialId: string;
  numero: string;
  serie: string;
  chaveNfe: string | null;
  dataEmissao: string;
  dataLancamento: string;
  valorTotal: number;
  observacao: string | null;
  createdByUsuarioId: string;
  movimentacaoEntradaId: string;
  itens: ItemResponse[];
}

export async function previewXml(file: File): Promise<PreviewResponse> {
  const form = new FormData();
  form.append('file', file);
  const { data } = await api.post<PreviewResponse>('/notas-fiscais/preview-xml', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return data;
}

export async function lancarNotaFiscal(payload: LancarRequest): Promise<NotaFiscal> {
  const { data } = await api.post<NotaFiscal>('/notas-fiscais/lancar', payload);
  return data;
}

export async function listarNotasFiscais(filialId?: string): Promise<NotaFiscal[]> {
  const params: Record<string, string> = {};
  if (filialId) params.filialId = filialId;
  const { data } = await api.get<NotaFiscal[]>('/notas-fiscais', { params });
  return data;
}

export async function buscarNotaFiscal(id: string): Promise<NotaFiscal> {
  const { data } = await api.get<NotaFiscal>(`/notas-fiscais/${id}`);
  return data;
}
