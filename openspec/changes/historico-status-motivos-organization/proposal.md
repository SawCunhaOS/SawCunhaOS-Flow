## Why

O schema v2 já redesenhou `Company`/`Employee`/`Login` para tratar `STATUS` como cache sincronizado por trigger a partir de tabelas de histórico (`*_STATUS_HISTORY`), com 4 transições nomeadas (`activate`/`inactivate`/`disable`/`enable`) e motivo obrigatório vindo de 4 catálogos de referência (`REASON_ACTIVATE/INACTIVATE/DISABLE/ENABLE`). A camada de domínio JPA já foi sincronizada com isso (`atualizacao-entidades-jpa-liquibase-v2`, completo), mas o contrato público OpenAPI ainda está no modelo v1: rotas de transição incompletas, sem `reasonId`, sem histórico consultável, e um endpoint (`deleteLogin`) que viola o vocabulário fechado atual do banco.

## What Changes

**Escopo desta rodada: somente o contrato OpenAPI (YAML).** Implementação de código (permissões, use cases, delegates, testes, reatribuição de perfis) fica para uma change futura.

- Adicionar `requestBody { reasonId (required), observation (optional) }` às rotas já existentes `/companies/{id}/enable`, `/disable`, `/employees/{id}/enable`, `/disable`
- **BREAKING**: Renomear `DELETE /companies/{id}` (UC-008) → `PUT /companies/{id}/block` e `DELETE /employees/{id}` (UC-043) → `PUT /employees/{id}/block` (já eram soft-delete para `DISABLED`, só precisavam do verbo/rota corretos); `x-authorize` migra `DELETE_COMPANY`/`DELETE_EMPLOYEE` → `BLOCK_COMPANY`/`BLOCK_EMPLOYEE`
- Criar `PUT /companies/{id}/unblock`, `PUT /employees/{id}/unblock` (novos — não existiam nem como delete)
- Criar `PUT /logins/{id}/enable`, `PUT /logins/{id}/disable` (novos); adicionar `requestBody` às rotas existentes `/logins/{id}/block`, `/unblock`
- **BREAKING**: Remover `DELETE /logins/{id}` (`deleteLogin`, UC-066) do contrato — `LOGIN.STATUS` não tem mais `DELETED` no vocabulário fechado
- Criar CRUD de referência para os catálogos `ReasonActivate`, `ReasonInactivate`, `ReasonDisable`, `ReasonEnable`
- Criar `GET /companies/{id}/status-history`, `GET /employees/{id}/status-history`, `GET /logins/{id}/status-history` (paginado, somente leitura)
- Adicionar `reasonActivateId` (obrigatório) a `CreateCompanyRequest`, `CreateEmployeeRequest`, `CreateLoginRequest`, `CreateEmployeeLoginRequest` — a trigger de histórico exige motivo mesmo na primeira linha (criação)
- `x-authorize` novo no contrato: `BLOCK_COMPANY`, `UNBLOCK_COMPANY`, `BLOCK_EMPLOYEE`, `UNBLOCK_EMPLOYEE`, `ENABLE_LOGIN`, `DISABLE_LOGIN` (granulares, não reaproveitam `UPDATE_LOGIN_STATUS`; entradas correspondentes em `ScosOrganizationPermission` ficam para a change de implementação)

## Capabilities

> Nota: `openspec/specs/model-company/`, `model-employee/`, `model-login/` (baseline existente) cobrem a camada JPA/entidade (mapeamento de colunas, enums de domínio), não o contrato OpenAPI — nenhum requisito deles muda aqui. Esta change é 100% contrato (`etc/api/organization/*.yml`), então as 3 capabilities abaixo são todas novas.

### New Capabilities
- `status-transition-contract`: as 4 rotas de transição de status simétricas (`enable`/`disable`/`block`/`unblock`) com `reasonId` obrigatório, aplicadas a `Company`/`Employee`/`Login`; `reasonActivateId` obrigatório nos 4 endpoints de criação; remoção de `deleteLogin`
- `reason-status-catalog`: CRUD de referência para os 4 catálogos de motivo (`ReasonActivate`, `ReasonInactivate`, `ReasonDisable`, `ReasonEnable`), mesmo schema shape (`code`/`description`/`entityType`/`active`), filtráveis por `entityType`
- `status-history-query`: consulta paginada e somente-leitura ao histórico de status auditável dos 3 agregados (`Company`/`Employee`/`Login`), populado exclusivamente pelas próprias transições

### Modified Capabilities
Nenhuma — sem capability existente em `openspec/specs/` cobrindo o contrato OpenAPI destes endpoints.

## Impact

- `etc/api/organization/ScosOrganization_Company.yml`, `ScosOrganization_Employee.yml`, `ScosOrganization_Login.yml` — rotas de transição de status modificadas/criadas, schemas de criação modificados, novo endpoint de histórico
- Novo arquivo dedicado para os 4 catálogos `Reason*` (decisão de localização em `design.md`)
- `ScosOrganizationPermission` (código) — fora de escopo desta change; migração de `x-authorize` (`DELETE_COMPANY`/`DELETE_EMPLOYEE` → `BLOCK_COMPANY`/`BLOCK_EMPLOYEE`, reatribuição de perfis) fica para a implementação futura
- Qualquer client (mesmo interno/dev) que chame `DELETE /companies/{id}`, `DELETE /employees/{id}` ou `DELETE /logins/{id}` quebra — sem período de transição, sistema sem produção
- Sem impacto de banco — schema v2 já existe e já suporta isso; só o contrato OpenAPI muda
