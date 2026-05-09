import { toast } from 'sonner';

import { extractProblem } from './api';

/**
 * Traduz erro Axios em mensagem de toast.
 *
 * O backend usa RFC 7807 (Problem Details) — o `GlobalExceptionHandler`
 * em web-commons retorna `detail` com mensagem amigável e `errors` com
 * field-level validation. Formatação:
 *   - 400 com `errors`: lista os campos inválidos.
 *   - Demais: mostra `detail` ou fallback genérico.
 */
export function toastError(fallback: string, error: unknown) {
  const problem = extractProblem(error);
  if (problem?.errors && Object.keys(problem.errors).length > 0) {
    const fields = Object.entries(problem.errors)
      .map(([field, msg]) => `${field}: ${msg}`)
      .join('\n');
    toast.error(problem.title ?? 'Validação falhou', { description: fields });
    return;
  }
  toast.error(problem?.title ?? fallback, {
    description: problem?.detail ?? undefined,
  });
}
