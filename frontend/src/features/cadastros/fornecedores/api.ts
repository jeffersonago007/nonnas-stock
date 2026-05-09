import { api } from '@/lib/api';

export interface Fornecedor {
  id: string;
  razaoSocial: string;
  cnpj: string;
  cnpjFormatado: string;
  ativo: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface FornecedorCreateRequest {
  razaoSocial: string;
  cnpj: string;
}

export interface FornecedorUpdateRequest {
  razaoSocial: string;
}

export interface FornecedoresFiltro {
  ativo?: boolean;
  q?: string;
}

export async function listarFornecedores(filtros: FornecedoresFiltro): Promise<Fornecedor[]> {
  const params: Record<string, string | boolean> = {};
  if (filtros.ativo !== undefined) params.ativo = filtros.ativo;
  if (filtros.q) params.q = filtros.q;
  const { data } = await api.get<Fornecedor[]>('/fornecedores', { params });
  return data;
}

export async function criarFornecedor(payload: FornecedorCreateRequest): Promise<Fornecedor> {
  const { data } = await api.post<Fornecedor>('/fornecedores', payload);
  return data;
}

export async function atualizarFornecedor(
  id: string,
  payload: FornecedorUpdateRequest,
): Promise<Fornecedor> {
  const { data } = await api.put<Fornecedor>(`/fornecedores/${id}`, payload);
  return data;
}

export async function desativarFornecedor(id: string): Promise<Fornecedor> {
  const { data } = await api.patch<Fornecedor>(`/fornecedores/${id}/desativar`);
  return data;
}

export async function ativarFornecedor(id: string): Promise<Fornecedor> {
  const { data } = await api.patch<Fornecedor>(`/fornecedores/${id}/ativar`);
  return data;
}
