## Context

`etc/database/domain_model.md` é a fonte da verdade do schema físico do projeto (convenção estabelecida na change arquivada `2026-06-08-adequacao-liquibase-domain-model`). O Liquibase hoje implementa a versão anterior do domain model (~16 tabelas, sem `CHECK`, com só 4 triggers de validação/normalização). O domain model foi atualizado localmente e agora especifica ~34 tabelas, ~20 `CHECK`, ~15 triggers e ~20 índices — incluindo SQL completo pronto para as functions/triggers, o que reduz a ambiguidade de implementação.

Sistema sem versão em produção: não há dados a preservar, não há necessidade de migração incremental compatível com dados existentes.

## Goals / Non-Goals

**Goals:**
- `liquibase update` em banco limpo produz exatamente o schema do domain model atual: tabelas, colunas, tipos, nullability, UKs/FKs, `CHECK`, triggers, índices
- Cada changeSet com `rollback` funcional, seguindo os padrões de nomenclatura já em uso (`PK_*`, `FK_*`, `UK_*`, `IDX_*`, `TRG_*`, `CHK_*`)
- Reaproveitar os padrões de arquivo já estabelecidos: 1 `.yml` por tabela em `v1.0.0/tables/`, 1 par `.sql`+entrada YAML por function/trigger

**Non-Goals:**
- Alterar entidades JPA ou qualquer código Java — consequência prevista, mas escopo de outra change
- Migração de dados — banco é recriado do zero
- Popular `SCOS_LEGAL_NATURE`/`SCOS_CNAE` com os códigos oficiais IBGE — sobem como schema vazio; seed é tarefa separada (decisão confirmada com o usuário)
- Quebrar esta change em várias menores por área de domínio — decisão confirmada com o usuário de manter como uma change só, mesmo cobrindo 7 capabilities

## Decisions

| Decisão | Escolha | Alternativa descartada | Motivo |
|---|---|---|---|
| Estratégia de arquivo | Reescrever/estender os `.yml` existentes em `v1.0.0/tables/` | Nova pasta `v2.0.0/` com migrations incrementais | Sem produção — reescrita limpa é mais simples que compatibilizar incrementalmente |
| `CHECK` de vocabulário fechado | `addNotNullConstraint`/SQL direto (`sql:` cujo texto é o `ALTER TABLE ... ADD CONSTRAINT ... CHECK`) já pronto no domain model | `ENUM` nativo do Postgres | Domain model já decidiu por `VARCHAR` + `CHECK` explicitamente — não reabrir essa decisão aqui |
| Functions/triggers | Uma function por entidade (ex: `fn_before_insert_company_status_history`, `fn_before_insert_employee_status_history`, `fn_before_insert_login_status_history` — não uma função genérica com SQL dinâmico) | Function genérica parametrizada por nome de tabela via `EXECUTE format(...)` | Decisão já tomada no domain model: mais verboso, porém mais simples de depurar e sem risco de erro de tipo em runtime |
| `fn_block_delete` | Uma única function compartilhada entre as 8 tabelas que bloqueiam DELETE, usando `TG_TABLE_NAME` na mensagem | Uma function por tabela | Já é o padrão especificado no domain model — função não depende de coluna específica |
| Ordem de criação das tabelas | Respeitar dependência de FK explicitamente em `tables.yml` (catálogos/motivos antes das tabelas que os referenciam; `SCOS_LEGAL_NATURE`/`SCOS_CNAE` antes de `SCOS_COMPANY`; `SCOS_ADDRESS_TYPE`/`SCOS_CONTACT_TYPE` antes de `*_ADDRESS`/`*_CONTACT`; `SCOS_OUTBOX_TOPIC` antes de `SCOS_OUTBOX_EVENT`) | FKs deferrable + qualquer ordem | Consistente com a decisão já registrada na rodada anterior — migrations simples, sem truques |
| Capability boundaries (specs) | 7 capabilities: 4 novas (`schema-outbox`, `schema-status-history`, `schema-fiscal-data`, `schema-type-catalog`) + 3 modificadas (`schema-organization`, `schema-access`, remoção de `schema-integration-keycloak`) | 1 capability única "schema-tudo" ou 1 capability por tabela | Agrupamento por conceito de domínio coeso (mesmo critério já usado nas capabilities existentes do projeto) — nem monolítico demais, nem granularidade artificial por tabela |
| Views (`vw_login_context`, `vw_authority_response`) | Revisão pontual dentro desta change, sem spec própria (nenhuma capability existente cobre views; tratado como consequência de implementação) | Criar capability `schema-views` | Nenhum precedente no projeto trata views como capability spec-worthy; overhead desnecessário para um ajuste mecânico de coluna renomeada |

## Risks / Trade-offs

- **[Risco]** Checksum do Liquibase quebra se o banco de dev não for recriado do zero → **[Mitigação]** documentar em tasks.md a necessidade de `dropAll`/banco novo antes de aplicar; não é um cenário de rollback incremental
- **[Risco]** Ordem de criação incorreta causa falha de FK em cascata (ex: `SCOS_COMPANY` referenciando `SCOS_LEGAL_NATURE` antes dela existir) → **[Mitigação]** `tables.yml` final construído explicitamente na ordem de dependência, validado por `liquibase update` completo antes de considerar a change pronta
- **[Risco]** Views quebram silenciosamente com colunas renomeadas (`KEYCLOAK_ID`→`EXTERNAL_ID`, `TYPE` texto→FK) e isso só aparece em tempo de query, não em `liquibase update` → **[Mitigação]** tarefa explícita de grep nas `.sql` de view por essas colunas antes de fechar a change
- **[Trade-off]** 4 tabelas de motivo (`REASON_ACTIVATE/INACTIVATE/DISABLE/ENABLE`) duplicam estrutura quase idêntica (só `CODE`/`DESCRIPTION`/`ENTITY_TYPE`) em vez de uma tabela genérica com `REASON_TYPE` — decisão já tomada no domain model, não revisitada aqui; aceito porque simplifica o `CHECK` de transição em `*_STATUS_HISTORY` (FK direta por coluna em vez de FK genérica + validação de tipo)

## Migration Plan

1. Criar tabelas de catálogo/referência primeiro (sem dependências novas): `SCOS_ADDRESS_TYPE`, `SCOS_CONTACT_TYPE`, `SCOS_LEGAL_NATURE`, `SCOS_CNAE`, `SCOS_OUTBOX_TOPIC`, as 4 `SCOS_REASON_*`
2. Alterar tabelas existentes (`SCOS_POSITION`, `SCOS_EMPLOYEE`, `SCOS_COMPANY`, `SCOS_LOGIN`, `*_ADDRESS`, `*_CONTACT`) — novas colunas e troca de `TYPE` texto por FK
3. Criar tabelas dependentes de 1º grau: `SCOS_COMPANY_CNAE_SECONDARY`, `SCOS_POSITION_WORK_SCHEDULE`, `SCOS_EMPLOYEE_WORK_SCHEDULE`, `SCOS_REASON_POSITION_CHANGE` → `SCOS_EMPLOYEE_POSITION_HISTORY`, `SCOS_LOGIN_PROFILE`, `SCOS_OUTBOX_EVENT` → `SCOS_OUTBOX_EVENT_LOG`/`SCOS_OUTBOX_EVENT_DEAD_LETTER`, as 3 `*_STATUS_HISTORY`
4. Remover `scos_integration_keycloak.yml`, `scos_integration_keycloak_log.yml`, `scos_integration_message_invalid.yml` e suas referências em `tables.yml`
5. Adicionar `CHECK` constraints (via changeSets `sql:` separados, após todas as tabelas existirem)
6. Adicionar functions e triggers (na ordem: funções primeiro, depois os `CREATE TRIGGER` que as referenciam)
7. Adicionar índices novos em `indexes.yml`
8. Revisar/ajustar views afetadas
9. Validar `liquibase update` completo em banco limpo e `liquibase rollback` de ponta a ponta

Rollback: cada changeSet carrega seu próprio `rollback:` (drop table/constraint/trigger/function), seguindo o padrão já usado no projeto — não há plano de rollback "big bang" separado.

## Open Questions

Nenhuma — decisões de escopo (change única) e de seed de dados fiscais (schema vazio) já foram confirmadas com o usuário durante a exploração.
