## Why

O schema v2 adicionou `CONTRACT_TYPE`/`PROBATION_END_DATE` em `SCOS_EMPLOYEE`, `IS_TRUST_POSITION` em `SCOS_POSITION`, e criou `SCOS_REASON_POSITION_CHANGE`+`SCOS_EMPLOYEE_POSITION_HISTORY` (histórico imutável de cargo, fechado automaticamente por trigger). Nenhum desses campos/tabelas está exposto no contrato OpenAPI hoje. Além disso, `TransferEmployeeRequest` não tem `reasonPositionChangeId`, mas a tabela de histórico exige motivo `NOT NULL` sempre que uma nova atribuição de cargo é criada — sem o campo, `/transfer` quebraria contra o banco atual.

## What Changes

**Escopo desta rodada: somente o contrato OpenAPI (YAML).** Implementação de código (permissões, use cases, delegates, testes) fica para uma change futura.

- `Position`/`CreatePositionRequest`/`UpdatePositionRequest` ganham `isTrustPosition: boolean` (default `false`)
- `Employee`/`CreateEmployeeRequest`/`UpdateEmployeeRequest` ganham `contractType` (novo enum `EmployeeContractType`: `CLT`/`PJ`/`ESTAGIO`/`TEMPORARIO`, obrigatório) e `probationEndDate` (date, opcional)
- Criar CRUD de referência `ReasonPositionChange` (`code`, `description`, `active`, sem `entityType`)
- **BREAKING**: `TransferEmployeeRequest` passa a exigir `reasonPositionChangeId` sempre — todo `/transfer` insere uma nova linha em `EmployeePositionHistory`, mesmo quando `positionId` não muda (decidido com o usuário: toda transferência é um marco na linha do tempo do funcionário, não só mudança de cargo)
- Criar `GET /v1/employees/{id}/position-history` (paginado)
- `x-authorize` novo por rota no contrato (entradas correspondentes em `ScosOrganizationPermission` ficam para a change de implementação)

## Capabilities

### New Capabilities
- `employee-position-contract-data`: `Employee.contractType`/`Employee.probationEndDate` e `Position.isTrustPosition` no contrato OpenAPI
- `employee-position-history`: catálogo `ReasonPositionChange`, `reasonPositionChangeId` obrigatório em `/transfer`, e consulta paginada ao histórico de cargo do funcionário

### Modified Capabilities
(nenhuma — as specs existentes `model-employee`/`schema-organization` cobrem a camada JPA/banco, já sincronizada em `atualizacao-entidades-jpa-liquibase-v2`; esta mudança é só o contrato OpenAPI, uma camada ainda não capturada em nenhuma spec existente)

## Impact

- `etc/api/organization/ScosOrganization_Department-Position.yml` — `Position`/`Create`/`UpdatePositionRequest` modificados; schemas novos `ReasonPositionChange` + DTOs; paths novos `/v1/reason-position-change*`
- `etc/api/organization/ScosOrganization_Employee.yml` — `Employee`/`Create`/`UpdateEmployeeRequest` modificados; `TransferEmployeeRequest` modificado (breaking); enum novo `EmployeeContractType`; path novo `GET /v1/employees/{id}/position-history`
- `ScosOrganizationPermission` (código) — fora de escopo desta change; entradas de `x-authorize` ficam pendentes para a implementação futura
- Sem impacto de banco — schema v2 já existe e a camada JPA já foi sincronizada (`atualizacao-entidades-jpa-liquibase-v2`, completo)
