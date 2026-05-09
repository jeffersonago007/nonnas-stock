import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';

interface Props {
  title: string;
  description: string;
  task: string;
}

export function PagePlaceholder({ title, description, task }: Props) {
  return (
    <div className="space-y-4">
      <h1 className="text-3xl">{title}</h1>
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">{description}</CardTitle>
          <CardDescription>Implementação prevista para {task}.</CardDescription>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground">
            Estrutura, design tokens e roteamento já estão configurados. Conteúdo dinâmico chega
            junto com a entrega de {task}.
          </p>
        </CardContent>
      </Card>
    </div>
  );
}
