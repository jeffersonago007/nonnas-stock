import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, FilePlus, Plus, Trash2, Upload } from 'lucide-react';
import { toast } from 'sonner';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { PageHeader } from '@/components/PageHeader';
import { toastError } from '@/lib/toastError';
import { listarFiliais } from '@/features/cadastros/filiais/api';
import { listarUnidades } from '@/features/cadastros/insumos/api';
import { useAuthStore } from '@/features/auth/store';

import {
  type ItemRequest,
  type LancarRequest,
  lancarNotaFiscal,
  previewXml,
} from './api';

interface ItemForm {
  codigoInsumo: string;
  nomeInsumo: string;
  unidadeMedidaId: string;
  codigoFornecedor: string;
  descricaoOrigem: string;
  quantidade: string;
  valorUnitario: string;
  lote: string;
  dataValidade: string;
}

function itemVazio(unidadeDefault?: string): ItemForm {
  return {
    codigoInsumo: '',
    nomeInsumo: '',
    unidadeMedidaId: unidadeDefault ?? '',
    codigoFornecedor: '',
    descricaoOrigem: '',
    quantidade: '',
    valorUnitario: '',
    lote: '',
    dataValidade: '',
  };
}

export function LancarNotaFiscalPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const usuarioId = useAuthStore((s) => s.user?.id);

  const filiaisQuery = useQuery({ queryKey: ['filiais'], queryFn: () => listarFiliais() });
  const unidadesQuery = useQuery({ queryKey: ['unidades-medida'], queryFn: listarUnidades });

  const [filialId, setFilialId] = useState('');
  const [cnpjFornecedor, setCnpjFornecedor] = useState('');
  const [razaoFornecedor, setRazaoFornecedor] = useState('');
  const [numero, setNumero] = useState('');
  const [serie, setSerie] = useState('1');
  const [chaveNfe, setChaveNfe] = useState('');
  const [dataEmissao, setDataEmissao] = useState(new Date().toISOString().slice(0, 16));
  const [valorTotal, setValorTotal] = useState('');
  const [observacao, setObservacao] = useState('');
  const [itens, setItens] = useState<ItemForm[]>([itemVazio()]);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Mapa rápido código→id de unidade pra resolver "uCom" do XML.
  const unidadePorCodigo = useMemo(() => {
    const map: Record<string, string> = {};
    (unidadesQuery.data ?? []).forEach((u) => {
      map[u.codigo.toUpperCase()] = u.id;
    });
    return map;
  }, [unidadesQuery.data]);

  // Default da filial = primeira disponível (operador troca depois).
  useEffect(() => {
    if (!filialId && filiaisQuery.data && filiaisQuery.data.length > 0) {
      setFilialId(filiaisQuery.data[0].id);
    }
  }, [filiaisQuery.data, filialId]);

  const previewMutation = useMutation({
    mutationFn: previewXml,
    onSuccess: (preview) => {
      setCnpjFornecedor(preview.emitente.cnpj);
      setRazaoFornecedor(preview.emitente.razaoSocial);
      setNumero(preview.numero);
      setSerie(preview.serie);
      setChaveNfe(preview.chaveAcesso);
      setDataEmissao(preview.dataEmissao.slice(0, 16));
      setValorTotal(String(preview.valorTotal));
      setItens(
        preview.itens.map((it) => ({
          codigoInsumo: it.codigoFornecedor,
          nomeInsumo: it.descricao,
          unidadeMedidaId: unidadePorCodigo[it.unidadeComercial.toUpperCase()] ?? '',
          codigoFornecedor: it.codigoFornecedor,
          descricaoOrigem: it.descricao,
          quantidade: String(it.quantidade),
          valorUnitario: String(it.valorUnitario),
          lote: '',
          dataValidade: '',
        })),
      );
      toast.success(`XML lido: ${preview.itens.length} item(ns)`);
    },
    onError: (error) => toastError('Não foi possível ler o XML', error),
  });

  const lancarMutation = useMutation({
    mutationFn: lancarNotaFiscal,
    onSuccess: () => {
      toast.success('Nota fiscal lançada');
      queryClient.invalidateQueries({ queryKey: ['notas-fiscais'] });
      queryClient.invalidateQueries({ queryKey: ['saldo'] });
      navigate('/notas-fiscais');
    },
    onError: (error) => toastError('Não foi possível lançar a nota', error),
  });

  function adicionarItem() {
    setItens((prev) => [...prev, itemVazio()]);
  }
  function removerItem(idx: number) {
    setItens((prev) => prev.filter((_, i) => i !== idx));
  }
  function atualizarItem(idx: number, campo: keyof ItemForm, valor: string) {
    setItens((prev) => prev.map((it, i) => (i === idx ? { ...it, [campo]: valor } : it)));
  }

  function lancar() {
    if (!filialId) {
      toast.error('Selecione a filial');
      return;
    }
    if (!usuarioId) {
      toast.error('Sessão expirada — relogue');
      return;
    }
    if (!cnpjFornecedor || !razaoFornecedor) {
      toast.error('Informe CNPJ e razão social do fornecedor');
      return;
    }
    if (itens.length === 0) {
      toast.error('Adicione ao menos um item');
      return;
    }

    const itensReq: ItemRequest[] = itens.map((it) => ({
      insumo: {
        codigo: it.codigoInsumo,
        nome: it.nomeInsumo,
        unidadeBaseId: it.unidadeMedidaId,
      },
      codigoFornecedor: it.codigoFornecedor || null,
      descricaoOrigem: it.descricaoOrigem,
      quantidade: Number(it.quantidade),
      unidadeMedidaId: it.unidadeMedidaId,
      valorUnitario: Number(it.valorUnitario),
      valorTotal: Number(it.quantidade) * Number(it.valorUnitario),
      lote: it.lote || null,
      dataValidade: it.dataValidade || null,
    }));

    const payload: LancarRequest = {
      filialId,
      usuarioId,
      fornecedor: { cnpj: cnpjFornecedor, razaoSocial: razaoFornecedor },
      numero,
      serie,
      chaveNfe: chaveNfe || null,
      dataEmissao: new Date(dataEmissao).toISOString(),
      valorTotal: Number(valorTotal),
      observacao: observacao || null,
      itens: itensReq,
    };
    lancarMutation.mutate(payload);
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Lançar nota fiscal"
        description="Importe via XML NF-e ou preencha manualmente. Fornecedor e insumos novos serão cadastrados automaticamente."
        actions={
          <Button variant="outline" onClick={() => navigate('/notas-fiscais')}>
            <ArrowLeft className="h-4 w-4" /> Voltar
          </Button>
        }
      />

      <Tabs defaultValue="xml" className="space-y-4">
        <TabsList>
          <TabsTrigger value="xml">
            <Upload className="mr-2 h-4 w-4" /> Upload XML NF-e
          </TabsTrigger>
          <TabsTrigger value="manual">
            <FilePlus className="mr-2 h-4 w-4" /> Manual
          </TabsTrigger>
        </TabsList>

        <TabsContent value="xml">
          <div className="rounded-md border bg-card p-4">
            <Label htmlFor="xml-file">Arquivo .xml da NF-e</Label>
            <div className="flex gap-2 mt-1.5">
              <Input
                id="xml-file"
                ref={fileInputRef}
                type="file"
                accept=".xml,application/xml,text/xml"
              />
              <Button
                type="button"
                disabled={previewMutation.isPending}
                onClick={() => {
                  const file = fileInputRef.current?.files?.[0];
                  if (!file) {
                    toast.error('Selecione um arquivo XML');
                    return;
                  }
                  previewMutation.mutate(file);
                }}
              >
                {previewMutation.isPending ? 'Lendo…' : 'Carregar XML'}
              </Button>
            </div>
            <p className="mt-2 text-xs text-muted-foreground">
              Os campos abaixo serão pré-preenchidos. Você pode revisar e editar antes de lançar.
            </p>
          </div>
        </TabsContent>

        <TabsContent value="manual">
          <div className="rounded-md border bg-card p-4 text-sm text-muted-foreground">
            Preencha os campos abaixo manualmente. Use Manual quando não tiver o XML da NF-e.
          </div>
        </TabsContent>
      </Tabs>

      <div className="rounded-md border bg-card p-4 space-y-4">
        <h2 className="font-display text-lg">Cabeçalho</h2>
        <div className="grid gap-3 md:grid-cols-3">
          <div className="space-y-1.5">
            <Label htmlFor="filial">Filial</Label>
            <Select value={filialId} onValueChange={setFilialId}>
              <SelectTrigger id="filial">
                <SelectValue placeholder="Selecione" />
              </SelectTrigger>
              <SelectContent>
                {filiaisQuery.data?.map((f) => (
                  <SelectItem key={f.id} value={f.id}>
                    {f.nome}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="dataEmissao">Data de emissão</Label>
            <Input
              id="dataEmissao"
              type="datetime-local"
              value={dataEmissao}
              onChange={(e) => setDataEmissao(e.target.value)}
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="valorTotal">Valor total</Label>
            <Input
              id="valorTotal"
              type="number"
              step="0.01"
              value={valorTotal}
              onChange={(e) => setValorTotal(e.target.value)}
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="numero">Número</Label>
            <Input id="numero" value={numero} onChange={(e) => setNumero(e.target.value)} />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="serie">Série</Label>
            <Input id="serie" value={serie} onChange={(e) => setSerie(e.target.value)} />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="chave">Chave NF-e (44 dígitos)</Label>
            <Input
              id="chave"
              value={chaveNfe}
              onChange={(e) => setChaveNfe(e.target.value)}
              maxLength={44}
              placeholder="Opcional para lançamento manual"
              className="font-mono text-xs"
            />
          </div>
        </div>
      </div>

      <div className="rounded-md border bg-card p-4 space-y-4">
        <h2 className="font-display text-lg">Fornecedor</h2>
        <div className="grid gap-3 md:grid-cols-2">
          <div className="space-y-1.5">
            <Label htmlFor="cnpj">CNPJ (14 dígitos sem pontuação)</Label>
            <Input
              id="cnpj"
              value={cnpjFornecedor}
              onChange={(e) => setCnpjFornecedor(e.target.value.replace(/\D/g, ''))}
              maxLength={14}
              className="font-mono"
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="razao">Razão social</Label>
            <Input
              id="razao"
              value={razaoFornecedor}
              onChange={(e) => setRazaoFornecedor(e.target.value)}
            />
          </div>
        </div>
        <p className="text-xs text-muted-foreground">
          Se o fornecedor já existir (mesmo CNPJ), o sistema reaproveita. Senão, cria um novo.
        </p>
      </div>

      <div className="rounded-md border bg-card p-4 space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="font-display text-lg">Itens ({itens.length})</h2>
          <Button type="button" variant="outline" size="sm" onClick={adicionarItem}>
            <Plus className="h-4 w-4" /> Adicionar item
          </Button>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="border-b text-left text-xs uppercase tracking-wide text-muted-foreground">
              <tr>
                <th className="px-2 py-2 w-[140px]">Cód insumo</th>
                <th className="px-2 py-2 min-w-[180px]">Descrição</th>
                <th className="px-2 py-2 w-[100px]">Qtd</th>
                <th className="px-2 py-2 w-[140px]">Unidade</th>
                <th className="px-2 py-2 w-[120px]">Valor unit.</th>
                <th className="px-2 py-2 w-[120px]">Lote</th>
                <th className="px-2 py-2 w-[140px]">Validade</th>
                <th className="px-2 py-2 w-[40px]"></th>
              </tr>
            </thead>
            <tbody>
              {itens.map((item, idx) => (
                <tr key={idx} className="border-b last:border-0">
                  <td className="px-2 py-1.5">
                    <Input
                      value={item.codigoInsumo}
                      onChange={(e) => atualizarItem(idx, 'codigoInsumo', e.target.value)}
                      className="font-mono text-xs"
                    />
                  </td>
                  <td className="px-2 py-1.5">
                    <Input
                      value={item.descricaoOrigem}
                      onChange={(e) => {
                        atualizarItem(idx, 'descricaoOrigem', e.target.value);
                        if (!item.nomeInsumo) atualizarItem(idx, 'nomeInsumo', e.target.value);
                      }}
                    />
                  </td>
                  <td className="px-2 py-1.5">
                    <Input
                      type="number"
                      step="0.0001"
                      value={item.quantidade}
                      onChange={(e) => atualizarItem(idx, 'quantidade', e.target.value)}
                    />
                  </td>
                  <td className="px-2 py-1.5">
                    <Select
                      value={item.unidadeMedidaId}
                      onValueChange={(v) => atualizarItem(idx, 'unidadeMedidaId', v)}
                    >
                      <SelectTrigger>
                        <SelectValue placeholder="Sel." />
                      </SelectTrigger>
                      <SelectContent>
                        {unidadesQuery.data?.map((u) => (
                          <SelectItem key={u.id} value={u.id}>
                            {u.codigo}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </td>
                  <td className="px-2 py-1.5">
                    <Input
                      type="number"
                      step="0.0001"
                      value={item.valorUnitario}
                      onChange={(e) => atualizarItem(idx, 'valorUnitario', e.target.value)}
                    />
                  </td>
                  <td className="px-2 py-1.5">
                    <Input
                      value={item.lote}
                      onChange={(e) => atualizarItem(idx, 'lote', e.target.value)}
                      placeholder="(auto)"
                    />
                  </td>
                  <td className="px-2 py-1.5">
                    <Input
                      type="date"
                      value={item.dataValidade}
                      onChange={(e) => atualizarItem(idx, 'dataValidade', e.target.value)}
                    />
                  </td>
                  <td className="px-2 py-1.5 text-right">
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      onClick={() => removerItem(idx)}
                      disabled={itens.length === 1}
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <div className="rounded-md border bg-card p-4 space-y-2">
        <Label htmlFor="obs">Observação</Label>
        <Textarea
          id="obs"
          value={observacao}
          onChange={(e) => setObservacao(e.target.value)}
          rows={2}
        />
      </div>

      <div className="flex justify-end gap-2">
        <Button variant="outline" onClick={() => navigate('/notas-fiscais')}>
          Cancelar
        </Button>
        <Button onClick={lancar} disabled={lancarMutation.isPending}>
          {lancarMutation.isPending ? 'Lançando…' : 'Lançar nota'}
        </Button>
      </div>
    </div>
  );
}
