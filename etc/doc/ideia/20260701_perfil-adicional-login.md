# Perfil Adicional de Login

**Data**: 2026-07-01
**Status**: 🔄 Em Análise
**Tipo**: 🆕 Nova Feature

---

## ⚠️ Princípio SRP — Uma Funcionalidade por Ideia

> **Regra**: Uma ideia = uma funcionalidade. Features independentes → arquivos separados.

- **Nome da funcionalidade**: `perfil-adicional-login`
- **Resumo em uma frase**: Expor CRUD do relacionamento N:N `SCOS_LOGIN_PROFILE` (perfis adicionais de um login), distinto do endpoint existente que substitui o perfil principal.

**Checklist SRP**:
- [x] Esta ideia cobre exatamente uma funcionalidade — gestão de perfis adicionais de um login
- [x] Não mistura com histórico de status, fiscal, cargo, jornada, catálogo de endereço/contato ou outbox — ideias próprias
- [x] O nome é específico

---

## 1️⃣ Visão

### Problema
`SCOS_LOGIN_PROFILE` (schema v2) é uma tabela N:N nova: um login pode ter perfis **adicionais** além do principal (`SCOS_LOGIN.PROFILE_ID`). O contrato hoje só tem `PUT /logins/{id}/profile/{profileId}` — que **substitui** o perfil principal (`updateLoginProfile`, UC-065), um conceito diferente (1:1, `Login.profileId`). Não existe nenhum endpoint para o relacionamento N:N.

### Objetivo
Existe endpoint para listar, adicionar e remover perfis adicionais de um login, sem conflitar com o endpoint existente de substituição do perfil principal.

### Fora de Escopo
- `PUT /logins/{id}/profile/{profileId}` (substituição do perfil principal) — já existe, não muda
- Histórico de status, dados fiscais, cargo/contrato, jornada, catálogo de endereço/contato, outbox — ideias próprias

---

## 2️⃣ Requisitos

### Funcionais
- [ ] **RF-01**: Criar `GET /v1/logins/{id}/profiles` — lista paginada dos perfis adicionais do login (distinto do singular `/profile/{profileId}` existente)
- [ ] **RF-02**: Criar `POST /v1/logins/{id}/profiles/{profileId}` — adiciona um perfil adicional ao login
- [ ] **RF-03**: Criar `DELETE /v1/logins/{id}/profiles/{profileId}` — remove um perfil adicional do login
- [ ] **RF-04** (achado na revisão): Toda rota nova precisa de `x-authorize` + entrada correspondente em `ScosOrganizationPermission` — `GET/CREATE/DELETE_LOGIN_PROFILE`

### Não-Funcionais
- [ ] **RNF-01**: Nenhuma alteração de schema no banco — schema v2 já existe
- [ ] **RNF-02**: Nomenclatura das rotas (plural `/profiles`) deixa claro que é distinto do singular `/profile/{profileId}` já existente — evitar ambiguidade na documentação
- [ ] **RNF-03** (achado na revisão): `GET /v1/logins/{id}/profiles` responde `GetAllLoginProfilesResponse { data: array, paginatedDTO }` com `paginationFilter` (query, obrigatório) — mesmo padrão do resto do contrato. `POST`/`DELETE` não precisam de schema de request (chave vem toda pelos path params, sem body)

---

## 3️⃣ Arquitetura

### Componentes Afetados
```
etc/api/organization/
└── ScosOrganization_Login.yml
    └── paths novos: GET/POST/DELETE /v1/logins/{id}/profiles*
```

### Detalhamento de Campos por Arquivo YAML

| Arquivo YAML | Schema/Path | Campo | Tipo | Nota |
|---|---|---|---|---|
| `ScosOrganization_Login.yml` | `GetAllLoginProfilesResponse` (novo) | `data`, `paginatedDTO` | `$ref array Profile` / `$ref ScosPaginated` | padrão SCOS lista paginada |
| `ScosOrganization_Login.yml` | `GET /v1/logins/{id}/profiles` (novo) | — | — | paginado, distinto do singular `/profile/{profileId}` já existente |
| `ScosOrganization_Login.yml` | `POST /v1/logins/{id}/profiles/{profileId}` (novo) | — | — | chave via path params, sem body; valida `profileId ≠ Login.profileId` (trigger) |
| `ScosOrganization_Login.yml` | `DELETE /v1/logins/{id}/profiles/{profileId}` (novo) | — | — | sem body; idempotência (`204` vs `404`) a decidir no `/propose` |

### Fluxo Principal
```
POST /logins/{id}/profiles/{profileId}
  → valida login existe, profileId existe e active=true
  → valida ainda não existe essa combinação (PK composta loginId+profileId)
  → usecase cria SCOS_LOGIN_PROFILE
```

### Decisões Técnicas
| Decisão | Escolha | Alternativa Descartada | Motivo |
|---------|---------|------------------------|--------|
| Shape das rotas | Sub-recurso plural (`/profiles`, distinto do singular `/profile/{profileId}`) | Outro verbo/nomenclatura | Decidido com o usuário — reaproveita padrão REST direto, plural deixa claro que é N:N |

### Banco de Dados
- **Impacto**: ❌ Não — schema já existe (`adequacao-liquibase-domain-model-v2`, completo)

---

## 4️⃣ Implementação

*Detalhamento de arquivos/tarefas fica para a fase de `/openspec-propose`.*

### Riscos e Edge Cases
1. (resolvido na revisão) Perfil adicional igual ao perfil principal (`Login.profileId`) — **não é permitido**: `TRG_VALIDATE_LOGIN_PROFILE_NOT_PRIMARY` bloqueia no banco (`BEFORE INSERT OR UPDATE`). O `POST /logins/{id}/profiles/{profileId}` precisa validar isso antes de chamar o banco e devolver `4XX` amigável — sem a validação client-side, o erro que sobe é a exceção genérica da trigger
2. Remover um perfil adicional que já não existe (idempotência do `DELETE`) — definir se retorna `204` silencioso ou `404`

---

## 📎 Referências
- Domain model: `etc/database/domain_model.md`
- Migrations v2: `openspec/changes/adequacao-liquibase-domain-model-v2/`

---
