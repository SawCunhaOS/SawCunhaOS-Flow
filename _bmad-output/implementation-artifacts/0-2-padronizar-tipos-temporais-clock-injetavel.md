# Story 0.2: Padronizar Tipos Temporais e Introduzir Clock Injetável

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como Desenvolvedor da plataforma,
Eu quero que todo campo de instante no domínio use `Instant` (nunca `LocalDateTime`) e que a hora "agora" venha sempre de um `Clock` injetável,
Para que a persistência corresponda de fato à coluna `TIMESTAMPTZ` real e testes de janela de tempo sejam determinísticos, sem `sleep`.

## Acceptance Criteria

1. **Given** os 12 campos `LocalDateTime` "simples" hoje mapeados contra coluna `TIMESTAMPTZ` (`Login.lastUsedAt`, `OutboxEvent.{startedAt,processedAt,createdAt,updatedAt}`, `OutboxEventDeadLetter.createdAt`, `OutboxEventLog.createdAt`, `CompanyStatusHistory.createdAt`, `EmployeeStatusHistory.createdAt`, `LoginStatusHistory.createdAt`, `EmployeePositionHistory.createdAt`, `Cnae.createdAt`, `LegalNature.createdAt`, `CompanyCnaeSecondary.createdAt`, `LoginProfile.createdAt`, `ProfileResource.createdAt`) **When** esta story é implementada **Then** todos passam a `Instant`, preservando nome de campo, nome de coluna e semântica **And** `PositionWorkSchedule`/`EmployeeWorkSchedule` (`LocalTime`) e `Employee.birthDate`/`dateOfHiring`/`probationEndDate`, `Company.foundationDate`, `EmployeePositionHistory.startDate`/`endDate` (`LocalDate`) permanecem intocados — já corretos pela regra de tipos temporais.
2. **Given** `Resource.definitionUpdatedAt` hoje é `java.time.LocalDate` (import fully-qualified) contra uma coluna que **hoje é `DATE`**, não `TIMESTAMPTZ` (divergência confirmada contra o changelog em relação à premissa original desta story) **When** esta story é implementada **Then** a coluna `SCOS_RESOURCE.DEFINITION_UPDATED_AT` é migrada para `TIMESTAMPTZ` via novo changeSet Liquibase (`id: 20260719-Samuel.Cunha-001`) com `rollback` **And** o campo Java passa a `Instant`.
3. **Given** a mudança de tipo de `definitionUpdatedAt` **When** esta story é implementada **Then** se propaga por `RegisterResourceInput` (domain/dto), `ResourceRepository.upsert` (domain/internal, native query), `RegistryResourceInput` (usecase) e `RegistreServiceImpl` (grpc-boot) **And** o contrato gRPC (`registry.proto`) continua enviando `updated_at` como `string` — só o parse interno muda de `LocalDate.parse(...)` para uma conversão em `Instant` (ver Dev Notes, decisão a confirmar).
4. **Given** `ScosSystem.matchesSecret` hoje chama `LocalDateTime.now()` diretamente (único `.now()` remanescente no domain hoje — o de `rotateSecret` já foi removido em edição em andamento) **When** esta story é implementada **Then** nenhuma chamada a `.now()` permanece no módulo `domain` (verificável por grep) **And** o "agora" é recebido via `Clock` injetado no `ScosSystemServiceBean`, nunca lido estaticamente pela entidade.
5. **Given** o bug de semântica em que `PREVIOUS_SECRET_EXPIRES_AT` grava o instante da rotação (não a expiração) e `matchesSecret` soma o grace na leitura — mudar a constante de grace alteraria retroativamente secrets já rotacionados **When** esta story é implementada **Then** `rotateSecret(String newRawSecret, Instant expiresAt)` passa a receber a expiração **já calculada** pelo chamador (o Service, quando existir um) **And** `matchesSecret(String provided, Instant now)` só compara `now.isBefore(previousSecretExpiresAt)`, nunca soma duração.
6. **Given** `secretKey`/`previousSecretKey` trafegam em texto claro em memória (via `SecretKeyConverter`) e `ScosSystem` hoje **não tem** `@ToString` **When** esta story é implementada **Then** `ScosSystem` ganha `@ToString` de classe com `@ToString.Exclude` nos dois campos — não é um exclude "solto": a classe não gerava `toString()` nenhum antes, então esta task cria o método já protegido.
7. **Given** `ResourceServiceBeanTest` (domain) e `RegistryResourcesUseCaseBeanTest` (usecase) — únicos testes hoje que tocam os campos alterados; os outros 12 campos não têm nenhum teste unitário que os referencie, só `@CreationTimestamp` gerenciado pelo Hibernate **When** esta story é implementada **Then** ambos são atualizados para `Instant` sem mudar a asserção de comportamento.
8. **Given** não existe `ScosSystemServiceBeanTest` hoje **When** esta story é implementada **Then** um novo arquivo de teste é criado cobrindo o grace period do secret com `Clock.fixed`: válido dentro da janela, inválido após — determinístico, sem `sleep`.
9. **Given** `BaseEntity` (biblioteca externa `scos-foundation-utils`) ainda expõe `createdAt`/`updatedAt` como `LocalDateTime`, herdado por `ScosSystem`/`Login`/`Resource`/etc. **When** esta story é implementada **Then** esse resíduo **não é alterado aqui** — está fora do controle deste projeto (não é módulo deste repositório), só documentado em Dev Notes e em `project-context.md`.

## Tasks / Subtasks

- [ ] Task 1: Trocar tipo dos 12 campos `createdAt`/`lastUsedAt` sem cascata (AC: 1)
  - [ ] `flow-organization-domain/.../access/login/internal/Login.java` — `lastUsedAt`: `LocalDateTime` → `Instant` (import `java.time.Instant`).
  - [ ] `flow-organization-domain/.../outbox/internal/OutboxEvent.java` — `startedAt`, `processedAt`, `createdAt`, `updatedAt`.
  - [ ] `flow-organization-domain/.../outbox/internal/OutboxEventDeadLetter.java` — `createdAt`.
  - [ ] `flow-organization-domain/.../outbox/internal/OutboxEventLog.java` — `createdAt`.
  - [ ] `flow-organization-domain/.../access/status/internal/CompanyStatusHistory.java` — `createdAt`.
  - [ ] `flow-organization-domain/.../access/status/internal/EmployeeStatusHistory.java` — `createdAt`.
  - [ ] `flow-organization-domain/.../access/status/internal/LoginStatusHistory.java` — `createdAt`.
  - [ ] `flow-organization-domain/.../corporate/employee/internal/EmployeePositionHistory.java` — `createdAt` (manter `startDate`/`endDate` como `LocalDate`, intocados).
  - [ ] `flow-organization-domain/.../corporate/company/internal/Cnae.java` — `createdAt`.
  - [ ] `flow-organization-domain/.../corporate/company/internal/LegalNature.java` — `createdAt`.
  - [ ] `flow-organization-domain/.../corporate/company/internal/CompanyCnaeSecondary.java` — `createdAt`.
  - [ ] `flow-organization-domain/.../access/profile/internal/LoginProfile.java` — `createdAt`.
  - [ ] `flow-organization-domain/.../access/resource/internal/ProfileResource.java` — `createdAt`.
  - [ ] Em todos: trocar só `import java.time.LocalDateTime;` → `import java.time.Instant;` e o tipo do campo. `@CreationTimestamp` do Hibernate aceita `Instant` nativamente (nenhuma outra mudança). Nenhum teste unitário toca esses 12 campos hoje (confirmado por grep) — nada a atualizar neles.

- [ ] Task 2: `ScosSystem` — Clock injetável e correção de semântica do secret rotation (AC: 1, 4, 5, 6)
  - [ ] `ScosSystem.java` (`flow-organization-domain/.../access/system/internal/ScosSystem.java`): `previousSecretExpiresAt` `LocalDateTime` → `Instant`.
  - [ ] Assinatura `rotateSecret(String newRawSecret, Instant expiresAt)` — recebe a expiração já calculada pelo chamador; não computa nada internamente. Javadoc explicitando que o método está pronto mas **sem call site hoje** (ver Dev Notes — mesmo padrão de `CompanyService.assertNoCycle` da Story 1.1).
  - [ ] Assinatura `matchesSecret(String provided, Instant now)` — remove `LocalDateTime.now()` de dentro do método; `now` passa a vir de fora.
  - [ ] Adicionar `@ToString(exclude = {"secretKey", "previousSecretKey"})` na classe (hoje não existe `@ToString` nenhum — esta task cria o método já protegido, não é um exclude vazio).
  - [ ] Criar `flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/config/ClockConfig.java` (pacote novo `domain/config/` — hoje não existe nenhuma classe `@Configuration` no módulo `domain`):
    ```java
    package br.com.sawcunhaos.organization.domain.config;

    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;

    import java.time.Clock;

    @Configuration
    public class ClockConfig {

        @Bean
        public Clock clock() {
            return Clock.systemUTC();
        }
    }
    ```
  - [ ] `ScosSystemServiceBean.java` (`flow-organization-domain/.../access/system/service/ScosSystemServiceBean.java`): adicionar campo `private final Clock clock;` (ordem: `scosSystemRepository`, `systemSecretCryptoService`, `clock` — `@RequiredArgsConstructor` gera o construtor nessa ordem). Em `validateSecretKey`, trocar `scosSystem.matchesSecret(secretKey)` por `scosSystem.matchesSecret(secretKey, clock.instant())`.

- [ ] Task 3: `Resource.definitionUpdatedAt` — migração de coluna e cascata cross-módulo (AC: 2, 3, 7)
  - [ ] Liquibase — `flow-organization-resources/src/main/resources/db/changelog/organization/v1.0.0/tables/scos_resource.yml`: acrescentar (não editar) um segundo `changeSet` no mesmo arquivo, no padrão já usado em `scos_legal_nature.yml`/`scos_cnae.yml` para `modifyDataType`:
    ```yaml
      - changeSet:
          id: 20260719-Samuel.Cunha-001
          author: Samuel.Cunha
          comment: "Migra SCOS_RESOURCE.DEFINITION_UPDATED_AT de DATE para TIMESTAMPTZ (Story 0.2 — tipos temporais); passa a representar instante, não mais data de calendário"
          changes:
            - modifyDataType:
                tableName: SCOS_RESOURCE
                schemaName: scos
                columnName: DEFINITION_UPDATED_AT
                newDataType: TIMESTAMPTZ
          rollback:
            - sql:
                sql: "ALTER TABLE scos.SCOS_RESOURCE ALTER COLUMN DEFINITION_UPDATED_AT TYPE DATE USING DEFINITION_UPDATED_AT::date"
    ```
    Rollback usa `sql`/`USING` explícito (não `modifyDataType` simétrico) porque `TIMESTAMPTZ → DATE` é um downcast — ver Dev Notes.
  - [ ] `Resource.java` (`flow-organization-domain/.../access/resource/internal/Resource.java`): `definitionUpdatedAt` de `java.time.LocalDate` (fully-qualified) para `java.time.Instant` — trocar para import formal (`import java.time.Instant;`).
  - [ ] `RegisterResourceInput.java` (`flow-organization-domain/.../access/resource/dto/RegisterResourceInput.java`): `updatedAt` `LocalDate` → `Instant`.
  - [ ] `ResourceRepository.java` (`flow-organization-domain/.../access/resource/internal/ResourceRepository.java`): parâmetro `definitionUpdatedAt` do `upsert(...)` nativo, `LocalDate` → `Instant`; import correspondente.
  - [ ] `RegistryResourceInput.java` (`flow-organization-usecase/.../access/resource/registry/RegistryResourceInput.java`): `updatedAt` `LocalDate` → `Instant`.
  - [ ] `RegistreServiceImpl.java` (`flow-organization-grpc-boot/.../delegate/RegistreServiceImpl.java`): troca `.updatedAt(LocalDate.parse(resource.getUpdatedAt()))` por `.updatedAt(LocalDate.parse(resource.getUpdatedAt()).atStartOfDay(ZoneOffset.UTC).toInstant())` — mantém o formato de entrada do wire (`registry.proto` continua `string updated_at`, formato de data pura tipo `"2026-07-09"`), só a representação interna vira `Instant`. **Não mudar `registry.proto`.**

- [ ] Task 4: Testes (AC: 7, 8)
  - [ ] `ResourceServiceBeanTest.java` (`flow-organization-domain/src/test/.../access/resource/service/`): `DEFINITION_UPDATED_AT` de `LocalDate.of(2026, 7, 9)` para `LocalDate.of(2026, 7, 9).atStartOfDay(ZoneOffset.UTC).toInstant()`; import `java.time.LocalDate` → `java.time.Instant` (+ `ZoneOffset` só na constante, pode ser local). Nenhuma outra asserção muda.
  - [ ] `RegistryResourcesUseCaseBeanTest.java` (`flow-organization-usecase/src/test/.../access/resource/registry/`): mesma troca em `input()` e na asserção `assertThat(r.updatedAt()).isEqualTo(...)`.
  - [ ] Criar `ScosSystemServiceBeanTest.java` (`flow-organization-domain/src/test/java/br/com/sawcunhaos/organization/domain/access/system/service/`) — **arquivo novo**, primeiro teste da classe. `@ExtendWith(MockitoExtension.class)`, `@Mock ScosSystemRepository`/`@Mock SystemSecretCryptoService`, **sem** `@InjectMocks` (Clock precisa ser uma instância real `Clock.fixed(...)`, não um mock — instanciar `ScosSystemServiceBean` manualmente no `@BeforeEach`/em cada teste). Cobrir em `validateSecretKey`:
    - secret atual (`secretKey`) bate → não lança.
    - secret anterior (`previousSecretKey`) dentro da janela de grace (`Clock.fixed` antes de `previousSecretExpiresAt`) → não lança.
    - secret anterior fora da janela de grace (`Clock.fixed` depois de `previousSecretExpiresAt`) → lança `ScosException` com `SCOS_SYSTEM_002.getCode()`.
    - secret errado, sem grace válido (`previousSecretExpiresAt == null`) → lança.
    - `code` inexistente → lança `SCOS_SYSTEM_001` (regressão do que `getByCode` já faz).

- [ ] Task 5: Guarda de escopo — NÃO fazer (AC: todas)
  - [ ] NÃO criar coluna nem entidade de timezone (fica para a Story 0.3).
  - [ ] NÃO implementar avaliador de turno (fica para a Story 0.3).
  - [ ] NÃO alterar nenhum outro changelog além do único `changeSet` de `SCOS_RESOURCE.DEFINITION_UPDATED_AT` — os outros 13 campos já são `TIMESTAMPTZ`, zero migração para eles.
  - [ ] NÃO alterar `PositionWorkSchedule`/`EmployeeWorkSchedule` (`LocalTime`) nem `Employee.birthDate`/`dateOfHiring`/`probationEndDate`, `Company.foundationDate`, `EmployeePositionHistory.startDate`/`endDate` (`LocalDate`) — já corretos.
  - [ ] NÃO corrigir `BaseEntity` (biblioteca externa `scos-foundation-utils`) — fora do controle deste projeto (AC 9).
  - [ ] NÃO alterar `registry.proto` — o campo `updated_at` continua `string` no contrato gRPC.

## Dev Notes

### Contexto crítico — leia antes de implementar

**A premissa original desta story ("todos os 14 campos já têm coluna TIMESTAMPTZ") está errada para 1 dos 14.** Verificação linha a linha contra `flow-organization-resources/.../v1.0.0/tables/*.yml` confirmou que `SCOS_RESOURCE.DEFINITION_UPDATED_AT` é `DATE`, não `TIMESTAMPTZ` (único changelog que toca essa coluna, sem `ALTER` posterior). Pela própria regra de tipos temporais desta story, `DATE` ↔ `LocalDate` — ou seja, `Resource.definitionUpdatedAt` já estava **correto** como `LocalDate`. **Decisão confirmada com o PM (2026-07-19):** migrar a coluna para `TIMESTAMPTZ` e o campo para `Instant` mesmo assim (opção escolhida explicitamente sobre deixar como `LocalDate`), aceitando a cascata cross-módulo descrita na Task 3. Não "reverta" essa decisão por conta própria — foi avaliada e escolhida deliberadamente, com as duas outras opções (deixar `LocalDate`, ou `Instant` sem migrar coluna) descartadas.

**`rotateSecret` não tem nenhum call site hoje** — `ScosSystemService` (specification) não declara esse método; é usado exclusivamente pela entidade, sem nenhum Use Case/Delegate que o invoque. Mesmo padrão da Story 1.1 (`CompanyService.assertNoCycle`): peça de fundação construída pronta e testada, sem endpoint associado ainda. **Não crie um endpoint/Use Case novo para "usar" o método** — está fora de escopo desta story.

**Estado em edição no momento em que esta story foi escrita:** `ScosSystem.java` e `ScosSystemServiceBean.java` já tinham mudanças não commitadas quando esta story foi criada — `rotateSecret` já recebia `LocalDateTime previousSecretExpiresAt` como parâmetro (não mais `Duration grace` + `.now()` interno) e `matchesSecret` já tinha perdido o parâmetro `Duration grace`, comparando direto contra `previousSecretExpiresAt`. Esta story assume esse estado como ponto de partida — só troca `LocalDateTime` por `Instant` nas assinaturas já reformuladas e resolve de onde vem o "agora" (`Clock`, não mais `.now()` estático).

### Cascata cross-módulo de `Resource.definitionUpdatedAt` — leia antes de tocar só em `domain`

O prompt original desta story cita apenas "código do módulo `flow-organization-domain`", mas a mudança de tipo de `definitionUpdatedAt` **atravessa 3 módulos**, porque o valor flui: `RegistreServiceImpl` (grpc-boot, parseia o `string` do proto) → `RegistryResourceInput` (usecase) → `RegistryResourcesUseCaseBean` (usecase, só repassa, não precisa mudar) → `RegisterResourceInput` (domain/dto) → `ResourceRepository.upsert` (domain/internal, native query) → coluna `SCOS_RESOURCE.DEFINITION_UPDATED_AT`. Os 4 arquivos de main (fora `RegistryResourcesUseCaseBean`, que só repassa por parâmetro genérico) e os 2 testes (`ResourceServiceBeanTest` no domain, `RegistryResourcesUseCaseBeanTest` no usecase) precisam mudar juntos, senão não compila.

### Decisão a confirmar — formato do `updated_at` no gRPC

`registry.proto:35` declara `string updated_at` na mensagem `Resource` — o contrato de rede **não muda** nesta story. Hoje `RegistreServiceImpl.registryResources` faz `LocalDate.parse(resource.getUpdatedAt())`, ou seja, quem chama esse gRPC hoje manda uma data pura (ex.: `"2026-07-09"`). Depois desta story, o parse vira `LocalDate.parse(resource.getUpdatedAt()).atStartOfDay(ZoneOffset.UTC).toInstant())` — **o formato de entrada esperado não muda**, só a representação interna. Isso preserva compatibilidade com quem já chama esse endpoint gRPC. Se no futuro esse campo precisar ser serializado de volta como saída (hoje não é — `RegistryResourcesRequest`/`Empty`, sem response com dado), o formato ISO-8601 completo de `Instant` (`2026-07-09T00:00:00Z`) seria observável para quem consome — **sinalizar ao time antes disso acontecer**, não decidir aqui.

### Onde cada peça vai (camadas)

- **`domain/config/`** (pacote **novo**, hoje não existe nenhuma classe `@Configuration` em `flow-organization-domain`): `ClockConfig.java`, único bean `Clock.systemUTC()`.
- **`domain/access/system/internal/ScosSystem.java`**: campo, `rotateSecret`, `matchesSecret`, `@ToString`.
- **`domain/access/system/service/ScosSystemServiceBean.java`**: injeção de `Clock`, chamada a `matchesSecret(secretKey, clock.instant())`.
- **`domain/access/resource/internal/Resource.java`** + **`domain/access/resource/internal/ResourceRepository.java`** + **`domain/access/resource/dto/RegisterResourceInput.java`**: tipo do campo/parâmetro.
- **`usecase/access/resource/registry/RegistryResourceInput.java`**: tipo do campo (record simples, sem lógica).
- **`grpc-boot/delegate/RegistreServiceImpl.java`**: ponto de conversão `String` (proto) → `Instant`.
- **Liquibase** (`flow-organization-resources`): único changeSet novo, em `scos_resource.yml`.
- Os outros 12 campos "simples" (Task 1) ficam só em `domain/*/internal/*.java` — sem cascata, `@CreationTimestamp` cobre tudo.

### SQL/YAML do changeSet — por que `sql`/`USING` no rollback

O precedente do projeto para `modifyDataType` (`scos_legal_nature.yml`, `scos_cnae.yml`) é sempre para `varchar(N)` → `varchar(M)` — cast trivial nos dois sentidos, `modifyDataType` simétrico no `rollback` funciona. Aqui não: `DATE → TIMESTAMPTZ` (ida) é um cast de assignment implícito no PostgreSQL (`ALTER TABLE ... ALTER COLUMN ... TYPE TIMESTAMPTZ` funciona sem `USING`), mas `TIMESTAMPTZ → DATE` (rollback) trunca informação e é mais seguro com `USING coluna::date` explícito — por isso o rollback usa `sql`/`USING` em vez de um `modifyDataType` simétrico. Não é o padrão usual do projeto para `modifyDataType`, mas é o padrão usual para `rollback` — obrigatório e correto tecnicamente (`project-context.md`, *Liquibase*: "todo changeSet: rollback obrigatório").

### Testing Standards

- Unitário: JUnit 5 + Mockito, `@ExtendWith(MockitoExtension.class)`, padrão Given/When/Then já usado em `CnaeServiceBeanTest`/`LegalNatureServiceBeanTest`/`ResourceServiceBeanTest` (mesmo pacote de testes).
- `ScosSystemServiceBeanTest` é **arquivo novo** — não existe hoje nenhum teste para essa classe. Seguir a convenção `XxxServiceBeanTest` (`project-context.md`, *Testing Rules*).
- `Clock.fixed(Instant.parse("2026-07-19T10:00:00Z"), ZoneOffset.UTC)` para os cenários de grace — nunca `Thread.sleep`.
- Como `Clock` precisa ser uma instância real (não mockável de forma útil aqui — o teste quer controlar o instante, não verificar interação), **não usar `@InjectMocks`** nesta classe de teste: construir `new ScosSystemServiceBean(scosSystemRepository, systemSecretCryptoService, fixedClock)` manualmente. Isso é uma pequena divergência do padrão `@Mock`/`@InjectMocks` usado nos outros testes do projeto — documentada aqui para não ser "corrigida" por engano depois.
- `ResourceServiceBeanTest`/`RegistryResourcesUseCaseBeanTest`: só troca de constante, mesma estrutura de teste, mesmas asserções.

### Project Structure Notes

- Pacote novo: `domain/config/` (só `ClockConfig.java`) — nenhum pacote de agregado existente é reaproveitado porque `Clock` é transversal, não pertence a nenhum bounded context específico.
- Nenhum módulo Maven novo.
- Nenhuma mudança em `etc/api/organization/*.yml` (REST) nem em `registry.proto` (gRPC) — só o parse interno do lado gRPC muda.
- `BaseEntity` (`scos-foundation-utils`, biblioteca externa) continua com `createdAt`/`updatedAt` em `LocalDateTime` — fora do escopo, documentado, não "corrigido por engano" durante a implementação.

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Epic 0 / Story 0.2] — Given/When/Then originais.
- [Source: _bmad-output/project-context.md#Tipos temporais (obrigatório)] — regra formal adicionada nesta mesma sessão.
- [Source: organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/access/system/internal/ScosSystem.java] — estado atual (em edição não commitada) de `rotateSecret`/`matchesSecret`/`previousSecretExpiresAt`.
- [Source: organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/access/system/service/ScosSystemServiceBean.java] — único call site de `matchesSecret` (`validateSecretKey`); `rotateSecret` sem call site.
- [Source: organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/access/system/specification/ScosSystemService.java] — confirma que `rotateSecret` não está na interface pública.
- [Source: organization/flow-organization-resources/src/main/resources/db/changelog/organization/v1.0.0/tables/scos_resource.yml:63-64] — coluna `DEFINITION_UPDATED_AT` é `DATE`, não `TIMESTAMPTZ`.
- [Source: organization/flow-organization-resources/src/main/resources/db/changelog/organization/v1.0.0/tables/scos_legal_nature.yml, scos_cnae.yml] — único precedente de `modifyDataType` no projeto.
- [Source: organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/access/resource/internal/ResourceRepository.java:38-73] — `upsert()` nativo, parâmetro `definitionUpdatedAt`.
- [Source: organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/access/resource/dto/RegisterResourceInput.java] — DTO domain, campo `updatedAt`.
- [Source: organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/access/resource/registry/RegistryResourceInput.java, RegistryResourcesUseCaseBean.java] — cascata no usecase.
- [Source: organization/flow-organization-grpc-boot/src/main/java/br/com/sawcunhaos/organization/grpc/boot/delegate/RegistreServiceImpl.java:78] — `LocalDate.parse(resource.getUpdatedAt())`, ponto de conversão do wire.
- [Source: organization/flow-organization-grpc-proto/src/main/proto/registry.proto:35] — `string updated_at`, contrato não muda.
- [Source: organization/flow-organization-domain/src/test/java/br/com/sawcunhaos/organization/domain/access/resource/service/ResourceServiceBeanTest.java:55] — constante a atualizar.
- [Source: organization/flow-organization-usecase/src/test/java/br/com/sawcunhaos/organization/application/usecase/access/resource/registry/RegistryResourcesUseCaseBeanTest.java:72,94] — asserções a atualizar.
- [Source: (biblioteca externa) scos-foundation-utils:1.2.0-SNAPSHOT, br.com.sawcunhaos.foundation.utils.entity.BaseEntity] — `createdAt`/`updatedAt` herdados continuam `LocalDateTime`, fora de escopo.

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
