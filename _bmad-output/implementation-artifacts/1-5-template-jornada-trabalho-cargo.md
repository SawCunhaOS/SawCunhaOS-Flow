---
baseline_commit: 4fe2d3799dd9ff2bc015c3a24ea66422d1fef1f9
---

# Story 1.5: Template de Jornada de Trabalho por Cargo

Status: review

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como Analista de RH,
Eu quero definir um template de Jornada de Trabalho por Cargo, com horário por dia da semana,
Para que todo Funcionário admitido nesse Cargo já nasça com uma jornada padrão.

## Acceptance Criteria

1. **Given** um Cargo existente **When** RH cadastra (`POST /v1/positions/{positionId}/work-schedule`) um horário para um dia da semana com `startTime < lunchStart < lunchEnd < endTime` **Then** o template é salvo (`201`) — nova linha em `SCOS_POSITION_WORK_SCHEDULE`, sem histórico (é configuração direta, não transição de status).
2. **Given** um payload de `create` **ou** `update` com horários fora de ordem (ex.: `lunchStart` depois de `lunchEnd`) **When** RH tenta salvar **Then** o sistema rejeita com `422` e um código novo (`SCOS_POSITION_WORK_SCHEDULE_003` — ver Dev Notes sobre numeração) — validado no Use Case/Service, **nunca** via `CHECK` no banco (não existe, confirmado no changelog) nem via anotação de Bean Validation cross-field (não há precedente disso no projeto).
3. **Given** um `positionId` que não existe **When** `GET`/`POST /v1/positions/{positionId}/work-schedule` é chamado **Then** o sistema rejeita com `404 SCOS_POSITION_001` — reaproveitado de `PositionService.findPositionById`, **sem** criar código novo para isso.
4. **Given** já existe um registro para o par `(positionId, dayOfWeek)` **When** RH tenta `POST` outro para o mesmo par **Then** o sistema rejeita com `409` e um código novo (`SCOS_POSITION_WORK_SCHEDULE_002`).
5. **Given** não existe registro para o par `(positionId, dayOfWeek)` **When** RH tenta `PUT`/`DELETE /v1/positions/{positionId}/work-schedule/{dayOfWeek}` **Then** o sistema rejeita com `404` e um código novo (`SCOS_POSITION_WORK_SCHEDULE_001`) — cobre tanto `positionId` inválido quanto dia não cadastrado, é uma checagem só (ver Dev Notes).
6. **Given** o contrato OpenAPI dos 4 endpoints (`getAllPositionWorkSchedule`/`createPositionWorkSchedule`/`updatePositionWorkSchedule`/`deletePositionWorkSchedule`), o enum `DayOfWeek` compartilhado, as 4 permissões (`GET`/`CREATE`/`UPDATE`/`DELETE_POSITION_WORK_SCHEDULE`) e o schema Liquibase de `SCOS_POSITION_WORK_SCHEDULE` **já existirem publicados hoje** **When** esta story é implementada **Then** cria **somente** as camadas `domain`/`usecase`/`api` (Use Case + Delegate do zero, como o épico pede) — **nenhuma** mudança em `etc/api/organization/*.yml`, `ScosGeotemporalPermission`, ou `flow-organization-resources` (Liquibase).
7. **Given** o CRUD de `EmployeeWorkSchedule` (mesmo padrão, sob `/v1/employees/{employeeId}/work-schedule`, contrato **também já publicado** em `ScosOrganization_Employee.yml`) não pertencer a nenhuma story do sprint atual **e** a cópia do template para o Funcionário na admissão pertencer à Story 2.1 **When** esta story é implementada **Then** nenhum dos dois é tocado — só `PositionWorkSchedule`.

## Tasks / Subtasks

- [x] Task 1: DTOs de domínio (AC: 1, 2)
  - [x] Criar `flow-organization-domain/.../corporate/position/dto/PositionWorkScheduleInput.java`:
    ```java
    @Builder
    public record PositionWorkScheduleInput(
            Long positionId,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime lunchStart,
            LocalTime lunchEnd,
            LocalTime endTime
    ) {
    }
    ```
    (`DayOfWeek` = `br.com.sawcunhaos.organization.domain.corporate.position.internal.DayOfWeek` — enum já existente, mesmo usado por `EmployeeWorkSchedule`; **não** criar um novo.)
  - [x] Criar `.../dto/PositionWorkScheduleOutput.java`:
    ```java
    @Builder
    public record PositionWorkScheduleOutput(
            Long id,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime lunchStart,
            LocalTime lunchEnd,
            LocalTime endTime
    ) {
    }
    ```

- [x] Task 2: Repositório — 3 métodos novos (AC: 3, 4, 5)
  - [x] Em `PositionWorkScheduleRepository.java` (já existe, hoje sem métodos além do CRUD base), adicionar:
    ```java
    QPositionWorkSchedule positionWorkSchedule = QPositionWorkSchedule.positionWorkSchedule;

    Optional<PositionWorkSchedule> findByPositionIdAndDayOfWeek(Long positionId, DayOfWeek dayOfWeek);

    boolean existsByPositionIdAndDayOfWeek(Long positionId, DayOfWeek dayOfWeek);

    default List<PositionWorkSchedule> findAllByPositionId(Long positionId) {
        Iterable<PositionWorkSchedule> found = findAll(
                positionWorkSchedule.position.id.eq(positionId),
                positionWorkSchedule.dayOfWeek.asc()
        );
        return StreamSupport.stream(found.spliterator(), false).toList();
    }
    ```
    `findByPositionIdAndDayOfWeek`/`existsByPositionIdAndDayOfWeek` são *derived query methods* (mesmo padrão de `existsByParentCompanyId` em `CompanyRepository`) — Spring Data gera a implementação, não escrever corpo. `findAllByPositionId` usa `QuerydslPredicateExecutor.findAll(Predicate, OrderSpecifier<?>...)`, que retorna `Iterable<T>` (confirmado via a própria interface do Spring Data Commons) — por isso o `StreamSupport`/`toList()`, não faça cast direto pra `List`. **Não precisa de `@Query` nativo/CTE aqui** — é uma lista plana por FK, sem hierarquia nem recursão (diferente da Story 1.1/1.3, que lidam com árvore de Company). Ordenação por `dayOfWeek.asc()` é alfabética (`FRIDAY < MONDAY < ...`, ordem da coluna `VARCHAR`), não cronológica da semana — nenhum AC/doc exige ordem cronológica, não construa um `CASE WHEN` pra isso.

- [x] Task 3: Códigos de erro novos — módulo próprio, não reaproveitar `SCOS_POSITION_0XX` (AC: 2, 4, 5)
  - [x] `ExceptionCodeError.java`: após o bloco `// Position` (linha ~68, depois de `SCOS_POSITION_005`), adicionar um bloco `// Position Work Schedule` com 3 constantes novas:
    ```java
    // Position Work Schedule
    /** Não existe horário cadastrado para este cargo neste dia da semana. HTTP 404. */
    SCOS_POSITION_WORK_SCHEDULE_001("SCOS_POSITION_WORK_SCHEDULE_001", 404, "SCOS_TITLE_NOT_FOUND"),
    /** Já existe um horário cadastrado para este cargo neste dia da semana. HTTP 409. */
    SCOS_POSITION_WORK_SCHEDULE_002("SCOS_POSITION_WORK_SCHEDULE_002", 409, "SCOS_TITLE_CONFLICT"),
    /** Ordem cronológica inválida: startTime < lunchStart < lunchEnd < endTime. HTTP 422. */
    SCOS_POSITION_WORK_SCHEDULE_003("SCOS_POSITION_WORK_SCHEDULE_003", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    ```
    **Por que módulo próprio e não `SCOS_POSITION_006`/`007`/`008`:** o padrão já em vigor no enum reserva prefixo dedicado pra cada sub-recurso de um agregado (`ADDRESS_TYPE`, `CONTACT_TYPE`, `CNAE`, `LEGAL_NATURE` são todos sub-recursos com numeração própria, nunca emendados no prefixo do "dono"). `PositionWorkSchedule` é sub-recurso de `Position` da mesma forma — segue o mesmo padrão, mesma convenção de nomenclatura já usada nas 4 permissões (`GET/CREATE/UPDATE/DELETE_POSITION_WORK_SCHEDULE`, já cadastradas com esse nome composto).
  - [x] `scos_message_organization.properties` (após a última linha de `SCOS_POSITION_0XX`):
    ```properties
    SCOS_POSITION_WORK_SCHEDULE_001=Não existe horário cadastrado para este cargo neste dia da semana.
    SCOS_POSITION_WORK_SCHEDULE_002=Já existe um horário cadastrado para este cargo neste dia da semana.
    SCOS_POSITION_WORK_SCHEDULE_003=A ordem dos horários é inválida: o início deve ser anterior ao início do almoço, que deve ser anterior ao fim do almoço, que deve ser anterior ao fim do expediente.
    ```
  - [x] `scos_message_organization_en.properties` (mesmas 3 chaves, texto em inglês, mesmo padrão dos outros pares PT/EN já existentes).

- [x] Task 4: `PositionWorkScheduleService` (specification) + `PositionWorkScheduleServiceBean` (AC: 1, 2, 3, 4, 5)
  - [x] Criar `.../position/specification/PositionWorkScheduleService.java`:
    ```java
    public interface PositionWorkScheduleService {
        List<PositionWorkScheduleOutput> findAllByPositionId(@NonNull Long positionId);
        PositionWorkScheduleOutput create(@NonNull Long positionId, @NonNull PositionWorkScheduleInput input);
        void update(@NonNull Long positionId, @NonNull DayOfWeek dayOfWeek, @NonNull PositionWorkScheduleInput input);
        void delete(@NonNull Long positionId, @NonNull DayOfWeek dayOfWeek);
    }
    ```
  - [x] Criar `.../position/service/PositionWorkScheduleMapper.java` (MapStruct, mesmo padrão de `PositionMapper`):
    ```java
    @Mapper(componentModel = "spring")
    public interface PositionWorkScheduleMapper {
        PositionWorkScheduleOutput toOutput(PositionWorkSchedule entity);
    }
    ```
  - [x] Criar `.../position/service/PositionWorkScheduleServiceBean.java`, injetando `PositionWorkScheduleRepository`, `PositionService` (para `findPositionById` — reaproveita a checagem 404 de existência do Cargo, **não** duplicar), `PositionWorkScheduleMapper`, `ScosUserAuthentication`:
    ```java
    @Override
    @Transactional(readOnly = true)
    public List<PositionWorkScheduleOutput> findAllByPositionId(@NonNull Long positionId) {
        positionService.findPositionById(positionId); // 404 SCOS_POSITION_001 se não existir
        return positionWorkScheduleRepository.findAllByPositionId(positionId).stream()
                .map(positionWorkScheduleMapper::toOutput)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = ScosException.class)
    public PositionWorkScheduleOutput create(@NonNull Long positionId, @NonNull PositionWorkScheduleInput input) {
        assertChronologicalOrder(input);
        Position position = positionService.findPositionById(positionId); // 404 SCOS_POSITION_001
        if (positionWorkScheduleRepository.existsByPositionIdAndDayOfWeek(positionId, input.dayOfWeek())) {
            throw new ScosException(SCOS_POSITION_WORK_SCHEDULE_002);
        }

        PositionWorkSchedule schedule = PositionWorkSchedule.builder()
                .position(position)
                .dayOfWeek(input.dayOfWeek())
                .startTime(input.startTime())
                .lunchStart(input.lunchStart())
                .lunchEnd(input.lunchEnd())
                .endTime(input.endTime())
                .build();
        schedule.updateAuditInfo(scosUserAuthentication.findUserAuthentication());

        return positionWorkScheduleMapper.toOutput(positionWorkScheduleRepository.merge(schedule));
    }

    @Override
    @Transactional(rollbackFor = ScosException.class)
    public void update(@NonNull Long positionId, @NonNull DayOfWeek dayOfWeek, @NonNull PositionWorkScheduleInput input) {
        assertChronologicalOrder(input);
        PositionWorkSchedule schedule = findScheduleOrThrow(positionId, dayOfWeek);

        schedule.setStartTime(input.startTime());
        schedule.setLunchStart(input.lunchStart());
        schedule.setLunchEnd(input.lunchEnd());
        schedule.setEndTime(input.endTime());
        schedule.updateAuditInfo(scosUserAuthentication.findUserAuthentication());

        positionWorkScheduleRepository.update(schedule);
    }

    @Override
    @Transactional(rollbackFor = ScosException.class)
    public void delete(@NonNull Long positionId, @NonNull DayOfWeek dayOfWeek) {
        positionWorkScheduleRepository.delete(findScheduleOrThrow(positionId, dayOfWeek));
    }

    private PositionWorkSchedule findScheduleOrThrow(Long positionId, DayOfWeek dayOfWeek) {
        return positionWorkScheduleRepository.findByPositionIdAndDayOfWeek(positionId, dayOfWeek)
                .orElseThrow(() -> new ScosException(SCOS_POSITION_WORK_SCHEDULE_001));
    }

    private void assertChronologicalOrder(PositionWorkScheduleInput input) {
        boolean inOrder = input.startTime().isBefore(input.lunchStart())
                && input.lunchStart().isBefore(input.lunchEnd())
                && input.lunchEnd().isBefore(input.endTime());
        if (!inOrder) {
            throw new ScosException(SCOS_POSITION_WORK_SCHEDULE_003);
        }
    }
    ```
    **Ordem das guardas — decisão deliberada, não é o padrão "unicidade → FK → regra" do resto do projeto:** `etc/doc/usecase/02-departamento-cargo.md` (UC-145) documenta explicitamente "Regras em ordem: 1. Todos presentes. 2. Ordem cronológica. 5. `positionId` deve existir. 7. Não pode existir `(positionId, dayOfWeek)`" — ou seja, ordem cronológica (422, checagem pura de payload, sem banco) roda **antes** da existência do Cargo (404) e da unicidade (409), invertendo a ordem "409 → 404 → 422" usada em `CompanyServiceBean`/`PositionServiceBean.create`. É intencional: falha rápido na checagem mais barata (sem round-trip de banco) antes das que dependem de consulta. Se preferir seguir o padrão geral do projeto em vez do documento de use case, é uma decisão de trade-off — não uma correção de bug; qualquer uma das duas ordens passa nos ACs (nenhum AC testa qual dos dois erros "ganha" quando ambos aconteceriam ao mesmo tempo). Para `update`, mesma ordem (cronológica antes da existência do par).
    `findScheduleOrThrow` cobre **as duas** causas de 404 do `update`/`delete` (`positionId` inválido e `dayOfWeek` não cadastrado) com uma única consulta — não faça duas checagens separadas (uma pra `positionId`, outra pro par), o UC-146/147 trata como um caso só.

- [x] Task 5: Use Cases + extensão do `PositionApiMapper` (AC: 1, 3, 4, 5, 6)
  - [x] Em `flow-organization-usecase/.../usecase/corporate/position/`, criar 4 pares interface+Bean, mesmo padrão de `CreatePositionUseCase(Bean)`:
    ```java
    public interface GetAllPositionWorkScheduleUseCase {
        List<PositionWorkSchedule> execute(@NonNull Long positionId); // PositionWorkSchedule = api.dto, não domain.internal
    }
    ```
    ```java
    @Service @RequiredArgsConstructor @Slf4j @Transactional(readOnly = true)
    class GetAllPositionWorkScheduleUseCaseBean implements GetAllPositionWorkScheduleUseCase {
        private final PositionWorkScheduleService positionWorkScheduleService;
        @Override
        public List<PositionWorkSchedule> execute(@NonNull Long positionId) {
            return positionWorkScheduleService.findAllByPositionId(positionId).stream()
                    .map(PositionApiMapper::toApiPositionWorkSchedule)
                    .toList();
        }
    }
    ```
    ```java
    public interface CreatePositionWorkScheduleUseCase {
        Long execute(@NonNull Long positionId, @NonNull CreatePositionWorkScheduleRequest request);
    }
    ```
    ```java
    @Service @RequiredArgsConstructor @Slf4j @Transactional(rollbackFor = ScosException.class)
    class CreatePositionWorkScheduleUseCaseBean implements CreatePositionWorkScheduleUseCase {
        private final PositionWorkScheduleService positionWorkScheduleService;
        @Override
        public Long execute(@NonNull Long positionId, @NonNull CreatePositionWorkScheduleRequest request) {
            PositionWorkScheduleInput input = PositionWorkScheduleInput.builder()
                    .dayOfWeek(DayOfWeek.valueOf(request.dayOfWeek().name()))
                    .startTime(request.startTime())
                    .lunchStart(request.lunchStart())
                    .lunchEnd(request.lunchEnd())
                    .endTime(request.endTime())
                    .build();
            return positionWorkScheduleService.create(positionId, input).id();
        }
    }
    ```
    Repetir para `UpdatePositionWorkScheduleUseCase(Bean)` (`execute(Long positionId, DayOfWeek dayOfWeek, UpdatePositionWorkScheduleRequest request)`, sem `dayOfWeek` no `PositionWorkScheduleInput` vindo do request — vem do path — e retorno `void`) e `DeletePositionWorkScheduleUseCase(Bean)` (`execute(Long positionId, DayOfWeek dayOfWeek)`, `void`).
    **`CreatePositionWorkScheduleUseCase` retorna `Long`, não o objeto `PositionWorkSchedule` mapeado (diferente de `CreatePositionUseCase`, que retorna `Position` completo):** o schema `PositionWorkSchedule` (YAML) **não tem campo `id`** — só `dayOfWeek`/`startTime`/`lunchStart`/`lunchEnd`/`endTime` (a chave natural do sub-recurso é `dayOfWeek`, não um id sequencial exposto). O `201` usa `CreateResponse`/`Create` (schema genérico, `data.id` obrigatório) — não dá pra montar isso a partir de um `PositionWorkSchedule` mapeado (não tem de onde tirar o id). Por isso o Use Case de `create` devolve só o `Long` do `POSITION_WORK_SCHEDULE_ID` gerado, e o Delegate monta o `CreateResponse` direto com ele — **não** tente reaproveitar `PositionApiMapper.toApiPositionWorkSchedule` no fluxo de criação, ele serve só pro `GET`.
  - [x] Estender `PositionApiMapper.java` (já existe, hoje só mapeia `Position`/`Department`) com:
    ```java
    static br.com.sawcunhaos.organization.api.dto.PositionWorkSchedule toApiPositionWorkSchedule(PositionWorkScheduleOutput output) {
        return br.com.sawcunhaos.organization.api.dto.PositionWorkSchedule.builder()
                .dayOfWeek(br.com.sawcunhaos.organization.api.dto.DayOfWeek.valueOf(output.dayOfWeek().name()))
                .startTime(output.startTime())
                .lunchStart(output.lunchStart())
                .lunchEnd(output.lunchEnd())
                .endTime(output.endTime())
                .build();
    }
    ```
    **Conflito de nome deliberado:** existe um `PositionWorkSchedule` de domínio (`domain.corporate.position.internal`, a entidade JPA) e um `PositionWorkSchedule` gerado da API (`api.dto`, o schema do YAML) — mesmo nome, pacotes diferentes. É o mesmo problema que já existe hoje entre `Position` (entidade) e `Position` (api dto), resolvido no `PositionApiMapper` atual **nunca importando os dois no mesmo arquivo** — só importa o `api.dto.Position` e usa `PositionOutput` (nome diferente) pro lado domain. Siga o mesmo princípio aqui: o método usa FQN pro tipo `api.dto` (como no snippet acima) ou importa só um dos dois e qualifica o outro — nunca os dois via `import` simples (erro de compilação por ambiguidade).
  - [x] `DayOfWeek.valueOf(apiEnum.name())` funciona porque os dois enums (`api.dto.DayOfWeek`, gerado do schema `ScosComponents.yml#DayOfWeek`, e `domain.corporate.position.internal.DayOfWeek`) têm exatamente os mesmos 7 nomes de constante (`MONDAY`..`SUNDAY`) — confirmado nos dois arquivos-fonte. Se preferir, um `Mapper` MapStruct mapeia enums de mesmo nome automaticamente sem código explícito — qualquer uma das duas abordagens é aceitável.

- [x] Task 6: `PositionDelegate` — 4 `@Override` novos (AC: 1, 3, 4, 5, 6)
  - [x] Injetar os 4 Use Cases novos em `PositionDelegate.java` (já existe) e sobrescrever os métodos que hoje caem no `default` gerado (`MethodNotImplementedException`):
    ```java
    @Override
    public GetAllPositionWorkScheduleResponse getAllPositionWorkSchedule(Long positionId, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        return getAllPositionWorkScheduleUseCase.execute(positionId); // confirmar assinatura exata pós generate-sources (ver Dev Notes)
    }

    @Override
    public CreateResponse createPositionWorkSchedule(Long positionId, CreatePositionWorkScheduleRequest createPositionWorkScheduleRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        return CreateResponse.builder()
                .data(Create.builder().id(createPositionWorkScheduleUseCase.execute(positionId, createPositionWorkScheduleRequest)).build())
                .build();
    }

    @Override
    public Void updatePositionWorkSchedule(Long positionId, DayOfWeek dayOfWeek, UpdatePositionWorkScheduleRequest updatePositionWorkScheduleRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        updatePositionWorkScheduleUseCase.execute(positionId, dayOfWeek, updatePositionWorkScheduleRequest);
        return null;
    }

    @Override
    public Void deletePositionWorkSchedule(Long positionId, DayOfWeek dayOfWeek, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        deletePositionWorkScheduleUseCase.execute(positionId, dayOfWeek);
        return null;
    }
    ```
  - [x] **Rodar `mvn generate-sources` (ou build completo) em `flow-organization-api` antes de escrever o Delegate** — os métodos `*PositionWorkSchedule` ainda não existem em `PositionApiDelegate` no código atual (nunca foram gerados, mesmo o YAML já publicado há tempo). Conferir a assinatura exata gerada pro `getAllPositionWorkSchedule` — schema `GetAllPositionWorkScheduleResponse` é `type: array` no YAML, então o generator tende a não criar uma classe wrapper e sim tipar o retorno como `List<PositionWorkSchedule>` direto (sem classe `GetAllPositionWorkScheduleResponse`); ajustar a assinatura do método/Use Case ao que for gerado de fato, não ao nome do schema.

- [x] Task 7: Testes (AC: 1, 2, 3, 4, 5)
  - [x] `PositionWorkScheduleServiceBeanTest.java` (novo, `flow-organization-domain`, mesmo padrão JUnit5+Mockito de `PositionServiceBeanTest`): caminho feliz de `create`/`update`/`delete`/`findAllByPositionId`; ordem cronológica inválida em `create` e em `update` → `SCOS_POSITION_WORK_SCHEDULE_003`; `positionId` inexistente em `create`/`findAllByPositionId` → `SCOS_POSITION_001` (mock de `positionService.findPositionById` lançando); par duplicado em `create` → `SCOS_POSITION_WORK_SCHEDULE_002`; par inexistente em `update`/`delete` → `SCOS_POSITION_WORK_SCHEDULE_001`.
  - [x] 4 arquivos de teste de Use Case (`flow-organization-usecase/src/test/.../corporate/position/`), mesmo padrão de `CreatePositionUseCaseBeanTest` (BDD `given`/`then`, `ArgumentCaptor`, teste de propagação de `ScosException`, teste de `@NonNull`/`NullPointerException`).
  - [x] `PositionControllerTest.java` (`flow-organization-boot`, já existe) — acrescentar seção `PUT/DELETE/POST/GET .../work-schedule`: caminho feliz completo (`POST` cria, `GET` lista e mostra o item, `PUT` atualiza, `DELETE` remove e `GET` some da lista); `422` ordem inválida; `409` par duplicado; `404` `positionId` inexistente (`GET`/`POST`); `404` par inexistente (`PUT`/`DELETE`); sem token `401`; sem permissão `403` (uma por rota, mesmo padrão já usado no arquivo). Seed atual (`SEEDED_ID=1`, sem nenhum horário cadastrado) já serve de base — não precisa de fixture nova além do que os próprios testes criam via `POST`.

- [x] Task 8: Guarda de escopo (AC: 6, 7)
  - [x] **Não** alterar `etc/api/organization/ScosOrganization_Department-Position.yml` nem `ScosComponents.yml` — os 4 endpoints, os 3 schemas (`PositionWorkSchedule`, `Create`/`UpdatePositionWorkScheduleRequest`) e o enum `DayOfWeek` já estão publicados e corretos.
  - [x] **Não** alterar `ScosOrganizationPermission.java` — as 4 permissões já existem (linhas 39-42).
  - [x] **Não** alterar Liquibase (`flow-organization-resources`) — tabela, FK, UK composta e `CHECK` de `dayOfWeek` já existem.
  - [x] **Não** implementar nada de `EmployeeWorkSchedule`/`ScosOrganization_Employee.yml` — mesmo padrão, contrato também já publicado, mas fora de qualquer story do sprint atual (não é scope creep desta story resolver esse gap do backlog).
  - [x] Opcional, não bloqueante: `etc/database/seed_data.sql` não tem as 4 linhas de `SCOS_RESOURCE` pra `GET/CREATE/UPDATE/DELETE_POSITION_WORK_SCHEDULE` (só existem no enum Java) — confirmado que isso **não afeta nenhum teste** (a autorização nos testes de integração vem de `Arrays.stream(ScosOrganizationPermission.values())` via WireMock, não do seed do banco — ver `ScosOrganizationWiremockUtil.fullAdminAuthority()`). Se quiser fechar esse gap de catálogo por completude, adicionar as 4 linhas seguindo o padrão exato de `GET_POSITION`/`CREATE_POSITION`/etc. (linha ~302-307) — mas isso não é requisito de nenhum AC, não é bloqueante.

## Dev Notes

### Contexto crítico — leia antes de implementar

**Isto não é greenfield puro — o contrato já existe integralmente, só falta a implementação.** `PositionWorkSchedule` (entidade JPA) e `PositionWorkScheduleRepository` (vazio, só CRUD base) já existem desde antes desta story. O YAML (`ScosOrganization_Department-Position.yml:439-534`) já publica os 4 endpoints, os schemas de request/response e o `x-authorize`; `ScosOrganizationPermission.java:39-42` já cadastra as 4 permissões (datadas de 2026-07-15); o Liquibase já criou a tabela com FK, `CHECK` de `dayOfWeek` e UK composta `(POSITION_ID, DAY_OF_WEEK)`. **Nada disso foi decidido nesta story** — vem de uma rodada de design anterior (`etc/doc/ideia/20260701_jornada-trabalho-position-employee.md`, `etc/doc/usecase/02-departamento-cargo.md` UC-144..147). Esta story só cria as camadas `domain`/`usecase`/`api` que ainda não existem.

**A UK composta `(POSITION_ID, DAY_OF_WEEK)` já garante o teto de 7 registros por Cargo** — é por isso que `GET .../work-schedule` retorna array direto, sem paginação (`GetAllPositionWorkScheduleResponse: type: array`, exceção deliberada ao padrão do resto do contrato, documentada no YAML). Não adicione paginação a essa rota achando que "toda listagem pagina" — seria inconsistente com o contrato já publicado.

**`SCOS_VALIDATION_007` (formato de horário inválido, ex. `"25:99"`) já existe no bundle de validação e é tratado de forma genérica pelo mecanismo global de erro de binding** (mesma família de `SCOS_VALIDATION_005`, usado para email) — **não crie um throw site pra isso** no Use Case/Service; é a primeira vez que o projeto usa `format: time` num campo gerado, então não há precedente local de outro endpoint pra conferir, mas o padrão (`x-required-message`/parse automático LocalTime pelo Spring/Jackson) é o mesmo já usado pros outros formatos. Se, ao implementar, o parse malformado não cair automaticamente em `SCOS_VALIDATION_007`, é uma configuração de infraestrutura de mensagem (fora do escopo de código de domínio desta story) — reportar, não tentar contornar com validação manual duplicada.

**Chronological check roda sem tocar banco — é por isso que a ordem de guardas foge do padrão "409 → 404 → 422" do resto do projeto.** Ver justificativa completa na Task 4. Isto é uma leitura deliberada do `etc/doc/usecase/02-departamento-cargo.md` (documento de design pré-épico, não ratificado pela arquitetura spine com um AD dedicado — FR-4 diz literalmente "Bean Validation padrão — sem AD nova"), não uma regra gravada em pedra: se o dev agent julgar mais consistente seguir "409/404 antes do 422" pra bater com `CompanyServiceBean`/`PositionServiceBean`, nenhum AC quebra — documentar a escolha feita, não deixar ambíguo.

**Nomes de classe colidem entre `domain.internal` e `api.dto` — dois lugares:** `PositionWorkSchedule` (entidade JPA vs. schema YAML) e o enum `DayOfWeek` (dois enums de mesmo nome, um em `domain.corporate.position.internal`, outro gerado em `api.dto` a partir de `ScosComponents.yml`). O projeto já resolve esse tipo de colisão pra `Position`/`Company` só importando um dos dois lados por arquivo (nunca os dois) — seguir o mesmo princípio, não renomear nenhuma das classes existentes pra "resolver" a colisão.

**O schema `PositionWorkSchedule` (API) não expõe `id`** — a chave natural do sub-recurso, do ponto de vista do cliente, é `(positionId, dayOfWeek)` (decisão de design já tomada: `dayOfWeek` como chave do sub-recurso, não um `id` sequencial — ver `20260701_jornada-trabalho-position-employee.md` §3, RNF-02). Isso força o Use Case de `create` a devolver só o `Long id` gerado (pro `201`/`CreateResponse` genérico), não o objeto `PositionWorkSchedule` mapeado — ver Task 5.

### Onde cada peça vai (camadas)

- **`domain/corporate/position/dto/`**: `PositionWorkScheduleInput`/`Output` (novos).
- **`domain/corporate/position/specification/PositionWorkScheduleService.java`** (novo).
- **`domain/corporate/position/service/`**: `PositionWorkScheduleServiceBean` + `PositionWorkScheduleMapper` (novos).
- **`domain/corporate/position/internal/PositionWorkScheduleRepository.java`** (já existe, 3 métodos novos).
- **`shared/exception/ExceptionCodeError.java` + `scos_message_organization[_en].properties`**: 3 códigos novos (`SCOS_POSITION_WORK_SCHEDULE_001/002/003`).
- **`usecase/corporate/position/`**: 4 pares Use Case+Bean novos + extensão de `PositionApiMapper.java` (já existe).
- **`api/delegate/position/PositionDelegate.java`** (já existe, 4 `@Override` novos).
- **Nenhuma mudança em**: `etc/api/organization/*.yml`, `ScosGeotemporalPermission`, Liquibase (`flow-organization-resources`), nada em `Employee`.

### Testing Standards

- Unitário de domínio: mesmo padrão `@ExtendWith(MockitoExtension.class)` + `@Mock`/`@InjectMocks` de `PositionServiceBeanTest`/`CompanyServiceBeanTest` — `assertThatThrownBy(...).hasFieldOrPropertyWithValue("code", SCOS_POSITION_WORK_SCHEDULE_0XX.getCode())`.
- Unitário de Use Case: mesmo padrão BDD (`given`/`then`, `ArgumentCaptor`) de `CreatePositionUseCaseBeanTest` — incluindo o teste de `@NonNull` (request/positionId nulo → `NullPointerException`, sem chamar o service).
- Integração full-stack: estender `PositionControllerTest.java` (Testcontainers Postgres+Redis já ativo via `ScosOrganizationTestUtil`), seguindo os helpers/constantes já existentes no arquivo (`create`, `disable`, `body`, `SEEDED_ID`).
- `code`/jDempotent: `createPositionWorkSchedule` já tem `x-jdempotentrequestpayload`/`x-jdempotentresource` (`cachePrefix: SCOS_ORGANIZATION_IDP_CREATE_POSITION_WORK_SCHEDULE`, `ttl: 1`) publicados no YAML — nada a fazer no código quanto a isso, só ter cuidado pra usar `dayOfWeek` único por teste que faz `POST` (mesmo cuidado já usado com `code` nos testes de Position/Company).

### Project Structure Notes

- Nenhum módulo Maven novo. Novo sub-pacote nenhum além dos arquivos citados — tudo dentro de `domain/corporate/position/{dto,specification,service,internal}`, `usecase/corporate/position/`, `api/delegate/position/` já existentes.
- Nenhuma mudança em `etc/`.

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Epic 1 / Story 1.5] — Given/When/Then originais.
- [Source: etc/doc/ideia/20260701_jornada-trabalho-position-employee.md] — decisões de design pré-épico: `dayOfWeek` como chave do sub-recurso (RNF-02), array sem paginação (RNF-03), validação no use case sem `CHECK` (RF-03), permissões compostas (RF-04).
- [Source: etc/doc/usecase/02-departamento-cargo.md#3.1] — UC-144..147, regras em ordem de `create`/`update`, mapeamento de status HTTP por cenário.
- [Source: etc/api/organization/ScosOrganization_Department-Position.yml:439-534,742-812] — os 4 endpoints e os 3 schemas já publicados, nenhuma mudança necessária.
- [Source: etc/api/organization/ScosComponents.yml:279-288] — enum `DayOfWeek` compartilhado (local canônico já decidido).
- [Source: organization/flow-organization-resources/.../tables/scos_position_work_schedule.yml] — schema já implementado, UK composta, todas colunas `NOT NULL`.
- [Source: organization/flow-organization-resources/.../checks/checks.yml:56] — `CHECK` de vocabulário de `dayOfWeek`, sem `CHECK` de ordem cronológica.
- [Source: organization/flow-organization-domain/.../corporate/position/internal/PositionWorkSchedule.java, PositionWorkScheduleRepository.java] — entidade e repositório já existentes, estado atual (sem métodos de consulta).
- [Source: organization/flow-organization-domain/.../corporate/position/internal/DayOfWeek.java] — enum canônico, reaproveitado também por `EmployeeWorkSchedule`.
- [Source: organization/flow-organization-domain/.../corporate/position/service/PositionServiceBean.java, PositionMapper.java] — padrão de Service/Mapper a replicar.
- [Source: organization/flow-organization-usecase/.../corporate/position/CreatePositionUseCaseBean.java, PositionApiMapper.java] — padrão de Use Case + api-mapper estático a replicar, incluindo o tratamento da colisão de nomes `Position`/`Position`.
- [Source: organization/flow-organization-usecase/src/test/.../corporate/position/CreatePositionUseCaseBeanTest.java] — padrão de teste BDD de Use Case a replicar.
- [Source: organization/flow-organization-api/.../delegate/position/PositionDelegate.java] — Delegate atual, 4 métodos novos a acrescentar.
- [Source: organization/flow-organization-infrastructure/.../enumaration/ScosOrganizationPermission.java:39-42] — as 4 permissões já cadastradas (2026-07-15).
- [Source: organization/flow-organization-shared/.../exception/ExceptionCodeError.java:61-68] — `SCOS_POSITION_001..005` já existentes; `SCOS_POSITION_WORK_SCHEDULE_001..003` novos desta story (módulo próprio).
- [Source: organization/flow-organization-shared/src/main/resources/scos_message_validation.properties:7, scos_message_validation_en.properties:7] — `SCOS_VALIDATION_007` (formato de horário) já existente.
- [Source: etc/database/seed_data.sql:302-307] — padrão de seed de `SCOS_RESOURCE` pra Position, referência pro item opcional da Task 8.
- [Source: organization/flow-organization-boot/src/test/.../position/PositionControllerTest.java] — helpers/constantes existentes a reaproveitar na Task 7.
- [Source: organization/flow-organization-boot/.../ScosOrganizationWiremockUtil.java:121,146,165-177] — `fullAdminAuthority()` deriva permissões do enum Java, não do seed do banco — base da nota "opcional, não bloqueante" da Task 8.
- [Source: _bmad-output/implementation-artifacts/0-2-padronizar-tipos-temporais-clock-injetavel.md#AC1] — confirma `LocalTime` como tipo já correto pra `PositionWorkSchedule`/`EmployeeWorkSchedule`, sem relação com a padronização de `Instant`/`Clock` daquela story.
- [Source: _bmad-output/planning-artifacts/architecture/architecture-SawCunhaOS-Organization-2026-07-18/ARCHITECTURE-SPINE.md] — Capability Map, FR-4: "Bean Validation padrão — sem AD nova" (sem decisão arquitetural específica pra esta story).

## Dev Agent Record

### Agent Model Used

Claude Sonnet 5 (claude-sonnet-5)

### Debug Log References

- `mvn generate-sources` em `flow-organization-usecase`/`flow-organization-api` confirmou os DTOs/assinaturas gerados exatamente como previsto nas Dev Notes: `getAllPositionWorkSchedule` retorna `List<PositionWorkSchedule>` direto (sem classe wrapper), `PositionWorkSchedule` (api.dto) sem campo `id`.
- `mvn -pl flow-organization-domain test -Dtest=PositionWorkScheduleServiceBeanTest -Denforcer.skip=true` → 11/11.
- Mesma ambiguidade de `update(any())` já vista nas Stories 1.2/1.3 (`JpaSpecificationExecutor.update(UpdateSpecification)` vs `BaseJpaRepository.update(S)`) — corrigida com `update(any(PositionWorkSchedule.class))`.
- `mvn -pl flow-organization-usecase test -Dtest=GetAllPositionWorkScheduleUseCaseBeanTest,CreatePositionWorkScheduleUseCaseBeanTest,UpdatePositionWorkScheduleUseCaseBeanTest,DeletePositionWorkScheduleUseCaseBeanTest -Denforcer.skip=true` → 10/10.
- Removidos 2 testes de "@NonNull nulo → NullPointerException" que eu mesmo escrevi errado para `GetAll`/`Delete` (parâmetros `Long`/`enum` soltos): confirmado por precedente (`DisablePositionUseCaseBeanTest`) que `org.jspecify.annotations.NonNull` não tem enforcement runtime — só gera NPE quando o próprio objeto é desreferenciado (records, como em `Create`/`Update` que chamam `request.dayOfWeek()`/`request.startTime()`). Mantidos só os testes de request nulo, que realmente NPEmam.
- `mvn -pl flow-organization-boot test -Dtest=PositionControllerTest -Denforcer.skip=true` → 51/51 (36 pré-existentes + 15 novos de work-schedule) já na primeira tentativa.
- `mvn -pl flow-organization-domain,flow-organization-usecase,flow-organization-infrastructure test -Denforcer.skip=true` → 225+196+3 = 424/424, sem regressão.
- `mvn -pl flow-organization-boot test -Denforcer.skip=true` → 385/385, sem regressão na suíte de integração completa.
- `-Denforcer.skip=true` usado só para contornar o enforcer pré-existente já quebrado (ver *Build quebrado* no `project-context.md`).

### Completion Notes List

- Contrato, permissões e Liquibase já publicados desde antes desta story (confirmado) — implementadas só as camadas `domain`/`usecase`/`api` que faltavam: DTOs (`PositionWorkScheduleInput`/`Output`), 3 métodos novos em `PositionWorkScheduleRepository` (2 derived queries + 1 QueryDSL, sem CTE — lista plana por FK), `PositionWorkScheduleService`/`ServiceBean`/`Mapper`, 4 Use Cases + extensão de `PositionApiMapper`, 4 `@Override` novos em `PositionDelegate`.
- Ordem de guarda deliberada (documentada, não é a "409→404→422" padrão do resto do projeto): ordem cronológica (422, sem round-trip de banco) roda antes da existência do Cargo (404) e da unicidade do par (409) — decisão de `etc/doc/usecase/02-departamento-cargo.md` (UC-144..147), registrada no Javadoc da classe.
- 3 códigos de erro novos em módulo próprio (`SCOS_POSITION_WORK_SCHEDULE_001/002/003`), não emendados em `SCOS_POSITION_00X` — segue o padrão já usado por outros sub-recursos (`ADDRESS_TYPE`, `CNAE`, etc.).
- Colisão de nomes deliberada entre `domain.internal.PositionWorkSchedule`/`DayOfWeek` e `api.dto.PositionWorkSchedule`/`DayOfWeek` resolvida importando só um lado por arquivo (nunca os dois via `import` simples) — mesmo princípio já usado pra `Position`/`Company`. `Update`/`DeleteUseCase` recebem `api.dto.DayOfWeek` (o que o Delegate gerado entrega) e convertem para `domain.internal.DayOfWeek` via FQN inline.
- `CreatePositionWorkScheduleUseCase` retorna `Long` (não o objeto mapeado) porque o schema `PositionWorkSchedule` não expõe `id` — decisão de design já tomada (chave natural do sub-recurso é `(positionId, dayOfWeek)`).
- Guarda de escopo (Task 8) confirmada via `git status`: zero mudança em `etc/api/organization/*.yml`, `ScosGeotemporalPermission`, Liquibase ou `Employee` — só `domain`/`usecase`/`api`/`shared` + testes.
- Item opcional da Task 8 (seed de `SCOS_RESOURCE` para as 4 permissões) não aplicado — não bloqueante, autorização nos testes vem do enum Java via WireMock, confirmado nos 385 testes de integração passando.

### File List

- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/corporate/position/dto/PositionWorkScheduleInput.java` (novo)
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/corporate/position/dto/PositionWorkScheduleOutput.java` (novo)
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/corporate/position/internal/PositionWorkScheduleRepository.java` (modificado — 3 métodos novos)
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/corporate/position/specification/PositionWorkScheduleService.java` (novo)
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/corporate/position/service/PositionWorkScheduleMapper.java` (novo)
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/corporate/position/service/PositionWorkScheduleServiceBean.java` (novo)
- `organization/flow-organization-domain/src/test/java/br/com/sawcunhaos/organization/domain/corporate/position/service/PositionWorkScheduleServiceBeanTest.java` (novo)
- `organization/flow-organization-shared/src/main/java/br/com/sawcunhaos/organization/shared/exception/ExceptionCodeError.java` (modificado — `SCOS_POSITION_WORK_SCHEDULE_001..003`)
- `organization/flow-organization-shared/src/main/resources/scos_message_organization.properties` (modificado — 3 mensagens PT)
- `organization/flow-organization-shared/src/main/resources/scos_message_organization_en.properties` (modificado — 3 mensagens EN)
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/corporate/position/GetAllPositionWorkScheduleUseCase.java` (novo)
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/corporate/position/GetAllPositionWorkScheduleUseCaseBean.java` (novo)
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/corporate/position/CreatePositionWorkScheduleUseCase.java` (novo)
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/corporate/position/CreatePositionWorkScheduleUseCaseBean.java` (novo)
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/corporate/position/UpdatePositionWorkScheduleUseCase.java` (novo)
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/corporate/position/UpdatePositionWorkScheduleUseCaseBean.java` (novo)
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/corporate/position/DeletePositionWorkScheduleUseCase.java` (novo)
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/corporate/position/DeletePositionWorkScheduleUseCaseBean.java` (novo)
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/corporate/position/PositionApiMapper.java` (modificado — `toApiPositionWorkSchedule`)
- `organization/flow-organization-usecase/src/test/java/br/com/sawcunhaos/organization/application/usecase/corporate/position/GetAllPositionWorkScheduleUseCaseBeanTest.java` (novo)
- `organization/flow-organization-usecase/src/test/java/br/com/sawcunhaos/organization/application/usecase/corporate/position/CreatePositionWorkScheduleUseCaseBeanTest.java` (novo)
- `organization/flow-organization-usecase/src/test/java/br/com/sawcunhaos/organization/application/usecase/corporate/position/UpdatePositionWorkScheduleUseCaseBeanTest.java` (novo)
- `organization/flow-organization-usecase/src/test/java/br/com/sawcunhaos/organization/application/usecase/corporate/position/DeletePositionWorkScheduleUseCaseBeanTest.java` (novo)
- `organization/flow-organization-api/src/main/java/br/com/sawcunhaos/organization/api/delegate/position/PositionDelegate.java` (modificado — 4 `@Override` novos)
- `organization/flow-organization-boot/src/test/java/br/com/sawcunhaos/organization/boot/api/position/PositionControllerTest.java` (modificado — 15 testes novos de work-schedule)

## Change Log

- 2026-07-22: Implementado template de Jornada de Trabalho por Cargo (CRUD completo `PositionWorkSchedule`) — contrato/permissões/Liquibase já publicados, só camadas domain/usecase/api novas; 25 arquivos (7 modificados, 18 novos); 36 testes novos (11 domain + 10 usecase + 15 integração), 0 regressão (424 domain/usecase/infra + 385 boot). Status → review.
