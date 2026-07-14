## Why

O agregado `Company` (entidade, `Cnpj` value object, transições de status) e o `CompanyRepository` já existem, mas não há CRUD cadastral: faltam o domain service, os use cases de criação/atualização/consulta e o `CompanyDelegate`. Os endpoints UC-001..005 (`/v1/companies`) não respondem, bloqueando o cadastro de empresas matriz e filiais — pré-requisito para funcionários, contatos, endereços e todo o restante do domínio organizacional.

## What Changes

- **POST `/v1/companies`** — cria empresa matriz (`parentCompanyId` nulo) ou filial → `201` (UC-001/002), inserindo a 1ª linha em `SCOS_COMPANY_STATUS_HISTORY` com `newStatus=ACTIVE`.
- **PUT `/v1/companies/{id}`** — atualiza dados cadastrais; `parentCompanyId` não editável → `204` (UC-005).
- **GET `/v1/companies/{id}`** — retorna `Company` completo → `200`; `404 SCOS_COMPANY_001` se inexistente (UC-003).
- **GET `/v1/companies`** — listagem paginada com filtro por status e/ou nome → `200` com array-resumo `Companies` + `paginatedDTO` (UC-004).
- Validações distribuídas por camada: contrato (formato/presença/DV do CNPJ Alfa), domain service (unicidade, FKs, compatibilidade de motivo, hierarquia), agregado (guardas de estado).
- Idempotência apenas no POST via `@JdempotentRequestPayload` sobre `taxIdentifier`.

Sem mudança de banco (tabelas `SCOS_COMPANY` e `SCOS_COMPANY_STATUS_HISTORY` já existem). Sem breaking changes.

## Capabilities

### New Capabilities
- `company-api`: CRUD cadastral da Empresa (criação, atualização, consulta por id e listagem paginada) — endpoints UC-001..005 de `/v1/companies`, com as regras de negócio de unicidade de CNPJ, integridade de FKs, compatibilidade de motivo de ativação e limite de profundidade de hierarquia. Espelha o padrão de `cnae-api`/`legal-nature-api`.

### Modified Capabilities
<!-- Nenhuma. O agregado/entidade Company (model-company) e seus repositórios já existem; esta mudança adiciona a camada de API/use case sem alterar requisitos do modelo de domínio. -->

## Impact

- **API (`scos-organization-api`)**: novo `CompanyDelegate implements CompanyApiDelegate`.
- **Use case (`scos-organization-usecase`)**: `Create/Update/Find/FindAll CompanyUseCase(+Bean)`, `CompanyApiMapper`.
- **Domínio (`scos-organization-domain`)**: `CompanyService` + `CompanyServiceBean` + `CompanyMapper`, DTOs `CompanyInput`/`CompanyOutput`. Reuso do service de `ReasonActivate`.
- **Contrato**: `etc/api/organization/ScosOrganization_Company.yml` (schemas já existem; confirmar operations/responses de UC-001..005).
- **Infra**: `ScosOrganizationPermission` — `CREATE_COMPANY`/`UPDATE_COMPANY`/`GET_COMPANY` se ausentes.
- **Dependências**: `scos-foundation-jdempotent` (Redis, POST), `scos-foundation-exception` (`ScosException`/RFC 9457).
- **Banco**: nenhum (tabelas e entidade prontas).
