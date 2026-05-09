import { create } from 'zustand';

/**
 * Filtro global de filial — afeta /estoque, /movimentacoes, /transferencias,
 * /alertas e o /dashboard. Persistido em sessionStorage (não localStorage)
 * para evitar fixação cruzada entre usuários no mesmo navegador.
 */
const STORAGE_KEY = 'nonnas.filialGlobal';

interface FilialFiltroState {
  filialId: string | null;
  setFilial: (id: string | null) => void;
}

function readInitial(): string | null {
  try {
    return sessionStorage.getItem(STORAGE_KEY);
  } catch {
    return null;
  }
}

export const useFilialFiltroStore = create<FilialFiltroState>((set) => ({
  filialId: readInitial(),
  setFilial: (id) => {
    if (id) sessionStorage.setItem(STORAGE_KEY, id);
    else sessionStorage.removeItem(STORAGE_KEY);
    set({ filialId: id });
  },
}));
