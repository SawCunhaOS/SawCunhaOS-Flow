---
baseline_commit: b72b2dcc0f63997fe313658e358e68dc506dd645
---

# Story 3.3: Aprovação de Reativação de Login

Status: review

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

- [x] Task 1: Generalizar `DecideLoginApprovalRequestUseCaseBean` por `requestType` (AC: 2, 4, 5)
  - [x] Em `DecideLoginApprovalRequestUseCaseBean` (Story 3.2), o bloco que hoje só chama `login.approve(reasonId)`/`login.reject(reasonId)` passa a despachar por `request.getRequestType()`:
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
  - [x] `login.getStatus() == INACTIVE ? activate() : enable()` só é válido porque, até a decisão, o Login **não mudou de estado** (AC 1) — o `status` no momento da decisão ainda é o mesmo de quando a solicitação foi aberta. Não precisa guardar "de onde veio" na `LoginApprovalRequest`.

- [x] Task 2: `enable`/`unblock` deixam de transicionar direto — passam a abrir `LoginApprovalRequest` (AC: 1, 3)
  - [x] Em `etc/api/organization/ScosOrganization_Login.yml`, `PUT /v1/logins/{id}/enable` (linha ~155-184) e `PUT /v1/logins/{id}/unblock` (linha ~249-278): atualizar `description` — de "reativa login... status muda para ACTIVE" para algo como "UC-141/UC-064 - Abre solicitação de reativação (Epic 3, cadeia de 3 níveis). O Login só transita para ACTIVE quando a solicitação é aprovada (`PUT /v1/login-approval-requests/{id}/approve`)". Trocar `requestBody` de `LoginStatusTransitionRequest` para um schema novo, mais simples (sem `reasonId` — motivo agora é dado na decisão, Story 3.2 Task 8): `LoginReactivationRequest { observation?: string }` (`observation` opcional, texto livre do solicitante, não confundir com o motivo formal do catálogo).
  - [x] **Não** tocar em `activateLogin`/`inactivateLogin` como nome de operação — o `operationId` continua `activateLogin`/`unblockLogin`, só o comportamento (e o schema do corpo) muda.
  - [x] Em `LoginServiceBean`/novo Use Case `RequestLoginReactivationUseCase(Bean)` (`usecase/access/login/`): carrega o `Login`, valida estado atual `IN (INACTIVE, BLOCKED)` (senão `422`, reaproveitando `SCOS_LOGIN_013` — transição inválida), valida que **não existe** `LoginApprovalRequest` `PENDING` para esse `loginId` (`LoginApprovalRequestRepository.findByLoginIdAndStatus`) — se existir, `422` com novo código `SCOS_LOGIN_019` (`SCOS_LOGIN_015`-`018` já estavam ocupados pela Story 3.2 — "Já existe uma solicitação de aprovação pendente para este Login."). Resolve `firstLevelFor`/monta `LoginApprovalRequest` (`requestType=REACTIVATE_LOGIN`, `escalationPolicy=INDEFINITE`, `requestedByLogin`=chamador), persiste. **Não** muda `login.status` — ele já está `INACTIVE`/`BLOCKED`, permanece assim (AC 1).
  - [x] `LoginDelegate.activateLogin`/`unblockLogin` (Story 3.1, hoje sem `@Override`) ganham implementação aqui, chamando `RequestLoginReactivationUseCase` — **não** `Login.activate()`/`enable()` direto.
  - [x] Adicionar `SCOS_LOGIN_019` em `ExceptionCodeError` (`422`) + mensagens PT-BR/EN.

- [x] Task 3: Guarda de escopo — `disable`/`block` continuam síncronos (AC: 6)
  - [x] **Não** tocar em `inactivateLogin`/`blockLogin` (`PUT .../disable`, `PUT .../block`) — continuam usando `LoginStatusTransitionRequest{reasonId}` e transição direta via `Login.inactivate()`/`Login.disable()`, sem `LoginApprovalRequest`. Implementá-los (ficaram sem `@Override` na Story 3.1) é **permitido** nesta story já que reaproveita o padrão síncrono já existente noutras entidades (Company/Employee `disable`), mas **não é o foco** — não implementado nesta story (YAGNI: nenhuma AC pede, fica para quando for necessário).
  - [x] **Não** aplicar a guarda "1 solicitação pendente por vez" (Task 2) a `disable`/`block` — eles não criam `LoginApprovalRequest`, então não competem com uma reativação pendente da mesma forma. Confirmado: se um Login tem uma reativação `PENDING` e RH aciona `disable`/`block` nele (ele está `INACTIVE`/`BLOCKED`), `disable`/`block` já falham por transição inválida vinda do próprio `Login.inactivate()`/`disable()` — `SCOS_LOGIN_013` cobre isso automaticamente, sem trabalho extra.

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

Claude Sonnet 5 (claude-sonnet-5)

### Debug Log References

Durante a validação, a suíte de integração (`LoginControllerTest`/`LoginApprovalRequestControllerTest`) esteve bloqueada por 4 causas raiz **pré-existentes no ambiente local, não introduzidas por esta story**, todas diagnosticadas e corrigidas:

1. **JAR obsoleto de `flow-organization-infrastructure` em `~/.m2`** — instalado antes da Story 3.2 finalizar as mensagens de permissão; faltava `GET_LOGIN_APPROVAL_REQUEST` no bundle `messages_organization_permission.properties`, causando `NoSuchMessageException` no startup (`ScosSystemRegistrationService`). Corrigido reinstalando o módulo.
2. **JAR obsoleto do módulo raiz `infrastructure`** (`br.com.sawcunhaos.flow:infrastructure`, declarado só no `pom.xml` da raiz do monorepo, fora do reactor `organization/`) — o `MessageConfiguration` instalado ainda usava o basename antigo `classpath:messages_permission` em vez do atual `classpath:messages_organization_permission`. Corrigido reinstalando o módulo.
3. **Volume Docker Postgres de teste desatualizado** (container `infra-postgresql-1`, criado ~34h antes, schema sem a coluna `REQUEST_TYPE` da Story 3.2) — Liquibase só roda na 1ª subida do container (singleton estático). Corrigido removendo container+volume (autorizado pelo usuário) para recriação limpa.
4. **JAR obsoleto de `flow-organization-resources`** (também module do `pom.xml` raiz, fora do reactor `organization/`) — changelog Liquibase empacotado sem o changeset que adiciona `REQUEST_TYPE`. Corrigido reinstalando o módulo.

Um 5º problema, este sim de código (contrato OpenAPI desta story): o corpo `LoginReactivationRequest` de `enable`/`unblock` ficou sem `x-jdempotentrequestpayload` no `requestBody` (a Story 3.1 marcava `reasonId`, removido nesta story). Sem isso, a chave de idempotência do jDempotent (Redis) considerava só o `id` do Login — chamadas repetidas ao mesmo Login (inclusive entre métodos de teste distintos) devolviam a resposta cacheada da 1ª, mascarando a execução real. Corrigido adicionando `x-jdempotentrequestpayload: true` no `requestBody` (nível correto, confirmado via `mustaches/bodyParams.mustache` do `scos-foundation-utils`) e usando `observation` único por teste. Um cenário adicional colidia com um teste pré-existente da Story 3.2 (`approve` com `reasonId=4`, sem campo para diferenciar) — resolvido com um novo motivo de catálogo dedicado (`SCOS_REASON_ACTIVATE` id=7, `LOGIN/REACTIVATION`).

### Completion Notes List

- Task 1: generalização do `switch` por `requestType` implementada em `LoginApprovalRequestServiceBean.decide()` (não em `DecideLoginApprovalRequestUseCaseBean` como o texto da story sugeria) — é ali que `login.approve()`/`reject()` já eram chamados; o Use Case Bean só traduz `DecisionType`→`LoginApprovalRequestStatus` e permanece inalterado. Decisão alinhada à convenção do projeto (regra de negócio no domain service).
- `SCOS_LOGIN_015` já estava ocupado (Story 3.2, "login duplicado"); o novo código de "solicitação pendente já existe" usa `SCOS_LOGIN_019` (015-018 também já ocupados).
- `enable`/`unblock` (`LoginDelegate.activateLogin`/`unblockLogin`) chamam `RequestLoginReactivationUseCase`, que delega para `LoginService.requestReactivation` (novo) — reaproveita o `openApprovalRequest` privado de `LoginServiceBean`, agora parametrizado por `LoginApprovalRequestType`.
- `disable`/`block` (Task 3) não foram implementados nesta story — permanecem sem `@Override`, exatamente como a Story 3.1 deixou. Confirmado que não é necessário nenhum trabalho extra para a guarda "reativação pendente bloqueia disable/block": `Login.inactivate()`/`disable()` já rejeitam com `SCOS_LOGIN_013` a partir de `INACTIVE`/`BLOCKED`.
- Testes: unitários (domain: `LoginServiceBeanTest`, `LoginApprovalRequestServiceBeanTest`; usecase: `RequestLoginReactivationUseCaseBeanTest`) cobrindo os cenários pedidos (ativação a partir de `INACTIVE`/`BLOCKED`, rejeição sem `LoginStatusHistory`/`OutboxEvent`, transição inválida, solicitação pendente duplicada). Integração: bloco novo em `LoginControllerTest` (`enable`/`unblock`, aprovação, rejeição, 422/404/403/401) usando 2 novos fixtures de seed (`scos-inactive` id=4, `scos-blocked` id=5) — não havia como alcançar esses estados via API já que `disable`/`block` não estão implementados.
- Revisão `ponytail-review` aplicada ao código de produção: nenhum achado (wrappers finos são fronteira de módulo obrigatória, não abstração especulativa).
- Suíte completa do módulo `boot` validada ao final: 500 testes (unitários + integração de todos os controllers) + 3 testes de consistência de permissões, 0 falhas.

### File List

- `etc/api/organization/ScosOrganization_Login.yml`
- `organization/flow-organization-shared/src/main/java/br/com/sawcunhaos/organization/shared/exception/ExceptionCodeError.java`
- `organization/flow-organization-shared/src/main/resources/scos_message_organization.properties`
- `organization/flow-organization-shared/src/main/resources/scos_message_organization_en.properties`
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/access/login/service/LoginApprovalRequestServiceBean.java`
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/access/login/service/LoginServiceBean.java`
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/access/login/specification/LoginService.java`
- `organization/flow-organization-domain/src/test/java/br/com/sawcunhaos/organization/domain/access/login/service/LoginApprovalRequestServiceBeanTest.java`
- `organization/flow-organization-domain/src/test/java/br/com/sawcunhaos/organization/domain/access/login/service/LoginServiceBeanTest.java`
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/access/login/RequestLoginReactivationUseCase.java` (novo)
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/access/login/RequestLoginReactivationUseCaseBean.java` (novo)
- `organization/flow-organization-usecase/src/test/java/br/com/sawcunhaos/organization/application/usecase/access/login/RequestLoginReactivationUseCaseBeanTest.java` (novo)
- `organization/flow-organization-api/src/main/java/br/com/sawcunhaos/organization/api/delegate/login/LoginDelegate.java`
- `organization/flow-organization-boot/src/test/java/br/com/sawcunhaos/organization/boot/api/login/LoginControllerTest.java`
- `organization/flow-organization-boot/src/test/resources/postgresql/setsup_database.sql`

## Change Log

| Data | Descrição |
|------|-----------|
| 2026-08-16 | Story criada — Epic 3, reativação de Login (`enable`/`unblock`) passa a exigir a mesma cadeia de aprovação da criação (Story 3.2), reaproveitando toda a infraestrutura sem duplicar. `disable`/`block` permanecem síncronos (reduzem acesso, não exigem aprovação). |
| 2026-08-17 | Implementação completa (Tasks 1-3): `switch` por `requestType` generalizado em `LoginApprovalRequestServiceBean.decide()`; `enable`/`unblock` passam a abrir `LoginApprovalRequest` via novo `RequestLoginReactivationUseCase`/`LoginService.requestReactivation`; `SCOS_LOGIN_019` adicionado. Corrigidos, no processo, 4 problemas de infraestrutura local pré-existentes (JARs `.m2` obsoletos de `flow-organization-infrastructure`, `infrastructure` raiz e `flow-organization-resources`; volume Docker Postgres de teste desatualizado) e 1 bug real de contrato (jDempotent sem `x-jdempotentrequestpayload` no `requestBody` de `enable`/`unblock`). Suíte completa do módulo `boot` (500 testes) e `ponytail-review` do código de produção: sem achados. |
