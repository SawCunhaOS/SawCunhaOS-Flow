## 1. Contrato — novo arquivo de catálogos de motivo

- [x] 1.1 Criar `etc/api/organization/ScosOrganization_Reason.yml` com schemas `ReasonActivate`, `ReasonInactivate`, `ReasonDisable`, `ReasonEnable` (`code`, `description`, `entityType` enum 3 valores `COMPANY`/`EMPLOYEE`/`LOGIN`, `active`)
- [x] 1.2 Adicionar `Create<X>Request`/`Update<X>Request`, `Get<X>Response { data }`, `GetAll<X>Response { data: array, paginatedDTO }` para os 4 catálogos, seguindo o padrão SCOS (`x-required-message`/`x-empty-message`)
- [x] 1.3 Adicionar paths `GET/POST /v1/reason-activate`, `GET/PUT /v1/reason-activate/{id}`, `PUT .../enable`, `PUT .../disable` (mesmo padrão para `reason-inactivate`, `reason-disable`, `reason-enable`), com `paginationFilter` obrigatório em toda listagem
- [x] 1.4 Adicionar `x-authorize` por rota nova no contrato (parte de `ScosOrganizationPermission` em código fica pra change de implementação — ver nota no relatório final)

## 2. Contrato — Company

- [x] 2.1 Adicionar `requestBody { reasonId, observation? }` a `PUT /companies/{id}/enable` e `/disable`
- [x] 2.2 Renomear `DELETE /companies/{id}` (UC-008) → `PUT /companies/{id}/block`, mesmo `requestBody`; migrar `x-authorize` `DELETE_COMPANY` → `BLOCK_COMPANY`
- [x] 2.3 Criar `PUT /companies/{id}/unblock` com `requestBody { reasonId, observation? }`; `x-authorize` `UNBLOCK_COMPANY`
- [x] 2.4 Adicionar `reasonActivateId` (obrigatório) a `CreateCompanyRequest`
- [x] 2.5 Criar `GET /companies/{id}/status-history` (paginado, somente leitura)
- [x] 2.6 Documentar na `description` de `block`/`enable`/`disable`/`unblock` que o nome da rota não bate 1:1 com a transição de domínio, e que `block` só é válido a partir de `ACTIVE`

## 3. Contrato — Employee

- [x] 3.1 Adicionar `requestBody { reasonId, observation? }` a `PUT /employees/{id}/enable` e `/disable`
- [x] 3.2 Renomear `DELETE /employees/{id}` (UC-043) → `PUT /employees/{id}/block`, mesmo `requestBody`; migrar `x-authorize` `DELETE_EMPLOYEE` → `BLOCK_EMPLOYEE`
- [x] 3.3 Criar `PUT /employees/{id}/unblock` com `requestBody { reasonId, observation? }`; `x-authorize` `UNBLOCK_EMPLOYEE`
- [x] 3.4 Adicionar `reasonActivateId` (obrigatório) a `CreateEmployeeRequest`
- [x] 3.5 Criar `GET /employees/{id}/status-history` (paginado, somente leitura)

## 4. Contrato — Login

- [x] 4.1 Criar `PUT /logins/{id}/enable`, `PUT /logins/{id}/disable` (novos), `requestBody { reasonId, observation? }`; `x-authorize` `ENABLE_LOGIN`/`DISABLE_LOGIN` (granulares, novas — não reaproveitar `UPDATE_LOGIN_STATUS`)
- [x] 4.2 Adicionar `requestBody { reasonId, observation? }` às rotas existentes `PUT /logins/{id}/block` e `/unblock` (mantêm `UPDATE_LOGIN_STATUS`, sem mudança de permissão)
- [x] 4.3 Remover `DELETE /logins/{id}` (`deleteLogin`, UC-066) do contrato e a permissão associada
- [x] 4.4 Adicionar `reasonActivateId` (obrigatório) a `CreateLoginRequest` e `CreateEmployeeLoginRequest`
- [x] 4.5 Criar `GET /logins/{id}/status-history` (paginado, somente leitura)
