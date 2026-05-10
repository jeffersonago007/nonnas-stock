import { api } from '@/lib/api';

export interface Insumo {
  id: string;
  codigo: string;
  nome: string;
  categoriaId: string;
  unidadeBaseId: string;
  controlaLote: boolean;
  controlaValidade: boolean;
  diasAlertaVencimento: number | null;
  ativo: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CategoriaInsumo {
  id: string;
  nome: string;
  ativa: boolean;
}

export interface UnidadeMedida {
  id: string;
  codigo: string;
  nome: string;
  tipo: string;
  ativa: boolean;
}

export interface InsumoCreateRequest {
  codigo: string;
  nome: string;
  categoriaId: string;
  unidadeBaseId: string;
  controlaLote?: boolean;
  controlaValidade?: boolean;
}

export interface InsumoUpdateRequest {
  nome: string;
  categoriaId?: string;
  controlaLote?: boolean;
  controlaValidade?: boolean;
  diasAlertaVencimento?: number | null;
}

export interface InsumosFiltro {
  categoriaId?: string;
  ativo?: boolean;
  q?: string;
}

export async function listarInsumos(filtros: InsumosFiltro): Promise<Insumo[]> {
  const params: Record<string, string | boolean> = {};
  if (filtros.categoriaId) params.categoriaId = filtros.categoriaId;
  if (filtros.ativo !== undefined) params.ativo = filtros.ativo;
  if (filtros.q) params.q = filtros.q;
  const { data } = await api.get<Insumo[]>('/insumos', { params });
  return data;
}

export async function criarInsumo(payload: InsumoCreateRequest): Promise<Insumo> {
  const { data } = await api.post<Insumo>('/insumos', payload);
  return data;
}

export async function atualizarInsumo(id: string, payload: InsumoUpdateRequest): Promise<Insumo> {
  const { data } = await api.put<Insumo>(`/insumos/${id}`, payload);
  return data;
}

export async function desativarInsumo(id: string): Promise<Insumo> {
  const { data } = await api.patch<Insumo>(`/insumos/${id}/desativar`);
  return data;
}

export async function ativarInsumo(id: string): Promise<Insumo> {
  const { data } = await api.patch<Insumo>(`/insumos/${id}/ativar`);
  return data;
}

export async function listarCategorias(): Promise<CategoriaInsumo[]> {
  const { data } = await api.get<CategoriaInsumo[]>('/categorias-insumo');
  return data;
}

export async function listarUnidades(): Promise<UnidadeMedida[]> {
  const { data } = await api.get<UnidadeMedida[]>('/unidades-medida');
  return data;
}
