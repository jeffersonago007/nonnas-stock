import { api } from '@/lib/api';

export interface Empresa {
  id: string;
  razaoSocial: string;
  cnpj: string;
  cnpjFormatado: string;
  ativa: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface EmpresaCreateRequest {
  razaoSocial: string;
  cnpj: string;
}

export interface EmpresaUpdateRequest {
  razaoSocial: string;
}

export async function listarEmpresas(): Promise<Empresa[]> {
  const { data } = await api.get<Empresa[]>('/empresas');
  return data;
}

export async function buscarEmpresa(id: string): Promise<Empresa> {
  const { data } = await api.get<Empresa>(`/empresas/${id}`);
  return data;
}

export async function criarEmpresa(payload: EmpresaCreateRequest): Promise<Empresa> {
  const { data } = await api.post<Empresa>('/empresas', payload);
  return data;
}

export async function atualizarEmpresa(
  id: string,
  payload: EmpresaUpdateRequest,
): Promise<Empresa> {
  const { data } = await api.put<Empresa>(`/empresas/${id}`, payload);
  return data;
}

export async function desativarEmpresa(id: string): Promise<Empresa> {
  const { data } = await api.patch<Empresa>(`/empresas/${id}/desativar`);
  return data;
}

export async function ativarEmpresa(id: string): Promise<Empresa> {
  const { data } = await api.patch<Empresa>(`/empresas/${id}/ativar`);
  return data;
}
