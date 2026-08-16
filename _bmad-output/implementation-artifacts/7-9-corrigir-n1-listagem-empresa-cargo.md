---
baseline_commit: b72b2dcc0f63997fe313658e358e68dc506dd645
---

# Story 7.9: Corrigir Consultas N+1 em Listagem de Empresa e Cargo

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como Analista de RH,
Eu quero que listar Empresas e Cargos não faça consultas extras ao banco proporcionais ao tamanho da página,
Para que a listagem continue rápida conforme o volume de dados cresce, e para que `GET /v1/positions` pare de devolver `department` sempre `null` (bug de contrato, achado junto).

> **Correção de rota:** não existe `GET /v1/departments/{id}/positions` no contrato (`etc/api/organization/ScosOrganization_Department-Position.yml`). A rota real é **`GET /v1/positions`**, com `departmentId` como **query param opcional** (`#/components/parameters/departmentIdFilter`, `required: false`) — confirmado lendo o YAML. Isso muda o design do fix do AC 2/Task 2: não dá pra assumir "toda a página pertence ao mesmo Departamento", porque o filtro pode não vir.

## Acceptance Criteria

1. **Given** `GET /v1/companies` **When** a página tem Empresas com `parentCompanyId` preenchido (filiais) **Then** a consulta não dispara 1 `SELECT` extra por filial pra resolver o `name`/`nameTreatment`/`taxIdentifier` da matriz — confirmado por leitura de código: `CompanyMapper.toCompanyOutput` (usado por `CompanyServiceBean.findAll`, linha 192) hidrata `parentCompany` (proxy `@ManyToOne(LAZY)`) por completo em cada linha, mas `CompanyApiMapper.toApiCompanies` (schema `Companies`, usado por `FindAllCompanyUseCaseBean` — o mapper de fato usado na listagem) **nunca lê esse campo** — dado buscado e descartado, 1 vez por filial da página.
2. **Given** `GET /v1/positions` (com ou sem `departmentId` informado — filtro é opcional no contrato, `PositionRepository.findAllFiltered` já trata `departmentId == null` corretamente, sem quebrar) **When** a página é montada **Then** cada `Position` retorna `department` populado (`code`/`description`/`active`/`id`) — hoje sempre `null`: `PositionMapper.toPositionOutput` (usado por `PositionServiceBean.findAll`, linha 111) tem `@Mapping(target = "department", ignore = true)`, e `PositionApiMapper.toApiPosition` (schema `Position`, completo — não existe uma versão resumida pra lista de Cargo) usa esse campo direto **And** a correção não dispara 1 consulta de Departamento por linha da página — dispara **no máximo 1 consulta batelada** (`IN (...)`), pelos IDs de Departamento distintos presentes na página (1 quando o filtro `departmentId` é usado, N quando não é, mas nunca mais que o tamanho da página, e nunca proporcional a "1 por linha").
3. **Given** `GET /v1/companies/{id}` (`FindCompanyUseCaseBean` → `CompanyApiMapper.toApiCompany`) e `GET /v1/positions/{id}` (`PositionServiceBean.findById` → `toPositionOutput` privado, que já enriquece `department`) **When** consultados **Then** continuam retornando `parentCompany`/`department` completos, sem nenhuma mudança de comportamento — a correção é só na listagem, o detalhe (1 linha) já era e continua correto.

## Tasks / Subtasks

- [ ] Task 1: `CompanyMapper` — mapeamento sem `parentCompany` para a listagem (AC: 1, 3)
  - [ ] Em `domain/corporate/company/service/CompanyMapper.java`, adicionar um segundo método `@Mapping`-anotado, mesma interface, mesma entrada (`Company`), mesma saída (`CompanyOutput`), só que ignorando `parentCompany`:
    ```java
    @Mapping(target = "taxIdentifier", source = "taxIdentifier.cnpj")
    @Mapping(target = "legalNatureId", source = "legalNature.id")
    @Mapping(target = "cnaePrincipalId", source = "cnaePrincipal.id")
    @Mapping(target = "parentCompany", ignore = true)
    CompanyOutput toCompanyOutputSummary(Company company);
    ```
    `toCompanyOutput` (existente, com `parentCompany` completo) **não muda** — continua servindo `findById`/`toApiCompany`.
  - [ ] Em `CompanyServiceBean.findAll` (linha 190-192), trocar `.map(companyMapper::toCompanyOutput)` por `.map(companyMapper::toCompanyOutputSummary)`.
  - [ ] **Não** tocar em `CompanyServiceBean.findById`/`findCompanyById` — continuam usando `toCompanyOutput` (com `parentCompany`), sem mudança.

- [ ] Task 2: `PositionServiceBean.findAll` — resolve os Departamentos da página numa consulta batelada (AC: 2, 3)
  - [ ] Em `PositionServiceBean.findAll` (linha 107-112), capturar `Page<Position> positionPage = positionRepository.findAllFiltered(departmentId, active, pageable);` (entidades, ainda sem mapear) e extrair os IDs de Departamento distintos **sem inicializar o proxy**: `positionPage.getContent().stream().map(p -> p.getDepartment().getId()).distinct().toList()` — `.getId()` de relação `@ManyToOne(LAZY)` é grátis (FK já carregada), mesmo padrão confirmado em `EmployeeServiceBean.toEmployeeOutput`.
  - [ ] Buscar os Departamentos de uma vez, batelado: `departmentRepository.findAllById(departmentIds)` — método **já herdado** de `CrudRepository` via `BaseJpaRepository` (`DepartmentRepository` extends `BaseJpaRepository<Department, Long>`), **não precisa criar método novo** — 1 `SELECT ... WHERE id IN (...)`, independente de quantos Departamentos distintos existirem na página (o pior caso é "1 por linha da página", nunca mais que isso, e nunca "1 consulta por linha").
  - [ ] Montar `Map<Long, DepartmentOutput> departmentsById` com `departmentMapper::toDepartmentOutput` sobre o resultado acima.
  - [ ] Mapear a página anexando o Departamento certo por linha, **sem** tocar em `position.getDepartment()` de novo (só o `.getId()` já usado): trocar `.map(positionMapper::toPositionOutput)` por algo como `.map(position -> positionMapper.toPositionOutput(position).toBuilder().department(departmentsById.get(position.getDepartment().getId())).build())` — **checar se `PositionOutput` (record `@Builder`) já suporta `toBuilder()`**; se não, adicionar `@Builder(toBuilder = true)` no record (mudança mínima, sem efeito colateral nos outros usos do builder).
  - [ ] Caso a página venha vazia, pular a consulta de Departamentos (`if (departmentIds.isEmpty())`) — evita `IN ()` inválido/desnecessário.
  - [ ] **Não** tocar em `PositionServiceBean.findById`/o método privado `toPositionOutput(Position)` (linha 159) — já resolve `department` corretamente (1 linha, sem N+1 nesse caminho).
  - [ ] **Não** trocar `positionMapper.toPositionOutput` (o método MapStruct, `@Mapping(target="department", ignore=true)`) — continua correto como está, é a base reaproveitada tanto por `findAll` (Task 2) quanto pelo método privado de `findById` (que sobrescreve `department` depois, já existente).

- [ ] Task 3: Testes (AC: 1, 2, 3)
  - [ ] `CompanyServiceBeanTest` (já existe): `findAllShouldReturnMappedPage` — trocar o mock de `companyMapper.toCompanyOutput(...)` por `companyMapper.toCompanyOutputSummary(...)` (o método agora de fato chamado por `findAll`); adicionar `verify(companyMapper, never()).toCompanyOutput(any())` no teste de `findAll`, pra travar a regressão (se alguém trocar de volta pro método caro, o teste denuncia).
  - [ ] `PositionServiceBeanTest` (já existe): dois cenários novos em `findAll` — (a) com `departmentId` informado: `departmentRepository.findAllById(...)` é chamado **exatamente 1 vez**, todos os `PositionOutput` retornados têm o mesmo `department`; (b) sem `departmentId` (página com Cargos de Departamentos diferentes): `departmentRepository.findAllById(...)` ainda é chamado **exatamente 1 vez** (não uma vez por Cargo), e cada `PositionOutput` recebe o `department` correspondente ao seu próprio `departmentId` (não o de outra linha).
  - [ ] Integração (`CompanyControllerTest`/`PositionControllerTest`, já existem): sem mudança de asserção — a resposta HTTP não muda de formato (só o `department` de `positions`, que estava faltando, agora aparece — se o teste de integração de `getAllPositions` não afirma `department` hoje, é sinal de que o bug já deveria ter sido pego antes; adicionar a asserção que falta).

## Dev Notes

### Contexto crítico — leia antes de implementar

**Achado numa auditoria pedida pelo usuário — "revisar o projeto procurando N+1" — não numa story nova sendo escrita do zero.** Os dois problemas são reais e já estão em produção (Epic 1, `done`), não hipóteses. Confirmados por leitura direta do código, não por suposição: os 13 `@Mapper` MapStruct do módulo `domain` foram auditados um por um — só `CompanyMapper` e `PositionMapper` têm `@Mapping` de relação; os outros 11 (`ReasonEnable/Disable/Activate/InactivateMapper`, `AuthorityResponseMapper`, `PositionWorkScheduleMapper`, `ContactTypeMapper`, `AddressTypeMapper`, `CnaeMapper`, `LegalNatureMapper`) são entidades planas, sem relação `@ManyToOne`/`@OneToMany` mapeada — confirmado sem risco de N+1.

**Por que os dois bugs são de natureza diferente, mesmo parecendo o mesmo padrão.** Company: a listagem builda um dado (`parentCompany`) que o schema de resposta **nunca usa** — puro desperdício, a correção é só parar de buscar. Position: a listagem builda `department: null`, mas o schema de resposta (`Position`, sem versão resumida pro Cargo — diferente de Company/Employee, que têm `Companies`/`Employees` resumidos) **espera** o campo populado — é regressão de dado, não só desperdício.

**Correção sobre a suposição inicial (importante, achada só depois de checar o contrato):** a primeira versão desta story assumia que `departmentId` era obrigatório em `GET .../positions`, então "toda a página tem o mesmo Departamento" e dava pra resolver 1 vez com `departmentService.findById(departmentId)`. **Isso está errado** — `departmentId` é query param **opcional** (`etc/api/organization/ScosOrganization_Department-Position.yml:548-551`, `required: false`), e o delegate propaga isso: `PositionDelegate.getAllPositions` faz `departmentId.orElse(null)` (linha 71), repassado até `PositionServiceBean.findAll(@NonNull Long departmentId, ...)` — o `@NonNull` aí é `org.jspecify.annotations.NonNull`, **anotação só de análise estática, sem checagem em runtime**, então `null` passa direto. `PositionRepository.findAllFiltered` já trata isso corretamente hoje (`if (departmentId == null) ... predicateDepartmentIdAndActive` monta o predicate condicionalmente) — ou seja, uma página **pode conter Cargos de Departamentos diferentes** quando o filtro não é usado. A correção certa é buscar os Departamentos distintos da página numa consulta batelada (`findAllById`, `IN (...)`), não presumir 1 Departamento por página.

**Por que não virou 1 query com `JOIN FETCH`/`@EntityGraph` em nenhum dos dois casos.** Para Company, não tem sentido — o dado nem é usado. Para Position, um `JOIN FETCH department` na query paginada funcionaria, mas seria mais mudança (tocar `PositionRepository.findAllFiltered`, que já é `default` QueryDSL) pro mesmo resultado que "resolver os Departamentos distintos numa query batelada, fora do loop" — que reaproveita um método já herdado (`findAllById`) e não mexe na query paginada existente. Escolhido pelo critério de menor mudança com o mesmo efeito (nº de queries fixo, não proporcional ao tamanho da página), não por regra geral "sempre prefira X".

**Nenhuma mudança de contrato OpenAPI, schema de banco ou permissão** — os dois bugs são só de camada Java (mapper + service), o `Position`/`Companies` do contrato já preveem os campos certos (é o código que não entrega).

### Onde cada peça vai (camadas)

- `domain/corporate/company/service/CompanyMapper.java`: `+toCompanyOutputSummary` (novo método, `toCompanyOutput` intocado).
- `domain/corporate/company/service/CompanyServiceBean.java`: `findAll` troca de mapper (1 linha).
- `domain/corporate/position/dto/PositionOutput.java`: `@Builder(toBuilder = true)` se ainda não tiver.
- `domain/corporate/position/service/PositionServiceBean.java`: `findAll` resolve `department` da página numa consulta batelada, fora do `.map()`.
- Nenhuma mudança em `usecase`, `api`, contrato OpenAPI, Liquibase ou permissões.

### Testing Standards

- Testes existentes (`CompanyServiceBeanTest`/`PositionServiceBeanTest`) são estendidos, não recriados — mesmo padrão Mockito já convencionado no projeto.
- O teste de "chamado 1 vez, não N vezes" (`verify(..., times(1))`) é o jeito certo de travar regressão de N+1 num teste unitário — não precisa de Testcontainers/contagem de query real pra isso.

### Project Structure Notes

- Nenhum pacote novo — tudo em classes já existentes desde o Epic 1.

### References

- [Source: organization/flow-organization-domain/.../corporate/company/service/CompanyServiceBean.java:190-192] — `findAll` atual, `.map(companyMapper::toCompanyOutput)` a trocar.
- [Source: organization/flow-organization-domain/.../corporate/company/service/CompanyMapper.java] — `toCompanyOutput` atual, base do novo `toCompanyOutputSummary`.
- [Source: organization/flow-organization-usecase/.../corporate/company/CompanyApiMapper.java:52-61] — `toApiCompanies`, confirma que `parentCompany` nunca é lido na listagem.
- [Source: organization/flow-organization-usecase/.../corporate/company/FindAllCompanyUseCaseBean.java] — confirma `toApiCompanies` é o mapper de fato usado por `GET /v1/companies`.
- [Source: organization/flow-organization-domain/.../corporate/position/service/PositionServiceBean.java:107-112,159-168] — `findAll` (bug) e `toPositionOutput` privado (referência correta, usado por `findById`).
- [Source: organization/flow-organization-domain/.../corporate/position/service/PositionMapper.java] — `@Mapping(target = "department", ignore = true)`, confirma a causa raiz.
- [Source: organization/flow-organization-usecase/.../corporate/position/PositionApiMapper.java:29-38] — `toApiPosition`, confirma que o schema de lista usa `department` sem versão resumida.
- [Source: organization/flow-organization-usecase/.../corporate/position/FindAllPositionUseCaseBean.java] — confirma `toApiPosition` (não uma versão resumida) é o mapper de fato usado por `GET /v1/positions`.
- [Source: organization/flow-organization-domain/.../corporate/employee/service/EmployeeServiceBean.java:491-506] — referência do padrão correto já usado no projeto (`.getSupervisor().getId()`/`.getCompany().getId()`/`.getPosition().getId()` — só `.getId()` de relação `LAZY`, nunca outro campo, dentro de um mapper usado por listagem); auditado e confirmado sem problema, usado como contraste pros 2 bugs encontrados.
- [Source: etc/api/organization/ScosOrganization_Department-Position.yml:163-186,547-556] — rota real `GET /v1/positions`, confirma `departmentId` (`departmentIdFilter`) é `required: false`. Não existe `/v1/departments/{id}/positions` no contrato.
- [Source: organization/flow-organization-api/.../delegate/position/PositionDelegate.java:70-72] — `getAllPositions`, confirma `departmentId.orElse(null)` — o filtro pode chegar `null` em `PositionServiceBean.findAll`.
- [Source: organization/flow-organization-domain/.../corporate/position/internal/PositionRepository.java:50-56] e `PositionPredicates.java` (`predicateDepartmentIdAndActive`) — confirmam que `departmentId == null` já é tratado hoje (sem filtro de Departamento), portanto a página pode ter Cargos de Departamentos diferentes.
- [Source: organization/flow-organization-domain/.../corporate/department/internal/DepartmentRepository.java] — `extends BaseJpaRepository<Department, Long>`, confirma `findAllById(Iterable<Long>)` já disponível (herdado de `CrudRepository`), sem precisar criar método novo.

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List

## Change Log

| Data | Descrição |
|------|-----------|
| 2026-08-16 | Story criada — auditoria de N+1 pedida pelo usuário no código já implementado (Epic 1, `done`), não nas specs do Epic 3. 2 bugs reais confirmados por leitura de código: `GET /v1/companies` busca e descarta `parentCompany` por filial da página; `GET /v1/positions` devolve `department: null` sempre (bug de contrato). Auditoria completa dos 13 `@Mapper` do módulo `domain` — só esses 2 tinham o problema. |
| 2026-08-16 | Correção pós-criação: rota do AC2/Task2 estava errada (`/v1/departments/{id}/positions` não existe; rota real é `GET /v1/positions`, `departmentId` opcional). Isso invalidava o fix original ("resolver Departamento 1 vez, assumindo página com Departamento único") — corrigido pra buscar os Departamentos distintos da página numa consulta batelada (`departmentRepository.findAllById`), que funciona com ou sem o filtro aplicado. |
