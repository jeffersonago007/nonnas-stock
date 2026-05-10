import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { z } from 'zod';
import { toast } from 'sonner';
import { Plus, Trash2 } from 'lucide-react';

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
import { toastError } from '@/lib/toastError';

import { type Contato, type Fornecedor, atualizarFornecedor, criarFornecedor } from './api';

const cnpjRegex = /^\d{14}$/;

const createSchema = z.object({
  razaoSocial: z.string().min(1, 'Razão social é obrigatória'),
  cnpj: z.string().regex(cnpjRegex, 'Informe 14 dígitos sem pontuação'),
});
const updateSchema = z.object({
  razaoSocial: z.string().min(1, 'Razão social é obrigatória'),
});

type CreateValues = z.infer<typeof createSchema>;
type UpdateValues = z.infer<typeof updateSchema>;

interface ContatoForm {
  nome: string;
  email: string;
  telefone: string;
}

const linhaVazia = (): ContatoForm => ({ nome: '', email: '', telefone: '' });

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  fornecedor: Fornecedor | null;
}

export function FornecedorFormDialog({ open, onOpenChange, fornecedor }: Props) {
  const isEdit = fornecedor !== null;
  const queryClient = useQueryClient();
  const [contatos, setContatos] = useState<ContatoForm[]>([]);

  const createForm = useForm<CreateValues>({
    resolver: zodResolver(createSchema),
    defaultValues: { razaoSocial: '', cnpj: '' },
  });
  const updateForm = useForm<UpdateValues>({
    resolver: zodResolver(updateSchema),
    defaultValues: { razaoSocial: '' },
  });

  useEffect(() => {
    if (!open) return;
    if (fornecedor) {
      updateForm.reset({ razaoSocial: fornecedor.razaoSocial });
      setContatos(
        fornecedor.contatos.map((c) => ({
          nome: c.nome ?? '',
          email: c.email ?? '',
          telefone: c.telefone ?? '',
        })),
      );
    } else {
      createForm.reset();
      setContatos([]);
    }
  }, [open, fornecedor, createForm, updateForm]);

  const contatosLimpos = (): Contato[] =>
    contatos
      .map((c) => ({
        nome: c.nome.trim() || null,
        email: c.email.trim() || null,
        telefone: c.telefone.trim() || null,
      }))
      .filter((c) => c.nome || c.email || c.telefone);

  const createMutation = useMutation({
    mutationFn: (values: CreateValues) =>
      criarFornecedor({ ...values, contatos: contatosLimpos() }),
    onSuccess: () => {
      toast.success('Fornecedor criado');
      queryClient.invalidateQueries({ queryKey: ['fornecedores'] });
      onOpenChange(false);
    },
    onError: (error) => toastError('Não foi possível criar o fornecedor', error),
  });
  const updateMutation = useMutation({
    mutationFn: (values: UpdateValues) =>
      atualizarFornecedor(fornecedor!.id, { ...values, contatos: contatosLimpos() }),
    onSuccess: () => {
      toast.success('Fornecedor atualizado');
      queryClient.invalidateQueries({ queryKey: ['fornecedores'] });
      onOpenChange(false);
    },
    onError: (error) => toastError('Não foi possível atualizar', error),
  });

  function adicionarLinha() {
    setContatos((prev) => [...prev, linhaVazia()]);
  }
  function removerLinha(idx: number) {
    setContatos((prev) => prev.filter((_, i) => i !== idx));
  }
  function atualizar(idx: number, campo: keyof ContatoForm, valor: string) {
    setContatos((prev) => prev.map((c, i) => (i === idx ? { ...c, [campo]: valor } : c)));
  }

  const contatosSection = (
    <div className="space-y-2">
      <div className="flex items-center justify-between">
        <Label>Contatos (opcional)</Label>
        <Button type="button" variant="outline" size="sm" onClick={adicionarLinha}>
          <Plus className="h-4 w-4" /> Adicionar contato
        </Button>
      </div>
      {contatos.length === 0 && (
        <p className="text-xs text-muted-foreground">
          Nenhum contato cadastrado. Adicione vendedores, financeiro etc. para enriquecer
          a ficha do fornecedor.
        </p>
      )}
      {contatos.map((c, idx) => (
        <div key={idx} className="grid grid-cols-12 gap-2 rounded-md border p-2">
          <Input
            className="col-span-4"
            placeholder="Nome"
            value={c.nome}
            onChange={(e) => atualizar(idx, 'nome', e.target.value)}
          />
          <Input
            className="col-span-4"
            placeholder="E-mail"
            type="email"
            value={c.email}
            onChange={(e) => atualizar(idx, 'email', e.target.value)}
          />
          <Input
            className="col-span-3"
            placeholder="Telefone"
            value={c.telefone}
            onChange={(e) => atualizar(idx, 'telefone', e.target.value)}
          />
          <Button
            type="button"
            variant="ghost"
            size="sm"
            className="col-span-1"
            onClick={() => removerLinha(idx)}
          >
            <Trash2 className="h-4 w-4" />
          </Button>
        </div>
      ))}
      <p className="text-xs text-muted-foreground">
        Pelo menos um campo (nome, e-mail ou telefone) precisa estar preenchido.
        Linhas em branco são ignoradas no envio.
      </p>
    </div>
  );

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>{isEdit ? 'Editar fornecedor' : 'Novo fornecedor'}</DialogTitle>
          <DialogDescription>
            {isEdit
              ? 'Edite razão social e contatos. CNPJ é imutável após cadastro.'
              : 'Cadastre um novo fornecedor pelo CNPJ. Contatos são opcionais.'}
          </DialogDescription>
        </DialogHeader>

        {isEdit ? (
          <form
            className="space-y-4"
            onSubmit={updateForm.handleSubmit((v) => updateMutation.mutate(v))}
            noValidate
          >
            <div className="space-y-2">
              <Label htmlFor="razaoSocial">Razão social</Label>
              <Input
                id="razaoSocial"
                aria-invalid={Boolean(updateForm.formState.errors.razaoSocial)}
                {...updateForm.register('razaoSocial')}
              />
              {updateForm.formState.errors.razaoSocial && (
                <p className="text-sm text-destructive">
                  {updateForm.formState.errors.razaoSocial.message}
                </p>
              )}
            </div>
            {contatosSection}
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
              <Label htmlFor="razaoSocial">Razão social</Label>
              <Input
                id="razaoSocial"
                aria-invalid={Boolean(createForm.formState.errors.razaoSocial)}
                {...createForm.register('razaoSocial')}
              />
              {createForm.formState.errors.razaoSocial && (
                <p className="text-sm text-destructive">
                  {createForm.formState.errors.razaoSocial.message}
                </p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="cnpj">CNPJ</Label>
              <Input
                id="cnpj"
                placeholder="14 dígitos sem pontuação"
                maxLength={14}
                aria-invalid={Boolean(createForm.formState.errors.cnpj)}
                {...createForm.register('cnpj')}
              />
              {createForm.formState.errors.cnpj && (
                <p className="text-sm text-destructive">
                  {createForm.formState.errors.cnpj.message}
                </p>
              )}
            </div>
            {contatosSection}
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
