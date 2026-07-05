# Regras e Casos de Uso por Endpoint — Empresa

> Parte do conjunto de documentos de regras por endpoint do scos-organization. Consultar **00-indice-central.md** para: modelo de status, catálogo de códigos de erro (`SCOS_VALIDATION_`/`SCOS_*`), chaves de configuração e achados críticos da validação cruzada contra o código-fonte.

---

## 1. Empresa (Company)

---

### GET /v1/companies
**UC-004** | `GET_COMPANY`

Lista empresas e filiais com paginação e filtros.

**Use Cases de Sucesso:**
- UC-S1: Listagem com paginação padrão retorna `200` com `paginatedDTO`
- UC-S2: Filtro por status e/ou nome retorna apenas os registros correspondentes

**Use Cases de Erro:**
- UC-E1: Token ausente ou expirado → `401`

---

### POST /v1/companies
**UC-001/002** | `CREATE_COMPANY`

Cria empresa matriz (`parentCompanyId` nulo) ou filial.

| Campo | O/F | Validação |
| --- | --- | --- |
| `name` | O | ausente → `SCOS_VALIDATION_003`; vazio → `SCOS_VALIDATION_001`; ≤ 250 chars |
| `nameTreatment` | O | ausente → `SCOS_VALIDATION_003`; vazio → `SCOS_VALIDATION_001`; ≤ 100 chars |
| `taxIdentifier` | O | ausente → `SCOS_VALIDATION_003`; 14 chars alfanuméricos; DV válido (CNPJ Alfa) |
| `foundationDate` | O | ausente → `SCOS_VALIDATION_003`; data válida, não futura |
| `sectorOfActivity` | O | ausente → `SCOS_VALIDATION_003`; pertence à lista fixa de setores; ≤ 100 chars |
| `reasonActivateId` | O | ausente → `SCOS_VALIDATION_003`; FK para `SCOS_REASON_ACTIVATE` |
| `observation` | F | — |
| `parentCompanyId` | F | FK para `SCOS_COMPANY` |
| `legalNatureId` | F | FK para `SCOS_LEGAL_NATURE` |
| `cnaePrincipalId` | F | FK para `SCOS_CNAE` |
| `stateRegistration` | F | texto livre ou literal `"ISENTO"` |
| `municipalRegistration` | F | — |

**Regras em ordem:**
1. Todos os campos obrigatórios presentes e não vazios.
2. `taxIdentifier`: 14 chars alfanuméricos (`A–Z`, `0–9`); posições 13–14 numéricas; DV válido pelo algoritmo CNPJ Alfa. `foundationDate`: data válida, não futura. `sectorOfActivity` na lista permitida.
3. Limites de tamanho conforme tabela acima.
4. `taxIdentifier` único em `SCOS_COMPANY` (todos os status) — colisão retorna `SCOS_COMPANY_002`.
5. `reasonActivateId` deve existir em `SCOS_REASON_ACTIVATE`. Se `parentCompanyId` informado: deve existir. Se `legalNatureId` informado: deve existir. Se `cnaePrincipalId` informado: deve existir.
6. `reasonActivateId` deve ter `ACTIVE=true` e `entityType=COMPANY`. Se `parentCompanyId`: empresa mãe deve ser `ACTIVE`.
7. Se `parentCompanyId`: profundidade resultante ≤ `COMPANY_HIERARCHY_MAX_DEPTH` (contando da raiz).

**Side Effects:** Insere linha em `SCOS_COMPANY_STATUS_HISTORY` com `newStatus=ACTIVE` e `reasonActivateId` informado.

**Use Cases de Sucesso:**
- UC-S1: Criação de empresa matriz → `201` com dados da empresa
- UC-S2: Criação de filial com `parentCompanyId` válido → `201`
- UC-S3: Criação com dados fiscais opcionais (`legalNatureId`, `cnaePrincipalId`) → `201`

**Use Cases de Erro:**
- UC-E1: `taxIdentifier` ausente → `400` `SCOS_VALIDATION_003`
- UC-E2: `taxIdentifier` com DV inválido → `400` `SCOS_VALIDATION_010`
- UC-E3: `taxIdentifier` já cadastrado → `409` `SCOS_COMPANY_002`
- UC-E4: `reasonActivateId` inexistente → `404` "motivo não encontrado"
- UC-E5: `reasonActivateId` com `ACTIVE=false` → `422` "motivo inativo"
- UC-E6: `reasonActivateId` com `entityType=EMPLOYEE` → `422` "motivo incompatível com a entidade"
- UC-E7: `parentCompanyId` inexistente → `404` "empresa mãe não encontrada"
- UC-E8: Empresa mãe com status `INACTIVE` ou `DISABLED` → `422` "empresa mãe não está ativa"
- UC-E9: Profundidade excede `COMPANY_HIERARCHY_MAX_DEPTH` → `422` "limite de hierarquia atingido"

---

### GET /v1/companies/{id}
**UC-003** | `GET_COMPANY`

**Use Cases de Sucesso:**
- UC-S1: Empresa encontrada → `200` com dados completos

**Use Cases de Erro:**
- UC-E1: `{id}` não encontrado → `404`

---

### PUT /v1/companies/{id}
**UC-005** | `UPDATE_COMPANY`

Atualiza dados cadastrais. `parentCompanyId` não é editável.

| Campo | O/F | Validação |
| --- | --- | --- |
| `name` | O | ausente → `SCOS_VALIDATION_003`; vazio → `SCOS_VALIDATION_001`; ≤ 250 chars |
| `nameTreatment` | O | ausente → `SCOS_VALIDATION_003`; vazio → `SCOS_VALIDATION_001`; ≤ 100 chars |
| `taxIdentifier` | O | ausente → `SCOS_VALIDATION_003`; 14 chars alfanuméricos; DV válido (CNPJ Alfa) |
| `foundationDate` | O | ausente → `SCOS_VALIDATION_003`; data válida, não futura |
| `sectorOfActivity` | O | ausente → `SCOS_VALIDATION_003`; pertence à lista fixa |
| `observation` | F | — |
| `legalNatureId` | F | FK para `SCOS_LEGAL_NATURE` |
| `cnaePrincipalId` | F | FK para `SCOS_CNAE` |
| `stateRegistration` | F | texto livre ou `"ISENTO"` |
| `municipalRegistration` | F | — |

**Regras em ordem:**
1–3. Mesmas do `POST` (exceto `reasonActivateId`).
4. `taxIdentifier` único excluindo o próprio `{id}` — colisão retorna `SCOS_COMPANY_002`.
5. Se `legalNatureId`/`cnaePrincipalId` informados: devem existir.

**Use Cases de Sucesso:**
- UC-S1: Dados válidos → `204`

**Use Cases de Erro:**
- UC-E1: Campo obrigatório ausente/vazio → `400` `SCOS_VALIDATION_003`/`SCOS_VALIDATION_001`
- UC-E2: `taxIdentifier` duplicado → `409` `SCOS_COMPANY_002`
- UC-E3: `{id}` não encontrado → `404`

---

### PUT /v1/companies/{id}/enable
**UC-006** | `ENABLE_COMPANY`

Transição `INACTIVE → ACTIVE`.

| Campo | O/F | Validação |
| --- | --- | --- |
| `reasonId` | O | FK para `SCOS_REASON_ACTIVATE` |
| `observation` | F | — |

**Regras em ordem:**
1. `reasonId` presente.
2. —
3. —
4. —
5. `{id}` deve existir. `reasonId` deve existir em `SCOS_REASON_ACTIVATE`.
6. Empresa deve estar `INACTIVE`. `reasonId` deve ter `ACTIVE=true` e `entityType=COMPANY`.

**Side Effects:** Insere linha em `SCOS_COMPANY_STATUS_HISTORY`.

**Use Cases de Sucesso:**
- UC-S1: Empresa `INACTIVE` + motivo válido → `204`

**Use Cases de Erro:**
- UC-E1: `reasonId` ausente → `400` `SCOS_VALIDATION_003`
- UC-E2: `{id}` não encontrado → `404`
- UC-E3: Empresa não está `INACTIVE` (é `ACTIVE` ou `DISABLED`) → `422` "transição inválida"
- UC-E4: `reasonId` inativo ou incompatível → `422`

---

### PUT /v1/companies/{id}/disable
**UC-007** | `DISABLE_COMPANY`

Transição `ACTIVE → INACTIVE`.

| Campo | O/F | Validação |
| --- | --- | --- |
| `reasonId` | O | FK para `SCOS_REASON_INACTIVATE` |
| `observation` | F | — |

**Regras em ordem:**
5. `{id}` deve existir. `reasonId` deve existir em `SCOS_REASON_INACTIVATE`.
6. Empresa deve estar `ACTIVE`. `reasonId` com `ACTIVE=true` e `entityType=COMPANY`.
7. Não pode existir filial direta (1º nível) com status `ACTIVE`.
8. ⚠️ **Regra alvo, ainda não implementada** (Documento Central, Seção 3.1): se `{id}` for uma matriz (`parentCompanyId IS NULL`), não pode ser a última matriz com status `ACTIVE` do sistema — `SCOS_COMPANY_005`.

**Side Effects:** Insere linha em `SCOS_COMPANY_STATUS_HISTORY`.

**Use Cases de Sucesso:**
- UC-S1: Empresa `ACTIVE` sem filiais ativas + motivo válido → `204`

**Use Cases de Erro:**
- UC-E1: `reasonId` ausente → `400`
- UC-E2: `{id}` não encontrado → `404`
- UC-E3: Empresa não está `ACTIVE` → `422` `SCOS_COMPANY_007`
- UC-E4: Existem filiais diretas `ACTIVE` → `422` "existem filiais ativas vinculadas"
- UC-E5: `reasonId` inativo/incompatível → `422`
- UC-E6 ⚠️ (regra alvo): última matriz ativa → `422` `SCOS_COMPANY_005`

---

### PUT /v1/companies/{id}/block
**UC-008** | `BLOCK_COMPANY`

Transição qualquer status → `DISABLED`.

| Campo | O/F | Validação |
| --- | --- | --- |
| `reasonId` | O | FK para `SCOS_REASON_DISABLE` |
| `observation` | F | — |

**Regras em ordem:**
5. `{id}` deve existir. `reasonId` deve existir em `SCOS_REASON_DISABLE`.
6. `reasonId` com `ACTIVE=true` e `entityType=COMPANY`.
7. ⚠️ **Regra alvo, ainda não implementada** (Documento Central, Seção 3.1): `{id}` não pode ser a única empresa `ACTIVE` de todo o sistema, de qualquer nível hierárquico — `SCOS_COMPANY_006`.

**Side Effects:** Insere linha em `SCOS_COMPANY_STATUS_HISTORY`.

**Use Cases de Sucesso:**
- UC-S1: Empresa em qualquer status + motivo válido → `204`

**Use Cases de Erro:**
- UC-E1: `reasonId` ausente → `400`
- UC-E2: `{id}` não encontrado → `404`
- UC-E3: `reasonId` inativo/incompatível → `422`
- UC-E4 ⚠️ (regra alvo): única empresa ativa do sistema → `422` `SCOS_COMPANY_006`

---

### PUT /v1/companies/{id}/unblock
**UC-137** | `UNBLOCK_COMPANY`

Transição `DISABLED → ACTIVE`.

| Campo | O/F | Validação |
| --- | --- | --- |
| `reasonId` | O | FK para `SCOS_REASON_ENABLE` |
| `observation` | F | — |

**Regras em ordem:**
5. `{id}` deve existir. `reasonId` deve existir em `SCOS_REASON_ENABLE`.
6. Empresa deve estar `DISABLED`. `reasonId` com `ACTIVE=true` e `entityType=COMPANY`.

**Side Effects:** Insere linha em `SCOS_COMPANY_STATUS_HISTORY`.

**Use Cases de Sucesso:**
- UC-S1: Empresa `DISABLED` + motivo válido → `204`

**Use Cases de Erro:**
- UC-E1: `{id}` não encontrado → `404`
- UC-E2: Empresa não está `DISABLED` → `422` "transição inválida"
- UC-E3: `reasonId` inativo/incompatível → `422`

---

### GET /v1/companies/{id}/status-history
**UC-138** | `GET_COMPANY_STATUS_HISTORY`

Histórico paginado de transições de status. Somente leitura.

**Use Cases de Sucesso:**
- UC-S1: `{id}` existente → `200` com lista paginada do mais recente ao mais antigo

**Use Cases de Erro:**
- UC-E1: `{id}` não encontrado → `404`

---

### GET /v1/companies/{id}/hierarchy
**UC-009** | `GET_COMPANY` — árvore recursiva completa.

### GET /v1/companies/{id}/branches
**UC-010** | `GET_COMPANY` — filiais diretas (1º nível, sem recursividade).

Ambos retornam `404` se `{id}` não existir.

---

### GET /v1/companies/{companyId}/contacts
**UC-011** | `GET_COMPANY_CONTACT` — lista contatos. `404` se `companyId` não existir.

---

### POST /v1/companies/{companyId}/contacts
**UC-012** | `CREATE_COMPANY_CONTACT`

| Campo | O/F | Validação |
| --- | --- | --- |
| `contactTypeId` | O | FK para `SCOS_CONTACT_TYPE` |
| `phone` | O | formato telefônico válido; ≤ 50 chars |
| `email` | O | formato e-mail válido (`SCOS_VALIDATION_005`); ≤ 255 chars |
| `responsiblePerson` | O | ≤ 255 chars |

**Regras em ordem:**
1. Todos presentes e não vazios.
2. Formato de `phone` e `email`.
3. Limites de tamanho.
4. `phone` único para `companyId` (`UNIQUE(COMPANY_ID, PHONE)`). `email` único para `companyId`. `contactTypeId` único para `companyId` (`UNIQUE(COMPANY_ID, CONTACT_TYPE_ID)`).
5. `companyId` deve existir. `contactTypeId` deve existir em `SCOS_CONTACT_TYPE`.
6. `contactTypeId` deve ter `ACTIVE=true` e `entityType=COMPANY`.

**Use Cases de Sucesso:**
- UC-S1: Campos válidos + tipo ativo → `201`

**Use Cases de Erro:**
- UC-E1: Campo obrigatório ausente → `400` `SCOS_VALIDATION_003`
- UC-E2: `phone` ou `email` com formato inválido → `400` `SCOS_VALIDATION_005` (e-mail)
- UC-E3: `phone` ou `email` ou `contactTypeId` já existe para esta empresa → `409`
- UC-E4: `companyId` não encontrado → `404`
- UC-E5: `contactTypeId` inexistente → `404`
- UC-E6: `contactTypeId` inativo ou `entityType=EMPLOYEE` → `422`

---

### GET /v1/companies/{companyId}/contacts/{id}
**UC-013** | `GET_COMPANY_CONTACT` — `404` se `companyId` ou `{id}` não existir.

### PUT /v1/companies/{companyId}/contacts/{id}
**UC-014** | `UPDATE_COMPANY_CONTACT` — mesmas regras do `POST`, excluindo o próprio `{id}` na unicidade. `204` ou erros equivalentes.

### DELETE /v1/companies/{companyId}/contacts/{id}
**UC-015** | `DELETE_COMPANY_CONTACT` — remove contato. `204`. `404` se não existir.

---

### GET /v1/companies/{companyId}/addresses
**UC-016** | `GET_COMPANY_ADDRESS` — lista endereços. `404` se `companyId` não existir.

---

### POST /v1/companies/{companyId}/addresses
**UC-017** | `CREATE_COMPANY_ADDRESS`

| Campo | O/F | Validação |
| --- | --- | --- |
| `addressTypeId` | O | FK para `SCOS_ADDRESS_TYPE` |
| `number` | O | — |
| `complement` | F | — |
| `latitude` | O | number; entre -90 e 90 |
| `longitude` | O | number; entre -180 e 180 |
| `addressId` | O | ID do serviço externo de endereços |

**Regras em ordem:**
1. Campos obrigatórios presentes.
2. `latitude` e `longitude` nos intervalos válidos.
3. —
4. `addressTypeId` único para `companyId` (`UNIQUE(COMPANY_ID, ADDRESS_TYPE_ID)`).
5. `companyId` deve existir. `addressTypeId` deve existir em `SCOS_ADDRESS_TYPE`.
6. `addressTypeId` com `ACTIVE=true` e `entityType=COMPANY`.
7. Ao persistir: `GEOLOCATION = POINT(longitude, latitude)`.

**Use Cases de Sucesso:**
- UC-S1: Campos válidos → `201`

**Use Cases de Erro:**
- UC-E1: Campo obrigatório ausente → `400` `SCOS_VALIDATION_003`
- UC-E2: `latitude`/`longitude` fora do intervalo → `400`
- UC-E3: `addressTypeId` já existe para esta empresa → `409`
- UC-E4: `companyId` não encontrado → `404`
- UC-E5: `addressTypeId` inexistente → `404`
- UC-E6: `addressTypeId` inativo ou `entityType=EMPLOYEE` → `422`

### GET /v1/companies/{companyId}/addresses/{id}
**UC-018** | `GET_COMPANY_ADDRESS` — `404` se não existir.

### PUT /v1/companies/{companyId}/addresses/{id}
**UC-019** | `UPDATE_COMPANY_ADDRESS` — `addressId` não editável. Mesmas regras do `POST`, excluindo o próprio `{id}`. `204`.

### DELETE /v1/companies/{companyId}/addresses/{id}
**UC-020** | `DELETE_COMPANY_ADDRESS` — remove vínculo. Registro externo não é afetado. `204`.

---

## 1.1 Natureza Jurídica (Legal Nature)

### GET /v1/legal-natures
**UC-093** | `GET_LEGAL_NATURE` — lista paginada.

### POST /v1/legal-natures
**UC-094** | `CREATE_LEGAL_NATURE`

| Campo | O/F | Validação |
| --- | --- | --- |
| `code` | O | único em `SCOS_LEGAL_NATURE`; ≤ 30 chars |
| `description` | O | ≤ 255 chars |

**Use Cases de Sucesso:** UC-S1: campos válidos → `201`
**Use Cases de Erro:** UC-E1: ausente/vazio → `400` | UC-E2: `code` duplicado → `409`

### GET /v1/legal-natures/{id}
**UC-095** | `GET_LEGAL_NATURE` — `404` se não existir.

### PUT /v1/legal-natures/{id}
**UC-096** | `UPDATE_LEGAL_NATURE` — mesmas validações, `code` único excluindo `{id}`. `204`.

### DELETE /v1/legal-natures/{id}
**UC-097** | `DELETE_LEGAL_NATURE` — **exclusão física** (não lógica).

**Regras em ordem:**
5. `{id}` deve existir.
7. Não pode existir `SCOS_COMPANY` com `legalNatureId = {id}`.

**Use Cases de Sucesso:** UC-S1: sem vínculos → `204`
**Use Cases de Erro:** UC-E1: `{id}` não encontrado → `404` | UC-E2: empresa vinculada → `422` "natureza jurídica em uso"

---

## 1.2 CNAE

### GET /v1/cnaes
**UC-098** | `GET_CNAE` — lista paginada.

### POST /v1/cnaes
**UC-099** | `CREATE_CNAE`

| Campo | O/F | Validação |
| --- | --- | --- |
| `code` | O | único em `SCOS_CNAE`; ≤ 30 chars |
| `description` | O | ≤ 255 chars |

**Use Cases de Sucesso:** UC-S1: `201`
**Use Cases de Erro:** UC-E1: ausente → `400` | UC-E2: `code` duplicado → `409`

### GET /v1/cnaes/{id}
**UC-100** | `GET_CNAE` — `404` se não existir.

### PUT /v1/cnaes/{id}
**UC-101** | `UPDATE_CNAE` — `code` único excluindo `{id}`. `204`.

### DELETE /v1/cnaes/{id}
**UC-102** | `DELETE_CNAE` — **exclusão física**.

**Regra:** `{id}` não pode estar referenciado em `SCOS_COMPANY.cnaePrincipalId` nem em `SCOS_COMPANY_CNAE_SECONDARY`.

**Use Cases de Erro:** UC-E1: `{id}` não encontrado → `404` | UC-E2: em uso → `422`

---

## 1.3 CNAEs Secundários da Empresa

### GET /v1/companies/{companyId}/cnaes-secondary
**UC-103** | `GET_COMPANY_CNAE_SECONDARY` — lista paginada. `404` se `companyId` não existir.

### POST /v1/companies/{companyId}/cnaes-secondary/{cnaeId}
**UC-104** | `CREATE_COMPANY_CNAE_SECONDARY` — sem request body.

**Regras em ordem:**
5. `companyId` deve existir. `cnaeId` deve existir em `SCOS_CNAE`.
7. `cnaeId` não pode estar já associado como CNAE secundário desta empresa (idempotente — ver UC-E2).

**Use Cases de Sucesso:** UC-S1: associação criada → `201`
**Use Cases de Erro:** UC-E1: `companyId` ou `cnaeId` não encontrado → `404` | UC-E2: associação já existe → `409`

### DELETE /v1/companies/{companyId}/cnaes-secondary/{cnaeId}
**UC-105** | `DELETE_COMPANY_CNAE_SECONDARY` — remove associação. `204`. `404` se não existir.
