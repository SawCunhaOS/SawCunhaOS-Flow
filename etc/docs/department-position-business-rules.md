# Regras de Negócio — Department & Position API

Fonte: `etc/api/organization/ScosOrganization_Department-Position.yml`

---

## 1. Visão geral do documento
Este documento descreve as regras de negócio, validações, casos de uso e fluxos para as entidades **Department** e **Position**. Destina-se a desenvolvedores, analistas de requisitos e QA — serve como referência para implementação, testes e integração com outros módulos (ex.: Employee, Login).

---

## 2. Detalhe: o que são Department e Position
- Department (Departamento): entidade organizacional que agrupa funções ou áreas do sistema. Campos principais: `id`, `code`, `description`.
- Position (Cargo): função ou posto associado a um `Department`. Campos principais: `id`, `code`, `description`, `departmentId`.

Notas importantes:
- Ambos são GLOBAIS no sistema (não dependen de `Company`).
- `Position` é usado para validar e persistir regras aplicáveis ao `Employee`.
- Consulte o modelo de domínio em `scos-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/model` para o mapeamento de entidades.

---

## 3. Regras de negócio (resumo)
- Escopo: `Department` e `Position` são globais.
- Unicidade:
  - `Department.code` — único globalmente.
  - `Position.code` — único globalmente.
- Validação referencial:
  - `departmentId` referenciado em `Position` deve existir e estar `active`.
  - Ao criar/atualizar `Employee`, validar `position_id` contra `Position` existente (active).
- Exclusão:
  - `Department` só pode ser removido se não houver `Position` ou `Employee` associado; caso contrário retornar 409/422.
  - Preferir soft-delete (`status = DELETED`) quando histórico precisar ser preservado.

---

## 4. Validações e mensagens (detalhadas)
- `code`: obrigatório, regex `^[A-Z0-9_-]{2,30}$` → SCOS-010 (formato) / SCOS-003 (obrigatório).
- `description`: obrigatório, max 250 chars → SCOS-001 / SCOS-004.

Códigos de resposta e mensagens (resumo):
- SCOS-001 — campo vazio
- SCOS-002 — valor abaixo do mínimo
- SCOS-003 — campo obrigatório
- SCOS-004 — valor acima do máximo
- SCOS-010 — formato inválido
- SCOS-011 — duplicidade / violação de unicidade
- SCOS-012 — recurso não encontrado
- SCOS-013 — conflito por dependência (não pode excluir)

Exemplo de mapeamento HTTP:
- 400 — validação de payload (SCOS-001 / SCOS-003 / SCOS-010)
- 404 — recurso não encontrado (SCOS-012)
- 409 — conflito (SCOS-011 / SCOS-013)

---

## 5. Use Cases (cada operação = UseCase)

Abaixo cada operação é descrita como um UseCase (entrada, validações, erros, persistência e eventos). Os campos de BD citados seguem o diagrama em `etc/database/department-position.puml`.

### Department — UseCases

#### UseCase: CreateDepartmentUseCase (POST `/v1/departments`)
- Request payload: `{ code, description }`
- Persiste em: `SCOS_DEPARTMENT` (`DEPARTMENT_ID`, `CODE`, `DESCRIPTION`, `CREATED_AT`, `UPDATED_AT`, `USER_AT`)
- Field validations (payload):
  - `code`: required (SCOS-003)
  - `description`: required (SCOS-001), max 250 chars (SCOS-004)
- Domain validations:
  - `CODE` uniqueness → domain error `SCOS_DEPARTMENT_002`
- Success: 201 Created + Location
- Audit: set `CREATED_AT`, `UPDATED_AT`, `USER_AT`
- Example errors:
  - 400 JSON validation → SCOS-003 / SCOS-010
  - 409 Conflict → SCOS_DEPARTMENT_002

#### UseCase: UpdateDepartmentUseCase (PUT `/v1/departments/{id}`)
- Request payload: `{ code, description }`
- Read / update `SCOS_DEPARTMENT` fields + `UPDATED_AT`, `USER_AT`
- Field validations: same as Create
- Domain validations:
  - Department exists → `SCOS_DEPARTMENT_001` -> 404/ScosException
  - `code` uniqueness (excluding current id) → `SCOS_DEPARTMENT_002`
- Success: 204 No Content

#### UseCase: GetDepartmentByIdUseCase (GET `/v1/departments/{id}`)
- Returns department DTO mapped from `SCOS_DEPARTMENT` (include `CREATED_AT`, `UPDATED_AT`, `USER_AT` in metadata)
- Errors:
  - Not found → `SCOS_DEPARTMENT_001` / SCOS-012 (404)
- Success: 200 + payload

#### UseCase: DeleteDepartmentUseCase (DELETE `/v1/departments/{id}`)
- Preconditions:
  - No `Position` linked (check `SCOS_POSITION` by `DEPARTMENT_ID`)
  - No `Employee` linked to any Position under department (query)
- Domain error if linked: `SCOS_DEPARTMENT_003` (alias `SCOS_DEPARTMENT_003`) → 409
- On success: perform soft-delete (set `STATUS = DELETED`) or hard-delete per policy → 204

#### UseCase: ListDepartmentsUseCase (GET `/v1/departments`)
- Supports pagination (`paginationFilter`) and sorting
- Returns paginated DTO (use `ScosPaginated` schema)
- Success: 200

---

### Position — UseCases

#### UseCase: CreatePositionUseCase (POST `/v1/positions`)
- Request payload: `{ code, description, departmentId }`
- Persiste em: `SCOS_POSITION` (`POSITION_ID`, `CODE`, `DESCRIPTION`, `DEPARTMENT_ID`, `CREATED_AT`, `UPDATED_AT`, `USER_AT`)
- Field validations:
  - `code`: required (SCOS-003), format `^[A-Z0-9_-]{2,30}$` (SCOS-010)
  - `description`: required (SCOS-001)
  - `departmentId`: required, must be valid (SCOS-003 / SCOS-012)
- Domain validations:
  - `departmentId` exists and active → `SCOS_DEPARTMENT_001` (or position-specific `SCOS_POSITION_003` if applicable)
  - `code` uniqueness → `SCOS_POSITION_002`
- Success: 201

#### UseCase: UpdatePositionUseCase (PUT `/v1/positions/{id}`)
- Payload: `{ code, description, departmentId }`
- Validations: same as Create
- Domain errors:
  - Position not found → `SCOS_POSITION_001`
  - Duplicate code → `SCOS_POSITION_002`
- Success: 204

#### UseCase: GetPositionByIdUseCase (GET `/v1/positions/{id}`)
- Returns Position DTO (include linked `departmentId` and audit fields)
- Errors: not found → `SCOS_POSITION_001` / SCOS-012 (404)
- Success: 200

#### UseCase: DeletePositionUseCase (DELETE `/v1/positions/{id}`)
- Preconditions:
  - No `Employee` linked to this position (check `SCOS_EMPLOYEE.POSITION_ID`)
- Domain error if linked: `SCOS_POSITION_003` → 409
- Success: 204

#### UseCase: ListPositionsUseCase (GET `/v1/positions`)
- Paginated list; supports filtering by `departmentId`
- Success: 200

---

### Exemplos JSON de Request / Response (por UseCase)

#### Department — CreateDepartmentUseCase
Request (POST /v1/departments)
```json
{
  "code": "IT",
  "description": "Information Technology"
}
```
Success (201 Created)
```json
{
  "data": { "id": 10 }
}
```
Validation error (400)
```json
{
  "data": {
    "message": "Validation failed",
    "codeError": "SCOS-003",
    "validationErrors": [
      { "attribute": "code", "message": "Campo obrigatório: code (SCOS-003)" }
    ]
  }
}
```
Conflict (409) — duplicate code
```json
{
  "data": {
    "message": "Conflict: duplicate resource",
    "codeError": "SCOS_DEPARTMENT_002",
    "validationErrors": [
      { "attribute": "code", "message": "Department code already exists (SCOS_DEPARTMENT_002)" }
    ]
  }
}
```

#### Department — UpdateDepartmentUseCase
Request (PUT /v1/departments/10)
```json
{
  "code": "IT",
  "description": "Updated Information Technology"
}
```
Success (204 No Content): sem corpo

Not found (404)
```json
{
  "data": { "message": "Not found", "codeError": "SCOS_DEPARTMENT_001" }
}
```

#### Department — GetDepartmentByIdUseCase
Success (200)
```json
{
  "data": {
    "id": 10,
    "code": "IT",
    "description": "Information Technology",
    "createdAt": "2026-02-16T10:00:00",
    "updatedAt": "2026-02-17T12:00:00",
    "userAt": "system"
  }
}
```
Not found (404)
```json
{ "data": { "message": "Not found", "codeError": "SCOS_DEPARTMENT_001" } }
```

#### Department — DeleteDepartmentUseCase
Success (204 No Content)

Conflict (409) — has linked positions/employees
```json
{
  "data": {
    "message": "Conflict: resource has dependencies",
    "codeError": "SCOS_DEPARTMENT_003",
    "validationErrors": [
      { "attribute": "departmentId", "message": "Cannot delete department with linked positions or employees (SCOS_DEPARTMENT_003)" }
    ]
  }
}
```

#### Department — ListDepartmentsUseCase (paginated)
Success (200)
```json
{
  "data": [
    { "id": 10, "code": "IT", "description": "Information Technology", "createdAt": "2026-02-16T10:00:00", "updatedAt": "2026-02-17T12:00:00", "userAt": "system" }
  ],
  "PaginatedDTO": { "sizePerPage": 10, "totalPages": 1, "totalElements": 1, "totalElementsPerPage": 1 }
}
```

---

#### Position — CreatePositionUseCase
Request (POST /v1/positions)
```json
{
  "code": "DEV",
  "description": "Developer",
  "departmentId": 10
}
```
Success (201 Created)
```json
{ "data": { "id": 21 } }
```
Validation error (400)
```json
{
  "data": {
    "message": "Validation failed",
    "codeError": "SCOS-003",
    "validationErrors": [
      { "attribute": "departmentId", "message": "Campo obrigatório: departmentId (SCOS-003)" }
    ]
  }
}
```
Department not found (404)
```json
{ "data": { "message": "Department not found", "codeError": "SCOS_DEPARTMENT_001" } }
```
Conflict (409) — duplicate code
```json
{
  "data": {
    "message": "Conflict: duplicate resource",
    "codeError": "SCOS_POSITION_002",
    "validationErrors": [
      { "attribute": "code", "message": "Position code already exists (SCOS_POSITION_002)" }
    ]
  }
}
```

#### Position — UpdatePositionUseCase
Request (PUT /v1/positions/21)
```json
{ "code": "DEV", "description": "Senior Developer", "departmentId": 10 }
```
Success (204 No Content)
Not found (404)
```json
{ "data": { "message": "Position not found", "codeError": "SCOS_POSITION_001" } }
```
Duplicate code (409)
```json
{ "data": { "message": "Conflict: duplicate resource", "codeError": "SCOS_POSITION_002" } }
```

#### Position — GetPositionByIdUseCase
Success (200)
```json
{
  "data": {
    "id": 21,
    "code": "DEV",
    "description": "Developer",
    "departmentId": 10,
    "createdAt": "2026-02-16T10:00:00",
    "updatedAt": "2026-02-17T12:00:00",
    "userAt": "system"
  }
}
```
Not found (404)
```json
{ "data": { "message": "Position not found", "codeError": "SCOS_POSITION_001" } }
```

#### Position — DeletePositionUseCase
Success (204 No Content)
Conflict (409) — employees linked
```json
{
  "data": {
    "message": "Conflict: position has linked employees",
    "codeError": "SCOS_POSITION_003",
    "validationErrors": [
      { "attribute": "positionId", "message": "Cannot delete position with linked employees (SCOS_POSITION_003)" }
    ]
  }
}
```

#### Position — ListPositionsUseCase (paginated)
Success (200)
```json
{
  "data": [
    { "id": 21, "code": "DEV", "description": "Developer", "departmentId": 10, "createdAt": "2026-02-16T10:00:00", "updatedAt": "2026-02-17T12:00:00", "userAt": "system" }
  ],
  "PaginatedDTO": { "sizePerPage": 10, "totalPages": 1, "totalElements": 1, "totalElementsPerPage": 1 }
}
```

---

## Error-code mapping (department / position)
| Código | Alias (legado) | Significado |
|--------|----------------|------------|
| `SCOS_DEPARTMENT_001` | `SCOS_DEPARTMENT_001` | Department não encontrado (GET/UPDATE/DELETE) |
| `SCOS_DEPARTMENT_002` | `SCOS_DEPARTMENT_002` | Código do Department duplicado (create/update) |
| `SCOS_DEPARTMENT_003` | `SCOS_DEPARTMENT_003` | Conflito: Department tem Positions/Employees vinculados (delete) |
| `SCOS_POSITION_001` | — | Position não encontrada |
| `SCOS_POSITION_002` | — | Código da Position duplicado |
| `SCOS_POSITION_003` | — | Conflito: Position com Employees vinculados (delete) |

> Observação: use os códigos `SCOS_...` como padrão;


## 6. Exemplo de fluxo (scenario)
Cenário: adicionar um cargo e associar a um funcionário

1. POST `/v1/departments` payload { code, description } → 201 (departmentId)
2. POST `/v1/positions` payload { code, description, departmentId } → 201 (positionId)
3. POST `/v1/employee` payload { ..., position_id: positionId } → 201 (employee criado)
4. DELETE `/v1/departments/{departmentId}` → 409 SCOS-013 (não é possível excluir com positions vinculadas)
5. DELETE `/v1/positions/{positionId}` → 204
6. DELETE `/v1/departments/{departmentId}` → 204 (após remoção das positions)

Validações/erros esperados no fluxo:
- Se `positionId` inválido ao criar Employee → 404 / SCOS-012
- Se `code` duplicado ao criar Department/Position → 409 / SCOS-011

---

## 7. Tabela de versão / histórico de alterações
| Versão | Data | Autor | Alteração |
|--------:|:-----:|:-----|:---------|
| 1.1 | 2026-02-16 | GitHub Copilot | Reestruturação do documento; definição de escopo global; inclusão de validações SCOS- e exemplos de fluxo |
| 1.0 | inicial | — | Documento base (origem a partir do OpenAPI) |

---

## 8. Observações técnicas e links úteis
- OpenAPI source: `etc/api/organization/ScosOrganization_Department-Position.yml`
- Modelo de domínio: `scos-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/model`
- Use `x-jdempotentresource` em endpoints de criação para evitar duplicações.

