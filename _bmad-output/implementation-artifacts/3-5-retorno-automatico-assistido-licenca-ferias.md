---
baseline_commit: b72b2dcc0f63997fe313658e358e68dc506dd645
---

# Story 3.5: Retorno Automático Assistido de Licença/Férias

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como sistema,
Eu quero abrir sozinho a solicitação de reativação de Login na data prevista de retorno de uma licença/férias,
Para que RH não precise lembrar de reativar manualmente.

## Acceptance Criteria

1. **Given** a data prevista de retorno (`EmployeeStatusHistory.expectedReturnDate`, Story 2.4) chega **When** o job agendado roda **Then** abre uma `LoginApprovalRequest` (`requestType=REACTIVATE_LOGIN`, `escalationPolicy=AUTO_CANCEL`, `requestedByLogin=NULL` — obrigatório pelo `CHECK chk_login_approval_request_requester` do banco) **And** ainda exige aprovação humana, na mesma cadeia de 3 níveis (Story 3.2/3.3), com o mesmo escalonamento por SLA de 1 dia útil por nível **And** o Login **não** transita para `ACTIVE` sozinho — só quando alguém aprova.
2. **Given** ninguém decide em 5 dias úteis corridos desde a **abertura** da solicitação (não desde o último escalonamento) **When** o prazo estoura **Then** a solicitação é cancelada automaticamente (`status=CANCELLED`) **And** RH precisa reabrir manualmente quando a pessoa de fato retornar (reabrir = acionar `enable`/`unblock` de novo, fluxo já existente da Story 3.3 — nenhuma rota nova para "reabrir").
3. **Given** o Funcionário foi desligado por outro motivo antes da data prevista (uma transição de status mais recente já substituiu a de licença) **When** a data chegaria **Then** o gatilho automático não dispara — só a linha de `EmployeeStatusHistory` **mais recente** do Funcionário com `expectedReturnDate` preenchida conta.
4. **Given** um Funcionário já teve uma `LoginApprovalRequest` de retorno automático aberta para esta mesma licença (aprovada, rejeitada ou cancelada) **When** o job roda de novo (dias seguintes, antes de RH reabrir manualmente) **Then** não abre uma segunda solicitação para o mesmo episódio de licença — o gatilho é "uma vez por retorno previsto", não "todo dia até alguém decidir".

## Tasks / Subtasks

- [ ] Task 1: `EmployeeReturnFromLeaveJob` — abre a solicitação na data prevista (AC: 1, 3, 4)
  - [ ] Criar `domain/access/status/service/EmployeeReturnFromLeaveJob.java` (`@Component`, `@Scheduled` — mesma frequência do job de escalonamento da Story 3.2, ou diária de manhã cedo; **decisão de design a confirmar com o usuário**, o épico só fala em "a data chega", não a frequência de checagem. Esta story assume 1x/dia, já que `expectedReturnDate` é `LocalDate`, sem granularidade de hora).
  - [ ] Query: `EmployeeStatusHistoryRepository` (já existe) precisa de um método novo — `findAllByExpectedReturnDateLessThanEqualAndIsLatestForEmployee(LocalDate today)` **ou**, mais simples de implementar corretamente: buscar todas as linhas com `expectedReturnDate <= today` e, para cada uma, confirmar que é a **última** `EmployeeStatusHistory` daquele Funcionário (`ORDER BY createdAt DESC LIMIT 1` = a própria linha) antes de agir — evita depender de uma query complexa, ao custo de checar 1 a 1. **Decisão de design a confirmar com o usuário:** se o volume justificar, isso vira uma query SQL dedicada (`DISTINCT ON` por `employeeId` ordenado por `createdAt DESC`) em vez de N consultas — esta story assume a versão simples primeiro (menos código, funciona, mas não é a mais eficiente em escala).
  - [ ] Para cada linha qualificada (é a mais recente do Funcionário **e** `expectedReturnDate <= hoje`): resolve o(s) `Login`(s) do Funcionário (`Employee.login`, `Set<Login>` — **atenção**: um Funcionário pode ter mais de 1 Login, campo já mapeado assim em `Employee.java`; decisão de design a confirmar — abrir 1 `LoginApprovalRequest` por Login `INACTIVE`/`BLOCKED` do Funcionário, ignorando os que já estão `ACTIVE`/`PENDING_APPROVAL`/`REJECTED`); para cada Login elegível, checa se **já existe** qualquer `LoginApprovalRequest` (`requestType=REACTIVATE_LOGIN`, `escalationPolicy=AUTO_CANCEL`) criada **depois** de `employeeStatusHistory.createdAt` para aquele Login (AC 4 — evita reabrir todo dia); se não existir, cria uma nova via o mesmo `LoginApprovalChainResolver.firstLevelFor` da Story 3.2, com `requestedByLogin=null`.
  - [ ] Adicionar `LoginApprovalRequestRepository.existsByLoginIdAndRequestTypeAndEscalationPolicyAndCreatedAtAfter(Long loginId, LoginApprovalRequestType type, LoginApprovalRequestEscalationPolicy policy, Instant after)` (`default`, predicate simples).
  - [ ] Criar `EmployeeReturnFromLeaveJobTest.java`, `Clock.fixed`, cobrindo: retorno na data → abre solicitação; Funcionário com transição mais recente que não é a de licença → não abre; solicitação já aberta para o mesmo episódio → não abre de novo; Login já `ACTIVE` (RH já reativou manualmente antes do job rodar) → não abre.

- [ ] Task 2: Auto-cancelamento em 5 dias úteis — estende o job de escalonamento da Story 3.2 (AC: 2)
  - [ ] Adicionar `LoginApprovalRequest.cancel(Instant now)` (mesmo padrão de `approve`/`reject`): `status=CANCELLED`, `decidedAt=now`, sem `decidedByLogin` (ninguém decidiu — foi o sistema). Lança `ScosException` se `status != PENDING`.
  - [ ] Em `LoginApprovalEscalationJob` (Story 3.2 Task 9), antes de escalar cada solicitação `PENDING` vencida, checar: `if (request.getEscalationPolicy() == AUTO_CANCEL && now.isAfter(businessDayCalculator.plusBusinessDays(request.getCreatedAt(), 5, zone))) { request.cancel(now); continue; }` — cancelamento tem prioridade sobre escalonamento (se os 5 dias úteis totais já passaram, não importa se ainda "teria" próximo nível pra escalar). Para `escalationPolicy == INDEFINITE`, esse bloco nunca dispara (sem limite total).
  - [ ] **Não** criar um job separado só para isso — reaproveitar o mesmo `@Scheduled` da Story 3.2 evita duas varreduras da mesma tabela na mesma janela de tempo.
  - [ ] Estender `LoginApprovalEscalationJobTest` (Story 3.2): solicitação `AUTO_CANCEL` com 5 dias úteis vencidos → `CANCELLED`, não escala; solicitação `AUTO_CANCEL` dentro dos 5 dias mas com SLA de nível vencido → escala normalmente (mesmo comportamento de `INDEFINITE`); solicitação `INDEFINITE` nunca cancela sozinha, não importa o tempo.

- [ ] Task 3: Guarda de escopo (AC: 2, 4)
  - [ ] **Não** implementar "reabertura automática" — quando `RH` reabre manualmente (via `enable`/`unblock`, Story 3.3), é um novo request comum (`requestedByLogin` preenchido, `escalationPolicy=INDEFINITE`) — **não** reaproveita `AUTO_CANCEL`. A reabertura manual sai do "modo automático" definitivamente para aquele episódio.
  - [ ] **Não** criar nenhuma rota nova — nem para "forçar o gatilho", nem para "cancelar manualmente antes dos 5 dias". Só o job cancela por tempo; RH sempre pode aprovar/rejeitar a solicitação `PENDING` normalmente, pelos endpoints já existentes (Story 3.2), antes do prazo estourar.
  - [ ] **Não** mexer em `EmployeeStatusHistory`/`expectedReturnDate` (Story 2.4) — só leitura.

## Dev Notes

### Contexto crítico — leia antes de implementar

**`AUTO_CANCEL` só existe por causa desta story — mas o `CHECK` do banco já a previa desde 2026-07-18.** `chk_login_approval_request_requester` (`ESCALATION_POLICY='AUTO_CANCEL' ⟹ REQUESTED_BY_LOGIN_ID IS NULL`) confirma que a spine já sabia, antes do detalhamento das stories, que solicitações abertas pelo sistema (sem um humano "requisitante") existiriam. Esta story é a primeira (e única, no Epic 3) a produzir `escalationPolicy=AUTO_CANCEL`.

**"5 dias úteis" é do momento da CRIAÇÃO da solicitação, não do último escalonamento.** Fácil de confundir com o SLA por nível (1 dia útil, reiniciado a cada escalonamento). São dois relógios diferentes rodando ao mesmo tempo na mesma `LoginApprovalRequest`: o SLA por nível (`slaDeadline`, sobrescrito a cada escalonamento, controla QUANDO escalar) e o teto de 5 dias úteis (calculado a partir de `createdAt`, que nunca muda, controla QUANDO desistir de vez). `AUTO_CANCEL` tem os dois; `INDEFINITE` só tem o primeiro.

**"Mesma licença"/"mesmo episódio" (AC 4) é resolvido comparando datas, não por uma coluna de rastreio nova.** Não existe (e esta story não cria) uma FK de `LoginApprovalRequest` para `EmployeeStatusHistory`. A checagem "já existe solicitação para este episódio" compara `LoginApprovalRequest.createdAt > employeeStatusHistory.createdAt` — funciona porque qualquer solicitação de retorno automático só pode ter sido criada **depois** do evento de licença que a originou. Se um novo episódio de licença acontecer depois (`disable` de novo, novo `expectedReturnDate`), a comparação naturalmente usa o `createdAt` mais recente da licença mais recente — sem confundir episódios.

**Um Funcionário pode ter mais de 1 Login (`Employee.login` é `Set<Login>`).** Nenhuma story anterior do Epic 3 tratou disso explicitamente (a criação, Story 3.1, e a reativação, Story 3.3, agem sobre 1 Login por vez, identificado por id na URL). Esta story é a primeira a precisar **resolver todos os Logins de um Funcionário** — trate cada um independentemente (um pode já estar `ACTIVE` por outro motivo, outro `BLOCKED` esperando este gatilho).

### Onde cada peça vai (camadas)

- `domain/access/status/service/EmployeeReturnFromLeaveJob.java` (novo).
- `domain/access/login/internal/LoginApprovalRequestRepository.java` (Story 3.2): `+existsByLoginIdAndRequestTypeAndEscalationPolicyAndCreatedAtAfter`.
- `domain/access/login/internal/LoginApprovalRequest.java` (Story 3.2): `+cancel(Instant)`.
- `domain/access/login/service/LoginApprovalEscalationJob.java` (Story 3.2): +branch de auto-cancelamento (Task 2).
- Nenhuma mudança de schema — reaproveita tudo que já existe desde a Story 3.2 (Task 1 dela já incluiu `AUTO_CANCEL` no `CHECK` de `escalation_policy`, herdado direto do banco desde 2026-07-18).

### Testing Standards

- `EmployeeReturnFromLeaveJobTest`/`LoginApprovalEscalationJobTest` (estendido): `Clock.fixed`, determinístico, sem `sleep` — mesma regra de todo o projeto desde a Story 0.2.
- Integração: se o tempo permitir, um teste ponta a ponta (via `boot`) simulando: `disable` com `expectedReturnDate`, avançar o `Clock` de teste (ou ajustar `expectedReturnDate` para o passado no seed), rodar o job manualmente (chamando o método `@Scheduled` direto, não esperando o cron real), confirmar `LoginApprovalRequest` criada.

### Project Structure Notes

- Nenhum pacote novo — `EmployeeReturnFromLeaveJob` fica em `domain/access/status/service/` (mesmo pacote de `EmployeeStatusHistory`, já que é o agregado que ele consulta), não em `domain/access/login/` (que é sobre o efeito, não a origem).

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Epic 3 / Story 3.5] — Given/When/Then originais.
- [Source: organization/flow-organization-domain/.../access/status/internal/EmployeeStatusHistory.java:83] — `expectedReturnDate` (`LocalDate`), criado pela Story 2.4, consumido aqui.
- [Source: organization/flow-organization-resources/.../checks/checks.yml:35-39] — `chk_login_approval_request_requester`, confirma `AUTO_CANCEL⟹requestedByLoginId NULL` já previsto desde 2026-07-18.
- [Source: organization/flow-organization-domain/.../corporate/employee/internal/Employee.java:107] — `Set<Login> login` — um Funcionário pode ter mais de um Login, tratado explicitamente nesta story pela primeira vez no Epic 3.
- [Source: _bmad-output/implementation-artifacts/3-2-cadeia-aprovacao-criacao-login.md] — `LoginApprovalChainResolver`, `BusinessDayCalculator`, `LoginApprovalEscalationJob` reaproveitados/estendidos.
- [Source: _bmad-output/implementation-artifacts/3-3-aprovacao-reativacao-login.md] — reabertura manual (`enable`/`unblock`) é o caminho de volta quando o auto-cancelamento acontece; nenhuma rota nova criada aqui.
- [Source: _bmad-output/implementation-artifacts/2-4-licenca-ferias-colocar-funcionario-licenca.md] — origem de `expectedReturnDate`, contexto de negócio da licença/férias.

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List

## Change Log

| Data | Descrição |
|------|-----------|
| 2026-08-16 | Story criada — Epic 3, job de retorno automático de licença/férias (`AUTO_CANCEL`, reaproveitando toda a cadeia da Story 3.2). Auto-cancelamento em 5 dias úteis dobra no mesmo job de escalonamento em vez de criar um segundo `@Scheduled`. Decisões de design sinalizadas: frequência do job, tratamento de Funcionário com múltiplos Logins, estratégia de query (N consultas vs. `DISTINCT ON`). |
