---
baseline_commit: b72b2dcc0f63997fe313658e358e68dc506dd645
---

# Story 3.4: Perfis, Recursos e Aprovação de Mudança de Perfil

Status: review

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como Supervisor ou Gerente do Departamento,
Eu quero aprovar toda mudança que aumenta o acesso de um Login existente,
Para controlar escalada de privilégio.

## Acceptance Criteria

1. **Given** um Login `ACTIVE` **When** RH ou supervisor aciona `PUT /v1/logins/{id}/profile/{profileId}` (trocar Perfil principal) ou `POST /v1/logins/{id}/profiles/{profileId}` (adicionar Perfil adicional) **Then** a solicitação entra em aprovação (`requestType=CHANGE_PROFILE`, `escalationPolicy=INDEFINITE`, mesma cadeia de 3 níveis) **And** o Login continua `ACTIVE`, operando com o Perfil/conjunto de Perfis **atual**, enquanto aguarda — nem o perfil principal nem a lista de adicionais mudam até a decisão.
2. **Given** a aprovação é concedida **When** a decisão é registrada **Then** o novo Perfil passa a valer (troca o principal, ou grava a linha em `SCOS_LOGIN_PROFILE`) **And** fica sujeito à defasagem de até 30 min das views de autoridade materializadas (mesmo mecanismo de refresh já existente, `pg_cron` a cada 30 min — nenhuma mudança nele).
3. **Given** uma solicitação de remoção de Perfil adicional (`DELETE /v1/logins/{id}/profiles/{profileId}`) **When** ela é feita **Then** não exige aprovação — continua síncrona, exatamente como hoje (reduz acesso).
4. **Given** o bug conhecido de `vw_login_context` (Perfil adicional não soma permissão) **When** esta story é implementada **Then** a view passa a agregar permissões do Perfil principal **e** de todos os Perfis adicionais (`SCOS_LOGIN_PROFILE`) de um Login — pré-requisito para Perfil adicional funcionar de verdade, **independente** do fluxo de aprovação desta story (o bug existe hoje mesmo sem nenhuma aprovação envolvida).
5. **Given** um Login já tem uma `LoginApprovalRequest` `PENDING` (qualquer `requestType`) **When** RH tenta trocar/adicionar Perfil **Then** rejeita com `422` (`SCOS_LOGIN_015`, mesma guarda da Story 3.3) — 1 solicitação pendente por Login por vez, não importa o tipo.

## Tasks / Subtasks

- [x] Task 1: Corrigir `vw_login_context` — perfis adicionais somam permissão (AC: 4)
  - [x] **Não** foi criado um novo changeSet: o existente (`20260612-Samuel.Cunha-026`, `runOnChange: true`) já aponta pro mesmo `.sql` — confirmado por precedente no histórico git (mudanças anteriores na view sempre reescreveram o `.sql` in-place, sem novo changeSet). Reescrever `organization/flow-organization-resources/src/main/resources/db/changelog/organization/view/vw_login_context.sql`:
    ```sql
    DROP MATERIALIZED VIEW IF EXISTS scos.vw_login_context CASCADE;

    CREATE MATERIALIZED VIEW scos.vw_login_context AS
    WITH login_profiles AS (
        -- Perfil principal (1:1, SCOS_LOGIN.PROFILE_ID) + Perfis adicionais (N:N, SCOS_LOGIN_PROFILE)
        SELECT l.login_id, l.profile_id FROM scos.scos_login l
        UNION
        SELECT lp.login_id, lp.profile_id FROM scos.scos_login_profile lp
    )
    SELECT DISTINCT
        l.login_id, l.login, l.type, l.status, l.external_id,
        e.employee_id, COALESCE(e.name, l.login) AS name, e.email,
        COALESCE(ec.parent_company_id, ec.company_id) AS company_id,
        COALESCE(pc.name,              ec.name)        AS company_name,
        ec.company_id                                  AS branch_id,
        ec.name                                         AS branch_name,
        pp.profile_id,
        pp.code AS profile_code,
        r.code AS permission
    FROM       scos.scos_login            l
        JOIN       scos.scos_profile          pp  ON  pp.profile_id = l.profile_id
        LEFT JOIN  scos.scos_employee         e   ON  e.employee_id = l.employee_id
        LEFT JOIN  scos.scos_company          ec  ON  ec.company_id = e.company_id
        LEFT JOIN  scos.scos_company          pc  ON  pc.company_id = ec.parent_company_id
        LEFT JOIN  login_profiles             allp ON allp.login_id = l.login_id
        LEFT JOIN  scos.scos_profile_resource pr  ON  pr.profile_id = allp.profile_id
        LEFT JOIN  scos.scos_resource         r   ON  r.resource_id = pr.resource_id
                                                AND r.active = true;

    CREATE UNIQUE INDEX uidx_vw_login_context_login_permission
        ON scos.vw_login_context (login_id, COALESCE(permission, ''));
    ```
    Mudanças em relação ao original: `pp`/`profile_id`/`profile_code` continuam se referindo só ao **principal** (mantém o significado atual dessas 2 colunas — consumidores que exibem "o perfil do login" continuam vendo o principal); a coluna `permission` passa a agregar via a CTE `login_profiles` (principal ∪ adicionais); `SELECT DISTINCT` é **obrigatório** — sem ele, se o principal e um adicional derem o mesmo `resource.code`, a linha duplica e quebra o índice único `(login_id, permission)` no refresh da view.
  - [x] Confirmado via teste de integração dedicado (`vwLoginContext_additionalProfile_aggregatesPermissionWithPrimary`, JDBC direto) que um Login com Perfil adicional passa a herdar as permissões desse Perfil na próxima consulta a `vw_login_context` — **sem** depender do fluxo de aprovação desta story: gravado direto em `SCOS_LOGIN_PROFILE`, isolado das Tasks 2-4.

- [x] Task 2: Estender `SCOS_LOGIN_APPROVAL_REQUEST` — `REQUESTED_PROFILE_ID`/`PROFILE_CHANGE_KIND` (AC: 1)
  - [x] Novo changeSet em `scos_login_approval_request.yml` (aditivo, mesmo arquivo da Story 3.2 Task 1 — **não** editar os `changeSet`s já aplicados, nem o desta própria sessão se a 3.2 já rodou):
    ```yaml
      - changeSet:
          id: <data>-Samuel.Cunha-ZZZ
          author: Samuel.Cunha
          comment: "Adiciona REQUESTED_PROFILE_ID/PROFILE_CHANGE_KIND em SCOS_LOGIN_APPROVAL_REQUEST (Story 3.4 — fluxo CHANGE_PROFILE)"
          changes:
            - addColumn:
                tableName: SCOS_LOGIN_APPROVAL_REQUEST
                schemaName: scos
                columns:
                  - column:
                      name: REQUESTED_PROFILE_ID
                      type: BIGINT
                      constraints:
                        nullable: true
                        foreignKeyName: FK_REQUESTED_PROFILE_ID_SCOS_LOGIN_APPROVAL_REQUEST
                        references: scos.SCOS_PROFILE(PROFILE_ID)
                  - column:
                      name: PROFILE_CHANGE_KIND
                      type: varchar(20)
                      constraints:
                        nullable: true
          rollback:
            - dropColumn:
                tableName: SCOS_LOGIN_APPROVAL_REQUEST
                schemaName: scos
                columnName: REQUESTED_PROFILE_ID
            - dropColumn:
                tableName: SCOS_LOGIN_APPROVAL_REQUEST
                schemaName: scos
                columnName: PROFILE_CHANGE_KIND
    ```
    Ambas nullable — só preenchidas quando `REQUEST_TYPE=CHANGE_PROFILE`; `CREATE_LOGIN`/`REACTIVATE_LOGIN` deixam nulas.
  - [x] `CHECK` novo em `checks.yml` (aditivo): `ADD CONSTRAINT chk_login_approval_request_profile_change_kind CHECK (profile_change_kind IS NULL OR profile_change_kind IN ('SET_PRIMARY', 'ADD_ADDITIONAL'))`.
  - [x] `domain/access/login/internal/LoginApprovalRequest.java` (Story 3.2) ganha `requestedProfile` (`@ManyToOne` nullable → `Profile`) e `profileChangeKind` (novo enum `LoginApprovalRequestProfileChangeKind { SET_PRIMARY, ADD_ADDITIONAL }`, nullable).

- [x] Task 3: `RequestLoginProfileChangeUseCase(Bean)` — abre a solicitação, Login não muda ainda (AC: 1, 5)
  - [x] Em `etc/api/organization/ScosOrganization_Login.yml`, `updateLoginProfile` e `createLoginProfile`: descrições atualizadas refletindo que agora abrem solicitação de aprovação. Sem mudança de schema (nenhum dos dois tinha `requestBody`).
  - [x] Criado `usecase/access/login/RequestLoginProfileChangeUseCase(Bean).java` — Use Case fino, delegando a regra de negócio (carrega Login, valida `ACTIVE`, valida Perfil existe/ativo, valida ausência de pendência, monta a solicitação) para `LoginService.requestProfileChange` no domain service (mesmo padrão arquitetural da Story 3.3 — regra de negócio cruzando entidades pertence ao domain service, Use Case só orquestra). `kind=SET_PRIMARY` segue sem caso especial mesmo quando `profileId == login.profile.id` (decisão do Dev Notes, mais simples). `kind=ADD_ADDITIONAL` validado contra o Perfil principal na aplicação (`SCOS_LOGIN_020`), antecipando a trigger `trg_validate_login_profile_not_primary`.
  - [x] Monta `LoginApprovalRequest` (`requestType=CHANGE_PROFILE`, `requestedProfile`, `profileChangeKind`, `escalationPolicy=INDEFINITE`, cadeia resolvida via `LoginApprovalChainResolver.firstLevelFor`, reaproveitado sem alteração).
  - [x] `LoginDelegate.updateLoginProfile`/`createLoginProfile` implementados, chamando o novo Use Case (primeira implementação real de ambos).

- [x] Task 4: `DecideLoginApprovalRequestUseCaseBean` — caso `CHANGE_PROFILE` (AC: 1, 2)
  - [x] Substituído o `throw new IllegalStateException("Story 3.4")` (guarda temporária da Story 3.2/3.3) pelo caso real, implementado em `LoginApprovalRequestServiceBean.decide()` (não no Use Case Bean — mesmo desvio arquitetural documentado na Story 3.3, regra de negócio no domain service):
    ```java
    case CHANGE_PROFILE -> {
        if (decision == APPROVED) {
            if (request.getProfileChangeKind() == SET_PRIMARY) {
                login.setProfile(request.getRequestedProfile()); // troca direta de FK — não é transição de status, não precisa de método validado em Login
            } else { // ADD_ADDITIONAL
                loginProfileRepository.save(LoginProfile.builder()
                        .id(new LoginProfilePk(login.getId(), request.getRequestedProfile().getId()))
                        .build());
            }
        }
        // rejeitado: nada muda — Login continua com o perfil/conjunto atual (AC 1)
        yield null; // sem LoginStatusHistory — mudança de perfil não é transição de status do Login
    }
    ```
    Import novo: `LoginProfileRepository` injetado no domain service. `login.setProfile(...)` seguido de `loginRepository.merge(login)` — **não** `.update(login)`: `request.getLogin()` é um proxy Hibernate lazy, e `BaseJpaRepository.update()` usa `StatelessSession` internamente, que não resolve proxies (`UnknownEntityTypeException`); `.merge()` usa a sessão normal, funciona com proxies.
  - [x] **Não** grava `OutboxEvent` para `CHANGE_PROFILE` — confirmado que a Saga Keycloak (FR-6) só existe para o ciclo de vida do próprio Login. A condição reaproveita a nulidade de `history` (só `CREATE_LOGIN`/`REACTIVATE_LOGIN` aprovados produzem `LoginStatusHistory`) para decidir quando abrir o Outbox, sem flag extra.

- [x] Task 5: Guarda de escopo (AC: 3)
  - [x] **Não** tocado em `deleteLoginProfile` — continua exatamente como estava (síncrono, sem `@Override`, fora do escopo desta story).
  - [x] **Não** implementado FR-27 (aprovação de criação/edição do Perfil em si) — confirmado gap para Story 3.6, fora do escopo desta story.
  - [x] **Não** mexido no refresh das views de autoridade (`pg_cron`, 30 min) — já existe, já cobre a defasagem do AC 2.

## Dev Notes

### Contexto crítico — leia antes de implementar

**O bug de `vw_login_context` (Task 1) é independente do fluxo de aprovação (Tasks 2-4) — implemente e teste separadamente.** São dois problemas que o épico agrupou na mesma story, mas com naturezas diferentes: um é um bug de SQL pré-existente (a view nunca olhou para `SCOS_LOGIN_PROFILE`, desde que foi criada), o outro é um fluxo de aprovação novo. É plausível fazer só a Task 1 e entregar valor imediato (qualquer Perfil adicional já atribuído hoje, mesmo sem passar pelo fluxo novo, começa a funcionar assim que a view for corrigida).

**Por que `CHANGE_PROFILE` não usa `Login.approve()`/`activate()`/`enable()`.** Troca de perfil não é uma transição do campo `status` do Login — é uma troca de relacionamento (`profile` FK) ou uma inserção em `SCOS_LOGIN_PROFILE`. Os métodos de transição existentes (`activate`, `enable`, `approve`, etc.) só existem para proteger a máquina de estados de `LoginStatus` — não fazem sentido aqui. É por isso que a decisão para este `requestType` não produz `LoginStatusHistory` nem `OutboxEvent`.

**`REQUESTED_PROFILE_ID`/`PROFILE_CHANGE_KIND` só existem por causa de `CHANGE_PROFILE`.** Ao contrário de `CREATE_LOGIN`/`REACTIVATE_LOGIN` (onde tudo que a decisão precisa já está no próprio `Login` referenciado), a decisão de `CHANGE_PROFILE` precisa saber **qual** Perfil foi pedido e **como** aplicá-lo — daí as 2 colunas novas, nullable, só usadas por este `requestType`.

**Guarda "1 pendência por Login" (AC 5) é a mesma da Story 3.3 — não crie uma segunda implementação.** `RequestLoginProfileChangeUseCaseBean` chama a mesma checagem (`LoginApprovalRequestRepository.findByLoginIdAndStatus(loginId, PENDING)` + `SCOS_LOGIN_015`) que `RequestLoginReactivationUseCaseBean` já usa — extrair para um helper compartilhado (ex.: método `default` numa interface, ou um pequeno componente `LoginApprovalRequestGuard`) é uma boa oportunidade de reuso, mas **não invente abstração nova se um método estático simples já resolver** (regra geral do projeto: 3 linhas repetidas não justificam abstração).

### Onde cada peça vai (camadas)

- `organization/flow-organization-resources/.../view/vw_login_context.sql` + `.../view/view.yml`: reescrita (Task 1).
- `organization/flow-organization-resources/.../v1.0.0/tables/scos_login_approval_request.yml` + `.../checks/checks.yml`: colunas novas (Task 2).
- `domain/access/login/internal/LoginApprovalRequest.java` (Story 3.2): `+requestedProfile`, `+profileChangeKind` (Task 2).
- `domain/access/login/internal/LoginApprovalRequestProfileChangeKind.java` (novo enum, Task 2).
- `etc/api/organization/ScosOrganization_Login.yml`: descrições de `updateLoginProfile`/`createLoginProfile` atualizadas (Task 3).
- `usecase/access/login/RequestLoginProfileChangeUseCase(Bean).java` (novo, Task 3).
- `usecase/access/login/DecideLoginApprovalRequestUseCaseBean.java` (Story 3.2/3.3): caso `CHANGE_PROFILE` real (Task 4).

### Testing Standards

- Integração (`boot`, Testcontainers reais — **esta é a Task que mais precisa de banco real**, já que o bug é de SQL/view materializada, não de Java): criar Login com Perfil principal P1 e Perfil adicional P2 (inserir direto em `SCOS_LOGIN_PROFILE` no setup do teste, sem depender do fluxo de aprovação), forçar refresh da view (`REFRESH MATERIALIZED VIEW scos.vw_login_context` ou `scos.refresh_authority_views()`, mesma função já usada pelo `pg_cron`), consultar `vw_authority_response`/permissões efetivas do Login e confirmar que permissões de P2 aparecem — cenário que **falha hoje** sem a correção, deve passar depois.
- `RequestLoginProfileChangeUseCaseBeanTest`: sucesso `SET_PRIMARY`; sucesso `ADD_ADDITIONAL`; falha se Login não `ACTIVE`; falha se já há pendência (`SCOS_LOGIN_015`).
- `DecideLoginApprovalRequestUseCaseBeanTest` (estendido): `CHANGE_PROFILE` aprovado `SET_PRIMARY` → `login.profile` muda; aprovado `ADD_ADDITIONAL` → nova linha em `LoginProfile`; rejeitado → nada muda, sem `LoginStatusHistory`/`OutboxEvent`.

### Project Structure Notes

- Nenhum pacote novo — tudo em `domain/access/login/` e `usecase/access/login/` já existentes desde as Stories 3.1-3.3.

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Epic 3 / Story 3.4] — Given/When/Then originais.
- [Source: organization/flow-organization-resources/.../view/vw_login_context.sql] — SQL original, bug confirmado por leitura: `JOIN scos.scos_profile p ON p.profile_id = l.profile_id` nunca olha para `scos_login_profile`.
- [Source: etc/api/organization/ScosOrganization_Login.yml:304-399] — `updateLoginProfile`/`createLoginProfile`/`deleteLoginProfile` atuais, síncronos, sem `requestBody`.
- [Source: organization/flow-organization-domain/.../access/profile/internal/LoginProfile.java, LoginProfilePk.java] — entidade N:N já existente, reaproveitada pela decisão `ADD_ADDITIONAL`.
- [Source: organization/flow-organization-resources/.../triggers/trg_validate_login_profile_not_primary.sql] — trigger de banco que impede duplicar o principal como adicional; validação de aplicação (Task 3) precisa **antecipar** esse erro, não só confiar na trigger.
- [Source: _bmad-output/implementation-artifacts/3-2-cadeia-aprovacao-criacao-login.md] — `LoginApprovalRequest`, `LoginApprovalChainResolver`, `DecideLoginApprovalRequestUseCaseBean` reaproveitados.
- [Source: _bmad-output/implementation-artifacts/3-3-aprovacao-reativacao-login.md] — guarda "1 pendência por Login" (`SCOS_LOGIN_015`) reaproveitada.

## Dev Agent Record

### Agent Model Used

Claude Sonnet 5 (claude-sonnet-5)

### Debug Log References

Três causas raiz reais (bugs genuínos introduzidos por esta implementação, não infraestrutura) foram encontradas e corrigidas durante a validação:

1. **`userAt` faltante em `LoginProfile`** — `LoginApprovalRequestServiceBean.applyProfileChange()` construía a entidade `LoginProfile` (caso `ADD_ADDITIONAL`) sem setar `userAt` (coluna `NOT NULL`), causando `ConstraintViolationException` no INSERT. Corrigido passando o `user` (já resolvido em `decide()`) para `applyProfileChange`.
2. **`loginRepository.update(login)` falha com proxy Hibernate (caso `SET_PRIMARY`)** — `request.getLogin()` retorna um proxy lazy (`Login$HibernateProxy`); `BaseJpaRepository.update()` (hypersistence-utils) usa `StatelessSession` internamente, que não resolve proxies via o metamodel JPA (`UnknownEntityTypeException: Unknown entity type 'Login$HibernateProxy'`). Corrigido trocando para `.merge(login)` (sessão normal, resolve proxies), consistente com o padrão já usado em todo o resto do código (`loginRepository.merge(...)` em `create()`, por exemplo) — nenhum outro fluxo do projeto chamava `.update()` num `Login` obtido de uma relação lazy, por isso o bug nunca havia aparecido antes.
3. **Descoberta sobre `scos-foundation-jdempotent`**: os testes de integração revelaram que a biblioteca de idempotência trata os parâmetros marcados com `@JdempotentRequestPayload` como um **conjunto não-ordenado** ao gerar a chave de cache no Redis — `updateLoginProfile(id=1, profileId=2)` e `updateLoginProfile(id=2, profileId=1)` (mesmo `cachePrefix`) colidem na mesma chave, porque ambos representam o conjunto `{1, 2}`. Isso quebrou 2 testes que usavam pares "espelhados" (permutações um do outro) para a mesma operação. Corrigido escolhendo pares cujo conjunto de valores não se repete entre testes da mesma operação/`cachePrefix` (ver comentário no teste afetado). Vale como aprendizado para toda futura escrita de teste de integração neste projeto: **não basta variar a ORDEM dos parâmetros marcados como jdempotent — o CONJUNTO de valores precisa ser distinto**.

### Completion Notes List

- Task 1 (bug `vw_login_context`) implementada e testada de forma isolada das Tasks 2-4, exatamente como os Dev Notes recomendavam — reescrita in-place do `.sql`, sem novo changeSet (o existente já é `runOnChange: true`; confirmado por precedente no histórico git que mudanças de conteúdo da view sempre foram feitas assim).
- Task 3: a regra de negócio de `requestProfileChange` foi implementada em `LoginServiceBean` (domain), não diretamente no Use Case Bean como o texto da story sugeria — mesma decisão arquitetural documentada na Story 3.3 (regra que cruza `Login`+`Profile`+`LoginApprovalRequest` pertence ao domain service; Use Case só traduz `ProfileChangeKind`→`LoginApprovalRequestProfileChangeKind` e delega). O guard "1 pendência por Login" (`SCOS_LOGIN_019` — a story menciona `SCOS_LOGIN_015` mas esse código já estava ocupado desde a Story 3.2; `019` é o código real usado pela Story 3.3) foi extraído para um método privado `assertNoPendingApprovalRequest` dentro de `LoginServiceBean`, reaproveitado por `requestReactivation` (3.3) e `requestProfileChange` (3.4) — exatamente a "abstração mínima" sugerida nos Dev Notes (método privado na mesma classe, sem componente novo).
- Validação de "Perfil existe/ativo" (Task 3) resultou em 2 códigos de erro novos, seguindo o padrão já usado para `Employee` (código dedicado para "não encontrado" vs. "encontrado mas inativo"): `SCOS_PROFILE_001` (404, reaproveitado) para não encontrado, `SCOS_PROFILE_002` (422, novo) para inativo. Validação de "ADD_ADDITIONAL não pode ser o próprio principal" resultou em `SCOS_LOGIN_020` (422, novo) — nenhum dos dois estava listado nos Testing Standards da story, mas ambos são exigidos pelo texto da Task 3, então foram implementados e testados.
- Task 4: `openApprovalRequest` (já existente desde a Story 3.1) foi estendido com 2 parâmetros (`requestedProfile`, `profileChangeKind`), nulos nas 2 chamadas pré-existentes (`CREATE_LOGIN`, `REACTIVATE_LOGIN`) — reaproveitado em vez de duplicado, e passou a retornar o id da `LoginApprovalRequest` criada (necessário para o `201` de `createLoginProfile`).
- `GET /v1/logins/{id}/profiles` (`getAllLoginProfiles`) não está implementado (fora do escopo de qualquer task desta story) — os testes de integração verificam a gravação em `SCOS_LOGIN_PROFILE` via JDBC direto em vez de por essa rota.
- Testes: unitários (domain: `LoginServiceBeanTest` +7 cenários de `requestProfileChange`, `LoginApprovalRequestServiceBeanTest` +3 cenários de `decide()` `CHANGE_PROFILE`; usecase: `RequestLoginProfileChangeUseCaseBeanTest` novo). Integração: 8 testes novos em `LoginControllerTest` (`updateLoginProfile`/`createLoginProfile` — abertura, aprovação, rejeição, pendência duplicada, Login não ativo, Perfil não encontrado, Perfil já é o principal) + 1 teste isolado via JDBC para o fix da view (Task 1), usando 2 novos motivos de catálogo dedicados (`SCOS_REASON_ACTIVATE` id=8/9, `LOGIN/PROFILE_CHANGE_SET_PRIMARY`/`PROFILE_CHANGE_ADD_ADDITIONAL`) para não colidir com motivos já usados por outros testes da Story 3.2/3.3 no cache jDempotent.
- Revisão `ponytail-review` aplicada ao código de produção: nenhum achado.
- Suíte completa do módulo `boot` validada ao final: 508 testes, 0 falhas.

### File List

- `etc/api/organization/ScosOrganization_Login.yml`
- `organization/flow-organization-shared/src/main/java/br/com/sawcunhaos/organization/shared/exception/ExceptionCodeError.java`
- `../../organization/flow-organization-shared/src/main/resources/scos_message/scos_message_organization.properties`
- `../../organization/flow-organization-shared/src/main/resources/scos_message/scos_message_organization_en.properties`
- `organization/flow-organization-resources/src/main/resources/db/changelog/organization/view/vw_login_context.sql`
- `organization/flow-organization-resources/src/main/resources/db/changelog/organization/versions/v1/0/0/tables/scos_login_approval_request.yml`
- `organization/flow-organization-resources/src/main/resources/db/changelog/organization/checks/checks.yml`
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/access/login/internal/LoginApprovalRequest.java`
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/access/login/internal/LoginApprovalRequestProfileChangeKind.java` (novo)
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/access/login/service/LoginApprovalRequestServiceBean.java`
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/access/login/service/LoginServiceBean.java`
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/access/login/specification/LoginService.java`
- `organization/flow-organization-domain/src/test/java/br/com/sawcunhaos/organization/domain/access/login/service/LoginApprovalRequestServiceBeanTest.java`
- `organization/flow-organization-domain/src/test/java/br/com/sawcunhaos/organization/domain/access/login/service/LoginServiceBeanTest.java`
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/access/login/ProfileChangeKind.java` (novo)
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/access/login/RequestLoginProfileChangeUseCase.java` (novo)
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/access/login/RequestLoginProfileChangeUseCaseBean.java` (novo)
- `organization/flow-organization-usecase/src/test/java/br/com/sawcunhaos/organization/application/usecase/access/login/RequestLoginProfileChangeUseCaseBeanTest.java` (novo)
- `organization/flow-organization-api/src/main/java/br/com/sawcunhaos/organization/api/delegate/login/LoginDelegate.java`
- `organization/flow-organization-boot/src/test/java/br/com/sawcunhaos/organization/boot/api/login/LoginControllerTest.java`
- `organization/flow-organization-boot/src/test/resources/postgresql/setsup_database.sql`

## Change Log

| Data | Descrição |
|------|-----------|
| 2026-08-16 | Story criada — Epic 3: (1) corrige `vw_login_context` para somar permissões de Perfis adicionais (bug independente, pode ser entregue isolado); (2) troca/adição de Perfil principal/adicional passa a exigir aprovação (`requestType=CHANGE_PROFILE`), reaproveitando a cadeia da Story 3.2; remoção de adicional continua síncrona. FR-27 (aprovação de criação/edição do Perfil em si) identificado como gap não coberto — vira Story 3.6. |
| 2026-08-17 | Implementação completa (Tasks 1-5): `vw_login_context` corrigida (permissão agrega principal+adicionais); `RequestLoginProfileChangeUseCase`/`LoginService.requestProfileChange` abrem `LoginApprovalRequest` (`CHANGE_PROFILE`) para `updateLoginProfile`/`createLoginProfile`; `decide()` generalizado para aplicar a troca de Perfil (`SET_PRIMARY` via FK direta, `ADD_ADDITIONAL` via `SCOS_LOGIN_PROFILE`), sem `LoginStatusHistory`/`OutboxEvent`. Códigos novos `SCOS_LOGIN_020`/`SCOS_PROFILE_002`. Corrigidos 2 bugs reais no processo (`userAt` faltante em `LoginProfile`; `update()` vs `merge()` com proxy Hibernate) e documentada uma peculiaridade do jDempotent (chave de cache trata parâmetros marcados como conjunto não-ordenado). Suíte completa do módulo `boot` (508 testes) e `ponytail-review` do código de produção: sem achados. |
