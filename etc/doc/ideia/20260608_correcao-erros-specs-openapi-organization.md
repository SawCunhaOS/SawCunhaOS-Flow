# Correção de Erros nas Specs OpenAPI — Módulo Organization

**Data**: 2026-06-08  
**Status**: 🔄 Em Análise  
**Tipo**: 🐛 Bug Fix

---

## ⚠️ Princípio SRP — Uma Funcionalidade por Ideia

- **Nome da funcionalidade**: `correcao-erros-specs-openapi-organization`
- **Resumo em uma frase**: Corrigir erros de referência de schema, operationId, parâmetros e server URL nas specs OpenAPI do módulo Organization.

**Checklist SRP**:
- [x] Esta ideia cobre exatamente uma funcionalidade
- [x] Não mistura features independentes no mesmo arquivo
- [x] O nome é específico (não genérico como "melhorar módulo X")

---

## 1️⃣ Visão

### Problema

As specs OpenAPI do módulo Organization possuem erros de copy-paste que causam divergência entre a documentação e o contrato real da API. Esses erros afetam geração de código (openapi-generator), testes contrato, e onboarding de novos desenvolvedores.

Erros identificados:

| # | Arquivo | Local | Problema |
|---|---------|-------|---------|
| E-01 | `ScosOrganization_Employee.yml` | linha 117 (`updateEmployee` requestBody) | Referencia `UpdateEmployeeAddressRequest` — deveria ser `UpdateEmployeeRequest` (schema renomeado de `UpdateEmployeeDTO`) |
| E-02 | `ScosOrganization_Employee.yml` | linha 294 (`updateEmployeeContact` requestBody) | Referencia `UpdateEmployeeContactDTO` — schema inexistente; deveria ser `UpdateEmployeeContactRequest` |
| E-03 | `ScosOrganization_Employee.yml` | linha 425 (`updateEmployeeAddress` requestBody) | Referencia `UpdateEmployeeContactRequest` — deveria ser `UpdateEmployeeAddressRequest` |
| E-04 | `ScosOrganization_Employee.yml` | schema `UpdateEmployeeDTO` | Nome segue padrão `*DTO` (input); padrão do projeto é `*Request` — renomear para `UpdateEmployeeRequest` |
| E-05 | `ScosOrganization_Login.yml` | linha 50–56 (`POST /v1/employee/{employeeId}/login`) | `summary`, `description` e `operationId` dizem "Update employee contact" — deveria ser "Create employee login" / `createEmployeeLogin` |
| E-06 | `ScosOrganization_Login.yml` | linha 247–248 (`GET /v1/employee/login/info`) | Referencia params `idEmployee` e `idRequest` como path params, mas a URI `/v1/employee/login/info` não contém `{employeeId}` nem `{id}` |
| E-07 | `ScosOrganization_Login.yml` | linha 398 (`DELETE /v1/profile/{id}`) | Referencia param `idEmployee` mas URI `/v1/profile/{id}` não contém `{employeeId}` |
| E-08 | `ScosOrganization_Employee.yml` | linha 15 (`servers.url`) | Valor `organization/api` — sem barra inicial (inconsistente com Company: `/organization/api`) |
| E-09 | `ScosOrganization_Department-Position.yml` | linha 15 (`servers.url`) | Valor `organization/api` — sem barra inicial |
| E-10 | `ScosOrganization_Login.yml` | linha 15 (`servers.url`) | Valor `organization/api` — sem barra inicial |

### Objetivo

Todas as specs OpenAPI do módulo Organization com schemas refs, operationIds, parâmetros e server URL corretos. Critério: specs passam em validação de linter (spectral ou similar) sem erros de referência.

### Fora de Escopo

- Mudanças de design REST (enable/disable como sub-resource vs state field) — pertence a `20260608_adequacao-rest-nivel2-organization.md`
- Adição de novos endpoints — pertence a outra ideia
- Implementação Java (controllers, services) — esta ideia é somente spec YAML

---

## 2️⃣ Requisitos

### Funcionais

- [ ] **RF-01** *(E-04)*: Schema `UpdateEmployeeDTO` renomeado para `UpdateEmployeeRequest` — alinhamento com padrão `*Request` para DTOs de entrada (igual `UpdateCompanyRequest`, `UpdateDepartmentRequest`)
- [ ] **RF-02** *(E-01)*: `updateEmployee` (PUT `/v1/employee/{id}`) referencia `UpdateEmployeeRequest`
- [ ] **RF-03** *(E-02)*: `updateEmployeeContact` (PUT `/v1/employee/{employeeId}/contact/{id}`) referencia `UpdateEmployeeContactRequest`
- [ ] **RF-04** *(E-03)*: `updateEmployeeAddress` (PUT `/v1/employee/{employeeId}/address/{id}`) referencia `UpdateEmployeeAddressRequest`
- [ ] **RF-05** *(E-05)*: `POST /v1/employee/{employeeId}/login` tem `summary`, `description` e `operationId` corretos (`createEmployeeLogin`)
- [ ] **RF-06** *(E-06)*: `GET /v1/employee/login/info` não referencia path params inexistentes na URI
- [ ] **RF-07** *(E-07)*: `DELETE /v1/profile/{id}` não referencia param `idEmployee` inexistente na URI
- [ ] **RF-08** *(E-08, E-09, E-10)*: Todos os arquivos de spec usam `servers.url: /organization/api` (com barra inicial)

### Não-Funcionais

- [ ] **RNF-01**: Nenhuma mudança breaking no contrato HTTP (correção de documentação, não de comportamento)

---

## 3️⃣ Arquitetura

### Componentes Afetados

```
etc/api/organization/
├── ScosOrganization_Employee.yml: modificação (schema refs, server URL)
├── ScosOrganization_Login.yml: modificação (operationId, params inválidos, server URL)
└── ScosOrganization_Department-Position.yml: modificação (server URL)
```

### Fluxo Principal

```
Spec YAML com erros → Correção manual linha a linha → Spec YAML válida
```

### Decisões Técnicas

| Decisão | Escolha | Alternativa Descartada | Motivo |
|---------|---------|------------------------|--------|
| `GET /login/info` — remoção de params inválidos | Remover `idEmployee` e `idRequest` dos params | Adicionar `{employeeId}` e `{id}` na URI | Endpoint busca dados do token JWT autenticado, não por path param; sem implementação Java existente, remoção é segura |
| `UpdateEmployeeDTO` — renomear | `UpdateEmployeeRequest` | Manter `UpdateEmployeeDTO` | Padrão do projeto para DTOs de entrada é `*Request` (`UpdateCompanyRequest`, `UpdateDepartmentRequest`, `UpdatePositionRequest`); `*DTO` é usado para objetos de resposta |
| Server URL | `/organization/api` (com `/`) | `organization/api` (sem `/`) | Padrão OpenAPI 3.x requer path absoluto ou relativo bem-formado; Company já usa com `/` |

### Banco de Dados

- **Impacto**: ❌ Não

---

## 4️⃣ Implementação

### Arquivos

**Modificados**:
- `etc/api/organization/ScosOrganization_Employee.yml` — renomear `UpdateEmployeeDTO` → `UpdateEmployeeRequest`; corrigir schema refs nas linhas 117, 294 e 425; corrigir server URL
- `etc/api/organization/ScosOrganization_Login.yml` — corrigir operationId/summary/description no POST login; remover params inválidos em GET /login/info; remover param inválido em DELETE /profile/{id}; corrigir server URL
- `etc/api/organization/ScosOrganization_Department-Position.yml` — corrigir server URL

### Tarefas

- [ ] **T-01** *(E-04)*: Renomear schema `UpdateEmployeeDTO` → `UpdateEmployeeRequest` em `Employee.yml` (definição + todas as refs)
- [ ] **T-02** *(E-01)*: Corrigir `updateEmployee` requestBody: `UpdateEmployeeAddressRequest` → `UpdateEmployeeRequest` (`Employee.yml:117`)
- [ ] **T-03** *(E-02)*: Corrigir `updateEmployeeContact` requestBody: `UpdateEmployeeContactDTO` → `UpdateEmployeeContactRequest` (`Employee.yml:294`)
- [ ] **T-04** *(E-03)*: Corrigir `updateEmployeeAddress` requestBody: `UpdateEmployeeContactRequest` → `UpdateEmployeeAddressRequest` (`Employee.yml:425`)
- [ ] **T-05** *(E-05)*: Corrigir `summary`, `description`, `operationId` do `POST /login` para `createEmployeeLogin` (`Login.yml:50-56`)
- [ ] **T-06** *(E-06)*: Remover referências a params `idEmployee` e `idRequest` de `GET /employee/login/info` (`Login.yml:247-248`)
- [ ] **T-07** *(E-07)*: Remover referência a param `idEmployee` de `DELETE /profile/{id}` (`Login.yml:398`)
- [ ] **T-08** *(E-08, E-09, E-10)*: Unificar `servers.url` para `/organization/api` em `Employee.yml`, `Department-Position.yml` e `Login.yml`

### Riscos e Edge Cases

1. ~~`GET /login/info` sem path params pressupõe que o endpoint extrai o employeeId do JWT — confirmar que a implementação Java já faz isso antes de remover os params~~ **Resolvido**: Não existe controller Java para Employee/Login ainda. Remoção dos params inválidos é segura.
2. ~~Schema `UpdateEmployeeDTO` existe no Employee.yml mas não é referenciado; confirmar que tem os campos corretos antes de trocar o ref~~ **Resolvido**: `UpdateEmployeeDTO` (linhas 604–653) contém os campos corretos para atualização de employee (`name`, `nameTreatment`, `taxIdentifier`, `email`, `birthDate`, `dateOfHiring`, `observation`, `supervisor_id`, `company_id`, `position_id`). Apenas o nome está errado.

---


