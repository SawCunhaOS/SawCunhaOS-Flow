# Dados Fiscais da Empresa

**Data**: 2026-07-01
**Status**: 🔄 Em Análise
**Tipo**: 🆕 Nova Feature

---

## ⚠️ Princípio SRP — Uma Funcionalidade por Ideia

> **Regra**: Uma ideia = uma funcionalidade. Features independentes → arquivos separados.

- **Nome da funcionalidade**: `dados-fiscais-company`
- **Resumo em uma frase**: Expor no contrato de `Company` os novos dados fiscais (natureza jurídica, CNAE principal e secundários, inscrição estadual/municipal) introduzidos no schema v2.

**Checklist SRP**:
- [x] Esta ideia cobre exatamente uma funcionalidade — dados fiscais da empresa
- [x] Não mistura com histórico de status, catálogo de endereço/contato, cargo, jornada, outbox ou perfil — cada um é ideia própria
- [x] O nome é específico

---

## 1️⃣ Visão

### Problema
`SCOS_COMPANY` ganhou 4 colunas novas no schema v2: `LEGAL_NATURE_ID` (FK, nullable), `CNAE_PRINCIPAL_ID` (FK, nullable), `STATE_REGISTRATION`, `MUNICIPAL_REGISTRATION`. Também existem as tabelas novas `SCOS_LEGAL_NATURE`, `SCOS_CNAE` (catálogos, pensados para seed IBGE mas sem seed oficial neste momento) e `SCOS_COMPANY_CNAE_SECONDARY` (N:N, CNAEs secundários da empresa). Nada disso está no contrato `ScosOrganization_Company.yml` hoje.

### Objetivo
`Company` expõe os 4 campos fiscais novos; existem catálogos com CRUD completo para `LegalNature`/`Cnae`; existe sub-recurso para gerenciar os CNAEs secundários de uma empresa.

### Fora de Escopo
- Seed oficial de dados IBGE (`LEGAL_NATURE`/`CNAE`) — decisão já tomada fora de escopo desde a change Liquibase v2
- Histórico de status, catálogo de endereço/contato, cargo/contrato, jornada, outbox, perfil adicional — ideias próprias

---

## 2️⃣ Requisitos

### Funcionais
- [ ] **RF-01**: Criar CRUD mínimo de referência `LegalNature` (`code`, `description`) — `GET /v1/legal-natures`, `POST /v1/legal-natures`, `GET /v1/legal-natures/{id}`, `PUT /v1/legal-natures/{id}` (só `code`/`description`), `DELETE /v1/legal-natures/{id}` (físico — sem soft-delete, sem `active`)
- [ ] **RF-02**: Criar CRUD mínimo de referência `Cnae` (`code`, `description`) — mesmo padrão de `LegalNature`
- [ ] **RF-03**: Adicionar a `CompanyOutput`/`CreateCompanyRequest`/`UpdateCompanyRequest`: `legalNatureId` (nullable), `cnaePrincipalId` (nullable), `stateRegistration` (nullable, aceita `ISENTO`), `municipalRegistration` (nullable)
- [ ] **RF-04**: Criar sub-recurso `CompanyCnaeSecondary` — `GET /v1/companies/{companyId}/cnaes-secondary` (lista), `POST /v1/companies/{companyId}/cnaes-secondary/{cnaeId}` (adiciona), `DELETE /v1/companies/{companyId}/cnaes-secondary/{cnaeId}` (remove) — PK composta, sem update (é associação pura)
- [ ] **RF-05** (achado na revisão): Toda rota nova precisa de `x-authorize` + entrada correspondente em `ScosOrganizationPermission` — `GET/CREATE/UPDATE/DELETE_LEGAL_NATURE`, `GET/CREATE/UPDATE/DELETE_CNAE`, `GET/CREATE/DELETE_COMPANY_CNAE_SECONDARY`

### Não-Funcionais
- [ ] **RNF-01**: `LegalNature`/`Cnae` não têm `UPDATED_AT`/`USER_AT`/`ACTIVE` no banco — decidido com o usuário manter o schema como está, sem migration nova. `PUT`/`DELETE` no contrato refletem isso: sem campo de auditoria na resposta, sem `enable`/`disable` (não existe estado inativo), `DELETE` é exclusão física real
- [ ] **RNF-02**: Nenhuma alteração de schema no banco — schema v2 já existe, decisão confirmada de não abrir migration pra isso
- [ ] **RNF-03** (achado na revisão): DTOs seguem o padrão SCOS do restante do arquivo — `CreateLegalNatureRequest`/`UpdateLegalNatureRequest` (mesmo padrão pra `Cnae`), `GetLegalNatureResponse { data }`, `GetAllLegalNaturesResponse { data: array, paginatedDTO }`. `GET /v1/legal-natures`, `GET /v1/cnaes` e `GET /v1/companies/{companyId}/cnaes-secondary` usam `paginationFilter` (query, obrigatório) — sem exceção nenhuma lista do contrato atual foge da paginação, mesmo catálogos pequenos

---

## 3️⃣ Arquitetura

### Componentes Afetados
```
etc/api/organization/
└── ScosOrganization_Company.yml
    ├── schemas: CompanyOutput/CreateCompanyRequest/UpdateCompanyRequest (mod — 4 campos fiscais)
    ├── schemas novos: LegalNature, Cnae, CompanyCnaeSecondary
    └── paths novos: /v1/legal-natures*, /v1/cnaes*, /v1/companies/{companyId}/cnaes-secondary*
```

### Detalhamento de Campos por Arquivo YAML

| Arquivo YAML | Schema/Path | Campo | Tipo | Nota |
|---|---|---|---|---|
| `ScosOrganization_Company.yml` | `CompanyOutput`, `CreateCompanyRequest`, `UpdateCompanyRequest` (mod) | `legalNatureId` | `integer (format: int64)`, nullable | FK `LegalNature` |
| `ScosOrganization_Company.yml` | `CompanyOutput`, `CreateCompanyRequest`, `UpdateCompanyRequest` (mod) | `cnaePrincipalId` | `integer (format: int64)`, nullable | FK `Cnae` |
| `ScosOrganization_Company.yml` | `CompanyOutput`, `CreateCompanyRequest`, `UpdateCompanyRequest` (mod) | `stateRegistration` | `string`, nullable | aceita literal `"ISENTO"` |
| `ScosOrganization_Company.yml` | `CompanyOutput`, `CreateCompanyRequest`, `UpdateCompanyRequest` (mod) | `municipalRegistration` | `string`, nullable | sem regra de formato adicional |
| `ScosOrganization_Company.yml` | `LegalNature` (novo) | `code`, `description` | string / string | sem `active`/auditoria — banco não tem essas colunas |
| `ScosOrganization_Company.yml` | `Cnae` (novo) | `code`, `description` | string / string | mesmo padrão de `LegalNature` |
| `ScosOrganization_Company.yml` | `CreateLegalNatureRequest`/`UpdateLegalNatureRequest`, `CreateCnaeRequest`/`UpdateCnaeRequest` (novos) | `code`, `description` | string / string | `UpdateXRequest` só troca esses 2 campos |
| `ScosOrganization_Company.yml` | `GetLegalNatureResponse`/`GetAllLegalNaturesResponse`, `GetCnaeResponse`/`GetAllCnaesResponse` (novos) | `data`, `paginatedDTO` | `$ref` / `$ref ScosPaginated` | padrão SCOS `{ data }` / `{ data: array, paginatedDTO }` |
| `ScosOrganization_Company.yml` | `CompanyCnaeSecondary` (novo, associação pura) | `companyId`, `cnaeId` | `integer (format: int64)` × 2 | PK composta, sem campo próprio além da FK dupla |
| `ScosOrganization_Company.yml` | `GET/POST /v1/legal-natures`, `GET/PUT/DELETE /v1/legal-natures/{id}` (novos) | — | — | mesmo conjunto para `/v1/cnaes`; `DELETE` é exclusão física real |
| `ScosOrganization_Company.yml` | `GET /v1/companies/{companyId}/cnaes-secondary`, `POST/DELETE .../cnaes-secondary/{cnaeId}` (novos) | — | — | sem `PUT` — é associação pura |

### Fluxo Principal
```
POST /companies/{companyId}/cnaes-secondary/{cnaeId}
  → valida companyId existe, cnaeId existe
  → usecase cria SCOS_COMPANY_CNAE_SECONDARY (companyId, cnaeId)
```

### Decisões Técnicas
| Decisão | Escolha | Alternativa Descartada | Motivo |
|---------|---------|------------------------|--------|
| CRUD de LegalNature/Cnae | Mínimo, sem migration — `PUT` só troca `code`/`description`, `DELETE` físico, sem `active`/soft-delete | Migration nova adicionando `ACTIVE`/`UPDATED_AT`/`USER_AT` (igualando ao padrão `Department`/`ReasonX`) | Decidido com o usuário — evita abrir change de banco só pra isso; aceita o schema mínimo como está |
| Onde ficam os endpoints | Dentro de `ScosOrganization_Company.yml` | Arquivo próprio | Catálogos são exclusivos de `Company`, sem uso em `Employee` — diferente do catálogo de endereço/contato que é compartilhado |

### Banco de Dados
- **Impacto**: ❌ Não — schema já existe (`adequacao-liquibase-domain-model-v2`, completo)

---

## 4️⃣ Implementação

*Detalhamento de arquivos/tarefas fica para a fase de `/openspec-propose`.*

### Riscos e Edge Cases
1. `LegalNature`/`Cnae` sem `ACTIVE`/`UPDATED_AT` no banco — resolvido: `DELETE` é exclusão física real. Se um `Cnae`/`LegalNature` em uso for excluído, a FK em `Company`/`CompanyCnaeSecondary` quebra (constraint de FK deve rejeitar, não há soft-delete pra evitar isso) — comportamento aceito, mas documentar no contrato que `DELETE` só funciona se não houver referência
2. `cnaePrincipalId` (`Company`) e `CompanyCnaeSecondary` podem referenciar o mesmo `Cnae` — não há regra de banco impedindo isso; considerar se faz sentido validar (principal não deveria repetir como secundário) na camada de use case

---

## 📎 Referências
- Domain model: `etc/database/domain_model.md`
- Migrations v2: `openspec/changes/adequacao-liquibase-domain-model-v2/`

---
