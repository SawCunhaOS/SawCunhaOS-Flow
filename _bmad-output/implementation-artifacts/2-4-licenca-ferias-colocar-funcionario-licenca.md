---
baseline_commit: ad3702111db59c2d7375471f632be53b9a8f3e40
---

# Story 2.4: Licença/Férias — Colocar Funcionário em Licença

Status: review

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como Analista de RH,
Eu quero colocar um Funcionário em licença médica ou férias informando uma data prevista de retorno,
Para registrar o afastamento sem perder o vínculo.

## Acceptance Criteria

1. **Given** um Funcionário `ACTIVE` **When** RH aciona `PUT /v1/employees/{id}/disable` (`DISABLE_EMPLOYEE`, já implementado pela Story 2.2) informando `reasonId` de código `VACATION` ou `MEDICAL_LEAVE` (2 registros novos no catálogo `SCOS_REASON_INACTIVATE`, `entityType=EMPLOYEE`) e, opcionalmente, `expectedReturnDate` **Then** o Funcionário passa para `INACTIVE` (`204`, mesmo fluxo já existente) **And** a linha gravada em `SCOS_EMPLOYEE_STATUS_HISTORY` para essa transição inclui o `expectedReturnDate` informado (coluna nova, nullable).
2. **Given** `reasonId` de qualquer código **diferente** de `VACATION`/`MEDICAL_LEAVE` **When** RH informa `expectedReturnDate` mesmo assim **Then** o sistema rejeita com `422 SCOS_EMPLOYEE_024` — nenhuma data de retorno é aceita fora desses 2 motivos (mesmo que o campo seja tecnicamente opcional no schema).
3. **Given** `reasonId` de código `VACATION`/`MEDICAL_LEAVE` **When** RH **não** informa `expectedReturnDate` **Then** a transição é aceita normalmente, sem exigir a data — o campo é opcional mesmo para esses 2 motivos (RH pode não saber a data exata ainda).
4. **Given** o gatilho automático de reativação do Login na data prevista de retorno depende de `LoginApprovalRequest` (Epic 3, hoje `backlog`) e o disparo do Kill Switch depende do mecanismo de invalidação de sessão (Epic 6, hoje `backlog`) **When** esta story é implementada **Then** **não** implementa: (a) nenhum cascade de status para o Login vinculado ao Funcionário; (b) nenhuma chamada a qualquer mecanismo de Kill Switch/denylist; (c) nenhum job ou gatilho automático que abra solicitação de reativação na data prevista; (d) nenhum cancelamento automático em dias úteis. Todos ficam para a story final do Epic 3 — mesma resolução que a Story 2.2 já registrou para o cascade de Login em geral, e que o próprio `epics.md` (AC 3 original desta story) já reconhece para o gatilho automático.

## Tasks / Subtasks

- [x] Task 1: Contrato — `expectedReturnDate` opcional em `EmployeeStatusTransitionRequest` (AC: 1, 2, 3)
  - [x] Em `etc/api/organization/ScosOrganization_Employee.yml`, no schema `EmployeeStatusTransitionRequest` (linha 1260, hoje só `reasonId` + `observation`), adicionar:
    ```yaml
    expectedReturnDate:
      type: string
      format: date
      description: >-
        Data prevista de retorno. Só é aceita quando reasonId referencia um motivo de código VACATION ou MEDICAL_LEAVE
        em SCOS_REASON_INACTIVATE (rota disable) - qualquer outro motivo rejeita se este campo vier preenchido.
        Sempre opcional, mesmo para VACATION/MEDICAL_LEAVE.
    ```
    **Escopo deliberadamente amplo do campo:** o schema é compartilhado pelas 4 rotas de transição (`enable`/`disable`/`block`/`unblock`, confirmado em `ScosOrganization_Employee.yml:179,210,242,273`) — criar 4 schemas dedicados só para isolar 1 campo seria over-engineering para o que esta story precisa. `enable`/`block`/`unblock` simplesmente nunca leem este campo (Task 6/8) — se um cliente enviar `expectedReturnDate` para essas rotas, é ignorado, não rejeitado. Se o QA achar isso um problema real, adicionar rejeição explícita depois é um `if` a mais, não uma mudança de schema.
    Rodar `mvn generate-sources` em `usecase`/`api` depois de editar.

- [x] Task 2: Liquibase — coluna nova em `SCOS_EMPLOYEE_STATUS_HISTORY` (AC: 1)
  - [x] Em `organization/flow-organization-resources/.../v1.0.0/tables/scos_employee_status_history.yml` (já existe, 1 `changeSet`), **acrescentar** um segundo `changeSet` no mesmo arquivo (mesmo padrão já usado em `scos_legal_nature.yml`/`scos_cnae.yml`, que acumulam múltiplos `changeSet` no mesmo arquivo ao longo do tempo — não criar arquivo novo, a tabela já existe e não há problema de ordem de FK aqui):
    ```yaml
      - changeSet:
          id: <próximo id livre, formato YYYYMMDD-Samuel.Cunha-NNN, data da implementação>
          author: Samuel.Cunha
          comment: "Adiciona EXPECTED_RETURN_DATE em SCOS_EMPLOYEE_STATUS_HISTORY (Story 2.4 — licença/férias, FR-28)"
          changes:
            - addColumn:
                tableName: SCOS_EMPLOYEE_STATUS_HISTORY
                schemaName: scos
                columns:
                  - column:
                      name: EXPECTED_RETURN_DATE
                      type: DATE
                      constraints:
                        nullable: true
          rollback:
            - dropColumn:
                tableName: SCOS_EMPLOYEE_STATUS_HISTORY
                schemaName: scos
                columnName: EXPECTED_RETURN_DATE
    ```
    **Não** adicionar nenhuma `CHECK` de banco correlacionando `EXPECTED_RETURN_DATE` com o `CODE` do `REASON_INACTIVATE_ID` da mesma linha — nenhuma outra coluna de motivo neste projeto tem esse tipo de correlação física (a validação de "motivo compatível" já é sempre em código de aplicação, nunca em `CHECK`); replicar esse padrão, não inventar um novo. `tables.yml` (índice de includes) **não** precisa de entrada nova — o arquivo já está incluído.

- [x] Task 3: Seed do catálogo — `VACATION`/`MEDICAL_LEAVE` em `SCOS_REASON_INACTIVATE` (AC: 1, 2, 3)
  - [x] Em `etc/database/seed_data.sql` (bloco `SCOS_REASON_INACTIVATE`, linha 100-105), adicionar 2 linhas novas ao `VALUES` já existente:
    ```sql
    ('VACATION',      'Férias',          'EMPLOYEE', true, NOW(), 'seed'),
    ('MEDICAL_LEAVE', 'Licença médica',  'EMPLOYEE', true, NOW(), 'seed')
    ```
    (mesmo `INSERT`/`ON CONFLICT DO NOTHING` já existente — só estender o `VALUES`, não duplicar o `INSERT`).
  - [x] Mesmas 2 linhas em `organization/flow-organization-boot/src/test/resources/postgresql/setsup_database.sql` (bloco equivalente, linha 174-179) — **antes** da linha `ARCHIVED_REASON`/`COMPANY` já existente (linha 184), para preservar o padrão "catálogo real primeiro, registro de teste depois" já usado no arquivo. Os ids gerados não precisam coincidir entre os dois arquivos (nenhum código deste projeto resolve motivo por id, sempre por `CODE` — mesmo padrão de `NEW_HIRE`, Story 2.1).

- [x] Task 4: `EmployeeStatusHistory` (entidade de domínio) — 1 campo novo (AC: 1)
  - [x] Em `organization/flow-organization-domain/.../access/status/internal/EmployeeStatusHistory.java` (já existe), adicionar, ao lado de `observation`:
    ```java
    @Column(name = "EXPECTED_RETURN_DATE")
    private LocalDate expectedReturnDate;
    ```

- [x] Task 5: Código de erro novo — `SCOS_EMPLOYEE_024` (AC: 2)
  - [x] **Antes de codar, confirme o próximo número livre**: `grep SCOS_EMPLOYEE_ ExceptionCodeError.java` — as Stories 2.1 (`002..013`), 2.2 (`014..020`) e 2.3 (`021..023`) reservam a faixa até `023`; ajuste se algum número real já divergir quando esta story for implementada.
  - [x] `ExceptionCodeError.java`:
    ```java
    /** expectedReturnDate informado, mas o motivo de inativação não é VACATION nem MEDICAL_LEAVE. HTTP 422. */
    SCOS_EMPLOYEE_024("SCOS_EMPLOYEE_024", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    ```
  - [x] `scos_message_organization.properties`: `SCOS_EMPLOYEE_024=A data prevista de retorno só é aceita para os motivos Férias ou Licença Médica.`
  - [x] Mesma chave em `scos_message_organization_en.properties`, em inglês.

- [x] Task 6: `EmployeeService.inactivate` — 1 parâmetro novo na assinatura já existente (AC: 1, 2, 3)
  - [x] Em `EmployeeService.java` (Story 2.2 declara `void inactivate(@NonNull Long id, @NonNull Long reasonInactivateId, String observation)`), **alterar a assinatura** para:
    ```java
    /** Inativa um Funcionário ACTIVE/DISABLED. expectedReturnDate só é aceito quando reasonInactivateId é VACATION/MEDICAL_LEAVE (422 SCOS_EMPLOYEE_024 caso contrário). */
    void inactivate(@NonNull Long id, @NonNull Long reasonInactivateId, String observation, LocalDate expectedReturnDate);
    ```
    **Não** criar um método sobrecarregado nem uma segunda assinatura — é a mesma transição de domínio (rota `disable`), só ganhando 1 parâmetro. Atualizar o único ponto de chamada existente (Task 8).

- [x] Task 7: `EmployeeServiceBean.inactivate` — implementar a validação + persistência do novo campo (AC: 1, 2, 3)
  - [x] No mesmo método já implementado pela Story 2.2, adicionar a checagem antes de persistir o histórico:
    ```java
    private static final String VACATION_REASON_CODE = "VACATION";
    private static final String MEDICAL_LEAVE_REASON_CODE = "MEDICAL_LEAVE";

    @Override
    @Transactional(rollbackFor = ScosException.class)
    public void inactivate(@NonNull Long id, @NonNull Long reasonInactivateId, String observation, LocalDate expectedReturnDate) {
        log.info("Inactivate Employee: {}", id);
        Employee employee = findEmployeeById(id);
        ReasonInactivateOutput reason = validateReasonInactivateAndGet(reasonInactivateId); // ver nota abaixo
        assertExpectedReturnDateAllowed(reason, expectedReturnDate);
        String user = scosUserAuthentication.findUserAuthentication();

        EmployeeStatusHistory history = employee.inactivate(reasonInactivateId);
        history.setObservation(observation);
        history.setExpectedReturnDate(expectedReturnDate);
        history.setUserAt(user);
        employeeStatusHistoryRepository.merge(history);
    }

    private void assertExpectedReturnDateAllowed(ReasonInactivateOutput reason, LocalDate expectedReturnDate) {
        if (expectedReturnDate == null) {
            return;
        }
        boolean isLeaveReason = VACATION_REASON_CODE.equals(reason.code()) || MEDICAL_LEAVE_REASON_CODE.equals(reason.code());
        if (!isLeaveReason) {
            throw new ScosException(SCOS_EMPLOYEE_024);
        }
    }
    ```
  - [x] **`validateReasonInactivate` (Story 2.2) precisa passar a retornar o `ReasonInactivateOutput`, não só validar e descartar** — troque a assinatura de `private void validateReasonInactivate(Long reasonInactivateId)` para `private ReasonInactivateOutput validateReasonInactivateAndGet(Long reasonInactivateId)`, mantendo exatamente as mesmas 2 checagens (`SCOS_EMPLOYEE_015`/`016`) e devolvendo o objeto já buscado no final — é a mesma chamada a `reasonInactivateService.findById(...)` que já existe, só não descartando o retorno. **Não** duplicar a busca com uma segunda chamada a `findById`.

- [x] Task 8: `InactivateEmployeeUseCaseBean` — repassar o campo novo (AC: 1, 2, 3)
  - [x] Em `usecase/corporate/employee/InactivateEmployeeUseCaseBean.java` (Story 2.2, já existe), atualizar a única linha de chamada:
    ```java
    employeeService.inactivate(id, request.reasonId(), request.observation(), request.expectedReturnDate());
    ```
  - [x] **Não** tocar em `ActivateEmployeeUseCaseBean`/`BlockEmployeeUseCaseBean`/`UnblockEmployeeUseCaseBean` nem em `EmployeeDelegate` — nenhum dos 4 métodos de `EmployeeService` que eles chamam (`activate`/`disable`/`enable`) ganha o parâmetro novo, só `inactivate`. O `Delegate.inactivateEmployee` já existente (Story 2.2) não muda — continua só chamando `inactivateEmployeeUseCase.execute(id, employeeStatusTransitionRequest)`, o `DTO` inteiro já carrega o campo novo automaticamente após o `generate-sources` da Task 1.

- [x] Task 9: Testes (AC: 1, 2, 3, 4)
  - [x] `EmployeeServiceBeanTest.java` (já existe): estender os testes de `inactivate` (Story 2.2) com os 4 novos cenários — `reasonId=VACATION` + `expectedReturnDate` informado → sucesso, `EmployeeStatusHistory` capturado via `ArgumentCaptor` com `expectedReturnDate` correto; `reasonId=MEDICAL_LEAVE` + `expectedReturnDate` informado → idem; `reasonId=VACATION`/`MEDICAL_LEAVE` sem `expectedReturnDate` → sucesso, campo `null` no histórico; `reasonId` de outro código (ex.: `RESIGNATION`) + `expectedReturnDate` informado → `SCOS_EMPLOYEE_024`, **nenhuma** escrita em `employeeStatusHistoryRepository` (verificar via `then(...).should(never())`).
  - [x] `InactivateEmployeeUseCaseBeanTest.java` (já existe, Story 2.2): 1 teste novo confirmando que `request.expectedReturnDate()` é repassado ao `employeeService.inactivate(...)` via `ArgumentCaptor`.
  - [x] `EmployeeControllerTest.java` (já existe): no bloco de `disable` (Story 2.2), adicionar: `204` com `reasonId=VACATION` (seed nova, Task 3) + `expectedReturnDate` → confirmar via query direta que a linha de `SCOS_EMPLOYEE_STATUS_HISTORY` tem `EXPECTED_RETURN_DATE` preenchido; mesmo teste para `MEDICAL_LEAVE`; `204` com `VACATION` **sem** `expectedReturnDate` → `EXPECTED_RETURN_DATE` nulo na linha; `422 SCOS_EMPLOYEE_024` ao informar `expectedReturnDate` com `reasonId` de código diferente (ex.: o `RESIGNATION` já seedado).

- [x] Task 10: Guarda de escopo (AC: 4)
  - [x] **Não** implementar nenhum cascade de status para o Login vinculado ao Funcionário — mesmo gap já documentado pela Story 2.2 (Task 8 daquela story): a infraestrutura (`LoginService` com transição de status, Saga/Outbox Keycloak) não existe, é escopo do Epic 3 (`backlog`).
  - [x] **Não** implementar nenhuma chamada a Kill Switch/Redis/denylist — mecanismo inteiro (AD-1) é escopo do Epic 6 (`backlog`), nenhuma linha de código de Kill Switch existe hoje no repositório.
  - [x] **Não** criar nenhum job agendado, listener ou gatilho que leia `EXPECTED_RETURN_DATE` e abra automaticamente uma solicitação de reativação — depende de `LoginApprovalRequest` (Epic 3, `backlog`), que ainda não existe como entidade nem como tabela populável (o schema já existe na spine, mas nenhum Java consome). Fica para a story final do Epic 3, conforme o próprio `epics.md` já registra.
  - [x] **Não** implementar nenhum cancelamento automático em dias úteis — mesma dependência de `LoginApprovalRequest`/Epic 3.
  - [x] **Não** adicionar rejeição explícita de `expectedReturnDate` nas rotas `enable`/`block`/`unblock` — decisão deliberada de escopo mínimo (Task 1); o campo é simplesmente ignorado nessas 3 rotas.

## Dev Notes

### Contexto crítico — leia antes de implementar

**Depende da Story 2.2 já implementada** (`EmployeeService.inactivate`/`EmployeeServiceBean.inactivate`/`InactivateEmployeeUseCaseBean` precisam existir com a assinatura de 3 parâmetros antes de ganharem o 4º aqui). Se 2.2 ainda não foi implementada, implemente-a primeiro.

**Esta story é uma fração pequena e deliberada de FR-28 (PRD, `prd.md` linha 308-317) — não a feature completa.** FR-28 descreve o ciclo inteiro: motivo dedicado + data de retorno (esta story) **e** cascade de Login + Kill Switch + gatilho automático de reativação + cancelamento em dias úteis (não esta story). O texto de FR-28 diz "Login vinculado ao Funcionário é desativado junto, disparando o Kill Switch normalmente — mesma regra de qualquer `disable`, sem exceção por ser licença": isso é uma descrição do **comportamento-alvo do sistema completo**, não uma instrução para esta story inventar Login/Kill Switch — ambos são mecanismos que hoje **não existem no código** (confirmado: nenhum `LoginService` com transição de status, nenhuma classe de Kill Switch/Redis denylist no repositório; `epic-3`/`epic-6` estão `backlog` em `sprint-status.yaml`). Implementá-los aqui, fora de ordem e sem a base que Epic 3/6 ainda vão construir, seria inventar infraestrutura que a própria arquitetura (`ARCHITECTURE-SPINE.md`, AD-1/AD-4) já reserva para depois. Esta story entrega só a parte que **é** de Epic 2 (Funcionário): o motivo dedicado e a data de retorno guardada no histórico. O próprio `epics.md` (AC 3 original) já reconhece isso para o gatilho automático — esta resolução só estende a mesma lógica para o cascade de Login e o Kill Switch, que o texto do AC 1 do épico menciona mas nenhum código-fonte real sustenta ainda.

**O campo `expectedReturnDate` não existia em nenhum lugar do contrato antes desta story** — confirmado por busca no YAML inteiro (nenhum `expectedReturnDate`/`dataRetorno`/campo equivalente em `ScosOrganization_Employee.yml`). Diferente das Stories 2.1/2.2/2.3 (que só fecham lacunas de Java sobre contrato já publicado), esta story **muda o contrato** (Task 1) — segue a ordem obrigatória do projeto: YAML primeiro, depois `generate-sources`, depois o código.

**Por que a coluna nova vai em `SCOS_EMPLOYEE_STATUS_HISTORY`, não em `SCOS_EMPLOYEE`:** a data prevista de retorno é um atributo do **evento de afastamento** (uma linha específica de histórico, com seu motivo e sua data), não um atributo persistente do Funcionário — uma licença anterior já encerrada não deveria continuar carregando uma data de retorno "pendurada" no registro principal do Funcionário. Mesmo raciocínio que já rege as 4 colunas `REASON_*_ID` daquela tabela (nullable, uma populada por linha, dependendo da transição).

**`VACATION`/`MEDICAL_LEAVE` são identificados por `CODE`, nunca por id** — mesmo padrão já estabelecido por `NEW_HIRE` (Story 2.1, `ReasonPositionChange`). Os ids gerados variam entre `seed_data.sql` (produção) e `setsup_database.sql` (teste) porque o segundo tem registros extras só de teste intercalados (Task 3) — nenhum código deste projeto deveria depender de um id fixo de catálogo.

**Por que `validateReasonInactivate` (Story 2.2) precisa mudar de `void` para retornar `ReasonInactivateOutput`:** a Story 2.2 só usava o motivo para validar `active`/`entityType` e descartava o resultado. Esta story precisa do `.code()` do mesmo objeto já buscado — buscar de novo seria uma segunda consulta redundante ao mesmo repositório na mesma chamada. Mudar o retorno de `void` para o DTO já obtido é a menor alteração possível no método existente (mesma lógica interna, só devolve o que já tinha em mãos).

### Onde cada peça vai (camadas)

- **`etc/api/organization/ScosOrganization_Employee.yml`**: 1 campo novo em `EmployeeStatusTransitionRequest` (Task 1) — única mudança de contrato desta story.
- **`organization/flow-organization-resources/.../v1.0.0/tables/scos_employee_status_history.yml`**: 1 `changeSet` novo (Task 2).
- **`etc/database/seed_data.sql`** + **`organization/flow-organization-boot/src/test/resources/postgresql/setsup_database.sql`**: 2 linhas novas cada, catálogo `SCOS_REASON_INACTIVATE` (Task 3).
- **`domain/access/status/internal/EmployeeStatusHistory.java`** (já existe — 1 campo novo).
- **`shared/exception/ExceptionCodeError.java`** + **`scos_message_organization[_en].properties`**: 1 código novo (`SCOS_EMPLOYEE_024`).
- **`domain/corporate/employee/specification/EmployeeService.java`** + **`.../service/EmployeeServiceBean.java`** (já existem — assinatura de `inactivate` alterada, `validateReasonInactivate` alterado).
- **`usecase/corporate/employee/InactivateEmployeeUseCaseBean.java`** (já existe — 1 linha alterada).
- Nenhuma mudança em `EmployeeDelegate`, `ActivateEmployeeUseCase(Bean)`, `BlockEmployeeUseCase(Bean)`, `UnblockEmployeeUseCase(Bean)`, permissão, ou qualquer artefato de Login/Kill Switch.

### Testing Standards

- Unitário de domínio: mesmo padrão de `EmployeeServiceBeanTest` já usado pelas Stories 2.1/2.2/2.3 — `Clock.fixed(...)` não é necessário aqui (nenhuma regra de data relativa a "hoje", `expectedReturnDate` é só persistido, não comparado a `LocalDate.now()`).
- Unitário de Use Case: mesmo padrão BDD de `InactivateEmployeeUseCaseBeanTest`.
- Integração: adicionar aos blocos já existentes de `EmployeeControllerTest.java`. Seed novo do catálogo (Task 3) precisa estar disponível antes de qualquer teste desta story rodar.

### Project Structure Notes

- Nenhum pacote Maven novo — todas as mudanças caem em arquivos já existentes (criados pelas Stories 2.1/2.2) ou em arquivos de configuração/seed já existentes.
- Única mudança de schema desta story inteira: 1 coluna nullable em 1 tabela já existente (Task 2) — sem tabela nova, sem índice novo, sem trigger novo.

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Epic 2 / Story 2.4] — Given/When/Then originais; AC 3 original já reconhece o gatilho automático como fora de escopo.
- [Source: _bmad-output/planning-artifacts/prds/prd-SawCunhaOS-Organization-2026-07-18/prd.md#FR-28] — especificação completa da feature-alvo (motivo dedicado, data de retorno, cascade de Login, Kill Switch, gatilho automático, cancelamento em dias úteis); só a primeira parte é escopo desta story, resolução em Dev Notes.
- [Source: _bmad-output/planning-artifacts/prds/prd-SawCunhaOS-Organization-2026-07-18/prd.md#Glossário, "Data prevista de retorno"] — campo sempre opcional, mesmo para Férias/Licença Médica.
- [Source: _bmad-output/planning-artifacts/architecture/architecture-SawCunhaOS-Organization-2026-07-18/ARCHITECTURE-SPINE.md#Capability Map, AD-1, AD-4] — Kill Switch (AD-1) e aprovação de Login (AD-4/`LoginApprovalRequest`) são mecanismos de Epic 3/6, ainda não implementados em Java — confirma o corte de escopo desta story.
- [Source: etc/api/organization/ScosOrganization_Employee.yml:162-286,1260-1273] — as 4 rotas de transição compartilhando `EmployeeStatusTransitionRequest`; schema atual sem `expectedReturnDate` (gap fechado pela Task 1).
- [Source: organization/flow-organization-resources/.../v1.0.0/tables/scos_employee_status_history.yml] — tabela já existente, 1 `changeSet` (Task 2 acrescenta o segundo).
- [Source: organization/flow-organization-resources/.../v1.0.0/tables/scos_department_manager.yml] — precedente exato de `addColumn` + `rollback: dropColumn` em tabela já existente (mesmo padrão replicado na Task 2).
- [Source: etc/database/seed_data.sql:100-106] + [organization/flow-organization-boot/src/test/resources/postgresql/setsup_database.sql:174-186] — seeds atuais de `SCOS_REASON_INACTIVATE`, estendidos pela Task 3.
- [Source: organization/flow-organization-domain/.../access/status/internal/ReasonInactivate.java, .../status/dto/ReasonInactivateOutput.java, .../status/specification/ReasonInactivateService.java] — `CODE` já exposto por `ReasonInactivateOutput.code()`, reaproveitado sem mudança.
- [Source: _bmad-output/implementation-artifacts/2-1-admissao-funcionario-copia-jornada-trabalho.md] — origem do padrão "resolver motivo por `CODE`, nunca por id" (`NEW_HIRE`), replicado aqui para `VACATION`/`MEDICAL_LEAVE`.
- [Source: _bmad-output/implementation-artifacts/2-2-ciclo-vida-funcionario.md] — origem de `EmployeeService.inactivate`/`EmployeeServiceBean.inactivate`/`validateReasonInactivate`/`InactivateEmployeeUseCaseBean`, todos alterados por esta story; origem do precedente de resolução "cascade de Login é gap conhecido, fora do código atual" (Task 8 daquela story), estendido aqui para Kill Switch.
- [Source: _bmad-output/implementation-artifacts/2-3-recontratacao-funcionario.md] — reserva a faixa de erro `SCOS_EMPLOYEE_021..023`; esta story usa `024`.

## Dev Agent Record

### Agent Model Used

Claude Sonnet 5 (claude-sonnet-5), via Claude Code, workflow `bmad-dev-story`.

### Debug Log References

- Ambiente sem acesso ao Docker (usuário fora do grupo `docker`, `sudo` sem TTY disponível) — os testes de integração do módulo `boot` (`EmployeeControllerTest`, que sobem Postgres/Redis/WireMock via `ComposeContainer`) foram **escritos mas não executados**. Unitário (`domain`) e Use Case (`usecase`) foram executados e passam. Rodar `mvn -pl organization/flow-organization-boot test -Denforcer.skip=true` localmente (com Docker acessível) antes de aprovar a review.
- `maven-surefire-plugin` neste ambiente resolveu para a versão `2.17` (não suporta JUnit 5, executa silenciosamente 0 testes com `mvn test`/`mvn -Dtest=...`) — pré-existente, fora do escopo desta story. Contorno usado para validar: `mvn org.apache.maven.plugins:maven-surefire-plugin:3.5.4:test -Denforcer.skip=true ...` (versão já presente no cache local `~/.m2`).
- `mvn clean install`/`mvn test` continuam exigindo `-Denforcer.skip=true` (débito pré-existente, documentado em `project-context.md`).
- Ajuste de dívida cascata: a Task 3 inseriu `VACATION`/`MEDICAL_LEAVE` em `SCOS_REASON_INACTIVATE` **antes** dos registros de teste `ARCHIVED_REASON` em `setsup_database.sql`, deslocando os ids gerados desses registros de 4→6 e 5→7. Corrigidos os `IDs` hardcoded que dependiam disso: `ReasonInactivateControllerTest.SEEDED_INACTIVE_ID` (4L→6L) e `EmployeeControllerTest.REASON_INACTIVATE_EMPLOYEE_INACTIVE` (5L→7L). Constantes homônimas em `ReasonEnableControllerTest`/`ReasonDisableControllerTest`/`ReasonActivateControllerTest`/`ContactTypeControllerTest`/`AddressTypeControllerTest` referenciam outras tabelas de catálogo e não foram afetadas (confirmado por inspeção).

### Completion Notes List

- Todas as 4 ACs implementadas: `expectedReturnDate` opcional no contrato (Task 1), coluna nova nullable em `SCOS_EMPLOYEE_STATUS_HISTORY` (Task 2), catálogo `VACATION`/`MEDICAL_LEAVE` semeado em produção e teste (Task 3), entidade de domínio atualizada (Task 4), erro `SCOS_EMPLOYEE_024` com mensagens PT-BR/EN (Task 5), validação de compatibilidade motivo×data em `EmployeeServiceBean.inactivate` (Task 6/7), repasse no Use Case (Task 8).
- Nenhum código de Login/Kill Switch/gatilho automático foi tocado (Task 10, guarda de escopo) — confirmado por não haver qualquer alteração fora dos arquivos listados abaixo.
- `validateReasonInactivate` (Story 2.2) mudou de `void` para retornar `ReasonInactivateOutput` (renomeado `validateReasonInactivateAndGet`), sem duplicar a busca ao repositório — mesma chamada reaproveitada.
- Testes unitários (`domain`): suíte completa (281 testes) passa, incluindo os 4 cenários novos de `inactivate` (VACATION com data, MEDICAL_LEAVE com data, VACATION/MEDICAL_LEAVE sem data, motivo incompatível com data → 422).
- Testes de Use Case (`usecase`): suíte completa (211 testes) passa, incluindo repasse de `expectedReturnDate`.
- Testes de integração (`boot`, `EmployeeControllerTest`): 4 cenários novos escritos no bloco `disable` seguindo o padrão existente — **não executados nesta sessão** (ver Debug Log). Recomendo rodar antes de mover para `done`.

### File List

- `etc/api/organization/ScosOrganization_Employee.yml`
- `etc/database/seed_data.sql`
- `organization/flow-organization-resources/src/main/resources/db/changelog/organization/v1.0.0/tables/scos_employee_status_history.yml`
- `organization/flow-organization-boot/src/test/resources/postgresql/setsup_database.sql`
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/access/status/internal/EmployeeStatusHistory.java`
- `organization/flow-organization-shared/src/main/java/br/com/sawcunhaos/organization/shared/exception/ExceptionCodeError.java`
- `../../organization/flow-organization-shared/src/main/resources/scos_message/scos_message_organization.properties`
- `../../organization/flow-organization-shared/src/main/resources/scos_message/scos_message_organization_en.properties`
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/corporate/employee/specification/EmployeeService.java`
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/corporate/employee/service/EmployeeServiceBean.java`
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/corporate/employee/InactivateEmployeeUseCaseBean.java`
- `organization/flow-organization-domain/src/test/java/br/com/sawcunhaos/organization/domain/corporate/employee/service/EmployeeServiceBeanTest.java`
- `organization/flow-organization-usecase/src/test/java/br/com/sawcunhaos/organization/application/usecase/corporate/employee/InactivateEmployeeUseCaseBeanTest.java`
- `organization/flow-organization-boot/src/test/java/br/com/sawcunhaos/organization/boot/api/employee/EmployeeControllerTest.java`
- `organization/flow-organization-boot/src/test/java/br/com/sawcunhaos/organization/boot/api/reason/ReasonInactivateControllerTest.java`

## Change Log

| Data | Descrição |
|------|-----------|
| 2026-08-15 | Story implementada: `expectedReturnDate` opcional em `EmployeeStatusTransitionRequest` (rota `disable`), coluna `EXPECTED_RETURN_DATE` em `SCOS_EMPLOYEE_STATUS_HISTORY`, catálogo `VACATION`/`MEDICAL_LEAVE` em `SCOS_REASON_INACTIVATE`, validação motivo×data (`SCOS_EMPLOYEE_024`). 5 testes unitários de domínio novos, 1 de Use Case, 4 de integração (não executados — ambiente sem Docker). Status → review. |
