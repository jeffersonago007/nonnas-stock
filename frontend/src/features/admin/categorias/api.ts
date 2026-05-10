import { api } from '@/lib/api';

export interface Categoria {
  id: string;
  categoriaPaiId: string | null;
  nome: string;
  ativa: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CategoriaCreateRequest {
  nome: string;
  categoriaPaiId?: string | null;
}

export interface CategoriaUpdateRequest {
  nome: string;
}

export async function listarCategorias(): Promise<Categoria[]> {
  const { data } = await api.get<Categoria[]>('/categorias-insumo');
  return data;
}

export async function buscarCategoria(id: string): Promise<Categoria> {
  const { data } = await api.get<Categoria>(`/categorias-insumo/${id}`);
  return data;
}

export async function criarCategoria(payload: CategoriaCreateRequest): Promise<Categoria> {
  const { data } = await api.post<Categoria>('/categorias-insumo', payload);
  return data;
}

export async function atualizarCategoria(
  id: string,
  payload: CategoriaUpdateRequest,
): Promise<Categoria> {
  const { data } = await api.put<Categoria>(`/categorias-insumo/${id}`, payload);
  return data;
}

export async function desativarCategoria(id: string): Promise<Categoria> {
  const { data } = await api.patch<Categoria>(`/categorias-insumo/${id}/desativar`);
  return data;
}

export async function ativarCategoria(id: string): Promise<Categoria> {
  const { data } = await api.patch<Categoria>(`/categorias-insumo/${id}/ativar`);
  return data;
}
