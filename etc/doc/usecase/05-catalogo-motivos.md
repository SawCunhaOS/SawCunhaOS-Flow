# Regras e Casos de Uso por Endpoint — Catálogo e Motivos

> Parte do conjunto de documentos de regras por endpoint do scos-organization. Consultar **00-indice-central.md** para: modelo de status, catálogo de códigos de erro (`SCOS_VALIDATION_`/`SCOS_*`), chaves de configuração e achados críticos da validação cruzada contra o código-fonte.

---

## 9. Catálogo de Tipos (Catalog)

> `entityType` válidos: `COMPANY` ou `EMPLOYEE`. Cada tipo de endereço/contato se aplica a uma entidade específica.

---

### GET /v1/address-types
**UC-081** | `GET_ADDRESS_TYPE` — lista paginada, filtrável por `entityType`.

### POST /v1/address-types
**UC-082** | `CREATE_ADDRESS_TYPE`

| Campo | O/F | Validação |
| --- | --- | --- |
| `code` | O | único em `SCOS_ADDRESS_TYPE`; ≤ 30 chars |
| `description` | O | ≤ 255 chars |
| `entityType` | O | enum: `COMPANY`, `EMPLOYEE` |

**Use Cases de Sucesso:** UC-S1: campos válidos → `201`
**Use Cases de Erro:** UC-E1: campo ausente/vazio → `400` | UC-E2: `entityType` inválido → `400` | UC-E3: `code` duplicado → `409`

### GET /v1/address-types/{id}
**UC-083** | `GET_ADDRESS_TYPE` — `404` se não existir.

### PUT /v1/address-types/{id}
**UC-084** | `UPDATE_ADDRESS_TYPE` — `code` único excluindo `{id}`. `204`.
**Use Cases de Erro:** UC-E1: ausente → `400` | UC-E2: `code` duplicado → `409` | UC-E3: `{id}` não encontrado → `404`

### PUT /v1/address-types/{id}/enable
**UC-085** | `ENABLE_ADDRESS_TYPE` — `ACTIVE=false → true`. `404`/`422` padrão.

### PUT /v1/address-types/{id}/disable
**UC-086** | `DISABLE_ADDRESS_TYPE` — `ACTIVE=true → false`.

> ⚠️ Inativar um tipo em uso não remove os vínculos existentes — apenas impede novos cadastros com esse tipo.

**Use Cases de Erro:** UC-E1: `{id}` não encontrado → `404` | UC-E2: já inativo → `422`

---

### GET /v1/contact-types
**UC-087** | `GET_CONTACT_TYPE` — lista paginada, filtrável por `entityType`.

### POST /v1/contact-types
**UC-088** | `CREATE_CONTACT_TYPE`

Campos idênticos ao `POST /v1/address-types`. Valida unicidade em `SCOS_CONTACT_TYPE`.

**Use Cases de Sucesso:** UC-S1: → `201`
**Use Cases de Erro:** UC-E1: ausente → `400` | UC-E2: `code` duplicado → `409`

### GET /v1/contact-types/{id}
**UC-089** | `GET_CONTACT_TYPE` — `404` se não existir.

### PUT /v1/contact-types/{id}
**UC-090** | `UPDATE_CONTACT_TYPE` — `code` único excluindo `{id}`. `204`.

### PUT /v1/contact-types/{id}/enable
**UC-091** | `ENABLE_CONTACT_TYPE` — `ACTIVE=false → true`.

### PUT /v1/contact-types/{id}/disable
**UC-092** | `DISABLE_CONTACT_TYPE` — `ACTIVE=true → false`. Mesma nota sobre vínculos existentes.

---

## 10. Motivos de Transição de Status (Reason)

> Todos os módulos Reason têm a mesma estrutura. `entityType` válidos: `COMPANY` ou `EMPLOYEE`.

### 10.1 Motivos de Ativação (Reason Activate)

### GET /v1/reason-activate
**UC-113** | `GET_REASON_ACTIVATE` — lista paginada, filtrável por `entityType`.

### POST /v1/reason-activate
**UC-114** | `CREATE_REASON_ACTIVATE`

| Campo | O/F | Validação |
| --- | --- | --- |
| `code` | O | único em `SCOS_REASON_ACTIVATE`; ≤ 30 chars |
| `description` | O | ≤ 255 chars |
| `entityType` | O | enum: `COMPANY`, `EMPLOYEE` |

**Use Cases de Sucesso:** UC-S1: → `201`
**Use Cases de Erro:** UC-E1: ausente → `400` | UC-E2: `entityType` inválido → `400` | UC-E3: `code` duplicado → `409`

### GET /v1/reason-activate/{id} — **UC-115** | `GET_REASON_ACTIVATE` — `404` se não existir.
### PUT /v1/reason-activate/{id} — **UC-116** | `UPDATE_REASON_ACTIVATE` — `code` único excluindo `{id}`. `204`.
### PUT /v1/reason-activate/{id}/enable — **UC-117** | `ENABLE_REASON_ACTIVATE` — `ACTIVE=false → true`.
### PUT /v1/reason-activate/{id}/disable — **UC-118** | `DISABLE_REASON_ACTIVATE` — `ACTIVE=true → false`.

---

### 10.2 Motivos de Inativação (Reason Inactivate)

### GET /v1/reason-inactivate — **UC-119** | `GET_REASON_INACTIVATE`
### POST /v1/reason-inactivate — **UC-120** | `CREATE_REASON_INACTIVATE` — campos e validações idênticos ao Reason Activate.
### GET /v1/reason-inactivate/{id} — **UC-121** | `GET_REASON_INACTIVATE`
### PUT /v1/reason-inactivate/{id} — **UC-122** | `UPDATE_REASON_INACTIVATE`
### PUT /v1/reason-inactivate/{id}/enable — **UC-123** | `ENABLE_REASON_INACTIVATE`
### PUT /v1/reason-inactivate/{id}/disable — **UC-124** | `DISABLE_REASON_INACTIVATE`

---

### 10.3 Motivos de Bloqueio (Reason Disable)

### GET /v1/reason-disable — **UC-125** | `GET_REASON_DISABLE`
### POST /v1/reason-disable — **UC-126** | `CREATE_REASON_DISABLE`
### GET /v1/reason-disable/{id} — **UC-127** | `GET_REASON_DISABLE`
### PUT /v1/reason-disable/{id} — **UC-128** | `UPDATE_REASON_DISABLE`
### PUT /v1/reason-disable/{id}/enable — **UC-129** | `ENABLE_REASON_DISABLE`
### PUT /v1/reason-disable/{id}/disable — **UC-130** | `DISABLE_REASON_DISABLE`

---

### 10.4 Motivos de Desbloqueio (Reason Enable)

### GET /v1/reason-enable — **UC-131** | `GET_REASON_ENABLE`
### POST /v1/reason-enable — **UC-132** | `CREATE_REASON_ENABLE`
### GET /v1/reason-enable/{id} — **UC-133** | `GET_REASON_ENABLE`
### PUT /v1/reason-enable/{id} — **UC-134** | `UPDATE_REASON_ENABLE`
### PUT /v1/reason-enable/{id}/enable — **UC-135** | `ENABLE_REASON_ENABLE`
### PUT /v1/reason-enable/{id}/disable — **UC-136** | `DISABLE_REASON_ENABLE`

---

