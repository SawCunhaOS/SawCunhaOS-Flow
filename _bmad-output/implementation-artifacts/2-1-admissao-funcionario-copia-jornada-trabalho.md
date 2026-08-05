# Story 2.1: Admissão de Funcionário com Cópia de Jornada de Trabalho

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como Analista de RH,
Eu quero admitir um Funcionário vinculado a um Cargo, com validações de idade mínima, domínio de e-mail e CPF, e a Jornada de Trabalho do Cargo copiada automaticamente para o seu registro,
Para que ele já nasça pronto para operar sob as regras corretas.

## Acceptance Criteria

1. **Given** um Cargo `active=true` com template de Jornada de Trabalho definido (Story 1.5, `SCOS_POSITION_WORK_SCHEDULE`) **When** RH admite um Funcionário (`POST /v1/employees`, `CREATE_EMPLOYEE`) informando esse `positionId` **Then** o Funcionário é criado com `status=ACTIVE` (`201`) **And** para cada dia da semana com registro no template do Cargo, uma linha equivalente é copiada para `SCOS_EMPLOYEE_WORK_SCHEDULE` (cópia única — editar o template do Cargo depois **não** afeta o que já foi copiado) **And** dia sem registro no template permanece "não definido" para o Funcionário (nunca herda depois).
2. **Given** `EMPLOYEE_MIN_AGE` (config `INTEGER`) e `EMPLOYEE_EMAIL_DOMAIN` (config `STRING`) já cadastrados em `SCOS_CONFIGURATION` **When** RH admite um Funcionário cuja idade em `dateOfHiring` (não em "hoje") é menor que `EMPLOYEE_MIN_AGE`, **ou** cujo `email` não termina em `@{EMPLOYEE_EMAIL_DOMAIN}` **Then** o sistema rejeita com `422` (`SCOS_EMPLOYEE_011` idade / `SCOS_EMPLOYEE_010` domínio) — lidas via `OrganizationConfigurationRepository`, nunca hardcoded.
3. **Given** um `taxIdentifier` (CPF) já cadastrado em `SCOS_EMPLOYEE` (qualquer status) **When** RH tenta admitir novo Funcionário com o mesmo CPF **Then** o sistema rejeita com `409 SCOS_EMPLOYEE_002` **And** o mesmo vale para `email` duplicado → `409 SCOS_EMPLOYEE_003`.
4. **Given** hoje não existe nenhum Use Case/Delegate/Service de `Employee` (só a entidade de domínio com os 4 métodos de transição de status, e `EmployeePositionQueryServiceBean` de leitura) **When** esta story é implementada **Then** cria do zero: `EmployeeService`/`EmployeeServiceBean` (domain), `CreateEmployeeUseCase`/`Bean` (usecase), `EmployeeDelegate` (api, pacote novo) — só o suficiente para `POST /v1/employees`; `GET`/`enable`/`disable`/`block`/`unblock`/`rehire`/`transfer` ficam para as Stories 2.2/2.3 (guarda de escopo, ver Task 10).
5. **Given** `companyId`, `positionId` e `reasonActivateId` devem existir e `positionId` deve estar `active=true`, `companyId` deve estar `ACTIVE`, `reasonActivateId` deve estar `active=true` com `entityType=EMPLOYEE` **When** qualquer uma dessas condições falha **Then** o sistema rejeita com `404` (FK inexistente: `SCOS_COMPANY_001`/`SCOS_POSITION_001`/`SCOS_REASON_ACTIVATE_001`, todos reaproveitados de código já existente) ou `422` (estado inválido: `SCOS_EMPLOYEE_005` empresa não ativa / `SCOS_EMPLOYEE_006` cargo inativo / `SCOS_EMPLOYEE_008` motivo inativo / `SCOS_EMPLOYEE_009` motivo incompatível com `EMPLOYEE`) — **nunca** persiste o Funcionário nem a Jornada nessas condições.
6. **Given** `supervisorId` é opcional **When** informado **Then** deve existir (`404 SCOS_EMPLOYEE_004`, código novo — não há "employee not found" reaproveitável, `SCOS_EMPLOYEE_001` já é usado para outra coisa) e estar `ACTIVE` (`422 SCOS_EMPLOYEE_007`) — regra 5/6 de `etc/doc/usecase/03-funcionario.md`, não coberta pela lista de "Use Cases de Erro" ilustrativa daquele doc, mas explícita nas "Regras em ordem".
7. **Given** a criação bem-sucedida **When** confirmada **Then** insere, na mesma transação: (a) o Funcionário; (b) a primeira linha em `SCOS_EMPLOYEE_STATUS_HISTORY` com `status=ACTIVE` e o `reasonActivateId` informado (mesmo padrão de `CompanyServiceBean.create`); (c) uma linha em `SCOS_EMPLOYEE_POSITION_HISTORY` com `startDate=dateOfHiring`, `endDate=null`, referenciando o motivo semeado `SCOS_REASON_POSITION_CHANGE.CODE='NEW_HIRE'` (id 1 no seed, **nunca hardcoded** — resolver por `CODE`, ver Dev Notes) — `CreateEmployeeRequest` **não** tem campo `reasonPositionChangeId`, é resolvido pelo Service; (d) as linhas de `SCOS_EMPLOYEE_WORK_SCHEDULE` copiadas do Cargo (AC 1).

## Tasks / Subtasks

- [ ] Task 1: Corrigir gap de contrato — `maxLength` ausente em `CreateEmployeeRequest` (AC: 2, 3)
  - [ ] Em `etc/api/organization/ScosOrganization_Employee.yml`, no schema `CreateEmployeeRequest` (linha 948), adicionar `maxLength` nos 3 campos que hoje não têm (confirmado: `Company`/`ScosOrganization_Company.yml` já usa esse padrão para os mesmos limites — `name: 250`, `email: 255`):
    ```yaml
    name:
      type: string
      maxLength: 250
      x-required-message: SCOS_VALIDATION_003
      x-empty-message: SCOS_VALIDATION_001
    nameTreatment:
      type: string
      maxLength: 100
      x-required-message: SCOS_VALIDATION_003
      x-empty-message: SCOS_VALIDATION_001
    ...
    email:
      type: string
      maxLength: 255
      x-required-message: SCOS_VALIDATION_003
      x-empty-message: SCOS_VALIDATION_001
    ```
    **Por quê isto é bloqueante, não cosmético:** a coluna real é `NAME varchar(250)`/`NAME_TREATMENT varchar(100)`/`EMAIL varchar(255)` (`scos_employee.yml`). Sem `maxLength` no contrato, um payload que excede o limite não falha com `400` limpo — quebra no INSERT com erro de truncamento do Postgres, vazando como `500`. `etc/doc/usecase/03-funcionario.md` já documenta esses 3 limites na tabela de campos ("Tamanhos conforme tabela", regra 3) — o YAML publicado está em drift em relação ao próprio doc de spec. Rodar `mvn generate-sources` em `usecase`/`api` depois de editar.

- [ ] Task 2: DTOs de domínio (AC: 1, 2, 3, 5, 6, 7)
  - [ ] Criar `flow-organization-domain/.../corporate/employee/dto/EmployeeInput.java` (mesmo pacote-padrão de `CompanyInput`/`PositionInput`):
    ```java
    @Builder
    public record EmployeeInput(
            String name,
            String nameTreatment,
            String taxIdentifier,
            String email,
            LocalDate birthDate,
            String observation,
            LocalDate dateOfHiring,
            EmployeeContractType contractType,
            LocalDate probationEndDate,
            Long supervisorId,
            Long companyId,
            Long positionId,
            Long reasonActivateId
    ) {
    }
    ```
    (`EmployeeContractType` = `domain.corporate.employee.internal.EmployeeContractType`, enum já existente `CLT/PJ/ESTAGIO/TEMPORARIO` — não criar um novo.)
  - [ ] Criar `.../employee/dto/EmployeeOutput.java` — **deliberadamente flat, sem sub-objetos aninhados de Company/Position/Supervisor** (ver Dev Notes, "Por que `EmployeeOutput` não espelha o schema `Employee` completo"):
    ```java
    @Builder
    public record EmployeeOutput(
            Long id,
            String name,
            String nameTreatment,
            String taxIdentifier,
            String email,
            LocalDate birthDate,
            String observation,
            LocalDate dateOfHiring,
            EmployeeContractType contractType,
            LocalDate probationEndDate,
            StatusEmployee status,
            Long supervisorId,
            Long companyId,
            Long positionId
    ) {
    }
    ```

- [ ] Task 3: Repositórios — 3 métodos novos em 2 repositórios já existentes (AC: 3, 6, 7)
  - [ ] Em `EmployeeQueryRepository.java` (já existe, hoje só tem `existsByPositionId`/`existsByPositionIdAndStatus`), adicionar, mesmo padrão QueryDSL já usado no arquivo:
    ```java
    default boolean existsByTaxIdentifier(String taxIdentifier) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();
        booleanBuilder.and(qEmployee.taxIdentifier.cpf.eq(taxIdentifier));
        return exists(booleanBuilder.getValue());
    }

    default boolean existsByEmail(String email) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();
        booleanBuilder.and(qEmployee.email.email.eq(email));
        return exists(booleanBuilder.getValue());
    }
    ```
    `taxIdentifier.cpf`/`email.email` são os nomes de campo do `@Embeddable` (`Cpf.cpf`, `Email.email`) expostos no Q-type gerado — mesmo padrão de `company.taxIdentifier.cnpj` em `CompanyRepository.existsByTaxIdentifier` (`CompanyRepository.java:52`).
  - [ ] Em `ReasonPositionChangeRepository.java` (`.../employee/internal/`, hoje um shell vazio), adicionar:
    ```java
    Optional<ReasonPositionChange> findByCode(String code);
    ```
    Derived query simples do Spring Data (mesmo estilo de `EmployeeQueryRepository.findById`/`findAll` — não precisa de QueryDSL para um lookup de igualdade simples).

- [ ] Task 4: Expor `CompanyService.findCompanyById` — hoje só existe como método **privado** dentro de `CompanyServiceBean` (AC: 5)
  - [ ] Em `CompanyService.java` (specification, `.../company/specification/`), adicionar à interface:
    ```java
    /** Busca a entidade Company pelo id, para composição por outros agregados (ex.: Employee). */
    Company findCompanyById(@NonNull Long companyId);
    ```
  - [ ] Em `CompanyServiceBean.java:322`, o método `private Company findCompanyById(@NonNull Long companyId) { ... }` já existe com o corpo certo (`companyRepository.findById(companyId).orElseThrow(() -> new ScosException(SCOS_COMPANY_001))`) — só trocar `private` por `@Override public`, sem mudar o corpo.
  - [ ] **Por que isto é necessário, não incidental:** `Employee` referencia `Company` como `@ManyToOne`, precisando da entidade real (não só do `CompanyOutput` DTO que `CompanyService.findById` já expõe) para setar a FK e para checar `.isActive()` sem round-trip extra. Este é exatamente o padrão já em vigor entre `Position`↔`Department`: `PositionService.findPositionById(Long): Position` e `DepartmentService.findDepartmentById(Long): Department` (`DepartmentService.java`) já existem publicamente **só** para permitir essa composição cross-agregado. `Company` é o único dos três que ainda não expunha o equivalente — este story fecha essa lacuna com uma mudança de 1 linha (visibilidade), não uma reescrita.

- [ ] Task 5: Códigos de erro novos — `SCOS_EMPLOYEE_002..013` (AC: 2, 3, 5, 6)
  - [ ] `ExceptionCodeError.java`, após `SCOS_EMPLOYEE_001` (linha 112), adicionar:
    ```java
    /** CPF já cadastrado em outro funcionário (qualquer status). HTTP 409. */
    SCOS_EMPLOYEE_002("SCOS_EMPLOYEE_002", 409, "SCOS_TITLE_CONFLICT"),
    /** E-mail já cadastrado em outro funcionário (qualquer status). HTTP 409. */
    SCOS_EMPLOYEE_003("SCOS_EMPLOYEE_003", 409, "SCOS_TITLE_CONFLICT"),
    /** Supervisor informado não corresponde a nenhum funcionário existente. HTTP 404. */
    SCOS_EMPLOYEE_004("SCOS_EMPLOYEE_004", 404, "SCOS_TITLE_NOT_FOUND"),
    /** Empresa informada não está ACTIVE. HTTP 422. */
    SCOS_EMPLOYEE_005("SCOS_EMPLOYEE_005", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Cargo informado está inativo (active=false). HTTP 422. */
    SCOS_EMPLOYEE_006("SCOS_EMPLOYEE_006", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Supervisor informado não está ACTIVE. HTTP 422. */
    SCOS_EMPLOYEE_007("SCOS_EMPLOYEE_007", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Motivo de ativação informado está inativo. HTTP 422. */
    SCOS_EMPLOYEE_008("SCOS_EMPLOYEE_008", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Motivo de ativação incompatível com a entidade Funcionário (entityType != EMPLOYEE). HTTP 422. */
    SCOS_EMPLOYEE_009("SCOS_EMPLOYEE_009", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Domínio do e-mail não corresponde a EMPLOYEE_EMAIL_DOMAIN. HTTP 422. */
    SCOS_EMPLOYEE_010("SCOS_EMPLOYEE_010", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Idade em dateOfHiring é menor que EMPLOYEE_MIN_AGE. HTTP 422. */
    SCOS_EMPLOYEE_011("SCOS_EMPLOYEE_011", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Data de nascimento no futuro. HTTP 422. */
    SCOS_EMPLOYEE_012("SCOS_EMPLOYEE_012", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Data de admissão anterior à data de nascimento. HTTP 422. */
    SCOS_EMPLOYEE_013("SCOS_EMPLOYEE_013", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    ```
    **Antes de codar, confirme que `002..013` ainda estão livres** (`grep SCOS_EMPLOYEE_ ExceptionCodeError.java`) — se outra story/branch já reservou algum desses números nesse meio-tempo, renumerar em sequência, mantendo a ordem lógica acima.
    `012`/`013` são **`[ASSUMPTION]`**: `etc/doc/usecase/03-funcionario.md` lista "birthDate não futura" e "dateOfHiring não anterior a birthDate" na regra 2, mas **não** têm nenhum `UC-E` dedicado nem código reservado (diferente de idade mínima = `UC-E9`, domínio de e-mail = `UC-E2`) — decisão de criar 2 códigos novos em vez de reaproveitar algo é a mais consistente com o resto do arquivo, mas não está no doc-fonte; sinalizar para o PM se preferir um único código genérico para os dois.
  - [ ] `scos_message_organization.properties` (após a linha 57, `SCOS_EMPLOYEE_001=...`):
    ```properties
    SCOS_EMPLOYEE_002=Já existe um funcionário cadastrado com esse CPF.
    SCOS_EMPLOYEE_003=Já existe um funcionário cadastrado com esse e-mail.
    SCOS_EMPLOYEE_004=O supervisor informado não corresponde a nenhum funcionário existente.
    SCOS_EMPLOYEE_005=A empresa informada não está ativa.
    SCOS_EMPLOYEE_006=O cargo informado está inativo.
    SCOS_EMPLOYEE_007=O supervisor informado não está ativo.
    SCOS_EMPLOYEE_008=O motivo de ativação informado está inativo.
    SCOS_EMPLOYEE_009=O motivo de ativação informado é incompatível com a entidade Funcionário.
    SCOS_EMPLOYEE_010=O domínio do e-mail informado não é permitido.
    SCOS_EMPLOYEE_011=O funcionário não atinge a idade mínima exigida na data de admissão.
    SCOS_EMPLOYEE_012=A data de nascimento não pode ser no futuro.
    SCOS_EMPLOYEE_013=A data de admissão não pode ser anterior à data de nascimento.
    ```
  - [ ] `scos_message_organization_en.properties` — mesmas 12 chaves, texto em inglês, mesmo padrão dos pares PT/EN já existentes.

- [ ] Task 6: `EmployeeService` (specification) + `EmployeeServiceBean` — o núcleo da regra de negócio (AC: 1, 2, 3, 5, 6, 7)
  - [ ] Criar `.../employee/specification/EmployeeService.java`:
    ```java
    public interface EmployeeService {
        EmployeeOutput create(@NonNull EmployeeInput input);
    }
    ```
  - [ ] Criar `.../employee/service/EmployeeServiceBean.java`, injetando `EmployeeQueryRepository`, `EmployeeStatusHistoryRepository` (já existe, mesmo padrão de `CompanyStatusHistoryRepository`), `EmployeePositionHistoryRepository` (já existe), `EmployeeWorkScheduleRepository` (já existe, shell), `ReasonPositionChangeRepository`, `PositionWorkScheduleRepository` (já existe — reaproveitar `findAllByPositionId`, Story 1.5), `PositionService`, `CompanyService`, `ReasonActivateService`, `OrganizationConfigurationRepository`, `Clock`, `ScosUserAuthentication`:
    ```java
    private static final String NEW_HIRE_REASON_CODE = "NEW_HIRE";

    @Override
    @Transactional(rollbackFor = ScosException.class)
    public EmployeeOutput create(@NonNull EmployeeInput input) {
        log.info("Create Employee: {}", input.taxIdentifier());

        if (employeeQueryRepository.existsByTaxIdentifier(input.taxIdentifier())) {
            throw new ScosException(SCOS_EMPLOYEE_002);
        }
        if (employeeQueryRepository.existsByEmail(input.email())) {
            throw new ScosException(SCOS_EMPLOYEE_003);
        }

        Company company = findActiveCompanyOrThrow(input.companyId());
        Position position = findActivePositionOrThrow(input.positionId());
        Employee supervisor = resolveActiveSupervisor(input.supervisorId());
        validateReasonActivate(input.reasonActivateId());
        assertEmailDomain(input.email());
        assertBirthDateAndHiringDate(input.birthDate(), input.dateOfHiring());
        assertMinimumAge(input.birthDate(), input.dateOfHiring());

        String user = scosUserAuthentication.findUserAuthentication();

        Employee employee = Employee.builder()
                .name(input.name())
                .nameTreatment(input.nameTreatment())
                .taxIdentifier(new Cpf(input.taxIdentifier()))
                .email(new Email(input.email()))
                .birthDate(input.birthDate())
                .observation(input.observation())
                .dateOfHiring(input.dateOfHiring())
                .contractType(input.contractType())
                .probationEndDate(input.probationEndDate())
                .status(StatusEmployee.ACTIVE)
                .supervisor(supervisor)
                .position(position)
                .company(company)
                .build();
        employee.updateAuditInfo(user);
        employee = employeeQueryRepository.merge(employee);

        employeeStatusHistoryRepository.merge(
                EmployeeStatusHistory.builder()
                        .employee(employee)
                        .status(StatusEmployee.ACTIVE)
                        .reasonActivate(ReasonActivate.builder().id(input.reasonActivateId()).build())
                        .userAt(user)
                        .build()
        );

        employeePositionHistoryRepository.merge(
                EmployeePositionHistory.builder()
                        .employee(employee)
                        .position(position)
                        .startDate(input.dateOfHiring())
                        .reasonPositionChange(newHireReason())
                        .userAt(user)
                        .build()
        );

        copyWorkScheduleFromPosition(employee, position.getId());

        return toEmployeeOutput(employee);
    }

    private void copyWorkScheduleFromPosition(Employee employee, Long positionId) {
        positionWorkScheduleRepository.findAllByPositionId(positionId).forEach(template ->
                employeeWorkScheduleRepository.merge(
                        EmployeeWorkSchedule.builder()
                                .employee(employee)
                                .dayOfWeek(template.getDayOfWeek())
                                .startTime(template.getStartTime())
                                .lunchStart(template.getLunchStart())
                                .lunchEnd(template.getLunchEnd())
                                .endTime(template.getEndTime())
                                .build()
                )
        );
    }

    private ReasonPositionChange newHireReason() {
        return reasonPositionChangeRepository.findByCode(NEW_HIRE_REASON_CODE)
                .orElseThrow(() -> new ScosException(SCOS_CONFIGURATION_001)); // defensivo — catálogo é semeado via Liquibase/seed, não deveria faltar
    }

    private Company findActiveCompanyOrThrow(Long companyId) {
        Company company = companyService.findCompanyById(companyId); // 404 SCOS_COMPANY_001 se não existir
        if (!company.isActive()) {
            throw new ScosException(SCOS_EMPLOYEE_005);
        }
        return company;
    }

    private Position findActivePositionOrThrow(Long positionId) {
        Position position = positionService.findPositionById(positionId); // 404 SCOS_POSITION_001 se não existir
        if (!position.isActive()) {
            throw new ScosException(SCOS_EMPLOYEE_006);
        }
        return position;
    }

    private Employee resolveActiveSupervisor(Long supervisorId) {
        if (supervisorId == null) {
            return null;
        }
        Employee supervisor = employeeQueryRepository.findById(supervisorId)
                .orElseThrow(() -> new ScosException(SCOS_EMPLOYEE_004));
        if (supervisor.getStatus() != StatusEmployee.ACTIVE) {
            throw new ScosException(SCOS_EMPLOYEE_007);
        }
        return supervisor;
    }

    private void validateReasonActivate(Long reasonActivateId) {
        ReasonActivateOutput reasonActivate = reasonActivateService.findById(reasonActivateId); // 404 SCOS_REASON_ACTIVATE_001
        if (!reasonActivate.active()) {
            throw new ScosException(SCOS_EMPLOYEE_008);
        }
        if (reasonActivate.entityType() != EntityType.EMPLOYEE) {
            throw new ScosException(SCOS_EMPLOYEE_009);
        }
    }

    private void assertEmailDomain(String email) {
        String domain = employeeEmailDomain();
        if (!email.endsWith("@" + domain)) {
            throw new ScosException(SCOS_EMPLOYEE_010);
        }
    }

    private void assertBirthDateAndHiringDate(LocalDate birthDate, LocalDate dateOfHiring) {
        if (birthDate.isAfter(LocalDate.now(clock))) { // AD-8: "agora" só via Clock injetável, nunca LocalDate.now() cru
            throw new ScosException(SCOS_EMPLOYEE_012);
        }
        if (dateOfHiring.isBefore(birthDate)) {
            throw new ScosException(SCOS_EMPLOYEE_013);
        }
    }

    private void assertMinimumAge(LocalDate birthDate, LocalDate dateOfHiring) {
        int age = Period.between(birthDate, dateOfHiring).getYears(); // idade EM dateOfHiring, não "hoje" — dateOfHiring pode ser futura (UC-S3)
        if (age < minimumEmployeeAge()) {
            throw new ScosException(SCOS_EMPLOYEE_011);
        }
    }

    private int minimumEmployeeAge() {
        return Integer.parseInt(
                organizationConfigurationRepository.findById(ConfigurationKey.EMPLOYEE_MIN_AGE)
                        .orElseThrow(() -> new ScosException(SCOS_CONFIGURATION_001))
                        .getValue()
        );
    }

    private String employeeEmailDomain() {
        return organizationConfigurationRepository.findById(ConfigurationKey.EMPLOYEE_EMAIL_DOMAIN)
                .orElseThrow(() -> new ScosException(SCOS_CONFIGURATION_001))
                .getValue();
    }

    private EmployeeOutput toEmployeeOutput(Employee employee) {
        return EmployeeOutput.builder()
                .id(employee.getId())
                .name(employee.getName())
                .nameTreatment(employee.getNameTreatment())
                .taxIdentifier(employee.getTaxIdentifier().getCpf())
                .email(employee.getEmail().getEmail())
                .birthDate(employee.getBirthDate())
                .observation(employee.getObservation())
                .dateOfHiring(employee.getDateOfHiring())
                .contractType(employee.getContractType())
                .probationEndDate(employee.getProbationEndDate())
                .status(employee.getStatus())
                .supervisorId(employee.getSupervisor() != null ? employee.getSupervisor().getId() : null)
                .companyId(employee.getCompany().getId())
                .positionId(employee.getPosition().getId())
                .build();
    }
    ```
    **Ordem das guardas — segue explicitamente `etc/doc/usecase/03-funcionario.md` (regras 1-7), que já é "unicidade → FK/existência → estado/regra", igual ao padrão geral do projeto (`CompanyServiceBean`)** — diferente da Story 1.5, aqui **não** há justificativa para inverter a ordem. CPF/email duplicados (409) primeiro, depois existência de FKs (404), depois estado das FKs e motivo (422), depois data/idade (422). Cada checagem de estado (`findActiveCompanyOrThrow`/`findActivePositionOrThrow`) já resolve existência (404) **e** estado (422) numa função só, mesmo padrão de `findActiveDepartmentOrThrow` em `PositionServiceBean`.

- [ ] Task 7: `CreateEmployeeUseCase` + `Bean` (AC: 1, 4, 7)
  - [ ] Criar `flow-organization-usecase/.../usecase/corporate/employee/CreateEmployeeUseCase.java`:
    ```java
    public interface CreateEmployeeUseCase {
        Long execute(@NonNull CreateEmployeeRequest createEmployeeRequest);
    }
    ```
  - [ ] Criar `.../employee/CreateEmployeeUseCaseBean.java`:
    ```java
    @Service @RequiredArgsConstructor @Slf4j @Transactional(rollbackFor = ScosException.class)
    class CreateEmployeeUseCaseBean implements CreateEmployeeUseCase {
        private final EmployeeService employeeService;

        @Override
        public Long execute(@NonNull CreateEmployeeRequest request) {
            log.info("Create employee: {}", request.taxIdentifier());

            EmployeeInput input = EmployeeInput.builder()
                    .name(request.name())
                    .nameTreatment(request.nameTreatment())
                    .taxIdentifier(request.taxIdentifier())
                    .email(request.email())
                    .birthDate(request.birthDate())
                    .observation(request.observation())
                    .dateOfHiring(request.dateOfHiring())
                    .contractType(br.com.sawcunhaos.organization.domain.corporate.employee.internal.EmployeeContractType.valueOf(request.contractType().name()))
                    .probationEndDate(request.probationEndDate())
                    .supervisorId(request.supervisorId())
                    .companyId(request.companyId())
                    .positionId(request.positionId())
                    .reasonActivateId(request.reasonActivateId())
                    .build();

            return employeeService.create(input).id();
        }
    }
    ```
    `EmployeeContractType.valueOf(request.contractType().name())` usa FQN pro lado `domain.internal` porque `api.dto.EmployeeContractType` (gerado, mesmos 4 valores `CLT/PJ/ESTAGIO/TEMPORARIO`) tem o mesmo nome simples — mesmo problema/solução já usado pra `DayOfWeek` na Story 1.5 (`PositionApiMapper`). Se preferir, um mapper MapStruct de enum resolve sem FQN — qualquer uma das duas formas é aceitável.
    **Por que `execute` retorna `Long`, não o objeto `Employee` mapeado (diferente de `CreatePositionUseCase`, que devolve `api.dto.Position` completo):** o schema `Employee` (YAML) referencia sub-objetos `Supervisor`/`EmployeeCompany`/`Position` completos — montar isso exigiria um `EmployeeApiMapper` com mapeamento aninhado de 3 agregados, puro trabalho especulativo: **nenhum AC desta story valida o corpo de uma resposta de leitura**, só que o `201` aconteça e os efeitos colaterais (status history, position history, work schedule) estejam corretos. `POST /v1/employees` usa `201_CREATED` (o wrapper genérico `Create{id}`, confirmado no YAML) — só o `id` importa pro Delegate. Adiar o `EmployeeApiMapper` completo para a story que implementar `GET /v1/employees/{id}` evita construir uma peça que ninguém consome ainda.

- [ ] Task 8: `EmployeeDelegate` — pacote novo (AC: 1, 4, 7)
  - [ ] **Rodar `mvn generate-sources` em `flow-organization-usecase`/`flow-organization-api` antes de escrever o Delegate** — confirmar a assinatura exata gerada de `EmployeeApiDelegate.createEmployee(...)` (mesma recomendação da Story 1.5: o schema `Employee`/`CreateEmployeeRequest` nunca foi exercitado pelo generator neste módulo ainda, mesmo publicado há tempo).
  - [ ] Criar `flow-organization-api/.../api/delegate/employee/EmployeeDelegate.java` (pacote **novo** — hoje só existem `catalog`, `company`, `configuration`, `department`, `position`, `reason`):
    ```java
    @Component
    @RequiredArgsConstructor
    @Slf4j
    public class EmployeeDelegate implements EmployeeApiDelegate {

        private final CreateEmployeeUseCase createEmployeeUseCase;

        @Override
        public CreateResponse createEmployee(CreateEmployeeRequest createEmployeeRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
            return CreateResponse.builder()
                    .data(Create.builder().id(createEmployeeUseCase.execute(createEmployeeRequest)).build())
                    .build();
        }
    }
    ```
    Todo outro método de `EmployeeApiDelegate` (get/enable/disable/block/unblock/rehire/transfer/hierarchy/subordinates/position-history/work-schedule/contacts/addresses) **fica sem `@Override`** — cai no `default` gerado (`MethodNotImplementedException` ou equivalente), exatamente como qualquer delegate novo neste projeto antes de suas stories específicas serem implementadas. Não implementar nenhum deles aqui (Task 10).

- [ ] Task 9: Testes (AC: 1, 2, 3, 5, 6, 7)
  - [ ] `EmployeeServiceBeanTest.java` (novo, `flow-organization-domain`, `@ExtendWith(MockitoExtension.class)`, mocks de todos os collaborators listados na Task 6, `Clock.fixed(...)` para os testes de data): caminho feliz (Funcionário `ACTIVE`, `EmployeeStatusHistory` com `reasonActivate` correto, `EmployeePositionHistory` com o `ReasonPositionChange` de `code=NEW_HIRE` — capturar via `ArgumentCaptor` e confirmar que `reasonPositionChangeRepository.findByCode("NEW_HIRE")` foi chamado, não um id hardcoded); cópia de `N` dias do template Position→Employee Work Schedule (incluindo caso de 0 dias no template = nenhuma linha copiada, e caso de template com todos os 7 dias); CPF duplicado → `SCOS_EMPLOYEE_002`; email duplicado → `SCOS_EMPLOYEE_003`; `companyId`/`positionId`/`reasonActivateId` inexistentes → 404 reaproveitados; empresa não ativa → `SCOS_EMPLOYEE_005`; cargo inativo → `SCOS_EMPLOYEE_006`; motivo inativo/incompatível → `SCOS_EMPLOYEE_008`/`009`; `supervisorId` inexistente → `SCOS_EMPLOYEE_004`; supervisor não ativo → `SCOS_EMPLOYEE_007`; domínio de e-mail errado → `SCOS_EMPLOYEE_010`; idade insuficiente **calculada sobre `dateOfHiring`, não sobre `Clock.now()`** (caso de teste explícito: `dateOfHiring` futura com `birthDate` que só atinge `EMPLOYEE_MIN_AGE` na data futura) → sucesso, e o caso inverso → `SCOS_EMPLOYEE_011`; `birthDate` futura → `SCOS_EMPLOYEE_012`; `dateOfHiring` antes de `birthDate` → `SCOS_EMPLOYEE_013`.
  - [ ] `CreateEmployeeUseCaseBeanTest.java` (novo, `flow-organization-usecase`), mesmo padrão BDD de `CreateDepartmentUseCaseBeanTest`/`CreatePositionUseCaseBeanTest`: mapeamento request→input via `ArgumentCaptor`; propagação de `ScosException` quando o service lança; `request == null` → `NullPointerException` sem interagir com o service.
  - [ ] `EmployeeControllerTest.java` (novo, `flow-organization-boot`, `extends ScosOrganizationTestUtil` — **não** re-anotar com `@SpringBootTest`/`@Testcontainers`, ver `project-context.md`): caminho feliz completo `POST /v1/employees` → `201` + confirmar via query direta (ou endpoint de leitura, se já existir seed suficiente) que `SCOS_EMPLOYEE_STATUS_HISTORY`, `SCOS_EMPLOYEE_POSITION_HISTORY` e `SCOS_EMPLOYEE_WORK_SCHEDULE` foram populados; `409` CPF duplicado; `409` email duplicado; `404` `companyId`/`positionId`/`reasonActivateId`/`supervisorId` inexistentes; `422` cada uma das regras de estado/idade/domínio/data; `401` sem token; `403` sem `CREATE_EMPLOYEE`. **CPF é o campo `x-jdempotentrequestpayload`** (YAML linha 964) — cada teste que faz `POST` precisa de `taxIdentifier` único, mesmo cuidado já documentado em `project-context.md` para `code`/jDempotent. Cargo do seed (`SEEDED_ID`) precisa ter ao menos 1 linha de `PositionWorkSchedule` para exercitar a cópia — se o seed atual não tiver, adicionar via `POST` no próprio teste antes do `POST /v1/employees` (nunca editar `setsup_database.sql` para isso: cria acoplamento entre testes).

- [ ] Task 10: Guarda de escopo (AC: 4)
  - [ ] **Não** implementar `GET /v1/employees`, `GET /v1/employees/{id}`, `enable`/`disable`/`block`/`unblock`, `rehire`, `transfer`, `hierarchy`, `subordinates`, `position-history`, `contacts`, `addresses`, nem o CRUD de `EmployeeWorkSchedule` avulso (`GET/POST/PUT/DELETE /v1/employees/{employeeId}/work-schedule`) — todos já publicados no contrato, nenhum é tocado por esta story (Story 2.2/2.3 e backlog).
  - [ ] **Não** construir `EmployeeApiMapper`/`EmployeeOutput` aninhado com `Supervisor`/`EmployeeCompany`/`Position` completos — motivo já explicado na Task 7.
  - [ ] **Não** corrigir o defeito de contrato conhecido em `/v1/employees/rehire` (GET/PUT indevidamente aninhados, `etc/doc/usecase/03-funcionario.md` linha 63) — fora do escopo desta story, reportado, não bloqueante para `POST /v1/employees`.
  - [ ] **Não** adicionar entradas de descrição para `EMPLOYEE_MIN_AGE`/`EMPLOYEE_EMAIL_DOMAIN` nos bundles de mensagem (gap real, confirmado ausente — `ConfigurationServiceBean.getAllKeys()` chamaria `localeService.getMessage("EMPLOYEE_MIN_AGE")` sem chave correspondente) — só afeta o endpoint `GET /v1/configurations/keys`, que não faz parte de nenhum AC desta story; registrar como nota de backlog, não corrigir aqui silenciosamente.

## Dev Notes

### Contexto crítico — leia antes de implementar

**Isto não é greenfield puro na maior parte, mas Employee especificamente é.** O contrato (`POST /v1/employees`, `CreateEmployeeRequest`, permissão `CREATE_EMPLOYEE`) e o schema Liquibase (`scos_employee.yml`, `scos_employee_work_schedule.yml`, `scos_employee_position_history.yml`) já existem publicados — nada disso é decidido nesta story. Mas, diferente da Story 1.5 (onde só faltava a camada Java de um sub-recurso), aqui **não existe nenhum código de aplicação de Employee** — nem Service, nem Use Case, nem Delegate, nem pacote `api/delegate/employee/`. É a primeira vez que este agregado ganha qualquer camada além da entidade JPA e um serviço de consulta read-only.

**A pegadinha mais cara de errar nesta story: `EmployeePositionHistory.reasonPositionChangeId` é `NOT NULL` no banco (`scos_employee_position_history.yml`, `FK_REASON_POSITION_CHANGE_ID_SCOS_EMPLOYEE_POSITION_HISTORY`), mas `CreateEmployeeRequest` não tem nenhum campo `reasonPositionChangeId`.** Não é um contrato quebrado — é intencional: `etc/database/seed_data.sql:135` semeia `SCOS_REASON_POSITION_CHANGE(CODE='NEW_HIRE', DESCRIPTION='Atribuição inicial de cargo na contratação')` exatamente para este caso. O Service resolve esse motivo sozinho, buscando por `CODE='NEW_HIRE'` (nunca por `id=1` hardcoded — o id é `IDENTITY`, não garantidamente `1` em todo ambiente; só o `CODE` semeado é estável). Sem isso, a criação do Funcionário quebra com violação de `NOT NULL`/FK no INSERT de `EmployeePositionHistory` — um 500 feio em vez de qualquer coisa relacionada às regras de negócio do AC.

**Idade mínima é calculada sobre `dateOfHiring`, não sobre a data atual.** `dateOfHiring` pode ser futura (UC-S3 do doc de spec, "pré-cadastro"). `Period.between(birthDate, dateOfHiring).getYears() >= EMPLOYEE_MIN_AGE` é a fórmula certa — usar `LocalDate.now()`/`Clock` aqui em vez de `dateOfHiring` é um bug sutil que só aparece em admissões futuras (fáceis de não testar). Já "birthDate não pode ser no futuro" **é** relativo a "hoje" — e por isso, e só por isso, precisa do `Clock` injetável (bean já existe, `domain/config/ClockConfig.java`, `Clock.systemUTC()` em produção) — **nunca `LocalDate.now()` cru**, mesma regra AD-8/Story 0.2 já em vigor no domínio inteiro (`project-context.md`, seção "Tipos temporais"). Isto não é astrologia arquitetural: é a mesma convenção que já pegou bugs reais no projeto (Story 0.2).

**`CompanyService` não expõe (ainda) uma forma de outro agregado pegar a entidade `Company` real** — só `findById(Long): CompanyOutput` (DTO). `Position`↔`Department` já resolveram exatamente esse problema (`PositionService.findPositionById`/`DepartmentService.findDepartmentById`, ambos retornando a entidade). Esta story fecha a mesma lacuna pro lado `Company`, com a mudança mínima possível: o método já existe **implementado e correto** dentro de `CompanyServiceBean` como `private` (linha 322) — só sobe de visibilidade e entra na interface. Não é um redesenho.

**Ordem de validação é a mesma do resto do projeto (`unicidade → FK/existência → estado/regra`), sem a inversão que a Story 1.5 documentou para Position Work Schedule** — aqui não há justificativa de "checagem sem round-trip de banco primeiro", porque quase toda checagem desta story já depende de uma consulta (Company/Position/Employee/ReasonActivate). Seguir `etc/doc/usecase/03-funcionario.md` "Regras em ordem" (1 a 7) ao pé da letra.

### Por que `EmployeeOutput`/`CreateEmployeeUseCase` não espelham o schema `Employee` completo

O schema de leitura `Employee` (YAML linha 832) tem `supervisor: Supervisor{id,name}`, `company: EmployeeCompany{id,name}`, `position: Position` (objeto completo, com `department` aninhado). Popular isso de verdade — como `CreatePositionUseCase` faz para `Position` (retorna `api.dto.Position` completo, `PositionApiMapper.toApiPosition`) — exigiria juntar `PositionMapper`+`DepartmentMapper` (ambos já existem, mas produzir a saída completa não é gratuito) só para o Delegate descartar tudo e usar `.id()` no `CreateResponse` (o contrato usa `201_CREATED` genérico, não um `200` com `data: Employee`). Como nenhum AC desta story lê o corpo de uma resposta de leitura de Funcionário, construir esse mapeamento agora é trabalho especulativo que só a story de `GET /v1/employees/{id}` de fato precisa. `EmployeeOutput` (domain) fica flat com IDs de FK, e o Use Case devolve só o `Long` do id gerado — mesmo shape de retorno de `CreatePositionWorkScheduleUseCase` (Story 1.5), por um motivo diferente (lá o schema não tinha `id`; aqui tem, mas nada o consome ainda).

### Onde cada peça vai (camadas)

- **`domain/corporate/employee/dto/`**: `EmployeeInput`/`EmployeeOutput` (novos).
- **`domain/corporate/employee/specification/EmployeeService.java`** (novo).
- **`domain/corporate/employee/service/EmployeeServiceBean.java`** (novo — o grosso da lógica).
- **`domain/corporate/employee/internal/EmployeeQueryRepository.java`** (já existe, 2 métodos novos: `existsByTaxIdentifier`/`existsByEmail`).
- **`domain/corporate/employee/internal/ReasonPositionChangeRepository.java`** (já existe vazio, 1 método novo: `findByCode`).
- **`domain/corporate/company/specification/CompanyService.java`** + **`.../company/service/CompanyServiceBean.java`** (já existem — 1 método exposto, sem novo código).
- **`shared/exception/ExceptionCodeError.java`** + **`scos_message_organization[_en].properties`**: 12 códigos novos (`SCOS_EMPLOYEE_002..013`).
- **`usecase/corporate/employee/`** (pacote **novo**): `CreateEmployeeUseCase`/`Bean`.
- **`api/delegate/employee/EmployeeDelegate.java`** (pacote **novo**, 1 `@Override`).
- **`etc/api/organization/ScosOrganization_Employee.yml`**: 3 `maxLength` adicionados em `CreateEmployeeRequest` (gap de contrato, Task 1) — única mudança de contrato desta story; nenhum endpoint novo, nenhuma permissão nova (`CREATE_EMPLOYEE` já existe, `ScosOrganizationPermission.java:110`), nenhuma mudança de Liquibase.

### Testing Standards

- Unitário de domínio: `@ExtendWith(MockitoExtension.class)` + `@Mock`/`@InjectMocks`, mesmo padrão de `CompanyServiceBeanTest`/`PositionServiceBeanTest` — `assertThatThrownBy(...).hasFieldOrPropertyWithValue("code", SCOS_EMPLOYEE_0XX.getCode())`. Datas: `Clock.fixed(Instant..., ZoneOffset.UTC)` injetado via construtor de teste, nunca `Clock.systemUTC()` num teste.
- Unitário de Use Case: mesmo padrão BDD de `CreateDepartmentUseCaseBeanTest`/`CreatePositionUseCaseBeanTest`.
- Integração: novo `EmployeeControllerTest.java` estendendo `ScosOrganizationTestUtil` (Testcontainers Postgres+Redis já ativo, containers `static`, **nunca** `@Testcontainers`/`@Container` na subclasse). `code`/jDempotent: `taxIdentifier` único por teste que faz `POST` (é o campo `x-jdempotentrequestpayload`, não um `code` genérico — mesma classe de cuidado, campo diferente).

### Project Structure Notes

- 2 pacotes Maven **novos** (primeira vez que existem): `usecase/corporate/employee/` e `api/delegate/employee/`. Nenhum módulo Maven novo.
- Nenhuma mudança de Liquibase (schema já implementado integralmente para esta story).
- Única mudança de contrato: 3 `maxLength` em `ScosOrganization_Employee.yml` (Task 1) — não é endpoint/schema novo, é fechamento de gap num schema já publicado.

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Epic 2 / Story 2.1] — Given/When/Then originais.
- [Source: _bmad-output/planning-artifacts/prds/prd-SawCunhaOS-Organization-2026-07-18/prd.md#FR-5] — regras testáveis, `EMPLOYEE_MIN_AGE`/`EMPLOYEE_EMAIL_DOMAIN` via Configuration, `rehire` fora de escopo aqui.
- [Source: etc/doc/usecase/03-funcionario.md#POST /v1/employees, UC-035] — tabela de campos, "Regras em ordem" (1-7), Use Cases de Sucesso/Erro — fonte primária da ordem de validação e dos cenários de teste.
- [Source: etc/doc/usecase/03-funcionario.md#4.1 Jornada de Trabalho do Funcionário] — semântica da cópia (aplicação, não automática; sem histórico; dia sem linha = "não definido", nunca fallback pro template).
- [Source: _bmad-output/planning-artifacts/architecture/architecture-SawCunhaOS-Organization-2026-07-18/ARCHITECTURE-SPINE.md#Capability Map] — FR-4/FR-5 "Bean Validation padrão — sem AD nova"; nenhuma das AD-1..9 rege diretamente esta story (são de Login/turno/Kill Switch).
- [Source: etc/api/organization/ScosOrganization_Employee.yml:52-77,948-1002,832-867] — operação `createEmployee`, schema `CreateEmployeeRequest` (sem `maxLength`, gap confirmado), schema de leitura `Employee` completo (motivo de `EmployeeOutput` ficar flat).
- [Source: etc/api/organization/ScosOrganization_Company.yml:1030,1036,1189] — precedente de `maxLength: 250/100/255` pros mesmos limites, usado como referência da Task 1.
- [Source: etc/database/seed_data.sql:130-139] — catálogo `SCOS_REASON_POSITION_CHANGE` semeado, `CODE='NEW_HIRE'` (id 1, não hardcodar).
- [Source: organization/flow-organization-resources/.../tables/scos_employee_position_history.yml:40-49] — `REASON_POSITION_CHANGE_ID NOT NULL`, a pegadinha central desta story.
- [Source: organization/flow-organization-resources/.../tables/scos_employee.yml, scos_employee_work_schedule.yml] — colunas/constraints reais (`UK_TAX_IDENTIFIER_SCOS_EMPLOYEE`, `UK_EMAIL_SCOS_EMPLOYEE`, `UK_EMPLOYEE_ID_DAY_OF_WEEK_SCOS_EMPLOYEE_WORK_SCHEDULE`).
- [Source: organization/flow-organization-domain/.../corporate/employee/internal/Employee.java] — entidade já existente, 4 métodos de transição de status (padrão "retorna history, não persiste").
- [Source: organization/flow-organization-domain/.../corporate/employee/internal/EmployeeQueryRepository.java] — repositório já existente, 2 métodos novos desta story.
- [Source: organization/flow-organization-domain/.../corporate/company/service/CompanyServiceBean.java:107-149,322-336,422-427] — padrão de `create()` a replicar (unicidade→FK→regra, `updateAuditInfo`, grava status history na mesma transação), `findCompanyById` privado (Task 4), `maxHierarchyDepth()` como padrão de leitura de `Configuration`.
- [Source: organization/flow-organization-domain/.../company/specification/CompanyService.java, .../department/specification/DepartmentService.java, .../position/specification/PositionService.java] — precedente `findDepartmentById`/`findPositionById` (entidade, não DTO) que justifica expor `CompanyService.findCompanyById`.
- [Source: organization/flow-organization-domain/.../position/service/PositionServiceBean.java:44-75] — `findActiveDepartmentOrThrow`, padrão espelhado por `findActiveCompanyOrThrow`/`findActivePositionOrThrow` desta story.
- [Source: organization/flow-organization-domain/.../position/service/PositionWorkScheduleServiceBean.java, .../position/internal/PositionWorkScheduleRepository.java] — `findAllByPositionId` (Story 1.5) reaproveitado para a cópia de jornada.
- [Source: organization/flow-organization-domain/.../employee/internal/EmployeeWorkSchedule.java] — entidade já existente, mesma forma de `PositionWorkSchedule`, `DayOfWeek` compartilhado.
- [Source: organization/flow-organization-domain/.../access/status/internal/EmployeeStatusHistory.java, CompanyStatusHistory.java] — mesma forma, confirma o padrão de gravação do histórico inicial.
- [Source: organization/flow-organization-domain/.../employee/internal/EmployeePositionHistory.java, ReasonPositionChange.java, EmployeeContractType.java] — entidades já existentes usadas por esta story.
- [Source: organization/flow-organization-domain/.../configuration/internal/ConfigurationKey.java:26-30] — `EMPLOYEE_MIN_AGE`/`EMPLOYEE_EMAIL_DOMAIN` já cadastrados, não precisam ser criados.
- [Source: organization/flow-organization-domain/.../access/status/internal/EntityType.java, .../access/status/specification/ReasonActivateService.java, .../access/status/dto/ReasonActivateOutput.java] — `EntityType.EMPLOYEE` já existe; `validateReasonActivate` espelha `CompanyServiceBean.validateReasonActivate` (linha 328).
- [Source: SawCunhaOS-Foundation/utils/.../valueobjects/Cpf.java, Email.java] — validação de formato/DV acontece no construtor do Value Object (lança `ScosException` mapeado globalmente pra `SCOS_VALIDATION_011`/`SCOS_VALIDATION_005`) — não escrever validação manual duplicada no Service.
- [Source: organization/flow-organization-domain/.../config/ClockConfig.java, .../access/system/service/ScosSystemServiceBean.java:105] — `Clock` bean já existente (Story 0.2/AD-8), padrão de injeção a seguir para "birthDate não futura".
- [Source: organization/flow-organization-infrastructure/.../enumaration/ScosOrganizationPermission.java:110] — `CREATE_EMPLOYEE` já cadastrada, nada a fazer aqui.
- [Source: organization/flow-organization-shared/.../exception/ExceptionCodeError.java:42-43,78-112,155-160] — blocos `Configuration`/`Company`/`Employee`/`ReasonActivate` já existentes; `SCOS_EMPLOYEE_002..013` novos desta story.
- [Source: organization/flow-organization-usecase/.../corporate/position/CreatePositionUseCase(Bean).java, PositionApiMapper.java] — padrão de Use Case + api-mapper a considerar (e por que esta story diverge, retornando `Long`).
- [Source: organization/flow-organization-usecase/.../corporate/position/CreatePositionWorkScheduleUseCase(Bean).java] (Story 1.5) — padrão de retorno `Long` a replicar exatamente.
- [Source: organization/flow-organization-api/.../delegate/position/PositionDelegate.java:81-89] — padrão exato de `CreateResponse`/`Create` a replicar no `EmployeeDelegate`.
- [Source: organization/flow-organization-domain/src/test/.../employee/service/EmployeePositionQueryServiceBeanTest.java] — único teste hoje relacionado a Employee; confirma padrão Mockito do módulo.
- [Source: organization/flow-organization-usecase/src/test/.../corporate/department/CreateDepartmentUseCaseBeanTest.java] — padrão de teste BDD de Use Case a replicar.
- [Source: organization/flow-organization-boot/src/test/resources/postgresql/setsup_database.sql:558-562] — seed de teste de `EMPLOYEE_MIN_AGE=18`, `EMPLOYEE_EMAIL_DOMAIN=sawcunhaos.com.br` já presentes (confirmar se ambiente real/produção tem changeset equivalente — não localizado, ressalva registrada).
- [Source: _bmad-output/implementation-artifacts/1-5-template-jornada-trabalho-cargo.md] — story de referência de qualidade/formato para esta (padrão de Tasks/Dev Notes/References), e origem de `PositionWorkScheduleRepository.findAllByPositionId` reaproveitado aqui.
- [Source: _bmad-output/implementation-artifacts/0-2-padronizar-tipos-temporais-clock-injetavel.md] — origem da convenção `Clock` injetável (AD-8) aplicada à checagem de `birthDate` futura.

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List
