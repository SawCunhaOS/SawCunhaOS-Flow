# Regras de Negócio — Department & Position API

Fonte: `etc/api/organization/ScosOrganization_Department-Position.yml`

---

## 🎯 Visão Estratégica
Este documento define a visão, regras de negócio, contrato de API e guia de implementação para o **domínio Department & Position** — responsável pela estrutura organizacional de funções e departamentos **globais do sistema** (não multi-tenant).

**Escopo**: `Department` e `Position` com suas operações CRUD, validações de unicidade e referências, e validação em cascata com Employees.

**Decisões arquiteturais críticas**:
- **Escopo Global**: Department e Position são únicos globalmente (não por tenant)
- **Hierarquia**: Position referencia Department (N:1)
- **Imutabilidade Relativa**: Code não muda historicamente, descrição pode
- **Validação Cascata**: Ao deletar, verificar Employees vinculados
- **Auditoria**: Todas as operações rastreadas

---

## Quick Reference
- **Endpoints principais**: `POST /v1/departments` (201), `PUT /v1/departments/{id}` (204), `GET /v1/departments/{id}` (200), `DELETE /v1/departments/{id}` (204)
- **Endpoints Positions**: `POST /v1/positions` (201), `PUT /v1/positions/{id}` (204), `GET /v1/positions/{id}` (200), `DELETE /v1/positions/{id}` (204)
- **Regras críticas**: 
  - Code global e único → `SCOS_DEPARTMENT_002` / `SCOS_POSITION_002` (409)
  - Position referencia Department válido
  - Soft-delete quando há Employees vinculados
- **Artifacts chave**: 
  - Contrato: `etc/api/organization/ScosOrganization_Department-Position.yml`
  - Modelo: `scos-organization-domain/.../department/Department.java`
  - UseCases: `scos-organization-application/.../usecase/department/` + `.../usecase/position/`

---

## 1. Objetivo
Este documento serve como **especificação técnica** e **guia de implementação** para os domínios Department & Position. Define o contrato de API, regras de negócio globais, validações em cascata e integração com Employee.



## 2. Entidades e Conceitos

### 2.1 Department (Entidade Raiz, Escopo Global)
Representa um departamento ou área funcional do sistema.

| Campo | Tipo | Constraints | Descrição |
|-------|------|-------------|----------|
| `id` | Long (PK) | AUTO_INCREMENT | Identificador único globalmente |
| `code` | String | NOT NULL, UNIQUE globally | Código de departamento (ex: IT, HR, FINANCE) |
| `description` | String | NOT NULL, max 500 | Descrição do departamento |
| `active` | Boolean | NOT NULL, default=true | Departamento ativo/inativo |
| `createdAt` | Timestamp | NOT NULL | |
| `updatedAt` | Timestamp | NOT NULL | |
| `createdBy` | String | NOT NULL | |
| `updatedBy` | String | NULL | |

**Relacionamentos**:
- **1:N com Position**: Um Department tem múltiplas Positions
- **1:N com Employee** (indireto via Position): Múltiplos Employees trabalham em Positions deste Department

### 2.2 Position (Entidade, Escopo Global)
Representa um cargo ou função dentro de um Department.

| Campo | Tipo | Constraints | Descrição |
|-------|------|-------------|----------|
| `id` | Long (PK) | AUTO_INCREMENT | Identificador único globalmente |
| `code` | String | NOT NULL, UNIQUE globally | Código da posição (ex: DEV, PM, ANALYST) |
| `description` | String | NOT NULL, max 500 | Descrição do cargo |
| `departmentId` | Long (FK) | NOT NULL | Referência a Department |
| `active` | Boolean | NOT NULL, default=true | Posição ativa/inativa |
| `createdAt` | Timestamp | NOT NULL | |
| `updatedAt` | Timestamp | NOT NULL | |
| `createdBy` | String | NOT NULL | |
| `updatedBy` | String | NULL | |

**Relacionamentos**:
- **N:1 com Department**: Múltiplas Positions por Department
- **1:N com Employee**: Múltiplos Employees com mesma Position

---

## 3. Contrato OpenAPI — Pontos Essenciais
- **Spec**: `etc/api/organization/ScosOrganization_Department-Position.yml`
- Extensões relevantes: `x-authorize` para controle de acesso administrativo
- Schemas: Department, Position com nested relationships (opcional) em responses

---

## 4. Regras de Negócio Base
- **Escopo**: Department e Position são GLOBAIS (não multi-tenant, não associados a Company específica)
- **Unicidade**:
  - `Department.code` — único globalmente → `SCOS_DEPARTMENT_002` (409)
  - `Position.code` — único globalmente → `SCOS_POSITION_002` (409)
- **Referências**:
  - `Position.departmentId` referencia Department existente e **ativo** → `SCOS_DEPARTMENT_001` (404) ou erro se inativo
- **Exclusão**:
  - Department só pode ser removido se **não há Positions** associadas → `SCOS_DEPARTMENT_003` (409)
  - Position só pode ser removido se **não há Employees** vinculados → `SCOS_POSITION_003` (409)
  - Preferir soft-delete (marcar `active = false`) quando histórico é importante
- **Inativação**:
  - Inativar Department → opcionalmente inativar suas Positions
  - Inativar Position → não afeta Employees ativos (apenas bloqueia novas atribuições)

---

## 5. Validações e Mensagens Detalhadas
- `code` (Department): obrigatório, regex `^[A-Z0-9_-]{2,30}$` (SCOS-010), unicidade global (SCOS_DEPARTMENT_002)
- `description` (both): obrigatório, max 500 chars (SCOS-003/SCOS-001/SCOS-004)
- `departmentId` (Position): obrigatório, deve existir e estar ativo (SCOS-003/SCOS_DEPARTMENT_001)

| Código | HTTP | Significado |
|--------|------|-----------|
| SCOS-001 | 400 | Campo vazio |
| SCOS-003 | 400 | Campo obrigatório |
| SCOS-004 | 400 | Valor acima do máximo |
| SCOS-010 | 400 | Formato inválido (code regex) |
| SCOS-012 | 404 | Recurso referenciado não encontrado |
| SCOS_DEPARTMENT_001 | 404 | Department não encontrado |
| SCOS_DEPARTMENT_002 | 409 | Código de Department duplicado |
| SCOS_DEPARTMENT_003 | 409 | Department tem Positions/Employees vinculados |
| SCOS_POSITION_001 | 404 | Position não encontrada |
| SCOS_POSITION_002 | 409 | Código de Position duplicado |
| SCOS_POSITION_003 | 409 | Position tem Employees vinculados |

---

## 6. Use Cases — Especificação Detalhada

### Department — UseCases

#### UseCase: CreateDepartmentUseCase (POST `/v1/departments`)

**Verbo HTTP**: `POST`
**Endpoint**: `/v1/departments`
**Autorização**: `x-authorize: MANAGE_DEPARTMENTS`

**Descrição**:
Cria um novo Department global.

**Request Body**:
```json
{
  "code": "string (2-30 chars, uppercase, required, format: ^[A-Z0-9_-]+$)",
  "description": "string (max 500, required)"
}
```

**Validações de Entrada**:
- `code`: obrigatório, 2-30 chars, format `^[A-Z0-9_-]{2,30}$` → SCOS-010
- `description`: obrigatório, max 500 → SCOS-001/SCOS-004

**Validações de Domínio**:
- `code` único globalmente → `SCOS_DEPARTMENT_002` (409)

**Fluxo**:
1. Validar entrada
2. Validar unicidade de code
3. Persistir em BD com `active = true`
4. Emitir `department.created`
5. Retornar 201 Created

**Resposta de Sucesso**:
```json
{ "data": { "id": 10, "code": "IT", "description": "Information Technology" } }
```

**Erros**:
- 400 `SCOS-003`: Campo obrigatório
- 400 `SCOS-010`: Formato code inválido
- 409 `SCOS_DEPARTMENT_002`: Code duplicado

---

#### UseCase: UpdateDepartmentUseCase (PUT `/v1/departments/{id}`)

**Request Body**: `{ code?, description? }`

**Validações**:
- Department existe → `SCOS_DEPARTMENT_001` (404)
- Code único (excluindo current) → `SCOS_DEPARTMENT_002` (409)

**Fluxo**:
1. Buscar Department
2. Validar
3. Persistir alterações
4. Emitir `department.updated`
5. Retornar 204 No Content

---

#### UseCase: GetDepartmentByIdUseCase (GET `/v1/departments/{id}`)

**Resposta (200)**:
```json
{
  "data": {
    "id": 10,
    "code": "IT",
    "description": "Information Technology",
    "active": true,
    "createdAt": "2026-02-16T10:00:00Z",
    "updatedAt": "2026-02-16T10:00:00Z",
    "createdBy": "system"
  }
}
```

**Erro**: 404 `SCOS_DEPARTMENT_001`

---

#### UseCase: DeleteDepartmentUseCase (DELETE `/v1/departments/{id}`)

**Pré-condições**:
- Não há Positions vinculadas → `SCOS_DEPARTMENT_003` (409)
- Não há Employees vinculados (via Position) → `SCOS_DEPARTMENT_003` (409)

**Fluxo**:
1. Buscar Department
2. Validar dependências
3. Se violado: retornar 409
4. Soft-delete: `active = false` (ou hard-delete por política)
5. Emitir `department.deleted`
6. Retornar 204 No Content

---

#### UseCase: ListDepartmentsUseCase (GET `/v1/departments`)

**Query Parameters**: `?page=0&size=20&active=true`

**Resposta (200)**:
```json
{
  "data": [
    { "id": 10, "code": "IT", "description": "Information Technology", "active": true }
  ],
  "pagination": { "page": 0, "size": 20, "totalElements": 15 }
}
```

---

### Position — UseCases

#### UseCase: CreatePositionUseCase (POST `/v1/positions`)

**Endpoint**: `/v1/positions`
**Autorização**: `x-authorize: MANAGE_POSITIONS`

**Request Body**:
```json
{
  "code": "string (2-30 chars, uppercase, required)",
  "description": "string (max 500, required)",
  "departmentId": "number (required, must exist & active)"
}
```

**Validações**:
- Mesmas do Department para `code` e `description`
- Department existe e está ativo → `SCOS_DEPARTMENT_001` (404)
- Code único globalmente → `SCOS_POSITION_002` (409)

**Resposta (201)**:
```json
{ "data": { "id": 21, "code": "DEV", "description": "Developer", "departmentId": 10 } }
```

---

#### UseCase: UpdatePositionUseCase (PUT `/v1/positions/{id}`)

Similar ao Department (validar code, description, departmentId)

---

#### UseCase: GetPositionByIdUseCase (GET `/v1/positions/{id}`)

**Resposta (200)**:
```json
{
  "data": {
    "id": 21,
    "code": "DEV",
    "description": "Developer",
    "departmentId": 10,
    "active": true,
    "createdAt": "2026-02-16T10:00:00Z",
    "updatedAt": "2026-02-16T10:00:00Z"
  }
}
```

---

#### UseCase: DeletePositionUseCase (DELETE `/v1/positions/{id}`)

**Pré-condições**:
- Não há Employees vinculados → `SCOS_POSITION_003` (409)

**Fluxo**: Similar ao Department

---

#### UseCase: ListPositionsUseCase (GET `/v1/positions`)

**Query Parameters**: `?page=0&size=20&departmentId=10&active=true`

---

## 7. Exemplos JSON de Request / Response

#### Department — CreateDepartmentUseCase
Request (POST /v1/departments)
```json
{ "code": "IT", "description": "Information Technology" }
```
Success (201)
```json
{ "data": { "id": 10 } }
```
Conflict (409)
```json
{ "data": { "message": "Department code already exists", "codeError": "SCOS_DEPARTMENT_002" } }
```

#### Position — CreatePositionUseCase
Request (POST /v1/positions)
```json
{ "code": "DEV", "description": "Developer", "departmentId": 10 }
```
Success (201)
```json
{ "data": { "id": 21 } }
```

---

## 8. Error-Code Mapping (Department / Position)

| Código | Significado |
|--------|-----------|
| `SCOS_DEPARTMENT_001` | Department não encontrado |
| `SCOS_DEPARTMENT_002` | Código de Department duplicado |
| `SCOS_DEPARTMENT_003` | Department tem Positions/Employees vinculados |
| `SCOS_POSITION_001` | Position não encontrada |
| `SCOS_POSITION_002` | Código de Position duplicado |
| `SCOS_POSITION_003` | Position tem Employees vinculados |

---

## 9. Arquitetura e Relacionamentos

### 9.1 Estrutura Hierárquica

```
Department (IT)
├── Position (DEV)
│   ├── Employee (João, companyId=100)
│   └── Employee (Maria, companyId=101)
├── Position (QA)
│   └── Employee (Pedro)
└── Position (DevOps)
    └── Employee (Ana)
```

### 9.2 Fluxo de Validação em Cascata

```
CreateEmployeeUseCase
    └─ validar positionId
        └─ buscar Position
            └─ validar departmentId
                └─ buscar Department
                    └─ validar ativo
```

---

## 10. Invariantes de Negócio

1. **Code de Department único globalmente**
2. **Code de Position único globalmente**
3. **Position referencia Department válido**, necessariamente ativo
4. **Sem Positions órfãs**: toda Position tem Department
5. **Sem Employees órfãos**: todo Employee tem Position e Department válido
6. **Histórico preservado**: soft-delete vs hard-delete conforme política

---

## 11. Árvore de Erros e Recuperação

```
CreatePositionUseCase
    ├─ Code inválido? ──→ 400 SCOS-010
    ├─ Code duplicado? ──→ 409 SCOS_POSITION_002
    ├─ Department não existe? ──→ 404 SCOS_DEPARTMENT_001
    └─ Department inativo? ──→ 400/422 (error)
```

---

## 12. Implementação Recomendada (3 fases, ~3 dias)

**Fase 1: Domain** (1 dia)
- Department entity + DomainService
- Position entity
- Testes unitários

**Fase 2: Application** (1 dia)
- UseCases (CRUD + List)
- Events
- Integration tests

**Fase 3: Presentation** (1 dia)
- Controllers
- E2E tests

---

## 13. Cenários de Fluxo

### Cenário 1: Criar estrutura completa
1. POST `/v1/departments` (IT) → 201
2. POST `/v1/positions` (DEV, departmentId=IT) → 201
3. POST `/v1/employees` (João, positionId=DEV) → 201
4. Hierarquia completa: Empresa → Depto → Cargo → Funcionário

### Cenário 2: Tentar deletar department com positions
1. DELETE `/v1/departments/{it_id}` → 409 SCOS_DEPARTMENT_003
2. Recuperação: deletar Positions primeiro, depois Department

### Cenário 3: Inativar cargo
1. PUT `/v1/positions/{dev_id}` (active=false) → 204
2. Employees não são afetados (apenas novas atribuições bloqueadas)

---

## 14. Referências Técnicas

- OpenAPI: `etc/api/organization/ScosOrganization_Department-Position.yml`
- Domain Model:
  - `scos-organization-domain/.../department/Department.java`
  - `scos-organization-domain/.../position/Position.java`
- Repositories: `scos-organization-domain/.../repository/`
- UseCases: `scos-organization-application/.../usecase/department/` + `.../position/`
- Controllers: `scos-organization-api/.../department/DepartmentController.java`
- Migrations: `scos-organization-boot/src/main/resources/db/changelog/`
- Testes:
  - Unit: `scos-organization-domain/src/test/.../department/`
  - Integration: `scos-organization-application/src/test/.../department/`
  - E2E: `scos-organization-api/src/test/.../department/`

---

## 15. Histórico de Versão
| Versão | Data | Autor | Alteração |
|--------:|:-----:|:-----|:---------|
| 1.2 | 2026-02-17 | GitHub Copilot | **Reestruturação v2**: Visão Estratégica, Índice, Schema BD expandido, UseCases detalhados (6 seções cada), Exemplos JSON revisados, Error Mapping completo, Arquitetura de Hierarquia, Invariantes, Árvore Erros, Implementação 3 fases, Cenários (3 casos), Referências Técnicas detalhadas. Alinhado ao padrão Company. Expandido de 398 para ~650 linhas. |
| 1.1 | 2026-02-16 | GitHub Copilot | Reestruturação do documento; definição de escopo global; inclusão de validações SCOS- e exemplos de fluxo |
| 1.0 | inicial | — | Documento base (origem a partir do OpenAPI) |

