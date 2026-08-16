---
baseline_commit: b72b2dcc0f63997fe313658e358e68dc506dd645
---

# Story 4.1: Criação de Login de Sistema/Serviço (EXTERNAL/SERVICE)

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como membro de TI,
Eu quero criar um Login sem Funcionário vinculado,
Para que ferramentas ou integrações automatizadas consumam a API com identidade própria.

## Acceptance Criteria

1. **Given** TI cria um Login do tipo `EXTERNAL` ou `SERVICE` (sem `employeeId`) via `POST /v1/logins` **When** a solicitação é enviada **Then** entra em `PENDING_APPROVAL` **And** o aprovador é resolvido diretamente pelo grupo `APPROVE_SYSTEM_ACCESS`, sem hierarquia e sem supervisor — reaproveita **sem alterar** `LoginService.create` (Story 3.1) e `LoginApprovalChainResolver.firstLevelFor`/`LoginApprovalRequest` (Story 3.2), que já resolvem `type != EMPLOYEE` direto pra `SYSTEM_ACCESS_GROUP` (FR-26).
2. **Given** hoje `CreateLoginRequest` exige `reasonActivateId` e a descrição do endpoint diz "Inicia a Saga Keycloak" (mesmo bug de contrato que a Story 3.1 corrigiu em `CreateEmployeeLoginRequest`, deliberadamente não tocado lá) **When** esta story é implementada **Then** o contrato é corrigido: `reasonActivateId` sai do corpo, descrição passa a refletir `PENDING_APPROVAL`.
3. **Given** um membro do grupo `APPROVE_SYSTEM_ACCESS` tenta aprovar a própria solicitação **When** chama a ação de aprovar **Then** o sistema rejeita, exceto pela válvula de última instância — **mesmo mecanismo da Story 3.2 (`DecideLoginApprovalRequestUseCase`, AC 3 dela), sem alteração**: o cálculo de auto-aprovação já é agnóstico a `requestType`/`Login.type`.
4. **Given** ninguém do grupo decide em 1 dia útil **When** o SLA estoura **Then** o sistema **não** escala pra nenhum nível adicional (já é `SYSTEM_ACCESS_GROUP` desde o início — mesmo comportamento "permanece pendente" do `LoginApprovalEscalationJob`, Story 3.2 AC 4, sem alteração) **And** o "renotifica o grupo inteiro" do texto original do épico **não** é implementado nesta story — depende do Motor de Notificação (Etapa 3/P2), que não existe, mesma guarda de escopo já aplicada em toda solicitação de aprovação do Epic 3.
5. **Given** um Login `EXTERNAL`/`SERVICE` `INACTIVE`/`BLOCKED` precisa ser reativado **When** `enable`/`unblock` é acionado **Then** segue o mesmo mecanismo de aprovação da Story 3.3 (`RequestLoginReactivationUseCase`) — **sem alteração**, esse Use Case já não distingue `Login.type`.

## Tasks / Subtasks

- [ ] Task 1: Corrigir o contrato — `CreateLoginRequest` perde `reasonActivateId` (AC: 2)
  - [ ] Em `etc/api/organization/ScosOrganization_Login.yml`, `CreateLoginRequest` (linhas ~1000-1025): remover `reasonActivateId` de `required` e de `properties`. Atualizar `description` do schema.
  - [ ] `POST /v1/logins` (`createLogin`, linhas ~55-80): atualizar `description` — mesmo texto padrão da Story 3.1 (`createEmployeeLogin`): "UC-057 - Cria login em PENDING_APPROVAL. A Saga Keycloak só dispara quando a aprovação leva o Login a ACTIVE".
  - [ ] `LoginStatus` (schema) **não** precisa de mudança — `PENDING_APPROVAL` já foi adicionado pela Story 3.1 (schema compartilhado por todos os tipos de Login).
  - [ ] Rodar `mvn clean generate-sources` em `flow-organization-usecase`/`flow-organization-api` para confirmar `CreateLoginRequest` gerado sem `reasonActivateId`.

- [ ] Task 2: `CreateLoginUseCase(Bean)` + Delegate (AC: 1)
  - [ ] Criar `usecase/access/login/CreateLoginUseCase(Bean).java` — `execute(String login, Long profileId, LoginType type)` (`type` restrito a `EXTERNAL`/`SERVICE` — validar no Use Case, `422` se vier `EMPLOYEE` por engano; `EMPLOYEE` só nasce via `CreateEmployeeLoginUseCase`, Story 3.1, path diferente). Fluxo, **idêntico ao que `CreateEmployeeLoginUseCaseBean` já faz desde a Story 3.2** (que estendeu a criação pra abrir a `LoginApprovalRequest` na mesma transação): `loginService.create(LoginInput sem employeeId)` → `LoginApprovalChainResolver.firstLevelFor(login)` (resolve direto `SYSTEM_ACCESS_GROUP`, `login.employee == null`) → monta `LoginApprovalRequest` (`requestType=CREATE_LOGIN`, `escalationPolicy=INDEFINITE`, `requestedByLogin`=chamador) → persiste.
  - [ ] **Não duplicar** a lógica de abrir `LoginApprovalRequest` — extrair pra um método compartilhado se `CreateEmployeeLoginUseCaseBean`/`CreateLoginUseCaseBean` acabarem com código muito parecido (ex.: um `LoginApprovalRequestFactory`/método `default` numa interface, ou um pequeno componente injetado nos dois) — **decisão de implementação, não de design**: só extrair se a duplicação for real na hora de escrever, não abstrair preventivamente (mesmo critério ponytail já aplicado nas stories anteriores do Epic 3).
  - [ ] `LoginDelegate` (Story 3.1) ganha `@Override` de `createLogin`.

- [ ] Task 3: Guarda de escopo (AC: 4)
  - [ ] **Não** implementar renotificação — Motor de Notificação é Etapa 3/P2, não existe. `LoginApprovalEscalationJob` (Story 3.2) já trata `SYSTEM_ACCESS_GROUP` sem próximo nível corretamente (permanece `PENDING`); nada a mudar nele.
  - [ ] **Não** tocar em `RequestLoginReactivationUseCase`/`DecideLoginApprovalRequestUseCase` (Stories 3.2/3.3) — já funcionam para qualquer `Login.type` sem modificação. Rodar os testes de integração existentes com um Login `EXTERNAL`/`SERVICE` é suficiente para confirmar (Task 4), não é motivo pra tocar no código.
  - [ ] **Não** implementar Story 4.2 (aprovação de criação/edição de Perfil, FR-27) — **já coberta pela Story 3.6** (Epic 3), criada numa sessão anterior antes de se descobrir que FR-27 pertence ao Epic 4. Decisão do usuário: manter o trabalho na Story 3.6 em vez de duplicar aqui — ver nota em `epics.md`.

- [ ] Task 4: Testes (AC: 1, 3, 5)
  - [ ] `CreateLoginUseCaseBeanTest` novo — sucesso (`EXTERNAL`/`SERVICE`); rejeita `type=EMPLOYEE`; `LoginApprovalRequest` criada já em `SYSTEM_ACCESS_GROUP` (sem `SUPERVISOR`/`MANAGER` no caminho).
  - [ ] Integração (`LoginControllerTest`): `POST /v1/logins` sucesso `201`, `PENDING_APPROVAL`; fluxo completo `POST` → `GET /v1/login-approval-requests?loginId=X` mostra `currentLevel=SYSTEM_ACCESS_GROUP` desde a criação → `PUT approve` (por um titular de `APPROVE_SYSTEM_ACCESS`) → Login `ACTIVE`. Reaproveitar o mesmo titular de `APPROVE_SYSTEM_ACCESS` semeado para os testes da Story 3.2, não semear um segundo.
  - [ ] Rodar (não reescrever) os testes de `enable`/`unblock`/decisão da Story 3.3 com um Login `EXTERNAL`/`SERVICE` como caso adicional, confirmando que nada nesses fluxos assume `Login.type == EMPLOYEE`.

## Dev Notes

### Contexto crítico — leia antes de implementar

**Esta story é quase só o Delegate + 1 Use Case fino — o motor inteiro já existe.** `epics.md` já documenta a dependência: "Epic 4 depende de Epic 3 (mecanismo de Login/Perfil/aprovação já precisa existir; o grupo `APPROVE_SYSTEM_ACCESS` em si é semeado via schema, não criado pelo fluxo)". Isso não é retórica — na prática, `LoginService.create`, `LoginApprovalChainResolver`, `LoginApprovalRequest`, `DecideLoginApprovalRequestUseCase`, `LoginApprovalEscalationJob` e o `RequestLoginReactivationUseCase` (Stories 3.1-3.3) já foram desenhados para serem agnósticos a `Login.type` — esta story só liga o fio que faltava: o endpoint `POST /v1/logins` e o Use Case fino que abre a criação para `EXTERNAL`/`SERVICE`.

**Por que esta story existe separada da Story 3.1, e não foi absorvida por ela.** Numa sessão anterior, a Story 3.1 chegou a incluir `POST /v1/logins` no próprio escopo — uma validação cruzada com os demais épicos (pedida pelo usuário) achou que isso invadia FR-26, que `epics.md` mapeia explicitamente para o Epic 4 (`FRs covered: FR-26, FR-27`), não Epic 3 (`FRs covered: FR-6, FR-7, FR-23, FR-24, FR-25`). A Story 3.1 foi corrigida para não tocar em `POST /v1/logins`; esta story assume o que sobrou.

**Grupo `APPROVE_SYSTEM_ACCESS` no Keycloak — mesmo gap operacional já sinalizado no Epic 3.** `epics.md` já reconhece: "o grupo em si é semeado via schema, não criado pelo fluxo". Confirmado nesta análise: o realm (`etc/infra/keycloak/Scos_Realm.json`) não tem esse grupo cadastrado. Fora do escopo de código desta story — é trabalho de ops/seed, mesma observação já registrada para a Story 3.2.

### Onde cada peça vai (camadas)

- `etc/api/organization/ScosOrganization_Login.yml`: `CreateLoginRequest` perde `reasonActivateId`; `createLogin` descrição atualizada.
- `usecase/access/login/CreateLoginUseCase(Bean).java` (novo).
- `api/delegate/login/LoginDelegate.java` (Story 3.1): `+@Override createLogin`.
- Nenhuma mudança em `domain` além do que as Stories 3.1/3.2/3.3 já entregam — este épico só consome.

### Testing Standards

- Mesmo padrão de toda a Story 3.x (Mockito, Given/When/Then, `ScosOrganizationTestUtil` para integração).
- Não recriar cenários já cobertos pelas Stories 3.2/3.3 (auto-aprovação, escalonamento, reativação) — só confirmar que funcionam também para `EXTERNAL`/`SERVICE`, como caso adicional, não como suíte paralela.

### Project Structure Notes

- Nenhum pacote novo — tudo em `usecase/access/login/`, já existente desde a Story 3.1.

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Epic 4 / Story 4.1] — Given/When/Then originais.
- [Source: _bmad-output/planning-artifacts/epics.md#Epic List] — "Epic 4 depende de Epic 3 (mecanismo já precisa existir; grupo `APPROVE_SYSTEM_ACCESS` semeado via schema)".
- [Source: _bmad-output/implementation-artifacts/3-1-criacao-login-vinculado-funcionario.md] — `LoginService.create`, `CreateEmployeeLoginUseCase`, `LoginDelegate` — base direta desta story; Change Log dela registra a correção de escopo que originou esta story.
- [Source: _bmad-output/implementation-artifacts/3-2-cadeia-aprovacao-criacao-login.md] — `LoginApprovalChainResolver.firstLevelFor` (branch `EXTERNAL`/`SERVICE`, já genérico), `DecideLoginApprovalRequestUseCase`, `LoginApprovalEscalationJob` — reaproveitados sem alteração.
- [Source: _bmad-output/implementation-artifacts/3-3-aprovacao-reativacao-login.md] — `RequestLoginReactivationUseCase` — reaproveitado sem alteração (AC 5).
- [Source: _bmad-output/implementation-artifacts/3-6-aprovacao-criacao-alteracao-perfil-recursos.md] — já cobre FR-27 (Story 4.2 original); não recriar.

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List

## Change Log

| Data | Descrição |
|------|-----------|
| 2026-08-16 | Story criada — Epic 4, criação de Login `EXTERNAL`/`SERVICE`. Nasceu de uma correção de escopo na Story 3.1 (FR-26 pertence ao Epic 4, não Epic 3, conforme mapeamento em `epics.md`). Quase todo o mecanismo (Service/Resolver/ApprovalRequest/Decide/Escalation) já existe, criado pelas Stories 3.1-3.3 — esta story só liga `POST /v1/logins`. |
