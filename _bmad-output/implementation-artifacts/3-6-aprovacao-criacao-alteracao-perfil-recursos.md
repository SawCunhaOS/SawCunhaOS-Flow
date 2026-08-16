---
baseline_commit: b72b2dcc0f63997fe313658e358e68dc506dd645
---

# Story 3.6: Aprovação de Criação/Alteração de Perfil e Recursos

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como titular da permissão `APPROVE_SYSTEM_ACCESS`,
Eu quero aprovar toda criação de um novo Perfil e toda mudança no conjunto de Recursos de um Perfil existente,
Para controlar escalada de privilégio na origem — antes de qualquer Login sequer poder ser trocado para esse Perfil.

**Nota de rastreabilidade (validação cruzada entre épicos, mesma sessão):** esta story cobre FR-27, que `epics.md` mapeia formalmente para a **Epic 4 Story 4.2** ("Criação e Edição de Perfil com Aprovação"). Quando esta Story 3.6 foi criada, só se checou o bloco do Epic 3 no `epics.md`, sem notar que a 4.2 já existia com o mesmo escopo. Decisão do usuário: manter o trabalho aqui em vez de mover pro Epic 4 — a Story 4.2 em `epics.md` foi marcada como redundante, aponta pra este arquivo.

## Acceptance Criteria

1. **Given** RH aciona `POST /v1/profiles` (criar Perfil) **When** a solicitação é enviada **Then** entra em aprovação — o Perfil **não** é criado ainda, só a proposta (código + descrição) fica registrada, pendente.
2. **Given** RH aciona `PUT /v1/profiles/{id}/resources` (substituir o conjunto de Recursos de um Perfil existente) **When** a solicitação é enviada **Then** entra em aprovação — o conjunto de Recursos **efetivo** do Perfil não muda ainda **And** Logins que já usam esse Perfil continuam com o conjunto de Recursos **anterior** em vigor até a decisão (FR-27, literal).
3. **Given** uma solicitação de criação de Perfil ou de mudança de Recursos é aprovada **When** a decisão é registrada **Then** o Perfil passa a existir (caso `CREATE_PROFILE`) ou o conjunto de Recursos efetivo é substituído pelo proposto (caso `UPDATE_RESOURCES`) — sujeito à mesma defasagem de até 30 min das views de autoridade já existente.
4. **Given** a solicitação é rejeitada **When** a decisão é registrada **Then** nada muda — o Perfil proposto nunca chega a existir, ou o conjunto de Recursos efetivo permanece o anterior.
5. **Given** o FR-27 não menciona SLA nem escalonamento por nível (diferente do FR-23, que é explícito: "SLA de 1 dia útil... escalonamento automático") **When** esta story é implementada **Then** a aprovação é de **1 único nível** — qualquer titular de `APPROVE_SYSTEM_ACCESS` decide diretamente, sem cadeia Supervisor→Gerente e sem job de escalonamento.
6. **Given** `updateProfile` (código/descrição), `deleteProfile`, `enable`/`disable` de Perfil **When** esta story é implementada **Then** permanecem síncronos, sem aprovação — não alteram o conjunto de Recursos nem criam um Perfil novo, fora do texto literal do FR-27.

## Tasks / Subtasks

- [ ] Task 1: Nova tabela `SCOS_PROFILE_APPROVAL_REQUEST` (+ recursos propostos) — **gap de schema, não existia antes desta story** (AC: 1, 2)
  - [ ] Novo changeSet em `organization/flow-organization-resources/src/main/resources/db/changelog/organization/v1.0.0/tables/scos_profile_approval_request.yml` (arquivo novo, primeira versão do agregado — diferente de `LoginApprovalRequest`, este é criado do zero nesta story, sem precedente no schema):
    ```yaml
    databaseChangeLog:
      - changeSet:
          id: <data>-Samuel.Cunha-XXX
          author: Samuel.Cunha
          comment: "Criação da tabela SCOS_PROFILE_APPROVAL_REQUEST (Epic 3, Story 3.6 — aprovação de criação/alteração de Perfil, FR-27, gap identificado na revalidação do épico)"
          changes:
            - createTable:
                tableName: SCOS_PROFILE_APPROVAL_REQUEST
                schemaName: scos
                columns:
                  - column: {name: PROFILE_APPROVAL_REQUEST_ID, type: BIGINT, autoIncrement: true, constraints: {primaryKey: true, primaryKeyName: PK_SCOS_PROFILE_APPROVAL_REQUEST, nullable: false}}
                  - column: {name: PROFILE_ID, type: BIGINT, constraints: {nullable: true, foreignKeyName: FK_PROFILE_ID_SCOS_PROFILE_APPROVAL_REQUEST, references: scos.SCOS_PROFILE(PROFILE_ID)}}
                  - column: {name: REQUEST_TYPE, type: varchar(20), constraints: {nullable: false}}
                  - column: {name: PROPOSED_CODE, type: varchar(255), constraints: {nullable: true}}
                  - column: {name: PROPOSED_DESCRIPTION, type: varchar(255), constraints: {nullable: true}}
                  - column: {name: PROPOSED_RESOURCE_IDS, type: uuid[], constraints: {nullable: true}}
                  - column: {name: STATUS, type: varchar(20), defaultValue: PENDING, constraints: {nullable: false}}
                  - column: {name: REQUESTED_BY_LOGIN_ID, type: BIGINT, constraints: {nullable: false, foreignKeyName: FK_REQUESTED_BY_LOGIN_ID_SCOS_PROFILE_APPROVAL_REQUEST, references: scos.SCOS_LOGIN(LOGIN_ID)}}
                  - column: {name: DECIDED_BY_LOGIN_ID, type: BIGINT, constraints: {nullable: true, foreignKeyName: FK_DECIDED_BY_LOGIN_ID_SCOS_PROFILE_APPROVAL_REQUEST, references: scos.SCOS_LOGIN(LOGIN_ID)}}
                  - column: {name: DECIDED_AT, type: TIMESTAMPTZ, constraints: {nullable: true}}
                  - column: {name: CREATED_AT, type: TIMESTAMPTZ, defaultValueComputed: NOW(), constraints: {nullable: false}}
                  - column: {name: UPDATED_AT, type: TIMESTAMPTZ, constraints: {nullable: false}}
                  - column: {name: USER_AT, type: varchar(255), constraints: {nullable: false}}
          rollback:
            - dropTable: {tableName: SCOS_PROFILE_APPROVAL_REQUEST, schemaName: scos, cascadeConstraints: true}
    ```
    `PROFILE_ID` nullable — nulo quando `REQUEST_TYPE=CREATE_PROFILE` (o Perfil ainda não existe); preenchido quando `REQUEST_TYPE=UPDATE_RESOURCES`. `PROPOSED_CODE`/`PROPOSED_DESCRIPTION` só usados por `CREATE_PROFILE`. **Sem** `SLA_DEADLINE`/níveis/escalonamento — AC 5, aprovação de 1 nível só.
  - [ ] **Conjunto de Recursos proposto vira coluna, não tabela.** `PROPOSED_RESOURCE_IDS uuid[]` (acima, mesmo `changeSet`) guarda a lista completa de `Resource.id` propostos — só preenchida quando `REQUEST_TYPE=UPDATE_RESOURCES` (`CREATE_PROFILE` nasce sem Recursos, mesma regra que `createProfile` já tem hoje). **Não** criar uma segunda tabela `SCOS_PROFILE_APPROVAL_REQUEST_RESOURCE` — o projeto já tem exatamente esse padrão em produção para "lista de valores associada a 1 linha, sem metadado próprio por item": `vw_authority_response.permissions` é `text[]` via `@JdbcTypeCode(SqlTypes.ARRAY)`. Réplica direta: `@JdbcTypeCode(SqlTypes.ARRAY) @Column(name="PROPOSED_RESOURCE_IDS", columnDefinition="uuid[]") private List<UUID> proposedResourceIds;` em `ProfileApprovalRequest.java` (Task 4) — sem entidade de junção, sem `@EmbeddedId`, sem segundo `dropTable` no rollback.
  - [ ] `CHECK`s em `checks.yml` (aditivo, mesmo arquivo): `chk_profile_approval_request_type CHECK (request_type IN ('CREATE_PROFILE', 'UPDATE_RESOURCES'))`, `chk_profile_approval_request_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))`, `chk_profile_approval_request_profile CHECK ((request_type = 'CREATE_PROFILE' AND profile_id IS NULL) OR (request_type = 'UPDATE_RESOURCES' AND profile_id IS NOT NULL))`, `chk_profile_approval_request_proposed CHECK ((request_type = 'CREATE_PROFILE' AND proposed_code IS NOT NULL AND proposed_description IS NOT NULL) OR (request_type = 'UPDATE_RESOURCES' AND proposed_code IS NULL AND proposed_description IS NULL))`.
  - [ ] Trigger de bloqueio de `DELETE` (`trg_block_delete_profile_approval_request`, mesmo padrão de todas as tabelas de aprovação/histórico do projeto) + índice em `PROFILE_ID`.
  - [ ] **Decisão de design a confirmar com o usuário — a mais estrutural desta story:** este desenho cria um agregado de aprovação **paralelo** a `LoginApprovalRequest` (Story 3.2), com schema próprio, em vez de generalizar `LoginApprovalRequest` para cobrir também Perfil. Alternativa descartada: fazer `LOGIN_ID` nullable em `SCOS_LOGIN_APPROVAL_REQUEST` e reaproveitar a mesma tabela para tudo — rejeitada porque a tabela já tem `LOGIN_ID` `NOT NULL` desde a criação original (2026-07-18) e semântica fortemente amarrada a Login (níveis Supervisor/Gerente não fazem sentido pra Perfil); forçar os dois num único agregado misturaria uma aprovação de 1 nível sem SLA com uma de 3 níveis com SLA, no mesmo modelo. Esta story assume que são dois agregados de aprovação genuinamente diferentes — confirme antes do `dev-story` se preferir uma generalização única.

- [ ] Task 2: Permissão `APPROVE_SYSTEM_ACCESS` é reaproveitada — nenhuma permissão nova (AC: 5)
  - [ ] `APPROVE_SYSTEM_ACCESS` já foi criada na Story 3.2 (Task 6 dela) — **não** duplicar. Esta story só passa a **consumi-la** num segundo contexto (decisão de Perfil, não só nível 3 de Login).
  - [ ] Nova permissão só para o **gate** do endpoint de decisão: `DECIDE_PROFILE_APPROVAL_REQUEST` (mesmo espírito de `DECIDE_LOGIN_APPROVAL_REQUEST` — gate grosso; a regra fina "só quem tem `APPROVE_SYSTEM_ACCESS` decide de verdade" é 100% de negócio no Use Case, AC 5). Adicionar em `ScosOrganizationPermission.java` + `GET_PROFILE_APPROVAL_REQUEST` (consultar pendentes). `x-authorize` de `createProfile`/`updateProfileResources` **não muda** (`CREATE_PROFILE`/`UPDATE_PROFILE` continuam sendo quem pode *solicitar*; `DECIDE_PROFILE_APPROVAL_REQUEST`+`APPROVE_SYSTEM_ACCESS` é quem *decide* — mesma separação solicitante/aprovador de todo o Epic 3).

- [ ] Task 3: Contrato — `createProfile`/`updateProfileResources` viram solicitação; endpoints de decisão novos (AC: 1, 2, 3, 4)
  - [ ] Em `ScosOrganization_Login.yml`, `createProfile` (linha ~499-523): `description` passa a "UC-067 - Solicita a criação de um perfil de acesso. Fica pendente até aprovação (APPROVE_SYSTEM_ACCESS, FR-27) — o Perfil só existe de fato quando aprovado". **Resposta muda de `201`/recurso criado para `202`-equivalente do projeto** — como o padrão do projeto só usa `200/201/204/4XX/5XX` (nenhum `202` em uso em nenhum outro endpoint), **decisão de design**: manter `201` mas a resposta representa a **solicitação** criada (`ProfileApprovalRequest`), não o Perfil (que ainda não existe) — o corpo do `201` (se houver `data`) precisa reforçar isso na doc para não confundir consumidores da API. Confirmar com o usuário se isso é aceitável ou se vale quebrar o padrão de status HTTP aqui.
  - [ ] `updateProfileResources` (linha ~663-687): mesma lógica — `description` atualizada, `204` passa a significar "solicitação registrada", não "recursos já trocados".
  - [ ] Novos paths (mesmo arquivo ou um `ScosOrganization_ProfileApprovalRequest.yml` — mesma decisão de arquivo único-vs-separado da Story 3.2 Task 7, manter consistente com o que foi escolhido lá):
    - `GET /v1/profile-approval-requests` (filtro `status`) — `x-authorize: [GET_PROFILE_APPROVAL_REQUEST]`.
    - `GET /v1/profile-approval-requests/{id}` — idem.
    - `PUT /v1/profile-approval-requests/{id}/approve` — sem corpo (nenhum motivo de catálogo `Reason*` existe para isso — Perfil não tem histórico de status como Login/Company; **decisão de design**: sem `Reason*`, diferente de tudo mais no projeto, porque não existe catálogo aplicável — confirmar se o usuário quer criar um `ReasonProfileApproval` só para isto, ou aceitar a assimetria). `x-authorize: [DECIDE_PROFILE_APPROVAL_REQUEST]`. `204`.
    - `PUT /v1/profile-approval-requests/{id}/reject` — corpo `{observation}` (texto livre, sem catálogo, mesma razão acima). `204`.
  - [ ] **Não** criar uma segunda consulta de "quem tem `APPROVE_SYSTEM_ACCESS` hoje" — a Story 3.2 já cria `GET /v1/login-approval-requests/system-access-approvers` (Task 7 dela) para exatamente essa permissão, reaproveitada aqui sem mudança (o aprovador de `ProfileApprovalRequest` é o mesmo universo de pessoas que decide no nível 3 de `LoginApprovalRequest` — mesma permissão, mesma consulta). Se o nome do endpoint incomodar por estar sob `/login-approval-requests/` sem ser sobre Login, é um ajuste de nomenclatura pra decidir com o usuário quando as duas stories forem implementadas juntas — não motivo pra duplicar a consulta.

- [ ] Task 4: Entidade `ProfileApprovalRequest` + Use Cases (AC: 1, 2, 3, 4, 5)
  - [ ] `domain/access/profile/internal/ProfileApprovalRequest.java`, `ProfileApprovalRequestType.java` (`CREATE_PROFILE, UPDATE_RESOURCES`), `ProfileApprovalRequestStatus.java` (`PENDING, APPROVED, REJECTED`) — espelham a Task 1, incluindo `proposedResourceIds` (`List<UUID>`, `@JdbcTypeCode(SqlTypes.ARRAY)`). Métodos `approve(Login decidedBy, Instant now)`/`reject(Login decidedBy, Instant now)`, mesmo padrão de `LoginApprovalRequest` (Story 3.2), sem `isExceptionSelfApproval` (não existe conceito de "self" aqui — quem solicita não é necessariamente quem seria o aprovador de si mesmo, já que a decisão é sempre de terceiro com a permissão, não de uma pessoa específica resolvida).
  - [ ] `domain/access/profile/internal/ProfileApprovalRequestRepository.java` — `findAllFiltered(status, pageable)`. **Sem** método pra ler recursos propostos separadamente — `proposedResourceIds` já vem junto ao carregar a entidade (é coluna, não relação).
  - [ ] `usecase/access/profile/RequestCreateProfileUseCase(Bean)` — valida `code` único entre Perfis **e** entre outras `ProfileApprovalRequest(CREATE_PROFILE, PENDING)` (evitar duas solicitações do mesmo código simultâneas), grava a solicitação.
  - [ ] `usecase/access/profile/RequestUpdateProfileResourcesUseCase(Bean)` — valida `Profile` existe/ativo, grava a solicitação com `proposedResourceIds` (coluna `uuid[]`, sem tabela auxiliar).
  - [ ] `usecase/access/profile/DecideProfileApprovalRequestUseCase(Bean)` — checa `DECIDE_PROFILE_APPROVAL_REQUEST` (gate, já coberto por `x-authorize`) e, na regra fina, `APPROVE_SYSTEM_ACCESS` (AC 5 — sem isso, `403`/`422`, mesmo padrão da Story 3.2). Se `APPROVED` e `CREATE_PROFILE`: cria o `Profile` de fato (`ProfileRepository.save`) com `proposedCode`/`proposedDescription`, `active=true`. Se `APPROVED` e `UPDATE_RESOURCES`: substitui `ProfileResource` do Perfil pelo conjunto proposto (delete + insert, reaproveitando `ProfileResourceRepository`/`ProfileResourcePk` já existentes — mesma operação que `updateProfileResources` faria hoje se fosse síncrono). Se `REJECTED`: nada muda.
  - [ ] `api/delegate/profile/ProfileApprovalRequestDelegate.java` (ou estender `ProfileDelegate` se ele já existir de outra story — **checar antes**: nenhuma story anterior deste projeto criou `ProfileDelegate`, então provavelmente é novo aqui também, junto com `createProfile`/`updateProfileResources` que também nunca foram implementados).

- [ ] Task 5: Guarda de escopo (AC: 6)
  - [ ] **Não** tocar em `updateProfile` (código/descrição), `deleteProfile`, `activateProfile`/`inactivateProfile` — permanecem síncronos, fora do texto literal do FR-27.
  - [ ] **Não** criar cadeia de níveis nem SLA/escalonamento para `ProfileApprovalRequest` — 1 nível só (AC 5), diferente de `LoginApprovalRequest`.
  - [ ] **Não** implementar `getAllProfiles`/`getProfileById`/`getProfileResources` se ainda não existirem — fora do escopo desta story (só o necessário para a aprovação).

## Dev Notes

### Contexto crítico — leia antes de implementar

**Esta story não estava nas 5 originais do Epic 3 — é um gap encontrado nesta sessão de criação de stories.** FR-27 ("Criar um novo Perfil ou alterar quais Recursos um Perfil existente contém exige aprovação do mesmo grupo de FR-26") não tinha nenhuma story cobrindo-o: a Story 3.4 original só cobre FR-25 (Login trocando de Perfil já existente), nunca FR-27 (o próprio Perfil sendo criado ou tendo seus Recursos alterados). Confirmado por leitura de todos os ACs da Story 3.4 do épico — nenhum menciona `createProfile`/`updateProfileResources`.

**Esta é a story com mais decisões de arquitetura não-triviais do Epic 3 — leia todas as marcações "decisão de design" antes de implementar.** Resumo das 3 principais: (1) agregado de aprovação **separado** de `LoginApprovalRequest`, não generalização de uma tabela única; (2) `createProfile` continua respondendo `201` mesmo sem criar o Perfil de fato — resposta representa a solicitação, não o recurso final; (3) decisão de aprovar/rejeitar não tem catálogo `Reason*` (não existe um aplicável), diferente de todo o resto do projeto.

**Por que não há cadeia de 3 níveis aqui.** FR-23 (Login) é explícito sobre "Supervisor → Gerente → grupo" com SLA e escalonamento. FR-27 só diz "aprovação do mesmo grupo de FR-26" — e FR-26 já é resolução direta (sem cadeia, Login `EXTERNAL`/`SERVICE` vai direto pro grupo). Perfil não tem "dono" (Funcionário) para ancorar Supervisor/Gerente — não existe interpretação razoável de uma cadeia aqui. 1 nível é a leitura mais fiel ao texto, não uma simplificação arbitrária.

### Onde cada peça vai (camadas)

- `organization/flow-organization-resources/.../v1.0.0/tables/scos_profile_approval_request.yml` (novo) + `checks.yml`/`triggers.yml` (aditivos).
- `domain/access/profile/internal/`: `ProfileApprovalRequest` (com `proposedResourceIds uuid[]`), 2 enums, `ProfileApprovalRequestRepository` (novos).
- `usecase/access/profile/`: 3 Use Cases novos.
- `api/delegate/profile/`: `ProfileDelegate`/`ProfileApprovalRequestDelegate` (novos — nenhum existia antes).
- `infrastructure/enumaration/ScosOrganizationPermission.java`: `+DECIDE_PROFILE_APPROVAL_REQUEST`, `+GET_PROFILE_APPROVAL_REQUEST` (reaproveita `APPROVE_SYSTEM_ACCESS` da Story 3.2, sem duplicar).

### Testing Standards

- `RequestCreateProfileUseCaseBeanTest`/`RequestUpdateProfileResourcesUseCaseBeanTest`/`DecideProfileApprovalRequestUseCaseBeanTest` — padrão Mockito já convencionado.
- Integração: novo `ProfileControllerTest` — `createProfile` não deixa o Perfil visível em `GET /v1/profiles` até aprovar; `updateProfileResources` não muda `GET /v1/profiles/{id}/resources` até aprovar; após aprovar, os dois refletem.

### Project Structure Notes

- Pacotes novos: `usecase/access/profile`, `api/delegate/profile` — `Profile`/`Resource` (domain) já existiam (Story anterior desconhecida ou fundação do projeto), mas **nenhuma camada de aplicação** (`usecase`/`api`) existia para eles antes desta story — confirmado por busca no código (nenhum arquivo em `usecase/**/profile/**` ou `api/delegate/profile/**` antes desta sessão).

### References

- [Source: _bmad-output/planning-artifacts/epics.md] — FR-27 (Requirements Inventory), sem story própria antes desta sessão — gap confirmado por leitura de todo o Epic 3 do épico.
- [Source: etc/api/organization/ScosOrganization_Login.yml:499-523, 663-687] — `createProfile`/`updateProfileResources` atuais, síncronos.
- [Source: etc/api/organization/ScosOrganization_Login.yml:1137-1147] — `UpdateProfileResourcesRequest.resourceIds` já `format: uuid` — confirma `Resource.id` como `UUID`, não `Long`.
- [Source: organization/flow-organization-domain/.../access/resource/internal/Resource.java:47-50] — `@GeneratedValue(strategy = GenerationType.UUID)`, confirma o tipo.
- [Source: organization/flow-organization-domain/.../access/resource/internal/ProfileResource.java, ProfileResourcePk.java] — associação N:N já existente, reaproveitada na aprovação (Task 4).
- [Source: _bmad-output/implementation-artifacts/3-2-cadeia-aprovacao-criacao-login.md] — `APPROVE_SYSTEM_ACCESS` já criada lá, reaproveitada aqui num segundo contexto (não recriar).

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List

## Change Log

| Data | Descrição |
|------|-----------|
| 2026-08-16 | Story criada — gap encontrado na revalidação do Epic 3 contra o código atual: FR-27 (aprovação de criação de Perfil e de mudança do conjunto de Recursos) não tinha story própria nas 5 originais. Desenho novo, agregado `ProfileApprovalRequest` separado de `LoginApprovalRequest`, aprovação de 1 nível só (sem cadeia/SLA). Múltiplas decisões de arquitetura sinalizadas para confirmação antes do `dev-story`. |
| 2026-08-16 | Nota adicionada à Task 3 (revisão pedida pelo usuário): reaproveitar `GET /v1/login-approval-requests/system-access-approvers` (Story 3.2) para "quem pode decidir aqui" em vez de duplicar a consulta — mesma permissão `APPROVE_SYSTEM_ACCESS`. |
| 2026-08-16 | **Simplificação (ponytail-review pedido pelo usuário):** `SCOS_PROFILE_APPROVAL_REQUEST_RESOURCE` (tabela de junção + entidade `ProfileApprovalRequestResource`) removida — virou coluna `PROPOSED_RESOURCE_IDS uuid[]` na própria `SCOS_PROFILE_APPROVAL_REQUEST`, mesmo padrão já em produção em `vw_authority_response.permissions`. -1 tabela, -1 entidade, -1 FK. |
