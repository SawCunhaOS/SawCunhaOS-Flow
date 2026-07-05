## Why

A entidade de domínio `Position` já existe (`Position.activate()`/`deactivate()`, `PositionRepository` com `existsByCode`/`existsByCodeAndNotId`/`existsByDepartmentId`) e as permissões (`GET/CREATE/UPDATE/ENABLE/DISABLE_POSITION`) já estão em `ScosOrganizationPermission`. Mas não existe camada de serviço de domínio, Use Cases nem `PositionDelegate` — `delegate/position/` está vazio e `usecase/corporate/position/` não existe. Nenhum dos 6 endpoints de Position do contrato OpenAPI funciona hoje. Essa camada aparentemente existiu (task 5.7 de `adequacao-rest-nivel2-organization`) mas foi perdida no refactor de entidades JPA/Liquibase v2.

## What Changes

- Criar `PositionInput`/`PositionOutput` (dto), `PositionMapper`, `PositionService` (specification) + `PositionServiceBean` em `scos-organization-domain`, seguindo exatamente o padrão já usado por `DepartmentService`
- Criar os 6 Use Cases (`Create/Update/Find/FindAll/Enable/DisablePositionUseCase` + Bean) em `scos-organization-usecase`
- Criar `PositionDelegate implements PositionApiDelegate` em `scos-organization-api`
- Reaproveitar `DepartmentService.findDepartmentById()` já existente para validar `departmentId` (existência + `SCOS_DEPARTMENT_001`) e status ativo (`SCOS_DEPARTMENT_006`) no create/update de Position
- Expor um método público no agregado Employee (`employee/specification/`, hoje vazio) para checar funcionário `ACTIVE` vinculado a um cargo — necessário para bloquear `disable` (`SCOS_POSITION_003`) sem violar o encapsulamento do pacote `employee.internal`
- Corrigir `GET /v1/positions` e `GET /v1/departments` para aplicarem de fato o filtro `active` — o parâmetro já existia nos dois contratos OpenAPI mas era ignorado nas duas implementações (Position nunca existia; Department ignorava silenciosamente)
- **Fora de escopo**: Jornada de Trabalho do Cargo (`/v1/positions/{positionId}/work-schedule*`, já implementada em `jornada-trabalho-position-employee`) e Motivo de Mudança de Cargo (`/v1/reason-position-change*`)

## Capabilities

### New Capabilities

- `position-api`: Expõe os 6 endpoints de Position (`GET /v1/positions`, `POST /v1/positions`, `GET /v1/positions/{id}`, `PUT /v1/positions/{id}`, `PUT /v1/positions/{id}/enable`, `PUT /v1/positions/{id}/disable`) conforme UC-028 a UC-033, com validação de departamento ativo, bloqueio de disable por funcionário ativo vinculado, e filtros `departmentId`/`active` na listagem.
- `department-api`: Cobre a correção pontual do filtro `active` de `GET /v1/departments` (o resto do CRUD de Department já está implementado e fora do escopo de documentação desta capability — só a correção do filtro é normatizada aqui).

### Modified Capabilities

_Nenhuma — não existe spec de capability publicada para Department em `openspec/specs/`; a correção do filtro `active` em Department é registrada como capability nova (`department-api`), não como delta de uma capability existente._

## Impact

- `scos-organization-domain/src/`: novos `dto/`, `service/`, `specification/` sob `corporate/position/`; novo método público em `corporate/employee/specification/` (pacote hoje vazio); `DepartmentRepository`/`DepartmentService`/`DepartmentServiceBean` ajustados para o filtro `active`
- `scos-organization-usecase/src/`: novo pacote `application/usecase/corporate/position/` com 6 Use Cases; `FindAllDepartmentUseCase`/`Bean` ajustados para repassar `active`
- `scos-organization-api/src/`: novo `PositionDelegate` em `delegate/position/` (hoje vazio); `DepartmentDelegate.getAllDepartments` passa a repassar `active`
- Testes unitários novos: `PositionServiceBeanTest`, `DepartmentServiceBeanTest`, `EmployeePositionQueryServiceBeanTest`
- Sem impacto em Liquibase (tabelas já existem)
- Sem impacto no contrato OpenAPI (os parâmetros `active`/`departmentId` já existiam nos dois YAMLs)
