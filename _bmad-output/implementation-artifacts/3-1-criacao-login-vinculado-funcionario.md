---
baseline_commit: b72b2dcc0f63997fe313658e358e68dc506dd645
---

# Story 3.1: Criação de Login Vinculado a Funcionário (mecânica base)

Status: review

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
5. **Given** o código hoje usa uma permissão única (`UPDATE_LOGIN_STATUS`) para `block`/`unblock` **When** esta story é implementada **Then** `BLOCK_LOGIN` e `UNBLOCK_LOGIN` passam a existir em `ScosGeotemporalPermission`, `x-authorize` de `/v1/logins/{id}/block` e `/v1/logins/{id}/unblock` migram para elas **And** `UPDATE_LOGIN_STATUS` é removida do enum de permissões (nenhum outro `x-authorize` a usa — confirmado por leitura de todo `ScosOrganization_Login.yml`).
6. **Given** nenhum Use Case/Delegate/Repository de `Login` existe hoje (só a entidade `Login`, `LoginStatus`, `LoginType`, 2 `BusinessRule` de estado e `LoginRepository.findByLogin`) **When** esta story é implementada **Then** cria a camada completa de Use Case de escrita (`CreateEmployeeLoginUseCase` + Bean) e o `LoginDelegate`, seguindo o padrão já usado por `Employee`/`Company` (Story 2.1/1.2) — `CreateLoginUseCase` (EXTERNAL/SERVICE) fica para a Story 4.1.
7. **Given** nenhuma story do roadmap inteiro (auditado em `epics.md` de ponta a ponta) implementa consulta de Login — sem isso, RH cria o Login (AC 1) mas não tem como descobrir depois "quais Logins este Funcionário tem" nem "qual o status deste Login" via API **When** esta story é implementada **Then** `GET /v1/logins/{id}` (`getLoginById`), `GET /v1/logins` (`getAllLogins`, filtros `type`/`status`/`employeeId` já contratados) e `GET /v1/employees/{employeeId}/logins` (`getAllEmployeeLogins`) passam a funcionar **And** `getLoginMe`/`getEmployeeLoginById`/demais rotas de Login continuam fora de escopo (AC 6/7 guarda de escopo, ver Dev Notes).

## Tasks / Subtasks

- [x] Task 1: Corrigir o contrato — `LoginStatus` ganha `PENDING_APPROVAL`, criação de Employee Login perde `reasonActivateId` (AC: 1, 2, 3)
  - [x] Em `etc/api/organization/ScosOrganization_Login.yml`, schema `LoginStatus` (linha ~841-846): adicionar `PENDING_APPROVAL` ao `enum` — **este schema é compartilhado** por todos os tipos de Login (`EMPLOYEE`/`EXTERNAL`/`SERVICE`), correto corrigir aqui de uma vez só, mesmo a criação de `EXTERNAL`/`SERVICE` sendo Epic 4.
  - [x] `CreateEmployeeLoginRequest` (linha ~1026-1044): remover `reasonActivateId` de `required` e de `properties`. Atualizar `description` do schema removendo a menção a "motivo da criação".
  - [x] `POST /v1/employees/{employeeId}/logins` (`createEmployeeLogin`, linha ~424-451): atualizar `description` — trocar "Inicia a Saga Keycloak" por algo como "UC-054 - Cria login em PENDING_APPROVAL. A Saga Keycloak só dispara quando a aprovação (Story 3.2) leva o Login a ACTIVE".
  - [x] **Não** tocar em `CreateLoginRequest`/`POST /v1/logins` (`createLogin`) — mesmo bug de contrato (`reasonActivateId` presumindo `ACTIVE` imediato), mas é escopo da Story 4.1, que faz seu próprio "contrato antes do código" quando chegar a vez.
  - [x] Rodar `mvn clean generate-sources` em `flow-organization-usecase` e `flow-organization-api` para confirmar que `CreateEmployeeLoginRequest` gerado não tem mais `reasonActivateId` e que `LoginStatus` gerado inclui `PENDING_APPROVAL`. **Gap de infraestrutura achado e corrigido:** nenhum dos dois poms (`usecase`/`api`) tinha `<execution>` do `openapi-generator-maven-plugin` apontando pra `ScosOrganization_Login.yml` — o spec de 1225 linhas nunca tinha sido conectado ao build, então nada de `Login` jamais foi gerado em nenhuma story anterior. Adicionado `<execution id="ScosOrganization_Login">` em ambos os poms, espelhando exatamente o bloco de `ScosOrganization_Employee` (mesmo `generatorName`/`configOptions`/`typeMappings`). Confirmado: `mvn clean generate-sources` roda limpo, `CreateEmployeeLoginRequest` gerado só tem `login`/`profileId`, `LoginStatus` gerado tem `PENDING_APPROVAL`.

- [x] Task 2: `LoginStatus` Java ganha `PENDING_APPROVAL`/`REJECTED`; regras de estado cobrem `PENDING_APPROVAL` (AC: 1, 3)
  - [x] Em `domain/access/login/internal/LoginStatus.java`, adicionar `PENDING_APPROVAL` e `REJECTED` ao enum — ordem sugerida `PENDING_APPROVAL, ACTIVE, INACTIVE, BLOCKED, REJECTED` (mesma ordem do `CHECK` `chk_login_status_history_status` do banco, só para legibilidade — o enum Java não impõe ordem funcional).
  - [x] Criar `domain/access/login/internal/rules/LoginPendingApprovalRule.java`, mesmo padrão exato de `LoginInactiveRule`/`LoginBlockedRule` (`@ScosRule(N)`, `BusinessRule<LoginStatus>`, retorna o novo código de erro quando `context == PENDING_APPROVAL`). Usar `@ScosRule(3)` (as duas existentes são `1`/`2`). Teste unitário novo (`LoginPendingApprovalRuleTest`, JUnit 5 + AssertJ) cobrindo `PENDING_APPROVAL` (retorna o código) e `ACTIVE` (retorna vazio) — passou.
  - [x] Adicionar `SCOS_LOGIN_014` em `ExceptionCodeError` (`422`, `SCOS_TITLE_BUSINESS_RULE_VIOLATION`) + mensagens PT-BR/EN em `scos_message_organization[_en].properties` ("O Login informado está pendente de aprovação.").
  - [x] **Não** criar regra para `REJECTED` nesta story — `REJECTED` só passa a ser alcançável pela Story 3.2 (rejeição de aprovação); declarar o valor no enum agora evita migração de enum depois, mas o bloqueio de autenticação para esse estado é tarefa da 3.2.
  - **Achado (resolve a dúvida em aberto do Dev Notes):** o enforcement de fato existe — `AuthorityResponseServiceBean.validate(login)` (`domain/access/login/service/`) chama `loginRolesService.validateStatusLogin(authorityResponse.getStatus())`, que roda `RuleChain<LoginStatus>` sobre **todos** os beans `BusinessRule<LoginStatus>` (injeção `List<BusinessRule<LoginStatus>>` via Spring, `LoginRolesService`) e lança `ScosException` no primeiro que bater. `LoginPendingApprovalRule` sendo um `@ScosRule` novo entra automaticamente nessa lista — nenhum código adicional de "fiação" foi necessário. Confirmado por leitura de código, não suposição.

- [x] Task 3: Permissões — `BLOCK_LOGIN`/`UNBLOCK_LOGIN` substituem `UPDATE_LOGIN_STATUS` (AC: 5)
  - [x] Em `ScosOrganizationPermission.java`, remover `UPDATE_LOGIN_STATUS` e adicionar `BLOCK_LOGIN`/`UNBLOCK_LOGIN` no mesmo grupo (`"Access"`, `"Login"`), versão `"1.0.0"`, data `"2026-08-16"` (data real desta implementação).
  - [x] Em `ScosOrganization_Login.yml`, `x-authorize` de `blockLogin` (linha 244) vira `[BLOCK_LOGIN]`, de `unblockLogin` (linha 275) vira `[UNBLOCK_LOGIN]`.
  - [x] Atualizar `messages_geotemporal_permission.properties`/`_en`: remover a entrada de `UPDATE_LOGIN_STATUS`, adicionar `BLOCK_LOGIN`/`UNBLOCK_LOGIN` (PT-BR e EN, 1:1).
  - [x] `mvn -pl flow-organization-infrastructure test -Dtest=PermissionsConsistencyTest -Denforcer.skip=true` passou — só o aviso pré-existente dos 5 `DELETE_*` órfãos (dívida já documentada em `project-context.md`), nenhuma falha.

- [x] Task 4: `LoginRepository` — métodos necessários para escrita e consulta (AC: 6, 7)
  - [x] Adicionar `boolean existsByLogin(String login)` — confirmado o nome gerado pelo QueryDSL APT: `QLogin.login1` (colisão com o nome da classe `Login`), `default boolean existsByLogin(String login) { return exists(QLogin.login1.login.eq(login)); }`.
  - [x] **Não** duplicar `findByLogin` (já existia, mantido).
  - [x] `LoginPredicates.java` (`final` package-private em `internal`) com `predicateTypeAndStatusAndEmployeeId(LoginType, LoginStatus, Long)`, 3 filtros opcionais via `BooleanBuilder`, mesmo padrão de `EmployeeQueryPredicates.predicateCompanyIdAndPositionIdAndStatus`. `default Page<Login> findAllFiltered(...)` em `LoginRepository` com o atalho "nenhum filtro" → `findAll(pageable)`.
  - [x] `LoginPredicatesTest.java` (JUnit 5 puro, `predicate.toString()`, mesmo padrão de `EmployeeQueryPredicatesTest`) — 4 cenários (só type, só status, só employeeId, os 3 combinados) — passou.

- [x] Task 5: `LoginService`/`LoginServiceBean` (domain) — criar Login em `PENDING_APPROVAL`; consultas (AC: 1, 4, 6, 7)
  - [x] `domain/access/login/dto/LoginInput.java`/`LoginOutput.java` (records) — `LoginOutput` com `id`/`login`/`externalId`/`type`/`status`/`profileId`/`employeeId` + `profileCode`/`profileDescription`/`employeeName` nullable (só `findById` preenche).
  - [x] `domain/access/login/specification/LoginService.java` — `create(LoginInput)`, `findById(Long)`, `findAll(LoginType, LoginStatus, Long employeeId, Pageable)`. `create` genérico de tipo, restrição a `EMPLOYEE` fica no Use Case (Task 6).
  - [x] `domain/access/login/service/LoginServiceBean.java` — `existsByLogin` (**não** reaproveitou `SCOS_LOGIN_001`: é um placeholder órfão travado por teste (`ExceptionCodeErrorTest.orphanCodeKeepsGenericDefault`, 400/GENERIC) — mudar seu httpCode/title pra 409/CONFLICT quebraria esse teste; criado `SCOS_LOGIN_015` novo, 409 CONFLICT), resolve `Profile` via `ProfileRepository.findById` (**novo** `SCOS_PROFILE_001`, 404 — módulo `SCOS_PROFILE` não tinha nenhum código de erro ainda) e `Employee` via `EmployeeQueryRepository.findById` (reaproveita `SCOS_EMPLOYEE_014`, já existia) só quando `employeeId != null`, monta `Login.builder().status(PENDING_APPROVAL)...build()`, persiste via `loginRepository.merge` (padrão `BaseJpaRepository`, mesmo de `Position`/`Company`).
    - `findById`/`findAll` compartilham um único `toLoginOutput(login, denormalize)` privado (parâmetro booleano, não duas cópias de mapeamento) — `findById` chama com `denormalize=true` (lê `profile.getCode()`/`.getDescription()`/`employee.getName()`, 1 linha, 2 proxies `LAZY` sem custo relevante); `findAll` chama com `false` (só `.getId()` de ambas relações — grátis, mesmo padrão de `EmployeeServiceBean.toEmployeeOutput`). Novo `SCOS_LOGIN_016` (404) pra `findById` sem resultado.
  - [x] **Não** escreveu `LoginStatusHistory` na criação — confirmado, `create` só persiste o `Login`, sem histórico (estado inicial, não transição).
  - Teste unitário `LoginServiceBeanTest` (9 cenários: create sucesso c/ e s/ employeeId, 3 cenários de erro, findById sucesso/404, findAll sem denormalizar + delegação de filtros) — passou.

- [x] Task 6: Use Case + Delegate de criação (AC: 6)
  - [x] `usecase/access/login/CreateEmployeeLoginUseCase(Bean)` — valida via `EmployeeService.findById` que o Funcionário existe e está `ACTIVE` (novo `SCOS_EMPLOYEE_025`, 422 — checado antes: não existia nenhum `SCOS_EMPLOYEE_0XX` de "funcionário não ativo" nos 24 já usados, todos com semântica diferente).
  - [x] `api/delegate/login/LoginDelegate implements LoginApiDelegate` — implementa `createEmployeeLogin` + os 3 `@Override` de consulta da Task 7 (resto sem `@Override`, escopo parcial). **Correção sobre a nota original:** `201_CREATED` **usa** `data:` — é `$ref: '#/components/schemas/CreateResponse'` (`{data: {id}}`), confirmado lendo `ScosComponents.yml:178-183`; a nota "sem `data:`, `return null`" da story original estava errada. Delegate segue o padrão real do projeto (`CreateResponse.builder().data(Create.builder().id(...).build()).build()`, mesmo de `DepartmentDelegate.createDepartment`).
  - [x] `createLogin` (EXTERNAL/SERVICE) sem `@Override` — Story 4.1.
  - **Achado bloqueante corrigido (fora do escopo literal da Task 1, mas necessário pra ela funcionar):** ligar `ScosOrganization_Login.yml` ao build (Task 1) expôs que `CreateLoginRequest.type` (propriedade `type` com `enum` inline, `[EXTERNAL, SERVICE]`) gera um record referenciando uma classe `TypeEnum` que os templates mustache deste projeto **não emitem** para enum inline-em-propriedade (só para schema nomeado no top-level, que é o padrão usado em toda a spec) — erro de compilação bloqueava o módulo `usecase` inteiro. Corrigido substituindo o enum inline por `$ref: '#/components/schemas/LoginType'` (reaproveita o schema já existente, restrição de negócio "só EXTERNAL/SERVICE, não EMPLOYEE" documentada em comentário YAML, não no schema — fica pra Story 4.1 aplicar em código). `CreateLoginRequest` continua não implementado (Use Case/Delegate), só o contrato ficou compilável.

- [x] Task 7: Use Cases de consulta + `LoginApiMapper` (AC: 7)
  - [x] `usecase/access/login/LoginApiMapper.java` — `toApiLogin`/`toApiLoginSummary`/`toApiType`/`toApiStatus`/`toDomainType`/`toDomainStatus`. `LoginType`/`LoginStatus` existem em **ambos** os pacotes `domain.access.login.internal` e `api.dto` com o mesmo nome simples — resolvido com FQN inline nos métodos de conversão (sem alias de import em Java), mesmo padrão de necessidade que já existe pra `EmployeeApiMapper.toDomainStatus`/`toApiStatus` (lá sem colisão de nome, aqui com).
  - [x] `usecase/access/login/FindLoginUseCase(Bean).java`, `FindAllLoginUseCase(Bean).java`, `FindAllEmployeeLoginUseCase(Bean).java` (delega 1 linha pro `FindAllLoginUseCase`, sem duplicar paginação/filtro/mapeamento).
  - [x] `LoginDelegate` ganha `getLoginById`/`getAllLogins`/`getAllEmployeeLogins`.
  - Testes unitários: `CreateEmployeeLoginUseCaseBeanTest` (3), `FindLoginUseCaseBeanTest` (2), `FindAllLoginUseCaseBeanTest` (3), `FindAllEmployeeLoginUseCaseBeanTest` (2) — 10 cenários, todos passaram.

- [x] Task 8: Guarda de escopo (AC: 1, 4, 6, 7)
  - [x] **Não** implementar `createLogin`/`CreateLoginUseCase` (`POST /v1/logins`, EXTERNAL/SERVICE) — confirmado sem `@Override` no `LoginDelegate`.
  - [x] **Não** implementar `getLoginMe`, `getEmployeeLoginById`, `updateLogin`, `enable`/`disable`/`block`/`unblock`, `status-history`, profile-de-login — confirmado: `LoginDelegate` tem exatamente 4 `@Override` (`createEmployeeLogin`, `getLoginById`, `getAllLogins`, `getAllEmployeeLogins`), nenhum outro.
  - [x] **Não** implementar a Saga Keycloak/`OutboxEvent` — confirmado, `grep` por `OutboxEvent`/`OutboxTopic` no pacote `access/login` da `usecase`: zero ocorrências.
  - [x] **Não** implementar `LoginApprovalRequest` — confirmado, nenhuma classe/uso criado.
  - [x] **Não** tocar em `LoginType`/`Profile`/`Resource`/`System` além de leitura — confirmado, `ProfileRepository.findById`/`EmployeeQueryRepository.findById` são os únicos acessos, sem escrita.
  - [x] **Não** criar `ProfileService` — confirmado, `find . -iname "ProfileService*.java"` não retorna nada.

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
- Integração: `LoginControllerTest` (`extends ScosOrganizationTestUtil`) — 21 cenários, todos passando (83s). Cobre: `POST /v1/employees/{employeeId}/logins` (sucesso + confirma `PENDING_APPROVAL` via `GET` real, login duplicado `409`, profile não encontrado `404`, employee não encontrado `404`, employee não `ACTIVE` `422`, validação `400`, `401`/`403`); `GET /v1/logins/{id}` (sucesso completo com `profile`/`employee` aninhados, `404`, `401`/`403`); `GET /v1/logins` (lista resumida, filtro `type`, filtro `employeeId`, `401`/`403`); `GET /v1/employees/{employeeId}/logins` (escopado, `401`/`403`). Seed usado: `SCOS_EMPLOYEE` id=1 (ACTIVE), `SCOS_PROFILE` id=1/2, `SCOS_LOGIN` id=1 (`scos-admin`, EMPLOYEE)/id=2 (`scos-api`, SERVICE) — `scos-admin` reaproveitado pro cenário de duplicidade.
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

Claude Sonnet 5 (bmad-dev-story), com aplicação da skill `ponytail` (full) durante toda a implementação.

### Debug Log References

- `mvn clean generate-sources` (usecase+api) — confirmou geração de `Login`/`LoginStatus`/`CreateEmployeeLoginRequest` após corrigir os 2 gaps de infraestrutura (pom sem `<execution>` pro spec de Login; `CreateLoginRequest.type` com enum inline incompatível com os templates mustache).
- `mvn -pl flow-organization-infrastructure test -Dtest=PermissionsConsistencyTest` — 1:1 confirmado entre `x-authorize` e `ScosOrganizationPermission` após `BLOCK_LOGIN`/`UNBLOCK_LOGIN` substituírem `UPDATE_LOGIN_STATUS`.
- Suíte de regressão completa (`mvn test`, reactor `organization/`) — **1079 testes, 0 falhas, 0 erros** (shared 11, domain 354, infrastructure 3, usecase 227, boot 476 incl. os 21 novos de `LoginControllerTest`, grpc-boot 8).

### Completion Notes List

- Todas as 8 Tasks concluídas e testadas (unitário + integração). Todos os 7 ACs satisfeitos.
- 2 gaps de infraestrutura pré-existentes (não desta story) achados e corrigidos porque bloqueavam a Task 1: (a) nenhum pom (`usecase`/`api`) tinha `<execution>` do openapi-generator pra `ScosOrganization_Login.yml` — nunca tinha sido gerado; (b) `CreateLoginRequest.type` (enum inline numa propriedade, não schema nomeado) não compilava com os templates mustache do projeto — corrigido com `$ref` pro schema `LoginType` já existente.
- 1 correção de entendimento equivocado do próprio texto da story: `createEmployeeLogin` usa `data:` (schema `CreateResponse`), não é `204_NO_CONTENT` sem wrapper como a nota original da Task 6 dizia — corrigido lendo `ScosComponents.yml` antes de escrever o Delegate.
- 5 códigos de erro novos: `SCOS_LOGIN_014` (pendente de aprovação), `SCOS_LOGIN_015` (login duplicado, 409 — não reaproveitou `SCOS_LOGIN_001` porque esse é um placeholder travado por teste), `SCOS_LOGIN_016` (não encontrado, 404), `SCOS_PROFILE_001` (não encontrado, 404 — primeiro código desse módulo), `SCOS_EMPLOYEE_025` (não ativo, 422).
- **Achado de ambiente, não desta story:** durante a implementação, foi detectada modificação concorrente no mesmo working tree (um novo módulo `geotemporal/` sendo adicionado, com renomeação de `organization/flow-organization-infrastructure/.../messages_permission.properties` → `messages_organization_permission.properties`, e ajuste do `OrganizationMessageConfiguration` compartilhado). Confirmado que o conteúdo desta story (`BLOCK_LOGIN`/`UNBLOCK_LOGIN`) sobreviveu à renomeação e a suíte de regressão passou 100% — nada foi corrigido ou revertido, é trabalho de outra sessão/pessoa, fora do escopo desta story. Reportado ao usuário no resumo final.

### File List

**Novos:**
- `etc/api/organization/ScosOrganization_Login.yml` *(modificado, não novo — ver abaixo)*
- `organization/flow-organization-api/src/main/java/br/com/sawcunhaos/organization/api/delegate/login/LoginDelegate.java`
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/access/login/dto/LoginInput.java`
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/access/login/dto/LoginOutput.java`
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/access/login/internal/LoginPredicates.java`
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/access/login/internal/rules/LoginPendingApprovalRule.java`
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/access/login/service/LoginServiceBean.java`
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/access/login/specification/LoginService.java`
- `organization/flow-organization-domain/src/test/java/br/com/sawcunhaos/organization/domain/access/login/internal/LoginPredicatesTest.java`
- `organization/flow-organization-domain/src/test/java/br/com/sawcunhaos/organization/domain/access/login/internal/rules/LoginPendingApprovalRuleTest.java`
- `organization/flow-organization-domain/src/test/java/br/com/sawcunhaos/organization/domain/access/login/service/LoginServiceBeanTest.java`
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/access/login/CreateEmployeeLoginUseCase.java`
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/access/login/CreateEmployeeLoginUseCaseBean.java`
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/access/login/FindAllEmployeeLoginUseCase.java`
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/access/login/FindAllEmployeeLoginUseCaseBean.java`
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/access/login/FindAllLoginUseCase.java`
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/access/login/FindAllLoginUseCaseBean.java`
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/access/login/FindLoginUseCase.java`
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/access/login/FindLoginUseCaseBean.java`
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/access/login/LoginApiMapper.java`
- `organization/flow-organization-usecase/src/test/java/br/com/sawcunhaos/organization/application/usecase/access/login/CreateEmployeeLoginUseCaseBeanTest.java`
- `organization/flow-organization-usecase/src/test/java/br/com/sawcunhaos/organization/application/usecase/access/login/FindLoginUseCaseBeanTest.java`
- `organization/flow-organization-usecase/src/test/java/br/com/sawcunhaos/organization/application/usecase/access/login/FindAllLoginUseCaseBeanTest.java`
- `organization/flow-organization-usecase/src/test/java/br/com/sawcunhaos/organization/application/usecase/access/login/FindAllEmployeeLoginUseCaseBeanTest.java`
- `organization/flow-organization-boot/src/test/java/br/com/sawcunhaos/organization/boot/api/login/LoginControllerTest.java`

**Modificados:**
- `etc/api/organization/ScosOrganization_Login.yml` — `LoginStatus`+`PENDING_APPROVAL`; `CreateEmployeeLoginRequest` sem `reasonActivateId`; descrição de `createEmployeeLogin`; `x-authorize` de `blockLogin`/`unblockLogin`; `CreateLoginRequest.type` via `$ref` (fix de geração, Story 4.1 ainda dona da lógica).
- `organization/flow-organization-usecase/pom.xml` — `<execution>` novo pro spec de Login.
- `organization/flow-organization-api/pom.xml` — `<execution>` novo pro spec de Login.
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/access/login/internal/LoginStatus.java` — `+PENDING_APPROVAL, +REJECTED`.
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/access/login/internal/LoginRepository.java` — `+existsByLogin`, `+findAllFiltered`.
- `organization/flow-organization-infrastructure/src/main/java/br/com/sawcunhaos/organization/infrastructure/enumaration/ScosOrganizationPermission.java` — `-UPDATE_LOGIN_STATUS`, `+BLOCK_LOGIN`, `+UNBLOCK_LOGIN`.
- `../../organization/flow-organization-infrastructure/src/main/resources/scos_message/messages_organization_permission.properties` *(nome atual — renomeado de `messages_permission.properties` por trabalho concorrente de outra sessão, ver Completion Notes)* — `-UPDATE_LOGIN_STATUS`, `+BLOCK_LOGIN`, `+UNBLOCK_LOGIN`.
- `../../organization/flow-organization-infrastructure/src/main/resources/scos_message/messages_organization_permission_en.properties` *(idem)* — mesma mudança, EN.
- `organization/flow-organization-shared/src/main/java/br/com/sawcunhaos/organization/shared/exception/ExceptionCodeError.java` — `+SCOS_LOGIN_014/015/016`, `+SCOS_PROFILE_001`, `+SCOS_EMPLOYEE_025`.
- `../../organization/flow-organization-shared/src/main/resources/scos_message/scos_message_organization.properties` — mensagens PT dos 5 códigos acima.
- `../../organization/flow-organization-shared/src/main/resources/scos_message/scos_message_organization_en.properties` — mensagens EN dos 5 códigos acima.

## Change Log

| Data | Descrição |
|------|-----------|
| 2026-08-16 | Story criada — Epic 3, mecânica base de criação de Login. Contrato corrigido (remove `reasonActivateId` da criação, adiciona `PENDING_APPROVAL`), `BLOCK_LOGIN`/`UNBLOCK_LOGIN` separadas, camada Java completa do zero (Service/UseCase/Delegate). Gaps de código-vs-épico documentados (Saga Keycloak sem dispatcher, enforcement de `PENDING_APPROVAL` a confirmar). |
| 2026-08-16 | AC 7 adicionado — gap encontrado numa revisão pedida pelo usuário: nenhuma story do roadmap inteiro implementava consulta de Login (`getLoginById`/`getAllLogins`/`getAllEmployeeLogins`), tornando o fluxo inutilizável de ponta a ponta. Movido para esta story (não uma nova) por ser extensão natural da "mecânica base". |
| 2026-08-16 | **Correção de escopo:** `POST /v1/logins` (EXTERNAL/SERVICE, `CreateLoginUseCase`) removido desta story — validação cruzada com os demais épicos (pedida pelo usuário) achou que `epics.md` já mapeia FR-26 (Login sem Funcionário) para o **Epic 4**, não Epic 3, e que uma versão anterior desta story invadia esse escopo. `LoginService`/`LoginStatus` continuam genéricos o bastante pra Epic 4 reaproveitar sem duplicar. |
| 2026-08-16 | **N+1 corrigido (ponytail-review pedido pelo usuário):** `LoginOutput`/`LoginServiceBean.findAll` denormalizava `profileCode`/`profileDescription`/`employeeName` pra toda linha de uma página — nenhum consumidor desse caminho (`toApiLoginSummary`) usa esses campos, e lê-los exigiria inicializar 2 proxies `LAZY` por Login (padrão que o resto do código nunca usa — `EmployeeServiceBean` só lê `.getId()` de relação lazy). `findAll` agora só lê `.getId()`; `findById` (1 linha) é quem denormaliza. |
| 2026-08-16 | **Implementação completa (dev-story).** Todas as 8 Tasks, 7 ACs. 2 gaps de infraestrutura pré-existentes corrigidos (pom sem `<execution>` pro spec de Login; enum inline incompatível em `CreateLoginRequest.type`). 5 códigos de erro novos (`SCOS_LOGIN_014/015/016`, `SCOS_PROFILE_001`, `SCOS_EMPLOYEE_025`). Testes: 3 unitários de domínio (regra + predicates + service, 15 cenários), 4 unitários de Use Case (10 cenários), 1 de integração (`LoginControllerTest`, 21 cenários). Regressão completa do reactor `organization`: 1079 testes, 0 falhas. |
