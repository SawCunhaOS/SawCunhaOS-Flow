---
baseline_commit: b72b2dcc0f63997fe313658e358e68dc506dd645
---

# Story 3.5: Retorno Automático Assistido de Licença/Férias

Status: review

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

- [x] Task 1: `EmployeeReturnFromLeaveJob` — abre a solicitação na data prevista (AC: 1, 3, 4)
  - [x] Criar `domain/access/status/service/EmployeeReturnFromLeaveJob.java` (`@Component`, `@Scheduled(cron = "0 0 6 * * *", zone = "UTC")` — 1x/dia, cedo; decisão confirmada com o usuário).
  - [x] Query: `EmployeeStatusHistoryRepository.findAllByExpectedReturnDateLessThanEqual(LocalDate today)` (N linhas com `expectedReturnDate <= today`) + `findTopByEmployeeIdOrderByCreatedAtDesc(Long employeeId)`, checando 1 a 1 se a linha candidata é a mais recente do Funcionário — versão simples confirmada com o usuário (sem `DISTINCT ON`).
  - [x] Para cada linha qualificada: resolve os `Login`s do Funcionário (`Employee.login`, `Set<Login>`), abrindo 1 `LoginApprovalRequest` por Login `INACTIVE`/`BLOCKED` (ignorando `ACTIVE`/`PENDING_APPROVAL`/`REJECTED` — decisão confirmada com o usuário); para cada Login elegível, checa se já existe solicitação (`REACTIVATE_LOGIN`/`AUTO_CANCEL`) criada depois de `employeeStatusHistory.createdAt` (AC 4); se não existir, cria uma nova via `LoginApprovalChainResolver.firstLevelFor`, com `requestedByLogin=null`.
  - [x] Adicionado `LoginApprovalRequestRepository.existsByLoginIdAndRequestTypeAndEscalationPolicyAndCreatedAtAfter(Long loginId, LoginApprovalRequestType type, LoginApprovalRequestEscalationPolicy policy, Instant after)` (`default`, QueryDSL).
  - [x] Criado `EmployeeReturnFromLeaveJobTest.java`, `Clock.fixed`, cobrindo: retorno na data → abre solicitação; Funcionário com transição mais recente que não é a de licença → não abre; solicitação já aberta para o mesmo episódio → não abre de novo; Login já `ACTIVE` → não abre.

- [x] Task 2: Auto-cancelamento em 5 dias úteis — estende o job de escalonamento da Story 3.2 (AC: 2)
  - [x] Adicionado `LoginApprovalRequest.cancel(Instant now)` (mesmo padrão de `approve`/`reject`): `status=CANCELLED`, `decidedAt=now`, sem `decidedByLogin`. Lança `ScosException SCOS_LOGIN_APPROVAL_REQUEST_002` se `status != PENDING`.
  - [x] Em `LoginApprovalEscalationJob`, antes de escalar cada solicitação `PENDING` vencida, checa `isPastAutoCancelDeadline` (`AUTO_CANCEL` + 5 dias úteis desde `createdAt` vencidos) → `cancel()` em vez de escalar. `INDEFINITE` nunca cancela sozinho.
  - [x] Não criado job separado — reaproveita o mesmo `@Scheduled` da Story 3.2.
  - [x] Estendido `LoginApprovalEscalationJobTest`: solicitação `AUTO_CANCEL` com 5 dias úteis vencidos → `CANCELLED`, não escala; solicitação `AUTO_CANCEL` dentro dos 5 dias mas com SLA de nível vencido → escala normalmente; solicitação `INDEFINITE` nunca cancela sozinha.

- [x] Task 3: Guarda de escopo (AC: 2, 4)
  - [x] Nenhuma "reabertura automática" implementada — reabertura manual (`enable`/`unblock`, Story 3.3) continua criando request comum (`INDEFINITE`), fora do escopo desta story.
  - [x] Nenhuma rota nova criada.
  - [x] Nenhuma mudança em `EmployeeStatusHistory`/`expectedReturnDate` — só leitura.

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

Claude Sonnet 5

### Debug Log References

- As 3 decisões de design sinalizadas na story (frequência do job, estratégia de query, tratamento de múltiplos Logins) foram confirmadas com o usuário via pergunta direta antes da implementação — todas as opções recomendadas/simples foram aceitas.
- `mvn -pl organization/flow-organization-domain test -Denforcer.skip=true` — 418 testes, 0 falhas.
- `mvn install -DskipTests -Denforcer.skip=true` (raiz) — compilação completa do reactor (domain/usecase/api/boot/infrastructure/shared/resources) sem erros.
- Skill `ponytail-review` aplicada ao diff da story: 2 achados menores corrigidos (`.map().map()` encadeado → 1 `.map()` com lambda comparando ids; constante `SYSTEM_USER` usada 1x → literal `"SYSTEM"` inline, mesmo padrão de `ScosSystemServiceBean`/`"REGISTRY"`). Resto do diff (repositórios, `cancel()`, branch de auto-cancelamento, testes) já seguia os padrões existentes do módulo, sem indireção nova.

### Completion Notes List

- `EmployeeReturnFromLeaveJob` (novo, `domain/access/status/service/`): `@Scheduled(cron = "0 0 6 * * *", zone = "UTC")`, 1x/dia. Busca `EmployeeStatusHistory` com `expectedReturnDate <= hoje`, confirma 1 a 1 que é a transição mais recente do Funcionário (AC 3), resolve todos os Logins `INACTIVE`/`BLOCKED` do Funcionário e abre 1 `LoginApprovalRequest` (`REACTIVATE_LOGIN`/`AUTO_CANCEL`/`requestedByLogin=null`) por Login elegível, salvo se já existir uma solicitação para o mesmo episódio (AC 4, comparando `createdAt`). `userAt="SYSTEM"` no audit (mesmo padrão de `ScosSystemServiceBean`/`"REGISTRY"`).
- `LoginApprovalRequest.cancel(Instant)`: novo método de transição (`status=CANCELLED`, `decidedAt=now`, sem `decidedByLogin`), mesmo padrão/guarda (`assertPending`) de `approve`/`reject`/`escalate`.
- `LoginApprovalEscalationJob`: antes de escalar, checa se a solicitação é `AUTO_CANCEL` e já passou do teto de 5 dias úteis desde `createdAt` — se sim, cancela em vez de escalar (prioridade sobre escalonamento). `INDEFINITE` nunca entra nesse branch. Reaproveita o `@Scheduled` existente (Story 3.2), sem job dedicado.
- Repositórios: `EmployeeStatusHistoryRepository` ganhou `findAllByExpectedReturnDateLessThanEqual` (QueryDSL, `default`) e `findTopByEmployeeIdOrderByCreatedAtDesc` (derivada Spring Data); `LoginApprovalRequestRepository` ganhou `existsByLoginIdAndRequestTypeAndEscalationPolicyAndCreatedAtAfter` (QueryDSL, `default`) — converte `Instant`→`LocalDateTime` em UTC ao comparar com `createdAt` (dívida conhecida da `BaseEntity` da foundation, já documentada no project-context).
- Nenhuma mudança de schema/Liquibase — reaproveita tudo que já existe desde a Story 3.2 (`AUTO_CANCEL` já estava no `CHECK` de `escalation_policy` desde 2026-07-18) e a Story 2.4 (`expectedReturnDate`).
- Nenhum endpoint novo, nenhuma mudança de contrato OpenAPI.

### File List

- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/access/status/service/EmployeeReturnFromLeaveJob.java` (novo)
- `organization/flow-organization-domain/src/test/java/br/com/sawcunhaos/organization/domain/access/status/service/EmployeeReturnFromLeaveJobTest.java` (novo)
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/access/status/internal/EmployeeStatusHistoryRepository.java` (alterado)
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/access/login/internal/LoginApprovalRequestRepository.java` (alterado)
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/access/login/internal/LoginApprovalRequest.java` (alterado)
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/access/login/service/LoginApprovalEscalationJob.java` (alterado)
- `organization/flow-organization-domain/src/test/java/br/com/sawcunhaos/organization/domain/access/login/service/LoginApprovalEscalationJobTest.java` (alterado)

## Change Log

| Data | Descrição |
|------|-----------|
| 2026-08-16 | Story criada — Epic 3, job de retorno automático de licença/férias (`AUTO_CANCEL`, reaproveitando toda a cadeia da Story 3.2). Auto-cancelamento em 5 dias úteis dobra no mesmo job de escalonamento em vez de criar um segundo `@Scheduled`. Decisões de design sinalizadas: frequência do job, tratamento de Funcionário com múltiplos Logins, estratégia de query (N consultas vs. `DISTINCT ON`). |
| 2026-08-17 | Story implementada — `EmployeeReturnFromLeaveJob` novo (Task 1); `LoginApprovalRequest.cancel()` + branch de auto-cancelamento no `LoginApprovalEscalationJob` (Task 2); guarda de escopo confirmada (Task 3). Decisões de design confirmadas com o usuário (frequência diária, query simples por N consultas, 1 solicitação por Login elegível). Revisão `ponytail` aplicada, 2 achados menores corrigidos. 418 testes do módulo domain passando, reactor completo compila. |
