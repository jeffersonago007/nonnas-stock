import { api } from '@/lib/api';

export interface CargaPreviewLinha {
  numeroLinha: number;
  insumoId: string;
  unidadeId: string;
  numeroLote: string;
  quantidade: number;
  valorUnitario: number;
  dataFabricacao: string | null;
  dataValidade: string | null;
}

export interface CargaPreview {
  hashPlanilha: string;
  nomeArquivo: string;
  totalLinhas: number;
  linhas: CargaPreviewLinha[];
}

export interface CargaResultado {
  id: string;
  filialId: string;
  hashPlanilha: string;
  nomeArquivo: string;
  registrosProcessados: number;
  registrosFalhos: number;
  solicitadoPor: string;
  createdAt: string;
}

export async function previewCargaInicial(file: File): Promise<CargaPreview> {
  const fd = new FormData();
  fd.append('file', file);
  const { data } = await api.post<CargaPreview>('/cargas-iniciais/preview', fd, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return data;
}

export async function confirmarCargaInicial(
  filialId: string,
  solicitadoPor: string,
  file: File,
): Promise<CargaResultado> {
  const fd = new FormData();
  fd.append('filialId', filialId);
  fd.append('solicitadoPor', solicitadoPor);
  fd.append('file', file);
  const { data } = await api.post<CargaResultado>('/cargas-iniciais', fd, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return data;
}

/**
 * Constrói um arquivo CSV em memória a partir das linhas digitadas
 * manualmente, no schema esperado pelo backend (CsvParser):
 * insumo_id;unidade_id;numero_lote;quantidade;valor_unitario;data_fabricacao;data_validade
 */
export function montarCsvLinhaALinha(linhas: CargaPreviewLinha[]): File {
  const header =
    'insumo_id;unidade_id;numero_lote;quantidade;valor_unitario;data_fabricacao;data_validade';
  const corpo = linhas.map((l) =>
    [
      l.insumoId,
      l.unidadeId,
      l.numeroLote,
      l.quantidade.toString(),
      l.valorUnitario.toString(),
      l.dataFabricacao ?? '',
      l.dataValidade ?? '',
    ].join(';'),
  );
  const csv = [header, ...corpo].join('\n');
  return new File([csv], 'carga-manual.csv', { type: 'text/csv' });
}
