import { api } from '@/lib/api';

export type TipoAlerta =
  | 'ESTOQUE_MINIMO_PERCENTUAL'
  | 'ESTOQUE_MINIMO_ABSOLUTO'
  | 'VENCIMENTO_PROXIMO_DIAS'
  | 'RUPTURA';

export type StatusAlerta = 'ATIVO' | 'RESOLVIDO_AUTO' | 'RESOLVIDO_MANUAL';

export type Prioridade = 'BAIXA' | 'MEDIA' | 'ALTA' | 'CRITICA';

export interface AlertaConfig {
  id: string;
  tipo: TipoAlerta;
  insumoId?: string | null;
  filialId?: string | null;
  threshold?: number | null;
  ativo: boolean;
  prioridade: Prioridade;
  observacao?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AlertaConfigCreateRequest {
  tipo: TipoAlerta;
  insumoId?: string | null;
  filialId?: string | null;
  threshold?: number | null;
  prioridade: Prioridade;
  observacao?: string;
}

export interface AlertaConfigUpdateRequest {
  threshold?: number | null;
  prioridade?: Prioridade;
  observacao?: string | null;
  ativo: boolean;
}

export interface AlertaDisparado {
  id: string;
  configId: string;
  tipo: TipoAlerta;
  insumoId: string;
  filialId: string;
  loteId?: string | null;
  status: StatusAlerta;
  saldoNoDisparo?: number | null;
  dataDisparo: string;
  dataResolucao?: string | null;
  visualizadoEm?: string | null;
  visualizadoPor?: string | null;
}

export async function listarConfigs(): Promise<AlertaConfig[]> {
  const { data } = await api.get<AlertaConfig[]>('/alertas-config');
  return data;
}

export async function criarConfig(payload: AlertaConfigCreateRequest): Promise<AlertaConfig> {
  const { data } = await api.post<AlertaConfig>('/alertas-config', payload);
  return data;
}

export async function atualizarConfig(
  id: string,
  payload: AlertaConfigUpdateRequest,
): Promise<AlertaConfig> {
  const { data } = await api.put<AlertaConfig>(`/alertas-config/${id}`, payload);
  return data;
}

export async function listarDisparados(filtros: {
  status?: StatusAlerta | null;
  filialId?: string | null;
  tipo?: TipoAlerta | null;
}): Promise<AlertaDisparado[]> {
  const params: Record<string, string> = {};
  if (filtros.status) params.status = filtros.status;
  if (filtros.filialId) params.filialId = filtros.filialId;
  if (filtros.tipo) params.tipo = filtros.tipo;
  const { data } = await api.get<AlertaDisparado[]>('/alertas-disparados', { params });
  return data;
}

export async function resolverDisparado(id: string, usuarioId: string): Promise<AlertaDisparado> {
  const { data } = await api.post<AlertaDisparado>(`/alertas-disparados/${id}/resolver`, {
    usuarioId,
  });
  return data;
}

export async function visualizarDisparado(id: string, usuarioId: string): Promise<AlertaDisparado> {
  const { data } = await api.post<AlertaDisparado>(`/alertas-disparados/${id}/visualizar`, {
    usuarioId,
  });
  return data;
}
