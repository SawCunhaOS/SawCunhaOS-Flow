## ADDED Requirements

### Requirement: Enum EntityType compartilhado
O sistema SHALL fornecer um enum `EntityType` com os valores `COMPANY`, `EMPLOYEE` e `LOGIN`, compartilhado pelas quatro tabelas de motivo (`ReasonActivate`, `ReasonInactivate`, `ReasonDisable`, `ReasonEnable`).

#### Scenario: Enum com três valores
- **WHEN** o enum `EntityType` é inspecionado
- **THEN** deve conter exatamente `COMPANY`, `EMPLOYEE`, `LOGIN`

### Requirement: Entidades de motivo pré-cadastrado
O sistema SHALL fornecer as entidades `ReasonActivate`, `ReasonInactivate`, `ReasonDisable` e `ReasonEnable`, cada uma estendendo `BaseEntity`, anotada com `@Auditable`, com campos `code` (`CODE`), `description` (`DESCRIPTION`), `entityType` (`ENTITY_TYPE`, `@Enumerated(EnumType.STRING)`) e `active` (`ACTIVE`).

#### Scenario: Entidade estende BaseEntity
- **WHEN** qualquer uma das quatro entidades de motivo é inspecionada
- **THEN** deve estender `BaseEntity` e estar anotada com `@Auditable`

#### Scenario: Repositórios criados
- **WHEN** o módulo é inspecionado
- **THEN** deve existir `ReasonActivateRepository`, `ReasonInactivateRepository`, `ReasonDisableRepository` e `ReasonEnableRepository`, cada um estendendo `BaseJpaRepository<T, Long>` + `JpaSpecificationExecutor<T>` + `QuerydslPredicateExecutor<T>`

### Requirement: Entidades de histórico de status imutáveis
O sistema SHALL fornecer `CompanyStatusHistory`, `EmployeeStatusHistory` e `LoginStatusHistory` — entidades imutáveis (sem `UPDATED_AT`), sem estender `BaseEntity`, com `createdAt` (`@CreationTimestamp`) e `userAt` declarados diretamente (padrão `ProfileResource` adaptado a PK simples). Cada uma tem FK para a entidade dona, `status`, `previousStatus` (nullable — preenchido por trigger de banco, nunca setado pela aplicação) e as quatro FKs de motivo (`reasonActivateId`, `reasonInactivateId`, `reasonDisableId`, `reasonEnableId`), todas nullable.

#### Scenario: Entidade não estende BaseEntity
- **WHEN** qualquer uma das três entidades de histórico é inspecionada
- **THEN** não deve estender `BaseEntity`; deve ter `createdAt` com `@CreationTimestamp` e `userAt` direto

#### Scenario: previousStatus nunca setado pela aplicação
- **WHEN** a entidade de histórico é construída pela aplicação antes de persistir
- **THEN** o campo `previousStatus` permanece `null` — é responsabilidade da trigger de banco preenchê-lo

#### Scenario: Repositórios criados
- **WHEN** o módulo é inspecionado
- **THEN** deve existir `CompanyStatusHistoryRepository`, `EmployeeStatusHistoryRepository` e `LoginStatusHistoryRepository`, cada um estendendo `BaseJpaRepository<T, Long>` + `JpaSpecificationExecutor<T>` + `QuerydslPredicateExecutor<T>`

### Requirement: Contrato de transição de status via histórico
Os métodos de transição de status de `Company`, `Employee` e `Login` (`activate`, `inactivate`, `disable`, `enable`) SHALL retornar uma instância não persistida do histórico correspondente em vez de mutar `status` diretamente, validando a transição client-side conforme a tabela abaixo (`Login` usa `BLOCKED` no lugar de `DISABLED`):

| Método | Transição válida | Motivo exigido |
|---|---|---|
| `activate(reasonActivateId)` | `INACTIVE → ACTIVE` | `reasonActivateId` |
| `inactivate(reasonInactivateId)` | `ACTIVE\|DISABLED/BLOCKED → INACTIVE` | `reasonInactivateId` |
| `disable(reasonDisableId)` | `ACTIVE → DISABLED/BLOCKED` | `reasonDisableId` |
| `enable(reasonEnableId)` | `DISABLED/BLOCKED → ACTIVE` | `reasonEnableId` |

#### Scenario: activate a partir de INACTIVE
- **WHEN** `company.activate(reasonActivateId)` é chamado com `status == INACTIVE`
- **THEN** retorna um `CompanyStatusHistory` não persistido com `status = ACTIVE` e `reasonActivateId` preenchido; `company.status` permanece inalterado em memória

#### Scenario: activate a partir de estado inválido lança exceção
- **WHEN** `company.activate(reasonActivateId)` é chamado com `status != INACTIVE`
- **THEN** lança `ScosException`

#### Scenario: disable exige status ACTIVE
- **WHEN** `employee.disable(reasonDisableId)` é chamado com `status != ACTIVE`
- **THEN** lança `ScosException`

#### Scenario: enable a partir de DISABLED/BLOCKED
- **WHEN** `login.enable(reasonEnableId)` é chamado com `status == BLOCKED`
- **THEN** retorna um `LoginStatusHistory` não persistido com `status = ACTIVE` e `reasonEnableId` preenchido

#### Scenario: enable a partir de estado inválido lança exceção
- **WHEN** `login.enable(reasonEnableId)` é chamado com `status != BLOCKED`
- **THEN** lança `ScosException`
