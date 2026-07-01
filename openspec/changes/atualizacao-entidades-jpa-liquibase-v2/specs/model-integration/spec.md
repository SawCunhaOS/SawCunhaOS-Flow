## REMOVED Requirements

### Requirement: Entidade IntegrationKeycloak criada conforme schema do banco
**Reason**: `SCOS_INTEGRATION_KEYCLOAK` foi removida no schema v2 (`adequacao-liquibase-domain-model-v2`) — substituída pelo mecanismo genérico de outbox (`model-outbox`), que cobre qualquer integração assíncrona ou direta, não só Keycloak.
**Migration**: Remover `IntegrationKeycloak` e `IntegrationKeycloakRepository`. Novas integrações usam `OutboxTopic`/`OutboxEvent` (ver `model-outbox`); campos específicos de Keycloak (`realm`, `email`, `username`) migram para dentro de `OutboxEvent.payload` (JSONB).

### Requirement: Entidade IntegrationKeycloakLog criada como tabela imutável
**Reason**: `SCOS_INTEGRATION_KEYCLOAK_LOG` foi removida no schema v2 — substituída por `SCOS_OUTBOX_EVENT_LOG`, genérico para qualquer consumidor (`CONSUMER` identifica o worker).
**Migration**: Remover `IntegrationKeycloakLog` e `IntegrationKeycloakLogRepository`. Usar `OutboxEventLog` (ver `model-outbox`).

### Requirement: Entidade IntegrationMessageInvalid criada como tabela imutável
**Reason**: `SCOS_INTEGRATION_MESSAGE_INVALID` foi removida no schema v2 — substituída por `SCOS_OUTBOX_EVENT_DEAD_LETTER`, com mais contexto para reprocessamento manual (guarda `TOPIC`, `SOURCE`, `ERROR_TYPE`, `RETRY_COUNT`, não só o texto cru).
**Migration**: Remover `IntegrationMessageInvalid` e `IntegrationMessageInvalidRepository`. Usar `OutboxEventDeadLetter` (ver `model-outbox`).
