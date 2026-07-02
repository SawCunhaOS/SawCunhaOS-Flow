## Context

O schema v2 (`adequacao-liquibase-domain-model-v2`, completo) introduziu um padrão de outbox genérico multi-backend (`SCOS_OUTBOX_TOPIC`/`SCOS_OUTBOX_EVENT`/`SCOS_OUTBOX_EVENT_LOG`/`SCOS_OUTBOX_EVENT_DEAD_LETTER`), substituindo o antigo domínio dedicado de integração Keycloak (`IntegrationKeycloak`/`IntegrationKeycloakLog`/`IntegrationMessageInvalid`, tabelas removidas). A camada JPA já foi sincronizada (`atualizacao-entidades-jpa-liquibase-v2`, completo). O contrato público (`ScosOrganization_Integration.yml`) ainda referencia as tabelas antigas e precisa ser substituído.

## Goals / Non-Goals

**Goals:**
- Expor consulta/monitoria do outbox genérico — eventos, log de tentativas por consumidor, dead-letters
- Permitir reprocessamento pontual (`retry`) de um evento `FAILED`
- Remover o contrato antigo (`ScosOrganization_Integration.yml`), que referencia tabelas inexistentes

**Non-Goals:**
- CRUD de `OutboxTopic` (configuração de tópico/backend) — fica interno, sem endpoint público nesta rodada
- Criação de evento via API — eventos nascem só internamente, disparados pelos agregados de domínio
- Implementação do relay/worker que processa os eventos — decisão de infraestrutura/código, não de contrato
- Implementação de código (permissões, use case, domain, delegates, testes) da própria monitoria de outbox — fora de escopo desta change; entra em change futura separada

## Decisions

- **Arquivo novo `ScosOrganization_Outbox.yml`, não reaproveitamento de `Integration.yml`**: mais claro que é um contrato novo e genérico, desacoplado do conceito antigo "integração Keycloak". Alternativa descartada: renomear/reaproveitar `Integration.yml` — rejeitada por manter o nome legado associado a um conceito que não existe mais.
- **Nome do enum de status: `OutboxEventStatus`** (não `StatusOutboxEvent`) — alinhado ao nome do enum de domínio já decidido em `atualizacao-entidades-jpa-liquibase-v2` (RF-14), evitando um schema OpenAPI com nome diferente do enum Java equivalente. Valores (`PENDING`/`PROCESSING`/`PROCESSED`/`FAILED`) confirmados contra o `CHECK` real do banco (`chk_outbox_event_status`, `scos-organization-boot/src/main/resources/db/changelog/checks/checks.yml`).
- **`payload`/`responseData` como `type: object` livre (sem schema fixo)**: o payload varia por tópico/consumidor (JSONB no banco); um schema tipado geraria expectativa incorreta de formato único. Alternativa descartada: schema `oneOf` por tópico — rejeitada por complexidade desproporcional ao valor (endpoint é só monitoria/leitura).
- **`OutboxTopic` fora de escopo**: decidido com o usuário manter configuração de tópico interna (sem endpoint), já que não há necessidade operacional de expô-la nesta rodada.
- **3 listas (`outbox-events`, `.../logs`, `dead-letters`) todas paginadas, sem exceção**: volume potencialmente alto (eventos + retries), ao contrário da exceção deliberada de `jornada-trabalho-position-employee` (teto de 7 registros) — aqui a paginação é ainda mais necessária que no resto do contrato, não uma dispensa.
- **Sem `POST` de criação**: eventos são efeito colateral de ações de domínio (ex.: `Login.activate()` dispara evento pro Keycloak via outbox), nunca uma operação direta do cliente da API.

## Risks / Trade-offs

- [Remover `Integration.yml` quebra qualquer client existente que chame `/v1/integrations/keycloak*`, mesmo em dev/homolog] → Comunicar a remoção antes de publicar; não há período de transição (sistema sem produção, mesma decisão já tomada nas outras 6 ideias irmãs desta rodada)
- [`retry` chamado num evento que não é `FAILED` reprocessaria algo indevido] → Use case rejeita com `4XX` explícito antes de tocar o banco, não deixa a regra de negócio virar erro genérico
- [`payload`/`responseData` sem schema tipado dificultam validação client-side] → Aceito conscientemente — é dado de monitoria/leitura, não um contrato de escrita; documentar isso na `description` de cada schema
- [Permissões antigas de Keycloak (`RETRY_INTEGRATION_KEYCLOAK` ou equivalente) somem do `ScosOrganizationPermission` junto com o arquivo] → Qualquer perfil que já tivesse essa permissão atribuída perde a concessão; reatribuir com as novas permissões de outbox na implementação
