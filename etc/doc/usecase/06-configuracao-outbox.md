# Regras e Casos de Uso por Endpoint — Configuração e Outbox

> Parte do conjunto de documentos de regras por endpoint do scos-organization. Consultar **00-indice-central.md** para: modelo de status, catálogo de códigos de erro (`SCOS_VALIDATION_`/`SCOS_*`), chaves de configuração e achados críticos da validação cruzada contra o código-fonte.

---

## 11. Configuração

### GET /v1/configurations
**UC-077** | `GET_CONFIGURATION` — valores sensíveis mascarados.

### GET /v1/configurations/{id}
**UC-078** | `GET_CONFIGURATION` — por identificador textual da chave. `404` se não existir.

### PUT /v1/configurations/{id}
**UC-079** | `UPDATE_CONFIGURATION`

| Campo | O/F | Validação |
| --- | --- | --- |
| `value` | O | compatível com o `type` da chave |

**Regras:**
1. `value` presente e não vazio.
2. `value` validado conforme `type`: numérico para `INTEGER`, `true`/`false` para `BOOLEAN`, JSON válido para `JSON`.
5. Chave `{id}` deve existir (novas chaves não são criadas via API).

**Chaves consumidas pela lógica desta API:**
| Chave | Tipo | Usada em |
| --- | --- | --- |
| `EMPLOYEE_MIN_AGE` | `INTEGER` | Criação de Funcionário |
| `COMPANY_HIERARCHY_MAX_DEPTH` | `INTEGER` | Criação de Filial |
| `LOGIN_INACTIVITY_TIMEOUT_DAYS` | `INTEGER` | Background: inativação automática de Login |
| `EMPLOYEE_EMAIL_DOMAIN` | `STRING` | Criação/atualização de Funcionário |
| `DEFAULT_COMPANY_ID` | `INTEGER` | Atributos do Keycloak para Login EXTERNAL/SERVICE |

**Use Cases de Sucesso:** UC-S1: `value` compatível com `type` → `204`
**Use Cases de Erro:** UC-E1: `value` ausente/vazio → `400` | UC-E2: `value` incompatível com `type` → `400` `SCOS_CONFIGURATION_002` | UC-E3: chave `{id}` não existe → `400` `SCOS_CONFIGURATION_001`

> ⚠️ Os dois códigos são `HTTP 400`, não `404`/`422` — confirmado por Javadoc no enum (Documento 00, Seção 2.2), contraintuitivo porque "chave não encontrada" normalmente seria `404` em REST. Nenhum dos dois tem texto de mensagem escrito em nenhum idioma ainda (Documento 07, Seção 3) — o comportamento em runtime para o usuário final depende de como o framework trata chave de mensagem ausente.

---

## 12. Outbox

> Substitui o antigo módulo de Integração (`/v1/integrations`). Cobertura genérica: não apenas Keycloak, mas qualquer evento assíncrono publicado pelo sistema.

### GET /v1/outbox-events
**UC não numerado** | `GET_OUTBOX_EVENT` — lista paginada de eventos, filtrável por tópico, status e ID do agregado de origem.

### GET /v1/outbox-events/{id}
**UC não numerado** | `GET_OUTBOX_EVENT` — detalhe do evento com payload e dados de resposta (quando backend é `DIRECT_API`). `404` se não existir.

### GET /v1/outbox-events/{id}/logs
**UC não numerado** | `GET_OUTBOX_EVENT` — lista paginada das tentativas de entrega. `404` se não existir.

### PUT /v1/outbox-events/{id}/retry
**UC não numerado** | `RETRY_OUTBOX_EVENT`

**Regras:**
1. `{id}` deve existir.
2. Status atual deve ser `FAILED`.
7. Ao reprocessar: verifica idempotência antes de chamar o destino (ex.: `KEYCLOAK_ID` já existente = atualizar, não criar).

**Side Effects:** Status → `PENDING`. `RETRY_COUNT` zerado. Processador retoma o fluxo.

**Use Cases de Sucesso:** UC-S1: evento `FAILED` → `204`
**Use Cases de Erro:** UC-E1: `{id}` não encontrado → `404` | UC-E2: status não é `FAILED` → `422`

### GET /v1/outbox-events/dead-letters
**UC não numerado** | `GET_OUTBOX_EVENT_DEAD_LETTER` — lista paginada de eventos que excederam tentativas ou chegaram corrompidos. Somente leitura.

### GET /v1/outbox-topics
**UC-156** | `GET_OUTBOX_TOPIC` — lista paginada de tópicos cadastrados com seu roteamento. Somente leitura.

### GET /v1/outbox-topics/{topic}
**UC-157** | `GET_OUTBOX_TOPIC` — detalhe de roteamento pelo nome do tópico (chave natural). `404` se não existir.
