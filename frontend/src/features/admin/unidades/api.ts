import { api } from '@/lib/api';

export type UnidadeTipo = 'PESO' | 'VOLUME' | 'UNIDADE';

export interface Unidade {
  id: string;
  codigo: string;
  nome: string;
  tipo: UnidadeTipo;
  ativa: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface UnidadeCreateRequest {
  codigo: string;
  nome: string;
  tipo: UnidadeTipo;
}

export interface UnidadeUpdateRequest {
  nome: string;
}

export async function listarUnidades(): Promise<Unidade[]> {
  const { data } = await api.get<Unidade[]>('/unidades-medida');
  return data;
}

export async function buscarUnidade(id: string): Promise<Unidade> {
  const { data } = await api.get<Unidade>(`/unidades-medida/${id}`);
  return data;
}

export async function criarUnidade(payload: UnidadeCreateRequest): Promise<Unidade> {
  const { data } = await api.post<Unidade>('/unidades-medida', payload);
  return data;
}

export async function atualizarUnidade(
  id: string,
  payload: UnidadeUpdateRequest,
): Promise<Unidade> {
  const { data } = await api.put<Unidade>(`/unidades-medida/${id}`, payload);
  return data;
}

export async function desativarUnidade(id: string): Promise<Unidade> {
  const { data } = await api.patch<Unidade>(`/unidades-medida/${id}/desativar`);
  return data;
}

export async function ativarUnidade(id: string): Promise<Unidade> {
  const { data } = await api.patch<Unidade>(`/unidades-medida/${id}/ativar`);
  return data;
}
