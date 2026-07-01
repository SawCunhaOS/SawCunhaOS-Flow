## Why

A change `adequacao-liquibase-domain-model-v2` recriou o schema físico (~34 tabelas — 21 novas, ~10 modificadas, 3 removidas), mas foi estritamente Liquibase, sem tocar código Java. O módulo `scos-organization-domain` ainda reflete o schema v1: faltam entidades JPA para as tabelas novas, sobram entidades para as removidas, e as existentes têm colunas divergentes. Hibernate em modo `validate` falharia ao subir a aplicação. Além disso, o novo schema introduz um vocabulário fechado (`CHECK`) que o código atual viola — `StatusCompany`/`StatusEmployee.DELETED` e `LoginStatus.LOCKED`/`DELETED` não existem mais no banco.

## What Changes

- **BREAKING**: `Company.delete()` e `Employee.delete()` removidos — `DELETED` não existe mais no vocabulário fechado; `INACTIVE` passa a ser o estado terminal definitivo
- **BREAKING**: `StatusCompany`/`StatusEmployee` perdem o valor `DELETED`; `LoginStatus` perde `DELETED`/`LOCKED`, ganha `BLOCKED`
- **BREAKING**: `Company.activate/inactivate`, `Employee.activate/inactivate/disable`, e os equivalentes de `Login` deixam de mutar `status` diretamente — passam a criar e retornar um registro de histórico (`CompanyStatusHistory`/`EmployeeStatusHistory`/`LoginStatusHistory`), que o chamador persiste; `status` na tabela principal vira cache sincronizado por trigger de banco
- **BREAKING**: `Login.keycloakId` renomeado para `externalId` (só o campo interno — entidade, `VwAuthorityResponse`, `AuthorityResponseOutput`; contratos públicos OpenAPI/gRPC ficam fora de escopo)
- **Adicionar** 4 tabelas de motivo (`ReasonActivate/Inactivate/Disable/Enable`) e 3 de histórico de status, compartilhando enum `EntityType {COMPANY, EMPLOYEE, LOGIN}`
- **Adicionar** histórico de cargo (`EmployeePositionHistory`, `ReasonPositionChange`) e horários de trabalho (`PositionWorkSchedule` template, `EmployeeWorkSchedule` efetivo), com enum compartilhado `DayOfWeek`
- **Adicionar** mecanismo de outbox genérico (`OutboxTopic`, `OutboxEvent`, `OutboxEventLog`, `OutboxEventDeadLetter`), substituindo o antigo Keycloak dedicado
- **Remover** `IntegrationKeycloak`, `IntegrationKeycloakLog`, `IntegrationMessageInvalid` e seus repositórios (tabelas removidas no v2)
- **Adicionar** dados fiscais da empresa (`LegalNature`, `Cnae`, `CompanyCnaeSecondary`) e os campos correspondentes em `Company`
- **Adicionar** catálogo dinâmico de tipo (`AddressType`, `ContactType`), substituindo o campo `type: String` livre em `EmployeeAddress`/`CompanyAddress`/`EmployeeContact`/`CompanyContact` por FK tipada
- **Adicionar** `LoginProfile` (perfis adicionais por login, além do `Profile` principal)
- **Adicionar** `Position.isTrustPosition`, `Employee.contractType` (novo enum `EmployeeContractType`), `Employee.probationEndDate`
- **Corrigir** `EmployeeAddress.number` de `long` para `int` (schema é `INT`; nunca deu erro por *widening* do Hibernate, mas é mapeamento incorreto)

## Capabilities

### New Capabilities
- `model-status-history`: entidades `CompanyStatusHistory`/`EmployeeStatusHistory`/`LoginStatusHistory`, `ReasonActivate/Inactivate/Disable/Enable`, enum `EntityType`, e o novo contrato de transição de status via histórico (usado por `model-company`/`model-employee`/`model-login`)
- `model-position-history`: `EmployeePositionHistory`, `ReasonPositionChange`, `PositionWorkSchedule`, `EmployeeWorkSchedule`, enum `DayOfWeek`
- `model-outbox`: `OutboxTopic`, `OutboxEvent`, `OutboxEventLog`, `OutboxEventDeadLetter`
- `model-fiscal-data`: `LegalNature`, `Cnae`, `CompanyCnaeSecondary`
- `model-catalog-type`: `AddressType`, `ContactType`, e a migração de `type: String` para FK tipada em `EmployeeAddress`/`CompanyAddress`/`EmployeeContact`/`CompanyContact`
- `model-login-profile`: `LoginProfile`

### Modified Capabilities
- `model-company`: remove `delete()`/`DELETED`; `activate/inactivate/disable/enable` passam a criar histórico via reason; adiciona `legalNature`/`cnaePrincipal`/`stateRegistration`/`municipalRegistration`
- `model-employee`: remove `delete()`/`DELETED`; `activate/inactivate/disable/enable` passam a criar histórico via reason; adiciona `contractType`/`probationEndDate`; corrige `EmployeeAddress.number` para `int`; adiciona `Position.isTrustPosition`
- `model-login`: `LoginStatus` perde `DELETED`/`LOCKED`, ganha `BLOCKED`; `keycloakId` renomeado para `externalId`; regras `LoginDeletedRule` removida, `LoginLockedRule` renomeada e renumerada
- `model-integration`: capability retirada — substituída por `model-outbox`
- `status-deleted`: capability retirada — o soft-delete via `DELETED` não existe mais no vocabulário fechado do banco

## Impact

- **Módulo afetado**: apenas `scos-organization-domain` (nenhuma referência às entidades tocadas existe hoje em `usecase`/`api`/`infrastructure`, confirmado por busca no repositório) — blast radius isolado
- **Nenhuma migration Liquibase criada** — já produzidas por `adequacao-liquibase-domain-model-v2`, fonte da verdade
- **Mensagens/exceções**: `SCOS_LOGIN_012` removida de `ExceptionCodeError` e dos bundles i18n (`DELETED` deixa de existir); `@ScosRule` de `LoginInactiveRule`/`LoginLockedRule` renumeradas para preencher a lacuna
- **Hibernate schema-validation**: deve passar sem erros contra o banco gerado pela v2
