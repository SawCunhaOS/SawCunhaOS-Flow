## Context

`CompanyAddress`/`EmployeeAddress`/`CompanyContact`/`EmployeeContact` usam `type: string` livre no contrato público. O schema v2 já substituiu isso por FK para `SCOS_ADDRESS_TYPE`/`SCOS_CONTACT_TYPE`, catálogos dinâmicos compartilhados entre `Company` e `Employee` via coluna `ENTITY_TYPE` (2 valores: `COMPANY`/`EMPLOYEE`, confirmado pelo `CHECK` real `chk_address_type_entity_type`/`chk_contact_type_entity_type`). `DELETE` é bloqueado por trigger no banco — só existe `ACTIVE`/desativação.

## Goals / Non-Goals

**Goals:**
- CRUD de referência completo (sem `DELETE`) para `AddressType`/`ContactType`
- Migrar os 4 schemas de endereço/contato (`Company*`/`Employee*`) de `type: string` para `addressTypeId`/`contactTypeId` (FK, obrigatório)
- Corrigir `EmployeeAddress.number` para `int32` (alinhado ao banco `INT`)
- Decidir onde os schemas/paths de `AddressType`/`ContactType` ficam fisicamente, evitando duplicação entre `ScosOrganization_Company.yml` e `ScosOrganization_Employee.yml`

**Non-Goals:**
- Migração de dados existentes (sistema sem produção)
- Suportar `entityType=LOGIN` em `AddressType`/`ContactType` — o `CHECK` do banco só permite `COMPANY`/`EMPLOYEE`; não confundir com o enum de 3 valores (`+LOGIN`) usado pelos catálogos `Reason*` (change irmã `historico-status-motivos-organization`) — são dois enums OpenAPI distintos, não compartilhados
- Implementação de código (permissões, use case, domain, delegates, testes) — fora de escopo desta change; entra em change futura separada

## Decisions

### Onde ficam os schemas/paths de AddressType/ContactType
**Escolha: arquivo novo dedicado `etc/api/organization/ScosOrganization_Catalog.yml`**, contendo os schemas `AddressType`/`ContactType` (+ `Create`/`Update`/`Get`/`GetAll*`) e os paths `/v1/address-types*`/`/v1/contact-types*`. `ScosOrganization_Company.yml` e `ScosOrganization_Employee.yml` referenciam esses schemas via `$ref: './ScosOrganization_Catalog.yml#/components/schemas/AddressType'` (padrão já usado no projeto para `$ref` cross-file, ex. `ScosComponents.yml#/components/schemas/ScosPaginated`).

**Alternativa descartada: colocar em `ScosComponents.yml`.** `ScosComponents.yml` hoje contém só infraestrutura genérica reaproveitável em qualquer contrato (paginação, respostas de erro padrão, enum `Direction`) — nenhum schema de domínio. `AddressType`/`ContactType` são dados de referência específicos do bounded context `organization` (Company/Employee), não infraestrutura. Um arquivo novo por conceito de domínio já é o padrão do repositório (`Company.yml`, `Employee.yml`, `Login.yml`, `Department-Position.yml`, `Configuration.yml`) — `Catalog.yml` estende esse padrão para "catálogos de referência compartilhados entre agregados", e deixa espaço pra outras mudanças da mesma leva (ex. os 4 catálogos `Reason*` de `historico-status-motivos-organization`) seguirem a mesma convenção sem forçar tudo dentro de `ScosComponents.yml`.

### entityType de AddressType/ContactType é enum próprio de 2 valores
`AddressType.entityType`/`ContactType.entityType` usam um enum OpenAPI nomeado próprio (`CatalogEntityType` ou nome equivalente definido em `ScosOrganization_Catalog.yml`, 2 valores: `COMPANY`/`EMPLOYEE`) — **não** reaproveita o enum de 3 valores (`COMPANY`/`EMPLOYEE`/`LOGIN`) que os catálogos `Reason*` vão usar. Ainda que a camada JPA possa compartilhar um único enum Java `EntityType`, o contrato OpenAPI precisa impedir `LOGIN` aqui na validação de schema (`4XX` de contrato), já que o `CHECK` do banco (`chk_address_type_entity_type`) rejeitaria fisicamente essa combinação com um erro genérico de banco.

### Sem DELETE em AddressType/ContactType
Não expor `DELETE /v1/address-types/{id}` nem `/v1/contact-types/{id}` — o trigger do banco bloqueia fisicamente a exclusão; o contrato não deve oferecer uma operação que sempre falha. Só `enable`/`disable`.

### Breaking change direto
`type: string` → `addressTypeId`/`contactTypeId` é substituição direta, sem os dois campos coexistindo — decidido previamente (sistema sem produção, sem necessidade de período de transição).

## Risks / Trade-offs

- [Risco] Breaking change no contrato público quebra qualquer client já integrado, mesmo em dev/homolog → [Mitigação] comunicar antes de publicar a mudança; sem produção, o custo de correção é baixo
- [Risco] `entityType` do `AddressType`/`ContactType` escolhido pode não bater com o agregado do endereço/contato sendo criado (`entityType=COMPANY` usado numa `EmployeeAddress`) → [Mitigação] regra de negócio validada no use case (não expressável só pelo contrato OpenAPI); documentar claramente na `description` de cada path
- [Risco] Introduzir `ScosOrganization_Catalog.yml` como arquivo novo é uma decisão de convenção que outras changes da mesma leva (`historico-status-motivos-organization`) podem replicar de forma inconsistente se decidirem algo diferente para os catálogos `Reason*` → [Mitigação] nomear a decisão explicitamente aqui para servir de referência; ambas as changes documentam a decisão de local no próprio `design.md`
