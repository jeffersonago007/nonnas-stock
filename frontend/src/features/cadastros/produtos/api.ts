import { api } from '@/lib/api';

export type TipoProdutoVendavel = 'FABRICADO' | 'REVENDA';

export interface Produto {
  id: string;
  codigo: string;
  nome: string;
  categoria: string;
  tipo: TipoProdutoVendavel;
  insumoRevendaId: string | null;
  ativo: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ProdutoCreateRequest {
  codigo: string;
  nome: string;
  categoria: string;
  tipo: TipoProdutoVendavel;
  insumoRevendaId?: string | null;
}

export interface ProdutoUpdateRequest {
  nome: string;
  categoria: string;
}

export interface ProdutosFiltro {
  categoria?: string;
  ativo?: boolean;
  tipo?: TipoProdutoVendavel;
  q?: string;
}

export async function listarProdutos(filtros: ProdutosFiltro): Promise<Produto[]> {
  const params: Record<string, string | boolean> = {};
  if (filtros.categoria) params.categoria = filtros.categoria;
  if (filtros.ativo !== undefined) params.ativo = filtros.ativo;
  if (filtros.tipo) params.tipo = filtros.tipo;
  if (filtros.q) params.q = filtros.q;
  const { data } = await api.get<Produto[]>('/produtos-vendaveis', { params });
  return data;
}

export async function criarProduto(payload: ProdutoCreateRequest): Promise<Produto> {
  const { data } = await api.post<Produto>('/produtos-vendaveis', payload);
  return data;
}

export async function atualizarProduto(
  id: string,
  payload: ProdutoUpdateRequest,
): Promise<Produto> {
  const { data } = await api.put<Produto>(`/produtos-vendaveis/${id}`, payload);
  return data;
}

export async function desativarProduto(id: string): Promise<Produto> {
  const { data } = await api.patch<Produto>(`/produtos-vendaveis/${id}/desativar`);
  return data;
}

export async function ativarProduto(id: string): Promise<Produto> {
  const { data } = await api.patch<Produto>(`/produtos-vendaveis/${id}/ativar`);
  return data;
}

export async function listarCategoriasProduto(): Promise<string[]> {
  const { data } = await api.get<string[]>('/produtos-vendaveis/categorias');
  return data;
}
