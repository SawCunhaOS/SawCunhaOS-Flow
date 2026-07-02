## ADDED Requirements

### Requirement: CRUD de referência para os 4 catálogos de motivo
O sistema SHALL prover CRUD de referência para `ReasonActivate`, `ReasonInactivate`, `ReasonDisable` e `ReasonEnable`, cada um com o schema `{ code, description, entityType, active }`, seguindo o padrão `list`/`create`/`get`/`update`/`enable`/`disable` (sem `DELETE` físico).

#### Scenario: Criar motivo de ativação
- **WHEN** um cliente autorizado envia `POST /v1/reason-activate` com `code`, `description` e `entityType` válidos
- **THEN** o sistema cria o registro e retorna `201 Created`

#### Scenario: Listar motivos com paginação
- **WHEN** um cliente autorizado envia `GET /v1/reason-activate` com `paginationFilter`
- **THEN** o sistema retorna `200 OK` com `data: array` e `paginatedDTO`

#### Scenario: Desativar motivo em vez de excluir
- **WHEN** um cliente autorizado envia `PUT /v1/reason-activate/{id}/disable`
- **THEN** o sistema marca o registro como `active=false`, sem excluí-lo fisicamente

### Requirement: entityType filtra o catálogo de motivo por agregado
`entityType` SHALL aceitar exatamente 3 valores (`COMPANY`/`EMPLOYEE`/`LOGIN`), e `GET /v1/reason-activate` (e as outras 3 rotas de motivo) SHALL aceitar filtro `?entityType=` para retornar só os motivos aplicáveis ao agregado consultado.

#### Scenario: Filtrar motivos por entityType
- **WHEN** um cliente autorizado envia `GET /v1/reason-activate?entityType=LOGIN`
- **THEN** o sistema retorna só os registros com `entityType=LOGIN`

#### Scenario: entityType inválido é rejeitado
- **WHEN** um cliente autorizado envia `POST /v1/reason-activate` com `entityType` fora de `COMPANY`/`EMPLOYEE`/`LOGIN`
- **THEN** o sistema retorna `4XX` de validação de contrato
