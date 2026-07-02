## Why

O schema v2 removeu `IntegrationKeycloak`/`IntegrationKeycloakLog`/`IntegrationMessageInvalid` (já removidas da camada JPA em `atualizacao-entidades-jpa-liquibase-v2`) e introduziu um padrão de outbox genérico multi-backend (`SCOS_OUTBOX_EVENT`/`SCOS_OUTBOX_EVENT_LOG`/`SCOS_OUTBOX_EVENT_DEAD_LETTER`). O contrato público (`ScosOrganization_Integration.yml`) ainda expõe `/v1/integrations/keycloak*` e `/v1/integrations/messages-invalid`, endpoints que referenciam tabelas que não existem mais no banco.

## What Changes

**Escopo desta rodada: somente o contrato OpenAPI (YAML).** Implementação de código (permissões, use cases, delegates, testes) fica para uma change futura.

- Criar `ScosOrganization_Outbox.yml` com endpoints de monitoria/leitura + retry pontual do outbox genérico (cobre qualquer backend/consumidor, não só Keycloak)
- **BREAKING**: Remover `ScosOrganization_Integration.yml` do contrato inteiro — `/v1/integrations/keycloak*` e `/v1/integrations/messages-invalid` deixam de existir; qualquer client (mesmo interno, mesmo dev/homolog) que dependa desses endpoints quebra
- `OutboxTopic` (config de tópico) permanece configuração interna, sem endpoint exposto nesta rodada — decisão explícita
- Sem `POST` de criação de evento — eventos nascem internamente, disparados pelos agregados de domínio

## Capabilities

### New Capabilities
- `outbox-event-monitoring`: consulta paginada de eventos do outbox (`GET /v1/outbox-events`, `GET /v1/outbox-events/{id}`), consulta paginada de log de tentativas por evento (`GET /v1/outbox-events/{id}/logs`), reprocessamento pontual (`PUT /v1/outbox-events/{id}/retry`), e consulta paginada de dead-letters (`GET /v1/outbox-events/dead-letters`)

### Modified Capabilities
(nenhuma — `model-integration` e `schema-integration-keycloak`, capabilities existentes em `openspec/specs/`, descrevem a camada JPA/banco do domínio Keycloak antigo, já removida em change anterior (`atualizacao-entidades-jpa-liquibase-v2`); esta change é só contrato OpenAPI, não altera requisito de domínio/banco nenhum. A remoção de `ScosOrganization_Integration.yml` está documentada acima em "What Changes"/abaixo em "Impact" por não ter uma capability formal correspondente em `openspec/specs/` para dar delta)

## Impact

- `etc/api/organization/ScosOrganization_Outbox.yml` — arquivo novo (paths, schemas: `OutboxEvent`, `OutboxEventLog`, `OutboxEventDeadLetter`, enum `OutboxEventStatus`)
- `etc/api/organization/ScosOrganization_Integration.yml` — **removido** (breaking change de contrato)
- `ScosOrganizationPermission` (código) — fora de escopo desta change; adição de `GET_OUTBOX_EVENT`/`RETRY_OUTBOX_EVENT`/`GET_OUTBOX_EVENT_DEAD_LETTER` e remoção das permissões antigas de Keycloak ficam pendentes para a implementação futura
- Sem impacto de banco — schema v2 já existe (`adequacao-liquibase-domain-model-v2`, completo)
