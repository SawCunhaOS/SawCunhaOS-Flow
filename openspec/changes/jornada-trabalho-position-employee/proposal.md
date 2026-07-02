## Why

`SCOS_POSITION_WORK_SCHEDULE` (template de jornada por cargo) e `SCOS_EMPLOYEE_WORK_SCHEDULE` (jornada efetiva por funcionário) são tabelas novas do schema v2, mas nenhuma tem endpoint hoje. Sem contrato, não há como cadastrar/consultar a jornada de trabalho via API.

## What Changes

**Escopo desta rodada: somente o contrato OpenAPI (YAML).** Implementação de código (permissões, use cases, delegates, testes) fica para uma change futura.

- Criar CRUD de `PositionWorkSchedule` em `ScosOrganization_Department-Position.yml`: `GET/POST /v1/positions/{positionId}/work-schedule`, `PUT/DELETE /v1/positions/{positionId}/work-schedule/{dayOfWeek}`
- Criar CRUD de `EmployeeWorkSchedule` em `ScosOrganization_Employee.yml`, mesmo padrão, sob `/v1/employees/{employeeId}/work-schedule`
- **BREAKING (não aplicável — endpoints novos, sem contrato prévio a quebrar)**
- `GET .../work-schedule` retorna array direto (`WorkScheduleOutput[]`), sem `paginationFilter`/`paginatedDTO` — única exceção deliberada de paginação em todo o contrato, justificada pelo teto real de 7 registros (UK composta `(positionId/employeeId, dayOfWeek)` no banco)
- Adicionar `x-authorize` no contrato: `GET/CREATE/UPDATE/DELETE_POSITION_WORK_SCHEDULE`, mesmo conjunto para `EMPLOYEE_WORK_SCHEDULE` (entradas correspondentes em `ScosOrganizationPermission` ficam para a change de implementação)

## Capabilities

### New Capabilities
- `position-work-schedule`: CRUD de horário de trabalho template por cargo, por dia da semana (`dayOfWeek` como chave do sub-recurso)
- `employee-work-schedule`: CRUD de horário de trabalho efetivo por funcionário, mesmo padrão de `position-work-schedule`

### Modified Capabilities
(nenhuma — tabelas e endpoints inteiramente novos, sem capability existente cobrindo jornada de trabalho)

## Impact

- `etc/api/organization/ScosOrganization_Department-Position.yml` — paths novos `/v1/positions/{positionId}/work-schedule*`, schemas novos `PositionWorkSchedule`/`CreatePositionWorkScheduleRequest`/`UpdatePositionWorkScheduleRequest`
- `etc/api/organization/ScosOrganization_Employee.yml` — paths novos `/v1/employees/{employeeId}/work-schedule*`, schemas novos `EmployeeWorkSchedule`/`CreateEmployeeWorkScheduleRequest`/`UpdateEmployeeWorkScheduleRequest`
- `etc/api/organization/ScosComponents.yml` — enum novo `DayOfWeek` (compartilhado entre os dois arquivos acima via `$ref`, mesmo padrão já usado por `Direction`)
- `ScosOrganizationPermission` (código) — fora de escopo desta change; entradas de `x-authorize` ficam pendentes para a implementação futura
- Sem impacto de banco — schema v2 já existe (`adequacao-liquibase-domain-model-v2`, completo)
- Fora de escopo: mecanismo de cópia template→efetivo (código/use case interno, não endpoint público)
