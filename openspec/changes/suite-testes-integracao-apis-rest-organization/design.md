## Context

O módulo `scos-organization-boot` já tem uma infra de IT madura: `ScosOrganizationTestUtil`
sobe containers singleton (Postgres 5432 / Redis 6379 via `ComposeContainer`), aplica seed
por `@Sql` (setup BEFORE / TRUNCATE RESTART IDENTITY AFTER) e provê MockMvc autoconfigurado,
tokens JWT de teste (`ScosJwtTestSupport`) e stubs de WireMock/GrpcMock
(`ScosOrganizationWiremockUtil`, `stubValidateAuthorityWithoutPermissions`). O exemplar
`DepartmentControllerTest` demonstra a matriz completa de cenários (happy path, segurança,
erros RFC 9457, idempotência, regras de negócio).

Estado atual: 1 delegate coberto (Department), 1 stub vazio (Configuration) e 9 delegates
sem cobertura. O objetivo é replicar o exemplar para os 10 delegates restantes, sem tocar em
código de aplicação.

## Goals / Non-Goals

**Goals:**
- 1 classe de IT por delegate REST implementado, replicando a cobertura do exemplar por shape.
- Verificação de contrato: segurança (JWT/permissão), erro RFC 9457, idempotência, regras de negócio.
- `mvn verify` verde exercitando o stack real de ponta a ponta.
- Reuso total da infra existente — zero containers/contextos novos.

**Non-Goals:**
- Base abstrata por shape com subclasses (mantém-se 1 classe autocontida por delegate).
- Cobertura de APIs sem delegate (Company, Employee, Login, Outbox, work-schedule, reason-position-change).
- gRPC (registry/validateAuthority) e testes de carga k6 — changes separadas.
- Testes unitários de domain/use case (já existem).
- Qualquer mudança em delegates, use cases, domain, migrations ou seed.

## Decisions

**1. Uma classe por delegate, autocontida (não base abstrata por shape).**
Rationale: consistência direta com `DepartmentControllerTest`, leitura sem indireção, cada
arquivo é lido isoladamente. Alternativa descartada: base abstrata por shape + subclasses —
reduz duplicação mas adiciona indireção e acopla variações (filtros, códigos de erro) que
diferem por agregado.

**2. Grão de cobertura completo (igual Department), não smoke/núcleo.**
Rationale: máximo valor de regressão em segurança e contrato de erro, que são exatamente os
pontos que hoje escapam. Alternativa descartada: smoke — barato mas deixa passar as regressões
mais caras (403/401/RFC 9457).

**3. Reusar o singleton existente, não container por classe.**
Rationale: o compose publica portas fixas (5432/6379); dois Postgres simultâneos causariam
conflito de porta e "relation does not exist". O singleton + contexto Spring cacheado é
pré-requisito, não escolha estética. Exige Surefire em JVM único.

**4. Fixtures do seed `setsup_database.sql`, não fixtures inline.**
Rationale: o seed já cobre todos os agregados alvo (incluindo o vínculo company↔cnae/legal_nature
necessário para o 422 de DELETE com FK). POSTs de teste usam `code` único por método para não
colidir no jDempotent (Redis não reseta entre métodos).

**5. Códigos/mensagens de erro lidos das fontes canônicas antes de asserir.**
Rationale: cada agregado tem sua família `SCOS_XXX_00N` e mensagens PT-BR próprias em
`ExceptionCodeError`/bundle; asserts não podem presumir valores. Ler a fonte antes de escrever
cada assert de `code`/`title`/`detail`.

## Risks / Trade-offs

- **Códigos de erro divergentes por agregado** → Ler `ExceptionCodeError` (e o bundle i18n) de
  cada família antes de escrever os asserts; não copiar cegamente do Department.
- **DELETE com FK (Cnae/LegalNature) depende do seed** → Confirmar que o seed mantém a company
  do seed referenciando cnae/legal_nature ativos e que o TRUNCATE/RESTART não quebra a ordem de
  recriação; caso o vínculo não exista, ajustar o cenário para criar o vínculo no próprio teste.
- **Colisão de idempotência entre métodos (Redis compartilhado)** → Cada POST bem-sucedido usa
  `code` único por método; nunca reutilizar `code` de sucesso entre testes da mesma classe.
- **Filtros específicos não cobertos pelo exemplar** → Position (`departmentId`, `active`) e
  catálogos (`entityType`, `active`) exigem cenários extras além dos do Department.
- **Volume/tempo de verify** → ~25-35 casos × 10 classes aumenta o tempo total; mitigado pelo
  singleton (containers sobem 1×/JVM) e contexto Spring cacheado.

## Migration Plan

Não aplicável — mudança confinada a `src/test`. Deploy é a simples adição das classes; rollback
é remover os arquivos de teste. Sem impacto em runtime, banco ou contrato de API.

## Open Questions

- Confirmar, ao implementar Cnae/LegalNature, se o vínculo FK do seed é suficiente para disparar
  o 422 `_003` ou se o cenário precisa montar o vínculo explicitamente.
- Confirmar os nomes exatos das permissões por delegate no `ScosOrganizationPermission` para os
  cenários 403 (o stub nega todas, então o assert é sobre `SCOS-004`, mas o happy path depende
  da permissão correta ser concedida pelo GrpcMock padrão).
