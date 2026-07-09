## Why

Apenas **1 de 11** delegates REST implementados possui teste de integração completo
(`DepartmentControllerTest`); `ConfigurationControllerTest` é um stub vazio e os outros
9 delegates (AddressType, ContactType, Position, 4×Reason, Cnae, LegalNature) não têm
nenhuma cobertura de integração. Regressões em segurança (JWT/`@PreAuthorize`), contrato
de erro (RFC 9457 ProblemDetail), idempotência (jDempotent/Redis) e regras de negócio
passam despercebidas até produção. Esta change fecha a lacuna replicando a cobertura
full-stack do exemplar para todos os delegates existentes.

## What Changes

- Criar **9 novas classes de IT** — uma por delegate REST sem cobertura — que sobem o
  stack real (`ScosOrganizationTestUtil`) e exercitam MockMvc → filtros JWT →
  `@PreAuthorize` → delegate → use case → domain → Postgres real.
- Preencher o stub vazio **`ConfigurationControllerTest`** com a suíte completa do shape
  de configuração (GET all / GET keys / GET by id / PUT update).
- Cada IT replica a matriz de cenários do `DepartmentControllerTest` conforme o **shape**
  da API (enable/disable, DELETE ou configuration): happy path, 401 (sem token / token
  fora do JWKS), 403 (sem permissão), 404, 409 (code duplicado), 422 (regra de negócio),
  400 (validação de campos) e idempotência no POST.
- Toda resposta de erro é validada contra o contrato **RFC 9457** (`type`, `title`,
  `status`, `detail`, `instance`, `code`, `requestId`, `timestamp`; `errors[]` na validação).
- **Sem produção de código de aplicação**: nenhuma mudança em delegates, use cases, domain,
  migrations ou seed. Apenas `src/test`.

## Capabilities

### New Capabilities
- `rest-api-integration-testing`: Cobertura de teste de integração full-stack para os
  delegates REST implementados do módulo Organization — define a matriz de cenários
  obrigatória por shape de API (segurança, contrato de erro RFC 9457, idempotência, regras
  de negócio) e as restrições de infra (singleton de containers, isolamento por `@Sql`,
  `code` único por POST) que cada classe de IT deve satisfazer.

### Modified Capabilities
<!-- Nenhuma: esta change não altera requisitos de comportamento das APIs, apenas adiciona
     cobertura de teste sobre o comportamento já especificado. -->

## Impact

- **Código afetado**: somente `scos-organization-boot/src/test` — 9 classes novas de IT +
  preenchimento de `ConfigurationControllerTest`. Reuso sem alteração de
  `ScosOrganizationTestUtil`, `ScosJwtTestSupport`, `ScosOrganizationWiremockUtil`.
- **Infra de teste**: reusa os containers singleton (Postgres 5432 / Redis 6379) e o seed
  `setsup_database.sql`; nenhum container ou contexto Spring novo. Pré-requisito mantido:
  Surefire em JVM único (`forkCount=1`/`reuseForks=true`).
- **Build**: `mvn verify` passa a executar a suíte completa; tempo de verify sobe (~25-35
  casos × 10 classes), mitigado pelos containers singleton.
- **Fora de escopo** (não impactado): APIs sem delegate (Company, Employee, Login, Outbox,
  work-schedule, reason-position-change), gRPC (registry/validateAuthority), testes de carga
  k6 e testes unitários de domain/use case já existentes.
