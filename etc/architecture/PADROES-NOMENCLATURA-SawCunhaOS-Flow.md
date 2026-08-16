# Padrões de Nomenclatura — SawCunhaOS-Flow

> **Documento normativo.** Define como nomear módulos, pacotes, classes, membros, contratos,
> tabelas e testes no monorepo `flow`. Anexo de `PADROES-CODIGO-SawCunhaOS-Flow.md` (v2), que
> trata da forma do código; este trata do nome.
>
> Versão 1 — 2026-07-31. Marcadores: **DEVE** / **NÃO DEVE** / **DEVERIA** / **PODE**.
> 🔄 **Migração** = regra obrigatória para nome novo, com desvio conhecido no código atual
> (inventário na seção 15).

---

## 1. Princípios

Sete regras que valem para qualquer identificador do projeto:

1. **Inglês.** Todo identificador (módulo, pacote, classe, método, campo, coluna, enum, chave de
   contrato) **DEVE** estar em inglês. PT-BR só em Javadoc, comentário, `@DisplayName`, `comment`
   de changeSet e mensagem ao usuário.
2. **Palavra completa.** **NÃO DEVE** abreviar (`Pos`, `Dept`, `Cfg`, `Qty`). Exceções fechadas,
   e apenas essas: `id`, `pk`, `fk`, `dto`, `api`, `grpc`, `jwt`, `url`, `uri`, `http`.
3. **Ordem modificador + núcleo.** O núcleo é a última palavra. `CompanyStatus` é o status de uma
   empresa; `StatusCompany` não significa nada em inglês. 🔄 **Migração**
4. **Singular.** Classe, entidade, tabela, pacote e enum no singular (`Position`, `SCOS_POSITION`,
   `domain.corporate.position`). Plural apenas em rota REST (`/v1/positions`) e em coleção
   nomeada (`GetAllPositionsResponse`).
5. **Um conceito, um nome.** O termo canônico da seção 14 **DEVE** ser usado em todas as camadas,
   do YAML ao nome da coluna. Sinônimo é desvio, não estilo.
6. **O sufixo carrega o papel.** O leitor **DEVE** saber a camada e a responsabilidade pelo nome,
   sem abrir o arquivo. A tabela de sufixos da seção 5 é fechada.
7. **Nome errado se corrige na hora.** Erro de grafia em identificador público é bloqueante em
   revisão de PR, mesmo que "já esteja assim em outro lugar".

---

## 2. Módulos Maven

### 2.1 Gramática

```
<produto>-<contexto>-<camada>
```

- `<produto>`: `flow`, sempre
- `<contexto>`: o bounded context ou capacidade (`organization`, `security`, `notification`)
- `<camada>`: o papel arquitetural (`domain`, `usecase`, `api`, `boot`, `infrastructure`,
  `shared`, `resources`, `grpc-proto`, `grpc-boot`, `starter`)

Exemplos válidos: `flow-organization-domain`, `flow-organization-grpc-boot`,
`flow-security-starter`.

### 2.2 Regras

- Nome de diretório **DEVE** ser idêntico ao `artifactId`
- `artifactId` **DEVE** ser kebab-case minúsculo
- Módulo agregador (`packaging: pom`) **PODE** usar só o contexto (`organization`,
  `infrastructure`, `notification`, `geotemporal`, `server-fat`)
- Todo módulo **DEVE** declarar `<name>` legível e `<description>` de uma frase
- `<name>` **DEVE** corresponder ao `artifactId`. 🔄 **Migração**: `flow-security-starter` tem
  `<name>Scos Organization Security Start</name>`, que não bate com o artefato nem é uma frase
  completa
- Referência cruzada entre módulos **DEVE** usar o `artifactId` real. 🔄 **Migração**:
  `flow-organization-grpc-boot` declara dependência de `flow-security-starter`, artefato que não
  existe no monorepo com esse nome
- Camada nova (um `<camada>` fora da lista acima) **DEVE** ser acordada antes de criada

### 2.3 groupId

| Escopo | groupId |
|---|---|
| Raiz do monorepo | `br.com.sawcunhaos` |
| Módulos de produto | `br.com.sawcunhaos.flow` |

---

## 3. Pacotes

### 3.1 Gramática

```
br.com.sawcunhaos.<contexto>[.<subcontexto>].<camada>[.<bounded-context>].<agregado>.<papel>
```

Pacotes-raiz permitidos:

| Raiz | Uso |
|---|---|
| `br.com.sawcunhaos.organization` | o módulo de produto `organization` |
| `br.com.sawcunhaos.flow` | infraestrutura técnica compartilhada do monorepo |
| `br.com.sawcunhaos.security.starter` | a biblioteca de segurança reusável |

Raiz nova **DEVE** ser acordada. 🔄 **Migração**: existe um quarto raiz,
`br.com.sawcunhaos.foundation.security`, usado apenas por dois testes do `flow-security-starter`,
cujo código principal vive em `br.com.sawcunhaos.security.starter`. Teste **DEVE** ficar no mesmo
pacote da classe testada.

### 3.2 Regras

- Minúsculo, sem underscore, sem camelCase, sem número: `reasonactivate`, não `reason_activate`
  nem `reasonActivate`
- Singular: `position`, não `positions`
- Segmento de agregado composto vira palavra única e minúscula (`addresstype`, `contacttype`,
  `legalnature`, `reasoninactivate`)
- Papéis de domínio são fixos e não se inventa outro: `internal`, `dto`, `service`,
  `specification`, `rules`
- Nome de pacote **DEVE** estar corretamente grafado. 🔄 **Migração**:
  `infrastructure.enumaration` (correto: `enumeration`)

### 3.3 Mapa de pacotes por camada

| Camada | Pacote |
|---|---|
| Delegate REST | `organization.api.delegate.<agregado>` |
| DTO/Controller gerados | `organization.api.dto` / `organization.api.controller` |
| Use case | `organization.application.usecase.<bc>.<agregado>` |
| Domínio | `organization.domain.<bc>.<agregado>.{internal,dto,service,specification}` |
| Infra do módulo | `organization.infrastructure.<assunto>` |
| Compartilhado do módulo | `organization.shared.{exception,validation,converter,utils}` |
| Composition root REST | `organization.boot` |
| Composition root gRPC | `organization.grpc.boot[.delegate,.configuration]` |
| Contratos gRPC gerados | `organization.grpc.proto` |

---

## 4. O prefixo `Scos`

O prefixo não é decorativo e **NÃO DEVE** ser aplicado por hábito. Existem exatamente dois casos
que o justificam:

**Caso A: o "SCOS" faz parte do substantivo do domínio.**
A classe representa um conceito cujo nome real inclui a plataforma: `ScosSystem` (um sistema
registrado no SCOS), `ScosSystemRepository`, `ScosPermission`, `ScosOrganizationPermission`.
Aqui `Scos` é parte do nome, não um prefixo técnico.

**Caso B: infraestrutura técnica que competiria por nome com um tipo de framework.**
A classe é configuração, contexto de segurança, propriedade ou utilitário de plataforma, e sem o
prefixo colidiria conceitualmente com Spring, Hibernate ou gRPC: `ScosDataSourceConfiguration`,
`ScosHttpSecurityConfiguration`, `ScosSecurityContext`, `ScosAuthentication`,
`ScosHikariConfigProperties`, `ScosGrpcClientConfiguration`.

**Onde o prefixo NÃO DEVE aparecer:**

- Entidade de negócio: `Position`, `Company`, `Department`, `Employee`
- Delegate: `CompanyDelegate`
- Use case: `CreatePositionUseCase`
- Domain service de negócio: `PositionService`, `PositionServiceBean`
- Mapper, DTO, record de filtro: `PositionMapper`, `PositionOutput`, `PositionFilter`
- Repository de agregado de negócio: `PositionRepository`

Regra prática: se a classe existiria com o mesmo nome em qualquer sistema de RH, **NÃO DEVE**
levar `Scos`. Se ela só faz sentido dentro da plataforma SCOS, **PODE**.

Classe de bootstrap é caso B e **DEVE** ser `Scos<Contexto>[Grpc]Application`
(`ScosOrganizationApplication`, `ScosOrganizationGrpcApplication`, `ScosFlowApplication`).

---

## 5. Sufixos de papel (lista fechada)

| Sufixo | Camada | Visibilidade | O que é |
|---|---|---|---|
| `Delegate` | api | `public` | implementação da interface gerada do OpenAPI |
| `UseCase` | usecase | `public` | interface de um caso de uso, método único `execute` |
| `UseCaseBean` | usecase | package-private | implementação do caso de uso |
| `ApiMapper` | usecase | `public` | MapStruct entre DTO de domínio e DTO da API |
| `Service` | domain/specification | `public` | interface do serviço de domínio |
| `ServiceBean` | domain/service | package-private | implementação do serviço de domínio |
| `Mapper` | domain/service | `public` | MapStruct entre entidade e `Output` |
| `Repository` | domain/internal | `public` | repositório Spring Data do agregado |
| `Input` | domain/dto | `public` | record de entrada do serviço de domínio |
| `Output` | domain/dto | `public` | record de saída do serviço de domínio |
| `Filter` | domain/dto | `public` | record de critérios de busca opcionais |
| `Pk` | domain/internal | `public` | chave composta `@Embeddable` |
| `History` | domain/internal | `public` | entidade de histórico de um agregado |
| `Rule` | domain/internal/rules | package-private | regra de negócio isolada (`BusinessRule`) |
| `Evaluator` | domain | `public` (interface) | avaliador de política sem estado (ex.: `ShiftWindowEvaluator`) |
| `Configuration` | qualquer | `public` | classe `@Configuration` |
| `Properties` | qualquer | `public` | binding de `@ConfigurationProperties` |
| `Interceptor` | infra/grpc | `public` | interceptor gRPC ou HTTP |
| `Handler` | infra | `public` | tratador de exceção ou evento |
| `Filter` (servlet) | infra | `public` | filtro de servlet. Só neste contexto o sufixo é ambíguo com `Filter` de DTO; o pacote desfaz |
| `Listener` | infra | `public` | listener de evento de aplicação ou JPA |
| `Factory` | infra | `public` | fábrica de objeto de infraestrutura |
| `Converter` | shared | `public` | `AttributeConverter` do JPA |
| `Utils` | shared | `public final` | utilitário estático, construtor privado |
| `Application` | boot | `public` | classe de bootstrap Spring |

**Regras de sufixo:**

- `Utils` **DEVE** ser sempre plural. 🔄 **Migração**: convivem `PasswordGenerateUtil`,
  `AuthenticationUtils`, `FilterUtils`
- `Configuration` **DEVE** ser escrito por extenso. 🔄 **Migração**: `ScosGrpcSecurityConfig`
- Implementação **DEVE** usar `Bean`. **NÃO DEVE** usar `Impl`. 🔄 **Migração**:
  `RegistreServiceImpl`, `ValidateAuthorityServiceImpl` (gRPC)
- Prefixo `Vw` para entidade mapeada em view **DEVE** ser substituído pelo sufixo `View`
  (`AuthorityResponseView`). 🔄 **Migração**: `VwAuthorityResponse`

---

## 6. Classes por camada

### 6.1 Delegate

```
<Agregado>Delegate
```
`CompanyDelegate`, `PositionDelegate`, `ReasonActivateDelegate`.
O `<Agregado>` **DEVE** ser idêntico ao nome da tag do OpenAPI e ao prefixo da interface gerada
(`CompanyApiDelegate`).

### 6.2 Use case

```
<Verbo><Agregado>[<Subrecurso>]UseCase
<Verbo><Agregado>[<Subrecurso>]UseCaseBean
```

Verbos permitidos, com semântica fixa:

| Verbo | Semântica |
|---|---|
| `Create` | cria um recurso novo |
| `Update` | altera dados cadastrais de um recurso existente |
| `Delete` | remove fisicamente (raro; a maioria dos casos é `Disable`) |
| `Find` | busca um recurso por identificador |
| `FindAll` | busca paginada de coleção |
| `GetAll` | busca de coleção sem paginação (sub-recurso) |
| `Enable` / `Disable` | liga e desliga o recurso, sem motivo obrigatório |
| `Activate` / `Inactivate` | transição de status de negócio, com motivo obrigatório |
| `Block` / `Unblock` | bloqueio administrativo |

Verbo fora desta lista **DEVE** ser acordado antes de introduzido.
`Get` no singular **NÃO DEVE** ser usado para busca por id; o verbo é `Find`.

Exemplos: `CreatePositionUseCase`, `FindAllCompanyUseCase`,
`UpdatePositionWorkScheduleUseCase`, `InactivateCompanyUseCase`.

### 6.3 Domínio

```
<Agregado>Service            interface, pacote specification
<Agregado>ServiceBean        implementação, pacote service
<Agregado>Mapper             MapStruct, pacote service
<Agregado>Repository         pacote internal
<Agregado>                   entidade, substantivo singular, pacote internal
<Agregado><Parte>            entidade filha: CompanyAddress, CompanyContact, EmployeeWorkSchedule
<Agregado><Parte>Pk          chave composta: CompanyAddressPk
<Agregado>StatusHistory      histórico de status: CompanyStatusHistory
<Agregado>Status             enum de status: CompanyStatus  🔄 hoje StatusCompany
<Agregado>Input/Output/Filter  records, pacote dto
```

Serviço de domínio que não é CRUD de agregado **DEVE** nomear a responsabilidade, não o agregado:
`ShiftWindowEvaluator`, `EmployeePositionQueryService`, `SystemSecretCryptoService`.

### 6.4 Infraestrutura e configuração

```
Scos<Assunto>Configuration       ScosDataSourceConfiguration, ScosHttpSecurityConfiguration
Scos<Assunto>Properties          ScosHikariConfigProperties, ScosGRPCProperties
<Assunto>Interceptor             TokenAuthorizationInterceptor, GrpcLoggingInterceptor
<Assunto>Handler                 GrpcGlobalExceptionHandler, AccessDeniedExceptionHandler
```

Sigla no meio do nome **DEVE** seguir camelCase de sigla, com apenas a primeira letra maiúscula:
`ScosGrpcProperties`, não `ScosGRPCProperties`. 🔄 **Migração**

### 6.5 Enums

- Nome: substantivo singular, sem sufixo `Enum` (`CompanyStatus`, `DayOfWeek`,
  `ScosOrganizationPermission`)
- Constantes: `UPPER_SNAKE_CASE`
- Constante de status **DEVE** ser adjetivo ou particípio de estado (`ACTIVE`, `INACTIVE`,
  `BLOCKED`, `PENDING_APPROVAL`)
- Constante de permissão: `<ACTION>_<RESOURCE>` (seção 10)
- Constante de erro: nome semântico do problema (seção 11)

---

## 7. Membros: campos, constantes, parâmetros, métodos

### 7.1 Campos

- `private final` em lowerCamelCase
- Campo de dependência injetada **DEVE** repetir o nome do tipo em lowerCamelCase:
  `private final PositionService positionService;`, `private final CreatePositionUseCase
  createPositionUseCase;`. **NÃO DEVE** encurtar para `service` ou `useCase`
- Campo de entidade **DEVE** ser o mesmo nome do conceito na coluna, em camelCase:
  coluna `IS_TRUST_POSITION` para campo `isTrustPosition`

### 7.2 Constantes

- `private static final` em `UPPER_SNAKE_CASE`
- Constante de teste **DEVE** dizer o papel do valor, não o valor:
  `CODE_COMPANY_NOT_FOUND`, `SEEDED_ID`, `NONEXISTENT_ID`, `COMPANIES_URI`

### 7.3 Métodos

Prefixos verbais permitidos e o que cada um garante:

| Prefixo | Contrato |
|---|---|
| `execute` | único método público de um `UseCase` |
| `create` | persiste um recurso novo e devolve `Output` |
| `update` | altera um recurso existente; retorno `void` |
| `find` | busca por id; lança exceção de não encontrado |
| `findAll` | busca paginada; devolve `Page` |
| `get` | acessor simples, sem I/O e sem regra |
| `exists` | devolve `boolean`, sem efeito colateral |
| `is` / `has` | predicado sobre estado em memória, devolve `boolean` |
| `to<Tipo>` | conversão pura entre representações |
| `validate` | valida e lança em caso de violação; retorno `void` |
| `activate` / `deactivate` / `enable` / `disable` / `block` / `unblock` | mutação de estado na entidade |
| `register` | registra recurso em sistema externo |
| `handle` | trata exceção ou evento |

Regras adicionais:

- Método privado de busca que valida e lança **DEVE** terminar em `OrThrow`:
  `findActiveDepartmentOrThrow`
- Método que devolve `Optional` **NÃO DEVE** começar com `get`
- Nome de método **NÃO DEVE** repetir o nome da classe: em `PositionService`, o método é
  `create`, não `createPosition`. A exceção é o método que devolve a entidade
  (`findPositionById`), onde o tipo importa para o leitor
- Booleano **NÃO DEVE** ser negativo (`isNotActive`); use a forma positiva

### 7.4 Parâmetros

- lowerCamelCase, nome completo
- Parâmetro cujo tipo é um DTO **DEVE** repetir o nome do tipo:
  `execute(CreatePositionRequest createPositionRequest)`
- Identificador **DEVE** ser qualificado quando houver mais de um: `positionId`, `departmentId`,
  nunca `id` solto em método com dois identificadores

---

## 8. Contrato OpenAPI

### 8.1 Arquivos

```
etc/api/organization/ScosOrganization_<Área>.yml
etc/api/organization/ScosComponents.yml
```

- `<Área>` em PascalCase; área composta usa hífen (`ScosOrganization_Department-Position.yml`)
- Um arquivo por área funcional, não por agregado

### 8.2 Elementos

| Elemento | Padrão | Exemplo |
|---|---|---|
| `tags` | substantivo singular em PascalCase com espaço | `Reason Position Change` |
| `operationId` | `<verbo><Recurso>` em lowerCamelCase | `getAllDepartments`, `createPosition` |
| Rota | plural, kebab-case, com prefixo de versão | `/v1/positions/{id}/work-schedules` |
| Schema de recurso | substantivo singular | `Position`, `Department` |
| Schema de resposta única | `Get<Recurso>Response` | `GetPositionResponse` |
| Schema de resposta em lista | `GetAll<Recursos>Response` | `GetAllPositionsResponse` |
| Schema de request | `<Verbo><Recurso>Request` | `CreatePositionRequest` |
| Schema de transição | `<Recurso>StatusTransitionRequest` | `CompanyStatusTransitionRequest` |
| Parâmetro de filtro | lowerCamelCase + sufixo `Filter` | `activeFilter`, `paginationFilter` |
| Header | forma canônica HTTP | `X-Request-ID`, `Accept-Language` |
| Extensão | `x-` minúsculo | `x-authorize`, `x-jdempotentrequestpayload` |

O `operationId` **DEVE** ser exatamente o nome do método do delegate. O nome do schema **DEVE**
ser exatamente o nome da classe gerada em `api.dto`.

---

## 9. gRPC e protobuf

| Elemento | Padrão | Exemplo |
|---|---|---|
| Arquivo `.proto` | minúsculo, singular, sem prefixo | `authority.proto`, `registry.proto` |
| `java_package` | `br.com.sawcunhaos.organization.grpc.proto` | |
| `java_outer_classname` | `<Assunto>Proto` em PascalCase | `AuthorityProto` |
| `service` | `<Verbo><Assunto>Service` ou `<Assunto>Service` | `ValidateAuthorityService`, `RegistryService` |
| `rpc` | lowerCamelCase, verbo primeiro | `validateAuthority`, `registrySystem` |
| `message` de entrada | `<Rpc>Request` | `AuthorityRequest`, `RegistrySystemRequest` |
| `message` de saída | `<Rpc>Response` | `AuthorityResponse` |
| Campo de message | `snake_case` (convenção protobuf) | `company_id`, `branch_name` |
| Implementação Java | `<Service>Bean` | `RegistryServiceBean` 🔄 hoje `RegistreServiceImpl` |

O nome do `service` no proto e o da classe Java **DEVEM** ser o mesmo radical. 🔄 **Migração**:
o proto declara `RegistryService` e a classe é `RegistreServiceImpl`, com erro de grafia e com
sufixo fora do padrão.

---

## 10. Banco de dados

| Objeto | Padrão | Exemplo |
|---|---|---|
| Schema | minúsculo | `scos` |
| Tabela | `SCOS_<ENTIDADE>` em UPPER_SNAKE singular | `SCOS_POSITION`, `SCOS_COMPANY_ADDRESS` |
| Tabela de histórico | `SCOS_<ENTIDADE>_STATUS_HISTORY` | `SCOS_COMPANY_STATUS_HISTORY` |
| Tabela de associação | `SCOS_<A>_<B>` | `SCOS_PROFILE_RESOURCE`, `SCOS_COMPANY_CNAE_SECONDARY` |
| Coluna | UPPER_SNAKE | `IS_TRUST_POSITION`, `CREATED_AT` |
| Coluna de PK | `<ENTIDADE>_ID` | `POSITION_ID` |
| Coluna de FK | `<ENTIDADE_REFERENCIADA>_ID` | `DEPARTMENT_ID` |
| Primary key | `PK_SCOS_<TABELA>` | `PK_SCOS_POSITION` |
| Foreign key | `FK_<COLUNA>_SCOS_<TABELA>` | `FK_DEPARTMENT_ID_SCOS_POSITION` |
| Arquivo de changelog | `scos_<tabela>.yml` minúsculo | `scos_position.yml` |
| `changeSet.id` | `AAAAMMDD-Nome.Sobrenome-NNN` | `20260606-Samuel.Cunha-002` |
| `author` | `Nome.Sobrenome` | `Samuel.Cunha` |

O nome da entidade Java, o da tabela e o do arquivo de changelog **DEVEM** ser o mesmo substantivo:
`Position` → `SCOS_POSITION` → `scos_position.yml`.

---

## 11. Permissões e códigos de erro

### 11.1 Permissão

```
<ACTION>_<RESOURCE>
```
`GET_DEPARTMENT`, `CREATE_POSITION_WORK_SCHEDULE`, `UPDATE_LOGIN_STATUS`.

- `<ACTION>` sai do mesmo vocabulário de verbos da seção 6.2, em maiúsculas
- `<RESOURCE>` é o agregado ou sub-recurso em UPPER_SNAKE singular
- O nome da constante **DEVE** ser idêntico ao valor de `x-authorize` no YAML

### 11.2 Código de erro

Duas coisas distintas, com regras distintas:

| Elemento | Padrão | Muda? |
|---|---|---|
| Nome da constante | semântico, UPPER_SNAKE, descreve o problema | pode ser renomeado |
| Valor de `code` | `SCOS_<MÓDULO>_<NNN>`, três dígitos | **imutável após publicação** |

```java
POSITION_NOT_FOUND("SCOS_POSITION_001", 404, "SCOS_TITLE_NOT_FOUND"),
POSITION_CODE_ALREADY_EXISTS("SCOS_POSITION_002", 409, "SCOS_TITLE_CONFLICT"),
POSITION_ALREADY_ACTIVE("SCOS_POSITION_004", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
```

- `<MÓDULO>` é o agregado em maiúsculas, igual ao usado na tabela e na permissão
- Numeração sequencial, nunca reaproveitada
- Nome da constante **DEVE** ser legível no ponto do `throw`. 🔄 **Migração**: hoje o nome é
  igual ao código
- Enum **DEVE** ser `<Contexto>ExceptionCode`. 🔄 **Migração**: hoje é o global `ExceptionCodeError`
- `title` sai da lista fechada: `SCOS_TITLE_NOT_FOUND`, `SCOS_TITLE_CONFLICT`,
  `SCOS_TITLE_BUSINESS_RULE_VIOLATION`, `SCOS_TITLE_EXTERNAL_INTEGRATION_FAILURE`,
  `SCOS_TITLE_UNAUTHORIZED`, `SCOS_TITLE_INTERNAL_ERROR`, `SCOS_TITLE_GENERIC`

### 11.3 Bundles

| Arquivo | Conteúdo |
|---|---|
| `scos_message_<assunto>.properties` | PT-BR (default) |
| `scos_message_<assunto>_en.properties` | inglês |
| `messages_<assunto>.properties` | bundle de infraestrutura (permissões) |
| `key_<assunto>_description.properties` | descrição de chave de configuração |

A chave **DEVE** ser exatamente o valor de `code` do erro, ou o nome da constante de permissão.

---

## 12. Testes

### 12.1 Classe

```
<ClasseTestada>Test
```

Sufixos derivados, todos terminando em `Test`:

| Nome | Cobre |
|---|---|
| `<UseCase>BeanTest` / `<Service>BeanTest` | unitário de implementação |
| `<Agregado>ControllerTest` | integração full-stack via MockMvc |
| `<Agregado>RepositoryPersistenceTest` | persistência real do repositório |
| `<Classe>MapperTest` | mapeamento |
| `<Assunto>ConsistencyTest` | consistência entre artefatos (YAML ↔ enum, enum ↔ bundle) |
| `ArquiteturaTest` | regras de camada (ArchUnit) |

Classe de apoio de teste **DEVE** usar sufixo `TestUtil` ou `TestSupport`
(`ScosOrganizationTestUtil`, `ScosJwtTestSupport`) e **NÃO DEVE** terminar em `Test`.

### 12.2 Método

```
<metodo>_<cenario>_returns<Resultado>       teste de integração
<metodo>_<cenarioEResultado>                teste unitário
```

`getAll_withValidToken_returns200`, `executes_withNullTrustFlag_mapsToFalse`.

O `@DisplayName` acompanha em PT-BR:
- Unitário: descreve o comportamento (`"isTrustPosition nulo no request vira false no input"`)
- Integração: `MÉTODO /rota — cenário (status)`

---

## 13. Arquivos de configuração e recursos

| Arquivo | Padrão |
|---|---|
| `application.yml`, `application-<perfil>.yml` | perfil minúsculo: `application-dev.yml` |
| `bootstrap.yml`, `logback-spring.xml` | nome padrão do framework, sem prefixo |
| Compose de infra | `docker-compose-<serviço>.yml` em `etc/infra/` |
| Variável de ambiente | `SCOS_<ASSUNTO>_<PROPRIEDADE>` em UPPER_SNAKE |
| Documento de caso de uso | `NN-<assunto-kebab>.md` em `etc/doc/usecase/` |

---

## 14. Vocabulário canônico

O termo da coluna "Canônico" **DEVE** ser usado em todas as camadas. Os sinônimos listados
**NÃO DEVEM** aparecer em identificador nenhum.

| Canônico | Significado | Não usar |
|---|---|---|
| `Company` | empresa, matriz ou filial | Enterprise, Organization, Firm, Branch (filial é `Company` com hierarquia) |
| `Department` | departamento | Sector, Area, Division |
| `Position` | cargo | Job, Role, Occupation |
| `PositionWorkSchedule` | jornada padrão do cargo | Shift, Journey, Timetable |
| `Employee` | funcionário | Worker, Staff, Collaborator |
| `Login` | credencial de acesso de um funcionário | User, Account, Credential |
| `Profile` | perfil de acesso | Group, Role |
| `Resource` | permissão atômica | Permission (reservado ao enum), Grant, Scope |
| `Reason<Transição>` | motivo obrigatório de transição de status | Justification, Motive |
| `Cnae` / `LegalNature` | classificação fiscal brasileira | mantidos no termo local, sem tradução |
| `AddressType` / `ContactType` | catálogos auxiliares | Kind, Category |
| `Outbox` | tabela de eventos para publicação | EventQueue, Publisher |
| `ScosSystem` | sistema externo registrado na plataforma | Client, App, Service |
| `active` | flag booleana de habilitado | enabled, isEnabled, status |
| `status` | estado de negócio com transição e motivo | situation, state |

Termo novo do domínio **DEVE** entrar nesta tabela no mesmo PR em que aparece no código.

---

## 15. Divergências atuais

| # | Divergência | Onde | Regra |
|---|---|---|---|
| N-1 | `<name>` do módulo não bate com o `artifactId` | `flow-security-starter` (`Scos Organization Security Start`) | §2.2 |
| N-2 | Dependência declarada para artefato inexistente | `flow-organization-grpc-boot` → `flow-security-starter` | §2.2 |
| N-3 | Quarto pacote-raiz usado só por teste | `br.com.sawcunhaos.foundation.security` | §3.1 |
| N-4 | Pacote com erro de grafia | `infrastructure.enumaration` | §3.2 |
| N-5 | Classe com erro de grafia | `PaginatioUtils` | §1.7 |
| N-6 | Classe gRPC com erro de grafia e sufixo errado | `RegistreServiceImpl` (proto diz `RegistryService`) | §9 |
| N-7 | Sufixo `Impl` em vez de `Bean` | `RegistreServiceImpl`, `ValidateAuthorityServiceImpl` | §5 |
| N-8 | `Util` no singular | `PasswordGenerateUtil` | §5 |
| N-9 | `Config` abreviado | `ScosGrpcSecurityConfig` | §5 |
| N-10 | Sigla toda em maiúsculas | `ScosGRPCProperties` | §6.4 |
| N-11 | Ordem modificador/núcleo invertida | `StatusCompany`, `StatusEmployee` | §1.3 |
| N-12 | Prefixo `Vw` em entidade de view | `VwAuthorityResponse` | §5 |
| N-13 | Constante de erro com nome numérico | todo o `ExceptionCodeError` | §11.2 |

N-1 a N-10 e N-12 são renomeações locais, de baixo risco, e cabem no FR-8 do PRD de
Padronização. N-11 e N-13 tocam código de mais de uma camada e seguem a fase indicada no PRD.
