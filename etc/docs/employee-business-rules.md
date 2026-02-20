# Regras de Negócio — Employee API

Fonte: `etc/api/organization/ScosOrganization_Employee.yml`

---

## 🎯 Visão Estratégica
Este documento define a visão, regras de negócio, contrato de API e guia de implementação para o **domínio Employee** — a entidade central que representa colaboradores, vinculados a Company, Position, com credenciais (Login) e relacionamentos hierárquicos.

**Escopo**: `Employee`, `EmployeeContact` e `EmployeeAddress` com suas operações CRUD, validações de referência, eventos de domínio e sincronização com Login/Keycloak.

**Decisões arquiteturais críticas**:
- **Unicidade**: CPF/email únicos no sistema (single‑tenant)
- **Referências Obrigatórias**: Todo Employee deve ter Company e Position válidas
- **Hierarquia Opcional**: Pode ter supervisor (outro Employee), sem ciclos
- **Login Integrado**: Ao criar Employee ativo, pode-se criar Login correspondente
- **Soft-Delete**: Preferir `status = DELETED` quando há histórico
- **Auditoria Completa**: Todas as operações geram eventos + trilha

---

## Quick Reference
- **Endpoints principais**: `POST /v1/employees` (201), `PUT /v1/employees/{id}` (204), `GET /v1/employees/{id}` (200), `DELETE /v1/employees/{id}` (204), `PUT /v1/employees/{id}/status` (204)
- **Regras críticas**: 
  - CPF/email únicos no sistema → erro `SCOS_EMPLOYEE_002` (409)
  - Company e Position devem existir e estar ativos
  - Supervisor não pode criar ciclos
  - Employee **sempre** sofre `soft-delete` (`status = DELETED`); *hard‑delete não é suportado*. Ao deletar um Employee, todos os `Login` vinculados devem ser marcados como `DELETED` (soft‑delete).
- **Artifacts chave**: 
  - Contrato: `etc/api/organization/ScosOrganization_Employee.yml`
  - Modelo: `scos-organization-domain/.../employee/Employee.java`
  - UseCases: `scos-organization-application/.../usecase/employee/`
  - Sub-recursos: Contact, Address

---

## 1. Objetivo
Este documento serve como **especificação técnica** e **guia de implementação** para o domínio Employee. Define o contrato de API, regras de negócio, validações em cascata (Company, Position, Supervisor), operações CRUD com sub-recursos e integração com Login.

## 2. Entidades e Conceitos

### 2.1 Employee (Entidade Raiz)
Representa um colaborador/funcionário da organização.

| Campo | Tipo | Constraints | Descrição |
|-------|------|-------------|----------|
| `id` | Long (PK) | AUTO_INCREMENT | Identificador único |
| `name` | String | NOT NULL, 2-250 chars | Nome completo |
| `nameTreatment` | String | NOT NULL, max 100 | Apelido/abreviação |
| `taxIdentifier` | String (CPF) | NOT NULL, UNIQUE | CPF formatado (XXX.XXX.XXX-XX) |
| `email` | String | NOT NULL, UNIQUE | Email do colaborador |
| `birthDate` | Date | NOT NULL, < hireDate | Data de nascimento |
| `hireDate` | Date | NOT NULL, ≤ NOW() | Data de contratação |
| `companyId` | Long (FK) | NOT NULL | Referência a Company (ativa) |
| `positionId` | Long (FK) | NOT NULL | Referência a Position (ativa) |
| `supervisorId` | Long (FK) | NULLABLE, NOT IN CYCLE | Referência a Employee superior (sem ciclos) |
| `status` | Enum | NOT NULL, default='ACTIVE' | `ACTIVE`, `INACTIVE`, `DELETED` |
| `inactiveReason` | String | NULLABLE | Motivo da inativação (licença, demissão, etc.) |
| `createdAt` | Timestamp | NOT NULL, AUTO | |
| `updatedAt` | Timestamp | NOT NULL, AUTO | |
| `createdBy` | String | NOT NULL | |
| `updatedBy` | String | NULL | |

**Relacionamentos**:
- **N:1 com Company**: Múltiplos Employees por Company
- **N:1 com Position**: Múltiplos Employees com mesmo Position
- **N:1 com Employee (self-ref)**: Via `supervisorId` para hierarquia
- **1:1 com Login**: Cada Employee pode ter um Login (opcional)
- **1:N com EmployeeContact**: Múltiplos contatos (email, phone)
- **1:N com EmployeeAddress**: Múltiplos endereços

### 2.2 EmployeeContact (Sub-recurso)
Contatos associados (phone, email alternativo, pessoa de emergência).

| Campo | Tipo | Constraints | Descrição |
|-------|------|-------------|----------|
| `id` | Long (PK) | AUTO_INCREMENT | |
| `employeeId` | Long (FK) | NOT NULL | Referência a Employee |
| `contactType` | Enum | NOT NULL | `PHONE`, `EMAIL`, `EMERGENCY_PERSON` |
| `value` | String | NOT NULL | Número, email, nome |
| `label` | String | NULLABLE | Descrição (ex: "celular pessoal") |
| `createdAt` | Timestamp | NOT NULL | |
| `updatedAt` | Timestamp | NOT NULL | |

### 2.3 EmployeeAddress (Sub-recurso)
Endereços do funcionário (residência, segunda residência, etc.).

| Campo | Tipo | Constraints | Descrição |
|-------|------|-------------|----------|
| `id` | Long (PK) | AUTO_INCREMENT | |
| `employeeId` | Long (FK) | NOT NULL | Referência a Employee |
| `street` | String | NOT NULL | Logradouro |
| `number` | String | NOT NULL | Número |
| `complement` | String | NULLABLE | Complemento |
| `neighborhood` | String | NOT NULL | Bairro |
| `city` | String | NOT NULL | Município |
| `state` | String (2) | NOT NULL | UF |
| `country` | String | NOT NULL, default='BR' | Código país |
| `zipCode` | String | NOT NULL | CEP |
| `addressType` | Enum | NOT NULL | `RESIDENTIAL`, `COMMERCIAL`, `OTHER` |
| `isPrimary` | Boolean | NOT NULL, default=false | Endereço principal |
| `createdAt` | Timestamp | NOT NULL | |
| `updatedAt` | Timestamp | NOT NULL | |

---

## 3. Contrato OpenAPI — Pontos Essenciais
- **Spec**: `etc/api/organization/ScosOrganization_Employee.yml`
- Campos multi-idioma: `nameTreatment` pode ter localizações
- Sub-recursos: contatos e endereços como nested em responses

---

## 4. Regras de Negócio Base

- **Escopo**: Employee é um recurso no escopo do sistema (vinculado a Company)
- **Unicidade**:
  - `taxIdentifier` (CPF) — único no sistema → `SCOS_EMPLOYEE_002` (409)
  - `email` — único no sistema → `SCOS_EMPLOYEE_003` (409)
- **Datas**:
  - `birthDate` < `hireDate` → erro customizado (400/422)
  - `hireDate` ≤ hoje → SCOS-002 (400)
- **Referências**:
  - `companyId`: deve existir e estar `ACTIVE` → `SCOS_COMPANY_001` (404)
    - Nota: se a Company for **filial** e estiver `INACTIVE`, a criação de novos `Employee` para essa filial deve ser proibida.
  - `positionId`: deve existir e estar ativo → erro customizado (404)
  - `supervisorId`: se informado, deve existir, estar ACTIVE, e não formar ciclos → `SCOS_EMPLOYEE_004` (400)
- **Exclusão**:
  - Se há Login ativo: soft-delete (`status = DELETED`)
  - Se há subordinados: proibir.
  - Retornar 409 `SCOS_EMPLOYEE_005` se violado
- **Status**:
  - `ACTIVE`: plenamente operacional
  - `INACTIVE`: suspenso (licença, etc.)
  - `DELETED`: soft-delete (histórico preservado)
- **Sincronização**:
  - Ao criar Employee: preparar para possível criação de Login
  - Ao inativar: opcionalmente bloquear Login correspondente
  - Ao deletar: sincronizar com Keycloak (desabilitar login)

---

## 5. Validações e Mensagens Detalhadas

| Campo | Validação | Código |
|-------|-----------|--------|
| `name` | Obrigatório, 2-250 chars | SCOS-003 / SCOS-001 / SCOS-004 |
| `email` | Obrigatório, formato email, único | SCOS-003 / SCOS-010 / SCOS_EMPLOYEE_003 |
| `taxIdentifier` | Obrigatório, CPF válido, único | SCOS-003 / SCOS-010 / SCOS_EMPLOYEE_002 |
| `birthDate` | Obrigatório, < hireDate | SCOS-003 / SCOS-002 |
| `hireDate` | Obrigatório, ≤ hoje | SCOS-003 / SCOS-002 |
| `companyId` | Obrigatório, deve existir | SCOS-003 / SCOS_COMPANY_001 |
| `positionId` | Obrigatório, deve existir | SCOS-003 / SCOS-012 |
| `supervisorId` | Opcional, sem ciclos | SCOS_EMPLOYEE_004 |

**Mapeamento HTTP**:
- 400: SCOS-001, SCOS-002, SCOS-003, SCOS-004, SCOS-010, SCOS_EMPLOYEE_004
- 404: SCOS_COMPANY_001, SCOS-012, SCOS_EMPLOYEE_001
- 409: SCOS_EMPLOYEE_002, SCOS_EMPLOYEE_003, SCOS_EMPLOYEE_005

---

## 6. Use Cases — Especificação Detalhada

### Employee — UseCases

#### UseCase: CreateEmployeeUseCase (POST `/v1/employees`)

**Verbo HTTP**: `POST`
**Endpoint**: `/v1/employees`
**Autorização**: `x-authorize: CREATE_EMPLOYEE`

**Descrição**:
Cria um novo Employee vinculado a Company e Position.

**Request Body (JSON)**:
```json
{
  "name": "string (2-250 chars, required)",
  "nameTreatment": "string (max 100, required)",
  "taxIdentifier": "string CPF (XXX.XXX.XXX-XX, required)",
  "email": "string (email format, required)",
  "birthDate": "date (YYYY-MM-DD, required, < hireDate)",
  "hireDate": "date (YYYY-MM-DD, required, ≤ today)",
  "companyId": "number (required, must exist & ACTIVE)",
  "positionId": "number (required, must exist)",
  "supervisorId": "number (optional, if provided no cycles)"
}
```

**Validações de Entrada**:
- `name`: obrigatório, 2-250 chars
- `email`: obrigatório, formato email
- `taxIdentifier`: obrigatório, formato CPF válido
- `birthDate`, `hireDate`: obrigatórios, datas válidas
- `companyId`, `positionId`: obrigatórios

**Validações de Domínio**:
- ✅ **CPF único**: não existe CPF duplicado no sistema → `SCOS_EMPLOYEE_002` (409)
- ✅ **Email único**: não existe email duplicado no sistema → `SCOS_EMPLOYEE_003` (409)
- ✅ **Company válida**: companyId existe e status=ACTIVE → `SCOS_COMPANY_001` (404)
- ✅ **Position válida**: positionId existe → `SCOS-012` (404)
- ✅ **Datas válidas**: birthDate < hireDate, hireDate ≤ hoje → SCOS-002 (400)
- ✅ **Supervisor válido**: se informado, existe, está ACTIVE, sem ciclos → `SCOS_EMPLOYEE_004` (400)

**Fluxo de Execução**:
1. Validar entrada
2. Validar domínio (CPF, email, company, position, supervisor)
3. Persistir em BD com `status = ACTIVE`
4. Emitir `employee.created` (Kafka)
5. Retornar 201 Created + Location

**Resposta de Sucesso (201 Created)**:
```json
{
  "data": {
    "id": 1000,
    "name": "João Silva",
    "email": "joao@company.com",
    "taxIdentifier": "123.456.789-00",
    "birthDate": "1990-05-15",
    "hireDate": "2020-01-10",
    "companyId": 100,
    "positionId": 21,
    "status": "ACTIVE",
    "createdAt": "2026-02-17T10:30:00Z"
  }
}
```

**Respostas de Erro**:
- 400 `SCOS-003`: Campo obrigatório
- 400 `SCOS-010`: Formato inválido (CPF, email)
- 400 `SCOS-002`: Data futura ou incoerência
- 400 `SCOS_EMPLOYEE_004`: Ciclo em supervisor
- 404 `SCOS_COMPANY_001`: Company não existe
- 409 `SCOS_EMPLOYEE_002`: CPF duplicado
- 409 `SCOS_EMPLOYEE_003`: Email duplicado

**Efeitos Colaterais**:
- ✅ Evento `employee.created` (Kafka)
- ✅ Auditoria registrada
- ✅ Possibilidade de criar Login associado (via relacionamento)

---

#### UseCase: UpdateEmployeeUseCase (PUT `/v1/employees/{id}`)

**Verbo HTTP**: `PUT`
**Endpoint**: `/v1/employees/{id}`
**Autorização**: `x-authorize: UPDATE_EMPLOYEE`

**Request Body**: mesmo do Create (CPF + email não alteráveis)

**Validações**:
- Employee existe → `SCOS_EMPLOYEE_001` (404)
- CPF + email imutáveis (não podem ser alterados)
- Mesmo check de referências (company, position, supervisor)

**Fluxo**:
1. Buscar Employee
2. Validar (não alterar CPF/email)
3. Validar domínio
4. Persistir alterações
5. Se `positionId` ou `companyId` alterado: validar impacto (departamentos, etc.)
6. Emitir `employee.updated`
7. Retornar 204 No Content

---

#### UseCase: GetEmployeeByIdUseCase (GET `/v1/employees/{id}`)

**Verbo HTTP**: `GET`
**Endpoint**: `/v1/employees/{id}`
**Autorização**: `x-authorize: READ_EMPLOYEE`

**Resposta de Sucesso (200 OK)**:
```json
{
  "data": {
    "id": 1000,
    "name": "João Silva",
    "email": "joao@company.com",
    "taxIdentifier": "123.456.789-00",
    "birthDate": "1990-05-15",
    "hireDate": "2020-01-10",
    "companyId": 100,
    "positionId": 21,
    "supervisorId": null,
    "status": "ACTIVE",
    "createdAt": "2026-02-17T10:30:00Z",
    "updatedAt": "2026-02-17T10:30:00Z",
    "createdBy": "admin@org.com",
    "contacts": [
      { "id": 2000, "type": "PHONE", "value": "+55 11 99999-9999" }
    ],
    "addresses": [
      { "id": 3000, "street": "Rua A", "number": "123", "city": "São Paulo", "isPrimary": true }
    ]
  }
}
```

---

#### UseCase: Enable/Disable Employee (PUT `/v1/employees/{id}/status`)

**Endpoint**: `PUT /v1/employees/{id}/status`
**Autorização**: `x-authorize: MANAGE_EMPLOYEE_STATUS`

**Request Body**:
```json
{ "status": "ACTIVE | INACTIVE", "reason": "string (optional)" }
```

**Fluxo**:
1. Buscar Employee
2. Validar transição permitida
3. Se inativar (INACTIVE):
   - Atualizar `status`, `inactiveReason`
   - Opcionalmente bloquear Login correspondente
4. Se ativar: rever bloqueios
5. Emitir `employee.status.changed`
6. Retornar 204 No Content

---

#### UseCase: DeleteEmployeeUseCase (DELETE `/v1/employees/{id}`)

**Endpoint**: `DELETE /v1/employees/{id}`
**Autorização**: `x-authorize: DELETE_EMPLOYEE`

**Pré-condições**:
- DELETE do Employee realiza **soft‑delete** do Employee e de quaisquer Logins vinculados (marcar `status = DELETED`); **hard‑delete não é suportado**.
- Se há subordinados (outros Employees com `supervisorId` = id): proibir. Se houver dependências que impeçam remoção lógica, retornar 409 `SCOS_EMPLOYEE_005`.

**Fluxo**:
1. Buscar Employee
2. Validar dependências
3. Se dependências: retornar 409
4. Soft-delete: `status = DELETED`
4.a Soft-delete dos Logins vinculados: `UPDATE scos_login SET status = 'DELETED', updated_at = NOW() WHERE employee_id = :id AND status <> 'DELETED'`
5. Registrar auditoria para Employee e Logins afetados
6. Retornar 204 No Content

---

#### UseCase: ListEmployeesUseCase (GET `/v1/employees`)

**Query Parameters**:
```
?page=0&size=20&companyId=100&positionId=21&status=ACTIVE&name=João
```

**Resposta (200 OK)**:
```json
{
  "data": [
    { "id": 1000, "name": "João Silva", "email": "joao@company.com", "companyId": 100, "status": "ACTIVE" }
  ],
  "pagination": { "page": 0, "size": 20, "totalElements": 250 }
}
```

---

## 7. Sub-recursos — EmployeeContact

**Endpoint base**: `/v1/employees/{employeeId}/contacts`

### 7.1 CreateEmployeeContactUseCase (POST)

**Request Body**:
```json
{
  "contactType": "PHONE | EMAIL | EMERGENCY_PERSON (required)",
  "value": "string (required)",
  "label": "string (optional)"
}
```

**Validações**:
- Employee existe → `SCOS_EMPLOYEE_001` (404)
- `contactType` válido
- `value` formatado conforme tipo (phone: E.164, email: RFC 5322)

**Resposta**: 201 Created

---

### 7.2 UpdateEmployeeContactUseCase (PUT `/v1/employees/{employeeId}/contacts/{id}`)

**Resposta**: 204 No Content

---

### 7.3 GetEmployeeContactUseCase (GET `/v1/employees/{employeeId}/contacts/{id}`)

**Resposta**: 200 OK com contato

---

### 7.4 DeleteEmployeeContactUseCase (DELETE)

**Resposta**: 204 No Content

---

### 7.5 ListEmployeeContactsUseCase (GET `/v1/employees/{employeeId}/contacts`)

**Resposta**: 200 OK (paginated)

---

## 8. Sub-recursos — EmployeeAddress

**Endpoint base**: `/v1/employees/{employeeId}/addresses`

(Estrutura similar a EmployeeContact, com validações geográficas)

### 8.1-8.5: CRUD + List (similar a Company)

---

## 9. Exemplos JSON

### Employee — CreateEmployeeUseCase

**Request**:
```json
{
  "name": "Maria da Silva",
  "nameTreatment": "Maria",
  "taxIdentifier": "987.654.321-11",
  "email": "maria@company.com",
  "birthDate": "1992-08-20",
  "hireDate": "2021-03-15",
  "companyId": 100,
  "positionId": 21,
  "supervisorId": 1000
}
```

**Success (201)**:
```json
{ "data": { "id": 1001, "name": "Maria da Silva", "status": "ACTIVE" } }
```

---

## 10. Error-Code Mapping (Employee)

| Código | Significado |
|--------|-----------|
| `SCOS_EMPLOYEE_001` | Employee não encontrado |
| `SCOS_EMPLOYEE_002` | CPF duplicado |
| `SCOS_EMPLOYEE_003` | Email duplicado |
| `SCOS_EMPLOYEE_004` | Ciclo detectado em supervisor |
| `SCOS_EMPLOYEE_005` | Employee tem dependências (não pode deletar). Observação: Logins vinculados não são considerados dependências bloqueantes — são soft‑deleted automaticamente ao remover o Employee. |

---

## 11. Arquitetura e Relacionamentos

### 11.1 Hierarquia Employee

```
Employee (CEO, supervisorId=null)
├── Employee (Director, supervisorId=CEO)
│   ├── Employee (Manager, supervisorId=Director)
│   │   └── Employee (Team Lead, supervisorId=Manager)
│   │       └── Employee (Developer, supervisorId=Team Lead)
│   └── Employee (Other Manager)
└── Employee (Other Director)
```

**Prevenção de ciclos**: validado at domain level (A → B → A é proibido)

### 11.2 Fluxo criar Employee + Login

```
CreateEmployeeUseCase
    ↓
[Persist Employee, status=ACTIVE]
    ↓
[Evento employee.created]
    ↓
[Admin pode então: CreateLoginUseCase]
    ├─ username
    ├─ email (reusa do Employee)
    ├─ profileId
    └─ password
    ↓
[Login criado, sincronizado com Keycloak]
```

---

## 12. Invariantes de Negócio

1. **CPF único**
2. **Email único**
3. **Company válida**: todo Employee referencia Company ativa
4. **Position válida**: todo Employee referencia Position ativa
5. **Supervisor sem ciclos**: não há ciclos em `supervisorId`
6. **Datas coerentes**: `birthDate` < `hireDate` ≤ hoje
7. **Soft-delete com Login**: se há Login ativo, o Employee e os Logins vinculados são marcados `DELETED` (sem hard‑delete)
8. **Status imutável em deletado**: não alterar deletado sem ativar primeiro

---

## 13. Árvore de Erros

```
CreateEmployeeUseCase
    ├─ CPF duplicado? ──→ 409 SCOS_EMPLOYEE_002
    ├─ Email duplicado? ──→ 409 SCOS_EMPLOYEE_003
    ├─ Company não existe? ──→ 404 SCOS_COMPANY_001
    ├─ Position não existe? ──→ 404 SCOS-012
    ├─ Datas incoerentes? ──→ 400 SCOS-002
    └─ Supervisor ciclo? ──→ 400 SCOS_EMPLOYEE_004
```

---

## 14. Implementação Recomendada (4 fases, ~4-5 dias)

Fase 1: Domain (1 dia) - Employee entity, validações
Fase 2: Application (1-2 dias) - UseCases, eventos
Fase 3: Infrastructure + Login (1 dia) - integração Login/Keycloak
Fase 4: Presentation (1 dia) - controllers, E2E

---

## 15. Cenários de Fluxo

### Cenário 1: Criar Employee + Login
1. POST `/v1/employees` → 201
2. POST `/v1/logins` (username, profileId) → 201
3. Ambos sincronizados em Keycloak

### Cenário 2: Inativar Employee
1. PUT `/v1/employees/{id}/status` (INACTIVE) → 204
2. Opcionalmente: Login bloqueado
3. Employee não pode ser atribuído a Positions novas

### Cenário 3: Tentar deletar com Login ativo
1. DELETE `/v1/employees/{id}` → 409 SCOS_EMPLOYEE_005
2. Recuperação: soft-delete automático (não hard-delete)

---

## 16. Referências Técnicas

- OpenAPI: `etc/api/organization/ScosOrganization_Employee.yml`
- Domain: `scos-organization-domain/.../employee/Employee.java`
- UseCases: `scos-organization-application/.../usecase/employee/`
- Controllers: `scos-organization-api/.../employee/EmployeeController.java`
- Testes: `scos-organization-domain/src/test/.../employee/EmployeeTest.java`

---

## 17. Histórico de Versão

| Versão | Data | Autor | Alteração |
|--------:|:-----:|:-----|:---------|
| 1.1 | 2026-02-17 | GitHub Copilot | **Reestruturação completa**: Visão Estratégica, Índice, Schema BD (Employee, Contact, Address), UseCases detalhados (Create, Update, Get, Status, Delete, List), Sub-recursos Contact/Address (CRUD+List), Exemplos JSON, Error Mapping, Arquitetura (hierarquia, fluxo Login), Invariantes, Árvore Erros, Implementação 4 fases, Cenários, Referências. Expandido de 67 para ~1.100 linhas. |
| 1.0 | inicial | — | Documento base (origem OpenAPI) |
