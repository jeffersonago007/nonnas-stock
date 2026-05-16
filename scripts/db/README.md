# Scripts SQL operacionais

Scripts ad-hoc para administração do banco. **Não são migrations Flyway** — são executados manualmente via `psql -f <arquivo>` quando necessário (reset de ambiente de dev, seeds não-versionados, manutenção pontual).

> ⚠️ Nenhum desses scripts deve rodar em produção sem revisão. Para dev/homologação apenas.

## Índice

| Arquivo | Quando usar | Reversível? |
|---|---|---|
| [reset_para_base_limpa.sql](reset_para_base_limpa.sql) | Zera todas as transações e cadastros, preservando empresa principal (Nonna Paola), 2 filiais (Interlagos + Vila das Belezas), usuário `admin@nonnas.com`, categoria seed "A classificar" e as 7 unidades-base seed (G/KG/ML/L/UN/CX/PORCAO). Útil pra começar uma rodada de teste manual com banco "recém-instalado". | Não (faça `pg_dump` antes se quiser voltar) |
| [seed_categorias_restaurante.sql](seed_categorias_restaurante.sql) | Cria 21 categorias padrão de restaurante baseadas em ERPs de mercado (Linx Food, Consinco, Sankhya, Goomer): alimentos, bebidas e operacionais. Não toca na seed "A classificar" (V015). | Sim (`DELETE FROM categorias_insumo WHERE nome IN (...)`) |

## Como executar

```powershell
# Conexão dev local (ver application-dev.yml)
$env:PGPASSWORD = 'nonnas_dev'
& "C:\Program Files\PostgreSQL\16\bin\psql.exe" `
  -h localhost -U nonnas -d nonnas_stock `
  -v ON_ERROR_STOP=1 -f scripts\db\<arquivo>.sql
```

`ON_ERROR_STOP=1` aborta a transação no primeiro erro. Todos os scripts aqui usam `BEGIN/COMMIT` para que falhas sejam atômicas.

## Quando criar um novo script aqui (vs. Flyway)

- **Flyway (Vnnn em `<modulo>/src/main/resources/db/migration/`)** — mudanças de schema versionadas, replicáveis em todos ambientes. Imutáveis depois do merge.
- **scripts/db/ (aqui)** — operações pontuais de dados: reset, seed não-determinístico, correção emergencial, exploração. Não fazem parte do schema oficial.
