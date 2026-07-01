# Atualização das Entidades JPA e Repositórios Conforme Liquibase v2

**Data**: 2026-07-01
**Status**: 🔄 Em Análise
**Tipo**: 🔧 Refatoração

---

## ⚠️ Princípio SRP — Uma Funcionalidade por Ideia

> **Regra**: Uma ideia = uma funcionalidade. Features independentes → arquivos separados.

- **Nome da funcionalidade**: `atualizacao-entidades-jpa-liquibase-v2`
- **Resumo em uma frase**: Sincronizar as entidades JPA e repositórios do módulo `scos-organization-domain` com o schema físico produzido pela change `adequacao-liquibase-domain-model-v2` (outbox, histórico de status auditável, dados fiscais, catálogo dinâmico de tipos, histórico de cargo, horários de trabalho).

**Checklist SRP**:
- [x] Esta ideia cobre exatamente uma funcionalidade — sincronizar a camada de domínio JPA ao schema v2
- [x] Não mistura features independentes no mesmo arquivo — mesmo precedente da rodada anterior (`atualizacao-models-dominio-conforme-banco`, 2026-06-08) e da própria change `adequacao-liquibase-domain-model-v2`: as ~6 áreas de domínio afetadas são consequência direta de uma única funcionalidade ("sincronizar JPA ao domain model v2"), decidido com o usuário
- [x] O nome é específico

---

## 1️⃣ Visão

### Problema
A change `adequacao-liquibase-domain-model-v2` (completa, 57/57 tarefas) recriou o schema físico com ~34 tabelas — 21 novas, ~10 modificadas, 3 removidas — mas foi **estritamente Liquibase**, sem tocar código Java (decisão explícita de escopo). O módulo `scos-organization-domain` ainda reflete o schema v1: faltam entidades JPA para todas as tabelas novas, sobram entidades para as 3 tabelas removidas, e as entidades existentes têm colunas renomeadas/adicionadas/trocadas por FK que não estão mapeadas. Qualquer tentativa de usar o Hibernate contra o banco atual falha ou usa colunas inexistentes.

Adicionalmente, o novo schema introduz uma restrição de **vocabulário fechado** via `CHECK` constraint que o código de domínio atual viola: `StatusCompany`/`StatusEmployee` têm o valor `DELETED`, `LoginStatus` tem `LOCKED`/`DELETED` — nenhum aceito pelos novos `CHECK`s (`ACTIVE/INACTIVE/DISABLED` para company/employee, `ACTIVE/INACTIVE/BLOCKED` para login). Os métodos `delete()` de `Company`/`Employee` e as regras `LoginDeletedRule`/`LoginLockedRule` quebrariam em runtime contra o banco novo.

### Objetivo
Todas as entidades JPA e repositórios de `scos-organization-domain` devem refletir exatamente o schema Liquibase v2 e o `domain_model.md`. Critério de sucesso: nenhum campo mapeado que não exista no banco, nenhuma tabela relevante sem entidade correspondente, `Employee`/`Company`/`Login` usando o padrão de histórico de status como fonte de verdade, zero erro de schema-validation do Hibernate.

### Fora de Escopo
- Alterações nas migrations Liquibase — já feitas em `adequacao-liquibase-domain-model-v2`, são a fonte da verdade
- Camadas `usecase`/`api`/`infrastructure` — hoje não referenciam nenhuma das entidades afetadas fora de `domain` (confirmado por busca no repositório), então o blast radius desta ideia é `scos-organization-domain` isolado. Casos de uso que consumirão as entidades novas (outbox, histórico de status, cadastro de motivos/tipos, dados fiscais) ficam para ideias futuras
- Rename do campo público `keycloakId` nos contratos OpenAPI/gRPC (`etc/api/organization/ScosOrganization_Login.yml`, `ScosOrganization_Integration.yml`) — só o campo interno (`Login.keycloakId` → `externalId`, `VwAuthorityResponse`, `AuthorityResponseOutput`, `AuthorityResponseMapper`) é renomeado aqui, para bater com a coluna `EXTERNAL_ID`. Mudança de contrato público é decisão separada (contrato antes do código)
- Seed de dados oficiais (`SCOS_LEGAL_NATURE`/`SCOS_CNAE` IBGE) — já decidido fora de escopo na change Liquibase
- Migração de dados — sistema sem produção, banco recriado do zero

---

## 2️⃣ Requisitos

### Funcionais — agrupados por área (espelha as capabilities da change Liquibase v2)

#### A. Histórico de status — redesenho do padrão de domínio (decisão tomada com o usuário)
`STATUS` em `SCOS_COMPANY`/`SCOS_EMPLOYEE`/`SCOS_LOGIN` passa a ser cache sincronizado por trigger a partir de `*_STATUS_HISTORY` — a aplicação não deve mais mutar `STATUS` diretamente.

- [ ] **RF-01**: Criar `CompanyStatusHistory`, `EmployeeStatusHistory`, `LoginStatusHistory` (entidades imutáveis — sem `UPDATED_AT`, padrão local `createdAt`+`userAt` como em `ProfileResource`, sem estender `BaseEntity`) com FKs para as 4 tabelas de motivo
- [ ] **RF-02**: Criar `ReasonActivate`, `ReasonInactivate`, `ReasonDisable`, `ReasonEnable` (entidades padrão de referência — `code`/`description`/`active`/`entityType`, estendem `BaseEntity`, `@Auditable`), compartilhando um enum `EntityType { COMPANY, EMPLOYEE, LOGIN }`
- [ ] **RF-03**: Redesenhar `Company`/`Employee`/`Login` — remover mutação direta de `status`; os métodos de transição (`activate`/`inactivate`/`disable`/`enable`) passam a criar o registro de histórico correspondente conforme a tabela de transição do domain model:
  - `activate(reasonActivateId)`: apenas a partir de `INACTIVE` → `ACTIVE`
  - `inactivate(reasonInactivateId)`: para `INACTIVE` (a partir de qualquer estado)
  - `disable(reasonDisableId)`: para `DISABLED`/`BLOCKED`
  - `enable(reasonEnableId)`: apenas a partir de `DISABLED`/`BLOCKED` → `ACTIVE` (distinto de `activate`)
  - Remover `delete()` — o conceito `DELETED` não existe mais no vocabulário fechado; `INACTIVE` passa a ser o estado terminal definitivo
- [ ] **RF-04**: Remover `DELETED` de `StatusCompany`/`StatusEmployee`; remover `DELETED`/`LOCKED` de `LoginStatus`, renomear para `BLOCKED` (alinhado ao `CHECK`); atualizar/remover `LoginDeletedRule` e renomear `LoginLockedRule` → regra de `BLOCKED`
- [ ] **RF-05**: Criar repositórios `CompanyStatusHistoryRepository`, `EmployeeStatusHistoryRepository`, `LoginStatusHistoryRepository`, `ReasonActivateRepository`, `ReasonInactivateRepository`, `ReasonDisableRepository`, `ReasonEnableRepository` (padrão `ProfileResourceRepository`: `BaseJpaRepository` + `JpaSpecificationExecutor` + `QuerydslPredicateExecutor`)

#### B. Organização (extensão)
- [ ] **RF-06**: `Position` — adicionar `isTrustPosition` (boolean)
- [ ] **RF-07**: `Employee` — adicionar `contractType` (novo enum `EmployeeContractType`: `CLT`/`PJ`/`ESTAGIO`/`TEMPORARIO`), `probationEndDate`
- [ ] **RF-08**: `Company` — adicionar `legalNature` (`@ManyToOne` → `LegalNature`, nullable), `cnaePrincipal` (`@ManyToOne` → `Cnae`, nullable), `stateRegistration`, `municipalRegistration`

#### C. Histórico de cargo (novo)
- [ ] **RF-09**: Criar `ReasonPositionChange` (padrão referência, `BaseEntity`, `@Auditable`) + `ReasonPositionChangeRepository`
- [ ] **RF-10**: Criar `EmployeePositionHistory` (imutável — sem `UPDATED_AT`) com FKs para `Employee`, `Position`, `ReasonPositionChange` + `EmployeePositionHistoryRepository`
- [ ] **RF-11**: Criar `PositionWorkSchedule`, `EmployeeWorkSchedule` (`BaseEntity`, `@Auditable`) com FK para `Position`/`Employee` e enum compartilhado `DayOfWeek` (`MONDAY`..`SUNDAY`) + repositórios

#### D. Outbox e integrações externas (substitui Keycloak dedicado)
- [ ] **RF-12**: Remover `IntegrationKeycloak`, `IntegrationKeycloakLog`, `IntegrationMessageInvalid` e seus repositórios (tabelas removidas no v2)
- [ ] **RF-13**: Criar `OutboxTopic` (PK `String` = `TOPIC`, enum `OutboxBackend`: `PGMQ`/`KAFKA`/`DIRECT_API`, `BaseEntity`, `@Auditable`) + repositório
- [ ] **RF-14**: Criar `OutboxEvent` (`payload`/`responseData` JSONB via `@Type(JsonBinaryType.class)` do Hypersistence Utils, enum `OutboxEventStatus`: `PENDING`/`PROCESSING`/`PROCESSED`/`FAILED`) — **atenção**: `CREATED_AT` tem `DEFAULT NOW()` mas `UPDATED_AT`/`USER_AT` são `NULL`, não se encaixa no padrão `BaseEntity` (que assume ambos `NOT NULL`); mapear campos diretamente, sem estender `BaseEntity` — validar durante o `/propose`
- [ ] **RF-15**: Criar `OutboxEventLog`, `OutboxEventDeadLetter` (imutáveis, `USER_AT` nullable — variação do padrão `ProfileResource` onde `userAt` é `NOT NULL`; validar durante o `/propose`)
- [ ] **RF-16**: `Login` — renomear `keycloakId` (`UUID`) → `externalId`, coluna `EXTERNAL_ID`; propagar rename para `VwAuthorityResponse.keycloakId` → `externalId` (coluna da view também foi renomeada no v2) e `AuthorityResponseOutput`/`AuthorityResponseMapper`

#### E. Acesso e permissões (extensão)
- [ ] **RF-17**: Criar `LoginProfile` (PK composta `loginId`+`profileId`, imutável, padrão `ProfileResource`) + repositório

#### F. Dados fiscais da empresa
- [ ] **RF-18**: Criar `LegalNature`, `Cnae` (**atenção**: só têm `CODE`/`DESCRIPTION`/`CREATED_AT` — nem `UPDATED_AT` nem `USER_AT`; não se encaixam nem no padrão `BaseEntity` nem no padrão `ProfileResource` local — entidade mínima própria, mapear durante `/propose`)
- [ ] **RF-19**: Criar `CompanyCnaeSecondary` (PK composta `companyId`+`cnaeId`, imutável, padrão `ProfileResource`) + repositório

#### G. Catálogo de tipos de endereço/contato (substitui texto livre)
- [ ] **RF-20**: Criar `AddressType`, `ContactType` (padrão referência, `entityType` reaproveitando enum `EntityType` do item A, `BaseEntity`, `@Auditable`) + repositórios
- [ ] **RF-21**: `EmployeeAddress`/`CompanyAddress` — trocar `type: String` por `addressType: AddressType` (`@ManyToOne`, FK `ADDRESS_TYPE_ID`); corrigir `EmployeeAddress.number` de `long` para `int` (alinhado ao domain model e a `CompanyAddress.number`)
- [ ] **RF-22**: `EmployeeContact`/`CompanyContact` — trocar `type: String` por `contactType: ContactType` (`@ManyToOne`, FK `CONTACT_TYPE_ID`)

### Não-Funcionais
- [ ] **RNF-01**: Nenhuma alteração de schema no banco — só código Java muda
- [ ] **RNF-02**: Hibernate schema-validation deve passar sem erros contra o banco gerado pela v2
- [ ] **RNF-03**: Toda entidade nova segue os padrões já estabelecidos (ver `🔒 Padrões Obrigatórios` da ideia anterior — herança `BaseEntity` vs. imutável local, `@Auditable`/`auditRead`, PK composta via `@EmbeddedId`, VOs da foundation, enums `EnumType.STRING`), exceto onde a estrutura do banco genuinamente não se encaixa (outbox, fiscal — sinalizado acima)
- [ ] **RNF-04**: Repositórios seguem `BaseJpaRepository` + `JpaSpecificationExecutor` + `QuerydslPredicateExecutor`, sem classe de implementação (`ProfileResourceRepository` como referência)

---

## 3️⃣ Arquitetura

### Componentes Afetados
```
scos-organization-domain/src/main/java/br/com/sawcunhaos/organization/domain/
├── corporate/
│   ├── company/internal/       : Company (mod), CompanyAddress (mod), CompanyContact (mod),
│   │                              LegalNature (novo), Cnae (novo), CompanyCnaeSecondary (novo)
│   ├── employee/internal/      : Employee (mod), EmployeeAddress (mod), EmployeeContact (mod),
│   │                              EmployeeContractType (novo enum), ReasonPositionChange (novo),
│   │                              EmployeePositionHistory (novo)
│   └── position/internal/      : Position (mod — isTrustPosition),
│                                  PositionWorkSchedule (novo), EmployeeWorkSchedule (novo — talvez em employee/),
│                                  DayOfWeek (novo enum compartilhado)
├── access/
│   ├── login/internal/         : Login (mod — externalId), LoginStatus (mod — remove DELETED/LOCKED, add BLOCKED),
│   │                              VwAuthorityResponse (mod — externalId), rules/* (mod)
│   ├── profile/internal/       : LoginProfile (novo)
│   ├── status/internal/        : CompanyStatusHistory, EmployeeStatusHistory, LoginStatusHistory (novos),
│                                  ReasonActivate, ReasonInactivate, ReasonDisable, ReasonEnable (novos),
│                                  EntityType (novo enum compartilhado)
│   ├── resource/internal/      : AddressType, ContactType (novos — ou pacote catalog/ próprio)
│   └── integration/internal/   : IntegrationKeycloak*, IntegrationMessageInvalid — REMOVER
└── outbox/internal/            : OutboxTopic, OutboxEvent, OutboxEventLog, OutboxEventDeadLetter (novo pacote)
```
> Estrutura de pacotes exata (ex: onde fica `status history`, `outbox`, `catálogo de tipos`) fica para decisão no `/propose` — o layout acima é ilustrativo, seguindo o agrupamento por bounded context já usado em `access/`, `corporate/`.

### Decisões Técnicas
| Decisão | Escolha | Alternativa Descartada | Motivo |
|---------|---------|------------------------|--------|
| Escopo da ideia | Uma ideia só, todas as áreas | Split por bounded context | Mesmo precedente da change Liquibase v2 e da rodada anterior de sync JPA — decidido com o usuário |
| `STATUS` em Company/Employee/Login | Métodos de domínio passam a criar registro de histórico (fonte de verdade) em vez de mutar `status` | Manter mutação direta e só mapear as entidades novas sem integrar | Decidido com o usuário — reflete o modelo real ponta a ponta desde já, evita uma segunda rodada de refatoração quando os use cases forem implementados |
| `DELETED`/`LOCKED` nos enums de status | Remover — `INACTIVE` vira o estado terminal | Manter no Java mas nunca persistir | Decidido com o usuário — alinhamento estrito ao vocabulário do `CHECK`, elimina estado "fantasma" que não existe no banco |
| Rename `keycloakId` → `externalId` | Só o campo interno (entidade/DTO/mapper) | Also renomear contrato público OpenAPI/gRPC | Decidido com o usuário — contrato é mudança separada, "contrato antes do código" |
| PK/imutabilidade das entidades novas | Seguir padrão `ProfileResource` (local `createdAt`/`userAt`, sem `BaseEntity`) para tabelas sem `UPDATED_AT` | Criar uma superclasse `ImmutableEntity` nova | Nenhum novo padrão deve ser introduzido — mesma regra da rodada anterior |

### Banco de Dados
- **Impacto**: ❌ Não — nenhuma migration nova, apenas código Java

---

## 4️⃣ Implementação

*Detalhamento de arquivos/tarefas fica para a fase de `/openspec-propose`.*

### Riscos e Edge Cases
1. `OutboxEvent`/`OutboxEventLog`/`OutboxEventDeadLetter` têm `UPDATED_AT`/`USER_AT` nullable (ou ausentes) — não se encaixam no padrão `BaseEntity` usado em todo o resto do domínio. Precisa decisão explícita de mapeamento no `/propose` (campos diretos vs. alguma variação)
2. `SCOS_LEGAL_NATURE`/`SCOS_CNAE` não têm `USER_AT` nem `UPDATED_AT` — tabelas de seed puro, padrão ainda mais mínimo que `ProfileResource`
3. Redesenho de `activate`/`inactivate`/`disable`/`enable` como criadores de histórico muda a assinatura pública desses métodos — nenhum caller existe hoje fora do próprio módulo `domain` (confirmado), então o impacto real é zero nesta rodada, mas os métodos antigos claramente documentavam guards (`SCOS_COMPANY_007`, `SCOS_EMPLOYEE_001`) que precisam de nova lógica de guard client-side além do `CHECK` de transição do banco
4. `LoginStatus.LOCKED` → `BLOCKED` também exige revisar `SCOS_LOGIN_011`/`SCOS_LOGIN_012` (mensagens de erro) e a remoção de `LoginDeletedRule` pode deixar `@ScosRule(1)` com buraco na ordem — revisar `ScosRule` de todas as regras remanescentes
5. `EmployeeAddress.number` está como `long` hoje; banco é `INT` — nunca deu erro porque Hibernate aceita widening, mas é uma divergência de tipo que vale corrigir junto
6. `CHK_*_STATUS_TRANSITION` (banco) e o guard client-side (Java) precisam expressar exatamente a mesma regra de transição — dessincronia gera exceção genérica do Postgres em vez de `ScosException` amigável

---

## 📎 Referências
- Domain model: `etc/database/domain_model.md` (fonte da verdade do schema v2)
- Migrations: `scos-organization-boot/src/main/resources/db/changelog/` (produzidas por `adequacao-liquibase-domain-model-v2`)
- `checks.yml`: `scos-organization-boot/src/main/resources/db/changelog/checks/checks.yml` — vocabulário fechado de `STATUS`/`TYPE`/`ENTITY_TYPE`/`BACKEND`/`DAY_OF_WEEK`
- Rodada anterior de sync JPA (arquivada): `openspec/changes/archive/2026-06-08-atualizacao-models-dominio-conforme-banco/`
- Change Liquibase que originou este gap: `openspec/changes/adequacao-liquibase-domain-model-v2/`

---
