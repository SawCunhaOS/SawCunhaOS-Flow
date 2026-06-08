## 1. Verificação de Pré-condições

- [ ] 1.1 Verificar se o valor `ENABLE` está gravado no banco: `SELECT DISTINCT STATUS FROM scos.SCOS_LOGIN WHERE STATUS = 'ENABLE'` — resultado determina se migration Liquibase é necessária (tarefa 5.9)
- [x] 1.2 Verificar se as entidades Java `Department` e `Position` possuem campo `active` — se ausentes, checar dependência com a change de domain model antes de implementar tarefas 3.2/3.3

## 2. Renomeação de URIs nas Specs OpenAPI

- [x] 2.1 Atualizar `ScosOrganization_Company.yml`: renomear `/v1/company` → `/v1/companies`, `/company/{companyId}/contact` → `/companies/{companyId}/contacts`, `/company/{companyId}/address` → `/companies/{companyId}/addresses` em todos os paths
- [x] 2.2 Atualizar `ScosOrganization_Employee.yml`: renomear `/v1/employee` → `/v1/employees`, `/employee/{employeeId}/contact` → `/employees/{employeeId}/contacts`, `/employee/{employeeId}/address` → `/employees/{employeeId}/addresses`
- [x] 2.3 Atualizar `ScosOrganization_Login.yml`: renomear `/v1/employee/{employeeId}/login` → `/v1/employees/{employeeId}/logins` em todos os paths; renomear `/v1/profile` → `/v1/profiles`
- [x] 2.4 Substituir `GET /v1/employee/login/info` por `GET /v1/logins/me` na spec — novo path no nível raiz, sem parâmetros de path

## 3. Conformidade REST Level 2 e RFC 9110 nas Specs OpenAPI

- [x] 3.1 Remover operação `DELETE /v1/profiles/{id}/features` com `requestBody` de `ScosOrganization_Login.yml` — manter apenas o `PUT` existente para substituição completa da lista
- [x] 3.2 Adicionar `PUT /v1/departments/{id}/enable` e `PUT /v1/departments/{id}/disable` em `ScosOrganization_Department-Position.yml` com resposta `204 No Content` e `x-authorize: [ENABLE_DEPARTMENT]` / `[DISABLE_DEPARTMENT]`
- [x] 3.3 Adicionar `PUT /v1/positions/{id}/enable` e `PUT /v1/positions/{id}/disable` em `ScosOrganization_Department-Position.yml` com resposta `204 No Content` e `x-authorize: [ENABLE_POSITION]` / `[DISABLE_POSITION]`

## 4. Padronização de Schemas nas Specs OpenAPI

- [x] 4.1 Renomear schemas de Profile em `ScosOrganization_Login.yml`: `CreateProfileDTO` → `CreateProfileRequest`, `UpdateProfileDTO` → `UpdateProfileRequest`, `GetProfileDTO` → `GetProfileResponse`, `GetAllProfilesDTO` → `GetAllProfilesResponse`
- [x] 4.2 Renomear schemas de Login em `ScosOrganization_Login.yml`: `CreatedEmployeeLoginDTO` → `CreateEmployeeLoginRequest`, `UpdateEmployeeLoginDTO` → `UpdateEmployeeLoginRequest`, `GetEmployeeLoginDTO` → `GetEmployeeLoginResponse`, `GetAllEmployeeLoginDTO` → `GetAllEmployeeLoginsResponse`
- [x] 4.3 Corrigir refs quebradas em `ScosOrganization_Login.yml`: `createEmployeeLogin` referenciava `CreateEmployeeLoginRequest` mas a definição era `CreatedEmployeeLoginDTO` — garantir que ref e definição coincidem após 4.2
- [x] 4.4 Corrigir `LoginStatus.ENABLE` → `ENABLED` no enum `LoginStatus` de `ScosOrganization_Login.yml`

## 5. Implementação Java

- [x] 5.1 Atualizar `@RequestMapping` no `CompanyController`: URL routing é 100% gerado do YAML — nenhuma mudança Java necessária; spec atualizada na tarefa 2.1
- [ ] 5.2 Atualizar `@RequestMapping` no `EmployeeController`: Employee spec não está no openapi-generator — escopo futuro (adicionar execução ao pom.xml)
- [ ] 5.3 Atualizar `@RequestMapping` no `LoginController`/`ProfileController`: Login spec não está no openapi-generator — escopo futuro
- [ ] 5.4 Remover método `deleteFeaturesInProfile` do controller — Login spec não está no generator; delegate não existe ainda — escopo futuro
- [ ] 5.5 Criar endpoint `GET /v1/logins/me` no `LoginController` — Login spec não está no generator — escopo futuro
- [x] 5.6 Adicionar métodos `enable`/`disable` no `DepartmentController` e criar `ActivateDepartmentUseCase`/`InactivateDepartmentUseCase`
- [x] 5.7 Adicionar métodos `enable`/`disable` no `PositionController` e criar `ActivatePositionUseCase`/`InactivatePositionUseCase`
- [ ] 5.8 Corrigir enum Java `LoginStatus`: `ENABLE` → `ENABLED` — Login spec não está no generator; enum Java não existe ainda — escopo futuro
- [ ] 5.9 Se tarefa 1.1 encontrou dados com `STATUS='ENABLE'` no banco: criar migration Liquibase `UPDATE scos.SCOS_LOGIN SET STATUS='ENABLED' WHERE STATUS='ENABLE'`

## 6. Validação

- [x] 6.1 Verificar que nenhuma operação DELETE define `requestBody` nas 4 specs OpenAPI — PASS (parse YAML confirma 0 DELETE com requestBody)
- [x] 6.2 Verificar que URIs singulares `/v1/company`, `/v1/employee`, `/v1/profile` não aparecem como paths — PASS (0 matches)
- [x] 6.3 Verificar que schemas renomeados em 4.1/4.2 usam sufixo `*Request`/`*Response` e nomes antigos `*DTO` foram removidos — PASS; DTOs restantes (`FeaturesDTO`, `ProfileDTO`, `LoginsDTO`, etc.) são schemas internos fora do escopo desta change
- [x] 6.4 Verificar que `LoginStatus` contém `ENABLED` e não contém `ENABLE` — PASS (linha 689: `- ENABLED`)
- [ ] 6.5 Verificar que `GET /v1/logins/me` retorna os dados do usuário autenticado quando chamado com JWT válido
- [ ] 6.6 Verificar que `PUT /v1/departments/{id}/enable` e `PUT /v1/departments/{id}/disable` retornam `204 No Content`
- [ ] 6.7 Verificar que `PUT /v1/positions/{id}/enable` e `PUT /v1/positions/{id}/disable` retornam `204 No Content`
