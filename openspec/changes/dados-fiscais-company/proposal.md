## Why

`SCOS_COMPANY` ganhou 4 colunas novas no schema v2 (`LEGAL_NATURE_ID`, `CNAE_PRINCIPAL_ID`, `STATE_REGISTRATION`, `MUNICIPAL_REGISTRATION`) e as tabelas `SCOS_LEGAL_NATURE`, `SCOS_CNAE` e `SCOS_COMPANY_CNAE_SECONDARY`, mas nada disso está exposto em `ScosOrganization_Company.yml`. O contrato público de `Company` está incompleto em relação ao schema físico já existente.

## What Changes

**Escopo desta rodada: somente o contrato OpenAPI (YAML).** Implementação de código (permissões, use cases, delegates, testes) fica para uma change futura.

- Adicionar a `CompanyOutput`/`CreateCompanyRequest`/`UpdateCompanyRequest`: `legalNatureId` (nullable), `cnaePrincipalId` (nullable), `stateRegistration` (nullable, aceita `"ISENTO"`), `municipalRegistration` (nullable)
- Criar CRUD mínimo de referência `LegalNature` (`code`, `description`) — sem `active`/soft-delete, `DELETE` físico real (schema do banco não tem `UPDATED_AT`/`USER_AT`/`ACTIVE`)
- Criar CRUD mínimo de referência `Cnae` — mesmo padrão de `LegalNature`
- Criar sub-recurso `CompanyCnaeSecondary` (`GET`/`POST`/`DELETE /v1/companies/{companyId}/cnaes-secondary`) — associação N:N pura, PK composta, sem `PUT`
- Adicionar `x-authorize` novo por rota no contrato: `GET/CREATE/UPDATE/DELETE_LEGAL_NATURE`, `GET/CREATE/UPDATE/DELETE_CNAE`, `GET/CREATE/DELETE_COMPANY_CNAE_SECONDARY` (entradas correspondentes em `ScosOrganizationPermission` ficam para a change de implementação)

## Capabilities

### New Capabilities
- `legal-nature-catalog`: CRUD de referência mínimo (`code`/`description`, sem auditoria) para natureza jurídica da empresa
- `cnae-catalog`: CRUD de referência mínimo (`code`/`description`, sem auditoria) para CNAE
- `company-cnae-secondary`: associação N:N entre `Company` e `Cnae` (CNAEs secundários), sem update

### Modified Capabilities
- `model-company`: `CompanyOutput`/`CreateCompanyRequest`/`UpdateCompanyRequest` passam a expor `legalNatureId`, `cnaePrincipalId`, `stateRegistration`, `municipalRegistration`

## Impact

- `etc/api/organization/ScosOrganization_Company.yml` — schemas `CompanyOutput`/`CreateCompanyRequest`/`UpdateCompanyRequest` modificados; schemas novos `LegalNature`, `Cnae`, `CompanyCnaeSecondary` e respectivos `Create`/`Update`/`Get`/`GetAll` DTOs; paths novos `/v1/legal-natures*`, `/v1/cnaes*`, `/v1/companies/{companyId}/cnaes-secondary*`
- `ScosOrganizationPermission` (código) — fora de escopo desta change; entradas de `x-authorize` ficam pendentes para a implementação futura
- Sem impacto de banco — schema v2 já existe (`adequacao-liquibase-domain-model-v2`, completo)
- Sem seed oficial de dados IBGE (`LEGAL_NATURE`/`CNAE`) — fora de escopo, decidido na change Liquibase v2
