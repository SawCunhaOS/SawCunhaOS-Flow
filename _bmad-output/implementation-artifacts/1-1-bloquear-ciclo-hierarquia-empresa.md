# Story 1.1: Bloquear Ciclo na Hierarquia de Empresa

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como Analista de RH,
Eu quero que o sistema rejeite qualquer ciclo na hierarquia de empresas (direto ou indireto),
Para que a árvore Empresa/Filial nunca fique estruturalmente inválida.

## Acceptance Criteria

1. **Given** Empresa A é filial de B, e B é filial de C **When** o guard de ciclo (`CompanyService.assertNoCycle`) é chamado para avaliar C como filial de A (fechando o ciclo) **Then** lança `ScosException` com código `SCOS_COMPANY_004` **And** nenhuma alteração é persistida.
2. **Given** uma Empresa sem nenhuma relação de hierarquia com outra **When** ela é avaliada como candidata a matriz de uma nova filial **Then** o guard aceita normalmente (nenhuma exceção).
3. **Given** a checagem de ciclo é executada em hierarquia com múltiplos níveis (3+) **When** o método roda **Then** usa CTE recursiva (`WITH RECURSIVE`) no PostgreSQL — AD-7 da spine — nunca caminhada em memória Java.
4. **Given** hoje NÃO existe nenhum endpoint/Use Case que altere `parentCompanyId` de uma Empresa já existente (`UpdateCompanyRequest` não tem esse campo; `CompanyInput`/`UpdateCompanyUseCaseBean`/`CompanyService` já documentam `parentCompanyId` como imutável na atualização) **When** esta story é implementada **Then** o guard é construído como método reutilizável em `CompanyService`/`CompanyRepository` — testado direto (unit + integration), sem novo endpoint, sem novo campo no YAML, sem novo Use Case/Delegate — pronto para quando existir edição de hierarquia em Etapa futura **And** o teste de regressão existente `CompanyServiceBeanTest.updateShouldPersistWhenValidAndNotTouchParent` continua passando sem alteração (parentCompanyId segue imutável nesta story).

## Tasks / Subtasks

- [ ] Task 1: Query nativa recursiva no repositório (AC: 1, 2, 3)
  - [ ] Adicionar em `CompanyRepository` (`flow-organization-domain/.../corporate/company/internal/CompanyRepository.java`) um método nativo `@Query` com `WITH RECURSIVE` que sobe a cadeia de `PARENT_COMPANY_ID` a partir de `candidateParentCompanyId` e verifica se `companyId` aparece nela (ver SQL exato em Dev Notes).
  - [ ] Expor como `default boolean wouldCreateCycle(Long companyId, Long candidateParentCompanyId)`, convertendo o `int` (0/1) da query nativa — não vincular boolean/EXISTS direto ao retorno do `@Query` (ver Dev Notes, risco de mapeamento nativo).
- [ ] Task 2: Guard no domain service (AC: 1, 2, 4)
  - [ ] Adicionar `void assertNoCycle(@NonNull Long companyId, @NonNull Long candidateParentCompanyId)` à interface `CompanyService` (specification), com Javadoc explicando que é guarda antecipada sem call site nesta Etapa.
  - [ ] Implementar em `CompanyServiceBean`: chama `companyRepository.wouldCreateCycle(...)`; se `true`, lança `new ScosException(SCOS_COMPANY_004)` (código e mensagens PT/EN já existem — nada novo a adicionar no bundle).
- [ ] Task 3: Testes (AC: 1, 2, 3, 4)
  - [ ] Unitário: acrescentar métodos em `CompanyServiceBeanTest.java` (mesmo padrão Given/When/Then já usado ali) — mocka `companyRepository.wouldCreateCycle` retornando `true`/`false`, assere throw com `SCOS_COMPANY_004.getCode()` / ausência de throw.
  - [ ] Integração (query real): nova classe em `flow-organization-boot/src/test/java/.../` estendendo `ScosOrganizationTestUtil`, mas **sem usar MockMvc** — autowira `CompanyRepository` direto e monta uma hierarquia de 3+ níveis via `companyRepository.merge(...)` para provar que a CTE detecta o ciclo indireto e aceita o caso sem ciclo. Manter sufixo `*ControllerTest` mesmo sem chamada HTTP (ver Dev Notes — é convenção deliberada do projeto, não erro).
  - [ ] Rodar (ou inspecionar) `CompanyServiceBeanTest.updateShouldPersistWhenValidAndNotTouchParent` para confirmar que nada nesta story o quebra — não é pra alterar esse teste.
- [ ] Task 4: Não expandir escopo (AC: 4)
  - [ ] NÃO alterar `etc/api/organization/ScosOrganization_Company.yml` (nem `UpdateCompanyRequest`, nem endpoint novo).
  - [ ] NÃO criar Delegate/Use Case de edição de hierarquia — decisão confirmada com o PM em 2026-07-18: este guard fica pronto e sem uso até a Etapa que abrir edição de `parentCompanyId`.

## Dev Notes

### Contexto crítico — leia antes de implementar

**`parentCompanyId` é imutável hoje — isso já foi confirmado e é intencional, não um bug a corrigir.**
- `Company.java` (entidade) só seta `parentCompany` via `@Builder`, usado exclusivamente em `CompanyServiceBean.create()`.
- `CompanyServiceBean.update()` NUNCA toca `parentCompany` — não lê `companyInput.parentCompanyId()`.
- `CompanyInput` (domain DTO) tem Javadoc explícito: *"Na atualização, `parentCompanyId` (...) é ignorado (...) é imutável"*.
- `UpdateCompanyUseCaseBean` (usecase) nem lê o campo do request.
- `UpdateCompanyRequest` (schema OpenAPI, `etc/api/organization/ScosOrganization_Company.yml`) **não tem** `parentCompanyId` — só `CreateCompanyRequest` tem.
- Existe um teste de regressão que já prova isso: `CompanyServiceBeanTest.updateShouldPersistWhenValidAndNotTouchParent` (linha ~248) — cria uma Company com `parentCompany` setado, chama `update()`, e assere que o parent não mudou.

**Consequência para esta story:** dado que `parentCompanyId` só é setado na criação (quando a Empresa nova ainda não tem `COMPANY_ID` gerado, logo não pode ser ancestral de ninguém), um ciclo é estruturalmente impossível de ocorrer HOJE por qualquer caminho real do sistema. O PRD já registrou essa lacuna explicitamente:

> `[NOTE FOR PM: (...) Hoje isso é estruturalmente improvável porque parentCompanyId não é editável após a criação, mas a regra deve existir antes de qualquer endpoint futuro de edição de hierarquia.]` — PRD FR-1, `_bmad-output/planning-artifacts/prds/prd-SawCunhaOS-Organization-2026-07-18/prd.md:206`

**Decisão confirmada com o PM (2026-07-18):** implementar o guard (`assertNoCycle`/`wouldCreateCycle`) como peça reutilizável em `CompanyService`/`CompanyRepository` — testada diretamente (unit + integration), SEM criar endpoint, SEM adicionar campo ao YAML, SEM Use Case novo. É trabalho de fundação para a primeira story futura que abrir edição de hierarquia — não é código morto por engano, é intencional. **Não "corrija" isso adicionando um endpoint por conta própria.**

### SQL da CTE recursiva (AD-7)

Único precedente de `@Query` nativo no projeto: `ResourceRepository.upsert()` (`flow-organization-domain/.../access/resource/internal/ResourceRepository.java:38-71`) — mesmo padrão de `@Query(value = "...", nativeQuery = true)` + `@Param`, texto SQL em text-block, tabela prefixada por schema `scos.`.

Lógica: para saber se atribuir `candidateParentCompanyId` como pai de `companyId` fecha um ciclo, suba a cadeia de pais a partir de `candidateParentCompanyId` — se `companyId` aparecer nessa cadeia (ou for o próprio `candidateParentCompanyId`), há ciclo.

```java
@Query(value = """
        WITH RECURSIVE ancestors AS (
            SELECT COMPANY_ID, PARENT_COMPANY_ID
            FROM scos.SCOS_COMPANY
            WHERE COMPANY_ID = :candidateParentCompanyId
            UNION ALL
            SELECT c.COMPANY_ID, c.PARENT_COMPANY_ID
            FROM scos.SCOS_COMPANY c
            INNER JOIN ancestors a ON c.COMPANY_ID = a.PARENT_COMPANY_ID
        )
        SELECT CASE WHEN EXISTS (SELECT 1 FROM ancestors WHERE COMPANY_ID = :companyId) THEN 1 ELSE 0 END
        """, nativeQuery = true)
int wouldCreateCycleFlag(@Param("companyId") Long companyId, @Param("candidateParentCompanyId") Long candidateParentCompanyId);

default boolean wouldCreateCycle(Long companyId, Long candidateParentCompanyId) {
    return wouldCreateCycleFlag(companyId, candidateParentCompanyId) == 1;
}
```

**Por que `CASE WHEN ... THEN 1 ELSE 0 END` e não `EXISTS(...)` direto:** o único precedente nativo do projeto (`ResourceRepository.upsert`) retorna `int` (linhas afetadas), não boolean escalar. Mapear um `boolean`/`EXISTS` cru como retorno de `@Query(nativeQuery=true)` é frágil entre driver/Hibernate; `int` + `default` method wrapper é o padrão mais seguro e já usado no projeto (ver `existsByTaxIdentifier`, `existsByCnaePrincipalId` etc. no mesmo `CompanyRepository` — todos `default boolean` sobre uma consulta QueryDSL/JPA; aqui é a mesma ideia, só que a fonte é nativa).

### Onde cada peça vai (camadas)

- **Repositório** (`domain/corporate/company/internal/CompanyRepository.java`): método nativo + wrapper `default boolean`. Já existe, só adicionar métodos.
- **Specification** (`domain/corporate/company/specification/CompanyService.java`): novo método na interface pública.
- **Bean** (`domain/corporate/company/service/CompanyServiceBean.java`): implementação, `@Transactional(readOnly = true)` (é só leitura — consistente com `findById`/`findAll` no mesmo arquivo).
- `@NonNull` nos parâmetros de `assertNoCycle` é `org.jspecify.annotations.NonNull` (regra do projeto para método público de domain service) — não `lombok.NonNull`.
- **Nada em `usecase`, `api` ou `boot` (Liquibase)** — não há Use Case, Delegate nem mudança de schema nesta story. `SCOS_COMPANY_004` já existe no enum (`ExceptionCodeError.java:75`) e nas mensagens PT/EN (`scos_message_organization.properties:33`, `scos_message_organization_en.properties:33`) — não criar/duplicar.

### Testing Standards

- Unitário: `flow-organization-domain/src/test/java/.../company/service/CompanyServiceBeanTest.java` — JUnit 5 + Mockito, `@ExtendWith(MockitoExtension.class)`, `@Mock`/`@InjectMocks` já configurados no arquivo; seguir o padrão `assertThatThrownBy(...).hasFieldOrPropertyWithValue("code", SCOS_COMPANY_004.getCode())` já usado nos outros testes de `create`.
- Integração real da query nativa: só é possível em Postgres real (schema `scos.`, sintaxe `WITH RECURSIVE`) — módulo `boot`, estendendo `ScosOrganizationTestUtil` (Testcontainers singleton Postgres+Redis, `@Sql` isola por método). Autowire `CompanyRepository` direto (o contexto `@SpringBootTest` expõe qualquer bean, não só os do controller). **Não precisa de MockMvc/HTTP** — não existe endpoint pra chamar.
- Nomenclatura: manter sufixo `*ControllerTest` mesmo sem MockMvc, por convenção explícita do projeto de não fragmentar a nomenclatura de teste de integração ("Ao criar teste novo de integração, siga o padrão vigente (`*ControllerTest`) para não fragmentar" — `project-context.md`, seção *Nomenclatura de teste de integração*). Não renomear para `*IT`/`*RepositoryTest`.
- `code`/idempotência (jDempotent) não se aplicam aqui — não há chamada de API/POST envolvida, só leitura.

### Project Structure Notes

- Nenhum módulo novo, nenhum pacote novo — tudo dentro de `domain/corporate/company/{internal,specification,service}` já existentes.
- Nenhuma mudança em `etc/api/organization/*.yml`, nenhuma mudança em Liquibase (`flow-organization-resources`).
- Sem conflito de convenção detectado — método nativo segue exatamente o único precedente do projeto (`ResourceRepository`).

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Story 1.1] — Given/When/Then originais.
- [Source: _bmad-output/planning-artifacts/prds/prd-SawCunhaOS-Organization-2026-07-18/prd.md#FR-1] — `[NOTE FOR PM]` sobre imutabilidade de `parentCompanyId` e regra antecipada.
- [Source: _bmad-output/planning-artifacts/architecture/architecture-SawCunhaOS-Organization-2026-07-18/ARCHITECTURE-SPINE.md#AD-7] — CTE recursiva obrigatória, não caminhada Java, não closure table.
- [Source: organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/corporate/company/service/CompanyServiceBean.java] — `depthOf()` (linha 230) só sobe; `resolveParentCompany()` (linha 216) é o único ponto que hoje toca `parentCompany` fora da criação.
- [Source: organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/corporate/company/dto/CompanyInput.java] — Javadoc de imutabilidade de `parentCompanyId`.
- [Source: organization/flow-organization-domain/src/test/java/br/com/sawcunhaos/organization/domain/corporate/company/service/CompanyServiceBeanTest.java#updateShouldPersistWhenValidAndNotTouchParent] — regressão a preservar.
- [Source: organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/access/resource/internal/ResourceRepository.java] — único precedente de `@Query(nativeQuery=true)` no projeto.
- [Source: organization/flow-organization-shared/src/main/java/br/com/sawcunhaos/organization/shared/exception/ExceptionCodeError.java:75] — `SCOS_COMPANY_004` já reservado.
- [Source: organization/flow-organization-shared/src/main/resources/scos_message_organization.properties:33, scos_message_organization_en.properties:33] — mensagens PT/EN já existentes.
- [Source: etc/api/organization/ScosOrganization_Company.yml:1092-1119] — `UpdateCompanyRequest` sem `parentCompanyId`.
- [Source: organization/flow-organization-boot/src/test/java/br/com/sawcunhaos/organization/boot/infrastructure/ScosOrganizationTestUtil.java] — base de integração (Testcontainers singleton, `@SpringBootTest`).

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
