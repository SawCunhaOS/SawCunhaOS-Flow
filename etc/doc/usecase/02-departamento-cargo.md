# Regras e Casos de Uso por Endpoint — Departamento e Cargo

> Parte do conjunto de documentos de regras por endpoint do scos-organization. Consultar **00-indice-central.md** para: modelo de status, catálogo de códigos de erro (`SCOS_VALIDATION_`/`SCOS_*`), chaves de configuração e achados críticos da validação cruzada contra o código-fonte.

---

## 2. Departamento (Department)

### GET /v1/departments
**UC-022** | `GET_DEPARTMENT` — lista paginada.

### POST /v1/departments
**UC-021** | `CREATE_DEPARTMENT`

| Campo | O/F | Validação |
| --- | --- | --- |
| `code` | O | único em `SCOS_DEPARTMENT`; ≤ 30 chars |
| `description` | O | ≤ 255 chars |

**Use Cases de Sucesso:** UC-S1: campos válidos → `201`
**Use Cases de Erro:** UC-E1: campo ausente/vazio → `400` | UC-E2: `code` duplicado → `409`

### GET /v1/departments/{id}
**UC-023** | `GET_DEPARTMENT` — `404` se não existir.

### PUT /v1/departments/{id}
**UC-024** | `UPDATE_DEPARTMENT` — `code` único excluindo `{id}`. `204`.

### PUT /v1/departments/{id}/enable
**UC-025** | `ENABLE_DEPARTMENT`

**Regras:** `{id}` deve existir. Deve estar `ACTIVE=false`.

**Use Cases de Sucesso:** UC-S1: → `204`
**Use Cases de Erro:** UC-E1: `{id}` não encontrado → `404` | UC-E2: já está ativo → `422` `SCOS_DEPARTMENT_004`

### PUT /v1/departments/{id}/disable
**UC-026** | `DISABLE_DEPARTMENT`

**Regras:** `{id}` deve existir. Deve estar `ACTIVE=true`. Não pode ter Cargos ativos.

**Use Cases de Sucesso:** UC-S1: → `204`
**Use Cases de Erro:** UC-E1: `{id}` não encontrado → `404` | UC-E2: já inativo → `422` `SCOS_DEPARTMENT_005` | UC-E3: cargos ativos → `422` `SCOS_DEPARTMENT_003`

---

## 3. Cargo (Position)

### GET /v1/positions
**UC-029** | `GET_POSITION` — lista paginada, filtrável por `departmentId`.

### POST /v1/positions
**UC-028** | `CREATE_POSITION`

| Campo | O/F | Validação |
| --- | --- | --- |
| `code` | O | único em `SCOS_POSITION`; ≤ 30 chars |
| `description` | O | ≤ 30 chars |
| `departmentId` | O | FK para `SCOS_DEPARTMENT` |

**Regras em ordem:**
1–3. Campos obrigatórios, tamanhos.
4. `code` único em `SCOS_POSITION`.
5. `departmentId` deve existir.
6. Departamento deve ter `ACTIVE=true`.

**Use Cases de Sucesso:** UC-S1: `201`
**Use Cases de Erro:** UC-E1: campo ausente → `400` | UC-E2: `code` duplicado → `409` `SCOS_POSITION_002` | UC-E3: `departmentId` não encontrado → `404` `SCOS_DEPARTMENT_001` | UC-E4: departamento inativo → `422` `SCOS_DEPARTMENT_006`

### GET /v1/positions/{id}
**UC-030** | `GET_POSITION` — inclui dados do departamento. `404` se não existir.

### PUT /v1/positions/{id}
**UC-031** | `UPDATE_POSITION`

Mesmos campos que o `POST`. `code` único excluindo `{id}`. Se `departmentId` muda: novo deve existir e estar `ACTIVE=true`.

**Use Cases de Sucesso:** UC-S1: `204`
**Use Cases de Erro:** UC-E1: `{id}` não encontrado → `404` | UC-E2: `code` duplicado → `409` | UC-E3: novo `departmentId` inativo → `422`

### DELETE /v1/positions/{id}
**UC-034** | `DELETE_POSITION` — exclusão lógica (`ACTIVE=false`).

**Regra:** não pode existir Funcionário com `positionId = {id}` e status `ACTIVE`.

**Use Cases de Sucesso:** UC-S1: → `204`
**Use Cases de Erro:** UC-E1: `{id}` não encontrado → `404` | UC-E2: funcionários ativos vinculados → `422` `SCOS_POSITION_003`

### PUT /v1/positions/{id}/enable
**UC-032** | `ENABLE_POSITION` — `ACTIVE=false → true`.

**Use Cases de Erro:** UC-E1: `{id}` não encontrado → `404` | UC-E2: já está ativo → `422` `SCOS_POSITION_004`

### PUT /v1/positions/{id}/disable
**UC-033** | `DISABLE_POSITION` — `ACTIVE=true → false`. Bloqueado se funcionários `ACTIVE` vinculados.

**Use Cases de Erro:** UC-E1: `{id}` não encontrado → `404` | UC-E2: já inativo → `422` `SCOS_POSITION_005` | UC-E3: funcionários ativos vinculados → `422` `SCOS_POSITION_003`

> ⚠️ Nenhum dos códigos `SCOS_DEPARTMENT_004`/`005`/`006` nem `SCOS_POSITION_004`/`005` tem texto de mensagem escrito em português ou inglês hoje (Documento 07, Seção 3) — só existem no enum Java com Javadoc descrevendo o cenário. Comportamento de runtime pro usuário final não confirmado.

---

## 3.1 Jornada de Trabalho do Cargo (Position Work Schedule)

Funciona como **template**: define o horário padrão do Cargo, um registro por dia da semana. Não é copiado automaticamente para nenhum Funcionário — a cópia para `SCOS_EMPLOYEE_WORK_SCHEDULE` (Documento 03, Seção 4.1) é uma ação explícita da aplicação, não um trigger de banco. **Editar o template do Cargo depois não afeta** funcionários que já têm sua própria cópia — os dois conjuntos de dados ficam independentes a partir do momento da cópia.

### GET /v1/positions/{positionId}/work-schedule
**UC-144** | `GET_POSITION_WORK_SCHEDULE` — retorna array direto (sem paginação), um item por dia da semana cadastrado. `404` se `positionId` não existir.

### POST /v1/positions/{positionId}/work-schedule
**UC-145** | `CREATE_POSITION_WORK_SCHEDULE`

| Campo | O/F | Validação |
| --- | --- | --- |
| `dayOfWeek` | O | enum: `MONDAY`..`SUNDAY` |
| `startTime` | O | formato `HH:mm` ou `HH:mm:ss` (`SCOS_VALIDATION_007` se inválido) |
| `lunchStart` | O | mesmo formato; posterior a `startTime` |
| `lunchEnd` | O | mesmo formato; posterior a `lunchStart` |
| `endTime` | O | mesmo formato; posterior a `lunchEnd` |

**Regras em ordem:**
1. Todos presentes.
2. Ordem cronológica: `startTime < lunchStart < lunchEnd < endTime`.
5. `positionId` deve existir.
7. Não pode existir registro para este `(positionId, dayOfWeek)`.

**Use Cases de Sucesso:** UC-S1: `201`
**Use Cases de Erro:** UC-E1: campo ausente → `400` | UC-E2: ordem de horários inválida → `422` | UC-E3: `(positionId, dayOfWeek)` já existe → `409` | UC-E4: `positionId` não encontrado → `404`

### PUT /v1/positions/{positionId}/work-schedule/{dayOfWeek}
**UC-146** | `UPDATE_POSITION_WORK_SCHEDULE`

Campos: `startTime`, `lunchStart`, `lunchEnd`, `endTime` (todos obrigatórios). Mesma regra de ordem cronológica.

**Regras:** `(positionId, dayOfWeek)` deve existir.

**Use Cases de Sucesso:** UC-S1: → `204`
**Use Cases de Erro:** UC-E1: combinação não encontrada → `404` | UC-E2: ordem inválida → `422`

### DELETE /v1/positions/{positionId}/work-schedule/{dayOfWeek}
**UC-147** | `DELETE_POSITION_WORK_SCHEDULE` — remove o dia da semana do template. `204`. `404` se não existir.

---

## 3.2 Motivo de Mudança de Cargo (Reason Position Change)

### GET /v1/reason-position-change
**UC-106** | `GET_REASON_POSITION_CHANGE` — lista paginada.

### POST /v1/reason-position-change
**UC-107** | `CREATE_REASON_POSITION_CHANGE`

| Campo | O/F | Validação |
| --- | --- | --- |
| `code` | O | único em `SCOS_REASON_POSITION_CHANGE`; ≤ 30 chars |
| `description` | O | ≤ 255 chars |

**Use Cases de Sucesso:** UC-S1: `201`
**Use Cases de Erro:** UC-E1: campo ausente → `400` | UC-E2: `code` duplicado → `409`

### GET /v1/reason-position-change/{id}
**UC-108** | `GET_REASON_POSITION_CHANGE` — `404` se não existir.

### PUT /v1/reason-position-change/{id}
**UC-109** | `UPDATE_REASON_POSITION_CHANGE` — `code` único excluindo `{id}`. `204`.

### PUT /v1/reason-position-change/{id}/enable
**UC-110** | `ENABLE_REASON_POSITION_CHANGE` — `ACTIVE=false → true`. `422` se já ativo.

### PUT /v1/reason-position-change/{id}/disable
**UC-111** | `DISABLE_REASON_POSITION_CHANGE` — `ACTIVE=true → false`. `422` se já inativo.
