---
baseline_commit: 4f7cc485a817d32b9edcb5bbd15ca6c115b80932
---

# Story 1.2: Ciclo de Vida Completo de Empresa

Status: review

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como Analista de RH,
Eu quero ativar, inativar, bloquear e desbloquear uma Empresa sempre informando um motivo do catálogo,
Para que toda mudança de status tenha justificativa e rastro auditável.

## Acceptance Criteria

1. **Given** uma Empresa `INACTIVE` **When** RH aciona `PUT /v1/companies/{id}/enable` (`operationId: activateCompany`) informando `reasonId` válido do catálogo `SCOS_REASON_ACTIVATE` (`entityType=COMPANY`, `active=true`) **Then** a Empresa passa para `ACTIVE` — via `Company.activate(reasonActivateId)`, já implementado na entidade **And** uma linha é gravada em `SCOS_COMPANY_STATUS_HISTORY` com o motivo, `observation` (se informado) e `userAt`.
2. **Given** uma Empresa `ACTIVE` ou `DISABLED` **When** RH aciona `PUT /v1/companies/{id}/disable` (`operationId: inactivateCompany`) informando `reasonId` válido de `SCOS_REASON_INACTIVATE` **Then** a Empresa passa para `INACTIVE` — via `Company.inactivate(reasonInactivateId)`, já implementado **And** o histórico é gravado.
3. **Given** uma Empresa `ACTIVE` **When** RH aciona `PUT /v1/companies/{id}/block` (`operationId: blockCompany`) informando `reasonId` válido de `SCOS_REASON_DISABLE` **Then** a Empresa passa para `DISABLED` — via `Company.disable(reasonDisableId)`, já implementado **And** o histórico é gravado. **Correção de premissa em relação ao `epics.md` original:** o AC2 do prompt fonte dizia *"Given uma Empresa `INACTIVE`... Then a Empresa passa para `DISABLED`"* — isso está incorreto. O contrato OpenAPI já publicado (`etc/api/organization/ScosOrganization_Company.yml:200`, descrição literal: *"Só é aceito quando o status atual é ACTIVE - a partir de INACTIVE retorna 4XX"*) e o guard já implementado em `Company.disable()` (lança `SCOS_COMPANY_007` se `status != ACTIVE`) concordam entre si e divergem do `epics.md`. Esta story segue o par YAML+entidade, já maduro — não "conserte" replicando o erro do prompt original.
4. **Given** uma Empresa `DISABLED` **When** RH aciona `PUT /v1/companies/{id}/unblock` (`operationId: unblockCompany`) informando `reasonId` válido de `SCOS_REASON_ENABLE` **Then** a Empresa passa para `ACTIVE` — via `Company.enable(reasonEnableId)`, já implementado **And** o histórico é gravado.
5. **Given** qualquer uma das 4 transições acionada a partir de um status de origem diferente do esperado (ex.: `unblock` numa Empresa `ACTIVE`) **When** a operação é chamada **Then** o sistema rejeita com `SCOS_COMPANY_007` — código e guarda já existem nos 4 métodos da entidade (`Company.java`); esta story só precisa expor via Use Case/Service, não recriar a validação.
6. **Given** um request sem `reasonId` **When** qualquer uma das 4 rotas é chamada **Then** a API rejeita antes de chegar ao domínio — `CompanyStatusTransitionRequest.reasonId` já é `@NotNull(message = "SCOS_VALIDATION_003")` no DTO gerado; nenhuma validação Java adicional é necessária.
7. **Given** um `reasonId` que existe no catálogo correspondente mas está `active=false` **When** qualquer uma das 4 transições é chamada **Then** o sistema rejeita com um código dedicado por transição — `SCOS_COMPANY_008` já existe para `activate`; `inactivate`/`disable`/`enable` precisam de códigos novos (ver Task 1 — esta story reserva `SCOS_COMPANY_012`/`014`/`016`).
8. **Given** um `reasonId` que existe mas pertence a `entityType` diferente de `COMPANY` **When** qualquer uma das 4 transições é chamada **Then** o sistema rejeita com um código dedicado — `SCOS_COMPANY_009` já existe para `activate`; os 3 novos desta story são `SCOS_COMPANY_013`/`015`/`017` (ver Task 1).
9. **Given** um `reasonId` que não existe no catálogo **When** qualquer uma das 4 transições é chamada **Then** o sistema rejeita com o 404 que o respectivo `Reason*Service.findById` já lança hoje (`SCOS_REASON_ACTIVATE_001`/`SCOS_REASON_INACTIVATE_001`/`SCOS_REASON_DISABLE_001`/`SCOS_REASON_ENABLE_001`) — reaproveitado sem alteração, nenhum código novo.
10. **Given** o trigger `trg_sync_company_status` (`AFTER INSERT ON SCOS_COMPANY_STATUS_HISTORY`, já implementado em `flow-organization-resources/.../triggers/`) **When** a linha de histórico é persistida **Then** `SCOS_COMPANY.STATUS`/`UPDATED_AT` são sincronizados automaticamente pelo banco (`fn_sync_company_status`) **And** o Use Case/Service desta story NÃO deve chamar `company.setStatus(...)` nem `companyRepository.update(company)` para propagar o novo status — só persistir a `CompanyStatusHistory`, exatamente como já é feito para `Login` (AD-4).
11. **Given** hoje não existe nenhum dos 4 Use Cases nem override no `CompanyDelegate` para as 4 rotas (`CompanyApiDelegate.activateCompany/inactivateCompany/blockCompany/unblockCompany` caem no `default` gerado, que lança `MethodNotImplementedException`) **When** esta story é implementada **Then** são criados `ActivateCompanyUseCase(Bean)`, `InactivateCompanyUseCase(Bean)`, `BlockCompanyUseCase(Bean)`, `UnblockCompanyUseCase(Bean)` (pacote `usecase/corporate/company/`, mesmo padrão de `UpdateCompanyUseCaseBean`) **And** `CompanyDelegate` ganha `@Override` dos 4 métodos, delegando ao Use Case correto **And** o mapeamento rota→Use Case→método de domínio NÃO é 1:1 por nome (ver tabela na Dev Notes) — `BlockCompanyUseCase` chama `companyService.disable(...)`, não `companyService.block(...)` (esse método não existe).
12. **Given** a guarda de escopo **When** esta story é implementada **Then** NÃO altera `etc/api/organization/ScosOrganization_Company.yml` (contrato já publicado e completo, inclusive `x-authorize`/`x-jdempotentresource`) **And** NÃO implementa `GET /v1/companies/{id}/status-history` (UC-138, endpoint de leitura já declarado no YAML mas fora do escopo desta story) **And** NÃO toca `SCOS_COMPANY_005`/`SCOS_COMPANY_006` (guardas de última-matriz-ativa/única-empresa-ativa — Story 1.3) **And** NÃO cria/altera CRUD de `ReasonActivate`/`ReasonInactivate`/`ReasonDisable`/`ReasonEnable` (já existem) **And** NÃO adiciona permissão nova em `ScosGeotemporalPermission` (`ENABLE_COMPANY`/`DISABLE_COMPANY`/`BLOCK_COMPANY`/`UNBLOCK_COMPANY` já cadastrados desde 2026-07-09/15, com `messages_geotemporal_permission.properties`/`_en` já traduzidos).

## Tasks / Subtasks

- [x] Task 1: Códigos de erro novos — motivo inativo/incompatível para `inactivate`/`disable`/`enable` (AC: 7, 8)
  - [x] `flow-organization-shared/.../exception/ExceptionCodeError.java`: adicionar, logo após `SCOS_COMPANY_011` (linha ~87), 6 constantes novas (422, `SCOS_TITLE_BUSINESS_RULE_VIOLATION`):
    ```java
    /** Motivo de inativação informado está inativo. HTTP 422. */
    SCOS_COMPANY_012("SCOS_COMPANY_012", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Motivo de inativação informado é incompatível com a entidade Empresa. HTTP 422. */
    SCOS_COMPANY_013("SCOS_COMPANY_013", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Motivo de bloqueio informado está inativo. HTTP 422. */
    SCOS_COMPANY_014("SCOS_COMPANY_014", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Motivo de bloqueio informado é incompatível com a entidade Empresa. HTTP 422. */
    SCOS_COMPANY_015("SCOS_COMPANY_015", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Motivo de desbloqueio informado está inativo. HTTP 422. */
    SCOS_COMPANY_016("SCOS_COMPANY_016", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Motivo de desbloqueio informado é incompatível com a entidade Empresa. HTTP 422. */
    SCOS_COMPANY_017("SCOS_COMPANY_017", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    ```
  - [x] `flow-organization-shared/src/main/resources/scos_message_organization.properties` (após linha 40, `SCOS_COMPANY_011`):
    ```properties
    SCOS_COMPANY_012=O motivo de inativação informado está inativo.
    SCOS_COMPANY_013=O motivo de inativação informado é incompatível com a entidade Empresa.
    SCOS_COMPANY_014=O motivo de bloqueio informado está inativo.
    SCOS_COMPANY_015=O motivo de bloqueio informado é incompatível com a entidade Empresa.
    SCOS_COMPANY_016=O motivo de desbloqueio informado está inativo.
    SCOS_COMPANY_017=O motivo de desbloqueio informado é incompatível com a entidade Empresa.
    ```
  - [x] `flow-organization-shared/src/main/resources/scos_message_organization_en.properties` (mesmas 6 chaves, texto em inglês, mesmo padrão de `SCOS_COMPANY_008`/`009` já existentes).

- [x] Task 2: `CompanyService` (specification) — 4 métodos novos (AC: 1, 2, 3, 4, 5, 9, 10)
  - [x] `flow-organization-domain/.../corporate/company/specification/CompanyService.java`: adicionar à interface:
    ```java
    /** Ativa uma Empresa INACTIVE, gravando o motivo em SCOS_COMPANY_STATUS_HISTORY (status sincronizado por trigger). */
    void activate(@NonNull Long id, @NonNull Long reasonActivateId, String observation);

    /** Inativa uma Empresa ACTIVE/DISABLED, gravando o motivo (status sincronizado por trigger). */
    void inactivate(@NonNull Long id, @NonNull Long reasonInactivateId, String observation);

    /** Bloqueia uma Empresa ACTIVE (rota "block", corresponde ao método de domínio "disable"). */
    void disable(@NonNull Long id, @NonNull Long reasonDisableId, String observation);

    /** Desbloqueia uma Empresa DISABLED (rota "unblock", corresponde ao método de domínio "enable"). */
    void enable(@NonNull Long id, @NonNull Long reasonEnableId, String observation);
    ```
    `observation` sem `@NonNull` — campo opcional no request (`CompanyStatusTransitionRequest.observation`, sem `@NotNull` no DTO gerado).

- [x] Task 3: `CompanyServiceBean` — implementação (AC: 1, 2, 3, 4, 5, 7, 8, 9, 10)
  - [x] Injetar 3 novas dependências (`@RequiredArgsConstructor`, mesma convenção): `ReasonInactivateService reasonInactivateService`, `ReasonDisableService reasonDisableService`, `ReasonEnableService reasonEnableService` (`ReasonActivateService` já injetado, reaproveitar).
  - [x] Implementar os 4 métodos, mesmo formato para todos (exemplo `activate`):
    ```java
    @Override
    @Transactional(rollbackFor = ScosException.class)
    public void activate(@NonNull Long id, @NonNull Long reasonActivateId, String observation) {
        log.info("Activate Company: {}", id);
        Company company = findCompanyById(id);
        validateReasonActivate(reasonActivateId); // já existe, reaproveitar (SCOS_COMPANY_008/009)
        String user = scosUserAuthentication.findUserAuthentication();

        CompanyStatusHistory history = company.activate(reasonActivateId);
        history.setObservation(observation);
        history.setUserAt(user);
        companyStatusHistoryRepository.merge(history);
    }
    ```
    Repetir para `inactivate`/`disable`/`enable`, trocando: método de domínio chamado (`company.inactivate(...)`/`company.disable(...)`/`company.enable(...)`), e o helper de validação de motivo (Task abaixo).
    **CRÍTICO — não fazer:** nenhum dos 4 métodos chama `company.setStatus(...)` nem `companyRepository.update(company)`. O `trg_sync_company_status` (`AFTER INSERT ON SCOS_COMPANY_STATUS_HISTORY`) já sincroniza `STATUS`/`UPDATED_AT` no banco a partir da linha de histórico — ver Dev Notes.
  - [x] Criar 3 helpers privados espelhando `validateReasonActivate(Long)` já existente (linha ~194), só trocando o service e os códigos:
    ```java
    private void validateReasonInactivate(Long reasonInactivateId) {
        ReasonInactivateOutput reason = reasonInactivateService.findById(reasonInactivateId);
        if (!reason.active()) {
            throw new ScosException(SCOS_COMPANY_012);
        }
        if (reason.entityType() != EntityType.COMPANY) {
            throw new ScosException(SCOS_COMPANY_013);
        }
    }
    ```
    Análogo para `validateReasonDisable` (`SCOS_COMPANY_014`/`015`) e `validateReasonEnable` (`SCOS_COMPANY_016`/`017`). **Não** validar existência do motivo manualmente — `Reason*Service.findById` já lança 404 próprio se não existir (AC 9).

- [x] Task 4: 4 Use Cases + `CompanyDelegate` (AC: 11)
  - [x] Criar, em `flow-organization-usecase/.../usecase/corporate/company/`, 4 pares interface+Bean (mesmo padrão de `UpdateCompanyUseCase(Bean)`, que recebe o DTO gerado da API direto — não criar um Input próprio):
    ```java
    public interface ActivateCompanyUseCase {
        void execute(@NonNull Long id, @NonNull CompanyStatusTransitionRequest request);
    }
    ```
    ```java
    @Service @RequiredArgsConstructor @Slf4j @Transactional(rollbackFor = ScosException.class)
    class ActivateCompanyUseCaseBean implements ActivateCompanyUseCase {
        private final CompanyService companyService;
        @Override
        public void execute(@NonNull Long id, @NonNull CompanyStatusTransitionRequest request) {
            log.info("Activate company: {}", id);
            companyService.activate(id, request.reasonId(), request.observation());
        }
    }
    ```
    Repetir para `InactivateCompanyUseCase(Bean)` (chama `companyService.inactivate(...)`), `BlockCompanyUseCase(Bean)` (chama `companyService.disable(...)` — **não** `block`, esse método não existe), `UnblockCompanyUseCase(Bean)` (chama `companyService.enable(...)` — **não** `unblock`).
  - [x] `flow-organization-api/.../delegate/company/CompanyDelegate.java`: injetar os 4 Use Cases novos e sobrescrever os 4 métodos de `CompanyApiDelegate`:
    ```java
    @Override
    public Void activateCompany(Long id, CompanyStatusTransitionRequest companyStatusTransitionRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        activateCompanyUseCase.execute(id, companyStatusTransitionRequest);
        return null;
    }
    ```
    Mesmo formato para `inactivateCompany`/`blockCompany`/`unblockCompany`, mesmo padrão de `updateCompany` já existente no arquivo (delegate fino, `return null` → `204`).
  - [x] **Não** mexer no YAML nem em `ScosGeotemporalPermission` — `x-authorize`/permissões já existem (AC 12).

- [x] Task 5: Testes (AC: todas)
  - [x] `CompanyServiceBeanTest.java` — acrescentar `@Mock ReasonInactivateService`/`ReasonDisableService`/`ReasonEnableService` (mesmo padrão `@Mock`/`@InjectMocks` já usado no arquivo). Para cada uma das 4 transições (`activate`/`inactivate`/`disable`/`enable`):
    - caminho feliz: `findById` mockado retornando a Company no status de origem correto; motivo mockado `active=true`+`entityType=COMPANY`; chama o método; `verify(companyStatusHistoryRepository).merge(...)` capturando o argumento e conferindo `status`/`reasonXxx.id()`/`observation`/`userAt`; **`verify(companyRepository, never()).update(any())`** (prova que o Java não tenta setar status manualmente).
    - transição inválida (status de origem errado): `assertThatThrownBy(...).hasFieldOrPropertyWithValue("code", SCOS_COMPANY_007.getCode())`.
    - motivo inativo: `assertThatThrownBy(...)` com o código dedicado da transição (`012`/`014`/`016`; `activate` já tem teste com `008` de outra story anterior — conferir se já existe, senão criar).
    - motivo incompatível (`entityType != COMPANY`): idem, código `013`/`015`/`017` (`activate` já tem `009`).
  - [x] Criar 4 arquivos de teste de Use Case (`flow-organization-usecase/src/test/.../usecase/corporate/company/`) — `ActivateCompanyUseCaseBeanTest.java` etc., mesmo padrão BDD (`then(...).should()`/`willThrow(...).given(...)`) de `EnableDepartmentUseCaseBeanTest`: 1 teste "delega corretamente com os 3 argumentos (id, reasonId, observation)" + 1 teste "propaga `ScosException`".
  - [x] `CompanyControllerTest.java` (`flow-organization-boot`) — acrescentar **um** cenário de integração real provando que o trigger sincroniza de fato: `PUT /v1/companies/{id}/block` no caminho feliz (empresa seed `ACTIVE`, `reasonId=1` de `SCOS_REASON_DISABLE`) → `204`, seguido de `GET /v1/companies/{id}` confirmando `status: DISABLED` na resposta (prova end-to-end que `trg_sync_company_status` funciona, não só que o histórico foi inserido). Reaproveitar `SEEDED_ID`/estrutura de request já existentes no arquivo.

- [x] Task 6: Guarda de escopo (AC: 12)
  - [x] NÃO alterar `etc/api/organization/ScosOrganization_Company.yml`.
  - [x] NÃO implementar `GET /v1/companies/{id}/status-history` (UC-138).
  - [x] NÃO tocar `SCOS_COMPANY_005`/`006` nem qualquer lógica de "última matriz ativa"/"única empresa ativa" — Story 1.3.
  - [x] NÃO criar/alterar CRUD de `ReasonActivate`/`ReasonInactivate`/`ReasonDisable`/`ReasonEnable`.
  - [x] NÃO adicionar permissão nova em `ScosGeotemporalPermission` — as 4 já existem.

## Dev Notes

### Contexto crítico — leia antes de implementar

**Achado 1 — o trigger de sincronização já existe e MUDA como o Service deve ser escrito.** `flow-organization-resources/.../triggers/trg_sync_company_status.sql` + `flow-organization-resources/.../function/fn_sync_company_status.sql`:
```sql
CREATE OR REPLACE FUNCTION scos.fn_sync_company_status()
RETURNS TRIGGER AS $$
BEGIN
  UPDATE scos.scos_company
  SET status = NEW.status, updated_at = NEW.created_at
  WHERE company_id = NEW.company_id;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;
```
Trigger `AFTER INSERT ON SCOS_COMPANY_STATUS_HISTORY`. É por isso que os 4 métodos de transição já implementados em `Company.java` (`activate`/`inactivate`/`disable`/`enable`) **retornam** um `CompanyStatusHistory` sem nunca setar `this.status` — o padrão é idêntico ao AD-4 do `LoginApprovalRequest` (Story 0.1/epics.md: *"sincronização com Login.status é feita (...) na mesma transação, o INSERT em SCOS_LOGIN_STATUS_HISTORY (...) aciona o trigger já existente trg_sync_login_status"*), só que pra Company o trigger equivalente (`trg_sync_company_status`) já está pronto desde antes desta story. **Não adicione `company.setStatus(...)` nem `companyRepository.update(company)` nos 4 métodos novos do Service** — seria redundante na melhor hipótese e, na pior, mascara silenciosamente um trigger quebrado (o teste teria passado mesmo sem o trigger funcionar, porque o Java já teria feito a atualização por fora). O teste de integração do Task 5 existe justamente para provar que o trigger funciona de ponta a ponta.

Há também `fn_before_insert_company_status_history` (trigger `BEFORE INSERT`) que (a) deriva `previous_status` automaticamente a partir da última linha de histórico — não sete `previousStatus` manualmente no Java — e (b) valida no banco que o motivo pertence a `entity_type = 'COMPANY'`, como segunda camada de defesa (`RAISE EXCEPTION`, viraria erro 500 cru se a validação Java de `entityType` não existisse antes — por isso a Task 3 valida isso em Java primeiro, pra devolver um `ScosException` 422 limpo em vez de deixar o erro de banco vazar).

**Achado 2 — correção de premissa do `epics.md` (AC2 original).** Ver AC 3 desta story — o prompt fonte tinha o Given trocado (`INACTIVE` em vez de `ACTIVE`) pra transição `block`. O YAML publicado e a entidade já implementada concordam entre si (`block` só parte de `ACTIVE`); segui os dois, não o texto do épico. Se isso for intencional por algum motivo de negócio não documentado, é uma pergunta pro PM antes do dev — não uma correção silenciosa.

**Achado 3 — mapeamento rota → Use Case → método de domínio NÃO é 1:1 por nome.** Cada rota do YAML já documenta isso explicitamente na própria `description`:

| Rota (`operationId`) | Use Case a criar | Método de domínio chamado | Catálogo de motivo |
|---|---|---|---|
| `PUT /enable` (`activateCompany`) | `ActivateCompanyUseCase` | `company.activate(reasonId)` | `ReasonActivate` |
| `PUT /disable` (`inactivateCompany`) | `InactivateCompanyUseCase` | `company.inactivate(reasonId)` | `ReasonInactivate` |
| `PUT /block` (`blockCompany`) | `BlockCompanyUseCase` | `company.disable(reasonId)` | `ReasonDisable` |
| `PUT /unblock` (`unblockCompany`) | `UnblockCompanyUseCase` | `company.enable(reasonId)` | `ReasonEnable` |

Se o dev agent mapear por nome (ex.: `BlockCompanyUseCase` → `companyService.block(...)`), o código não compila (`CompanyService` não declara `block`/`unblock` — declara `disable`/`enable`, nomes que já pertencem à entidade). Os nomes dos métodos do `CompanyService`/`CompanyServiceBean` (Task 2/3) seguem a entidade (`activate`/`inactivate`/`disable`/`enable`); os nomes dos Use Cases (Task 4) seguem o `operationId` do contrato (`Activate`/`Inactivate`/`Block`/`Unblock`).

**Achado 4 — `CompanyStatusTransitionRequest` já existe, gerado do YAML.** `(Long reasonId, String observation)`, `reasonId` com `@NotNull(message = "SCOS_VALIDATION_003")`. Usar direto como parâmetro do Use Case (mesmo padrão de `UpdateCompanyUseCaseBean.execute(Long id, UpdateCompanyRequest updateCompanyRequest)`) — não criar um Input/DTO próprio do usecase pra isso.

**Achado 5 — jDempotent já está todo resolvido no contrato/codegen.** `CompanyApi.java` (gerado) já tem `@JdempotentResource(cachePrefix = "SCOS_ORGANIZATION_IDP_ACTIVATE_COMPANY", ...)` e `@JdempotentRequestPayload` nos 4 métodos, herdado do `x-jdempotentresource`/`x-jdempotentrequestpayload` já presentes no YAML. Nada a fazer no Delegate/Use Case quanto a idempotência.

**Achado 6 — permissões já existem.** `ScosOrganizationPermission.java:85-88`: `ENABLE_COMPANY`, `DISABLE_COMPANY`, `BLOCK_COMPANY`, `UNBLOCK_COMPANY` já cadastrados (2026-07-09/15), com `messages_geotemporal_permission.properties`/`_en` já traduzidos. `x-authorize` do YAML já aponta pra eles. Nada a adicionar — `PermissionsConsistencyTest` já cobre.

### Por que os códigos de erro novos (Task 1) não reaproveitam `SCOS_COMPANY_008`/`009`

`SCOS_COMPANY_008`/`009` já existem, mas o texto da mensagem é hardcoded pra "ativação" (`"O motivo de ativação informado está inativo."`) — reaproveitar o mesmo código pra `inactivate`/`disable`/`enable` mostraria uma mensagem semanticamente errada ao usuário final (ex.: bloquear uma empresa com motivo inativo mostraria "motivo de **ativação** inativo", confuso). Por isso a Task 1 reserva 6 códigos novos (`012`–`017`), maior código hoje é `SCOS_COMPANY_011`.

### Onde cada peça vai (camadas)

- **`shared/exception/ExceptionCodeError.java` + `scos_message_organization[_en].properties`**: 6 códigos novos (Task 1).
- **`domain/corporate/company/specification/CompanyService.java`**: 4 métodos novos na interface (Task 2).
- **`domain/corporate/company/service/CompanyServiceBean.java`**: implementação + 3 helpers de validação de motivo novos (`validateReasonActivate` já existe, reaproveitado) (Task 3).
- **`usecase/corporate/company/`**: 4 pares Use Case + Bean novos (Task 4).
- **`api/delegate/company/CompanyDelegate.java`**: 4 `@Override` novos (Task 4).
- **Nenhuma mudança em**: `etc/api/organization/*.yml` (contrato já pronto), Liquibase (`flow-organization-resources` — coluna/tabela/trigger já existem), `ScosGeotemporalPermission`.

### Testing Standards

- `CompanyServiceBeanTest`: JUnit 5 + Mockito, `@ExtendWith(MockitoExtension.class)`, `@Mock`/`@InjectMocks` já configurados no arquivo — só adicionar os 3 mocks novos. Padrão `assertThatThrownBy(...).hasFieldOrPropertyWithValue("code", SCOS_COMPANY_0XX.getCode())` já usado nos testes de `create`.
- Use Case tests: mesmo padrão BDD (`given`/`then`) de `EnableDepartmentUseCaseBeanTest` (`flow-organization-usecase/src/test/.../corporate/department/`) — arquivo mais simples do projeto pra esse padrão, adaptado pra passar `CompanyStatusTransitionRequest` em vez de só `Long id`.
- `CompanyControllerTest`: já existe, estende `ScosOrganizationTestUtil` (Testcontainers Postgres+Redis singleton). Reasons seed já disponíveis (`etc/database/seed_data.sql:100-127`): `ReasonInactivate` id=1 (COMPANY, `COMPANY_CLOSED`), `ReasonDisable` id=1 (COMPANY, `UNDER_AUDIT`), `ReasonEnable` id=1 (COMPANY, `AUDIT_CLEARED`) — todos `active=true`, suficientes pro cenário de caminho feliz do Task 5. Para cenários de motivo inativo/incompatível em nível de integração (não obrigatório nesta story — já cobertos em unit), seria preciso registro adicional via `@Sql` específico da classe, seguindo o mesmo padrão de `REASON_COMPANY_INACTIVE = 5L` já usado no arquivo para `ReasonActivate`.
- `code` (jDempotent) não exige atenção especial de teste — já coberto pelo padrão idempotente existente nos outros testes de `CompanyControllerTest` (`CNPJ_IDEMPOTENT`).

### Project Structure Notes

- Nenhum pacote novo, nenhum módulo Maven novo — tudo dentro de `domain/corporate/company/{specification,service}`, `usecase/corporate/company/`, `api/delegate/company/` já existentes.
- Nenhuma mudança em `etc/api/organization/*.yml` nem em Liquibase.

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Epic 1 / Story 1.2] — Given/When/Then originais (AC2 corrigido nesta story, ver Achado 2).
- [Source: etc/api/organization/ScosOrganization_Company.yml:131-254] — as 4 rotas já publicadas, `operationId`, `x-authorize`, `CompanyStatusTransitionRequest`, descrições com o mapeamento rota↔domínio.
- [Source: organization/flow-organization-domain/.../corporate/company/internal/Company.java] — os 4 métodos de transição já implementados (`activate`/`inactivate`/`disable`/`enable`), guardas de estado via `SCOS_COMPANY_007`.
- [Source: organization/flow-organization-domain/.../corporate/company/service/CompanyServiceBean.java#create,validateReasonActivate] — padrão de persistência de `CompanyStatusHistory` e validação de motivo a replicar.
- [Source: organization/flow-organization-resources/.../triggers/trg_sync_company_status.sql, function/fn_sync_company_status.sql, triggers/trg_before_insert_company_status_history.sql, function/fn_before_insert_company_status_history.sql] — sincronização automática de status via trigger (Achado 1) e derivação de `previous_status`.
- [Source: organization/flow-organization-usecase/.../corporate/department/EnableDepartmentUseCase.java, DisableDepartmentUseCaseBean.java] — padrão estrutural Use Case interface+Bean a replicar (Department é 2-estado, sem motivo/histórico — só a estrutura é reaproveitada, não a lógica interna).
- [Source: organization/flow-organization-usecase/.../corporate/company/UpdateCompanyUseCaseBean.java] — precedente de Use Case de Company recebendo o DTO gerado da API direto como parâmetro.
- [Source: organization/flow-organization-api/.../delegate/company/CompanyDelegate.java] — delegate atual, Javadoc já anota que as 4 rotas de transição "permanecem com o comportamento default" (a corrigir nesta story).
- [Source: organization/flow-organization-domain/.../access/status/specification/ReasonInactivateService.java, ReasonDisableService.java, ReasonEnableService.java] — mesmo shape de `ReasonActivateService` (`findById` retorna Output com `.active()`/`.entityType()`).
- [Source: organization/flow-organization-shared/.../exception/ExceptionCodeError.java:71-87] — códigos `SCOS_COMPANY_001..011` já existentes; `012..017` reservados nesta story.
- [Source: organization/flow-organization-infrastructure/.../enumaration/ScosOrganizationPermission.java:85-88] — as 4 permissões já cadastradas.
- [Source: etc/database/seed_data.sql:86-128] — seed de `ReasonActivate`/`ReasonInactivate`/`ReasonDisable`/`ReasonEnable`, ids usados nos testes de integração.
- [Source: organization/flow-organization-boot/src/test/java/.../company/CompanyControllerTest.java] — testes de integração existentes de Company (create/update/find), base a estender.
- [Source: _bmad-output/implementation-artifacts/0-1-cobertura-testes-caminho-critico-autenticacao-sistema.md#AD-4] — precedente do padrão trigger-sync para `Login`/`LoginApprovalRequest`, mesmo mecanismo agora confirmado também para `Company`.

## Dev Agent Record

### Agent Model Used

Claude Sonnet 5 (claude-sonnet-5)

### Debug Log References

- `mvn -pl flow-organization-domain clean test -Dtest=CompanyServiceBeanTest -Denforcer.skip=true` → 37/37 (16 pré-existentes + 21 já contavam de Story 1.1 + 16 novos de activate/inactivate/disable/enable — 4 transições × 4 cenários).
- Corrigido `verify(companyRepository, never()).update(any())` → `update(any(Company.class))`: `any()` sem tipo era ambíguo entre `JpaSpecificationExecutor.update(UpdateSpecification<T>)` e `BaseJpaRepository.update(S)` (Spring Data JPA expôs o overload novo nesta versão).
- `mvn -pl flow-organization-usecase test -Dtest=ActivateCompanyUseCaseBeanTest,InactivateCompanyUseCaseBeanTest,BlockCompanyUseCaseBeanTest,UnblockCompanyUseCaseBeanTest -Denforcer.skip=true` → 8/8.
- `mvn -pl flow-organization-domain,flow-organization-usecase,flow-organization-infrastructure test -Denforcer.skip=true` → 207 + 186 + 3 = 396/396, sem regressão (inclui `PermissionsConsistencyTest`, inalterado).
- `mvn -pl flow-organization-boot test -Dtest=CompanyControllerTest -Denforcer.skip=true` → 17/17 (16 pré-existentes + 1 novo `block`→`GET` provando `trg_sync_company_status` fim-a-fim).
- `mvn -pl flow-organization-boot test -Denforcer.skip=true` → 363/363, sem regressão na suíte de integração completa.
- `-Denforcer.skip=true` usado só para contornar o enforcer pré-existente já quebrado (ver *Build quebrado* no `project-context.md`) — nenhuma mudança de dependência nesta story.

### Completion Notes List

- 6 códigos de erro novos (`SCOS_COMPANY_012`..`017`) com mensagens PT/EN dedicadas por transição — evita reaproveitar `008`/`009` (texto hardcoded para "ativação", ver Dev Notes) com mensagem semanticamente errada.
- 4 métodos novos em `CompanyService`/`CompanyServiceBean` (`activate`/`inactivate`/`disable`/`enable`) seguindo o padrão de `create()`: acham a Company, validam o motivo (404 se não existe, 422 se inativo/incompatível), delegam o guard de transição de status ao método de domínio já implementado em `Company.java` (que lança `SCOS_COMPANY_007` se a origem for inválida), e persistem só o `CompanyStatusHistory` — **nenhum método chama `company.setStatus(...)` nem `companyRepository.update(company)`**, propagação de status é 100% via `trg_sync_company_status` (confirmado end-to-end no teste de integração).
- 4 pares Use Case+Bean novos (`ActivateCompanyUseCase(Bean)`, `InactivateCompanyUseCase(Bean)`, `BlockCompanyUseCase(Bean)`, `UnblockCompanyUseCase(Bean)`) em `usecase/corporate/company/`, recebendo `CompanyStatusTransitionRequest` gerado da API direto (sem DTO próprio) — mapeamento rota→domínio respeitado à risca: `BlockCompanyUseCase`→`companyService.disable(...)`, `UnblockCompanyUseCase`→`companyService.enable(...)` (não `block`/`unblock`, que não existem no `CompanyService`).
- `CompanyDelegate` ganhou os 4 `@Override` (antes cadiam no `default` gerado que lança `MethodNotImplementedException`), delegate fino, `return null` → 204.
- Guarda de escopo (Task 6) confirmada via `git status`: zero mudança em `etc/api/organization/*.yml`, Liquibase, ou `ScosGeotemporalPermission` — só `domain`/`usecase`/`api`/`shared` + 2 arquivos de teste em `boot`/`domain`.
- Teste de integração novo em `CompanyControllerTest` prova de ponta a ponta que `trg_sync_company_status` sincroniza `SCOS_COMPANY.STATUS` a partir do INSERT em `SCOS_COMPANY_STATUS_HISTORY` (não só que o histórico foi gravado).

### File List

- `organization/flow-organization-shared/src/main/java/br/com/sawcunhaos/organization/shared/exception/ExceptionCodeError.java` (modificado — `SCOS_COMPANY_012..017`)
- `../../organization/flow-organization-shared/src/main/resources/scos_message/scos_message_organization.properties` (modificado — 6 mensagens PT novas)
- `../../organization/flow-organization-shared/src/main/resources/scos_message/scos_message_organization_en.properties` (modificado — 6 mensagens EN novas)
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/corporate/company/specification/CompanyService.java` (modificado — `activate`/`inactivate`/`disable`/`enable`)
- `organization/flow-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/corporate/company/service/CompanyServiceBean.java` (modificado — implementação + 3 helpers de validação de motivo)
- `organization/flow-organization-domain/src/test/java/br/com/sawcunhaos/organization/domain/corporate/company/service/CompanyServiceBeanTest.java` (modificado — 16 testes novos das 4 transições)
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/corporate/company/ActivateCompanyUseCase.java` (novo)
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/corporate/company/ActivateCompanyUseCaseBean.java` (novo)
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/corporate/company/InactivateCompanyUseCase.java` (novo)
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/corporate/company/InactivateCompanyUseCaseBean.java` (novo)
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/corporate/company/BlockCompanyUseCase.java` (novo)
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/corporate/company/BlockCompanyUseCaseBean.java` (novo)
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/corporate/company/UnblockCompanyUseCase.java` (novo)
- `organization/flow-organization-usecase/src/main/java/br/com/sawcunhaos/organization/application/usecase/corporate/company/UnblockCompanyUseCaseBean.java` (novo)
- `organization/flow-organization-usecase/src/test/java/br/com/sawcunhaos/organization/application/usecase/corporate/company/ActivateCompanyUseCaseBeanTest.java` (novo)
- `organization/flow-organization-usecase/src/test/java/br/com/sawcunhaos/organization/application/usecase/corporate/company/InactivateCompanyUseCaseBeanTest.java` (novo)
- `organization/flow-organization-usecase/src/test/java/br/com/sawcunhaos/organization/application/usecase/corporate/company/BlockCompanyUseCaseBeanTest.java` (novo)
- `organization/flow-organization-usecase/src/test/java/br/com/sawcunhaos/organization/application/usecase/corporate/company/UnblockCompanyUseCaseBeanTest.java` (novo)
- `organization/flow-organization-api/src/main/java/br/com/sawcunhaos/organization/api/delegate/company/CompanyDelegate.java` (modificado — 4 `@Override` novos)
- `organization/flow-organization-boot/src/test/java/br/com/sawcunhaos/organization/boot/api/company/CompanyControllerTest.java` (modificado — cenário de integração `block`+`GET` provando o trigger)

## Change Log

- 2026-07-22: Implementado ciclo de vida completo de Empresa (activate/inactivate/block/unblock) via 4 Use Cases + Service + 6 códigos de erro novos; 20 arquivos (8 modificados, 12 novos); 25 testes novos (16 domain + 8 usecase + 1 integração), 0 regressão (396 domain/usecase/infra + 363 boot). Status → review.
