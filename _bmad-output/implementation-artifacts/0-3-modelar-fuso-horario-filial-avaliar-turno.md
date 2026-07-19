# Story 0.3: Modelar Fuso Horário por Filial e Avaliar Janela de Turno

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como sistema,
Eu quero saber o fuso horário efetivo de cada filial e converter um `Instant` em hora local dela em um único ponto do código,
Para que a futura checagem de bloqueio de turno (Epic 5) compare corretamente contra a Jornada de Trabalho, inclusive quando o turno cruza a meia-noite.

## Acceptance Criteria

1. **Given** o fuso por filial não existe hoje — `SCOS_COMPANY` sem coluna de timezone, `SCOS_CONFIGURATION` é global (sem `COMPANY_ID`), nenhum `ZoneId` no código; `README.md:32` promete "Personalização por empresa (timezone, idioma)" e `README.md:981,1024` mostra um exemplo de schema/JSON com `timezone` — nenhum dos dois é o schema/código real (`SCOS_COMPANY` de verdade não tem essa coluna) **When** esta story é implementada **Then** `SCOS_COMPANY` ganha `TIME_ZONE VARCHAR(64)` (identificador IANA, ex. `America/Manaus`), `NOT NULL DEFAULT 'America/Sao_Paulo'`.
2. **Given** o sistema não foi liberado e nenhum ambiente executou `scos_company.yml` ainda **When** a coluna é adicionada **Then** entra editando o `changeSet` **baseline** (`id: 20260606-Samuel.Cunha-003`, `createTable`) diretamente — não um `addColumn` corretivo separado — decisão confirmada com o PM (2026-07-19): o baseline deve refletir o schema pretendido enquanto isso for possível; a partir do momento em que entrar em homologação, toda alteração de schema volta a exigir `changeSet` novo.
3. **Given** `etc/database/seed_data.sql:144` insere `SCOS_COMPANY` listando colunas explicitamente (`PARENT_COMPANY_ID, NAME, ..., STATUS, UPDATED_AT, USER_AT` — sem `TIME_ZONE`) **When** a coluna se torna `NOT NULL` **Then** o `INSERT` do seed continua funcionando sem nenhuma alteração no arquivo, porque o `defaultValue` fica no nível do SQL (Postgres aplica o default a qualquer coluna omitida da lista), não só no lado Java.
4. **Given** quem já rodou este changelog localmente (máquina de dev, ou Testcontainer com volume persistente) **When** subir esta mudança **Then** vai ter falha de checksum Liquibase — Dev Notes documenta o contorno (`drop`/recreate do schema local, ou `liquibase clearCheckSums`).
5. **Given** a entidade `Company` **When** esta story é implementada **Then** ganha campo `private ZoneId timeZone`, `@Convert(converter = ZoneIdConverter.class)`, `@Builder.Default` inicializado com a mesma constante usada como fallback do Service (`Company.DEFAULT_TIME_ZONE = ZoneId.of("America/Sao_Paulo")`) — obrigatório por `@Builder.Default` sempre que há inicializador em classe `@Builder` (`project-context.md`).
6. **Given** não existe hoje nenhum `AttributeConverter` `@Component` no projeto — o único precedente, `SecretKeyConverter`, **não é** `@Component` (bug separado, não corrigido nesta story) **When** o converter novo é criado **Then** `ZoneIdConverter` é `@Component @Converter`, em `flow-organization-shared/.../shared/converter/`, mesmo pacote do precedente — sem criptografia, só `ZoneId ↔ String` (id IANA).
7. **Given** a decisão de produto D3 (2026-07-19): alterar o fuso de uma filial reinterpretaria decisões de turno já auditadas — proibido após a criação **When** esta story é implementada **Then** nenhum campo `timeZone` é exposto em `CompanyInput`, na API (`etc/api/organization/*.yml`) ou em qualquer Use Case — regra só documentada em Javadoc na entidade (mesmo padrão de `parentCompanyId` em `CompanyInput`: documentado, não travado por remoção de setter).
8. **Given** a decisão de produto D2 (2026-07-19): herança recursiva subindo `parentCompany` até achar o primeiro `timeZone` não nulo **When** `CompanyService.resolveEffectiveZoneId(Long companyId)` é chamado **Then** resolve subindo a cadeia no mesmo padrão (não-CTE) de `CompanyServiceBean.depthOf()` — `AD-7` da spine só exige CTE recursiva para **descer** a subárvore (verificação de filial ativa), nunca para subir ancestrais **And** se a cadeia inteira estiver nula (cenário defensivo — sob D1/`NOT NULL` não deveria ocorrer em uso normal), cai em `Company.DEFAULT_TIME_ZONE`.
9. **Given** não existe hoje nenhum bounded context "shift" **When** `ShiftWindowEvaluator` é criado **Then** vive em pacote novo `domain/shift/` (`specification/` + `service/`, mesmo padrão specification+Bean do projeto), **sem** dependência de repositório ou de `CompanyService` — recebe `ZoneId` já resolvido, puro e testável sem mock.
10. **Given** a decisão de produto D4 (2026-07-19): limites `[startTime, endTime]` inclusivos nas duas pontas — consistente com a notação de intervalo fechado que FR-9 já usa (`epics.md`) **When** `ShiftWindowEvaluator.isWithinShift` avalia um horário local **Then** `time == startTime` e `time == endTime` contam como **dentro** do turno.
11. **Given** o requisito de tratar turno que cruza meia-noite (`endTime < startTime`, ex. `22:00 → 06:00`) **When** `isWithinShift` avalia **Then** usa dois ramos: `start <= end` (janela normal, comparação direta) e `start > end` (cruza meia-noite, união de duas faixas) — caso de teste obrigatório: `23:30` dentro, `07:00` fora.
12. **Given** FR-9 já registra "fora do intervalo `[startTime, endTime]` (**exceto almoço**)" **When** `isWithinShift` avalia **Then** o intervalo de almoço (`lunchStart`/`lunchEnd`) é excluído do turno — mesma lógica de janela reaplicada, não pedido explicitamente na lista de testes do prompt mas decorre direto do texto de FR-9 já aprovado.
13. **Given** o requisito "conversão `Instant → hora local` deve existir em UM único ponto do código" **When** qualquer código futuro (Filter, Use Case do Epic 5) precisar da hora local da filial **Then** chama `ShiftWindowEvaluator.isWithinShift` — nenhum outro lugar do `domain` replica `instant.atZone(zone)` (verificável por grep após a implementação).
14. **Given** os testes obrigatórios do prompt (mesma `Instant` avaliada em fusos diferentes resulta em horas locais e decisões diferentes; turno normal dentro/antes/depois; turno noturno cruzando meia-noite 23:30 dentro / 07:00 fora; borda exata em `startTime`/`endTime`; filial sem fuso próprio resolve por herança) **When** esta story é implementada **Then** todos usam `Clock.fixed` (`Clock` introduzido na Story 0.2), determinísticos, sem `sleep`, cobrindo `CompanyServiceBeanTest` (`resolveEffectiveZoneId`) e um novo `ShiftWindowEvaluatorBeanTest`.
15. **Given** a guarda de escopo **When** esta story é implementada **Then** NÃO implementa `ShiftEnforcementFilter` nem nenhum endpoint (são as stories de FR-9/10/12 do Epic 5) **And** NÃO altera `SCOS_CONFIGURATION` **And** NÃO toca nos tipos temporais já resolvidos na Story 0.2 (`Instant`/`Clock`) — só os usa.

## Tasks / Subtasks

- [ ] Task 1: Liquibase — editar o changeSet **baseline** de `scos_company.yml` (AC: 1, 2, 3, 4)
  - [ ] `flow-organization-resources/src/main/resources/db/changelog/organization/v1.0.0/tables/scos_company.yml`: dentro do `changeSet id: 20260606-Samuel.Cunha-003` já existente, acrescentar coluna na lista de `createTable` (logo antes de `CREATED_AT`, mesmo bloco `columns:`):
    ```yaml
              - column:
                  name: TIME_ZONE
                  type: varchar(64)
                  defaultValue: America/Sao_Paulo
                  constraints:
                    nullable: false
    ```
    **Não criar um `changeSet` novo** — esta é a única story do projeto até agora que edita um changeSet já existente em vez de adicionar um corretivo (`scos_legal_nature.yml`/`scos_cnae.yml` fizeram `modifyDataType` em changeSet separado); a diferença é que aqueles dois já tinham rodado em algum lugar, este não rodou em lugar nenhum ainda (ver Dev Notes).
  - [ ] Dev Notes: registrar o aviso de checksum para quem já rodou localmente.

- [ ] Task 2: `Company` — campo `timeZone` e `ZoneIdConverter` (AC: 5, 6, 7)
  - [ ] `flow-organization-domain/.../corporate/company/internal/Company.java`: adicionar
    ```java
    public static final ZoneId DEFAULT_TIME_ZONE = ZoneId.of("America/Sao_Paulo");
    ```
    e o campo:
    ```java
    /**
     * Fuso horário efetivo desta empresa (IANA). Imutável após a criação — decisão de
     * produto D3 (2026-07-19): alterar reinterpretaria decisões de bloqueio de turno já
     * auditadas. Nenhum Use Case/endpoint desta Etapa expõe alteração; não adicionar um
     * "update timezone" sem revisitar essa decisão.
     */
    @Convert(converter = ZoneIdConverter.class)
    @Column(name = "TIME_ZONE")
    @Builder.Default
    private ZoneId timeZone = DEFAULT_TIME_ZONE;
    ```
    Imports novos: `java.time.ZoneId`, `jakarta.persistence.Convert`, `br.com.sawcunhaos.organization.shared.converter.ZoneIdConverter`.
  - [ ] Criar `flow-organization-shared/src/main/java/br/com/sawcunhaos/organization/shared/converter/ZoneIdConverter.java`:
    ```java
    package br.com.sawcunhaos.organization.shared.converter;

    import jakarta.persistence.AttributeConverter;
    import jakarta.persistence.Converter;
    import org.springframework.stereotype.Component;

    import java.time.ZoneId;

    @Component
    @Converter
    public class ZoneIdConverter implements AttributeConverter<ZoneId, String> {

        @Override
        public String convertToDatabaseColumn(ZoneId zoneId) {
            return zoneId == null ? null : zoneId.getId();
        }

        @Override
        public ZoneId convertToEntityAttribute(String dbData) {
            return dbData == null ? null : ZoneId.of(dbData);
        }
    }
    ```
    Mesmo pacote de `SecretKeyConverter` (referência de padrão). **Não adicionar `@Component` no `SecretKeyConverter` existente** — bug separado, fora de escopo.

- [ ] Task 3: `CompanyService.resolveEffectiveZoneId` (AC: 8)
  - [ ] `flow-organization-domain/.../corporate/company/specification/CompanyService.java`: adicionar à interface:
    ```java
    /** Resolve o fuso horário efetivo, subindo a cadeia de {@code parentCompany} até achar o primeiro não nulo (D2). */
    ZoneId resolveEffectiveZoneId(@NonNull Long companyId);
    ```
    Import `java.time.ZoneId`. **Retorna `ZoneId` (tipo JDK), nunca `Company`** — a interface `specification` não pode vazar `internal.Company` para fora do agregado (mesma regra que já vale para `CompanyOutput` em vez de `Company` nos outros métodos).
  - [ ] `flow-organization-domain/.../corporate/company/service/CompanyServiceBean.java`: implementar, mesmo padrão de loop de `depthOf()` (linha ~230), mas subindo a cadeia procurando o primeiro `timeZone` não nulo em vez de contar profundidade:
    ```java
    @Override
    @Transactional(readOnly = true)
    public ZoneId resolveEffectiveZoneId(@NonNull Long companyId) {
        Company current = findCompanyById(companyId);
        while (current != null) {
            if (current.getTimeZone() != null) {
                return current.getTimeZone();
            }
            current = current.getParentCompany();
        }
        return Company.DEFAULT_TIME_ZONE;
    }
    ```
    Import `java.time.ZoneId`. `@Transactional(readOnly = true)` é necessário para manter a sessão aberta durante os `getParentCompany()` (`LAZY`) — mesmo motivo de `depthOf()` só funcionar dentro de um método transacional que já carregou a cadeia.

- [ ] Task 4: `ShiftWindowEvaluator` — novo bounded context `domain/shift/` (AC: 9, 10, 11, 12, 13)
  - [ ] Criar `flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/shift/specification/ShiftWindowEvaluator.java`:
    ```java
    package br.com.sawcunhaos.organization.domain.shift.specification;

    import org.jspecify.annotations.NonNull;

    import java.time.Instant;
    import java.time.LocalTime;
    import java.time.ZoneId;

    /**
     * Único ponto do domínio que converte um {@link Instant} em hora local de uma filial e
     * avalia se cai dentro de uma Jornada de Trabalho (turno). Não repetir {@code atZone()} em
     * nenhum outro lugar — Filter/Use Case futuros (Epic 5) chamam este serviço.
     */
    public interface ShiftWindowEvaluator {

        /**
         * @param instant   momento avaliado
         * @param zoneId    fuso efetivo da filial (ver {@code CompanyService.resolveEffectiveZoneId})
         * @param startTime início do turno (inclusive)
         * @param lunchStart início do almoço (inclusive, excluído do turno)
         * @param lunchEnd  fim do almoço (inclusive, excluído do turno)
         * @param endTime   fim do turno (inclusive)
         * @return {@code true} se dentro do turno e fora do almoço; trata turno que cruza meia-noite ({@code endTime < startTime})
         */
        boolean isWithinShift(
                @NonNull Instant instant,
                @NonNull ZoneId zoneId,
                @NonNull LocalTime startTime,
                @NonNull LocalTime lunchStart,
                @NonNull LocalTime lunchEnd,
                @NonNull LocalTime endTime
        );
    }
    ```
  - [ ] Criar `flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/shift/service/ShiftWindowEvaluatorBean.java`:
    ```java
    package br.com.sawcunhaos.organization.domain.shift.service;

    import br.com.sawcunhaos.organization.domain.shift.specification.ShiftWindowEvaluator;
    import org.springframework.stereotype.Service;

    import java.time.Instant;
    import java.time.LocalTime;
    import java.time.ZoneId;

    @Service
    class ShiftWindowEvaluatorBean implements ShiftWindowEvaluator {

        @Override
        public boolean isWithinShift(
                Instant instant, ZoneId zoneId,
                LocalTime startTime, LocalTime lunchStart, LocalTime lunchEnd, LocalTime endTime
        ) {
            LocalTime localTime = instant.atZone(zoneId).toLocalTime();
            return isWithinWindow(localTime, startTime, endTime)
                    && !isWithinWindow(localTime, lunchStart, lunchEnd);
        }

        private boolean isWithinWindow(LocalTime time, LocalTime start, LocalTime end) {
            if (!start.isAfter(end)) {
                return !time.isBefore(start) && !time.isAfter(end);
            }
            // cruza meia-noite (ex.: 22:00 -> 06:00)
            return !time.isBefore(start) || !time.isAfter(end);
        }
    }
    ```
    `class` package-private, mesmo padrão specification+Bean do projeto (`CompanyService`/`CompanyServiceBean` em pacotes diferentes, Bean concreta sem modificador público). **Sem** `@RequiredArgsConstructor`/campos — zero dependência, serviço puro.

- [ ] Task 5: Testes (AC: 14)
  - [ ] `CompanyServiceBeanTest.java` (`flow-organization-domain/src/test/.../corporate/company/service/`) — acrescentar métodos para `resolveEffectiveZoneId`:
    - empresa com `timeZone` próprio → retorna o próprio, não sobe a cadeia.
    - empresa com `timeZone == null` e pai com `timeZone` próprio → retorna o do pai (herança 1 nível).
    - empresa com `timeZone == null`, pai com `timeZone == null`, avô com `timeZone` próprio → retorna o do avô (herança 2 níveis, prova a recursão de D2).
    - empresa com `timeZone == null` e toda a cadeia até a matriz também `null` → retorna `Company.DEFAULT_TIME_ZONE` (fallback defensivo).
    - mesmo padrão de mock já usado no arquivo (`@Mock CompanyRepository`, `@InjectMocks CompanyServiceBean`) — `findById` mockado para cada nível da cadeia.
  - [ ] Criar `ShiftWindowEvaluatorBeanTest.java` (`flow-organization-domain/src/test/java/br/com/sawcunhaos/organization/domain/shift/service/`) — **arquivo novo**, JUnit 5 puro (sem Mockito — `ShiftWindowEvaluatorBean` não tem dependência). Cobrir:
    - mesma `Instant` avaliada com `ZoneId.of("America/Sao_Paulo")` e `ZoneId.of("America/Manaus")` (fuso -3 vs -4) contra o mesmo template de turno → horas locais diferentes e, com um `Instant` escolhido na borda, decisão de turno diferente entre os dois fusos.
    - turno normal (`08:00`–`17:00`, sem cruzar meia-noite): horário dentro, antes (`07:00`), depois (`18:00`).
    - turno noturno cruzando meia-noite (`22:00`–`06:00`): `23:30` dentro, `07:00` fora — caso obrigatório do prompt.
    - borda exata: horário `== startTime` e `== endTime` → dentro (D4, inclusive).
    - horário dentro do almoço (`lunchStart`–`lunchEnd`) → fora do turno, mesmo estando dentro de `[startTime, endTime]`.
    - todos os `Instant` construídos direto (`Instant.parse(...)`) — evaluator não usa `Clock`, não precisa de `Clock.fixed` aqui (só quem chama, no futuro Filter/Use Case, decide o "agora" via `Clock`); citar isso em Dev Notes para não confundir com a obrigação de `Clock.fixed` da Story 0.2.

- [ ] Task 6: Guarda de escopo (AC: 15)
  - [ ] NÃO implementar `ShiftEnforcementFilter` nem nenhum endpoint/Use Case/Delegate — fica para as stories de FR-9/FR-10/FR-12 do Epic 5.
  - [ ] NÃO alterar `SCOS_CONFIGURATION` nem `OrganizationConfiguration`.
  - [ ] NÃO expor `timeZone` em `CompanyInput`, em `etc/api/organization/*.yml` ou em qualquer Use Case — só a entidade e o `CompanyService.resolveEffectiveZoneId` (uso interno/futuro).
  - [ ] NÃO tocar em `Instant`/`Clock` além de usá-los — tipos temporais já resolvidos na Story 0.2.
  - [ ] NÃO criar `changeSet` Liquibase novo para esta coluna — é edição do baseline (Task 1).
  - [ ] NÃO corrigir `SecretKeyConverter` (bug separado, não-`@Component`).

## Dev Notes

### Contexto crítico — leia antes de implementar

**Fato apurado em auditoria de código (não em suposição):** o fuso por filial **não existe hoje**. `SCOS_COMPANY` não tem coluna de timezone (`flow-organization-resources/.../scos_company.yml`), `SCOS_CONFIGURATION` é uma tabela **global** sem `COMPANY_ID` (`domain/configuration/internal/OrganizationConfiguration.java`), e não há nenhum `ZoneId` no código-fonte do projeto até esta story. `README.md` promete a capacidade ("Personalização por empresa (timezone, idioma)", linha 32) e mostra um exemplo de schema/JSON com campo `timezone` (linhas 981, 1024) — mas esse exemplo é de uma tabela `companies` (minúscula, `type: HEADQUARTERS`, `tradeName`) que **não corresponde** ao schema real (`SCOS_COMPANY`, `NAME_TREATMENT`, sem `type`) — é o mesmo padrão de drift documentação-vs-código já catalogado em `project-context.md` (*Documentação do projeto que está ERRADA*). Não é um bug a corrigir no README nesta story; é só a prova de que a capacidade nunca foi implementada de verdade.

**Por que editar o changeSet baseline em vez de um `addColumn` corretivo — decisão do PM (2026-07-19), não um atalho tomado por conta própria:** o precedente do projeto para correção de coluna já existente (`scos_legal_nature.yml`/`scos_cnae.yml`, `modifyDataType` de `CODE`) sempre usou um `changeSet` **novo**, porque aquelas tabelas já tinham rodado em algum lugar. `SCOS_COMPANY` não — **nenhum ambiente executou este changelog ainda** (projeto sem CI, sem homologação, `project-context.md` confirma "Sem CI"). O PM decidiu que, enquanto isso for verdade, o baseline deve refletir o schema pretendido, evitando um `addColumn` fantasma logo atrás do `createTable` original. **Esse baseline congela assim que o sistema entrar em homologação** — a partir daí, qualquer alteração de schema (inclusive desta mesma tabela) volta a exigir `changeSet` novo, sem exceção. Não generalizar essa decisão para outras tabelas sem confirmar de novo.

**Aviso a propagar para quem já tem ambiente local:** quem já rodou este changelog numa máquina de dev, ou num Testcontainer com volume persistente (fora do padrão singleton descrito em `project-context.md` — que normalmente recria a cada sessão de teste, mas alguém pode ter montado volume próprio manualmente), vai ter **falha de checksum do Liquibase** na próxima subida, porque o conteúdo do changeSet `20260606-Samuel.Cunha-003` mudou depois de já ter sido registrado em `DATABASECHANGELOG`. Contorno: `drop`/recriar o schema local, ou `liquibase clearCheckSums` (limpa os checksums registrados, forçando Liquibase a aceitar o novo conteúdo sem tentar reaplicar o changeSet). Isso não é um problema em CI/homologação porque, por definição, ninguém rodou esse baseline lá ainda.

**Precedente de `AttributeConverter` (`SecretKeyConverter`) tem um bug conhecido e deliberadamente não corrigido aqui:** `@Converter` sem `@Component`, mas com `@RequiredArgsConstructor` sobre um campo `final` (`SystemSecretCryptoService crypto`) — sem construtor sem-argumentos, o Hibernate só consegue instanciá-lo via `SpringBeanContainer` (que pode ou não estar configurado; não verificado nesta story). `ZoneIdConverter` desta story **é** `@Component` desde o início — não repita o problema, mas também não "conserte" o `SecretKeyConverter` por iniciativa própria; é bug separado, fora do pedido desta story.

### Por que `resolveEffectiveZoneId` não é CTE recursiva (AD-7 não se aplica aqui)

`AD-7` da spine (`ARCHITECTURE-SPINE.md:106-110`) exige `WITH RECURSIVE` especificamente para **descer** a subárvore inteira (verificação de filial ativa em qualquer nível abaixo — FR-2). Herança de fuso é o caminho **inverso**: subir de uma empresa até a raiz, um relacionamento `@ManyToOne` de cada vez — exatamente o que `CompanyServiceBean.depthOf()` (linha ~230) já faz em Java puro, sem CTE, para calcular profundidade. `resolveEffectiveZoneId` reaproveita a mesma forma de loop, só troca "contar" por "procurar o primeiro não nulo". Não introduzir uma CTE aqui — seria inconsistente com o precedente já estabelecido para o mesmo tipo de caminhada (ascendente).

### Por que `ShiftWindowEvaluator` não depende de `CompanyService`

Desenho deliberado: `ShiftWindowEvaluator` recebe `ZoneId` já resolvido, não `companyId`. Isso mantém o avaliador **puro** (sem repositório, sem transação, sem mock necessário em teste) e testável isoladamente da árvore de herança de `Company`. Quem for montar a checagem completa no futuro (Filter do Epic 5) chama `CompanyService.resolveEffectiveZoneId(companyId)` primeiro, depois `ShiftWindowEvaluator.isWithinShift(...)` — dois passos, duas responsabilidades, dois testes independentes.

### Algoritmo de `isWithinWindow` — por que funciona pro caso de cruzar meia-noite

```
start <= end  (janela normal, ex. 08:00-17:00):
    dentro ⟺ start <= time <= end

start > end   (cruza meia-noite, ex. 22:00-06:00):
    dentro ⟺ time >= start OU time <= end
    (a janela é [start, 23:59:59...] UNIÃO [00:00..., end])
```
Verificado manualmente contra os valores do prompt: `22:00 → 06:00`, `23:30` → `23:30 >= 22:00` → dentro. `07:00` → `07:00 >= 22:00` falso, `07:00 <= 06:00` falso → fora. A mesma função resolve tanto o turno principal quanto o almoço (chamada duas vezes) — não precisa de uma segunda implementação.

### Testing Standards

- `resolveEffectiveZoneId`: adicionar ao `CompanyServiceBeanTest` existente, mesmo padrão `@Mock`/`@InjectMocks`/Given-When-Then já usado (`createShouldThrowWhenTaxIdentifierAlreadyExists` etc.) — nomear no padrão `resolveEffectiveZoneIdShould...When...`.
- `ShiftWindowEvaluatorBeanTest`: **arquivo novo**, primeiro teste do bounded context `shift`. Sem Mockito (`ShiftWindowEvaluatorBean` não tem campo nenhum) — `new ShiftWindowEvaluatorBean()` direto. `Instant` construído com `Instant.parse("2026-07-19T20:00:00Z")` etc.; combinar com `ZoneId.of(...)` explícito em cada cenário — este teste não precisa de `Clock` (quem precisa de `Clock.fixed`, por ser Story 0.2, é o futuro caller que decide "agora"; o evaluator só recebe o `Instant` já pronto).
- Nenhum teste de integração (`*ControllerTest`) necessário nesta story — não há endpoint novo, `TIME_ZONE` não é lida por nenhuma query nativa nova (ao contrário do `Resource.definitionUpdatedAt` na Story 0.2).

### Project Structure Notes

- Pacote novo: `domain/shift/` (`specification/`, `service/`) — primeiro bounded context novo desde a auditoria original do projeto; mesma estrutura specification+Bean já usada em `corporate/`, `access/`, `configuration/`, `outbox/`.
- `ZoneIdConverter` entra em `flow-organization-shared/.../shared/converter/`, ao lado de `SecretKeyConverter` — módulo já é dependência de `domain` (confirmado: `ScosSystem.java` já importa `SecretKeyConverter` de lá).
- Nenhum módulo Maven novo.
- Nenhuma mudança em `etc/api/organization/*.yml` — `timeZone` não é exposto via API nesta story (AC 7, Task 6).

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Epic 0 / Story 0.3] — Given/When/Then originais.
- [Source: README.md:32,981,1024] — promessa de "timezone por empresa" não implementada; exemplo de schema desatualizado/fictício.
- [Source: organization/flow-organization-resources/src/main/resources/db/changelog/organization/v1.0.0/tables/scos_company.yml] — changeSet baseline `20260606-Samuel.Cunha-003`, editado nesta story.
- [Source: etc/database/seed_data.sql:144-163] — `INSERT INTO scos.SCOS_COMPANY` com lista explícita de colunas, sem `TIME_ZONE` — motivo do `defaultValue` ser obrigatório no SQL, não só no Java.
- [Source: organization/flow-organization-domain/.../corporate/company/internal/Company.java] — entidade editada; `isMatrix()`/`isActive()` como precedente de método de domínio simples na entidade.
- [Source: organization/flow-organization-domain/.../corporate/company/service/CompanyServiceBean.java#depthOf,resolveParentCompany] — precedente de caminhada ascendente em Java (não CTE) reaproveitado para `resolveEffectiveZoneId`.
- [Source: organization/flow-organization-domain/.../corporate/company/specification/CompanyService.java] — todos os métodos hoje retornam DTO (`CompanyOutput`)/tipo primitivo, nunca `Company`; `resolveEffectiveZoneId` segue a mesma regra retornando `ZoneId`.
- [Source: organization/flow-organization-domain/.../corporate/company/dto/CompanyInput.java] — precedente de campo imutável documentado só via Javadoc (`parentCompanyId`), sem trava de setter — mesmo padrão aplicado a `timeZone` (D3).
- [Source: organization/flow-organization-shared/.../shared/converter/SecretKeyConverter.java] — referência de padrão de `AttributeConverter`; confirmado NÃO `@Component` hoje (bug separado, não corrigido).
- [Source: _bmad-output/planning-artifacts/architecture/architecture-SawCunhaOS-Organization-2026-07-18/ARCHITECTURE-SPINE.md#AD-2,AD-3,AD-7] — `ShiftEnforcementFilter` fica para o Epic 5 (fora de escopo aqui); `AD-7` só cobre descida de subárvore, não a herança ascendente desta story.
- [Source: organization/flow-organization-domain/.../corporate/position/internal/PositionWorkSchedule.java, corporate/employee/internal/EmployeeWorkSchedule.java] — campos `LocalTime` (`startTime`/`lunchStart`/`lunchEnd`/`endTime`) que `ShiftWindowEvaluator` recebe como parâmetro; nenhum dos dois é importado pelo pacote `shift` (evaluator recebe primitivos, não os entities — evita acoplamento cross-agregado).
- [Source: organization/flow-organization-domain/src/test/java/.../corporate/company/service/CompanyServiceBeanTest.java] — padrão de teste (`@Mock`/`@InjectMocks`, nomenclatura `xShouldYWhenZ`) replicado nos testes novos.
- [Source: _bmad-output/implementation-artifacts/0-2-padronizar-tipos-temporais-clock-injetavel.md] — `Clock`/`Instant` desta story vêm de lá; não reabrir escopo de tipos temporais aqui.

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
