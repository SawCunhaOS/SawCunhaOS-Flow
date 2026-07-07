## Why

O contrato OpenAPI de Natureza Jurídica (`ScosOrganization_Company.yml`, `/v1/legal-natures`, UC-093 a UC-097) já está fechado — os delegates (`LegalNatureApiDelegate`), a entidade JPA `LegalNature` (`corporate/company/internal/LegalNature.java`) e a tabela `SCOS_LEGAL_NATURE` (Liquibase) existem. Mas não há implementação: sem `LegalNatureRepository`, sem camada de domínio (`dto`/`service`/`specification`), sem Use Cases, sem `Delegate` e sem entradas em `ScosOrganizationPermission`. Os 5 endpoints retornam o `501` default do delegate gerado.

## What Changes

- Cria `LegalNatureRepository` (`corporate/company/internal/`) com `existsByCode`/`existsByCodeAndNotId` (default methods QueryDSL), hoje inexistente
- Cria a camada de domínio de `LegalNature` (`dto/LegalNatureInput`+`LegalNatureOutput`, `specification/LegalNatureService`, `service/LegalNatureMapper`+`LegalNatureServiceBean`) em `scos-organization-domain`, seguindo o padrão já usado por `CnaeService`/`ContactTypeService`
- Cria 5 Use Cases (`Create`/`Update`/`Find`/`FindAll`/`Delete`) em `scos-organization-usecase`, pacote `application/usecase/corporate/company/fiscal/legalnature/`
- Cria `LegalNatureDelegate implements LegalNatureApiDelegate` em `scos-organization-api` (`delegate/company/`)
- **DELETE físico com guarda de vínculo** (mesmo padrão do change `crud-cnae`): a natureza jurídica só é excluída se não referenciada por `SCOS_COMPANY.legalNatureId`. A guarda é **em application/domínio** (não há trigger de bloqueio no banco para `SCOS_LEGAL_NATURE`). Diferente do CNAE, há **um único vínculo** a checar (não existe tabela de natureza jurídica secundária)
- Adiciona `existsByLegalNatureId` a `CompanyRepository` (método de guarda)
- Adiciona 4 entradas em `ScosOrganizationPermission` (`GET_LEGAL_NATURE`, `CREATE_LEGAL_NATURE`, `UPDATE_LEGAL_NATURE`, `DELETE_LEGAL_NATURE`) — nomes já fixados pelo `x-authorize` do contrato
- Adiciona 3 constantes em `ExceptionCodeError` (`SCOS_LEGAL_NATURE_001` 404, `SCOS_LEGAL_NATURE_002` 409, `SCOS_LEGAL_NATURE_003` 422), reaproveitando as categorias `SCOS_TITLE_NOT_FOUND`/`SCOS_TITLE_CONFLICT`/`SCOS_TITLE_BUSINESS_RULE_VIOLATION` existentes — nenhuma categoria nova
- **Alinha o tamanho de `code` ao documentado (≤ 30)**: novo changeSet Liquibase `ALTER COLUMN SCOS_LEGAL_NATURE.CODE` de `varchar(10)` → `varchar(30)` (hoje o DB limita a 10, divergente do UC-094; mesmo precedente aplicado no change `crud-cnae`). A constraint `UK_CODE_SCOS_LEGAL_NATURE` é preservada
- Adiciona `maxLength: 30` (`code`) e `maxLength: 255` (`description`) aos schemas `CreateLegalNatureRequest`/`UpdateLegalNatureRequest` no contrato OpenAPI → validação de tamanho retorna `400` limpo (regenera DTOs com `@Size`)

## Capabilities

### New Capabilities
- `legal-nature-api`: comportamento de runtime dos 5 endpoints de Natureza Jurídica (`GET/POST /v1/legal-natures`, `GET/PUT/DELETE /v1/legal-natures/{id}`) — unicidade de `code` por catálogo (409 na criação; excluindo o próprio `{id}` na atualização), `404` quando `{id}` não existe, `422` na exclusão de natureza jurídica ainda vinculada a empresa, validação de tamanho (400) e autorização por permissão

### Modified Capabilities
_Nenhuma — o contrato OpenAPI (`/v1/legal-natures`) já existe; esta change adiciona apenas o comportamento de runtime não normatizado por specs existentes, mesmo padrão de `cnae-api`/`contact-type-api`._

## Impact

- `scos-organization-domain/src/`: novos `corporate/company/dto/{LegalNatureInput,LegalNatureOutput}`, `corporate/company/specification/LegalNatureService`, `corporate/company/service/{LegalNatureMapper,LegalNatureServiceBean}`; novo `corporate/company/internal/LegalNatureRepository`; `CompanyRepository` +`existsByLegalNatureId`
- `scos-organization-usecase/src/`: novo pacote `application/usecase/corporate/company/fiscal/legalnature/` com 5 Use Cases (interface pública + `@Service` package-private Bean cada) + `LegalNatureApiMapper`
- `scos-organization-api/src/`: novo `delegate/company/LegalNatureDelegate`
- `scos-organization-infrastructure`: `ScosOrganizationPermission` +4 entradas
- `scos-organization-shared`: `ExceptionCodeError` +3 constantes; `scos_message_organization[_en].properties` +3 mensagens de `detail`
- `scos-organization-boot`: novo changeSet Liquibase `ALTER COLUMN SCOS_LEGAL_NATURE.CODE varchar(10)→varchar(30)` (tabela e UNIQUE já existem)
- Contrato OpenAPI: `+maxLength` em `code`/`description` de `CreateLegalNatureRequest`/`UpdateLegalNatureRequest` → regeneração dos DTOs
- Testes unitários novos: `LegalNatureServiceBeanTest` (Mockito) — mesma barra do change `crud-cnae` (sem testes de use case / Testcontainers, ausentes no projeto)
- **Fora de escopo**: CRUD de CNAE (change `crud-cnae`)
