## ADDED Requirements

### Requirement: GET /v1/outbox-events retorna lista paginada filtrável
O sistema SHALL retornar lista paginada de `OutboxEvent` na rota `GET /v1/outbox-events`, filtrável por `topic`, `status` (`OutboxEventStatus`) e `aggregateId`. Paginação (`paginationFilter`) é obrigatória.

#### Scenario: Listagem sem filtro
- **WHEN** `GET /v1/outbox-events` é chamado com `paginationFilter` válido, sem outros filtros
- **THEN** retorna HTTP 200 com `data[]` e `paginatedDTO` preenchidos, contendo todos os eventos

#### Scenario: Filtro por status
- **WHEN** `GET /v1/outbox-events?status=FAILED` é chamado
- **THEN** retorna HTTP 200 com `data[]` contendo só eventos `status = FAILED`

#### Scenario: Filtro por topic e aggregateId combinados
- **WHEN** `GET /v1/outbox-events?topic=LOGIN_KEYCLOAK&aggregateId=42` é chamado
- **THEN** retorna HTTP 200 com `data[]` contendo só eventos desse `topic` e `aggregateId`

### Requirement: GET /v1/outbox-events/{id} retorna detalhe com payload livre
O sistema SHALL retornar o detalhe de um `OutboxEvent` por `id`, incluindo `payload` (JSON livre, sem schema fixo — varia por `topic`/consumidor), `responseData` (preenchido só quando o backend do tópico é `DIRECT_API`), `retryCount` e `message`. SHALL retornar HTTP 404 quando o `id` não existe.

#### Scenario: Evento encontrado
- **WHEN** `GET /v1/outbox-events/{id}` é chamado para um `id` existente
- **THEN** retorna HTTP 200 com `data.payload`, `data.retryCount` e `data.message` preenchidos

#### Scenario: responseData preenchido só em DIRECT_API
- **WHEN** `GET /v1/outbox-events/{id}` é chamado para um evento cujo tópico usa backend `DIRECT_API`
- **THEN** `data.responseData` vem preenchido com a resposta da chamada

#### Scenario: Evento não encontrado
- **WHEN** `GET /v1/outbox-events/{id}` é chamado para um `id` que não existe
- **THEN** retorna HTTP 404

### Requirement: GET /v1/outbox-events/{id}/logs retorna log paginado de tentativas
O sistema SHALL retornar lista paginada de `OutboxEventLog` (tentativas de entrega por consumidor: `consumer`, `success`, `response`, `createdAt`) na rota `GET /v1/outbox-events/{id}/logs`. Paginação é obrigatória — um evento com muitos retries pode acumular bastante linha.

#### Scenario: Consulta de log de um evento com múltiplas tentativas
- **WHEN** `GET /v1/outbox-events/{id}/logs` é chamado para um evento com 3 tentativas registradas
- **THEN** retorna HTTP 200 com `data[]` contendo as 3 linhas, ordenadas por `createdAt`

#### Scenario: Evento sem nenhuma tentativa ainda
- **WHEN** `GET /v1/outbox-events/{id}/logs` é chamado para um evento `PENDING` (nunca processado)
- **THEN** retorna HTTP 200 com `data = []`

### Requirement: PUT /v1/outbox-events/{id}/retry só reprocessa evento FAILED
O sistema SHALL reprocessar um `OutboxEvent` (voltar `status` para `PENDING`) só quando o `status` atual é `FAILED`. SHALL rejeitar com HTTP 4XX quando o `status` atual não é `FAILED` (ex.: `PROCESSED`, `PENDING`, `PROCESSING`).

#### Scenario: Retry de evento FAILED é aceito
- **WHEN** `PUT /v1/outbox-events/{id}/retry` é chamado para um evento com `status = FAILED`
- **THEN** retorna HTTP 204 e o evento volta para `status = PENDING`

#### Scenario: Retry de evento PROCESSED é rejeitado
- **WHEN** `PUT /v1/outbox-events/{id}/retry` é chamado para um evento com `status = PROCESSED`
- **THEN** retorna HTTP 4XX e o `status` do evento não muda

#### Scenario: Retry de evento inexistente
- **WHEN** `PUT /v1/outbox-events/{id}/retry` é chamado para um `id` que não existe
- **THEN** retorna HTTP 404

### Requirement: GET /v1/outbox-events/dead-letters retorna lista paginada filtrável
O sistema SHALL retornar lista paginada de `OutboxEventDeadLetter` na rota `GET /v1/outbox-events/dead-letters`, filtrável por `topic` e `errorType`. Paginação é obrigatória.

#### Scenario: Listagem de dead-letters sem filtro
- **WHEN** `GET /v1/outbox-events/dead-letters` é chamado com `paginationFilter` válido
- **THEN** retorna HTTP 200 com `data[]` e `paginatedDTO` preenchidos

#### Scenario: Filtro por errorType
- **WHEN** `GET /v1/outbox-events/dead-letters?errorType=MAX_RETRIES_EXCEEDED` é chamado
- **THEN** retorna HTTP 200 com `data[]` contendo só dead-letters desse `errorType`

### Requirement: Contrato de outbox não expõe criação de evento nem CRUD de tópico
O sistema SHALL NOT expor endpoint de criação de `OutboxEvent` via API pública (eventos nascem só internamente, disparados pelos agregados de domínio). O sistema SHALL NOT expor CRUD de `OutboxTopic` nesta rodada — configuração de tópico permanece interna.

#### Scenario: Não existe POST /v1/outbox-events
- **WHEN** o contrato `ScosOrganization_Outbox.yml` é inspecionado
- **THEN** não existe operação `POST` na rota `/v1/outbox-events`

#### Scenario: Não existe rota de OutboxTopic
- **WHEN** o contrato `ScosOrganization_Outbox.yml` é inspecionado
- **THEN** não existe nenhuma rota `/v1/outbox-topics*`

### Requirement: ScosOrganization_Integration.yml é removido do contrato
O sistema SHALL remover `ScosOrganization_Integration.yml` do contrato público — as rotas `/v1/integrations/keycloak*` e `/v1/integrations/messages-invalid` deixam de existir, pois referenciavam tabelas removidas no schema v2 (`IntegrationKeycloak`/`IntegrationKeycloakLog`/`IntegrationMessageInvalid`).

#### Scenario: Rotas antigas de integração não existem mais
- **WHEN** o diretório `etc/api/organization/` é inspecionado após a mudança
- **THEN** não existe mais o arquivo `ScosOrganization_Integration.yml`

### Requirement: Toda rota nova exige x-authorize e permissão correspondente
O sistema SHALL exigir `x-authorize` em toda rota nova de `ScosOrganization_Outbox.yml`, com entrada correspondente em `ScosOrganizationPermission`: `GET_OUTBOX_EVENT` (para as consultas), `RETRY_OUTBOX_EVENT` (para o retry), `GET_OUTBOX_EVENT_DEAD_LETTER` (para dead-letters).

#### Scenario: Requisição sem permissão é rejeitada
- **WHEN** um cliente sem a permissão `RETRY_OUTBOX_EVENT` chama `PUT /v1/outbox-events/{id}/retry`
- **THEN** retorna HTTP 4XX de autorização
