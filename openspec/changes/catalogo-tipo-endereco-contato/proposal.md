## Why

`CompanyAddress`/`EmployeeAddress`/`CompanyContact`/`EmployeeContact` ainda expõem `type: string` livre no contrato público, mas o schema v2 já substituiu isso por FK para catálogos dinâmicos `SCOS_ADDRESS_TYPE`/`SCOS_CONTACT_TYPE` (compartilhados entre `COMPANY` e `EMPLOYEE`, com `DELETE` bloqueado por trigger). O contrato está desalinhado do banco físico já existente.

## What Changes

**Escopo desta rodada: somente o contrato OpenAPI (YAML).** Implementação de código (permissões, use cases, delegates, testes) fica para uma change futura.

- Criar CRUD de referência `AddressType` (`code`, `description`, `entityType`, `active`) — sem `DELETE` (trigger do banco bloqueia fisicamente); só `enable`/`disable`
- Criar CRUD de referência `ContactType` — mesmo padrão de `AddressType`
- **BREAKING**: `CompanyAddress`/`EmployeeAddress` (e `Create`/`Update*AddressRequest`) trocam `type: string` por `addressTypeId: integer (format: int64)`, obrigatório
- **BREAKING**: `CompanyContact`/`EmployeeContact` (e `Create`/`Update*ContactRequest`) trocam `type: string` por `contactTypeId: integer (format: int64)`, obrigatório
- Corrigir `EmployeeAddress.number` para `integer (format: int32)` (estava mais largo que o `INT` do banco)
- `x-authorize` novo no contrato: `GET/CREATE/UPDATE_ADDRESS_TYPE`, `ENABLE/DISABLE_ADDRESS_TYPE`, mesmo conjunto para `CONTACT_TYPE` (entradas correspondentes em `ScosOrganizationPermission` ficam para a change de implementação)

## Capabilities

### New Capabilities
- `address-type-catalog`: CRUD de referência `AddressType` (`code`/`description`/`entityType`/`active`, sem `DELETE`) e migração de `CompanyAddress`/`EmployeeAddress` de `type: string` para `addressTypeId` (FK), incluindo a correção de tipo de `EmployeeAddress.number`
- `contact-type-catalog`: CRUD de referência `ContactType` (mesmo padrão) e migração de `CompanyContact`/`EmployeeContact` de `type: string` para `contactTypeId` (FK)

### Modified Capabilities
(nenhuma — as specs existentes `company-contact-schema`/`model-company`/`model-employee` cobrem apenas estrutura JPA (PK composta, `auditRead`, nomes de coluna); a migração `type` → FK no contrato OpenAPI é comportamento novo, não coberto por elas)

## Impact

- `etc/api/organization/ScosOrganization_Company.yml` — `CompanyAddress`/`CompanyContact` (mod); schemas/paths novos de `AddressType`/`ContactType` (local exato definido em `design.md`)
- `etc/api/organization/ScosOrganization_Employee.yml` — `EmployeeAddress`/`EmployeeContact` (mod)
- `ScosOrganizationPermission` (código) — fora de escopo desta change; entradas de `x-authorize` ficam pendentes para a implementação futura
- Breaking change de contrato público, aceito diretamente (sistema sem produção)
- Sem impacto de banco — schema v2 já existe (`adequacao-liquibase-domain-model-v2`, completo)
