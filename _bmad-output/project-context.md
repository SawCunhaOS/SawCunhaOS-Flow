---
project_name: 'SawCunhaOS-Organization'
user_name: '_SawCunhaOS'
date: '2026-07-15'
sections_completed:
  ['technology_stack', 'language_rules', 'framework_rules', 'testing_rules', 'quality_rules', 'workflow_rules', 'anti_patterns']
status: 'complete'
rule_count: 120
existing_patterns_found: 12
optimized_for_llm: true
---

# Project Context for AI Agents

_Este arquivo contém regras e padrões críticos que agentes de IA devem seguir ao implementar código neste projeto. Foco em detalhes não óbvios que os agentes podem deixar passar._

---

## Technology Stack & Versions

**Fonte de verdade = parent `br.com.sawcunhaos:scos-bom:1.3.1`.**
NUNCA declarar `<version>` para o que o BOM gerencia — e NÃO duplicar o número aqui.
Precisa da versão exata? Leia o BOM:
`~/.m2/repository/br/com/sawcunhaos/scos-bom/1.3.1/scos-bom-1.3.1.pom`

| Camada | Tecnologia | Versão |
|---|---|---|
| Linguagem | Java | **25** (`release=25`) — pinado no pom raiz |
| Framework | Spring Boot / Framework / Cloud / Data | **4.X.X** — via BOM |
| Persistência | PostgreSQL **18+**, Liquibase, QueryDSL, hypersistence-utils | via BOM |
| Mapeamento | MapStruct | **1.6.3** — pinado no pom raiz (`org.mapstruct.version`) |
| | Lombok | via BOM (`${lombok.version}`) |
| Nulidade | jspecify (`@NonNull`) | via BOM |
| Contrato | openapi-generator-maven-plugin | **7.17.0** — pinado no `pluginManagement` raiz |
| | maven-failsafe-plugin | **3.2.5** — pinado no `pluginManagement` raiz |
| Foundation | `scos-foundation` | **1.2.0-SNAPSHOT** — pinado no pom raiz |
| Testes | JUnit, Mockito, Testcontainers, WireMock | via BOM |
| | GrpcMock | **1.1.1** — pinado inline no pom do `boot` (fora do BOM) |
| RPC | gRPC / protobuf | via BOM |

**Infra:** Keycloak (OAuth2/JWT), Redis (cache + idempotência jDempotent), PostgreSQL, gRPC, Paketo buildpacks.

**Restrições que agentes quebram:**

- `com.google.errorprone:error_prone_annotations` **fixado em 2.48.0** no `dependencyManagement` raiz. Enforcer `RequireUpperBoundDeps` falha sem isso (grpc-stub traz 2.45.0 como nearest; guava/gson pedem maior). Não remover.
- Lombok **excluído** do fat jar (`spring-boot-maven-plugin` no pom raiz).
- `spring-boot-maven-plugin` com `<skip>true</skip>` em `api`; só `boot` empacota (`finalName=ScosOrganizationApplication`).
- `boot` exige `--add-opens java.base/{java.util,java.lang,java.time,sun.misc}=ALL-UNNAMED` — presente em `jvmArguments` E no `argLine` do surefire. Mexeu no surefire, mantenha.
- Imagem via buildpacks Paketo, builder **pinado em tag** (`builder-noble-java-tiny:0.0.153`). NUNCA `latest` (RF-02). `BP_JVM_VERSION=25`, `BP_JVM_TYPE=JRE`.
- Repo de SNAPSHOT: Central Portal (`central-portal-snapshots`), releases desabilitado.

## Critical Implementation Rules

### Language-Specific Rules (Java 25)

- **Nulidade:** `org.jspecify.annotations.NonNull` em parâmetros de métodos públicos de Use Case e domain service. NÃO usar `lombok.NonNull` nem `jakarta.validation.constraints.NotNull` para esse fim.
- **Lombok obrigatório** — não escrever getter/setter/construtor à mão:
  - Entidades/POJOs: `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`
  - Beans Spring: `@RequiredArgsConstructor` + campos `private final` (injeção por construtor). NUNCA `@Autowired` em campo, nunca `new` para bean gerenciado.
  - Log: `@Slf4j`. Nunca declarar `Logger` manualmente.
  - Enums de constantes: `@AllArgsConstructor(access = AccessLevel.PRIVATE) @Getter`
- **`@Builder.Default`** obrigatório em campo com inicializador quando a classe usa `@Builder` (ex.: `private boolean active = true`) — sem isso o builder gera `false`/`null`.
- **Imports:** um por classe, sem wildcard (`import a.b.*`). Imports estáticos são usados para constantes de `ExceptionCodeError` dentro de entidades.
- **Cabeçalho de licença Apache 2.0** em TODO arquivo `.java` novo — copiar o bloco de comentário do topo dos arquivos vizinhos.
- **Records** para DTOs de domínio (`DepartmentInput`/`DepartmentOutput`) — acesso via `.code()`, não `.getCode()`. DTOs gerados do OpenAPI também são records com builder.
- **Exceções:** só `ScosException` (de `foundation.utils.exception`) com constante de `ExceptionCodeError`. NUNCA `RuntimeException`/`IllegalArgumentException` cru, nunca mensagem em string literal. Detalhes em *Erros (RFC 9457)*.

#### Tipos temporais (obrigatório)

| Pergunta que o dado responde | Tipo Java | Coluna |
|---|---|---|
| "Em que instante aconteceu?" (fato) | `Instant` | `TIMESTAMPTZ` |
| "Que dia?" (calendário) | `LocalDate` | `DATE` |
| "Que hora do relógio?" (jornada/template) | `LocalTime` | `TIME` |

`LocalDateTime` é **proibido** no domínio — toda coluna de instante do schema é `TIMESTAMPTZ`, logo `Instant` é o único tipo Java correto para ela. `.now()` também é **proibido** no domínio (entidade ou service) — o "agora" só entra via `Clock` injetável (bean `Clock.systemUTC()` em produção; `Clock.fixed(...)` em teste, determinístico, sem `sleep`). Exceção: `@CreationTimestamp`/`@UpdateTimestamp` do Hibernate, que gerenciam o "agora" fora do código Java e são compatíveis com `Instant`.

**Débito conhecido, fora do controle deste projeto:** `BaseEntity` (biblioteca externa `scos-foundation-utils`) ainda expõe `createdAt`/`updatedAt` como `LocalDateTime` — toda entidade que estende `BaseEntity` (`ScosSystem`, `Login`, `Resource`, etc.) herda esse tipo mesmo depois de qualquer padronização feita neste projeto. Só corrigível atualizando a foundation.

### Framework-Specific Rules

#### Fluxo OpenAPI — NÃO ÓBVIO, erro mais comum

Contrato primeiro: editar `etc/api/organization/*.yml` ANTES de qualquer código.

A geração é **dividida entre dois módulos** — quem gera o quê:

| Módulo | Config | Gera |
|---|---|---|
| `flow-organization-usecase` | `generateModels=true`, `generateApis=false` | **DTOs** → `br.com.sawcunhaos.organization.api.dto` |
| `flow-organization-api` | `generateModels=false` (APIs por padrão) | **interfaces** `XxxApiDelegate` → `br.com.sawcunhaos.organization.api.controller` |

Consequências que agentes erram:

- DTO novo no YAML → recompilar **`usecase`**, não `api`.
- Spec nova? Precisa de execution do plugin nos **DOIS** poms (`api` E `usecase`).
- Código gerado em `target/generated-sources/openapi/` — NUNCA editar à mão.
- Templates mustache customizados (`<templateResourcePath>mustaches</templateResourcePath>`, vindos de `scos-foundation-utils`). São eles que geram record + builder.

#### Fronteiras de módulo (dependências reais)

```
boot → api, usecase, domain, infrastructure   (único que empacota)
api  → usecase (COM exclusão explícita de domain), foundation-utils/exception
usecase → domain, foundation-utils/exception
domain → foundation
shared → foundation (ExceptionCodeError + bundle de mensagens)
```

- **`api` NÃO enxerga `domain`** — a exclusão é intencional no pom. Delegate não pode importar entidade/domain service. Se precisar, o Use Case é que traduz.
- **Use Case usa DTO da API na assinatura pública** (`CreateDepartmentUseCase.execute(CreateDepartmentRequest)` retorna `api.dto.Department`). É o padrão vigente — não "corrigir" para DTO próprio.
- Use Case converte `api.dto.*` ⇄ `domain.<bc>.<agregado>.dto.*Input/*Output` no Bean.

#### Camadas — o que vai onde

- **Delegate** (`api/delegate/<agregado>/XxxDelegate implements XxxApiDelegate`, `@Component`): fino. Só chama Use Case e monta a resposta. Zero regra de negócio. Retorno `Void` → `return null` (ex.: 204).
- **Use Case** (`usecase/<bounded-context>/<agregado>/`): interface **pública** + `XxxUseCaseBean` **package-private** `@Service`, mesmo pacote. Orquestra, não decide.
- **Domain service** (`domain/<bc>/<agregado>/specification/` = interface, `service/XxxServiceBean` = impl): regra que cruza entidades/validações.
- **Entidade** (`domain/<bc>/<agregado>/internal/`): regra do próprio estado (`activate()`, `deactivate()` lançando `ScosException`). Estende `BaseEntity` (de `foundation.utils.entity`), anotada `@Auditable`.
- **Bounded contexts ativos:** `corporate/` (company, department, employee, position, catalog), `access/` (login, profile, resource, system, status), `configuration/`, `outbox/`.

#### Transações

- `@Transactional(rollbackFor = ScosException.class)` nos Use Case Beans de escrita.
- `@Transactional(readOnly = true)` em domain services de leitura.
- `org.springframework.transaction.annotation.Transactional` (não a do Jakarta).

#### Repositórios

- Em `domain/<bc>/<agregado>/internal/`, `@Repository`.
- Estende `BaseJpaRepository<T, ID>` + `JpaSpecificationExecutor<T>` + `QuerydslPredicateExecutor<T>`.
- Consultas de existência/dinâmicas via **QueryDSL** (`BooleanBuilder`, `QXxx.xxx`), como `default` method na própria interface. Sem JPQL concatenado.
- Relações `@OneToMany` sempre `FetchType.LAZY`.

#### Liquibase (módulo `boot`, `resources/db/changelog/`)

- Só **YAML** para changelog; `sql` apenas para view/function/procedure/trigger.
- `vX.Y.Z/` contém APENAS `tables/` e `indexes/`; `function/`, `view/`, `procedure/`, `triggers/` ficam na **raiz** (evoluem fora da versão, `runOnChange: true`).
- Todo changeSet: `id` único + `author` + **`rollback` obrigatório** + `tagDatabase`.
- Nomes: tabela `SCOS_<ENTIDADE>`, PK `PK_SCOS_<T>`, FK `FK_<COL>_SCOS_<T>`, UK `UK_<COL>_SCOS_<T>`, sequence renomeada para `SEQ_<COL>`.
- Auditoria é application-side: colunas `CREATED_AT`, `UPDATED_AT`, `USER_AT`.

#### Erros (RFC 9457)

- Código: `SCOS_<MÓDULO>_<NNN>` (regex `^SCOS_[A-Z0-9]+_\d{3}$`), incremental por módulo.
- Constante em `shared/exception/ExceptionCodeError` carregando `httpCode` + `title` (`SCOS_TITLE_NOT_FOUND`=404, `_CONFLICT`=409, `_BUSINESS_RULE_VIOLATION`=422, `_EXTERNAL_INTEGRATION_FAILURE`=502, `_INTERNAL_ERROR`=500, `_UNAUTHORIZED`=401, `_GENERIC`=400).
- Mensagem PT-BR **e** EN em `shared/src/main/resources/scos_message_organization[_en].properties`.
- Resposta serializada pelo `ExceptionsHandler` do `scos-foundation-exception`.

#### Segurança / permissões

- `x-authorize` no YAML → constante **obrigatória** em `infrastructure/enumaration/ScosOrganizationPermission`.
- Permissão: `ACTION_RESOURCE` (`CREATE_COMPANY`). Feature: `ORGANIZATION_<DOMÍNIO>_<SCOPE>`. Cache: `SCOS_ORGANIZATION_<TAG>`.
- Leitura → `*_VIEW` + `ORGANIZATION_VIEW`; escrita → `*_MANAGEMENT` + `ORGANIZATION_MANAGEMENT`; sensível (senha/bloqueio) → `ORGANIZATION_EMPLOYEE_LOGIN_MANAGEMENT` + `ORGANIZATION_MANAGEMENT`.

#### Resposta OpenAPI

- `200` → schema próprio com wrapper `data:`
- `201` → `$ref: '../ScosComponents.yml#/components/responses/201_CREATED'`
- `204` → `$ref: '../ScosComponents.yml#/components/responses/204_NO_CONTENT'`
- `4XX`/`5XX` → `$ref` para `ScosComponents.yml`

### Testing Rules

#### Onde cada teste mora

| Módulo | Tipo | Contagem atual |
|---|---|---|
| `usecase` | unitário (Mockito) — `XxxUseCaseBeanTest` | 66 |
| `domain` | unitário (Mockito) — `XxxServiceBeanTest` | 13 |
| `boot` | **integração** — `XxxControllerTest` | 14 |
| `infrastructure` | contrato — `PermissionsConsistencyTest` | 1 |
| `shared` | unitário | 1 |
| `api`, `grpc/*` | — | **0 (lacuna)** |

- Teste unitário: JUnit 5 + Mockito, padrão **Given/When/Then**, `@DisplayName`.
- Nome espelha a classe: `CreateDepartmentUseCaseBean` → `CreateDepartmentUseCaseBeanTest`, mesmo pacote.

#### Contrato OpenAPI ⇄ permissões — `PermissionsConsistencyTest`

Em `infrastructure`, lê os YAMLs de `../etc/api/organization` com snakeyaml (sem dependência nova; `junit-jupiter` e `snakeyaml` já estavam no pom). Três garantias:

1. Toda permissão em `x-authorize` tem constante no enum → **falha** (403 permanente se faltar).
2. Toda operation declara `x-authorize` → **falha** (endpoint público se faltar).
3. Constante do enum sem uso em nenhum `x-authorize` → **só avisa** via `System.out`, não falha. Pode ser reservada ou consumida fora do OpenAPI (ex.: gRPC). Hoje avisa 5 `DELETE_*`.

`ScosComponents.yml` é excluído: é biblioteca de componentes, seu `GET /` é exemplo ilustrativo sem execution em nenhum módulo. Se `Files.list` não achar spec, o teste **falha** em vez de passar vazio.

Mexeu em `x-authorize`? Rode: `mvn -pl flow-organization-infrastructure test -Dtest=PermissionsConsistencyTest -Denforcer.skip=true` (o `-Denforcer.skip` é necessário só enquanto o build estiver quebrado — ver *Build quebrado*).

#### Integração — `ScosOrganizationTestUtil` (LEIA ANTES DE ESCREVER)

Toda classe de integração **estende `ScosOrganizationTestUtil`** e só adiciona `@Test`. NÃO re-anotar a subclasse. A base já traz `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")` + `@Sql`.

Stack real levantado: Postgres + Redis (ComposeContainer de `etc/infra/*.yml`), WireMock :7080 (Keycloak/JWT), GrpcMock :8090 (registry/authority). Caminho exercitado: MockMvc → filtros JWT → `@PreAuthorize` → delegate → use case → domain → Postgres real (triggers/views).

**Armadilhas — cada uma já causou bug:**

- **Containers são `static` singleton, NÃO `@Container`/`@Testcontainers`.** O compose publica portas fixas (5432/6379); reiniciar por classe → conflito de porta e "relation does not exist" (contexto Spring é cacheado, Liquibase roda só na 1ª). Não adicione `@Testcontainers` na subclasse.
- **Exige surefire em JVM único** (`forkCount=1`, `reuseForks=true` — padrão). Paralelizar a suíte quebra o singleton.
- **`@Sql` isola por método:** `setsup_database.sql` (BEFORE) recria o seed, `delete_all.sql` (AFTER) faz `TRUNCATE ... RESTART IDENTITY` → IDs voltam determinísticos.
- **Redis/jDempotent NÃO é resetado entre métodos.** Toda criação/atualização bem-sucedida precisa de `code` **único** por teste, senão o jDempotent devolve a resposta cacheada do payload anterior. Causa falha fantasma.
- Path do MockMvc é relativo ao servlet — **sem** o context-path `/organization` (ex.: `/api/v1/departments`).
- Cache Spring de autoridades (`scos:authority:ctx`) é limpo pela base via `CacheManager`.

#### Nomenclatura de teste de integração — DÍVIDA CONHECIDA

Chamam-se `*ControllerTest` → rodam no **surefire** (fase `test`), não no failsafe. `maven-failsafe-plugin` está configurado mas **não tem alvo `*IT`**. Ao criar teste novo de integração, siga o padrão vigente (`*ControllerTest`) para não fragmentar. Migrar para `*IT` + failsafe é decisão de suíte inteira — nunca metade.

#### O que asserir em teste de integração

Cenário de falha valida o ProblemDetail (RFC 9457) inteiro: `type`, `title`, `status`, `detail`, `instance` + propriedades `code` / `requestId` / `timestamp` (+ `errors` na validação de campos). `detail`/`title` são comparados em **PT-BR** literal. Códigos transversais: `SCOS-001` (validação), `SCOS-004` (acesso negado).

### Code Quality & Style Rules

#### Estrutura de pastas (obrigatória, por agregado)

```
domain/<bounded-context>/<agregado>/
├── internal/       entidades JPA, enums, repositórios (acesso direto ao banco)
├── dto/            XxxInput / XxxOutput (records)
├── specification/  interface do domain service
└── service/        XxxServiceBean (impl)
```

`internal/` é fronteira: nada fora do agregado importa de `internal/` de outro agregado sem passar pelo `specification/`.

#### Nomenclatura

| Artefato | Padrão | Exemplo |
|---|---|---|
| Delegate | `<Agregado>Delegate` | `DepartmentDelegate` |
| Use Case | `<Verbo><Agregado>UseCase` + `...Bean` | `CreateDepartmentUseCase(Bean)` |
| Domain service | `<Agregado>Service` + `<Agregado>ServiceBean` | `DepartmentServiceBean` |
| Entidade | singular, tabela `SCOS_<ENTIDADE>` | `Department` → `SCOS_DEPARTMENT` |
| DTO domínio | `<Agregado>Input` / `<Agregado>Output` | `DepartmentInput` |
| Código de erro | `SCOS_<MÓDULO>_<NNN>` | `SCOS_DEPARTMENT_001` |
| Spec OpenAPI | `ScosOrganization_<Tag>.yml` | `ScosOrganization_Company.yml` |
| Teste | `<ClasseTestada>Test` | `DepartmentServiceBeanTest` |

Verbos de Use Case em uso: `Create`, `Update`, `Find`, `FindAll`, `Enable`, `Disable`. Reutilize — não invente `Get`/`List`/`Remove`.

#### Package base

`br.com.sawcunhaos.organization` — SEMPRE. `br.com.sawcunhaos.foundation.*` é biblioteca externa (não criar classe lá).

#### Idioma

- Código, nome de classe/método/variável: **inglês**.
- Javadoc, comentário, mensagem de erro ao usuário, doc em `etc/`: **PT-BR** (bundle `_en` existe em paralelo para mensagens).

#### Documentação

- Javadoc em regra de negócio deve declarar o erro: `@throws ScosException SCOS_DEPARTMENT_004 se já estiver ativo.`
- Classe de teste de integração carrega Javadoc explicando infra levantada, isolamento e dados do seed usados. Mantenha ao editar — é o que evita quebrar o singleton.
- Comentário explica **por quê** (ex.: o bloco no pom raiz explicando o pin do `error_prone_annotations`), nunca o quê.

#### Formatação (`.editorconfig`)

LF, `insert_final_newline`, UTF-8. `.java`/`pom.xml`: 4 espaços. `.yml`: 2 espaços, **sem** newline final. `.mustache`: 4 espaços, sem newline final.

> Não há Checkstyle/Spotless ativo. O BOM declara `checkstyle-maven-plugin` + `scos-build-config`, mas nenhum módulo o executa — estilo hoje é convenção, não gate.

### Development Workflow Rules

#### Ordem obrigatória para endpoint novo

1. **YAML primeiro** — `etc/api/organization/*.yml` (contrato antes do código)
2. Execution do openapi-generator nos poms de `api` **e** `usecase` (se spec nova)
3. Delegate em `api/.../delegate/<agregado>/`
4. Use Case (interface + Bean package-private) em `usecase/.../<bc>/<agregado>/`
5. Domain service + entidade em `domain/.../<bc>/<agregado>/`
6. `ExceptionCodeError` + mensagens PT-BR/EN em `shared`
7. Se novo `x-authorize`: constante em `ScosGeotemporalPermission` + descrição em `messages_geotemporal_permission.properties` **e** `_en` (os 3 são 1:1) → validar com `PermissionsConsistencyTest`
8. Changelog Liquibase em `boot` (com rollback)
9. Teste unitário (`usecase`/`domain`) + integração (`boot`)

#### Git

- Branch principal: **`develop`** (não `main`). PR alveja `develop`.
- Branch de trabalho: `feature/<assunto>` (ex.: `feature/organization`, `feature/bmad`).
- Commit: prefixo Conventional (`feat:`, `fix:`) + descrição em **PT-BR**. O histórico atual concatena vários assuntos num commit — **não replicar**; um assunto por commit.

#### Build

```bash
mvn clean install              # raiz — obrigatório após mexer em YAML de spec
mvn test                       # unitários + integração (surefire, JVM único)
```

- ⚠️ **Ambos falham hoje** no enforcer (ver *Build quebrado* em Critical Don't-Miss). Contorno temporário: `-Denforcer.skip=true`.
- Testes de integração exigem **Docker rodando** (ComposeContainer sobe Postgres/Redis).
- Mexeu em spec OpenAPI? `mvn clean` no módulo afetado — `target/generated-sources` fica velho e o erro aparece como "símbolo não encontrado" em DTO.

#### Configuração / segredos

- Perfis: `application.yml`, `application-dev.yml`, `bootstrap.yml` (Spring Cloud Config). Perfil `test` para integração.
- `.env.example` versionado; `.env` NÃO. Vars documentadas em `etc/doc/config-env-vars.md`.
- Infra local: `etc/infra/docker-compose-{database,redis,keycloak,grafana}.yml`.
- Segredos via Jasypt — nunca literal no YAML.

#### O que NÃO existe (não assuma gate)

- **Sem CI.** `pom.xml` declara `ciManagement` = GitHub Actions, mas `.github/workflows` não existe. Nada roda automaticamente no push/PR.
- Sem PR template, CODEOWNERS ou git hooks.

### Critical Don't-Miss Rules

#### ⚠️ Documentação do projeto que está ERRADA (verifique, não confie)

| Fonte | Afirma | Realidade |
|---|---|---|
| `api-development-guidelines.md` | DTOs gerados em `flow-organization-api/target/...` | Gerados em **`flow-organization-usecase`** (`generateModels=true` lá; `false` em `api`) |
| `api-development-guidelines.md` | Exemplo `DepartmentDomainService` | Nome real: `DepartmentServiceBean implements DepartmentService` (specification+Bean) |
| `pom.xml` (`ciManagement`) | GitHub Actions | Sem `.github/workflows` |
| `pom.xml` (raiz) | Build íntegro | **`mvn clean install` NÃO passa** — enforcer reprova (ver *Build quebrado*) |

Ao seguir um doc do `etc/`, confirme no código antes. Achou drift novo? Corrija o doc.

#### 🔴 Build quebrado — pré-existente

`maven-enforcer-plugin:3.6.3` reprova `RequireUpperBoundDeps` em `flow-security-starter` e `flow-organization-infrastructure`:

- `org.hibernate.orm:hibernate-core:7.4.1.Final`
- `io.prometheus:prometheus-metrics-{core,exposition-formats,tracer-common}:1.5.1` (micrometer-registry-prometheus pede 1.7.0)
- `com.squareup.okio:okio:3.4.0` (grpc-okhttp) vs `3.6.0` (okhttp via zipkin)
- `com.google.errorprone:error_prone_annotations`

Hoje só se roda teste com `-Denforcer.skip=true`. **Não use esse flag para "resolver"** — é máscara. A convergência precisa ser fixada no `dependencyManagement`, como já foi feito com `error_prone_annotations:2.48.0`.

#### Anti-padrões — NÃO faça

- ❌ Editar arquivo em `target/generated-sources/` (some no próximo build).
- ❌ Codificar delegate/use case antes de atualizar o YAML da spec.
- ❌ Importar `domain` a partir de `flow-organization-api` (exclusão intencional no pom).
- ❌ `@Autowired` em campo, ou `new` para bean gerenciado → use `@RequiredArgsConstructor`.
- ❌ Regra de negócio no delegate → entidade (estado próprio) ou domain service (cruza entidades).
- ❌ `throw new RuntimeException("msg")` → `ScosException(ExceptionCodeError.SCOS_X_NNN)`.
- ❌ Código de erro novo sem mensagem PT-BR **e** EN no bundle → resposta vaza a chave crua.
- ❌ `@Testcontainers`/`@Container` em teste de integração → conflito de porta (ver Testing Rules).
- ❌ Reusar `code` entre testes de integração → jDempotent devolve resposta cacheada.
- ❌ changeSet Liquibase sem `rollback`.
- ❌ Declarar `<version>` de dependência gerida pelo BOM.
- ❌ Remover o pin de `error_prone_annotations:2.48.0` → enforcer quebra o build.
- ❌ `builder:latest` do Paketo (RF-02) — tag pinada sempre.
- ❌ `LocalDateTime` em campo de domínio, ou `.now()` direto em entidade/service → `Instant` + `Clock` injetável (ver *Tipos temporais*).

#### Segurança

- Endpoint novo **sem** `x-authorize` no YAML fica sem `@PreAuthorize` → **exposto**. Guardado por `PermissionsConsistencyTest`.
- `x-authorize` só é efetivo se a constante existir em `ScosGeotemporalPermission` E o papel estiver no realm do Keycloak (`Scos_Realm.json`) e no seed do DB (`etc/database/seed_data.sql`). Falta de sincronia = 403 em runtime, silencioso em compile.
- `PermissionsConsistencyTest` guarda **nome**, não propagação: ele prova que a constante existe, NÃO que o papel chegou ao Keycloak/seed. Essa parte segue manual.
- Permissão nova = 3 lugares, sempre: constante no enum + entrada em `messages_geotemporal_permission.properties` **e** `_en`. Os três são 1:1 (129/129/129 hoje).
- PII: mascarar em log via `scos-foundation-privacy` (`%mask`/`%maskmdc`). Nunca logar documento/senha cru. Entidade com PII → `@Auditable` + `auditEncryptFields`.
- Segredo só via Jasypt/env. Nunca literal em YAML ou commit.

#### Gotchas de performance

- `@OneToMany` LAZY + serialização → N+1. Use QueryDSL/`JpaSpecificationExecutor` com projeção, ou hypersistence-utils. Nunca `EAGER` para "resolver" lazy init.
- Cache Redis (`scos:authority:ctx`, `SCOS_ORGANIZATION_<TAG>`) sobrevive entre testes e entre requests — invalidação errada gera bug fantasma.
- Postgres tem views/triggers versionadas em `changelog/{view,triggers}/`. Mudou o schema? Confira se a view acompanha — o teste de integração roda contra o banco real e pega isso; H2 (usado em `usecase`/`api`) não pega.

---

## Usage Guidelines

**Para agentes de IA:**

- Leia este arquivo ANTES de implementar qualquer código.
- Siga TODAS as regras exatamente como documentadas.
- Na dúvida, prefira a opção mais restritiva.
- Este arquivo vence quando conflitar com os docs em `etc/` — veja *Documentação do projeto que está ERRADA*. Se o código contradisser este arquivo, o **código** vence: corrija este arquivo.
- Novo padrão emergiu? Atualize aqui.

**Para humanos:**

- Mantenha enxuto e focado no que o agente precisa — regra óbvia de Java/Spring não entra.
- Atualize quando o stack ou os padrões mudarem.
- Revise periodicamente; remova regra que virou óbvia.
- **Versões:** o BOM (`scos-bom`) é a fonte de verdade. Não pine número aqui — foi exatamente assim que o drift "Spring Boot 4.0.x" nasceu.

**Dívidas registradas (não são regras — são o que falta):**

1. 🔴 **Build quebrado:** enforcer `RequireUpperBoundDeps` reprova `flow-security-starter` e `infrastructure`. Nada compila sem `-Denforcer.skip=true`. Bloqueia todo o resto.
2. Sem CI (`.github/workflows` ausente). Enquanto não houver, `PermissionsConsistencyTest` só protege quem lembrar de rodá-lo.
3. `api`, `grpc/*` sem testes.
4. Testes de integração como `*ControllerTest` no surefire; failsafe sem alvo.
5. `etc/architecture/api-development-guidelines.md` desatualizado em 2 pontos (DTOs, `DepartmentDomainService`).
6. 5 permissões `DELETE_*` órfãs no enum (sem `x-authorize`): remover do enum ou criar os endpoints.
7. As 32 permissões adicionadas em 2026-07-15 podem não estar no realm do Keycloak nem no seed — não verificado.
8. `BaseEntity` (foundation externa) ainda usa `LocalDateTime` para `createdAt`/`updatedAt` herdados por toda entidade — fora do controle deste projeto até a foundation ser atualizada (ver *Tipos temporais*).

**Resolvido em 2026-07-15:** `PermissionsConsistencyTest` criado (`infrastructure`); 32 permissões que estavam em `x-authorize` sem constante no enum foram adicionadas (97 → 129), com i18n pt/en. Eram 403 permanente esperando quem chamasse.

Last Updated: 2026-07-19
