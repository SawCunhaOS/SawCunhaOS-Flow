## Why

`SCOS_LOGIN_PROFILE` (schema v2) é uma tabela N:N nova que permite um login ter perfis **adicionais** além do principal (`SCOS_LOGIN.PROFILE_ID`). O contrato hoje só tem `PUT /logins/{id}/profile/{profileId}`, que substitui o perfil principal (relação 1:1) — não existe nenhum endpoint para gerenciar o relacionamento N:N.

## What Changes

**Escopo desta rodada: somente o contrato OpenAPI (YAML).** Implementação de código (permissões, use cases, delegates, testes) fica para uma change futura.

- Criar `GET /v1/logins/{id}/profiles` — lista paginada dos perfis adicionais do login
- Criar `POST /v1/logins/{id}/profiles/{profileId}` — adiciona um perfil adicional ao login
- Criar `DELETE /v1/logins/{id}/profiles/{profileId}` — remove um perfil adicional do login
- Adicionar `x-authorize` no contrato: `GET_LOGIN_PROFILE`, `CREATE_LOGIN_PROFILE`, `DELETE_LOGIN_PROFILE` (entradas correspondentes em `ScosOrganizationPermission` ficam para a change de implementação)

## Capabilities

### New Capabilities
- `login-additional-profiles`: CRUD (list/add/remove) do relacionamento N:N `SCOS_LOGIN_PROFILE` — perfis adicionais de um login, distinto do perfil principal 1:1 (`Login.profileId`, endpoint `PUT /logins/{id}/profile/{profileId}` já existente e inalterado)

### Modified Capabilities
(nenhuma — `model-login` cobre a camada JPA/coluna `PROFILE_ID`, já sincronizada; este change é só contrato OpenAPI de um relacionamento novo, aditivo, sem alterar comportamento do perfil principal)

## Impact

- `etc/api/organization/ScosOrganization_Login.yml` — paths novos `GET/POST/DELETE /v1/logins/{id}/profiles*`; schema novo `GetAllLoginProfilesResponse`
- `ScosOrganizationPermission` (código) — fora de escopo desta change; as 3 entradas de permissão ficam pendentes para a implementação futura
- Sem impacto de banco — schema v2 já existe (`adequacao-liquibase-domain-model-v2`, completo)
- Regra de negócio: perfil adicional não pode ser igual ao perfil principal (`TRG_VALIDATE_LOGIN_PROFILE_NOT_PRIMARY` no banco) — use case precisa validar antes, para devolver `4XX` amigável em vez da exceção genérica da trigger
