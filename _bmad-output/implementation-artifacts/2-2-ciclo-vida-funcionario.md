---
baseline_commit: 38f943aeb7cbbe0fb9d458e0de21d5d565a0350b
---

# Story 2.2: Ciclo de Vida de Funcionário

Status: review

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como Analista de RH,
Eu quero ativar, inativar, bloquear e desbloquear um Funcionário sempre informando um motivo,
Para acompanhar o ciclo de vida dele com rastro auditável.

## Acceptance Criteria

1. **Given** um Funcionário `INACTIVE` **When** RH aciona `PUT /v1/employees/{id}/enable` (`ENABLE_EMPLOYEE`) informando `reasonId` válido de `SCOS_REASON_ACTIVATE` (`active=true`, `entityType=EMPLOYEE`) **Then** o Funcionário passa para `ACTIVE` (`204`) **And** uma linha é gravada em `SCOS_EMPLOYEE_STATUS_HISTORY` com `status=ACTIVE` **And** o `STATUS` da linha `SCOS_EMPLOYEE` é sincronizado pelo trigger `trg_sync_employee_status` já existente (nenhum UPDATE explícito na entidade) **And** nenhum Login vinculado é reativado (FR-14 — reativação nunca restaura estado de outro agregado).
2. **Given** um Funcionário `ACTIVE` **When** RH aciona `PUT /v1/employees/{id}/disable` (`DISABLE_EMPLOYEE`) informando `reasonId` válido de `SCOS_REASON_INACTIVATE` **Then** o Funcionário passa para `INACTIVE` (`204`) **And** grava histórico de status **And** **não** toca em nenhum Login vinculado (ver Dev Notes — "Por que o cascade de Login descrito no doc de spec não é implementado aqui").
3. **Given** um Funcionário em qualquer status **When** RH aciona `PUT /v1/employees/{id}/block` (`BLOCK_EMPLOYEE`) informando `reasonId` válido de `SCOS_REASON_DISABLE` **Then** só é aceito quando o status atual é `ACTIVE` — a partir de `INACTIVE` retorna `422 SCOS_EMPLOYEE_001` (mesma guarda que a entidade `Employee.disable()` já implementa; o texto "qualquer status" de `03-funcionario.md` diverge do YAML/entidade — seguir YAML/entidade, ver Dev Notes) **And**, quando aceito, o Funcionário passa para `DISABLED` (`204`) e grava histórico.
4. **Given** um Funcionário `DISABLED` **When** RH aciona `PUT /v1/employees/{id}/unblock` (`UNBLOCK_EMPLOYEE`) informando `reasonId` válido de `SCOS_REASON_ENABLE` **Then** o Funcionário passa para `ACTIVE` (`204`) **And** grava histórico **And** nenhum Login vinculado é reativado (mesma regra do AC 1).
5. **Given** um `{id}` de Funcionário inexistente, em qualquer uma das 4 rotas **When** a requisição chega **Then** o sistema rejeita com `404` código novo `SCOS_EMPLOYEE_014` (não existe hoje um "employee not found" reaproveitável — `SCOS_EMPLOYEE_001` é a transição inválida, `SCOS_EMPLOYEE_004` é especificamente "supervisor não encontrado", ambos de significado diferente).
6. **Given** um `reasonId` inativo (`active=false`) ou incompatível (`entityType != EMPLOYEE`), em qualquer uma das 4 rotas **When** a requisição chega **Then** o sistema rejeita com `422` — `enable` reaproveita os códigos já existentes `SCOS_EMPLOYEE_008`/`009` (criados na Story 2.1 para a mesma validação em `create()`); `disable`/`block`/`unblock` usam códigos novos desta story (`015`/`016`, `017`/`018`, `019`/`020`).
7. **Given** nenhum dos 4 Use Cases existe hoje para Employee (só a entidade de domínio com os 4 métodos de transição, já implementados desde antes desta story) **When** esta story é implementada **Then** cria os 4 do zero (`ActivateEmployeeUseCase`, `InactivateEmployeeUseCase`, `BlockEmployeeUseCase`, `UnblockEmployeeUseCase`, cada um com seu `Bean`), mesmo padrão de `ActivateCompanyUseCase`/`InactivateCompanyUseCase`/`BlockCompanyUseCase`/`UnblockCompanyUseCase` (Story 1.2).

## Tasks / Subtasks

- [x] Task 1: Códigos de erro novos — `SCOS_EMPLOYEE_014..020` (AC: 5, 6)
  - [x] **Antes de codar, confirme o próximo número livre**: `grep SCOS_EMPLOYEE_ ExceptionCodeError.java` — a Story 2.1 reserva `002..013`; se ela ainda não foi implementada ou reservou números diferentes, ajuste a sequência abaixo mantendo a mesma ordem lógica.
  - [x] Em `organization/flow-organization-shared/src/main/java/br/com/sawcunhaos/organization/shared/exception/ExceptionCodeError.java`, logo após a última entrada `SCOS_EMPLOYEE_0NN` existente, adicionar:
    ```java
    /** Funcionário informado não encontrado. HTTP 404. */
    SCOS_EMPLOYEE_014("SCOS_EMPLOYEE_014", 404, "SCOS_TITLE_NOT_FOUND"),
    /** Motivo de inativação informado está inativo. HTTP 422. */
    SCOS_EMPLOYEE_015("SCOS_EMPLOYEE_015", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Motivo de inativação incompatível com a entidade Funcionário (entityType != EMPLOYEE). HTTP 422. */
    SCOS_EMPLOYEE_016("SCOS_EMPLOYEE_016", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Motivo de bloqueio informado está inativo. HTTP 422. */
    SCOS_EMPLOYEE_017("SCOS_EMPLOYEE_017", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Motivo de bloqueio incompatível com a entidade Funcionário. HTTP 422. */
    SCOS_EMPLOYEE_018("SCOS_EMPLOYEE_018", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Motivo de desbloqueio informado está inativo. HTTP 422. */
    SCOS_EMPLOYEE_019("SCOS_EMPLOYEE_019", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Motivo de desbloqueio incompatível com a entidade Funcionário. HTTP 422. */
    SCOS_EMPLOYEE_020("SCOS_EMPLOYEE_020", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    ```
    **Não** criar códigos novos para "reason inactive/incompatible" da **rota `enable`** (transição de domínio `activate` — não confundir com a rota `unblock`, que é a transição de domínio `enable`) — ela reutiliza `SCOS_EMPLOYEE_008`/`009` (Story 2.1), chamando o mesmo método privado `validateReasonActivate` já implementado em `EmployeeServiceBean` (mesmo padrão de `CompanyServiceBean.validateReasonActivate`, reaproveitado por `create()` **e** `activate()`).
  - [x] Em `scos_message_organization.properties`, logo após a última chave `SCOS_EMPLOYEE_0NN`:
    ```properties
    SCOS_EMPLOYEE_014=Funcionário não encontrado.
    SCOS_EMPLOYEE_015=O motivo de inativação informado está inativo.
    SCOS_EMPLOYEE_016=O motivo de inativação informado é incompatível com a entidade Funcionário.
    SCOS_EMPLOYEE_017=O motivo de bloqueio informado está inativo.
    SCOS_EMPLOYEE_018=O motivo de bloqueio informado é incompatível com a entidade Funcionário.
    SCOS_EMPLOYEE_019=O motivo de desbloqueio informado está inativo.
    SCOS_EMPLOYEE_020=O motivo de desbloqueio informado é incompatível com a entidade Funcionário.
    ```
  - [x] Mesmas 7 chaves em `scos_message_organization_en.properties`, texto em inglês (espelhar `SCOS_COMPANY_012..017` como referência de fraseado).

- [x] Task 2: `EmployeeService` (specification) — adicionar 4 assinaturas à interface já existente (AC: 1, 2, 3, 4)
  - [x] Em `organization/flow-organization-domain/.../corporate/employee/specification/EmployeeService.java` (criada pela Story 2.1 com só `create`), adicionar:
    ```java
    /** Ativa um Funcionário INACTIVE, gravando o motivo em SCOS_EMPLOYEE_STATUS_HISTORY (status sincronizado por trigger). */
    void activate(@NonNull Long id, @NonNull Long reasonActivateId, String observation);

    /** Inativa um Funcionário ACTIVE/DISABLED, gravando o motivo (status sincronizado por trigger). */
    void inactivate(@NonNull Long id, @NonNull Long reasonInactivateId, String observation);

    /** Bloqueia um Funcionário ACTIVE (rota "block", corresponde ao método de domínio "disable"). */
    void disable(@NonNull Long id, @NonNull Long reasonDisableId, String observation);

    /** Desbloqueia um Funcionário DISABLED (rota "unblock", corresponde ao método de domínio "enable"). */
    void enable(@NonNull Long id, @NonNull Long reasonEnableId, String observation);
    ```
    **Não** criar uma segunda interface/arquivo — é o mesmo `EmployeeService` da Story 2.1, só ganhando métodos novos.

- [x] Task 3: `EmployeeServiceBean` — implementar os 4 métodos (AC: 1, 2, 3, 4, 5, 6)
  - [x] No mesmo arquivo `.../corporate/employee/service/EmployeeServiceBean.java` (criado pela Story 2.1), injetar 3 collaborators novos via campo `final` (o `@RequiredArgsConstructor` já existente atualiza o construtor sozinho — **não** escrever construtor manual): `ReasonInactivateService`, `ReasonDisableService`, `ReasonEnableService`. `ReasonActivateService` e `EmployeeStatusHistoryRepository` **já estão injetados** pela Story 2.1 — reaproveitar, não duplicar o campo.
  - [x] Implementar, espelhando exatamente `CompanyServiceBean.activate/inactivate/disable/enable` (`organization/flow-organization-domain/.../corporate/company/service/CompanyServiceBean.java:240-320`), **sem** as guardas de hierarquia que só existem em Company (`assertNotLastActiveMatrix`/`assertNoActiveDescendant`/`assertNotOnlyActiveCompany` não têm equivalente em Employee — nenhum AC desta story pede isso):
    ```java
    @Override
    @Transactional(rollbackFor = ScosException.class)
    public void activate(@NonNull Long id, @NonNull Long reasonActivateId, String observation) {
        log.info("Activate Employee: {}", id);
        Employee employee = findEmployeeById(id);
        validateReasonActivate(reasonActivateId); // já existe (Story 2.1) — reaproveitar, não duplicar
        String user = scosUserAuthentication.findUserAuthentication();

        EmployeeStatusHistory history = employee.activate(reasonActivateId);
        history.setObservation(observation);
        history.setUserAt(user);
        employeeStatusHistoryRepository.merge(history);
    }

    @Override
    @Transactional(rollbackFor = ScosException.class)
    public void inactivate(@NonNull Long id, @NonNull Long reasonInactivateId, String observation) {
        log.info("Inactivate Employee: {}", id);
        Employee employee = findEmployeeById(id);
        validateReasonInactivate(reasonInactivateId);
        String user = scosUserAuthentication.findUserAuthentication();

        EmployeeStatusHistory history = employee.inactivate(reasonInactivateId);
        history.setObservation(observation);
        history.setUserAt(user);
        employeeStatusHistoryRepository.merge(history);
    }

    @Override
    @Transactional(rollbackFor = ScosException.class)
    public void disable(@NonNull Long id, @NonNull Long reasonDisableId, String observation) {
        log.info("Disable (block) Employee: {}", id);
        Employee employee = findEmployeeById(id);
        validateReasonDisable(reasonDisableId);
        String user = scosUserAuthentication.findUserAuthentication();

        EmployeeStatusHistory history = employee.disable(reasonDisableId);
        history.setObservation(observation);
        history.setUserAt(user);
        employeeStatusHistoryRepository.merge(history);
    }

    @Override
    @Transactional(rollbackFor = ScosException.class)
    public void enable(@NonNull Long id, @NonNull Long reasonEnableId, String observation) {
        log.info("Enable (unblock) Employee: {}", id);
        Employee employee = findEmployeeById(id);
        validateReasonEnable(reasonEnableId);
        String user = scosUserAuthentication.findUserAuthentication();

        EmployeeStatusHistory history = employee.enable(reasonEnableId);
        history.setObservation(observation);
        history.setUserAt(user);
        employeeStatusHistoryRepository.merge(history);
    }

    private Employee findEmployeeById(@NonNull Long employeeId) {
        return employeeQueryRepository.findById(employeeId).orElseThrow(
                () -> new ScosException(SCOS_EMPLOYEE_014)
        );
    }

    private void validateReasonInactivate(Long reasonInactivateId) {
        ReasonInactivateOutput reason = reasonInactivateService.findById(reasonInactivateId);
        if (!reason.active()) {
            throw new ScosException(SCOS_EMPLOYEE_015);
        }
        if (reason.entityType() != EntityType.EMPLOYEE) {
            throw new ScosException(SCOS_EMPLOYEE_016);
        }
    }

    private void validateReasonDisable(Long reasonDisableId) {
        ReasonDisableOutput reason = reasonDisableService.findById(reasonDisableId);
        if (!reason.active()) {
            throw new ScosException(SCOS_EMPLOYEE_017);
        }
        if (reason.entityType() != EntityType.EMPLOYEE) {
            throw new ScosException(SCOS_EMPLOYEE_018);
        }
    }

    private void validateReasonEnable(Long reasonEnableId) {
        ReasonEnableOutput reason = reasonEnableService.findById(reasonEnableId);
        if (!reason.active()) {
            throw new ScosException(SCOS_EMPLOYEE_019);
        }
        if (reason.entityType() != EntityType.EMPLOYEE) {
            throw new ScosException(SCOS_EMPLOYEE_020);
        }
    }
    ```
  - [x] **Não** implementar guarda de status para `disable`/`inactivate`/`enable`/`activate` no Service — a guarda (`SCOS_EMPLOYEE_001`, estado atual incompatível com a transição) já está no agregado `Employee` (`Employee.java`, métodos `activate/inactivate/disable/enable`, já existentes antes desta story). O Service só busca a entidade, valida o motivo e chama o método de domínio — mesma divisão de responsabilidade de `CompanyServiceBean`.
  - [x] **Não** criar `findEmployeeById` como método público na interface `EmployeeService`, nem reaproveitar o `resolveActiveSupervisor` da Story 2.1 — são checagens de significado diferente (supervisor FK opcional vs. o próprio Funcionário alvo da transição), mesmo padrão de `CompanyServiceBean` ter seu próprio `findCompanyById` privado (não o `findById` de outro agregado).

- [x] Task 4: 4 Use Cases + Beans — pacote já existente (AC: 7)
  - [x] Em `organization/flow-organization-usecase/.../usecase/corporate/employee/` (pacote criado pela Story 2.1 para `CreateEmployeeUseCase`), criar, espelhando **exatamente** `ActivateCompanyUseCase(Bean)`/`InactivateCompanyUseCase(Bean)`/`BlockCompanyUseCase(Bean)`/`UnblockCompanyUseCase(Bean)` (`organization/flow-organization-usecase/.../corporate/company/`):
    ```java
    public interface ActivateEmployeeUseCase {
        void execute(@NonNull Long id, @NonNull EmployeeStatusTransitionRequest request);
    }
    ```
    ```java
    @Service
    @RequiredArgsConstructor
    @Slf4j
    @Transactional(rollbackFor = ScosException.class)
    class ActivateEmployeeUseCaseBean implements ActivateEmployeeUseCase {

        private final EmployeeService employeeService;

        @Override
        public void execute(@NonNull Long id, @NonNull EmployeeStatusTransitionRequest request) {
            log.info("Activate employee: {}", id);
            employeeService.activate(id, request.reasonId(), request.observation());
        }
    }
    ```
    Repetir para `InactivateEmployeeUseCase(Bean)` → `employeeService.inactivate(...)`, `BlockEmployeeUseCase(Bean)` → `employeeService.disable(...)`, `UnblockEmployeeUseCase(Bean)` → `employeeService.enable(...)`. DTO `EmployeeStatusTransitionRequest` já é gerado a partir do schema OpenAPI (`ScosOrganization_Employee.yml`, já publicado — nenhuma mudança de contrato necessária nesta story).

- [x] Task 5: `EmployeeDelegate` — adicionar 4 métodos à classe já existente (AC: 1, 2, 3, 4, 7)
  - [x] Em `organization/flow-organization-api/.../api/delegate/employee/EmployeeDelegate.java` (criada pela Story 2.1 com só `createEmployee`), injetar os 4 Use Cases novos (campos `final`, `@RequiredArgsConstructor` já existente cobre) e implementar, espelhando `CompanyDelegate.activateCompany/inactivateCompany/blockCompany/unblockCompany`:
    ```java
    @Override
    public Void activateEmployee(Long id, EmployeeStatusTransitionRequest employeeStatusTransitionRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        activateEmployeeUseCase.execute(id, employeeStatusTransitionRequest);
        return null;
    }

    @Override
    public Void inactivateEmployee(Long id, EmployeeStatusTransitionRequest employeeStatusTransitionRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        inactivateEmployeeUseCase.execute(id, employeeStatusTransitionRequest);
        return null;
    }

    @Override
    public Void blockEmployee(Long id, EmployeeStatusTransitionRequest employeeStatusTransitionRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        blockEmployeeUseCase.execute(id, employeeStatusTransitionRequest);
        return null;
    }

    @Override
    public Void unblockEmployee(Long id, EmployeeStatusTransitionRequest employeeStatusTransitionRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        unblockEmployeeUseCase.execute(id, employeeStatusTransitionRequest);
        return null;
    }
    ```
    **Rodar `mvn generate-sources` em `flow-organization-usecase`/`flow-organization-api` antes de escrever isto**, para confirmar a assinatura exata gerada de `EmployeeApiDelegate.activateEmployee(...)` etc. (mesma recomendação já seguida nas Stories 1.5 e 2.1 — o schema nunca foi exercitado pelo generator neste módulo ainda).
  - [x] Nenhuma mudança em `etc/api/organization/ScosOrganization_Employee.yml` — os 4 endpoints (`enable`/`disable`/`block`/`unblock`), o schema `EmployeeStatusTransitionRequest` e as 4 permissões (`ENABLE_EMPLOYEE`/`DISABLE_EMPLOYEE`/`BLOCK_EMPLOYEE`/`UNBLOCK_EMPLOYEE`, `ScosOrganizationPermission.java:113,114,117,118`) **já estão publicados** — confirmar com leitura, não reescrever.

- [x] Task 6: Seed de teste — motivos `EMPLOYEE` inativos, para exercitar os casos `422 reason inactive` em teste de integração (AC: 6)
  - [x] Em `organization/flow-organization-boot/src/test/resources/postgresql/setsup_database.sql`, **nenhum motivo `entityType=EMPLOYEE` com `active=false` existe hoje** em nenhum dos 4 catálogos (`SCOS_REASON_ACTIVATE/INACTIVATE/DISABLE/ENABLE`) — só há registros `COMPANY`/inativos (ids 5/4/4/4, usados pelos testes de Company). Adicionar, mesmo padrão das linhas 165-169/182-186/199-203/216-220 (já existentes para `COMPANY`), um registro inativo `EMPLOYEE` em `SCOS_REASON_INACTIVATE`, `SCOS_REASON_DISABLE` e `SCOS_REASON_ENABLE` (a Story 2.1 pode já ter resolvido o caso de `SCOS_REASON_ACTIVATE`/`EMPLOYEE` inativo para seu próprio teste de `create` — conferir antes de duplicar):
    ```sql
    -- (em SCOS_REASON_INACTIVATE, após a linha do ARCHIVED_REASON/COMPANY)
    INSERT INTO scos.SCOS_REASON_INACTIVATE (CODE, DESCRIPTION, ENTITY_TYPE, ACTIVE, UPDATED_AT, USER_AT)
    VALUES ('ARCHIVED_REASON', 'Motivo de inativação arquivado (inativo p/ testes)', 'EMPLOYEE', false, NOW(), 'seed')
    ON CONFLICT DO NOTHING;
    -- (mesmo padrão em SCOS_REASON_DISABLE e SCOS_REASON_ENABLE, ENTITY_TYPE='EMPLOYEE')
    ```
    Anotar o `..._ID` gerado (comentário `-- REASON_..._ID gerado: N`, mesmo estilo do arquivo) para uso nas constantes do teste de integração (Task 7).
  - [x] Para o teste `422 reason incompatible` (entityType errado), **não** precisa de seed novo — reaproveitar um id `COMPANY`-scoped já existente (ex.: `REASON_INACTIVATE_ID=1`, `COMPANY_CLOSED`) aplicado a uma rota de Employee, mesmo truque já usado por `CompanyControllerTest` na direção oposta (reason `EMPLOYEE` aplicado a rota de Company).

- [x] Task 7: Testes (AC: 1, 2, 3, 4, 5, 6, 7)
  - [x] `EmployeeServiceBeanTest.java` (já existe, criado pela Story 2.1) — adicionar blocos `activate*`/`inactivate*`/`disable*`/`enable*`, espelhando **exatamente** os 23 testes de `CompanyServiceBeanTest.java:396-720` (`activateShouldPersistHistoryAndNotUpdateCompanyWhenValid`, `activateShouldThrowWhenCompanyIsNotInactive`, `activateShouldThrowWhenReasonIsInactive`, `activateShouldThrowWhenReasonEntityTypeIsIncompatible`, e o mesmo padrão × 4 verbos) — trocando `Company`/`company` por `Employee`/`employee`, `StatusCompany` por `StatusEmployee`, e omitindo os testes de guarda de hierarquia (`...LastActiveMatrix`/`...OnlyActiveCompany`/`...ActiveDescendant` — sem equivalente em Employee). Adicionar também: `activate/inactivate/disable/enableShouldThrowWhenEmployeeNotFound` → `SCOS_EMPLOYEE_014` (novo, sem equivalente direto em Company porque lá `findCompanyById` já tinha teste próprio de outros ACs).
  - [x] 4 arquivos novos em `flow-organization-usecase/src/test/.../usecase/corporate/employee/`: `ActivateEmployeeUseCaseBeanTest.java`, `InactivateEmployeeUseCaseBeanTest.java`, `BlockEmployeeUseCaseBeanTest.java`, `UnblockEmployeeUseCaseBeanTest.java` — mesmo padrão exato de `InactivateCompanyUseCaseBeanTest.java` (2 testes cada: delega `id`/`reasonId`/`observation` ao service correto; propaga `ScosException` quando o service falha).
  - [x] `EmployeeControllerTest.java` (já existe, criado pela Story 2.1) — adicionar 4 blocos (`enable`/`disable`/`block`/`unblock`), espelhando `CompanyControllerTest.java:301-364` (inclui o teste `block_seededActive_returns204AndSyncsStatusViaTrigger`, provando end-to-end que `trg_sync_employee_status` sincroniza `SCOS_EMPLOYEE.STATUS` via `GET` depois do `PUT`). Cobrir por rota: sucesso `204`; `{id}` inexistente `404 SCOS_EMPLOYEE_014`; status incompatível `422 SCOS_EMPLOYEE_001` (`enable` a partir de `ACTIVE`, `disable` a partir de `INACTIVE` — **não** testar `DISABLED` aqui, ver Dev Notes, `Employee.inactivate()` aceita `DISABLED` como origem válida e retorna `204`; `block` a partir de `INACTIVE`, `unblock` a partir de `ACTIVE`/`INACTIVE`); `reasonId` inativo e incompatível (`422`, códigos da Task 1); `401` sem token; `403` sem a permissão da rota. O Funcionário seed (`EMPLOYEE_ID=1`, `ACTIVE`) pode ser mutado diretamente pelos testes de transição — mesmo padrão já usado por `CompanyControllerTest` no `COMPANY_ID=1` seed (isolamento entre testes já garantido pela infra herdada de `ScosOrganizationTestUtil`, não recriar aqui).

- [x] Task 8: Guarda de escopo (AC: 1, 2, 3, 4)
  - [x] **Não** implementar nenhum cascade para `Login` vinculado ao Funcionário. `etc/doc/usecase/03-funcionario.md` (linhas 168-170 e 187-189) e a descrição do YAML (`disable`/`block`) mencionam "Logins vinculados são inativados e desabilitados no Keycloak via Saga" — **esse mecanismo não existe no código hoje** (não há `LoginService`/`LoginServiceBean` com transição de status, nem Saga/Outbox para Keycloak; `Login.java` só tem `rules/LoginInactiveRule`/`LoginBlockedRule` de leitura). É uma referência antecipada ao Epic 3 (Login com Aprovação, hoje `backlog`), que só existe depois que `LoginApprovalRequest`/mecanismo de aprovação forem construídos. O AC desta story (epics.md) **não** pede esse cascade — implementá-lo aqui exigiria inventar uma dependência que ainda não existe. Deixar como nota de gap conhecido entre o doc de spec e o estado real do código, não código morto/half-implementado.
  - [x] **Não** implementar `GET /v1/employees/{id}/status-history` (`UC-140`, permissão `GET_EMPLOYEE_STATUS_HISTORY` já existe) — fora do AC desta story, e `CompanyDelegate` (referência) também não implementa o equivalente `getCompanyStatusHistory` ainda (mesmo gap, não é regressão introduzida aqui).
  - [x] **Não** tocar em `rehire`/`transfer`/`hierarchy`/`subordinates`/`position-history`/`GET /v1/employees`/`GET /v1/employees/{id}` — Stories 2.3 e além.
  - [x] **Não** adicionar guarda de "última matriz ativa"/"única empresa ativa"/"descendente ativo" a Employee — são regras exclusivas da hierarquia de Company (Story 1.3), sem equivalente em Employee (Funcionário não tem subárvore).

## Dev Notes

### Contexto crítico — leia antes de implementar

**Isto é extensão, não greenfield.** Diferente da Story 2.1 (onde nada de Employee existia além da entidade), esta story parte de uma base já criada por ela: `EmployeeService`/`EmployeeServiceBean` (domain), `EmployeeDelegate` (api) e `EmployeeServiceBeanTest`/`EmployeeControllerTest` já existem quando esta story começa. **Todo o trabalho aqui é adicionar métodos a arquivos existentes — nunca criar uma segunda interface/classe/arquivo de teste com nome parecido.** Se a Story 2.1 ainda não foi implementada quando esta for iniciada, implemente-a primeiro (dependência de ordem, não apenas de épico) — os 4 métodos novos de `EmployeeService` pressupõem os campos `EmployeeQueryRepository`/`EmployeeStatusHistoryRepository`/`ReasonActivateService`/`ScosUserAuthentication` já injetados por ela, e o método privado `validateReasonActivate` já implementado por ela é reaproveitado (não reescrito) pelo `activate()` desta story.

**O contrato inteiro já está publicado — zero mudança de OpenAPI, Liquibase ou permissão nesta story.** Confirmado por leitura direta: os 4 endpoints (`PUT /v1/employees/{id}/enable|disable|block|unblock`), o schema `EmployeeStatusTransitionRequest`, as 4 permissões (`ENABLE_EMPLOYEE`/`DISABLE_EMPLOYEE`/`BLOCK_EMPLOYEE`/`UNBLOCK_EMPLOYEE`) e o trigger de sincronização de status (`trg_sync_employee_status`/`fn_sync_employee_status.sql`) já existem no repositório. Os 4 métodos de domínio (`Employee.activate/inactivate/disable/enable`, que retornam `EmployeeStatusHistory` sem persistir) também já existem, com a guarda de estado (`SCOS_EMPLOYEE_001`) já implementada. O trabalho desta story é **só** a camada de aplicação que falta: Use Case → Service → persistência do histórico, exatamente como a Story 1.2 fez para Company.

**`epics.md` também diverge no precondicionante do `block`:** o texto-fonte da story em `epics.md` usa "Given um Funcionário INACTIVE, When RH aciona block" — mas isso é redação solta do épico (rascunho menos detalhado que Story 1.2), não um requisito verificado. A entidade `Employee.disable()` (já implementada) e a Story 1.2/Company (precedente direto) concordam: `block` só aceita a partir de `ACTIVE`. Este story-file já resolve a favor do código real (AC 3) — não seguir a redação literal de `epics.md` neste ponto específico.

**Divergência doc × YAML/entidade no `block`:** `etc/doc/usecase/03-funcionario.md` (linha 178) descreve `UC-043`/`block` como "Transição qualquer → DISABLED" e não lista nenhum erro de estado incompatível na tabela "Use Cases de Erro". Isso está **incompleto** — a descrição do YAML (`ScosOrganization_Employee.yml:231`) é explícita: *"Só é aceito quando o status atual é ACTIVE - a partir de INACTIVE retorna 4XX"*, e o método de domínio `Employee.disable()` (chamado pela rota `block`) já lança `SCOS_EMPLOYEE_001` se o status atual não for `ACTIVE`. Siga o YAML/entidade (fonte de verdade de comportamento real), não a tabela do doc — mesma divergência (e mesma resolução) já existe no par Company doc/YAML/`CompanyServiceBean.disable`.

**Divergência doc/YAML × entidade no `disable` (sentido oposto ao do `block`):** `03-funcionario.md` (UC-040) e o YAML dizem "Status deve ser ACTIVE"/"transição ACTIVE para INACTIVE" para a rota `disable`. Mas `Employee.inactivate()` (já implementado, `Employee.java:124-137`) só rejeita se o status atual já for `INACTIVE` — **aceita `ACTIVE` OU `DISABLED`** como origem (javadoc do próprio método: "Encerra definitivamente o funcionário a partir de ACTIVE ou DISABLED"). Aqui é o entity que é **mais permissivo** que doc/YAML, o oposto do caso `block` (onde doc era mais permissivo que entity/YAML). Mesmo padrão já existe em `Company.inactivate()` (idêntico, não é bug introduzido por esta story) — **não** adicionar guarda extra no Service para restringir a `ACTIVE` apenas; isso mudaria comportamento já em produção para Company sem pedido de nenhum AC. Ao testar, `disable` a partir de `DISABLED` deve retornar `204` (sucesso), não `422` — só `disable` a partir de `INACTIVE` já-inativo é que dispara `SCOS_EMPLOYEE_001`.

**Por que o cascade de Login descrito no doc de spec não é implementado aqui:** ver Task 8. Resumo: a infraestrutura que o cascade dependeria (`LoginService` com transição de status, Saga/Outbox Keycloak) é escopo do Epic 3 (`backlog` no sprint status), que sequencialmente vem **depois** do Epic 2. Implementar o cascade agora exigiria inventar classes que nenhuma story ainda especificou.

### Onde cada peça vai (camadas)

- **`domain/corporate/employee/specification/EmployeeService.java`** (já existe — 4 assinaturas novas).
- **`domain/corporate/employee/service/EmployeeServiceBean.java`** (já existe — 4 métodos novos + `findEmployeeById`/`validateReasonInactivate`/`validateReasonDisable`/`validateReasonEnable` privados + 3 novos campos injetados).
- **`shared/exception/ExceptionCodeError.java`** + **`scos_message_organization[_en].properties`**: 7 códigos novos (`SCOS_EMPLOYEE_014..020`).
- **`usecase/corporate/employee/`** (já existe, pacote da Story 2.1): 4 pares Use Case/Bean novos.
- **`api/delegate/employee/EmployeeDelegate.java`** (já existe — 4 `@Override` novos).
- **`flow-organization-boot/src/test/resources/postgresql/setsup_database.sql`**: até 3 linhas de seed novas (motivos `EMPLOYEE` inativos em `INACTIVATE`/`DISABLE`/`ENABLE`).
- Nenhuma mudança em `etc/api/organization/ScosOrganization_Employee.yml`, `ScosOrganizationPermission.java` ou Liquibase.

### Testing Standards

- Unitário de domínio: `@ExtendWith(MockitoExtension.class)` + `@Mock`/`@InjectMocks`, mesmo padrão de `CompanyServiceBeanTest` (`assertThatThrownBy(...).hasFieldOrPropertyWithValue("code", SCOS_EMPLOYEE_0XX.getCode())`), adicionado ao `EmployeeServiceBeanTest` já existente.
- Unitário de Use Case: mesmo padrão BDD (`then(...).should()...`/`willThrow`) de `InactivateCompanyUseCaseBeanTest`.
- Integração: adicionar aos blocos já existentes de `EmployeeControllerTest.java` (`extends ScosOrganizationTestUtil`, containers `static`, **nunca** `@Testcontainers`/`@Container` na subclasse). Cada teste que muta o Funcionário seed (`EMPLOYEE_ID=1`) segue o mesmo padrão já usado por `CompanyControllerTest` no `COMPANY_ID=1`.

### Project Structure Notes

- Nenhum pacote Maven novo — tudo cai em pacotes já criados pela Story 2.1 (`usecase/corporate/employee/`, `api/delegate/employee/`) ou já existentes desde antes dela (`domain/corporate/employee/service|specification/`).
- Nenhuma mudança de Liquibase, OpenAPI ou permissão — confirmado por leitura, ver "Contexto crítico" acima.

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Epic 2 / Story 2.2] — Given/When/Then originais.
- [Source: etc/doc/usecase/03-funcionario.md#PUT /v1/employees/{id}/enable|disable|block|unblock, UC-039/040/043/139] — regras por rota, "Use Cases de Erro"; divergência de `block` sinalizada em Dev Notes.
- [Source: etc/api/organization/ScosOrganization_Employee.yml:162-286,1260-1273] — os 4 endpoints e o schema `EmployeeStatusTransitionRequest`, já publicados, nenhuma mudança necessária.
- [Source: organization/flow-organization-infrastructure/.../enumaration/ScosOrganizationPermission.java:113-118] — `ENABLE_EMPLOYEE`/`DISABLE_EMPLOYEE`/`BLOCK_EMPLOYEE`/`UNBLOCK_EMPLOYEE` já cadastradas.
- [Source: organization/flow-organization-domain/.../corporate/employee/internal/Employee.java:109-167] — os 4 métodos de transição de status já implementados (`activate/inactivate/disable/enable`), guarda `SCOS_EMPLOYEE_001` já lançada por eles.
- [Source: organization/flow-organization-domain/.../access/status/internal/EmployeeStatusHistory.java, EmployeeStatusHistoryRepository.java] — entidade/repositório já existentes, mesma forma de `CompanyStatusHistory`.
- [Source: organization/flow-organization-resources/.../triggers/trg_sync_employee_status.sql, function/fn_sync_employee_status.sql] — trigger de sincronização de `SCOS_EMPLOYEE.STATUS` já implementado.
- [Source: organization/flow-organization-domain/.../corporate/company/specification/CompanyService.java, .../company/service/CompanyServiceBean.java:240-320,328-384] — padrão exato a replicar: 4 assinaturas + 4 implementações + 4 validadores privados de motivo.
- [Source: organization/flow-organization-usecase/.../corporate/company/ActivateCompanyUseCase(Bean).java, InactivateCompanyUseCase(Bean).java, BlockCompanyUseCase(Bean).java, UnblockCompanyUseCase(Bean).java] — padrão exato dos 4 Use Cases (Story 1.2).
- [Source: organization/flow-organization-api/.../delegate/company/CompanyDelegate.java:90-112] — padrão exato dos 4 métodos de Delegate.
- [Source: organization/flow-organization-domain/.../access/status/internal/EntityType.java] — `EntityType.EMPLOYEE` já existe, usado pelos 3 novos validadores de motivo.
- [Source: organization/flow-organization-domain/.../access/status/specification/ReasonInactivateService.java, ReasonDisableService.java, ReasonEnableService.java, dto/ReasonInactivateOutput.java, ReasonDisableOutput.java, ReasonEnableOutput.java] — serviços/DTOs de catálogo já genéricos (cross-aggregate), reaproveitados sem alteração.
- [Source: organization/flow-organization-shared/.../exception/ExceptionCodeError.java:78-113] — blocos `Company`/`Employee` existentes; `SCOS_EMPLOYEE_014..020` novos desta story.
- [Source: organization/flow-organization-domain/src/test/.../company/service/CompanyServiceBeanTest.java:396-720] — os 23 testes de `activate/inactivate/disable/enable` a replicar (menos as guardas de hierarquia, exclusivas de Company).
- [Source: organization/flow-organization-usecase/src/test/.../company/InactivateCompanyUseCaseBeanTest.java] — padrão exato de teste de Use Case a replicar × 4.
- [Source: organization/flow-organization-boot/src/test/.../company/CompanyControllerTest.java:39-75,301-364] — padrão de constantes/helpers e dos testes de integração de transição (incluindo a prova end-to-end via trigger) a replicar.
- [Source: organization/flow-organization-boot/src/test/resources/postgresql/setsup_database.sql:153-231,277-304] — seeds já existentes de `SCOS_REASON_*` (`EMPLOYEE`, ids 2 em cada catálogo, todos `active=true`) e do Funcionário seed (`EMPLOYEE_ID=1`, `ACTIVE`); ausência de seed `EMPLOYEE`/inativo mapeada na Task 6.
- [Source: _bmad-output/implementation-artifacts/2-1-admissao-funcionario-copia-jornada-trabalho.md] — story anterior: origem de `EmployeeService`/`EmployeeServiceBean`/`EmployeeDelegate`/`EmployeeServiceBeanTest`/`EmployeeControllerTest`, e dos códigos `SCOS_EMPLOYEE_002..013` já reservados (não colidir).
- [Source: _bmad-output/implementation-artifacts/1-2-ciclo-vida-completo-empresa.md] — story de referência de qualidade/formato (padrão Company replicado ponto a ponto nesta story).

## Dev Agent Record

### Agent Model Used

Claude Sonnet 5 (claude-sonnet-5)

### Debug Log References

- `mvn -pl organization/flow-organization-domain,organization/flow-organization-usecase -am test` — suíte completa dos 2 módulos: 0 falhas, 0 erros.
- `mvn -pl organization/flow-organization-boot test` (com `-Denforcer.skip=true` só para rodar localmente — ver nota abaixo) — 15 classes de teste de integração, 421 testes, 0 falhas, 0 erros. `EmployeeControllerTest`: 37/37 verde.
- Nota de ambiente: `flow-organization-boot`/`flow-organization-infrastructure` falham no `maven-enforcer-plugin` (`RequireUpperBoundDeps`) por divergências de versão pré-existentes e não relacionadas a esta story (hibernate-core, error_prone_annotations, prometheus-metrics, okio, puxadas por módulos de fundação/infra). Rodei os testes de integração com `-Denforcer.skip=true` só nesta sessão de validação, sem alterar nenhum `pom.xml`.
- Bug de teste encontrado e corrigido durante a validação: os testes de integração das 4 rotas de transição usavam o mesmo payload (`reasonId` sem `observation`) em múltiplas chamadas de precondição, colidindo com o cache de idempotência (`x-jdempotentrequestpayload`/Redis, TTL configurado no YAML) — a 2ª chamada com payload idêntico recebia a resposta cacheada da 1ª sem executar a lógica de negócio de fato. Corrigido adicionando um nonce (`observation` com timestamp) nas chamadas de setup (helper `transition()`). Também troquei a verificação pós-trigger de `GET /v1/employees/{id}` (rota inexistente — fora de escopo desta story, ver Task 8) por leitura direta via `employeeQueryRepository`.

### Completion Notes List

- Ao iniciar esta execução, as Tasks 1–6 (códigos de erro, `EmployeeService`/`EmployeeServiceBean`, os 4 Use Cases/Beans, `EmployeeDelegate`, seed SQL) já estavam implementadas em uma sessão anterior (working tree com mudanças não commitadas), mas os checkboxes da story ainda não refletiam isso. Revalidei cada uma lendo o diff correspondente antes de marcar como concluída.
- Task 7 (testes) estava incompleta: faltavam os 4 arquivos `*UseCaseBeanTest` (Activate/Inactivate/Block/Unblock) e os blocos de integração das 4 rotas em `EmployeeControllerTest`. Criei os 4 testes de Use Case espelhando `InactivateCompanyUseCaseBeanTest` (2 testes cada: delegação e propagação de `ScosException`) e adicionei ~350 linhas de testes de integração cobrindo, por rota: sucesso 204 + prova de sincronização via trigger, 404 (id inexistente), 422 de status incompatível (guarda de domínio), 422×2 de motivo inativo/incompatível, 401 e 403.
- Corrigido também um erro de compilação pré-existente em `EmployeeServiceBeanTest.java` (imports faltando de `ReasonInactivateOutput`/`ReasonDisableOutput`/`ReasonEnableOutput`) que impedia o módulo de compilar.
- Task 8 (guarda de escopo) não exigia código — apenas confirmar que nenhum cascade de Login, `GET /v1/employees/{id}`, `GET /v1/employees/{id}/status-history` ou lógica de `rehire`/hierarquia foi implementada nesta story. Confirmado por leitura: nada disso existe no diff.

### File List

**Modificados:**
- `organization/flow-organization-shared/src/main/java/br/com/sawcunhaos/organization/shared/exception/ExceptionCodeError.java`
- `../../organization/flow-organization-shared/src/main/resources/scos_message/scos_message_organization.properties`
- `../../organization/flow-organization-shared/src/main/resources/scos_message/scos_message_organization_en.properties`
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/corporate/employee/specification/EmployeeService.java`
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/corporate/employee/service/EmployeeServiceBean.java`
- `organization/flow-organization-domain/src/test/java/br/com/sawcunhaos/organization/domain/corporate/employee/service/EmployeeServiceBeanTest.java`
- `organization/flow-organization-api/src/main/java/br/com/sawcunhaos/organization/api/delegate/employee/EmployeeDelegate.java`
- `organization/flow-organization-boot/src/test/resources/postgresql/setsup_database.sql`
- `organization/flow-organization-boot/src/test/java/br/com/sawcunhaos/organization/boot/api/employee/EmployeeControllerTest.java`

**Novos:**
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/corporate/employee/ActivateEmployeeUseCase.java`
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/corporate/employee/ActivateEmployeeUseCaseBean.java`
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/corporate/employee/InactivateEmployeeUseCase.java`
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/corporate/employee/InactivateEmployeeUseCaseBean.java`
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/corporate/employee/BlockEmployeeUseCase.java`
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/corporate/employee/BlockEmployeeUseCaseBean.java`
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/corporate/employee/UnblockEmployeeUseCase.java`
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/corporate/employee/UnblockEmployeeUseCaseBean.java`
- `organization/flow-organization-usecase/src/test/java/br/com/sawcunhaos/organization/application/usecase/corporate/employee/ActivateEmployeeUseCaseBeanTest.java`
- `organization/flow-organization-usecase/src/test/java/br/com/sawcunhaos/organization/application/usecase/corporate/employee/InactivateEmployeeUseCaseBeanTest.java`
- `organization/flow-organization-usecase/src/test/java/br/com/sawcunhaos/organization/application/usecase/corporate/employee/BlockEmployeeUseCaseBeanTest.java`
- `organization/flow-organization-usecase/src/test/java/br/com/sawcunhaos/organization/application/usecase/corporate/employee/UnblockEmployeeUseCaseBeanTest.java`

## Change Log

| Data | Descrição |
|------|-----------|
| 2026-08-05 | Story implementada: 4 Use Cases (Activate/Inactivate/Block/Unblock) + Service + Delegate + 7 códigos de erro novos (`SCOS_EMPLOYEE_014..020`) + seeds de teste. Testes completos: 37 unitários em `EmployeeServiceBeanTest`, 8 em `*UseCaseBeanTest`, 37 de integração em `EmployeeControllerTest`. Status → review. |
