## 1. ScosOrganization_Employee.yml — Nomenclatura e Referências

- [x] 1.1 Renomear schema `UpdateEmployeeDTO` → `UpdateEmployeeRequest` na seção `components.schemas`
- [x] 1.2 Corrigir `updateEmployee` (PUT `/v1/employee/{id}`) requestBody: `UpdateEmployeeAddressRequest` → `UpdateEmployeeRequest`
- [x] 1.3 Corrigir `updateEmployeeContact` (PUT `/v1/employee/{employeeId}/contact/{id}`) requestBody: `UpdateEmployeeContactDTO` → `UpdateEmployeeContactRequest`
- [x] 1.4 Corrigir `updateEmployeeAddress` (PUT `/v1/employee/{employeeId}/address/{id}`) requestBody: `UpdateEmployeeContactRequest` → `UpdateEmployeeAddressRequest`
- [x] 1.5 Corrigir `servers.url`: `organization/api` → `/organization/api`

## 2. ScosOrganization_Login.yml — Metadata e Parâmetros

- [x] 2.1 Corrigir `POST /v1/employee/{employeeId}/login`: `operationId: updateEmployeeContact` → `createEmployeeLogin`
- [x] 2.2 Corrigir `POST /v1/employee/{employeeId}/login`: `summary` e `description` para `Create employee login`
- [x] 2.3 Remover params `idEmployee` e `idRequest` de `GET /v1/employee/login/info`
- [x] 2.4 Remover param `idEmployee` de `DELETE /v1/profile/{id}`
- [x] 2.5 Corrigir `servers.url`: `organization/api` → `/organization/api`

## 3. ScosOrganization_Department-Position.yml — Server URL

- [x] 3.1 Corrigir `servers.url`: `organization/api` → `/organization/api`

## 4. Validação

- [x] 4.1 Verificar que todas as `$ref` de schema nos 3 arquivos resolvem para schemas existentes (sem referências pendentes) — fix adicional: `InsideFlowPartners_Department-Position.yml` → `ScosOrganization_Department-Position.yml` em `Employee.yml:523`
- [x] 4.2 Verificar que nenhum parâmetro `in: path` referencia variável ausente na URI do endpoint
