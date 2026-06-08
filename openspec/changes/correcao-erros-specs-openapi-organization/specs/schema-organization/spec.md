## ADDED Requirements

### Requirement: OpenAPI Employee — nomenclatura de schemas de entrada
Todos os schemas de input (requestBody) no contrato OpenAPI de Employee SHALL usar o sufixo `*Request`. O schema `UpdateEmployeeDTO` SHALL ser renomeado para `UpdateEmployeeRequest`.

#### Scenario: Schema de atualização de employee com nome correto
- **WHEN** spec `ScosOrganization_Employee.yml` é carregada
- **THEN** schema `UpdateEmployeeRequest` existe e `UpdateEmployeeDTO` não existe

#### Scenario: updateEmployee referencia schema correto
- **WHEN** `PUT /v1/employee/{id}` é processado pelo openapi-generator
- **THEN** requestBody referencia `UpdateEmployeeRequest`

### Requirement: OpenAPI Employee — referências de schema corretas nos endpoints
Cada endpoint de atualização de Employee SHALL referenciar o schema correspondente ao seu recurso.

#### Scenario: updateEmployeeContact referencia UpdateEmployeeContactRequest
- **WHEN** spec `ScosOrganization_Employee.yml` é carregada
- **THEN** `PUT /v1/employee/{employeeId}/contact/{id}` requestBody referencia `UpdateEmployeeContactRequest`

#### Scenario: updateEmployeeAddress referencia UpdateEmployeeAddressRequest
- **WHEN** spec `ScosOrganization_Employee.yml` é carregada
- **THEN** `PUT /v1/employee/{employeeId}/address/{id}` requestBody referencia `UpdateEmployeeAddressRequest`

### Requirement: OpenAPI Login — operationId e metadata corretos
O endpoint `POST /v1/employee/{employeeId}/login` SHALL ter `operationId: createEmployeeLogin`, `summary: Create employee login` e `description: Create employee login`.

#### Scenario: operationId correto no POST login
- **WHEN** spec `ScosOrganization_Login.yml` é carregada
- **THEN** `POST /v1/employee/{employeeId}/login` tem `operationId: createEmployeeLogin`

### Requirement: OpenAPI Login — parâmetros de path coerentes com URI
Parâmetros declarados como `in: path` SHALL existir como variável `{param}` na URI do endpoint correspondente.

#### Scenario: GET /login/info sem params de path inválidos
- **WHEN** spec `ScosOrganization_Login.yml` é carregada
- **THEN** `GET /v1/employee/login/info` não declara `idEmployee` nem `idRequest` como parâmetros

#### Scenario: DELETE /profile/{id} sem param idEmployee
- **WHEN** spec `ScosOrganization_Login.yml` é carregada
- **THEN** `DELETE /v1/profile/{id}` não declara `idEmployee` como parâmetro

### Requirement: OpenAPI — servers.url uniforme no módulo Organization
Todos os arquivos de spec do módulo Organization SHALL usar `servers.url: /organization/api` (com barra inicial).

#### Scenario: Employee.yml com server URL correto
- **WHEN** spec `ScosOrganization_Employee.yml` é carregada
- **THEN** `servers[0].url` é `/organization/api`

#### Scenario: Department-Position.yml com server URL correto
- **WHEN** spec `ScosOrganization_Department-Position.yml` é carregada
- **THEN** `servers[0].url` é `/organization/api`

#### Scenario: Login.yml com server URL correto
- **WHEN** spec `ScosOrganization_Login.yml` é carregada
- **THEN** `servers[0].url` é `/organization/api`
