## MODIFIED Requirements

### Requirement: StatusEmployee com valores corretos
O enum `StatusEmployee` SHALL conter exatamente os valores `ACTIVE`, `INACTIVE` e `DISABLED` — o valor `DELETED` não existe mais, pois viola o `CHECK` de vocabulário fechado do schema v2.

#### Scenario: Enum com três valores
- **WHEN** o enum `StatusEmployee` é inspecionado
- **THEN** deve conter os valores `ACTIVE`, `INACTIVE` e `DISABLED` e nenhum outro

#### Scenario: Método delete ausente
- **WHEN** a entidade `Employee` é inspecionada
- **THEN** não deve existir método `delete()`

### Requirement: Employee com métodos de negócio baseados em histórico de status
Os métodos `activate(reasonActivateId)`, `inactivate(reasonInactivateId)`, `disable(reasonDisableId)` e `enable(reasonEnableId)` da entidade `Employee` SHALL validar a transição client-side e retornar uma instância não persistida de `EmployeeStatusHistory` em vez de mutar `status` diretamente.

#### Scenario: activate retorna histórico sem mutar status
- **WHEN** `employee.activate(reasonActivateId)` é chamado com `status == INACTIVE`
- **THEN** retorna `EmployeeStatusHistory` com `status = ACTIVE` e `reasonActivateId` preenchido

#### Scenario: inactivate a partir de ACTIVE ou DISABLED
- **WHEN** `employee.inactivate(reasonInactivateId)` é chamado com `status == ACTIVE` ou `status == DISABLED`
- **THEN** retorna `EmployeeStatusHistory` com `status = INACTIVE` e `reasonInactivateId` preenchido

#### Scenario: enable exige status DISABLED
- **WHEN** `employee.enable(reasonEnableId)` é chamado com `status != DISABLED`
- **THEN** lança `ScosException`

### Requirement: EmployeeAddress com auditRead
A entidade `EmployeeAddress` SHALL usar `@Auditable(auditRead = true)` por conter geolocalização e endereço residencial. O campo `number` SHALL ser do tipo `int` (não `long`), alinhado ao tipo `INT` da coluna no banco.

#### Scenario: Anotação auditRead presente em EmployeeAddress
- **WHEN** a entidade `EmployeeAddress` é inspecionada
- **THEN** deve estar anotada com `@Auditable(auditRead = true)`

#### Scenario: number como int
- **WHEN** o campo `number` de `EmployeeAddress` é inspecionado
- **THEN** deve ser do tipo `int`, não `long`

## ADDED Requirements

### Requirement: Employee com tipo de contrato e período de experiência
A entidade `Employee` SHALL incluir os campos `contractType` (novo enum `EmployeeContractType` com valores `CLT`, `PJ`, `ESTAGIO`, `TEMPORARIO`, `@Enumerated(EnumType.STRING)`, coluna `CONTRACT_TYPE`) e `probationEndDate` (`PROBATION_END_DATE`, nullable).

#### Scenario: EmployeeContractType com quatro valores
- **WHEN** o enum `EmployeeContractType` é inspecionado
- **THEN** deve conter exatamente `CLT`, `PJ`, `ESTAGIO`, `TEMPORARIO`

#### Scenario: Campos presentes em Employee
- **WHEN** a entidade `Employee` é inspecionada
- **THEN** deve conter `contractType` (`EmployeeContractType`) e `probationEndDate` (nullable)

### Requirement: Position com indicador de cargo de confiança
A entidade `Position` SHALL incluir o campo `isTrustPosition` (`IS_TRUST_POSITION`, `boolean`, default `false`).

#### Scenario: Campo presente em Position
- **WHEN** a entidade `Position` é inspecionada
- **THEN** deve conter o campo `isTrustPosition` do tipo `boolean`
