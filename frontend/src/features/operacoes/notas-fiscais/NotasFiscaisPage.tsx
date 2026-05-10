import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Plus, FileText } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { PageHeader } from '@/components/PageHeader';
import { DataTable, type ColumnDef } from '@/components/data-table/DataTable';
import { useFilialFiltroStore } from '@/features/filtroGlobal/store';

import { type NotaFiscal, listarNotasFiscais } from './api';
import { NotaFiscalDetailDialog } from './NotaFiscalDetailDialog';

function formatBRL(valor: number): string {
  return valor.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('pt-BR');
}

export function NotasFiscaisPage() {
  const navigate = useNavigate();
  const filialId = useFilialFiltroStore((s) => s.filialId);
  const [detalheId, setDetalheId] = useState<string | null>(null);

  const notasQuery = useQuery({
    queryKey: ['notas-fiscais', filialId],
    queryFn: () => listarNotasFiscais(filialId ?? undefined),
  });

  const columns: ColumnDef<NotaFiscal>[] = [
    {
      key: 'numero',
      header: 'Nº / Série',
      cell: (n) => (
        <span className="font-medium">
          {n.numero}/{n.serie}
        </span>
      ),
      className: 'w-[120px]',
    },
    { key: 'data', header: 'Emissão', cell: (n) => formatDate(n.dataEmissao) },
    {
      key: 'chave',
      header: 'Chave NF-e',
      cell: (n) => (
        <span className="font-mono text-xs">
          {n.chaveNfe ? n.chaveNfe : <em className="text-muted-foreground">manual</em>}
        </span>
      ),
    },
    { key: 'itens', header: 'Itens', cell: (n) => n.itens.length, className: 'w-[80px]' },
    {
      key: 'valor',
      header: 'Valor total',
      cell: (n) => formatBRL(n.valorTotal),
      className: 'w-[140px] text-right',
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Notas fiscais"
        description="Histórico de notas fiscais lançadas (entrada de mercadoria por NF-e)."
        actions={
          <Button onClick={() => navigate('/notas-fiscais/lancar')}>
            <Plus className="h-4 w-4" /> Lançar nota
          </Button>
        }
      />

      <DataTable
        data={notasQuery.data}
        columns={columns}
        isLoading={notasQuery.isLoading}
        isError={notasQuery.isError}
        rowKey={(n) => n.id}
        onRowClick={(n) => setDetalheId(n.id)}
        emptyState={
          <div className="space-y-3">
            <FileText className="mx-auto h-10 w-10 text-muted-foreground" />
            <p>Nenhuma nota fiscal lançada ainda.</p>
            <Button variant="outline" onClick={() => navigate('/notas-fiscais/lancar')}>
              <Plus className="h-4 w-4" /> Lançar primeira nota
            </Button>
          </div>
        }
      />

      <NotaFiscalDetailDialog
        notaId={detalheId}
        onOpenChange={(open) => {
          if (!open) setDetalheId(null);
        }}
      />
    </div>
  );
}
