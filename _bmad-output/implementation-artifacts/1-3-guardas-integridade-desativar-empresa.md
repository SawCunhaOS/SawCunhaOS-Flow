---
baseline_commit: abaef10db144e2f2767f57a1f06e0f32998498aa
---

# Story 1.3: Guardas de Integridade ao Desativar Empresa

Status: review

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como Analista de RH,
Eu quero que o sistema impeça inativar a última matriz ativa do sistema, bloquear a única empresa ativa do sistema, e desativar/bloquear qualquer Empresa com filial ativa em qualquer nível abaixo dela,
Para que a operação nunca deixe o ISP sem empresa raiz ativa nem crie filial órfã.

## Acceptance Criteria

1. **Given** existe só uma Empresa matriz (`parentCompany == null`) `ACTIVE` no sistema **When** RH aciona `PUT /v1/companies/{id}/disable` (`inactivateCompany`, chama `CompanyService.inactivate`) nessa matriz **Then** o sistema rejeita com `SCOS_COMPANY_005` **And** nenhuma alteração é persistida (nem `CompanyStatusHistory`, nem `SCOS_COMPANY.STATUS`).
2. **Given** existe mais de uma Empresa matriz `ACTIVE` no sistema **When** RH inativa uma delas **Then** a guarda do AC 1 não bloqueia (outras guardas desta story continuam se aplicando normalmente).
3. **Given** existe só uma Empresa `ACTIVE` no sistema, em qualquer nível da hierarquia (matriz ou filial) **When** RH aciona `PUT /v1/companies/{id}/block` (`blockCompany`, chama `CompanyService.disable`) nessa Empresa **Then** o sistema rejeita com `SCOS_COMPANY_006`.
4. **Given** existem 2+ Empresas `ACTIVE` no sistema **When** RH bloqueia uma delas **Then** a guarda do AC 3 não bloqueia (outras guardas desta story continuam se aplicando normalmente).
5. **Given** uma Empresa tem ao menos uma filial com status `ACTIVE` em qualquer nível da sua subárvore (filha direta ou descendente indireto) **When** RH tenta `inactivate` (`PUT /disable`) **or** `disable`/block (`PUT /block`) essa Empresa **Then** o sistema rejeita com um código novo (`SCOS_COMPANY_018` — ver Dev Notes sobre numeração) **And** a checagem é uma CTE recursiva (`WITH RECURSIVE`) descendo a árvore por `PARENT_COMPANY_ID`, nunca caminhada em memória Java (AD-7 da spine — mesmo princípio da CTE de `wouldCreateCycle` da Story 1.1, mas em direção oposta: desce em vez de subir).
6. **Given** uma Empresa sem nenhuma filial `ACTIVE` em nenhum nível abaixo **When** `inactivate`/`disable` é chamado e nenhuma das outras guardas rejeita **Then** a transição prossegue normalmente (histórico gravado, trigger sincroniza o status — comportamento já implementado pela Story 1.2).
7. **Given** esta story depende da Story 1.2 (`CompanyService.inactivate`/`disable` e os 4 Use Cases ainda não existem no código-fonte hoje) **When** esta story é implementada **Then** a Story 1.2 deve estar `done` antes — as guardas desta story são inseridas **dentro** dos métodos `CompanyServiceBean.inactivate(...)`/`disable(...)` que a Story 1.2 cria, não em métodos novos de fluxo.
8. **Given** a Story 1.2 (Task 5) cria um teste de integração de caminho feliz para `block` usando a única Empresa seed ativa (`SEEDED_ID=1`, `CompanyControllerTest`) **When** esta story adiciona a guarda `SCOS_COMPANY_006` (única empresa ativa) **Then** esse teste especificamente quebra (bloquear a única ativa agora é rejeitado) **And** esta story ajusta esse teste — criar uma segunda Empresa `ACTIVE` antes do `block`, ou trocar o alvo — não é regressão silenciosa, é conhecida e corrigida aqui.

## Tasks / Subtasks

- [x] Task 0: Pré-requisito — confirmar Story 1.2 concluída (AC: 7)
  - [x] Antes de iniciar, confirmar que `CompanyServiceBean.inactivate(...)`/`disable(...)` já existem no código (Story 1.2 `done`). Se não existirem, **parar e reportar bloqueio** — não implementar os métodos da Story 1.2 como parte desta story.
  - [x] Confirmar a numeração final de `SCOS_COMPANY_0XX` que a Story 1.2 reservou (esperado `012`–`017`); se a Story 1.2 usou números diferentes, ajustar o código novo desta story (Task 3) para o primeiro número livre acima do maior já usado — não colidir.

- [x] Task 1: `CompanyRepository` — CTE recursiva descendente (AC: 5, 6)
  - [x] Adicionar em `CompanyRepository` (`flow-organization-domain/.../corporate/company/internal/CompanyRepository.java`) um método nativo `@Query` com `WITH RECURSIVE` que desce a árvore a partir de `companyId` (via `PARENT_COMPANY_ID`) e verifica se algum descendente (qualquer nível) tem `STATUS = 'ACTIVE'`:
    ```java
    @Query(value = """
            WITH RECURSIVE descendants AS (
                SELECT COMPANY_ID, STATUS
                FROM scos.SCOS_COMPANY
                WHERE PARENT_COMPANY_ID = :companyId
                UNION ALL
                SELECT c.COMPANY_ID, c.STATUS
                FROM scos.SCOS_COMPANY c
                INNER JOIN descendants d ON c.PARENT_COMPANY_ID = d.COMPANY_ID
            )
            SELECT CASE WHEN EXISTS (SELECT 1 FROM descendants WHERE STATUS = 'ACTIVE') THEN 1 ELSE 0 END
            """, nativeQuery = true)
    int hasActiveDescendantFlag(@Param("companyId") Long companyId);

    default boolean hasActiveDescendant(Long companyId) {
        return hasActiveDescendantFlag(companyId) == 1;
    }
    ```
  - [x] Mesmo padrão de `int` + `default boolean` wrapper da Story 1.1 (`wouldCreateCycle`) — não usar `EXISTS(...)` cru como retorno de `@Query(nativeQuery=true)` (frágil entre driver/Hibernate, ver Dev Notes da Story 1.1).
  - [x] Import de `@Query`/`@Param` (`org.springframework.data.jpa.repository.Query`, `org.springframework.data.repository.query.Param`) — não existem ainda neste arquivo, adicionar.

- [x] Task 2: `CompanyRepository` — checagem de "outra matriz ativa" (AC: 1, 2)
  - [x] Adicionar, **via QueryDSL comum** (não nativo — não é recursivo, é uma existência flat sobre `parentCompany IS NULL`), mesmo estilo de `existsByStatus` já existente no arquivo:
    ```java
    /**
     * Verifica se existe outra Empresa matriz (parentCompany nulo) ativa, excluindo a própria.
     *
     * @param companyId empresa a ser excluída da checagem
     * @return true se existe outra matriz ACTIVE, false caso contrário
     */
    default boolean existsOtherActiveMatrix(Long companyId) {
        return exists(
                company.parentCompany.isNull()
                        .and(company.status.eq(StatusCompany.ACTIVE))
                        .and(company.id.ne(companyId))
        );
    }
    ```
  - [x] **Não** implementar isso como CTE nativa — só a checagem de subárvore (Task 1) precisa de recursão (AD-7 é específico para "filial ativa em qualquer nível", não para a checagem de matriz).
  - [x] Para a guarda de "única empresa ativa do sistema" (AC 3, 4) **não criar método novo** — reaproveitar `companyRepository.existsByStatus(companyId, StatusCompany.ACTIVE)`, já implementado (linha ~84) e com exatamente essa semântica ("existe alguma empresa com o status informado, excluindo a própria").

- [x] Task 3: Código de erro novo — filial ativa em subárvore (AC: 5)
  - [x] `flow-organization-shared/.../exception/ExceptionCodeError.java`: adicionar, após o range reservado pela Story 1.2 (`SCOS_COMPANY_017`), uma constante nova:
    ```java
    /** Não é possível desativar/bloquear a empresa: existe filial ativa em algum nível da subárvore. HTTP 422. */
    SCOS_COMPANY_018("SCOS_COMPANY_018", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    ```
  - [x] `scos_message_organization.properties` (após a última linha de `SCOS_COMPANY_0XX`):
    ```properties
    SCOS_COMPANY_018=Não é possível desativar ou bloquear a empresa, pois existe uma filial ativa em algum nível da hierarquia.
    ```
  - [x] `scos_message_organization_en.properties`:
    ```properties
    SCOS_COMPANY_018=It is not possible to inactivate or block the company because there is an active subsidiary at some level of the hierarchy.
    ```
  - [x] **`SCOS_COMPANY_005`/`006` já existem no enum e nas mensagens PT/EN** (reservados desde a Story 1.2 — sem throw site até hoje). **Não recriar, não duplicar** — só usar. Confirmar em `ExceptionCodeError.java:76,78` e `scos_message_organization[_en].properties:34-35`.
  - [x] **Não reaproveitar `SCOS_COMPANY_003`** para a guarda de subárvore — apesar de reservado e sem throw site, sua mensagem ("colaboradores/employees vinculados") é semanticamente sobre Funcionário, não sobre filial. Não há FR nesta Etapa que aponte throw site para ele; pertence a outra story/decisão futura, fora de escopo aqui.

- [x] Task 4: Guardas em `CompanyServiceBean` (AC: 1, 2, 3, 4, 5, 6, 7)
  - [x] Dentro do método `inactivate(@NonNull Long id, @NonNull Long reasonInactivateId, String observation)` (criado pela Story 1.2), inserir as duas guardas **antes** de `validateReasonInactivate(...)` e antes de `company.inactivate(...)`:
    ```java
    @Override
    @Transactional(rollbackFor = ScosException.class)
    public void inactivate(@NonNull Long id, @NonNull Long reasonInactivateId, String observation) {
        log.info("Inactivate Company: {}", id);
        Company company = findCompanyById(id);
        assertNotLastActiveMatrix(company);
        assertNoActiveDescendant(company);
        validateReasonInactivate(reasonInactivateId);
        String user = scosUserAuthentication.findUserAuthentication();

        CompanyStatusHistory history = company.inactivate(reasonInactivateId);
        history.setObservation(observation);
        history.setUserAt(user);
        companyStatusHistoryRepository.merge(history);
    }
    ```
  - [x] Dentro do método `disable(@NonNull Long id, @NonNull Long reasonDisableId, String observation)` (rota `block`, criado pela Story 1.2), inserir as duas guardas antes de `validateReasonDisable(...)` e antes de `company.disable(...)`:
    ```java
    Company company = findCompanyById(id);
    assertNotOnlyActiveCompany(company);
    assertNoActiveDescendant(company);
    validateReasonDisable(reasonDisableId);
    // ... resto igual ao padrão da Story 1.2
    ```
  - [x] Adicionar 3 helpers privados:
    ```java
    private void assertNotLastActiveMatrix(Company company) {
        if (company.isMatrix() && company.isActive() && !companyRepository.existsOtherActiveMatrix(company.getId())) {
            throw new ScosException(SCOS_COMPANY_005);
        }
    }

    private void assertNotOnlyActiveCompany(Company company) {
        if (!companyRepository.existsByStatus(company.getId(), StatusCompany.ACTIVE)) {
            throw new ScosException(SCOS_COMPANY_006);
        }
    }

    private void assertNoActiveDescendant(Company company) {
        if (companyRepository.hasActiveDescendant(company.getId())) {
            throw new ScosException(SCOS_COMPANY_018);
        }
    }
    ```
  - [x] `Company.isMatrix()`/`isActive()` já existem na entidade (`Company.java:123-129`) — reaproveitar, não recriar.
  - [x] **Ordem intencional:** guardas estruturais desta story (005/006/018) rodam **antes** da validação de motivo (012–017, Story 1.2) — falha rápido na regra mais fundamental primeiro. Ordem relativa entre `assertNot...` e `assertNoActiveDescendant` entre si é livre (checagens independentes).
  - [x] `assertNoActiveDescendant` roda para **ambos** `inactivate` e `disable` — não depende do status atual da empresa sendo transicionada, só dos descendentes.
  - [x] `assertNotLastActiveMatrix` só bloqueia quando a própria empresa é matriz (`isMatrix()`) e está `ACTIVE` hoje — se já está `DISABLED`, inativá-la não remove nenhuma matriz ativa do conjunto (guarda não se aplica).
  - [x] `assertNotOnlyActiveCompany` não precisa checar `company.isActive()` explicitamente — `disable()`/`block` só é alcançável a partir de `ACTIVE` (guard `SCOS_COMPANY_007` em `Company.disable()` já impede origem diferente); a própria empresa sempre conta como a "ativa" candidata a ficar sozinha.

- [x] Task 5: Testes unitários (AC: 1, 2, 3, 4, 5, 6)
  - [x] `CompanyServiceBeanTest.java` — para `inactivate`: (a) matriz `ACTIVE` única → mock `existsOtherActiveMatrix` retorna `false` → `assertThatThrownBy(...).hasFieldOrPropertyWithValue("code", SCOS_COMPANY_005.getCode())`; (b) matriz `ACTIVE` com outra matriz ativa → mock retorna `true` → sem throw dessa guarda (seguir até o resto do fluxo); (c) filial (não-matriz) `ACTIVE` sem outra matriz ativa → guarda 005 não dispara (`isMatrix()` falso); (d) `hasActiveDescendant` mockado `true` → throw `SCOS_COMPANY_018`; (e) `hasActiveDescendant` `false` e nenhuma outra guarda → `verify(companyStatusHistoryRepository).merge(...)`.
  - [x] Idem para `disable` (block): (a) `existsByStatus(id, ACTIVE)` mockado `false` → throw `SCOS_COMPANY_006`; (b) mockado `true` → sem throw dessa guarda; (c)/(d) mesma cobertura de `hasActiveDescendant` de cima.
  - [x] Mesmo padrão `@Mock`/`@InjectMocks`/`assertThatThrownBy(...).hasFieldOrPropertyWithValue(...)` já usado no arquivo (ver testes de `create`, Story 1.1).

- [x] Task 6: Teste de integração da CTE recursiva (AC: 5, 6)
  - [x] Nova classe em `flow-organization-boot/src/test/java/.../company/`, mesmo padrão da Story 1.1 (Task 3): estende `ScosOrganizationTestUtil`, **sem MockMvc**, autowira `CompanyRepository` direto. Monta hierarquia de 3 níveis via `companyRepository.merge(...)` (matriz `ACTIVE` → filial nível 1 `INACTIVE` → filial nível 2 `ACTIVE`) e prova que `hasActiveDescendant(matrizId)` retorna `true` (descendente indireto ativo é encontrado) e que uma matriz sem nenhum descendente ativo retorna `false`.
  - [x] Manter sufixo `*ControllerTest` mesmo sem chamada HTTP — convenção deliberada do projeto (mesma justificativa da Story 1.1).

- [x] Task 7: Ajustar teste de integração da Story 1.2 quebrado por `SCOS_COMPANY_006` (AC: 3, 8)
  - [x] Em `CompanyControllerTest.java` (`flow-organization-boot`), localizar o teste que a Story 1.2 (Task 5) cria para o caminho feliz de `PUT /v1/companies/{id}/block` usando `SEEDED_ID` (única Empresa seed `ACTIVE`). Ajustar: **criar uma segunda Empresa `ACTIVE`** via `POST /v1/companies` (CNPJ dedicado, ex. `CNPJ_SECOND_ACTIVE`) antes do `block`, garantindo que `SEEDED_ID` deixa de ser a única ativa — só então o `block` de `SEEDED_ID` deve suceder (`204`) e o teste original (prova que o trigger sincroniza `DISABLED`) continua válido.
  - [x] Adicionar cenário novo: `block` de `SEEDED_ID` **sem** criar outra empresa antes → `422` com `code = SCOS_COMPANY_006`.
  - [x] Adicionar cenário novo para `SCOS_COMPANY_005`: como só existe uma matriz seed (`SEEDED_ID`, matriz), tentar `PUT /disable` (`inactivate`) diretamente nela sem criar outra matriz → `422` `SCOS_COMPANY_005`. Se a Story 1.2 já cobriu esse caminho de outra forma, ajustar/mesclar em vez de duplicar.
  - [x] Adicionar cenário para `SCOS_COMPANY_018`: criar filial `ACTIVE` sob `SEEDED_ID` (`POST` com `parentCompanyId=SEEDED_ID`), então tentar `block`/`disable` em `SEEDED_ID` → `422` `SCOS_COMPANY_018`.

- [x] Task 8: Guarda de escopo (AC: 7)
  - [x] **Não** alterar `etc/api/organization/ScosOrganization_Company.yml` — os 4 endpoints e o `$ref` genérico de `4XX` já cobrem os códigos novos, sem necessidade de documentar cada código individualmente (mesmo padrão de `SCOS_COMPANY_007`).
  - [x] **Não** criar Use Case novo nem alterar `CompanyDelegate` — as guardas vivem inteiramente em `CompanyServiceBean` (camada `domain`), a Story 1.2 já resolve o mapeamento rota→Use Case→`CompanyService`.
  - [x] **Não** adicionar permissão nova em `ScosGeotemporalPermission` — `DISABLE_COMPANY`/`BLOCK_COMPANY` já existem e já cobrem essas rotas.
  - [x] **Não** implementar os 4 métodos de transição da Story 1.2 do zero — se ao abrir esta story eles não existirem, é sinal de que a Story 1.2 não foi concluída (ver Task 0).

## Dev Notes

### Contexto crítico — leia antes de implementar

**Esta story depende estruturalmente da Story 1.2 e não pode ser implementada isolada.** Hoje (no código-fonte atual, antes da Story 1.2), `CompanyServiceBean` só tem `create`/`update`/`findById`/`findAll`/`resolveEffectiveZoneId` — não existe `activate`/`inactivate`/`disable`/`enable`, e `CompanyService` (specification) não declara esses métodos. As guardas desta story (`assertNotLastActiveMatrix`, `assertNotOnlyActiveCompany`, `assertNoActiveDescendant`) são inseridas **dentro** dos métodos `inactivate`/`disable` que a Story 1.2 cria — não é possível escrever essas guardas sem que 1.2 já exista. A própria `epics.md` já registra essa dependência (Story 1.3, Given/When/Then final) e a Story 1.2 já reserva `SCOS_COMPANY_005`/`006` explicitamente para esta story (Task 6 "Guarda de escopo" da Story 1.2: *"NÃO tocar SCOS_COMPANY_005/006 (...) — Story 1.3"*).

**Os códigos `SCOS_COMPANY_005` e `006` já existem no enum e nas mensagens — não são novos.** Conferir `ExceptionCodeError.java:76,78`:
```java
SCOS_COMPANY_005("SCOS_COMPANY_005", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
/** Sem throw site hoje — reservado para bloqueio da única empresa ativa. HTTP 422. */
SCOS_COMPANY_006("SCOS_COMPANY_006", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
```
E as mensagens (`scos_message_organization.properties:34-35`) já dizem exatamente o texto certo ("Não é permitido inativar a última empresa matriz ativa do sistema." / "Não é permitido bloquear a única empresa ativa do sistema."). Esta story só precisa **criar o throw site** — não toca no enum nem nas properties para 005/006, só para o código novo (018, Task 3).

**`SCOS_COMPANY_003` não é o código certo para a guarda de subárvore, apesar de também estar "reservado sem throw site".** Sua mensagem é sobre "colaboradores/employees vinculados" (Funcionário), não sobre filial ativa — não há nenhuma FR desta Etapa que aponte esse código para a checagem de subárvore. Não reaproveitar por engano só porque "já existe e está livre".

**A CTE da Story 1.1 (`wouldCreateCycle`) sobe a árvore; a desta story desce — são queries diferentes, não a mesma reaproveitada literalmente.** A frase do `epics.md` ("a checagem reaproveita a mesma CTE recursiva da Story 1.1") se refere à **técnica** (AD-7: `WITH RECURSIVE` no banco, `default` method no repositório, nunca caminhada Java) — não ao SQL exato. `wouldCreateCycle` (Story 1.1) sobe por `PARENT_COMPANY_ID` a partir do candidato a pai, procurando a própria empresa entre os ancestrais. A checagem desta story (`hasActiveDescendant`) desce por `PARENT_COMPANY_ID` a partir da própria empresa, procurando `STATUS = 'ACTIVE'` entre os descendentes. A própria spine já registra que descer a árvore é **trabalho novo**, não uma extensão do que já existe: *"`CompanyServiceBean.depthOf()` hoje só resolve profundidade subindo (...); não existe nenhum código que desça a árvore — este é trabalho novo"* (AD-7, Rule).

**A guarda de "outra matriz ativa" (AC 1, 2) NÃO precisa de CTE recursiva** — é uma checagem flat (`parentCompany IS NULL AND status = ACTIVE AND id != :companyId`), resolvida com QueryDSL comum (`exists(...)`), mesmo estilo de `existsByStatus` já no repositório. Só a checagem de "filial ativa em qualquer nível" (subárvore, profundidade arbitrária) exige recursão — não confundir as duas guardas e não escrever as três como CTE por uniformidade aparente.

**Regressão conhecida e esperada: o teste de integração de `block` que a Story 1.2 cria vai quebrar.** A Story 1.2 (Task 5) cria um cenário de integração usando `SEEDED_ID` (a única Empresa do seed, `ACTIVE`, matriz) para provar que `PUT /v1/companies/{id}/block` sincroniza o status via trigger. Com a guarda `SCOS_COMPANY_006` desta story, bloquear a única empresa ativa do sistema passa a ser rejeitado — esse teste específico da Story 1.2 vai falhar assim que a guarda entrar. **Isto é esperado, não uma regressão a ignorar silenciosamente** — a Task 7 desta story existe justamente para corrigi-lo (criar uma segunda empresa ativa antes do `block` do `SEEDED_ID`). Ver seed: `etc/database/seed_data.sql` só cadastra **uma** `SCOS_COMPANY` (`COMPANY_ID=1`, matriz, `ACTIVE`) — não há segunda empresa disponível hoje para os testes de integração que não colida com essa guarda.

### SQL da CTE recursiva descendente (AD-7)

Único precedente de `@Query` nativo no projeto além do `ResourceRepository.upsert()`: `CompanyRepository.wouldCreateCycleFlag` (Story 1.1) — mesmo padrão, texto SQL em text-block, tabela prefixada `scos.`, retorno `int` (nunca `boolean`/`EXISTS` cru — ver justificativa na Story 1.1).

```java
@Query(value = """
        WITH RECURSIVE descendants AS (
            SELECT COMPANY_ID, STATUS
            FROM scos.SCOS_COMPANY
            WHERE PARENT_COMPANY_ID = :companyId
            UNION ALL
            SELECT c.COMPANY_ID, c.STATUS
            FROM scos.SCOS_COMPANY c
            INNER JOIN descendants d ON c.PARENT_COMPANY_ID = d.COMPANY_ID
        )
        SELECT CASE WHEN EXISTS (SELECT 1 FROM descendants WHERE STATUS = 'ACTIVE') THEN 1 ELSE 0 END
        """, nativeQuery = true)
int hasActiveDescendantFlag(@Param("companyId") Long companyId);

default boolean hasActiveDescendant(Long companyId) {
    return hasActiveDescendantFlag(companyId) == 1;
}
```

### Onde cada peça vai (camadas)

- **Repositório** (`domain/corporate/company/internal/CompanyRepository.java`): `hasActiveDescendantFlag`/`hasActiveDescendant` (nativo, Task 1) + `existsOtherActiveMatrix` (QueryDSL, Task 2). Reaproveita `existsByStatus` já existente (guarda 006).
- **Shared** (`flow-organization-shared/.../exception/ExceptionCodeError.java` + `scos_message_organization[_en].properties`): 1 código novo (`018`, Task 3) — `005`/`006` já existem, só uso.
- **Bean** (`domain/corporate/company/service/CompanyServiceBean.java`): 3 guardas privadas + 2 pontos de chamada dentro de `inactivate`/`disable` (Task 4) — métodos criados pela Story 1.2, não por esta.
- **Nada em `usecase`, `api`, `boot` (Liquibase) ou YAML** — só o teste de integração em `boot` (Tasks 6, 7).

### Testing Standards

- Unitário: `flow-organization-domain/src/test/java/.../company/service/CompanyServiceBeanTest.java` — JUnit 5 + Mockito, `@ExtendWith(MockitoExtension.class)`, mesmo padrão `assertThatThrownBy(...).hasFieldOrPropertyWithValue("code", SCOS_COMPANY_0XX.getCode())` já usado nos testes de `create`/Story 1.1.
- Integração da query recursiva: só é possível em Postgres real (Testcontainers, `flow-organization-boot`) — nova classe estendendo `ScosOrganizationTestUtil`, autowire `CompanyRepository` direto, **sem MockMvc**. Sufixo `*ControllerTest` mantido por convenção do projeto mesmo sem chamada HTTP (mesma decisão da Story 1.1 — não fragmentar nomenclatura de teste de integração).
- Integração full-stack (`CompanyControllerTest.java`): ajustar o teste de `block` da Story 1.2 (Task 7) + 3 cenários novos (005/006/018) — reaproveitar `SEEDED_ID`, `REASON_COMPANY_ACTIVE`/`REASON_COMPANY_INACTIVE` e o helper `createBody(...)` já existentes no arquivo.
- `code`/jDempotent não exigem atenção nova — os endpoints já são idempotentes desde a Story 1.2, nenhuma mudança de contrato aqui.

### Project Structure Notes

- Nenhum módulo novo, nenhum pacote novo — tudo dentro de `domain/corporate/company/{internal,service}` (Story 1.3) mais o que a Story 1.2 já criou em `usecase`/`api`.
- Nenhuma mudança em `etc/api/organization/*.yml`, nenhuma mudança em Liquibase (`flow-organization-resources`) — os 4 endpoints e o schema já existem desde a Story 1.2/schema seed.
- Único ponto de atenção estrutural: a ordem de implementação real (1.2 antes de 1.3) é uma dependência de sprint, não só de documentação — ver Task 0.

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Epic 1 / Story 1.3] — Given/When/Then originais, dependência explícita da Story 1.2.
- [Source: _bmad-output/planning-artifacts/architecture/architecture-SawCunhaOS-Organization-2026-07-18/ARCHITECTURE-SPINE.md#AD-7] — CTE recursiva obrigatória para "filial ativa em qualquer nível"; nota de que descer a árvore é trabalho novo (`depthOf()` só sobe).
- [Source: _bmad-output/implementation-artifacts/1-1-bloquear-ciclo-hierarquia-empresa.md] — precedente de `@Query(nativeQuery=true)` com `WITH RECURSIVE` (`wouldCreateCycleFlag`), padrão `int` + `default boolean` wrapper, convenção de teste `*ControllerTest` sem MockMvc.
- [Source: _bmad-output/implementation-artifacts/1-2-ciclo-vida-completo-empresa.md#Task 6] — reserva explícita de `SCOS_COMPANY_005`/`006` para esta story; estrutura dos métodos `inactivate`/`disable` que esta story estende; teste de integração de `block` com `SEEDED_ID` que esta story precisa ajustar (Task 5 da Story 1.2).
- [Source: organization/flow-organization-domain/.../corporate/company/internal/Company.java:123-129] — `isMatrix()`/`isActive()` já implementados, reaproveitados sem alteração.
- [Source: organization/flow-organization-domain/.../corporate/company/internal/CompanyRepository.java:84-86] — `existsByStatus(Long, StatusCompany)` já implementado, reaproveitado para a guarda `SCOS_COMPANY_006` sem criar método novo.
- [Source: organization/flow-organization-domain/.../corporate/company/service/CompanyServiceBean.java] — estado atual (sem `inactivate`/`disable`) confirmando a dependência da Story 1.2; padrão `findCompanyById`/`@Transactional(rollbackFor = ScosException.class)` a seguir.
- [Source: organization/flow-organization-shared/.../exception/ExceptionCodeError.java:71-87] — `SCOS_COMPANY_001..011` já existentes; `005`/`006` reservados sem throw site; `012..017` reservados pela Story 1.2; `018` novo desta story.
- [Source: organization/flow-organization-shared/src/main/resources/scos_message_organization.properties:30-39, scos_message_organization_en.properties:30-39] — mensagens PT/EN já existentes para `005`/`006`; `003` confirmado como "colaboradores/employees", não relacionado a esta story.
- [Source: etc/api/organization/ScosOrganization_Company.yml:131-255] — rotas `enable`/`disable`/`block`/`unblock` já publicadas, sem necessidade de alteração (4XX genérico cobre os códigos novos).
- [Source: etc/database/seed_data.sql:142-167] — única `SCOS_COMPANY` seedada (`COMPANY_ID=1`, matriz, `ACTIVE`) — base do ajuste de teste da Task 7.
- [Source: organization/flow-organization-boot/src/test/java/.../company/CompanyControllerTest.java] — `SEEDED_ID`, `createBody(...)`, `REASON_COMPANY_ACTIVE`/`REASON_COMPANY_INACTIVE` já existentes, reaproveitados nos novos cenários.

## Dev Agent Record

### Agent Model Used

Claude Sonnet 5 (claude-sonnet-5)

### Debug Log References

- Task 0: confirmado Story 1.2 `done` no código-fonte (`activate`/`inactivate`/`disable`/`enable` já existiam em `CompanyServiceBean`) e numeração `SCOS_COMPANY_012..017` conforme esperado — nenhum ajuste de numeração necessário, `018` livre.
- Achado durante Task 5: build falhou com "cannot find symbol SCOS_COMPANY_018" e uma cascata de erros não relacionados (Q-classes de outras entidades) ao rodar `mvn -pl flow-organization-domain clean test` isolado — causa raiz: `flow-organization-shared` tinha sido só `compile`d, não `install`ado, então o classpath do `domain` ainda apontava pro jar antigo do `shared` (sem o código novo) no `~/.m2`. Corrigido com `mvn -pl flow-organization-shared install -DskipTests`. Lição: após editar um módulo upstream (`shared`), rodar `install` (não só `compile`) antes de testar módulos downstream isoladamente.
- `mvn -pl flow-organization-domain clean test -Dtest=CompanyServiceBeanTest -Denforcer.skip=true` → 44/44 (37 pré-existentes + 7 novos: 4 guardas de `inactivate` + 3 de `disable`).
- `mvn -pl flow-organization-boot test -Dtest=CompanyControllerTest,CompanyActiveDescendantGuardControllerTest,CompanyCycleGuardControllerTest -Denforcer.skip=true` → 20 + 2 + 2 = 24/24 (teste de `block` da Story 1.2 corrigido criando 2ª empresa ativa antes; 3 cenários novos de 005/006/018; CTE descendente provada contra Postgres real).
- `mvn -pl flow-organization-domain,flow-organization-usecase,flow-organization-infrastructure test -Denforcer.skip=true` → 214+186+3 = 403/403, sem regressão (inclui `PermissionsConsistencyTest`, inalterado).
- `mvn -pl flow-organization-boot test -Denforcer.skip=true` → 368/368, sem regressão na suíte de integração completa.
- `-Denforcer.skip=true` usado só para contornar o enforcer pré-existente já quebrado (ver *Build quebrado* no `project-context.md`) — nenhuma mudança de dependência nesta story.

### Completion Notes List

- Guarda de "última matriz ativa" (`SCOS_COMPANY_005`) e "única empresa ativa" (`SCOS_COMPANY_006`) implementadas dentro de `inactivate`/`disable` (métodos já criados pela Story 1.2) — códigos e mensagens PT/EN já existiam desde a Story 1.2 (reservados), só criado o throw site.
- Guarda de "filial ativa em qualquer nível da subárvore" (`SCOS_COMPANY_018`, código novo) usa CTE recursiva **descendente** (`hasActiveDescendant`), tecnicamente análoga a `wouldCreateCycle` (Story 1.1, que sobe) mas SQL distinto — mesma convenção `int` + `default boolean` wrapper.
- Guarda de "outra matriz ativa" (`existsOtherActiveMatrix`) resolvida com QueryDSL comum (`exists(...)`), não recursiva — só a checagem de subárvore precisa de `WITH RECURSIVE` (AD-7).
- Ordem das guardas em `inactivate`/`disable`: estruturais desta story (005/006/018) rodam **antes** da validação de motivo (012–017, Story 1.2) — falha rápido na regra mais fundamental.
- Regressão conhecida e esperada corrigida (Task 7): o teste de `block` da Story 1.2 usava a única Empresa seed (`SEEDED_ID`) — com `SCOS_COMPANY_006` isso passou a ser rejeitado; corrigido criando uma 2ª empresa `ACTIVE` antes do `block`. 3 cenários novos de integração adicionados (`005`, `006`, `018`), reaproveitando `SEEDED_ID` e helpers já existentes no arquivo.
- Reaproveitadas 2 constantes de CNPJ que estavam declaradas mas sem uso no arquivo (`CNPJ_UPDATE`→`CNPJ_SECOND_ACTIVE`, `CNPJ_DUP_A`→`CNPJ_FILIAL_ACTIVE_UNDER_SEEDED`) em vez de inventar CNPJs novos sem checksum verificado.
- Guarda de escopo (Task 8) confirmada via `git status`: zero mudança em YAML, Use Case, `CompanyDelegate` ou `ScosGeotemporalPermission` — só `domain`/`shared` + 2 arquivos de teste em `boot`.

### File List

- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/corporate/company/internal/CompanyRepository.java` (modificado — `existsOtherActiveMatrix`, `hasActiveDescendantFlag`/`hasActiveDescendant`)
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/corporate/company/service/CompanyServiceBean.java` (modificado — 3 guardas + 2 pontos de chamada em `inactivate`/`disable`)
- `organization/flow-organization-domain/src/test/java/br/com/sawcunhaos/organization/domain/corporate/company/service/CompanyServiceBeanTest.java` (modificado — 7 testes novos + stubs ajustados em 6 testes existentes da Story 1.2)
- `organization/flow-organization-shared/src/main/java/br/com/sawcunhaos/organization/shared/exception/ExceptionCodeError.java` (modificado — `SCOS_COMPANY_018`)
- `../../organization/flow-organization-shared/src/main/resources/scos_message/scos_message_organization.properties` (modificado — mensagem PT de `018`)
- `../../organization/flow-organization-shared/src/main/resources/scos_message/scos_message_organization_en.properties` (modificado — mensagem EN de `018`)
- `organization/flow-organization-boot/src/test/java/br/com/sawcunhaos/organization/boot/api/company/CompanyActiveDescendantGuardControllerTest.java` (novo — integração da CTE descendente)
- `organization/flow-organization-boot/src/test/java/br/com/sawcunhaos/organization/boot/api/company/CompanyControllerTest.java` (modificado — teste de `block` corrigido + 3 cenários novos de 005/006/018)

## Change Log

- 2026-07-22: Implementadas guardas de integridade estrutural de Empresa (última matriz ativa, única empresa ativa, filial ativa em subárvore via CTE recursiva descendente) dentro de `inactivate`/`disable` (Story 1.2); 8 arquivos (7 modificados, 1 novo); 9 testes novos (7 unit + 2 integração) + 3 cenários novos de integração + 1 teste corrigido (regressão conhecida e esperada), 0 regressão (403 domain/usecase/infra + 368 boot). Status → review.
