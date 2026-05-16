import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { z } from 'zod';
import { toast } from 'sonner';

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
import { listarFiliais } from '@/features/cadastros/filiais/api';

import {
  CANAIS,
  CANAL_LABEL,
  type CanalTipo,
  type CredencialCanal,
  atualizarCredencial,
  criarCredencial,
} from './api';

const canalEnum = z.enum(['IFOOD', 'NOVENTANOVE_FOOD', 'KEETA', 'OPEN_DELIVERY_GENERICO']);

const createSchema = z.object({
  canalTipo: canalEnum,
  filialId: z.string().uuid('Selecione uma filial'),
  merchantExternoId: z.string().min(1, 'ID do merchant no canal é obrigatório').max(120),
  clientId: z.string().min(1, 'Client ID é obrigatório').max(255),
  clientSecret: z.string().min(1, 'Client secret é obrigatório'),
  baseUrl: z.string().url('URL inválida').optional().or(z.literal('')),
  observacao: z.string().max(500).optional().or(z.literal('')),
});

const updateSchema = z.object({
  baseUrl: z.string().url('URL inválida').optional().or(z.literal('')),
  observacao: z.string().max(500).optional().or(z.literal('')),
  /** vazio = não rotaciona; preenchido = rotaciona. */
  clientSecret: z.string().optional().or(z.literal('')),
});

type CreateValues = z.infer<typeof createSchema>;
type UpdateValues = z.infer<typeof updateSchema>;

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  credencial: CredencialCanal | null;
}

export function CredencialFormDialog({ open, onOpenChange, credencial }: Props) {
  const isEdit = credencial !== null;
  const queryClient = useQueryClient();

  const filiaisQuery = useQuery({
    queryKey: ['filiais'],
    queryFn: () => listarFiliais(),
    enabled: open && !isEdit,
  });

  const createForm = useForm<CreateValues>({
    resolver: zodResolver(createSchema),
    defaultValues: {
      canalTipo: 'IFOOD',
      filialId: '',
      merchantExternoId: '',
      clientId: '',
      clientSecret: '',
      baseUrl: '',
      observacao: '',
    },
  });
  const updateForm = useForm<UpdateValues>({
    resolver: zodResolver(updateSchema),
    defaultValues: { baseUrl: '', observacao: '', clientSecret: '' },
  });

  useEffect(() => {
    if (open && credencial) {
      updateForm.reset({
        baseUrl: credencial.baseUrl ?? '',
        observacao: credencial.observacao ?? '',
        clientSecret: '',
      });
    } else if (open && !credencial) {
      createForm.reset({
        canalTipo: 'IFOOD',
        filialId: '',
        merchantExternoId: '',
        clientId: '',
        clientSecret: '',
        baseUrl: '',
        observacao: '',
      });
    }
  }, [open, credencial, createForm, updateForm]);

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['canais-credenciais'] });
  };

  const createMutation = useMutation({
    mutationFn: (v: CreateValues) =>
      criarCredencial({
        canalTipo: v.canalTipo,
        filialId: v.filialId,
        merchantExternoId: v.merchantExternoId,
        clientId: v.clientId,
        clientSecret: v.clientSecret,
        baseUrl: v.baseUrl || null,
        observacao: v.observacao || null,
      }),
    onSuccess: () => {
      toast.success('Credencial criada');
      invalidate();
      onOpenChange(false);
    },
    onError: (e) => toastError('Não foi possível criar a credencial', e),
  });

  const updateMutation = useMutation({
    mutationFn: (v: UpdateValues) =>
      atualizarCredencial(credencial!.id, {
        baseUrl: v.baseUrl || null,
        observacao: v.observacao || null,
        clientSecret: v.clientSecret && v.clientSecret.length > 0 ? v.clientSecret : null,
      }),
    onSuccess: (_, vars) => {
      toast.success(
        vars.clientSecret ? 'Credencial atualizada e segredo rotacionado' : 'Credencial atualizada',
      );
      invalidate();
      onOpenChange(false);
    },
    onError: (e) => toastError('Não foi possível atualizar', e),
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{isEdit ? 'Editar credencial' : 'Nova credencial'}</DialogTitle>
          <DialogDescription>
            {isEdit
              ? 'Canal, filial, merchant ID e client ID não são editáveis (crie nova credencial). Deixe o campo de segredo em branco para mantê-lo; preencha para rotacionar.'
              : 'O segredo é cifrado antes de salvar e não volta no response.'}
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
                <p className="font-medium">{CANAL_LABEL[credencial!.canalTipo]}</p>
              </div>
              <div>
                <Label className="text-xs text-muted-foreground">Merchant externo</Label>
                <p className="font-medium">{credencial!.merchantExternoId}</p>
              </div>
              <div>
                <Label className="text-xs text-muted-foreground">Client ID</Label>
                <code className="text-xs">{credencial!.clientId}</code>
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="baseUrl">Base URL (opcional)</Label>
              <Input
                id="baseUrl"
                placeholder="https://merchant-api.ifood.com.br"
                {...updateForm.register('baseUrl')}
              />
              {updateForm.formState.errors.baseUrl && (
                <p className="text-sm text-destructive">
                  {updateForm.formState.errors.baseUrl.message}
                </p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="clientSecret">Client secret (deixe vazio para manter)</Label>
              <Input
                id="clientSecret"
                type="password"
                placeholder="Preencha apenas para rotacionar"
                {...updateForm.register('clientSecret')}
              />
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
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-2">
                <Label htmlFor="canalTipo">Canal</Label>
                <Select
                  value={createForm.watch('canalTipo')}
                  onValueChange={(v) =>
                    createForm.setValue('canalTipo', v as CanalTipo, { shouldValidate: true })
                  }
                >
                  <SelectTrigger id="canalTipo">
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
                <Label htmlFor="filialId">Filial</Label>
                <Select
                  value={createForm.watch('filialId')}
                  onValueChange={(v) => createForm.setValue('filialId', v, { shouldValidate: true })}
                >
                  <SelectTrigger id="filialId" aria-invalid={Boolean(createForm.formState.errors.filialId)}>
                    <SelectValue
                      placeholder={filiaisQuery.isLoading ? 'Carregando…' : 'Selecione…'}
                    />
                  </SelectTrigger>
                  <SelectContent>
                    {filiaisQuery.data?.map((f) => (
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
            </div>
            <div className="space-y-2">
              <Label htmlFor="merchantExternoId">ID do merchant no canal</Label>
              <Input
                id="merchantExternoId"
                placeholder="ID que o canal usa pra identificar essa loja"
                {...createForm.register('merchantExternoId')}
              />
              {createForm.formState.errors.merchantExternoId && (
                <p className="text-sm text-destructive">
                  {createForm.formState.errors.merchantExternoId.message}
                </p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="clientId">Client ID</Label>
              <Input id="clientId" {...createForm.register('clientId')} />
              {createForm.formState.errors.clientId && (
                <p className="text-sm text-destructive">
                  {createForm.formState.errors.clientId.message}
                </p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="clientSecret">Client secret</Label>
              <Input
                id="clientSecret"
                type="password"
                {...createForm.register('clientSecret')}
              />
              {createForm.formState.errors.clientSecret && (
                <p className="text-sm text-destructive">
                  {createForm.formState.errors.clientSecret.message}
                </p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="baseUrl">Base URL (opcional)</Label>
              <Input
                id="baseUrl"
                placeholder="https://merchant-api.ifood.com.br"
                {...createForm.register('baseUrl')}
              />
              {createForm.formState.errors.baseUrl && (
                <p className="text-sm text-destructive">
                  {createForm.formState.errors.baseUrl.message}
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
