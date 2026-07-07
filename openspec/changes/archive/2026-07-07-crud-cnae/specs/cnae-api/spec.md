## ADDED Requirements

### Requirement: Listagem paginada de CNAE
`GET /v1/cnaes` SHALL retornar `200` com a lista paginada de CNAE (envelope `data`), exigindo a permissão `GET_CNAE`.

#### Scenario: Listar com paginação padrão
- **WHEN** um cliente autorizado envia `GET /v1/cnaes`
- **THEN** o sistema retorna `200` com a página de CNAE

### Requirement: Criação de CNAE
`POST /v1/cnaes` SHALL criar um CNAE com `code` (≤ 30 chars, obrigatório) e `description` (≤ 255 chars, obrigatório), retornando `201`, e exige a permissão `CREATE_CNAE`.

#### Scenario: Criar com dados válidos
- **WHEN** um cliente autorizado envia `POST /v1/cnaes` com `code` e `description` válidos e `code` inédito
- **THEN** o sistema persiste o CNAE e retorna `201`

#### Scenario: Campo obrigatório ausente ou vazio
- **WHEN** um cliente envia `POST /v1/cnaes` sem `code`/`description` ou com valor vazio
- **THEN** o sistema retorna `400` (`SCOS_VALIDATION_003` ausente / `SCOS_VALIDATION_001` vazio)

### Requirement: Validação de tamanho de code e description
O sistema SHALL rejeitar com `400` `code` acima de 30 caracteres ou `description` acima de 255 caracteres, tanto em `POST` quanto em `PUT`. O limite SHALL ser declarado no contrato (`maxLength`) e respaldado pela coluna `SCOS_CNAE.CODE varchar(30)` / `DESCRIPTION varchar(255)`.

#### Scenario: code maior que 30 caracteres
- **WHEN** um cliente envia `POST /v1/cnaes` com `code` de 31+ caracteres
- **THEN** o sistema retorna `400` antes de persistir (sem `DataIntegrityViolation`)

#### Scenario: description maior que 255 caracteres
- **WHEN** um cliente envia `POST /v1/cnaes` com `description` de 256+ caracteres
- **THEN** o sistema retorna `400` antes de persistir

### Requirement: Unicidade de code é validada por catálogo
O sistema SHALL rejeitar a criação ou atualização de `Cnae` com `code` já existente em `SCOS_CNAE`, retornando `409` (`SCOS_CNAE_002`). A validação de unicidade SHALL considerar apenas registros de `Cnae`, não de outros catálogos.

#### Scenario: Criar com code duplicado
- **WHEN** um cliente envia `POST /v1/cnaes` com `code` já cadastrado em outro `Cnae`
- **THEN** o sistema retorna `409` com `code=SCOS_CNAE_002`

#### Scenario: Atualizar excluindo o próprio id da checagem
- **WHEN** um cliente envia `PUT /v1/cnaes/{id}` mantendo o mesmo `code` que o registro já tinha
- **THEN** o sistema NÃO rejeita por duplicidade (a checagem exclui o próprio `{id}`)

#### Scenario: Atualizar para code de outro CNAE existente
- **WHEN** um cliente envia `PUT /v1/cnaes/{id}` com `code` que já pertence a outro `Cnae`
- **THEN** o sistema retorna `409` com `code=SCOS_CNAE_002`

### Requirement: CNAE inexistente retorna 404
Operações (`GET`/`PUT`/`DELETE`) que referenciam um `{id}` de `Cnae` inexistente SHALL retornar `404` (`SCOS_CNAE_001`).

#### Scenario: Buscar id inexistente
- **WHEN** um cliente envia `GET /v1/cnaes/{id}` para um id que não existe
- **THEN** o sistema retorna `404` com `code=SCOS_CNAE_001`

#### Scenario: Atualizar id inexistente
- **WHEN** um cliente envia `PUT /v1/cnaes/{id}` para um id que não existe
- **THEN** o sistema retorna `404` com `code=SCOS_CNAE_001`

### Requirement: Atualização de CNAE
`PUT /v1/cnaes/{id}` SHALL atualizar `code` e `description` de um CNAE existente, retornando `204`, e exige a permissão `UPDATE_CNAE`. As mesmas validações de obrigatoriedade, tamanho e unicidade do `POST` SHALL ser aplicadas.

#### Scenario: Atualizar com dados válidos
- **WHEN** um cliente autorizado envia `PUT /v1/cnaes/{id}` com `code`/`description` válidos
- **THEN** o sistema atualiza o CNAE e retorna `204`

### Requirement: Exclusão física de CNAE com guarda de vínculo
`DELETE /v1/cnaes/{id}` SHALL remover fisicamente o CNAE e retornar `204`, exigindo a permissão `DELETE_CNAE`. A exclusão SHALL ser rejeitada com `422` (`SCOS_CNAE_003`) quando o CNAE ainda estiver referenciado como CNAE principal de alguma empresa (`SCOS_COMPANY.cnaePrincipalId`) **ou** como CNAE secundário (`SCOS_COMPANY_CNAE_SECONDARY`). A guarda SHALL ser aplicada na camada de aplicação/domínio (não existe trigger de bloqueio no banco para `SCOS_CNAE`).

#### Scenario: Excluir CNAE sem vínculos
- **WHEN** um cliente autorizado envia `DELETE /v1/cnaes/{id}` para um CNAE não referenciado por nenhuma empresa
- **THEN** o sistema remove o registro e retorna `204`

#### Scenario: Excluir CNAE usado como principal
- **WHEN** um cliente envia `DELETE /v1/cnaes/{id}` para um CNAE referenciado por `SCOS_COMPANY.cnaePrincipalId`
- **THEN** o sistema retorna `422` com `code=SCOS_CNAE_003`, sem remover o registro

#### Scenario: Excluir CNAE usado como secundário
- **WHEN** um cliente envia `DELETE /v1/cnaes/{id}` para um CNAE referenciado em `SCOS_COMPANY_CNAE_SECONDARY`
- **THEN** o sistema retorna `422` com `code=SCOS_CNAE_003`, sem remover o registro

### Requirement: Cada operação exige a permissão correspondente
O sistema SHALL exigir `GET_CNAE`, `CREATE_CNAE`, `UPDATE_CNAE` ou `DELETE_CNAE` conforme a operação, registradas em `ScosOrganizationPermission`.

#### Scenario: Permissões novas estão registradas
- **WHEN** o sistema inicializa
- **THEN** `ScosOrganizationPermission` contém as 4 entradas de CNAE (`GET`/`CREATE`/`UPDATE`/`DELETE_CNAE`)
