## ADDED Requirements

### Requirement: Transições de estado via PUT sub-resource sem body
O sistema SHALL implementar transições de estado (ativar/desativar/bloquear/desbloquear) usando `PUT` sobre um sub-resource nomeado, sem request body. O método PUT é idempotente: chamar o endpoint N vezes produz o mesmo resultado.

#### Scenario: Ativar company via PUT sem body
- **WHEN** `PUT /v1/companies/{id}/enable` é requisitado sem body
- **THEN** o sistema retorna `204 No_Content` e o status da company é `ACTIVE`

#### Scenario: Desativar company via PUT sem body
- **WHEN** `PUT /v1/companies/{id}/disable` é requisitado sem body
- **THEN** o sistema retorna `204 No_Content` e o status da company é `INACTIVE`

#### Scenario: Idempotência do enable
- **WHEN** `PUT /v1/companies/{id}/enable` é requisitado duas vezes consecutivas
- **THEN** ambas as chamadas retornam `204 No_Content` e o estado final é `ACTIVE`

#### Scenario: Ativar employee via PUT sem body
- **WHEN** `PUT /v1/employees/{id}/enable` é requisitado sem body
- **THEN** o sistema retorna `204 No_Content` e o campo `active` do employee é `true`

#### Scenario: Bloquear login via PUT sem body
- **WHEN** `PUT /v1/employees/{employeeId}/logins/{id}/block` é requisitado sem body
- **THEN** o sistema retorna `204 No_Content` e o status do login é `BLOCKED`

#### Scenario: Desbloquear login via PUT sem body
- **WHEN** `PUT /v1/employees/{employeeId}/logins/{id}/unblock` é requisitado sem body
- **THEN** o sistema retorna `204 No_Content` e o status do login é `ENABLED`

### Requirement: Toggle de status uniforme em todos os recursos de organização
O sistema SHALL fornecer endpoints de toggle de status (`enable`/`disable`) para todos os recursos que possuem estado ativo/inativo: Company, Employee, Department e Position. A ausência de toggle em qualquer desses recursos é uma inconsistência de design.

#### Scenario: Department possui endpoint de ativação
- **WHEN** `PUT /v1/departments/{id}/enable` é requisitado
- **THEN** o sistema retorna `204 No_Content` e o campo `active` do department é `true`

#### Scenario: Department possui endpoint de desativação
- **WHEN** `PUT /v1/departments/{id}/disable` é requisitado
- **THEN** o sistema retorna `204 No_Content` e o campo `active` do department é `false`

#### Scenario: Position possui endpoint de ativação
- **WHEN** `PUT /v1/positions/{id}/enable` é requisitado
- **THEN** o sistema retorna `204 No_Content` e o campo `active` do position é `true`

#### Scenario: Position possui endpoint de desativação
- **WHEN** `PUT /v1/positions/{id}/disable` é requisitado
- **THEN** o sistema retorna `204 No_Content` e o campo `active` do position é `false`

#### Scenario: Company responde a enable e disable
- **WHEN** os endpoints `PUT /v1/companies/{id}/enable` e `PUT /v1/companies/{id}/disable` são requisitados
- **THEN** ambos retornam `204 No_Content`

#### Scenario: Employee responde a enable e disable
- **WHEN** os endpoints `PUT /v1/employees/{id}/enable` e `PUT /v1/employees/{id}/disable` são requisitados
- **THEN** ambos retornam `204 No_Content`

### Requirement: DELETE sem requestBody (RFC 9110 §9.3.5)
O sistema SHALL não possuir endpoints DELETE com requestBody. O endpoint `DELETE /v1/profiles/{id}/features` com body SHALL ser removido. A remoção de features de um profile SHALL ser realizada via `PUT /v1/profiles/{id}/features` com a lista final desejada (substituição completa).

#### Scenario: Remover todas as features de um profile
- **WHEN** `PUT /v1/profiles/{id}/features` é requisitado com body `{"features": []}`
- **THEN** o sistema retorna `204 No_Content` e o profile não possui mais nenhuma feature

#### Scenario: Remover features específicas de um profile
- **WHEN** `PUT /v1/profiles/{id}/features` é requisitado com body contendo apenas as features a manter
- **THEN** o sistema retorna `204 No_Content` e o profile possui exatamente as features da lista enviada

#### Scenario: DELETE com body não existe na spec
- **WHEN** a spec OpenAPI `ScosOrganization_Login.yml` é inspecionada
- **THEN** não existe operação DELETE em `/v1/profiles/{id}/features` com requestBody definido

#### Scenario: Nenhum endpoint DELETE possui requestBody
- **WHEN** todas as specs OpenAPI do módulo Organization são inspecionadas
- **THEN** nenhuma operação DELETE define requestBody
