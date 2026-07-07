## ADDED Requirements

### Requirement: Listagem paginada de Natureza Jurídica
`GET /v1/legal-natures` SHALL retornar `200` com a lista paginada de naturezas jurídicas (envelope `data`), exigindo a permissão `GET_LEGAL_NATURE`.

#### Scenario: Listar com paginação padrão
- **WHEN** um cliente autorizado envia `GET /v1/legal-natures`
- **THEN** o sistema retorna `200` com a página de naturezas jurídicas

### Requirement: Criação de Natureza Jurídica
`POST /v1/legal-natures` SHALL criar uma natureza jurídica com `code` (≤ 30 chars, obrigatório) e `description` (≤ 255 chars, obrigatório), retornando `201`, e exige a permissão `CREATE_LEGAL_NATURE`.

#### Scenario: Criar com dados válidos
- **WHEN** um cliente autorizado envia `POST /v1/legal-natures` com `code` e `description` válidos e `code` inédito
- **THEN** o sistema persiste a natureza jurídica e retorna `201`

#### Scenario: Campo obrigatório ausente ou vazio
- **WHEN** um cliente envia `POST /v1/legal-natures` sem `code`/`description` ou com valor vazio
- **THEN** o sistema retorna `400` (`SCOS_VALIDATION_003` ausente / `SCOS_VALIDATION_001` vazio)

### Requirement: Unicidade de code é validada por catálogo
O sistema SHALL rejeitar a criação ou atualização de `LegalNature` com `code` já existente em `SCOS_LEGAL_NATURE`, retornando `409` (`SCOS_LEGAL_NATURE_002`). A validação de unicidade SHALL considerar apenas registros de `LegalNature`, não de outros catálogos.

#### Scenario: Criar com code duplicado
- **WHEN** um cliente envia `POST /v1/legal-natures` com `code` já cadastrado em outra `LegalNature`
- **THEN** o sistema retorna `409` com `code=SCOS_LEGAL_NATURE_002`

#### Scenario: Atualizar excluindo o próprio id da checagem
- **WHEN** um cliente envia `PUT /v1/legal-natures/{id}` mantendo o mesmo `code` que o registro já tinha
- **THEN** o sistema NÃO rejeita por duplicidade (a checagem exclui o próprio `{id}`)

#### Scenario: Atualizar para code de outra natureza jurídica existente
- **WHEN** um cliente envia `PUT /v1/legal-natures/{id}` com `code` que já pertence a outra `LegalNature`
- **THEN** o sistema retorna `409` com `code=SCOS_LEGAL_NATURE_002`

### Requirement: Natureza jurídica inexistente retorna 404
Operações (`GET`/`PUT`/`DELETE`) que referenciam um `{id}` de `LegalNature` inexistente SHALL retornar `404` (`SCOS_LEGAL_NATURE_001`).

#### Scenario: Buscar id inexistente
- **WHEN** um cliente envia `GET /v1/legal-natures/{id}` para um id que não existe
- **THEN** o sistema retorna `404` com `code=SCOS_LEGAL_NATURE_001`

#### Scenario: Atualizar id inexistente
- **WHEN** um cliente envia `PUT /v1/legal-natures/{id}` para um id que não existe
- **THEN** o sistema retorna `404` com `code=SCOS_LEGAL_NATURE_001`

### Requirement: Atualização de Natureza Jurídica
`PUT /v1/legal-natures/{id}` SHALL atualizar `code` e `description` de uma natureza jurídica existente, retornando `204`, e exige a permissão `UPDATE_LEGAL_NATURE`. As mesmas validações de obrigatoriedade, tamanho e unicidade do `POST` SHALL ser aplicadas.

#### Scenario: Atualizar com dados válidos
- **WHEN** um cliente autorizado envia `PUT /v1/legal-natures/{id}` com `code`/`description` válidos
- **THEN** o sistema atualiza a natureza jurídica e retorna `204`

### Requirement: Validação de tamanho de code e description
O sistema SHALL rejeitar com `400` `code` acima de 30 caracteres ou `description` acima de 255 caracteres, tanto em `POST` quanto em `PUT`. O limite SHALL ser declarado no contrato (`maxLength`) e respaldado pela coluna `SCOS_LEGAL_NATURE.CODE varchar(30)` / `DESCRIPTION varchar(255)`.

#### Scenario: code maior que 30 caracteres
- **WHEN** um cliente envia `POST /v1/legal-natures` com `code` de 31+ caracteres
- **THEN** o sistema retorna `400` antes de persistir (sem `DataIntegrityViolation`)

#### Scenario: description maior que 255 caracteres
- **WHEN** um cliente envia `POST /v1/legal-natures` com `description` de 256+ caracteres
- **THEN** o sistema retorna `400` antes de persistir

### Requirement: Exclusão física de Natureza Jurídica com guarda de vínculo
`DELETE /v1/legal-natures/{id}` SHALL remover fisicamente a natureza jurídica e retornar `204`, exigindo a permissão `DELETE_LEGAL_NATURE`. A exclusão SHALL ser rejeitada com `422` (`SCOS_LEGAL_NATURE_003`) quando ainda existir alguma empresa referenciando-a via `SCOS_COMPANY.legalNatureId` (em qualquer status). A guarda SHALL ser aplicada na camada de aplicação/domínio (não existe trigger de bloqueio no banco para `SCOS_LEGAL_NATURE`).

#### Scenario: Excluir natureza jurídica sem vínculos
- **WHEN** um cliente autorizado envia `DELETE /v1/legal-natures/{id}` para uma natureza jurídica não referenciada por nenhuma empresa
- **THEN** o sistema remove o registro e retorna `204`

#### Scenario: Excluir natureza jurídica em uso
- **WHEN** um cliente envia `DELETE /v1/legal-natures/{id}` para uma natureza jurídica referenciada por `SCOS_COMPANY.legalNatureId`
- **THEN** o sistema retorna `422` com `code=SCOS_LEGAL_NATURE_003`, sem remover o registro

### Requirement: Cada operação exige a permissão correspondente
O sistema SHALL exigir `GET_LEGAL_NATURE`, `CREATE_LEGAL_NATURE`, `UPDATE_LEGAL_NATURE` ou `DELETE_LEGAL_NATURE` conforme a operação, registradas em `ScosOrganizationPermission`.

#### Scenario: Permissões novas estão registradas
- **WHEN** o sistema inicializa
- **THEN** `ScosOrganizationPermission` contém as 4 entradas de Natureza Jurídica (`GET`/`CREATE`/`UPDATE`/`DELETE_LEGAL_NATURE`)
