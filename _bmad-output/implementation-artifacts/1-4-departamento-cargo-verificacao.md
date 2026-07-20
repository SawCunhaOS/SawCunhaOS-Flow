# Story 1.4: Departamento e Cargo (Verificação)

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como Analista de RH,
Eu quero cadastrar Departamentos e Cargos vinculados a um Departamento ativo, com ciclo enable/disable,
Para organizar a estrutura interna da empresa.

## Acceptance Criteria

1. **Given** a implementação já existente de `DepartmentServiceBean`/`PositionServiceBean` (confirmada em auditoria de código, FR-3) **When** os testes de regressão (unitários e integração) são executados **Then** confirmam que criar (`PositionServiceBean.create`) **ou** atualizar (`PositionServiceBean.update`) um Cargo apontando para um Departamento `INACTIVE` é rejeitado com `SCOS_DEPARTMENT_006` — via `findActiveDepartmentOrThrow(departmentId)`.
2. **Given** um Departamento com ao menos um Cargo `ACTIVE` vinculado **When** RH tenta `PUT /v1/departments/{id}/disable` **Then** o sistema rejeita com `SCOS_DEPARTMENT_003` — via `DepartmentRepository.existsByIdAndPositionsActive(departmentId)` em `DepartmentServiceBean.disable`.
3. **Given** um Cargo com ao menos um Funcionário `ACTIVE` vinculado **When** RH tenta `PUT /v1/positions/{id}/disable` **Then** o sistema rejeita com `SCOS_POSITION_003` — via `EmployeePositionQueryService.existsActiveEmployeeInPosition(positionId)` em `PositionServiceBean.disable`.
4. **Given** esta funcionalidade já está implementada em produção (não é código novo) **When** esta story é executada **Then** nenhuma linha de código de produção (`domain`/`usecase`/`api`/`boot`, exceto testes) é criada ou alterada — só verificação (rodar suíte existente) e fechamento de gaps de teste (Task 1).
5. **Given** os ACs 1–3 já têm teste unitário (`CompanyServiceBeanTest`-style, ver Dev Notes) **e** os ACs 2/3 já têm teste de integração full-stack (`DepartmentControllerTest`/`PositionControllerTest`) **When** se audita a suíte completa **Then** só o AC 1 (Departamento inativo) **não tem** cobertura de integração full-stack hoje — só unitária — **and** esta story fecha esse gap especificamente (Task 1), sem duplicar o que já existe para os ACs 2/3.

## Tasks / Subtasks

- [ ] Task 0: Rodar e confirmar a suíte de regressão existente (AC: 1, 2, 3)
  - [ ] Rodar `DepartmentServiceBeanTest`, `PositionServiceBeanTest`, `EmployeePositionQueryServiceBeanTest` (módulo `flow-organization-domain`) e `DepartmentControllerTest`, `PositionControllerTest` (módulo `flow-organization-boot`) — todos devem passar sem alteração, confirmando que as 3 guardas já funcionam (ver mapeamento exato em Dev Notes).
  - [ ] Se algum desses testes falhar hoje, **parar e reportar** — isso indica uma regressão real introduzida por outra story (ex.: Story 1.1/1.2/1.3 mexendo em `Company`, não em `Department`/`Position` — não deveria haver interferência, mas confirmar).
  - [ ] Registrar no Completion Notes List quais testes comprovam cada AC (não é preciso recriá-los).

- [ ] Task 1: Fechar o gap de integração — Cargo com Departamento inativo (AC: 1, 5)
  - [ ] Em `PositionControllerTest.java` (`flow-organization-boot/src/test/java/.../position/`), adicionar:
    - Constante `private static final String DEPARTMENTS_URI = "/api/v1/departments";` (não existe hoje neste arquivo — `DepartmentControllerTest` tem a sua própria, escopo de classe diferente, não importável).
    - Constantes `private static final String CODE_DEPARTMENT_INACTIVE = "SCOS_DEPARTMENT_006";` e `private static final String DETAIL_DEPARTMENT_INACTIVE = "Não é possível associar o cargo a um departamento inativo.";` (texto exato — conferir `scos_message_organization.properties:22` antes de colar, ver Dev Notes).
    - Helper privado `createDepartment(String code, String description)` (POST `/api/v1/departments`, mesmo shape de `departmentBody` em `DepartmentControllerTest`, devolve o id gerado) e `disableDepartment(long id)` (PUT `/api/v1/departments/{id}/disable`) — **duplicar localmente**, não importar de `DepartmentControllerTest` (convenção do projeto: cada `*ControllerTest` é autocontido, sem helpers compartilhados entre classes de teste — ver `ScosOrganizationTestUtil`, que não expõe nada de Department/Position).
  - [ ] Adicionar teste `create_departmentInactive_returns422`: cria um Departamento novo (`createDepartment(...)`), desativa (`disableDepartment(...)`), tenta `POST /v1/positions` com esse `departmentId` → `422`, `code = SCOS_DEPARTMENT_006`, `detail = DETAIL_DEPARTMENT_INACTIVE`.
  - [ ] Adicionar teste `update_departmentInactive_returns422`: cria uma Position válida no Departamento seed ativo (`create(...)`, já existente), cria+desativa um **segundo** Departamento, tenta `PUT /v1/positions/{id}` movendo a Position pro Departamento inativo → `422`, `code = SCOS_DEPARTMENT_006`.
  - [ ] Mesmo padrão dos testes vizinhos já existentes no arquivo (`create_departmentNotFound_returns404`, linha ~225) — só troca o cenário (Departamento existe mas inativo, em vez de inexistente).

- [ ] Task 2: Guarda de escopo — confirmar zero código de produção novo (AC: 4)
  - [ ] **Não** alterar `DepartmentServiceBean.java`, `PositionServiceBean.java`, `EmployeePositionQueryServiceBean.java`, `Department.java`, `Position.java` — as 3 guardas já estão implementadas e corretas.
  - [ ] **Não** alterar `ExceptionCodeError.java` nem `scos_message_organization[_en].properties` — `SCOS_DEPARTMENT_003`/`006`/`SCOS_POSITION_003` já existem com throw site funcionando (diferente da Story 1.3, que precisou reservar código novo).
  - [ ] **Não** alterar `etc/api/organization/*.yml`, nenhum Use Case, Delegate ou permissão nova — os endpoints já existem e já funcionam.
  - [ ] Único artefato tocado nesta story: `PositionControllerTest.java` (Task 1).

## Dev Notes

### Contexto crítico — leia antes de implementar

**Esta é uma story de verificação/regressão, não de implementação nova — as 3 guardas do FR-3 já existem e já funcionam em produção.** Confirmado lendo o código atual (não é auditoria de terceiros, é leitura direta):

- `DepartmentServiceBean.disable()` (`flow-organization-domain/.../department/service/DepartmentServiceBean.java:113-128`) já chama `departmentRepository.existsByIdAndPositionsActive(departmentId)` e lança `SCOS_DEPARTMENT_003` se `true` — **antes** de `department.deactivate()`.
- `PositionServiceBean.disable()` (`.../position/service/PositionServiceBean.java:125-140`) já chama `employeePositionQueryService.existsActiveEmployeeInPosition(positionId)` e lança `SCOS_POSITION_003` se `true`.
- `PositionServiceBean.create()`/`update()` (mesmo arquivo, linhas 52-96) já chamam `findActiveDepartmentOrThrow(departmentId)` (linha 151-157), que lança `SCOS_DEPARTMENT_006` se `!department.isActive()` — **em ambos** os métodos, inclusive quando o `departmentId` não muda no `update` (não há atalho que pule a revalidação).

**Cobertura de teste já existente (não recriar):**

| AC | Guarda | Teste unitário existente | Teste de integração existente |
|---|---|---|---|
| 1 (create) | `SCOS_DEPARTMENT_006` | `PositionServiceBeanTest.createShouldThrowWhenDepartmentIsInactive` (linha ~153) | **NENHUM** — gap fechado na Task 1 |
| 1 (update) | `SCOS_DEPARTMENT_006` | `PositionServiceBeanTest.updateShouldRevalidateDepartmentEvenWhenUnchanged` (linha ~195) | **NENHUM** — gap fechado na Task 1 |
| 2 | `SCOS_DEPARTMENT_003` | `DepartmentServiceBeanTest.disableShouldThrowWhenActivePositionLinked` (linha ~254) | `DepartmentControllerTest.disableDepartment_withActivePositions_returns422` (linha ~572) |
| 3 | `SCOS_POSITION_003` | `PositionServiceBeanTest.disableShouldThrowWhenActiveEmployeeLinked` (linha ~305) + `EmployeePositionQueryServiceBeanTest` (ambos os casos true/false) | `PositionControllerTest.disable_withActiveEmployees_returns422` (linha ~493) |

**O único gap real é o AC 1 no nível de integração full-stack** — os ACs 2 e 3 já têm as duas camadas (unit + integration), o AC 1 só tem unit. Isso é consistente com a nota do `epics.md` ("nenhum código novo é criado — só verificação/regressão") — a "regressão" que falta *escrever* é só essa, o resto já roda hoje. Não adicionar testes duplicados para os ACs 2/3 achando que "mais cobertura é sempre melhor" — foco no gap real.

**Seed disponível:** `etc/database/seed_data.sql` só tem **um** Departamento (`id=1`, `TI`, `ACTIVE`) e uma Position (`id=1`, `ADMIN_SISTEMA`, `ACTIVE`, `department_id=1`) com um Funcionário `ACTIVE` vinculado. Para testar "Departamento inativo" é preciso criar um Departamento novo via API e desativá-lo dentro do próprio teste — **não editar o seed** (mesma decisão já tomada implicitamente pelos testes existentes de `DepartmentControllerTest`, que fazem `createDepartment(...)` + `disableDepartment(...)` ao invés de mexer no `seed_data.sql`).

**`PositionControllerTest` e `DepartmentControllerTest` são classes de teste independentes, sem helpers compartilhados** — `ScosOrganizationTestUtil` (base comum) não expõe nada de Department/Position, cada `*ControllerTest` define seus próprios helpers privados (`create`, `disable`, `body`, etc.). A Task 1 duplica `createDepartment`/`disableDepartment` dentro de `PositionControllerTest` — isso é consistente com o padrão já em uso no arquivo, não uma violação de DRY a corrigir.

### Onde cada peça vai (camadas)

- **Nenhuma mudança em `domain`, `usecase`, `api`, `boot` (produção) ou `etc/api/organization/*.yml`** — story de verificação pura.
- **Único arquivo tocado:** `flow-organization-boot/src/test/java/br/com/sawcunhaos/organization/boot/api/position/PositionControllerTest.java` (2 testes novos + 2 helpers privados + 2 constantes, Task 1).

### Testing Standards

- Os testes novos seguem exatamente o padrão já em uso no arquivo: `@Test` + `@DisplayName`, `mockMvc.perform(...)`, `httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, ...)`, `jsonPath("$.status")`/`jsonPath("$.code")`/`jsonPath("$.detail")`. Reaproveitar `body(code, description, departmentId)` e `create(code, description, departmentId)` já existentes para a parte de Position; só `createDepartment`/`disableDepartment` são novos (Department).
- Testcontainers (Postgres real) já ativo via `ScosOrganizationTestUtil` — nenhuma configuração nova de teste necessária.
- `code` (jDempotent) do POST de Position é keyed em `code` (campo do payload) — usar um `code` único por teste novo (ex.: `"DEP_INAT01"`/`"DEP_INAT02"`), mesmo cuidado já visto nos outros testes do arquivo.

### Project Structure Notes

- Nenhum módulo, pacote ou classe de produção novo.
- Único arquivo modificado: o teste de integração de Position já citado.

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Epic 1 / Story 1.4] — Given/When/Then originais, nota "já implementado e funcional".
- [Source: organization/flow-organization-domain/.../department/service/DepartmentServiceBean.java:113-128] — guarda `SCOS_DEPARTMENT_003` já implementada em `disable()`.
- [Source: organization/flow-organization-domain/.../position/service/PositionServiceBean.java:52-157] — guardas `SCOS_DEPARTMENT_006` (`create`/`update`, via `findActiveDepartmentOrThrow`) e `SCOS_POSITION_003` (`disable`, via `EmployeePositionQueryService`) já implementadas.
- [Source: organization/flow-organization-domain/.../employee/service/EmployeePositionQueryServiceBean.java] — `existsActiveEmployeeInPosition`, já implementado e testado.
- [Source: organization/flow-organization-domain/src/test/java/.../department/service/DepartmentServiceBeanTest.java:254-265] — `disableShouldThrowWhenActivePositionLinked`, cobertura unitária existente do AC 2.
- [Source: organization/flow-organization-domain/src/test/java/.../position/service/PositionServiceBeanTest.java:153-163,195-206,305-312] — cobertura unitária existente dos ACs 1 e 3.
- [Source: organization/flow-organization-domain/src/test/java/.../employee/service/EmployeePositionQueryServiceBeanTest.java] — cobertura unitária de `existsActiveEmployeeInPosition` (true/false).
- [Source: organization/flow-organization-boot/src/test/java/.../department/DepartmentControllerTest.java:559-601,643-660] — cobertura de integração existente do AC 2, e o padrão `departmentBody`/`createDepartment`/`disableDepartment` a replicar (duplicar) na Task 1.
- [Source: organization/flow-organization-boot/src/test/java/.../position/PositionControllerTest.java] — cobertura de integração existente do AC 3 (linha ~491-501); estado atual do arquivo (sem cenário de Departamento inativo) confirmando o gap da Task 1; helpers `body`/`create`/`disable` (linhas ~552-576) a reaproveitar.
- [Source: organization/flow-organization-shared/src/main/resources/scos_message_organization.properties:20-24] — mensagens PT de `SCOS_DEPARTMENT_003`/`006`, `SCOS_POSITION_003` (conferir texto exato ao escrever `DETAIL_DEPARTMENT_INACTIVE`).
- [Source: etc/database/seed_data.sql:170-183] — único Departamento/Position seedados, base do porquê a Task 1 precisa criar um Departamento novo em vez de reaproveitar o seed.

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
