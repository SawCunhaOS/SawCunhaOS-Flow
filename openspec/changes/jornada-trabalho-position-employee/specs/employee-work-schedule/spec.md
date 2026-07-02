## ADDED Requirements

### Requirement: CRUD de jornada de trabalho efetiva por funcionário
O sistema SHALL prover CRUD de `EmployeeWorkSchedule` por dia da semana, sob `/v1/employees/{employeeId}/work-schedule`, com `dayOfWeek` como chave do sub-recurso (não `id` sequencial), refletindo a UK composta `(EMPLOYEE_ID, DAY_OF_WEEK)` de `SCOS_EMPLOYEE_WORK_SCHEDULE`. A cópia inicial do template do cargo para a jornada efetiva do funcionário SHALL permanecer interna (código/use case), sem endpoint público de cópia.

#### Scenario: Criar horário de um dia
- **WHEN** um cliente autorizado envia `POST /v1/employees/{employeeId}/work-schedule` com `dayOfWeek`, `startTime`, `lunchStart`, `lunchEnd`, `endTime` válidos e ainda não existe registro para esse `(employeeId, dayOfWeek)`
- **THEN** o sistema cria o registro e retorna `201 Created`

#### Scenario: Criar horário para dia já cadastrado é rejeitado
- **WHEN** um cliente autorizado envia `POST /v1/employees/{employeeId}/work-schedule` com `dayOfWeek` já cadastrado para esse funcionário
- **THEN** o sistema rejeita e retorna `4XX`

#### Scenario: Atualizar horário de um dia
- **WHEN** um cliente autorizado envia `PUT /v1/employees/{employeeId}/work-schedule/{dayOfWeek}` com novos horários
- **THEN** o sistema atualiza o registro e retorna `204 No Content`

#### Scenario: Remover horário de um dia
- **WHEN** um cliente autorizado envia `DELETE /v1/employees/{employeeId}/work-schedule/{dayOfWeek}` para um dia cadastrado
- **THEN** o sistema remove o registro e retorna `204 No Content`

### Requirement: Listagem de jornada sem paginação
`GET /v1/employees/{employeeId}/work-schedule` SHALL retornar um array direto (`WorkScheduleOutput[]`), sem `paginationFilter` nem envelope `paginatedDTO`, dado que a UK composta `(EMPLOYEE_ID, DAY_OF_WEEK)` garante um teto real de 7 registros por funcionário.

#### Scenario: Listar jornada do funcionário
- **WHEN** um cliente autorizado envia `GET /v1/employees/{employeeId}/work-schedule`
- **THEN** o sistema retorna `200 OK` com um array de até 7 objetos `WorkScheduleOutput`, sem envelope de paginação

#### Scenario: Dia sem linha cadastrada não é preenchido automaticamente
- **WHEN** um funcionário não tem registro de `EmployeeWorkSchedule` para um `dayOfWeek` específico
- **THEN** esse dia simplesmente não aparece na resposta de `GET .../work-schedule` — o sistema não infere o horário do template do cargo como fallback

### Requirement: dayOfWeek restrito ao vocabulário fechado
O campo `dayOfWeek` SHALL aceitar exclusivamente os valores `MONDAY`, `TUESDAY`, `WEDNESDAY`, `THURSDAY`, `FRIDAY`, `SATURDAY`, `SUNDAY`, refletindo o `CHECK` `chk_employee_work_schedule_day` do banco, e SHALL usar o mesmo schema de enum `DayOfWeek` (`ScosComponents.yml`) usado por `position-work-schedule`.

#### Scenario: dayOfWeek inválido é rejeitado
- **WHEN** um cliente autorizado envia `POST /v1/employees/{employeeId}/work-schedule` com `dayOfWeek` fora do vocabulário fechado
- **THEN** o sistema rejeita e retorna `4XX`

### Requirement: Autorização do CRUD de EmployeeWorkSchedule
Cada operação SHALL exigir a permissão correspondente em `ScosOrganizationPermission`: `GET_EMPLOYEE_WORK_SCHEDULE`, `CREATE_EMPLOYEE_WORK_SCHEDULE`, `UPDATE_EMPLOYEE_WORK_SCHEDULE`, `DELETE_EMPLOYEE_WORK_SCHEDULE`.

#### Scenario: Requisição sem permissão é rejeitada
- **WHEN** um cliente sem a permissão `CREATE_EMPLOYEE_WORK_SCHEDULE` envia `POST /v1/employees/{employeeId}/work-schedule`
- **THEN** o sistema retorna `4XX` de autorização
