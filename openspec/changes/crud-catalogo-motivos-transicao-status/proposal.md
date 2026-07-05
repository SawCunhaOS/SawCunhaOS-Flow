## Why

O contrato OpenAPI dos 4 catálogos de motivo (`ReasonActivate`/`ReasonInactivate`/`ReasonDisable`/`ReasonEnable`) já foi fechado (`etc/api/organization/ScosOrganization_Reason.yml`, change `historico-status-motivos-organization`, capability `reason-status-catalog`). Hoje só existem a entidade JPA e o repositório vazio das 4 entidades — não há `dto/`, `service/`, `specification/`, Use Case, `Delegate`, nem entradas em `ScosOrganizationPermission`. Os 24 endpoints do contrato (UC-113 a UC-136) não funcionam.

## What Changes

- Criar `Reason{Activate,Inactivate,Disable,Enable}Input`/`Output` (dto), `Mapper`, `Service` (specification) + `ServiceBean` em `scos-organization-domain`, seguindo o padrão já usado por `Department`/`Position`
- Criar 6 Use Cases (`Create/Update/Find/FindAll/Enable/Disable...UseCase` + Bean + `ApiMapper`) por catálogo em `scos-organization-usecase` (24 no total)
- Criar 4 Delegates (`Reason{Activate,Inactivate,Disable,Enable}Delegate implements Reason*ApiDelegate`) em `scos-organization-api`
- Adicionar `existsByCode`/`existsByCodeAndNotId` aos 4 repositórios (unicidade de `code` escopada por catálogo, 4 tabelas independentes)
- Adicionar 20 novas entradas a `ScosOrganizationPermission` (`GET/CREATE/UPDATE/ENABLE/DISABLE_REASON_{ACTIVATE,INACTIVATE,DISABLE,ENABLE}`), nomes já fixados no `x-authorize` do contrato
- Adicionar 16 constantes a `ExceptionCodeError` (`SCOS_REASON_{ACTIVATE,INACTIVATE,DISABLE,ENABLE}_001..004`) reaproveitando as 7 categorias `SCOS_TITLE_*` já existentes, + 16 mensagens de `detail` pt-br/en
- Adotar `entityType` com 3 valores (`COMPANY`/`EMPLOYEE`/`LOGIN`), conforme o contrato YAML e o enum de domínio compartilhado `EntityType.java` — divergência confirmada com o usuário em relação à doc 05 (que cita só 2 valores; doc desatualizada, ajuste de doc fora de escopo)
- **Fora de escopo**: `reasonId` nos `enable`/`disable`/`block`/`unblock` de Company/Employee/Login (capability `status-transition-contract`); `GET .../status-history` paginado (capability `status-history-query`); catálogo `AddressType`/`ContactType` (ideia irmã `crud-catalogo-tipo-endereco-contato`); correção da doc 05; testes de integração com Testcontainers

## Capabilities

### New Capabilities

- `reason-catalog-api`: Expõe os 24 endpoints (6 por catálogo × 4 catálogos) de CRUD + enable/disable para `ReasonActivate`/`ReasonInactivate`/`ReasonDisable`/`ReasonEnable` (UC-113 a UC-136), com validação de unicidade de `code` por catálogo, `entityType` de 3 valores, e transições enable/disable idempotentes (422 se já no estado alvo).

### Modified Capabilities

_Nenhuma — a capability `reason-status-catalog` (contrato/dados dos catálogos) tem sua spec delta ainda pendente de archive dentro da change `historico-status-motivos-organization` (não existe `openspec/specs/reason-status-catalog/` na base). Esta change não altera esse contrato, apenas implementa a camada de código sobre ele; por isso é registrada como capability nova (`reason-catalog-api`), não como delta._

## Impact

- `scos-organization-domain/src/`: novos `dto/`, `service/`, `specification/` sob `access/status/` (8 dto + 4 mapper + 4 servicebean + 4 interface); `Reason{Activate,Inactivate,Disable,Enable}Repository` ganham `existsByCode`/`existsByCodeAndNotId`
- `scos-organization-usecase/src/`: novo pacote `application/usecase/access/status/reason{activate,inactivate,disable,enable}/` com 6 Use Cases cada + `ApiMapper` (24 Use Cases + 4 ApiMapper)
- `scos-organization-api/src/`: 4 novos Delegates em `delegate/reason/` (hoje vazio)
- `scos-organization-infrastructure/src/`: `ScosOrganizationPermission` +20 entradas
- `ExceptionCodeError` +16 constantes; `scos_message_organization[_en].properties` +16 mensagens de `detail` cada
- Testes unitários novos: `Reason{Activate,Inactivate,Disable,Enable}ServiceBeanTest` (Mockito, sem Testcontainers)
- Sem impacto em Liquibase (tabelas `SCOS_REASON_{ACTIVATE,INACTIVATE,DISABLE,ENABLE}` já existem)
- Sem impacto no contrato OpenAPI (já fechado)
