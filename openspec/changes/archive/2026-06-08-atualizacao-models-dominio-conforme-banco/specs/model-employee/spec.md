## ADDED Requirements

### Requirement: Employee mapeado conforme schema do banco
A entidade `Employee` SHALL mapear exatamente as colunas da tabela `SCOS_EMPLOYEE` definidas no Liquibase. Os campos `dateCreated` (`DATE_CREATED`) e `active` (`ACTIVE`) não existem no banco e MUST ser removidos. O campo `STATUS` (`VARCHAR(20)`) existe no banco e MUST ser mapeado como `StatusEmployee` com `@Enumerated(EnumType.STRING)`.

#### Scenario: Campos obsoletos removidos
- **WHEN** a entidade `Employee` é inspecionada
- **THEN** os campos `dateCreated` e `active` não devem existir na classe

#### Scenario: Campo status presente e tipado
- **WHEN** a entidade `Employee` é inspecionada
- **THEN** deve existir campo `status` do tipo `StatusEmployee` anotado com `@Enumerated(EnumType.STRING)` mapeando coluna `STATUS`

### Requirement: StatusEmployee com valores corretos
O enum `StatusEmployee` SHALL conter exatamente os valores `ACTIVE`, `INACTIVE` e `DISABLED`, seguindo o padrão de enum do projeto com `@Getter` e `displayName`.

#### Scenario: Enum com três valores
- **WHEN** o enum `StatusEmployee` é inspecionado
- **THEN** deve conter os valores `ACTIVE`, `INACTIVE` e `DISABLED` e nenhum outro

### Requirement: Employee com métodos de negócio baseados em status
A entidade `Employee` SHALL fornecer métodos `activate()`, `inactivate()` e `disable()` para transição de status, substituindo o campo `active` removido.

#### Scenario: Ativar employee
- **WHEN** `activate()` é chamado
- **THEN** `status` é definido como `StatusEmployee.ACTIVE`

#### Scenario: Inativar employee
- **WHEN** `inactivate()` é chamado
- **THEN** `status` é definido como `StatusEmployee.INACTIVE`

#### Scenario: Desabilitar employee
- **WHEN** `disable()` é chamado
- **THEN** `status` é definido como `StatusEmployee.DISABLED`

### Requirement: Employee com auditoria de leitura LGPD habilitada
A entidade `Employee` SHALL ser anotada com `@Auditable(auditRead = true)` para rastrear acessos de leitura em conformidade com a LGPD, dado que contém CPF, e-mail, nome e data de nascimento.

#### Scenario: Anotação auditRead presente
- **WHEN** a entidade `Employee` é inspecionada
- **THEN** deve estar anotada com `@Auditable(auditRead = true)`

### Requirement: EmployeeContact com PK correta e auditRead
A entidade `EmployeeContact` SHALL mapear a coluna PK como `EMPLOYEE_ID_CONTACT` (não `CONTACT_ID`) e SHALL usar `@Auditable(auditRead = true)` por conter telefone pessoal.

#### Scenario: Nome da coluna PK correto
- **WHEN** a entidade `EmployeeContact` é inspecionada
- **THEN** a coluna PK deve ser nomeada `EMPLOYEE_ID_CONTACT`

### Requirement: EmployeeAddressPk com nome de coluna correto
A classe `EmployeeAddressPk` SHALL mapear o campo de ID externo como `employeeIdAddress` com coluna `EMPLOYEE_ID_ADDRESS` (não `addressId` / `ADDRESS_ID`).

#### Scenario: Coluna do ID externo correta
- **WHEN** a classe `EmployeeAddressPk` é inspecionada
- **THEN** o campo de ID externo deve ser `employeeIdAddress` mapeando coluna `EMPLOYEE_ID_ADDRESS`

### Requirement: EmployeeAddress com auditRead
A entidade `EmployeeAddress` SHALL usar `@Auditable(auditRead = true)` por conter geolocalização e endereço residencial.

#### Scenario: Anotação auditRead presente em EmployeeAddress
- **WHEN** a entidade `EmployeeAddress` é inspecionada
- **THEN** deve estar anotada com `@Auditable(auditRead = true)`
