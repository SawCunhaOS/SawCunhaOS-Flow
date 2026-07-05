## 1. Domain — Employee (checagem cross-agregado, pré-requisito do disable)

- [x] 1.1 Adicionar `existsByPositionIdAndStatus(Long positionId, StatusEmployee status)` em `EmployeeQueryRepository` (QueryDSL: `qEmployee.position.id.eq(positionId).and(qEmployee.status.eq(status))`)
- [x] 1.2 Criar interface pública `employee/specification/` com método `boolean existsActiveEmployeeInPosition(Long positionId)` — `EmployeePositionQueryService`
- [x] 1.3 Implementar o Bean (`employee/service/`, `@Service` público, mesmo padrão de `DepartmentServiceBean`) chamando `EmployeeQueryRepository.existsByPositionIdAndStatus(positionId, StatusEmployee.ACTIVE)` — `EmployeePositionQueryServiceBean`

## 2. Domain — Position (dto, mapper, service)

- [x] 2.1 Criar `PositionInput` (record `@Builder`: `id, code, description, departmentId, isTrustPosition`) em `corporate/position/dto/`
- [x] 2.2 Criar `PositionOutput` (record `@Builder`: `id, code, description, active, isTrustPosition, department: DepartmentOutput`) em `corporate/position/dto/`
- [x] 2.3 Criar `PositionMapper` (`@Mapper(componentModel = "spring")`, compondo `DepartmentMapper`) em `corporate/position/service/` com `toPositionOutput(Position): PositionOutput` — `@Mapping(target = "isTrustPosition", source = "trustPosition")` explícito (getter Lombok de campo `isTrustPosition` gera propriedade JavaBean `trustPosition`)
- [x] 2.4 Criar `PositionService` (interface pública) em `corporate/position/specification/` com `create/update/findById/findAll(departmentId, active, Pageable)/enable/disable/findPositionById`
- [x] 2.5 Implementar `PositionServiceBean` (`@Service` público) em `corporate/position/service/`:
  - `create`: valida `existsByCode` → `SCOS_POSITION_002`; `departmentService.findDepartmentById(departmentId)` → `SCOS_DEPARTMENT_001`; valida `department.isActive()` → `SCOS_DEPARTMENT_006`; persiste; monta `PositionOutput` combinando `PositionMapper` (campos planos) + `DepartmentMapper` (nested `department`, mapeado explicitamente no service já que `PositionMapper.toPositionOutput` ignora `department` via `@Mapping(ignore = true)`)
  - `update`: `findPositionById` → `SCOS_POSITION_001`; `existsByCodeAndNotId` → `SCOS_POSITION_002`; revalida departamento sempre (mesma lógica do create, independente de ter mudado)
  - `findById`/`findAll`: `findAll` aplica filtro `departmentId` e filtro `active` via QueryDSL em `PositionRepository.findAllFiltered`, cada um só entra na query quando não-nulo
  - `enable`: `findPositionById` → `position.activate()` (lança `SCOS_POSITION_004` internamente)
  - `disable`: `findPositionById` → checa `existsActiveEmployeeInPosition(id)` → `SCOS_POSITION_003` se `true` → `position.deactivate()` (lança `SCOS_POSITION_005` internamente)
  - `findPositionById`: `positionRepository.findById(id).orElseThrow(() -> new ScosException(SCOS_POSITION_001))`

## 3. Use Cases

- [x] 3.1 Criar `CreatePositionUseCase` (interface) + `CreatePositionUseCaseBean` em `scos-organization-usecase/.../usecase/corporate/position/`
- [x] 3.2 Criar `UpdatePositionUseCase` (interface) + `UpdatePositionUseCaseBean`
- [x] 3.3 Criar `FindPositionUseCase` (interface) + `FindPositionUseCaseBean`
- [x] 3.4 Criar `FindAllPositionUseCase` (interface) + `FindAllPositionUseCaseBean` (recebe `PaginationFilter` + `departmentId`/`active` opcionais; monta `GetAllPositionsResponse` com `Position` aninhando `Department`) — usa `PositionApiMapper` (novo, package-private) pra mapear `PositionOutput → Position`/`Department` api dto
- [x] 3.5 Criar `EnablePositionUseCase` (interface) + `EnablePositionUseCaseBean`
- [x] 3.6 Criar `DisablePositionUseCase` (interface) + `DisablePositionUseCaseBean`

## 4. API Layer — Delegate

- [x] 4.1 Criar `PositionDelegate implements PositionApiDelegate` em `scos-organization-api/.../delegate/position/`, injetando os 6 Use Cases, seguindo o padrão de `DepartmentDelegate` (mapeamento manual DTO gerado ↔ tipos de aplicação)
- [x] 4.2 Confirmar que `getAllPositions` repassa os parâmetros `departmentId` e `active` da query para `FindAllPositionUseCase`

## 5. Testes

- [x] 5.1 Unit tests de `PositionServiceBean` (JUnit5 + Mockito + AssertJ) — cobrir os cenários de `specs/position-api/spec.md` (404/409/422 de cada regra, ordem de validação do disable, filtros departmentId/active, revalidação de departamento sempre no update) — `PositionServiceBeanTest`, 18 testes
- [x] 5.2 Unit test do método público de Employee (`existsActiveEmployeeInPosition`) — cenários com funcionário ativo e sem vínculo ativo — `EmployeePositionQueryServiceBeanTest`, 2 testes
- [ ] 5.3 Teste de integração com Testcontainers via `/testcontainers-integration` cobrindo os 6 endpoints fim a fim (`POST → GET → PUT → enable/disable`), incluindo o bloqueio de disable por funcionário ativo vinculado — **ADIADO** (decisão do usuário em 2026-07-05): nenhum módulo do projeto tem dependência de Testcontainers hoje; introduzir essa infra é decisão maior, tratada como change/tarefa futura separada, fora do escopo de `crud-cargo-position`

## 6. Validação

- [x] 6.1 Verificar que `GET /v1/positions/{id}` e `GET /v1/positions` retornam o objeto `department` completo aninhado — coberto por `PositionServiceBeanTest` (`create`/`findAll` verificam `result.department()`); sem teste de integração HTTP ainda (depende de 5.3)
- [x] 6.2 Verificar que `PUT /v1/positions/{id}` com `departmentId` igual ao atual, mas departamento desativado nesse meio tempo, retorna `422 SCOS_DEPARTMENT_006` — coberto por `updateShouldRevalidateDepartmentEvenWhenUnchanged` em `PositionServiceBeanTest`
- [ ] 6.3 Verificar que `PositionServiceBean`/`EmployeeQueryRepository` não introduzem N+1 na listagem paginada (validar plano de query ou contagem de queries no teste de integração) — adiado junto com 5.3

## 7. Fix — filtro `active` ignorado em GET /v1/positions e GET /v1/departments

- [x] 7.1 `PositionRepository.findAllFiltered` passa a receber `Boolean active` além de `departmentId`, aplicando `qPosition.active.eq(active)` na `BooleanBuilder` só quando não-nulo
- [x] 7.2 `PositionService`/`PositionServiceBean`/`FindAllPositionUseCase`/`FindAllPositionUseCaseBean`/`PositionDelegate.getAllPositions` propagam `active` ponta a ponta
- [x] 7.3 `DepartmentRepository`: removida a `@Query` JPQL de `findAll(Pageable)` que projetava direto pra `DepartmentOutput`; agora retorna `Page<Department>` (entidade) e ganhou `findAllFiltered(Boolean active, Pageable pageable)` via QueryDSL, no mesmo padrão de Position
- [x] 7.4 `DepartmentService`/`DepartmentServiceBean`/`FindAllDepartmentUseCase`/`FindAllDepartmentUseCaseBean`/`DepartmentDelegate.getAllDepartments` propagam `active` ponta a ponta; `DepartmentServiceBean.findAll` passou a mapear `Page<Department> → Page<DepartmentOutput>` via `DepartmentMapper` (antes vinha pronto da query JPQL)

## 8. Testes — Department

- [x] 8.1 Unit tests de `DepartmentServiceBean` (mesmo padrão de `PositionServiceBeanTest`) cobrindo create/update/findById/findAll (sem filtro e com filtro `active`)/enable/disable — `DepartmentServiceBeanTest`, 18 testes
