# Regras de Negócio — Company API

Fonte: `etc/api/organization/ScosOrganization_Company.yml`

---

## 🎯 Visão Estratégica
Este documento define a visão, regras de negócio, contrato de API e guia de implementação para o **domínio Company** — a entidade raiz da hierarquia organizacional. Company é o contexto delimitado (Bounded Context) que encapsula a estrutura empresarial para uma única organização (empresa ou grupo), relacionamentos com filiais e sincronização de identidades.

**Escopo**: `Company`, `CompanyContact` e `CompanyAddress` com suas operações CRUD, validações de domínio, eventos de domínio e integrações (Keycloak, Kafka).

**Decisões arquiteturais críticas**:
- **Unicidade**: CNPJ é único no escopo do sistema (single‑tenant)
- **Hierarquia Controlada**: Prevenção de ciclos até 5 níveis de profundidade
- **Soft-Delete como Padrão**: Quando há dependências ativas, marcar `status = DELETED`
- **Eventos de Domínio**: Mudanças críticas geram eventos Kafka para sincronização com IdP/consumidores
- **Teste em Camadas**: Testes unitários (domínio), integração (application), API (controller)

---

## Quick Reference
- **Endpoints principais**: `POST /v1/companies` (201), `PUT /v1/companies/{id}` (204), `GET /v1/companies/{id}` (200), `DELETE /v1/companies/{id}` (204), `PUT /v1/companies/{id}/status` (204)
- **Regras críticas**: 
  - CNPJ único no sistema → erro `SCOS_COMPANY_002` (409)
  - `foundationDate` ≤ hoje (sem data futura)
  - `parentCompanyId` sem ciclos (máx. 5 níveis recomendado)
  - Soft-delete quando há dependências ativas
- **Artifacts chave**: 
  - Contrato: `etc/api/organization/ScosOrganization_Company.yml`
  - Modelo: `scos-organization-domain/.../company/Company.java`
  - UseCases: `scos-organization-application/.../usecase/company/`
  - Migrations/triggers: `scos-organization-boot/src/main/resources/db/changelog/v1/0/0/`

---

## 1. Objetivo
Este documento serve como **especificação técnica** e **guia de implementação** para o domínio Company. Define o contrato de API (OpenAPI), regras de negócio, validações em cada camada (entrada, domínio, persistência) e diretrizes para testes automatizados.

## 2. Entidades e Conceitos

### 2.1 Company (Entidade Raiz)
Representa uma unidade organizacional (empresa, filial, divisão) no sistema single-tenant (aplicação para uma única empresa ou grupo).

| Campo | Tipo | Constraints | Descrição |
|-------|------|-------------|----------|
| `id` | Long (PK) | AUTO_INCREMENT | Identificador único dentro do BD |
| `name` | String | NOT NULL, 2-250 chars | Nome oficial da empresa |
| `nameTreatment` | String | NOT NULL, max 100 | Abreviação/tratamento usado em docs |
| `taxIdentifier` | String (CNPJ) | NOT NULL, UNIQUE | CNPJ formatado (XX.XXX.XXX/XXXX-XX) |
| `foundationDate` | Date | NOT NULL, ≤ NOW() | Data de fundação ou constituição |
| `sectorOfActivity` | Enum/String | NOT NULL | Classificação CNAE ou custom |
| `parentCompanyId` | Long (FK) | NULLABLE, NOT IN CYCLE | Referência a empresa-mãe (se filial) |
| `status` | Enum | NOT NULL, default='ACTIVE' | `ACTIVE`, `INACTIVE`, `DELETED` |
| `createdAt` | Timestamp | NOT NULL, AUTO | Data/hora de criação |
| `updatedAt` | Timestamp | NOT NULL, AUTO | Data/hora de última alteração |
| `createdBy` | String | NOT NULL | usuário que criou |
| `updatedBy` | String | NULL | usuário que atualizou |

**Relacionamentos**:
- **1:N com Department**: Uma company tem múltiplos departamentos
- **1:N com Employee**: Uma company tem múltiplos colaboradores (diretos)
- **1:N com Position**: Uma company tem múltiplas posições
- **1:N com CompanyContact**: Uma company tem múltiplos contatos
- **1:N com CompanyAddress**: Uma company tem múltiplos endereços
- **N:1 com Company (self-reference)**: Via `parentCompanyId` para hierarquia de filiais

### 2.2 CompanyContact (Value Object / Entidade Subordinada)
Representa contatos (email, phone, responsável) associados a uma company.

| Campo | Tipo | Constraints | Descrição |
|-------|------|-------------|----------|
| `id` | Long (PK) | AUTO_INCREMENT | |
| `companyId` | Long (FK) | NOT NULL | Referência a `SCOS_COMPANY` |
| `contactType` | Enum | NOT NULL | `EMAIL`, `PHONE`, `PERSON` |
| `value` | String | NOT NULL | Email, número de telefone ou nome |
| `responsiblePerson` | String | NULLABLE | Pessoa responsável pelo contato |
| `createdAt` | Timestamp | NOT NULL | |
| `updatedAt` | Timestamp | NOT NULL | |

### 2.3 CompanyAddress (Value Object / Entidade Subordinada)
Representa endereços físicos com informações geográficas.

| Campo | Tipo | Constraints | Descrição |
|-------|------|-------------|----------|
| `id` | Long (PK) | AUTO_INCREMENT | |
| `companyId` | Long (FK) | NOT NULL | Referência a `SCOS_COMPANY` |
| `street` | String | NOT NULL | Logradouro |
| `number` | String | NOT NULL | Número do imóvel |
| `complement` | String | NULLABLE | Complemento (apto, sala, etc.) |
| `neighborhood` | String | NOT NULL | Bairro |
| `city` | String | NOT NULL | Município |
| `state` | String (2) | NOT NULL | UF (ex: SP, RJ) |
| `country` | String | NOT NULL, default='BR' | Código de país (ISO 3166-1) |
| `zipCode` | String | NOT NULL | CEP ou código postal |
| `latitude` | Decimal | NULLABLE, -90..90 | Coordenada geográfica |
| `longitude` | Decimal | NULLABLE, -180..180 | Coordenada geográfica |
| `createdAt` | Timestamp | NOT NULL | |
| `updatedAt` | Timestamp | NOT NULL | |

## 3. Contrato OpenAPI — Pontos Essenciais
- **Spec**: `etc/api/organization/ScosOrganization_Company.yml` (schemas, `x-*`, exemplos)
- **Extensões relevantes**: 
  - `x-is-cnpj`: marca campos que contêm CNPJ
  - `x-jdempotentrequestpayload`: para operações idempotentes (criar com ID único de request)
  - `x-jdempotentresource`: marca endpoint como suportando idempotência
  - `x-authorize`: controle de acesso baseado em permissões
  - `x-required-message`: mensagem customizada para campos obrigatórios

---

## 4. Regras de Negócio Base
- Escopo: `Company` é um recurso no escopo do sistema (single‑tenant); regras de unicidade são globais.
- Unicidade:
  - `taxIdentifier` (CNPJ) — único no sistema → erro de domínio `SCOS_COMPANY_002`.
  - `name` — pode ter restrições por empresa/grupo (opcional).
- Validações principais:
  - `foundationDate` ≤ hoje
  - `parentCompanyId` — deve existir, estar `active` e não criar ciclos
  - `status` — enum: `ACTIVE`, `INACTIVE`, `DELETED`
- Exclusão / inativação:
  - `DELETE` físico permitido apenas se não houver dependências (employees, departments, filiais); caso contrário retornar 409/422 e preferir `status = DELETED` (soft-delete).
  - Matriz vs Filial:
    - **Matriz** (`parentCompanyId = null`): **não é permitido** inativar nem excluir uma empresa matriz se não existir **outra** empresa matriz com `status = ACTIVE`. Nesse caso retornar 409 `SCOS_COMPANY_005`.
    - **Filial** (`parentCompanyId != null`): inativação é permitida; quando `INACTIVE` deve impedir a criação de novos Employees para essa filial.
  - `INACTIVE` deve impedir criação de recursos dependentes e emitir eventos de sincronização (Keycloak, consumidores).
- Auditoria / eventos:
  - Registre auditoria (audit log) para criação/atualização/remoção.
  - **Política:** Nenhuma operação sobre `Company` ou seus sub‑recursos (`CompanyContact`, `CompanyAddress`) publica eventos Kafka nem sincroniza com Keycloak em qualquer momento.
  - **Observação:** Todas as ações relacionadas a `Company` e sub‑recursos são _audit‑only_ (apenas logs de auditoria).
- Segurança / autorização:
  - Operações controladas por `x-authorize` (ex.: `CREATE_COMPANY`, `UPDATE_COMPANY`, `DELETE_COMPANY`).

---

## 5. Validações e Mensagens Detalhadas
- `name`: obrigatório (SCOS-003), min 2, max 250 chars (SCOS-001 / SCOS-004).
- `nameTreatment`: obrigatório (SCOS-003), max 100 chars.
- `taxIdentifier`: obrigatório (SCOS-003), formato CNPJ válido (SCOS-010), unicidade (SCOS_COMPANY_002).
- `foundationDate`: obrigatório (SCOS-003), formato `date`, não pode ser futuro (SCOS-002).
- `sectorOfActivity`: obrigatório (SCOS-003), validar lista de domínios quando aplicável.
- `parentCompanyId`: se informado, deve existir e estar `ACTIVE` (SCOS-012); negar ciclos (domínio `SCOS_COMPANY_004`).
- `status`: enum permitido (`ACTIVE`, `INACTIVE`, `DELETED`) — campo obrigatório nos updates.

Códigos de validação e mapeamento HTTP (resumo):
- SCOS-001 — campo vazio (400)
- SCOS-002 — valor inválido / regra de negócio (400)
- SCOS-003 — campo obrigatório (400)
- SCOS-004 — valor acima do máximo (400)
- SCOS-010 — formato inválido (ex.: CNPJ inválido) (400)
- SCOS-011 — duplicidade/violação de unicidade (409)
- SCOS-012 — recurso não encontrado (404)
- SCOS-013 — conflito por dependência (409)

Códigos específicos de Company:
- SCOS_COMPANY_001 — Company não encontrada (404)
- SCOS_COMPANY_002 — CNPJ já cadastrado (409)
- SCOS_COMPANY_003 — Conflito: Company com dependências ativas (409)
- SCOS_COMPANY_004 — Ciclo detectado em parentCompany (400)

---

## 6. Use Cases — Especificação Detalhada

Abaixo cada operação é descrita como um UseCase completo, com entrada, validações, estados, erros, efeitos colaterais e exemplos. Campos de BD seguem o diagrama em `etc/database/company.puml`.

### Company — UseCases

#### UseCase: CreateCompanyUseCase (POST `/v1/companies`)

**Verbo HTTP**: `POST`
**Endpoint**: `/v1/companies`
**Autorização**: `x-authorize: CREATE_COMPANY`

**Descrição**:
Cria uma nova entidade Company. A operação é idempotente se suportando header `Idempotency-Key` (conforme `x-jdempotentresource`). O CNPJ deve ser único no escopo do sistema; ciclos em `parentCompanyId` são prevenidos na validação de domínio.

**Request Headers**:
```
Content-Type: application/json
Idempotency-Key: [UUID] (opcional, para idempotência)
Authorization: Bearer {token}
```

**Request Body (JSON)**:
```json
{
  "name": "string (2-250 chars, required)",
  "nameTreatment": "string (max 100, required)",
  "taxIdentifier": "string CNPJ (required, format: XX.XXX.XXX/XXXX-XX)",
  "foundationDate": "date (YYYY-MM-DD, required, ≤ today)",
  "sectorOfActivity": "string (required, ex: MANUFACTURING, RETAIL, SERVICES)",
  "parentCompanyId": "number (optional, if provided must exist & be ACTIVE)"
}
```

**Validações de Entrada (Field-Level)**:
- `name`: obrigatório (SCOS-003), min 2, max 250 chars (SCOS-001/SCOS-004)
- `nameTreatment`: obrigatório (SCOS-003), max 100 chars (SCOS-004)
- `taxIdentifier`: obrigatório (SCOS-003), formato CNPJ válido (SCOS-010), sem formatação obrigatória
- `foundationDate`: obrigatório (SCOS-003), formato `YYYY-MM-DD`, não pode ser futuro (SCOS-002)
- `sectorOfActivity`: obrigatório (SCOS-003), validar contra list de valores permitidos
- `parentCompanyId`: opcional (se null, company é raiz); se informado, validar tipo Long

**Validações de Domínio (Business Rules)**:
- ✅ **Unicidade de CNPJ**: verificar se `taxIdentifier` (com formatting removido) já existe no sistema → `SCOS_COMPANY_002` (409 Conflict)
- ✅ **ParentCompany válida**: se `parentCompanyId` informado:
  - Company com esse ID deve existir no BD → SCOS-012 (404 Not Found)
  - Company deve ter `status = ACTIVE` → erro customizado (400/422)
  - Não pode criar ciclo: validar que `parentCompanyId` não é descendente de alguma futura company filho do novo → `SCOS_COMPANY_004` (400 Bad Request)
- ✅ **Foundation date**: validar que é ≤ data de hoje (usando timezone da aplicação) → SCOS-002 (400)
- ✅ **Sector validation**: se houver enum de setores válidos, validar contra lista

**Fluxo de Execução**:
1. **Receber** payload e headers
2. **Validar entrada** (field-level) → retornar 400 + erros se inválido
3. **Validar domínio** (regras de negócio) → retornar 409/400/404 conforme caso
4. **Persistir** em `SCOS_COMPANY` com `STATUS = ACTIVE` (default)
5. **Atualizar audit fields**: `CREATED_AT = NOW()`, `UPDATED_AT = NOW()`, `CREATED_BY = currentUser`
6. **Registrar auditoria**: 
   - Auditoria: `AUDIT_EVENT (entity=COMPANY, action=CREATE, ...)`
   - **Nota**: Criação/edição de Company e de seus sub‑recursos são **audit‑only**; **não** publicar eventos Kafka nem sincronizar com Keycloak.
7. **Retornar** 201 Created + Location header

**Resposta de Sucesso (201 Created)**:
```
Location: /v1/companies/{companyId}
Content-Type: application/json

{
  "data": {
    "id": 100,
    "name": "Acme S.A.",
    "nameTreatment": "ACME",
    "taxIdentifier": "12.345.678/0001-95",
    "foundationDate": "2000-05-20",
    "sectorOfActivity": "MANUFACTURING",
    "parentCompanyId": null,
    "status": "ACTIVE",
    "createdAt": "2026-02-17T10:30:00Z",
    "updatedAt": "2026-02-17T10:30:00Z",
    "createdBy": "user.123@org.com"
  }
}
```

**Respostas de Erro**:

| Código HTTP | Código Erro | Cenário | Resposta |
|-------------|------------|---------|----------|
| 400 | SCOS-003 | Campo obrigatório faltando | `{ "data": { "message": "...", "codeError": "SCOS-003", "attribute": "name" } }` |
| 400 | SCOS-001 | Campo vazio | `{ "data": { "message": "Field cannot be empty", "codeError": "SCOS-001", "attribute": "name" } }` |
| 400 | SCOS-004 | Campo acima do máximo | `{ "data": { "message": "Cannot exceed 250 chars", "codeError": "SCOS-004", "attribute": "name", "max": 250 } }` |
| 400 | SCOS-010 | Formato CNPJ inválido | `{ "data": { "message": "Invalid CNPJ format", "codeError": "SCOS-010", "attribute": "taxIdentifier" } }` |
| 400 | SCOS-002 | Foundation date futuro | `{ "data": { "message": "Cannot be future date", "codeError": "SCOS-002", "attribute": "foundationDate" } }` |
| 404 | SCOS-012 | Parent company não existe | `{ "data": { "message": "Parent company not found", "codeError": "SCOS-012", "attribute": "parentCompanyId" } }` |
| 400 | SCOS_COMPANY_004 | Ciclo detectado em parent | `{ "data": { "message": "Cycle detected in company hierarchy", "codeError": "SCOS_COMPANY_004", "attribute": "parentCompanyId" } }` |
| 409 | SCOS_COMPANY_002 | CNPJ já existe | `{ "data": { "message": "CNPJ already registered", "codeError": "SCOS_COMPANY_002", "attribute": "taxIdentifier" } }` |

**Efeitos Colaterais**:
- ✅ Log de auditoria registrado em `AUDIT_LOG` (create)
- ⚠️ **Observação**: criação/edição de Company **não emitem eventos Kafka nem sincronizam com Keycloak**. Sincronização com Keycloak e eventos Kafka ocorrem apenas em mudanças de status (ex.: `INACTIVE`, `DELETED`) ou remoção, conforme política de integração.

**Pré-condições**:
- Usuário autenticado e autorizado com `CREATE_COMPANY`
- Organização/empresa ativa e válida (extraído de contexto de autenticação)
- Observação: é permitido cadastrar mais de uma `Company` classificada como **matriz** (`parentCompanyId = null`).

**Pós-condições**:
- Company criada com `status = ACTIVE`
- Pode ter `parentCompanyId` vinculado (se validado)
- Ready para receber Departments, Employees, Contacts, Addresses

**Dados Persistidos em Banco**:
```sql
INSERT INTO SCOS_COMPANY 
  (NAME, NAME_TREATMENT, TAX_IDENTIFIER, FOUNDATION_DATE, SECTOR_OF_ACTIVITY, 
   PARENT_COMPANY_ID, STATUS, CREATED_AT, UPDATED_AT, CREATED_BY)
VALUES
  ('Acme S.A.', 'ACME', '12345678000195', '2000-05-20', 'MANUFACTURING', 
   NULL, 'ACTIVE', NOW(), NOW(), 'user.123@org.com');
```

---

#### UseCase: UpdateCompanyUseCase (PUT `/v1/companies/{id}`)

**Verbo HTTP**: `PUT`
**Endpoint**: `/v1/companies/{id}`
**Path Parameter**: `id` (Long) — Company ID a atualizar
**Autorização**: `x-authorize: UPDATE_COMPANY`

**Descrição**:
Atualiza campos de uma Company existente. Validação de ciclos é feita se `parentCompanyId` for alterado. O CNPJ **não pode ser alterado** nessa operação (prevenção de fraude); para mudança de CNPJ, aplicar soft-delete + create novo.

**Request Body (JSON)**:
```json
{
  "name": "string (2-250 chars, required)",
  "nameTreatment": "string (max 100, required)",
  "foundationDate": "date (YYYY-MM-DD, required)",
  "sectorOfActivity": "string (required)",
  "parentCompanyId": "number (optional, null para remover parent)",
  "status": "enum (ACTIVE|INACTIVE|DELETED, optional)"
}
```

**Validações de Entrada**:
- Mesmas do Create para: `name`, `nameTreatment`, `foundationDate`, `sectorOfActivity`
- `status`: se informado, deve ser um dos enum permitidos

**Validações de Domínio**:
- ✅ **Company existe**: Company com `id` deve existir → `SCOS_COMPANY_001` (404)
- ✅ **Parent válida**: se `parentCompanyId` alterado, mesmo check do Create (existência, ACTIVE, sem ciclo)
- ✅ **Status transition**: validar transições de status permitidas:
  - `ACTIVE` → `INACTIVE`: bloqueado se há dependências em estado pendente? (política)
  - `INACTIVE` → `ACTIVE`: permitido
  - `DELETED`: apenas via DELETE endpoint, não via PUT
- ✅ **CNPJ imutável**: se `taxIdentifier` vier no payload, retornar erro (409/422)

**Fluxo de Execução**:
1. **Validar entrada** (field-level)
2. **Buscar** Company por ID → 404 se não existe
3. **Validar domínio**
4. **Atualizar** campos em memória
5. **Persistir** em BD: `UPDATED_AT = NOW()`, `UPDATED_BY = currentUser`
6. **Registrar auditoria** (update)
7. **Retornar** 204 No Content (sem body)

**Nota**: Atualizações de `Company` não devem emitir eventos Kafka nem sincronizar com Keycloak; apenas alterações de status e deleções geram eventos.

**Resposta de Sucesso (204 No Content)**:
```
HTTP/1.1 204 No Content
Location: /v1/companies/{id}
```

**Respostas de Erro**:
- 404 `SCOS_COMPANY_001`: Company não encontrada
- 409 `SCOS_COMPANY_004`: Ciclo detectado em parent
- 404 `SCOS-012`: Parent company não existe
- 422: Se tentar alterar CNPJ

**Efeitos Colaterais**:
- Auditoria registrada (update)
- **Não** emitir evento Kafka `company.updated` nem sincronizar com Keycloak para updates.

---

#### UseCase: GetCompanyByIdUseCase (GET `/v1/companies/{id}`)

**Verbo HTTP**: `GET`
**Endpoint**: `/v1/companies/{id}`
**Path Parameter**: `id` (Long) — Company ID
**Autorização**: `x-authorize: READ_COMPANY` (ou público, conforme política)

**Descrição**:
Retorna detalhes de uma Company específica, incluindo fields de auditoria (`createdAt`, `updatedAt`, `createdBy`).

**Resposta de Sucesso (200 OK)**:
```json
{
  "data": {
    "id": 100,
    "name": "Acme S.A.",
    "nameTreatment": "ACME",
    "taxIdentifier": "12.345.678/0001-95",
    "foundationDate": "2000-05-20",
    "sectorOfActivity": "MANUFACTURING",
    "parentCompanyId": null,
    "status": "ACTIVE",
    "createdAt": "2026-02-17T10:30:00Z",
    "updatedAt": "2026-02-17T10:30:00Z",
    "createdBy": "user.123@org.com",
    "updatedBy": null
  }
}
```

**Resposta de Erro**:
- 404 `SCOS_COMPANY_001`: Company não encontrada

---

#### UseCase: Enable/Disable Company (PUT `/v1/companies/{id}/status`)

**Verbo HTTP**: `PUT`
**Endpoint**: `/v1/companies/{id}/status`
**Autorização**: `x-authorize: MANAGE_COMPANY_STATUS`

**Descrição**:
Altera o status de uma Company de `ACTIVE` para `INACTIVE` ou vice-versa. Mudança para `DELETED` deve ser feita via DELETE endpoint.

**Request Body (JSON)**:
```json
{
  "status": "ACTIVE | INACTIVE (required)"
}
```

**Validações**:
- ✅ Company existe → 404
- ✅ Se transicionar para `INACTIVE`:
  - Se Company é **matriz** (`parentCompanyId = null`): só permitido se existir **outra** Company classificada como matriz com `status = ACTIVE`; caso contrário retornar 409 `SCOS_COMPANY_005`.
  - Se Company é **filial** (`parentCompanyId != null`): inativação permitida; após `INACTIVE` bloquear a criação de novos `Employee` para essa filial.
  - Verificar dependências ativas (Employees, Departments) e aplicar políticas de bloqueio conforme necessário.
- ✅ Status válido (não pode ser `DELETED` aqui)
- ✅ Em `INACTIVE`: bloquear criação de novos Departments/Employees vinculados à Company

**Fluxo**:
1. Validar entrada
2. Buscar Company
3. Validar transição de status (incluindo regra de matriz)
4. Atualizar `STATUS` e `UPDATED_AT`
5. Registrar auditoria da mudança de status
6. Se status → `INACTIVE`, registrar auditoria e aplicar regras de bloqueio (ex.: impedir criação de `Employee` para filiais inativas)

**Efeitos Colaterais**:
- Auditoria registrada para alterações de status
- Se `INACTIVE`: consumidores internos devem bloquear criação de dependências (ex.: `Employee` para filiais inativas)
- Observação: **nenhuma** sincronização com Keycloak e **nenhum** evento Kafka são publicados para `Company` ou seus sub‑recursos.

**Resposta de Sucesso (204 No Content)**:
```
HTTP/1.1 204 No Content
```

**Efeitos Colaterais**:
- `company.status.changed` (Kafka)
- Se `INACTIVE`: consumidores devem bloquear criação de dependências

---

#### UseCase: DeleteCompanyUseCase (DELETE `/v1/companies/{id}`)

**Verbo HTTP**: `DELETE`
**Endpoint**: `/v1/companies/{id}`
**Autorização**: `x-authorize: DELETE_COMPANY`

**Descrição**:
Remove uma Company. Aplica soft-delete (`status = DELETED`) por padrão; hard-delete é opcional por política de retenção.

**Pré-condições (Validação de Dependências)**:
- ✅ Company não possui Employees ativos: se existirem, retornar 409 `SCOS_COMPANY_003`
- ✅ Company não possui Departments ativos: idem
- ✅ Company não possui Positions ativas: idem
- ✅ Company não possui child companies (via `parentCompanyId`): idem
- ✅ Se a Company for **matriz** (`parentCompanyId = null`): verificar existência de **outra** matriz com `status = ACTIVE`; se não houver, retornar 409 `SCOS_COMPANY_005`.
  
**Fluxo**:
1. Buscar Company → 404 se não existe
2. Validar dependências (executar queries de check)
3. Se há dependências ativas → 409 com `SCOS_COMPANY_003`
4. Se sem dependências:
   - Soft-delete: `UPDATE SCOS_COMPANY SET STATUS = 'DELETED', UPDATED_AT = NOW()`
   - Ou hard-delete: `DELETE FROM SCOS_COMPANY WHERE ID = :id` (por política)
5. Registrar auditoria da remoção (soft-delete/hard-delete)
6. Retornar 204 No Content

**Nota**: A remoção de `Company` é audit‑only — não publica eventos Kafka nem sincroniza com Keycloak.

**Resposta de Sucesso (204 No Content)**:
```
HTTP/1.1 204 No Content
```

**Resposta de Erro (Conflito por Dependência / Regras de Matriz)**:
```
HTTP/1.1 409 Conflict
Content-Type: application/json

{
  "data": {
    "message": "Cannot delete company with linked employees or child companies",
    "codeError": "SCOS_COMPANY_003",
    "details": {
      "activeEmployees": 5,
      "activeDepartments": 2,
      "childCompanies": 1
    }
  }
}
```

```
HTTP/1.1 409 Conflict
Content-Type: application/json

{
  "data": {
    "message": "Operation not allowed: cannot inactivate/delete the last root company (matriz)",
    "codeError": "SCOS_COMPANY_005",
    "details": { "requiredOtherActiveRootCompany": true }
  }
}
```

**Efeitos Colaterais**:
- `company.deleted` (Kafka) → para atualizar sincronização com Keycloak
- Auditoria registrada

---

#### UseCase: ListCompaniesUseCase (GET `/v1/companies`)

**Verbo HTTP**: `GET`
**Endpoint**: `/v1/companies`
**Query Parameters** (todos opcionais):
```
?page=0&size=20&sort=name:asc
&status=ACTIVE&name=Acme&taxIdentifier=12345678000195&parentCompanyId=5
```

**Autorização**: `x-authorize: LIST_COMPANY` ou público

**Descrição**:
Retorna lista paginada de Companies, com suporte a filtros e ordenação.

**Parâmetros**:
- `page`: número da página (zero-indexed, default=0)
- `size`: itens por página (padrão=20, máx=100)
- `sort`: campo e direção (ex: `name:asc`, `createdAt:desc`)
- **Filtros**:
  - `status`: enum (`ACTIVE`, `INACTIVE`, `DELETED`)
  - `name`: substring search (case-insensitive)
  - `taxIdentifier`: search exato após remover formatting
  - `parentCompanyId`: exact match

**Resposta de Sucesso (200 OK)**:
```json
{
  "data": [
    {
      "id": 100,
      "name": "Acme S.A.",
      "taxIdentifier": "12.345.678/0001-95",
      "status": "ACTIVE",
      "foundationDate": "2000-05-20"
    }
  ],
  "pagination": {
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8,
    "hasNext": true
  }
}
```

---

---

## 7. Sub-recursos — CompanyContact

CompanyContact é um sub-recurso subordinado a Company. Todas as operações exigem que a Company-mãe exista.

**Endpoint base**: `/v1/companies/{companyId}/contacts`

### 7.1 CreateCompanyContactUseCase (POST `/v1/companies/{companyId}/contacts`)

**Descrição**: Cria um novo contato associado a uma Company.

**Request Body**:
```json
{
  "contactType": "EMAIL | PHONE | PERSON (required)",
  "value": "string (required, ex: contact@acme.com ou +55 11 9999-9999)",
  "responsiblePerson": "string (optional)"
}
```

**Validações de Entrada**:
- `contactType`: deve ser um dos enum permitidos
- `value`: obrigatório, validar conforme tipo:
  - `EMAIL`: validar formato de email (RFC 5322)
  - `PHONE`: validar padrão E.164 quando possível
  - `PERSON`: validar string 2-200 chars
- `responsiblePerson`: opcional, max 100 chars

**Validações de Domínio**:
- Company com `{companyId}` deve existir → `SCOS_COMPANY_001` (404)
- Company deve estar `ACTIVE` (opcionalmente) ou permitir `ACTIVE`/`INACTIVE` conforme política
- Duplicidade: checks opcionais (ex: não permitir 2x mesmo email)

**Resposta de Sucesso (201 Created)**:
```json
{
  "data": {
    "id": 200,
    "companyId": 100,
    "contactType": "EMAIL",
    "value": "contact@acme.com",
    "responsiblePerson": "João Silva",
    "createdAt": "2026-02-17T11:00:00Z"
  }
}
```

**Respostas de Erro**:
- 404 `SCOS_COMPANY_001`: Company não existe
- 400 `SCOS-010`: Email/Phone inválido
- 400 `SCOS-003`: Campo obrigatório

**Efeitos Colaterais**:
- Auditoria registrada (company.contact.create) — **sem evento Kafka**
- Auditoria registrada

---

### 7.2 UpdateCompanyContactUseCase (PUT `/v1/companies/{companyId}/contacts/{id}`)

**Request Body**: mesmo do Create

**Validações**:
- Company existe → 404
- Contact existe (`{id}`) → erro customizado (404)
- Validações de entrada (field-level)

**Resposta de Sucesso (204 No Content)**

**Efeitos Colaterais**:
- Auditoria registrada (company.contact.update) — **sem evento Kafka**

---

### 7.3 GetCompanyContactUseCase (GET `/v1/companies/{companyId}/contacts/{id}`)

**Resposta (200 OK)**:
```json
{
  "data": {
    "id": 200,
    "companyId": 100,
    "contactType": "EMAIL",
    "value": "contact@acme.com",
    "responsiblePerson": "João Silva",
    "createdAt": "2026-02-17T11:00:00Z",
    "updatedAt": "2026-02-17T11:00:00Z"
  }
}
```

---

### 7.4 DeleteCompanyContactUseCase (DELETE `/v1/companies/{companyId}/contacts/{id}`)

**Validações**:
- Company existe → 404
- Contact existe → 404

**Resposta de Sucesso (204 No Content)**

**Efeitos Colaterais**:
- Auditoria registrada (company.contact.delete) — **sem evento Kafka**

---

### 7.5 ListCompanyContactsUseCase (GET `/v1/companies/{companyId}/contacts`)

**Query Parameters**:
- `page`, `size`, `sort`: paginação
- `contactType`: filtro por tipo

**Resposta (200 OK)**:
```json
{
  "data": [
    {
      "id": 200,
      "companyId": 100,
      "contactType": "EMAIL",
      "value": "contact@acme.com"
    }
  ],
  "pagination": { "page": 0, "size": 20, "totalElements": 5 }
}
```

---

## 8. Sub-recursos — CompanyAddress

CompanyAddress representa endereços físicos com coordenadas geográficas.

**Endpoint base**: `/v1/companies/{companyId}/addresses`

### 8.1 CreateCompanyAddressUseCase (POST `/v1/companies/{companyId}/addresses`)

**Request Body**:
```json
{
  "street": "string (required)",
  "number": "string (required)",
  "complement": "string (optional)",
  "neighborhood": "string (required)",
  "city": "string (required)",
  "state": "string 2 chars (required, ex: SP, RJ)",
  "country": "string (optional, default: BR, ISO 3166-1)",
  "zipCode": "string (required, CEP ou postal code)",
  "latitude": "number (optional, -90..90)",
  "longitude": "number (optional, -180..180)"
}
```

**Validações de Entrada**:
- `street`, `number`, `neighborhood`, `city`, `state`, `zipCode`: obrigatórios
- `state`: validar se é 2 chars, uppercase
- `country`: validar se é código ISO válido
- `zipCode`: validar formato por país (regex):
  - Brasil: `\d{5}-?\d{3}`
- `latitude`, `longitude`: se informados, validar ranges

**Validações de Domínio**:
- Company existe → `SCOS_COMPANY_001` (404)
- Company deve estar `ACTIVE` (opcionalmente)
- ZIP code format válido para o país

**Resposta de Sucesso (201 Created)**:
```json
{
  "data": {
    "id": 300,
    "companyId": 100,
    "street": "Rua das Flores",
    "number": "123",
    "neighborhood": "Centro",
    "city": "São Paulo",
    "state": "SP",
    "country": "BR",
    "zipCode": "01234-567",
    "latitude": -23.5505,
    "longitude": -46.6333,
    "createdAt": "2026-02-17T11:15:00Z"
  }
}
```

**Respostas de Erro**:
- 404 `SCOS_COMPANY_001`: Company não existe
- 400 `SCOS-010`: ZIP code inválido

**Efeitos Colaterais**:
- Auditoria registrada (company.address.create) — **sem evento Kafka**
- Auditoria

---

### 8.2 UpdateCompanyAddressUseCase (PUT `/v1/companies/{companyId}/addresses/{id}`)

**Request Body**: mesmo do Create

**Resposta de Sucesso (204 No Content)**

**Efeitos Colaterais**:
- Auditoria registrada (company.address.update) — **sem evento Kafka**

---

### 8.3 GetCompanyAddressUseCase (GET `/v1/companies/{companyId}/addresses/{id}`)

**Resposta (200 OK)**: retorna endereço completo com audit fields

---

### 8.4 DeleteCompanyAddressUseCase (DELETE `/v1/companies/{companyId}/addresses/{id}`)

**Resposta de Sucesso (204 No Content)**

**Efeitos Colaterais**:
- Auditoria registrada (company.address.delete) — **sem evento Kafka**

---

### 8.5 ListCompanyAddressesUseCase (GET `/v1/companies/{companyId}/addresses`)

**Query Parameters**:
- `page`, `size`, `sort`: paginação
- `state`: filtro por UF

**Resposta (200 OK)**: lista paginada de endereços

---

---

## 9. Exemplos JSON de Request / Response (por UseCase)

### Company — CreateCompanyUseCase
Request (POST /v1/companies)
```json
{
  "name": "Acme S.A.",
  "nameTreatment": "ACME",
  "taxIdentifier": "12.345.678/0001-95",
  "foundationDate": "2000-05-20",
  "sectorOfActivity": "MANUFACTURING",
  "parentCompanyId": null
}
```
Success (201 Created)
```json
{ "data": { "id": 100 } }
```
Validation error (400)
```json
{
  "data": {
    "message": "Validation failed",
    "codeError": "SCOS-003",
    "validationErrors": [ { "attribute": "taxIdentifier", "message": "CNPJ inválido (SCOS-010)" } ]
  }
}
```
Conflict (409) — CNPJ duplicado
```json
{
  "data": { "message": "Conflict: CNPJ already exists", "codeError": "SCOS_COMPANY_002" }
}
```

### Company — DeleteCompanyUseCase (conflito)
Conflict (409)
```json
{
  "data": {
    "message": "Conflict: company has active employees or child companies",
    "codeError": "SCOS_COMPANY_003",
    "validationErrors": [ { "attribute": "companyId", "message": "Cannot delete company with linked employees (SCOS_COMPANY_003)" } ]
  }
}
```

---

## 10. Error-Code Mapping (Company / Contact / Address)
| Código | Alias (legado) | Significado |
|--------|----------------|------------|
| `SCOS_COMPANY_001` | — | Company não encontrada (GET/UPDATE/DELETE) |
| `SCOS_COMPANY_002` | — | CNPJ duplicado / TaxIdentifier já cadastrado |
| `SCOS_COMPANY_003` | — | Conflito: Company tem dependências ativas (delete/inactivate) |
| `SCOS_COMPANY_004` | — | Ciclo detectado em parentCompany |
| `SCOS_CONTACT_001` | — | CompanyContact não encontrada |
| `SCOS_ADDRESS_001` | — | CompanyAddress não encontrada |

> Observação: utilize os códigos `SCOS_...` como padrão; combine com `SCOS-0xx` para validações genéricas.

---

| Código | Alias (legado) | Significado |
|--------|----------------|------------|
| `SCOS_COMPANY_001` | — | Company não encontrada (GET/UPDATE/DELETE) |
| `SCOS_COMPANY_002` | — | CNPJ duplicado / TaxIdentifier já cadastrado |
| `SCOS_COMPANY_003` | — | Conflito: Company tem dependências ativas (delete/inactivate) |
| `SCOS_COMPANY_004` | — | Ciclo detectado em parentCompany |
| `SCOS_CONTACT_001` | — | CompanyContact não encontrada |
| `SCOS_ADDRESS_001` | — | CompanyAddress não encontrada |
| `SCOS-001` | — | Campo vazio |
| `SCOS-002` | — | Valor inválido / regra de negócio violada |
| `SCOS-003` | — | Campo obrigatório ausente |
| `SCOS-004` | — | Valor acima do máximo permitido |
| `SCOS-010` | — | Formato inválido (CNPJ, email, telefone, CEP, etc.) |
| `SCOS-011` | — | Duplicidade/violação de unicidade |
| `SCOS-012` | — | Recurso referenciado não encontrado |
| `SCOS-013` | — | Conflito por dependência (genérico) |

> **Convenção**: Utilize os códigos `SCOS_<ENTITY>_0XX` para erros específicos do domínio e `SCOS-0XX` para validações genéricas (field-level).

---

## 11. Arquitetura e Relacionamentos

### 11.1 Estrutura Single-Tenant
O sistema opera em modo single‑tenant: todos os dados pertencem a uma única organização (empresa ou grupo). Filiais e unidades legais são modeladas via `parentCompanyId`. Validações de unicidade (ex.: CNPJ) são aplicadas globalmente no sistema.

```
Organização: ACME Group
├── Company ID=100 (Matriz, CNPJ: 12.345.678/0001-95)
├── Company ID=101 (Filial SP, parent=100)
└── Departments, Employees, ...
```

### 11.2 Hierarquia de Companies (via `parentCompanyId`)
Companies podem formar uma estrutura de árvore via `parentCompanyId`, representando filiais, divisões ou grupos.

```
Company (head)
├── Company (filial SP, parent=head)
│   ├── Company (sub-filial A, parent=filial SP)
│   └── Company (sub-filial B, parent=filial SP)
└── Company (filial RJ, parent=head)
```

**Restrições**:
- Máx. 5 níveis de profundidade recomendado
- Ciclos são prevenidos na validação de domínio
- Parent deve estar `ACTIVE` para criar dependências

### 11.3 Agregações com Outros Domínios
```
Company (raiz)
  ├─ 1:N ─> Department
  │           ├─ 1:N ─> Position
  │           └─ 1:N ─> Employee
  ├─ 1:N ─> Employee (diretos)
  ├─ 1:N ─> CompanyContact
  └─ 1:N ─> CompanyAddress
```

### 11.4 Fluxo de Sincronização com IdP (Keycloak)
Ao criar/atualizar/deletar Company, eventos são emitidos para sincronização:

```
CreateCompanyUseCase
    ↓
[Domain]
    ↓
[Persist to BD]
    ↓
[Audit logged — no Kafka event, no Keycloak sync for create]

--
Sincronização com Keycloak (quando aplicável)
- Nenhuma operação de `Company` ou de seus sub‑recursos publica eventos Kafka nem sincroniza com Keycloak.
--
```

---

## 12. Invariantes de Negócio

Invariantes são regras que **nunca podem ser violadas** em qualquer estado válido da Company:

1. **CNPJ é único** — não há duplicatas no sistema
1.a **Múltiplas empresas matriz permitidas** — é possível cadastrar mais de uma `Company` como matriz (`parentCompanyId = null`).
1.b **Proteção de matriz** — não é permitido inativar ou deletar a matriz se não existir outra matriz com `status = ACTIVE`.
2. **`parentCompanyId` sem ciclos** — Company A não pode ser avó de si mesma
3. **Profundidade máxima 5** — hierarquia não pode exceder 5 níveis
4. **Parent é ACTIVE** — se tem parent, o parent deve estar `ACTIVE`
5. **Status é um dos enums** — values permitidos: `ACTIVE`, `INACTIVE`, `DELETED`
6. **`foundationDate` ≤ hoje** — não há datas futuras
7. **Audit fields imutáveis após criação** — `createdAt`, `createdBy` nunca são alterados
8. **Soft-delete por padrão** — quando há dependências, marcar `DELETED` em vez de hard-delete

---

## 13. Árvore de Erros e Recuperação

### 13.1 Fluxo de Decisão — CreateCompanyUseCase

```
POST /v1/companies
    │
    ├─ Validação de Entrada
    │   ├─ name vazio? ──→ 400 SCOS-001
    │   ├─ name > 250? ──→ 400 SCOS-004
    │   ├─ taxIdentifier inválido? ──→ 400 SCOS-010
    │   └─ foundationDate futuro? ──→ 400 SCOS-002
    │
    ├─ Validação de Domínio
    │   ├─ CNPJ duplicado? ──→ 409 SCOS_COMPANY_002
    │   ├─ parentCompanyId não existe? ──→ 404 SCOS-012
    │   ├─ parent não está ACTIVE? ──→ 400/422
    │   └─ ciclo detectado? ──→ 400 SCOS_COMPANY_004
    │
    └─ Sucesso: 201 Created + Kafka event
```

### 13.2 Estratégia de Recuperação

| Erro | Status | Recuperação |
|------|--------|------------|
| SCOS-001/SCOS-003 | 400 | Validar entrada novamente, re-enviar payload completo |
| SCOS-010 | 400 | Verificar formatação (remover special chars, validar CNPJ) |
| SCOS_COMPANY_002 | 409 | Atualizar CNPJ ou usar soft-delete da company antiga + criar nova |
| SCOS-012 | 404 | Verificar se parentCompanyId existe; criar parent se necessário |
| SCOS_COMPANY_004 | 400 | Revisar hierarquia; remover parent ou alterar relacionamento |
| SCOS_COMPANY_003 | 409 | Inativar/deletar dependências primeiro, depois company |

---

## 14. Implementação Recomendada

### Fase 1: Camada de Domínio (1-2 dias)
1. **Criar `Company` entity** com fields e invariantes
   - File: `scos-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/model/company/Company.java`
   - Usar records ou classes imutáveis (Java 17+)
   - Adicionar `CompanyId` value object

2. **Criar domínio service** para validações
   - File: `scos-organization-domain/.../company/CompanyDomainService.java`
   - Métodos: `validateUniqueTaxIdentifier()`, `validateParentCompanyHierarchy()`, `checkCycle()`

3. **Criar `CompanyRepository` interface**
   - File: `scos-organization-domain/.../repository/company/CompanyRepository.java`

4. **Testes unitários**
   - File: `scos-organization-domain/src/test/.../company/CompanyTest.java`
   - Testes de invariantes, validações de domínio

### Fase 2: Camada de Aplicação (1-2 dias)
1. **Criar UseCases (Command Handlers)**
   - `CreateCompanyUseCase` → validar entrada + chamar domain service + persistir
   - `UpdateCompanyUseCase`
   - `GetCompanyByIdUseCase`
   - `DeleteCompanyUseCase` → checar dependências, emitir evento
   - `ListCompaniesUseCase` → com paginação e filtros

2. **Criar DTOs**
   - `CreateCompanyRequest`, `UpdateCompanyRequest`, `CompanyResponse`, etc.

3. **Criar Event Producers**
   - **Não** implementar Event Producers para `Company` nem para seus sub‑recursos (`CompanyContact`, `CompanyAddress`) — todas as operações são _audit‑only_.
   - Event Producers continuam válidos para outros domínios quando o negócio exigir.

4. **Testes de integração**
   - File: `.../integration/CreateCompanyUseCaseIntegrationTest.java`
   - Mock BD, verificar persist + eventos

### Fase 3: Camada de Infrastructure (1 dia)
1. **Implementar `CompanyRepository`** (JPA/Hibernate)
   - Queries para unicidade, checks de dependências
   - Named queries para filtros em List

2. **Implementar Event Producer** (Kafka)
   - Producer para tópicos `organization.company.v1`

3. **Migrations SQL**
   - Script criar tabela `SCOS_COMPANY`
   - Triggers de auditoria se aplicável

### Fase 4: Camada de Presentation (1 dia)
1. **Criar controllers REST**
   - File: `scos-organization-api/src/.../company/CompanyController.java`
   - Mapear endpoints OpenAPI

2. **Implementar ExceptionHandler**
   - Mapear domínio exceptions → HTTP status + error response

3. **Testes E2E/API**
   - Usar MockMvc ou TestClient

### Fase 5: Sub-recursos (1-2 dias)
1. Repetir Fases 1-4 para `CompanyContact` e `CompanyAddress`
2. Validar que sub-recursos são subordinados (FK obrigatório)

---

## 15. Exemplos de Fluxo (Cenários)
Cenário: criar empresa, adicionar endereço/contato e inativar

1. POST `/v1/companies` → 201 (companyId)
2. POST `/v1/companies/{companyId}/addresses` → 201
3. POST `/v1/companies/{companyId}/contacts` → 201
4. POST `/v1/departments` (associado à company) → 201
5. PUT `/v1/companies/{companyId}/status` payload `{ "status": "INACTIVE" }` → 204
   - Após `INACTIVE`, criação de novo `Employee` ou `Department` deve ser bloqueada
6. DELETE `/v1/companies/{companyId}` → 409 (SCOS_COMPANY_003) se houver dependências

Cenário: criar empresa, adicionar endereço/contato e inativar

1. POST `/v1/companies` → 201 (companyId)
2. POST `/v1/companies/{companyId}/addresses` → 201
3. POST `/v1/companies/{companyId}/contacts` → 201
4. POST `/v1/departments` (associado à company) → 201
5. PUT `/v1/companies/{companyId}/status` payload `{ "status": "INACTIVE" }` → 204
   - Após `INACTIVE`, criação de novo `Employee` ou `Department` deve ser bloqueada
6. DELETE `/v1/companies/{companyId}` → 409 (SCOS_COMPANY_003) se houver dependências

### Cenário 2: Criar filial de uma empresa existente

1. GET `/v1/companies/100` → verifica se é ACTIVE
2. POST `/v1/companies` com `parentCompanyId: 100` → 201
3. Validação: parentCompanyId=100 existe ✓, é ACTIVE ✓, não cria ciclo ✓
4. Nova company criada como filial de 100

### Cenário 3: Tentar deletar company com dependências

1. GET `/v1/companies/100` → company existe
2. DELETE `/v1/companies/100` → 409 (SCOS_COMPANY_003)
   - Motivo: tem 3 Employees, 2 Departments, 1 child company
3. Recuperação:
   - Inativar ou deletar dependências primeiro
   - OU usar soft-delete automático (política de BD)

### Cenário 4: Validação de ciclo em parent

1. Company A (id=1, parent=null) existe
2. Company B (id=2, parent=1) existe
3. Tentar: PUT `/v1/companies/1` com `parentCompanyId: 2` → 400 SCOS_COMPANY_004
   - Motivo: criaria ciclo (A → B → A)

---

## 16. Referências Técnicas

### 16.1 Paths e Arquivos Principais
- OpenAPI source: `etc/api/organization/ScosOrganization_Company.yml`
- Modelo de domínio: `scos-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/model/company/`
  - `Company.java` — Entidade raiz com invariantes
  - `CompanyContact.java` — Sub-recurso de contato
  - `CompanyAddress.java` — Sub-recurso de endereço
- Repositórios: `scos-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/repository/company/`
- UseCases/Commands: `scos-organization-application/src/main/java/br/com/sawcunhaos/organization/application/usecase/company/`
  - `CreateCompanyUseCase.java`
  - `UpdateCompanyUseCase.java`
  - `DeleteCompanyUseCase.java`
  - `GetCompanyByIdUseCase.java`
  - `ListCompaniesUseCase.java`
- Controllers REST: `scos-organization-api/src/main/java/br/com/sawcunhaos/organization/api/company/`
- Migrations/Triggers:
  - Table creation: `scos-organization-boot/src/main/resources/db/changelog/v1/0/0/001-create-scos-company-table.yml`
  - Functions: `scos-organization-boot/.../function/check_company_parent_company.sql`
  - Function: `scos-organization-boot/.../function/remove_formatting_tax_identifier.sql`
- Permissões: `scos-organization-infrastructure/src/main/java/br/com/sawcunhaos/organization/infrastructure/enumeration/ScosOrganizationPermission.java`
  - `CREATE_COMPANY`, `UPDATE_COMPANY`, `DELETE_COMPANY`, `READ_COMPANY`, `MANAGE_COMPANY_STATUS`
- Listeners de Evento: `scos-organization-infrastructure/src/main/java/br/com/sawcunhaos/organization/infrastructure/event/listener/`
- Utilitários de validação: `TaxIdentifier` helpers para CNPJ (presentes no projeto)

### 16.2 Integração com Keycloak
- **Criação de Company (matriz ou filial) NÃO sincroniza com Keycloak.** A criação em si não deve gerar ações no IdP.
- Ao **alterar status** (ex.: `INACTIVE`) ou ao **deletar** Company, emitir eventos que sincronizem o grupo/estado correspondente no Keycloak (desabilitar/remover grupo conforme o caso).

### 16.3 Estrutura de Testes Recomendada
```
scos-organization-domain/src/test/java/.../company/
├── CompanyTest.java (testes de invariantes)
├── CompanyDomainServiceTest.java (validações)

scos-organization-application/src/test/java/.../company/
├── CreateCompanyUseCaseIntegrationTest.java
├── UpdateCompanyUseCaseIntegrationTest.java
├── DeleteCompanyUseCaseIntegrationTest.java

scos-organization-api/src/test/java/.../company/
├── CompanyControllerTest.java (E2E com MockMvc)
```

---

## 17. Histórico de Versão
| Versão | Data | Autor | Alteração |
|--------:|:-----:|:-----|:---------|
| 1.3 | 2026-02-17 | GitHub Copilot | **Reestruturação completa**: adicionado Índice detalhado, Visão Estratégica, Entidades com schema de BD, UseCase com 6 seções cada (Request, Validações Entrada, Validações Domínio, Fluxo, Resp. Sucesso, Erros), Sub-recursos (Contact/Address) com 5 operações cada, Arquitetura & Relacionamentos, Invariantes de Negócio, Árvore de Erros, Recomendação de Implementação (5 fases), Cenários expandidos (4 cases), Referências Técnicas detalhadas, Estrutura de Testes. **Documento agora é referencial estratégico com detalhes técnicos completos**. |
| 1.2 | 2026-02-17 | GitHub Copilot | Reorganizado: seção "Tabela de versão" movida para o final; melhoria de estrutura e legibilidade |
| 1.1 | 2026-02-17 | GitHub Copilot | Reestruturação; alinhamento ao padrão Department/Position; inclusão de UseCases e códigos de erro |
| 1.0 | inicial | — | Documento extraído do OpenAPI |

