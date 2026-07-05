## Why

O contrato OpenAPI do catálogo de referência `AddressType`/`ContactType` já foi fechado no change `catalogo-tipo-endereco-contato` (completo), substituindo o campo livre `type: string` de `CompanyAddress`/`EmployeeAddress`/`CompanyContact`/`EmployeeContact` por FK (`addressTypeId`/`contactTypeId`). Esse change documentou explicitamente que a implementação de código ficaria para uma change futura — hoje só existem a entidade JPA e o repositório vazio (`corporate/catalog/internal/{AddressType,ContactType}[Repository].java`); não há `dto/`, `service/`, `specification/`, Use Case, `Delegate` nem entradas em `ScosOrganizationPermission`. Os 12 endpoints do contrato (UC-081 a UC-092) não funcionam.

## What Changes

- Cria a camada de domínio (`dto`/`service`/`specification`) para `AddressType` e `ContactType` em `scos-organization-domain`, seguindo o padrão já usado por `DepartmentService`/`PositionService`
- Adiciona `existsByCode`/`existsByCodeAndNotId` (QueryDSL default methods) aos repositórios, hoje vazios
- Cria os 6 Use Cases (`Create/Update/Find/FindAll/Enable/Disable`) por entidade em `scos-organization-usecase`
- Cria `AddressTypeDelegate`/`ContactTypeDelegate implements *ApiDelegate` em `scos-organization-api`
- Adiciona 12 novas entradas em `ScosOrganizationPermission` (`GET/CREATE/UPDATE/ENABLE/DISABLE_ADDRESS_TYPE`, mesmo conjunto para `CONTACT_TYPE`) — nomes já fixados pelo `x-authorize` do contrato
- Adiciona 8 novas constantes em `ExceptionCodeError` (`SCOS_ADDRESS_TYPE_001..004`, `SCOS_CONTACT_TYPE_001..004`), já com `httpCode`/`title` conforme o padrão implementado pela change `padronizacao-http-status-exception-code` (404/409/422, reaproveitando as 7 categorias `SCOS_TITLE_*` existentes — nenhuma categoria nova)
- Sem `DELETE` — catálogo é append-only, só `enable`/`disable` (trigger do banco bloqueia exclusão física)

## Capabilities

### New Capabilities
- `address-type-api`: comportamento de runtime dos 6 endpoints de `AddressType` (`GET/POST /v1/address-types`, `GET/PUT /v1/address-types/{id}`, `PUT .../enable`, `PUT .../disable`) — unicidade de `code` por catálogo, idempotência de `enable`/`disable` (422 se já no estado alvo), autorização por permissão, e a regra de que `disable` não remove vínculos existentes em `CompanyAddress`/`EmployeeAddress`
- `contact-type-api`: mesmo escopo de `address-type-api`, para `ContactType` (`/v1/contact-types`)

### Modified Capabilities
_Nenhuma — `address-type-catalog`/`contact-type-catalog` (specs do change `catalogo-tipo-endereco-contato`, ainda não arquivado) cobrem o formato do contrato OpenAPI (schema, enum de 2 valores, autorização por nome de permissão); não normatizam as regras de negócio de runtime (unicidade, idempotência, efeito de disable sobre vínculos) que são o objeto desta change — por isso capability nova (`*-api`), não modificação, mesmo padrão usado por `position-api`/`department-api` no change `crud-cargo-position`._

## Impact

- `scos-organization-domain/src/`: novos `dto/`, `service/`, `specification/` sob `corporate/catalog/`; `AddressTypeRepository`/`ContactTypeRepository` ganham `existsByCode`/`existsByCodeAndNotId`
- `scos-organization-usecase/src/`: novo pacote `application/usecase/corporate/catalog/{addresstype,contacttype}/` com 6 Use Cases cada
- `scos-organization-api/src/`: novo `delegate/catalog/` com `AddressTypeDelegate`/`ContactTypeDelegate`
- `scos-organization-infrastructure`: `ScosOrganizationPermission` +12 entradas
- `scos-organization-shared`: `ExceptionCodeError` +8 constantes; `scos_message_organization[_en].properties` +8 mensagens de `detail` cada
- Testes unitários novos: `AddressTypeServiceBeanTest`, `ContactTypeServiceBeanTest` (Mockito, sem Testcontainers)
- Sem impacto de banco — tabelas já existem (schema v2, change `adequacao-liquibase-domain-model-v2`, completo)
- Sem impacto no contrato OpenAPI — já fechado pelo change `catalogo-tipo-endereco-contato`
