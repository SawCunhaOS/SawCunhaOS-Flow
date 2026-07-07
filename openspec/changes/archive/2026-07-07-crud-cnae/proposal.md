## Why

O contrato OpenAPI de CNAE (`ScosOrganization_Company.yml`, `/v1/cnaes`, UC-098 a UC-102) já está fechado — os delegates (`CnaeApiDelegate`), a entidade JPA `Cnae` (`corporate/company/internal/Cnae.java`) e a tabela `SCOS_CNAE` (Liquibase) existem. Mas não há implementação: sem `CnaeRepository`, sem camada de domínio (`dto`/`service`/`specification`), sem Use Cases, sem `Delegate` e sem entradas em `ScosOrganizationPermission`. Os 5 endpoints retornam o `501` default do delegate gerado.

## What Changes

- Cria `CnaeRepository` (`corporate/company/internal/`) com `existsByCode`/`existsByCodeAndNotId` (default methods QueryDSL), hoje inexistente
- Cria a camada de domínio de `Cnae` (`dto/CnaeInput`+`CnaeOutput`, `specification/CnaeService`, `service/CnaeMapper`+`CnaeServiceBean`) em `scos-organization-domain`, seguindo o padrão de `ContactTypeService`/`ContactTypeServiceBean`
- Cria 5 Use Cases (`Create`/`Update`/`Find`/`FindAll`/`Delete`) em `scos-organization-usecase`, pacote `application/usecase/corporate/company/fiscal/cnae/`
- Cria `CnaeDelegate implements CnaeApiDelegate` em `scos-organization-api` (`delegate/company/`)
- **DELETE físico com guarda de vínculo** (diferente do padrão append-only de `AddressType`/`ContactType`): CNAE só é excluído se não referenciado por `SCOS_COMPANY.cnaePrincipalId` **nem** por `SCOS_COMPANY_CNAE_SECONDARY`. A guarda é **em application/domínio** (não há trigger de bloqueio no banco para `SCOS_CNAE`)
- Adiciona `existsByCnaePrincipalId` a `CompanyRepository` e `existsBy…CnaeId` a `CompanyCnaeSecondaryRepository` (métodos de guarda)
- Adiciona 4 entradas em `ScosOrganizationPermission` (`GET_CNAE`, `CREATE_CNAE`, `UPDATE_CNAE`, `DELETE_CNAE`) — nomes já fixados pelo `x-authorize` do contrato
- Adiciona 3 constantes em `ExceptionCodeError` (`SCOS_CNAE_001` 404, `SCOS_CNAE_002` 409, `SCOS_CNAE_003` 422), reaproveitando as categorias `SCOS_TITLE_NOT_FOUND`/`SCOS_TITLE_CONFLICT`/`SCOS_TITLE_BUSINESS_RULE_VIOLATION` existentes — nenhuma categoria nova
- **Alinha o tamanho de `code` ao documentado (≤ 30)**: novo changeSet Liquibase `ALTER COLUMN SCOS_CNAE.CODE` de `varchar(10)` → `varchar(30)` (hoje o DB limita a 10, divergente do UC-099). A constraint `UK_CODE_SCOS_CNAE` é preservada
- Adiciona `maxLength: 30` (`code`) e `maxLength: 255` (`description`) aos schemas `CreateCnaeRequest`/`UpdateCnaeRequest` no contrato OpenAPI → validação de tamanho retorna `400` limpo (regenera DTOs com `@Size`), fechando a lacuna que os catálogos `AddressType`/`ContactType` deixaram (sem `maxLength`)

## Capabilities

### New Capabilities
- `cnae-api`: comportamento de runtime dos 5 endpoints de CNAE (`GET/POST /v1/cnaes`, `GET/PUT/DELETE /v1/cnaes/{id}`) — unicidade de `code` por catálogo (409 na criação; excluindo o próprio `{id}` na atualização), `404` quando `{id}` não existe, `422` na exclusão de CNAE ainda vinculado a empresa (principal ou secundário), e autorização por permissão

### Modified Capabilities
_Nenhuma — o contrato OpenAPI (`/v1/cnaes`) já existe; esta change adiciona apenas o comportamento de runtime não normatizado por specs existentes, mesmo padrão de `position-api`/`contact-type-api`._

## Impact

- `scos-organization-domain/src/`: novos `corporate/company/dto/{CnaeInput,CnaeOutput}`, `corporate/company/specification/CnaeService`, `corporate/company/service/{CnaeMapper,CnaeServiceBean}`; novo `corporate/company/internal/CnaeRepository`; `CompanyRepository` +`existsByCnaePrincipalId`; `CompanyCnaeSecondaryRepository` +`existsBy…CnaeId`
- `scos-organization-usecase/src/`: novo pacote `application/usecase/corporate/company/fiscal/cnae/` com 5 Use Cases (interface pública + `@Service` package-private Bean cada)
- `scos-organization-api/src/`: novo `delegate/company/CnaeDelegate`
- `scos-organization-infrastructure`: `ScosOrganizationPermission` +4 entradas
- `scos-organization-shared`: `ExceptionCodeError` +3 constantes; `scos_message_organization[_en].properties` +3 mensagens de `detail`
- Testes unitários novos: `CnaeServiceBeanTest` (Mockito) + testes dos Use Cases; integração Testcontainers para unicidade de `code` e guarda de exclusão
- `scos-organization-boot`: novo changeSet Liquibase `ALTER COLUMN SCOS_CNAE.CODE varchar(10)→varchar(30)` (tabela e UNIQUE já existem)
- Contrato OpenAPI: `+maxLength` em `code`/`description` de `CreateCnaeRequest`/`UpdateCnaeRequest` → regeneração dos DTOs
- **Fora de escopo**: CRUD de Natureza Jurídica (change própria) e vínculo de CNAEs secundários da empresa (UC-103/104/105, seção 1.3)
