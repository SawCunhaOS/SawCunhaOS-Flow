# Cadastro e Atualização de Empresa (Company Core CRUD)

**Data**: 2026-07-07  
**Status**: 🔄 Em Análise  
**Tipo**: 🆕 Nova Feature

---

## ⚠️ Princípio SRP — Uma Funcionalidade por Ideia

> **Regra**: Uma ideia = uma funcionalidade. Features independentes → arquivos separados.

- **Nome da funcionalidade**: `company-core-crud`
- **Resumo em uma frase**: Expõe cadastro (POST), atualização (PUT), consulta por id (GET) e listagem paginada (GET) da Empresa, com as validações de campo nas camadas corretas.

**Checklist SRP**:
- [x] Esta ideia cobre exatamente uma funcionalidade (CRUD cadastral da Empresa)
- [x] Não mistura features independentes no mesmo arquivo
- [x] O nome é específico

**Fora deste arquivo (SRP — cada um vira sua própria ideia):**
- Ciclo de vida de status: enable/disable/block/unblock + status-history (UC-006/007/008/137/138) — transições já existem no agregado `Company`, mas os UseCases/endpoints são outra feature.
- Hierarquia: hierarchy/branches (UC-009/010).
- Sub-recursos: contatos (UC-011..015) e endereços (UC-016..020).
- Natureza Jurídica e CNAE — **já implementados** (commit `42068fb`).

---

## 1️⃣ Visão

### Problema
O agregado `Company` (entidade, `Cnpj` value object, transições de status) e o `CompanyRepository` já existem, mas **não há CRUD cadastral**: faltam `CompanyService`/Bean core, os UseCases de criação/atualização/consulta e o `CompanyDelegate`. Os endpoints UC-001..005 (`/v1/companies`) não respondem.

### Objetivo
Empresa matriz e filial podem ser cadastradas, atualizadas, consultadas por id e listadas com paginação/filtro, retornando os status HTTP e códigos de erro do `01-empresa.md`. Sucesso = os UC-S/UC-E de UC-001..005 verdes em teste de integração.

### Fora de Escopo
- Transições de status e histórico (arquivo próprio).
- Hierarquia (hierarchy/branches).
- Contatos e endereços da empresa.

---

## 2️⃣ Requisitos

### Funcionais
- [ ] **RF-01**: `POST /v1/companies` cria matriz (`parentCompanyId` nulo) ou filial → `201` (UC-001/002).
- [ ] **RF-02**: `PUT /v1/companies/{id}` atualiza dados cadastrais (`parentCompanyId` **não** editável) → `204` (UC-005).
- [ ] **RF-03**: `GET /v1/companies/{id}` → `200` com dados completos; `404` se não existir (UC-003).
- [ ] **RF-04**: `GET /v1/companies` lista paginada com filtro por status e/ou nome → `200` `paginatedDTO` (UC-004).
- [ ] **RF-05**: Criação insere 1ª linha em `SCOS_COMPANY_STATUS_HISTORY` com `newStatus=ACTIVE` e `reasonActivateId` (side effect UC-001).

### Não-Funcionais
- [ ] **RNF-01**: `taxIdentifier` (CNPJ Alfa) validado no formato/DV **antes** de qualquer acesso a banco.
- [ ] **RNF-02**: Idempotência no POST via `@JdempotentRequestPayload` já marcado no contrato.

---

## 3️⃣ Arquitetura

### Componentes Afetados
```
scos-organization-api
└── delegate/company/CompanyDelegate: adição (implements CompanyApiDelegate)

scos-organization-usecase
├── .../company/CreateCompanyUseCase(+Bean): adição
├── .../company/UpdateCompanyUseCase(+Bean): adição
├── .../company/FindCompanyUseCase(+Bean): adição
├── .../company/FindAllCompanyUseCase(+Bean): adição
└── .../company/CompanyApiMapper: adição

scos-organization-domain
├── .../company/specification/CompanyService: adição
├── .../company/service/CompanyServiceBean: adição
├── .../company/service/CompanyMapper: adição
└── .../company/dto/CompanyInput|CompanyOutput: adição
```

### Fluxo Principal (POST)
```
Request → [Delegate/boundary: formato+presença+DV] → [UseCase Bean: só orquestra]
        → [CompanyServiceBean: unicidade+FK+regras cross-entity]
        → [Company aggregate: monta 1ª linha StatusHistory] → 201
```

### ⭐ Decisão central — em que camada vai cada validação

> Regra: **a validação mora onde moram os dados que ela precisa.** UseCase nunca valida — só orquestra.

| A validação precisa de… | Camada | Exemplos (Company) | Erro |
|---|---|---|---|
| Só o valor do campo | **Contrato** (YAML → bean-validation + validator) | presença/vazio/tamanho (`name`≤250, `nameTreatment`≤100); `taxIdentifier` 14 alfanum + DV (`x-is-cnpj`); `foundationDate` não-futura; `sectorOfActivity` na lista fixa; `latitude`/`longitude` range | `SCOS_VALIDATION_001/003/010` |
| DB / outros agregados | **Domain Service** (`CompanyServiceBean`) | `taxIdentifier` único; `reasonActivateId` existe + `ACTIVE` + `entityType=COMPANY`; `parentCompany` existe + `ACTIVE`; `legalNatureId`/`cnaePrincipalId` existem; profundidade ≤ `COMPANY_HIERARCHY_MAX_DEPTH` | `SCOS_COMPANY_002`, `404`, `422` |
| Só o estado do próprio agregado | **Entity** (`Company`) | guardas de transição (já implementadas: `activate/inactivate/disable/enable`) | `SCOS_COMPANY_007` |
| Nada — orquestra | **UseCase Bean** | map API DTO → `CompanyInput`, chama service, map back | — |

### Decisões Técnicas
| Decisão | Escolha | Alternativa Descartada | Motivo |
|---------|---------|------------------------|--------|
| Validação de formato/DV do CNPJ | Contrato (`x-is-cnpj`) + `Cnpj` value object no domínio | Validar no UseCase | Falha barata antes do banco; VO garante invariante no domínio; padrão já existente (`CreateContactTypeUseCaseBean` não valida) |
| Validação de negócio (unicidade/FK/status) | `CompanyServiceBean` | UseCase Bean | Precisa de repositório/outros agregados; espelha `ContactTypeServiceBean.create` (`existsBy...` → `throw ScosException`) |
| Guarda de transição de status | Método no agregado `Company` | Service | Depende só do estado próprio; já implementado |

### Banco de Dados
- **Impacto**: ❌ Não (tabelas `SCOS_COMPANY` e `SCOS_COMPANY_STATUS_HISTORY` já existem; Company entity + repos prontos).

---

## 4️⃣ Implementação

### Arquivos

**Novos**:
- `scos-organization-api/.../delegate/company/CompanyDelegate.java` — expõe UC-001..005.
- `scos-organization-usecase/.../company/{Create,Update,Find,FindAll}CompanyUseCase(+Bean).java` — orquestração.
- `scos-organization-usecase/.../company/CompanyApiMapper.java` — API DTO ⇄ domain Input/Output.
- `scos-organization-domain/.../company/specification/CompanyService.java` + `service/CompanyServiceBean.java` + `service/CompanyMapper.java` — regras de negócio.
- `scos-organization-domain/.../company/dto/CompanyInput.java` + `CompanyOutput.java`.

**Modificados**:
- `etc/api/organization/ScosOrganization_Company.yml` — confirmar operations/responses de UC-001..005 (schemas já existem).
- `ScosOrganizationPermission` — `CREATE_COMPANY`/`UPDATE_COMPANY`/`GET_COMPANY` se ausentes.

### Tarefas
- [ ] **T-01**: Contrato — validar YAML dos 4 endpoints (200 com `data:`, 201/204/4XX via ScosComponents).
- [ ] **T-02**: Domínio — `CompanyService` + `CompanyServiceBean` (create/update/findById/findAll) com regras cross-entity.
- [ ] **T-03**: UseCases + `CompanyApiMapper` (sem validação).
- [ ] **T-04**: `CompanyDelegate`.
- [ ] **T-05**: Testes de integração (Testcontainers) cobrindo UC-S/UC-E de UC-001..005.

### Riscos e Edge Cases
1. Ordem das regras importa: formato (400) antes de unicidade (409) antes de compatibilidade de motivo (422) — respeitar a sequência do `01-empresa.md`.
2. `reasonActivateId` na criação: validar `entityType=COMPANY` e `ACTIVE=true` (UC-E5/E6) reusando o service de `ReasonActivate` já existente.
3. Filial: profundidade contada da raiz ≤ `COMPANY_HIERARCHY_MAX_DEPTH` (UC-E9).
4. Update não pode alterar `parentCompanyId`; unicidade de `taxIdentifier` exclui o próprio `{id}`.

---

## 📎 Referências
- `etc/doc/usecase/01-empresa.md` (UC-001..005)
- Padrão de referência: `CreateContactTypeUseCaseBean` / `ContactTypeServiceBean` / `ContactType`
- `00-indice-central.md` (catálogo de erros, `COMPANY_HIERARCHY_MAX_DEPTH`)

---
