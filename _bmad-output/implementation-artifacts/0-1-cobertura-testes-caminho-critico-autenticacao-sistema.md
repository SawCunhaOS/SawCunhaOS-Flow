---
baseline_commit: 89fb5239f217f634f2eb8470760a46fb6b747e9c
---

# Story 0.1: Cobertura de Testes do Caminho Crítico de Autenticação de Sistema

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como time de plataforma,
Eu quero cobertura de teste no caminho de autenticação sistema-a-sistema (interceptor gRPC, cifra do secret, validação do secret) e um CI mínimo rodando esses testes automaticamente,
Para que os bugs já identificados por auditoria fiquem provados fechados e não voltem a regredir silenciosamente.

**Pré-requisito de fato para considerar a Onda 2 (secret em repouso) entregue** — hoje o crypto e o schema estão corretos, mas o caminho de validação nunca foi exercitado por teste.

## Acceptance Criteria

1. **Given** `TokenAuthorizationInterceptor`, `SystemSecretCryptoService` e `ScosSystemServiceBean.validateSecretKey` têm hoje **zero teste** (confirmado — nenhum arquivo de teste existe para nenhum dos três; `grpc-boot` não tem sequer `src/test`) **When** esta story é implementada **Then** os três ganham cobertura determinística, sem rede, sem `sleep`.
2. **Given** `SystemSecretCryptoService.tagLength` (`flow-organization-shared/.../utils/SystemSecretCryptoService.java`) tem default de campo `12`, mas o default do `@Value("${scos.security.tag-length:128}")` é `128` — inconsistentes (`ivLength` não tem esse problema: campo e `@Value` são `12` nos dois) **When** esta story é implementada **Then** o default do campo é alinhado para `128`.
3. **Given** `bootstrap.yml` referencia `${scos.security.maser-key}` (typo — falta o "t") em vez de `master-key`, presente em **dois** arquivos — `flow-organization-boot/src/main/resources/bootstrap.yml:240` **e** `server-fat/src/main/resources/bootstrap.yml:240` (não só um) — enquanto `application-dev.yml:44` já define a chave correta `scos.security.master-key` **When** esta story é implementada **Then** ambos os `bootstrap.yml` são corrigidos para `master-key`.
4. **Given** o HEAD commitado de `ScosSystemServiceBean.validateSecretKey` tinha `if (scosSystem.matchesSecret(code, ONE_DAY))` — sem negação (lançava exceção quando o secret **conferia**) e primeiro argumento `code` em vez do secret — bug real e grave, mas a árvore de trabalho **não commitada** no momento desta auditoria já tem `if (!scosSystem.matchesSecret(secretKey))`, corrigido nos dois pontos **When** esta story é implementada **Then** o Task correspondente garante esse estado correto de forma idempotente (funciona partindo de qualquer um dos dois pontos de partida) e adiciona o teste que reprovaria contra o HEAD antigo.
5. **Given** achado próprio desta auditoria, fora da lista original de bugs do prompt: `TokenAuthorizationInterceptor.java:109` captura `catch (NoSuchElementException ex)`, mas `ScosSystemServiceBean`/`ScosSystemService` nunca lançam essa exceção — sempre `ScosException` (`SCOS_SYSTEM_001`/`SCOS_SYSTEM_002`), que estende `RuntimeException` diretamente, não `NoSuchElementException` — o `catch` nunca dispara; hoje, tanto "sistema inexistente" quanto "secret incorreto" vazam como exceção não tratada em vez de fechar a chamada com `UNAUTHENTICATED` **When** esta story é implementada **Then** o `catch` passa a ser `catch (ScosException ex)`.
6. **Given** os cenários obrigatórios de `TokenAuthorizationInterceptorTest` (KEY-ACCESS ausente, KEY-ACCESS incorreto, token ausente, token Base64 inválido, token em formato inesperado sem `:`, code de sistema inexistente, caminho feliz) **When** implementados **Then** cada cenário negativo prova `UNAUTHENTICATED`, listener noop retornado, `call.close()` chamado exatamente uma vez **And** o caminho feliz prova que `handler.startCall` é invocado e que o `SecurityContext` fica montado (verificável dentro dos callbacks `onMessage`/`onHalfClose` do listener retornado).
7. **Given** os cenários obrigatórios de `SystemSecretCryptoServiceTest` (roundtrip `encrypt`→`decrypt`; dois `encrypt()` do mesmo valor produzem IV e ciphertext diferentes; `decrypt()` com master-key diferente falha; `decrypt()` com payload corrompido/truncado falha) **When** implementados **Then** todos passam instanciando `SystemSecretCryptoService` sem contexto Spring completo — campos `@Value` setados via reflection, `init()` (`@PostConstruct`, package-private) chamado manualmente.
8. **Given** os cenários obrigatórios de `validateSecretKey` (secret correto autentica sem lançar; secret incorreto lança `ScosException` `SCOS_SYSTEM_002`; code de sistema inexistente lança `SCOS_SYSTEM_001`) **When** implementados **Then** todos passam **And** nenhum teste de grace period/`previousSecretKey`/`previousSecretExpiresAt` é incluído — guarda de escopo, pertence à Story 0.2.
9. **Given** o teste de wiring do `SecretKeyConverter` (Escopo D) **When** persiste um `ScosSystem` via `@DataJpaTest` e lê `SECRET_KEY` direto via `JdbcTemplate` **Then** prova que o valor gravado na coluna é **diferente** do secret original (cifrado em repouso) **And**, ao recarregar via `ScosSystemRepository` (nova leitura, sessão limpa), o campo em memória volta ao valor original em claro.
10. **Given** `SecretKeyConverter` hoje **não** é `@Component` e não tem construtor sem argumentos (`@RequiredArgsConstructor` sobre `SystemSecretCryptoService crypto`, campo `final`) **When** o teste do item 9 falhar por erro de instanciação/NPE do converter **Then** adicionar `@Component` a `SecretKeyConverter` é parte desta story (causa mais provável, não hipótese remota — sem `@Component` e sem construtor vazio, Hibernate não tem como resolvê-lo sozinho).
11. **Given** não existe `.github/workflows` no projeto hoje, e o enforcer `RequireUpperBoundDeps` falha hoje em `infrastructure`/`flow-security-starter` — **verificado ao vivo nesta auditoria** (`mvn validate` no módulo `infrastructure` reproduz o erro, ver Dev Notes), débito pré-existente já documentado em `project-context.md`, não desta story **When** `.github/workflows/ci.yml` é criado **Then** dispara em `push`/`pull_request` para `feature/**` e `main`, roda `mvn -B verify -Denforcer.skip=true` **And** não configura JaCoCo com threshold nem `dependency-check` — deliberadamente mínimo.
12. **Given** a guarda de escopo **When** esta story é implementada **Then** NÃO cobre `flow-organization-api` (zero testes — vira nota de backlog do Epic 0, já registrada) **And** NÃO cria teste `*IT`/Testcontainers (idem) **And** NÃO implementa Use Case de rotação de secret (ainda não existe) **And** NÃO toca em grace period/`previousSecretKey` (Story 0.2).
13. **Given** a Story 0.2 (já escrita antes desta, mesmo Epic 0) também descreve `ScosSystemServiceBeanTest.java` como "arquivo novo" **When** as stories forem implementadas na ordem numérica (0.1 antes de 0.2, como a numeração sugere) **Then** esta story (0.1) é quem de fato cria o arquivo; a Story 0.2, ao ser implementada depois, só estende esse arquivo já existente — não recriar do zero nem duplicar a classe de teste.

## Tasks / Subtasks

- [x] Task 1: `TokenAuthorizationInterceptor` — bug do tipo de exceção + teste (AC: 1, 5, 6)
  - [x] `flow-organization-grpc-boot/pom.xml`: adicionar bloco de teste (`grpc-boot` não tem nenhum hoje), mesmo conjunto exato já usado em `flow-organization-domain/pom.xml`:
    ```xml
    <!-- Test -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.junit.platform</groupId>
        <artifactId>junit-platform-launcher</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    ```
  - [x] `flow-organization-grpc-boot/.../configuration/interceptor/TokenAuthorizationInterceptor.java:109`: trocar `catch (NoSuchElementException ex)` por `catch (ScosException ex)`; remover `import java.util.NoSuchElementException;`; adicionar `import br.com.sawcunhaos.foundation.utils.exception.ScosException;`.
  - [x] Criar `flow-organization-grpc-boot/src/test/java/br/com/sawcunhaos/organization/grpc/boot/configuration/interceptor/TokenAuthorizationInterceptorTest.java` (mesmo pacote, primeiro teste do módulo). **Não usar GrpcMock** (o precedente do projeto, `flow-organization-boot`, usa GrpcMock pra simular um servidor gRPC completo em teste de integração — aqui é unit test de um `ServerInterceptor` isolado, mais simples mockar `ServerCall`/`ServerCallHandler`/`Metadata` direto com Mockito/objetos reais):
    - `Metadata` real (não mockar — é um simples holder chave→valor; `Metadata.Key.of("KEY-ACCESS", Metadata.ASCII_STRING_MARSHALLER)` recriado no teste com o mesmo nome funciona porque `Metadata` indexa por nome da chave, não por identidade do objeto `Key`).
    - `ServerCall<ReqT,RespT>` — classe abstrata, `Mockito.mock(ServerCall.class)`; `call.getMethodDescriptor()` estubado retornando um `MethodDescriptor` mockado com `getFullMethodName()` retornando uma string de `PROTECTED_METHODS` (ex. `"br.com.sawcunhaos.organization.grpc.proto.RegistryService/registryResources"`) ou uma string qualquer fora da lista para os cenários que não exigem token.
    - `ServerCallHandler<ReqT,RespT>` — interface, mock; `next.startCall(call, headers)` estubado retornando `Mockito.mock(ServerCall.Listener.class)`.
    - `ScosSystemService` — mock; `validateSecretKey(...)` estubado com `doNothing()` (caminho feliz) ou `doThrow(new ScosException(SCOS_SYSTEM_001))`/`doThrow(new ScosException(SCOS_SYSTEM_002))` (negativos).
    - `ScosGRPCProperties` — instância real, `setKeyAccess("expected-key")`.
    - Casos:
      - KEY-ACCESS ausente (`headers` sem a chave) → `UNAUTHENTICATED`, `call.close(...)` uma vez, `next.startCall` **nunca** chamado.
      - KEY-ACCESS incorreto (valor presente, diferente) → idem.
      - Método não-protegido + KEY-ACCESS correto → segue direto pra `next.startCall`, sem exigir token (cobre a ramificação `if (!PROTECTED_METHODS.contains(methodName))`).
      - Método protegido, token ausente (`AUTHORIZATION_KEY` não setado) → `UNAUTHENTICATED`, listener noop.
      - Token com Base64 inválido (string arbitrária que não decodifica) → idem.
      - Token decodificado sem `:` (ex. `"semseparador"`) → idem.
      - Code de sistema inexistente (`validateSecretKey` estubado lançando `ScosException(SCOS_SYSTEM_001)`) → idem — **este caso é o que reprova contra o código atual antes do fix do Task 1** (catch errado).
      - Caminho feliz: KEY-ACCESS correto, método protegido, token válido (`Base64.getEncoder().encodeToString("CODE:secret".getBytes())`), `validateSecretKey` não lança → `next.startCall` invocado, listener retornado não é o noop, `call.close()` **nunca** chamado nesse fluxo.

- [x] Task 2: `SystemSecretCryptoService` — fix `tagLength` + teste (AC: 2, 7)
  - [x] `flow-organization-shared/.../utils/SystemSecretCryptoService.java`: `private int tagLength = 12;` → `private int tagLength = 128;` — conferido: árvore de trabalho já estava com `128` (idem ao caso de `ScosSystemServiceBean` no Task 3); nenhuma alteração necessária.
  - [x] Criar `flow-organization-shared/src/test/java/br/com/sawcunhaos/organization/shared/utils/SystemSecretCryptoServiceTest.java` (mesmo pacote). Sem Mockito (módulo `shared` não tem — não adicionar; usar JUnit 5 puro, mesmo estilo de `ExceptionCodeErrorTest`). Setar `masterKeyBase64`/`ivLength`/`tagLength` via `java.lang.reflect.Field` (`setAccessible(true)`) — `init()` é package-private, chamável direto por estar no mesmo pacote. Constante de teste: um Base64 que decodifique para exatamente 32 bytes (AES-256) — não reusar o literal de `application-dev.yml:44` por acoplamento a config externa; definir um local, ex. `Base64.getEncoder().encodeToString(new byte[32])` preenchido com bytes fixos determinísticos.
    - Roundtrip: `encrypt("valor")` seguido de `decrypt(...)` retorna `"valor"`.
    - Dois `encrypt("valor")` seguidos → decodificar os dois resultados Base64, comparar os primeiros `ivLength` bytes de cada um (o IV é prefixado ao ciphertext, ver `encrypt()`) — devem ser diferentes; e as strings Base64 completas também devem ser diferentes.
    - `decrypt()` com uma segunda instância de `SystemSecretCryptoService` com master-key **diferente** (outros 32 bytes) tentando decifrar o output da primeira → `assertThrows(ScosException.class, ...)`, código `SCOS_SECURITY_DECRYPT` (funciona por construção — GCM autentica o ciphertext; documentar em Dev Notes que isso já era correto, só faltava prova).
    - `decrypt()` com payload corrompido: pegar um `encrypt()` válido, alterar um caractere do Base64 (ou truncar), chamar `decrypt()` → mesma exceção.

- [x] Task 3: `ScosSystemServiceBean.validateSecretKey` — garantir fix + testes (AC: 1, 4, 8, 13)
  - [x] `flow-organization-domain/.../access/system/service/ScosSystemServiceBean.java`: **conferir** que `validateSecretKey` está como:
    ```java
    if (!scosSystem.matchesSecret(secretKey)) {
        throw new ScosException(SCOS_SYSTEM_002);
    }
    ```
    Se o ponto de partida for o HEAD commitado (`if (scosSystem.matchesSecret(code, ONE_DAY))`), aplicar a correção: negar a condição e trocar o argumento de `code` para `secretKey`. Se a árvore de trabalho já estiver como acima (estado no momento desta auditoria), só confirmar e seguir — não é preciso reescrever. **Confirmado: árvore de trabalho já estava correta, nenhuma reescrita necessária.**
  - [x] Criar `flow-organization-domain/src/test/java/br/com/sawcunhaos/organization/domain/access/system/service/ScosSystemServiceBeanTest.java` (arquivo novo — mesmo padrão `@Mock`/`@InjectMocks` de `CompanyServiceBeanTest`). **Não construir o `ScosSystem` de teste via persistência/converter** — é `Mockito`, o repositório é mockado, o valor de `secretKey` no objeto de teste já é o texto plano que o teste decide, sem nenhuma cifra envolvida (a cifra só acontece na fronteira JPA, que este teste não atravessa — ver Task 4).
    - `validateSecretKeyShouldNotThrowWhenSecretMatches`: `ScosSystem` com `secretKey = "correct-secret"`, `findByCode` mockado retornando esse objeto, chama `validateSecretKey(code, "correct-secret")` → não lança.
    - `validateSecretKeyShouldThrowScosSystem002WhenSecretDoesNotMatch`: mesmo setup, chama com `"wrong-secret"` → `assertThatThrownBy(...).hasFieldOrPropertyWithValue("code", SCOS_SYSTEM_002.getCode())`.
    - `validateSecretKeyShouldThrowScosSystem001WhenCodeNotFound`: `findByCode` retorna `Optional.empty()` → `SCOS_SYSTEM_001`.
    - **Não** setar `previousSecretKey`/`previousSecretExpiresAt` em nenhum teste — mantém fora do caminho de grace period (guarda de escopo, AC 12).

- [x] Task 4: Wiring do `SecretKeyConverter` — teste de persistência (AC: 1, 9, 10)
  - [x] `flow-organization-domain/pom.xml`: adicionar H2 em escopo de teste (módulo não tem hoje):
    ```xml
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>test</scope>
    </dependency>
    ```
  - [x] Criar `flow-organization-domain/src/test/java/br/com/sawcunhaos/organization/domain/access/system/internal/ScosSystemRepositoryPersistenceTest.java` (**primeiro `@DataJpaTest` do projeto** — sem precedente a seguir, ver Dev Notes para diagnóstico se algo não subir de primeira):
    ```java
    @DataJpaTest
    @Import({SecretKeyConverter.class, SystemSecretCryptoService.class})
    @TestPropertySource(properties = "scos.security.master-key=" + /* Base64 de 32 bytes fixo */ "")
    class ScosSystemRepositoryPersistenceTest {

        @Autowired private ScosSystemRepository scosSystemRepository;
        @Autowired private TestEntityManager entityManager;
        @Autowired private JdbcTemplate jdbcTemplate;

        @Test
        void secretKeyShouldBeEncryptedAtRestAndDecryptedOnReload() {
            ScosSystem system = ScosSystem.builder()
                    .code("TEST_SYS").name("Test").description("Test")
                    .secretKey("plain-secret-value").status("ACTIVE").version("1.0.0")
                    .build();
            system.updateAuditInfo("test");

            ScosSystem saved = entityManager.persistFlushFind(system);
            entityManager.clear();

            String rawColumn = jdbcTemplate.queryForObject(
                    "SELECT SECRET_KEY FROM SCOS_SYSTEM WHERE SYSTEM_ID = ?",
                    String.class, saved.getId());
            assertThat(rawColumn).isNotEqualTo("plain-secret-value");

            ScosSystem reloaded = scosSystemRepository.findById(saved.getId()).orElseThrow();
            assertThat(reloaded.getSecretKey()).isEqualTo("plain-secret-value");
        }
    }
    ```
  - [x] Rodar/inspecionar o teste. **Se falhar por `NPE`/erro de instanciação do converter** (causa mais provável — ver AC 10): adicionar `@Component` a `flow-organization-shared/.../converter/SecretKeyConverter.java`. **Não** adicionar construtor sem-args nem mudar `@RequiredArgsConstructor` — só o `@Component` (Spring Boot já faz a ponte Hibernate↔Spring bean container automaticamente via `HibernateJpaAutoConfiguration`, incluída na fatia `@DataJpaTest`; evidência de que essa ponte já funciona no projeto: `ScosHibernateAuditListener`, da foundation, tem exatamente essa mesma forma — `@RequiredArgsConstructor` sobre dependência Spring, sem construtor vazio — e já é instanciado com sucesso pelo Hibernate hoje).
    - **Resultado real:** o teste, como o próprio Task 4 prescreve, usa `@Import({SecretKeyConverter.class, SystemSecretCryptoService.class})` — isso já registra `SecretKeyConverter` como bean Spring independente de `@Component`, então o teste passou de primeira sem precisar do `@Component`. Porém: `ScosOrganizationApplication` declara `@ComponentScan(basePackages = {"br.com.sawcunhaos"})`, que cobriria `br.com.sawcunhaos.organization.shared.converter` — e `SecretKeyConverter` não tinha `@Component`, então em produção (fora deste teste isolado, que não passa pelo component-scan real) ele nunca seria descoberto pelo scan; como também não tem construtor sem-args (`@RequiredArgsConstructor` sobre `crypto`), a resolução via reflection do Hibernate falharia. Adicionado `@Component` mesmo assim — corrige o risco real de NPE/falha de instanciação em produção na primeira escrita de um `ScosSystem`, consistente com a própria justificativa da AC 10.
    - **Achado adicional, fora das ACs originais:** nenhum módulo do projeto configura `@EnableJpaRepositories(repositoryBaseClass=...)` — sem isso, os métodos extras de `BaseJpaRepository` (`persist`/`merge`/`update`/`lockById`, usados por `ScosSystemServiceBean.createSystem`/`updateSystem`) não resolvem via `SimpleJpaRepository` e o Spring Data tenta (e falha) interpretá-los como método de query derivada. Sem esse ajuste, o teste deste Task nem chegava a criar o bean do repositório. Configurado `repositoryBaseClass = BaseJpaRepositoryImpl.class` **apenas no `@EnableJpaRepositories` deste teste** (não alterei configuração de produção — fora do escopo desta story); registrar como débito a investigar separadamente se o mesmo já não está quebrado em produção.
    - **Ajustes de infraestrutura de teste não previstos no código-esqueleto original** (Spring Boot 4.1 reorganizou pacotes de teste): `DataJpaTest`/`TestEntityManager`/`EntityScan` migraram de pacote (`org.springframework.boot.data.jpa.test.autoconfigure`, `org.springframework.boot.jpa.test.autoconfigure`, `org.springframework.boot.persistence.autoconfigure`) e exigem a dependência `spring-boot-starter-data-jpa-test` (adicionada ao pom, versão gerida pelo BOM). Módulo `domain` não tem `@SpringBootApplication` (é biblioteca) — criado `DomainTestApplication` (`@SpringBootConfiguration @AutoConfigurationPackage`) em `src/test/java/.../domain/` para servir de âncora de contexto a qualquer `@DataJpaTest` futuro no módulo. `@EntityScan`/`@EnableJpaRepositories` restritos a `ScosSystem`/`ScosSystemRepository` para não escanear entidades com tipo JSON do hypersistence-utils (exigem Jackson, ausente do classpath de teste deste módulo).

- [x] Task 5: `bootstrap.yml` × 2 — corrigir typo (AC: 3)
  - [x] `organization/flow-organization-boot/src/main/resources/bootstrap.yml:240`: `master-key: ${scos.security.maser-key}` → `master-key: ${scos.security.master-key}`. **Já corrigido** no commit `faee837` ("fix: Correcao no campo do bootstrap"), anterior ao início desta story — conferido, nenhuma alteração necessária.
  - [x] `server-fat/src/main/resources/bootstrap.yml:240`: idem — mesmo commit, já corrigido.
  - [x] `organization/flow-organization-grpc-boot/src/main/resources/bootstrap.yml`: **não tocar** — conferido: tem `security.enabled: false` e uma linha `master-key: ${scos.security.master-key}` já correta (sem typo); módulo usa `TokenAuthorizationInterceptor`, mecanismo próprio, não depende do `scos-foundation-security`.

- [x] Task 6: CI mínimo (AC: 11)
  - [x] Criar `.github/workflows/ci.yml`:
    ```yaml
    name: CI

    on:
      push:
        branches: [main, "feature/**"]
      pull_request:
        branches: [main, "feature/**"]

    jobs:
      verify:
        runs-on: ubuntu-latest
        steps:
          - uses: actions/checkout@v4
          - uses: actions/setup-java@v4
            with:
              distribution: temurin
              java-version: "25"
          - uses: actions/cache@v4
            with:
              path: ~/.m2/repository
              key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}
              restore-keys: ${{ runner.os }}-maven-
          - run: mvn -B verify -Denforcer.skip=true
    ```
    `-Denforcer.skip=true` é **necessário hoje** — verificado ao vivo nesta auditoria (`mvn -q -pl organization/flow-organization-infrastructure -am validate` reproduz `Require upper bound dependencies error` para `error_prone_annotations`/`prometheus-metrics-*`, débito pré-existente do `project-context.md`, item 1 de *Dívidas registradas*). **Não** é uma "correção" — é o mesmo contorno documentado que o projeto já usa localmente. Não remover sem antes resolver a convergência no `dependencyManagement` raiz.
  - [x] **Não** adicionar JaCoCo com `<rules>`/threshold, nem `dependency-check-maven` com feed NVD — deliberadamente fora desta story (iniciativa de CI mais ampla, mapeada separadamente).

- [x] Task 7: Guarda de escopo (AC: 12)
  - [x] NÃO cobrir `flow-organization-api` — zero testes hoje, fica como nota de backlog do Epic 0 (já registrada em `epics.md`). Confirmado: nenhum arquivo tocado nesse módulo.
  - [x] NÃO criar teste `*IT`/Testcontainers — mesma lógica, backlog do épico. Confirmado.
  - [x] NÃO implementar Use Case de rotação de secret — não existe ainda. Confirmado.
  - [x] NÃO escrever teste de grace period, `previousSecretKey` ou rotação — pertence à Story 0.2 (Clock/Instant). Confirmado: `ScosSystemServiceBeanTest` só cobre os 3 cenários prescritos.
  - [x] NÃO configurar JaCoCo check/threshold nem `dependency-check` no workflow — só execução. Confirmado no `ci.yml`.

## Dev Notes

### Contexto crítico — leia antes de implementar

**Estado HEAD vs. árvore de trabalho — leia isto antes de "corrigir" `validateSecretKey`.** No momento em que esta story foi escrita, o repositório tinha mudanças não commitadas em `ScosSystem.java`/`ScosSystemServiceBean.java` (`git status` mostrava os dois como modificados). O `git diff` contra HEAD mostrava exatamente:
```diff
-        if (scosSystem.matchesSecret(code, ONE_DAY) ) {
+        if (!scosSystem.matchesSecret(secretKey) ) {
             throw new ScosException(SCOS_SYSTEM_002);
         }
```
Ou seja: **o bug descrito no prompt original desta story (condição sem negação + argumento `code` no lugar do secret) é real no HEAD commitado, mas já estava corrigido na árvore de trabalho não commitada** no momento desta auditoria. Essa mudança pode ou não sobreviver até quem implementar esta story (pode ser commitada, descartada, ou reescrita por outra story no meio do caminho). O Task 3 foi escrito para ser **idempotente**: primeiro confira o estado atual do método; só reescreva se necessário. O teste que você escrever é o que garante isso não regredir de novo, independente de qual das duas versões você encontrar.

**Por que NÃO chamar `SystemSecretCryptoService` dentro de `matchesSecret`/`validateSecretKey`** (o prompt original desta story sugeria "decifrando via SystemSecretCryptoService antes da comparação constant-time" — **não fazer isso**, é um mal-entendido, não uma instrução a seguir literalmente): `ScosSystem.secretKey`/`previousSecretKey` têm `@Convert(converter = SecretKeyConverter.class)` (`ScosSystem.java:59-64`). O `AttributeConverter` já decifra na leitura e cifra na escrita **de forma transparente, na fronteira JPA/Hibernate** — quando o objeto `ScosSystem` está em memória (vindo do repositório), `secretKey` já é texto plano. Chamar `SystemSecretCryptoService.decrypt(...)` de novo em cima de um valor que já está em claro tentaria decifrar texto plano como se fosse ciphertext Base64+IV — ia falhar com `ScosException(SCOS_SECURITY_DECRYPT)` na maioria dos casos, ou pior, silenciosamente em algum caso degenerado. `matchesSecret` compara direto contra o campo em memória (`constantTimeEquals`), sem chamada de crypto nenhuma — está certo assim. Confirmado também pelo lado do chamador: `TokenAuthorizationInterceptor.java:100-108` decodifica o token Base64 (`code:secret`) e passa `parts[1]` — o secret **em texto plano**, como o cliente o conhece — direto para `validateSecretKey`. Não há nenhum ponto no caminho onde um valor cifrado chegaria a este método.

**Achado próprio: bug do tipo de exceção no interceptor (AC 5), não estava na lista original do prompt.** `TokenAuthorizationInterceptor.java:107-113`:
```java
try {
    scosSystemService.validateSecretKey(parts[0], parts[1]);
} catch (NoSuchElementException ex) {
    ...
}
```
`ScosSystemServiceBean`/`ScosSystemService` **nunca** lançam `java.util.NoSuchElementException` — só `ScosException` (`br.com.sawcunhaos.foundation.utils.exception.ScosException extends RuntimeException`, confirmado lendo a classe na foundation). O `catch` está pegando o tipo errado; nunca vai disparar. Hoje, se o code não existe (`SCOS_SYSTEM_001`) ou o secret está errado (`SCOS_SYSTEM_002`), a exceção sobe sem tratamento pelo interceptor — não vira `UNAUTHENTICATED` limpo. Esse é o motivo pelo qual o teste do cenário "code de sistema inexistente" (Task 1) **vai reprovar contra o código atual antes do fix** — exatamente o mesmo padrão que "escrever o teste primeiro expõe o bug" já visto em `validateSecretKey`.

**Bootstrap.yml — dois arquivos, não um.** O prompt original citou "o bootstrap.yml" no singular. Busquei o typo `maser-key` no projeto inteiro: aparece em `organization/flow-organization-boot/src/main/resources/bootstrap.yml:240` **e** em `server-fat/src/main/resources/bootstrap.yml:240` — mesma linha, mesmo typo, dois arquivos (são composition roots distintos, cada um com seu próprio `bootstrap.yml`). `application-dev.yml:44` (também duplicado nos mesmos dois módulos) já define a chave certa, `scos.security.master-key`. `flow-organization-grpc-boot/bootstrap.yml` **não** tem essa linha — esse módulo usa `security.enabled: false` e autentica via `TokenAuthorizationInterceptor`, mecanismo separado do `scos-foundation-security`.

**Enforcer quebrado — verificado ao vivo, não só citado do `project-context.md`.** Rodei `mvn -q -pl organization/flow-organization-infrastructure -am validate` durante esta auditoria (2026-07-19) e reproduzi o erro `Require upper bound dependencies error` para `com.google.errorprone:error_prone_annotations` (2.48.0 gerenciado vs. 2.49.0 nearest de `caffeine`) e para `io.prometheus:prometheus-metrics-{core,exposition-formats,tracer-common}` (1.5.1 gerenciado vs. 1.7.0 nearest de `micrometer-registry-prometheus`). Isso confirma que o workflow de CI desta story **precisa** de `-Denforcer.skip=true` pra sequer rodar — sem isso, `mvn -B verify` falha na fase `validate`, antes de qualquer teste executar, para QUALQUER módulo que dependa (direta ou transitivamente) de `infrastructure`. Não investiguei além disso (corrigir a convergência é escopo de outra iniciativa, já registrada como dívida no `project-context.md`).

### `@DataJpaTest` é novo neste projeto — nenhum precedente a seguir

Todo teste de domain hoje é unitário Mockito (`XxxServiceBeanTest`); toda persistência real é testada só em `boot`, via `ScosOrganizationTestUtil` (Testcontainers Postgres singleton). Esta story introduz a primeira fatia intermediária: `@DataJpaTest` com H2 embarcado, sem Postgres, sem Liquibase (Hibernate gera o schema da entidade automaticamente — `domain` não tem nenhum `application.yml` hoje, então não há `ddl-auto`/`spring.liquibase` conflitando com o default do slice). Pontos de atenção:
- `@DataJpaTest` **não** faz component-scan completo — só pega `@Entity`, repositórios Spring Data e infraestrutura JPA. `SecretKeyConverter`/`SystemSecretCryptoService` (Task 4) precisam de `@Import(...)` explícito na classe de teste, senão o contexto nem os enxerga.
- `SystemSecretCryptoService.masterKeyBase64` precisa vir de algum lugar — `@TestPropertySource(properties = "scos.security.master-key=...")` direto na classe de teste, autocontido, sem depender de `application-dev.yml`.
- Ponte Hibernate↔Spring para instanciar o converter: não configurada explicitamente em nenhum lugar do projeto (`hibernate.resource.beans.container` não aparece em nenhum `.yml`) — mas `ScosHibernateAuditListener` (foundation) já prova que o Spring Boot padrão resolve isso sozinho via `HibernateJpaAutoConfiguration`, incluída na fatia `@DataJpaTest`. Alta confiança de que funciona, não testado ao vivo nesta auditoria (não rodei Maven pra este cenário específico — só o `validate` do enforcer). Se travar, comece o diagnóstico por aí.

### Testing Standards

- `TokenAuthorizationInterceptorTest`: JUnit 5 + Mockito puro, sem `@SpringBootTest` — o interceptor é testado como POJO (`new TokenAuthorizationInterceptor(mockService, properties)`), sem subir contexto Spring nem servidor gRPC real.
- `SystemSecretCryptoServiceTest`: JUnit 5 puro (módulo `shared` não tem Mockito/AssertJ — não adicionar, usar `org.junit.jupiter.api.Assertions`, mesmo estilo de `ExceptionCodeErrorTest`).
- `ScosSystemServiceBeanTest`: JUnit 5 + Mockito + AssertJ, mesmo padrão de `CompanyServiceBeanTest`/`CnaeServiceBeanTest` (`@Mock`/`@InjectMocks`, nomenclatura `xShouldYWhenZ`).
- `ScosSystemRepositoryPersistenceTest`: `@DataJpaTest`, primeiro do projeto — ver seção acima.

### Project Structure Notes

- `flow-organization-grpc-boot` ganha `src/test/java` pela primeira vez.
- `flow-organization-shared` ganha um segundo arquivo de teste (`ExceptionCodeErrorTest` já existe) — sem nova dependência de teste.
- `flow-organization-domain` ganha H2 (só teste) e o primeiro `@DataJpaTest`.
- `.github/workflows/` é criado pela primeira vez no projeto.
- Nenhum módulo Maven novo.

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Epic 0 / Story 0.1] — Given/When/Then originais.
- [Source: organization/flow-organization-grpc-boot/.../configuration/interceptor/TokenAuthorizationInterceptor.java] — lido por completo; linhas 69-113 são o núcleo testado; linha 109 é o bug do tipo de exceção (achado próprio).
- [Source: organization/flow-organization-shared/.../utils/SystemSecretCryptoService.java] — lido por completo; `tagLength` (linha ~44) é o bug do default.
- [Source: organization/flow-organization-shared/.../converter/SecretKeyConverter.java] — confirmado: `@Converter` sem `@Component`, `@RequiredArgsConstructor` sem construtor vazio.
- [Source: organization/flow-organization-domain/.../access/system/service/ScosSystemServiceBean.java, .../access/system/internal/ScosSystem.java] — estado HEAD (via `git diff`) e estado da árvore de trabalho, ambos citados acima.
- [Source: organization/flow-organization-domain/.../access/system/specification/ScosSystemService.java] — confirma que `validateSecretKey` só pode lançar `ScosException` (`getByCode`) ou nada.
- [Source: organization/flow-organization-boot/src/main/resources/bootstrap.yml:240, server-fat/src/main/resources/bootstrap.yml:240] — typo `maser-key`, dois arquivos.
- [Source: organization/flow-organization-boot/src/main/resources/application-dev.yml:44, server-fat/src/main/resources/application-dev.yml:44] — chave correta já existe, só a interpolação do bootstrap está errada.
- [Source: organization/flow-organization-domain/pom.xml] — bloco de dependência de teste replicado em `grpc-boot`.
- [Source: _bmad-output/project-context.md#Build quebrado] — enforcer já documentado; reproduzido ao vivo nesta auditoria.
- [Source: _bmad-output/implementation-artifacts/0-2-padronizar-tipos-temporais-clock-injetavel.md] — dependência de ordenação (AC 13): aquela story também cria `ScosSystemServiceBeanTest`; esta é quem cria de fato se implementada primeiro.
- [Source: organization/flow-organization-domain/src/test/java/.../corporate/company/service/CompanyServiceBeanTest.java] — padrão de teste replicado.

## Dev Agent Record

### Agent Model Used

claude-sonnet-5

### Debug Log References

- `mvn -pl organization/flow-organization-grpc-boot test -Dtest=TokenAuthorizationInterceptorTest -Denforcer.skip=true` → 8/8 passam.
- `mvn -pl organization/flow-organization-shared test -Dtest=SystemSecretCryptoServiceTest -Denforcer.skip=true` → 4/4 passam.
- `mvn -pl organization/flow-organization-domain test -Dtest=ScosSystemServiceBeanTest -Denforcer.skip=true` → 3/3 passam.
- `mvn -pl organization/flow-organization-domain test -Dtest=ScosSystemRepositoryPersistenceTest -Denforcer.skip=true` → 1/1 passa (ver Completion Notes para o caminho até chegar lá).
- `mvn test -Denforcer.skip=true` (raiz do reactor `organization`, Docker ativo) → **736 testes, 0 falhas, 0 erros, 0 skipped** — regressão completa, inclui os 12 relatórios de `flow-organization-boot` (integração real Postgres/Redis/WireMock/GrpcMock).

### Completion Notes List

- **Tasks 2, 3, 5 eram idempotentes e já estavam corrigidas** na árvore de trabalho antes desta implementação (confirmado, não reescrito): `SystemSecretCryptoService.tagLength` já era `128`; `ScosSystemServiceBean.validateSecretKey` já usava `!matchesSecret(secretKey)`; os dois `bootstrap.yml` já tinham `master-key` correto (commit `faee837`, anterior a esta story). Testes foram escritos do mesmo jeito para provar o comportamento e travar contra regressão futura.
- **Task 1**: corrigido `catch (NoSuchElementException ex)` → `catch (ScosException ex)` em `TokenAuthorizationInterceptor` (bug real — o catch antigo nunca disparava, exceção subia sem tratamento). `grpc-boot` ganhou `src/test` pela primeira vez.
- **Task 4 — maior complexidade real da story**, bem além do previsto no esqueleto original:
  - `@DataJpaTest`/`TestEntityManager`/`@EntityScan` mudaram de pacote no Spring Boot 4.1 (`org.springframework.boot.data.jpa.test.autoconfigure`, `org.springframework.boot.jpa.test.autoconfigure`, `org.springframework.boot.persistence.autoconfigure`) e passaram a exigir a dependência separada `spring-boot-starter-data-jpa-test` — adicionada ao pom (versão gerida pelo BOM).
  - Módulo `domain` não tem `@SpringBootApplication` (é biblioteca) — criada `DomainTestApplication` (`@SpringBootConfiguration @AutoConfigurationPackage`) para servir de âncora de contexto a `@DataJpaTest`.
  - `@EntityScan`/`@EnableJpaRepositories` restritos a `ScosSystem`/`ScosSystemRepository` — o slice padrão escanearia todas as entidades/repositórios do módulo, incluindo tipos JSON do hypersistence-utils que exigem Jackson (ausente do classpath de teste da lib).
  - **Achado real, fora das ACs**: nenhum módulo do projeto configura `@EnableJpaRepositories(repositoryBaseClass=...)`. Sem isso, os métodos extras de `BaseJpaRepository` (`persist`/`merge`/`update`/`lockById`, usados por `ScosSystemServiceBean`) não resolvem contra `SimpleJpaRepository` — Spring Data tenta interpretá-los como derivação de query e falha (`PropertyReferenceException: No property 'update' found`). Configurado `repositoryBaseClass = BaseJpaRepositoryImpl.class` **só no `@EnableJpaRepositories` deste teste** — não é escopo desta story mudar a configuração de produção. Registrar como débito a investigar (log histórico de `flow-organization-boot/logs/backup/json/application.2026-07-14.json` mostra falha de bean relacionada ao `SecretKeyConverter` em execução passada, consistente com a fragilidade dessa área).
  - AC 10 se confirmou parcialmente: o teste passou de primeira com `@Import(SecretKeyConverter.class, ...)` (que já registra o bean independente de `@Component`), então a condição literal do AC ("se falhar, adicionar `@Component`") não disparou. Ainda assim, adicionado `@Component` a `SecretKeyConverter`: `ScosOrganizationApplication` declara `@ComponentScan(basePackages = {"br.com.sawcunhaos"})`, que cobre o pacote do converter — sem `@Component` e sem construtor sem-args, o Hibernate não teria como resolver o bean em produção (mesmo cenário do log de erro citado acima). Correção preventiva de baixo risco, alinhada com a própria justificativa do AC.
- **Task 6**: `.github/workflows/ci.yml` criado (primeiro do projeto), sem `insert_final_newline` (segue `.editorconfig` para `*.yml`).
- Nenhuma Acceptance Criteria pendente; guarda de escopo (AC 12 / Task 7) conferida.

### File List

- `organization/flow-organization-grpc-boot/pom.xml` (modificado — deps de teste)
- `organization/flow-organization-grpc-boot/src/main/java/br/com/sawcunhaos/organization/grpc/boot/configuration/interceptor/TokenAuthorizationInterceptor.java` (modificado — fix do tipo de exceção)
- `organization/flow-organization-grpc-boot/src/test/java/br/com/sawcunhaos/organization/grpc/boot/configuration/interceptor/TokenAuthorizationInterceptorTest.java` (novo)
- `organization/flow-organization-shared/src/main/java/br/com/sawcunhaos/organization/shared/converter/SecretKeyConverter.java` (modificado — `@Component`)
- `organization/flow-organization-shared/src/test/java/br/com/sawcunhaos/organization/shared/utils/SystemSecretCryptoServiceTest.java` (novo)
- `organization/flow-organization-domain/pom.xml` (modificado — H2 + `spring-boot-starter-data-jpa-test`)
- `organization/flow-organization-domain/src/test/java/br/com/sawcunhaos/organization/domain/DomainTestApplication.java` (novo)
- `organization/flow-organization-domain/src/test/java/br/com/sawcunhaos/organization/domain/access/system/service/ScosSystemServiceBeanTest.java` (novo)
- `organization/flow-organization-domain/src/test/java/br/com/sawcunhaos/organization/domain/access/system/internal/ScosSystemRepositoryPersistenceTest.java` (novo)
- `.github/workflows/ci.yml` (novo)

## Change Log

| Data | Mudança |
|---|---|
| 2026-07-20 | Implementação completa das Tasks 1–7. Fix do tipo de exceção no `TokenAuthorizationInterceptor`; `@Component` em `SecretKeyConverter`; cobertura de teste em `TokenAuthorizationInterceptor`, `SystemSecretCryptoService`, `ScosSystemServiceBean` e wiring de persistência do `SecretKeyConverter` (`@DataJpaTest`); CI mínimo criado. Regressão completa: 736 testes, 0 falhas. |
