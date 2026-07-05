## ADDED Requirements

### Requirement: Unicidade de code é validada por catálogo
O sistema SHALL rejeitar a criação ou atualização de `ContactType` com `code` já existente em `SCOS_CONTACT_TYPE`, retornando `409` (`SCOS_CONTACT_TYPE_002`). A validação de unicidade SHALL considerar apenas os registros de `ContactType`, não de outros catálogos.

#### Scenario: Criar com code duplicado
- **WHEN** um cliente envia `POST /v1/contact-types` com `code` já cadastrado em outro `ContactType`
- **THEN** o sistema retorna `409` com `code=SCOS_CONTACT_TYPE_002`

#### Scenario: Atualizar excluindo o próprio id da checagem
- **WHEN** um cliente envia `PUT /v1/contact-types/{id}` mantendo o mesmo `code` que o registro já tinha
- **THEN** o sistema NÃO rejeita por duplicidade (a checagem exclui o próprio `{id}`)

#### Scenario: Mesmo code em catálogos diferentes é permitido
- **WHEN** um `AddressType` já existe com `code=X` e um cliente cria um `ContactType` também com `code=X`
- **THEN** o sistema aceita a criação — unicidade é escopada por catálogo

### Requirement: ContactType inexistente retorna 404
Operações que referenciam um `{id}` de `ContactType` inexistente SHALL retornar `404` (`SCOS_CONTACT_TYPE_001`).

#### Scenario: Buscar id inexistente
- **WHEN** um cliente envia `GET /v1/contact-types/{id}` para um id que não existe
- **THEN** o sistema retorna `404` com `code=SCOS_CONTACT_TYPE_001`

### Requirement: enable/disable são idempotentes e rejeitam estado já atingido
`PUT /v1/contact-types/{id}/enable` SHALL retornar `422` (`SCOS_CONTACT_TYPE_003`) se o registro já estiver `active=true`. `PUT /v1/contact-types/{id}/disable` SHALL retornar `422` (`SCOS_CONTACT_TYPE_004`) se já estiver `active=false`.

#### Scenario: Ativar um ContactType já ativo
- **WHEN** um cliente envia `PUT /v1/contact-types/{id}/enable` para um registro com `active=true`
- **THEN** o sistema retorna `422` com `code=SCOS_CONTACT_TYPE_003`, sem alterar o registro

#### Scenario: Inativar um ContactType já inativo
- **WHEN** um cliente envia `PUT /v1/contact-types/{id}/disable` para um registro com `active=false`
- **THEN** o sistema retorna `422` com `code=SCOS_CONTACT_TYPE_004`, sem alterar o registro

### Requirement: disable não remove vínculos existentes
Inativar um `ContactType` SHALL apenas marcar `active=false` — SHALL NOT remover ou invalidar `CompanyContact`/`EmployeeContact` que já referenciam esse `contactTypeId`. O efeito de `disable` é bloquear apenas novos cadastros com esse tipo.

#### Scenario: Inativar tipo em uso não quebra contatos existentes
- **WHEN** um `ContactType` referenciado por um `CompanyContact` existente é inativado via `PUT /v1/contact-types/{id}/disable`
- **THEN** o `CompanyContact` existente continua acessível e válido, sem alteração

### Requirement: Cada operação exige a permissão correspondente
O sistema SHALL exigir `GET_CONTACT_TYPE`, `CREATE_CONTACT_TYPE`, `UPDATE_CONTACT_TYPE`, `ENABLE_CONTACT_TYPE` ou `DISABLE_CONTACT_TYPE` conforme a operação, registradas em `ScosOrganizationPermission`.

#### Scenario: Permissão nova está registrada
- **WHEN** o sistema inicializa
- **THEN** `ScosOrganizationPermission` contém as 5 entradas de `ContactType` (`GET`/`CREATE`/`UPDATE`/`ENABLE`/`DISABLE_CONTACT_TYPE`)
