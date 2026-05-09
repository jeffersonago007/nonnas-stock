import { api } from '@/lib/api';

export interface Filial {
  id: string;
  empresaId: string;
  nome: string;
  cnpj: string;
  cnpjFormatado: string;
  endereco: string | null;
  ativa: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Empresa {
  id: string;
  razaoSocial: string;
  cnpj: string;
  cnpjFormatado: string;
  ativa: boolean;
}

export interface FilialCreateRequest {
  empresaId: string;
  nome: string;
  cnpj: string;
  endereco?: string;
}

export interface FilialUpdateRequest {
  nome: string;
  endereco?: string;
}

export async function listarFiliais(empresaId?: string): Promise<Filial[]> {
  const { data } = await api.get<Filial[]>('/filiais', {
    params: empresaId ? { empresaId } : undefined,
  });
  return data;
}

export async function buscarFilial(id: string): Promise<Filial> {
  const { data } = await api.get<Filial>(`/filiais/${id}`);
  return data;
}

export async function criarFilial(payload: FilialCreateRequest): Promise<Filial> {
  const { data } = await api.post<Filial>('/filiais', payload);
  return data;
}

export async function atualizarFilial(id: string, payload: FilialUpdateRequest): Promise<Filial> {
  const { data } = await api.put<Filial>(`/filiais/${id}`, payload);
  return data;
}

export async function desativarFilial(id: string): Promise<Filial> {
  const { data } = await api.patch<Filial>(`/filiais/${id}/desativar`);
  return data;
}

export async function ativarFilial(id: string): Promise<Filial> {
  const { data } = await api.patch<Filial>(`/filiais/${id}/ativar`);
  return data;
}

export async function listarEmpresas(): Promise<Empresa[]> {
  const { data } = await api.get<Empresa[]>('/empresas');
  return data;
}
