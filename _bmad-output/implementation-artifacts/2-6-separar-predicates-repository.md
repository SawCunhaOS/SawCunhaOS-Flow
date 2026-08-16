---
baseline_commit: ce439e75063a7a7a4448ab1b28323b3727af66e9
---

# Story 2.6: Separar Regras de Montagem de Predicate dos Repositories

Status: review

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como Desenvolvedor da plataforma,
Eu quero que toda montagem dinâmica de predicate QueryDSL (`BooleanBuilder`) hoje embutida em métodos `default` de Repository — e, num caso, dentro de um Service — viva numa classe `XxxPredicates` dedicada no mesmo pacote `internal`, e que a suíte de testes do módulo `domain` volte a executar de verdade,
Para separar a regra de montagem da regra de pesquisa, com cobertura de teste unitário isolada, sem mudar nenhum comportamento existente e sem depender de workaround manual para rodar `mvn test`.

## Acceptance Criteria

1. **Given** o módulo `domain` (e potencialmente outros) hoje reporta `Tests run: 0` ao rodar `mvn test`, porque `maven-surefire-plugin` não tem `<version>` pinada em nenhum `pluginManagement` do monorepo — resolve para `2.17` (versão mais antiga cacheada no `.m2` local, sem provider de JUnit Platform/JUnit 5) **When** esta story é implementada **Then** `maven-surefire-plugin` ganha `<version>3.5.4</version>` no `<pluginManagement>` do `pom.xml` raiz (mesmo bloco onde `maven-failsafe-plugin` já está pinado em `3.2.5`) **And** `mvn -pl organization/flow-organization-domain -am test -Denforcer.skip=true` volta a executar a suíte completa (confirmado nesta análise: antes da correção, `Tests run: 0, Failures: 0, Errors: 0, Skipped: 0`; depois, `Tests run: 286, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`) **And** o workaround manual documentado nas Dev Agent Records das Stories 2.3/2.5 (`mvn org.apache.maven.plugins:maven-surefire-plugin:3.5.4:test ...`) deixa de ser necessário para rodar `mvn test` normalmente.
2. **Given** `AddressTypeRepository`/`ContactTypeRepository` (`catalog/internal`) já têm a montagem extraída para `AddressTypePredicates`/`ContactTypePredicates` nesta sessão de trabalho (working tree, ainda não commitado — ver `git status`) **When** esta story é implementada **Then** as duas classes ganham cobertura de teste unitário dedicada (`AddressTypePredicatesTest`/`ContactTypePredicatesTest`), sem mudar assinatura ou comportamento do repositório **And** `AddressTypePredicates` ganha o cabeçalho de licença Apache 2.0 que hoje falta (inconsistência confirmada por leitura: `ContactTypePredicates` já tem, `AddressTypePredicates` não).
3. **Given** os 4 repositórios simétricos de motivo (`ReasonEnableRepository`/`ReasonDisableRepository`/`ReasonActivateRepository`/`ReasonInactivateRepository`, `access/status/internal`) — confirmados por `diff` como idênticos em forma, cada um com 3 métodos `default` (`existsByCodeAndEntityType`, `existsByCodeAndEntityTypeAndNotId`, `findAllFiltered`) montando `BooleanBuilder` inline **When** esta story é implementada **Then** cada um ganha sua própria `ReasonXPredicates` (`ReasonEnablePredicates`, `ReasonDisablePredicates`, `ReasonActivatePredicates`, `ReasonInactivatePredicates`), mesmo padrão exato de `AddressTypePredicates` **And** cada uma ganha teste unitário dedicado.
4. **Given** `EmployeeQueryRepository` (`corporate/employee/internal`), `PositionRepository` (`corporate/position/internal`) e `DepartmentRepository` (`corporate/department/internal`) — cada um com métodos `default` que montam `BooleanBuilder` para `exists`/`findOne`/`findAllFiltered` **When** esta story é implementada **Then** cada um ganha sua `XxxPredicates` dedicada (`EmployeeQueryPredicates`, `PositionPredicates`, `DepartmentPredicates`), com teste unitário dedicado **And** `DepartmentRepository.existsByIdAndPositionsActive` (predicate direto `qDepartment.id.eq(...).and(qDepartment.positions.any().active.isTrue())`, sem `BooleanBuilder`) permanece **intocado** — fora do critério de escopo desta story (ver Dev Notes).
5. **Given** `CompanyServiceBean.findAll` (`corporate/company/service`) — único Service encontrado no módulo `domain` que monta um `BooleanBuilder` contra `QCompany` diretamente, cruzando a fronteira de `internal/` a partir da camada `service/` **When** esta story é implementada **Then** a montagem migra para uma nova classe `CompanyPredicates` em `corporate/company/internal/`, exposta por um novo método `default Page<Company> findAllFiltered(StatusCompany status, String name, Pageable pageable)` em `CompanyRepository` **And** `CompanyServiceBean.findAll` passa só a chamar `companyRepository.findAllFiltered(status, name, pageable)`, sem importar `BooleanBuilder`/`QCompany` **And** o comportamento observável não muda: sem nenhum filtro informado, a consulta continua retornando todas as empresas (fallback `id.isNotNull()` quando o predicate está vazio, preservado ao pé da letra).
6. **Given** todas as classes `XxxPredicates` criadas ou testadas por esta story **When** os testes unitários são escritos **Then** seguem o mesmo padrão: JUnit 5 puro (sem `@SpringBootTest`, sem H2/Testcontainers), classe de teste no mesmo pacote `internal` (necessário — as classes `Predicates` e seus métodos são package-private) **And** a asserção compara `predicate.toString()` contra o `Predicate` esperado, montado no próprio teste com o mesmo `Q`-type (ver Dev Notes para o padrão exato).
7. **Given** nenhuma regra de negócio muda nesta story — é refatoração pura (mesma assinatura pública, mesmo resultado de query, SQL gerado idêntico) **When** esta story é implementada **Then** a suíte de testes já existente (`domain`, `usecase`, `boot`) continua passando, agora executando de verdade (AC 1), sem nenhuma alteração de asserção fora dos arquivos tocados por esta story — a única exceção esperada é `CompanyServiceBeanTest.findAllShouldReturnMappedPage` (AC 5, muda o método mockado de `companyRepository.findAll(Predicate, Pageable)` para `companyRepository.findAllFiltered(...)`).

## Tasks / Subtasks

- [x] Task 1: Corrigir `maven-surefire-plugin` sem versão pinada — testes não executavam (AC: 1)
  - [x] Em `pom.xml` (raiz), dentro de `<build><pluginManagement><plugins>`, adicionar `<version>3.5.4</version>` ao `<plugin>` `maven-surefire-plugin` (hoje só declara `groupId`/`artifactId`, sem versão — por isso resolve para a versão mais antiga cacheada localmente, `2.17`, que não tem provider JUnit 5/JUnit Platform e por isso não descobre nenhum `@Test`, sem erro, só `Tests run: 0`). Mesmo padrão de `maven-failsafe-plugin`, já pinado em `3.2.5` duas entradas abaixo no mesmo bloco.
  - [x] Confirmar com `mvn -pl organization/flow-organization-domain -am help:effective-pom -Denforcer.skip=true` (ou lendo a saída) que o `<version>` resolvido do `maven-surefire-plugin` passou a ser `3.5.4` (era `2.17`).
  - [x] Rodar `mvn -pl organization/flow-organization-domain -am test -Denforcer.skip=true` e confirmar que a suíte completa (286 testes, conforme Story 2.5) executa e passa — verificado nesta sessão de criação da story, `BUILD SUCCESS`.
  - [x] Repetir em `organization/flow-organization-usecase` (217 testes esperados, conforme Story 2.5) e, se Docker estiver disponível neste ambiente, `organization/flow-organization-boot`.
  - [x] **Não** pinar versão de `maven-surefire-plugin` em nenhum `pom.xml` filho — o `pluginManagement` do `pom.xml` raiz já propaga para todos os módulos (nenhum filho declara `<version>` própria hoje, todos herdam do pai).
  - [x] **Não** mexer no `<argLine>` de `--add-opens` já configurado em `flow-organization-boot`/`server-fat` — só a versão do plugin muda, a configuração de execução permanece.

- [x] Task 2: Finalizar `catalog` — testes + cabeçalho de licença (AC: 2)
  - [x] Adicionar cabeçalho Apache 2.0 em `AddressTypePredicates.java` (copiar bloco exato do topo de `ContactTypePredicates.java`, mesma pasta).
  - [x] Criar `AddressTypePredicatesTest.java` em `flow-organization-domain/src/test/.../corporate/catalog/internal/` (pacote igual ao main — classe é package-private), cobrindo os 3 métodos: `predicateCodeAndEntityType`, `predicateCodeAndEntityTypeAndNotId`, `predicateEntityAndActive` (3 variações: só `active`, só `entityType`, ambos nulos → predicate vazio).
  - [x] Criar `ContactTypePredicatesTest.java`, mesmo padrão, cobrindo `predicateCodeAndEntityType`, `predicateCodeAndEntityAndNotId`, `predicateEntityTypeAndActive`.
  - [x] **Não** alterar `AddressTypeRepository.java`/`ContactTypeRepository.java`/`AddressTypePredicates.java`/`ContactTypePredicates.java` além do cabeçalho de licença — a extração já está correta e completa.

- [x] Task 3: Extrair Predicates dos 4 repositórios `Reason*` (AC: 3)
  - [x] Em `access/status/internal/`, criar `ReasonEnablePredicates.java` (classe `final`, package-private), espelhando **exatamente** a forma de `AddressTypePredicates`:
    ```java
    package br.com.sawcunhaos.organization.domain.access.status.internal;

    import com.querydsl.core.BooleanBuilder;
    import com.querydsl.core.types.Predicate;

    import java.util.Objects;

    final class ReasonEnablePredicates {
        private static final QReasonEnable qReasonEnable = QReasonEnable.reasonEnable;

        static Predicate predicateCodeAndEntityType(String code, EntityType entityType) {
            BooleanBuilder booleanBuilder = new BooleanBuilder();
            booleanBuilder.and(qReasonEnable.code.eq(code))
                    .and(qReasonEnable.entityType.eq(entityType));
            return booleanBuilder.getValue();
        }

        static Predicate predicateCodeAndEntityTypeAndNotId(String code, EntityType entityType, Long reasonEnableId) {
            BooleanBuilder booleanBuilder = new BooleanBuilder();
            booleanBuilder.and(qReasonEnable.code.eq(code))
                    .and(qReasonEnable.entityType.eq(entityType))
                    .and(qReasonEnable.id.ne(reasonEnableId));
            return booleanBuilder.getValue();
        }

        static Predicate predicateEntityTypeAndActive(EntityType entityType, Boolean active) {
            BooleanBuilder booleanBuilder = new BooleanBuilder();
            if (Objects.nonNull(active)) {
                booleanBuilder.and(qReasonEnable.active.eq(active));
            }
            if (Objects.nonNull(entityType)) {
                booleanBuilder.and(qReasonEnable.entityType.eq(entityType));
            }
            return booleanBuilder.getValue();
        }
    }
    ```
    Repetir **exatamente** o mesmo padrão (só trocando `ReasonEnable`→`ReasonDisable`/`ReasonActivate`/`ReasonInactivate` e `qReasonEnable`→`qReasonDisable`/etc.) para as outras 3 classes — os 4 repositórios são idênticos em forma (confirmado por `diff` nesta análise), então as 4 `Predicates` também devem ser.
  - [x] Refatorar cada `ReasonXRepository.java`: remover `import com.querydsl.core.BooleanBuilder;` e o campo `QReasonX qReasonX = QReasonX.reasonX;` da interface (passa a viver só dentro de `ReasonXPredicates`); adicionar `import static ...ReasonXPredicates.predicateCodeAndEntityType;` (+ os outros 2 métodos); substituir o corpo dos 3 métodos `default` por chamada direta ao predicate correspondente — mesmo padrão exato de `AddressTypeRepository` (linhas 26-28 e 40-60 do arquivo já refatorado).
  - [x] Criar `ReasonEnablePredicatesTest.java`/`ReasonDisablePredicatesTest.java`/`ReasonActivatePredicatesTest.java`/`ReasonInactivatePredicatesTest.java`, mesmo pacote `internal`, cobrindo os 3 métodos de cada classe (mesma cobertura da Task 2).

- [x] Task 4: Extrair `EmployeeQueryPredicates` (AC: 4)
  - [x] Em `corporate/employee/internal/`, criar `EmployeeQueryPredicates.java` com `predicatePositionId(Long)`, `predicatePositionIdAndStatus(Long, StatusEmployee)`, `predicateTaxIdentifier(String)`, `predicateEmail(String)`, `predicateTaxIdentifierAndStatus(String, StatusEmployee)` e `predicateCompanyIdAndPositionIdAndStatus(Long companyId, Long positionId, StatusEmployee status)` (esta última é a versão dinâmica usada por `findAllFiltered`, mesmo padrão condicional de `predicateEntityAndActive`).
  - [x] Refatorar `EmployeeQueryRepository.java`: os 5 `default boolean`/`Optional` (`existsByPositionId`, `existsByPositionIdAndStatus`, `existsByTaxIdentifier`, `existsByEmail`, `findByTaxIdentifierAndStatus`) passam a chamar o predicate correspondente; `findAllFiltered` passa a montar via `predicateCompanyIdAndPositionIdAndStatus` (mantendo o atalho `if (Objects.isNull(companyId) && Objects.isNull(positionId) && Objects.isNull(status)) return findAll(pageable);` **na Repository**, não na Predicates — mesmo padrão de `AddressTypeRepository.findAllFiltered`, que faz o atalho de "nenhum filtro" no repositório e só delega a montagem condicional à classe Predicates).
  - [x] Criar `EmployeeQueryPredicatesTest.java`, cobrindo os 6 métodos (para o dinâmico, testar: só companyId; só positionId; só status; os 3 combinados).

- [x] Task 5: Extrair `PositionPredicates` (AC: 4)
  - [x] Em `corporate/position/internal/`, criar `PositionPredicates.java` com `predicateDepartmentId(Long)`, `predicateCodeAndNotId(Long, String)`, `predicateCode(String)`, `predicateDepartmentIdAndActive(Long, Boolean)` (dinâmica).
  - [x] Refatorar `PositionRepository.java`: `existsByDepartmentId`, `existsByCodeAndNotId`, `existsByCode` chamam o predicate correspondente; `findAllFiltered` mantém o atalho de "nenhum filtro" no repositório e delega a montagem a `predicateDepartmentIdAndActive`.
  - [x] Criar `PositionPredicatesTest.java`, cobrindo os 4 métodos (dinâmico: só departmentId; só active; ambos).

- [x] Task 6: Extrair `DepartmentPredicates` (AC: 4)
  - [x] Em `corporate/department/internal/`, criar `DepartmentPredicates.java` com `predicateCodeAndNotId(Long, String)`, `predicateCode(String)`, `predicateActive(Boolean)` (usada por `findAllFiltered`, que hoje só tem 1 filtro — ainda assim usa `BooleanBuilder`, por isso entra no escopo).
  - [x] Refatorar `DepartmentRepository.java`: os 3 métodos passam a chamar o predicate correspondente. **Não** tocar em `existsByIdAndPositionsActive` (AC 4 — usa predicate direto sem `BooleanBuilder`, fora do critério de escopo desta story).
  - [x] Criar `DepartmentPredicatesTest.java`, cobrindo os 3 métodos extraídos.

- [x] Task 7: Extrair `CompanyPredicates` + refatorar `CompanyServiceBean.findAll` (AC: 5)
  - [x] Em `corporate/company/internal/`, criar `CompanyPredicates.java`:
    ```java
    package br.com.sawcunhaos.organization.domain.corporate.company.internal;

    import com.querydsl.core.BooleanBuilder;
    import com.querydsl.core.types.Predicate;

    final class CompanyPredicates {
        private static final QCompany qCompany = QCompany.company;

        static Predicate predicateStatusAndName(StatusCompany status, String name) {
            BooleanBuilder booleanBuilder = new BooleanBuilder();
            if (status != null) {
                booleanBuilder.and(qCompany.status.eq(status));
            }
            if (name != null && !name.isBlank()) {
                booleanBuilder.and(qCompany.name.containsIgnoreCase(name));
            }
            if (!booleanBuilder.hasValue()) {
                booleanBuilder.and(qCompany.id.isNotNull());
            }
            return booleanBuilder.getValue();
        }
    }
    ```
    Réplica **exata** da lógica hoje em `CompanyServiceBean.findAll` (linhas 195-204), incluindo o fallback `id.isNotNull()` quando nenhum filtro é informado — não mudar esse comportamento.
  - [x] Em `CompanyRepository.java`, adicionar:
    ```java
    default Page<Company> findAllFiltered(StatusCompany status, String name, Pageable pageable) {
        return findAll(CompanyPredicates.predicateStatusAndName(status, name), pageable);
    }
    ```
    Sem import novo necessário — `CompanyPredicates` fica no mesmo pacote `internal` que `CompanyRepository`.
  - [x] Em `CompanyServiceBean.java`, substituir o corpo de `findAll` (linhas 192-207) por:
    ```java
    @Override
    @Transactional(readOnly = true)
    public Page<CompanyOutput> findAll(StatusCompany status, String name, @NonNull Pageable pageable) {
        log.info("Find All Companies, Status: {}, Name: {}", status, name);
        return companyRepository.findAllFiltered(status, name, pageable).map(companyMapper::toCompanyOutput);
    }
    ```
    Remover os imports `com.querydsl.core.BooleanBuilder` e `br.com.sawcunhaos.organization.domain.corporate.company.internal.QCompany` (confirmar antes que não são usados em nenhum outro método da classe — já verificado nesta análise: `QCompany` só aparece nesse método).
  - [x] Em `CompanyServiceBeanTest.java`, atualizar `findAllShouldReturnMappedPage` (linhas 321-334): trocar `when(companyRepository.findAll(any(Predicate.class), any(Pageable.class)))` por `when(companyRepository.findAllFiltered(eq(StatusCompany.ACTIVE), eq("Saw"), any(Pageable.class)))`. **Não** remover o import `com.querydsl.core.types.Predicate` da classe de teste — ainda é usado por `legalNatureRepository.exists(any(Predicate.class))`/`cnaeRepository.exists(any(Predicate.class))` (linhas 188, 199), intocados por esta story.
  - [x] Criar `CompanyPredicatesTest.java` em `corporate/company/internal/`, cobrindo `predicateStatusAndName`: só `status`; só `name`; ambos; nenhum informado (asserir que o resultado é equivalente a `id.isNotNull()` — este é o branch mais fácil de quebrar sem perceber, teste-o explicitamente).

- [x] Task 8: Guarda de escopo (AC: 4, 7)
  - [x] **Não** extrair predicate de nenhum método que monta a expressão QueryDSL sem `BooleanBuilder` (ex.: `CompanyRepository.existsByTaxIdentifierAndNotId`, `existsOtherActiveMatrix`, `findAllNotDisabled`, `findNotDisabledById`, `DepartmentRepository.existsByIdAndPositionsActive`) — o critério de escopo desta story é a presença de `BooleanBuilder` (marcador mecânico de "montagem"), confirmado por `grep -rl "BooleanBuilder" .../domain/src/main/java`. Repositórios fora dessa lista (`CnaeRepository`, `CompanyAddressRepository`, `CompanyCnaeSecondaryRepository`, `CompanyContactRepository`, `LegalNatureRepository`, `EmployeeAddressQueryRepository`, `EmployeeContactQueryRepository`, `EmployeePositionHistoryRepository`, `EmployeeWorkScheduleRepository`, `ReasonPositionChangeRepository`, `PositionWorkScheduleRepository`, e todos os de `access/login`, `access/profile`, `access/resource`, `outbox`) não usam `BooleanBuilder` e ficam de fora.
  - [x] **Não** criar nenhum changeSet Liquibase, endpoint, permissão ou mudança de contrato OpenAPI — esta story é refatoração interna do módulo `domain` + 1 linha de build (`pom.xml` raiz), sem efeito em `usecase`/`api`/`boot` além do único ajuste de mock em `CompanyServiceBeanTest` (Task 7).
  - [x] **Não** alterar o SQL/QueryDSL gerado — cada `predicate*` deve produzir exatamente a mesma árvore de condições que o código original montava inline (é o que os testes de `.toString()` verificam).
  - [x] **Não** tentar corrigir o enforcer quebrado (`RequireUpperBoundDeps`, débito pré-existente documentado em `project-context.md`) — fora de escopo desta story; `-Denforcer.skip=true` continua necessário para builds que tocam `flow-security-starter`/`flow-organization-infrastructure`.

- [x] Task 9: Regressão completa (AC: 7)
  - [x] Rodar a suíte completa do módulo `domain`: `mvn -pl organization/flow-organization-domain test -Denforcer.skip=true` (contorno do enforcer quebrado, documentado em `project-context.md` — agora sem precisar do workaround de versão do surefire, corrigido na Task 1) — confirmar que nenhum teste pré-existente muda de comportamento além do já previsto (Task 7, `CompanyServiceBeanTest`).
  - [x] Rodar a suíte de `usecase` e, se Docker estiver disponível, `boot` — nenhum dos dois deveria ser afetado (nenhuma assinatura pública de `Service`/`Repository` muda, exceto a adição de `findAllFiltered` em `CompanyRepository`, que é aditiva).

## Dev Notes

### Contexto crítico — leia antes de implementar

**Parte do trabalho já está feita no working tree, não commitada.** `git status` no início desta sessão mostra `AddressTypePredicates.java` (novo), `AddressTypeRepository.java` (modificado), `ContactTypePredicates.java` (novo, staged), `ContactTypeRepository.java` (modificado) — a extração do padrão Predicate para o bounded context `catalog` **já aconteceu**. Esta story formaliza esse trabalho (Task 2: só cabeçalho de licença + testes) e **estende o mesmo padrão** para o resto do módulo `domain` (Tasks 3-7), que foi auditado nesta sessão de criação da story via `grep -rl "BooleanBuilder"` em todo `flow-organization-domain/src/main/java`.

**`mvn test` não executava nenhum teste — verificado, não é suposição.** `maven-surefire-plugin` não tem `<version>` em nenhum `pluginManagement` do monorepo; nesta sessão, `mvn help:effective-pom` confirmou resolução para `2.17` (a versão mais antiga entre as 3 cacheadas em `~/.m2/repository/org/apache/maven/plugins/maven-surefire-plugin/`: `2.17`, `3.5.4`, `3.6.0-M1`). Surefire `2.17` não tem provider para JUnit Platform (JUnit 5) — o resultado não é erro, é silêncio: `Tests run: 0, Failures: 0, Errors: 0, Skipped: 0`, reactor `BUILD SUCCESS`, nenhum sinal de que algo está errado. Isso já forçava workaround manual documentado nas Dev Agent Records das Stories 2.3/2.5 (invocar o plugin por coordenada completa: `mvn org.apache.maven.plugins:maven-surefire-plugin:3.5.4:test ...`). A correção (Task 1) elimina a causa raiz — pinar a versão no `pluginManagement` do `pom.xml` raiz, exatamente como `maven-failsafe-plugin` já está pinado (`3.2.5`) duas entradas abaixo, no mesmo bloco. Verificado nesta sessão: com o pin, `mvn -pl organization/flow-organization-domain -am test -Denforcer.skip=true` executa e passa os 286 testes existentes.

**Critério de escopo do refactor de Predicates — por que `BooleanBuilder` e não "todo predicate QueryDSL".** Muitos repositórios (`CompanyRepository`, `DepartmentRepository.existsByIdAndPositionsActive`, etc.) montam predicates diretamente com `.and()` encadeado no próprio `Q`-type, sem instanciar `BooleanBuilder` — isso não é "montagem" no mesmo sentido: é uma única expressão QueryDSL, não uma composição condicional de partes. `BooleanBuilder` é o marcador mecânico usado nesta story para decidir o que entra no escopo: presença de `BooleanBuilder` = lógica de montagem que vale a pena isolar e testar separadamente da consulta. Isso é decisão explícita do usuário que pediu esta story ("todos os repository e verifica se existe service com esse comportamento") — não adivinhado; ver Task 8 para a lista completa do que foi auditado e ficou fora.

**`CompanyServiceBean.findAll` é o único caso fora de um Repository.** Auditoria (`grep -rl "BooleanBuilder" .../ServiceBean.java`) encontrou só esse um caso — nenhum outro Service do módulo `domain` monta QueryDSL diretamente. É o caso mais importante da story: hoje o Service importa `QCompany` (tipo gerado do agregado `Company`, pacote `internal`) e monta o predicate ele mesmo, o que é exatamente a violação de camada que o pedido original do usuário aponta ("separar a regra de montagem da de pesquisa") — a montagem deveria estar perto do repositório que a consome, não na camada de orquestração de negócio.

**Por que os testes de Predicates comparam `predicate.toString()` e não executam contra banco.** Não existe precedente de teste de `Repository`/`Predicate` isolado neste módulo (só há 1 teste em pacote `internal` hoje, `ScosSystemRepositoryPersistenceTest`, que é `@DataJpaTest` e testa outra coisa — persistência de entidade, não montagem de predicate). QueryDSL `BooleanBuilder`/`Predicate` não precisa de contexto Spring nem banco para ser testado — `.toString()` produz uma representação determinística da árvore de condições (ex.: `code = X && entityType = Y`), suficiente para verificar que a combinação certa de `.and()` foi montada, sem duplicar cobertura de integração que `*ControllerTest` já faz.

**Diferença de padrão entre o atalho "sem filtro" e a montagem condicional.** Em `AddressTypeRepository.findAllFiltered` (referência já implementada), o atalho `if (Objects.isNull(entityType) && Objects.isNull(active)) return findAll(pageable);` fica **no repositório**, não na classe `Predicates` — a `Predicates` só resolve a combinação condicional quando pelo menos um filtro existe. Replicar esse split exato nas Tasks 4 e 5 (`EmployeeQueryPredicates`/`PositionPredicates`), que têm o mesmo formato de `findAllFiltered`. `CompanyPredicates` (Task 7) é a exceção: o atalho de "nenhum filtro" vira o fallback `id.isNotNull()` **dentro** da própria `Predicates`, porque é assim que o código original (`CompanyServiceBean.findAll`) já funciona — não inventar um atalho novo que mudaria o predicate gerado (SQL diferente, mesmo resultado, mas comportamento não é bit-a-bit idêntico ao original).

### Onde cada peça vai (camadas)

- `pom.xml` (raiz): `maven-surefire-plugin` ganha `<version>3.5.4</version>` no `pluginManagement` (Task 1) — único arquivo fora do módulo `domain` tocado por esta story.
- `corporate/catalog/internal/`: `AddressTypePredicates`/`ContactTypePredicates` (já existem) + 2 testes novos (Task 2).
- `access/status/internal/`: 4 `ReasonXPredicates` novas + 4 testes novos; 4 `ReasonXRepository` refatorados (Task 3).
- `corporate/employee/internal/`: `EmployeeQueryPredicates` nova + teste; `EmployeeQueryRepository` refatorado (Task 4).
- `corporate/position/internal/`: `PositionPredicates` nova + teste; `PositionRepository` refatorado (Task 5).
- `corporate/department/internal/`: `DepartmentPredicates` nova + teste; `DepartmentRepository` refatorado (Task 6).
- `corporate/company/internal/`: `CompanyPredicates` nova + teste; `CompanyRepository` ganha `findAllFiltered` novo (aditivo, nada removido).
- `corporate/company/service/`: `CompanyServiceBean.findAll` refatorado (remove `BooleanBuilder`/`QCompany`); `CompanyServiceBeanTest` ajustado no mock de `findAll`.
- Nenhuma mudança em `usecase`, `api`, `boot`, YAML de contrato, Liquibase, ou `ScosGeotemporalPermission`.

### Testing Standards

- Teste de `Predicates`: JUnit 5 puro (`@Test`, sem `@ExtendWith(MockitoExtension.class)`, sem Spring) — a classe só monta e retorna `com.querydsl.core.types.Predicate`, não precisa de mock nem de contexto.
- Classe de teste no **mesmo pacote** `internal` do main (não `internal.predicates` nem outro sub-pacote) — obrigatório, porque `XxxPredicates` e seus métodos `static` são package-private (sem modificador), mesma visibilidade que `AddressTypePredicates`/`ContactTypePredicates` já usam.
- Asserção padrão: montar o `Predicate` esperado no teste com o mesmo `BooleanBuilder`/`Q`-type e comparar via `assertThat(actual.toString()).isEqualTo(expected.toString())` — evita depender de `equals()` de `com.querydsl.core.types.dsl` (implementação interna do QueryDSL, não é contrato público estável para comparação direta).
- Nomenclatura: `<ClassePredicates>Test`, mesmo padrão de `<ClasseTestada>Test` já convencionado no projeto.
- `CompanyServiceBeanTest` continua sendo teste de `ServiceBean` (Mockito, `@ExtendWith(MockitoExtension.class)`) — só o `when(...)` de `findAll` muda de alvo (Task 7); nenhum outro teste da classe é afetado.
- Depois da Task 1, `mvn test` volta a ser confiável — todas as Tasks seguintes devem ser validadas rodando a suíte de verdade, não só compilando.

### Project Structure Notes

- Nenhum pacote Maven novo — tudo cai em pacotes `internal` já existentes de cada agregado.
- Nenhuma classe `Predicates` é `public` — todas `final class` package-private, mesmo padrão de `AddressTypePredicates`/`ContactTypePredicates`, para não vazar detalhe de montagem QueryDSL fora do agregado (reforça a fronteira "nada fora do agregado importa de `internal/` de outro agregado", já documentada em `project-context.md`).
- `CompanyRepository.findAllFiltered` é o único método novo em uma interface pública de repositório (as outras 8 `Predicates` só reorganizam código já existente, sem adicionar método novo à API do repositório).
- A correção do `pom.xml` raiz (Task 1) é aditiva (`<version>` a mais num `<plugin>` já declarado) — não remove nem reordena nada existente.

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Epic 2 / Story 2.6] — Given/When/Then originais (story criada nesta sessão, a pedido do usuário — ajuste técnico antes de iniciar o Epic 3, não vem de FR do PRD).
- [Source: pom.xml:162-165] — declaração atual de `maven-surefire-plugin` sem `<version>`, causa raiz do AC 1 (Task 1).
- [Source: pom.xml:137-141] — `maven-failsafe-plugin` já pinado em `3.2.5`, padrão a replicar para `maven-surefire-plugin`.
- [Source: ~/.m2/repository/org/apache/maven/plugins/maven-surefire-plugin/] — versões cacheadas localmente (`2.17`, `3.5.4`, `3.6.0-M1`); `3.5.4` já validada nesta sessão e nas Dev Agent Records das Stories 2.3/2.5 como workaround manual.
- [Source: _bmad-output/implementation-artifacts/2-5-consulta-funcionario.md#Debug Log References] — origem do workaround manual de surefire (`mvn org.apache.maven.plugins:maven-surefire-plugin:3.5.4:test ...`), que a Task 1 desta story elimina.
- [Source: organization/flow-organization-domain/.../corporate/catalog/internal/AddressTypePredicates.java] — referência de forma exata da classe `Predicates` (campo `Q`-type `private final static`, métodos `static` retornando `Predicate`).
- [Source: organization/flow-organization-domain/.../corporate/catalog/internal/ContactTypePredicates.java] — mesma referência, já com cabeçalho de licença (padrão a replicar em `AddressTypePredicates`, Task 2).
- [Source: organization/flow-organization-domain/.../corporate/catalog/internal/AddressTypeRepository.java] — referência de como o repositório passa a importar e chamar os métodos estáticos da `Predicates` via `import static`.
- [Source: organization/flow-organization-domain/.../access/status/internal/ReasonEnableRepository.java, ReasonDisableRepository.java, ReasonActivateRepository.java, ReasonInactivateRepository.java] — confirmado por `diff` nesta sessão: as 4 classes são idênticas em forma (só nome/campo mudam), fonte da Task 3.
- [Source: organization/flow-organization-domain/.../corporate/employee/internal/EmployeeQueryRepository.java] — fonte da Task 4, inclui o padrão de atalho "sem filtro" em `findAllFiltered` a preservar.
- [Source: organization/flow-organization-domain/.../corporate/position/internal/PositionRepository.java] — fonte da Task 5.
- [Source: organization/flow-organization-domain/.../corporate/department/internal/DepartmentRepository.java] — fonte da Task 6, inclui `existsByIdAndPositionsActive` (fora de escopo, AC 4).
- [Source: organization/flow-organization-domain/.../corporate/company/internal/CompanyRepository.java] — fonte de referência dos métodos que ficam intocados (predicate direto sem `BooleanBuilder`, Task 8).
- [Source: organization/flow-organization-domain/.../corporate/company/service/CompanyServiceBean.java:192-207] — fonte exata da lógica a migrar para `CompanyPredicates` (Task 7), incluindo o fallback `id.isNotNull()`.
- [Source: organization/flow-organization-domain/src/test/.../corporate/company/service/CompanyServiceBeanTest.java:321-334] — teste a ajustar (Task 7); linhas 188/199 usam `Predicate` para outros mocks, intocadas.
- [Source: organization/flow-organization-domain/src/test/.../access/system/internal/ScosSystemRepositoryPersistenceTest.java] — único precedente de teste em pacote `internal` hoje no módulo (`@DataJpaTest`, propósito diferente — persistência, não montagem de predicate); confirma que colocar teste no pacote `internal` é padrão aceito no projeto.
- [Source: _bmad-output/project-context.md#Repositórios] — "Consultas de existência/dinâmicas via QueryDSL (BooleanBuilder, QXxx.xxx), como default method na própria interface" — regra existente que esta story refina, sem contradizer (o `default method` continua na interface; só a montagem interna do predicate sai para uma classe irmã).
- [Source: _bmad-output/project-context.md#Build] — "⚠️ Ambos falham hoje no enforcer... Contorno temporário: -Denforcer.skip=true" — débito pré-existente e distinto do AC 1 desta story; `-Denforcer.skip=true` continua necessário, a Task 1 só resolve o surefire (Task 8, guarda de escopo).

## Dev Agent Record

### Agent Model Used

Claude Sonnet 5 (claude-sonnet-5), via Claude Code, workflow `bmad-dev-story`.

### Debug Log References

- `maven-surefire-plugin` pinado em `3.5.4` no `pluginManagement` do `pom.xml` raiz (mesmo bloco de `maven-failsafe-plugin`). Confirmado via `mvn -pl organization/flow-organization-domain help:effective-pom -Denforcer.skip=true` que o resolvido passou de `2.17` para `3.5.4`.
- Suíte completa executada e verde nos 3 módulos, com Docker disponível neste ambiente (diferente das sessões anteriores, 2.4/2.5): `domain` 339 testes, `usecase` 217 testes, `boot` 455 testes — todos `Failures: 0, Errors: 0`. `mvn -pl organization/flow-organization-boot -am test -Denforcer.skip=true` rodou os containers reais (Postgres/Redis/WireMock/GrpcMock) sem intervenção manual.
- Todos os comandos usaram `-Denforcer.skip=true` (débito pré-existente do enforcer, fora de escopo desta story — Task 8).

### Completion Notes List

- Todas as 7 ACs implementadas. AC 1 (surefire): `pom.xml` raiz ganhou `<version>3.5.4</version>` no `maven-surefire-plugin` dentro de `pluginManagement`, eliminando o `Tests run: 0` silencioso; verificado com a suíte completa dos 3 módulos rodando de verdade.
- AC 2 (catalog): `AddressTypePredicates`/`ContactTypePredicates` já existiam no working tree; adicionado cabeçalho Apache 2.0 faltante em `AddressTypePredicates` e criados `AddressTypePredicatesTest`/`ContactTypePredicatesTest` (5 testes cada).
- AC 3 (Reason*): `ReasonEnablePredicates`/`ReasonDisablePredicates`/`ReasonActivatePredicates`/`ReasonInactivatePredicates` criadas, idênticas em forma (confirmado por design — mesmo padrão da catalog), cada uma com teste dedicado (5 testes cada).
- AC 4 (Employee/Position/Department): `EmployeeQueryPredicates` (6 métodos, 9 testes incluindo os 4 cenários do dinâmico), `PositionPredicates` (4 métodos, 6 testes), `DepartmentPredicates` (3 métodos, 3 testes) — `DepartmentRepository.existsByIdAndPositionsActive` intocado conforme guarda de escopo.
- AC 5 (Company): `CompanyPredicates` nova (`predicateStatusAndName`, réplica exata da lógica antes inline em `CompanyServiceBean.findAll`, incluindo o fallback `id.isNotNull()`); `CompanyRepository.findAllFiltered` novo (aditivo); `CompanyServiceBean.findAll` simplificado para delegar ao repositório, removendo `BooleanBuilder`/`QCompany`; `CompanyServiceBeanTest.findAllShouldReturnMappedPage` ajustado para mockar `findAllFiltered` em vez de `findAll(Predicate, Pageable)`; `CompanyPredicatesTest` novo (5 testes, incluindo o fallback vazio).
- AC 6 (padrão de teste): todas as 10 classes `XxxPredicatesTest` seguem JUnit 5 puro, mesmo pacote `internal` do main (visibilidade package-private), asserção via `predicate.toString()`.
- AC 7 (regressão): suíte completa dos 3 módulos passa; único ajuste de asserção fora dos arquivos novos foi o mock de `CompanyServiceBeanTest.findAllShouldReturnMappedPage`, previsto na story.
- Nenhuma mudança em `usecase`, `api`, `boot` (código), YAML de contrato, Liquibase ou `ScosGeotemporalPermission`.
- Notas: durante a análise de escopo (Task 8), identifiquei que `organization/flow-organization-domain/pom.xml`, `organization/flow-organization-resources/pom.xml`, `.../ScosFlowOrganizationLiquibaseProperties.java`, `.../triggers.yml` e `.../liquibase.properties` já estavam staged no início desta sessão com trabalho não relacionado a esta story (relocação do `liquibase-maven-plugin`, correção de path/extensão de changelog, `runOnChange: true` em triggers) — não tocados por esta story.

### File List

**Novos:**
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/access/status/internal/ReasonEnablePredicates.java`
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/access/status/internal/ReasonDisablePredicates.java`
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/access/status/internal/ReasonActivatePredicates.java`
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/access/status/internal/ReasonInactivatePredicates.java`
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/corporate/employee/internal/EmployeeQueryPredicates.java`
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/corporate/position/internal/PositionPredicates.java`
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/corporate/department/internal/DepartmentPredicates.java`
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/corporate/company/internal/CompanyPredicates.java`
- `organization/flow-organization-domain/src/test/java/br/com/sawcunhaos/organization/domain/corporate/catalog/internal/AddressTypePredicatesTest.java`
- `organization/flow-organization-domain/src/test/java/br/com/sawcunhaos/organization/domain/corporate/catalog/internal/ContactTypePredicatesTest.java`
- `organization/flow-organization-domain/src/test/java/br/com/sawcunhaos/organization/domain/access/status/internal/ReasonEnablePredicatesTest.java`
- `organization/flow-organization-domain/src/test/java/br/com/sawcunhaos/organization/domain/access/status/internal/ReasonDisablePredicatesTest.java`
- `organization/flow-organization-domain/src/test/java/br/com/sawcunhaos/organization/domain/access/status/internal/ReasonActivatePredicatesTest.java`
- `organization/flow-organization-domain/src/test/java/br/com/sawcunhaos/organization/domain/access/status/internal/ReasonInactivatePredicatesTest.java`
- `organization/flow-organization-domain/src/test/java/br/com/sawcunhaos/organization/domain/corporate/employee/internal/EmployeeQueryPredicatesTest.java`
- `organization/flow-organization-domain/src/test/java/br/com/sawcunhaos/organization/domain/corporate/position/internal/PositionPredicatesTest.java`
- `organization/flow-organization-domain/src/test/java/br/com/sawcunhaos/organization/domain/corporate/department/internal/DepartmentPredicatesTest.java`
- `organization/flow-organization-domain/src/test/java/br/com/sawcunhaos/organization/domain/corporate/company/internal/CompanyPredicatesTest.java`

**Modificados:**
- `pom.xml` (raiz — `maven-surefire-plugin` ganhou `<version>3.5.4</version>`)
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/corporate/catalog/internal/AddressTypePredicates.java` (cabeçalho de licença)
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/access/status/internal/ReasonEnableRepository.java`
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/access/status/internal/ReasonDisableRepository.java`
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/access/status/internal/ReasonActivateRepository.java`
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/access/status/internal/ReasonInactivateRepository.java`
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/corporate/employee/internal/EmployeeQueryRepository.java`
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/corporate/position/internal/PositionRepository.java`
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/corporate/department/internal/DepartmentRepository.java`
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/corporate/company/internal/CompanyRepository.java`
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/corporate/company/service/CompanyServiceBean.java`
- `organization/flow-organization-domain/src/test/java/br/com/sawcunhaos/organization/domain/corporate/company/service/CompanyServiceBeanTest.java`

## Change Log

| Data | Descrição |
|------|-----------|
| 2026-08-16 | Story criada — ajuste técnico do Epic 2: (1) extração de regras de montagem de Predicate (QueryDSL `BooleanBuilder`) dos Repositories (e de `CompanyServiceBean`) para classes `XxxPredicates` dedicadas, com testes unitários; (2) correção do `maven-surefire-plugin` sem `<version>` pinada no `pom.xml` raiz, causa raiz de `mvn test` reportar `Tests run: 0` silenciosamente — verificado e corrigido nesta sessão (`3.5.4`, mesmo padrão de `maven-failsafe-plugin`). |
| 2026-08-16 | Story implementada: todas as 9 tasks concluídas. `maven-surefire-plugin` corrigido; 8 classes `XxxPredicates` novas + 2 finalizadas (catalog); 10 classes de teste novas (48 testes de Predicates); `CompanyServiceBean.findAll`/`CompanyServiceBeanTest` refatorados. Suíte completa verde: `domain` 339, `usecase` 217, `boot` 455 (Docker disponível nesta sessão). Status → review. |
