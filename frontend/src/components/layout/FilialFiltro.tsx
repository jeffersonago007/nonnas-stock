import { useQuery } from '@tanstack/react-query';
import { Building2 } from 'lucide-react';

import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { listarFiliais } from '@/features/cadastros/filiais/api';
import { useFilialFiltroStore } from '@/features/filtroGlobal/store';

const TODAS = '__todas__';

export function FilialFiltro() {
  const filialId = useFilialFiltroStore((s) => s.filialId);
  const setFilial = useFilialFiltroStore((s) => s.setFilial);

  const filiaisQuery = useQuery({
    queryKey: ['filiais'],
    queryFn: () => listarFiliais(),
  });

  const ativas = filiaisQuery.data?.filter((f) => f.ativa) ?? [];

  return (
    <div className="flex items-center gap-2">
      <Building2 className="h-4 w-4 text-muted-foreground" />
      <Select
        value={filialId ?? TODAS}
        onValueChange={(v) => setFilial(v === TODAS ? null : v)}
      >
        <SelectTrigger className="h-8 w-[220px] text-sm">
          <SelectValue placeholder="Todas as filiais" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value={TODAS}>Todas as filiais</SelectItem>
          {ativas.map((f) => (
            <SelectItem key={f.id} value={f.id}>
              {f.nome}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  );
}
