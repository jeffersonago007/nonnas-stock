import { useQuery } from '@tanstack/react-query';

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';

import { buscarNotaFiscal } from './api';
import { listarFornecedores } from '@/features/cadastros/fornecedores/api';
import { listarInsumos, listarUnidades } from '@/features/cadastros/insumos/api';
import { listarFiliais } from '@/features/cadastros/filiais/api';

interface Props {
  notaId: string | null;
  onOpenChange: (open: boolean) => void;
}

function formatBRL(valor: number): string {
  return valor.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}
function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('pt-BR');
}
function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString('pt-BR');
}

export function NotaFiscalDetailDialog({ notaId, onOpenChange }: Props) {
  const open = notaId !== null;

  const notaQuery = useQuery({
    queryKey: ['nota-fiscal', notaId],
    queryFn: () => buscarNotaFiscal(notaId!),
    enabled: open,
  });

  // Lookups para resolver ids → nomes legíveis. Usa listas existentes do app
  // (já cacheadas pelo react-query em outras telas).
  const fornecedoresQuery = useQuery({
    queryKey: ['fornecedores'],
    queryFn: () => listarFornecedores({}),
    enabled: open,
  });
  const filiaisQuery = useQuery({
    queryKey: ['filiais'],
    queryFn: () => listarFiliais(),
    enabled: open,
  });
  const insumosQuery = useQuery({
    queryKey: ['insumos', { all: true }],
    queryFn: () => listarInsumos({}),
    enabled: open,
  });
  const unidadesQuery = useQuery({
    queryKey: ['unidades-medida'],
    queryFn: listarUnidades,
    enabled: open,
  });

  const fornecedorNome = (id: string) =>
    fornecedoresQuery.data?.find((f) => f.id === id)?.razaoSocial ?? id;
  const filialNome = (id: string) =>
    filiaisQuery.data?.find((f) => f.id === id)?.nome ?? id;
  const insumoNome = (id: string) =>
    insumosQuery.data?.find((i) => i.id === id)?.nome ?? id;
  const unidadeCodigo = (id: string) =>
    unidadesQuery.data?.find((u) => u.id === id)?.codigo ?? id;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-3xl">
        <DialogHeader>
          <DialogTitle>Detalhes da nota fiscal</DialogTitle>
          <DialogDescription>
            Visualização somente-leitura. Para corrigir uma nota errada, lance um ajuste de estoque.
          </DialogDescription>
        </DialogHeader>

        {notaQuery.isLoading && (
          <p className="text-sm text-muted-foreground">Carregando…</p>
        )}
        {notaQuery.isError && (
          <p className="text-sm text-destructive">
            Falha ao carregar a nota. Tente novamente.
          </p>
        )}

        {notaQuery.data && (
          <div className="space-y-4 max-h-[70vh] overflow-y-auto">
            <section className="grid gap-3 md:grid-cols-3 rounded-md border p-3 text-sm">
              <Field label="Número / Série" value={`${notaQuery.data.numero}/${notaQuery.data.serie}`} />
              <Field label="Emissão" value={formatDate(notaQuery.data.dataEmissao)} />
              <Field label="Lançamento" value={formatDateTime(notaQuery.data.dataLancamento)} />
              <Field label="Valor total" value={formatBRL(notaQuery.data.valorTotal)} />
              <Field label="Filial" value={filialNome(notaQuery.data.filialId)} />
              <Field
                label="Chave NF-e"
                value={notaQuery.data.chaveNfe ?? '— (manual)'}
                mono
              />
            </section>

            <section className="rounded-md border p-3 text-sm">
              <h3 className="mb-2 font-display text-base">Fornecedor</h3>
              <p>{fornecedorNome(notaQuery.data.fornecedorId)}</p>
            </section>

            {notaQuery.data.observacao && (
              <section className="rounded-md border p-3 text-sm">
                <h3 className="mb-2 font-display text-base">Observação</h3>
                <p className="whitespace-pre-wrap">{notaQuery.data.observacao}</p>
              </section>
            )}

            <section className="rounded-md border p-3 text-sm">
              <h3 className="mb-2 font-display text-base">
                Itens ({notaQuery.data.itens.length})
              </h3>
              <div className="overflow-x-auto">
                <table className="w-full text-xs">
                  <thead className="border-b text-left uppercase tracking-wide text-muted-foreground">
                    <tr>
                      <th className="px-2 py-1.5">Insumo</th>
                      <th className="px-2 py-1.5">Cód. forn.</th>
                      <th className="px-2 py-1.5 text-right">Qtd</th>
                      <th className="px-2 py-1.5">Un</th>
                      <th className="px-2 py-1.5 text-right">Valor unit.</th>
                      <th className="px-2 py-1.5 text-right">Subtotal</th>
                      <th className="px-2 py-1.5">Lote</th>
                      <th className="px-2 py-1.5">Validade</th>
                    </tr>
                  </thead>
                  <tbody>
                    {notaQuery.data.itens.map((it) => (
                      <tr key={it.id} className="border-b last:border-0">
                        <td className="px-2 py-1.5">
                          <div>{insumoNome(it.insumoId)}</div>
                          <div className="text-[10px] text-muted-foreground">
                            {it.descricaoOrigem}
                          </div>
                        </td>
                        <td className="px-2 py-1.5 font-mono text-[10px]">
                          {it.codigoFornecedor ?? '—'}
                        </td>
                        <td className="px-2 py-1.5 text-right">
                          {it.quantidade.toLocaleString('pt-BR')}
                        </td>
                        <td className="px-2 py-1.5">{unidadeCodigo(it.unidadeMedidaId)}</td>
                        <td className="px-2 py-1.5 text-right">{formatBRL(it.valorUnitario)}</td>
                        <td className="px-2 py-1.5 text-right">{formatBRL(it.valorTotal)}</td>
                        <td className="px-2 py-1.5">{it.lote ?? '—'}</td>
                        <td className="px-2 py-1.5">
                          {it.dataValidade ? formatDate(it.dataValidade) : '—'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </section>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}

function Field({ label, value, mono }: { label: string; value: string; mono?: boolean }) {
  return (
    <div>
      <div className="text-[11px] uppercase tracking-wide text-muted-foreground">{label}</div>
      <div className={mono ? 'font-mono text-xs break-all' : ''}>{value}</div>
    </div>
  );
}
