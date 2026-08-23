---
baseline_commit: ad3702111db59c2d7375471f632be53b9a8f3e40
---

# Story 2.5: Consulta de Funcionário — Listagem e Detalhe

Status: review

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como Analista de RH,
Eu quero listar Funcionários com paginação e filtros (empresa, cargo, status) e consultar o detalhe completo de um Funcionário,
Para localizar rapidamente quem eu preciso gerenciar.

## Acceptance Criteria

1. **Given** um Funcionário existente **When** RH aciona `GET /v1/employees/{id}` (`GET_EMPLOYEE`) **Then** o sistema retorna `200` com o corpo `GetEmployeeResponse`/`Employee` completo (`supervisor`/`company`/`position` aninhados, mesmo formato já usado pela resposta de `rehire`, Story 2.3) **And** `id` inexistente retorna `404 SCOS_EMPLOYEE_014` (código já existente, criado na Story 2.2 — reaproveitado, não um novo).
2. **Given** a listagem de Funcionários **When** RH aciona `GET /v1/employees` (`GET_EMPLOYEE`) sem nenhum filtro **Then** o sistema retorna `200` com todos os Funcionários paginados, corpo `GetAllEmployeesResponse` — `data` é uma lista do schema resumido `Employees` (`id`/`name`/`nameTreatment`/`email`/`status`, **sem** `company`/`position`/`supervisor` aninhados) **And** `paginatedDTO` com os metadados de paginação (mesmo formato de `GetAllCompaniesResponse`).
3. **Given** a mesma listagem **When** RH informa `companyId`, `positionId` e/ou `status` (todos opcionais na query string, `required: false` no contrato, combináveis entre si) **Then** o sistema retorna só os Funcionários que atendem **simultaneamente** a todos os filtros informados — filtro omitido não restringe nada (mesmo comportamento de `ContactTypeService.findAll`/`CompanyService.findAll`, já existentes).
4. **Given** o contrato hoje aninha incorretamente `getEmployeeById` (`GET`) e `updateEmployee` (`PUT`) sob o path `/v1/employees/rehire` — sem `{id}` no template, mesmo bloco do `POST rehireEmployee` (defeito já sinalizado pela Story 2.1 Task 10 e pela Story 2.3 AC 8/Task 11, nunca corrigido; confirmado por leitura do gerador: o Javadoc gerado de `EmployeeApiDelegate.getEmployeeById` hoje diz literalmente `"GET /v1/employees/rehire : Get employee by id"`) **When** esta story é implementada **Then** corrige o path para `/v1/employees/{id}` (novo bloco, com `get`/`put`) **And** `/v1/employees/rehire` fica só com `post` (`rehireEmployee`) **And** `updateEmployee` **não** ganha `@Override` no Delegate nesta story (fora de escopo, ver Task 9) — só o path é corrigido, a rota continua sem implementação Java.
5. **Given** nenhum Use Case/Delegate de listagem/detalhe existe hoje para Employee — só o primitivo de domínio `EmployeeService.findById` (criado pela Story 2.3 exclusivamente para uso interno do `rehire`, nunca exposto por rota própria) **When** esta story é implementada **Then** cria `FindEmployeeUseCase`/`FindAllEmployeeUseCase` (+ Beans) do zero, mesmo padrão de `FindCompanyUseCase`/`FindAllCompanyUseCase` (Company) e `FindPositionUseCase`/`FindAllPositionUseCase` (Position) — **reaproveitando** `EmployeeService.findById` (não duplicando a leitura por id) e `EmployeeApiMapper.toApiEmployee` (Story 2.3, não recriando a montagem do `Employee` aninhado).
6. **Given** sem token, ou com token válido mas sem a permissão `GET_EMPLOYEE` **When** qualquer uma das 2 rotas é chamada **Then** o sistema rejeita com `401` (sem token) ou `403 SCOS-004` (sem permissão) — `GET_EMPLOYEE` já está cadastrada em `ScosGeotemporalPermission` (Story 2.1), nenhuma permissão nova é criada nesta story.

## Tasks / Subtasks

- [x] Task 1: Corrigir defeito de contrato — mover `getEmployeeById`/`updateEmployee` para `/v1/employees/{id}` (AC: 1, 4)
  - [x] Em `etc/api/organization/ScosOrganization_Employee.yml`, o bloco `/v1/employees/rehire:` hoje contém 3 operações no mesmo path item: `post` (`rehireEmployee`), `get` (`getEmployeeById`) e `put` (`updateEmployee`) — as duas últimas usam `$ref: '#/components/parameters/idRequest'` (`name: id, in: path`, definido em `ScosComponents.yml:52-58`), mas o path **não tem** `{id}` no template, então o binding do path variable está estruturalmente quebrado (confirmado: o Javadoc já gerado por `mvn generate-sources` diz `"GET /v1/employees/rehire : Get employee by id"`).
  - [x] Extrair `get`/`put` do bloco `/v1/employees/rehire:` para um novo bloco `/v1/employees/{id}:`, posicionado logo após `/v1/employees/rehire:` (antes de `/v1/employees/{id}/enable:`). **Não** mudar nenhum `operationId`, `x-authorize`, schema `$ref` ou descrição — só mover as 2 operações de path item. Resultado esperado:
    ```yaml
    /v1/employees/rehire:
      post:
        tags: [Employee]
        summary: Rehire employee
        description: >-
          UC-155 - Recontrata um funcionário localizado pelo CPF, cujo status atual é INACTIVE. ...
        operationId: rehireEmployee
        parameters:
          - $ref: '../ScosComponents.yml#/components/parameters/X-Request-ID'
          - $ref: '../ScosComponents.yml#/components/parameters/Accept-Language'
        requestBody:
          required: true
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/RehireEmployeeRequest'
          x-jdempotentrequestpayload: true
        responses:
          '200':
            description: OK
            content:
              application/json:
                schema:
                  $ref: '#/components/schemas/GetEmployeeResponse'
          '4XX':
            $ref: '../ScosComponents.yml#/components/responses/4XX'
          '5XX':
            $ref: '../ScosComponents.yml#/components/responses/5XX'
        x-authorize: [REHIRE_EMPLOYEE]
        x-jdempotentresource:
          cachePrefix: SCOS_ORGANIZATION_IDP_REHIRE_EMPLOYEE
          ttl: 1

    /v1/employees/{id}:
      get:
        tags: [Employee]
        summary: Get employee by id
        description: UC-037 - Retorna dados completos incluindo cargo, empresa e supervisor
        operationId: getEmployeeById
        parameters:
          - $ref: '../ScosComponents.yml#/components/parameters/X-Request-ID'
          - $ref: '../ScosComponents.yml#/components/parameters/Accept-Language'
          - $ref: '../ScosComponents.yml#/components/parameters/idRequest'
        responses:
          '200':
            description: OK
            content:
              application/json:
                schema:
                  $ref: '#/components/schemas/GetEmployeeResponse'
          '4XX':
            $ref: '../ScosComponents.yml#/components/responses/4XX'
          '5XX':
            $ref: '../ScosComponents.yml#/components/responses/5XX'
        x-authorize: [GET_EMPLOYEE]
      put:
        tags: [Employee]
        summary: Update employee
        description: UC-038 - Atualiza dados pessoais. Para empresa, cargo ou supervisor use transfer
        operationId: updateEmployee
        parameters:
          - $ref: '../ScosComponents.yml#/components/parameters/X-Request-ID'
          - $ref: '../ScosComponents.yml#/components/parameters/Accept-Language'
          - $ref: '../ScosComponents.yml#/components/parameters/idRequest'
        requestBody:
          required: true
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/UpdateEmployeeRequest'
          x-jdempotentrequestpayload: true
        responses:
          '204':
            $ref: '../ScosComponents.yml#/components/responses/204_NO_CONTENT'
          '4XX':
            $ref: '../ScosComponents.yml#/components/responses/4XX'
          '5XX':
            $ref: '../ScosComponents.yml#/components/responses/5XX'
        x-authorize: [UPDATE_EMPLOYEE]
        x-jdempotentresource:
          cachePrefix: SCOS_ORGANIZATION_IDP_UPDATE_EMPLOYEE
          ttl: 1
    ```
  - [x] **Não** tocar no bloco `/v1/employees:` (linhas 27-77, já correto — `get`/`getAllEmployees` e `post`/`createEmployee` no path certo).
  - [x] Rodar `mvn clean generate-sources` em `flow-organization-usecase` **e** `flow-organization-api` logo após editar o YAML — o Javadoc gerado de `getEmployeeById` deve passar a dizer `"GET /v1/employees/{id} : Get employee by id"` (confirma a correção antes de escrever qualquer Java).

- [x] Task 2: `EmployeeQueryRepository` — 1 método novo, filtros opcionais via QueryDSL (AC: 2, 3)
  - [x] Em `EmployeeQueryRepository.java` (já existe), adicionar, mesmo padrão exato de `ContactTypeRepository.findAllFiltered` (`organization/flow-organization-domain/.../catalog/internal/ContactTypeRepository.java:64-79`):
    ```java
    default Page<Employee> findAllFiltered(Long companyId, Long positionId, StatusEmployee status, Pageable pageable) {
        if (Objects.isNull(companyId) && Objects.isNull(positionId) && Objects.isNull(status)) {
            return findAll(pageable);
        }

        BooleanBuilder booleanBuilder = new BooleanBuilder();

        if (Objects.nonNull(companyId)) {
            booleanBuilder.and(qEmployee.company.id.eq(companyId));
        }
        if (Objects.nonNull(positionId)) {
            booleanBuilder.and(qEmployee.position.id.eq(positionId));
        }
        if (Objects.nonNull(status)) {
            booleanBuilder.and(qEmployee.status.eq(status));
        }

        return findAll(booleanBuilder.getValue(), pageable);
    }
    ```
    Adicionar `import java.util.Objects;`. `findAll(Predicate, Pageable)` vem de `QuerydslPredicateExecutor`, já na interface — mesmo `findAll(Pageable)` sem filtro já existente na linha 32 é reaproveitado no atalho "nenhum filtro informado".

- [x] Task 3: `EmployeeService` (specification) — 1 assinatura nova (`findById` já existe, Story 2.3) (AC: 2, 3)
  - [x] Em `EmployeeService.java`, adicionar:
    ```java
    /** Lista Funcionários paginados, filtrando por companyId/positionId/status quando informados (todos opcionais). */
    Page<EmployeeOutput> findAll(Long companyId, Long positionId, StatusEmployee status, @NonNull Pageable pageable);
    ```
    Import novo: `org.springframework.data.domain.Page`, `org.springframework.data.domain.Pageable`. **Não** mexer em `findById` (já implementado e testado pela Story 2.3).

- [x] Task 4: `EmployeeServiceBean` — implementar `findAll` (AC: 2, 3)
  - [x] Adicionar, reaproveitando o `toEmployeeOutput` privado já existente (usado por `create`/`rehire`/`findById`):
    ```java
    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeOutput> findAll(Long companyId, Long positionId, StatusEmployee status, @NonNull Pageable pageable) {
        log.info("Find All Employees, CompanyId: {}, PositionId: {}, Status: {}", companyId, positionId, status);
        return employeeQueryRepository.findAllFiltered(companyId, positionId, status, pageable)
                .map(this::toEmployeeOutput);
    }
    ```

- [x] Task 5: `EmployeeApiMapper` — 3 métodos novos no mapper já existente (Story 2.3) (AC: 2)
  - [x] Em `usecase/corporate/employee/EmployeeApiMapper.java` (criado pela Story 2.3 com `toApiEmployee` completo), adicionar o par resumido + conversores de status, mesmo padrão exato de `CompanyApiMapper.toApiCompanies`/`toApiStatus`/`toDomainStatus` (`organization/flow-organization-usecase/.../corporate/company/CompanyApiMapper.java:51-61,75-82`):
    ```java
    /** Monta o {@code Employees} resumido (UC-036, listagem) a partir da saída do domínio — sem company/position/supervisor aninhados. */
    static Employees toApiEmployees(EmployeeOutput employee) {
        return Employees.builder()
                .id(employee.id())
                .name(employee.name())
                .nameTreatment(employee.nameTreatment())
                .email(employee.email())
                .status(toApiStatus(employee.status()))
                .build();
    }

    static EmployeeStatus toApiStatus(StatusEmployee status) {
        return status == null ? null : EmployeeStatus.valueOf(status.name());
    }

    static StatusEmployee toDomainStatus(EmployeeStatus status) {
        return status == null ? null : StatusEmployee.valueOf(status.name());
    }
    ```
    Imports novos: `br.com.sawcunhaos.organization.api.dto.Employees`, `br.com.sawcunhaos.organization.api.dto.EmployeeStatus`, `br.com.sawcunhaos.organization.domain.corporate.employee.internal.StatusEmployee`. **Não** alterar `toApiEmployee` existente (continua usando `EmployeeStatus.valueOf(employee.status().name())` inline — só as rotas novas passam a usar os conversores nomeados).

- [x] Task 6: `FindEmployeeUseCase`/`FindAllEmployeeUseCase` + Beans — pacote já existente (AC: 1, 2, 3, 5)
  - [x] Em `usecase/corporate/employee/` (pacote da Story 2.1), criar, espelhando **exatamente** `FindPositionUseCase(Bean)` (detalhe por id, `organization/flow-organization-usecase/.../corporate/position/FindPositionUseCaseBean.java`) e `FindAllCompanyUseCase(Bean)` (listagem paginada+filtros, `organization/flow-organization-usecase/.../corporate/company/FindAllCompanyUseCaseBean.java`):
    ```java
    public interface FindEmployeeUseCase {
        Employee execute(@NonNull Long id);
    }
    ```
    ```java
    @Service
    @RequiredArgsConstructor
    @Slf4j
    @Transactional(readOnly = true)
    class FindEmployeeUseCaseBean implements FindEmployeeUseCase {

        private final EmployeeService employeeService;
        private final CompanyService companyService;
        private final PositionService positionService;

        @Override
        public Employee execute(@NonNull Long id) {
            log.info("Find Employee: {}", id);
            EmployeeOutput output = employeeService.findById(id);
            CompanyOutput company = companyService.findById(output.companyId());
            PositionOutput position = positionService.findById(output.positionId());
            EmployeeOutput supervisor = output.supervisorId() != null ? employeeService.findById(output.supervisorId()) : null;
            return EmployeeApiMapper.toApiEmployee(output, company, position, supervisor);
        }
    }
    ```
    ```java
    public interface FindAllEmployeeUseCase {
        GetAllEmployeesResponse execute(@NonNull PaginationFilter paginationFilter, Long companyId, Long positionId, EmployeeStatus status);
    }
    ```
    ```java
    @Service
    @RequiredArgsConstructor
    @Slf4j
    @Transactional(readOnly = true)
    class FindAllEmployeeUseCaseBean implements FindAllEmployeeUseCase {

        private final EmployeeService employeeService;

        @Override
        public GetAllEmployeesResponse execute(@NonNull PaginationFilter paginationFilter, Long companyId, Long positionId, EmployeeStatus status) {
            log.info("Find All Employees, Page: {}, Size: {}, CompanyId: {}, PositionId: {}, Status: {}",
                    paginationFilter.page(), paginationFilter.sizePerPage(), companyId, positionId, status);

            Pageable pageable = PaginatioUtils.createPageable(paginationFilter);

            Page<EmployeeOutput> employeeOutput = employeeService.findAll(
                    companyId, positionId, EmployeeApiMapper.toDomainStatus(status), pageable
            );

            return GetAllEmployeesResponse.builder()
                    .data(employeeOutput.getContent().stream().map(EmployeeApiMapper::toApiEmployees).toList())
                    .paginatedDTO(PaginatioUtils.createScosPaginated(employeeOutput))
                    .build();
        }
    }
    ```
    `FindEmployeeUseCase.execute` retorna `br.com.sawcunhaos.organization.api.dto.Employee` — mesma colisão de nome simples com a entidade de domínio já resolvida pela Story 2.3 (`RehireEmployeeUseCase`); como o corpo aqui não referencia a entidade de domínio diretamente (só `EmployeeOutput`/`CompanyOutput`/`PositionOutput`), basta importar `api.dto.Employee` normalmente, sem FQN. `PaginatioUtils` já existe em `usecase/utils/` (reaproveitado de `ContactType`/`Company`, nenhuma mudança nele).

- [x] Task 7: `EmployeeDelegate` — 2 métodos novos na classe já existente (AC: 1, 2, 3, 6)
  - [x] **Confirmar antes** que a Task 1 (fix de contrato) já rodou `generate-sources` — a assinatura exata de `EmployeeApiDelegate.getEmployeeById`/`getAllEmployees` muda de path (`{id}` passa a existir de fato) mas os parâmetros Java não mudam.
  - [x] Em `EmployeeDelegate.java` (criada pela Story 2.1), injetar `FindEmployeeUseCase`/`FindAllEmployeeUseCase` (campos `final`, `@RequiredArgsConstructor` já cobre) e implementar, espelhando `CompanyDelegate.getCompanyById`/`getAllCompanies` (`organization/flow-organization-api/.../delegate/company/CompanyDelegate.java:73-82`):
    ```java
    @Override
    public GetAllEmployeesResponse getAllEmployees(PaginationFilter paginationFilter, Optional<UUID> xRequestID, Optional<String> acceptLanguage, Optional<Long> companyId, Optional<Long> positionId, Optional<EmployeeStatus> status) {
        return findAllEmployeeUseCase.execute(paginationFilter, companyId.orElse(null), positionId.orElse(null), status.orElse(null));
    }

    @Override
    public GetEmployeeResponse getEmployeeById(Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        return GetEmployeeResponse.builder()
                .data(findEmployeeUseCase.execute(id))
                .build();
    }
    ```
    **Não** implementar `updateEmployee` (mesmo bloco YAML após a Task 1, método `PUT` — AC 4, fora de escopo).

- [x] Task 8: Testes (AC: 1, 2, 3, 4, 5, 6)
  - [x] `EmployeeServiceBeanTest.java` (já existe): bloco `findAll` — sem filtro retorna todos paginados (mock `employeeQueryRepository.findAllFiltered(null, null, null, pageable)`); com só `companyId`; com só `positionId`; com só `status`; combinando os 3 — cada cenário só precisa verificar que `employeeQueryRepository.findAllFiltered` foi chamado com os argumentos certos (via `ArgumentCaptor` ou `verify`) e que o resultado é mapeado por `toEmployeeOutput`, mesmo nível de teste que `create`/`rehire` já têm (não precisa reimplementar QueryDSL no teste, só mockar o repositório).
  - [x] `FindEmployeeUseCaseBeanTest.java` (novo, `flow-organization-usecase/src/test/.../corporate/employee/`), mesmo padrão de `RehireEmployeeUseCaseBeanTest` (Story 2.3): caminho feliz com supervisor (verifica `company`/`position`/`supervisor` aninhados no `Employee` retornado); caminho feliz sem supervisor (`supervisorId() == null` → `employeeService.findById` **não** é chamado uma segunda vez, `then(employeeService).should(times(1))...`); propagação de `ScosException` quando `employeeService.findById` lança (`SCOS_EMPLOYEE_014`, sem precisar simular o código — só verificar propagação).
  - [x] `FindAllEmployeeUseCaseBeanTest.java` (novo, mesmo pacote), mesmo padrão BDD: mapeamento de filtros para `employeeService.findAll` via `ArgumentCaptor`/`verify` (companyId/positionId/status repassados sem transformação, exceto `status` que passa por `EmployeeApiMapper.toDomainStatus`); `paginatedDTO`/`data` montados a partir do `Page<EmployeeOutput>` mockado (2+ elementos, checar que vira lista de `Employees` resumido); filtros todos nulos → chamada com `(null, null, null, pageable)`.
  - [x] `EmployeeControllerTest.java` (já existe): novo bloco `GET /v1/employees/{id}` — sucesso `200` com corpo completo (`$.data.company.id`, `$.data.position.id`, `$.data.supervisor` quando aplicável, mesmo formato já validado pelo teste de `rehire`, Story 2.3); `404 SCOS_EMPLOYEE_014` para id inexistente; `401` sem token; `403` sem `GET_EMPLOYEE`. Novo bloco `GET /v1/employees` — sucesso sem filtro (`$.data` array, `$.paginatedDTO` existe, mesmo padrão de `CompanyControllerTest.getAll_withValidToken_returns200`); sucesso filtrando por `companyId`/`positionId`/`status` (criar 2+ Funcionários com atributos diferentes dentro do próprio teste, como já é padrão neste arquivo desde a Story 2.3 — Task 9 dela); `401` sem token; `403` sem `GET_EMPLOYEE`. **CPF único por Funcionário criado** (mesmo cuidado de idempotência já documentado no cabeçalho da classe).

- [x] Task 9: Guarda de escopo (AC: 4)
  - [x] **Não** implementar `updateEmployee` (`PUT /v1/employees/{id}`) — só o path é corrigido (Task 1), a rota continua sem `@Override` no Delegate.
  - [x] **Não** implementar `getEmployeeHierarchy`, `getEmployeeSubordinates`, `getEmployeePositionHistory`, `getEmployeeStatusHistory`, nem os endpoints de `Employee Contact`/`Employee Address`/`Employee Work Schedule` — nenhum é tocado por esta story, todos são recursos separados no mesmo arquivo YAML.
  - [x] **Não** criar nenhuma permissão nova — `GET_EMPLOYEE` já existe (`ScosGeotemporalPermission`, Story 2.1), reaproveitada pelas 2 rotas desta story.
  - [x] **Não** adicionar filtro novo além de `companyId`/`positionId`/`status` — são os 3 únicos já publicados no contrato (`companyIdFilter`/`positionIdFilter`/`statusFilter`, linhas 37-39 do YAML); não inventar filtro por nome/CPF que o contrato não pede.

## Dev Notes

### Contexto crítico — leia antes de implementar

**Depende das Stories 2.1 e 2.3 já implementadas.** `EmployeeService`/`EmployeeServiceBean`/`EmployeeDelegate` já existem (Story 2.1); `EmployeeService.findById` e `EmployeeApiMapper` (com `toApiEmployee` completo) já existem (Story 2.3, criados como primitivos internos do `rehire`). Esta story é a primeira a **expor** esses primitivos por rota própria — não recriar nada que já existe, só adicionar.

**O defeito de contrato (Task 1) é bloqueante, não cosmético.** Diferente das Stories 2.1/2.3 (que só *sinalizaram* o defeito e seguiram em frente, porque não precisavam de `getEmployeeById` funcionando), esta story **precisa** que `GET /v1/employees/{id}` roteie de verdade — hoje o path `/v1/employees/rehire` não tem `{id}`, então o parâmetro de path do `getEmployeeById` gerado não tem onde se ligar. Confirmado por leitura direta do código já gerado (`mvn generate-sources` já rodou nesta sessão de análise): o Javadoc do método diz `"GET /v1/employees/rehire : Get employee by id"`. Corrigir o YAML **antes** de qualquer código Java (mesma ordem "contrato primeiro" de sempre).

**Por que esta story não implementa `updateEmployee` mesmo corrigindo o path dele:** `updateEmployee` está no **mesmo bloco de path item** que `getEmployeeById` (`/v1/employees/{id}`, método `PUT`) — corrigir o path exige mover as duas operações juntas (elas compartilham o mesmo path item no YAML), mas isso não obriga a implementar as duas no Java. `updateEmployee` (atualização de dados pessoais) é uma feature própria, sem AC nesta story — fica com `@Override` ausente no Delegate, exatamente como já estava antes (só migrou de path).

**Por que o par completo/resumido (`Employee`/`Employees`) já existe no contrato, mas só um lado tem mapper hoje:** a Story 2.3 criou `EmployeeApiMapper.toApiEmployee` (completo, para `rehire`) porque precisava. O schema `Employees` (resumido, para a listagem) sempre esteve no YAML (linhas 818-831), mas nenhum código o consome ainda — Task 5 fecha essa lacuna, mesmo padrão de `CompanyApiMapper` (que desde a Story 1.x já tem os dois: `toApiCompany`/`toApiCompanies`).

**Filtros são todos opcionais e combináveis — não confundir com o padrão de `Position` (Story 1.4/1.5), que tem filtros obrigatórios.** `PositionService.findAll(@NonNull Long departmentId, @NonNull Boolean active, ...)` exige os 2 filtros sempre. Já `ContactTypeService.findAll(EntityType entityType, Boolean active, ...)` e `CompanyService.findAll(StatusCompany status, String name, ...)` aceitam qualquer combinação de nulos — **este** é o padrão certo para Employee, porque o contrato (`ScosOrganization_Employee.yml:37-39`) marca `companyId`/`positionId`/`status` como `required: false` nos 3.

### Onde cada peça vai (camadas)

- **`etc/api/organization/ScosOrganization_Employee.yml`**: bloco `/v1/employees/rehire:` perde `get`/`put`; novo bloco `/v1/employees/{id}:` ganha os dois (Task 1).
- **`domain/corporate/employee/internal/EmployeeQueryRepository.java`** (já existe — 1 método novo: `findAllFiltered`).
- **`domain/corporate/employee/specification/EmployeeService.java`** (já existe — 1 assinatura nova: `findAll`; `findById` já existe).
- **`domain/corporate/employee/service/EmployeeServiceBean.java`** (já existe — 1 método novo: `findAll`).
- **`usecase/corporate/employee/`** (já existe, pacote da Story 2.1): `FindEmployeeUseCase(Bean)` + `FindAllEmployeeUseCase(Bean)` novos; `EmployeeApiMapper` (Story 2.3) ganha 3 métodos novos.
- **`api/delegate/employee/EmployeeDelegate.java`** (já existe — 2 `@Override` novos).
- Nenhuma mudança em `ScosOrganizationPermission.java`, Liquibase, ou `ExceptionCodeError` (nenhum código de erro novo — `SCOS_EMPLOYEE_014`, 404 not-found, já existe desde a Story 2.2).

### Testing Standards

- Unitário de domínio: `@ExtendWith(MockitoExtension.class)`, mesmo padrão de `EmployeeServiceBeanTest` (Stories 2.1/2.2/2.3) — bloco novo `findAll`, sem precisar de `Clock` fixo (leitura não usa `Clock`).
- Unitário de Use Case: mesmo padrão BDD de `RehireEmployeeUseCaseBeanTest`/`FindAllCompanyUseCaseBeanTest` (se existir) — `given`/`then`/`ArgumentCaptor`.
- Integração: adicionar aos blocos já existentes de `EmployeeControllerTest.java` (`extends ScosOrganizationTestUtil`, containers `static`, **nunca** `@Testcontainers`/`@Container`). Rotas `GET` não têm `x-jdempotentrequestpayload` (idempotência é só para escrita) — não precisa de CPF único por chamada de leitura, só para os `POST /v1/employees` que o teste usa para montar cenário.

### Project Structure Notes

- Nenhum pacote Maven novo — tudo cai em pacotes já criados pela Story 2.1 (`usecase/corporate/employee/`, `api/delegate/employee/`) ou já existentes desde antes (`domain/corporate/employee/...`).
- Única mudança de contrato desta story inteira: mover 2 operações de path item (Task 1) — nenhum schema novo, nenhuma permissão nova, nenhuma mudança de Liquibase.

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Epic 2 / Story 2.5] — Given/When/Then originais (story criada nesta sessão — não existia no épico antes; gap identificado pelo usuário, confirmado por leitura: Epic 2 só ia até a Story 2.4).
- [Source: etc/api/organization/ScosOrganization_Employee.yml:27-51] — bloco `/v1/employees:` (`get`/`getAllEmployees`, já no path certo, com `companyIdFilter`/`positionIdFilter`/`statusFilter`).
- [Source: etc/api/organization/ScosOrganization_Employee.yml:79-161] — bloco `/v1/employees/rehire:` atual, com `post`/`get`/`put` incorretamente no mesmo path item (defeito corrigido pela Task 1).
- [Source: etc/api/organization/ScosOrganization_Employee.yml:777-796] — parâmetros `companyIdFilter`/`positionIdFilter`/`statusFilter`, todos `required: false`.
- [Source: etc/api/organization/ScosOrganization_Employee.yml:804-911] — schemas `EmployeeStatus`, `Employees` (resumido), `Employee` (completo), `GetEmployeeResponse`, `GetAllEmployeesResponse`.
- [Source: etc/api/organization/ScosComponents.yml:52-58] — `idRequest` (`name: id, in: path`), parâmetro compartilhado usado por `getEmployeeById`.
- [Source: organization/flow-organization-infrastructure/.../enumaration/ScosOrganizationPermission.java] — `GET_EMPLOYEE` já cadastrada (Story 2.1), reaproveitada sem mudança.
- [Source: organization/flow-organization-domain/.../catalog/internal/ContactTypeRepository.java:64-79] — padrão exato de `findAllFiltered` com `Objects.isNull`/`BooleanBuilder` a replicar (2 filtros lá, 3 aqui).
- [Source: organization/flow-organization-domain/.../catalog/specification/ContactTypeService.java:39] — assinatura de referência com filtros opcionais (não `@NonNull`).
- [Source: organization/flow-organization-domain/.../catalog/service/ContactTypeServiceBean.java:116-123] — implementação de referência do `findAll` delegando ao repositório.
- [Source: organization/flow-organization-domain/.../position/specification/PositionService.java:28] — contraexemplo: filtros **obrigatórios** (`@NonNull`), padrão diferente do usado nesta story (ver Dev Notes).
- [Source: organization/flow-organization-usecase/.../corporate/company/CompanyApiMapper.java:51-61,75-82] — padrão exato de `toApiCompanies`/`toApiStatus`/`toDomainStatus` replicado em `EmployeeApiMapper` (Task 5).
- [Source: organization/flow-organization-usecase/.../corporate/company/FindAllCompanyUseCaseBean.java] — padrão exato de `FindAllEmployeeUseCaseBean` (paginação + filtros + mapper resumido).
- [Source: organization/flow-organization-usecase/.../corporate/position/FindPositionUseCaseBean.java] — padrão exato de `FindEmployeeUseCaseBean` (busca por id + mapper).
- [Source: organization/flow-organization-usecase/.../utils/PaginatioUtils.java] — `createPageable`/`createScosPaginated`, já existente, reaproveitado sem mudança.
- [Source: organization/flow-organization-api/.../delegate/company/CompanyDelegate.java:73-82] — padrão exato dos 2 métodos de Delegate (`getAllCompanies`/`getCompanyById`).
- [Source: organization/flow-organization-api/target/generated-sources/openapi/.../controller/EmployeeApiDelegate.java:96-136] — assinaturas geradas atuais de `getAllEmployees`/`getEmployeeById` (path errado confirmado no Javadoc; parâmetros Java não mudam após a Task 1, só o path).
- [Source: _bmad-output/implementation-artifacts/2-3-recontratacao-funcionario.md] — origem de `EmployeeService.findById`, `EmployeeApiMapper.toApiEmployee`, e do padrão de resolução de defeito de contrato a favor do código real.
- [Source: _bmad-output/implementation-artifacts/2-1-admissao-funcionario-copia-jornada-trabalho.md] — origem de `EmployeeService`/`EmployeeServiceBean`/`EmployeeDelegate`/`EmployeeControllerTest`, e do primeiro sinal do defeito de contrato (Task 10 daquela story).

## Dev Agent Record

### Agent Model Used

Claude Sonnet 5 (claude-sonnet-5), via Claude Code, workflow `bmad-dev-story`.

### Debug Log References

- Mesmo ambiente sem acesso ao Docker documentado na Story 2.4 — os testes de integração novos em `EmployeeControllerTest` (`GET /v1/employees/{id}`, `GET /v1/employees`) foram **escritos mas não executados**. Unitário (`domain`) e Use Case (`usecase`) foram executados e passam. Rodar `mvn -pl organization/flow-organization-boot test -Denforcer.skip=true` localmente (com Docker acessível) antes de aprovar a review.
- Mesmo contorno de `maven-surefire-plugin` (pinado em `2.17` neste ambiente, incompatível com JUnit 5) usado para validar `domain`/`usecase`: `mvn org.apache.maven.plugins:maven-surefire-plugin:3.5.4:test -Denforcer.skip=true ...`.
- Task 1 confirmada por leitura do Javadoc gerado (`mvn generate-sources`): `EmployeeApiDelegate.getEmployeeById` passou de `"GET /v1/employees/rehire : Get employee by id"` para `"GET /v1/employees/{id} : Get employee by id"`.
- CPFs/CNPJs novos usados nos testes de integração foram gerados com dígito verificador calculado (algoritmo padrão CPF/CNPJ), não copiados de exemplo — necessário porque `Cpf`/validação de `taxIdentifier` rejeita DV inválido.

### Completion Notes List

- Todas as 6 ACs implementadas: defeito de contrato corrigido (`/v1/employees/{id}` com `get`/`put`, Task 1); `EmployeeQueryRepository.findAllFiltered` com os 3 filtros opcionais combináveis via QueryDSL (Task 2); `EmployeeService.findAll`/`EmployeeServiceBean.findAll` (Tasks 3/4); `EmployeeApiMapper.toApiEmployees`/`toApiStatus`/`toDomainStatus` (Task 5); `FindEmployeeUseCase(Bean)`/`FindAllEmployeeUseCase(Bean)` novos, reaproveitando `EmployeeService.findById` e `EmployeeApiMapper.toApiEmployee` (Task 6); `EmployeeDelegate.getAllEmployees`/`getEmployeeById` implementados (Task 7).
- `updateEmployee` permanece sem `@Override` no Delegate (Task 9, guarda de escopo) — só o path foi corrigido. Nenhuma rota de hierarquia/subordinados/histórico/contact/address/work-schedule foi tocada. Nenhuma permissão nova criada — `GET_EMPLOYEE` reaproveitada (Story 2.1).
- Testes unitários (`domain`): suíte completa (286 testes) passa, incluindo os 5 cenários novos de `findAll` (sem filtro, companyId, positionId, status, combinado).
- Testes de Use Case (`usecase`): suíte completa (217 testes) passa, incluindo os 6 cenários novos (`FindEmployeeUseCaseBeanTest` × 3, `FindAllEmployeeUseCaseBeanTest` × 3).
- Testes de integração (`boot`, `EmployeeControllerTest`): 11 cenários novos escritos (`GET /v1/employees/{id}` × 5, `GET /v1/employees` × 6) seguindo o padrão existente — **não executados nesta sessão** (ver Debug Log). Recomendo rodar antes de mover para `done`.

### File List

- `etc/api/organization/ScosOrganization_Employee.yml`
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/corporate/employee/internal/EmployeeQueryRepository.java`
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/corporate/employee/specification/EmployeeService.java`
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/corporate/employee/service/EmployeeServiceBean.java`
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/corporate/employee/EmployeeApiMapper.java`
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/corporate/employee/FindEmployeeUseCase.java` (novo)
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/corporate/employee/FindEmployeeUseCaseBean.java` (novo)
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/corporate/employee/FindAllEmployeeUseCase.java` (novo)
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/corporate/employee/FindAllEmployeeUseCaseBean.java` (novo)
- `organization/flow-organization-api/src/main/java/br/com/sawcunhaos/organization/api/delegate/employee/EmployeeDelegate.java`
- `organization/flow-organization-domain/src/test/java/br/com/sawcunhaos/organization/domain/corporate/employee/service/EmployeeServiceBeanTest.java`
- `organization/flow-organization-usecase/src/test/java/br/com/sawcunhaos/organization/application/usecase/corporate/employee/FindEmployeeUseCaseBeanTest.java` (novo)
- `organization/flow-organization-usecase/src/test/java/br/com/sawcunhaos/organization/application/usecase/corporate/employee/FindAllEmployeeUseCaseBeanTest.java` (novo)
- `organization/flow-organization-boot/src/test/java/br/com/sawcunhaos/organization/boot/api/employee/EmployeeControllerTest.java`

## Change Log

| Data | Descrição |
|------|-----------|
| 2026-08-15 | Story implementada: corrigido defeito de contrato (`getEmployeeById`/`updateEmployee` movidos para `/v1/employees/{id}`), `GET /v1/employees/{id}` e `GET /v1/employees` (paginação + filtros companyId/positionId/status) implementados via `FindEmployeeUseCase`/`FindAllEmployeeUseCase` novos. 5 testes unitários de domínio novos, 6 de Use Case, 11 de integração (não executados — ambiente sem Docker). Status → review. |
