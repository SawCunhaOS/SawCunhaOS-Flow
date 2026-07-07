# CRUD de Natureza Jurídica

**Data**: 2026-07-07  
**Status**: 🔄 Em Análise  
**Tipo**: 🆕 Nova Feature

---

## ⚠️ Princípio SRP — Uma Funcionalidade por Ideia

> **Regra**: Uma ideia = uma funcionalidade. Features independentes → arquivos separados.

- **Nome da funcionalidade**: `crud-natureza-juridica`
- **Resumo em uma frase**: CRUD do catálogo de Natureza Jurídica (código + descrição), com exclusão física guardada, para vínculo opcional das empresas (`legalNatureId`).

**Checklist SRP**:
- [x] Esta ideia cobre exatamente uma funcionalidade
- [x] Não mistura features independentes no mesmo arquivo
- [x] O nome é específico

> CNAE é feature separada → `20260707_crud-cnae.md`.

---

## 1️⃣ Visão

### Problema
O contrato OpenAPI (`ScosOrganization_Company.yml`, `/v1/legal-natures`), os delegates gerados, a entidade JPA `LegalNature` e a tabela `SCOS_LEGAL_NATURE` (Liquibase) já existem, mas **não há implementação**: sem repository, sem use cases, sem impl de delegate, sem permissões no enum.

### Objetivo
Implementar o CRUD completo de Natureza Jurídica conforme UC-093 a UC-097 (`01-empresa.md`, seção 1.1), com todos os endpoints respondendo os status corretos e guarda de exclusão física.

### Fora de Escopo
- CRUD de CNAE — outra ideia.
- Alteração do contrato OpenAPI (já pronto).
- Alteração da entidade `LegalNature` / tabela `SCOS_LEGAL_NATURE` (já prontas).

---

## 2️⃣ Requisitos

### Funcionais
- [ ] **RF-01**: `GET /v1/legal-natures` — lista paginada (UC-093) → `200`. Autoriza `GET_LEGAL_NATURE`.
- [ ] **RF-02**: `POST /v1/legal-natures` — cria com `code` (≤30, único em `SCOS_LEGAL_NATURE`) + `description` (≤255) (UC-094) → `201`. Autoriza `CREATE_LEGAL_NATURE`.
  - ausente/vazio → `400`; `code` duplicado → `409` `SCOS_LEGAL_NATURE_002`.
- [ ] **RF-03**: `GET /v1/legal-natures/{id}` (UC-095) → `200`; inexistente → `404` `SCOS_LEGAL_NATURE_001`. Autoriza `GET_LEGAL_NATURE`.
- [ ] **RF-04**: `PUT /v1/legal-natures/{id}` (UC-096) → `204`; `code` único excluindo o próprio `{id}` → `409`; `{id}` inexistente → `404`. Autoriza `UPDATE_LEGAL_NATURE`.
- [ ] **RF-05**: `DELETE /v1/legal-natures/{id}` — **exclusão física** (UC-097) → `204`. Autoriza `DELETE_LEGAL_NATURE`.
  - `{id}` inexistente → `404` `SCOS_LEGAL_NATURE_001`.
  - Em uso (existe `SCOS_COMPANY` com `legalNatureId = {id}`) → `422` `SCOS_LEGAL_NATURE_003` ("natureza jurídica em uso").

### Não-Funcionais
- [ ] **RNF-01**: Entidade já `@Auditable` — auditoria mantida.
- [ ] **RNF-02**: Respostas de erro via `ExceptionsHandler` (RFC 9457) e mensagens i18n.

---

## 3️⃣ Arquitetura

### Componentes Afetados
```
scos-organization-domain
├── company/internal/LegalNatureRepository.java ....... adição
└── company/internal/CompanyRepository.java ........... modificação (existsByLegalNatureId)

scos-organization-usecase
└── application/usecase/corporate/company/fiscal/legalnature/ . adição
    ├── CreateLegalNatureUseCase (+ Bean)
    ├── UpdateLegalNatureUseCase (+ Bean)
    ├── FindLegalNatureUseCase (+ Bean)
    ├── FindAllLegalNatureUseCase (+ Bean)
    ├── DeleteLegalNatureUseCase (+ Bean)
    └── LegalNatureApiMapper

scos-organization-api
└── api/delegate/company/LegalNatureDelegate.java ..... adição (implements LegalNatureApiDelegate)

scos-organization-infrastructure
└── enumaration/ScosOrganizationPermission.java ....... modificação (+GET/CREATE/UPDATE/DELETE_LEGAL_NATURE)

scos-organization-shared
└── scos_message_organization[_en].properties ......... modificação (SCOS_LEGAL_NATURE_001/002/003)
```

### Fluxo Principal
```
DELETE /v1/legal-natures/{id}
  → LegalNatureDelegate.deleteLegalNature
  → DeleteLegalNatureUseCase
      1. findById → 404 SCOS_LEGAL_NATURE_001 se ausente
      2. existsByLegalNatureId(id) → 422 SCOS_LEGAL_NATURE_003
      3. repository.delete → 204
```

### Decisões Técnicas
| Decisão | Escolha | Alternativa Descartada | Motivo |
|---------|---------|------------------------|--------|
| Localização | `usecase/corporate/company/fiscal/legalnature` | `catalog/` | Sub-entidade fiscal de Company; entidade vive em `company/internal` |
| Guarda de exclusão | Validação em use case (`existsByLegalNatureId`) | Trigger DB (precedente ContactType) | Erro de domínio explícito e testável; ⚠️ confirmar alinhamento no propose |
| Erro "em uso" | Código específico `SCOS_LEGAL_NATURE_003` | Código genérico | Rastreabilidade (decisão do usuário) |

### Banco de Dados
- **Impacto**: ❌ Não — tabela `SCOS_LEGAL_NATURE` e FK já existem via Liquibase.

---

## 4️⃣ Implementação

### Arquivos

**Novos**:
- `domain/.../company/internal/LegalNatureRepository.java` — `existsByCode`, `existsByCodeAndIdNot`, paginação, `findById`, `delete`.
- `usecase/.../company/fiscal/legalnature/{Create,Update,Find,FindAll,Delete}LegalNatureUseCase.java` (+ `...Bean`) — interface pública + `@Service` package-private.
- `usecase/.../company/fiscal/legalnature/LegalNatureApiMapper.java` — MapStruct entidade↔DTO.
- `api/delegate/company/LegalNatureDelegate.java` — `implements LegalNatureApiDelegate`.

**Modificados**:
- `domain/.../company/internal/CompanyRepository.java` — `existsByLegalNatureId(Long)`.
- `infrastructure/enumaration/ScosOrganizationPermission.java` — `GET_LEGAL_NATURE`, `CREATE_LEGAL_NATURE`, `UPDATE_LEGAL_NATURE`, `DELETE_LEGAL_NATURE`.
- `scos_message_organization.properties` + `_en` — `SCOS_LEGAL_NATURE_001` (não existe), `_002` (code duplicado), `_003` (em uso).

### Tarefas
- [ ] **T-01**: `LegalNatureRepository` + métodos de existência/unicidade.
- [ ] **T-02**: `existsByLegalNatureId` em `CompanyRepository`.
- [ ] **T-03**: Use cases Create/Update/Find/FindAll/Delete (+ Beans) e `LegalNatureApiMapper`.
- [ ] **T-04**: `LegalNatureDelegate implements LegalNatureApiDelegate`.
- [ ] **T-05**: Permissões no enum + mensagens i18n.
- [ ] **T-06**: Testes (unit use cases + integração Testcontainers p/ unicidade e guarda de delete).

### Riscos e Edge Cases
1. `legalNatureId` na `Company` é opcional (FK nullable) — a guarda `existsByLegalNatureId` deve considerar todos os status de empresa.
2. Unicidade de `code` case/trim — decidir normalização no propose (alinhar com outros catálogos).
3. Precedente do trigger de ContactType: decidir no propose se guarda fica só no app ou também em trigger.

---

## 📎 Referências
- `etc/doc/usecase/01-empresa.md` §1.1 (UC-093…UC-097)
- Padrão de referência: `usecase/corporate/catalog/contacttype/*`, `api/delegate/catalog/ContactTypeDelegate.java`
- Contrato: `etc/api/organization/ScosOrganization_Company.yml` (`/v1/legal-natures`)

---
