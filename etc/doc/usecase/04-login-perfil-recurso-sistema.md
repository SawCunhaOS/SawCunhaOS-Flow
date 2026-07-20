# Regras e Casos de Uso por Endpoint — Login, Perfil, Recurso e Sistema

> Parte do conjunto de documentos de regras por endpoint do scos-organization. Consultar **00-indice-central.md** para: modelo de status, catálogo de códigos de erro (`SCOS_VALIDATION_`/`SCOS_*`), chaves de configuração e achados críticos da validação cruzada contra o código-fonte.

---

## 5. Login

### GET /v1/logins
**UC-058** | `GET_LOGIN` — lista paginada, todos os status.

### POST /v1/logins
**UC-057** | `CREATE_LOGIN`

Cria login sem vínculo de funcionário. Tipo deve ser `EXTERNAL` ou `SERVICE`.

| Campo | O/F | Validação |
| --- | --- | --- |
| `login` | O | texto puro; ≤ 255 chars |
| `profileId` | O | FK para `SCOS_PROFILE` |
| `type` | O | enum: `EXTERNAL`, `SERVICE` (EMPLOYEE é rejeitado) |
| `reasonActivateId` | O | FK para `SCOS_REASON_ACTIVATE` |

**Regras em ordem:**
1. Todos presentes e não vazios.
2. `type` ∈ {`EXTERNAL`, `SERVICE`}.
3. `login` ≤ 255.
4. `login` único em `SCOS_LOGIN` (todos os status).
5. `profileId` deve existir. `reasonActivateId` deve existir em `SCOS_REASON_ACTIVATE`.
6. `profileId` com `ACTIVE=true`. `reasonActivateId` com `ACTIVE=true`.

**Side Effects:** Status → `ACTIVE` imediatamente (fire-and-forget). Saga Keycloak `TYPE=CREATE` via Outbox. Insere linha em `SCOS_LOGIN_STATUS_HISTORY`.

**Use Cases de Sucesso:**
- UC-S1: Login `EXTERNAL` com perfil válido → `201` com status `ACTIVE`
- UC-S2: Login `SERVICE` → `201`

**Use Cases de Erro:**
- UC-E1: `type = EMPLOYEE` → `400` "tipo não permitido neste endpoint"
- UC-E2: `login` já existe → `409`
- UC-E3: `profileId` inexistente → `404`
- UC-E4: `profileId` com `ACTIVE=false` → `422`
- UC-E5: `reasonActivateId` inativo → `422`

---

### GET /v1/logins/me
**UC-059** | `GET_LOGIN_INFO` — retorna contexto completo do usuário autenticado: dados pessoais, empresa, filial, perfil, permissões acumuladas (perfil principal + adicionais). `401` se token inválido.

### GET /v1/logins/{id}
**UC-060** | `GET_LOGIN` — `404` se não existir.

---

### PUT /v1/logins/{id}
**UC-062** | `UPDATE_LOGIN`

| Campo | O/F | Validação |
| --- | --- | --- |
| `login` | O | texto puro; ≤ 255 chars |

**Regras:**
3. `login` ≤ 255.
4. `login` único excluindo `{id}`.
5. `{id}` deve existir.

**Side Effects:** Saga Keycloak `TYPE=UPDATE`.

**Use Cases de Sucesso:** UC-S1: → `204`
**Use Cases de Erro:** UC-E1: `login` ausente/vazio → `400` | UC-E2: `login` duplicado → `409` | UC-E3: `{id}` não encontrado → `404`

---

### PUT /v1/logins/{id}/enable
**UC-141** | `ENABLE_LOGIN` | Transição `INACTIVE → ACTIVE`

| Campo | O/F | Validação |
| --- | --- | --- |
| `reasonId` | O | FK para `SCOS_REASON_ACTIVATE` |
| `observation` | F | — |

**Regras:** `{id}` deve existir. Status deve ser `INACTIVE`. `reasonId` com `ACTIVE=true`.

**Side Effects:** Insere linha em `SCOS_LOGIN_STATUS_HISTORY`. Saga Keycloak `TYPE=UPDATE (enabled=true)`.

**Use Cases de Sucesso:** UC-S1: → `204`
**Use Cases de Erro:** UC-E1: `{id}` não encontrado → `404` | UC-E2: não está `INACTIVE` → `422` `SCOS_LOGIN_013` | UC-E3: `reasonId` inativo → `422`

---

### PUT /v1/logins/{id}/disable
**UC-142** | `DISABLE_LOGIN` | Transição `ACTIVE → INACTIVE`

| Campo | O/F | Validação |
| --- | --- | --- |
| `reasonId` | O | FK para `SCOS_REASON_INACTIVATE` |
| `observation` | F | — |

**Regras:** `{id}` deve existir. Status deve ser `ACTIVE`. `reasonId` com `ACTIVE=true`.

**Side Effects:** Insere linha em `SCOS_LOGIN_STATUS_HISTORY`. Saga Keycloak `TYPE=UPDATE (enabled=false)`.

**Use Cases de Sucesso:** UC-S1: → `204`
**Use Cases de Erro:** UC-E1: `{id}` não encontrado → `404` | UC-E2: não está `ACTIVE` → `422` `SCOS_LOGIN_013`

---

### PUT /v1/logins/{id}/block
**UC-063** | `UPDATE_LOGIN_STATUS` | Transição qualquer → `BLOCKED`

> Permissão compartilhada com `unblock` — mesmo valor de `x-authorize` nos dois endpoints (diferente do padrão `ENABLE_*`/`DISABLE_*` distintos usados em outras transições).

| Campo | O/F | Validação |
| --- | --- | --- |
| `reasonId` | O | FK para `SCOS_REASON_DISABLE` |
| `observation` | F | — |

**Regras:** `{id}` deve existir. Status deve ser diferente de `BLOCKED`. `reasonId` com `ACTIVE=true`.

> ⚠️ `SCOS_LOGIN_013` (usado nas três transições acima) não tem texto de mensagem escrito em nenhum idioma hoje (Documento 07, Seção 3) — existe só no enum Java com Javadoc.

**Side Effects:** Insere linha em `SCOS_LOGIN_STATUS_HISTORY`. Saga Keycloak `TYPE=UPDATE (enabled=false)`.

**Use Cases de Sucesso:** UC-S1: Login `ACTIVE` → `BLOCKED` → `204` | UC-S2: Login `INACTIVE` → `BLOCKED` → `204`
**Use Cases de Erro:** UC-E1: `{id}` não encontrado → `404` | UC-E2: já `BLOCKED` → `422` `SCOS_LOGIN_011` | UC-E3: `reasonId` inativo → `422`

---

### PUT /v1/logins/{id}/unblock
**UC-064** | `UPDATE_LOGIN_STATUS` | Transição `BLOCKED → ACTIVE`

| Campo | O/F | Validação |
| --- | --- | --- |
| `reasonId` | O | FK para `SCOS_REASON_ENABLE` |
| `observation` | F | — |

**Regras:** `{id}` deve existir. Status deve ser `BLOCKED`. `reasonId` com `ACTIVE=true`.

**Side Effects:** Insere linha em `SCOS_LOGIN_STATUS_HISTORY`. Saga Keycloak `TYPE=UPDATE (enabled=true)`.

**Use Cases de Sucesso:** UC-S1: → `204`
**Use Cases de Erro:** UC-E1: `{id}` não encontrado → `404` | UC-E2: não está `BLOCKED` → `422` `SCOS_LOGIN_013`

---

### GET /v1/logins/{id}/status-history
**UC-143** | `GET_LOGIN_STATUS_HISTORY` — lista paginada, mais recente primeiro. `404` se `{id}` não existir.

---

### PUT /v1/logins/{id}/profile/{profileId}
**UC-065** | `UPDATE_LOGIN` — substitui o perfil principal.

**Regras:**
5. `{id}` deve existir. `profileId` deve existir em `SCOS_PROFILE`.
6. `profileId` deve ter `ACTIVE=true`.

> ⚠️ Novas permissões **não** valem imediatamente: `vw_login_context`/`vw_authority_response` são materialized views atualizadas a cada 30 minutos via `pg_cron` (Documento Central, Seção 3.3) — não há refresh síncrono na troca de perfil.

**Use Cases de Sucesso:** UC-S1: → `204`
**Use Cases de Erro:** UC-E1: `{id}` não encontrado → `404` | UC-E2: `profileId` inexistente → `404` | UC-E3: `profileId` inativo → `422`

---

### GET /v1/logins/{id}/profiles
**UC-152** | `GET_LOGIN_PROFILE` — lista paginada dos perfis adicionais (`SCOS_LOGIN_PROFILE`). Distinto do perfil principal (`SCOS_LOGIN.profileId`). `404` se `{id}` não existir.

---

### POST /v1/logins/{id}/profiles/{profileId}
**UC-153** | `CREATE_LOGIN_PROFILE` — sem request body.

Adiciona perfil adicional. As permissões dos perfis adicionais **devem** somar-se ao perfil principal — ⚠️ **bug confirmado:** a view que resolve permissões hoje não une `SCOS_LOGIN_PROFILE` (Documento Central, Seção 3.2); até a correção, essa soma não se reflete em `GET /v1/logins/me` nem na validação de autoridade.

**Regras:**
5. `{id}` deve existir. `profileId` deve existir em `SCOS_PROFILE`.
6. `profileId` deve ter `ACTIVE=true`.
7. `profileId` não pode ser o mesmo que o perfil principal do login (`SCOS_LOGIN.profileId`). `(loginId, profileId)` não pode já existir em `SCOS_LOGIN_PROFILE`.

**Use Cases de Sucesso:** UC-S1: → `201`
**Use Cases de Erro:** UC-E1: `{id}` não encontrado → `404` | UC-E2: `profileId` inexistente → `404` | UC-E3: `profileId` = perfil principal → `422` "já é o perfil principal" | UC-E4: associação já existe → `409`

---

### DELETE /v1/logins/{id}/profiles/{profileId}
**UC-154** | `DELETE_LOGIN_PROFILE` — remove perfil adicional. Idempotente: remover associação inexistente não gera erro. `404` apenas se o login `{id}` não existir.

---

### GET /v1/employees/{employeeId}/logins
**UC-055** | `GET_LOGIN` — logins do funcionário, todos os status. `404` se `employeeId` não existir.

---

### POST /v1/employees/{employeeId}/logins
**UC-054** | `CREATE_EMPLOYEE_LOGIN`

| Campo | O/F | Validação |
| --- | --- | --- |
| `login` | O | texto puro; ≤ 255 chars |
| `profileId` | O | FK para `SCOS_PROFILE` |
| `reasonActivateId` | O | FK para `SCOS_REASON_ACTIVATE` |

**Regras em ordem:**
1. Todos presentes e não vazios.
3. `login` ≤ 255.
4. `login` único em `SCOS_LOGIN` (todos os status).
5. `employeeId` deve existir. `profileId` deve existir. `reasonActivateId` deve existir.
6. Funcionário deve estar `ACTIVE`. `profileId` com `ACTIVE=true`. `reasonActivateId` com `ACTIVE=true`.

**Side Effects:** Status → `ACTIVE` imediatamente. Saga Keycloak `TYPE=CREATE` com atributos de empresa e filial do funcionário. Insere linha em `SCOS_LOGIN_STATUS_HISTORY`.

**Use Cases de Sucesso:**
- UC-S1: Funcionário `ACTIVE` + perfil ativo + motivo válido → `201` com status `ACTIVE`

**Use Cases de Erro:**
- UC-E1: `login` duplicado → `409`
- UC-E2: `employeeId` não encontrado → `404`
- UC-E3: Funcionário com status `INACTIVE` ou `DISABLED` → `422` "funcionário não ativo"
- UC-E4: `profileId` com `ACTIVE=false` → `422`
- UC-E5: `reasonActivateId` inativo → `422`

### GET /v1/employees/{employeeId}/logins/{id}
**UC-056** | `GET_LOGIN` — login específico com perfil e status. `404` se não existir.

---

## 6. Perfil (Profile)

### GET /v1/profiles
**UC-068** | `GET_PROFILE` — lista paginada.

### POST /v1/profiles
**UC-067** | `CREATE_PROFILE`

| Campo | O/F | Validação |
| --- | --- | --- |
| `code` | O | único em `SCOS_PROFILE`; ≤ 30 chars |
| `description` | O | ≤ 30 chars |

**Use Cases de Sucesso:** UC-S1: → `201` (perfil sem recursos, não concede acesso)
**Use Cases de Erro:** UC-E1: ausente/vazio → `400` | UC-E2: `code` duplicado → `409`

### GET /v1/profiles/{id}
**UC-069** | `GET_PROFILE` — inclui recursos atribuídos. `404` se não existir.

### PUT /v1/profiles/{id}
**UC-070** | `UPDATE_PROFILE`

`code` único excluindo `{id}`. Mesmas validações. `204`.

**Use Cases de Erro:** UC-E1: campo ausente → `400` | UC-E2: `code` duplicado → `409` | UC-E3: `{id}` não encontrado → `404`

### DELETE /v1/profiles/{id}
**UC-073** | `DELETE_PROFILE` — exclusão lógica (`ACTIVE=false`).

**Regras:**
1. `{id}` deve existir.
2. Não pode existir Login com `profileId = {id}` e status `ACTIVE`.
3. Perfil não pode ter `IS_SYSTEM=true`.

**Use Cases de Sucesso:** UC-S1: sem logins ativos, não é perfil de sistema → `204`
**Use Cases de Erro:** UC-E1: `{id}` não encontrado → `404` | UC-E2: logins ativos vinculados → `422` | UC-E3: `IS_SYSTEM=true` → `422` "perfil protegido"

### PUT /v1/profiles/{id}/enable
**UC-071** | `ENABLE_PROFILE` — `ACTIVE=false → true`. `{id}` deve existir e estar inativo.

**Use Cases de Erro:** UC-E1: `{id}` não encontrado → `404` | UC-E2: já ativo → `422`

### PUT /v1/profiles/{id}/disable
**UC-072** | `DISABLE_PROFILE` — `ACTIVE=true → false`. Bloqueado se IS_SYSTEM ou logins ativos.

**Use Cases de Erro:** UC-E1: `{id}` não encontrado → `404` | UC-E2: já inativo → `422` | UC-E3: logins `ACTIVE` vinculados → `422` | UC-E4: `IS_SYSTEM=true` → `422` "perfil protegido"

### GET /v1/profiles/{id}/resources
**UC-074** | `GET_PROFILE` — lista recursos atribuídos. `404` se `{id}` não existir.

### PUT /v1/profiles/{id}/resources
**UC-075** | `UPDATE_PROFILE`

| Campo | O/F | Validação |
| --- | --- | --- |
| `resourceIds` | O | array de UUIDs (pode ser vazio — remove tudo) |

**Regras:**
1. `resourceIds` presente.
2. Cada item em formato UUID válido.
5. Cada `resourceId` deve existir em `SCOS_RESOURCE`.
6. Cada `resourceId` deve estar `ACTIVE`.
7. Substituição total em transação única.

**Use Cases de Sucesso:**
- UC-S1: Lista com UUIDs válidos e ativos → `204`
- UC-S2: Lista vazia `[]` → `204` (remove todas as permissões do perfil)

**Use Cases de Erro:**
- UC-E1: `resourceIds` ausente → `400`
- UC-E2: Item com formato UUID inválido → `400` (rejeição de tipo pelo framework na desserialização — não encontrei código `SCOS_VALIDATION_`/`SCOS_` dedicado para este caso; é coerção de tipo, anterior à camada de validação de negócio)
- UC-E3: `resourceId` inexistente → `404`
- UC-E4: `resourceId` inativo → `422`

---

## 7. Recurso (Resource)

### GET /v1/resources
**UC-076** | `GET_RESOURCE` — lista paginada. **Somente leitura** — cadastro via scos-registry (gRPC).

---

## 8. Sistema (System)

### GET /v1/systems
**UC-158** | `GET_SYSTEM` — lista paginada de sistemas externos cadastrados. Somente leitura.

### GET /v1/systems/{id}
**UC-159** | `GET_SYSTEM` — detalhe de um sistema. `SECRET_KEY` nunca é retornada. `404` se não existir.

---

## 9. Comportamento Sistêmico — Inativação Automática por Desuso

Sem endpoint dedicado — processo de background, documentado aqui porque afeta diretamente o status de Login exposto por `GET /v1/logins`/`{id}`/`me`.

Um Login com status `ACTIVE` deve transicionar automaticamente para `INACTIVE` quando não é utilizado por um período definido na configuração `LOGIN_INACTIVITY_TIMEOUT_DAYS` (Documento 06).

**Estado atual do schema:** a coluna `SCOS_LOGIN.LAST_USED_AT` (`TIMESTAMPTZ`, `NULL` = nunca usado) **já existe**, com um índice parcial dedicado (`(LAST_USED_AT) WHERE STATUS = 'ACTIVE'`) especificamente para dar suporte a um job de varredura que ignora logins já `INACTIVE`/`BLOCKED`. Isso resolve a lacuna de schema que constava em versões anteriores deste documento.

**O que ainda não está definido:** o próprio comentário do schema é explícito — *"mecanismo de atualização (endpoint dedicado, evento de Outbox, etc.) ainda não definido, coluna adicionada só para destravar o schema"*. Ou seja: existe onde guardar o dado, mas não existe hoje nenhum endpoint REST documentado nos arquivos 01-06, nem menção de evento de Outbox (Documento 06), que escreva em `LAST_USED_AT`. Prováveis candidatos — a confirmar com o time — são a camada de autenticação (a cada login bem-sucedido via Keycloak) ou a cadeia de validação de autoridade (a cada chamada, gRPC ou `GET /v1/logins/me`); nenhum dos dois está confirmado no código-fonte que revisei.

