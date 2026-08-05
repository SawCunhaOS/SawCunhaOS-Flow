# Padrões de Escrita de Código — SawCunhaOS-Flow

> **Documento normativo.** Define o padrão que todo código novo **deve** seguir e o alvo para o qual
> o código existente será migrado. Extraído do repositório `SawCunhaOS/SawCunhaOS-Flow`
> (529 arquivos Java) e endurecido com as correções acordadas.
>
> Versão 2 — 2026-07-31. Complementa o `README.md` (arquitetura/estado) e o `CLAUDE.md` (fluxo).
> O plano de migração do código legado está no PRD de Padronização.

## Como ler este documento

| Marcador | Significado |
|---|---|
| **DEVE** / **NÃO DEVE** | Obrigatório. PR que viola é rejeitado. Onde há verificação automática, o build quebra. |
| **DEVERIA** | Recomendado. Desvio precisa ser justificado na descrição do PR. |
| **PODE** | Facultativo. |
| 🔄 **Migração** | Regra já obrigatória para código **novo**; o código existente ainda diverge e será convertido pelo PRD de Padronização. |

Regra de ouro: **código novo nasce no padrão alvo.** Não se replica desvio existente "para manter
consistência local" — a consistência é com este documento.

---

## 1. Estrutura do projeto

### 1.1 Monorepo Maven

Raiz `flow` (`packaging: pom`), parent externo `br.com.sawcunhaos:scos-bom`, Java 25 / Spring Boot 4.x.

```
flow/                                    artifactId: flow  (br.com.sawcunhaos)
├── organization/                        groupId: br.com.sawcunhaos.flow
│   ├── flow-organization-grpc-proto/    contratos protobuf
│   ├── flow-organization-shared/        catálogo de erros, bundles PT/EN, converters, validation
│   ├── flow-organization-infrastructure/ enums de permissão, config Liquibase, cross-cutting
│   ├── flow-organization-domain/        entidades JPA, repositórios, domain services, regras
│   ├── flow-organization-usecase/       orquestração (Use Cases)
│   ├── flow-organization-api/           delegates REST + DTOs gerados do OpenAPI
│   ├── flow-organization-boot/          composition root REST + testes de integração
│   ├── flow-organization-grpc-boot/     composition root gRPC
│   ├── flow-organization-resources/     changelogs Liquibase, seed
│   └── flow-security-starter/           lib de segurança reusável (pacote ...security.starter)
├── infrastructure/                      infra técnica compartilhada (...flow.infrastructure)
├── notification/  geotemporal/          módulos-esqueleto (reserva)
└── server-fat/                          composition root alternativo (fat-jar)
```

Os módulos **DEVEM** ser declarados no `pom.xml` na ordem de dependência
(proto → shared → infrastructure → domain → usecase → api → boot), não em ordem alfabética.

### 1.2 Fluxo de dependência entre camadas

```
delegate (api) → UseCase (usecase) → Service (domain/specification) → Repository (domain/internal)
```

Regras verificadas por teste ArchUnit (`ArquiteturaTest`), não por acordo verbal:

- Delegate **NÃO DEVE** referenciar `*Repository` nem `*ServiceBean`. Só Use Case.
- Use Case **NÃO DEVE** referenciar `*Repository` nem entidade JPA. Só a interface `...Service`
  do pacote `specification` e os records `dto`.
- Entidade JPA **NÃO DEVE** ser referenciada fora de `domain..internal`, `domain..service`
  e `domain..specification`.
- Nenhuma classe de `domain` **DEVE** referenciar `application.usecase` ou `api`.
- `*ServiceBean` de um agregado **NÃO DEVE** referenciar `*Repository` de outro agregado.
  A dependência entre agregados passa pela interface `...Service` do outro agregado.

### 1.3 Pacotes

Base: `br.com.sawcunhaos.organization`

| Camada | Pacote | Regra |
|---|---|---|
| API | `api.delegate.<agregado>` | um subpacote por agregado (`company`, `position`, `reason`, `catalog`) |
| Use Case | `application.usecase.<bounded-context>.<agregado>` | `corporate/`, `access/`, `configuration/` |
| Domínio | `domain.<bounded-context>.<agregado>.<papel>` | papéis fixos: `internal`, `dto`, `service`, `specification` |

Bounded contexts em uso: `corporate`, `access`, `configuration`, `outbox`, `shift`.
Novo bounded context **DEVE** ser acordado antes de criado; novo agregado dentro de um contexto
existente não precisa de acordo.

Papéis dentro do agregado de domínio:

- **`internal/`** — entidade JPA, `*Repository`, PKs compostas (`*Pk`), enums de estado, `rules/`
- **`dto/`** — records `*Input`, `*Output`, `*Filter`
- **`specification/`** — a interface pública `<Agregado>Service`
- **`service/`** — implementação `<Agregado>ServiceBean` + `<Agregado>Mapper` (MapStruct)

---

## 2. Convenções gerais de arquivo

### 2.1 Cabeçalho de licença

Todo arquivo `.java`, `.xml` e `.properties` **DEVE** começar com o bloco Apache 2.0 antes do
`package`. O Spotless aplica e verifica isso no build — não escreva o bloco à mão.

```java

/*
 *
 *  * Copyright 2026 SawCunha Open System - SawCunhaOS-Organization
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *
 */

package br.com.sawcunhaos.organization.application.usecase.corporate.position;
```

### 2.2 Formatação

- Indentação: 4 espaços (Java, `pom.xml`), 2 espaços (YAML)
- Fim de linha: **LF** (`end_of_line = lf`), arquivo termina com newline. 🔄 **Migração**
- Encoding: UTF-8
- Builder quebrado em um método por linha, continuação indentada com 8 espaços
- Assinatura longa **PODE** ficar em linha única (é o formato que o OpenAPI Generator produz)

### 2.3 Imports

- **NÃO DEVE** usar wildcard (`import x.*`)
- Ordem: `br.com.sawcunhaos.*` → terceiros (`com`, `io`, `jakarta`, `lombok`, `org`) → `java.*`
  → bloco de `import static` no fim
- Código de erro **DEVE** entrar por import estático individual, para o `throw` ficar legível

### 2.4 Idioma

| Elemento | Idioma |
|---|---|
| Classe, método, variável, pacote, coluna, enum | Inglês |
| Javadoc e comentário | PT-BR |
| Mensagem de log | Inglês, curta |
| `@DisplayName` de teste | PT-BR |
| Mensagem ao usuário (bundle) | PT-BR + EN |

### 2.5 Nomenclatura

> Regras detalhadas de nome (módulo, pacote, classe, membro, contrato, tabela, teste,
> vocabulário canônico) estão em **`PADROES-NOMENCLATURA-SawCunhaOS-Flow.md`**. O resumo abaixo
> é o mínimo verificável em revisão.

- Nome **DEVE** ser palavra completa e corretamente grafada. Abreviação e erro de digitação
  em nome público são bloqueantes em revisão. 🔄 **Migração** (`PaginatioUtils`,
  pacote `infrastructure.enumaration`)
- Sufixo **DEVE** identificar o papel: `Delegate`, `UseCase`, `UseCaseBean`, `Service`,
  `ServiceBean`, `Repository`, `Mapper`, `Input`, `Output`, `Filter`, `Configuration`,
  `Properties`, `Interceptor`, `Handler`, `Filter`, `Test`

### 2.6 Injeção de dependência

- **DEVE** ser por construtor, via `@RequiredArgsConstructor` com campos `private final`
- **NÃO DEVE** usar `@Autowired` em campo, inclusive em `@Configuration`. 🔄 **Migração**
  (`ScosDataSourceConfiguration`, `ScosAuditDataSourceConfiguration`)

### 2.7 Logging

- `@Slf4j` na classe; `log.info(...)` como primeira linha do método público de delegate,
  use case e domain service
- **DEVE** usar placeholder `{}`; **NÃO DEVE** concatenar string
- **NÃO DEVE** logar dado pessoal sem o mascaramento da foundation
- **NÃO DEVE** usar `System.out` / `System.err`. 🔄 **Migração** (1 ocorrência)

---

## 3. Padrão por tipo de artefato

### 3.1 Delegate (camada API)

**Local:** `flow-organization-api/.../api/delegate/<agregado>/<Agregado>Delegate.java`

**NÃO DEVE existir `@RestController` no projeto.** A interface `<Agregado>ApiDelegate` é gerada
pelo OpenAPI Generator (`delegatePattern: true`) a partir do YAML.

```java
/**
 * Implementação de {@link CompanyApiDelegate} — expõe o CRUD cadastral de {@code /v1/companies}
 * (UC-001..005) e as 4 transições de status (UC-006), delegando a validação de negócio aos
 * respectivos Use Cases.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CompanyDelegate implements CompanyApiDelegate {

    private final FindCompanyUseCase findCompanyUseCase;
    private final FindAllCompanyUseCase findAllCompanyUseCase;

    @Override
    public GetCompanyResponse getCompanyById(Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        return GetCompanyResponse.builder()
                .data(findCompanyUseCase.execute(id))
                .build();
    }

    @Override
    public GetAllCompaniesResponse getAllCompanies(PaginationFilter paginationFilter, Optional<UUID> xRequestID,
                                                  Optional<String> acceptLanguage, Optional<StatusCompany> status,
                                                  Optional<String> name) {
        return findAllCompanyUseCase.execute(paginationFilter,
                CompanyFilter.builder()
                        .status(status.orElse(null))
                        .name(name.orElse(null))
                        .build());
    }

    @Override
    public Void updateCompany(Long id, UpdateCompanyRequest request, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        updateCompanyUseCase.execute(id, request);
        return null;
    }
}
```

Regras:

- `public class` anotada `@Component` (**NÃO** `@Service`), `@RequiredArgsConstructor`, `@Slf4j`
- Um campo `final` por Use Case; nome do campo = nome da interface em lowerCamelCase
- **NÃO DEVE** conter lógica de negócio, `if` de regra, cálculo ou acesso a dado. Só monta o
  wrapper `data:` e delega
- Retorno `Void` → chama o use case e `return null;`
- Filtros opcionais **DEVEM** ser agrupados em um record `<Agregado>Filter` construído no
  delegate, em vez de virarem parâmetros posicionais anuláveis. 🔄 **Migração**
- `xRequestID` e `acceptLanguage` são recebidos e ignorados (tratados por filtro/interceptor)
- Javadoc de classe **DEVERIA** citar os UCs cobertos e o que fica com o comportamento default

### 3.2 Use Case — interface

**Local:** `flow-organization-usecase/.../application/usecase/<bc>/<agregado>/`
**Nome:** `<Verbo><Agregado>UseCase`. Verbos permitidos: `Create`, `Update`, `Delete`, `Find`,
`FindAll`, `GetAll`, `Enable`, `Disable`, `Activate`, `Inactivate`, `Block`, `Unblock`.
Verbo novo **DEVE** ser acordado antes de introduzido.

```java
public interface CreatePositionUseCase {
    Position execute(@NonNull CreatePositionRequest createPositionRequest);
}
```

- `public interface` com **exatamente um** método, chamado `execute`
- Entrada e saída **DEVEM** ser DTOs da API (`br.com.sawcunhaos.organization.api.dto`) ou tipos
  primitivos/`java.*`. **NÃO DEVE** expor tipo de domínio
- Nome do parâmetro = nome do tipo em lowerCamelCase

### 3.3 Nulidade — regra transversal (JSpecify)

Esta regra vale para **todas** as camadas e é verificada pelo NullAway no build.

- Todo parâmetro de método público de `UseCase`, `Service` e `Repository` **DEVE** ser anotado
  `@NonNull` ou `@Nullable` (`org.jspecify.annotations`)
- `@NonNull` significa que **nenhum chamador pode passar null**. Passar `optional.orElse(null)`
  para um parâmetro `@NonNull` é violação, não estilo. 🔄 **Migração** (hoje o
  `PositionDelegate` faz exatamente isso contra `PositionService.findAll`)
- Filtro opcional **DEVE** ser `@Nullable` explícito ou, preferencialmente, campo de um record
  `<Agregado>Filter` — que **DEVE** ser `@NonNull`, com campos internos anuláveis
- Retorno que pode ser vazio **DEVE** ser `Optional<T>`, nunca `null`

### 3.4 Use Case — implementação (`...Bean`)

```java
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = ScosException.class)
class CreatePositionUseCaseBean implements CreatePositionUseCase {

    private final PositionService positionService;
    private final PositionApiMapper positionApiMapper;

    @Override
    public Position execute(@NonNull CreatePositionRequest createPositionRequest) {
        log.info("Create position: {}", createPositionRequest.code());

        PositionInput positionInput = PositionInput.builder()
                .code(createPositionRequest.code())
                .description(createPositionRequest.description())
                .departmentId(createPositionRequest.departmentId())
                .isTrustPosition(Boolean.TRUE.equals(createPositionRequest.isTrustPosition()))
                .build();

        PositionOutput positionOutput = positionService.create(positionInput);

        return positionApiMapper.toApiPosition(positionOutput);
    }
}
```

Regras:

- Sufixo `Bean`, mesma pasta da interface, **visibilidade de pacote** (sem `public`).
  Vale para todo `*Bean` do projeto
- `@Service @RequiredArgsConstructor @Slf4j` obrigatórios
- **O Use Case é a única fronteira transacional.** `@Transactional` **DEVE** estar na classe:
  escrita `@Transactional(rollbackFor = ScosException.class)`, leitura
  `@Transactional(readOnly = true)`
- Corpo em 3 blocos separados por linha em branco: **request → Input**, **chamada ao service**,
  **Output → DTO da API**
- Boolean opcional do request **DEVE** ser tratado com `Boolean.TRUE.equals(...)`
- **NÃO DEVE** capturar `ScosException` — ela sobe para o handler central

### 3.5 Mapper da camada de use case

Mapeamento **DEVE** usar MapStruct nas duas camadas. Mapper estático manual não é mais aceito
para código novo. 🔄 **Migração** (os `*ApiMapper` atuais são `final class` com métodos `static`)

```java
@Mapper(componentModel = "spring", uses = DepartmentApiMapper.class)
public interface PositionApiMapper {

    Position toApiPosition(PositionOutput positionOutput);

    PositionWorkSchedule toApiPositionWorkSchedule(PositionWorkScheduleOutput output);
}
```

- Nome `<Agregado>ApiMapper`, um por pasta de agregado, injetado como bean
- Prefixo de método `toApi<Tipo>`
- Regra de nulo de associação é responsabilidade do MapStruct, não de `if` manual

### 3.6 Domain Service — interface (`specification/`)

```java
public interface PositionService {

    PositionOutput create(@NonNull PositionInput positionInput);
    void update(@NonNull PositionInput positionInput);
    PositionOutput findById(@NonNull Long positionId);
    Page<PositionOutput> findAll(@NonNull PositionFilter filter, @NonNull Pageable pageable);
    void enable(@NonNull Long positionId);
    void disable(@NonNull Long positionId);

    Position findPositionById(@NonNull Long positionId);

}
```

- Nome `<Agregado>Service`, no pacote `specification`
- CRUD agrupado sem linha em branco; método que expõe **entidade** fica separado por linha em
  branco no fim, e só **PODE** ser chamado por outro `*ServiceBean` — nunca por use case
  (regra verificada no ArchUnit)
- Todo parâmetro anotado conforme §3.3
- Paginação sempre `Page<XxxOutput>` + `Pageable`

### 3.7 Domain Service — implementação (`service/`)

```java
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(propagation = Propagation.MANDATORY)
class PositionServiceBean implements PositionService {

    private final PositionRepository positionRepository;
    private final PositionMapper positionMapper;
    private final DepartmentService departmentService;
    private final ScosUserAuthentication scosUserAuthentication;

    @Override
    public PositionOutput create(@NonNull PositionInput positionInput) {
        log.info("Create Position: {}", positionInput.code());

        if (positionRepository.existsByCode(positionInput.code())) {
            throw new ScosException(POSITION_CODE_ALREADY_EXISTS);
        }

        Department department = findActiveDepartmentOrThrow(positionInput.departmentId());

        Position position = Position.builder()
                .code(positionInput.code())
                .active(true)
                .department(department)
                .build();
        position.updateAuditInfo(scosUserAuthentication.findUserAuthentication());

        return positionMapper.toPositionOutput(positionRepository.merge(position));
    }
}
```

Regras:

- Sufixo `Bean`, **package-private**. 🔄 **Migração** (9 de 17 estão `public` hoje)
- **NÃO DEVE** abrir transação própria. A classe **DEVE** declarar
  `@Transactional(propagation = Propagation.MANDATORY)`, que falha alto se alguém chamar o
  domain service fora de um use case. **NÃO DEVE** haver `@Transactional` por método aqui.
  🔄 **Migração**
- É aqui que mora a regra de negócio: validação de unicidade e integridade antes de persistir,
  sempre com `throw new ScosException(<CODIGO_SEMANTICO>)`
- Auditoria **DEVE** ser aplicada com `updateAuditInfo(...)` antes de `merge`/`update`
- Persistência via `BaseJpaRepository`: `merge(...)` para criar, `update(...)` para alterar.
  **NÃO DEVE** usar `save(...)`
- Helper privado de busca com validação **DEVE** terminar em `OrThrow`
- Dependência de outro agregado **DEVE** passar pela interface `...Service` dele

### 3.8 Entidade JPA (`internal/`)

```java
@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "SCOS_POSITION")
@Auditable
public class Position extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "POSITION_ID")
    private Long id;
    @Column(name = "CODE", nullable = false)
    private String code;
    @Builder.Default
    @Column(name = "ACTIVE", nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DEPARTMENT_ID")
    private Department department;

    /**
     * Ativa a position.
     * @throws ScosException POSITION_ALREADY_ACTIVE se já estiver ativa.
     */
    public void activate() {
        if (this.active) {
            throw new ScosException(POSITION_ALREADY_ACTIVE);
        }
        this.active = true;
    }

    /** Altera os dados cadastrais da position. */
    public void changeRegistration(String code, String description, Department department) {
        this.code = code;
        this.description = description;
        this.department = department;
    }
}
```

Regras:

- **NÃO DEVE** usar `@Setter` na classe nem em campo de estado. Toda mutação **DEVE** passar por
  método de negócio nomeado (`activate()`, `deactivate()`, `changeRegistration(...)`).
  🔄 **Migração** — é a regra que protege as invariantes que a entidade já tenta defender
- `@AllArgsConstructor` **DEVE** ser `access = AccessLevel.PRIVATE` (existe para o `@Builder`)
- Ordem das anotações: Lombok → JPA (`@Entity`, `@Table`) → `@Auditable`
- **DEVE** estender `BaseEntity`
- Colunas escalares sem linha em branco entre si; associações separadas por linha em branco
- `@Column(name = "UPPER_SNAKE")` sempre explícito, com `nullable` quando aplicável
- Campo com valor default **DEVE** ter `@Builder.Default`
- `@ManyToOne` **DEVE** ser `fetch = FetchType.LAZY`
- Método que lança exceção **DEVE** ter Javadoc com `@throws` citando o código
- **NÃO DEVE** usar `LocalDateTime` nem `.now()` estático. `Instant` + `Clock` injetável

### 3.9 Repository (`internal/`)

```java
@Repository
public interface PositionRepository extends BaseJpaRepository<Position, Long>,
        JpaSpecificationExecutor<Position>, QuerydslPredicateExecutor<Position> {

    QPosition qPosition = QPosition.position;

    Page<Position> findAll(Pageable pageable);

    Optional<Position> findById(Long id);

    default boolean existsByCode(String code) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();

        booleanBuilder.and(qPosition.code.eq(code));

        return exists(booleanBuilder.getValue());
    }

    default Page<Position> findAllFiltered(@NonNull PositionFilter filter, Pageable pageable) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();

        if (filter.departmentId() != null) {
            booleanBuilder.and(qPosition.department.id.eq(filter.departmentId()));
        }
        if (filter.active() != null) {
            booleanBuilder.and(qPosition.active.eq(filter.active()));
        }

        return booleanBuilder.hasValue()
                ? findAll(booleanBuilder.getValue(), pageable)
                : findAll(pageable);
    }
}
```

Regras:

- **DEVE** estender as três interfaces: `BaseJpaRepository` (Hypersistence),
  `JpaSpecificationExecutor`, `QuerydslPredicateExecutor`
- Constante do Q-type declarada no topo
- Consulta dinâmica **DEVE** ser método `default` com `BooleanBuilder`. **NÃO DEVE** usar `@Query`
  nem derivação de nome com mais de dois critérios
- Nomes: `existsBy<Campo>`, `existsBy<Campo>AndNotId`, `findAllFiltered`
- Filtro **DEVE** chegar como record `<Agregado>Filter`, não como parâmetros anuláveis soltos.
  🔄 **Migração**
- Consulta hierárquica/recursiva **DEVE** ser resolvida no banco com `WITH RECURSIVE`.
  **NÃO DEVE** caminhar hierarquia em memória Java

### 3.10 DTOs de domínio (`dto/`)

```java
@Builder
public record PositionOutput(
        Long id,
        String code,
        String description,
        boolean active,
        boolean isTrustPosition,
        DepartmentOutput department
) {
}
```

- `record` + `@Builder`, sem método, sem validação, sem lógica
- Sufixos: `Input` (entrada do domain service), `Output` (saída), `Filter` (critério de busca)
- Um componente por linha; composição por outros `*Output`, nunca por entidade

### 3.11 Mapper MapStruct (`service/`)

```java
@Mapper(componentModel = "spring", uses = DepartmentMapper.class)
public interface PositionMapper {

    @Mapping(target = "isTrustPosition", source = "trustPosition")
    @Mapping(target = "department", ignore = true)
    PositionOutput toPositionOutput(Position position);

}
```

- Interface pública, `componentModel = "spring"`, `uses` para mappers de agregados relacionados
- Nome do método `to<Tipo>Output`
- `ignore = true` só para associação que exige carga controlada, e o preenchimento manual
  correspondente **DEVE** estar no `ServiceBean`

### 3.12 Catálogo de erros

**Local:** `flow-organization-shared/.../shared/exception/<Contexto>ExceptionCode.java` —
um enum **por bounded context** implementando `ExceptionCode`, não um enum único global.
🔄 **Migração** (hoje é o monolítico `ExceptionCodeError`)

```java
public enum CorporateExceptionCode implements ExceptionCode {

    // Position
    POSITION_NOT_FOUND("SCOS_POSITION_001", 404, "SCOS_TITLE_NOT_FOUND"),
    POSITION_CODE_ALREADY_EXISTS("SCOS_POSITION_002", 409, "SCOS_TITLE_CONFLICT"),
    /** Position já está ativa — impossível ativar novamente. */
    POSITION_ALREADY_ACTIVE("SCOS_POSITION_004", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
```

Regras:

- **O nome da constante é semântico**; o código de contrato (`SCOS_<MÓDULO>_<NNN>`) vive no campo
  `code`. O `throw` tem que ser legível sem consultar tabela. 🔄 **Migração**
- O valor de `code` **NÃO DEVE** mudar depois de publicado — é contrato de API
- Numeração **NÃO DEVE** ser reaproveitada, nem quando um código é aposentado
- `title` **DEVE** ser uma das categorias RFC 9457: `SCOS_TITLE_NOT_FOUND` (404),
  `SCOS_TITLE_CONFLICT` (409), `SCOS_TITLE_BUSINESS_RULE_VIOLATION` (422),
  `SCOS_TITLE_EXTERNAL_INTEGRATION_FAILURE` (502), `SCOS_TITLE_UNAUTHORIZED` (401),
  `SCOS_TITLE_INTERNAL_ERROR` (500)
- Toda constante nova **DEVE** ter chave nos bundles PT **e** EN. Um teste exaustivo itera
  `values()` e quebra o build se faltar qualquer chave. 🔄 **Migração**
- Erro de domínio **DEVE** ser `ScosException` + código de catálogo. **NÃO DEVE** lançar
  `RuntimeException`, `IllegalArgumentException` ou `IllegalStateException` crus

### 3.13 Enum de permissão

```java
GET_DEPARTMENT("GET_DEPARTMENT", "Corporate", "Department", "1.0.0", "2026-07-09", true),
```

- Nome no formato `ACTION_RESOURCE`, 1:1 com o `x-authorize` do YAML
- Tupla `(code, categoria, recurso, versão, updatedAt, active)`
- Alterar a definição **DEVE** bumpar `updatedAt` (o upsert do resource só propaga quando
  `active` ou `updatedAt` muda)
- A consistência entre YAML e enum é verificada por `PermissionsConsistencyTest`

### 3.14 Regras de negócio compostas

Para validação com várias regras independentes (`domain/.../internal/rules/`), **DEVE** usar
`BusinessRule<T>` + `RuleChain<T>`:

- `checkFirst(context)` quando o usuário deve ver o primeiro impedimento
- `checkAll(context)` quando o usuário deve ver todos de uma vez
- Cada `BusinessRule` devolve código de erro; a conversão em `ScosException` é da chain

### 3.15 Configuração e properties

- Prefixo `Scos` + sufixo `Configuration`; properties com sufixo `Properties`
- Toda configuração de runtime **DEVE** ser externalizada: `application.yml` só referencia
  `${SCOS_*}`. **NÃO DEVE** haver valor de ambiente hardcoded
- Interceptor → sufixo `Interceptor`; handler → `Handler`; filtro → `Filter`

### 3.16 Classe de bootstrap

- Nome `Scos<Contexto>Application`, uma por composition root
- `@SpringBootApplication` + `@ComponentScan(basePackages = {"br.com.sawcunhaos"})`
- Banner de inicialização com prefixo `[SCOS]` (nome, versão, build, java)

### 3.17 gRPC

- Implementação de serviço em `grpc.boot.delegate`, sufixo `ServiceImpl`
- Interceptors em `grpc.boot.configuration.interceptor`
- Exceção tratada por `GrpcGlobalExceptionHandler`, mapeando `ExceptionCode` para `Status`

---

## 4. Contrato OpenAPI (contract-first)

**A ordem é obrigatória: o YAML vem antes do código.** PR que adiciona delegate sem o contrato
correspondente é rejeitado.

**Local:** `etc/api/organization/ScosOrganization_<Área>.yml` + `ScosComponents.yml`

```yaml
paths:
  /v1/departments:
    get:
      tags: [Department]
      summary: Get all departments
      description: UC-022 - Lista todos os departamentos com paginação
      operationId: getAllDepartments
      parameters:
        - $ref: './ScosComponents.yml#/components/parameters/X-Request-ID'
        - $ref: './ScosComponents.yml#/components/parameters/Accept-Language'
        - $ref: './ScosComponents.yml#/components/parameters/paginationFilter'
      responses:
        '200':
          description: OK
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/GetAllDepartmentsResponse'
        '4XX':
          $ref: './ScosComponents.yml#/components/responses/4XX'
        '5XX':
          $ref: './ScosComponents.yml#/components/responses/5XX'
      x-authorize: [GET_DEPARTMENT]
```

Regras:

- `openapi: 3.0.3`, `servers: [- url: /organization/api]`, bloco `info` com licença Apache
- `summary` em inglês; `description` em PT-BR **prefixada pelo caso de uso** (`UC-021 - ...`)
- `operationId` em lowerCamelCase, igual ao nome do método do delegate
- Todo endpoint **DEVE** declarar `X-Request-ID` e `Accept-Language` por `$ref`
- Respostas: `200` com schema próprio contendo wrapper `data:`;
  `201` → `ScosComponents.yml#/components/responses/201_CREATED`; `204` → `204_NO_CONTENT`;
  `4XX` e `5XX` sempre por `$ref`
- Todo endpoint protegido **DEVE** ter `x-authorize: [PERMISSAO]`, espelhando o enum de permissão
- POST/PUT idempotente **DEVE** marcar `x-jdempotentrequestpayload: true` no `requestBody`
- Contrato publicado **NÃO DEVE** sofrer alteração incompatível (campo removido, tipo alterado,
  código de erro renomeado) sem nova versão de rota

---

## 5. Banco de dados (Liquibase)

> Regras detalhadas (estrutura de changelog, ordem de execução, anatomia do changeSet, tipos,
> índices, CHECK, funções, triggers, views, rollback, versionamento e seed) estão em
> **`PADROES-LIQUIBASE-SawCunhaOS-Flow.md`**. O resumo abaixo é o mínimo verificável em revisão.

**Local:** `flow-organization-resources/src/main/resources/db/changelog/organization/`

- Um arquivo por tabela, nome em `snake_case` minúsculo (`scos_position.yml`)
- `id` do changeSet: `AAAAMMDD-Nome.Sobrenome-NNN`; `author` igual ao segmento do meio
- `comment` em PT-BR **obrigatório** em todo changeSet
- `rollback` **obrigatório** em todo changeSet (`- empty` só quando não há reversão possível)
- `schemaName: scos` explícito; tabela `SCOS_<ENTIDADE>`; PK `PK_SCOS_<TABELA>`;
  FK `FK_<COLUNA>_SCOS_<TABELA>`; UK `UK_<COLUNAS>_SCOS_<TABELA>`
- Colunas em `UPPER_SNAKE_CASE`; toda tabela transacional com `CREATED_AT`, `UPDATED_AT`, `USER_AT`
- Tipos: `TIMESTAMPTZ` para instante, `DATE` para calendário, `TIME` para hora de parede,
  `varchar(n)` com tamanho explícito, `BOOLEAN` com `defaultValueBoolean`. Sem `TIMESTAMP` sem
  timezone e sem tipo `ENUM` nativo
- Enum persistido **DEVE** ter CHECK constraint espelhando as constantes do enum Java
- Tabela de catálogo e de histórico **DEVE** ter trigger de bloqueio de `DELETE` físico
- Função, trigger e view usam `sqlFile` + `runOnChange: true`; função e trigger exigem
  `splitStatements: false`
- ChangeSet aplicado **NÃO DEVE** ser editado. Corrige-se com changeSet novo
- Dado de teste **NÃO DEVE** entrar no changelog

## 6. Internacionalização

| Bundle | Módulo | Conteúdo |
|---|---|---|
| `scos_message_organization[_en].properties` | shared | mensagem de cada código de erro |
| `scos_message_validation[_en].properties` | shared | mensagens de validação de campo |
| `messages_permission[_en].properties` | infrastructure | descrição das permissões |
| `key_configuration_description[_en].properties` | domain | descrição das chaves de configuração |

- Chave = o próprio `code` do catálogo de erros
- Arquivo sem sufixo é PT-BR (default); `_en` é o inglês
- Chave nova **DEVE** entrar nos dois arquivos no mesmo commit (verificado por teste)

---

## 7. Testes

### 7.1 Teste unitário (`*BeanTest`)

```java
/** Testes de {@link CreatePositionUseCaseBean}: mapeamento request→input, output→api e propagação. */
@ExtendWith(MockitoExtension.class)
class CreatePositionUseCaseBeanTest {

    @Mock
    private PositionService positionService;

    @InjectMocks
    private CreatePositionUseCaseBean useCase;

    private PositionOutput output() { ... }

    @Test
    @DisplayName("mapeia request para input (id nulo) e retorna o output mapeado com department")
    void executes_mapsRequestToInputAndReturnsMappedOutput() {
        CreatePositionRequest request = new CreatePositionRequest("DEV", "Developer", 3L, true);
        given(positionService.create(any(PositionInput.class))).willReturn(output());

        Position result = useCase.execute(request);

        ArgumentCaptor<PositionInput> captor = ArgumentCaptor.forClass(PositionInput.class);
        then(positionService).should().create(captor.capture());
        assertThat(captor.getValue().code()).isEqualTo("DEV");
    }
}
```

- Classe package-private, sufixo `Test`, mesmo pacote da classe testada
- `@ExtendWith(MockitoExtension.class)` + `@Mock` / `@InjectMocks`
- **DEVE** usar BDDMockito (`given` / `then`). **NÃO DEVE** usar `when` / `verify`
- **DEVE** usar AssertJ (`assertThat`, `assertThatThrownBy`). **NÃO DEVE** usar `assertEquals`
  em teste novo. 🔄 **Migração**
- `@DisplayName` em PT-BR descrevendo comportamento observável
- Nome do método: `<metodo>_<cenario><Resultado>` em camelCase
- Corpo em 3 blocos separados por linha em branco (arrange / act / assert)
- Fixture montada por método privado

### 7.2 Teste de integração (`*ControllerTest`)

```java
/**
 * Teste de integração full-stack do cadastro da Empresa (UC-001..005).
 *
 * <p>Cada teste cria os próprios dados; nenhum depende de estado deixado por outro.
 */
class CompanyControllerTest extends ScosOrganizationTestUtil {

    private static final String COMPANIES_URI = "/api/v1/companies";

    @Test
    @DisplayName("GET /v1/companies — token válido lista com paginação (200)")
    void getAll_withValidToken_returns200() throws Exception { ... }
}
```

- **DEVE** estender `ScosOrganizationTestUtil` (MockMvc, Testcontainers, JWT, reseed)
- **DEVE** ter Javadoc de classe explicando premissas e armadilhas
- **Cada método de teste DEVE ser independente**: o reseed limpa banco **e** cache (Redis);
  nenhum método pode depender de dado criado por outro nem contornar cache de idempotência
  com valor único inventado. 🔄 **Migração**
- Literal reutilizado **DEVE** ser `private static final` no topo
- Blocos separados por faixa `// ====` com a operação HTTP e o UC
- `@DisplayName` no formato `MÉTODO /rota — cenário (status)`
- Nome do método: `<operacao>_<cenario>_returns<Status>`

### 7.3 Testes de arquitetura e consistência

Estes testes são parte do padrão, não extras. Quebra neles é quebra de build.

| Teste | Garante |
|---|---|
| `ArquiteturaTest` (ArchUnit) | as regras de dependência entre camadas de §1.2 |
| `PermissionsConsistencyTest` | `x-authorize` do YAML ↔ enum de permissão |
| `ExceptionCodeConsistencyTest` | todo código de erro tem chave nos bundles PT e EN e um `title` válido |

---

## 8. Verificação automatizada (build)

O build **DEVE** falhar em qualquer uma destas condições:

| Ferramenta | Verifica |
|---|---|
| Spotless | formatação, LF, cabeçalho de licença, ordem de imports, sem wildcard |
| NullAway | violação de `@NonNull` / `@Nullable` (§3.3) |
| ArchUnit | regras de camada (§1.2) |
| Maven Enforcer | convergência de dependência (já em uso) |
| Testes de consistência | §7.3 |

Nenhuma dessas verificações **DEVE** ser desabilitada por módulo sem registro no PRD de
Padronização.

---

## 9. Checklist — novo endpoint

1. Atualizar/criar o contrato em `etc/api/organization/*.yml` (**contrato antes do código**)
2. Adicionar a permissão ao enum se houver `x-authorize` novo
3. Criar/estender a tabela via changeSet Liquibase novo
4. Entidade (sem setter) + Repository em `domain/<bc>/<agregado>/internal/`
5. `Input`/`Output`/`Filter` em `dto/`, interface em `specification/`, `ServiceBean`
   package-private + `Mapper` em `service/`
6. Códigos no enum do bounded context + chaves nos bundles PT e EN
7. Use Case: interface pública + `Bean` package-private, com a transação na classe
8. Delegate em `api/delegate/<agregado>/`
9. Teste unitário do `Bean` + teste de integração independente
10. `mvn clean install` na raiz (o generator recria controller/DTO a partir do YAML)

## 10. Checklist — revisão de PR

- [ ] Contrato OpenAPI atualizado antes do código
- [ ] Cabeçalho de licença, LF, 4 espaços, sem import wildcard (Spotless passou)
- [ ] Nome completo e corretamente grafado; sufixo de papel correto
- [ ] `private final` + `@RequiredArgsConstructor`; sem `@Autowired` em campo
- [ ] `@NonNull` / `@Nullable` em todo parâmetro público, e nenhum caller passando null em `@NonNull`
- [ ] `*Bean` package-private (use case **e** domain service)
- [ ] `@Transactional` **só** no `UseCaseBean`; domain service com `propagation = MANDATORY`
- [ ] Entidade sem `@Setter`; mutação por método de negócio
- [ ] Erro via `ScosException` + código semântico do catálogo, com chave PT e EN
- [ ] Filtro opcional como record `*Filter`, não parâmetro anulável solto
- [ ] Mapeamento por MapStruct
- [ ] Teste unitário com BDDMockito + AssertJ; teste de integração independente
- [ ] Sem `LocalDateTime`, sem `.now()` estático, sem `System.out`

---

## 11. Estado atual vs. alvo

Itens marcados 🔄 acima já são obrigatórios para código novo e têm migração planejada no
**PRD de Padronização e Conformidade de Código**. Resumo do que ainda diverge hoje:

| Regra | Divergência atual |
|---|---|
| `*ServiceBean` package-private | 9 de 17 estão `public` |
| Transação só no use case | `@Transactional` duplicado em ambas as camadas |
| Entidade sem `@Setter` | todas as entidades expõem setters |
| Nulidade honesta | `@NonNull` recebendo `orElse(null)` em pelo menos um caminho |
| Catálogo por contexto, nome semântico | enum único global com nome numérico |
| MapStruct nas duas camadas | `*ApiMapper` manual estático na camada de use case |
| Verificação automatizada | Spotless, NullAway e ArchUnit ainda não existem no build |
| LF e cabeçalho de licença | 37 arquivos em CRLF, 21 sem cabeçalho |
| Nomes | `PaginatioUtils`, pacote `enumaration` |
| Teste de integração independente | suíte de Company depende de seed fixo e de cache não limpo |
