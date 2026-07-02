## ADDED Requirements

### Requirement: CRUD mínimo de Cnae
O sistema SHALL prover um catálogo de referência `Cnae` com os campos `code` e `description`, sem campos de auditoria (`active`, `updatedAt`, `userAt`) e sem soft-delete, refletindo o schema mínimo da tabela `SCOS_CNAE`.

#### Scenario: Criar CNAE
- **WHEN** um cliente autorizado envia `POST /v1/cnaes` com `code` e `description` válidos
- **THEN** o sistema cria o registro e retorna `201 Created`

#### Scenario: Listar CNAEs com paginação
- **WHEN** um cliente autorizado envia `GET /v1/cnaes` com `paginationFilter`
- **THEN** o sistema retorna `200 OK` com `GetAllCnaesResponse` (`data: array`, `paginatedDTO`)

#### Scenario: Buscar CNAE por id
- **WHEN** um cliente autorizado envia `GET /v1/cnaes/{id}` para um id existente
- **THEN** o sistema retorna `200 OK` com `GetCnaeResponse { data }`

#### Scenario: Buscar CNAE inexistente
- **WHEN** um cliente autorizado envia `GET /v1/cnaes/{id}` para um id que não existe
- **THEN** o sistema retorna `4XX`

#### Scenario: Atualizar CNAE
- **WHEN** um cliente autorizado envia `PUT /v1/cnaes/{id}` com `code`/`description` novos
- **THEN** o sistema atualiza o registro e retorna `204 No Content`

#### Scenario: Excluir CNAE sem referência
- **WHEN** um cliente autorizado envia `DELETE /v1/cnaes/{id}` para um registro sem `Company.cnaePrincipalId` nem `CompanyCnaeSecondary` referenciando-o
- **THEN** o sistema exclui fisicamente o registro e retorna `204 No Content`

#### Scenario: Excluir CNAE referenciado
- **WHEN** um cliente autorizado envia `DELETE /v1/cnaes/{id}` para um registro referenciado por `cnaePrincipalId` de alguma `Company` ou por alguma linha de `CompanyCnaeSecondary`
- **THEN** o sistema rejeita a exclusão e retorna `4XX`

### Requirement: Autorização do catálogo Cnae
Cada operação de `Cnae` SHALL exigir a permissão correspondente em `ScosOrganizationPermission`: `GET_CNAE`, `CREATE_CNAE`, `UPDATE_CNAE`, `DELETE_CNAE`.

#### Scenario: Requisição sem permissão é rejeitada
- **WHEN** um cliente sem a permissão `CREATE_CNAE` envia `POST /v1/cnaes`
- **THEN** o sistema retorna `4XX` de autorização
