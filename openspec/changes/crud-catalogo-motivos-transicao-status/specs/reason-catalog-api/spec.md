## ADDED Requirements

### Requirement: Camada de código dos 4 catálogos de motivo
O sistema SHALL implementar a camada de código (delegate, use case, domain service, repositório) para os 24 endpoints dos 4 catálogos `ReasonActivate`, `ReasonInactivate`, `ReasonDisable` e `ReasonEnable`, cada um expondo `list`/`create`/`get`/`update`/`enable`/`disable` sobre o schema `{ code, description, entityType, active }` já definido pelo contrato OpenAPI.

#### Scenario: Endpoint de criação funcional end-to-end
- **WHEN** um cliente autorizado envia `POST /v1/reason-disable` com `code`, `description` e `entityType` válidos
- **THEN** o `ReasonDisableDelegate` aciona o Use Case, que persiste via `ReasonDisableServiceBean`/`Repository` e retorna `201 Created`

#### Scenario: Endpoint de listagem paginada funcional end-to-end
- **WHEN** um cliente autorizado envia `GET /v1/reason-enable` com `paginationFilter`
- **THEN** o `ReasonEnableDelegate` retorna `200 OK` com `data: array` e `paginatedDTO`, populados a partir do repositório

### Requirement: Unicidade de code escopada por catálogo e por entityType
Cada um dos 4 catálogos SHALL validar unicidade de `code` de forma independente (4 tabelas próprias) e escopada por `entityType`: `create` verifica `existsByCodeAndEntityType`, `update` verifica `existsByCodeAndEntityTypeAndNotId` excluindo o próprio `{id}`. `code` e `entityType` juntos formam a chave natural — o mesmo `code` é permitido em `entityType`s diferentes dentro do mesmo catálogo.

#### Scenario: Code duplicado no mesmo catálogo e mesmo entityType é rejeitado
- **WHEN** um cliente autorizado envia `POST /v1/reason-activate` com um `code` já cadastrado em `ReasonActivate` para o mesmo `entityType`
- **THEN** o sistema retorna `409 Conflict` com `SCOS_REASON_ACTIVATE_002`

#### Scenario: Mesmo code em catálogos diferentes é permitido
- **WHEN** um `code` já existe em `ReasonActivate` e o mesmo valor é enviado em `POST /v1/reason-disable`
- **THEN** o sistema cria o registro em `ReasonDisable` normalmente, pois a unicidade não é cruzada entre catálogos

#### Scenario: Mesmo code em entityType diferente no mesmo catálogo é permitido
- **WHEN** um `code` já existe em `ReasonActivate` com `entityType=COMPANY` e o mesmo `code` é enviado em `POST /v1/reason-activate` com `entityType=LOGIN`
- **THEN** o sistema cria o segundo registro normalmente, pois `code` e `entityType` juntos formam a chave de unicidade

#### Scenario: Update não conflita consigo mesmo
- **WHEN** um cliente autorizado envia `PUT /v1/reason-inactivate/{id}` mantendo o mesmo `code`/`entityType` já pertencentes a `{id}`
- **THEN** o sistema aceita a atualização, pois `existsByCodeAndEntityTypeAndNotId` exclui o próprio registro da checagem

### Requirement: Enable/disable idempotente por catálogo
`PUT .../{id}/enable` e `PUT .../{id}/disable` SHALL retornar `422` quando o registro já está no estado alvo, para cada um dos 4 catálogos.

#### Scenario: Ativar motivo já ativo é rejeitado
- **WHEN** um cliente autorizado envia `PUT /v1/reason-enable/{id}/enable` para um registro com `active=true`
- **THEN** o sistema retorna `422` com `SCOS_REASON_ENABLE_003`

#### Scenario: Desativar motivo já inativo é rejeitado
- **WHEN** um cliente autorizado envia `PUT /v1/reason-activate/{id}/disable` para um registro com `active=false`
- **THEN** o sistema retorna `422` com `SCOS_REASON_ACTIVATE_004`

### Requirement: Motivo inexistente retorna 404 por catálogo
Cada uma das operações `get`/`update`/`enable`/`disable` SHALL retornar `404` com o código de erro específico do catálogo quando o `{id}` não existir.

#### Scenario: Buscar motivo inexistente
- **WHEN** um cliente autorizado envia `GET /v1/reason-disable/{id}` com um `id` que não existe em `ReasonDisable`
- **THEN** o sistema retorna `404` com `SCOS_REASON_DISABLE_001`

### Requirement: Autorização dedicada por catálogo e ação
Cada uma das 5 ações mutáveis/leitura (`GET`/`CREATE`/`UPDATE`/`ENABLE`/`DISABLE`) de cada um dos 4 catálogos SHALL exigir a permissão correspondente em `ScosOrganizationPermission` (`*_REASON_ACTIVATE`, `*_REASON_INACTIVATE`, `*_REASON_DISABLE`, `*_REASON_ENABLE`), conforme `x-authorize` do contrato.

#### Scenario: Requisição sem permissão é rejeitada
- **WHEN** um cliente sem a permissão `CREATE_REASON_ACTIVATE` envia `POST /v1/reason-activate`
- **THEN** o sistema retorna `403 Forbidden` antes de qualquer validação de negócio
