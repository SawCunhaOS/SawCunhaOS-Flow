# Regras e Casos de Uso por Endpoint — Funcionário

> Parte do conjunto de documentos de regras por endpoint do scos-organization. Consultar **00-indice-central.md** para: modelo de status, catálogo de códigos de erro (`SCOS_VALIDATION_`/`SCOS_*`), chaves de configuração e achados críticos da validação cruzada contra o código-fonte.

---

## 4. Funcionário (Employee)

### GET /v1/employees
**UC-036** | `GET_EMPLOYEE` — lista paginada com filtros.

---

### POST /v1/employees
**UC-035** | `CREATE_EMPLOYEE`

| Campo | O/F | Validação |
| --- | --- | --- |
| `name` | O | ≤ 250 chars |
| `nameTreatment` | O | ≤ 100 chars |
| `taxIdentifier` | O | 11 dígitos; DV CPF válido |
| `email` | O | formato válido (`SCOS_VALIDATION_005`); domínio = `EMPLOYEE_EMAIL_DOMAIN`; ≤ 255 chars |
| `birthDate` | O | data válida; não futura |
| `dateOfHiring` | O | data válida; pode ser futura |
| `contractType` | O | enum: `CLT`, `PJ`, `ESTAGIO`, `TEMPORARIO` |
| `probationEndDate` | F | data futura; obrigatório se `contractType=CLT` ou `ESTAGIO` com período de experiência |
| `observation` | F | — |
| `supervisorId` | F | FK para `SCOS_EMPLOYEE` |
| `companyId` | O | FK para `SCOS_COMPANY` |
| `positionId` | O | FK para `SCOS_POSITION` |
| `reasonActivateId` | O | FK para `SCOS_REASON_ACTIVATE` |

**Regras em ordem:**
1. Campos obrigatórios presentes e não vazios.
2. CPF com DV válido (`SCOS_VALIDATION_011` se inválido). `email` em formato válido (`SCOS_VALIDATION_005`) e domínio = `EMPLOYEE_EMAIL_DOMAIN` (config). `birthDate` não futura. `contractType` no enum.
3. Tamanhos conforme tabela.
4. `taxIdentifier` único em `SCOS_EMPLOYEE` (todos os status). `email` único em `SCOS_EMPLOYEE` (todos os status).
5. `companyId`, `positionId`, `reasonActivateId` devem existir. Se `supervisorId` informado: deve existir.
6. `companyId` deve estar `ACTIVE`. `positionId` deve ter `ACTIVE=true`. `reasonActivateId` com `ACTIVE=true` e `entityType=EMPLOYEE`. Se `supervisorId`: deve estar `ACTIVE`.
7. `birthDate` satisfaz `EMPLOYEE_MIN_AGE` em relação a `dateOfHiring`. `dateOfHiring` não anterior a `birthDate`.

**Side Effects:** Insere linha em `SCOS_EMPLOYEE_STATUS_HISTORY` e `SCOS_EMPLOYEE_POSITION_HISTORY`.

**Use Cases de Sucesso:**
- UC-S1: Funcionário CLT criado com todos os campos válidos → `201`
- UC-S2: Funcionário PJ sem `probationEndDate` → `201`
- UC-S3: `dateOfHiring` futura (pré-cadastro) → `201`

**Use Cases de Erro:**
- UC-E1: `taxIdentifier` com DV inválido → `400` `SCOS_VALIDATION_011`
- UC-E2: `email` com domínio incorreto → `422` "domínio não permitido"
- UC-E3: `taxIdentifier` já existe → `409` "CPF já cadastrado"
- UC-E4: `email` já existe → `409` "e-mail já cadastrado"
- UC-E5: `companyId` inexistente → `404`
- UC-E6: `companyId` com status `INACTIVE` ou `DISABLED` → `422` "empresa não ativa"
- UC-E7: `positionId` com `ACTIVE=false` → `422` "cargo inativo"
- UC-E8: `supervisorId` não `ACTIVE` → `422` "supervisor não ativo"
- UC-E9: Idade abaixo de `EMPLOYEE_MIN_AGE` → `422` "funcionário não atingiu a idade mínima"
- UC-E10: `reasonActivateId` incompatível → `422`

---

> ⚠️ **Defeito de contrato:** o path `/v1/employees/rehire` no YAML também declara `GET` e `PUT` com resumos idênticos aos de `/v1/employees/{id}` — cópia acidental, não comportamento pretendido. Reportar ao time de API. Apenas o `POST` abaixo é real.

### POST /v1/employees/rehire
**UC-155** | `REHIRE_EMPLOYEE`

Recontrata funcionário `INACTIVE` localizando-o pelo CPF.

| Campo | O/F | Validação |
| --- | --- | --- |
| `taxIdentifier` | O | CPF do funcionário a recontratar |
| `companyId` | O | pode ser igual ou diferente da empresa anterior |
| `positionId` | O | pode ser igual ou diferente do cargo anterior |
| `contractType` | O | enum: `CLT`, `PJ`, `ESTAGIO`, `TEMPORARIO` |
| `reasonActivateId` | O | FK para `SCOS_REASON_ACTIVATE` |
| `reasonPositionChangeId` | O | FK para `SCOS_REASON_POSITION_CHANGE` |
| `supervisorId` | F | novo supervisor (nulo = sem supervisor) |
| `contractType` | O | novo tipo de contrato |
| `probationEndDate` | F | novo fim de experiência |
| `dateOfRehire` | F | nova `DATE_OF_HIRING`; omitido = mantém a original |
| `observation` | F | — |

**Regras em ordem:**
1. Campos obrigatórios presentes.
2. CPF com DV válido.
4. `taxIdentifier` deve localizar exatamente um registro em `SCOS_EMPLOYEE`.
5. Funcionário localizado deve ter status `INACTIVE`. `companyId`, `positionId`, `reasonActivateId`, `reasonPositionChangeId` devem existir.
6. `companyId` deve estar `ACTIVE`. `positionId` com `ACTIVE=true`. `reasonActivateId` com `ACTIVE=true` e `entityType=EMPLOYEE`. `reasonPositionChangeId` com `ACTIVE=true`. Se `supervisorId`: deve existir e estar `ACTIVE`.

**Side Effects:**
1. Status → `ACTIVE`. Insere linha em `SCOS_EMPLOYEE_STATUS_HISTORY`.
2. Insere linha em `SCOS_EMPLOYEE_POSITION_HISTORY` (novo cargo, mesmo que seja o mesmo de antes) com `start_date`; a trigger `fn_close_previous_position` fecha automaticamente a linha aberta anterior, setando `end_date = start_date` da nova linha (sem gap nem sobreposição).
3. Reativa os Logins vinculados? ❌ **Não** — Logins permanecem no status em que estão.

**Use Cases de Sucesso:**
- UC-S1: Funcionário `INACTIVE` + campos válidos → `200` com dados atualizados
- UC-S2: Recontratação para empresa diferente → `200`
- UC-S3: `dateOfRehire` omitido → mantém `dateOfHiring` original

**Use Cases de Erro:**
- UC-E1: `taxIdentifier` não encontra nenhum funcionário → `404`
- UC-E2: Funcionário localizado tem status `ACTIVE` ou `DISABLED` → `422` "funcionário não está inativo"
- UC-E3: `companyId` não `ACTIVE` → `422`
- UC-E4: `reasonActivateId` inativo/incompatível → `422`

---

### GET /v1/employees/{id}
**UC-037** | `GET_EMPLOYEE` — dados completos com cargo, empresa, supervisor. `404` se não existir.

---

### PUT /v1/employees/{id}
**UC-038** | `UPDATE_EMPLOYEE`

Atualiza dados pessoais. `taxIdentifier`, `companyId`, `positionId` **não são editáveis** aqui.

| Campo | O/F | Validação |
| --- | --- | --- |
| `name` | O | ≤ 250 chars |
| `nameTreatment` | O | ≤ 100 chars |
| `email` | O | formato (`SCOS_VALIDATION_005`) + domínio `EMPLOYEE_EMAIL_DOMAIN` |
| `birthDate` | O | não futura |
| `dateOfHiring` | O | pode ser futura |
| `contractType` | O | enum CLT/PJ/ESTAGIO/TEMPORARIO |
| `probationEndDate` | F | — |
| `observation` | F | — |

**Regras em ordem:**
4. `email` único excluindo o próprio `{id}`.
7. Se `birthDate` alterada: satisfaz `EMPLOYEE_MIN_AGE`.

**Use Cases de Sucesso:** UC-S1: dados válidos → `204`
**Use Cases de Erro:** UC-E1: campo obrigatório ausente → `400` | UC-E2: `email` duplicado → `409` | UC-E3: domínio inválido → `422`

---

### PUT /v1/employees/{id}/enable
**UC-039** | `ENABLE_EMPLOYEE` | Transição `INACTIVE → ACTIVE`

| Campo | O/F | Validação |
| --- | --- | --- |
| `reasonId` | O | FK para `SCOS_REASON_ACTIVATE` |
| `observation` | F | — |

**Regras:** `{id}` deve existir. Status deve ser `INACTIVE`. `reasonId` com `ACTIVE=true` e `entityType=EMPLOYEE`.

> **Reativar Employee não reativa os Logins vinculados** — eles permanecem no status atual.

**Side Effects:** Insere linha em `SCOS_EMPLOYEE_STATUS_HISTORY`.

**Use Cases de Sucesso:** UC-S1: → `204`
**Use Cases de Erro:** UC-E1: `{id}` não encontrado → `404` | UC-E2: não está `INACTIVE` → `422` `SCOS_EMPLOYEE_001` | UC-E3: `reasonId` inativo/incompatível → `422`

---

### PUT /v1/employees/{id}/disable
**UC-040** | `DISABLE_EMPLOYEE` | Transição `ACTIVE → INACTIVE`

| Campo | O/F | Validação |
| --- | --- | --- |
| `reasonId` | O | FK para `SCOS_REASON_INACTIVATE` |
| `observation` | F | — |

**Regras:** `{id}` deve existir. Status deve ser `ACTIVE`. `reasonId` com `ACTIVE=true` e `entityType=EMPLOYEE`.

**Side Effects:**
1. Insere linha em `SCOS_EMPLOYEE_STATUS_HISTORY`.
2. Todos os Logins vinculados com status `ACTIVE` → `INACTIVE`. Dispara Saga Keycloak `TYPE=UPDATE (enabled=false)` para cada um.

**Use Cases de Sucesso:** UC-S1: → `204`
**Use Cases de Erro:** UC-E1: `{id}` não encontrado → `404` | UC-E2: não está `ACTIVE` → `422` `SCOS_EMPLOYEE_001` | UC-E3: `reasonId` inativo/incompatível → `422`

---

### PUT /v1/employees/{id}/block
**UC-043** | `BLOCK_EMPLOYEE` | Transição qualquer → `DISABLED`

| Campo | O/F | Validação |
| --- | --- | --- |
| `reasonId` | O | FK para `SCOS_REASON_DISABLE` |
| `observation` | F | — |

**Regras:** `{id}` deve existir. `reasonId` com `ACTIVE=true` e `entityType=EMPLOYEE`.

**Side Effects:**
1. Insere linha em `SCOS_EMPLOYEE_STATUS_HISTORY`.
2. Logins `ACTIVE` → `INACTIVE`. Dispara Saga Keycloak `TYPE=UPDATE (enabled=false)`.

**Use Cases de Sucesso:** UC-S1: qualquer status → `204`
**Use Cases de Erro:** UC-E1: `{id}` não encontrado → `404` | UC-E2: `reasonId` inativo/incompatível → `422`

---

### PUT /v1/employees/{id}/unblock
**UC-139** | `UNBLOCK_EMPLOYEE` | Transição `DISABLED → ACTIVE`

| Campo | O/F | Validação |
| --- | --- | --- |
| `reasonId` | O | FK para `SCOS_REASON_ENABLE` |
| `observation` | F | — |

**Regras:** `{id}` deve existir. Status deve ser `DISABLED`. `reasonId` com `ACTIVE=true` e `entityType=EMPLOYEE`.

> **Desbloquear Employee não reativa os Logins** — devem ser reativados individualmente.

**Side Effects:** Insere linha em `SCOS_EMPLOYEE_STATUS_HISTORY`.

**Use Cases de Sucesso:** UC-S1: → `204`
**Use Cases de Erro:** UC-E1: `{id}` não encontrado → `404` | UC-E2: não está `DISABLED` → `422` `SCOS_EMPLOYEE_001` | UC-E3: `reasonId` inativo/incompatível → `422`

---

### GET /v1/employees/{id}/status-history
**UC-140** | `GET_EMPLOYEE_STATUS_HISTORY` — lista paginada, mais recente primeiro. `404` se `{id}` não existir.

---

### PATCH /v1/employees/{id}/transfer
**UC-041** | `TRANSFER_EMPLOYEE`

| Campo | O/F | Validação |
| --- | --- | --- |
| `companyId` | F | nova empresa/filial |
| `positionId` | F | novo cargo |
| `supervisorId` | F | novo supervisor |
| `dateOfTransfer` | F | data válida |
| `reasonPositionChangeId` | O apenas se `positionId` muda | FK para `SCOS_REASON_POSITION_CHANGE` |

**Regras em ordem:**
1. Ao menos um campo opcional deve ser informado (payload vazio → `400`). Se `positionId` muda: `reasonPositionChangeId` obrigatório.
2. `dateOfTransfer` é data válida.
5. Campos informados devem existir.
6. `companyId` deve estar `ACTIVE`. `positionId` com `ACTIVE=true`. Se `supervisorId`: deve existir e estar `ACTIVE`. Se `reasonPositionChangeId`: deve ter `ACTIVE=true`.
7. `companyId` diferente do atual. `supervisorId` não pode ser o próprio funcionário nem subordinado dele.

**Side Effects:**
1. Insere linha em `SCOS_EMPLOYEE_POSITION_HISTORY` (sempre, independente de qual campo mudou), com `start_date = dateOfTransfer` (ou data atual, se omitido); `fn_close_previous_position` fecha a linha aberta anterior com `end_date = start_date` da nova.
2. Se `companyId` mudou: Saga Keycloak `TYPE=UPDATE` com novos atributos de empresa/filial.

**Use Cases de Sucesso:**
- UC-S1: Mudança de cargo com motivo → `204` + linha em position history
- UC-S2: Mudança só de supervisor sem `positionId` → `204` (sem `reasonPositionChangeId`)
- UC-S3: Mudança de empresa + cargo + supervisor → `204`

**Use Cases de Erro:**
- UC-E1: Payload totalmente vazio → `400`
- UC-E2: `positionId` muda mas `reasonPositionChangeId` ausente → `400`
- UC-E3: `companyId` igual ao atual → `422`
- UC-E4: `supervisorId` é subordinado do funcionário → `422`
- UC-E5: `companyId` não `ACTIVE` → `422`

---

### GET /v1/employees/{id}/hierarchy
**UC-042** | `GET_EMPLOYEE` — cadeia ascendente de supervisores. `404` se `{id}` não existir.

### GET /v1/employees/{id}/subordinates
**UC-080** | `GET_EMPLOYEE` — funcionários que reportam diretamente. `404` se `{id}` não existir.

### GET /v1/employees/{id}/position-history
**UC-112** | `GET_EMPLOYEE_POSITION_HISTORY` — histórico paginado de cargos. Linha com `endDate=null` é a posição atual. `404` se `{id}` não existir.

---

### GET /v1/employees/{employeeId}/contacts
**UC-044** | `GET_EMPLOYEE_CONTACT`

### POST /v1/employees/{employeeId}/contacts
**UC-045** | `CREATE_EMPLOYEE_CONTACT`

| Campo | O/F | Validação |
| --- | --- | --- |
| `contactTypeId` | O | FK para `SCOS_CONTACT_TYPE` |
| `phone` | O | formato telefônico; ≤ 50 chars |

**Regras:**
4. `phone` único para `employeeId` (`UNIQUE(EMPLOYEE_ID, PHONE)`). `contactTypeId` único para `employeeId` (`UNIQUE(EMPLOYEE_ID, CONTACT_TYPE_ID)`).
5. `employeeId` deve existir. `contactTypeId` deve existir em `SCOS_CONTACT_TYPE`.
6. `contactTypeId` com `ACTIVE=true` e `entityType=EMPLOYEE`.

**Use Cases de Sucesso:** UC-S1: → `201`
**Use Cases de Erro:** UC-E1: campo ausente → `400` | UC-E2: `phone` duplicado para funcionário → `409` | UC-E3: `contactTypeId` duplicado para funcionário → `409` | UC-E4: `contactTypeId` inativo/incompatível → `422` | UC-E5: `employeeId` não encontrado → `404`

### GET /v1/employees/{employeeId}/contacts/{id} — **UC-046** | `GET_EMPLOYEE_CONTACT`
### PUT /v1/employees/{employeeId}/contacts/{id} — **UC-047** | `UPDATE_EMPLOYEE_CONTACT` — mesmas regras, exclui `{id}` na unicidade. `204`.
### DELETE /v1/employees/{employeeId}/contacts/{id} — **UC-048** | `DELETE_EMPLOYEE_CONTACT` — `204`.

---

### GET /v1/employees/{employeeId}/addresses
**UC-049** | `GET_EMPLOYEE_ADDRESS`

### POST /v1/employees/{employeeId}/addresses
**UC-050** | `CREATE_EMPLOYEE_ADDRESS`

> `latitude`/`longitude` são **string** neste endpoint (diferente do endereço de Empresa). Validação numérica fica na aplicação.

| Campo | O/F | Validação |
| --- | --- | --- |
| `addressTypeId` | O | FK para `SCOS_ADDRESS_TYPE` |
| `number` | O | — |
| `complement` | F | — |
| `latitude` | O | string representando número entre -90 e 90 |
| `longitude` | O | string representando número entre -180 e 180 |
| `addressId` | O | ID do serviço externo |

**Regras:**
4. `addressTypeId` único para `employeeId` (`UNIQUE(EMPLOYEE_ID, ADDRESS_TYPE_ID)`).
5. `employeeId` deve existir. `addressTypeId` deve existir em `SCOS_ADDRESS_TYPE`.
6. `addressTypeId` com `ACTIVE=true` e `entityType=EMPLOYEE`.
7. `GEOLOCATION = POINT(longitude, latitude)`.

**Use Cases de Sucesso:** UC-S1: → `201`
**Use Cases de Erro:** UC-E1: campo ausente → `400` | UC-E2: `latitude`/`longitude` inválidos → `400` | UC-E3: `addressTypeId` duplicado → `409` | UC-E4: `addressTypeId` inativo/incompatível → `422`

### GET /v1/employees/{employeeId}/addresses/{id} — **UC-051** | `GET_EMPLOYEE_ADDRESS`
### PUT /v1/employees/{employeeId}/addresses/{id} — **UC-052** | `UPDATE_EMPLOYEE_ADDRESS` — `addressId` não editável. `204`.
### DELETE /v1/employees/{employeeId}/addresses/{id} — **UC-053** | `DELETE_EMPLOYEE_ADDRESS` — `204`.

---

## 4.1 Jornada de Trabalho do Funcionário (Employee Work Schedule)

É a **fonte de verdade efetiva** do horário do funcionário, populada por cópia do template do Cargo (Documento 02, Seção 3.1) — cópia feita pela aplicação, não automática. A partir daí é editável livremente, sem qualquer relação futura com o template de origem. **Um dia sem linha aqui significa "não definido"** — não é fallback para o horário do Cargo, é ausência de dado mesmo. Não há histórico: uma atualização sobrescreve via `UPDATE`, sem guardar o valor anterior.

### GET /v1/employees/{employeeId}/work-schedule
**UC-148** | `GET_EMPLOYEE_WORK_SCHEDULE` — retorna array direto por dia da semana. `404` se `employeeId` não existir.

### POST /v1/employees/{employeeId}/work-schedule
**UC-149** | `CREATE_EMPLOYEE_WORK_SCHEDULE`

Campos: `dayOfWeek` (enum `MONDAY..SUNDAY`), `startTime`, `lunchStart`, `lunchEnd`, `endTime` (todos obrigatórios, formato `HH:mm` ou `HH:mm:ss` — ambos aceitos, `SCOS_VALIDATION_007` se inválido). Ordem: `startTime < lunchStart < lunchEnd < endTime`.

**Regras:**
5. `employeeId` deve existir.
7. `(employeeId, dayOfWeek)` único.

**Use Cases de Sucesso:** UC-S1: → `201`
**Use Cases de Erro:** UC-E1: campo ausente → `400` | UC-E2: ordem inválida → `422` | UC-E3: `(employeeId, dayOfWeek)` já existe → `409`

### PUT /v1/employees/{employeeId}/work-schedule/{dayOfWeek}
**UC-150** | `UPDATE_EMPLOYEE_WORK_SCHEDULE` — mesma regra de ordem. `(employeeId, dayOfWeek)` deve existir. `204`.

### DELETE /v1/employees/{employeeId}/work-schedule/{dayOfWeek}
**UC-151** | `DELETE_EMPLOYEE_WORK_SCHEDULE` — remove o dia. `204`. `404` se não existir.
