## ADDED Requirements

### Requirement: CRUD de referência ReasonPositionChange
O sistema SHALL prover um catálogo de referência `ReasonPositionChange` com os campos `code`, `description` e `active`, sem `entityType` (aplica-se exclusivamente a `EMPLOYEE`).

#### Scenario: Criar motivo de mudança de cargo
- **WHEN** um cliente autorizado envia `POST /v1/reason-position-change` com `code` e `description` válidos
- **THEN** o sistema cria o registro e retorna `201 Created`

#### Scenario: Listar motivos com paginação
- **WHEN** um cliente autorizado envia `GET /v1/reason-position-change` com `paginationFilter`
- **THEN** o sistema retorna `200 OK` com `GetAllReasonPositionChangeResponse` (`data: array`, `paginatedDTO`)

#### Scenario: Desativar motivo de mudança de cargo
- **WHEN** um cliente autorizado envia `PUT /v1/reason-position-change/{id}/disable`
- **THEN** o sistema marca o registro como `active = false`

### Requirement: Autorização do catálogo ReasonPositionChange
Cada operação de `ReasonPositionChange` SHALL exigir a permissão correspondente em `ScosOrganizationPermission`: `GET_REASON_POSITION_CHANGE`, `CREATE_REASON_POSITION_CHANGE`, `UPDATE_REASON_POSITION_CHANGE`, `ENABLE_REASON_POSITION_CHANGE`, `DISABLE_REASON_POSITION_CHANGE`.

#### Scenario: Requisição sem permissão é rejeitada
- **WHEN** um cliente sem a permissão `CREATE_REASON_POSITION_CHANGE` envia `POST /v1/reason-position-change`
- **THEN** o sistema retorna `4XX` de autorização

### Requirement: transfer exige reasonPositionChangeId
`TransferEmployeeRequest` (`PATCH /v1/employees/{id}/transfer`) SHALL exigir `reasonPositionChangeId` sempre, independente de `positionId` ter sido informado ou não. Toda chamada bem-sucedida a `/transfer` SHALL inserir uma nova linha em `EmployeePositionHistory`, fechando a linha anteriormente aberta e abrindo outra com o `positionId` vigente (o novo, se informado; o mesmo de antes, caso contrário) e o `reasonPositionChangeId` informado.

#### Scenario: transfer sem reasonPositionChangeId é rejeitado
- **WHEN** um cliente autorizado envia `PATCH /v1/employees/{id}/transfer` sem `reasonPositionChangeId`
- **THEN** o sistema retorna `4XX`

#### Scenario: transfer de cargo gera histórico
- **WHEN** um cliente autorizado envia `PATCH /v1/employees/{id}/transfer` com `positionId` diferente do atual e `reasonPositionChangeId` válido
- **THEN** o sistema atualiza `Employee.positionId` e insere uma nova linha em `EmployeePositionHistory` com o novo `positionId` e o motivo informado

#### Scenario: transfer sem mudar positionId também gera histórico
- **WHEN** um cliente autorizado envia `PATCH /v1/employees/{id}/transfer` só com `companyId` ou `supervisorId` (sem `positionId`), e `reasonPositionChangeId` válido
- **THEN** o sistema insere uma nova linha em `EmployeePositionHistory` repetindo o `positionId` vigente, com o motivo informado

### Requirement: Consulta paginada ao histórico de cargo
O sistema SHALL prover `GET /v1/employees/{id}/position-history`, paginado, retornando `positionId`, `startDate`, `endDate` (nulo indica a linha atual), `reasonPositionChangeId`, `createdAt` e `userAt` de cada linha de `EmployeePositionHistory` do funcionário.

#### Scenario: Consultar histórico de cargo
- **WHEN** um cliente autorizado envia `GET /v1/employees/{id}/position-history` com `paginationFilter`
- **THEN** o sistema retorna `200 OK` com as linhas de histórico do funcionário, ordenadas do mais recente para o mais antigo

#### Scenario: Linha atual tem endDate nulo
- **WHEN** um cliente autorizado consulta `GET /v1/employees/{id}/position-history` de um funcionário sem transferências pendentes de fechamento
- **THEN** exatamente uma linha do retorno tem `endDate` nulo, representando a atribuição de cargo vigente

### Requirement: Autorização da consulta de histórico de cargo
`GET /v1/employees/{id}/position-history` SHALL exigir a permissão `GET_EMPLOYEE_POSITION_HISTORY` em `ScosOrganizationPermission`.

#### Scenario: Requisição sem permissão é rejeitada
- **WHEN** um cliente sem a permissão `GET_EMPLOYEE_POSITION_HISTORY` envia `GET /v1/employees/{id}/position-history`
- **THEN** o sistema retorna `4XX` de autorização
