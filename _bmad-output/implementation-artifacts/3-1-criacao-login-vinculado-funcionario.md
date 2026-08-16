---
baseline_commit: b72b2dcc0f63997fe313658e358e68dc506dd645
---

# Story 3.1: Criação de Login Vinculado a Funcionário (mecânica base)

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como Analista de RH,
Eu quero criar um Login vinculado a um Funcionário,
Para que ele tenha uma credencial de acesso ao sistema — que só passa a valer depois de aprovado.

**Fora de escopo desta story:** criação de Login `EXTERNAL`/`SERVICE` (sem Funcionário) é FR-26, mapeado pelo próprio `epics.md` para o **Epic 4** ("Depende de: Epic 3 — mecanismo de Login/Perfil/aprovação já precisa existir"), não Epic 3 — corrigido nesta sessão após uma revisão cruzada com os demais épicos ter achado que uma versão anterior desta story invadia esse escopo. `POST /v1/logins` fica para a Story 4.1.

## Acceptance Criteria

1. **Given** um Funcionário ativo **When** RH cria um Login para ele via `POST /v1/employees/{employeeId}/logins` **Then** o Login nasce em estado `PENDING_APPROVAL` **And** não autentica nem é aceito em nenhuma chamada de API enquanto pendente (`LoginInactiveRule`/`LoginBlockedRule` continuam bloqueando `INACTIVE`/`BLOCKED`; `PENDING_APPROVAL` precisa da mesma proteção — ver Dev Notes).
2. **Given** hoje `CreateEmployeeLoginRequest` exige `reasonActivateId` e a descrição do endpoint diz "Inicia a Saga Keycloak" (implicando `ACTIVE` imediato) **When** esta story é implementada **Then** o contrato é corrigido: `reasonActivateId` sai do corpo de criação (motivo de ativação só existe quando a aprovação é decidida — Story 3.2, fora de escopo aqui) **And** a descrição passa a refletir que o Login nasce `PENDING_APPROVAL`, sem Saga Keycloak nesta etapa. `CreateLoginRequest` (EXTERNAL/SERVICE) **não** é tocado aqui — é a Story 4.1 que corrige seu contrato quando o implementar, mesma lógica "contrato antes do código" aplicada no momento certo.
3. **Given** `LoginStatus` hoje só tem `ACTIVE`/`INACTIVE`/`BLOCKED`, tanto no enum Java quanto no schema OpenAPI **When** esta story é implementada **Then** ambos ganham `PENDING_APPROVAL` **And** `LoginStatus` Java também ganha `REJECTED` — já previsto no `CHECK` do banco (`chk_login_status_history_status`, migração de 2026-07-19, aceita `PENDING_APPROVAL/ACTIVE/INACTIVE/BLOCKED/REJECTED`) mas usado só pela Story 3.2 (decisão de rejeição) — esta story só declara o valor no enum, não o utiliza ainda.
4. **Given** um Login aprovado (mecanismo da Story 3.2, fora desta story) **When** ele transita de `PENDING_APPROVAL` para `ACTIVE` **Then** só então dispara a Saga Keycloak (grava um `OutboxEvent`, não chama a Keycloak Admin API direto) — **nesta story**, a única obrigação é deixar o método de domínio pronto para ser chamado pela 3.2 (ver Dev Notes, guarda de escopo).
5. **Given** o código hoje usa uma permissão única (`UPDATE_LOGIN_STATUS`) para `block`/`unblock` **When** esta story é implementada **Then** `BLOCK_LOGIN` e `UNBLOCK_LOGIN` passam a existir em `ScosOrganizationPermission`, `x-authorize` de `/v1/logins/{id}/block` e `/v1/logins/{id}/unblock` migram para elas **And** `UPDATE_LOGIN_STATUS` é removida do enum de permissões (nenhum outro `x-authorize` a usa — confirmado por leitura de todo `ScosOrganization_Login.yml`).
6. **Given** nenhum Use Case/Delegate/Repository de `Login` existe hoje (só a entidade `Login`, `LoginStatus`, `LoginType`, 2 `BusinessRule` de estado e `LoginRepository.findByLogin`) **When** esta story é implementada **Then** cria a camada completa de Use Case de escrita (`CreateEmployeeLoginUseCase` + Bean) e o `LoginDelegate`, seguindo o padrão já usado por `Employee`/`Company` (Story 2.1/1.2) — `CreateLoginUseCase` (EXTERNAL/SERVICE) fica para a Story 4.1.
7. **Given** nenhuma story do roadmap inteiro (auditado em `epics.md` de ponta a ponta) implementa consulta de Login — sem isso, RH cria o Login (AC 1) mas não tem como descobrir depois "quais Logins este Funcionário tem" nem "qual o status deste Login" via API **When** esta story é implementada **Then** `GET /v1/logins/{id}` (`getLoginById`), `GET /v1/logins` (`getAllLogins`, filtros `type`/`status`/`employeeId` já contratados) e `GET /v1/employees/{employeeId}/logins` (`getAllEmployeeLogins`) passam a funcionar **And** `getLoginMe`/`getEmployeeLoginById`/demais rotas de Login continuam fora de escopo (AC 6/7 guarda de escopo, ver Dev Notes).

## Tasks / Subtasks

- [ ] Task 1: Corrigir o contrato — `LoginStatus` ganha `PENDING_APPROVAL`, criação de Employee Login perde `reasonActivateId` (AC: 1, 2, 3)
  - [ ] Em `etc/api/organization/ScosOrganization_Login.yml`, schema `LoginStatus` (linha ~841-846): adicionar `PENDING_APPROVAL` ao `enum` — **este schema é compartilhado** por todos os tipos de Login (`EMPLOYEE`/`EXTERNAL`/`SERVICE`), correto corrigir aqui de uma vez só, mesmo a criação de `EXTERNAL`/`SERVICE` sendo Epic 4.
  - [ ] `CreateEmployeeLoginRequest` (linha ~1026-1044): remover `reasonActivateId` de `required` e de `properties`. Atualizar `description` do schema removendo a menção a "motivo da criação".
  - [ ] `POST /v1/employees/{employeeId}/logins` (`createEmployeeLogin`, linha ~424-451): atualizar `description` — trocar "Inicia a Saga Keycloak" por algo como "UC-054 - Cria login em PENDING_APPROVAL. A Saga Keycloak só dispara quando a aprovação (Story 3.2) leva o Login a ACTIVE".
  - [ ] **Não** tocar em `CreateLoginRequest`/`POST /v1/logins` (`createLogin`) — mesmo bug de contrato (`reasonActivateId` presumindo `ACTIVE` imediato), mas é escopo da Story 4.1, que faz seu próprio "contrato antes do código" quando chegar a vez.
  - [ ] Rodar `mvn clean generate-sources` em `flow-organization-usecase` e `flow-organization-api` para confirmar que `CreateEmployeeLoginRequest` gerado não tem mais `reasonActivateId` e que `LoginStatus` gerado inclui `PENDING_APPROVAL`.

- [ ] Task 2: `LoginStatus` Java ganha `PENDING_APPROVAL`/`REJECTED`; regras de estado cobrem `PENDING_APPROVAL` (AC: 1, 3)
  - [ ] Em `domain/access/login/internal/LoginStatus.java`, adicionar `PENDING_APPROVAL` e `REJECTED` ao enum — ordem sugerida `PENDING_APPROVAL, ACTIVE, INACTIVE, BLOCKED, REJECTED` (mesma ordem do `CHECK` `chk_login_status_history_status` do banco, só para legibilidade — o enum Java não impõe ordem funcional).
  - [ ] Criar `domain/access/login/internal/rules/LoginPendingApprovalRule.java`, mesmo padrão exato de `LoginInactiveRule`/`LoginBlockedRule` (`@ScosRule(N)`, `BusinessRule<LoginStatus>`, retorna o novo código de erro quando `context == PENDING_APPROVAL`). Usar `@ScosRule(3)` (as duas existentes são `1`/`2`).
  - [ ] Adicionar `SCOS_LOGIN_014` em `ExceptionCodeError` (`422`, `SCOS_TITLE_BUSINESS_RULE_VIOLATION`) + mensagens PT-BR/EN em `scos_message_organization[_en].properties` ("O Login informado está pendente de aprovação.").
  - [ ] **Não** criar regra para `REJECTED` nesta story — `REJECTED` só passa a ser alcançável pela Story 3.2 (rejeição de aprovação); declarar o valor no enum agora evita migração de enum depois, mas o bloqueio de autenticação para esse estado é tarefa da 3.2.

- [ ] Task 3: Permissões — `BLOCK_LOGIN`/`UNBLOCK_LOGIN` substituem `UPDATE_LOGIN_STATUS` (AC: 5)
  - [ ] Em `ScosOrganizationPermission.java`, remover `UPDATE_LOGIN_STATUS` e adicionar `BLOCK_LOGIN`/`UNBLOCK_LOGIN` no mesmo grupo (`"Access"`, `"Login"`), mesma versão/data das demais entradas de Login (`"1.0.0"`, `"2026-07-15"` — ajustar para a data real de implementação se o padrão do arquivo pedir a data do commit).
  - [ ] Em `ScosOrganization_Login.yml`, `x-authorize` de `blockLogin` (linha ~244) vira `[BLOCK_LOGIN]`, de `unblockLogin` (linha ~275) vira `[UNBLOCK_LOGIN]`.
  - [ ] Atualizar `messages_permission.properties`/`_en`: remover a entrada de `UPDATE_LOGIN_STATUS`, adicionar `BLOCK_LOGIN`/`UNBLOCK_LOGIN` (PT-BR e EN, 1:1, mesmo padrão das outras 3 entradas de Login).
  - [ ] Rodar `mvn -pl flow-organization-infrastructure test -Dtest=PermissionsConsistencyTest -Denforcer.skip=true` para confirmar 1:1 entre `x-authorize` e enum.

- [ ] Task 4: `LoginRepository` — métodos necessários para escrita e consulta (AC: 6, 7)
  - [ ] Adicionar `boolean existsByLogin(String login)` (mesmo padrão de `PositionRepository.existsByCode`/`DepartmentRepository.existsByCode` — predicate simples sem `BooleanBuilder`, direto: `default boolean existsByLogin(String login) { return exists(QLogin.login1.login.eq(login)); }`, nome do campo QueryDSL gerado depende do nome do atributo `login` colidir com o nome da classe `Login`— **confirmar o nome real gerado pelo QueryDSL APT** ao implementar, pode vir como `login1` por colisão de nome).
  - [ ] **Não** duplicar `findByLogin` (já existe).
  - [ ] Para AC 7 (`getAllLogins`/`getAllEmployeeLogins`): criar `LoginPredicates.java` (mesmo padrão da Story 2.6 — classe `final` package-private em `internal`, métodos `static Predicate` usando `BooleanBuilder` só quando há composição condicional) com `predicateTypeAndStatusAndEmployeeId(LoginType type, LoginStatus status, Long employeeId)` (todos os 3 filtros opcionais, mesmo padrão de `EmployeeQueryRepository.findAllFiltered`/`predicateCompanyIdAndPositionIdAndStatus`). Adicionar `default Page<Login> findAllFiltered(LoginType type, LoginStatus status, Long employeeId, Pageable pageable)` em `LoginRepository` — atalho "nenhum filtro" no repositório, montagem condicional na `Predicates` (mesmo split já convencionado).
  - [ ] Criar `LoginPredicatesTest.java` (mesmo padrão da Story 2.6 — JUnit 5 puro, `predicate.toString()`).

- [ ] Task 5: `LoginService`/`LoginServiceBean` (domain) — criar Login em `PENDING_APPROVAL`; consultas (AC: 1, 4, 6, 7)
  - [ ] Criar `domain/access/login/dto/LoginInput.java`/`LoginOutput.java` (records, mesmo padrão de `EmployeeInput`/`EmployeeOutput`). `LoginOutput` carrega os campos do próprio `Login` (`id`, `login`, `externalId`, `type`, `status`, `profileId`, `employeeId`) **mais `profileCode`/`profileDescription`/`employeeName`, nullable — só preenchidos por `findById`, nunca por `findAll`** (ver Task abaixo — `toApiLoginSummary`, consumidor de `findAll`, nem usa esses 3 campos; buscá-los pra cada linha de uma página seria N+1 sem necessidade nenhuma, e o resto do código deste projeto nunca lê além de `.getId()` numa relação `LAZY` dentro de um mapper de lista — `EmployeeServiceBean.toEmployeeOutput` é a referência).
  - [ ] Criar `domain/access/login/specification/LoginService.java` (interface pública) com `LoginOutput create(@NonNull LoginInput input)` — `LoginInput` carrega `login`, `profileId`, `type` (`EMPLOYEE`/`EXTERNAL`/`SERVICE`), `employeeId` (nullable, só para `EMPLOYEE`). **Sem** `reasonActivateId`/motivo — não existe mais na criação (AC 2). Mais `LoginOutput findById(@NonNull Long id)` e `Page<LoginOutput> findAll(LoginType type, LoginStatus status, Long employeeId, @NonNull Pageable pageable)` (AC 7). **`create` aceita `type` genérico de propósito** — só o Use Case (`CreateEmployeeLoginUseCase`, Task 6) fica restrito a `EMPLOYEE` nesta story; a Story 4.1 reaproveita o mesmo `LoginService.create` pra `EXTERNAL`/`SERVICE`, sem duplicar a camada de domínio.
  - [ ] Criar `domain/access/login/service/LoginServiceBean.java` (`@Service` package-private): valida `existsByLogin` (`SCOS_LOGIN_00X` — reaproveitar `SCOS_LOGIN_001`/verificar mensagem exata antes de reusar, senão criar novo código), resolve `Profile`/`Employee` por id (reaproveitar `ProfileRepository`/`EmployeeQueryRepository` já existentes), monta `Login.builder().status(LoginStatus.PENDING_APPROVAL).type(...).profile(...).employee(...).login(...).build()`, persiste via `LoginRepository.save`.
    - **`findById` (1 linha, `getLoginById`)**: mapeia tudo, incluindo `profile.getCode()`/`profile.getDescription()`/`employee.getName()` — 1 linha só, o custo de inicializar 2 proxies `LAZY` é irrelevante.
    - **`findAll` (paginado, `getAllLogins`)**: mapeia **só** `id`/`login`/`type`/`status`/`profileId`/`employeeId` (`.getId()` de `profile`/`employee` — grátis, não inicializa o proxy, mesmo padrão de `EmployeeServiceBean.toEmployeeOutput`). `profileCode`/`profileDescription`/`employeeName` ficam `null` no `LoginOutput` retornado por `findAll` — **não são lidos por ninguém nesse caminho** (`toApiLoginSummary`, Task 7, não tem esses campos no schema `LoginSummary`). Ler `.getCode()`/`.getName()` por linha de uma página inicializaria 1-2 proxies `LAZY` por Login — N+1 sem nenhum consumidor do outro lado.
  - [ ] **Não** escrever `LoginStatusHistory` na criação — histórico de status hoje só é criado pelas transições (`activate`/`inactivate`/`disable`/`enable`, que exigem motivo). Criar um Login em `PENDING_APPROVAL` não é uma "transição" no sentido do método `Login.activate()` etc. (que parte de um estado existente) — é o estado inicial. Se a Story 3.2 precisar de uma linha de histórico "nasceu PENDING_APPROVAL", ela adiciona lá — não invente isso aqui.

- [ ] Task 6: Use Case + Delegate de criação (AC: 6)
  - [ ] `usecase/access/login/CreateEmployeeLoginUseCase(Bean)` — para `POST /v1/employees/{employeeId}/logins`, valida (via `EmployeeService.findById`) que o Funcionário existe e está `ACTIVE` antes de criar o Login (mesmo espírito de guarda que `RehireEmployeeUseCase`/`EmployeeServiceBean` já aplicam noutros fluxos — não inventar novo código de erro sem checar se já existe um `SCOS_EMPLOYEE_0XX` de "funcionário não está ativo").
  - [ ] `api/delegate/login/LoginDelegate implements LoginApiDelegate` — implementa **só** `createEmployeeLogin` (`@Override`, resto da interface gerada fica sem `@Override`, mesmo padrão de escopo parcial já usado em `EmployeeDelegate` nas Stories 2.1/2.3/2.5). Resposta `201` via `ScosComponents.yml#/components/responses/201_CREATED` (sem `data:` — conferir se `createEmployeeLogin` no YAML já usa esse `$ref` antes de mudar o Delegate; se sim, `return null` como os demais `201` do projeto). **`createLogin` (EXTERNAL/SERVICE) fica sem `@Override` — é a Story 4.1 quem implementa.**

- [ ] Task 7: Use Cases de consulta + `LoginApiMapper` (AC: 7) — fecha o gap "sem isso não dá pra descobrir o Login criado"
  - [ ] Criar `usecase/access/login/LoginApiMapper.java` (interface `static`, mesmo padrão de `CompanyApiMapper`/`EmployeeApiMapper`): `toApiLogin(LoginOutput)` → `api.dto.Login` (`id`, `login`, `keycloakId` ← `output.externalId()` — **nomes diferentes de propósito**, `externalId` é o nome de domínio, `keycloakId` é o nome do contrato, mesmo campo; `type`, `status`, `profile: ProfileSummary(profileId, profileCode, profileDescription)`, `employee: employeeId != null ? LoginEmployee(employeeId, employeeName) : null`); `toApiLoginSummary(LoginOutput)` → `api.dto.LoginSummary` (`id`, `login`, `type`, `status`, sem perfil/funcionário aninhado — mesmo padrão resumido de `Employees`/`CompanyApiMapper.toApiCompanies`, Stories 2.5/1.x).
  - [ ] Criar `usecase/access/login/FindLoginUseCase(Bean).java` — `execute(@NonNull Long id)` → `api.dto.Login`, chama `loginService.findById` + `LoginApiMapper.toApiLogin`.
  - [ ] Criar `usecase/access/login/FindAllLoginUseCase(Bean).java` — `execute(@NonNull PaginationFilter, LoginType type, LoginStatus status, Long employeeId)` → `GetAllLoginsResponse`, mesmo padrão de `FindAllEmployeeUseCaseBean` (Story 2.5): `PaginatioUtils.createPageable`/`createScosPaginated`, mapeia via `toApiLoginSummary`.
  - [ ] Criar `usecase/access/login/FindAllEmployeeLoginUseCase(Bean).java` — `execute(@NonNull Long employeeId, @NonNull PaginationFilter)` → `GetAllLoginsResponse`. **Não reimplementar** paginação/filtro/mapeamento — o corpo é 1 linha delegando pro `FindAllLoginUseCase` já injetado: `return findAllLoginUseCase.execute(paginationFilter, null, null, employeeId);`. Continua existindo como classe própria (mantém "1 Use Case por operação de API", rastreável 1:1 com `getAllEmployeeLogins`), só não duplica a lógica.
  - [ ] `LoginDelegate` (Task 6) ganha 3 `@Override` novos: `getLoginById`, `getAllLogins`, `getAllEmployeeLogins`.

- [ ] Task 8: Guarda de escopo (AC: 1, 4, 6, 7)
  - [ ] **Não** implementar `createLogin`/`CreateLoginUseCase` (`POST /v1/logins`, EXTERNAL/SERVICE) — é FR-26, mapeado pelo `epics.md` para o **Epic 4** (Story 4.1), não Epic 3. Fica sem `@Override` no `LoginDelegate`. Ver nota de escopo no topo da story.
  - [ ] **Não** implementar `getLoginMe`, `getEmployeeLoginById`, `updateLogin`, `enable`/`disable`/`block`/`unblock`, `status-history`, profile-de-login — todos ficam sem `@Override` no `LoginDelegate`, fora desta story. `getEmployeeLoginById` fica de fora deliberadamente (redundante com `getLoginById` — mesma consulta, só escopada por `employeeId` no path; não vale duplicar Use Case só por isso, mas se o dev-story achar barato implementar junto, não há problema).
  - [ ] **Não** implementar a Saga Keycloak (produção de `OutboxEvent`) nesta story — ela só faz sentido quando o Login vira `ACTIVE`, o que só acontece via aprovação (Story 3.2). Deixar isso pronto para a 3.2 chamar significa: o método de domínio que fará `ACTIVE`+`OutboxEvent` **não é escrito aqui**, é escrito na 3.2. Esta story só entrega `PENDING_APPROVAL`.
  - [ ] **Não** implementar `LoginApprovalRequest` — tabela já existe no banco (migração 2026-07-18), mas nenhuma linha é criada por esta story. A Story 3.2 é quem abre a solicitação de aprovação ao criar o Login (ou logo em seguida). **Decisão de design a confirmar com o usuário:** esta story deixa o Login "parado" em `PENDING_APPROVAL` sem nenhuma `LoginApprovalRequest` associada ainda — a 3.2 assume a responsabilidade de abrir a solicitação no mesmo fluxo de criação (provavelmente injetando `LoginApprovalRequest` na criação, não como job separado). Ver Dev Notes da Story 3.2.
  - [ ] **Não** tocar em `LoginType`, `Profile`, `Resource`, `System` — só leitura (resolver `profileId`, `employeeId`).
  - [ ] **Não** criar `ProfileService`/camada de aplicação de `Profile` — a Story 3.6 é quem faz isso de verdade (CRUD completo via aprovação). Esta story só lê `Profile` por baixo, achatado em `LoginOutput` (Task 5).

## Dev Notes

### Contexto crítico — leia antes de implementar

**Esta story é fundação pura — quase todo o agregado `Login` está por fazer.** Auditoria de código (nesta sessão) confirmou: só existem `Login`/`LoginStatus`/`LoginType`/`LoginRepository` (com 1 método) + 2 `BusinessRule`. Nenhum Use Case, Delegate, Service ou Repository além disso. Diferente do Epic 2 (onde Company/Employee já tinham bastante pronto), aqui **tudo** é novo — trate os "padrões de referência" citados (Employee/Company) como inspiração de forma, não como algo a estender.

**O contrato atual (`ScosOrganization_Login.yml`) foi escrito ANTES do detalhamento do fluxo de aprovação e está errado em pontos estruturais.** Achados desta auditoria, confirmados por leitura linha a linha do YAML de 1225 linhas:
- `LoginStatus` (schema) não tem `PENDING_APPROVAL` — só `ACTIVE`/`BLOCKED`/`INACTIVE`.
- `CreateEmployeeLoginRequest` (e, no mesmo padrão, `CreateLoginRequest` — mas esse é escopo da Story 4.1) exige `reasonActivateId` com descrição "primeira linha de status ACTIVE em LoginStatusHistory" — ou seja, o contrato **hoje assume que criar um Login já deixa ele ACTIVE**. Isso contradiz frontalmente a Epic 3 Story 3.1 original ("Login nasce em PENDING_APPROVAL"). Esta story corrige o contrato de `CreateEmployeeLoginRequest`: motivo de ativação sai da criação, entra na decisão de aprovação (Story 3.2).
- `/v1/logins/{id}/enable` e `/v1/logins/{id}/unblock` hoje são endpoints **síncronos** (`LoginStatusTransitionRequest{reasonId}` → `204` imediato, sem aprovação). A Story 3.3 (fora desta story) precisa redesenhar esses dois para passar pela cadeia de aprovação (FR-24) — **não mexer neles aqui**, só documentando o achado para a 3.3 não ser pega de surpresa.

**Por que `PENDING_APPROVAL` entra no enum Java mas as regras de bloqueio de autenticação ficam parciais.** `LoginBlockedRule`/`LoginInactiveRule` (padrão `@ScosRule`, `BusinessRule<LoginStatus>`) são o mecanismo que impede um Login `BLOCKED`/`INACTIVE` de autenticar. Esta story cria o equivalente para `PENDING_APPROVAL` (Task 2) — mas **não sabe ainda**, sem ler a Story 3.2, se o bloqueio de autenticação de fato consome essas `BusinessRule` num filtro/interceptor real ou se elas são só validação de transição de domínio. Procure `BusinessRule<LoginStatus>` sendo consumida (ex.: um filtro de autenticação, ou validação em `Login.activate()`/etc.) antes de assumir que criar a regra sozinha já "bloqueia autenticação de fato" — se o mecanismo de enforcement ainda não existe em lugar nenhum, documente isso como gap encontrado e não invente um filtro de autenticação novo aqui (fora de escopo desta story, que é só criação).

**`REJECTED` é declarado mas não usado nesta story.** O `CHECK` do banco (`chk_login_status_history_status`, aplicado em 2026-07-19) já aceita `REJECTED` como valor de `LoginStatusHistory.status` — sinal de que a spine já previu esse estado terminal para quando uma `LoginApprovalRequest` é rejeitada (Story 3.2). Declarar agora no enum Java evita uma migração de enum isolada depois; usar de fato é trabalho da 3.2.

**Saga Keycloak (FR-6) — auditoria encontrou zero implementação, em qualquer story anterior.** `OutboxEvent`/`OutboxTopic`/`OutboxEventStatus`/`OutboxBackend` existem como entidades (schema completo: hash, payload JSON, retry/max retries, status), mas **nenhum código no projeto inteiro produz ou consome `OutboxEvent`** — nenhum `@Scheduled`, nenhum client Keycloak Admin, nenhum dispatcher. Isso é maior que uma story: é uma peça de infraestrutura própria (decidir backend PGMQ/Kafka/Direct, política de retry, quem consome). Esta story **não** tenta resolver isso — só deixa a porta pronta (Login chega a `ACTIVE` via aprovação, na Story 3.2) para quando o dispatcher existir. Sinalizado ao usuário no resumo desta sessão — não adivinhado aqui.

### Onde cada peça vai (camadas)

- `etc/api/organization/ScosOrganization_Login.yml`: `LoginStatus` ganha `PENDING_APPROVAL` (schema compartilhado); `CreateEmployeeLoginRequest` perde `reasonActivateId` (`CreateLoginRequest` fica para a Story 4.1); `x-authorize` de `block`/`unblock` migra.
- `domain/access/login/internal/LoginStatus.java`: `+PENDING_APPROVAL, +REJECTED`.
- `domain/access/login/internal/rules/LoginPendingApprovalRule.java` (novo).
- `domain/access/login/internal/LoginRepository.java`: `+existsByLogin`, `+findAllFiltered`.
- `domain/access/login/internal/LoginPredicates.java` (novo).
- `domain/access/login/dto/LoginInput.java`/`LoginOutput.java` (novos).
- `domain/access/login/specification/LoginService.java` (novo) + `service/LoginServiceBean.java` (novo) — `create`/`findById`/`findAll`.
- `usecase/access/login/CreateEmployeeLoginUseCase(Bean).java`/`FindLoginUseCase(Bean).java`/`FindAllLoginUseCase(Bean).java`/`FindAllEmployeeLoginUseCase(Bean).java`/`LoginApiMapper.java` (novos). `CreateLoginUseCase(Bean)` fica para a Story 4.1.
- `api/delegate/login/LoginDelegate.java` (novo, `@Override` parcial — 5 métodos: 2 de criação + 3 de consulta).
- `infrastructure/enumaration/ScosOrganizationPermission.java`: `-UPDATE_LOGIN_STATUS`, `+BLOCK_LOGIN`, `+UNBLOCK_LOGIN`.
- `shared/exception/ExceptionCodeError.java` + `scos_message_organization[_en].properties`: `+SCOS_LOGIN_014`.
- `shared/.../messages_permission[_en].properties`: `-UPDATE_LOGIN_STATUS`, `+BLOCK_LOGIN`, `+UNBLOCK_LOGIN`.
- Nenhuma mudança em Liquibase — `SCOS_LOGIN.STATUS` é `varchar(50)` sem `CHECK`, aceita `PENDING_APPROVAL` sem migração.

### Testing Standards

- Unitário de domínio: `LoginServiceBeanTest` novo (`create`/`findById`/`findAll`), padrão Mockito já convencionado (`@ExtendWith(MockitoExtension.class)`, Given/When/Then). `LoginPredicatesTest` novo (JUnit 5 puro, padrão Story 2.6).
- Unitário de Use Case: `CreateEmployeeLoginUseCaseBeanTest`/`FindLoginUseCaseBeanTest`/`FindAllLoginUseCaseBeanTest`/`FindAllEmployeeLoginUseCaseBeanTest` novos.
- Integração: novo `LoginControllerTest` (`extends ScosOrganizationTestUtil`) — agora que `getLoginById`/`getAllLogins` existem, o `POST` **pode validar via `GET` de verdade** (criar → `GET /v1/logins/{id}` confirma `PENDING_APPROVAL`), em vez de depender só do corpo do `201` ou de query direta no banco. Cobrir: `POST /v1/employees/{employeeId}/logins` (sucesso, `401`/`403`, `login` duplicado); os 3 `GET` novos (sucesso, filtros de `getAllLogins`, `404` de `getLoginById` para id inexistente, `401`/`403`).
- `PermissionsConsistencyTest` roda depois da Task 3 (remoção/adição de permissão) — não pular.

### Project Structure Notes

- Pacotes novos: `domain/access/login/dto`, `domain/access/login/specification`, `domain/access/login/service` (só `internal`/`internal/rules` existiam), `usecase/access/login`, `api/delegate/login`.
- Mesma fronteira de sempre: `api` não importa `domain`; Use Case traduz `api.dto.*` ⇄ `domain...dto.*Input/*Output`.

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Epic 3 / Story 3.1] — Given/When/Then originais.
- [Source: etc/api/organization/ScosOrganization_Login.yml:1026-1044] — `CreateEmployeeLoginRequest` atual, com `reasonActivateId` a remover (`CreateLoginRequest`, linhas 1000-1025, mesmo problema, fica para a Story 4.1).
- [Source: _bmad-output/planning-artifacts/epics.md#Epic List] — mapeamento FR→Épico: Epic 3 cobre FR-6/7/23/24/25; Epic 4 cobre FR-26/27 e "depende de Epic 3 (mecanismo já precisa existir)". Base da correção de escopo desta sessão (`POST /v1/logins` sai daqui, `LoginService`/`LoginApprovalChainResolver` continuam genéricos o bastante pra Epic 4 reaproveitar).
- [Source: etc/api/organization/ScosOrganization_Login.yml:841-846] — `LoginStatus` schema atual, sem `PENDING_APPROVAL`.
- [Source: etc/api/organization/ScosOrganization_Login.yml:217-278] — `block`/`unblock`, `x-authorize: [UPDATE_LOGIN_STATUS]` a substituir.
- [Source: organization/flow-organization-domain/.../access/login/internal/Login.java] — entidade atual, `status` sem `CHECK` no banco (`varchar(50)`).
- [Source: organization/flow-organization-domain/.../access/login/internal/LoginStatus.java] — enum atual (`ACTIVE, INACTIVE, BLOCKED`).
- [Source: organization/flow-organization-domain/.../access/login/internal/rules/LoginBlockedRule.java, LoginInactiveRule.java] — padrão `@ScosRule`/`BusinessRule<LoginStatus>` a replicar para `PENDING_APPROVAL`.
- [Source: organization/flow-organization-resources/.../db/changelog/organization/checks/checks.yml:28-29] — `chk_login_status_history_status`, confirma `PENDING_APPROVAL`/`REJECTED` já previstos no banco desde 2026-07-19.
- [Source: organization/flow-organization-resources/.../db/changelog/organization/v1.0.0/tables/scos_login.yml] — `STATUS varchar(50)`, sem `CHECK` — confirma que adicionar valor ao enum Java não exige migração.
- [Source: organization/flow-organization-domain/.../outbox/internal/OutboxEvent.java, OutboxTopic.java, OutboxBackend.java] — schema do Outbox já existe; nenhum produtor/consumidor implementado (auditado nesta sessão via grep em todo o módulo).
- [Source: organization/flow-organization-infrastructure/.../enumaration/ScosOrganizationPermission.java] — `UPDATE_LOGIN_STATUS`/`ENABLE_LOGIN`/`DISABLE_LOGIN` atuais; `UPDATE_LOGIN_STATUS` confirmado usado só por `block`/`unblock` (grep em todo `ScosOrganization_Login.yml`).
- [Source: organization/flow-organization-domain/.../corporate/employee/internal/EmployeeQueryRepository.java, Story 2.1] — padrão de `EmployeeDelegate` com `@Override` parcial, reaproveitado para `LoginDelegate`.
- [Source: _bmad-output/planning-artifacts/epics.md] — busca de ponta a ponta (todos os épicos) confirmou que nenhuma story do roadmap cobre `getLoginById`/`getAllLogins`/`getAllEmployeeLogins` — gap identificado numa revisão pedida pelo usuário após a criação inicial das 6 stories do Epic 3, corrigido aqui (AC 7).
- [Source: etc/api/organization/ScosOrganization_Login.yml:895-934] — schemas `LoginSummary`/`Login`/`LoginEmployee`/`ProfileSummary` já existentes no contrato (nunca consumidos por nenhum código), base de `LoginApiMapper`.
- [Source: organization/flow-organization-usecase/.../corporate/employee/FindAllEmployeeUseCaseBean.java, Story 2.5] — padrão exato de `FindAllLoginUseCaseBean`/`FindAllEmployeeLoginUseCaseBean` (paginação + filtros + mapper resumido).
- [Source: organization/flow-organization-domain/.../access/profile/internal/Profile.java] — sem `ProfileService`/DTO hoje; `LoginOutput` lê o campo achatado em vez de criar uma camada de aplicação nova só pra isso (decisão registrada acima).

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List

## Change Log

| Data | Descrição |
|------|-----------|
| 2026-08-16 | Story criada — Epic 3, mecânica base de criação de Login. Contrato corrigido (remove `reasonActivateId` da criação, adiciona `PENDING_APPROVAL`), `BLOCK_LOGIN`/`UNBLOCK_LOGIN` separadas, camada Java completa do zero (Service/UseCase/Delegate). Gaps de código-vs-épico documentados (Saga Keycloak sem dispatcher, enforcement de `PENDING_APPROVAL` a confirmar). |
| 2026-08-16 | AC 7 adicionado — gap encontrado numa revisão pedida pelo usuário: nenhuma story do roadmap inteiro implementava consulta de Login (`getLoginById`/`getAllLogins`/`getAllEmployeeLogins`), tornando o fluxo inutilizável de ponta a ponta. Movido para esta story (não uma nova) por ser extensão natural da "mecânica base". |
| 2026-08-16 | **Correção de escopo:** `POST /v1/logins` (EXTERNAL/SERVICE, `CreateLoginUseCase`) removido desta story — validação cruzada com os demais épicos (pedida pelo usuário) achou que `epics.md` já mapeia FR-26 (Login sem Funcionário) para o **Epic 4**, não Epic 3, e que uma versão anterior desta story invadia esse escopo. `LoginService`/`LoginStatus` continuam genéricos o bastante pra Epic 4 reaproveitar sem duplicar. |
| 2026-08-16 | **N+1 corrigido (ponytail-review pedido pelo usuário):** `LoginOutput`/`LoginServiceBean.findAll` denormalizava `profileCode`/`profileDescription`/`employeeName` pra toda linha de uma página — nenhum consumidor desse caminho (`toApiLoginSummary`) usa esses campos, e lê-los exigiria inicializar 2 proxies `LAZY` por Login (padrão que o resto do código nunca usa — `EmployeeServiceBean` só lê `.getId()` de relação lazy). `findAll` agora só lê `.getId()`; `findById` (1 linha) é quem denormaliza. |
