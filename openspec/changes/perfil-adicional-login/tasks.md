## 1. Contrato (YAML antes do código)

- [x] 1.1 Adicionar schema `GetAllLoginProfilesResponse { data: array, paginatedDTO }` em `etc/api/organization/ScosOrganization_Login.yml`
- [x] 1.2 Adicionar path `GET /v1/logins/{id}/profiles` (paginado, `paginationFilter` obrigatório) com `x-authorize: [GET_LOGIN_PROFILE]`
- [x] 1.3 Adicionar path `POST /v1/logins/{id}/profiles/{profileId}` (sem body, `201`) com `x-authorize: [CREATE_LOGIN_PROFILE]`
- [x] 1.4 Adicionar path `DELETE /v1/logins/{id}/profiles/{profileId}` (sem body, `204` idempotente) com `x-authorize: [DELETE_LOGIN_PROFILE]`
