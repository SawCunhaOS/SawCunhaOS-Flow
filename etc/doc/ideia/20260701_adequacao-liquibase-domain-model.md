# Adequação do Liquibase ao Domain Model (v2 — outbox, histórico, fiscal, catálogos)

**Data**: 2026-07-01
**Status**: 🔄 Em Análise
**Tipo**: 🗄️ Banco de Dados

---

## ⚠️ Princípio SRP — Uma Funcionalidade por Ideia

> **Regra**: Uma ideia = uma funcionalidade. Features independentes → arquivos separados.

- **Nome da funcionalidade**: `adequacao-liquibase-domain-model-v2`
- **Resumo em uma frase**: Reescrever/estender as migrations Liquibase para que o schema físico volte a corresponder exatamente ao `domain_model.md`, que cresceu de 16 para ~34 tabelas desde a última sincronização (2026-06-08).

**Checklist SRP**:
- [x] Esta ideia cobre exatamente uma funcionalidade — sincronizar schema físico ao domain model
- [x] Não mistura features independentes no mesmo arquivo — **decidido com o usuário**: mesmo cobrindo 6 áreas de domínio (organização, histórico de cargo, outbox, permissões, histórico de status, fiscal/catálogo), tudo é consequência de uma única funcionalidade ("sincronizar liquibase ao domain model"), mesmo precedente da rodada anterior. Uma change só.
- [x] O nome é específico

---

## 1️⃣ Visão

### Problema
`etc/database/domain_model.md` foi atualizado localmente (ainda não commitado — `git diff` mostra +1153/-105 linhas) desde a última rodada de sincronização do Liquibase (change arquivada `2026-06-08-adequacao-liquibase-domain-model`, que levou o schema de um modelo antigo para as 16-17 tabelas então vigentes). O domain model agora especifica:

- **18 tabelas novas** não representadas no Liquibase atual
- **3 tabelas removidas** do domain model mas ainda presentes no Liquibase (`SCOS_INTEGRATION_KEYCLOAK`, `SCOS_INTEGRATION_KEYCLOAK_LOG`, `SCOS_INTEGRATION_MESSAGE_INVALID` — substituídas pelo padrão outbox genérico)
- **Colunas novas/renomeadas** em tabelas existentes (ex: `SCOS_LOGIN.KEYCLOAK_ID` → `EXTERNAL_ID`; `SCOS_EMPLOYEE_CONTACT.TYPE` texto livre → `CONTACT_TYPE_ID` FK)
- **~20 `CHECK` constraints** de vocabulário fechado especificados com SQL pronto, nenhum implementado hoje
- **~15 triggers** novas (sync de cache STATUS/POSITION_ID, bloqueio de DELETE em 8 tabelas, validação de ENTITY_TYPE cruzado), todas com SQL completo no domain model
- **~20 índices** de performance novos, vários parciais

O Liquibase hoje só implementa uma fração do domain model atual — qualquer `liquibase update` em banco limpo produz um schema defasado.

### Objetivo
`liquibase update` em banco limpo produz exatamente o schema descrito em `domain_model.md`: todas as tabelas, colunas, tipos, nullability, UKs/FKs, `CHECK`s, triggers e índices. Escopo: **somente Liquibase** (`flow-organization-boot/src/main/resources/db/changelog/`) — mesma premissa da rodada anterior: sistema sem produção, reescrita limpa sem migração de dados.

### Fora de Escopo
- Entidades JPA / código Java — impacto secundário, ideia(s) separada(s)
- Alteração do domain model em si (já foi feita, é o insumo)
- Migração de dados
- Views (`vw_authority_response.sql`, `vw_login_context.sql`) — **precisam de revisão** por causa das colunas renomeadas em `SCOS_LOGIN`/`SCOS_EMPLOYEE_CONTACT`, mas se o ajuste for não-trivial pode virar tarefa própria dentro desta mesma ideia (é consequência direta, não feature nova)

---

## 2️⃣ Requisitos

### Funcionais — agrupados por área do domain model

#### A. Organização (extensão)
- [ ] Adicionar `IS_TRUST_POSITION` em `SCOS_POSITION`; ajustar `CODE` para UK composta com `DEPARTMENT_ID`
- [ ] Criar `SCOS_POSITION_WORK_SCHEDULE` (template de horário por cargo/dia) com `CHECK` de dia da semana
- [ ] Criar `SCOS_EMPLOYEE_WORK_SCHEDULE` (horário efetivo, cópia editável) com mesmo `CHECK`
- [ ] Adicionar `CONTRACT_TYPE`, `PROBATION_END_DATE`, `STATUS` em `SCOS_EMPLOYEE` (`STATUS` vira cache sincronizado por trigger — ver área E)
- [ ] Adicionar `LEGAL_NATURE_ID`, `CNAE_PRINCIPAL_ID`, `STATE_REGISTRATION`, `MUNICIPAL_REGISTRATION`, `STATUS` em `SCOS_COMPANY`

#### B. Histórico de cargo
- [ ] Criar `SCOS_REASON_POSITION_CHANGE` (motivos, sem `ENTITY_TYPE`, bloqueio de DELETE)
- [ ] Criar `SCOS_EMPLOYEE_POSITION_HISTORY` (linha do tempo imutável, índice único parcial `WHERE END_DATE IS NULL`)
- [ ] Trigger `TRG_CLOSE_PREVIOUS_POSITION` (fecha linha aberta antes do INSERT)
- [ ] Trigger `TRG_SYNC_EMPLOYEE_POSITION` (propaga `POSITION_ID` pra `SCOS_EMPLOYEE`)

#### C. Outbox e integrações externas (substitui Keycloak dedicado)
- [ ] Remover `scos_integration_keycloak.yml`, `scos_integration_keycloak_log.yml`, `scos_integration_message_invalid.yml`
- [ ] Criar `SCOS_OUTBOX_TOPIC` (roteamento, `CHECK` de `BACKEND`, bloqueio de DELETE)
- [ ] Criar `SCOS_OUTBOX_EVENT` (`CHECK` de `STATUS`, índice parcial `WHERE STATUS = 'PENDING'`)
- [ ] Criar `SCOS_OUTBOX_EVENT_LOG`
- [ ] Criar `SCOS_OUTBOX_EVENT_DEAD_LETTER`
- [ ] `SCOS_LOGIN.KEYCLOAK_ID` → renomear `EXTERNAL_ID`; adicionar `TYPE` (`CHECK`)

#### D. Acesso e permissões (extensão)
- [ ] Criar `SCOS_LOGIN_PROFILE` (perfis adicionais) + trigger `TRG_VALIDATE_LOGIN_PROFILE_NOT_PRIMARY`

#### E. Histórico de status e motivos (novo padrão de auditoria, 3 entidades)
- [ ] Criar `SCOS_REASON_ACTIVATE`, `SCOS_REASON_INACTIVATE`, `SCOS_REASON_DISABLE`, `SCOS_REASON_ENABLE` (todas com `ENTITY_TYPE`, `CHECK`, bloqueio de DELETE, índice parcial)
- [ ] Criar `SCOS_COMPANY_STATUS_HISTORY`, `SCOS_EMPLOYEE_STATUS_HISTORY`, `SCOS_LOGIN_STATUS_HISTORY` (imutáveis, `PREVIOUS_STATUS` derivado por trigger)
- [ ] Trigger `TRG_BEFORE_INSERT_*_STATUS_HISTORY` ×3 (deriva `PREVIOUS_STATUS`, valida `ENTITY_TYPE` do motivo)
- [ ] Trigger `TRG_SYNC_*_STATUS` ×3 (propaga `STATUS` pra tabela principal)
- [ ] `CHECK` de transição válida (tabela de transições no domain model, seção Triggers)

#### F. Dados fiscais da empresa
- [ ] Criar `SCOS_LEGAL_NATURE`, `SCOS_CNAE` **vazias** (só schema — sem `ACTIVE`/DELETE-block, código oficial IBGE). Seed de dados oficiais fica fora desta change, decisão explícita
- [ ] Criar `SCOS_COMPANY_CNAE_SECONDARY` (N:N, índice reverso)

#### G. Catálogo de tipos de endereço/contato (substitui texto livre)
- [ ] Criar `SCOS_ADDRESS_TYPE`, `SCOS_CONTACT_TYPE` (`ENTITY_TYPE`, `CHECK`, bloqueio de DELETE, índice parcial)
- [ ] `SCOS_EMPLOYEE_ADDRESS`/`SCOS_COMPANY_ADDRESS`: `TYPE` texto → `ADDRESS_TYPE_ID` FK; `GEOLOCATION` já era NOT NULL (mantém)
- [ ] `SCOS_EMPLOYEE_CONTACT`/`SCOS_COMPANY_CONTACT`: `TYPE` texto → `CONTACT_TYPE_ID` FK
- [ ] Trigger `TRG_VALIDATE_*_TYPE` ×4 (valida `ENTITY_TYPE` do tipo referenciado bate com a tabela)

#### H. Views (consequência das renomeações)
- [ ] Revisar `vw_login_context.sql` (referencia `KEYCLOAK_ID`?) e `vw_authority_response.sql` contra as colunas renomeadas

### Não-Funcionais
- [ ] **RNF-01**: Todo changeSet com `rollback` funcional
- [ ] **RNF-02**: IDs de changeSet no padrão `YYYYMMDD-Samuel.Cunha-NNN`
- [ ] **RNF-03**: Nomes de índice/constraint no padrão existente (`PK_*`, `FK_*`, `UK_*`, `IDX_*`, `CHK_*`, `TRG_*`)
- [ ] **RNF-04**: `liquibase update` em banco limpo sem erros; `rollback` desfaz sem erros
- [ ] **RNF-05**: Triggers/functions seguem o padrão já em uso (`sqlFile` + `.sql` versionado ao lado do YAML, `splitStatements: false`), como em `triggers/` e `function/` hoje

---

## 3️⃣ Arquitetura

### Componentes Afetados
```
flow-organization-boot/src/main/resources/db/changelog/
├── v1.0.0/tables/            18 novos arquivos .yml + ~10 modificados + 3 removidos + tables.yml reordenado
├── v1.0.0/indexes/           extensão de indexes.yml (~20 índices novos)
├── function/                 ~10 novas .sql (fn_before_insert_*, fn_sync_*, fn_block_delete, fn_validate_*)
├── triggers/                 ~15 novos pares .sql + entrada em triggers.yml
└── view/                     possível ajuste em vw_login_context.sql / vw_authority_response.sql
```

### Decisões Técnicas
| Decisão | Escolha | Alternativa Descartada | Motivo |
|---------|---------|------------------------|--------|
| Estratégia | Reescrita/extensão dos arquivos v1.0.0 existentes | Nova pasta v2.0.0 | Sistema sem produção — sem necessidade de versionar migração incremental |
| CHECK constraints | `addNotNullConstraint`/SQL direto via changeSet, seguindo o SQL pronto do domain model | Enum type no Postgres | Domain model já decidiu por `VARCHAR` + `CHECK` explicitamente |
| Functions/triggers | 1 function por entidade (não genérica com SQL dinâmico) | Function genérica parametrizada | Já é decisão tomada no domain model — mais fácil de depurar |
| Escopo | Somente Liquibase | Incluir JPA/views não-triviais | Mesma premissa da rodada anterior |

### Banco de Dados
- **Impacto**: ✅ Sim — o maior até agora
- **Tabelas criadas**: 18
- **Tabelas removidas**: 3 (integration_keycloak, integration_keycloak_log, integration_message_invalid)
- **Tabelas modificadas**: ~10
- **Banco recriado do zero**: sem migração de dados

---

## 4️⃣ Implementação

*Detalhamento de arquivos/tarefas fica para a fase de `/openspec-propose`, após decisão sobre split (ver Riscos abaixo).*

### Riscos e Edge Cases
1. Liquibase valida checksum — se o banco de dev não for recriado do zero, precisa `liquibase clearCheckSums` ou `dropAll` antes de aplicar
2. Ordem de criação das tabelas precisa respeitar FKs novas (ex: `SCOS_COMPANY` agora referencia `SCOS_LEGAL_NATURE`/`SCOS_CNAE`, que precisam existir antes)
3. Views existentes podem quebrar silenciosamente com as colunas renomeadas — checar antes de dar como concluído
4. `SCOS_LEGAL_NATURE`/`SCOS_CNAE` sobem vazias nesta change — seed de dados oficiais IBGE é tarefa/change separada

---

## 📎 Referências
- Domain model: `etc/database/domain_model.md` (não commitado — `git diff` local)
- Migrations atuais: `flow-organization-boot/src/main/resources/db/changelog/`
- Rodada anterior (arquivada): `openspec/changes/archive/2026-06-08-adequacao-liquibase-domain-model/`
