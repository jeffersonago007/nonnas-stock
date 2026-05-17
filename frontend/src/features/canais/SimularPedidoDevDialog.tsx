import { useEffect } from 'react';
import { Controller, useFieldArray, useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { z } from 'zod';
import { toast } from 'sonner';
import { Plus, Trash2, Wand2 } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { toastError } from '@/lib/toastError';

import {
  CANAIS,
  CANAL_LABEL,
  type CanalTipo,
  listarDepara,
  simularPedidoDev,
} from './api';

// Aceita "35", "35,5", "35.5" — normaliza vírgula→ponto antes de coerce.
const precoSchema = z
  .union([z.string(), z.number()])
  .transform((v) => (typeof v === 'string' ? parseFloat(v.replace(',', '.')) : v))
  .refine((n) => Number.isFinite(n) && n >= 0, { message: 'Preço inválido' });

const itemSchema = z.object({
  externalCode: z.string().min(1, 'Código externo obrigatório'),
  nome: z.string().min(1, 'Nome obrigatório'),
  quantidade: z.coerce.number().positive('Quantidade deve ser positiva'),
  unidade: z.string().min(1, 'Unidade obrigatória'),
  precoUnitario: precoSchema,
});

function formatPrecoPtBR(value: unknown): string {
  if (value === '' || value === null || value === undefined) return '';
  const n = typeof value === 'string' ? parseFloat(value.replace(',', '.')) : Number(value);
  if (!Number.isFinite(n)) return typeof value === 'string' ? value : '';
  return n.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

const taxaSchema = z
  .union([z.string(), z.number()])
  .optional()
  .transform((v) => {
    if (v === undefined || v === null || v === '') return undefined;
    const n = typeof v === 'string' ? parseFloat(v.replace(',', '.')) : v;
    return Number.isFinite(n) ? n : undefined;
  })
  .refine((n) => n === undefined || n >= 0, { message: 'Taxa não pode ser negativa' });

const schema = z.object({
  canal: z.enum(['IFOOD', 'NOVENTANOVE_FOOD', 'KEETA', 'OPEN_DELIVERY_GENERICO']),
  displayId: z.string().optional(),
  clienteNome: z.string().optional(),
  clienteTelefone: z.string().optional(),
  taxaEntrega: taxaSchema,
  taxaServico: taxaSchema,
  itens: z.array(itemSchema).min(1, 'Adicione ao menos 1 item'),
});

type Values = z.infer<typeof schema>;

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  filialId: string;
}

const ITEM_DEFAULT = {
  externalCode: '',
  nome: '',
  quantidade: 1,
  unidade: 'UN',
  precoUnitario: 0,
};

export function SimularPedidoDevDialog({ open, onOpenChange, filialId }: Props) {
  const queryClient = useQueryClient();

  const form = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: {
      canal: 'IFOOD',
      displayId: '',
      clienteNome: 'Cliente Demo',
      clienteTelefone: '',
      taxaEntrega: undefined,
      taxaServico: undefined,
      itens: [ITEM_DEFAULT],
    },
  });
  const { fields, append, remove } = useFieldArray({ control: form.control, name: 'itens' });

  // Sugestões: de-para já cadastrado pro canal selecionado.
  const canalSelecionado = form.watch('canal') as CanalTipo;
  const deparaQuery = useQuery({
    queryKey: ['canais-depara', canalSelecionado],
    queryFn: () => listarDepara(canalSelecionado),
    enabled: open,
  });

  useEffect(() => {
    if (open) {
      form.reset({
        canal: 'IFOOD',
        displayId: '',
        clienteNome: 'Cliente Demo',
        clienteTelefone: '',
        taxaEntrega: undefined,
        taxaServico: undefined,
        itens: [ITEM_DEFAULT],
      });
    }
  }, [open, form]);

  const mutation = useMutation({
    mutationFn: (v: Values) =>
      simularPedidoDev({
        canal: v.canal,
        filialId,
        displayId: v.displayId || null,
        clienteNome: v.clienteNome || null,
        clienteTelefone: v.clienteTelefone || null,
        taxaEntrega: v.taxaEntrega ?? null,
        taxaServico: v.taxaServico ?? null,
        itens: v.itens,
      }),
    onSuccess: (p) => {
      toast.success(`Pedido ${p.displayId ?? p.pedidoExternoId} criado`, {
        description: 'Clique em "Reprocessar" na linha do pedido para baixar o estoque.',
      });
      queryClient.invalidateQueries({ queryKey: ['canais-pedidos'] });
      onOpenChange(false);
    },
    onError: (e) => toastError('Não foi possível simular o pedido', e),
  });

  function aplicarDepara(idx: number, externalCode: string) {
    form.setValue(`itens.${idx}.externalCode`, externalCode, { shouldValidate: true });
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Wand2 className="h-4 w-4" /> Simular pedido (dev)
          </DialogTitle>
          <DialogDescription>
            Cria um pedido com status RECEBIDO na filial selecionada, como se tivesse chegado pelo
            canal. Em seguida clique "Reprocessar" na linha para acionar o pipeline real de baixa
            de estoque.
          </DialogDescription>
        </DialogHeader>

        <form
          className="space-y-4"
          onSubmit={form.handleSubmit((v) => mutation.mutate(v))}
          noValidate
        >
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-2">
              <Label htmlFor="canal">Canal</Label>
              <Select
                value={form.watch('canal')}
                onValueChange={(v) =>
                  form.setValue('canal', v as CanalTipo, { shouldValidate: true })
                }
              >
                <SelectTrigger id="canal">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {CANAIS.map((c) => (
                    <SelectItem key={c} value={c}>
                      {CANAL_LABEL[c]}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="displayId">Display ID (opcional)</Label>
              <Input
                id="displayId"
                placeholder="DEMO-001"
                {...form.register('displayId')}
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-2">
              <Label htmlFor="clienteNome">Cliente</Label>
              <Input id="clienteNome" {...form.register('clienteNome')} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="clienteTelefone">Telefone (opcional)</Label>
              <Input id="clienteTelefone" placeholder="(11) 9..." {...form.register('clienteTelefone')} />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-2">
              <Label htmlFor="taxaEntrega">Taxa de entrega (opcional)</Label>
              <Controller
                control={form.control}
                name="taxaEntrega"
                render={({ field }) => (
                  <Input
                    id="taxaEntrega"
                    type="text"
                    inputMode="decimal"
                    placeholder="0,00"
                    value={field.value === undefined || field.value === null
                      ? ''
                      : typeof field.value === 'number'
                        ? formatPrecoPtBR(field.value)
                        : String(field.value)}
                    onChange={(e) => field.onChange(e.target.value)}
                    onBlur={() => {
                      field.onChange(formatPrecoPtBR(field.value));
                      field.onBlur();
                    }}
                  />
                )}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="taxaServico">Taxa de serviço (opcional)</Label>
              <Controller
                control={form.control}
                name="taxaServico"
                render={({ field }) => (
                  <Input
                    id="taxaServico"
                    type="text"
                    inputMode="decimal"
                    placeholder="0,00"
                    value={field.value === undefined || field.value === null
                      ? ''
                      : typeof field.value === 'number'
                        ? formatPrecoPtBR(field.value)
                        : String(field.value)}
                    onChange={(e) => field.onChange(e.target.value)}
                    onBlur={() => {
                      field.onChange(formatPrecoPtBR(field.value));
                      field.onBlur();
                    }}
                  />
                )}
              />
            </div>
          </div>

          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <Label>Itens do pedido</Label>
              <Button type="button" size="sm" variant="outline" onClick={() => append(ITEM_DEFAULT)}>
                <Plus className="h-3 w-3" /> Adicionar item
              </Button>
            </div>

            <div className="max-h-[40vh] space-y-3 overflow-y-auto pr-1">
              {fields.map((field, idx) => (
                <div key={field.id} className="rounded-md border bg-muted/30 p-3 space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-medium text-muted-foreground">
                      Item #{idx + 1}
                    </span>
                    {fields.length > 1 && (
                      <Button
                        type="button"
                        size="sm"
                        variant="ghost"
                        onClick={() => remove(idx)}
                      >
                        <Trash2 className="h-3 w-3" /> Remover
                      </Button>
                    )}
                  </div>
                  <div className="grid grid-cols-3 gap-2">
                    <div className="col-span-2 space-y-1">
                      <Label className="text-xs">Código externo</Label>
                      <Input
                        placeholder="pizza-margherita-G"
                        {...form.register(`itens.${idx}.externalCode`)}
                      />
                      {deparaQuery.data && deparaQuery.data.length > 0 && (
                        <div className="flex flex-wrap gap-1 pt-1">
                          {deparaQuery.data.slice(0, 5).map((d) => (
                            <button
                              key={d.id}
                              type="button"
                              className="rounded bg-primary/10 px-2 py-0.5 text-xs text-primary hover:bg-primary/20"
                              onClick={() => aplicarDepara(idx, d.externalCode)}
                            >
                              {d.externalCode}
                            </button>
                          ))}
                        </div>
                      )}
                    </div>
                    <div className="space-y-1">
                      <Label className="text-xs">Nome no canal</Label>
                      <Input
                        placeholder="Pizza Margherita"
                        {...form.register(`itens.${idx}.nome`)}
                      />
                    </div>
                  </div>
                  <div className="grid grid-cols-3 gap-2">
                    <div className="space-y-1">
                      <Label className="text-xs">Quantidade</Label>
                      <Input
                        type="number"
                        step="1"
                        {...form.register(`itens.${idx}.quantidade`)}
                      />
                    </div>
                    <div className="space-y-1">
                      <Label className="text-xs">Unidade</Label>
                      <Input
                        placeholder="UN"
                        {...form.register(`itens.${idx}.unidade`)}
                      />
                    </div>
                    <div className="space-y-1">
                      <Label className="text-xs">Preço unitário</Label>
                      <Controller
                        control={form.control}
                        name={`itens.${idx}.precoUnitario`}
                        render={({ field }) => (
                          <Input
                            type="text"
                            inputMode="decimal"
                            placeholder="0,00"
                            value={field.value === undefined || field.value === null
                              ? ''
                              : typeof field.value === 'number'
                                ? formatPrecoPtBR(field.value)
                                : String(field.value)}
                            onChange={(e) => field.onChange(e.target.value)}
                            onBlur={() => {
                              field.onChange(formatPrecoPtBR(field.value));
                              field.onBlur();
                            }}
                          />
                        )}
                      />
                    </div>
                  </div>
                </div>
              ))}
            </div>

            {form.formState.errors.itens && (
              <p className="text-sm text-destructive">
                {form.formState.errors.itens.message ?? 'Verifique os itens'}
              </p>
            )}
          </div>

          <DialogFooter className="gap-2">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancelar
            </Button>
            <Button type="submit" disabled={mutation.isPending}>
              {mutation.isPending ? 'Criando…' : 'Simular'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
