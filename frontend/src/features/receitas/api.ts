import { api } from '@/lib/api';

export interface FichaItem {
  id: string;
  insumoId: string;
  unidadeId: string;
  quantidade: number;
}

export interface FichaTecnica {
  id: string;
  produtoVendavelId: string;
  versao: number;
  vigenteDesde: string;
  vigenteAte: string | null;
  ativa: boolean;
  itens: FichaItem[];
}

export interface FichaItemRequest {
  insumoId: string;
  unidadeId: string;
  quantidade: number;
}

export interface FichaRequest {
  itens: FichaItemRequest[];
}

export async function buscarFichaVigente(produtoId: string): Promise<FichaTecnica | null> {
  try {
    const { data } = await api.get<FichaTecnica>(
      `/produtos-vendaveis/${produtoId}/fichas/vigente`,
    );
    return data;
  } catch (error) {
    // Se ainda não existe ficha vigente, backend retorna 404 — UI trata
    // como estado normal "produto sem ficha".
    if (
      typeof error === 'object' &&
      error !== null &&
      'response' in error &&
      (error as { response: { status: number } }).response?.status === 404
    ) {
      return null;
    }
    throw error;
  }
}

export async function listarHistoricoFichas(produtoId: string): Promise<FichaTecnica[]> {
  const { data } = await api.get<FichaTecnica[]>(`/produtos-vendaveis/${produtoId}/fichas`);
  return data;
}

export async function criarFicha(produtoId: string, payload: FichaRequest): Promise<FichaTecnica> {
  const { data } = await api.post<FichaTecnica>(
    `/produtos-vendaveis/${produtoId}/fichas`,
    payload,
  );
  return data;
}

export async function atualizarFichaVigente(
  produtoId: string,
  payload: FichaRequest,
): Promise<FichaTecnica> {
  const { data } = await api.put<FichaTecnica>(
    `/produtos-vendaveis/${produtoId}/fichas/vigente`,
    payload,
  );
  return data;
}
