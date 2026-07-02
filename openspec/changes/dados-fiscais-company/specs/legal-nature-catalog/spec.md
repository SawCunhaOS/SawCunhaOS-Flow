## ADDED Requirements

### Requirement: CRUD mínimo de LegalNature
O sistema SHALL prover um catálogo de referência `LegalNature` com os campos `code` e `description`, sem campos de auditoria (`active`, `updatedAt`, `userAt`) e sem soft-delete, refletindo o schema mínimo da tabela `SCOS_LEGAL_NATURE`.

#### Scenario: Criar natureza jurídica
- **WHEN** um cliente autorizado envia `POST /v1/legal-natures` com `code` e `description` válidos
- **THEN** o sistema cria o registro e retorna `201 Created`

#### Scenario: Listar naturezas jurídicas com paginação
- **WHEN** um cliente autorizado envia `GET /v1/legal-natures` com `paginationFilter`
- **THEN** o sistema retorna `200 OK` com `GetAllLegalNaturesResponse` (`data: array`, `paginatedDTO`)

#### Scenario: Buscar natureza jurídica por id
- **WHEN** um cliente autorizado envia `GET /v1/legal-natures/{id}` para um id existente
- **THEN** o sistema retorna `200 OK` com `GetLegalNatureResponse { data }`

#### Scenario: Buscar natureza jurídica inexistente
- **WHEN** um cliente autorizado envia `GET /v1/legal-natures/{id}` para um id que não existe
- **THEN** o sistema retorna `4XX`

#### Scenario: Atualizar natureza jurídica
- **WHEN** um cliente autorizado envia `PUT /v1/legal-natures/{id}` com `code`/`description` novos
- **THEN** o sistema atualiza o registro e retorna `204 No Content`

#### Scenario: Excluir natureza jurídica sem referência
- **WHEN** um cliente autorizado envia `DELETE /v1/legal-natures/{id}` para um registro sem `Company` referenciando-o
- **THEN** o sistema exclui fisicamente o registro e retorna `204 No Content`

#### Scenario: Excluir natureza jurídica referenciada por Company
- **WHEN** um cliente autorizado envia `DELETE /v1/legal-natures/{id}` para um registro referenciado por `legalNatureId` de alguma `Company`
- **THEN** o sistema rejeita a exclusão e retorna `4XX`

### Requirement: Autorização do catálogo LegalNature
Cada operação de `LegalNature` SHALL exigir a permissão correspondente em `ScosOrganizationPermission`: `GET_LEGAL_NATURE`, `CREATE_LEGAL_NATURE`, `UPDATE_LEGAL_NATURE`, `DELETE_LEGAL_NATURE`.

#### Scenario: Requisição sem permissão é rejeitada
- **WHEN** um cliente sem a permissão `CREATE_LEGAL_NATURE` envia `POST /v1/legal-natures`
- **THEN** o sistema retorna `4XX` de autorização
