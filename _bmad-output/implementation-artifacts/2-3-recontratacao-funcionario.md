# Story 2.3: Recontratação de Funcionário (INACTIVE ou DISABLED)

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como Analista de RH,
Eu quero recontratar um Funcionário que estava `INACTIVE`, reatribuindo empresa/cargo/supervisor/tipo de contrato,
Para reintegrar alguém em uma posição diferente sem criar um registro novo.

## Acceptance Criteria

1. **Given** um Funcionário `INACTIVE` localizado pelo `taxIdentifier` (CPF) **When** RH aciona `POST /v1/employees/rehire` (`REHIRE_EMPLOYEE`) informando `companyId`, `positionId`, `contractType`, `reasonActivateId` e `reasonPositionChangeId` (todos **sempre obrigatórios**, mesmo que repitam os valores anteriores — recontratação **não herda** nada implicitamente) **Then** o Funcionário passa para `ACTIVE` (`200`, corpo `GetEmployeeResponse`/`Employee` completo, com `supervisor`/`company`/`position` aninhados) **And** `companyId`/`positionId`/`contractType`/`probationEndDate` do Funcionário são sobrescritos pelos novos valores **And** `supervisorId`, se omitido, fica `null` (não herda o supervisor anterior) **And** `id`, `taxIdentifier` e `email` originais são preservados — **não** cria um `EMPLOYEE` novo.
2. **Given** a transição bem-sucedida **When** confirmada **Then** insere, na mesma transação: (a) uma linha em `SCOS_EMPLOYEE_STATUS_HISTORY` com `status=ACTIVE` e o `reasonActivateId` informado; (b) uma nova linha em `SCOS_EMPLOYEE_POSITION_HISTORY` referenciando o `reasonPositionChangeId` informado — **sempre** gerada, mesmo quando `positionId` não muda em relação ao vínculo anterior **And** a linha de posição anteriormente aberta (`END_DATE IS NULL`) do mesmo Funcionário é fechada automaticamente pelo trigger já existente `trg_close_previous_position` (nenhum `UPDATE` explícito no código Java).
3. **Given** `dateOfRehire` informado no request **When** a recontratação é processada **Then** `SCOS_EMPLOYEE.DATE_OF_HIRING` é atualizado para esse valor **And** esse mesmo valor é o `startDate` da nova linha de `SCOS_EMPLOYEE_POSITION_HISTORY`. **Given** `dateOfRehire` omitido **Then** `DATE_OF_HIRING` mantém o valor original do primeiro vínculo (não é resetado) **And** o `startDate` da nova linha de posição usa a data atual (via `Clock` injetável — nunca a `dateOfHiring` antiga, ver Dev Notes sobre o risco de violar a ordem cronológica que o trigger de fechamento assume).
4. **Given** `companyId`, `positionId`, `reasonActivateId` ou `reasonPositionChangeId` inexistentes, ou `supervisorId` informado e inexistente **When** a requisição chega **Then** o sistema rejeita com `404` — `companyId`/`positionId`/`reasonActivateId` reaproveitam os códigos já existentes (`SCOS_COMPANY_001`/`SCOS_POSITION_001`/`SCOS_REASON_ACTIVATE_001`), `supervisorId` reaproveita `SCOS_EMPLOYEE_004` (Story 2.1), `reasonPositionChangeId` usa código novo `SCOS_EMPLOYEE_022` (não existe hoje nenhum "reason position change not found" reaproveitável — em `create()`, Story 2.1, o motivo é resolvido internamente por `CODE`, nunca pelo id vindo do cliente).
5. **Given** `companyId` inativo, `positionId` inativo, `supervisorId` informado e não `ACTIVE`, `reasonActivateId` inativo ou incompatível (`entityType != EMPLOYEE`), ou `reasonPositionChangeId` inativo **When** a requisição chega **Then** o sistema rejeita com `422` — os 5 primeiros reaproveitam códigos já existentes (`SCOS_EMPLOYEE_005/006/007/008/009`, Story 2.1), `reasonPositionChangeId` inativo usa código novo `SCOS_EMPLOYEE_023` (`ReasonPositionChange` não tem campo `entityType` — só uma checagem de `active`, não duas).
6. **Given** o `taxIdentifier` informado não corresponde a nenhum Funcionário com `status=INACTIVE` — nunca existiu, **ou** pertence a um Funcionário `ACTIVE`, **ou** pertence a um Funcionário `DISABLED` **When** a requisição chega **Then** o sistema rejeita com `404 SCOS_EMPLOYEE_021`, sempre o mesmo código nos três casos (o contrato, `ScosOrganization_Employee.yml` linha 86, é explícito: "Retorna 4XX se não existir funcionário INACTIVE com este CPF" — a resposta não distingue "nunca existiu" de "existe com outro status", propositalmente, para não vazar em qual status um CPF de terceiro está).
7. **Given** um Funcionário `DISABLED` **When** RH tenta `rehire` pelo CPF dele **Then** cai no mesmo `404 SCOS_EMPLOYEE_021` do AC 6 — **rehire não cobre `DISABLED`**, só `INACTIVE` (ver Dev Notes, "Resolução do texto de `epics.md`" — diverge do título desta story e do Given original, resolvido a favor do contrato real). O caminho correto para reativar um `DISABLED` sem reatribuição é `unblock` (Story 2.2); se precisar reatribuir empresa/cargo, hoje **não existe** rota que cubra `DISABLED` + reatribuição (gap conhecido, fora do escopo desta story — nenhum AC do épico pede isso).
8. **Given** hoje não existe nenhum Use Case/Delegate para a rota `rehire`, e o path `/v1/employees/rehire` já hospeda incorretamente `POST` (`rehireEmployee`), `GET` (`getEmployeeById`) e `PUT` (`updateEmployee`) no mesmo template de rota sem `{id}` (defeito de contrato já sinalizado pela Story 2.1, Task 10, "fora do escopo") **When** esta story é implementada **Then** implementa **só** o `POST` (`rehireEmployee`) — `getEmployeeById`/`updateEmployee` desse mesmo bloco YAML continuam sem `@Override` no Delegate, e o defeito de contrato (paths indevidamente aninhados) não é corrigido aqui.

## Tasks / Subtasks

- [ ] Task 1: Códigos de erro novos — `SCOS_EMPLOYEE_021..023` (AC: 4, 5, 6)
  - [ ] **Antes de codar, confirme o próximo número livre**: `grep SCOS_EMPLOYEE_ ExceptionCodeError.java` — hoje só existe `SCOS_EMPLOYEE_001` no código real; as Stories 2.1 (`002..013`) e 2.2 (`014..020`) ainda não foram implementadas. Se alguma delas já tiver sido codada com números diferentes quando esta story for implementada, ajuste a sequência abaixo mantendo a ordem lógica.
  - [ ] Em `organization/flow-organization-shared/src/main/java/br/com/sawcunhaos/organization/shared/exception/ExceptionCodeError.java`, após a última entrada `SCOS_EMPLOYEE_0NN`:
    ```java
    /** Nenhum Funcionário INACTIVE encontrado para o CPF informado (nunca existiu, ou pertence a ACTIVE/DISABLED). HTTP 404. */
    SCOS_EMPLOYEE_021("SCOS_EMPLOYEE_021", 404, "SCOS_TITLE_NOT_FOUND"),
    /** Motivo de mudança de cargo (reasonPositionChangeId) informado não encontrado. HTTP 404. */
    SCOS_EMPLOYEE_022("SCOS_EMPLOYEE_022", 404, "SCOS_TITLE_NOT_FOUND"),
    /** Motivo de mudança de cargo informado está inativo. HTTP 422. */
    SCOS_EMPLOYEE_023("SCOS_EMPLOYEE_023", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    ```
  - [ ] `scos_message_organization.properties`:
    ```properties
    SCOS_EMPLOYEE_021=Nenhum funcionário inativo foi encontrado para o CPF informado.
    SCOS_EMPLOYEE_022=O motivo de mudança de cargo informado não foi encontrado.
    SCOS_EMPLOYEE_023=O motivo de mudança de cargo informado está inativo.
    ```
  - [ ] Mesmas 3 chaves em `scos_message_organization_en.properties`, texto em inglês.

- [ ] Task 2: `RehireEmployeeInput` — DTO de domínio novo (AC: 1, 3)
  - [ ] Criar `flow-organization-domain/.../corporate/employee/dto/RehireEmployeeInput.java`, mesmo pacote de `EmployeeInput` (Story 2.1):
    ```java
    @Builder
    public record RehireEmployeeInput(
            String taxIdentifier,
            Long companyId,
            Long positionId,
            Long supervisorId,
            EmployeeContractType contractType,
            LocalDate probationEndDate,
            LocalDate dateOfRehire,
            Long reasonActivateId,
            Long reasonPositionChangeId,
            String observation
    ) {
    }
    ```
    **Não** reaproveitar `EmployeeInput` (Story 2.1) — campos obrigatórios diferentes (`reasonPositionChangeId` aqui, `name`/`email`/`birthDate` lá) e semântica diferente (recontratação nunca cria linha nova em `SCOS_EMPLOYEE`).

- [ ] Task 3: Repositório — 1 método novo em `EmployeeQueryRepository` (AC: 6, 7)
  - [ ] Em `EmployeeQueryRepository.java` (já existe, Story 2.1 adiciona `existsByTaxIdentifier`/`existsByEmail`), adicionar:
    ```java
    default Optional<Employee> findByTaxIdentifierAndStatus(String taxIdentifier, StatusEmployee status) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();
        booleanBuilder.and(qEmployee.taxIdentifier.cpf.eq(taxIdentifier))
                .and(qEmployee.status.eq(status));
        return findOne(booleanBuilder.getValue());
    }
    ```
    `findOne` vem de `QuerydslPredicateExecutor`, já na interface. **Não** criar um método `existsByTaxIdentifierAndStatus` separado — aqui precisamos da entidade completa (para `activate()` e reatribuição), não de um booleano.

- [ ] Task 4: `EmployeeService` (specification) — 2 assinaturas novas na interface já existente (AC: 1, 4, 5, 6, 7)
  - [ ] Em `EmployeeService.java` (criada pela Story 2.1, com `create` + os 4 métodos de transição da Story 2.2), adicionar:
    ```java
    /** Recontrata um Funcionário INACTIVE, reatribuindo empresa/cargo/supervisor/contrato. 404 SCOS_EMPLOYEE_021 se não houver Funcionário INACTIVE com esse CPF. */
    EmployeeOutput rehire(@NonNull RehireEmployeeInput input);

    /** Busca o Funcionário pelo id, para composição por outro fluxo (ex.: nome do supervisor no mapeamento de rehire). 404 SCOS_EMPLOYEE_014 se não existir. */
    EmployeeOutput findById(@NonNull Long id);
    ```
    `findById` é o mesmo padrão que `CompanyService.findById`/`PositionService.findById` já têm — Employee é o único dos três agregados que ainda não expunha essa leitura simples. **Não** é o início da feature `GET /v1/employees/{id}` completa (Use Case/Delegate/mapper aninhado ficam fora, Task 11) — é só o primitivo de domínio que esta story precisa para resolver o nome do supervisor na resposta de `rehire`.

- [ ] Task 5: `EmployeeServiceBean` — implementar `rehire`/`findById` (AC: 1, 2, 3, 4, 5, 6, 7)
  - [ ] Injetar `Clock` (bean já existente, `domain/config/ClockConfig.java`, AD-8) como campo `final` novo, se a Story 2.1/2.2 ainda não o tiver adicionado.
  - [ ] Implementar, reaproveitando **sem duplicar** os privados já criados pela Story 2.1 (`findActiveCompanyOrThrow`, `findActivePositionOrThrow`, `resolveActiveSupervisor`, `validateReasonActivate`) e pela Story 2.2 (`findEmployeeById`):
    ```java
    @Override
    @Transactional(rollbackFor = ScosException.class)
    public EmployeeOutput rehire(@NonNull RehireEmployeeInput input) {
        log.info("Rehire Employee: {}", input.taxIdentifier());

        Employee employee = employeeQueryRepository.findByTaxIdentifierAndStatus(input.taxIdentifier(), StatusEmployee.INACTIVE)
                .orElseThrow(() -> new ScosException(SCOS_EMPLOYEE_021));

        Company company = findActiveCompanyOrThrow(input.companyId());
        Position position = findActivePositionOrThrow(input.positionId());
        Employee supervisor = resolveActiveSupervisor(input.supervisorId());
        validateReasonActivate(input.reasonActivateId());
        ReasonPositionChange reasonPositionChange = findActiveReasonPositionChangeOrThrow(input.reasonPositionChangeId());

        String user = scosUserAuthentication.findUserAuthentication();
        LocalDate effectiveDate = input.dateOfRehire() != null ? input.dateOfRehire() : LocalDate.now(clock);

        // guarda de estado SCOS_EMPLOYEE_001 dentro de activate() é estruturalmente inalcançável aqui —
        // a busca acima já filtra status=INACTIVE; mantido por ser o único ponto de mutação de status do domínio.
        EmployeeStatusHistory history = employee.activate(input.reasonActivateId());
        history.setObservation(input.observation());
        history.setUserAt(user);
        employeeStatusHistoryRepository.merge(history);

        employee.setCompany(company);
        employee.setPosition(position);
        employee.setSupervisor(supervisor);
        employee.setContractType(input.contractType());
        employee.setProbationEndDate(input.probationEndDate());
        if (input.dateOfRehire() != null) {
            employee.setDateOfHiring(input.dateOfRehire());
        }
        employee = employeeQueryRepository.merge(employee);

        employeePositionHistoryRepository.merge(
                EmployeePositionHistory.builder()
                        .employee(employee)
                        .position(position)
                        .startDate(effectiveDate)
                        .reasonPositionChange(reasonPositionChange)
                        .userAt(user)
                        .build()
        ); // trg_close_previous_position fecha a linha aberta anterior; trg_sync_employee_position mantém SCOS_EMPLOYEE.POSITION_ID sincronizado (redundante com o setPosition acima, inofensivo)

        return toEmployeeOutput(employee);
    }

    @Override
    public EmployeeOutput findById(@NonNull Long id) {
        return toEmployeeOutput(findEmployeeById(id));
    }

    private ReasonPositionChange findActiveReasonPositionChangeOrThrow(Long reasonPositionChangeId) {
        ReasonPositionChange reason = reasonPositionChangeRepository.findById(reasonPositionChangeId)
                .orElseThrow(() -> new ScosException(SCOS_EMPLOYEE_022));
        if (!reason.isActive()) {
            throw new ScosException(SCOS_EMPLOYEE_023);
        }
        return reason;
    }
    ```
  - [ ] **Por que `setPosition`/`setCompany`/`setSupervisor`/`setContractType` são feitos explicitamente em Java, mesmo com o trigger `trg_sync_employee_position` já existente:** o trigger só sincroniza `POSITION_ID` (não `COMPANY_ID`/`SUPERVISOR_ID`/`CONTRACT_TYPE`, que não têm trigger equivalente) **e** roda como `UPDATE` SQL cru fora da sessão do Hibernate — sem o `set` explícito, o objeto `employee` em memória (usado por `toEmployeeOutput` na mesma chamada) ficaria com os valores antigos. Mesmo padrão que a Story 2.1 já segue em `create()` (seta `position`/`company` no builder mesmo sabendo que uma linha de histórico também será inserida).
  - [ ] **Por que `startDate` da nova linha de posição usa `Clock.now()` quando `dateOfRehire` é omitido, e não a `dateOfHiring` antiga do Funcionário:** `trg_close_previous_position` (`fn_close_previous_position.sql`) fecha a linha de posição ainda aberta (`END_DATE IS NULL`) do mesmo `EMPLOYEE_ID` fazendo `END_DATE = NEW.START_DATE`. Se `NEW.START_DATE` fosse a `dateOfHiring` original (ex.: 2020) e a linha aberta antiga já tivesse `START_DATE` posterior a isso (ex.: uma transferência em 2022, nunca fechada porque `disable`/`inactivate`, Story 2.2, não tocam em `EmployeePositionHistory`), o `UPDATE` produziria `END_DATE < START_DATE` — inconsistência silenciosa, sem constraint conhecida que a impeça. Usar a data efetiva real da recontratação (hoje, ou a data futura/passada explicitamente informada em `dateOfRehire`) evita esse cenário.
  - [ ] `[ASSUMPTION]`: o contrato (`ScosOrganization_Employee.yml:1098-1101`) não diz explicitamente qual data é o `startDate` da nova linha de `EmployeePositionHistory` quando `dateOfRehire` é omitido — só descreve o efeito sobre `DATE_OF_HIRING`. A escolha de usar `Clock.now()` como fallback (em vez de repetir a `dateOfHiring` antiga) é a mais consistente com a integridade do histórico de posição, mas não está escrita no doc-fonte — sinalizar para o PM se preferir outro comportamento.

- [ ] Task 6: `RehireEmployeeUseCase` + `Bean` — pacote já existente (AC: 1, 4, 5, 6, 7)
  - [ ] Em `usecase/corporate/employee/` (pacote criado pela Story 2.1), criar:
    ```java
    public interface RehireEmployeeUseCase {
        Employee execute(@NonNull RehireEmployeeRequest request); // Employee = api.dto.Employee
    }
    ```
    ```java
    @Service @RequiredArgsConstructor @Slf4j @Transactional(rollbackFor = ScosException.class)
    class RehireEmployeeUseCaseBean implements RehireEmployeeUseCase {

        private final EmployeeService employeeService;
        private final CompanyService companyService;
        private final PositionService positionService;

        @Override
        public br.com.sawcunhaos.organization.api.dto.Employee execute(@NonNull RehireEmployeeRequest request) {
            log.info("Rehire employee: {}", request.taxIdentifier());

            RehireEmployeeInput input = RehireEmployeeInput.builder()
                    .taxIdentifier(request.taxIdentifier())
                    .companyId(request.companyId())
                    .positionId(request.positionId())
                    .supervisorId(request.supervisorId())
                    .contractType(br.com.sawcunhaos.organization.domain.corporate.employee.internal.EmployeeContractType.valueOf(request.contractType().name()))
                    .probationEndDate(request.probationEndDate())
                    .dateOfRehire(request.dateOfRehire())
                    .reasonActivateId(request.reasonActivateId())
                    .reasonPositionChangeId(request.reasonPositionChangeId())
                    .observation(request.observation())
                    .build();

            EmployeeOutput output = employeeService.rehire(input);
            CompanyOutput company = companyService.findById(output.companyId());
            PositionOutput position = positionService.findById(output.positionId());
            EmployeeOutput supervisor = output.supervisorId() != null ? employeeService.findById(output.supervisorId()) : null;

            return EmployeeApiMapper.toApiEmployee(output, company, position, supervisor);
        }
    }
    ```
    FQN em `EmployeeContractType` (mesmo nome simples em `api.dto` e `domain.internal`) — mesma solução já usada por `CreateEmployeeUseCaseBean` (Story 2.1). O retorno é `br.com.sawcunhaos.organization.api.dto.Employee` — mesma colisão de nome simples com a entidade de domínio `Employee`; usar FQN na assinatura do método ou importar só o `api.dto.Employee` e referenciar a entidade de domínio via FQN dentro do corpo (o corpo aqui não usa a entidade diretamente, só `EmployeeOutput`/`CompanyOutput`/`PositionOutput`, então basta importar `api.dto.Employee` normalmente).

- [ ] Task 7: `EmployeeApiMapper` — novo, pacote `usecase/corporate/employee/` (AC: 1)
  - [ ] Este mapper é necessário (não deferível como em Story 2.1) porque o contrato de `rehire` responde `200` com o schema completo `Employee` (`supervisor`/`company`/`position` aninhados) — diferente de `create` (`201`, só `id`) e das rotas de transição (`204`). Criar `EmployeeApiMapper.java`, mesmo estilo de `PositionApiMapper`/`CompanyApiMapper` (classe `final`, construtor privado, métodos estáticos package-private — **não** importar/reaproveitar `PositionApiMapper` de `usecase.corporate.position`, que é package-private nesse outro pacote; duplicar a mesma pequena montagem de `Position`/`Department`, mesmo padrão de não-compartilhamento já usado entre os mappers existentes):
    ```java
    final class EmployeeApiMapper {

        private EmployeeApiMapper() {
        }

        static Employee toApiEmployee(EmployeeOutput employee, CompanyOutput company, PositionOutput position, EmployeeOutput supervisor) {
            return Employee.builder()
                    .id(employee.id())
                    .name(employee.name())
                    .nameTreatment(employee.nameTreatment())
                    .taxIdentifier(employee.taxIdentifier())
                    .email(employee.email())
                    .birthDate(employee.birthDate())
                    .dateOfHiring(employee.dateOfHiring())
                    .observation(employee.observation())
                    .status(EmployeeStatus.valueOf(employee.status().name()))
                    .contractType(EmployeeContractType.valueOf(employee.contractType().name()))
                    .probationEndDate(employee.probationEndDate())
                    .supervisor(toApiSupervisor(supervisor))
                    .company(toApiEmployeeCompany(company))
                    .position(toApiPosition(position))
                    .build();
        }

        private static Supervisor toApiSupervisor(EmployeeOutput supervisor) {
            if (supervisor == null) {
                return null;
            }
            return Supervisor.builder().id(supervisor.id()).name(supervisor.name()).build();
        }

        private static EmployeeCompany toApiEmployeeCompany(CompanyOutput company) {
            return EmployeeCompany.builder().id(company.id()).name(company.name()).build();
        }

        private static Position toApiPosition(PositionOutput position) {
            return Position.builder()
                    .id(position.id())
                    .code(position.code())
                    .description(position.description())
                    .active(position.active())
                    .isTrustPosition(position.isTrustPosition())
                    .department(toApiDepartment(position.department()))
                    .build();
        }

        private static Department toApiDepartment(DepartmentOutput department) {
            if (department == null) {
                return null;
            }
            return Department.builder()
                    .id(department.id())
                    .code(department.code())
                    .description(department.description())
                    .active(department.active())
                    .build();
        }
    }
    ```
    `EmployeeStatus`/`EmployeeContractType` em `.valueOf(...)` referem-se aos tipos `api.dto` (importados normalmente aqui — quem precisa de FQN é o lado `domain.internal`, dentro do Use Case, Task 6). `Employee`/`Position`/`Department`/`Supervisor`/`EmployeeCompany` são todos `api.dto` (gerados).

- [ ] Task 8: `EmployeeDelegate` — 1 método novo na classe já existente (AC: 1, 8)
  - [ ] **Rodar `mvn generate-sources` em `flow-organization-usecase`/`flow-organization-api` antes** — confirmar a assinatura exata gerada de `EmployeeApiDelegate.rehireEmployee(...)` (schema nunca exercitado pelo generator neste módulo, mesma recomendação de 1.5/2.1/2.2).
  - [ ] Em `EmployeeDelegate.java` (criada pela Story 2.1), injetar `RehireEmployeeUseCase` (campo `final`, `@RequiredArgsConstructor` já cobre) e implementar, espelhando `CompanyDelegate.getCompanyById` (`GetCompanyResponse.builder().data(...).build()`):
    ```java
    @Override
    public GetEmployeeResponse rehireEmployee(RehireEmployeeRequest rehireEmployeeRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        return GetEmployeeResponse.builder()
                .data(rehireEmployeeUseCase.execute(rehireEmployeeRequest))
                .build();
    }
    ```
    **Não** implementar `getEmployeeById`/`updateEmployee` (mesmo bloco YAML, path `/v1/employees/rehire`, métodos `GET`/`PUT` diferentes — AC 8, fora de escopo).

- [ ] Task 9: Seed de teste — nenhum Funcionário `INACTIVE` existe hoje no seed (AC: 6, 7)
  - [ ] `setsup_database.sql` só tem 1 Funcionário seedado (`EMPLOYEE_ID=1`, `ACTIVE`, "administrador"). **Não** adicionar um Funcionário `INACTIVE`/`DISABLED` fixo ao seed compartilhado — criar o cenário dentro do próprio teste de integração (`POST /v1/employees` com `taxIdentifier` único, depois `PUT .../disable` para chegar a `INACTIVE`, ou `.../disable` + `.../block` para chegar a `DISABLED`), mesmo cuidado já registrado nas Stories 2.1/2.2 sobre não acoplar cenários específicos ao seed compartilhado.

- [ ] Task 10: Testes (AC: 1, 2, 3, 4, 5, 6, 7)
  - [ ] `EmployeeServiceBeanTest.java` (já existe, criado pela Story 2.1): caminho feliz de `rehire` (funcionário volta `ACTIVE`, `EmployeeStatusHistory` com `reasonActivateId` correto, nova `EmployeePositionHistory` com o `reasonPositionChangeId` informado — capturar via `ArgumentCaptor`, `company`/`position`/`supervisor`/`contractType`/`probationEndDate` sobrescritos no objeto `Employee` retornado); `dateOfRehire` informado → `startDate` da nova posição = `dateOfRehire` **e** `Employee.dateOfHiring` atualizado; `dateOfRehire` omitido → `startDate` = `Clock.fixed(...)` injetado **e** `dateOfHiring` do funcionário permanece o original; `supervisorId` omitido → `Employee.supervisor` fica `null` mesmo que houvesse um supervisor antes; CPF sem Funcionário `INACTIVE` (nunca existiu, ou existe `ACTIVE`, ou existe `DISABLED` — 3 casos, mesmo código) → `SCOS_EMPLOYEE_021`; `companyId`/`positionId`/`reasonActivateId`/`supervisorId`/`reasonPositionChangeId` inexistentes → 404 (reaproveitados + `022`); empresa/cargo/supervisor inativos, motivo de ativação inativo/incompatível, motivo de mudança de cargo inativo → 422 (reaproveitados + `023`). Também `findById`: caminho feliz e `SCOS_EMPLOYEE_014` quando não existe (reaproveita `findEmployeeById` da Story 2.2).
  - [ ] `RehireEmployeeUseCaseBeanTest.java` (novo, `flow-organization-usecase/.../corporate/employee/`), mesmo padrão BDD de `CreateEmployeeUseCaseBeanTest`: mapeamento `request`→`input` via `ArgumentCaptor`; 3 collaborators (`employeeService`/`companyService`/`positionService`) chamados com os ids certos; `supervisorId == null` → `employeeService.findById` **não** é chamado (verificar via `then(employeeService).should(never())...`); propagação de `ScosException` quando `employeeService.rehire` lança.
  - [ ] `EmployeeControllerTest.java` (já existe, criado pela Story 2.1): caminho feliz completo `POST /v1/employees/rehire` → `200` + corpo com `status=ACTIVE`, `company`/`position`/`supervisor` aninhados corretos; confirmar via query direta que a linha antiga de `SCOS_EMPLOYEE_POSITION_HISTORY` foi fechada (`END_DATE` não nulo) e a nova está aberta; `404 SCOS_EMPLOYEE_021` para CPF de Funcionário `ACTIVE` (o seed `EMPLOYEE_ID=1`) e para CPF de Funcionário `DISABLED` (criado no próprio teste, Task 9) e para CPF nunca cadastrado; `404`/`422` de cada FK (Task 1); `401` sem token; `403` sem `REHIRE_EMPLOYEE`. `taxIdentifier` é `x-jdempotentrequestpayload` (YAML linha 1074) — mesmo cuidado de CPF único por teste que já existe em `EmployeeControllerTest` (Story 2.1).

- [ ] Task 11: Guarda de escopo (AC: 7, 8)
  - [ ] **Não** corrigir o defeito de contrato em `/v1/employees/rehire` (POST/GET/PUT indevidamente compartilhando o mesmo path sem `{id}`) — já reportado pela Story 2.1 (Task 10), não bloqueante para implementar só o `POST`.
  - [ ] **Não** implementar `getEmployeeById`/`updateEmployee` (mesmo bloco YAML) nem `GET /v1/employees`, `transfer`, `hierarchy`, `subordinates`, `position-history`, `status-history` — nenhum é tocado por esta story.
  - [ ] **Não** criar nenhuma rota/lógica para recontratar (com reatribuição) um Funcionário `DISABLED` — o contrato de `rehire` só cobre `INACTIVE` (AC 6, 7); reativar `DISABLED` sem reatribuição já é `unblock` (Story 2.2). Reatribuir um `DISABLED` é gap conhecido, fora de qualquer AC do épico.
  - [ ] **Não** construir `GetEmployeeUseCase`/Delegate dedicados para `GET /v1/employees/{id}` — `EmployeeService.findById` (Task 4/5) é só o primitivo de domínio reaproveitado internamente pelo Use Case de `rehire`, não uma feature de leitura completa.

## Dev Notes

### Contexto crítico — leia antes de implementar

**Depende, em ordem, das Stories 2.1 e 2.2 já implementadas.** Diferente delas (que criaram/estenderam `EmployeeService`/`EmployeeServiceBean`/`EmployeeDelegate` do zero ou quase), esta story só adiciona a um arquivo que já deveria existir com: `create()` + os privados `findActiveCompanyOrThrow`/`findActivePositionOrThrow`/`resolveActiveSupervisor`/`validateReasonActivate` (Story 2.1); `activate()`/`inactivate()`/`disable()`/`enable()` + `findEmployeeById` (Story 2.2). Se qualquer uma delas ainda não foi implementada quando esta story começar, implemente-as primeiro — o código desta story pressupõe esses métodos já existentes, reaproveitados sem reescrita.

**Resolução do texto de `epics.md` — `rehire` cobre só `INACTIVE`, não `DISABLED`.** O título desta story em `epics.md` ("Recontratação de Funcionário (INACTIVE ou DISABLED)") e o Given original ("um Funcionário INACTIVE ou DISABLED") divergem do contrato já publicado: `ScosOrganization_Employee.yml` (linhas 83-86) é explícito — `rehire` localiza o Funcionário **exclusivamente** por `taxIdentifier` com `status=INACTIVE`; CPF de Funcionário `ACTIVE` ou `DISABLED` retorna o mesmo `4XX` de "não encontrado". O próprio bullet final do AC do épico ("`unblock` já cobre o caminho rápido sem reatribuição — não duplica essa lógica") é a pista de que o rascunho do épico já antecipava essa divisão: `DISABLED` fica inteiramente com `unblock` (Story 2.2); `rehire` nunca precisa saber que `DISABLED` existe. Este story-file resolve a favor do contrato/YAML (mesma convenção já usada pelas Stories 2.1/2.2 quando `epics.md`/doc de spec divergem do artefato publicado) — **não** implementar nenhum caminho de `rehire` para `DISABLED`.

**O contrato inteiro já está publicado — zero mudança de OpenAPI, Liquibase ou permissão nesta story.** Endpoint (`POST /v1/employees/rehire`), schema `RehireEmployeeRequest`, permissão `REHIRE_EMPLOYEE` (com i18n já presente em `messages_permission[_en].properties`), trigger `trg_close_previous_position`/`fn_close_previous_position` e trigger `trg_sync_employee_position`/`fn_sync_employee_position` já existem no repositório, confirmados por leitura direta. O trabalho desta story é só a camada de aplicação: DTO → Service → Use Case → Mapper → Delegate.

**`fn_sync_employee_position` só sincroniza `POSITION_ID`** (`UPDATE scos.scos_employee SET position_id = NEW.position_id ... WHERE employee_id = NEW.employee_id`) — não existe trigger equivalente para `COMPANY_ID`, `SUPERVISOR_ID` ou `CONTRACT_TYPE`. O Service precisa setar esses 4 campos diretamente na entidade `Employee` (Task 5) — não dá para confiar em "o trigger resolve" para nenhum deles além de `position`, e mesmo para `position` o `set` explícito é necessário porque o trigger roda como `UPDATE` SQL cru fora da sessão do Hibernate (o objeto em memória não seria atualizado a tempo de `toEmployeeOutput` refletir o valor novo).

**`fn_close_previous_position` fecha a linha de posição aberta usando `NEW.start_date` como `END_DATE` da linha antiga** — se a nova linha usar uma data anterior à da linha antiga (ex.: repetir a `dateOfHiring` original de anos atrás), o resultado é `END_DATE < START_DATE` na linha antiga, sem nenhuma constraint conhecida que bloqueie isso. É por isso que o `startDate` da nova `EmployeePositionHistory` usa `Clock.now()` como fallback (Task 5) — nunca a `dateOfHiring` antiga do Funcionário.

**`ReasonPositionChange` não tem campo `entityType`** (diferente de `ReasonActivate`/`ReasonInactivate`/`ReasonDisable`/`ReasonEnable`, todos escopados por entidade) — só `active`. A validação de `reasonPositionChangeId` (Task 5) é uma checagem só, não duas.

**Por que esta story precisa de `EmployeeApiMapper` (Task 7), diferente da Story 2.1 que deliberadamente adiou isso para `create()`:** lá, `create` responde `201_CREATED` (só `id`) — nenhum AC valida corpo de leitura. Aqui, o contrato de `rehire` responde `200` com `GetEmployeeResponse`/`Employee` **completo** (`supervisor`/`company`/`position` aninhados) — não é opcional, é a forma da resposta. `EmployeeOutput` (domain) continua flat, como a Story 2.1 decidiu — a montagem do objeto aninhado acontece inteiramente no Use Case (Task 6), reaproveitando leituras que já existem publicamente (`CompanyService.findById`, `PositionService.findById`, mesmo padrão de `FindPositionUseCaseBean`/`PositionApiMapper`) mais o `EmployeeService.findById` novo (Task 4/5) só para o nome do supervisor.

### Onde cada peça vai (camadas)

- **`domain/corporate/employee/dto/RehireEmployeeInput.java`** (novo).
- **`domain/corporate/employee/specification/EmployeeService.java`** (já existe — 2 assinaturas novas: `rehire`, `findById`).
- **`domain/corporate/employee/service/EmployeeServiceBean.java`** (já existe — 2 métodos novos + `findActiveReasonPositionChangeOrThrow` privado + campo `Clock` se ainda não injetado).
- **`domain/corporate/employee/internal/EmployeeQueryRepository.java`** (já existe — 1 método novo: `findByTaxIdentifierAndStatus`).
- **`shared/exception/ExceptionCodeError.java`** + **`scos_message_organization[_en].properties`**: 3 códigos novos (`SCOS_EMPLOYEE_021..023`).
- **`usecase/corporate/employee/`** (já existe, pacote da Story 2.1): `RehireEmployeeUseCase`/`Bean` + `EmployeeApiMapper` (novo).
- **`api/delegate/employee/EmployeeDelegate.java`** (já existe — 1 `@Override` novo).
- Nenhuma mudança em `etc/api/organization/ScosOrganization_Employee.yml`, `ScosOrganizationPermission.java` ou Liquibase.

### Testing Standards

- Unitário de domínio: `@ExtendWith(MockitoExtension.class)`, mesmo padrão de `EmployeeServiceBeanTest` (Stories 2.1/2.2). `Clock.fixed(...)` para os casos de `dateOfRehire` omitido.
- Unitário de Use Case: mesmo padrão BDD de `CreateEmployeeUseCaseBeanTest`.
- Integração: adicionar aos blocos já existentes de `EmployeeControllerTest.java` (`extends ScosOrganizationTestUtil`, containers `static`, **nunca** `@Testcontainers`/`@Container`). `taxIdentifier` único por teste que faz `POST` (campo `x-jdempotentrequestpayload` de `rehire`, distinto do de `create` — cada rota tem seu próprio campo idempotente, mesmo CPF não pode ser reciclado entre um `POST /v1/employees` e um `POST /v1/employees/rehire` no mesmo teste sem gerar colisão de cache).

### Project Structure Notes

- Nenhum pacote Maven novo — tudo cai em pacotes já criados pela Story 2.1 (`usecase/corporate/employee/`, `api/delegate/employee/`) ou já existentes desde antes (`domain/corporate/employee/...`).
- Nenhuma mudança de Liquibase, OpenAPI ou permissão.

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Epic 2 / Story 2.3] — Given/When/Then originais; divergência `INACTIVE ou DISABLED` resolvida em Dev Notes.
- [Source: etc/api/organization/ScosOrganization_Employee.yml:79-113,832-867,898-902,1062-1114] — endpoint `rehireEmployee`, schemas `RehireEmployeeRequest`/`Employee`/`Supervisor`/`EmployeeCompany`/`GetEmployeeResponse`; descrição explícita "cujo status atual é INACTIVE" e "retorna 4XX se... pertence a um funcionário ACTIVE/DISABLED".
- [Source: etc/api/organization/ScosOrganization_Employee.yml:114-161] — mesmo path `/v1/employees/rehire` hospedando `GET`/`PUT` (`getEmployeeById`/`updateEmployee`) — defeito de contrato já sinalizado, não corrigido aqui (guarda de escopo, AC 8).
- [Source: organization/flow-organization-infrastructure/.../enumaration/ScosOrganizationPermission.java:115-116] — `REHIRE_EMPLOYEE` já cadastrada; `messages_permission[_en].properties:97` já com i18n.
- [Source: organization/flow-organization-resources/.../triggers/trg_close_previous_position.sql, function/fn_close_previous_position.sql] — fecha `END_DATE` da linha de posição aberta anterior a partir de `NEW.START_DATE`.
- [Source: organization/flow-organization-resources/.../triggers/triggers.yml:350, function/fn_sync_employee_position.sql] — sincroniza só `SCOS_EMPLOYEE.POSITION_ID` a partir de `INSERT` em `SCOS_EMPLOYEE_POSITION_HISTORY`.
- [Source: organization/flow-organization-domain/.../corporate/employee/internal/Employee.java:113-122] — `activate()` já implementado (Story 2.2), guarda `SCOS_EMPLOYEE_001` reaproveitada (estruturalmente inalcançável nesta story, ver Task 5).
- [Source: organization/flow-organization-domain/.../corporate/employee/internal/EmployeePositionHistory.java, ReasonPositionChange.java] — `ReasonPositionChange` sem campo `entityType` (diferente de `ReasonActivate`/etc.).
- [Source: organization/flow-organization-domain/.../company/specification/CompanyService.java:40, position/specification/PositionService.java:27] — `findById` já público em ambos, precedente direto de `EmployeeService.findById` (Task 4).
- [Source: organization/flow-organization-domain/.../position/dto/PositionOutput.java] — campos `id/code/description/active/isTrustPosition/department` reaproveitados por `EmployeeApiMapper.toApiPosition`.
- [Source: organization/flow-organization-usecase/.../corporate/position/FindPositionUseCaseBean.java, PositionApiMapper.java] — padrão exato "Service.findById → ApiMapper.toApiX" replicado no Use Case de `rehire`.
- [Source: organization/flow-organization-api/.../delegate/company/CompanyDelegate.java:78-82] — padrão exato `GetXResponse.builder().data(useCase.execute(...)).build()` replicado no `EmployeeDelegate.rehireEmployee`.
- [Source: organization/flow-organization-domain/.../config/ClockConfig.java] — bean `Clock` já existente (AD-8), reaproveitado para o fallback de `startDate` (Task 5).
- [Source: organization/flow-organization-boot/src/test/resources/postgresql/setsup_database.sql:278-310] — único Funcionário seedado (`EMPLOYEE_ID=1`, `ACTIVE`); nenhum `INACTIVE`/`DISABLED` — cenário deve ser criado dentro do teste (Task 9).
- [Source: _bmad-output/implementation-artifacts/2-1-admissao-funcionario-copia-jornada-trabalho.md] — origem de `EmployeeService`/`EmployeeServiceBean`/`EmployeeDelegate`/`EmployeeInput`/`EmployeeOutput`/`EmployeeControllerTest`, dos privados reaproveitados, e do defeito de contrato de `rehire` (Task 10 daquela story).
- [Source: _bmad-output/implementation-artifacts/2-2-ciclo-vida-funcionario.md] — origem de `activate/inactivate/disable/enable`, `findEmployeeById`, e do padrão de resolução de divergência doc×contrato a favor do contrato.

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List
