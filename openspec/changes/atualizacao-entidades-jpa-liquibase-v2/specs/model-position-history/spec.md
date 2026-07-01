## ADDED Requirements

### Requirement: Enum DayOfWeek compartilhado
O sistema SHALL fornecer um enum `DayOfWeek` com os valores `MONDAY` a `SUNDAY`, compartilhado por `PositionWorkSchedule` e `EmployeeWorkSchedule`.

#### Scenario: Enum com sete valores
- **WHEN** o enum `DayOfWeek` é inspecionado
- **THEN** deve conter exatamente `MONDAY`, `TUESDAY`, `WEDNESDAY`, `THURSDAY`, `FRIDAY`, `SATURDAY`, `SUNDAY`

### Requirement: Entidade ReasonPositionChange
O sistema SHALL fornecer a entidade `ReasonPositionChange`, estendendo `BaseEntity`, anotada com `@Auditable`, com campos `code`, `description` e `active` — sem `entityType` (motivo se aplica apenas a `EMPLOYEE`, não é compartilhado).

#### Scenario: Entidade sem campo entityType
- **WHEN** a entidade `ReasonPositionChange` é inspecionada
- **THEN** não deve existir campo `entityType`

#### Scenario: Repositório criado
- **WHEN** o módulo é inspecionado
- **THEN** deve existir `ReasonPositionChangeRepository` estendendo `BaseJpaRepository<ReasonPositionChange, Long>` + `JpaSpecificationExecutor` + `QuerydslPredicateExecutor`

### Requirement: Entidade EmployeePositionHistory imutável
O sistema SHALL fornecer `EmployeePositionHistory` — imutável (sem `UPDATED_AT`), sem estender `BaseEntity`, com `createdAt` e `userAt` diretos. Campos: FK `employee`, FK `position`, `startDate`, `endDate` (nullable — `NULL` = cargo atual), FK `reasonPositionChange` (`NOT NULL`).

#### Scenario: Entidade não estende BaseEntity
- **WHEN** a entidade `EmployeePositionHistory` é inspecionada
- **THEN** não deve estender `BaseEntity`; deve ter `createdAt` com `@CreationTimestamp` e `userAt` direto

#### Scenario: endDate nullable
- **WHEN** um registro de histórico representa o cargo atual do funcionário
- **THEN** `endDate` é `null`

#### Scenario: Repositório criado
- **WHEN** o módulo é inspecionado
- **THEN** deve existir `EmployeePositionHistoryRepository` estendendo `BaseJpaRepository<EmployeePositionHistory, Long>` + `JpaSpecificationExecutor` + `QuerydslPredicateExecutor`

### Requirement: Entidades PositionWorkSchedule e EmployeeWorkSchedule
O sistema SHALL fornecer `PositionWorkSchedule` (template de horário por cargo) e `EmployeeWorkSchedule` (horário efetivo do funcionário, cópia editável do template) — ambas estendendo `BaseEntity` (têm `UPDATED_AT`), anotadas com `@Auditable`, com FK dona (`position`/`employee`), `dayOfWeek` (`@Enumerated(EnumType.STRING)`), `startTime`, `lunchStart`, `lunchEnd`, `endTime` (todos `LocalTime`).

#### Scenario: Entidades estendem BaseEntity
- **WHEN** `PositionWorkSchedule` ou `EmployeeWorkSchedule` são inspecionadas
- **THEN** ambas devem estender `BaseEntity` e estar anotadas com `@Auditable`

#### Scenario: Repositórios criados
- **WHEN** o módulo é inspecionado
- **THEN** deve existir `PositionWorkScheduleRepository` e `EmployeeWorkScheduleRepository`, cada um estendendo `BaseJpaRepository<T, Long>` + `JpaSpecificationExecutor<T>` + `QuerydslPredicateExecutor<T>`
