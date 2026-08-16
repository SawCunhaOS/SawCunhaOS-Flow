---
baseline_commit: b72b2dcc0f63997fe313658e358e68dc506dd645
---

# Story 3.7: Cascade de Status — Funcionário Desativado/Bloqueado Inativa seus Logins

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como sistema,
Eu quero que desativar ou bloquear um Funcionário inative automaticamente todos os Logins `ACTIVE` vinculados a ele,
Para que ninguém continue com acesso depois que o vínculo com a empresa muda, sem RH precisar lembrar de fazer isso manualmente pra cada Login.

## Acceptance Criteria

1. **Given** um Funcionário `ACTIVE` com 1+ Logins `ACTIVE` **When** RH aciona `PUT /v1/employees/{id}/disable` (`EmployeeServiceBean.inactivate`, já implementado, Story 2.2) **Then**, além da própria transição do Funcionário, todos os Logins `ACTIVE` vinculados a ele transitam para `INACTIVE`, cada um gravando `LoginStatusHistory` **And** cada transição dispara a Saga Keycloak (grava `OutboxEvent`, tópico já semeado pela Story 3.2, sem dispatcher real — mesma guarda de escopo de toda story do Epic 3) — mesmo comportamento documentado em `etc/doc/usecase/03-funcionario.md` (UC-042, "Side Effects" item 2) e no FR-28 do PRD, nunca implementado até agora (Stories 2.2 e 2.4 deferiram explicitamente pra "a story final do Epic 3").
2. **Given** um Funcionário `ACTIVE`/`INACTIVE` com 1+ Logins `ACTIVE` **When** RH aciona `PUT /v1/employees/{id}/block` (`EmployeeServiceBean.disable`, já implementado, Story 2.2) **Then** o mesmo cascade do AC 1 acontece — **mesmo destino** (`INACTIVE`, não `BLOCKED`): o `etc/doc/usecase/03-funcionario.md` (UC-043) descreve o mesmo side effect pra `/block` que pra `/disable`, sem distinção.
3. **Given** um Funcionário reativado (`enable`/`unblock`, Story 2.2) **When** a reativação é confirmada **Then** nenhum Login vinculado é reativado automaticamente (FR-14, já confirmado pela Epic 6 Story 6.2 e pelos ACs 1/4 da própria Story 2.2) — cada Login segue seu próprio fluxo de aprovação (Story 3.3), sem cascade nesta direção. Esta story só cascateia status **redutor** (ACTIVE→INACTIVE/DISABLED do Funcionário), nunca o inverso.
4. **Given** um Funcionário com Logins em outros estados (`PENDING_APPROVAL`, `BLOCKED`, `INACTIVE`, `REJECTED`) **When** o cascade roda **Then** só os Logins `ACTIVE` são tocados — os demais permanecem no estado em que estavam (um Login já `BLOCKED` não vira `INACTIVE` por tabela; um `PENDING_APPROVAL` continua pendente, sujeito ao próprio fluxo de aprovação).
5. **Given** o motivo de inativação do Login cascateado **When** o cascade grava `LoginStatusHistory` **Then** usa o registro já semeado `ReasonInactivate(code=ACCOUNT_CLOSED, entityType=LOGIN)` (`etc/database/seed_data.sql`, `REASON_INACTIVATE_ID=3`) — **fixo, independente de qual motivo específico levou o Funcionário a `disable`/`block`** (não tenta mapear "motivo do Funcionário" → "motivo do Login", que não tem correspondência 1:1 no catálogo).

## Tasks / Subtasks

- [ ] Task 1: `LoginRepository`/`LoginService` — encontrar e inativar os Logins `ACTIVE` de um Funcionário (AC: 1, 2, 4)
  - [ ] Em `LoginRepository.java` (Story 3.1), adicionar `List<Login> findAllByEmployeeIdAndStatus(Long employeeId, LoginStatus status)` — predicate de 2 campos diretos (`qLogin.employee.id.eq(employeeId).and(qLogin.status.eq(status))`), sem `BooleanBuilder` (critério da Story 2.6 — não entra em `LoginPredicates`, é `default` direto na interface, mesmo padrão de `CompanyRepository.existsByStatus`).
  - [ ] Em `LoginService.java` (Story 3.1), adicionar `void inactivateAllActiveLoginsForEmployee(@NonNull Long employeeId)`.
  - [ ] Em `LoginServiceBean.java`, implementar: busca `findAllByEmployeeIdAndStatus(employeeId, ACTIVE)`; resolve o `ReasonInactivate` fixo via `reasonInactivateRepository.findByCodeAndEntityType("ACCOUNT_CLOSED", EntityType.LOGIN)` (método novo no repositório, `Optional<ReasonInactivate>`, mesmo padrão de `existsByCodeAndEntityType` já existente — se não encontrar, `ScosException` com um código novo, ex. `SCOS_LOGIN_016`, `500`/`SCOS_TITLE_INTERNAL_ERROR` — isso indicaria seed ausente, erro de configuração do ambiente, não erro de negócio do usuário); para cada `Login` encontrado: `LoginStatusHistory history = login.inactivate(reason.id())`, salva `login` + `history`, grava `OutboxEvent` (Saga Keycloak — reaproveitar o **mesmo** helper/padrão de gravação que a Story 3.2 Task 8 já usa na aprovação, não reinventar).
  - [ ] **Não** criar `LoginPredicates` nova pra isso — critério de escopo já estabelecido (Story 2.6): sem `BooleanBuilder`, sem classe `Predicates`.

- [ ] Task 2: Hook em `EmployeeServiceBean` — Epic 2, já implementado, ganha 1 chamada nova por método (AC: 1, 2)
  - [ ] Em `EmployeeServiceBean.inactivate(...)` (linha ~216-228), depois de `employeeStatusHistoryRepository.merge(history);`, adicionar `loginService.inactivateAllActiveLoginsForEmployee(id);`.
  - [ ] Em `EmployeeServiceBean.disable(...)` (linha ~248-258, nome do método de domínio — corresponde à rota `/block` do contrato, mesma inconsistência de nomenclatura já documentada noutras stories), mesma adição depois de `employeeStatusHistoryRepository.merge(history);`.
  - [ ] Injetar `LoginService` (interface, `domain/access/login/specification`) no construtor de `EmployeeServiceBean` (`@RequiredArgsConstructor` já cobre, só adicionar o campo `private final LoginService loginService;`). Cruza de `domain.corporate.employee` pra `domain.access.login` via interface `specification` — mesmo padrão de fronteira já usado por `EmployeeServiceBean` chamando `CompanyService`/`PositionService` (specifications de outros agregados), não uma exceção nova à regra.
  - [ ] **Não** tocar em `EmployeeServiceBean.activate(...)`/`enable(...)` (reativação) — AC 3, sem cascade nessa direção.
  - [ ] **Não** tocar em `create(...)`/`rehire(...)` — não são transições redutoras, fora do escopo desta story.

- [ ] Task 3: Guarda de escopo (AC: 1, 3)
  - [ ] **Não** implementar Kill Switch (Redis denylist) — é Epic 6 (`FRs covered: FR-13, FR-14`), que "depende de Epic 2 e Epic 3" — o hook que o Kill Switch vai precisar (chamar algo toda vez que um Login vira `INACTIVE`/`BLOCKED`) já existe naturalmente depois desta story (`Login.inactivate()` sendo chamado de um lugar central em `LoginServiceBean`) — Epic 6 estende esse ponto, não esta story.
  - [ ] **Não** implementar o dispatcher do Outbox — mesma guarda já aplicada em toda story do Epic 3 (Stories 3.1/3.2): só grava a linha.
  - [ ] **Não** cascatear Login `BLOCKED`→algo quando o Funcionário some — se um Login já está `BLOCKED` (bloqueio próprio, independente do Funcionário), o cascade desta story não o toca (AC 4) — nenhum código do épico pede isso, e inventar seria além do que o `etc/doc/usecase/03-funcionario.md` descreve ("Logins **ACTIVE** → INACTIVE", não "todos os Logins").
  - [ ] **Não** criar endpoint novo — toda a mudança é interna (`EmployeeServiceBean` + `LoginService`), nenhuma rota HTTP nova.

## Dev Notes

### Contexto crítico — leia antes de implementar

**Este gap não foi inventado agora — está documentado há duas stories.** Story 2.2 (Task 8, `2-2-ciclo-vida-funcionario.md`) e Story 2.4 (linha 22 e 146-149, `2-4-licenca-ferias-colocar-funcionario-licenca.md`) **ambas** registraram explicitamente: "nenhum cascade de status para o Login vinculado ao Funcionário... fica para a story final do Epic 3" — e citam a mesma fonte primária, `etc/doc/usecase/03-funcionario.md` (UC-042/UC-043, "Side Effects"), que descreve o comportamento completo desde antes do Epic 2 existir. Isso não foi coberto por nenhuma das Stories 3.1-3.6 originais — achado numa validação cruzada entre épicos pedida pelo usuário, depois que as 6 primeiras stories do Epic 3 já tinham sido criadas.

**Por que o motivo do Login é sempre `ACCOUNT_CLOSED`, fixo.** O catálogo `ReasonInactivate` é por `entityType` — um motivo de `EMPLOYEE` (ex.: `RESIGNATION`) não pode ser usado num `LoginStatusHistory`, que exige `entityType=LOGIN`. Não existe (e esta story não cria) um mapeamento "motivo do Funcionário → motivo do Login equivalente" — seria inventar uma regra de negócio que nenhuma fonte (PRD, `epics.md`, doc de spec) pede. `ACCOUNT_CLOSED` ("Encerramento definitivo da conta") já está semeado desde sempre em `etc/database/seed_data.sql` (`REASON_INACTIVATE_ID=3`, único registro `LOGIN` do catálogo) — a leitura mais direta é que ele já foi reservado exatamente para este cascade, mesmo nunca tendo sido consumido por código até agora.

**Por que `Login.inactivate()` já existia antes do Epic 3 e nunca tinha endpoint próprio.** Auditoria confirma: `ScosOrganization_Login.yml` **não tem** nenhuma rota `PUT /v1/logins/{id}/disable` (só `enable`/`block`/`unblock`) — ou seja, `Login.inactivate()` (`ACTIVE→INACTIVE`) só é alcançável por cascade, nunca por ação direta de RH sobre o próprio Login. Isso confirma, por engenharia reversa do código já existente, que o método sempre foi pensado para este uso — esta story é quem finalmente o consome pela primeira vez.

**Cruza fronteira de bounded context (`domain.corporate.employee` → `domain.access.login`) via `specification`, não `internal`.** Mesma regra já documentada no projeto ("nada fora do agregado importa de `internal/` de outro agregado sem passar pelo `specification/`") — `EmployeeServiceBean` já faz isso hoje com `CompanyService`/`PositionService`; injetar `LoginService` é extensão do mesmo padrão, não uma exceção.

**Transação única, ou uma por Login?** `EmployeeServiceBean.inactivate`/`.disable` já são `@Transactional(rollbackFor = ScosException.class)` (herdado da classe ou do método, confirmar ao implementar) — a chamada a `loginService.inactivateAllActiveLoginsForEmployee` entra na **mesma transação** do Use Case que a originou (nenhum `@Transactional` novo necessário em `LoginServiceBean` se o método for chamado dentro de uma transação já aberta pelo chamador; se `LoginServiceBean` for outro bean Spring com seu próprio `@Transactional`, o Spring propaga por padrão `REQUIRED` — confirmar que não há `REQUIRES_NEW` acidental, que separaria a transação e quebraria atomicidade "Funcionário desativado + Logins inativados juntos ou nada").

### Onde cada peça vai (camadas)

- `domain/access/login/internal/LoginRepository.java` (Story 3.1): `+findAllByEmployeeIdAndStatus`.
- `domain/access/login/specification/LoginService.java` + `service/LoginServiceBean.java` (Story 3.1): `+inactivateAllActiveLoginsForEmployee`.
- `domain/access/status/internal/ReasonInactivateRepository.java`: `+findByCodeAndEntityType` (se ainda não existir depois das Stories anteriores — conferir, pode já ter sido criado por alguma delas para outro fim).
- `domain/corporate/employee/service/EmployeeServiceBean.java` (Epic 2, já existe): `+LoginService` injetado; `inactivate`/`disable` ganham a chamada de cascade.
- `shared/exception/ExceptionCodeError.java`: `+SCOS_LOGIN_016` (seed ausente, erro de configuração — improvável em produção, mas não deve estourar `NullPointerException`).
- Nenhuma mudança de contrato OpenAPI, Liquibase ou permissão — cascade é efeito colateral interno, não uma ação que RH aciona diretamente.

### Testing Standards

- `LoginServiceBeanTest` (Story 3.1, estendido): `inactivateAllActiveLoginsForEmployee` — Funcionário com 2 Logins `ACTIVE` → ambos inativados, 2 `LoginStatusHistory`, 2 `OutboxEvent`; Funcionário com 1 Login `ACTIVE` e 1 `BLOCKED` → só o `ACTIVE` é tocado (AC 4); Funcionário sem Login nenhum → não quebra, no-op.
- `EmployeeServiceBeanTest` (Epic 2, já existe — estender, não recriar): `inactivate`/`disable` passam a verificar (via `Mockito.verify`) que `loginService.inactivateAllActiveLoginsForEmployee(id)` foi chamado exatamente 1 vez.
- Integração: estender `EmployeeControllerTest` (Epic 2) — `PUT /v1/employees/{id}/disable`/`/block` com um Funcionário que tem Login `ACTIVE` (criado no próprio teste) → depois da chamada, consultar `GET /v1/logins/{id}` (Story 3.1) confirma `status=INACTIVE`. Fluxo ponta a ponta real, não só unitário.

### Project Structure Notes

- Nenhum pacote novo — tudo em classes já criadas pelas Stories 2.2 (Epic 2) e 3.1 (Epic 3).

### References

- [Source: _bmad-output/implementation-artifacts/2-2-ciclo-vida-funcionario.md#Task 8, Dev Notes] — origem do gap, "fica para a story final do Epic 3".
- [Source: _bmad-output/implementation-artifacts/2-4-licenca-ferias-colocar-funcionario-licenca.md#linha 22, 146-149, 158] — reforça o mesmo gap, cita FR-28 completo.
- [Source: etc/doc/usecase/03-funcionario.md:168-170,187-189] — UC-042 (`/disable`)/UC-043 (`/block`), "Side Effects" com o comportamento exato a implementar.
- [Source: etc/database/seed_data.sql:97-108] — `ReasonInactivate` seed, `ACCOUNT_CLOSED`/`LOGIN` já reservado (`REASON_INACTIVATE_ID=3`).
- [Source: organization/flow-organization-domain/.../corporate/employee/service/EmployeeServiceBean.java:216-258] — `inactivate`/`disable` atuais, ponto de hook exato.
- [Source: organization/flow-organization-domain/.../access/login/internal/Login.java] — `inactivate(Long reasonInactivateId)` já existente (pré-Epic 3), nunca consumido — confirmado sem rota própria em `ScosOrganization_Login.yml`.
- [Source: _bmad-output/implementation-artifacts/3-1-criacao-login-vinculado-funcionario.md] — `LoginService`/`LoginServiceBean`/`LoginRepository` que esta story estende.
- [Source: _bmad-output/implementation-artifacts/3-2-cadeia-aprovacao-criacao-login.md#Task 1] — seed de `SCOS_OUTBOX_TOPIC` (Keycloak), reaproveitado sem duplicar; padrão de gravação de `OutboxEvent` (Task 8 dela) reaproveitado aqui.
- [Source: _bmad-output/planning-artifacts/epics.md#Epic 6] — "Epic 6 depende de Epic 2 e Epic 3" — confirma que o hook desta story é o ponto de extensão esperado pro Kill Switch, não algo a implementar aqui.

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List

## Change Log

| Data | Descrição |
|------|-----------|
| 2026-08-16 | Story criada — gap crítico encontrado numa validação cruzada entre épicos (pedida pelo usuário): Story 2.2 e 2.4 (Epic 2, ambas `done`) deferiram explicitamente o cascade de status Funcionário→Login pra "a story final do Epic 3", mas nenhuma das 6 stories originais do Epic 3 o implementava. Fecha FR-6/FR-13/FR-28 (parte do cascade), reaproveitando `Login.inactivate()` (já existente, nunca consumido) e o motivo `ACCOUNT_CLOSED` (já semeado, nunca usado). |
