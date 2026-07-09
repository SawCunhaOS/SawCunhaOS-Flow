# Suíte de Testes de Integração das APIs REST (Organization)

**Data**: 2026-07-09  
**Status**: 🔄 Em Análise  
**Tipo**: 🆕 Nova Feature

---

## ⚠️ Princípio SRP — Uma Funcionalidade por Ideia

> **Regra**: Uma ideia = uma funcionalidade. Features independentes → arquivos separados.

- **Nome da funcionalidade**: `suite-testes-integracao-apis-rest-organization`
- **Resumo em uma frase**: Cobrir todas as APIs REST já implementadas (delegates existentes) com testes de integração full-stack no padrão de `DepartmentControllerTest`, garantindo verificação de contrato (happy path, segurança, erros RFC 9457 e idempotência) contra a infra real.

**Checklist SRP**:
- [x] Esta ideia cobre exatamente uma funcionalidade (suíte de IT das APIs REST existentes)
- [x] Não mistura features independentes (gRPC, k6, APIs sem delegate ficam fora)
- [x] O nome é específico

---

## 1️⃣ Visão

### Problema
Só **1 de 11** delegates REST tem teste de integração completo (`DepartmentControllerTest`). `ConfigurationControllerTest` é um stub vazio. Os outros 9 delegates (AddressType, ContactType, Position, 4×Reason, Cnae, LegalNature) não têm nenhuma cobertura de integração. Regressões em segurança (JWT/permissão), contrato de erro (RFC 9457 ProblemDetail), idempotência (jDempotent/Redis) e regras de negócio passam despercebidas até produção.

### Objetivo
Ter **1 classe de IT por delegate REST implementado** (11 classes), cada uma replicando a cobertura completa do exemplar `DepartmentControllerTest`. Critério de sucesso: `mvn verify` executa toda a suíte verde, exercitando o caminho MockMvc → filtros JWT → `@PreAuthorize` → delegate → use case → domain → Postgres real, para cada operação de cada delegate.

### Fora de Escopo
- **APIs sem delegate implementado** — Company (CRUD), Employee, Login, Outbox, work-schedule, reason-position-change. YAML existe, mas sem `XxxDelegate` → não são "APIs já configuradas".
- **gRPC** (registry / validateAuthority) — exige infra de teste server-side própria; change separada.
- **Testes de carga k6** — cobertos por ideia própria (`20260705_suite-testes-k6-ambiente-integracao.md`).
- **Testes unitários de domain/use case** — já existem (`*ServiceBeanTest`), não é o alvo.

---

## 2️⃣ Requisitos

### Funcionais
Por delegate, replicar a matriz do `DepartmentControllerTest` conforme o **shape** da API:

- [ ] **RF-01 (Shape A — enable/disable)**: AddressType, ContactType, Position, ReasonActivate, ReasonInactivate, ReasonEnable, ReasonDisable — cobrir GET(lista+id), POST, PUT, `enable`, `disable`. Cenários por operação: happy path + 401 (sem token) + 401 (token inválido fora do JWKS) + 403 (sem permissão, `stubValidateAuthorityWithoutPermissions`) + 404 (id inexistente) + 409 (code duplicado) + 422 (já ativo / já inativo) + 400 (validação de campos) + idempotência no POST.
- [ ] **RF-02 (Shape B — DELETE)**: Cnae, LegalNature — cobrir GET(lista+id), POST, PUT, `DELETE`. Erros: 404 (`SCOS_CNAE_001`/`SCOS_LEGAL_NATURE_001`), 409 (`_002`, code duplicado), 422 (`_003`, exclusão com vínculo FK — usar a company do seed que referencia cnae/legal_nature).
- [ ] **RF-03 (Shape C — Configuration)**: preencher o stub — GET all, GET keys, GET by id (chave string), PUT update. Erros: 401/403/404 + validação. Sem create/delete (delegate não expõe).
- [ ] **RF-04**: Toda resposta de erro validada contra o contrato RFC 9457 (`type`, `title`, `status`, `detail`, `instance`, `code`, `requestId`, `timestamp`; `errors[]` na validação).

### Não-Funcionais
- [ ] **RNF-01**: Reusar a infra singleton existente (`ScosOrganizationTestUtil`) — sem novos containers, sem reiniciar contexto Spring (Liquibase roda 1×/JVM).
- [ ] **RNF-02**: Isolamento por método garantido pelo `@Sql` (setsup BEFORE / TRUNCATE RESTART IDENTITY AFTER). POSTs usam `code` único por teste (Redis/jDempotent não reseta entre métodos).
- [ ] **RNF-03**: Surefire em JVM único (`forkCount=1`/`reuseForks=true`) — pré-requisito do singleton de containers.

---

## 3️⃣ Arquitetura

### Componentes Afetados
```
scos-organization-boot (src/test)
├── api/catalog/AddressTypeControllerTest.java     : adição
├── api/catalog/ContactTypeControllerTest.java     : adição
├── api/position/PositionControllerTest.java       : adição
├── api/reason/ReasonActivateControllerTest.java   : adição
├── api/reason/ReasonInactivateControllerTest.java : adição
├── api/reason/ReasonEnableControllerTest.java     : adição
├── api/reason/ReasonDisableControllerTest.java    : adição
├── api/company/CnaeControllerTest.java            : adição
├── api/company/LegalNatureControllerTest.java     : adição
├── api/configuration/ConfigurationControllerTest.java : modificação (stub → completo)
└── infrastructure/ScosOrganizationTestUtil.java   : reuso (sem alteração esperada)
```

### Fluxo Principal (por teste)
```
MockMvc → filtro JWT (WireMock/JWKS) → @PreAuthorize (GrpcMock authority)
        → Delegate → UseCase → Domain → Postgres real → ProblemDetail/data-wrapper
```

### Decisões Técnicas
| Decisão | Escolha | Alternativa Descartada | Motivo |
|---------|---------|------------------------|--------|
| Estrutura de arquivos | 1 classe por delegate | Base abstrata por shape + subclasses | Consistência com `DepartmentControllerTest`; menos indireção, leitura direta |
| Grão de cobertura | Completo (igual Department) | Núcleo / Smoke | Máximo valor de regressão em segurança + contrato de erro |
| Infra | Reusar singleton existente | Container por classe | Evita conflito de porta fixa (5432/6379) e recriação de contexto |
| Fonte de fixtures | Seed `setsup_database.sql` | Fixtures inline por teste | Já cobre todos os agregados (1 linha cada) |

### Banco de Dados
- **Impacto**: ❌ Não (só leitura/escrita transacional via API; sem migration nova).
- Depende do seed atual cobrir todos os agregados — **confirmado** (28 inserts, todos os `SCOS_*` alvo presentes).

---

## 4️⃣ Implementação

### Arquivos

**Novos**: 9 classes de IT (ver árvore acima).

**Modificados**:
- `ConfigurationControllerTest.java` — stub vazio → suíte completa.

### Tarefas
- [ ] **T-01**: AddressType + ContactType (shape A, catálogo) — valida também filtro `entityType`/`active`.
- [ ] **T-02**: Position (shape A) — valida filtros `departmentId`/`active` e regra de vínculo com department.
- [ ] **T-03**: 4×Reason (shape A) — idênticos em forma; confirmar códigos de erro `SCOS_REASON_*` por variante.
- [ ] **T-04**: Cnae + LegalNature (shape B, DELETE) — cobrir 422 de exclusão com vínculo FK (company do seed).
- [ ] **T-05**: Configuration (shape C) — preencher stub; chave string, update-only.
- [ ] **T-06**: Rodar `mvn verify` completo e estabilizar (isolamento/idempotência).

### Riscos e Edge Cases
1. **Códigos de erro por agregado** — cada delegate tem sua família `SCOS_XXX_00N`; precisam ser lidos de `ExceptionCodeError` antes de escrever asserts (não assumir).
2. **DELETE com FK (Cnae/LegalNature)** — precisa que o seed mantenha o vínculo company↔cnae/legal_nature ativo para disparar 422 `_003`; validar que TRUNCATE/RESTART não quebra a ordem.
3. **Idempotência POST** — todo POST bem-sucedido precisa de `code` único no método (Redis não reseta entre métodos na mesma JVM).
4. **Filtros específicos** — Position (`departmentId`,`active`) e catálogos (`entityType`,`active`) têm ramos não cobertos pelo exemplar Department; exigem casos extras.
5. **Volume** — ~25-35 casos × 11 delegates → suíte grande; tempo de `verify` sobe (containers singleton mitigam).

---

## 📎 Referências
- Exemplar: `scos-organization-boot/src/test/.../api/department/DepartmentControllerTest.java`
- Infra base: `.../infrastructure/ScosOrganizationTestUtil.java`, `ScosJwtTestSupport.java`, `ScosOrganizationWiremockUtil.java`
- Contrato de erro: `ExceptionCodeError.java` (famílias `SCOS_*_00N`)
- Seed: `scos-organization-boot/src/test/resources/postgresql/setsup_database.sql`
- Ideia relacionada (fora de escopo): `20260705_suite-testes-k6-ambiente-integracao.md`

---
