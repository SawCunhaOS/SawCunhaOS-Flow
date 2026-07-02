# Histórico de Status e Motivos — Company/Employee/Login

**Data**: 2026-07-01
**Status**: 🔄 Em Análise
**Tipo**: 🆕 Nova Feature

---

## ⚠️ Princípio SRP — Uma Funcionalidade por Ideia

> **Regra**: Uma ideia = uma funcionalidade. Features independentes → arquivos separados.

- **Nome da funcionalidade**: `historico-status-motivos-organization`
- **Resumo em uma frase**: Atualizar os contratos OpenAPI de `Company`, `Employee` e `Login` para expor as 4 transições de status simétricas (activate/inactivate/disable/enable) com motivo obrigatório e histórico auditável, refletindo `SCOS_REASON_ACTIVATE/INACTIVATE/DISABLE/ENABLE` e `SCOS_*_STATUS_HISTORY` (schema v2).

**Checklist SRP**:
- [x] Esta ideia cobre exatamente uma funcionalidade — modelo de transição de status com motivo, aplicado simetricamente aos 3 agregados
- [x] Não mistura features independentes no mesmo arquivo — dados fiscais, cargo/contrato, jornada, catálogo de endereço/contato, outbox e perfil adicional são ideias próprias
- [x] O nome é específico

---

## 1️⃣ Visão

### Problema
O schema v2 já redesenhou `Company`/`Employee`/`Login` para tratar `STATUS` como cache sincronizado por trigger a partir de tabelas de histórico (`*_STATUS_HISTORY`), com 4 transições nomeadas (`activate`, `inactivate`, `disable`, `enable`) e motivo obrigatório vindo de 4 catálogos de referência (`REASON_ACTIVATE/INACTIVATE/DISABLE/ENABLE`, cada um filtrado por `ENTITY_TYPE`). A camada de domínio JPA já foi sincronizada com isso (`atualizacao-entidades-jpa-liquibase-v2`, completo).

O contrato público, porém, ainda está no modelo v1:
- `Company`/`Employee`: só têm `/enable` (activate) e `/disable` (inactivate) — sem rota para `disable`→DISABLED nem `enable`-de-volta — e sem request body (nenhum `reasonId`).
- `Login`: só tem `/block` (disable→BLOCKED) e `/unblock` (enable-de-volta) — sem `/enable`/`/disable` (activate/inactivate) — também sem request body.
- `deleteLogin` (UC-066) dispara exclusão definitiva via Saga Keycloak, mas `LOGIN.STATUS` não tem mais `DELETED` no vocabulário fechado — o endpoint não faz mais sentido.
- Não existe endpoint de consulta ao histórico de status em nenhum dos 3 agregados.
- **Achado na revisão**: `CreateCompanyRequest`/`CreateEmployeeRequest`/`CreateLoginRequest`/`CreateEmployeeLoginRequest` (schemas já existentes) não têm nenhum campo de motivo. Mas a trigger `TRG_BEFORE_INSERT_*_STATUS_HISTORY` exige `REASON_ACTIVATE_ID` preenchido **mesmo na primeira linha de histórico** (criação, `PREVIOUS_STATUS` nulo, `STATUS = ACTIVE`) — sem isso, o próprio `INSERT` do registro de criação falha. Os 4 endpoints de criação (`createCompany`, `createEmployee`, `createLogin`, `createEmployeeLogin`) precisam ganhar `reasonActivateId` obrigatório, não só as transições depois.
- **Achado na revisão**: `DELETE /companies/{id}` (UC-008) e `DELETE /employees/{id}` (UC-043) já dizem na própria descrição "Exclusão lógica. Registro permanece com status DISABLED" — ou seja, **já são** a transição `disable`→DISABLED, só sem `reasonId` e com o verbo/rota errado (semanticamente não é remoção). Não faltava criar `/block` do zero: falta reformular esse `DELETE` existente.

### Objetivo
Os 3 agregados (`Company`, `Employee`, `Login`) expõem as mesmas 4 rotas de transição, todas exigindo `reasonId` (do catálogo correspondente à transição e ao `entityType` do agregado) e `observation` opcional; os 4 endpoints de criação exigem `reasonActivateId`; mais um endpoint de consulta paginada ao histórico. Critério de sucesso: nenhuma linha de `*_STATUS_HISTORY` (criação ou transição) possível sem motivo rastreável, contrato sem operação que viole o vocabulário fechado do banco (`deleteLogin` removido) nem transição inválida pela matriz de estados.

### Matriz de transições válidas (`TRG_BEFORE_INSERT_*_STATUS_HISTORY`, `domain_model.md`)

| PREVIOUS_STATUS | STATUS | Motivo exigido | Rota |
|---|---|---|---|
| *(nenhum — criação)* | ACTIVE | `REASON_ACTIVATE_ID` | `POST /companies`, `/employees`, `/logins`, `/employees/{id}/logins` |
| INACTIVE | ACTIVE | `REASON_ACTIVATE_ID` | `/enable` |
| DISABLED/BLOCKED | ACTIVE | `REASON_ENABLE_ID` | `/unblock` |
| ACTIVE | INACTIVE | `REASON_INACTIVATE_ID` | `/disable` |
| DISABLED/BLOCKED | INACTIVE | `REASON_INACTIVATE_ID` | `/disable` |
| **ACTIVE (só)** | DISABLED/BLOCKED | `REASON_DISABLE_ID` | `/block` |

> **Importante — corrige suposição inicial**: `disable`/`/block` só é válido a partir de `ACTIVE`. `INACTIVE → DISABLED` **não é transição válida** (premissa explícita do `domain_model.md` — bloqueio temporário não faz sentido sobre algo já encerrado definitivamente). Chamar `/block` numa empresa/funcionário/login já `INACTIVE` deve retornar `4XX`, não é "qualquer→DISABLED" como uma leitura apressada do endpoint sugere.

### Fora de Escopo
- Dados fiscais de `Company` (`LEGAL_NATURE`/`CNAE`) — ideia própria
- Histórico de cargo (`EMPLOYEE_POSITION_HISTORY`/`REASON_POSITION_CHANGE`) — ideia própria, embora siga o mesmo espírito de "motivo obrigatório + histórico"
- Jornada de trabalho — ideia própria
- Catálogo de tipo de endereço/contato — ideia própria
- Outbox — ideia própria
- Perfil adicional de login (`LOGIN_PROFILE`) — ideia própria
- Implementação de use case/domain — já feita na rodada JPA anterior; aqui só o contrato

---

## 2️⃣ Requisitos

### Mapeamento transição de domínio → rota (decidido com o usuário)

| Transição (domínio) | Rota `Company` | Rota `Employee` | Rota `Login` |
|---|---|---|---|
| `activate` (INACTIVE→ACTIVE) | `PUT /companies/{id}/enable` (já existe) | `PUT /employees/{id}/enable` (já existe) | `PUT /logins/{id}/enable` (**novo**) |
| `inactivate` (qualquer→INACTIVE) | `PUT /companies/{id}/disable` (já existe) | `PUT /employees/{id}/disable` (já existe) | `PUT /logins/{id}/disable` (**novo**) |
| `disable` (**só ACTIVE**→DISABLED/BLOCKED) | `PUT /companies/{id}/block` (**renomeado de `DELETE /companies/{id}`, UC-008**) | `PUT /employees/{id}/block` (**renomeado de `DELETE /employees/{id}`, UC-043**) | `PUT /logins/{id}/block` (já existe) |
| `enable` (DISABLED/BLOCKED→ACTIVE) | `PUT /companies/{id}/unblock` (**novo, não existia nem como delete**) | `PUT /employees/{id}/unblock` (**novo, não existia nem como delete**) | `PUT /logins/{id}/unblock` (já existe) |

> Atenção: o nome da rota não bate 1:1 com o nome da transição do domínio nos casos `disable`/`enable` (mapeados para `/block`/`/unblock` por consistência com o padrão já usado no `Login`). Documentar isso claramente na `description` de cada `operationId`.
>
> `DELETE /companies/{id}`/`DELETE /employees/{id}` (UC-008/UC-043) somem do contrato — viram `PUT .../block`, decidido com o usuário (renomear em vez de manter `DELETE` com body ou duplicar rota). `x-authorize` migra de `DELETE_COMPANY`/`DELETE_EMPLOYEE` para os novos `BLOCK_COMPANY`/`BLOCK_EMPLOYEE`.

### Funcionais
- [ ] **RF-01**: Adicionar `requestBody` `{ reasonId: integer (required), observation: string (optional) }` às rotas já existentes `/companies/{id}/enable`, `/disable`, `/employees/{id}/enable`, `/disable`
- [ ] **RF-02**: Renomear `DELETE /companies/{id}` (UC-008) → `PUT /companies/{id}/block` e `DELETE /employees/{id}` (UC-043) → `PUT /employees/{id}/block`, com o mesmo request body (já eram soft-delete para `DISABLED`, só precisavam de `reasonId` e verbo/rota corretos); criar `PUT /companies/{id}/unblock`, `PUT /employees/{id}/unblock` (esses sim novos — não existiam nem como delete)
- [ ] **RF-03**: Criar `PUT /logins/{id}/enable`, `PUT /logins/{id}/disable`; adicionar o mesmo request body às rotas existentes `/logins/{id}/block`, `/unblock`
- [ ] **RF-04**: Remover `DELETE /logins/{id}` (`deleteLogin`, UC-066) do contrato
- [ ] **RF-05**: Criar CRUD de referência para `ReasonActivate`, `ReasonInactivate`, `ReasonDisable`, `ReasonEnable` — schema `{ code, description, entityType (COMPANY/EMPLOYEE/LOGIN), active }`, endpoints list/create/get/update/enable/disable seguindo o padrão de `/v1/departments`, filtráveis por `entityType`
- [ ] **RF-06**: Criar `GET /companies/{id}/status-history`, `GET /employees/{id}/status-history`, `GET /logins/{id}/status-history` — paginado, retornando `status`, `previousStatus`, o motivo associado (qualquer um dos 4 FKs que estiver preenchido), `observation`, `userAt`, `createdAt`
- [ ] **RF-07**: `x-authorize` novo por rota nova (`BLOCK_COMPANY`, `UNBLOCK_COMPANY`, `BLOCK_EMPLOYEE`, `UNBLOCK_EMPLOYEE`, `ENABLE_LOGIN`, `DISABLE_LOGIN`) — adicionar ao `ScosOrganizationPermission` quando for pra código. `ENABLE_LOGIN`/`DISABLE_LOGIN` são permissões granulares novas (uma por ação), seguindo o padrão de `Company`/`Employee` — decidido com o usuário não reaproveitar `UPDATE_LOGIN_STATUS` (permissão única hoje usada só por `block`/`unblock`, que continuam como estão, sem mudança de permissão)
- [ ] **RF-08** (achado na revisão): Adicionar `reasonActivateId` (obrigatório) a `CreateCompanyRequest`, `CreateEmployeeRequest`, `CreateLoginRequest`, `CreateEmployeeLoginRequest` — a primeira linha de `*_STATUS_HISTORY` (criação, `STATUS=ACTIVE`) exige o motivo assim como qualquer outra transição
- [ ] **RF-09** (achado na revisão): `/block` (`disable`) só é válido quando o status atual é `ACTIVE`; chamar em `INACTIVE` deve retornar `4XX` — documentar essa restrição na `description` do endpoint (banco não tem `CHECK` que impeça a chamada da API, só a trigger de histórico rejeitaria a combinação `PREVIOUS_STATUS=INACTIVE`/`REASON_DISABLE_ID`, então o use case precisa validar antes de deixar o erro genérico de banco estourar)

### Não-Funcionais
- [ ] **RNF-01**: Contrato antes do código — YAML primeiro, segue convenção do projeto
- [ ] **RNF-02**: Nenhuma alteração de schema no banco — schema v2 já existe e já suporta isso
- [ ] **RNF-03**: Respostas de erro seguem `$ref` padrão do `ScosComponents.yml`
- [ ] **RNF-04** (achado na revisão): DTOs seguem o padrão SCOS já usado em todo o arquivo — sem exceção nenhuma lista do contrato atual foge disso, nem catálogos pequenos: entrada via `CreateXRequest`/`UpdateXRequest` (com `x-required-message`/`x-empty-message` por campo obrigatório), saída singular via `GetXResponse { data: $ref X }`, saída em lista via `GetAllXResponse { data: array, paginatedDTO: $ref ScosComponents#/components/schemas/ScosPaginated }` com o parâmetro `paginationFilter` (query, obrigatório) — aplica-se a `GET /reason-activate` (e as outras 3 tabelas de motivo) e a `GET .../status-history` nos 3 agregados

---

## 3️⃣ Arquitetura

### Componentes Afetados
```
etc/api/organization/
├── ScosOrganization_Company.yml    : DELETE→block (renomeado, mod), unblock (novo), body em enable/disable (mod),
│                                     reasonActivateId em CreateCompanyRequest (mod), status-history (novo)
├── ScosOrganization_Employee.yml   : idem Company + reasonActivateId em CreateEmployeeRequest (mod)
├── ScosOrganization_Login.yml      : rotas enable/disable (novo), body em block/unblock (mod),
│                                     reasonActivateId em CreateLoginRequest/CreateEmployeeLoginRequest (mod),
│                                     status-history (novo), remove deleteLogin (mod)
└── [decisão pendente] onde ficam os 4 catálogos de motivo:
    arquivo próprio (ex: ScosOrganization_Reason.yml) vs. dentro de cada
    arquivo consumidor vs. ScosComponents.yml — decidir no /propose
```

### Detalhamento de Campos por Arquivo YAML

| Arquivo YAML | Schema/Path | Campo | Tipo | Nota |
|---|---|---|---|---|
| `ScosOrganization_Company.yml` | `PUT /companies/{id}/enable`, `/disable` (mod) — novo requestBody | `reasonId`, `observation` | `integer (format: int64)` obrigatório / `string` opcional | RF-01 |
| `ScosOrganization_Company.yml` | `DELETE /companies/{id}` → `PUT /companies/{id}/block` (renomeado); `PUT /companies/{id}/unblock` (novo) | `reasonId`, `observation` | idem acima | mesmo body de enable/disable; `x-authorize` `DELETE_COMPANY`→`BLOCK_COMPANY` |
| `ScosOrganization_Company.yml` | `CreateCompanyRequest` (mod) | `reasonActivateId` | `integer (format: int64)` | obrigatório (RF-08) |
| `ScosOrganization_Company.yml` | `GET /companies/{id}/status-history` (novo) | `status`, `previousStatus`, `reason*Id` (o preenchido), `observation`, `userAt`, `createdAt` | enum / enum / int64 / string / string / datetime | paginado, somente leitura |
| `ScosOrganization_Employee.yml` | `PUT /employees/{id}/enable`, `/disable` (mod) — novo requestBody | `reasonId`, `observation` | idem `Company` | RF-01 |
| `ScosOrganization_Employee.yml` | `DELETE /employees/{id}` → `PUT /employees/{id}/block` (renomeado); `PUT /employees/{id}/unblock` (novo) | `reasonId`, `observation` | idem acima | `x-authorize` `DELETE_EMPLOYEE`→`BLOCK_EMPLOYEE` |
| `ScosOrganization_Employee.yml` | `CreateEmployeeRequest` (mod) | `reasonActivateId` | `integer (format: int64)` | obrigatório (RF-08) |
| `ScosOrganization_Employee.yml` | `GET /employees/{id}/status-history` (novo) | idem `Company` | idem `Company` | paginado |
| `ScosOrganization_Login.yml` | `PUT /logins/{id}/enable`, `/disable` (novos) | `reasonId`, `observation` | idem `Company` | fecha as 4 transições no Login |
| `ScosOrganization_Login.yml` | `PUT /logins/{id}/block`, `/unblock` (mod) — novo requestBody | `reasonId`, `observation` | idem `Company` | já existiam, ganham body |
| `ScosOrganization_Login.yml` | `DELETE /logins/{id}` (`deleteLogin`) | — | — | **removido** do contrato (RF-04) |
| `ScosOrganization_Login.yml` | `CreateLoginRequest`, `CreateEmployeeLoginRequest` (mod) | `reasonActivateId` | `integer (format: int64)` | obrigatório (RF-08) |
| `ScosOrganization_Login.yml` | `GET /logins/{id}/status-history` (novo) | idem `Company` | idem `Company` | paginado |
| local a decidir (`/propose`) | `ReasonActivate`, `ReasonInactivate`, `ReasonDisable`, `ReasonEnable` (novos) | `code`, `description`, `entityType` (`COMPANY`/`EMPLOYEE`/`LOGIN`), `active` | string / string / enum (3 valores) / boolean | 4 catálogos, mesmo padrão `AddressType`; CRUD list/create/get/update/enable/disable. **Atenção**: enum próprio, 3 valores — confirmado pelo `CHECK` real (`chk_reason_*_entity_type`, `checks.yml`). **Não** é o mesmo `entityType` (2 valores, sem `LOGIN`) de `AddressType`/`ContactType` (idea `catalogo-tipo-endereco-contato`) — apesar da idea JPA (`atualizacao-entidades-jpa-liquibase-v2`, RF-20) sugerir reaproveitar "o enum `EntityType` do item A", isso só vale pro enum Java interno; no OpenAPI precisam ser 2 schemas de enum distintos, senão o contrato aceitaria `LOGIN` num `AddressType` e só falharia depois no `CHECK` do banco |

### Fluxo Principal
```
PUT /companies/{id}/disable {reasonId, observation?}
  → valida reasonId pertence a REASON_INACTIVATE + entityType=COMPANY + active=true
  → usecase → Company.inactivate(reasonId, observation)
  → grava CompanyStatusHistory (trigger sincroniza SCOS_COMPANY.STATUS)
```

### Decisões Técnicas
| Decisão | Escolha | Alternativa Descartada | Motivo |
|---------|---------|------------------------|--------|
| Rotas faltantes Company/Employee | Reaproveita nomes `/block`/`/unblock` do Login | Nomes próprios (`/deactivate` etc) | Decidido com o usuário — consistência entre os 3 agregados |
| `DELETE /companies/{id}`/`/employees/{id}` (UC-008/UC-043) | Renomeia pra `PUT .../block` (achado na revisão) | Mantém `DELETE` e adiciona body / mantém os dois convivendo | Decidido com o usuário — já eram a transição `disable`, só com verbo/rota semanticamente errados; renomear evita duas portas pro mesmo destino |
| Rotas faltantes Login | Adiciona `/enable`/`/disable` | Login só com block/unblock (INACTIVE só via cascade) | Decidido com o usuário — fecha as 4 transições também no Login |
| `reasonId` nas transições | Obrigatório sempre, nos 3 agregados | Obrigatório só em disable/inactivate; opcional sempre | Decidido com o usuário — rastreabilidade completa |
| `deleteLogin` (UC-066) | Remove do contrato | Vira hard delete real / mantém como está | Decidido com o usuário — vocabulário fechado não tem mais `DELETED` |
| Consulta de histórico | Expõe `GET .../status-history` nos 3 | Só banco, sem endpoint | Decidido com o usuário — auditoria acessível via API |
| Permissão de `enable`/`disable` no Login | Granular (`ENABLE_LOGIN`/`DISABLE_LOGIN`, novas) | Reaproveitar `UPDATE_LOGIN_STATUS` (padrão hoje usado por `block`/`unblock`) | Decidido com o usuário — consistência com o padrão granular já usado em `Company`/`Employee`; `block`/`unblock` do Login continuam com `UPDATE_LOGIN_STATUS`, sem mudança |

### Banco de Dados
- **Impacto**: ❌ Não — schema já existe (`adequacao-liquibase-domain-model-v2`, completo). Só contrato OpenAPI muda.

---

## 4️⃣ Implementação

*Detalhamento de arquivos/tarefas fica para a fase de `/openspec-propose`.*

### Riscos e Edge Cases
1. Nome da rota ≠ nome da transição de domínio nos casos `disable`/`enable` (mapeados para `/block`/`/unblock`) — pode confundir quem lê o contrato sem contexto; documentar bem na `description`
2. OpenAPI não valida FK — `reasonId` pertencer ao catálogo certo (`entityType` + tipo de transição) é regra de negócio, não de contrato; falha vira erro genérico se não tratada explicitamente no use case
3. Local de definição dos 4 catálogos de motivo (arquivo próprio vs. `ScosComponents.yml` vs. duplicado por consumidor) — decidir no `/propose`
4. `status-history` é somente leitura — nenhuma escrita direta, só populado pelas próprias transições; garantir que isso fique claro no contrato
5. (achado na revisão) `reasonActivateId` obrigatório nas 4 rotas de criação — sem isso o `INSERT` na tabela de histórico falha na trigger, quebrando a criação do próprio agregado; fácil de esquecer porque não parece uma "transição de status" à primeira vista
6. (achado na revisão) `/block` só é válido a partir de `ACTIVE` — `INACTIVE → DISABLED`/`BLOCKED` não existe na matriz de transições; o use case precisa rejeitar antes do banco, senão o erro que sobe é genérico (constraint da trigger), não um `ScosException` amigável
7. Não existe transição para o mesmo status (`ACTIVE→ACTIVE` etc.) — chamar `/enable` num agregado já `ACTIVE` deve ser rejeitado explicitamente pelo use case, mesma lógica do risco #6
8. (achado na revisão) Renomear `DELETE /companies/{id}`/`/employees/{id}` para `PUT .../block` é breaking change de verbo E rota — qualquer client que já chame `DELETE` quebra; `x-authorize` também migra (`DELETE_COMPANY`/`DELETE_EMPLOYEE` → `BLOCK_COMPANY`/`BLOCK_EMPLOYEE`), então permissões atribuídas a perfis existentes por código antigo deixam de valer e precisam ser reatribuídas

---

## 📎 Referências
- Domain model: `etc/database/domain_model.md`
- `checks.yml`: `scos-organization-boot/src/main/resources/db/changelog/checks/checks.yml`
- Sync JPA (completo): `openspec/changes/atualizacao-entidades-jpa-liquibase-v2/`
- Migrations v2: `openspec/changes/adequacao-liquibase-domain-model-v2/`

---
