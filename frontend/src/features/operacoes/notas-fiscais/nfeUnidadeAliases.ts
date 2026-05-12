// Mapeia variações comuns de `uCom` da NF-e para a sigla canônica cadastrada
// em `unidades_medida` (G/KG/ML/L/UN/CX/PORCAO). Quando não há equivalente
// no cadastro, retorna null — o operador escolhe manualmente.

const ALIASES: Record<string, string> = {
  UN: 'UN', UND: 'UN', UNID: 'UN', UNI: 'UN', U: 'UN',
  PC: 'UN', PCS: 'UN', PEC: 'UN', PECA: 'UN', PECAS: 'UN',
  KG: 'KG', QUILO: 'KG', QUILOS: 'KG', QUILOGRAMA: 'KG',
  G: 'G', GR: 'G', GRS: 'G', GRAMA: 'G', GRAMAS: 'G',
  L: 'L', LT: 'L', LTR: 'L', LITRO: 'L', LITROS: 'L',
  ML: 'ML', MLT: 'ML', MILILITRO: 'ML', MILILITROS: 'ML',
  CX: 'CX', CXA: 'CX', CAIXA: 'CX', CAIXAS: 'CX', CT: 'CX',
  PORCAO: 'PORCAO', PORCOES: 'PORCAO',
  PAR: 'PAR', PARES: 'PAR',
};

function normalizar(s: string): string {
  return s.normalize('NFD').replace(/[̀-ͯ]/g, '').toUpperCase().trim();
}

export function siglaCanonica(uCom: string | null | undefined): string | null {
  if (!uCom) return null;
  return ALIASES[normalizar(uCom)] ?? null;
}
