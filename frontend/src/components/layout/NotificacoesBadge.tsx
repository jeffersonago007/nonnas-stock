import { useEffect, useRef } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Bell } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';

import { Button } from '@/components/ui/button';
import {
  contarNaoLidas,
  listarNotificacoes,
  type Notificacao,
} from '@/features/notificacoes/api';

const POLL_INTERVAL_MS = 30_000; // master doc 15.4 — 30s polling

/**
 * Badge no header com contagem de não-lidas. Faz polling a cada 30s.
 * Quando uma notificação CRITICA nova chega, dispara um toast.
 */
export function NotificacoesBadge() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const ultimaCriticaIdRef = useRef<string | null>(null);

  const contagemQuery = useQuery({
    queryKey: ['notificacoes-contagem'],
    queryFn: contarNaoLidas,
    refetchInterval: POLL_INTERVAL_MS,
    refetchIntervalInBackground: false,
  });

  // Detecta NOVA notificação CRITICA não-lida e dispara toast.
  // Faz uma query leve por separado quando o contador aumenta — evita
  // poluir o cache da página /notificacoes com payload completo.
  const criticasQuery = useQuery({
    queryKey: ['notificacoes-criticas-recentes'],
    queryFn: () =>
      listarNotificacoes({ somenteNaoLidas: true, size: 5 }).then((all) =>
        all.filter((n: Notificacao) => n.prioridade === 'CRITICA'),
      ),
    refetchInterval: POLL_INTERVAL_MS,
    refetchIntervalInBackground: false,
    enabled: (contagemQuery.data ?? 0) > 0,
  });

  useEffect(() => {
    const criticas = criticasQuery.data ?? [];
    if (criticas.length === 0) return;
    const maisRecente = criticas[0];
    if (maisRecente.id !== ultimaCriticaIdRef.current) {
      ultimaCriticaIdRef.current = maisRecente.id;
      toast.error(maisRecente.titulo, {
        description: maisRecente.mensagem,
        action: maisRecente.linkAcao
          ? {
              label: 'Ver',
              onClick: () => navigate(maisRecente.linkAcao ?? '/notificacoes'),
            }
          : undefined,
        duration: 10_000,
      });
    }
  }, [criticasQuery.data, navigate]);

  // Mutações em outras telas (marcar lida etc.) também disparam refresh aqui.
  useEffect(() => {
    const interval = setInterval(() => {
      queryClient.invalidateQueries({ queryKey: ['notificacoes-contagem'] });
    }, POLL_INTERVAL_MS);
    return () => clearInterval(interval);
  }, [queryClient]);

  const count = contagemQuery.data ?? 0;

  return (
    <Button
      variant="ghost"
      size="sm"
      className="relative"
      onClick={() => navigate('/notificacoes')}
      aria-label={`Notificações${count > 0 ? ` (${count} não lidas)` : ''}`}
    >
      <Bell className="h-4 w-4" />
      {count > 0 && (
        <span className="absolute -right-1 -top-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-destructive px-1 text-[10px] font-medium text-destructive-foreground">
          {count > 99 ? '99+' : count}
        </span>
      )}
    </Button>
  );
}
