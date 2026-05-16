import { useEffect, useMemo } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { z } from 'zod';
import { toast } from 'sonner';
import { AlertTriangle } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
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
import type { Filial } from '@/features/cadastros/filiais/api';
import type { Produto } from '@/features/cadastros/produtos/api';

import {
  CANAL_LABEL,
  type CanalProdutoDePara,
  type CanalTipo,
  atualizarDepara,
  criarDepara,
} from './api';

const ESCOPO_GLOBAL = 'GLOBAL';
const ESCOPO_FILIAL = 'FILIAL';

const createSchema = z
  .object({
    externalCode: z.string().min(1, 'Código externo é obrigatório').max(120),
    escopo: z.enum([ESCOPO_GLOBAL, ESCOPO_FILIAL]),
    filialId: z.string().optional(),
    produtoVendavelId: z.string().uuid('Selecione um produto'),
    observacao: z.string().max(500).optional().or(z.literal('')),
  })
  .superRefine((data, ctx) => {
    if (data.escopo === ESCOPO_FILIAL && !data.filialId) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['filialId'],
        message: 'Selecione a filial',
      });
    }
  });

const updateSchema = z.object({
  produtoVendavelId: z.string().uuid('Selecione um produto'),
  observacao: z.string().max(500).optional().or(z.literal('')),
});

type CreateValues = z.infer<typeof createSchema>;
type UpdateValues = z.infer<typeof updateSchema>;

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  depara: CanalProdutoDePara | null;
  canalAtual: CanalTipo;
  existentesNoCanal: CanalProdutoDePara[];
  produtos: Produto[];
  filiais: Filial[];
}

export function DeparaFormDialog({
  open,
  onOpenChange,
  depara,
  canalAtual,
  existentesNoCanal,
  produtos,
  filiais,
}: Props) {
  const isEdit = depara !== null;
  const queryClient = useQueryClient();

  const createForm = useForm<CreateValues>({
    resolver: zodResolver(createSchema),
    defaultValues: {
      externalCode: '',
      escopo: ESCOPO_GLOBAL,
      filialId: '',
      produtoVendavelId: '',
      observacao: '',
    },
  });
  const updateForm = useForm<UpdateValues>({
    resolver: zodResolver(updateSchema),
    defaultValues: { produtoVendavelId: '', observacao: '' },
  });

  useEffect(() => {
    if (open && depara) {
      updateForm.reset({
        produtoVendavelId: depara.produtoVendavelId,
        observacao: depara.observacao ?? '',
      });
    } else if (open && !depara) {
      createForm.reset({
        externalCode: '',
        escopo: ESCOPO_GLOBAL,
        filialId: '',
        produtoVendavelId: '',
        observacao: '',
      });
    }
  }, [open, depara, createForm, updateForm]);

  // Lookup inline: ao digitar externalCode + escolher escopo, detecta
  // colisão entre o que o operador está montando e o que já existe no canal.
  const externalCode = createForm.watch('externalCode');
  const escopo = createForm.watch('escopo');
  const filialIdSel = createForm.watch('filialId');

  const colisoes = useMemo(() => {
    if (isEdit) return { global: null, especifico: null };
    const code = externalCode.trim();
    if (!code) return { global: null, especifico: null };
    const matchCode = existentesNoCanal.filter(
      (d) => d.externalCode.toLowerCase() === code.toLowerCase(),
    );
    const global = matchCode.find((d) => d.filialId === null) ?? null;
    const especifico =
      escopo === ESCOPO_FILIAL && filialIdSel
        ? matchCode.find((d) => d.filialId === filialIdSel) ?? null
        : null;
    return { global, especifico };
  }, [externalCode, escopo, filialIdSel, existentesNoCanal, isEdit]);

  const produtoMap = useMemo(() => {
    const m = new Map<string, string>();
    produtos.forEach((p) => m.set(p.id, p.nome));
    return m;
  }, [produtos]);

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['canais-depara'] });
  };

  const createMutation = useMutation({
    mutationFn: (v: CreateValues) =>
      criarDepara({
        canalTipo: canalAtual,
        externalCode: v.externalCode,
        filialId: v.escopo === ESCOPO_FILIAL ? v.filialId : null,
        produtoVendavelId: v.produtoVendavelId,
        observacao: v.observacao || null,
      }),
    onSuccess: () => {
      toast.success('Mapeamento criado');
      invalidate();
      onOpenChange(false);
    },
    onError: (e) => toastError('Não foi possível criar', e),
  });

  const updateMutation = useMutation({
    mutationFn: (v: UpdateValues) =>
      atualizarDepara(depara!.id, {
        produtoVendavelId: v.produtoVendavelId,
        observacao: v.observacao || null,
      }),
    onSuccess: () => {
      toast.success('Mapeamento atualizado');
      invalidate();
      onOpenChange(false);
    },
    onError: (e) => toastError('Não foi possível atualizar', e),
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{isEdit ? 'Editar mapeamento' : 'Novo mapeamento'}</DialogTitle>
          <DialogDescription>
            {isEdit
              ? 'Canal, código externo e escopo não são editáveis (remova e crie um novo se precisar mudar).'
              : `Canal: ${CANAL_LABEL[canalAtual]}. O código externo é o ID que o canal envia em cada item do pedido.`}
          </DialogDescription>
        </DialogHeader>

        {isEdit ? (
          <form
            className="space-y-4"
            onSubmit={updateForm.handleSubmit((v) => updateMutation.mutate(v))}
            noValidate
          >
            <div className="grid grid-cols-2 gap-3 text-sm">
              <div>
                <Label className="text-xs text-muted-foreground">Canal</Label>
                <p className="font-medium">{CANAL_LABEL[depara!.canalTipo]}</p>
              </div>
              <div>
                <Label className="text-xs text-muted-foreground">Código externo</Label>
                <code className="text-xs">{depara!.externalCode}</code>
              </div>
              <div className="col-span-2">
                <Label className="text-xs text-muted-foreground">Escopo</Label>
                <p>
                  {depara!.filialId === null
                    ? 'Global (todas as filiais)'
                    : `Filial específica: ${filiais.find((f) => f.id === depara!.filialId)?.nome ?? '—'}`}
                </p>
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="produtoVendavelId">Produto vendável</Label>
              <Select
                value={updateForm.watch('produtoVendavelId')}
                onValueChange={(v) =>
                  updateForm.setValue('produtoVendavelId', v, { shouldValidate: true })
                }
              >
                <SelectTrigger
                  id="produtoVendavelId"
                  aria-invalid={Boolean(updateForm.formState.errors.produtoVendavelId)}
                >
                  <SelectValue placeholder="Selecione…" />
                </SelectTrigger>
                <SelectContent>
                  {produtos.map((p) => (
                    <SelectItem key={p.id} value={p.id}>
                      {p.nome}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {updateForm.formState.errors.produtoVendavelId && (
                <p className="text-sm text-destructive">
                  {updateForm.formState.errors.produtoVendavelId.message}
                </p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="observacao">Observação</Label>
              <Textarea id="observacao" rows={2} {...updateForm.register('observacao')} />
            </div>
            <DialogFooter className="gap-2">
              <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                Cancelar
              </Button>
              <Button type="submit" disabled={updateMutation.isPending}>
                {updateMutation.isPending ? 'Salvando…' : 'Salvar'}
              </Button>
            </DialogFooter>
          </form>
        ) : (
          <form
            className="space-y-4"
            onSubmit={createForm.handleSubmit((v) => createMutation.mutate(v))}
            noValidate
          >
            <div className="space-y-2">
              <Label htmlFor="externalCode">Código externo</Label>
              <Input
                id="externalCode"
                placeholder="ID que o canal envia no item (ex.: 'pizza-margherita-G')"
                {...createForm.register('externalCode')}
              />
              {createForm.formState.errors.externalCode && (
                <p className="text-sm text-destructive">
                  {createForm.formState.errors.externalCode.message}
                </p>
              )}
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-2">
                <Label htmlFor="escopo">Escopo</Label>
                <Select
                  value={createForm.watch('escopo')}
                  onValueChange={(v) =>
                    createForm.setValue('escopo', v as typeof ESCOPO_GLOBAL, {
                      shouldValidate: true,
                    })
                  }
                >
                  <SelectTrigger id="escopo">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value={ESCOPO_GLOBAL}>Global (todas as filiais)</SelectItem>
                    <SelectItem value={ESCOPO_FILIAL}>Filial específica</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              {createForm.watch('escopo') === ESCOPO_FILIAL && (
                <div className="space-y-2">
                  <Label htmlFor="filialId">Filial</Label>
                  <Select
                    value={createForm.watch('filialId') ?? ''}
                    onValueChange={(v) =>
                      createForm.setValue('filialId', v, { shouldValidate: true })
                    }
                  >
                    <SelectTrigger
                      id="filialId"
                      aria-invalid={Boolean(createForm.formState.errors.filialId)}
                    >
                      <SelectValue placeholder="Selecione…" />
                    </SelectTrigger>
                    <SelectContent>
                      {filiais.map((f) => (
                        <SelectItem key={f.id} value={f.id}>
                          {f.nome}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  {createForm.formState.errors.filialId && (
                    <p className="text-sm text-destructive">
                      {createForm.formState.errors.filialId.message}
                    </p>
                  )}
                </div>
              )}
            </div>

            {/* Aviso de colisão inline */}
            {(colisoes.especifico || colisoes.global) && (
              <div className="rounded-md border border-amber-300 bg-amber-50 p-3 text-sm">
                <div className="mb-1 flex items-center gap-2 font-medium text-amber-900">
                  <AlertTriangle className="h-4 w-4" />
                  Já existe mapeamento para esse código externo
                </div>
                {colisoes.especifico && (
                  <p className="text-amber-900">
                    Específico desta filial → {' '}
                    <strong>{produtoMap.get(colisoes.especifico.produtoVendavelId) ?? 'produto'}</strong>.
                    Salvar este formulário vai falhar com erro de duplicidade — edite o existente em vez disso.
                  </p>
                )}
                {colisoes.global && !colisoes.especifico && (
                  <p className="text-amber-900">
                    Global ativo → <strong>{produtoMap.get(colisoes.global.produtoVendavelId) ?? 'produto'}</strong>.
                    {escopo === ESCOPO_FILIAL
                      ? ' Criar específico-filial sobrescreve o global apenas para essa filial (precedência por filial).'
                      : ' Criar outro global vai falhar com erro de duplicidade — edite o existente em vez disso.'}
                  </p>
                )}
              </div>
            )}

            <div className="space-y-2">
              <Label htmlFor="produtoVendavelId">Produto vendável</Label>
              <Select
                value={createForm.watch('produtoVendavelId') ?? ''}
                onValueChange={(v) =>
                  createForm.setValue('produtoVendavelId', v, { shouldValidate: true })
                }
              >
                <SelectTrigger
                  id="produtoVendavelId"
                  aria-invalid={Boolean(createForm.formState.errors.produtoVendavelId)}
                >
                  <SelectValue placeholder="Selecione…" />
                </SelectTrigger>
                <SelectContent>
                  {produtos.map((p) => (
                    <SelectItem key={p.id} value={p.id}>
                      {p.nome}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {createForm.formState.errors.produtoVendavelId && (
                <p className="text-sm text-destructive">
                  {createForm.formState.errors.produtoVendavelId.message}
                </p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="observacao">Observação</Label>
              <Textarea id="observacao" rows={2} {...createForm.register('observacao')} />
            </div>
            <DialogFooter className="gap-2">
              <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                Cancelar
              </Button>
              <Button type="submit" disabled={createMutation.isPending}>
                {createMutation.isPending ? 'Criando…' : 'Criar'}
              </Button>
            </DialogFooter>
          </form>
        )}
      </DialogContent>
    </Dialog>
  );
}
