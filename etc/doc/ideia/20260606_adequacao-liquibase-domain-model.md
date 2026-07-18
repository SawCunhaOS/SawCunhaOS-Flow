# Adequação do Liquibase ao Domain Model

**Data**: 2026-06-06  
**Status**: 🔄 Em Análise  
**Tipo**: 🗄️ Banco de Dados

---

## ⚠️ Princípio SRP — Uma Funcionalidade por Ideia

> **Regra**: Uma ideia = uma funcionalidade. Features independentes → arquivos separados.

- **Nome da funcionalidade**: `adequacao-liquibase-domain-model`
- **Resumo em uma frase**: Reescrever todas as migrations Liquibase para que o schema físico gerado seja exatamente o que o domain model especifica — nem mais, nem menos.

**Checklist SRP**:
- [x] Esta ideia cobre exatamente uma funcionalidade
- [x] Não mistura features independentes no mesmo arquivo
- [x] O nome é específico (não genérico como "melhorar módulo X")

---

## 1️⃣ Visão

### Problema
As migrations Liquibase existentes divergem do domain model (`etc/database/domain_model.md`). O schema físico resultante não implementa corretamente o design arquitetural — há tabelas ausentes, colunas com nullability errada, tamanhos de VARCHAR incorretos, campos extras sem correspondência no modelo, UKs ausentes e um modelo de permissões obsoleto (`SCOS_PERMISSION`). Qualquer desenvolvimento parte de uma base incorreta.

### Objetivo
Reescrever as migrations Liquibase para que `liquibase update` em banco limpo produza exatamente as 17 tabelas do domain model, com todas as colunas, tipos, tamanhos, nullability, UKs, FKs e constraints corretos. Escopo: **somente Liquibase** — nenhum outro arquivo é alterado. Sistema sem versão em produção — reescrita limpa, banco recriado do zero.

### Fora de Escopo
- Entidades JPA — impacto secundário, ideia separada
- Alteração do domain model
- Migração de dados (não há dados em produção)
- Qualquer código Java, YAML de aplicação ou outro arquivo fora de `db/changelog/`

---

## 2️⃣ Requisitos

### Funcionais

#### Tabelas a criar (ausentes no Liquibase)

- [ ] **RF-01**: Criar `SCOS_SYSTEM`
  - `SYSTEM_ID UUID PK` (gerado automaticamente)
  - `CODE VARCHAR(25) UK NOT NULL`
  - `DESCRIPTION VARCHAR(200) NOT NULL`
  - `SECRET_KEY TEXT NOT NULL`
  - `STATUS VARCHAR(50) NOT NULL`
  - `CREATED_AT TIMESTAMPTZ NOT NULL DEFAULT NOW()`
  - `UPDATED_AT TIMESTAMPTZ NOT NULL`
  - `USER_AT VARCHAR(255) NOT NULL`

- [ ] **RF-02**: Criar `SCOS_RESOURCE`
  - `RESOURCE_ID UUID PK` (gerado automaticamente)
  - `SYSTEM_ID UUID FK NOT NULL → SCOS_SYSTEM`
  - `CODE VARCHAR(50) UK NOT NULL`
  - `DESCRIPTION VARCHAR(255) NOT NULL`
  - `ACTIVE BOOLEAN NOT NULL DEFAULT TRUE`
  - `CREATED_AT TIMESTAMPTZ NOT NULL DEFAULT NOW()`
  - `UPDATED_AT TIMESTAMPTZ NOT NULL`
  - `USER_AT VARCHAR(255) NOT NULL`
  - `IDX_SYSTEM_ID_SCOS_RESOURCE` em `SYSTEM_ID`

- [ ] **RF-03**: Criar `SCOS_PROFILE_RESOURCE`
  - `PROFILE_ID BIGINT PK,FK NOT NULL → SCOS_PROFILE`
  - `RESOURCE_ID UUID PK,FK NOT NULL → SCOS_RESOURCE`
  - `CREATED_AT TIMESTAMPTZ NOT NULL DEFAULT NOW()`
  - `USER_AT VARCHAR(255) NOT NULL`
  - Sem `UPDATED_AT` (vínculo imutável — criado ou removido)
  - Índices em `PROFILE_ID` e `RESOURCE_ID`

#### Tabela a remover

- [ ] **RF-04**: Remover `scos_permission.yml` e remover referência em `tables.yml`

#### SCOS_DEPARTMENT — correções

- [ ] **RF-05**: Corrigir `SCOS_DEPARTMENT`
  - `DESCRIPTION VARCHAR(30)` (era `varchar(500)`)
  - `UPDATED_AT TIMESTAMPTZ NOT NULL` (era nullable)
  - `USER_AT VARCHAR(255) NOT NULL` (era `varchar(100)` nullable)

#### SCOS_POSITION — correções

- [ ] **RF-06**: Corrigir `SCOS_POSITION`
  - `DESCRIPTION VARCHAR(30)` (era `varchar(500)`)
  - `UPDATED_AT TIMESTAMPTZ NOT NULL` (era nullable)
  - `USER_AT VARCHAR(255) NOT NULL` (era `varchar(100)` nullable)

#### SCOS_COMPANY — correções

- [ ] **RF-07**: Corrigir `SCOS_COMPANY`
  - Remover coluna `ACTIVE BOOLEAN` (não existe no domain model)
  - `NAME VARCHAR(250)` (era `varchar(255)`)
  - `NAME_TREATMENT VARCHAR(100)` (era `varchar(255)`)
  - `UPDATED_AT TIMESTAMPTZ NOT NULL` (era nullable)
  - `USER_AT VARCHAR(255) NOT NULL` (era `varchar(100)` nullable)

#### SCOS_COMPANY_ADDRESS — reescrita estrutural

- [ ] **RF-08**: Reescrever `SCOS_COMPANY_ADDRESS` com PK composta
  - Remover PK surrogate `COMPANY_ADDRESS_ID` (e sua sequence — coberta por RF-20)
  - `COMPANY_ID_ADDRESS BIGINT PK` (referência externa, sem FK — integridade pela aplicação)
  - `COMPANY_ID BIGINT PK,FK NOT NULL → SCOS_COMPANY` (PK composta com `COMPANY_ID_ADDRESS`)
  - `TYPE VARCHAR(50) UK NOT NULL`
  - `NUMBER INT NOT NULL`
  - `COMPLEMENT VARCHAR(250) NULL`
  - `GEOLOCATION GEOMETRY(POINT) NOT NULL` (era nullable)
  - `CREATED_AT TIMESTAMPTZ NOT NULL DEFAULT NOW()`
  - `UPDATED_AT TIMESTAMPTZ NOT NULL` (era nullable)
  - `USER_AT VARCHAR(255) NOT NULL` (era `varchar(100)` nullable)
  - Remover UK antigo `(COMPANY_ID, ADDRESS_ID, TYPE)` — substituído por UK em `TYPE` por empresa

#### SCOS_COMPANY_CONTACT — correções

- [ ] **RF-09**: Corrigir `SCOS_COMPANY_CONTACT`
  - Renomear PK `CONTACT_ID` → `COMPANY_ID_CONTACT`
  - Remover coluna `RESPONSIBLE_PERSON` (não existe no domain model)
  - Adicionar `UK_PHONE_SCOS_COMPANY_CONTACT` em `PHONE`
  - Adicionar `UK_EMAIL_SCOS_COMPANY_CONTACT` em `EMAIL`
  - Adicionar `UK_TYPE_SCOS_COMPANY_CONTACT` em `TYPE`
  - `UPDATED_AT TIMESTAMPTZ NOT NULL` (era nullable)
  - `USER_AT VARCHAR(255) NOT NULL` (era `varchar(100)` nullable)

#### SCOS_EMPLOYEE — correções

- [ ] **RF-10**: Corrigir `SCOS_EMPLOYEE`
  - `COMPANY_ID BIGINT FK NOT NULL` (era nullable)
  - `POSITION_ID BIGINT FK NOT NULL` (era nullable)
  - Adicionar `STATUS VARCHAR(20) NOT NULL` (ausente; valores: `ACTIVE`, `INACTIVE`, `DISABLED`)
  - Remover coluna `ACTIVE BOOLEAN` (não existe no domain model)
  - `NAME VARCHAR(250)` (era `varchar(255)`)
  - `NAME_TREATMENT VARCHAR(100)` (era `varchar(255)`)
  - `EMAIL VARCHAR(255)` (era `varchar(320)`)
  - Adicionar `UK_EMAIL_SCOS_EMPLOYEE` em `EMAIL` (ausente)
  - `UPDATED_AT TIMESTAMPTZ NOT NULL` (era nullable)
  - `USER_AT VARCHAR(255) NOT NULL` (era `varchar(100)` nullable)

#### SCOS_EMPLOYEE_ADDRESS — correções

- [ ] **RF-11**: Corrigir `SCOS_EMPLOYEE_ADDRESS`
  - Renomear coluna `ADDRESS_ID` → `EMPLOYEE_ID_ADDRESS` (sem FK — referência externa)
  - PK composta permanece `(EMPLOYEE_ID_ADDRESS, EMPLOYEE_ID)` com novo nome
  - `GEOLOCATION GEOMETRY(POINT) NOT NULL` (era nullable)
  - `UPDATED_AT TIMESTAMPTZ NOT NULL` (era nullable)
  - `USER_AT VARCHAR(255) NOT NULL` (era `varchar(100)` nullable)

#### SCOS_EMPLOYEE_CONTACT — correções

- [ ] **RF-12**: Corrigir `SCOS_EMPLOYEE_CONTACT`
  - Renomear PK `CONTACT_ID` → `EMPLOYEE_ID_CONTACT`
  - Adicionar `UK_PHONE_SCOS_EMPLOYEE_CONTACT` em `PHONE`
  - Adicionar `UK_TYPE_SCOS_EMPLOYEE_CONTACT` em `TYPE`
  - `UPDATED_AT TIMESTAMPTZ NOT NULL` (era nullable)
  - `USER_AT VARCHAR(255) NOT NULL` (era `varchar(100)` nullable)

#### SCOS_PROFILE — correções

- [ ] **RF-13**: Corrigir `SCOS_PROFILE`
  - Remover coluna `FEATURES TEXT[]` (não existe no domain model)
  - Remover índice GIN em `FEATURES`
  - `CODE VARCHAR(30)` (era `varchar(50)`)
  - `DESCRIPTION VARCHAR(30)` (era `varchar(100)`)
  - `UPDATED_AT TIMESTAMPTZ NOT NULL` (era nullable)
  - `USER_AT VARCHAR(255) NOT NULL` (era `varchar(100)` nullable)

#### SCOS_LOGIN — reescrita estrutural

- [ ] **RF-14**: Reescrever `SCOS_LOGIN`
  - `EMPLOYEE_ID BIGINT FK NULL` (era NOT NULL — deve permitir nulo para usuários externos e contas de serviço)
  - Remover coluna `PASSWORD TEXT`
  - Remover coluna `SALT TEXT`
  - Remover coluna `DATE_LAST_CHANGE_PASSWORD`
  - Adicionar `TYPE VARCHAR(50) NOT NULL` (valores: `EMPLOYEE`, `EXTERNAL`, `SERVICE`)
  - `LOGIN VARCHAR(255)` (era `varchar(100)`)
  - `UPDATED_AT TIMESTAMPTZ NOT NULL` (era nullable)
  - `USER_AT VARCHAR(255) NOT NULL` (era `varchar(100)` nullable)

#### SCOS_CONFIGURATION — reescrita completa

- [ ] **RF-15**: Reescrever `SCOS_CONFIGURATION` com PK textual
  - `CONFIGURATION_ID VARCHAR(50) PK` (chave textual — ex: `TOKEN_EXPIRY_MINUTES`; sem auto-increment)
  - Remover colunas: `CONFIGURATION_KEY`, `DESCRIPTION`, `CONFIGURATION_VALUE`, `DEFAULT_VALUE`
  - Adicionar `VALUE TEXT NOT NULL`
  - Adicionar `TYPE VARCHAR(50) NOT NULL` (valores: `STRING`, `INTEGER`, `BOOLEAN`, `JSON`)
  - `CREATED_AT TIMESTAMPTZ NOT NULL DEFAULT NOW()`
  - `UPDATED_AT TIMESTAMPTZ NOT NULL` (era nullable)
  - `USER_AT VARCHAR(255) NOT NULL` (era `varchar(100)` nullable)

#### SCOS_INTEGRATION_KEYCLOAK — correções

- [ ] **RF-16**: Corrigir `SCOS_INTEGRATION_KEYCLOAK`
  - Adicionar `RETRY_COUNT INT NOT NULL DEFAULT 0`
  - Adicionar `MAX_RETRIES INT NOT NULL DEFAULT 3`

#### SCOS_INTEGRATION_KEYCLOAK_LOG — correções

- [ ] **RF-17**: Corrigir `SCOS_INTEGRATION_KEYCLOAK_LOG`
  - Remover coluna `UPDATED_AT` (tabela imutável — registros apenas inseridos)

#### SCOS_INTEGRATION_MESSAGE_INVALID — correções

- [ ] **RF-18**: Corrigir `SCOS_INTEGRATION_MESSAGE_INVALID`
  - `MESSAGE_INVALID VARCHAR(5000) NOT NULL` (era nullable)
  - Remover coluna `UPDATED_AT` (tabela imutável — registros apenas inseridos)

#### tables.yml — reorganização

- [ ] **RF-19**: Atualizar `tables.yml` com nova ordem respeitando dependências FK
  ```
  scos_department → scos_position → scos_company → scos_company_address
  → scos_company_contact → scos_employee → scos_employee_address
  → scos_employee_contact → scos_profile → scos_system → scos_resource
  → scos_profile_resource → scos_login → scos_configuration
  → scos_integration_keycloak → scos_integration_keycloak_log
  → scos_integration_message_invalid
  ```

#### Geração de IDs — GENERATED ALWAYS AS IDENTITY

- [ ] **RF-20**: Remover todas as sequences explícitas (`SEQ_*`) e usar `GENERATED ALWAYS AS IDENTITY` nas PKs do tipo `BIGINT`
  - Padrão SQL (ISO SQL:2003), nativo PostgreSQL 10+
  - PostgreSQL gerencia o identity internamente sem sequence visível no schema
  - Em Liquibase: `autoIncrement: true` + `generationType: always` na coluna PK
  - Aplica-se a: `SCOS_DEPARTMENT`, `SCOS_POSITION`, `SCOS_COMPANY`, `SCOS_COMPANY_CONTACT`, `SCOS_EMPLOYEE`, `SCOS_EMPLOYEE_CONTACT`, `SCOS_PROFILE`, `SCOS_LOGIN`, `SCOS_INTEGRATION_KEYCLOAK_LOG`
  - Não se aplica: PKs UUID (geradas por `gen_random_uuid()`), PK textual (`SCOS_CONFIGURATION`), PKs compostas (`SCOS_COMPANY_ADDRESS`, `SCOS_EMPLOYEE_ADDRESS`, `SCOS_PROFILE_RESOURCE`)

#### Índices de performance

- [ ] **RF-21**: Criar `IDX_RESOURCE_ID_SCOS_PROFILE_RESOURCE` em `SCOS_PROFILE_RESOURCE(RESOURCE_ID)`
  — cobre query inversa: "quais perfis têm este recurso?" (auditoria e revogação)

- [ ] **RF-22**: Criar `IDX_STATUS_SCOS_EMPLOYEE` em `SCOS_EMPLOYEE(STATUS)`
  — filtro por ACTIVE/INACTIVE/DISABLED em listagens e relatórios

- [ ] **RF-23**: Criar `IDX_STATUS_SCOS_COMPANY` em `SCOS_COMPANY(STATUS)`
  — filtro por status em listagens de empresas ativas

- [ ] **RF-24**: Criar `IDX_STATUS_SCOS_INTEGRATION_KEYCLOAK` em `SCOS_INTEGRATION_KEYCLOAK(STATUS)`
  — job assíncrono faz polling de registros `PENDING`; sem índice = seq scan a cada tick

- [ ] **RF-25**: Criar `IDX_KEYCLOAK_ID_SCOS_LOGIN` em `SCOS_LOGIN(KEYCLOAK_ID)`
  — lookup por KEYCLOAK_ID no fluxo de autenticação e verificação de status

- [ ] **RF-26**: Criar `IDX_KEYCLOAK_ID_SCOS_INTEGRATION_KEYCLOAK` em `SCOS_INTEGRATION_KEYCLOAK(KEYCLOAK_ID)`
  — lookup pós-criação no Keycloak para sincronizar o ID retornado

### Não-Funcionais
- [ ] **RNF-01**: Todos os changeSets devem ter `rollback` funcional
- [ ] **RNF-02**: IDs dos changeSets devem seguir o padrão `YYYYMMDD-Samuel.Cunha-NNN`
- [ ] **RNF-03**: Nomes de sequences, índices e constraints devem seguir o padrão existente no projeto (`SEQ_*`, `PK_*`, `FK_*`, `UK_*`, `IDX_*`)
- [ ] **RNF-04**: `liquibase update` em banco limpo deve executar sem erros
- [ ] **RNF-05**: `liquibase rollback` deve desfazer todas as alterações sem erros

---

## 3️⃣ Arquitetura

### Componentes Afetados
```
flow-organization-boot/src/main/resources/db/changelog/v1.0.0/tables/
├── tables.yml                            modificação — reordenar, adicionar novos, remover permission
├── scos_department.yml                   modificação — DESCRIPTION size, UPDATED_AT/USER_AT
├── scos_position.yml                     modificação — DESCRIPTION size, UPDATED_AT/USER_AT
├── scos_company.yml                      modificação — remove ACTIVE, sizes, UPDATED_AT/USER_AT
├── scos_company_address.yml              modificação — PK composta, remove sequence, GEOLOCATION NOT NULL
├── scos_company_contact.yml              modificação — rename PK, remove RESPONSIBLE_PERSON, add UKs
├── scos_employee.yml                     modificação — add STATUS, fix nullable, remove ACTIVE, sizes
├── scos_employee_address.yml             modificação — rename ADDRESS_ID, GEOLOCATION NOT NULL
├── scos_employee_contact.yml             modificação — rename PK, add UKs
├── scos_profile.yml                      modificação — remove FEATURES + GIN index, fix sizes
├── scos_login.yml                        modificação — remove PASSWORD/SALT/DATE_LAST_CHANGE, add TYPE, fix nullable
├── scos_configuration.yml               modificação — reescrita completa (PK textual)
├── scos_integration_keycloak.yml        modificação — add RETRY_COUNT, MAX_RETRIES
├── scos_integration_keycloak_log.yml    modificação — remove UPDATED_AT
├── scos_integration_message_invalid.yml modificação — MESSAGE_INVALID NOT NULL, remove UPDATED_AT
├── scos_system.yml                       adição — novo
├── scos_resource.yml                     adição — novo
├── scos_profile_resource.yml            adição — novo
└── scos_permission.yml                   remoção
```

### Fluxo Principal
```
Domain model (fonte da verdade) → Gap analysis → Reescrita migrations → liquibase update → Schema correto
```

### Decisões Técnicas
| Decisão | Escolha | Alternativa Descartada | Motivo |
|---------|---------|------------------------|--------|
| Estratégia | Reescrita dos arquivos v1.0.0 | ADD/ALTER COLUMN incremental | Sistema sem produção — reescrita mais limpa |
| SCOS_PERMISSION | Remover | Manter + adicionar novas | Modelo antigo inexistente no domain model |
| Ordem criação | Respeitar dependências FK | Qualquer ordem + deferrable FKs | Migrations simples, sem truques |
| Escopo | Somente Liquibase | Incluir JPA | Domain model → migrations primeiro; JPA é consequência |
| Geração de IDs | `GENERATED ALWAYS AS IDENTITY` | Sequences explícitas (`SEQ_*`) | Padrão SQL nativo PostgreSQL 10+; sem gestão manual de sequences |
| IDX_RESOURCE_ID em PROFILE_RESOURCE | Criar | Só PK composta | Cobre query inversa de auditoria e revogação |
| Índices de STATUS | Criar em EMPLOYEE, COMPANY, INTEGRATION_KEYCLOAK | Nenhum | Filtros frequentes; polling do job assíncrono |
| IDX_KEYCLOAK_ID | Criar em LOGIN e INTEGRATION_KEYCLOAK | Nenhum | Lookup frequente no fluxo de autenticação e sync |

### Banco de Dados
- **Impacto**: ✅ Sim
- **Tabelas criadas**: `SCOS_SYSTEM`, `SCOS_RESOURCE`, `SCOS_PROFILE_RESOURCE`
- **Tabela removida**: `SCOS_PERMISSION`
- **Tabelas modificadas**: 15 (todas as demais)
- **Banco recriado do zero**: sem migração de dados, sistema sem produção

---

## 4️⃣ Implementação

### Arquivos

**Novos**:
- `db/changelog/v1.0.0/tables/scos_system.yml`
- `db/changelog/v1.0.0/tables/scos_resource.yml`
- `db/changelog/v1.0.0/tables/scos_profile_resource.yml`

**Modificados**:
- `db/changelog/v1.0.0/tables/tables.yml`
- `db/changelog/v1.0.0/tables/scos_department.yml`
- `db/changelog/v1.0.0/tables/scos_position.yml`
- `db/changelog/v1.0.0/tables/scos_company.yml`
- `db/changelog/v1.0.0/tables/scos_company_address.yml`
- `db/changelog/v1.0.0/tables/scos_company_contact.yml`
- `db/changelog/v1.0.0/tables/scos_employee.yml`
- `db/changelog/v1.0.0/tables/scos_employee_address.yml`
- `db/changelog/v1.0.0/tables/scos_employee_contact.yml`
- `db/changelog/v1.0.0/tables/scos_profile.yml`
- `db/changelog/v1.0.0/tables/scos_login.yml`
- `db/changelog/v1.0.0/tables/scos_configuration.yml`
- `db/changelog/v1.0.0/tables/scos_integration_keycloak.yml`
- `db/changelog/v1.0.0/tables/scos_integration_keycloak_log.yml`
- `db/changelog/v1.0.0/tables/scos_integration_message_invalid.yml`

**Removidos**:
- `db/changelog/v1.0.0/tables/scos_permission.yml`

### Tarefas
- [ ] **T-01**: Criar `scos_system.yml` (RF-01)
- [ ] **T-02**: Criar `scos_resource.yml` (RF-02)
- [ ] **T-03**: Criar `scos_profile_resource.yml` (RF-03)
- [ ] **T-04**: Remover `scos_permission.yml` (RF-04)
- [ ] **T-05**: Corrigir `scos_department.yml` (RF-05)
- [ ] **T-06**: Corrigir `scos_position.yml` (RF-06)
- [ ] **T-07**: Corrigir `scos_company.yml` (RF-07)
- [ ] **T-08**: Reescrever `scos_company_address.yml` — PK composta (RF-08)
- [ ] **T-09**: Corrigir `scos_company_contact.yml` (RF-09)
- [ ] **T-10**: Corrigir `scos_employee.yml` (RF-10)
- [ ] **T-11**: Corrigir `scos_employee_address.yml` (RF-11)
- [ ] **T-12**: Corrigir `scos_employee_contact.yml` (RF-12)
- [ ] **T-13**: Corrigir `scos_profile.yml` (RF-13)
- [ ] **T-14**: Reescrever `scos_login.yml` — estrutura completa (RF-14)
- [ ] **T-15**: Reescrever `scos_configuration.yml` — PK textual (RF-15)
- [ ] **T-16**: Corrigir `scos_integration_keycloak.yml` (RF-16)
- [ ] **T-17**: Corrigir `scos_integration_keycloak_log.yml` (RF-17)
- [ ] **T-18**: Corrigir `scos_integration_message_invalid.yml` (RF-18)
- [ ] **T-19**: Atualizar `tables.yml` com nova ordem (RF-19)
- [ ] **T-20**: Remover todas as sequences explícitas e migrar PKs BIGINT para `GENERATED ALWAYS AS IDENTITY` (RF-20)
- [ ] **T-21**: Criar `IDX_RESOURCE_ID_SCOS_PROFILE_RESOURCE` (RF-21)
- [ ] **T-22**: Criar `IDX_STATUS_SCOS_EMPLOYEE` (RF-22)
- [ ] **T-23**: Criar `IDX_STATUS_SCOS_COMPANY` (RF-23)
- [ ] **T-24**: Criar `IDX_STATUS_SCOS_INTEGRATION_KEYCLOAK` (RF-24)
- [ ] **T-25**: Criar `IDX_KEYCLOAK_ID_SCOS_LOGIN` (RF-25)
- [ ] **T-26**: Criar `IDX_KEYCLOAK_ID_SCOS_INTEGRATION_KEYCLOAK` (RF-26)
- [ ] **T-27**: Validar `liquibase update` em banco limpo — sem erros
- [ ] **T-28**: Validar `liquibase rollback` — desfaz sem erros

### Riscos e Edge Cases
1. Liquibase valida checksum dos changeSets — se banco não for recriado, executar `liquibase clearCheckSums` antes
2. `GENERATED ALWAYS AS IDENTITY` requer que o Hibernate use `@GeneratedValue(strategy = GenerationType.IDENTITY)` nas entidades — ajuste de JPA é escopo separado mas deve ser considerado
3. `GENERATED ALWAYS AS IDENTITY` não permite INSERT com valor explícito sem `OVERRIDING SYSTEM VALUE` — cuidado em scripts de seed/fixture
4. `SCOS_CONFIGURATION` com PK textual — sem auto-increment; INSERT sempre deve fornecer o valor da chave explicitamente

---

## 📎 Referências
- Domain model: `etc/database/domain_model.md`
- Migrations atuais: `flow-organization-boot/src/main/resources/db/changelog/v1.0.0/tables/`
