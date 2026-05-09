import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';

export function DashboardPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl">Dashboard</h1>
        <p className="text-sm text-muted-foreground">
          Visão geral do estoque consolidado da rede.
        </p>
      </div>
      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <CardHeader>
            <CardDescription>Filiais ativas</CardDescription>
            <CardTitle className="text-3xl">—</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-xs text-muted-foreground">Conexão pendente — T13.</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardDescription>Alertas abertos</CardDescription>
            <CardTitle className="text-3xl">—</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-xs text-muted-foreground">Conexão pendente — T13.</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardDescription>Transferências em trânsito</CardDescription>
            <CardTitle className="text-3xl">—</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-xs text-muted-foreground">Conexão pendente — T14.</p>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
