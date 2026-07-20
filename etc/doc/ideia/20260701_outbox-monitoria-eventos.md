# Outbox — Monitoria de Eventos

**Data**: 2026-07-01
**Status**: 🔄 Em Análise
**Tipo**: 🆕 Nova Feature

---

## ⚠️ Princípio SRP — Uma Funcionalidade por Ideia

> **Regra**: Uma ideia = uma funcionalidade. Features independentes → arquivos separados.

- **Nome da funcionalidade**: `outbox-monitoria-eventos`
- **Resumo em uma frase**: Criar contrato novo de monitoria de eventos do Outbox (`OutboxEvent`/`OutboxEventLog`/`OutboxEventDeadLetter`), substituindo `ScosOrganization_Integration.yml` (Keycloak-específico, tabelas removidas no schema v2).

**Checklist SRP**:
- [x] Esta ideia cobre exatamente uma funcionalidade — expor o outbox genérico como sucessor do contrato de integração antigo
- [x] Não mistura com histórico de status, fiscal, cargo, jornada, catálogo de endereço/contato ou perfil — ideias próprias
- [x] O nome é específico

---

## 1️⃣ Visão

### Problema
O schema v2 removeu `IntegrationKeycloak`/`IntegrationKeycloakLog`/`IntegrationMessageInvalid` (já removidas da camada JPA na rodada anterior) e introduziu um padrão de outbox genérico multi-backend: `SCOS_OUTBOX_TOPIC` (config de tópico — `PGMQ`/`KAFKA`/`DIRECT_API`), `SCOS_OUTBOX_EVENT` (evento com payload JSONB, status, retry), `SCOS_OUTBOX_EVENT_LOG` (log de tentativas de entrega por consumidor) e `SCOS_OUTBOX_EVENT_DEAD_LETTER` (eventos que excederam retries ou chegaram corrompidos). `ScosOrganization_Integration.yml` ainda expõe `/v1/integrations/keycloak*` e `/v1/integrations/messages-invalid` — endpoints que referenciam tabelas que não existem mais.

### Objetivo
Existe um novo contrato de monitoria de eventos do outbox — consulta de eventos, reprocessamento (retry) e consulta de dead-letters — cobrindo qualquer backend/consumidor (não só Keycloak). `ScosOrganization_Integration.yml` é removido/substituído.

### Fora de Escopo
- CRUD de `OutboxTopic` (configuração de tópicos) — decidido como configuração interna, sem endpoint exposto nesta rodada
- Implementação do relay/worker que processa os eventos — já é decisão de infraestrutura/código, não de contrato
- Histórico de status, dados fiscais, cargo/contrato, jornada, catálogo de endereço/contato, perfil adicional — ideias próprias

---

## 2️⃣ Requisitos

### Funcionais
- [ ] **RF-01**: Criar `GET /v1/outbox-events` — lista paginada, filtrável por `topic`, `status` (`PENDING`/`PROCESSING`/`PROCESSED`/`FAILED`), `aggregateId`
- [ ] **RF-02**: Criar `GET /v1/outbox-events/{id}` — detalhe do evento, incluindo `payload`, `responseData` (quando `DIRECT_API`), `retryCount`, `message`
- [ ] **RF-03**: Criar `GET /v1/outbox-events/{id}/logs` — lista **paginada** de `OutboxEventLog` (tentativas por consumidor: `consumer`, `success`, `response`, `createdAt`) — evento com muitos retries pode acumular bastante linha
- [ ] **RF-04**: Criar `PUT /v1/outbox-events/{id}/retry` — reprocessa evento `FAILED` (equivalente ao antigo `retry` do Keycloak, agora genérico)
- [ ] **RF-05**: Criar `GET /v1/outbox-events/dead-letters` — lista paginada de `OutboxEventDeadLetter`, filtrável por `topic`, `errorType`
- [ ] **RF-06**: Remover `ScosOrganization_Integration.yml` do contrato (ou substituir integralmente pelo novo arquivo, conforme decisão de nomenclatura no `/propose`)
- [ ] **RF-07** (achado na revisão): Toda rota nova precisa de `x-authorize` + entrada correspondente em `ScosOrganizationPermission` — `GET_OUTBOX_EVENT`, `RETRY_OUTBOX_EVENT`, `GET_OUTBOX_EVENT_DEAD_LETTER`; remover as permissões antigas de Keycloak (`RETRY_INTEGRATION_KEYCLOAK` ou equivalente) junto com o arquivo antigo

### Não-Funcionais
- [ ] **RNF-01**: Endpoints são de monitoria/leitura + ação pontual de retry — sem `POST` de criação de evento (eventos nascem internamente, disparados pelos agregados de domínio, não via API pública)
- [ ] **RNF-02**: `OutboxTopic` não é exposto nesta rodada — decisão explícita do usuário
- [ ] **RNF-03** (achado na revisão): DTOs seguem o padrão SCOS — `GetOutboxEventResponse { data }` pro detalhe; `GetAllOutboxEventsResponse`, `GetAllOutboxEventLogsResponse`, `GetAllOutboxEventDeadLettersResponse` (todos `{ data: array, paginatedDTO }`) pras 3 listas (`RF-01`, `RF-03`, `RF-05`), todas com `paginationFilter` (query, obrigatório) — potencial de volume alto (eventos + retries) torna a paginação aqui ainda mais necessária que no resto do contrato, não uma exceção
- [ ] **RNF-03**: Nenhuma alteração de schema no banco — schema v2 já existe

---

## 3️⃣ Arquitetura

### Componentes Afetados
```
etc/api/organization/
├── ScosOrganization_Outbox.yml           (novo — substitui Integration.yml)
│   ├── paths: GET /outbox-events, GET /outbox-events/{id}, GET /outbox-events/{id}/logs,
│   │          PUT /outbox-events/{id}/retry, GET /outbox-events/dead-letters
│   └── schemas: OutboxEvent, OutboxEventLog, OutboxEventDeadLetter, OutboxEventStatus (enum)
└── ScosOrganization_Integration.yml      (remover)
```

### Detalhamento de Campos por Arquivo YAML

| Arquivo YAML | Schema/Path | Campo | Tipo | Nota |
|---|---|---|---|---|
| `ScosOrganization_Outbox.yml` (novo) | `OutboxEvent` (novo) | `id`, `topic`, `aggregateId`, `payload`, `status`, `retryCount`, `message`, `responseData` | int64 / string / string / `object` (JSONB livre) / `enum OutboxEventStatus` / int32 / string / `object` (só quando `DIRECT_API`) | payload/responseData sem schema fixo (RE-01) |
| `ScosOrganization_Outbox.yml` (novo) | `OutboxEventStatus` (enum novo) | `PENDING`, `PROCESSING`, `PROCESSED`, `FAILED` | enum | usado em `status` e no filtro de `GET /outbox-events`; nome alinhado ao enum de domínio já decidido na idea JPA (RF-14) — evita schema OpenAPI com nome diferente do enum Java |
| `ScosOrganization_Outbox.yml` (novo) | `OutboxEventLog` (novo) | `consumer`, `success`, `response`, `createdAt` | string / boolean / string / datetime | tentativa de entrega por consumidor |
| `ScosOrganization_Outbox.yml` (novo) | `OutboxEventDeadLetter` (novo) | `topic`, `errorType`, (+ dados do evento original) | string / string / — | filtrável por `topic`, `errorType` |
| `ScosOrganization_Outbox.yml` (novo) | `GetOutboxEventResponse`, `GetAllOutboxEventsResponse`, `GetAllOutboxEventLogsResponse`, `GetAllOutboxEventDeadLettersResponse` (novos) | `data`, `paginatedDTO` | `$ref` / `$ref ScosPaginated` | padrão SCOS; todas as 3 listas paginadas (RNF-03) |
| `ScosOrganization_Outbox.yml` (novo) | `GET /v1/outbox-events` (filtros `topic`,`status`,`aggregateId`), `GET /{id}`, `GET /{id}/logs`, `PUT /{id}/retry`, `GET /dead-letters` (filtros `topic`,`errorType`) | — | — | sem `POST` de criação — eventos nascem internamente (RNF-01) |

### Fluxo Principal
```
PUT /outbox-events/{id}/retry
  → valida evento existe e status=FAILED
  → usecase reseta retryCount ou incrementa, volta status para PENDING
  → relay/worker (fora do escopo desta ideia) reprocessa
```

### Decisões Técnicas
| Decisão | Escolha | Alternativa Descartada | Motivo |
|---------|---------|------------------------|--------|
| Escopo do contrato | Só monitoria de eventos (`OutboxEvent*`) | Monitoria + CRUD de `OutboxTopic` | Decidido com o usuário — configuração de tópico fica interna por ora |
| Nome do arquivo | Novo arquivo dedicado, substitui `Integration.yml` | Renomear/reaproveitar `Integration.yml` | Mais claro que é um contrato novo, desacoplado do conceito antigo "integração Keycloak" |

### Banco de Dados
- **Impacto**: ❌ Não — schema já existe (`adequacao-liquibase-domain-model-v2`, completo)

---

## 4️⃣ Implementação

*Detalhamento de arquivos/tarefas fica para a fase de `/openspec-propose`.*

### Riscos e Edge Cases
1. `OutboxEvent.payload`/`responseData` são JSONB — no contrato OpenAPI, provavelmente `type: object` livre (sem schema fixo, já que o payload varia por tópico/consumidor); documentar isso explicitamente para não gerar expectativa de schema tipado
2. `retry` em evento que não seja `FAILED` — precisa de regra de negócio clara (rejeitar com 4XX) para não reprocessar evento já `PROCESSED`
3. Remover `Integration.yml` é breaking change de contrato — qualquer client (mesmo interno) que dependa de `/v1/integrations/keycloak*` quebra; comunicar antes de publicar

---

## 📎 Referências
- Domain model: `etc/database/domain_model.md`
- Migrations v2: `openspec/changes/adequacao-liquibase-domain-model-v2/`
- Remoção do domínio Keycloak dedicado (JPA, completo): `openspec/changes/atualizacao-entidades-jpa-liquibase-v2/`

---
