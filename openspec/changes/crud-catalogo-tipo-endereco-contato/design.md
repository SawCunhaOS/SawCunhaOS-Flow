## Context

O contrato OpenAPI (`etc/api/organization/ScosOrganization_Catalog.yml`) já define os 12 endpoints de `AddressType`/`ContactType` — schemas, paths, `x-authorize` — fechado pelo change `catalogo-tipo-endereco-contato`. O que falta é só código:

```
scos-organization-domain/.../corporate/catalog/internal/AddressType.java          (entity, existe)
scos-organization-domain/.../corporate/catalog/internal/AddressTypeRepository.java (vazio, sem existsByCode)
scos-organization-domain/.../corporate/catalog/internal/ContactType.java          (entity, existe)
scos-organization-domain/.../corporate/catalog/internal/ContactTypeRepository.java (vazio, sem existsByCode)
```
Nenhum `dto/`, `service/`, `specification/`, Use Case, `Delegate`, ou entrada em `ScosOrganizationPermission`/`ExceptionCodeError` existe pra essas duas entidades.

Referência de padrão mais recente: change `crud-cargo-position`, que implementou exatamente esse tipo de CRUD de referência simples pro `Position`, replicando a estrutura já usada por `DepartmentService`:
```
domain/<ctx>/<agregado>/
  dto/{X}Input, {X}Output
  internal/{X}Repository        → existsByCode / existsByCodeAndNotId (QueryDSL default methods)
  service/{X}Mapper, {X}ServiceBean
  specification/{X}Service
usecase/application/usecase/<ctx>/<agregado>/
  Create/Update/Find/FindAll/Enable/Disable UseCase + Bean + ApiMapper
api/delegate/<agregado>/{X}Delegate implements {X}ApiDelegate
```
`AddressType`/`ContactType` são mais simples que `Position`: sem relação com outro agregado (Position valida `departmentId` contra `Department`), sem regra de bloqueio de `disable` por vínculo ativo (doc 05 diz explicitamente que inativar um tipo em uso não quebra vínculos existentes, só impede novos cadastros).

O padrão de erro (`code`/`httpCode`/`title`) já está pronto — implementado pela change `padronizacao-http-status-exception-code`: `ExceptionCodeError` tem construtor de 3 argumentos, e as 7 categorias de `title` (`SCOS_TITLE_NOT_FOUND`, `SCOS_TITLE_CONFLICT`, `SCOS_TITLE_BUSINESS_RULE_VIOLATION`, etc.) já existem — esta change só adiciona constantes que reaproveitam essas categorias, sem criar nenhuma nova.

## Goals / Non-Goals

**Goals:**
- Os 12 endpoints do contrato passam a funcionar de ponta a ponta (hoje nenhum deles tem código)
- Mesmo padrão de camadas do `DepartmentService`/`PositionService` — sem inventar estrutura nova
- `httpCode`/`title` corretos desde o primeiro commit (404/409/422), seguindo o padrão já implementado

**Non-Goals:**
- Migrar os Use Cases de `CompanyAddress`/`EmployeeAddress`/`CompanyContact`/`EmployeeContact` para validar o `addressTypeId`/`contactTypeId` referenciado (já são FK no JPA; se esses Use Cases resolvem a referência corretamente é outra frente)
- Catálogo `Reason*` — ideia/change irmã separada (`crud-catalogo-motivos-transicao-status`)
- Testes de integração com Testcontainers — não necessário nesta etapa
- Qualquer mudança no contrato OpenAPI — já fechado

## Decisions

### 1. Pacote compartilhado `corporate/catalog/`, não um por entidade
`AddressType`/`ContactType` já vivem juntas em `corporate/catalog/internal/` hoje. Os novos `dto/`, `service/`, `specification/` seguem a mesma granularidade (pacote único, classes por entidade), não o padrão `department/`/`position/` (um pacote por agregado). Alternativa descartada: separar em `corporate/catalog/addresstype/` e `corporate/catalog/contacttype/` — rejeitada porque criaria um split artificial de um par que sempre muda junto (mesmo par que fechou o contrato como uma coisa só).

### 2. Unicidade de `code` por catálogo, não cross-catálogo
`SCOS_ADDRESS_TYPE` e `SCOS_CONTACT_TYPE` são tabelas independentes — `existsByCode`/`existsByCodeAndNotId` validam dentro de cada catálogo. Um `code` igual em `AddressType` e `ContactType` é permitido.

### 3. Sem regra de bloqueio de disable por vínculo ativo
Ao contrário de `Department`/`Position` (que bloqueiam `disable` se há `Position`/`Employee` ativo vinculado, `SCOS_*_003`), `AddressType`/`ContactType` não têm essa regra — a doc 05 é explícita: inativar um tipo em uso não remove vínculos, só impede novos cadastros. Por isso o mapeamento de erro tem só 4 códigos por entidade (não encontrado, duplicado, já ativo, já inativo), não 5.

### 4. `entityType` no filtro de listagem é opcional
Mesmo padrão do `active` em `GET /v1/departments` — `entityType` já é opcional no contrato (`paginationFilter` obrigatório cobre paginação; `entityType` é um filtro adicional, não obrigatório).

### 5. Reaproveitar categorias de `title` existentes, não criar novas
`SCOS_ADDRESS_TYPE_001`/`SCOS_CONTACT_TYPE_001` (404) usam `SCOS_TITLE_NOT_FOUND`; `_002` (409) usa `SCOS_TITLE_CONFLICT`; `_003`/`_004` (422) usam `SCOS_TITLE_BUSINESS_RULE_VIOLATION` — as mesmas 7 chaves já implementadas pela change `padronizacao-http-status-exception-code`. Nenhuma chave `SCOS_TITLE_*` nova é necessária.

### 6. Mapeamento completo de erro

| Código | HTTP | Title | Base |
|---|---|---|---|
| `SCOS_ADDRESS_TYPE_001` | 404 | `SCOS_TITLE_NOT_FOUND` | `orElseThrow` — não encontrado |
| `SCOS_ADDRESS_TYPE_002` | 409 | `SCOS_TITLE_CONFLICT` | `existsByCode` — código duplicado |
| `SCOS_ADDRESS_TYPE_003` | 422 | `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | já ativo |
| `SCOS_ADDRESS_TYPE_004` | 422 | `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | já inativo |
| `SCOS_CONTACT_TYPE_001` | 404 | `SCOS_TITLE_NOT_FOUND` | `orElseThrow` — não encontrado |
| `SCOS_CONTACT_TYPE_002` | 409 | `SCOS_TITLE_CONFLICT` | `existsByCode` — código duplicado |
| `SCOS_CONTACT_TYPE_003` | 422 | `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | já ativo |
| `SCOS_CONTACT_TYPE_004` | 422 | `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | já inativo |

## Risks / Trade-offs

- **[Risco] Ordem entre changes** — esta change assume que `padronizacao-http-status-exception-code` já está implementada (está — 14/14 tasks completas). Se `ExceptionCodeError` regredisse pro construtor de 1 argumento, as 8 novas constantes quebrariam a build. → **Mitigação**: nenhuma ação necessária, já satisfeito.
- **[Trade-off] Pacote compartilhado** — manter `AddressType`/`ContactType` no mesmo pacote (`corporate/catalog/`) em vez de um por agregado reduz a aderência ao padrão Department/Position, ao custo de manter a granularidade já usada por `internal/` hoje. Reversível: pode-se separar depois sem quebrar contrato público.
- **[Risco] `entityType` do enum de domínio tem 3 valores, contrato de `AddressType`/`ContactType` só aceita 2** — o enum de domínio `EntityType` (compartilhado com `Reason*`/`StatusHistory`) tem `COMPANY`/`EMPLOYEE`/`LOGIN`, mas o schema OpenAPI de `AddressType`/`ContactType` só aceita `COMPANY`/`EMPLOYEE` (`chk_address_type_entity_type` no banco). → **Mitigação**: o `ApiMapper` de cada Use Case só precisa mapear os 2 valores que o contrato aceita; o valor `LOGIN` nunca chega ali porque o schema OpenAPI já rejeita antes.

## Migration Plan

Sem impacto de banco — tabelas já existem. Implementação aditiva (novo código, sem alterar contrato). Rollback trivial: reverter o commit remove os 12 endpoints, sem efeito em dado existente (tabelas ficam vazias/inalteradas).

## Open Questions

Nenhuma — decisões de pacote, unicidade e ausência de regra de bloqueio já confirmadas durante a exploração (ver idea file `20260705_crud-catalogo-tipo-endereco-contato.md`).
