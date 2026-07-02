## 1. Contrato OpenAPI — ScosOrganization_Department-Position.yml

- [x] 1.1 Adicionar `isTrustPosition: boolean` (default `false`) a `Position`, `CreatePositionRequest`, `UpdatePositionRequest`
- [x] 1.2 Criar schemas `ReasonPositionChange`, `CreateReasonPositionChangeRequest`, `UpdateReasonPositionChangeRequest`, `GetReasonPositionChangeResponse`, `GetAllReasonPositionChangeResponse`
- [x] 1.3 Criar paths `GET/POST /v1/reason-position-change`, `GET/PUT /v1/reason-position-change/{id}`, `PUT /v1/reason-position-change/{id}/enable`, `PUT /v1/reason-position-change/{id}/disable`, todos com `x-authorize`

## 2. Contrato OpenAPI — ScosOrganization_Employee.yml

- [x] 2.1 Criar enum `EmployeeContractType` (`CLT`, `PJ`, `ESTAGIO`, `TEMPORARIO`)
- [x] 2.2 Adicionar `contractType` (obrigatório) e `probationEndDate` (opcional) a `Employee`, `CreateEmployeeRequest`, `UpdateEmployeeRequest`
- [x] 2.3 Adicionar `reasonPositionChangeId` (obrigatório) a `TransferEmployeeRequest`
- [x] 2.4 Criar schema de resposta paginada de `GET /v1/employees/{id}/position-history` (`positionId`, `startDate`, `endDate`, `reasonPositionChangeId`, `createdAt`, `userAt`)
- [x] 2.5 Criar path `GET /v1/employees/{id}/position-history` com `x-authorize: [GET_EMPLOYEE_POSITION_HISTORY]` e `paginationFilter`
