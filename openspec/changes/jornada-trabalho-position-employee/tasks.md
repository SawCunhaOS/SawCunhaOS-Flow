## 1. Contrato OpenAPI — enum compartilhado

- [x] 1.1 Adicionar schema `DayOfWeek` (`MONDAY`..`SUNDAY`) em `etc/api/organization/ScosComponents.yml`, seguindo o padrão do enum `Direction` já existente

## 2. Contrato OpenAPI — PositionWorkSchedule

- [x] 2.1 Adicionar schemas `PositionWorkSchedule`, `CreatePositionWorkScheduleRequest`, `UpdatePositionWorkScheduleRequest`, `GetAllPositionWorkScheduleResponse` (array direto, sem `paginatedDTO`) em `etc/api/organization/ScosOrganization_Department-Position.yml`
- [x] 2.2 Adicionar paths `GET/POST /v1/positions/{positionId}/work-schedule`, `PUT/DELETE /v1/positions/{positionId}/work-schedule/{dayOfWeek}` com `x-authorize` (`GET/CREATE/UPDATE/DELETE_POSITION_WORK_SCHEDULE`)

## 3. Contrato OpenAPI — EmployeeWorkSchedule

- [x] 3.1 Adicionar schemas `EmployeeWorkSchedule`, `CreateEmployeeWorkScheduleRequest`, `UpdateEmployeeWorkScheduleRequest`, `GetAllEmployeeWorkScheduleResponse` (array direto, sem `paginatedDTO`) em `etc/api/organization/ScosOrganization_Employee.yml`
- [x] 3.2 Adicionar paths `GET/POST /v1/employees/{employeeId}/work-schedule`, `PUT/DELETE /v1/employees/{employeeId}/work-schedule/{dayOfWeek}` com `x-authorize` (`GET/CREATE/UPDATE/DELETE_EMPLOYEE_WORK_SCHEDULE`)
