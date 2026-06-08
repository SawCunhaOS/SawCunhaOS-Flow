## ADDED Requirements

### Requirement: URIs de recursos raiz de coleções no plural
O sistema SHALL expor todos os recursos raiz que representam coleções usando substantivos no plural na URI. Especificamente: `companies`, `employees`, `profiles`. Os recursos `departments`, `positions` e `features` já estão corretos e não mudam.

#### Scenario: Coleção de companies acessível no plural
- **WHEN** `GET /v1/companies` é requisitado
- **THEN** o sistema retorna `200 OK` com a lista de companies

#### Scenario: URI singular de company não é acessível
- **WHEN** `GET /v1/company` é requisitado
- **THEN** o sistema retorna `404 Not Found`

#### Scenario: Coleção de employees acessível no plural
- **WHEN** `GET /v1/employees` é requisitado
- **THEN** o sistema retorna `200 OK` com a lista de employees

#### Scenario: URI singular de employee não é acessível
- **WHEN** `GET /v1/employee` é requisitado
- **THEN** o sistema retorna `404 Not Found`

#### Scenario: Coleção de profiles acessível no plural
- **WHEN** `GET /v1/profiles` é requisitado
- **THEN** o sistema retorna `200 OK` com a lista de profiles

#### Scenario: URI singular de profile não é acessível
- **WHEN** `GET /v1/profile` é requisitado
- **THEN** o sistema retorna `404 Not Found`

### Requirement: Sub-recursos de coleções no plural
O sistema SHALL usar substantivos no plural para todos os sub-recursos que representam coleções: `contacts`, `addresses`, `logins`.

#### Scenario: Contatos de company acessíveis no plural
- **WHEN** `GET /v1/companies/{companyId}/contacts` é requisitado com `companyId` válido
- **THEN** o sistema retorna `200 OK` com a lista de contatos

#### Scenario: URI singular de contato de company não é acessível
- **WHEN** `GET /v1/companies/{companyId}/contact` é requisitado
- **THEN** o sistema retorna `404 Not Found`

#### Scenario: Endereços de employee acessíveis no plural
- **WHEN** `GET /v1/employees/{employeeId}/addresses` é requisitado com `employeeId` válido
- **THEN** o sistema retorna `200 OK` com a lista de endereços

#### Scenario: Logins de employee acessíveis no plural
- **WHEN** `GET /v1/employees/{employeeId}/logins` é requisitado com `employeeId` válido
- **THEN** o sistema retorna `200 OK` com a lista de logins

### Requirement: Schemas de request com sufixo Request
O sistema SHALL nomear todos os schemas de entrada (request body) com o sufixo `Request`. Schemas de Profile e Login atualmente nomeados com `*DTO` SHALL ser renomeados.

#### Scenario: Schema de criação de profile com sufixo correto
- **WHEN** a spec OpenAPI `ScosOrganization_Login.yml` é inspecionada
- **THEN** o schema de criação de profile se chama `CreateProfileRequest` (não `CreateProfileDTO`)

#### Scenario: Schema de atualização de login com sufixo correto
- **WHEN** a spec OpenAPI `ScosOrganization_Login.yml` é inspecionada
- **THEN** o schema de atualização de login se chama `UpdateEmployeeLoginRequest` (não `UpdateEmployeeLoginDTO`)

#### Scenario: Referência interna de criação de login consistente
- **WHEN** a spec OpenAPI `ScosOrganization_Login.yml` é inspecionada
- **THEN** a operação `createEmployeeLogin` referencia `CreateEmployeeLoginRequest` e esse schema existe com esse nome exato

### Requirement: Schemas de response com sufixo Response
O sistema SHALL nomear todos os schemas de resposta com o sufixo `Response`. Schemas de Profile e Login atualmente nomeados com `*DTO` SHALL ser renomeados.

#### Scenario: Schema de busca de profile com sufixo correto
- **WHEN** a spec OpenAPI `ScosOrganization_Login.yml` é inspecionada
- **THEN** o schema de resposta de profile por id se chama `GetProfileResponse` (não `GetProfileDTO`)

#### Scenario: Schema de listagem de login com sufixo correto
- **WHEN** a spec OpenAPI `ScosOrganization_Login.yml` é inspecionada
- **THEN** o schema de listagem de logins se chama `GetAllEmployeeLoginsResponse` (não `GetAllEmployeeLoginDTO`)

### Requirement: LoginStatus representa estado, não ação
O sistema SHALL usar o valor `ENABLED` (past participle) no enum `LoginStatus` para representar o estado ativo de um login. O valor `ENABLE` (verbo no infinitivo) não SHALL existir.

#### Scenario: Enum LoginStatus contém ENABLED
- **WHEN** o enum `LoginStatus` na spec é inspecionado
- **THEN** contém o valor `ENABLED`

#### Scenario: Enum LoginStatus não contém ENABLE
- **WHEN** o enum `LoginStatus` na spec é inspecionado
- **THEN** não contém o valor `ENABLE`

#### Scenario: Consistência dos demais valores do enum
- **WHEN** o enum `LoginStatus` na spec é inspecionado
- **THEN** contém exatamente: `ENABLED`, `BLOCKED`, `INACTIVE`, `PENDING`, `PENDING_PASSWORD_CHANGE`

### Requirement: Endpoint de informação de login sem colisão de path
O sistema SHALL expor as informações do login do usuário autenticado em `GET /v1/logins/me`, fora da hierarquia `/employees/{id}`, evitando ambiguidade de path.

#### Scenario: Endpoint de login info acessível em /v1/logins/me
- **WHEN** `GET /v1/logins/me` é requisitado com token JWT válido
- **THEN** o sistema retorna `200 OK` com as informações do login autenticado

#### Scenario: Endpoint antigo não é acessível
- **WHEN** `GET /v1/employee/login/info` é requisitado
- **THEN** o sistema retorna `404 Not Found`

#### Scenario: Identity extraída do JWT, não do path
- **WHEN** `GET /v1/logins/me` é requisitado sem employeeId no path
- **THEN** o sistema usa o subject do JWT para identificar o usuário e retorna os dados corretos
