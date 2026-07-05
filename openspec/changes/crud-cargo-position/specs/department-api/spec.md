## ADDED Requirements

### Requirement: GET /v1/departments filtra de fato por active
O sistema SHALL restringir a listagem de `GET /v1/departments` aos departamentos com o status informado quando o parâmetro de query `active` é enviado. Quando `active` não é informado, a listagem SHALL retornar departamentos de ambos os status.

#### Scenario: Listagem sem filtro
- **WHEN** `GET /v1/departments` é chamado sem o parâmetro `active`
- **THEN** retorna HTTP 200 com `data[]` contendo departamentos ativos e inativos

#### Scenario: Listagem filtrada por active=true
- **WHEN** `GET /v1/departments?active=true` é chamado e existem departamentos ativos e inativos
- **THEN** retorna HTTP 200 com `data[]` contendo somente departamentos com `active == true`

#### Scenario: Listagem filtrada por active=false
- **WHEN** `GET /v1/departments?active=false` é chamado e existem departamentos ativos e inativos
- **THEN** retorna HTTP 200 com `data[]` contendo somente departamentos com `active == false`
