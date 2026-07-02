## Context

`PositionWorkSchedule`/`EmployeeWorkSchedule` são duas faces da mesma estrutura de horário (dia da semana + 4 horários), uma template por cargo e outra efetiva por funcionário, copiada da primeira via código (fora do escopo do contrato). Ambas as tabelas do schema v2 (`SCOS_POSITION_WORK_SCHEDULE`/`SCOS_EMPLOYEE_WORK_SCHEDULE`) têm UK composta `(FK, DAY_OF_WEEK)` e `CHECK` fechado em `DAY_OF_WEEK` (`chk_position_work_schedule_day`/`chk_employee_work_schedule_day` em `scos-organization-boot/src/main/resources/db/changelog/checks/checks.yml`: `MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY`).

## Goals / Non-Goals

**Goals:**
- CRUD completo de horário por dia da semana no template do cargo e na jornada efetiva do funcionário
- Enum `DayOfWeek` compartilhado entre os dois arquivos YAML consumidores, sem duplicação de schema

**Non-Goals:**
- Endpoint público de cópia template→efetivo (permanece código/use case interno)
- Histórico de jornada anterior (tabela não suporta hoje — ideia separada se necessário no futuro)
- Qualquer alteração de schema de banco (v2 já completo)
- Implementação de código (permissões, use case, domain, delegates, testes) — fora de escopo desta change; entra em change futura separada

## Decisions

### Local canônico do enum `DayOfWeek`
**Escolha**: `ScosComponents.yml`, como schema nomeado `DayOfWeek` (`type: string`, `enum: [MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY]`), referenciado via `$ref: './ScosComponents.yml#/components/schemas/DayOfWeek'` tanto em `ScosOrganization_Department-Position.yml` (schema `PositionWorkSchedule.dayOfWeek` e path param `{dayOfWeek}`) quanto em `ScosOrganization_Employee.yml` (schema `EmployeeWorkSchedule.dayOfWeek` e path param `{dayOfWeek}`).

**Motivo**: mesmo precedente já usado pelo enum `Direction` neste arquivo — schema central `type: string`/`enum:` em `ScosComponents.yml`, propriedades de outros schemas apontam pra ele via `$ref` (ver `ScosPaginated.direction` → `$ref: '#/components/schemas/Direction'`). Evita declarar o mesmo enum duas vezes (uma por arquivo consumidor), que divergiria se um dos dois for atualizado sem o outro.

**Alternativa descartada**: declarar `DayOfWeek` inline em cada arquivo (como o parâmetro de path `month` em `ScosComponents.yml` faz) — descartada porque `month` só é usado num único endpoint, sem reuso; `DayOfWeek` é usado em 2 arquivos × 2 lugares cada (schema de corpo + parâmetro de path), reuso genuíno que pede schema nomeado central.

### Chave do sub-recurso é `dayOfWeek`, não `id`
**Escolha**: rota `PUT/DELETE .../work-schedule/{dayOfWeek}` usa o próprio enum como chave.
**Motivo**: UK composta `(positionId/employeeId, dayOfWeek)` já garante unicidade — `dayOfWeek` é uma chave de negócio natural, sem necessidade de `id` sequencial.
**Alternativa descartada**: `id` sequencial da tabela como chave de rota — rejeitada, adicionaria um lookup indireto sem necessidade.

### `GET .../work-schedule` sem paginação
**Escolha**: retorna array direto `WorkScheduleOutput[]`, sem `paginationFilter`/`paginatedDTO`.
**Motivo**: teto real de 7 registros (um por dia da semana, garantido pela UK composta) torna paginação sem propósito — decidido com o usuário. Esta é a única exceção deliberada ao padrão de paginação em todas as 7 mudanças de contrato desta rodada; documentar isso na `description` do endpoint para não parecer descuido de quem for revisar o contrato depois.
**Alternativa descartada**: manter `paginationFilter`/`paginatedDTO` por consistência cega com o resto do contrato — rejeitada porque paginar uma lista com teto de 7 é ruído sem benefício real.

## Risks / Trade-offs

- [Risco] `EmployeeWorkSchedule` não tem histórico — um `UPDATE` sobrescreve o horário anterior sem rastro → [Mitigação] aceito nesta rodada; se auditoria de jornada anterior virar necessidade, é uma ideia nova (a tabela não suporta hoje, exigiria migration)
- [Risco] Dia sem linha cadastrada pode ser mal-interpretado como "segue o cargo" (fallback automático) quando na verdade significa "não definido" → [Mitigação] documentar explicitamente essa semântica na `description` de cada endpoint de `GET`/`POST`
- [Risco] `startTime < lunchStart < lunchEnd < endTime` não é validável só pelo contrato OpenAPI → [Mitigação] regra de use case, documentada no schema via `description`; banco também não tem `CHECK` pra essa ordenação (confirmado em `checks.yml`), então a validação é responsabilidade exclusiva da camada de aplicação
