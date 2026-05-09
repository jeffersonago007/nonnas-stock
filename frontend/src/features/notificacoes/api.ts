import { api } from '@/lib/api';

export type Prioridade = 'INFO' | 'AVISO' | 'CRITICA';

export interface Notificacao {
  id: string;
  tipo: string;
  prioridade: Prioridade;
  titulo: string;
  mensagem: string;
  linkAcao: string | null;
  metadata: string | null;
  criadaEm: string;
  lidaEm: string | null;
  arquivadaEm: string | null;
}

export interface ListarFiltros {
  tipo?: string;
  incluirArquivadas?: boolean;
  somenteNaoLidas?: boolean;
  page?: number;
  size?: number;
}

export async function listarNotificacoes(f: ListarFiltros = {}): Promise<Notificacao[]> {
  const params: Record<string, string | boolean | number> = {};
  if (f.tipo) params.tipo = f.tipo;
  if (f.incluirArquivadas !== undefined) params.incluirArquivadas = f.incluirArquivadas;
  if (f.somenteNaoLidas !== undefined) params.somenteNaoLidas = f.somenteNaoLidas;
  params.page = f.page ?? 0;
  params.size = f.size ?? 20;
  const { data } = await api.get<Notificacao[]>('/notificacoes', { params });
  return data;
}

export async function contarNaoLidas(): Promise<number> {
  const { data } = await api.get<{ naoLidas: number }>('/notificacoes/contagem-nao-lidas');
  return data.naoLidas;
}

export async function marcarLida(id: string): Promise<void> {
  await api.post(`/notificacoes/${id}/marcar-lida`);
}

export async function marcarTodasLidas(): Promise<number> {
  const { data } = await api.post<{ naoLidas: number }>('/notificacoes/marcar-todas-lidas');
  return data.naoLidas;
}

export async function arquivar(id: string): Promise<void> {
  await api.post(`/notificacoes/${id}/arquivar`);
}
