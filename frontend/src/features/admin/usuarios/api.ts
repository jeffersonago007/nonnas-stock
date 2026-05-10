import { api } from '@/lib/api';

export type Perfil = 'ADMIN' | 'GERENTE' | 'OPERADOR' | 'CONSULTA';

export interface Usuario {
  id: string;
  filialId: string | null;
  nome: string;
  email: string;
  perfil: Perfil;
  ativo: boolean;
  travada: boolean;
  bloqueadoAte: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface UsuarioCreateRequest {
  filialId?: string | null;
  nome: string;
  email: string;
  senha: string;
  perfil: Perfil;
}

export interface UsuarioUpdateRequest {
  nome: string;
}

export async function listarUsuarios(): Promise<Usuario[]> {
  const { data } = await api.get<Usuario[]>('/usuarios');
  return data;
}

export async function buscarUsuario(id: string): Promise<Usuario> {
  const { data } = await api.get<Usuario>(`/usuarios/${id}`);
  return data;
}

export async function criarUsuario(payload: UsuarioCreateRequest): Promise<Usuario> {
  const { data } = await api.post<Usuario>('/usuarios', payload);
  return data;
}

export async function atualizarUsuario(
  id: string,
  payload: UsuarioUpdateRequest,
): Promise<Usuario> {
  const { data } = await api.put<Usuario>(`/usuarios/${id}`, payload);
  return data;
}

export async function desativarUsuario(id: string): Promise<Usuario> {
  const { data } = await api.patch<Usuario>(`/usuarios/${id}/desativar`);
  return data;
}

export async function ativarUsuario(id: string): Promise<Usuario> {
  const { data } = await api.patch<Usuario>(`/usuarios/${id}/ativar`);
  return data;
}
