---
baseline_commit: b72b2dcc0f63997fe313658e358e68dc506dd645
---

# Story 3.4: Perfis, Recursos e Aprovação de Mudança de Perfil

Status: ready-for-dev

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

- [ ] Task 1: Corrigir `vw_login_context` — perfis adicionais somam permissão (AC: 4)
  - [ ] Novo changeSet em `organization/flow-organization-resources/src/main/resources/db/changelog/organization/view/view.yml` (mesmo padrão `runOnChange: true` já usado por todas as views/triggers do projeto — **não** editar o arquivo `vw_login_context.sql` como se fosse a primeira versão; o changeSet aponta pro mesmo `.sql`, e o `.sql` em si é reescrito, porque views/triggers evoluem fora de versão, confirmado pelo padrão do projeto em `etc/architecture` — "function/view/procedure/triggers ficam na raiz, runOnChange:true").
  - [ ] Reescrever `organization/flow-organization-resources/src/main/resources/db/changelog/organization/view/vw_login_context.sql`:
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
  - [ ] Confirmar (teste de integração, Task 4) que um Login com Perfil adicional passa a herdar as permissões desse Perfil na próxima consulta a `vw_login_context`/`vw_authority_response` — **sem** depender do fluxo de aprovação desta story: um Perfil adicional já associado via `SCOS_LOGIN_PROFILE` antes desta migração já se beneficia da correção assim que a view for recriada.

- [ ] Task 2: Estender `SCOS_LOGIN_APPROVAL_REQUEST` — `REQUESTED_PROFILE_ID`/`PROFILE_CHANGE_KIND` (AC: 1)
  - [ ] Novo changeSet em `scos_login_approval_request.yml` (aditivo, mesmo arquivo da Story 3.2 Task 1 — **não** editar os `changeSet`s já aplicados, nem o desta própria sessão se a 3.2 já rodou):
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
  - [ ] `CHECK` novo em `checks.yml` (aditivo): `ADD CONSTRAINT chk_login_approval_request_profile_change_kind CHECK (profile_change_kind IS NULL OR profile_change_kind IN ('SET_PRIMARY', 'ADD_ADDITIONAL'))`.
  - [ ] `domain/access/login/internal/LoginApprovalRequest.java` (Story 3.2) ganha `requestedProfile` (`@ManyToOne` nullable → `Profile`) e `profileChangeKind` (novo enum `LoginApprovalRequestProfileChangeKind { SET_PRIMARY, ADD_ADDITIONAL }`, nullable).

- [ ] Task 3: `RequestLoginProfileChangeUseCase(Bean)` — abre a solicitação, Login não muda ainda (AC: 1, 5)
  - [ ] Em `etc/api/organization/ScosOrganization_Login.yml`, `updateLoginProfile` (linha ~304-325) e `createLoginProfile` (linha ~353-376): atualizar `description` — de "Substitui o perfil... Novas permissões valem na próxima validação" / "Adiciona um perfil adicional" para refletir que agora abre solicitação de aprovação, Login continua no perfil atual até a decisão. **Sem mudança de schema** — os dois já não têm `requestBody` (chave 100% via path param `profileId`), continuam assim.
  - [ ] Criar `usecase/access/login/RequestLoginProfileChangeUseCase(Bean).java` — `execute(Long loginId, Long profileId, ProfileChangeKind kind)`. Fluxo: carrega `Login`, valida `status == ACTIVE` (senão `422`, `SCOS_LOGIN_013`); valida `Profile` existe/ativo (reaproveitar `ProfileRepository`); valida ausência de `LoginApprovalRequest` `PENDING` (`SCOS_LOGIN_015`, mesma guarda da Story 3.3 — reaproveitar, **não duplicar** a checagem); para `kind=SET_PRIMARY`, opcionalmente short-circuit se `profileId == login.profile.id` (no-op, mas decisão de design: **deixar seguir e abrir a solicitação mesmo assim, ou devolver sem-op?** — esta story assume abrir a solicitação normalmente, mais simples, sem caso especial; ajustar se o usuário preferir um atalho); para `kind=ADD_ADDITIONAL`, a trigger `trg_validate_login_profile_not_primary` do banco já impede duplicar o principal como adicional — mas essa trigger é na tabela `SCOS_LOGIN_PROFILE`, que só é escrita na **aprovação** (Task 4), não nesta solicitação — validar isso na aplicação também (não dá pra confiar só na trigger, que só dispara tarde demais para dar um erro amigável na hora certa).
  - [ ] Monta `LoginApprovalRequest` (`requestType=CHANGE_PROFILE`, `requestedProfile`, `profileChangeKind`, `escalationPolicy=INDEFINITE`, cadeia resolvida do mesmo jeito — `LoginApprovalChainResolver.firstLevelFor` não muda, é sobre o Funcionário dono do Login, não sobre o Perfil).
  - [ ] `LoginDelegate.updateLoginProfile`/`createLoginProfile` chamam o novo Use Case em vez do que quer que fizesse a escrita direta antes (nenhum dos dois tinha Use Case implementado ainda — primeira implementação real).

- [ ] Task 4: `DecideLoginApprovalRequestUseCaseBean` — caso `CHANGE_PROFILE` (AC: 1, 2)
  - [ ] Substituir o `throw new IllegalStateException("Story 3.4")` (guarda temporária da Story 3.2 Task 1) pelo caso real:
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
    Import novo: `LoginProfileRepository` injetado no Use Case. `login.setProfile(...)` é aceitável aqui porque `profile` é um relacionamento simples (FK), não um estado com regra de transição — diferente de `status`, que sempre passa por método validado (`activate`/`enable`/`approve`/etc.).
  - [ ] **Não** gravar `OutboxEvent` para `CHANGE_PROFILE` — a Saga Keycloak (FR-6) só existe para o ciclo de vida do próprio Login (criar/reativar), não para troca de perfil. Confirmar isso é intencional lendo FR-6/FR-25 — nenhum dos dois menciona sincronização de Keycloak para troca de perfil.

- [ ] Task 5: Guarda de escopo (AC: 3)
  - [ ] **Não** tocar em `deleteLoginProfile` — continua exatamente como está (síncrono, idempotente, sem aprovação). Implementá-lo (sem `@Override` ainda) é permitido se sobrar tempo, mas não é o foco.
  - [ ] **Não** implementar FR-27 (aprovação de criação/edição do **Perfil em si** — quais Recursos ele agrega) — isso é a Story 3.6 (gap encontrado nesta sessão, fora do que o épico original previa para a 3.4). Esta story cobre só FR-25 (Login trocando de Perfil), não FR-27 (Perfil sendo criado/editado).
  - [ ] **Não** mexer no refresh das views de autoridade (`pg_cron`, 30 min) — já existe, já cobre a defasagem mencionada no AC 2.

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

### Debug Log References

### Completion Notes List

### File List

## Change Log

| Data | Descrição |
|------|-----------|
| 2026-08-16 | Story criada — Epic 3: (1) corrige `vw_login_context` para somar permissões de Perfis adicionais (bug independente, pode ser entregue isolado); (2) troca/adição de Perfil principal/adicional passa a exigir aprovação (`requestType=CHANGE_PROFILE`), reaproveitando a cadeia da Story 3.2; remoção de adicional continua síncrona. FR-27 (aprovação de criação/edição do Perfil em si) identificado como gap não coberto — vira Story 3.6. |
