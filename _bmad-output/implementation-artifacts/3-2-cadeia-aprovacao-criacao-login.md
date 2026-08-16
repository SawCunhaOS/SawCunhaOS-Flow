---
baseline_commit: b72b2dcc0f63997fe313658e358e68dc506dd645
---

# Story 3.2: Cadeia de Aprovação de Criação de Login

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como Supervisor ou Gerente do Departamento,
Eu quero aprovar ou rejeitar uma solicitação de Login,
Para garantir que ninguém ganha acesso sem o aval de outra pessoa.

## Acceptance Criteria

1. **Given** um Login `PENDING_APPROVAL` recém-criado (Story 3.1) **When** a criação é concluída **Then** o sistema abre, na mesma transação, uma `LoginApprovalRequest` com `requestType=CREATE_LOGIN`, `escalationPolicy=INDEFINITE`, `currentLevel` resolvido pela cadeia (ver AC 2), `requestedByLoginId` = quem criou, `status=PENDING`, `slaDeadline` = 1 dia útil a partir de agora.
2. **Given** um Login tipo `EMPLOYEE` **When** o sistema resolve o nível inicial da cadeia **Then** segue `SUPERVISOR → MANAGER (Gerente do Departamento) → SYSTEM_ACCESS_GROUP`, pulando um nível quando o Funcionário resolvido para aquele nível está ausente (sem supervisor cadastrado; departamento sem `manager`) **And**, para Login `EXTERNAL`/`SERVICE` (sem Funcionário vinculado), a cadeia pula direto para `SYSTEM_ACCESS_GROUP` (FR-26) — **este branch é infraestrutura genérica reaproveitada pelo Epic 4** (`Depende de: Epic 3 — mecanismo já precisa existir`, `epics.md`), não algo que esta story expõe por endpoint próprio: FR-26/27 (criação de Login `EXTERNAL`/`SERVICE`, aprovação de Perfil) são do Epic 4, não Epic 3 — corrigido nesta sessão após revisão cruzada com os demais épicos (Story 3.1 também ajustada, `POST /v1/logins` saiu de lá).
3. **Given** um aprovador tenta decidir (`approve`/`reject`) uma solicitação onde o Funcionário resolvido para o nível atual é ele mesmo **When** chama a ação **Then** a API rejeita com `422` **And** a única exceção é a válvula de última instância — um aprovador com a permissão `APPROVE_SYSTEM_ACCESS` pode decidir mesmo sendo o resolvido, mas a decisão grava `isExceptionSelfApproval=true`, sempre auditável separadamente.
4. **Given** 1 dia útil sem decisão no nível atual **When** um job agendado varre solicitações `PENDING` com `slaDeadline` vencido **Then** escala para o próximo nível da cadeia (`SUPERVISOR→MANAGER→SYSTEM_ACCESS_GROUP`), atualizando `currentLevel`, o campo `*_ESCALATED_AT` correspondente e um novo `slaDeadline` **And** se já estiver em `SYSTEM_ACCESS_GROUP` e o SLA vencer, a solicitação permanece pendente nesse nível (sem próximo nível para escalar — política `INDEFINITE` não cancela sozinha).
5. **Given** o Funcionário é desligado enquanto seu Login segue `PENDING_APPROVAL` **When** o aprovador tenta decidir **Then** o sistema reverifica o status do Funcionário no momento da decisão (não só na criação) **And** bloqueia a aprovação (`422`) se o Funcionário já não estiver `ACTIVE`.
6. **Given** nenhuma notificação existe ainda (Motor de Notificação é Etapa 3/P2) **When** o aprovador quer agir **Then** consulta solicitações pendentes via `GET /v1/login-approval-requests` (filtrável por `status`), sem depender de aviso proativo.
7. **Given** uma solicitação é aprovada **When** a decisão é registrada **Then** o Login transita de `PENDING_APPROVAL` para `ACTIVE`, grava `LoginStatusHistory` (`reasonActivateId` agora informado na decisão, não na criação — Story 3.1 AC 2) e dispara a Saga Keycloak (grava `OutboxEvent`, tópico Keycloak, sem dispatcher real — mesma guarda de escopo da Story 3.1) **And**, se rejeitada, o Login transita para `REJECTED` (estado terminal, sem histórico de reversão).
8. **Given** hoje não existe nenhuma API para definir o Gerente do Departamento — `MANAGER_ID` existe no banco desde a Etapa 1 (`scos_department_manager.yml`), mas nunca foi exposto; sem isso, o nível `MANAGER` da cadeia nunca resolve de verdade em produção **When** esta story é implementada **Then** `PUT /v1/departments/{id}` (`updateDepartment`, já existente, `UPDATE_DEPARTMENT`) ganha `managerId` opcional no corpo (definir/trocar/remover o gerente, `null` remove) **And** `GET /v1/departments/{id}`/`GET /v1/departments` passam a retornar `managerId` no `Department`.
9. **Given** RH quer consultar o histórico de decisões de um Login específico, ou saber quem pode decidir hoje no nível `SYSTEM_ACCESS_GROUP` **When** aciona `GET /v1/login-approval-requests?loginId=X` (filtro novo, além de `status`) ou `GET /v1/login-approval-requests/system-access-approvers` (novo) **Then** o primeiro devolve todas as solicitações (pendentes e decididas) daquele Login especificamente **And** o segundo devolve a lista de Logins `ACTIVE` que hoje têm a permissão `APPROVE_SYSTEM_ACCESS` (via `vw_login_context`/`vw_authority_response`, mesma fonte que o `@PreAuthorize` já usa — não uma nova forma de calcular permissão).
10. **Given** não existe Motor de Notificação (AC 6) — sem ele, o único jeito de um aprovador saber que precisa agir é olhando a solicitação e reconhecendo que é ele **When** consulta `GET /v1/login-approval-requests` ou `GET /v1/login-approval-requests/{id}` **Then** a resposta inclui quem é o aprovador resolvido para o `currentLevel` de cada solicitação (`SUPERVISOR`/`MANAGER` → nome/id do Funcionário resolvido; `SYSTEM_ACCESS_GROUP` → `null`, esse nível não resolve uma pessoa específica, ver Story 3.2 Dev Notes) **And** esse valor é calculado na hora da consulta (via `LoginApprovalChainResolver`), nunca guardado — o organograma pode mudar entre a abertura da solicitação e a consulta.

## Tasks / Subtasks

- [ ] Task 1: Estender `SCOS_LOGIN_APPROVAL_REQUEST` — coluna `REQUEST_TYPE` (AC: 1)
  - [ ] Novo changeSet em `organization/flow-organization-resources/src/main/resources/db/changelog/organization/v1.0.0/tables/scos_login_approval_request.yml` (mesmo arquivo, novo `changeSet` ao final — **nunca** editar o `changeSet` de 2026-07-18 já aplicado):
    ```yaml
      - changeSet:
          id: <data-implementação>-Samuel.Cunha-XXX
          author: Samuel.Cunha
          comment: "Adiciona REQUEST_TYPE em SCOS_LOGIN_APPROVAL_REQUEST (Epic 3 — discrimina CREATE_LOGIN/REACTIVATE_LOGIN/CHANGE_PROFILE, todos reaproveitando a mesma tabela/cadeia)"
          changes:
            - addColumn:
                tableName: SCOS_LOGIN_APPROVAL_REQUEST
                schemaName: scos
                columns:
                  - column:
                      name: REQUEST_TYPE
                      type: varchar(30)
                      constraints:
                        nullable: false
          rollback:
            - dropColumn:
                tableName: SCOS_LOGIN_APPROVAL_REQUEST
                schemaName: scos
                columnName: REQUEST_TYPE
    ```
    Sem `defaultValue` — toda linha nova (a partir desta story) grava `REQUEST_TYPE` explicitamente; não há linha pré-existente (tabela vazia, criada em 2026-07-18, ainda sem nenhum produtor).
  - [ ] Novo `changeSet` em `organization/flow-organization-resources/src/main/resources/db/changelog/organization/checks/checks.yml` (mesmo arquivo, ao final, **não** editar os `ALTER TABLE` de migrações já aplicadas):
    ```yaml
      - changeSet:
          id: <data-implementação>-Samuel.Cunha-YYY
          author: Samuel.Cunha
          comment: "CHECK de REQUEST_TYPE em SCOS_LOGIN_APPROVAL_REQUEST (Epic 3)"
          changes:
            - sql:
                sql: >-
                  ALTER TABLE scos.scos_login_approval_request
                    ADD CONSTRAINT chk_login_approval_request_type CHECK (request_type IN ('CREATE_LOGIN', 'REACTIVATE_LOGIN', 'CHANGE_PROFILE'));
          rollback:
            - sql:
                sql: ALTER TABLE scos.scos_login_approval_request DROP CONSTRAINT IF EXISTS chk_login_approval_request_type;
    ```
    `CHANGE_PROFILE` já entra no `CHECK` agora (a Story 3.4 é quem grava esse valor, mas o `CHECK` nasce completo para não precisar de outra migração de constraint depois — `ALTER ... ADD CONSTRAINT` de novo seria mais uma migração evitável).
  - [ ] **Pré-requisito descoberto nesta análise, sem o qual `OutboxEvent` não grava:** `OutboxEvent.topic` é `@ManyToOne` `NOT NULL` pra `OutboxTopic` (FK) — não existe hoje nenhuma linha semeada em `SCOS_OUTBOX_TOPIC` pra Keycloak, em nenhum lugar do projeto (`etc/database/seed_data.sql` auditado, sem menção). Como esta story (Task 8) é a **primeira** a de fato inserir um `OutboxEvent`, adicionar em `etc/database/seed_data.sql`:
    ```sql
    INSERT INTO scos.SCOS_OUTBOX_TOPIC (TOPIC, BACKEND, TARGET_SYSTEM, DEFAULT_MAX_RETRIES, ACTIVE, UPDATED_AT, USER_AT)
    VALUES ('KEYCLOAK_LOGIN_SYNC', 'DIRECT_API', 'keycloak', 3, true, NOW(), 'seed')
    ON CONFLICT DO NOTHING;
    ```
    **Decisão de design a confirmar com o usuário:** nome do tópico (`KEYCLOAK_LOGIN_SYNC`) e `BACKEND=DIRECT_API` são um palpite razoável (sem dispatcher ainda, qualquer backend serve de placeholder) — ajustar quando o dispatcher real for desenhado. Também adicionar a mesma linha no seed usado pelos testes de integração (`@Sql` de setup do `ScosOrganizationTestUtil`, mesmo arquivo/prática já usada por `ReasonInactivate`/etc.) — sem isso, todo teste de aprovação que chega a `ACTIVE` falha ao gravar `OutboxEvent`.

- [ ] Task 2: `Department.manager` — campo Java + API de gerenciamento (coluna `MANAGER_ID` já existe no banco desde 2026-07-18, nunca foi exposta) (AC: 2, 8)
  - [ ] Em `domain/corporate/department/internal/Department.java`, adicionar:
    ```java
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MANAGER_ID")
    private Employee manager;
    ```
    Import novo: `br.com.sawcunhaos.organization.domain.corporate.employee.internal.Employee`, `jakarta.persistence.JoinColumn`. **Não** criar changeSet — a coluna e a FK (`FK_MANAGER_ID_SCOS_DEPARTMENT`) já existem (`scos_department_manager.yml`, 2026-07-18).
  - [ ] **Sem API pra definir isso, o nível `MANAGER` nunca resolve em produção** (`department.manager` fica sempre `null`) — reaproveitar o endpoint de update já existente em vez de criar um novo:
    - Em `etc/api/organization/ScosOrganization_Department-Position.yml`, schema `UpdateDepartmentRequest` (linha ~609-620) ganha `managerId` **opcional** (`type: integer, format: int64, nullable: true` — não entra em `required`, já que `code`/`description` continuam obrigatórios e `managerId` é independente). `description` de `updateDepartment` (linha ~100) atualiza para mencionar o gerente.
    - Schema `Department` (linha ~570-581) ganha `managerId` (nullable) na resposta — `GET /v1/departments`/`GET /v1/departments/{id}` passam a expor.
    - Em `DepartmentServiceBean.update(...)` (já existente desde o Epic 1), passa a aceitar `managerId` no `DepartmentInput` e resolver/gravar `manager` (reaproveitar `EmployeeService`/`EmployeeQueryRepository` já existente pra resolver o `Employee` por id — **sem** validar que o gerente pertence ao próprio departamento ou é ativo além do que as FKs já garantem; se quiser essa regra de negócio adicional, é decisão fora desta story). `managerId=null` no corpo remove o gerente atual (seta `manager=null`).
    - **Não** criar endpoint dedicado (`PUT /v1/departments/{id}/manager`) — reaproveitar `updateDepartment` é mais simples e consistente com o resto do projeto (Department já atualiza `code`/`description` num único `PUT`).

- [ ] Task 3: Enums novos do agregado `LoginApprovalRequest` (AC: 1, 2, 4, 7)
  - [ ] `domain/access/login/internal/LoginApprovalRequestLevel.java`: `SUPERVISOR, MANAGER, SYSTEM_ACCESS_GROUP` (mesma ordem/nomes do `CHECK chk_login_approval_request_level`).
  - [ ] `domain/access/login/internal/LoginApprovalRequestEscalationPolicy.java`: `INDEFINITE, AUTO_CANCEL` (mesmos nomes do `CHECK chk_login_approval_request_policy`).
  - [ ] `domain/access/login/internal/LoginApprovalRequestStatus.java`: `PENDING, APPROVED, REJECTED, CANCELLED` (mesmos nomes do `CHECK chk_login_approval_request_status`).
  - [ ] `domain/access/login/internal/LoginApprovalRequestType.java`: `CREATE_LOGIN, REACTIVATE_LOGIN, CHANGE_PROFILE` (mesmos nomes do `CHECK` da Task 1).

- [ ] Task 4: Entidade `LoginApprovalRequest` + `LoginApprovalRequestRepository` (AC: 1, 4)
  - [ ] `domain/access/login/internal/LoginApprovalRequest.java` — espelha exatamente `scos_login_approval_request.yml` (Task 1 inclusa): `id`, `login` (`@ManyToOne` → `Login`), `requestedByLogin` (`@ManyToOne` nullable → `Login`), `decidedByLogin` (`@ManyToOne` nullable → `Login`), `currentLevel` (`LoginApprovalRequestLevel`), `escalationPolicy` (`LoginApprovalRequestEscalationPolicy`), `status` (`LoginApprovalRequestStatus`), `requestType` (`LoginApprovalRequestType`), `isExceptionSelfApproval` (`boolean`), `slaDeadline`/`supervisorNotifiedAt`/`managerEscalatedAt`/`systemGroupEscalatedAt`/`decidedAt` (`Instant` — coluna `TIMESTAMPTZ`, ver regra de tipos temporais do projeto). Estende `BaseEntity`, `@Auditable`.
    - Métodos de domínio (não persistem, devolvem o novo estado para o Service salvar — mesmo padrão de `Login.activate()`/`Company` transitions):
      - `escalate(LoginApprovalRequestLevel nextLevel, Instant now, Instant newDeadline)`: atualiza `currentLevel`, o campo `*_ESCALATED_AT` do nível alcançado e `slaDeadline`. Lança `ScosException` se `status != PENDING`.
      - `approve(Login decidedByLogin, boolean isException, Instant now)`: seta `status=APPROVED`, `decidedByLogin`, `decidedAt=now`, `isExceptionSelfApproval=isException`. Lança se `status != PENDING`.
      - `reject(Login decidedByLogin, boolean isException, Instant now)`: idem, `status=REJECTED`.
  - [ ] `domain/access/login/internal/LoginApprovalRequestRepository.java` — `extends BaseJpaRepository<LoginApprovalRequest, Long>, JpaSpecificationExecutor<...>, QuerydslPredicateExecutor<...>`. Métodos: `Optional<LoginApprovalRequest> findByLoginIdAndStatus(Long loginId, LoginApprovalRequestStatus status)` (para achar a solicitação pendente de um Login — usada pela decisão e por outras stories), `Page<LoginApprovalRequest> findAllFiltered(LoginApprovalRequestStatus status, Pageable pageable)` (`default`, mesmo padrão de `findAllFiltered` já convencionado — se `status` vier nulo, `findAll(pageable)`; senão, predicate simples `qLoginApprovalRequest.status.eq(status)`, sem precisar de `Predicates` dedicada por ser só 1 filtro sem `BooleanBuilder` — ver Story 2.6, critério de escopo), `List<LoginApprovalRequest> findAllByStatusAndSlaDeadlineBefore(LoginApprovalRequestStatus status, Instant deadline)` (consumida pelo job de escalonamento, Task 7 — reaproveita o índice parcial `idx_scos_login_approval_request_sla_scan`, já existe no banco).

- [ ] Task 5: `LoginApprovalChainResolver` — resolve o nível/aprovador (AC: 2, 3)
  - [ ] Criar `domain/access/login/service/LoginApprovalChainResolver.java` (classe de domínio, não `@Service` Spring — função pura reaproveitável por Use Cases e pelo job), com:
    - `Optional<LoginApprovalRequestLevel> firstLevelFor(Login login)`: para `login.type == EMPLOYEE`, tenta `SUPERVISOR`; se `login.employee.supervisor == null`, tenta `MANAGER` (`login.employee.position.department.manager`); se também ausente, `SYSTEM_ACCESS_GROUP`. Para `login.type != EMPLOYEE`, sempre `SYSTEM_ACCESS_GROUP` (FR-26). Retorna vazio só se algo estiver estruturalmente quebrado (não deveria acontecer — `SYSTEM_ACCESS_GROUP` sempre resolve, por definição, para "qualquer titular da permissão").
    - `Optional<LoginApprovalRequestLevel> nextLevelAfter(LoginApprovalRequestLevel current, Login login)`: mesma lógica de "pular ausente", começando do nível seguinte a `current`. `SUPERVISOR→MANAGER→SYSTEM_ACCESS_GROUP→(vazio, não escala mais)`.
    - `Optional<Employee> resolveApproverEmployee(LoginApprovalRequestLevel level, Login login)`: para `SUPERVISOR`/`MANAGER`, devolve o `Employee` resolvido (supervisor ou gerente do departamento); para `SYSTEM_ACCESS_GROUP`, devolve vazio — **esse nível não resolve uma pessoa específica**, resolve uma permissão (`APPROVE_SYSTEM_ACCESS`, ver Task 6). Essa é a decisão de design mais importante da story — documentada em Dev Notes.
  - [ ] Criar `LoginApprovalChainResolverTest.java` (JUnit 5 + Mockito ou puro, dependendo se a classe usa alguma dependência injetada — se for função pura sobre entidades já carregadas, teste sem mock, só montando `Employee`/`Department`/`Position` em memória): cobrir supervisor presente; supervisor ausente → cai pra manager; manager também ausente → cai pra `SYSTEM_ACCESS_GROUP`; `EXTERNAL`/`SERVICE` → direto `SYSTEM_ACCESS_GROUP`.

- [ ] Task 6: Permissões novas — `GET_LOGIN_APPROVAL_REQUEST`, `DECIDE_LOGIN_APPROVAL_REQUEST`, `APPROVE_SYSTEM_ACCESS` (AC: 3, 6, 7)
  - [ ] Em `ScosOrganizationPermission.java`, novo grupo `"Access"`/`"Login Approval Request"`: `GET_LOGIN_APPROVAL_REQUEST` (ver pendentes), `DECIDE_LOGIN_APPROVAL_REQUEST` (chamar `approve`/`reject` — gate grosso do endpoint, qualquer holder pode *chamar*, a regra de "é o aprovador certo" é de negócio, não de permissão).
  - [ ] `APPROVE_SYSTEM_ACCESS` é permissão **separada**, mesmo grupo — representa literalmente o "grupo" `APPROVE_SYSTEM_ACCESS` do FR-23/26/27. Quem a possui: (a) pode decidir quando `currentLevel == SYSTEM_ACCESS_GROUP` **mesmo sem ser um "aprovador resolvido"** (esse nível não resolve pessoa) e (b) pode usar a válvula de auto-aprovação de exceção (AC 3) em qualquer nível.
  - [ ] **Decisão de design a confirmar com o usuário:** `DECIDE_LOGIN_APPROVAL_REQUEST` é necessária em todo Login `ACTIVE` (todo mundo pode ser supervisor/gerente um dia) ou deveria ser condicionada a ter subordinados/departamento? Dado que o projeto não tem conceito de "perfil automático por hierarquia", a saída mais simples e consistente com o resto do sistema é conceder `DECIDE_LOGIN_APPROVAL_REQUEST` a qualquer Perfil que RH decidir (padrão de permissão comum, sem automatismo) — a checagem real de elegibilidade (é você o supervisor/gerente resolvido?) é 100% de negócio, na Task 8. Isso é o mesmo modelo dos demais `x-authorize` do projeto (gate grosso + regra fina no Service).
  - [ ] Atualizar `messages_geotemporal_permission.properties`/`_en` com as 3 novas entradas.

- [ ] Task 7: Contrato OpenAPI — endpoints de aprovação (AC: 6, 7, 9, 10) — **novo arquivo, contrato antes do código**
  - [ ] Criar `etc/api/organization/ScosOrganization_LoginApprovalRequest.yml` (arquivo próprio, mesmo padrão de outros domínios com peso próprio — ou adicionar ao `ScosOrganization_Login.yml` existente; **decisão de design a confirmar com o usuário**: este story assume arquivo separado por coesão, mas o projeto não tem um padrão explícito de "1 arquivo por agregado" — `ScosOrganization_Login.yml` já mistura Login/Profile/Resource/System num só arquivo. Ajustar se o usuário preferir manter tudo no arquivo existente).
  - [ ] `GET /v1/login-approval-requests` — lista paginada, filtros opcionais `status` **e `loginId`** (novo — permite `GET /v1/login-approval-requests?loginId=X` para o histórico completo, pendente+decidido, de um Login específico; sem `loginId`, lista o sistema inteiro, como antes) (`x-authorize: [GET_LOGIN_APPROVAL_REQUEST]`). Resposta inclui `id`, `loginId`, `requestType`, `currentLevel`, `status`, `escalationPolicy`, `slaDeadline`, `decidedByLoginId`, `decidedAt`, `createdAt`, **`currentApprover`** (novo — objeto `{employeeId, employeeName}` ou `null` quando `currentLevel=SYSTEM_ACCESS_GROUP`, AC 10) (`decidedByLoginId`/`decidedAt` novos na resposta — sem eles, "histórico" não mostra quem decidiu nem quando).
  - [ ] `GET /v1/login-approval-requests/{id}` — detalhe, mesmo corpo de resposta (incluindo `currentApprover`) (`x-authorize: [GET_LOGIN_APPROVAL_REQUEST]`).
  - [ ] `GET /v1/login-approval-requests/system-access-approvers` — **novo**, lista os Logins `ACTIVE` que hoje têm a permissão `APPROVE_SYSTEM_ACCESS` (consulta direta em `VwAuthorityResponseRepository`/`vw_login_context` filtrando `permission='APPROVE_SYSTEM_ACCESS'` — mesma fonte de verdade que o `@PreAuthorize` já usa, não uma segunda forma de calcular quem tem a permissão). Resposta: lista de `LoginSummary` (`id`, `login`, `type`, `status` — schema já existente em `ScosOrganization_Login.yml`). `x-authorize: [GET_LOGIN_APPROVAL_REQUEST]` (mesma permissão de consulta das demais rotas deste recurso — **não** inventar uma permissão nova só pra essa rota).
  - [ ] `PUT /v1/login-approval-requests/{id}/approve` — corpo `{reasonId}` (referencia `ReasonActivate`, mesmo catálogo que a criação de Login usava antes — Story 3.1 AC 2 moveu o motivo pra cá). `x-authorize: [DECIDE_LOGIN_APPROVAL_REQUEST]`. `204`.
  - [ ] `PUT /v1/login-approval-requests/{id}/reject` — corpo `{reasonId, observation}` (referenciar qual catálogo `Reason*` faz sentido para rejeição — **não existe catálogo `ReasonReject`** hoje; decisão de design: reaproveitar `ReasonInactivate` (mesmo "motivo de não seguir ativo") ou criar catálogo novo — **confirmar com o usuário antes de implementar**, esta story assume reaproveitar `ReasonInactivate` por não introduzir agregado novo sem necessidade clara). `x-authorize: [DECIDE_LOGIN_APPROVAL_REQUEST]`. `204`.
  - [ ] `LoginApprovalRequestRepository.findAllFiltered` (Task 4) ganha o parâmetro `loginId` (nullable) — mesmo padrão de atalho já convencionado no projeto: sem `status` nem `loginId`, `findAll(pageable)`; com qualquer um dos dois, monta o predicate correspondente.
  - [ ] `VwAuthorityResponseRepository` (já existe, `access/login/internal/`, hoje só tem `findByLogin` — **sem** `QuerydslPredicateExecutor`, só `BaseJpaRepository`). `permissions` em `VwAuthorityResponse` é `text[]` (`@JdbcTypeCode(SqlTypes.ARRAY)`) — containment de array não é trivial em JPQL/QueryDSL puro. Adicionar via `@Query` nativa (mesmo padrão já usado por `CompanyRepository.wouldCreateCycleFlag`/`hasActiveDescendantFlag` para o que QueryDSL não cobre bem):
    ```java
    @Query(value = "SELECT * FROM scos.vw_authority_response WHERE :permission = ANY(permissions) AND status = 'ACTIVE'", nativeQuery = true)
    List<VwAuthorityResponse> findAllByPermission(@Param("permission") String permission);
    ```
    Reaproveitar a view/entidade já mapeada — **não** criar uma nova.
  - [ ] Adicionar `LoginApprovalRequestLevel`/`Status`/`EscalationPolicy`/`RequestType` como schemas `enum` espelhando os `CHECK` do banco (Task 1/3).

- [ ] Task 8: Use Cases — criação injeta `LoginApprovalRequest`; consulta; decisão (aprovar/rejeitar) (AC: 1, 2, 3, 5, 6, 7, 9, 10)
  - [ ] Em `CreateLoginUseCaseBean`/`CreateEmployeeLoginUseCaseBean` (Story 3.1), **adicionar** a abertura da `LoginApprovalRequest` na mesma transação da criação do Login: `LoginApprovalChainResolver.firstLevelFor(login)`, monta `LoginApprovalRequest` (`requestType=CREATE_LOGIN`, `escalationPolicy=INDEFINITE`, `requestedByLogin` = login autenticado no momento da chamada — resolver via `ScosUserAuthentication.findUserAuthentication()` → `LoginRepository.findByLogin`, `slaDeadline` = `BusinessDayCalculator.plusBusinessDays(clock.instant(), 1)`, Task 9), persiste via `LoginApprovalRequestRepository.save`. **Isso significa voltar à Story 3.1 e adicionar essa chamada** — coordenar como uma única unidade de trabalho se a 3.1 ainda não foi implementada quando esta story começar; se já foi, é um Task extra nela.
  - [ ] `usecase/access/login/DecideLoginApprovalRequestUseCase(Bean)` — `execute(Long requestId, DecisionType decision, Long reasonId, String observation)`. Fluxo: carrega `LoginApprovalRequest`; resolve `currentLoginResolved = LoginRepository.findByLogin(scosUserAuthentication.findUserAuthentication())`; resolve o aprovador esperado via `LoginApprovalChainResolver.resolveApproverEmployee(request.currentLevel, request.login)`; checa elegibilidade (AC 3):
    ```
    isResolvedApprover = approverEmployee.isPresent() && approverEmployee.get().equals(currentLogin.employee)
    isSystemGroupHolder = currentPermissions.contains(APPROVE_SYSTEM_ACCESS)  // já disponível via ScosUserAuthentication/contexto de permissão, mesmo mecanismo do @PreAuthorize
    isSelfApproval = request.login.equals(currentLogin)  // a pessoa está decidindo a própria solicitação

    if (isSelfApproval && !isSystemGroupHolder) → 422 (AC 3, bloqueio)
    if (!isResolvedApprover && !isSystemGroupHolder) → 422 (não é o aprovador certo nem tem a válvula)
    isException = isSelfApproval  // grava sempre que foi a válvula quem decidiu, mesmo não sendo auto-aprovação "clássica"
    ```
    Reverifica `request.login.employee` (se `EMPLOYEE`) via `EmployeeService.findById` → `status == ACTIVE` (AC 5), senão `422`.
  - [ ] Adicionar 2 métodos novos em `Login.java` (mesmo padrão de `activate()`/`inactivate()`/`disable()`/`enable()` já existentes — validam a transição e devolvem `LoginStatusHistory`, não persistem):
    ```java
    /** Aprova o login a partir de PENDING_APPROVAL. Não persiste. @throws ScosException SCOS_LOGIN_013 se o status atual não for PENDING_APPROVAL. */
    public LoginStatusHistory approve(Long reasonActivateId) {
        if (this.status != LoginStatus.PENDING_APPROVAL) {
            throw new ScosException(ExceptionCodeError.SCOS_LOGIN_013);
        }
        return LoginStatusHistory.builder()
                .login(this)
                .status(LoginStatus.ACTIVE)
                .reasonActivate(ReasonActivate.builder().id(reasonActivateId).build())
                .build();
    }

    /** Rejeita o login a partir de PENDING_APPROVAL. Estado terminal, sem reversão. Não persiste. @throws ScosException SCOS_LOGIN_013 se o status atual não for PENDING_APPROVAL. */
    public LoginStatusHistory reject(Long reasonInactivateId) {
        if (this.status != LoginStatus.PENDING_APPROVAL) {
            throw new ScosException(ExceptionCodeError.SCOS_LOGIN_013);
        }
        return LoginStatusHistory.builder()
                .login(this)
                .status(LoginStatus.REJECTED)
                .reasonInactivate(ReasonInactivate.builder().id(reasonInactivateId).build())
                .build();
    }
    ```
    Chamar `login.approve(reasonId)` (não mutar `login.status` direto — mantém o padrão do agregado, `status` só muda através de um método validado). No `DecideLoginApprovalRequestUseCaseBean`: se `APPROVED`, `LoginStatusHistory history = login.approve(reasonId)`, salva `login` + `history`, grava `OutboxEvent` (Saga Keycloak, guarda de escopo — só a linha, sem dispatcher). Se `REJECTED`, `LoginStatusHistory history = login.reject(reasonId)`, salva os dois, sem `OutboxEvent` (Login rejeitado nunca chega a existir no Keycloak). Em ambos os casos, chama também `request.approve(...)`/`request.reject(...)` (na `LoginApprovalRequest`, Task 4) para fechar a solicitação.
  - [ ] **Nota para a Story 3.3 (fora desta story):** `login.approve()`/`login.reject()` só cobrem a transição a partir de `PENDING_APPROVAL` (fluxo `CREATE_LOGIN`). Quando a 3.3 redirecionar `enable`/`unblock` pela mesma cadeia (`requestType=REACTIVATE_LOGIN`), a decisão precisa reaproveitar os métodos **já existentes** `activate()` (a partir de `INACTIVE`) e `enable()` (a partir de `BLOCKED`) — não `approve()`. `DecideLoginApprovalRequestUseCaseBean` desta story só sabe lidar com `requestType=CREATE_LOGIN`; generalizar o dispatch por `requestType`/`login.status` é tarefa explícita da Story 3.3 (ver Task 1 dela).
  - [ ] **Não** implementar reenvio de notificação nem qualquer mecanismo assíncrono de aviso — AC 6 é só o `GET` de consulta.
  - [ ] `usecase/access/login/LoginApprovalRequestApiMapper.java` (`static`, mesmo padrão de `LoginApiMapper`, Story 3.1) — `toApiLoginApprovalRequest(LoginApprovalRequest request, Optional<Employee> resolvedApprover)` monta o corpo de resposta (Task 7), incluindo `currentApprover` (AC 10): `resolvedApprover.map(e -> new CurrentApprover(e.getId(), e.getName())).orElse(null)` — `null` quando `currentLevel=SYSTEM_ACCESS_GROUP` (o resolver não devolve pessoa nesse nível, por design — Dev Notes) ou quando o nível resolvido está de fato vago (não deveria acontecer, mas o mapper não deve quebrar se acontecer).
  - [ ] `usecase/access/login/FindLoginApprovalRequestUseCase(Bean).java` — `execute(@NonNull Long id)`: carrega a `LoginApprovalRequest`, resolve `LoginApprovalChainResolver.resolveApproverEmployee(request.currentLevel, request.login)` **na hora** (não lê nada persistido — AC 10 é explícito: "calculado na hora da consulta, nunca guardado"), monta via `LoginApprovalRequestApiMapper`.
  - [ ] `usecase/access/login/FindAllLoginApprovalRequestUseCase(Bean).java` — `execute(@NonNull PaginationFilter, LoginApprovalRequestStatus status, Long loginId)`: pagina via `LoginApprovalRequestRepository.findAllFiltered` (Task 7), resolve o aprovador **de cada item da página** (não da tabela inteira — só o necessário) e mapeia. **Custo aceito conscientemente:** resolve N+1 chamadas ao `LoginApprovalChainResolver` por página (1 por solicitação) — cada uma é leitura em memória de relações já carregadas (`login.employee.supervisor`, `.position.department.manager`), não é N+1 de query ao banco (as relações `@ManyToOne` `LAZY` já vêm no mesmo `SELECT` se o `Login` for carregado com `JOIN FETCH`, ou N+1 de query pequena se não — **decisão de design**: se a paginação for grande e isso pesar, é o primeiro lugar a otimizar com `JOIN FETCH`/projeção; não otimizar preventivamente agora, ponytail: medir antes de complicar).
  - [ ] `usecase/access/login/FindSystemAccessApproversUseCase(Bean).java` — `execute()`: `VwAuthorityResponseRepository.findAllByPermission("APPROVE_SYSTEM_ACCESS")` (Task 7), mapeia pra `LoginSummary` (reaproveitar `LoginApiMapper.toApiLoginSummary`? Os tipos são diferentes — `VwAuthorityResponse` não é `LoginOutput`. **Não** forçar reuso do mapper de `Login` só por semelhança de shape; montar `LoginSummary` direto a partir de `VwAuthorityResponse` aqui mesmo, é 4 campos, não precisa de mapper dedicado).
  - [ ] `LoginDelegate` (Story 3.1) ganha 5 `@Override` novos: `getAllLoginApprovalRequests`, `getLoginApprovalRequestById`, `getSystemAccessApprovers`, `approveLoginApprovalRequest`, `rejectLoginApprovalRequest` — ou `LoginApprovalRequestDelegate` próprio, mesma decisão de arquivo único-vs-separado da Task 7.

- [ ] Task 9: `BusinessDayCalculator` + job de escalonamento SLA (AC: 1, 4)
  - [ ] Criar `domain/access/login/service/BusinessDayCalculator.java` (ou `shared/utils/` se fizer mais sentido de compartilhamento futuro — **decisão de design**: esta story coloca em `domain/access/login`, único consumidor hoje; mover depois se algum outro agregado precisar): `Instant plusBusinessDays(Instant from, int days, ZoneId zone)` — pula sábado/domingo (`DayOfWeek.SATURDAY`/`SUNDAY` do `java.time`, não confundir com `corporate.position.internal.DayOfWeek`, enum próprio do projeto para jornada de trabalho). **Sem calendário de feriados** — não existe em nenhum lugar do projeto hoje, não inventar aqui; fora de escopo, sinalizado como limitação conhecida.
  - [ ] Criar `LoginApprovalEscalationJob.java` (`@Component`, `@Scheduled(cron = "...")` — **primeiro uso de `@Scheduled` no projeto inteiro**; confirmar que `@EnableScheduling` está presente em algum `@Configuration` do módulo `boot` — se não estiver, adicionar). Frequência sugerida: a cada 15-30 min (mesma ordem de grandeza do refresh das views de autoridade, 30 min) — **decisão de design a confirmar com o usuário**, o épico só diz "SLA de 1 dia útil", não a frequência do polling.
    ```java
    @Scheduled(fixedRate = ..., initialDelay = ...)
    @Transactional
    void run() {
        List<LoginApprovalRequest> overdue = repository.findAllByStatusAndSlaDeadlineBefore(PENDING, clock.instant());
        for (var request : overdue) {
            resolver.nextLevelAfter(request.getCurrentLevel(), request.getLogin())
                .ifPresent(next -> request.escalate(next, clock.instant(), calculator.plusBusinessDays(clock.instant(), 1, zone)));
            // se nextLevelAfter vazio (já em SYSTEM_ACCESS_GROUP), não faz nada — AC 4
        }
    }
    ```
  - [ ] Criar `LoginApprovalEscalationJobTest.java` com `Clock.fixed(...)` determinístico (padrão já convencionado, Story 0.2) — sem `sleep`, sem depender de tempo real.

- [ ] Task 10: Guarda de escopo (AC: 1-7)
  - [ ] **Não** implementar Story 3.3 (`enable`/`unblock` passando pela cadeia), 3.4 (perfil), 3.5 (retorno automático) ou 3.6 (aprovação de Perfil/Resource) — esta story só cobre `requestType=CREATE_LOGIN`. Os outros `requestType` (`REACTIVATE_LOGIN`, `CHANGE_PROFILE`) já entram no `CHECK` (Task 1) para não precisar de outra migração de constraint, mas nenhum código desta story os produz.
  - [ ] **Não** implementar o dispatcher/consumidor do Outbox — só a gravação da linha na aprovação (AC 7). Mesma guarda da Story 3.1.
  - [ ] **Não** criar calendário de feriados — `BusinessDayCalculator` só pula fim de semana.
  - [ ] **Não** implementar Motor de Notificação — AC 6 é só consulta via `GET`.

## Dev Notes

### Contexto crítico — leia antes de implementar

**Esta story é o motor reaproveitado por 3.3/3.4/3.5/3.6.** `LoginApprovalRequest`, `LoginApprovalChainResolver`, `BusinessDayCalculator` e `LoginApprovalEscalationJob` — tudo criado aqui — são a base que as 4 stories seguintes do Epic 3 (e a nova 3.6, gap encontrado nesta sessão) reaproveitam sem recriar. Ao implementar, pense em `requestType` como o único "conhecimento específico de fluxo" que entra nessa tabela genérica — a cadeia de aprovação em si (3 níveis, SLA, escalonamento) é agnóstica ao motivo da solicitação.

**Decisão de design mais importante da story: o nível `SYSTEM_ACCESS_GROUP` não resolve uma pessoa.** O FR-23/26 fala em "grupo `APPROVE_SYSTEM_ACCESS`" — mas o projeto não tem, em lugar nenhum, um mecanismo de "listar membros de um grupo Keycloak" (confirmado por auditoria: `ScosUserAuthentication.findUserAuthentication()` só devolve o username do chamador, nunca resolve outra pessoa; não há client de Admin API do Keycloak em uso). A saída adotada: `SYSTEM_ACCESS_GROUP` = "qualquer titular da permissão `APPROVE_SYSTEM_ACCESS`, verificada no momento da decisão" — o mesmo mecanismo `x-authorize`/`@PreAuthorize` que já governa todo o resto do sistema, sem precisar de uma nova capacidade de "consultar grupo". **Isso é uma decisão de arquitetura desta story, não uma tradução literal do épico — sinalizada explicitamente para você confirmar antes do `dev-story`, junto com as demais marcadas "decisão de design" nas Tasks.**

**A válvula de auto-aprovação de exceção (AC 3) usa a MESMA permissão `APPROVE_SYSTEM_ACCESS`.** O FR-23 fala em "mitigação de última instância" sem detalhar quem pode acioná-la — a leitura mais consistente com o resto do desenho (nível 3 = titular da permissão decide por qualquer motivo) é: só quem já tem `APPROVE_SYSTEM_ACCESS` pode decidir a própria solicitação, e quando o faz, `isExceptionSelfApproval=true` fica marcado para sempre (auditoria já cobre isso via `@Auditable` da entidade + o próprio campo).

**Cadeia "pula nível ausente" — o que conta como ausente, o que NÃO foi implementado.** Implementado: sem supervisor cadastrado (`employee.supervisor == null`); departamento sem gerente (`department.manager == null`). **Não** implementado (fora de escopo, não inventado): dedupe entre níveis (ex.: se o supervisor E o gerente forem a mesma pessoa, ambos são perguntados — o épico só fala em pular por ausência ou conflito **com o solicitante**, não conflito entre níveis). Se esse comportamento for indesejado, é ajuste de escopo a decidir com o usuário, não uma correção silenciosa aqui.

**Reverificação de status do Funcionário (AC 5) é só na DECISÃO, não na criação.** A Story 3.1 já valida `ACTIVE` na criação (Task 6 dela). Esta story adiciona a segunda checagem, no momento de `approve`/`reject` — o Funcionário pode ter sido desligado entre a criação e a decisão (às vezes dias depois, dado o SLA e possível escalonamento).

**`Clock` obrigatório em toda lógica de tempo** (regra já estabelecida na Story 0.2) — `slaDeadline`, escalonamento, `BusinessDayCalculator` — nunca `Instant.now()` direto em domínio/service. Bean `Clock` já existe (`ClockConfig`), reaproveitar.

### Onde cada peça vai (camadas)

- `organization/flow-organization-resources/.../v1.0.0/tables/scos_login_approval_request.yml` + `.../checks/checks.yml`: novos changeSets aditivos (Task 1).
- `domain/corporate/department/internal/Department.java`: `+manager` (Task 2).
- `etc/api/organization/ScosOrganization_Department-Position.yml`: `Department`/`UpdateDepartmentRequest` ganham `managerId` (Task 2).
- `domain/corporate/department/service/DepartmentServiceBean.java` + `dto/DepartmentInput.java` (já existentes desde o Epic 1): `+managerId` (Task 2).
- `domain/access/login/internal/`: 4 enums novos + `LoginApprovalRequest` + `LoginApprovalRequestRepository` (Tasks 3-4).
- `domain/access/login/internal/VwAuthorityResponseRepository.java` (já existe): `+findAllByPermission` nativa (Task 7).
- `domain/access/login/service/`: `LoginApprovalChainResolver`, `BusinessDayCalculator`, `LoginApprovalEscalationJob` (Tasks 5, 9).
- `infrastructure/enumaration/ScosOrganizationPermission.java`: `+3` permissões (Task 6).
- `etc/api/organization/ScosOrganization_LoginApprovalRequest.yml` (novo, ou seção nova no arquivo existente — decisão a confirmar) (Task 7).
- `usecase/access/login/`: `DecideLoginApprovalRequestUseCase(Bean)`, `FindLoginApprovalRequestUseCase(Bean)`, `FindAllLoginApprovalRequestUseCase(Bean)`, `FindSystemAccessApproversUseCase(Bean)`, `LoginApprovalRequestApiMapper` novos; `CreateLoginUseCaseBean`/`CreateEmployeeLoginUseCaseBean` (Story 3.1) ganham a abertura da solicitação (Task 8).
- `api/delegate/login/LoginDelegate.java` (Story 3.1) ganha os 5 novos `@Override` (`getAllLoginApprovalRequests`/`getLoginApprovalRequestById`/`getSystemAccessApprovers`/`approveLoginApprovalRequest`/`rejectLoginApprovalRequest`) — ou um `LoginApprovalRequestDelegate` próprio, espelhando a decisão de arquivo único-vs-separado da Task 7.
- `api/delegate/department/DepartmentDelegate.java` (já existe desde o Epic 1): `updateDepartment` passa `managerId` adiante (Task 2).

### Testing Standards

- `LoginApprovalChainResolverTest`: função pura, sem mock necessário se as entidades forem montadas em memória (`Employee.builder()...build()` sem persistir).
- `LoginApprovalEscalationJobTest`: `Clock.fixed`, sem `sleep`, determinístico (padrão Story 0.2/0.3).
- `DecideLoginApprovalRequestUseCaseBeanTest`: cobrir os 4 cenários do AC 3 (aprovador certo decide; não-aprovador sem `APPROVE_SYSTEM_ACCESS` → 422; auto-aprovação sem a permissão → 422; auto-aprovação com a permissão → sucesso + `isExceptionSelfApproval=true`) + AC 5 (Funcionário desligado entre criação e decisão → 422).
- `FindLoginApprovalRequestUseCaseBeanTest`/`FindAllLoginApprovalRequestUseCaseBeanTest`: `currentApprover` presente para `SUPERVISOR`/`MANAGER`; `null` para `SYSTEM_ACCESS_GROUP` (AC 10).
- `DepartmentServiceBeanTest` (já existe): novo cenário — `update` com `managerId` grava o gerente; `managerId=null` remove.
- Integração: novo bloco em `LoginControllerTest` ou `LoginApprovalRequestControllerTest` — depende da decisão de arquivo único-vs-separado (Task 7); cobrir os 2 `GET` de listagem (incluindo `loginId` isolando o histórico de 1 Login) + `GET system-access-approvers` (Login com `APPROVE_SYSTEM_ACCESS` aparece, sem ela não aparece — precisa de Testcontainers real, já que depende da materialized view) + 2 `PUT`, `401`/`403`, fluxo feliz completo (`POST` cria → `GET` lista pendente → `PUT approve` → Login `ACTIVE`). `DepartmentControllerTest` (já existe): novo cenário de `PUT` com `managerId`.

### Project Structure Notes

- Pacotes novos: `domain/access/login/service` (job + resolver + calculator), nenhum pacote Maven novo.
- `LoginApprovalRequest` fica em `internal` (mesma fronteira de agregado que `Login`) — não é um agregado à parte, é uma entidade auxiliar do agregado `Login` (decisão consistente com `LoginStatusHistory`/`CompanyStatusHistory`, que também vivem dentro do agregado que descrevem, mesmo em pacote `access.status` compartilhado).

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Epic 3 / Story 3.2] — Given/When/Then originais.
- [Source: organization/flow-organization-resources/.../v1.0.0/tables/scos_login_approval_request.yml] — schema completo da tabela (auditado nesta sessão), base de `LoginApprovalRequest`.
- [Source: organization/flow-organization-resources/.../checks/checks.yml:31-39] — `CHECK`s de `level`/`policy`/`status`/`requester`, base dos enums e da regra `AUTO_CANCEL⟹requestedByLoginId NULL` (Story 3.5 usa essa regra).
- [Source: organization/flow-organization-resources/.../v1.0.0/indexes/login_approval_request.yml] — índices já existentes, incluindo o parcial de SLA (`idx_scos_login_approval_request_sla_scan`, `WHERE status='PENDING'`) reaproveitado pelo job (Task 9).
- [Source: organization/flow-organization-resources/.../v1.0.0/tables/scos_department_manager.yml] — `MANAGER_ID` já existe no banco desde 2026-07-18; só falta o campo Java + API (Task 2).
- [Source: etc/api/organization/ScosOrganization_Department-Position.yml:97-123, 570-620] — `updateDepartment`/`UpdateDepartmentRequest`/`Department` atuais (código/descrição só); reaproveitados para expor `managerId` em vez de criar rota nova.
- [Source: organization/flow-organization-domain/.../corporate/department/service/DepartmentServiceBean.java:72] — `update(DepartmentInput)` já existente desde o Epic 1, estendido para aceitar `managerId`.
- [Source: organization/flow-organization-domain/.../corporate/employee/internal/Employee.java:94-100] — `supervisor`/`position` já existentes, base da resolução de cadeia.
- [Source: organization/flow-organization-domain/.../access/login/internal/VwAuthorityResponse.java, VwAuthorityResponseRepository.java] — view agregada por Login (`permissions text[]`), reaproveitada para `GET .../system-access-approvers` via `@Query` nativa (mesmo padrão de `CompanyRepository.wouldCreateCycleFlag`).
- [Source: organization/flow-organization-foundation ScosUserAuthentication.java] — `findUserAuthentication()` só devolve `String` (username do chamador); confirma que "resolver outra pessoa por grupo Keycloak" não é uma capacidade existente (base da decisão de design do nível `SYSTEM_ACCESS_GROUP`).
- [Source: organization/flow-organization-domain/.../config/ClockConfig.java] — bean `Clock` já existente, reaproveitado por `BusinessDayCalculator`/job.
- [Source: _bmad-output/implementation-artifacts/3-1-criacao-login-vinculado-funcionario.md] — Story anterior no Epic 3: cria `Login`/`LoginStatus.PENDING_APPROVAL`/`CreateLoginUseCase(Bean)` que esta story estende (Task 8); também cria `LoginApiMapper`/`toApiLoginSummary`, reaproveitado como referência de forma (não de tipo) por `LoginApprovalRequestApiMapper`.
- AC 10 (`currentApprover`) e a extensão da Task 8 (Use Cases de consulta) vieram de uma revisão pedida pelo usuário após a criação inicial das 6 stories do Epic 3 — sem esse campo, `GET /v1/login-approval-requests` não respondia "quem decide agora", o mesmo problema que a notificação ausente (AC 6) já deixava difícil.

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List

## Change Log

| Data | Descrição |
|------|-----------|
| 2026-08-16 | Story criada — Epic 3, motor da cadeia de aprovação (3 níveis, SLA, escalonamento). `LoginApprovalRequest` novo (estende schema já existente), `Department.manager` (Java, coluna já existia), `BusinessDayCalculator`, job de escalonamento (primeiro `@Scheduled` do projeto). Decisões de design documentadas explicitamente para confirmação: resolução do nível `SYSTEM_ACCESS_GROUP` via permissão (não grupo Keycloak), catálogo de motivo de rejeição (reaproveita `ReasonInactivate`), arquivo OpenAPI único vs. separado, frequência do job. |
| 2026-08-16 | AC 8/9 adicionados a pedido do usuário, após pergunta direta sobre gaps: (1) API de gerenciamento do Gerente do Departamento — sem ela, o nível `MANAGER` nunca resolvia em produção; reaproveita `updateDepartment` já existente em vez de rota nova; (2) filtro `loginId` em `GET /v1/login-approval-requests` para consulta de histórico por Login; (3) `GET /v1/login-approval-requests/system-access-approvers`, novo, lista quem tem `APPROVE_SYSTEM_ACCESS` hoje via `vw_authority_response`. |
| 2026-08-16 | AC 10 adicionado — revisão pedida pelo usuário confirmou que a resposta de `GET /v1/login-approval-requests*` não expunha quem é o aprovador resolvido no nível atual. `currentApprover` adicionado ao schema, calculado na hora da consulta (nunca persistido). Use Cases de consulta (`FindLoginApprovalRequestUseCase`, `FindAllLoginApprovalRequestUseCase`, `FindSystemAccessApproversUseCase`) e `LoginApprovalRequestApiMapper` adicionados à Task 8 (antes só cobria criação/decisão). |
| 2026-08-16 | Clarificado no AC 2: o branch `EXTERNAL`/`SERVICE → SYSTEM_ACCESS_GROUP` do `LoginApprovalChainResolver` é infraestrutura genérica pro Epic 4 reaproveitar — FR-26/27 não são desta story (achado da validação cruzada com os demais épicos, mesma sessão que corrigiu a Story 3.1). |
| 2026-08-16 | Task 1 ganhou o seed de `SCOS_OUTBOX_TOPIC` (Keycloak) — achado na mesma validação cruzada: sem essa linha, `OutboxEvent.topic` (FK `NOT NULL`) impediria qualquer gravação, e esta story é a primeira do projeto a gravar um `OutboxEvent` de verdade. |
