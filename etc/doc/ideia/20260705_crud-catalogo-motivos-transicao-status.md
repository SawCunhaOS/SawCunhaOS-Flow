# Implementação do CRUD de Catálogo — Motivos de Transição de Status (Reason)

**Data**: 2026-07-05
**Status**: 🔄 Em Análise
**Tipo**: 🆕 Nova Feature

---

## ⚠️ Princípio SRP — Uma Funcionalidade por Ideia

- **Nome da funcionalidade**: `crud-catalogo-motivos-transicao-status`
- **Resumo em uma frase**: Implementar a camada de código (domain/usecase/api/permissão) dos 24 endpoints dos 4 catálogos de motivo (`ReasonActivate`/`Inactivate`/`Disable`/`Enable`) cujo contrato OpenAPI já existe.

**Checklist SRP**:
- [x] Esta ideia cobre exatamente uma funcionalidade
- [x] Não mistura features independentes no mesmo arquivo
- [x] O nome é específico (não genérico como "melhorar módulo X")

> Os 4 `Reason*` são tratados numa única ideia (não 4) porque: (1) já foram tratados como um único grupo — capability `reason-status-catalog` — no change `historico-status-motivos-organization`; (2) a doc 05 declara explicitamente "Todos os módulos Reason têm a mesma estrutura"; (3) já vivem no mesmo pacote de domínio (`access/status/internal/`); (4) não existe cenário de negócio onde um seria implementado sem os outros 3.

---

## 1️⃣ Visão

### Problema

O contrato OpenAPI dos 4 catálogos de motivo já foi fechado no change `historico-status-motivos-organization` (completo, novo arquivo `etc/api/organization/ScosOrganization_Reason.yml`), como parte da capability `reason-status-catalog`. Esse change também tratou `status-transition-contract` (rotas `enable`/`disable`/`block`/`unblock` de Company/Employee/Login com `reasonId`) e `status-history-query` (histórico paginado) — ambas fora de escopo aqui.

Hoje existe apenas a entidade JPA e o repositório vazio para as 4 entidades:
```
scos-organization-domain/.../access/status/internal/ReasonActivate.java   (+ Repository sem métodos custom)
scos-organization-domain/.../access/status/internal/ReasonInactivate.java (+ Repository sem métodos custom)
scos-organization-domain/.../access/status/internal/ReasonDisable.java    (+ Repository sem métodos custom)
scos-organization-domain/.../access/status/internal/ReasonEnable.java     (+ Repository sem métodos custom)
```
Não existe `dto/`, `service/`, `specification/`, nenhum Use Case, nenhum `Delegate`, e `ScosOrganizationPermission` não tem nenhuma entrada `*_REASON_*`. Os 24 endpoints do contrato (UC-113 a UC-136, doc `05-catalogo-motivos.md`) não funcionam.

**Achado de divergência doc vs. contrato** (confirmado com o usuário: contrato prevalece): a doc 05 diz `entityType` = `COMPANY`/`EMPLOYEE` (2 valores). O contrato real (`ScosOrganization_Reason.yml`, schema `ReasonEntityType`) e o enum de domínio compartilhado `EntityType.java` têm 3 valores: `COMPANY`/`EMPLOYEE`/`LOGIN`. Esta ideia segue o contrato (3 valores) — a doc 05 está desatualizada nesse ponto e deveria ser corrigida à parte.

### Objetivo

Implementar os 24 endpoints (6 por catálogo × 4 catálogos) seguindo o mesmo padrão de `crud-catalogo-tipo-endereco-contato` / `crud-cargo-position`.

### Fora de Escopo

- `status-transition-contract` — `reasonId` nos `enable`/`disable`/`block`/`unblock` de Company/Employee/Login, incluindo `BLOCK_COMPANY`/`UNBLOCK_COMPANY`/etc. em `ScosOrganizationPermission`
- `status-history-query` — `GET .../status-history` paginado de Company/Employee/Login
- Catálogo `AddressType`/`ContactType` — ideia separada `crud-catalogo-tipo-endereco-contato`
- Correção da doc 05 (`entityType` 2→3 valores) — ajuste de documentação, não de código
- Testes de integração com Testcontainers (não necessário nesta etapa, por instrução explícita)

---

## 2️⃣ Requisitos

### Funcionais
- [ ] **RF-01**: `GET /v1/reason-activate` — lista paginada, filtro opcional `entityType` (`COMPANY`/`EMPLOYEE`/`LOGIN`) (UC-113)
- [ ] **RF-02**: `POST /v1/reason-activate` — cria com `code` (único em `SCOS_REASON_ACTIVATE`, ≤30), `description` (≤255), `entityType` (UC-114)
- [ ] **RF-03**: `GET /v1/reason-activate/{id}` — 404 se não existir (UC-115)
- [ ] **RF-04**: `PUT /v1/reason-activate/{id}` — `code` único excluindo `{id}` (UC-116)
- [ ] **RF-05**: `PUT /v1/reason-activate/{id}/enable` / `.../disable` (UC-117, UC-118)
- [ ] **RF-06**: RF-01 a RF-05 espelhados para `ReasonInactivate` em `/v1/reason-inactivate` (UC-119 a UC-124)
- [ ] **RF-07**: idem para `ReasonDisable` em `/v1/reason-disable` (UC-125 a UC-130)
- [ ] **RF-08**: idem para `ReasonEnable` em `/v1/reason-enable` (UC-131 a UC-136)
- [ ] **RF-09**: Novas entradas em `ScosOrganizationPermission`: `GET/CREATE/UPDATE/ENABLE/DISABLE_REASON_ACTIVATE`, mesmo conjunto para `REASON_INACTIVATE`, `REASON_DISABLE`, `REASON_ENABLE` (20 entradas, nomes já fixados em `x-authorize` do contrato)

### Não-Funcionais
- [ ] **RNF-01**: Mensagens de erro via `ScosException`/bundle i18n, padrão já usado por Department/Position
- [ ] **RNF-02**: Sem `DELETE` — inativação lógica apenas

---

## 3️⃣ Arquitetura

### Componentes Afetados
```
scos-organization-domain
├── access/status/dto/Reason{Activate,Inactivate,Disable,Enable}Input.java, Output.java: adição (8 classes)
├── access/status/internal/Reason{Activate,Inactivate,Disable,Enable}Repository.java: modificação (existsByCode/existsByCodeAndNotId)
├── access/status/service/Reason{Activate,Inactivate,Disable,Enable}Mapper.java, ServiceBean.java: adição (8 classes)
└── access/status/specification/Reason{Activate,Inactivate,Disable,Enable}Service.java: adição (4 interfaces)

scos-organization-usecase
└── application/usecase/access/status/
    ├── reasonactivate/ (Create/Update/Find/FindAll/Enable/DisableReasonActivateUseCase + Bean, ApiMapper): adição
    ├── reasoninactivate/ (idem): adição
    ├── reasondisable/ (idem): adição
    └── reasonenable/ (idem): adição

scos-organization-api
└── delegate/reason/
    ├── ReasonActivateDelegate.java implements ReasonActivateApiDelegate
    ├── ReasonInactivateDelegate.java implements ReasonInactivateApiDelegate
    ├── ReasonDisableDelegate.java implements ReasonDisableApiDelegate
    └── ReasonEnableDelegate.java implements ReasonEnableApiDelegate

scos-organization-infrastructure
└── enumaration/ScosOrganizationPermission.java: modificação (+20 entradas)
```

### Fluxo Principal
```
Delegate → UseCase → Reason{X}Service (specification)
   → ServiceBean valida unicidade de code (existsByCode/existsByCodeAndNotId)
   → Mapper (dto ↔ entity) → Repository (JPA)
UseCase → ApiMapper (domain dto ↔ OpenAPI generated dto) → Delegate → response
```

### Decisões Técnicas
| Decisão | Escolha | Alternativa Descartada | Motivo |
|---------|---------|------------------------|--------|
| Pacote das 4 entidades | `access/status/{dto,service,specification}` compartilhado, classes por entidade | Pacote por entidade | `internal/` já bundla as 4 (+ `EntityType`, +3 `StatusHistory`) juntas hoje — mesma granularidade, ajustável no `design.md` |
| `entityType` — 2 vs. 3 valores | 3 valores (`COMPANY`/`EMPLOYEE`/`LOGIN`), conforme contrato YAML e `EntityType.java` | 2 valores conforme doc 05 | Confirmado com o usuário: contrato é fonte da verdade (regra do projeto — contrato antes do código); doc 05 está desatualizada |
| Unicidade de `code` | Escopo por catálogo (4 tabelas independentes) | Unicidade cross-catálogo | Doc 05 e schema físico tratam os 4 como catálogos distintos (`SCOS_REASON_ACTIVATE` etc.) |

### Banco de Dados
- **Impacto**: ❌ Não — tabelas `SCOS_REASON_{ACTIVATE,INACTIVATE,DISABLE,ENABLE}` já existem (schema v2, change `atualizacao-entidades-jpa-liquibase-v2`, completo)

### Mensagens de Validação (código + HTTP + título + PT-BR + EN)

> Padrão do projeto — **já implementado** pela change `padronizacao-http-status-exception-code` (`ExceptionCodeError.java` hoje tem construtor de 3 argumentos: `code`, `httpCode`, `title`). Cada constante nova aqui segue o mesmo padrão: `code` em `ExceptionCodeError`, `httpCode` (int), `title` (chave de uma das 7 categorias já existentes — `SCOS_TITLE_NOT_FOUND`/`SCOS_TITLE_CONFLICT`/`SCOS_TITLE_BUSINESS_RULE_VIOLATION`/etc., **nenhuma categoria nova a criar**), e o texto de `detail` (pt-br/en) em `scos_message_organization.properties`/`_en.properties`. Sem código "003 em uso" — disable de um motivo não bloqueia por vínculo existente em `*StatusHistory` (mesma regra dos catálogos de endereço/contato).

| Código | HTTP | Title | PT-BR (detail) | EN (detail) |
|---|---|---|---|---|
| `SCOS_REASON_ACTIVATE_001` | 404 | `SCOS_TITLE_NOT_FOUND` | O motivo de ativação informado não existe. | The informed activation reason does not exist. |
| `SCOS_REASON_ACTIVATE_002` | 409 | `SCOS_TITLE_CONFLICT` | Já existe um motivo de ativação cadastrado com esse código. | There is already an activation reason registered with this code. |
| `SCOS_REASON_ACTIVATE_003` | 422 | `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | O motivo de ativação informado já está ativo. | The informed activation reason is already active. |
| `SCOS_REASON_ACTIVATE_004` | 422 | `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | O motivo de ativação informado já está inativo. | The informed activation reason is already inactive. |
| `SCOS_REASON_INACTIVATE_001` | 404 | `SCOS_TITLE_NOT_FOUND` | O motivo de inativação informado não existe. | The informed inactivation reason does not exist. |
| `SCOS_REASON_INACTIVATE_002` | 409 | `SCOS_TITLE_CONFLICT` | Já existe um motivo de inativação cadastrado com esse código. | There is already an inactivation reason registered with this code. |
| `SCOS_REASON_INACTIVATE_003` | 422 | `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | O motivo de inativação informado já está ativo. | The informed inactivation reason is already active. |
| `SCOS_REASON_INACTIVATE_004` | 422 | `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | O motivo de inativação informado já está inativo. | The informed inactivation reason is already inactive. |
| `SCOS_REASON_DISABLE_001` | 404 | `SCOS_TITLE_NOT_FOUND` | O motivo de bloqueio informado não existe. | The informed block reason does not exist. |
| `SCOS_REASON_DISABLE_002` | 409 | `SCOS_TITLE_CONFLICT` | Já existe um motivo de bloqueio cadastrado com esse código. | There is already a block reason registered with this code. |
| `SCOS_REASON_DISABLE_003` | 422 | `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | O motivo de bloqueio informado já está ativo. | The informed block reason is already active. |
| `SCOS_REASON_DISABLE_004` | 422 | `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | O motivo de bloqueio informado já está inativo. | The informed block reason is already inactive. |
| `SCOS_REASON_ENABLE_001` | 404 | `SCOS_TITLE_NOT_FOUND` | O motivo de desbloqueio informado não existe. | The informed unblock reason does not exist. |
| `SCOS_REASON_ENABLE_002` | 409 | `SCOS_TITLE_CONFLICT` | Já existe um motivo de desbloqueio cadastrado com esse código. | There is already an unblock reason registered with this code. |
| `SCOS_REASON_ENABLE_003` | 422 | `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | O motivo de desbloqueio informado já está ativo. | The informed unblock reason is already active. |
| `SCOS_REASON_ENABLE_004` | 422 | `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | O motivo de desbloqueio informado já está inativo. | The informed unblock reason is already inactive. |

> Validação de campo obrigatório/`entityType` enum inválido (400) fica a cargo do `x-required-message`/`x-empty-message` já definidos no contrato OpenAPI (`SCOS_VALIDATION_*` genérico), não precisa de código novo aqui.

---

## 4️⃣ Implementação

### Arquivos

**Novos**:
- `access/status/dto/Reason{Activate,Inactivate,Disable,Enable}{Input,Output}.java` (8)
- `access/status/service/Reason{Activate,Inactivate,Disable,Enable}{Mapper,ServiceBean}.java` (8)
- `access/status/specification/Reason{Activate,Inactivate,Disable,Enable}Service.java` (4)
- `usecase/access/status/reason{activate,inactivate,disable,enable}/*UseCase[Bean].java` (6 × 4 = 24) + `ApiMapper.java` (4)
- `api/delegate/reason/Reason{Activate,Inactivate,Disable,Enable}Delegate.java` (4)
- Testes unitários: `Reason{Activate,Inactivate,Disable,Enable}ServiceBeanTest.java` (Mockito, sem Testcontainers)

**Modificados**:
- `access/status/internal/Reason{Activate,Inactivate,Disable,Enable}Repository.java` — `existsByCode`/`existsByCodeAndNotId`
- `ScosOrganizationPermission.java` — +20 entradas
- `ExceptionCodeError.java` — +16 constantes (`SCOS_REASON_{ACTIVATE,INACTIVATE,DISABLE,ENABLE}_001..004`), cada uma já com os 3 argumentos do construtor atual (`code`, `httpCode`, `title` — reaproveitando as 7 categorias de `SCOS_TITLE_*` já existentes, nenhuma nova)
- `scos_message_organization.properties` / `scos_message_organization_en.properties` — +16 mensagens de `detail` cada (pt-br/en); nenhuma chave `SCOS_TITLE_*` nova necessária

### Tarefas
- [ ] **T-01**: Domain — dto/service/specification `ReasonActivate` + `existsByCode*`
- [ ] **T-02**: Domain — idem `ReasonInactivate`
- [ ] **T-03**: Domain — idem `ReasonDisable`
- [ ] **T-04**: Domain — idem `ReasonEnable`
- [ ] **T-05**: Usecase — 6 Use Cases + Bean + ApiMapper `ReasonActivate`
- [ ] **T-06**: Usecase — idem `ReasonInactivate`
- [ ] **T-07**: Usecase — idem `ReasonDisable`
- [ ] **T-08**: Usecase — idem `ReasonEnable`
- [ ] **T-09**: Api — 4 Delegates
- [ ] **T-10**: `ScosOrganizationPermission` — 20 novas entradas
- [ ] **T-11**: `ExceptionCodeError` — 16 novas constantes com `httpCode`/`title` conforme a tabela (construtor de 3 args já existe, implementado pela change `padronizacao-http-status-exception-code`) + `scos_message_organization[_en].properties` — 16 mensagens de `detail` × 2 idiomas
- [ ] **T-12**: Testes unitários dos 4 `ServiceBean`

### Riscos e Edge Cases
1. `entityType` com 3 valores — confirmar que os generated DTOs OpenAPI (`ReasonEntityType`) já refletem isso antes de escrever o `ApiMapper` (evitar mismatch de enum em build)
2. `code` duplicado valida só dentro do mesmo catálogo (4 tabelas independentes)
3. `enable`/`disable` idempotentes devem retornar 422 se já no estado alvo
4. Esses 4 catálogos são referenciados por FK em `CompanyStatusHistory`/`EmployeeStatusHistory`/`LoginStatusHistory` (`REASON_ACTIVATE_ID` etc.) — `disable` não deve quebrar histórico existente, só bloquear novo uso (mesma regra do catálogo de endereço/contato)

---

## 📎 Referências
- `etc/doc/usecase/05-catalogo-motivos.md` (seção 10, UC-113 a UC-136)
- Change completo `historico-status-motivos-organization` (contrato OpenAPI, capability `reason-status-catalog`)
- Change de referência `crud-cargo-position` (padrão de implementação)
- Change completa `padronizacao-http-status-exception-code` — já implementada; define o construtor de 3 argumentos (`code`/`httpCode`/`title`) de `ExceptionCodeError` e as 7 chaves `SCOS_TITLE_*` reaproveitadas aqui
- Idea irmã: `20260705_crud-catalogo-tipo-endereco-contato.md`

---
