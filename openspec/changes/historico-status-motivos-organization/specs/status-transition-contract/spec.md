## ADDED Requirements

### Requirement: Rotas de transição de status simétricas em Company/Employee/Login
`Company`, `Employee` e `Login` SHALL expor as mesmas 4 rotas de transição de status: `PUT .../enable` (activate), `PUT .../disable` (inactivate), `PUT .../block` (disable), `PUT .../unblock` (enable). Para `Company`/`Employee`, `PUT .../block` SHALL substituir a antiga rota `DELETE .../{id}`. Para `Login`, `PUT .../enable` e `PUT .../disable` SHALL ser rotas novas.

#### Scenario: Company e Employee expõem as 4 rotas
- **WHEN** o contrato de `Company`/`Employee` é inspecionado
- **THEN** existem `PUT .../enable`, `PUT .../disable`, `PUT .../block`, `PUT .../unblock`, e não existe mais `DELETE .../{id}`

#### Scenario: Login expõe as 4 rotas
- **WHEN** o contrato de `Login` é inspecionado
- **THEN** existem `PUT /logins/{id}/enable`, `/disable`, `/block`, `/unblock`

### Requirement: Motivo obrigatório em toda transição de status
Toda rota de transição de status (`enable`/`disable`/`block`/`unblock`) nos 3 agregados SHALL exigir `requestBody { reasonId: integer (required), observation: string (optional) }`.

#### Scenario: Transição sem reasonId é rejeitada
- **WHEN** um cliente autorizado envia `PUT /companies/{id}/disable` sem `reasonId` no body
- **THEN** o sistema retorna `4XX` de validação de contrato

#### Scenario: Transição com reasonId válido é aceita
- **WHEN** um cliente autorizado envia `PUT /companies/{id}/disable` com `reasonId` referenciando um `ReasonInactivate` existente e ativo
- **THEN** o sistema retorna `204 No Content`

### Requirement: block só é válido a partir de ACTIVE
`PUT .../block` (transição `disable`, destino `DISABLED`/`BLOCKED`) SHALL ser aceito apenas quando o status atual do agregado é `ACTIVE`. Chamar `block` num agregado `INACTIVE` MUST retornar `4XX`.

#### Scenario: block a partir de ACTIVE é aceito
- **WHEN** um cliente autorizado envia `PUT /companies/{id}/block` para uma empresa com `status=ACTIVE`
- **THEN** o sistema retorna `204 No Content` e o status muda para `DISABLED`

#### Scenario: block a partir de INACTIVE é rejeitado
- **WHEN** um cliente autorizado envia `PUT /companies/{id}/block` para uma empresa com `status=INACTIVE`
- **THEN** o sistema retorna `4XX`, sem alterar o status

### Requirement: Sem transição para o mesmo status
Nenhuma rota de transição SHALL aceitar uma chamada que resultaria no mesmo status atual do agregado (ex.: `enable` num agregado já `ACTIVE`).

#### Scenario: enable em agregado já ACTIVE é rejeitado
- **WHEN** um cliente autorizado envia `PUT /employees/{id}/enable` para um funcionário com `status=ACTIVE`
- **THEN** o sistema retorna `4XX`

### Requirement: reasonActivateId obrigatório na criação
`CreateCompanyRequest`, `CreateEmployeeRequest`, `CreateLoginRequest` e `CreateEmployeeLoginRequest` SHALL exigir o campo `reasonActivateId` (integer, obrigatório), pois a primeira linha de histórico de status (criação, `STATUS=ACTIVE`) exige motivo rastreável assim como qualquer transição posterior.

#### Scenario: Criação sem reasonActivateId é rejeitada
- **WHEN** um cliente autorizado envia `POST /v1/companies` sem `reasonActivateId`
- **THEN** o sistema retorna `4XX` de validação de contrato

#### Scenario: Criação com reasonActivateId válido é aceita
- **WHEN** um cliente autorizado envia `POST /v1/companies` com `reasonActivateId` referenciando um `ReasonActivate` existente, ativo e com `entityType=COMPANY`
- **THEN** o sistema cria a empresa e retorna `201 Created`

### Requirement: deleteLogin removido do contrato
`DELETE /logins/{id}` (`deleteLogin`) SHALL ser removido do contrato, pois `LOGIN.STATUS` não tem mais o valor `DELETED` no vocabulário fechado do banco.

**Reason**: o vocabulário fechado de `LOGIN.STATUS` (`ACTIVE`/`INACTIVE`/`BLOCKED`) não inclui mais `DELETED` — o endpoint de exclusão definitiva não corresponde a nenhuma transição válida.
**Migration**: clientes que dependiam de `deleteLogin` devem usar `PUT /logins/{id}/block` (bloqueio) ou `PUT /logins/{id}/disable` (encerramento definitivo via `INACTIVE`), conforme a intenção de negócio.

#### Scenario: deleteLogin não existe mais
- **WHEN** o contrato de `Login` é inspecionado
- **THEN** não existe operação `DELETE /logins/{id}`
