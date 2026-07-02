## ADDED Requirements

### Requirement: Listagem paginada de perfis adicionais do login
O sistema SHALL prover `GET /v1/logins/{id}/profiles`, retornando `GetAllLoginProfilesResponse { data: array, paginatedDTO }` com os perfis adicionais (`SCOS_LOGIN_PROFILE`) do login informado. O parâmetro `paginationFilter` (query) MUST ser obrigatório, seguindo o padrão do restante do contrato.

#### Scenario: Listar perfis adicionais existentes
- **WHEN** um cliente autorizado envia `GET /v1/logins/{id}/profiles` com `paginationFilter` para um login com perfis adicionais cadastrados
- **THEN** o sistema retorna `200 OK` com `GetAllLoginProfilesResponse` contendo os perfis adicionais e `paginatedDTO`

#### Scenario: Listar login sem perfis adicionais
- **WHEN** um cliente autorizado envia `GET /v1/logins/{id}/profiles` para um login sem nenhum perfil adicional
- **THEN** o sistema retorna `200 OK` com `data` vazio

### Requirement: Adição de perfil adicional ao login
O sistema SHALL prover `POST /v1/logins/{id}/profiles/{profileId}`, sem request body (chave via path params), criando um registro em `SCOS_LOGIN_PROFILE`. O perfil adicional MUST NOT ser igual ao perfil principal do login (`Login.profileId`) — essa validação MUST ocorrer no use case antes da persistência, retornando `4XX` amigável, e não depender apenas da trigger `TRG_VALIDATE_LOGIN_PROFILE_NOT_PRIMARY` do banco.

#### Scenario: Adicionar perfil adicional válido
- **WHEN** um cliente autorizado envia `POST /v1/logins/{id}/profiles/{profileId}` para um `profileId` ativo, existente, e diferente do perfil principal do login
- **THEN** o sistema cria o registro em `SCOS_LOGIN_PROFILE` e retorna `201 Created`

#### Scenario: Rejeitar perfil adicional igual ao perfil principal
- **WHEN** um cliente autorizado envia `POST /v1/logins/{id}/profiles/{profileId}` com `profileId` igual ao `Login.profileId` do login
- **THEN** o sistema rejeita com `4XX` antes de acionar a trigger do banco

#### Scenario: Rejeitar duplicidade de perfil adicional
- **WHEN** um cliente autorizado envia `POST /v1/logins/{id}/profiles/{profileId}` para uma combinação `(loginId, profileId)` já existente
- **THEN** o sistema retorna `4XX`

### Requirement: Remoção idempotente de perfil adicional do login
O sistema SHALL prover `DELETE /v1/logins/{id}/profiles/{profileId}`, sem request body, removendo o registro correspondente em `SCOS_LOGIN_PROFILE`. A operação SHALL ser idempotente: remover uma associação que não existe MUST retornar `204 No Content`, não `404`.

#### Scenario: Remover perfil adicional existente
- **WHEN** um cliente autorizado envia `DELETE /v1/logins/{id}/profiles/{profileId}` para uma associação existente
- **THEN** o sistema remove o registro e retorna `204 No Content`

#### Scenario: Remover perfil adicional inexistente é idempotente
- **WHEN** um cliente autorizado envia `DELETE /v1/logins/{id}/profiles/{profileId}` para uma combinação `(loginId, profileId)` que não existe em `SCOS_LOGIN_PROFILE`
- **THEN** o sistema retorna `204 No Content`, sem erro

### Requirement: Autorização dos endpoints de perfil adicional
Cada operação de `login-additional-profiles` SHALL exigir a permissão correspondente em `ScosOrganizationPermission`: `GET_LOGIN_PROFILE`, `CREATE_LOGIN_PROFILE`, `DELETE_LOGIN_PROFILE`.

#### Scenario: Requisição sem permissão é rejeitada
- **WHEN** um cliente sem a permissão `CREATE_LOGIN_PROFILE` envia `POST /v1/logins/{id}/profiles/{profileId}`
- **THEN** o sistema retorna `4XX` de autorização
