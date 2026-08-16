---
baseline_commit: b72b2dcc0f63997fe313658e358e68dc506dd645
---

# Story 3.3: Aprovação de Reativação de Login

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como Supervisor ou Gerente do Departamento,
Eu quero que reativar um Login exija a mesma aprovação de criação,
Para nunca reativar acesso sem aval humano.

## Acceptance Criteria

1. **Given** um Login `INACTIVE` ou `BLOCKED` **When** RH aciona `PUT /v1/logins/{id}/enable` ou `PUT /v1/logins/{id}/unblock` **Then** a solicitação entra na mesma cadeia de 3 níveis da Story 3.2 (`requestType=REACTIVATE_LOGIN`, `escalationPolicy=INDEFINITE`), sem redução de níveis **And** o Login **permanece** no seu estado atual (`INACTIVE`/`BLOCKED`) — a transição síncrona que esses 2 endpoints faziam antes desta story deixa de existir.
2. **Given** a solicitação de reativação está pendente **When** o Login permanece no seu estado atual **Then** nunca transita para `ACTIVE` sem a aprovação completa — mesmo mecanismo de decisão da Story 3.2 (`GET`/`approve`/`reject`), generalizado para reconhecer `requestType=REACTIVATE_LOGIN`.
3. **Given** um Login já tem uma `LoginApprovalRequest` `PENDING` em aberto **When** RH aciona `enable`/`unblock` de novo (ou `disable`/`block`/`enable`/`unblock` em qualquer combinação enquanto isso) **Then** o sistema rejeita com `422` — só 1 solicitação pendente por Login por vez.
4. **Given** a aprovação é concedida **When** a decisão é registrada **Then** o Login transita para `ACTIVE` usando o método de domínio correto conforme o estado de origem (`activate()` se veio de `INACTIVE`, `enable()` se veio de `BLOCKED`) — **não** `Login.approve()` (esse é exclusivo do fluxo de criação, Story 3.2) — e dispara a Saga Keycloak (mesma guarda de escopo: só grava `OutboxEvent`, sem dispatcher).
5. **Given** a aprovação é rejeitada **When** a decisão é registrada **Then** o Login **permanece** no estado de origem (`INACTIVE`/`BLOCKED`) — diferente do fluxo de criação (Story 3.2), onde rejeitar leva a `REJECTED` (estado terminal novo). Aqui não há "novo" estado terminal: rejeitar uma reativação só significa "continua como estava".
6. **Given** `disable`/`block` (que **reduzem** acesso, ACTIVE→INACTIVE/BLOCKED) **When** esta story é implementada **Then** permanecem **síncronos**, sem aprovação — só `enable`/`unblock` (que **aumentam** acesso) passam pela cadeia. Confirma o mesmo princípio que a Story 3.4 aplica a Perfil (reduzir não exige aprovação, aumentar exige).

## Tasks / Subtasks

- [ ] Task 1: Generalizar `DecideLoginApprovalRequestUseCaseBean` por `requestType` (AC: 2, 4, 5)
  - [ ] Em `DecideLoginApprovalRequestUseCaseBean` (Story 3.2), o bloco que hoje só chama `login.approve(reasonId)`/`login.reject(reasonId)` passa a despachar por `request.getRequestType()`:
    ```java
    LoginStatusHistory history = switch (request.getRequestType()) {
        case CREATE_LOGIN -> decision == APPROVED ? login.approve(reasonId) : login.reject(reasonId);
        case REACTIVATE_LOGIN -> decision == APPROVED
                ? (login.getStatus() == LoginStatus.INACTIVE ? login.activate(reasonId) : login.enable(reasonId))
                : null; // rejeição de reativação não muda o Login — AC 5
        case CHANGE_PROFILE -> throw new IllegalStateException("Story 3.4"); // guarda temporária, substituída lá
    };
    ```
    Se `history != null`, salva `login` + `history` e grava `OutboxEvent` só quando o resultado for `ACTIVE` (aprovação de `CREATE_LOGIN` ou `REACTIVATE_LOGIN`) — rejeição nunca dispara Saga.
  - [ ] `login.getStatus() == INACTIVE ? activate() : enable()` só é válido porque, até a decisão, o Login **não mudou de estado** (AC 1) — o `status` no momento da decisão ainda é o mesmo de quando a solicitação foi aberta. Não precisa guardar "de onde veio" na `LoginApprovalRequest`.

- [ ] Task 2: `enable`/`unblock` deixam de transicionar direto — passam a abrir `LoginApprovalRequest` (AC: 1, 3)
  - [ ] Em `etc/api/organization/ScosOrganization_Login.yml`, `PUT /v1/logins/{id}/enable` (linha ~155-184) e `PUT /v1/logins/{id}/unblock` (linha ~249-278): atualizar `description` — de "reativa login... status muda para ACTIVE" para algo como "UC-141/UC-064 - Abre solicitação de reativação (Epic 3, cadeia de 3 níveis). O Login só transita para ACTIVE quando a solicitação é aprovada (`PUT /v1/login-approval-requests/{id}/approve`)". Trocar `requestBody` de `LoginStatusTransitionRequest` para um schema novo, mais simples (sem `reasonId` — motivo agora é dado na decisão, Story 3.2 Task 8): `LoginReactivationRequest { observation?: string }` (`observation` opcional, texto livre do solicitante, não confundir com o motivo formal do catálogo).
  - [ ] **Não** tocar em `activateLogin`/`inactivateLogin` como nome de operação — o `operationId` continua `activateLogin`/`unblockLogin`, só o comportamento (e o schema do corpo) muda.
  - [ ] Em `LoginServiceBean`/novo Use Case `RequestLoginReactivationUseCase(Bean)` (`usecase/access/login/`): carrega o `Login`, valida estado atual `IN (INACTIVE, BLOCKED)` (senão `422`, reaproveitando `SCOS_LOGIN_013` — transição inválida), valida que **não existe** `LoginApprovalRequest` `PENDING` para esse `loginId` (`LoginApprovalRequestRepository.findByLoginIdAndStatus`) — se existir, `422` com novo código `SCOS_LOGIN_015` ("Já existe uma solicitação de aprovação pendente para este Login."). Resolve `firstLevelFor`/monta `LoginApprovalRequest` (`requestType=REACTIVATE_LOGIN`, `escalationPolicy=INDEFINITE`, `requestedByLogin`=chamador), persiste. **Não** muda `login.status` — ele já está `INACTIVE`/`BLOCKED`, permanece assim (AC 1).
  - [ ] `LoginDelegate.activateLogin`/`unblockLogin` (Story 3.1, hoje sem `@Override`) ganham implementação aqui, chamando `RequestLoginReactivationUseCase` — **não** `Login.activate()`/`enable()` direto.
  - [ ] Adicionar `SCOS_LOGIN_015` em `ExceptionCodeError` (`422`) + mensagens PT-BR/EN.

- [ ] Task 3: Guarda de escopo — `disable`/`block` continuam síncronos (AC: 6)
  - [ ] **Não** tocar em `inactivateLogin`/`blockLogin` (`PUT .../disable`, `PUT .../block`) — continuam usando `LoginStatusTransitionRequest{reasonId}` e transição direta via `Login.inactivate()`/`Login.disable()`, sem `LoginApprovalRequest`. Implementá-los (ficaram sem `@Override` na Story 3.1) é **permitido** nesta story já que reaproveita o padrão síncrono já existente noutras entidades (Company/Employee `disable`), mas **não é o foco** — só implementar se sobrar tempo, sem inventar aprovação onde o épico não pede.
  - [ ] **Não** aplicar a guarda "1 solicitação pendente por vez" (Task 2) a `disable`/`block` — eles não criam `LoginApprovalRequest`, então não competem com uma reativação pendente da mesma forma. Mas: **decisão de design a confirmar com o usuário** — se um Login tem uma reativação `PENDING` e RH aciona `disable`/`block` nele (ele está `INACTIVE`/`BLOCKED`, então `disable`/`block` já falhariam por transição inválida vinda do próprio `Login.inactivate()`/`disable()` — `SCOS_LOGIN_013` cobre isso automaticamente, sem trabalho extra).

## Dev Notes

### Contexto crítico — leia antes de implementar

**Esta story é pequena de propósito — quase tudo já foi construído na 3.2.** O trabalho real é: (a) generalizar 1 `switch` no Use Case de decisão (Task 1) e (b) trocar o corpo/comportamento de 2 endpoints que hoje são síncronos (Task 2). Não recrie `LoginApprovalChainResolver`, `BusinessDayCalculator` ou o job de escalonamento — são agnósticos a `requestType` desde que a 3.2 os implementou corretamente.

**Rejeitar uma reativação é diferente de rejeitar uma criação.** Na Story 3.2 (`CREATE_LOGIN`), rejeitar leva a `LoginStatus.REJECTED` — um estado terminal novo, porque não havia "estado anterior" para o Login voltar (ele nunca chegou a existir de verdade). Aqui (`REACTIVATE_LOGIN`), o Login **já tinha** um estado (`INACTIVE`/`BLOCKED`) antes do pedido — rejeitar só significa "continua como estava", sem histórico novo de status (a `LoginApprovalRequest` em si registra `status=REJECTED`, isso já é auditoria suficiente; não force uma linha de `LoginStatusHistory` sem mudança real de status).

**Por que `enable`/`unblock` perdem o `reasonId` do corpo, mas `disable`/`block` mantêm.** Segue exatamente o mesmo raciocínio da Story 3.1 AC 2 (criação perde `reasonActivateId` porque o motivo de ativação passa a fazer sentido só na aprovação, não na solicitação) — aplicado agora às 2 rotas que também levam a `ACTIVE` por aprovação. `disable`/`block` continuam sem aprovação (AC 6), então continuam pedindo o motivo na hora, como sempre pediram.

### Onde cada peça vai (camadas)

- `usecase/access/login/DecideLoginApprovalRequestUseCaseBean.java` (Story 3.2): generaliza o `switch` por `requestType` (Task 1).
- `etc/api/organization/ScosOrganization_Login.yml`: `enable`/`unblock` trocam `LoginStatusTransitionRequest` por `LoginReactivationRequest` novo; descrições atualizadas (Task 2).
- `usecase/access/login/RequestLoginReactivationUseCase(Bean).java` (novo).
- `api/delegate/login/LoginDelegate.java` (Story 3.1): `activateLogin`/`unblockLogin` ganham `@Override`.
- `shared/exception/ExceptionCodeError.java`: `+SCOS_LOGIN_015`.
- Nenhuma mudança em `disable`/`block`, `LoginApprovalChainResolver`, job de escalonamento, ou schema de banco.

### Testing Standards

- `DecideLoginApprovalRequestUseCaseBeanTest` (Story 3.2, estendido): novos cenários — `REACTIVATE_LOGIN` aprovado a partir de `INACTIVE` (chama `activate`); a partir de `BLOCKED` (chama `enable`); `REACTIVATE_LOGIN` rejeitado (Login permanece no estado, sem `LoginStatusHistory` novo, sem `OutboxEvent`).
- `RequestLoginReactivationUseCaseBeanTest` novo: sucesso a partir de `INACTIVE`/`BLOCKED`; falha se já `ACTIVE`/`PENDING_APPROVAL`/`REJECTED` (transição inválida); falha se já existe `LoginApprovalRequest` `PENDING` (`SCOS_LOGIN_015`).
- Integração: `LoginControllerTest`, bloco `enable`/`unblock` reescrito — não assume mais `204` com transição imediata; assume `204` com Login inalterado + nova `LoginApprovalRequest` `PENDING` visível via `GET /v1/login-approval-requests`.

### Project Structure Notes

- Nenhum pacote novo — tudo em `usecase/access/login/` e `domain/access/login/` já criados pelas Stories 3.1/3.2.

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Epic 3 / Story 3.3] — Given/When/Then originais.
- [Source: etc/api/organization/ScosOrganization_Login.yml:155-278] — `enable`/`disable`/`block`/`unblock` atuais, síncronos, base do redesenho (só `enable`/`unblock` mudam).
- [Source: _bmad-output/implementation-artifacts/3-1-criacao-login-vinculado-funcionario.md] — `Login.activate()`/`enable()`/`inactivate()`/`disable()` já existentes (não desta story — pré-existentes no código antes até do Epic 3), reaproveitados sem alteração de assinatura.
- [Source: _bmad-output/implementation-artifacts/3-2-cadeia-aprovacao-criacao-login.md] — `LoginApprovalRequest`, `LoginApprovalChainResolver`, `DecideLoginApprovalRequestUseCaseBean`, `Login.approve()`/`reject()` — toda a infraestrutura reaproveitada por esta story, sem duplicar.

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List

## Change Log

| Data | Descrição |
|------|-----------|
| 2026-08-16 | Story criada — Epic 3, reativação de Login (`enable`/`unblock`) passa a exigir a mesma cadeia de aprovação da criação (Story 3.2), reaproveitando toda a infraestrutura sem duplicar. `disable`/`block` permanecem síncronos (reduzem acesso, não exigem aprovação). |
