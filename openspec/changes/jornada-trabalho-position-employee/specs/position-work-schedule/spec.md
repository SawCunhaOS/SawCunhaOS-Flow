## ADDED Requirements

### Requirement: CRUD de jornada de trabalho template por cargo
O sistema SHALL prover CRUD de `PositionWorkSchedule` por dia da semana, sob `/v1/positions/{positionId}/work-schedule`, com `dayOfWeek` como chave do sub-recurso (não `id` sequencial), refletindo a UK composta `(POSITION_ID, DAY_OF_WEEK)` de `SCOS_POSITION_WORK_SCHEDULE`.

#### Scenario: Criar horário de um dia
- **WHEN** um cliente autorizado envia `POST /v1/positions/{positionId}/work-schedule` com `dayOfWeek`, `startTime`, `lunchStart`, `lunchEnd`, `endTime` válidos e ainda não existe registro para esse `(positionId, dayOfWeek)`
- **THEN** o sistema cria o registro e retorna `201 Created`

#### Scenario: Criar horário para dia já cadastrado é rejeitado
- **WHEN** um cliente autorizado envia `POST /v1/positions/{positionId}/work-schedule` com `dayOfWeek` já cadastrado para esse cargo
- **THEN** o sistema rejeita e retorna `4XX`

#### Scenario: Atualizar horário de um dia
- **WHEN** um cliente autorizado envia `PUT /v1/positions/{positionId}/work-schedule/{dayOfWeek}` com novos horários
- **THEN** o sistema atualiza o registro e retorna `204 No Content`

#### Scenario: Remover horário de um dia
- **WHEN** um cliente autorizado envia `DELETE /v1/positions/{positionId}/work-schedule/{dayOfWeek}` para um dia cadastrado
- **THEN** o sistema remove o registro e retorna `204 No Content`

### Requirement: Listagem de jornada sem paginação
`GET /v1/positions/{positionId}/work-schedule` SHALL retornar um array direto (`WorkScheduleOutput[]`), sem `paginationFilter` nem envelope `paginatedDTO`, dado que a UK composta `(POSITION_ID, DAY_OF_WEEK)` garante um teto real de 7 registros por cargo.

#### Scenario: Listar jornada do cargo
- **WHEN** um cliente autorizado envia `GET /v1/positions/{positionId}/work-schedule`
- **THEN** o sistema retorna `200 OK` com um array de até 7 objetos `WorkScheduleOutput`, sem envelope de paginação

### Requirement: dayOfWeek restrito ao vocabulário fechado
O campo `dayOfWeek` SHALL aceitar exclusivamente os valores `MONDAY`, `TUESDAY`, `WEDNESDAY`, `THURSDAY`, `FRIDAY`, `SATURDAY`, `SUNDAY`, refletindo o `CHECK` `chk_position_work_schedule_day` do banco.

#### Scenario: dayOfWeek inválido é rejeitado
- **WHEN** um cliente autorizado envia `POST /v1/positions/{positionId}/work-schedule` com `dayOfWeek` fora do vocabulário fechado
- **THEN** o sistema rejeita e retorna `4XX`

### Requirement: Autorização do CRUD de PositionWorkSchedule
Cada operação SHALL exigir a permissão correspondente em `ScosOrganizationPermission`: `GET_POSITION_WORK_SCHEDULE`, `CREATE_POSITION_WORK_SCHEDULE`, `UPDATE_POSITION_WORK_SCHEDULE`, `DELETE_POSITION_WORK_SCHEDULE`.

#### Scenario: Requisição sem permissão é rejeitada
- **WHEN** um cliente sem a permissão `CREATE_POSITION_WORK_SCHEDULE` envia `POST /v1/positions/{positionId}/work-schedule`
- **THEN** o sistema retorna `4XX` de autorização
