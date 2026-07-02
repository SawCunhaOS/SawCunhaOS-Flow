## ADDED Requirements

### Requirement: Position expõe isTrustPosition
`Position`, `CreatePositionRequest` e `UpdatePositionRequest` SHALL expor o campo `isTrustPosition` (boolean, default `false`), correspondente à coluna `IS_TRUST_POSITION` de `SCOS_POSITION`.

#### Scenario: Criar cargo sem informar isTrustPosition
- **WHEN** um cliente autorizado envia `POST /v1/positions` sem `isTrustPosition`
- **THEN** o sistema cria o cargo com `isTrustPosition` igual a `false`

#### Scenario: Criar cargo de confiança
- **WHEN** um cliente autorizado envia `POST /v1/positions` com `isTrustPosition` igual a `true`
- **THEN** o sistema cria o cargo com `isTrustPosition` igual a `true`

#### Scenario: GetPositionResponse retorna isTrustPosition
- **WHEN** um cliente autorizado envia `GET /v1/positions/{id}`
- **THEN** a resposta inclui o campo `isTrustPosition`

### Requirement: Employee expõe contractType e probationEndDate
`Employee`, `CreateEmployeeRequest` e `UpdateEmployeeRequest` SHALL expor `contractType` (enum `EmployeeContractType`: `CLT`, `PJ`, `ESTAGIO`, `TEMPORARIO` — obrigatório) e `probationEndDate` (date, opcional), correspondentes às colunas `CONTRACT_TYPE` e `PROBATION_END_DATE` de `SCOS_EMPLOYEE`.

#### Scenario: Criar funcionário sem contractType é rejeitado
- **WHEN** um cliente autorizado envia `POST /v1/employees` sem `contractType`
- **THEN** o sistema retorna `4XX`

#### Scenario: Criar funcionário CLT com probationEndDate
- **WHEN** um cliente autorizado envia `POST /v1/employees` com `contractType` igual a `CLT` e `probationEndDate` preenchido
- **THEN** o sistema cria o funcionário persistindo os dois campos

#### Scenario: Criar funcionário sem probationEndDate
- **WHEN** um cliente autorizado envia `POST /v1/employees` com `contractType` preenchido e sem `probationEndDate`
- **THEN** o sistema cria o funcionário com `probationEndDate` nulo

#### Scenario: contractType inválido é rejeitado
- **WHEN** um cliente autorizado envia `POST /v1/employees` ou `PUT /v1/employees/{id}` com `contractType` fora do vocabulário `CLT`/`PJ`/`ESTAGIO`/`TEMPORARIO`
- **THEN** o sistema retorna `4XX`

#### Scenario: GetEmployeeResponse retorna contractType e probationEndDate
- **WHEN** um cliente autorizado envia `GET /v1/employees/{id}`
- **THEN** a resposta `Employee` inclui `contractType` e `probationEndDate`
