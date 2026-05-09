import { cn } from '@/lib/utils';

interface Props {
  active: boolean;
  activeLabel?: string;
  inactiveLabel?: string;
}

export function StatusBadge({ active, activeLabel = 'Ativo', inactiveLabel = 'Inativo' }: Props) {
  return (
    <span
      className={cn(
        'inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium',
        active
          ? 'bg-secondary/10 text-secondary'
          : 'bg-muted text-muted-foreground',
      )}
    >
      {active ? activeLabel : inactiveLabel}
    </span>
  );
}
