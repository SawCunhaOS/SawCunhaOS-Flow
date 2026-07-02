## 1. Contrato OpenAPI

- [x] 1.1 Criar `etc/api/organization/ScosOrganization_Outbox.yml` com info/servers/tags padrão
- [x] 1.2 Definir enum `OutboxEventStatus` (`PENDING`/`PROCESSING`/`PROCESSED`/`FAILED`)
- [x] 1.3 Definir schema `OutboxEvent` (`id`, `topic`, `aggregateId`, `payload: object`, `status`, `retryCount`, `message`, `responseData: object`)
- [x] 1.4 Definir schema `OutboxEventLog` (`consumer`, `success`, `response`, `createdAt`)
- [x] 1.5 Definir schema `OutboxEventDeadLetter` (`topic`, `errorType`, dados do evento original)
- [x] 1.6 Definir `GetOutboxEventResponse { data }`
- [x] 1.7 Definir `GetAllOutboxEventsResponse`, `GetAllOutboxEventLogsResponse`, `GetAllOutboxEventDeadLettersResponse` (`{ data: array, paginatedDTO }`)
- [x] 1.8 Path `GET /v1/outbox-events` (filtros `topic`, `status`, `aggregateId`, `paginationFilter` obrigatório) + `x-authorize: [GET_OUTBOX_EVENT]`
- [x] 1.9 Path `GET /v1/outbox-events/{id}` + `x-authorize: [GET_OUTBOX_EVENT]`
- [x] 1.10 Path `GET /v1/outbox-events/{id}/logs` (`paginationFilter` obrigatório) + `x-authorize: [GET_OUTBOX_EVENT]`
- [x] 1.11 Path `PUT /v1/outbox-events/{id}/retry` (204, sem body) + `x-authorize: [RETRY_OUTBOX_EVENT]`
- [x] 1.12 Path `GET /v1/outbox-events/dead-letters` (filtros `topic`, `errorType`, `paginationFilter` obrigatório) + `x-authorize: [GET_OUTBOX_EVENT_DEAD_LETTER]`
- [x] 1.13 Remover `etc/api/organization/ScosOrganization_Integration.yml` do repositório
