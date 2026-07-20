# Isolamento de Permissões por Sistema (Multi-Tenant) no validateAuthority

**Data**: 2026-07-09  
**Status**: 🔄 Em Análise  
**Tipo**: 🆕 Nova Feature | ⚡ Performance | 🗄️ Banco de Dados

---

## ⚠️ Princípio SRP — Uma Funcionalidade por Ideia

> **Regra**: Uma ideia = uma funcionalidade. Features independentes → arquivos separados.

- **Nome da funcionalidade**: `permission-multi-tenant-isolation`
- **Resumo em uma frase**: Torna a chave única do `Resource` `(CODE, SYSTEM_ID)` (permitindo que sistemas diferentes reusem o mesmo código de permissão) e faz o `validateAuthority` retornar **apenas** as permissões do sistema chamador (autenticado via token), reduzindo o payload e isolando permissões por tenant.

**Checklist SRP**:
- [x] Esta ideia cobre exatamente uma funcionalidade (isolamento de permissões por sistema)
- [x] Não mistura features independentes (paridade do `registrySystem` está em arquivo próprio)
- [x] O nome é específico

**Fora deste arquivo (SRP — cada um vira sua própria ideia):**
- Paridade de validação/erro do `registrySystem` → `20260709_registrysystem-validacao-tratamento-erro-parity.md`.
- Endpoint REST de listagem/atribuição de permissões agrupadas ao perfil (front).

---

## 1️⃣ Visão

### Problema
Hoje `SCOS_RESOURCE.CODE` é **único global** (`UK_CODE_SCOS_RESOURCE`). Consequências:
- **Sem isolamento por sistema.** Dois sistemas registrados não podem ter uma permissão com o mesmo código (ex.: ambos `GET_DEPARTMENT`) — o segundo registro colide na UK.
- **`validateAuthority(login)` retorna TODAS as permissões** do login, independentemente do sistema chamador. O `vw_login_context` faz `profile → profile_resource → resource(r.code)` sem filtrar por `system_id`. Um sistema recebe permissões que pertencem a recursos de outros sistemas.
- **Payload maior que o necessário** → custo de rede/serialização e de avaliação no consumidor.
- O sistema chamador **já é conhecido** (o `TokenAuthorizationInterceptor` autentica via `code:secret` e coloca `systemCode` no `SecurityContext`), mas essa informação **não é usada** para filtrar.

### Objetivo
- `SCOS_RESOURCE` passa a ter UK `(CODE, SYSTEM_ID)` → sistemas distintos reusam códigos.
- `validateAuthority` retorna **apenas** as permissões cujo `resource.system` = sistema chamador.
- Índice único das views materializadas passa a incluir `system_id` (evita colisão com códigos duplicados).
- Sucesso = registrar `GET_DEPARTMENT` em 2 sistemas sem colisão; `validateAuthority` chamado pelo sistema X retorna só as permissões de X para o login; a materialized view sobe sem violar o índice único.

### Fora de Escopo
- Alterar a identificação do sistema no payload do `AuthorityRequest` (o sistema vem do **token autenticado**, não do request — sem mudança de proto).
- Paridade do `registrySystem` (arquivo próprio).
- Login/perfil multi-sistema no cadastro (perfis continuam como estão; só a leitura filtra).

---

## 2️⃣ Requisitos

### Funcionais
- [ ] **RF-01**: UK do `SCOS_RESOURCE` muda de `CODE` para `(CODE, SYSTEM_ID)` (`UK_CODE_SYSTEM_SCOS_RESOURCE`); dois sistemas podem registrar o mesmo `CODE`.
- [ ] **RF-02**: `validateAuthority` filtra as permissões pelo `systemCode` do chamador (obtido do `SecurityContext` setado pelo `TokenAuthorizationInterceptor`) — retorna só as permissões daquele sistema.
- [ ] **RF-03**: `vw_login_context` e `vw_authority_response` carregam `system_id`/`system_code`; o índice único passa a incluir o sistema (`(login_id, system_id, COALESCE(permission,''))`).
- [ ] **RF-04**: O use case `ValidateAuthorityUseCase.execute` passa a receber o `systemCode` (além do `login`) e a query filtra por ele.

### Não-Funcionais
- [ ] **RNF-01**: **Performance** — payload de permissões reduzido ao escopo do sistema; filtro apoiado em índice por `system_id`.
- [ ] **RNF-02**: Zero regressão para sistemas que hoje têm só um conjunto de permissões (comportamento equivalente quando há um único sistema).
- [ ] **RNF-03**: Sistema não publicado → editar `scos_resource.yml` + views direto (sem migration incremental).

---

## 3️⃣ Arquitetura

### Componentes Afetados
```
flow-organization-boot
├── db/.../tables/scos_resource.yml      : UK CODE → (CODE, SYSTEM_ID)
├── db/.../view/vw_login_context.sql     : +system_id/system_code; índice único +system
└── db/.../view/vw_authority_response.sql: +system_id/system_code; índice/where por sistema

flow-organization-domain
└── access/authority/... (AuthorityResponseService / repo) : filtrar por systemCode

flow-organization-usecase
└── access/authority/validate/ValidateAuthorityUseCase(+Bean): execute(login, systemCode)

grpc/flow-organization-grpc-boot
└── delegate/ValidateAuthorityServiceImpl : obter systemCode do SecurityContext e repassar

⚠️ DEPENDÊNCIA — permission-metadata-registry-enrichment (não arquivada):
└── ResourceRepository.upsert : ON CONFLICT (CODE) → ON CONFLICT (CODE, SYSTEM_ID)
```

### Fluxo Principal
```
Sistema X  ─validateAuthority(login)─►  interceptor autentica (principal = systemCode=X)
        ▼
ValidateAuthorityServiceImpl lê systemCode do SecurityContext
        ▼
ValidateAuthorityUseCase.execute(login, systemCode=X)
        ▼
query vw_authority_response WHERE login=? AND system_code=X (índice login+system)
        ▼
AuthorityResponse.permissions = só as permissões de alice no sistema X
```

### Decisões Técnicas
| Decisão | Escolha (proposta) | Alternativa Descartada | Motivo |
|---------|--------------------|------------------------|--------|
| Identificação do sistema chamador | `SecurityContext` (principal = systemCode do token) | Adicionar `system` no `AuthorityRequest` | Sistema já autenticado no interceptor; não confiar no payload; sem mudança de proto |
| Onde filtrar por sistema | Nas views materializadas (+`system_id`) e `WHERE system_code=?` | Filtrar em memória no consumidor | Filtro no banco com índice; payload já sai reduzido; alinhado à arquitetura de views + pg_cron |
| Chave única do Resource | `(CODE, SYSTEM_ID)` | Manter `CODE` global | Habilita multi-tenant; permite reuso de códigos entre sistemas |
| Índice único da view | `(login_id, system_id, COALESCE(permission,''))` | `(login_id, permission)` | Códigos duplicados entre sistemas colidiriam no índice atual |

### Banco de Dados
- **Impacto**: ✅ Sim.
- `SCOS_RESOURCE`: trocar `UK_CODE_SCOS_RESOURCE` (CODE) por `UK_CODE_SYSTEM_SCOS_RESOURCE` (CODE, SYSTEM_ID). O índice `IDX_SYSTEM_ID_SCOS_RESOURCE` já existe.
- `vw_login_context`: expor `r.system_id` (e/ou `system_code` via join em `scos_system`); índice único `(login_id, system_id, COALESCE(permission,''))`.
- `vw_authority_response`: agrupar/expor por `(login_id, system_id)`; ajustar índice único e a query consumidora.

---

## 4️⃣ Implementação

### Arquivos

**Modificados**:
- `flow-organization-boot/.../tables/scos_resource.yml` — UK `(CODE, SYSTEM_ID)`.
- `flow-organization-boot/.../view/vw_login_context.sql` — +`system_id`/`system_code`; índice único +system.
- `flow-organization-boot/.../view/vw_authority_response.sql` — +`system`; where/índice por sistema.
- `flow-organization-usecase/.../authority/validate/ValidateAuthorityUseCase(+Bean)` — `execute(login, systemCode)`.
- `flow-organization-domain/.../access/authority/...` (service/repo) — filtro por `systemCode`.
- `grpc/.../delegate/ValidateAuthorityServiceImpl.java` — ler `systemCode` do `SecurityContext`.
- **[dependência]** `flow-organization-domain/.../resource/internal/ResourceRepository.java` — `ON CONFLICT (CODE, SYSTEM_ID)` (altera a change `permission-metadata-registry-enrichment`).

### Tarefas
- [ ] **T-01**: `scos_resource.yml` — UK `(CODE, SYSTEM_ID)`.
- [ ] **T-02**: Ajustar `ResourceRepository.upsert` para `ON CONFLICT (CODE, SYSTEM_ID)` (coordenar com a change anterior).
- [ ] **T-03**: `vw_login_context` — expor sistema + índice único `(login_id, system_id, permission)`.
- [ ] **T-04**: `vw_authority_response` — sistema no agrupamento/índice; query consumidora filtra por sistema.
- [ ] **T-05**: `ValidateAuthorityUseCase.execute(login, systemCode)` + repo/service filtrando.
- [ ] **T-06**: `ValidateAuthorityServiceImpl` — obter `systemCode` do `SecurityContext` (`Objects.requireNonNull(SecurityContextHolder...).getPrincipal()`).
- [ ] **T-07**: Teste de integração — mesmo `CODE` em 2 sistemas; `validateAuthority` de X retorna só permissões de X; view sobe sem violar índice único.

### Riscos e Edge Cases
1. **Coordenação com `permission-metadata-registry-enrichment`** (não arquivada): o `ON CONFLICT (CODE)` do upsert precisa virar `(CODE, SYSTEM_ID)`. Decidir: incorporar aqui, ou emendar aquela change antes de arquivar.
2. **Perfil com recursos de múltiplos sistemas**: um `profile_resource` pode apontar recursos de sistemas diferentes. Com o filtro, cada sistema vê só sua fatia — confirmar se é o comportamento desejado (provável sim).
3. **Refresh das materialized views (pg_cron 30min)**: registro de novos resources só reflete no `validateAuthority` após refresh — comportamento atual, mas o isolamento por sistema não muda isso; documentar.
4. **`system_code` vs `system_id` no filtro**: o principal é o `systemCode`; a view pode expor `system_code` (join em `scos_system`) para filtrar direto, ou resolver `code→id` antes. Definir na proposta.
5. **Compatibilidade**: enquanto a change anterior não é ajustada, registrar o mesmo código em 2 sistemas ainda falha na UK antiga — a ordem de aplicação importa.

---

## 📎 Referências
- `flow-organization-boot/.../view/vw_login_context.sql` e `vw_authority_response.sql` (fonte das permissões)
- `grpc/.../interceptor/TokenAuthorizationInterceptor.java` (principal = systemCode)
- Change relacionada (a ser ajustada): `openspec/changes/permission-metadata-registry-enrichment/` (upsert `ON CONFLICT`)
- Spec existente: `openspec/specs/schema-permissions-multisistema/spec.md`

---
