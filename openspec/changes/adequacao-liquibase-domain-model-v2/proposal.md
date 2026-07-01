## Why

`etc/database/domain_model.md` foi atualizado (outbox pattern, histórico de status auditável, dados fiscais, catálogo dinâmico de tipos de endereço/contato, histórico de cargo, horários de trabalho) e cresceu de ~16 para ~34 tabelas desde a última sincronização do Liquibase. O schema físico atual reflete apenas o domain model antigo — qualquer `liquibase update` em banco limpo hoje produz um schema defasado, faltando tabelas inteiras e com 3 tabelas obsoletas (`SCOS_INTEGRATION_KEYCLOAK*`) ainda presentes.

## What Changes

- Criar 18 tabelas novas: `SCOS_POSITION_WORK_SCHEDULE`, `SCOS_EMPLOYEE_WORK_SCHEDULE`, `SCOS_REASON_POSITION_CHANGE`, `SCOS_EMPLOYEE_POSITION_HISTORY`, `SCOS_OUTBOX_TOPIC`, `SCOS_OUTBOX_EVENT`, `SCOS_OUTBOX_EVENT_LOG`, `SCOS_OUTBOX_EVENT_DEAD_LETTER`, `SCOS_REASON_ACTIVATE`, `SCOS_REASON_INACTIVATE`, `SCOS_REASON_DISABLE`, `SCOS_REASON_ENABLE`, `SCOS_COMPANY_STATUS_HISTORY`, `SCOS_EMPLOYEE_STATUS_HISTORY`, `SCOS_LOGIN_STATUS_HISTORY`, `SCOS_LEGAL_NATURE`, `SCOS_CNAE`, `SCOS_COMPANY_CNAE_SECONDARY`, `SCOS_ADDRESS_TYPE`, `SCOS_CONTACT_TYPE`, `SCOS_LOGIN_PROFILE` (21 no total, contando as agrupadas acima)
- **BREAKING**: Remover `SCOS_INTEGRATION_KEYCLOAK`, `SCOS_INTEGRATION_KEYCLOAK_LOG`, `SCOS_INTEGRATION_MESSAGE_INVALID` — substituídas pelo padrão outbox genérico
- **BREAKING**: Renomear `SCOS_LOGIN.KEYCLOAK_ID` → `EXTERNAL_ID`
- **BREAKING**: Trocar `TYPE` (texto livre) por `ADDRESS_TYPE_ID`/`CONTACT_TYPE_ID` (FK) em `SCOS_EMPLOYEE_ADDRESS`, `SCOS_COMPANY_ADDRESS`, `SCOS_EMPLOYEE_CONTACT`, `SCOS_COMPANY_CONTACT`
- Adicionar colunas: `SCOS_POSITION.IS_TRUST_POSITION`; `SCOS_EMPLOYEE.CONTRACT_TYPE`/`PROBATION_END_DATE`; `SCOS_COMPANY.LEGAL_NATURE_ID`/`CNAE_PRINCIPAL_ID`/`STATE_REGISTRATION`/`MUNICIPAL_REGISTRATION`; `SCOS_LOGIN.TYPE`
- Adicionar ~20 `CHECK` constraints de vocabulário fechado (status, tipos, backend, dia da semana) — nenhum existe hoje
- Adicionar ~15 triggers: sync de cache (`STATUS`, `POSITION_ID`), bloqueio de `DELETE` em 8 tabelas de referência, validação cruzada de `ENTITY_TYPE`, derivação de `PREVIOUS_STATUS`
- Adicionar ~20 índices de performance, vários parciais
- Revisar `vw_login_context.sql`/`vw_authority_response.sql` contra colunas renomeadas/removidas
- Escopo estritamente Liquibase (`scos-organization-boot/src/main/resources/db/changelog/`) — sem JPA, sem migração de dados (sistema sem produção, banco recriado do zero)

## Capabilities

### New Capabilities
- `schema-outbox`: tabelas do padrão outbox genérico (`SCOS_OUTBOX_TOPIC/EVENT/EVENT_LOG/EVENT_DEAD_LETTER`) para entrega assíncrona confiável a brokers ou APIs externas, com retry e rastreabilidade
- `schema-status-history`: histórico auditável de transição de status (`ACTIVE`/`INACTIVE`/`DISABLED`|`BLOCKED`) para `SCOS_COMPANY`, `SCOS_EMPLOYEE` e `SCOS_LOGIN`, com motivos pré-cadastrados por tipo de transição e triggers de derivação/sincronização
- `schema-fiscal-data`: tabelas de referência fiscal da empresa (`SCOS_LEGAL_NATURE`, `SCOS_CNAE`, `SCOS_COMPANY_CNAE_SECONDARY`)
- `schema-type-catalog`: catálogo dinâmico de tipos de endereço e contato (`SCOS_ADDRESS_TYPE`, `SCOS_CONTACT_TYPE`), substituindo texto livre por FK validada

### Modified Capabilities
- `schema-organization`: adiciona `SCOS_POSITION_WORK_SCHEDULE`/`SCOS_EMPLOYEE_WORK_SCHEDULE` (horário de trabalho template/efetivo), `SCOS_REASON_POSITION_CHANGE`/`SCOS_EMPLOYEE_POSITION_HISTORY` (histórico de cargo), novas colunas em `SCOS_POSITION`/`SCOS_EMPLOYEE`/`SCOS_COMPANY`, e troca `TYPE` texto por FK de catálogo em `*_ADDRESS`/`*_CONTACT`
- `schema-access`: renomeia `SCOS_LOGIN.KEYCLOAK_ID` → `EXTERNAL_ID`, adiciona `SCOS_LOGIN_PROFILE` (perfis adicionais de um login)
- `schema-integration-keycloak`: capability inteira removida — as 3 tabelas que definia deixam de existir, substituídas por `schema-outbox`

## Impact

- `scos-organization-boot/src/main/resources/db/changelog/v1.0.0/tables/`: ~18 arquivos novos, ~10 modificados, 3 removidos, `tables.yml` reordenado por dependência de FK
- `scos-organization-boot/src/main/resources/db/changelog/v1.0.0/indexes/`: extensão com ~20 índices novos
- `scos-organization-boot/src/main/resources/db/changelog/function/` e `triggers/`: ~10 novas functions e ~15 novas triggers (arquivos `.sql` + entrada YAML)
- `scos-organization-boot/src/main/resources/db/changelog/view/`: possível ajuste em `vw_login_context.sql`/`vw_authority_response.sql`
- Nenhum código Java, DTO ou entidade JPA é alterado nesta change
- Sem impacto em produção — sistema ainda não implantado, banco recriado do zero
