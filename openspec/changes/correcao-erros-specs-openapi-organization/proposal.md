## Why

As specs OpenAPI do módulo Organization contêm 10 erros de copy-paste (referências de schema incorretas, `operationId` errado, parâmetros de path inválidos e `servers.url` sem barra inicial) que impedem geração correta de código via openapi-generator e causam divergência entre documentação e contrato real da API.

## What Changes

- Renomear schema `UpdateEmployeeDTO` → `UpdateEmployeeRequest` para alinhar com padrão `*Request` do projeto para DTOs de entrada
- Corrigir `updateEmployee` requestBody: `UpdateEmployeeAddressRequest` → `UpdateEmployeeRequest`
- Corrigir `updateEmployeeContact` requestBody: `UpdateEmployeeContactDTO` → `UpdateEmployeeContactRequest` (schema inexistente)
- Corrigir `updateEmployeeAddress` requestBody: `UpdateEmployeeContactRequest` → `UpdateEmployeeAddressRequest`
- Corrigir `POST /v1/employee/{employeeId}/login`: `summary`, `description` e `operationId` apontam para "Update employee contact" — corrigir para `createEmployeeLogin`
- Remover params `idEmployee` e `idRequest` de `GET /v1/employee/login/info` (path params sem `{var}` correspondente na URI)
- Remover param `idEmployee` de `DELETE /v1/profile/{id}` (path param sem `{employeeId}` na URI)
- Unificar `servers.url` para `/organization/api` em `Employee.yml`, `Department-Position.yml` e `Login.yml`

Nenhuma mudança breaking no contrato HTTP — apenas correções de documentação e nomenclatura interna.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `schema-organization`: Contratos OpenAPI do módulo Organization corrigidos — referências de schema, operationId, parâmetros e server URL alinhados com padrão do projeto

## Impact

- `etc/api/organization/ScosOrganization_Employee.yml` — 5 correções (schema rename + 3 refs + server URL)
- `etc/api/organization/ScosOrganization_Login.yml` — 4 correções (operationId/summary/desc + 2 params inválidos + server URL)
- `etc/api/organization/ScosOrganization_Department-Position.yml` — 1 correção (server URL)
- Código Java gerado (target/generated-sources): será regenerado sem impacto comportamental
