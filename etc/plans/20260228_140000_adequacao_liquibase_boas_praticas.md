# Adequação Completa dos Arquivos Liquibase — Boas Práticas

**Data de Criação**: 2026-02-28 14:00:00  
**Status**: ⚠️ Aprovado com Ressalvas — 3ª rodada de revisão concluída; 7 novos bloqueantes identificados; FASE 5 bloqueada (seed incompleto + decisão B-3a pendente)  
**ID da Decisão**: ADR-002  
**Tipo**: 🔧 Refatoração

---

## 1️⃣ Solicitação do Usuário

### Requisição Original
```
Seguindo as boas práticas do Liquibase, valide se todos os arquivos de Liquibase do 
projeto seguem o padrão e as boas práticas. Crie um plano para adequar todos os arquivos. 
Lembrando que o projeto está na fase inicial, pode recriar do zero pois não irá afetar ninguém.
```

### Arquivos Analisados
- `scos-organization-boot/src/main/resources/db/changelog/db.changelog-master.yaml` *(extensão incorreta — deve ser `.yml`)*
- `scos-organization-boot/src/main/resources/db/changelog/property.yml`
- `scos-organization-boot/src/main/resources/db/changelog/init.yml` *(deve ser removido — master inclui `v1.0.0/` diretamente)*
- `scos-organization-boot/src/main/resources/db/changelog/v1/0/0/v1.0.0.yml` *(estrutura de pasta incorreta — deve ser `v1.0.0/`)*
- `scos-organization-boot/src/main/resources/db/changelog/v1/0/0/tables/tables.yml` + 15 arquivos de tabela
- `scos-organization-boot/src/main/resources/db/changelog/v1/0/0/function/function.yml` + 3 SQLs *(deve estar no nível raiz de `db/changelog/`)*
- `scos-organization-boot/src/main/resources/db/changelog/v1/0/0/trigger/trigger.yml` + 4 SQLs *(deve estar no nível raiz e pasta se chama `triggers/`)*
- `scos-organization-boot/src/main/resources/db/changelog/v1/0/0/insert/insert.yml` + 1 SQL
- `scos-organization-boot/src/main/resources/db/changelog/v1/0/0/views/views.yml` + 2 SQLs *(deve estar no nível raiz e pasta se chama `view/`)*

### Prioridade
- [x] 🟡 Média (melhoria importante — base do projeto está sendo estabelecida)

---

## 2️⃣ Objetivo do Desenvolvimento

> **Definido por**: 🔧 @dev-senior

### Problema Identificado

Os arquivos Liquibase do projeto apresentam **22 violações de boas práticas**, distribuídas entre problemas críticos de estrutura, inconsistências de tipo de dados, ausência de rollback e bugs funcionais.

**Sintomas identificados na análise**:
1. `tagDatabase` embutido dentro de changeSets que contêm DDL
2. `tagDatabase: v1.0.0` repetido em todos os 20+ changeSets  
3. Tipo `DATETIME` inválido para PostgreSQL (deveria ser `TIMESTAMP`)
4. Tipo `LONGTEXT` inválido para PostgreSQL (deveria ser `TEXT`)
5. Bug crítico: `trigger.yml` aponta `trg_check_employee_supervisor.sql` duas vezes, excluindo `trg_check_company_parent_company`
6. Rollback ausente em múltiplos changeSets
7. `author` inconsistente (`Samuel.Cunha` vs `Samuel Cunha`)
8. IDs de changeSet sem padrão único (`SGC_DDMM...` vs nome semântico)
9. Mix de configuração MySQL no `property.yml` (stack é PostgreSQL only)
10. `autoIncrement: false` desnecessário em colunas FK
11. Funções SQL sem schema `scos.` qualificado de forma inconsistente
12. `serial` + `renameSequence` como anti-pattern (deveria ser `BIGSERIAL` ou `createSequence` explícito)
13. `splitStatements: false` + `endDelimiter: ";"` — parâmetros conflitantes
14. `TEXT[]` e `JSONB` sem marcador `dbms: postgresql`
15. Indentação inconsistente no `views.yml`
16. Ausência de `comment` na maioria dos changeSets
17. Extensão `db.changelog-master.yaml` inconsistente com `.yml` dos demais
18. `ACTIVE` fora de ordem nos campos de `scos_employee`
19. `scos_configuration.yml` com `autoIncrement: true` redundante no `SERIAL`
20. `configure_system.sql` não insere `active` em `scos_department` / `scos_position` após os changeSets de adequação adicionarem a coluna
21. Sequências sendo renomeadas após criação por `serial`; sem índices explícitos para FK de alta cardinalidade
22. `tagDatabase` não deve estar antes do `createTable` no mesmo changeSet — a ordem importa para rollback

### Objetivo Principal
Reescrever todos os arquivos Liquibase do zero, seguindo as boas práticas oficiais, com tipagem PostgreSQL correta, changeSets atômicos, rollbacks completos, IDs padronizados, schema qualificado e sem redundâncias.

### Critérios de Sucesso
- [ ] Nenhum uso de `DATETIME` ou `LONGTEXT` — apenas tipos nativos PostgreSQL
- [ ] `tagDatabase` em changeSet próprio e dedicado, aparecendo **uma única vez**
- [ ] Todos os changeSets possuem `comment` e `rollback` explícito
- [ ] ID de changeSet no formato `<YYYYMMDD>-<author>-<seq>` de forma consistente
- [ ] `author` padronizado como `Samuel.Cunha` em todos os arquivos
- [ ] Bug do trigger corrigido
- [ ] `property.yml` com apenas propriedades PostgreSQL
- [ ] Funções SQL com schema `scos.` qualificado em todos
- [ ] `createSequence` explícito com `incrementBy: 50` alinhado com `allocationSize=50` do Hibernate
- [ ] Tipos PostgreSQL-específicos (`TEXT[]`, `JSONB`) com marcador `dbms: postgresql`
- [ ] `configure_system.sql` com `active=true` em department/position e status `PENDING_PASSWORD_CHANGE` no login
- [ ] `db.changelog-master.yaml` renomeado para `.yml` + `bootstrap.yml` e `liquibase.properties` atualizados
- [ ] `CREATED_AT` com `defaultValueComputed: "NOW()"` em todas as tabelas
- [ ] Índice `IDX_PARENT_COMPANY_ID_SCOS_COMPANY` criado
- [ ] Índice GIN `IDX_GIN_FEATURES_SCOS_PROFILE` criado
- [ ] `OBSERVATION` em `SCOS_COMPANY` alterado para `nullable: true`
- [ ] Perfil ADMIN com todas as features de administração
- [ ] `tagDatabase: v1.0.0` como único changeSet dedicado, posicionado antes de todos os DDLs

### Não-Objetivos
- Alterar a lógica de negócio das funções e triggers SQL
- Alterar o modelo de dados (tabelas, colunas, relacionamentos)
- Criar novos recursos ou tabelas não existentes

---

## 3️⃣ Diagnóstico Detalhado por Arquivo

> **Análise por**: 🗄️ @dba

### 3.1 Violações Críticas

#### VIOLAÇÃO 1 — `tagDatabase` dentro de changeSets com DDL
**Arquivos afetados**: Todos os 20+ changeSets de tabela, função, trigger, view e insert  
**Problema**: O `tagDatabase` dentro de um changeSet não garante posicionamento correto no histórico. Se o changeSet falha após o DDL mas durante o tag, o banco fica em estado inconsistente. Além disso, todos os changeSets estão repetindo `tag: v1.0.0`, o que invalida semanticamente o conceito de tag versioning.

**Prática correta segundo Liquibase**:
```yaml
# changeSet DEDICADO e EXCLUSIVO para tag — executado ANTES das mudanças
- changeSet:
    id: 20260228-Samuel.Cunha-001
    author: Samuel.Cunha
    comment: "Tag de início da versão v1.0.0"
    changes:
      - tagDatabase:
          tag: v1.0.0-start
    rollback: []
```

#### VIOLAÇÃO 2 — Tipos de dados incompatíveis com PostgreSQL
**Arquivos afetados**: Todos os changelogs de tabela  

| Tipo usado (errado) | Tipo correto (PostgreSQL) | Tabelas afetadas |
|---|---|---|
| `DATETIME` | `TIMESTAMPTZ` | Todas as tabelas (campos `CREATED_AT`, `UPDATED_AT`, `DATE_CREATED`, etc.) |
| `LONGTEXT` | `TEXT` | `SCOS_COMPANY`, `SCOS_EMPLOYEE`, `SCOS_COMPANY_ADDRESS`, `SCOS_EMPLOYEE_ADDRESS` |

**Detalhe**: Liquibase mapeia `DATETIME` para `TIMESTAMP WITHOUT TIME ZONE` no PostgreSQL internamente, mas o uso correto e explícito é `TIMESTAMPTZ` para campos de auditoria e `TIMESTAMP` para datas sem fuso. `LONGTEXT` é tipo MySQL — PostgreSQL usa `TEXT` ilimitado.

#### VIOLAÇÃO 3 — Bug no `trigger.yml`
**Arquivo**: `v1/0/0/trigger/trigger.yml`  
**Problema**: O segundo changeSet (`SGC_1801202514142`) aponta para `trg_check_employee_supervisor.sql` ao invés de `trg_check_company_parent_company.sql`. O trigger de verificação de empresa-pai **nunca é criado**.

```yaml
# ERRADO — segundo changeSet aponta para o arquivo errado:
- changeSet:
    id: SGC_1801202514142
    changes:
      - sqlFile:
          path: trg_check_employee_supervisor.sql  # ← BUG: deveria ser trg_check_company_parent_company.sql
```

#### VIOLAÇÃO 4 — `serial` + `renameSequence` (anti-pattern)
**Arquivos afetados**: Todos os changeSets de tabela com PK auto-increment  
**Problema**: O tipo `serial` cria automaticamente uma sequência com nome gerado pelo PostgreSQL (`tabela_coluna_seq`). O changeSet então a renomeia com `renameSequence`, o que é frágil e pode causar erros em ambientes onde a sequência ainda não foi criada (ex: testes de integração com ordem de execução diferente).

**Prática correta**:
```yaml
# Opção A: usar BIGSERIAL (mais simples)
- column:
    name: COMPANY_ID
    type: BIGSERIAL
    constraints:
      primaryKey: true
      primaryKeyName: PK_SCOS_COMPANY
      nullable: false

# Opção B: createSequence explícito + defaultValueSequenceNext
- createSequence:
    sequenceName: SEQ_COMPANY_ID
    dataType: BIGINT
    startValue: 1
    incrementBy: 1
- createTable:
    ...
    - column:
        name: COMPANY_ID
        type: BIGINT
        defaultValueSequenceNext: SEQ_COMPANY_ID
```
**Decisão para este projeto**: Usar **Opção B** (`createSequence` explícito) para controle total e alinhamento com o padrão já estabelecido de nomes `SEQ_*`.

#### VIOLAÇÃO 5 — Rollback ausente
**Arquivos sem rollback ou com rollback incompleto**:

| Arquivo | Problema |
|---|---|
| `scos_company_contact.yml` | Sem `rollback` |
| `scos_permission.yml` | Sem `rollback` |
| `scos_configuration.yml` | Sem `rollback` |
| `function/function.yml` | Todos os 3 changeSets sem rollback |
| `trigger/trigger.yml` | Todos os 4 changeSets sem rollback |
| `insert/insert.yml` | Sem rollback |
| `views/views.yml` | Sem rollback |

**Prática correta**: Todo changeSet deve ter rollback explícito. Para funções/triggers/views, o rollback é `DROP`:
```yaml
rollback:
  - sql:
      sql: DROP FUNCTION IF EXISTS scos.check_employee_supervisor();
```

### 3.2 Violações de Padronização

#### VIOLAÇÃO 6 — ID de changeSet inconsistente
**Problema**: Mistura de formatos
- `SGC_1901202509251` — data no formato DDMMYYYYHHMM + sequencial
- `v_scos_cache_login` — nome semântico (sem data)
- `SGC_2802202612001` — mesmo padrão mas com data diferente

**Padrão recomendado** (alinhado com Liquibase best practices):
```
<YYYYMMDD>-<author>-<NNN>
Exemplo: 20260228-Samuel.Cunha-001
```

#### VIOLAÇÃO 7 — `author` inconsistente
| Formato encontrado | Ocorrências |
|---|---|
| `Samuel.Cunha` | `scos_company.yml`, `scos_login.yml` e outros |
| `Samuel Cunha` | `function.yml`, `trigger.yml`, `insert.yml`, `views.yml` |

**Padrão adotado**: `Samuel.Cunha` (sem espaço, alinhado com a maioria).

#### VIOLAÇÃO 8 — Extensão de arquivo inconsistente
- `db.changelog-master.yaml` (`.yaml`)
- Todos os demais arquivos (`.yml`)

**Decisão**: Padronizar todos como `.yml`.

#### VIOLAÇÃO 9 — `property.yml` com configurações MySQL
**Problema**: O stack do projeto é **PostgreSQL exclusivo** (ver `copilot-instructions.md`), mas `property.yml` define propriedades para `mysql`, `oracle` e `mssql`.

**Solução**: Remover todas as propriedades não-PostgreSQL, simplificando o arquivo.

#### VIOLAÇÃO 10 — `autoIncrement: false` desnecessário em FK
Ocorre em `scos_login.yml`, `scos_employee_address.yml`, `scos_integration_keycloak_log.yml` e outros. Colunas FK não são auto-increment — o atributo é desnecessário e confuso.

#### VIOLAÇÃO 11 — `splitStatements: false` + `endDelimiter: ";"`
**Arquivo**: `function.yml`, `trigger.yml`, `insert.yml`, `views.yml`  
**Problema**: Quando `splitStatements: false`, o Liquibase não divide o SQL por delimitador — portanto `endDelimiter` não tem efeito e é ruído.  
Para funções e triggers PostgreSQL (que usam `$$`), o correto é:
```yaml
splitStatements: false
# SEM endDelimiter
```

#### VIOLAÇÃO 12 — Funções SQL sem schema qualificado consistente
| Função | Schema atual |
|---|---|
| `check_employee_supervisor()` | sem schema (público) |
| `check_company_parent_company()` | `scos.` (correto) |
| `remove_formatting_tax_identifier()` | sem schema (público) |

**Solução**: Todas as funções devem usar `scos.` como schema, inclusive nas referências dos triggers.

#### VIOLAÇÃO 13 — `TEXT[]` e `JSONB` sem `dbms: postgresql`
Embora o projeto use apenas PostgreSQL, esses tipos devem ser marcados para deixar explícita a dependência:
```yaml
- createTable:
    tableName: SCOS_PERMISSION
    dbms: postgresql
    columns: ...
```

#### VIOLAÇÃO 14 — Indentação inconsistente em `views.yml`
```yaml
# ERRADO — sem indentação nos changeSets
databaseChangeLog:
- changeSet:
    id: v_scos_cache_login

# CORRETO — alinhado com os demais arquivos
databaseChangeLog:
  - changeSet:
      id: ...
```

#### VIOLAÇÃO 15 — Ausência de `comment` na maioria dos changeSets
Boa prática: todo changeSet deve ter `comment` descritivo.

#### VIOLAÇÃO 16 — `tagDatabase` deve ser o ÚLTIMO item do changeSet (ordem)
Quando `tagDatabase` é misturado com DDL, mesmo que seja colocado, ele deve ser o **último** para que a tag aponte para o estado após todas as mudanças. Atualmente alguns estão como primeiro item.

#### VIOLAÇÃO 17 — `ACTIVE` fora de posição em `scos_employee.yml`
O campo `ACTIVE` aparece como penúltimo, depois dos campos de auditoria. Deve vir antes de `CREATED_AT/UPDATED_AT/USER_AT`.

#### VIOLAÇÃO 18 — Segunda migration de `SCOS_DEPARTMENT` e `SCOS_POSITION` sem dados iniciais
O `configure_system.sql` (insert) não inclui o campo `active` para os registros de `scos_department` e `scos_position`, mas esses campos foram adicionados como `NOT NULL` nos changeSets de adequação (`SGC_2802202612001` e `SGC_2802202612002`). Isso causará erro no insert se executado após as duas migrations.

#### VIOLAÇÃO 19 — `SCOS_COMPANY_ADDRESS` sem `constraintName` na unique constraint
```yaml
# ERRADO — sem constraintName:
- addUniqueConstraint:
    tableName: SCOS_COMPANY_ADDRESS
    columnNames: COMPANY_ID, ADDRESS_ID, TYPE

# CORRETO:
- addUniqueConstraint:
    tableName: SCOS_COMPANY_ADDRESS
    columnNames: COMPANY_ID, ADDRESS_ID, TYPE
    constraintName: UK_COMPANY_ADDRESS_TYPE_SCOS_COMPANY_ADDRESS
    validate: true
```

#### VIOLAÇÃO 20 — Ausência de índices em FKs de alta cardinalidade
As tabelas `SCOS_EMPLOYEE`, `SCOS_COMPANY_ADDRESS`, `SCOS_COMPANY_CONTACT`, `SCOS_EMPLOYEE_ADDRESS`, `SCOS_EMPLOYEE_CONTACT` e `SCOS_LOGIN` não declaram índices explícitos para suas colunas FK. O PostgreSQL não cria automaticamente índices para FK (ao contrário do MySQL).

#### VIOLAÇÃO 21 — `scos_configuration.yml` com `autoIncrement: true` redundante
O tipo `SERIAL` já implica auto-incremento. O atributo `autoIncrement: true` é redundante e o `autoIncrement: false` nas demais colunas é desnecessário.

#### VIOLAÇÃO 22 — `trg_remove_formatting_tax_identifier_company.sql` e `trg_remove_formatting_tax_identifier_employee.sql` sem `OR REPLACE`
```sql
-- ERRADO:
CREATE TRIGGER trg_remove_formatting_tax_identifier_company ...

-- CORRETO (PostgreSQL 14+):
CREATE OR REPLACE TRIGGER trg_remove_formatting_tax_identifier_company ...
```

---

## 4️⃣ Solução Proposta — Recriação do Zero

> **Por ser projeto em fase inicial**, a abordagem é **recriar todos os arquivos Liquibase do zero**, eliminando o histórico problemático e estabelecendo uma base sólida.

### 4.1 Estrutura de Pastas — Nova Estrutura (conforme `api-development-guidelines.md`)

> **Módulo**: `scos-organization-boot` (conforme `api-development-guidelines.md` linha 199)
> **Regra**: `function/`, `view/`, `triggers/` e `procedure/` ficam no **nível raiz** de `db/changelog/` — fora da pasta de versão — pois são gerenciados via `runOnChange: true`. A pasta de versão `v1.0.0/` contém apenas DDL versionado (tables + insert).

```
scos-organization-boot/src/main/resources/db/changelog/
├── db.changelog-master.yml         # renomear de .yaml para .yml
├── property.yml                    # simplificado (apenas PostgreSQL)
├── function/                       # nível raiz — runOnChange: true
│   ├── function.yml
│   ├── check_employee_supervisor.sql
│   ├── check_company_parent_company.sql
│   └── remove_formatting_tax_identifier.sql
├── view/                           # nível raiz — runOnChange: true (era views/)
│   ├── view.yml
│   ├── v_scos_cache_login.sql
│   └── v_scos_cache_permission.sql
├── procedure/                      # nível raiz — vazio no momento
│   └── procedure.yml
├── triggers/                       # nível raiz — changeSet versionado (era trigger/)
│   ├── triggers.yml
│   ├── trg_check_employee_supervisor.sql
│   ├── trg_check_company_parent_company.sql
│   ├── trg_remove_formatting_tax_identifier_company.sql
│   └── trg_remove_formatting_tax_identifier_employee.sql
└── v1.0.0/                         # era v1/0/0/
    ├── v1.0.0.yml
    ├── tables/
    │   ├── tables.yml
    │   ├── scos_department.yml
    │   ├── scos_position.yml
    │   ├── scos_company.yml
    │   ├── scos_company_address.yml
    │   ├── scos_company_contact.yml
    │   ├── scos_employee.yml
    │   ├── scos_employee_address.yml
    │   ├── scos_employee_contact.yml
    │   ├── scos_permission.yml
    │   ├── scos_profile.yml
    │   ├── scos_login.yml
    │   ├── scos_integration_keycloak.yml
    │   ├── scos_integration_keycloak_log.yml
    │   ├── scos_integration_message_invalid.yml
    │   └── scos_configuration.yml
    └── insert/
        ├── insert.yml
        └── configure_system.sql
```

**Resumo das mudanças estruturais** em relação à situação atual:

| Aspecto | Situação atual (incorreta) | Nova estrutura (guidelines) |
|---|---|---|
| Módulo | `scos-organization-boot` | `scos-organization-boot` ✅ *(sem mudança de módulo)* |
| Pasta de versão | `v1/0/0/` | `v1.0.0/` |
| Pasta de views | `v1/0/0/views/` (dentro da versão) | `view/` (raiz de changelog) |
| Pasta de triggers | `v1/0/0/trigger/` (dentro da versão) | `triggers/` (raiz de changelog) |
| Pasta de funções | `v1/0/0/function/` (dentro da versão) | `function/` (raiz de changelog) |
| `init.yml` | Presente como intermediário | Removido — master inclui v1.0.0 diretamente |
| `procedure/` | Ausente | Criado vazio (raiz de changelog) |

### 4.2 Padrão de ID de ChangeSet
```
<YYYYMMDD>-<author>-<NNN>
Exemplos:
  20260228-Samuel.Cunha-001   ← tag v1.0.0 (ÚNICO tag, posicionado ANTES de tudo)
  20260228-Samuel.Cunha-002   ← createSequence SEQ_DEPARTMENT_ID
  20260228-Samuel.Cunha-003   ← createTable SCOS_DEPARTMENT
  ...
```

> **Revisão @arquiteto (R5)**: Usar **apenas UM tag** com nome `v1.0.0`, posicionado como **primeiro changeSet** (antes de todas as tabelas). Não usar "tag de fim" — isso inverte a semântica do `rollbackTag`. O comando `rollbackTag=v1.0.0` desfaz tudo **após** o tag, portanto o tag deve estar no início para que o rollback desfaça toda a versão.

### 4.3 Template de ChangeSet (DDL)
```yaml
- changeSet:
    id: 20260228-Samuel.Cunha-003
    author: Samuel.Cunha
    comment: "Cria tabela SCOS_DEPARTMENT"
    dbms: postgresql
    changes:
      - createTable:
          tableName: SCOS_DEPARTMENT
          ...
    rollback:
      - dropTable:
          cascadeConstraints: true
          tableName: SCOS_DEPARTMENT
```

### 4.4 Template de ChangeSet (tagDatabase — changeSet dedicado)
```yaml
- changeSet:
    id: 20260228-Samuel.Cunha-001
    author: Samuel.Cunha
    comment: "Tag de início da versão v1.0.0"
    changes:
      - tagDatabase:
          tag: v1.0.0
    rollback: []
```

### 4.5 Template de ChangeSet (sqlFile — função/trigger/view)

> **Decisão do usuário (2026-02-28)**: Adotar `runOnChange: true` para funções e views SQL — simplifica manutenção futura re-executando quando o SQL muda, sem necessidade de novo changeSet.

```yaml
- changeSet:
    id: 20260228-Samuel.Cunha-045
    author: Samuel.Cunha
    comment: "Cria função scos.check_employee_supervisor"
    dbms: postgresql
    runOnChange: true
    changes:
      - sqlFile:
          path: check_employee_supervisor.sql
          relativeToChangelogFile: true
          encoding: utf8
          splitStatements: false
    rollback:
      - sql:
          sql: DROP FUNCTION IF EXISTS scos.check_employee_supervisor();
```

> **Nota**: `runOnChange: true` aplica-se a **funções** e **views**. Triggers NÃO usam `runOnChange` pois dependem das funções e têm lógica de criação mais complexa — para triggers, usar novo changeSet versionado.

### 4.6 Template de Sequência + Tabela (substituindo serial + renameSequence)
```yaml
- changeSet:
    id: 20260228-Samuel.Cunha-002
    author: Samuel.Cunha
    comment: "Cria sequência SEQ_DEPARTMENT_ID"
    dbms: postgresql
    changes:
      - createSequence:
          sequenceName: SEQ_DEPARTMENT_ID
          schemaName: scos          # +@dba F5.4 — schema explícito obrigatório
          dataType: BIGINT
          startValue: 1
          incrementBy: 50    # Alinhado com allocationSize=50 do @SequenceGenerator Hibernate
          minValue: 1
          cycle: false             # +@dba F5.1 — defensive DDL
    rollback:
      - dropSequence:
          sequenceName: SEQ_DEPARTMENT_ID
          schemaName: scos

- changeSet:
    id: 20260228-Samuel.Cunha-003
    author: Samuel.Cunha
    comment: "Cria tabela SCOS_DEPARTMENT"
    dbms: postgresql
    changes:
      - createTable:
          tableName: SCOS_DEPARTMENT
          schemaName: scos
          columns:
            - column:
                name: DEPARTMENT_ID
                type: BIGINT
                defaultValueSequenceNext: scos.SEQ_DEPARTMENT_ID  # +@dba F5.4 — schema qualificado
                constraints:
                  primaryKey: true
                  primaryKeyName: PK_SCOS_DEPARTMENT
                  nullable: false
            ...
    rollback:
      - dropTable:
          cascadeConstraints: true
          tableName: SCOS_DEPARTMENT
          schemaName: scos
```

### 4.7 Mapeamento de Tipos Corrigidos
| Tipo atual (errado) | Tipo correto | Observação |
|---|---|---|
| `DATETIME` | `TIMESTAMPTZ` | Para campos de auditoria (`CREATED_AT`, `UPDATED_AT`) e instantes de processo |
| `DATETIME` (data sem hora) | `DATE` | Campos `DATE_CREATED`, `BIRTH_DATE`, `DATE_OF_HIRING` já corretos — não alterar |
| `DATETIME` em `SCOS_INTEGRATION_KEYCLOAK` | `TIMESTAMPTZ` | `DATE_CREATED`, `DATE_START`, `DATE_END` são instantes de processo — devem ser `TIMESTAMPTZ` |
| `LONGTEXT` | `TEXT` | Para campos de texto ilimitado |
| `serial` | `BIGINT` + `defaultValueSequenceNext` | PK com sequência explícita |
| `BOOLEAN` (PostgreSQL) | `BOOLEAN` | Já correto; remover variável `${typeBoolean}` |
| `TEXT[]` | `TEXT[]` com `dbms: postgresql` | Arrays PostgreSQL |
| `JSONB` | `JSONB` com `dbms: postgresql` | JSON binário PostgreSQL |

### 4.8 Índices FK e especiais a criar
| Tabela | Coluna | Tipo | Nome do índice | Justificativa |
|---|---|---|---|---|
| `SCOS_EMPLOYEE` | `COMPANY_ID` | BTREE | `IDX_COMPANY_ID_SCOS_EMPLOYEE` | FK |
| `SCOS_EMPLOYEE` | `POSITION_ID` | BTREE | `IDX_POSITION_ID_SCOS_EMPLOYEE` | FK |
| `SCOS_EMPLOYEE` | `SUPERVISOR_ID` | BTREE | `IDX_SUPERVISOR_ID_SCOS_EMPLOYEE` | FK auto-referencial |
| `SCOS_COMPANY` | `PARENT_COMPANY_ID` | BTREE | `IDX_PARENT_COMPANY_ID_SCOS_COMPANY` | **+@dba B2** FK auto-referencial — queries de hierarquia |
| `SCOS_COMPANY_ADDRESS` | `COMPANY_ID` | BTREE | `IDX_COMPANY_ID_SCOS_COMPANY_ADDRESS` | FK |
| `SCOS_COMPANY_CONTACT` | `COMPANY_ID` | BTREE | `IDX_COMPANY_ID_SCOS_COMPANY_CONTACT` | FK |
| `SCOS_EMPLOYEE_ADDRESS` | `EMPLOYEE_ID` | BTREE | `IDX_EMPLOYEE_ID_SCOS_EMPLOYEE_ADDRESS` | FK |
| `SCOS_EMPLOYEE_CONTACT` | `EMPLOYEE_ID` | BTREE | `IDX_EMPLOYEE_ID_SCOS_EMPLOYEE_CONTACT` | FK |
| `SCOS_LOGIN` | `PROFILE_ID` | BTREE | `IDX_PROFILE_ID_SCOS_LOGIN` | FK |
| `SCOS_LOGIN` | `EMPLOYEE_ID` | BTREE | `IDX_EMPLOYEE_ID_SCOS_LOGIN` | FK |
| `SCOS_POSITION` | `DEPARTMENT_ID` | BTREE | `IDX_DEPARTMENT_ID_SCOS_POSITION` | FK |
| `SCOS_INTEGRATION_KEYCLOAK_LOG` | `INTEGRATION_KEYCLOAK_ID` | BTREE | `IDX_INTEGRATION_KEYCLOAK_ID_LOG` | FK |
| `SCOS_PROFILE` | `FEATURES` | **GIN** | `IDX_GIN_FEATURES_SCOS_PROFILE` | **+@dba R4** — queries `array @> ARRAY['feature']` |
| `SCOS_INTEGRATION_KEYCLOAK_LOG` | `STATUS, DATE_CREATED` | BTREE | `IDX_STATUS_DATE_SCOS_INTEGRATION_KEYCLOAK_LOG` | **+@dba D2** — queries de monitoramento |

> **+@dba F5.2/F5.3** — O `createIndex` GIN em `SCOS_PROFILE.FEATURES` exige os atributos `indexType: gin` e `dbms: postgresql` explícitos, caso contrário o Liquibase cria BTREE silenciosamente:
> ```yaml
> - createIndex:
>     indexName: IDX_GIN_FEATURES_SCOS_PROFILE
>     tableName: SCOS_PROFILE
>     schemaName: scos
>     indexType: gin
>     dbms: postgresql
>     columns:
>       - column:
>           name: FEATURES
> ```

---

## 5️⃣ Plano de Execução

### FASE 1 — Arquivos de Configuração e Entrada
| # | Arquivo | Ação | Prioridade |
|---|---|---|---|
| 1.1 | `scos-organization-boot/src/main/resources/db/changelog/db.changelog-master.yaml` | Renomear para `db.changelog-master.yml` (`.yaml` → `.yml`) | 🔴 Alta |
| 1.1a | `scos-organization-boot/bootstrap.yml` | **+@arquiteto B1+N1** Atualizar `scos.liquibase.change-log` (este é o único arquivo com essa propriedade) de `.yaml` → `.yml`; também corrigir o placeholder default: `${scos.liquibase.enabled:false}` (era `:true` — **+@arquiteto N2**) | 🔴 **BLOQUEANTE** |
| 1.1b | `scos-organization-boot/liquibase.properties` (se existir) | Atualizar `changelogFile=/db/changelog/db.changelog-master.yaml` → `.yml` | 🔴 **BLOQUEANTE** |
| 1.2 | `scos-organization-boot/src/main/resources/db/changelog/property.yml` | Remover propriedades MySQL/Oracle/MSSQL; manter apenas PostgreSQL; remover `${typeBoolean}` | 🔴 Alta |
| 1.3 | `scos-organization-boot/src/main/resources/db/changelog/init.yml` | **Remover** — master passará a incluir `v1.0.0/v1.0.0.yml` diretamente (sem intermediário) | 🔴 Alta |
| 1.4 | `db.changelog-master.yml` | Reescrever includes na ordem correta (**+@arquiteto N4/N8**): `property.yml` **primeiro** e obrigatório → `v1.0.0/v1.0.0.yml` → `function/function.yml` → `triggers/triggers.yml` → `view/view.yml` → `procedure/procedure.yml` | 🔴 Alta |
| 1.5 | `scos-organization-boot/src/main/resources/application.yml` | **+@arquiteto R3** Criar `application-local.yml` com `scos.liquibase.enabled: true` para uso no dev — não alterar `application.yml` | 🟡 Média |
| 1.6 | `scos-organization-boot/src/main/java/**/*Application.java` | **+@arquiteto N3** Verificar se `LiquibaseAutoConfiguration` está excluído via `@SpringBootApplication(exclude = LiquibaseAutoConfiguration.class)` **ou** adicionar `spring.liquibase.enabled: false` em `bootstrap.yml`. Sem isso, o Spring Boot 4.x pode tentar executar Liquibase com o path antigo `.yaml` em paralelo ao custom `LiquibaseConfig`. | 🔴 **BLOQUEANTE** |
| 1.7 | Testes de integração (TestContainers) | **+@arquiteto N5** Configurar `@Container PostgreSQLContainer` com `.withInitScript("db/install_database.sql")` para criar schema `scos` antes do Hibernate `ddl-auto: validate`. Sem isso, todos os testes de integração falham com schema inexistente. Habilitar `scos.liquibase.enabled=true` no perfil de teste. | 🔴 **BLOQUEANTE** |

### FASE 2 — Reestruturação de Pastas e Tabelas

**2.0 — Reorganização de pastas** (antes de editar qualquer arquivo):
1. Criar `scos-organization-boot/src/main/resources/db/changelog/v1.0.0/` (substitui `v1/0/0/`)
2. Mover `v1/0/0/tables/` → `v1.0.0/tables/`
3. Mover `v1/0/0/insert/` → `v1.0.0/insert/`
4. Mover `v1/0/0/function/` → nível raiz `function/`
5. Mover `v1/0/0/trigger/` → nível raiz `triggers/` *(renomear pasta)*
6. Mover `v1/0/0/views/` → nível raiz `view/` *(renomear pasta)*
7. Criar pasta vazia `procedure/` com `procedure.yml` vazio no nível raiz
8. Remover pasta `v1/0/0/` após migração

**Recriação completa de cada arquivo de tabela** — ordem preserva dependências:

| # | Arquivo | Mudanças principais |
|---|---|---|
| 2.1 | `scos_department.yml` | Unificar os 2 changeSets em 1; `createSequence` explícito; `BIGINT` PK; `TIMESTAMPTZ`; índice FK; rollback; comment |
| 2.2 | `scos_position.yml` | Unificar os 2 changeSets em 1; `createSequence` explícito; `BIGINT` PK; `ACTIVE`; `TIMESTAMPTZ`; índice FK; rollback; comment |
| 2.3 | `scos_company.yml` | `createSequence` explícito; `BIGINT` PK; `TEXT` no lugar de `LONGTEXT`; `TIMESTAMPTZ`; índice FK `PARENT_COMPANY_ID` (+@dba B2); `OBSERVATION` → `nullable: true` (+@especialista R3); rollback; comment |
| 2.4 | `scos_company_address.yml` | `createSequence` explícito; `TEXT` lugar de `LONGTEXT`; `TIMESTAMPTZ`; `constraintName` na unique; índice FK; rollback; comment |
| 2.5 | `scos_company_contact.yml` | `createSequence` explícito; `TIMESTAMPTZ`; índice FK; rollback (faltante); comment |
| 2.6 | `scos_employee.yml` | `createSequence` explícito; `TEXT` lugar de `LONGTEXT`; `TIMESTAMPTZ`; reordenar `ACTIVE`; índices FK; rollback; comment |
| 2.7 | `scos_employee_address.yml` | `TIMESTAMPTZ`; `TEXT` lugar de `LONGTEXT`; índice FK; `ADDRESS_ID` + `remarks: "ID externo do serviço de endereços"` (+@dba R2); rollback; comment |
| 2.8 | `scos_employee_contact.yml` | `createSequence` explícito; `TIMESTAMPTZ`; índice FK; comment |
| 2.9 | `scos_permission.yml` | `TIMESTAMPTZ`; `dbms: postgresql` na tabela; rollback (faltante); comment |
| 2.10 | `scos_profile.yml` | `createSequence` explícito; `TIMESTAMPTZ`; `dbms: postgresql` na tabela; índice GIN em `FEATURES` (+@dba R4); rollback; comment |
| 2.11 | `scos_login.yml` | `createSequence` explícito; `TIMESTAMPTZ`; remover `autoIncrement: false`; índices FK; rollback; comment |
| 2.11a | `scos_login.yml` | **+@dba B-NEW-3** Adicionar changeSet `addCheckConstraint` para `STATUS IN ('PENDING_PASSWORD_CHANGE', 'ACTIVE', 'INACTIVE', 'BLOCKED')` com rollback `dropCheckConstraint` |
| 2.12 | `scos_integration_keycloak.yml` | `TIMESTAMPTZ`; `dbms: postgresql` (JSONB); rollback (incompleto); comment |
| 2.13 | `scos_integration_keycloak_log.yml` | `TIMESTAMPTZ`; remover `autoIncrement: false`; índice FK; rollback; comment |
| 2.14 | `scos_integration_message_invalid.yml` | `TIMESTAMPTZ`; rollback; comment |
| 2.15 | `scos_configuration.yml` | `createSequence` explícito; `TIMESTAMPTZ`; remover `autoIncrement: true/false` redundantes; rollback; comment |

**Padrão obrigatório para TODOS os arquivos de tabela** (+@dba B1):
- `CREATED_AT`: `defaultValueComputed: "NOW()"` + `nullable: false`
- `UPDATED_AT`: `nullable: true` (preenchido pela camada JPA via `@PreUpdate`)
- `incrementBy: 50` em todas as sequências (alinhado com `allocationSize=50` Hibernate)

### FASE 3 — Funções SQL e Changelogs (function/ — nível raiz)
> Após FASE 2.0, os arquivos já estarão em `db/changelog/function/`.

| # | Arquivo | Mudanças |
|---|---|---|
| 3.0 | `function/function.yml` | **+@dba B-NEW-1** Adicionar changeSet `20260228-Samuel.Cunha-044` de limpeza `DROP FUNCTION IF EXISTS public.check_employee_supervisor()` e `DROP FUNCTION IF EXISTS public.remove_formatting_tax_identifier()` **antes** dos changeSets das funções corrigidas; rollback vazio |
| 3.1 | `function/check_employee_supervisor.sql` | Adicionar schema `scos.`; usar `CREATE OR REPLACE FUNCTION scos.check_employee_supervisor()` (+@arquiteto P4) |
| 3.2 | `function/check_company_parent_company.sql` | Já tem `scos.` — verificar e adicionar `OR REPLACE` (+@arquiteto P4) |
| 3.3 | `function/remove_formatting_tax_identifier.sql` | Adicionar schema `scos.`; usar `CREATE OR REPLACE FUNCTION scos.remove_formatting_tax_identifier()` (+@arquiteto P4) |
| 3.4 | `function/function.yml` | IDs padronizados; `dbms: postgresql`; **`runOnChange: true`** (decisão do usuário); `splitStatements: false` sem `endDelimiter`; rollback (`DROP FUNCTION IF EXISTS scos.*`); comment |

### FASE 4 — Triggers SQL e Changelogs (triggers/ — nível raiz)
> Após FASE 2.0, os arquivos já estarão em `db/changelog/triggers/` (renomeado de `trigger/`).

| # | Arquivo | Mudanças |
|---|---|---|
| 4.1 | `triggers/trg_check_employee_supervisor.sql` | Atualizar referência da função para `scos.check_employee_supervisor` |
| 4.2 | `triggers/trg_check_company_parent_company.sql` | Atualizar referência da função para `scos.check_company_parent_company` |
| 4.3 | `triggers/trg_remove_formatting_tax_identifier_company.sql` | Adicionar `OR REPLACE`; atualizar referência da função para `scos.remove_formatting_tax_identifier` |
| 4.4 | `triggers/trg_remove_formatting_tax_identifier_employee.sql` | Adicionar `OR REPLACE`; atualizar referência da função para `scos.remove_formatting_tax_identifier` |
| 4.5 | `triggers/triggers.yml` | **Corrigir bug** (2º changeSet aponta para arquivo errado); renomear de `trigger.yml` → `triggers.yml`; IDs padronizados; `dbms: postgresql`; `splitStatements: false` sem `endDelimiter`; rollback (`DROP TRIGGER IF EXISTS`); comment |

### FASE 5 — Insert e Views
> Insert permanece em `v1.0.0/insert/`; views movem para `view/` no nível raiz (FASE 2.0 já terá feito o mv).

**⚠️ FASE 5 BLOQUEADA** até resolução de:
- **B-NEW-4**: `configure_system.sql` atual é apenas um stub de 3 linhas (INSERT ... SELECT scos_permission). O seed completo não foi escrito. (**+@especialista 4/5**)
- **B-3a**: Confirmar Mecanismo de features do ADMIN antes de codificar o seed (ver §[PONTO DE PARADA]).

| # | Arquivo | Mudanças |
|---|---|---|
| 5.0 | `v1.0.0/insert/configure_system.sql` | **+@especialista 4 — B-NEW-4** Reescrever do zero na ordem FK: **(1)** `scos_company` [tax_identifier='00000000000191', observation=NULL] → **(2)** `scos_department` [active=true] → **(3)** `scos_position` [active=true] → **(4)** `scos_permission` [~47 códigos de `x-authorize` — ver item 5.0a] → **(5)** `scos_profile` [ADMIN com features ARRAY[] — ver ponto B-3a] → **(6)** `scos_employee` → **(7)** `scos_login` [status='PENDING_PASSWORD_CHANGE', date_last_change_password=NULL] → **(8)** `scos_profile_permission` (INSERT...SELECT existente) | 🔴 **BLOQUEANTE** |
| 5.0a | Inventário de `scos_permission` | **+@especialista 8** Mapear todos os ~47 códigos `x-authorize` ativos nos OpenAPI como permissões a inserir. Adicionar tabela ao plano antes de codificar o seed. | 🔴 **BLOQUEANTE** |
| 5.1 | `v1.0.0/insert/configure_system.sql` | **+@especialista B1** `active = true` nos inserts de `scos_department` e `scos_position` (coberto por item 5.0) |
| 5.1b | `v1.0.0/insert/configure_system.sql` | **+@especialista B2** ✅ **DECIDIDO pelo usuário**: usar `'PENDING_PASSWORD_CHANGE'` (coberto por item 5.0) |
| 5.1c | `v1.0.0/insert/configure_system.sql` | **+@especialista R1** `date_last_change_password = NULL` (coberto por item 5.0) |
| 5.1d | `v1.0.0/insert/configure_system.sql` | **+@especialista R2 + B-3a** Features do perfil ADMIN — lista completa após decisão do usuário (ver [PONTO DE PARADA]) |
| 5.1e | `v1.0.0/insert/configure_system.sql` | **+@especialista R4** `tax_identifier` = `'00000000000191'` (coberto por item 5.0) |
| 5.2 | `v1.0.0/insert/insert.yml` | IDs padronizados; `dbms: postgresql`; rollback com ordem FK correta (+@especialista R6): Login → Employee → `scos_profile_permission` → Profile → Position → Department → Company; comment |
| 5.3 | `view/v_scos_cache_login.sql` | Sem mudança de lógica; garantir `CREATE OR REPLACE VIEW scos.v_scos_cache_login` (+@arquiteto P4) |
| 5.4 | `view/v_scos_cache_permission.sql` | Sem mudança de lógica; garantir `CREATE OR REPLACE VIEW scos.v_scos_cache_permission` (+@arquiteto P4) |
| 5.5 | `view/view.yml` | Renomear de `views.yml` → `view.yml`; corrigir indentação; IDs padronizados; `dbms: postgresql`; **`runOnChange: true`** (decisão do usuário); `splitStatements: false` sem `endDelimiter`; rollback (`DROP VIEW IF EXISTS`); comment; **nota**: mudanças estruturais de coluna requerem novo changeSet (+@dba R-NEW-1) |
| 5.6 | Redis (passo de deploy) | **+@especialista S** Documentar `FLUSHDB` Redis quando `view/view.yml` for modificado em deploy — evita cache desatualizado após alteração de views |

### FASE 6 — Arquivos de Orquestração
| # | Arquivo | Mudanças |
|---|---|---|
| 6.1 | `v1.0.0/v1.0.0.yml` | Adicionar changeSet `tagDatabase: v1.0.0` como **primeiro changeSet** (antes dos includes); includes apenas: `tables/tables.yml` e `insert/insert.yml` — **único tag, sem tag de fim** (+@arquiteto R5). **Nota (+@arquiteto N9)**: o exemplo em `api-development-guidelines.md` contém a violação 1 (tagDatabase misturado com DDL) — seguir o template §4.4 deste plano, não o exemplo do guideline. |
| 6.2 | `db.changelog-master.yml` | Ordem de includes (**+@arquiteto N4 — obrigatório**): `property.yml` **primeiro** → `v1.0.0/v1.0.0.yml` → `function/function.yml` → `triggers/triggers.yml` → `view/view.yml` → `procedure/procedure.yml`. Adicionar comentário de dependência (+@especialista 6): `# ATENÇÃO: function deve preceder triggers — triggers dependem das funções scos.*` |
| 6.3 | Sequência global | Ordem final de execução: **tag-v1.0.0** → tables → insert → functions → triggers → views |
| 6.4 | `procedure/procedure.yml` | Criar com conteúdo mínimo válido (**+@dba F4.2/@arquiteto N7**): `databaseChangeLog:` (lista nula, sem `[]`). Evitar `databaseChangeLog: []` que pode emitir WARN no Liquibase 4.25+ ou NPE em versões < 4.8. |
| 6.5 | `scos-organization-boot/src/main/resources/application-local.yml` | Criar com `scos.liquibase.enabled: true` para validação local das migrations (+@arquiteto R3) |

---

## 6️⃣ Checklist de Validação Pós-Execução

**Pré-condição obrigatória (+@dba F1/F2)**
- [ ] **Clean install confirmada**: schema `scos`, `DATABASECHANGELOG` e `DATABASECHANGELOGLOCK` não existem no banco alvo. Esta migration é incompatível com ambientes que executaram os changeSets antigos (`SGC_*`). Executar `install_database.sql` do zero antes de qualquer FASE.

**Estrutura e configuração**
- [ ] `scos-organization-boot` — `bootstrap.yml` (ou `application.yml`) e `liquibase.properties` atualizados para `db.changelog-master.yml` (+@arquiteto B1)
- [ ] `bootstrap.yml`: placeholder `${scos.liquibase.enabled:false}` (default `false`) (+@arquiteto N2)
- [ ] `LiquibaseAutoConfiguration` excluído via `exclude` na `@SpringBootApplication` **ou** `spring.liquibase.enabled: false` em `bootstrap.yml` (+@arquiteto N3)
- [ ] `application-local.yml` criado com `scos.liquibase.enabled: true`
- [ ] `property.yml` é o **primeiro** include em `db.changelog-master.yml` (+@arquiteto N4)
- [ ] `mvn liquibase:validate` sem erros (com `scos.liquibase.enabled=true`)
- [ ] `mvn liquibase:updateSQL` gera SQL sem `DATETIME` ou `LONGTEXT`
- [ ] Em caso de falha durante apply: executar `mvn liquibase:releaseLocks` antes de nova tentativa (+@dba F4.3)

**Banco de dados**
- [ ] `docker-compose` sobe banco limpo e aplica todas as migrations sem erro
- [ ] Rollback completo funciona: `mvn liquibase:rollback -Dliquibase.rollbackTag=v1.0.0`
- [ ] Trigger `trg_check_company_parent_company` é criado (bug corrigido)
- [ ] Funções estão no schema `scos` (via `\df scos.*` no psql)
- [ ] Índices FK existem: `SELECT indexname FROM pg_indexes WHERE tablename = 'scos_employee'`
- [ ] Índice `IDX_PARENT_COMPANY_ID_SCOS_COMPANY` existe (+@dba B2)
- [ ] Índice GIN `IDX_GIN_FEATURES_SCOS_PROFILE` existe com tipo GIN (não BTREE) (+@dba F5.2)
- [ ] `CREATED_AT` tem `DEFAULT NOW()` — verificar via `\d scos_company`
- [ ] Sequências criadas no schema `scos` (via `SELECT sequencename, schemaname FROM pg_sequences WHERE schemaname = 'scos'`) (+@dba F5.4)
- [ ] Verificar `DATABASECHANGELOG.FILENAME` aponta para paths `v1.0.0/` e `function/`, `triggers/`, `view/` (não `v1/0/0/`)

**Dados iniciais**
- [ ] `scos_department` e `scos_position` inseridos com `active = true` (+@especialista B1)
- [ ] `scos_login.status` = `'PENDING_PASSWORD_CHANGE'` (+@especialista B2)
- [ ] `configure_system.sql` atualizado — substituir `'ENABLE'` por `'PENDING_PASSWORD_CHANGE'` no insert de login (+@dba B-NEW-2)
- [ ] Perfil ADMIN tem todas as features de administração (+@especialista R2)
- [ ] Naming de features alinhado com mecanismo de autorização real — **validar antes de codar o seed** (+@especialista B-3a)
- [ ] `scos_company.tax_identifier` = `'00000000000191'` (+@especialista R4)
- [ ] `scos_company.observation` aceita `NULL` (+@especialista R3)
- [ ] `scos_permission` tem inventário completo (~47 códigos `x-authorize`) (+@especialista 8)

**Hibernate**
- [ ] `@SequenceGenerator(allocationSize = 50)` alinhado com `incrementBy: 50` das sequências (+@dba R1)
- [ ] Entidades JPA com `CREATED_AT/UPDATED_AT` usam `Instant` ou `OffsetDateTime` (não `LocalDateTime`) para compatibilidade com `TIMESTAMPTZ` (+@arquiteto N6)
- [ ] `mvn spring-boot:run` sobe sem erros de `ddl-auto: validate` (+@arquiteto R4)
- [ ] Testes de integração (TestContainers) passam com `install_database.sql` como init script (+@arquiteto N5)

---

## 7️⃣ Riscos e Mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Renomear `master.yaml` sem atualizar `bootstrap.yml` e `liquibase.properties` | Alto | **Crítico** | Atualizar os 3 arquivos em conjunto na FASE 1 (+@arquiteto B1) |
| Reordenação de changeSets quebra dependências entre tabelas | Médio | Alto | Manter sequência de `tables.yml` exatamente como está |
| `scos.` schema não existir no banco | Baixo | Alto | Schema criado em `install_database.sql` — não duplicar via Liquibase |
| **Aplicação em ambiente com changeSets antigos (`SGC_*`)** | Alto | **Crítico** | **+@dba F1/F2** Esta migration requer clean install obrigatória. Qualquer banco com `SGC_*` no `DATABASECHANGELOG` deve ter todo o schema `scos` e tabelas de controle Liquibase recriadas antes da aplicação. |
| TestContainers sem `install_database.sql` como init script | Alto | **Crítico** | **+@arquiteto N5** Escalado a bloqueante — `ddl-auto: validate` falha com schema inexistente. Configurar `.withInitScript("db/install_database.sql")` na FASE 1.7. |
| `LiquibaseAutoConfiguration` do Spring Boot subindo em paralelo | Alto | **Crítico** | **+@arquiteto N3** Verificar exclusão via `@SpringBootApplication(exclude)` ou `spring.liquibase.enabled: false`. Resolver na FASE 1.6. |
| `property.yml` ausente ou fora de ordem no master | Alto | **Crítico** | **+@arquiteto N4** `property.yml` deve ser o **primeiro** include. Sem ele, variáveis Liquibase ficam sem resolução. |
| `allocationSize` Hibernate vs `incrementBy` sequência desalinhados | Alto | Médio | Usar `incrementBy: 50` + `@SequenceGenerator(allocationSize=50)` (+@dba R1) |
| `createSequence` sem `schemaName: scos` | Alto | **Crítico** | **+@dba F5.4** Sequências criadas no schema errado (`public`). Adicionar `schemaName: scos` e usar `scos.SEQ_*` em `defaultValueSequenceNext`. |
| `createIndex` GIN sem `indexType: gin` | Alto | Alto | **+@dba F5.2** Sem `indexType: gin`, Liquibase cria BTREE silenciosamente. Ver template em §4.8. |
| Hibernate `ddl-auto: validate` rejeita tipos renomeados | Médio | Alto | Verificar todas as entidades JPA após as migrations (+@arquiteto R4); `LocalDateTime` incompatível com `TIMESTAMPTZ` (+@arquiteto N6) |
| `scos_login.STATUS` sem `CHECK` constraint | Alto | **Crítico** | Adicionar `addCheckConstraint` com valores permitidos em `scos_login.yml` (+@dba B-NEW-3) |
| Funções sem `scos.` geram duplicata em `public.` com `runOnChange` | Alto | **Crítico** | Changesets de limpeza de `public.*` antes das funções corrigidas (+@dba B-NEW-1) |
| Views de cache c/ `runOnChange` — mudanças estruturais quebram `OR REPLACE` | Médio | Alto | Novo changeSet com `DROP+CREATE` para mudanças de colunas em views (+@dba R-NEW-1) |
| Naming de features ADMIN diverge de `x-authorize` do OpenAPI | Alto | **Crítico** | Definir mecanismo de autorização antes de codar o seed (+@especialista B-3a) |
| `configure_system.sql` incompleto (stub de 3 linhas) | Alto | **Crítico** | **+@especialista 4** Reescrever do zero com todos os INSERTs na ordem FK correta. Bloqueia FASE 5. |
| `login-business-rules.md` sem `PENDING_PASSWORD_CHANGE` no ciclo de vida | Médio | Médio | Atualizar documento de regras de negócio (+@especialista R-1a) |
| Cache Redis desatualizado após alteração de views em deploy | Baixo | Médio | Documentar `FLUSHDB` Redis no processo de deploy (+@especialista S) |

---

## 8️⃣ Delegações

**1ª Rodada (concluída)**
- 🗻️ **@arquiteto**: ✅ Revisão concluída — ver §9.1
- 🗤️🏽 **@dba**: ✅ Revisão concluída — ver §9.2
- 🎯 **@especialista**: ✅ Revisão concluída — ver §9.3

**2ª Rodada (pós-decisões do usuário — concluída)**
- 🗤️🏽 **@dba**: ✅ Re-revisão concluída — ver §9.4
- 🗻️ **@arquiteto**: ✅ Re-revisão concluída — ver §9.5
- 🎯 **@especialista**: ✅ Re-revisão concluída — ver §9.6

**3ª Rodada (pós-atualização estrutural de pastas e módulo — concluída)**
- 🗻️ **@arquiteto**: ✅ Re-revisão concluída — ver §9.7
- 🗤️🏽 **@dba**: ✅ Re-revisão concluída — ver §9.8
- 🎯 **@especialista**: ✅ Re-revisão concluída — ver §9.9

---

## 9️⃣ Pareceres dos Agentes

### 9.1 — 🏗️ @arquiteto
**Veredicto**: ⚠️ Aprovado com Ressalvas — 1 bloqueio resolvido no plano

| # | Ponto | Status | Impacto |
|---|---|---|---|
| B1 | Atualizar `bootstrap.yml` e `liquibase.properties` na renomeação | ❌→✅ Incorporado | Alto |
| R1 | TestContainers precisa de `install_database.sql` como init script | ⚠️ Ressalva | Alto |
| R2 | Estrutura `v1/0/0/` — convenção, não bloqueio | ⚠️ Ressalva | Médio |
| R3 | Liquibase desabilitado por padrão — criar `application-local.yml` | ⚠️→✅ Incorporado | Médio |
| R4 | Hibernate `ddl-auto: validate` requer entidades JPA alinhadas | ⚠️ Ressalva | Médio |
| R5 | Semântica de `tagDatabase`: apenas UM tag no início | ⚠️→✅ Incorporado | Médio |
| S2 | `runOnChange: true` para funções e views (evita novo ID a cada mudança) | 💡→✅ **DECIDIDO** — adotado para funções e views | Médio |

**Decisão pendente**: `v1/0/0/` vs `v1.0.0/` — apenas estética, sem impacto funcional. Manter como está.

---

### 9.2 — 🗄️ @dba
**Veredicto**: ⚠️ Aprovado com Ressalvas — 2 bloqueios resolvidos no plano

| # | Ponto | Status | Impacto |
|---|---|---|---|
| A1 | `TIMESTAMPTZ` para auditoria; `DATE` para datas de negócio | ✅ | Alto |
| A3 | `createSequence` explícito — correto para Hibernate | ✅ | Alto |
| A4 | Schema `scos.` já existe em `install_database.sql` | ✅ Confirmado | Alto |
| B1 | `CREATED_AT` precisa de `DEFAULT NOW()` — não era coberto pelo plano | ❌→✅ Incorporado | Alto |
| B2 | Índice `PARENT_COMPANY_ID` em `SCOS_COMPANY` estava faltando | ❌→✅ Incorporado | Alto |
| B3 | `SUPERVISOR_ID` — executar `EXPLAIN ANALYZE` após migration | ⚠️ Ressalva | Alto |
| R1 | `incrementBy: 50` alinhado com `allocationSize=50` Hibernate | ⚠️→✅ Incorporado | Alto |
| R2 | `ADDRESS_ID` em `EMPLOYEE_ADDRESS` — adicionar `remarks` de ID externo | ⚠️→✅ Incorporado | Médio |
| R4 | Índice GIN em `SCOS_PROFILE.FEATURES` | ⚠️→✅ Incorporado | Médio |
| D1 | `SCOS_LOGIN.USERNAME` e `EMAIL` — verificar se já existem UNIQUE constraints | 💡 Sugestão | Médio |
| D5 | `SCOS_CONFIGURATION` acessada frequentemente — verificar cache Redis | 💡 Sugestão | Baixo |

---

### 9.3 — 🎯 @especialista
**Veredicto**: 🛑 NÃO APROVADO — 2 bloqueios requerem decisão antes de implementar

| # | Ponto | Status | Impacto |
|---|---|---|---|
| B1 | `active` ausente em inserts de `scos_department` / `scos_position` | ❌→✅ Incorporado | Alto |
| B2 | Status `login`: `'ENABLE'` (OpenAPI) ≠ `'ACTIVE'` (regras de negócio) | ❌→✅ **DECIDIDO** — usar `'PENDING_PASSWORD_CHANGE'` | Alto |
| R1 | Senha hardcoded — adotar `PENDING_PASSWORD_CHANGE` + `date_last_change_password = NULL` | ⚠️→✅ Incorporado | Médio |
| R2 | Perfil ADMIN com features insuficientes (`ORGANIZATION_MANAGEMENT` apenas) | ⚠️→✅ Incorporado | Alto |
| R3 | `OBSERVATION` como `NOT NULL` em `SCOS_COMPANY` — deve ser nullable | ⚠️→✅ Incorporado | Médio |
| R4 | `tax_identifier` empresa alfanumérico → CNPJ numérico `'00000000000191'` | ⚠️→✅ Incorporado | Baixo |
| R6 | Rollback sem `scos_profile_permission` na ordem de delete | ⚠️→✅ Incorporado | Médio |

> **✅ Decisão tomada pelo usuário**: `LOGIN.STATUS` = `'PENDING_PASSWORD_CHANGE'` para o seed de dados iniciais. O OpenAPI deve ser atualizado para incluir esse valor. Adicionalmente, `runOnChange: true` foi adotado para funções e views.

---

### 9.4 — 🗄️ @dba (2ª Revisão)
**Veredicto**: ⚠️ Aprovado com Ressalvas — 3 novos bloqueantes identificados e incorporados

| # | Ponto | Classificação | Status |
|---|---|---|---|
| B-NEW-1 | Funções `check_employee_supervisor` e `remove_formatting_tax_identifier` sem `scos.` — `runOnChange` criará duplicata em `public.` silenciosamente | 🔴 Bloqueante | ✅ Incorporado — FASE 3 item 3.0 (changeSet limpeza) |
| B-NEW-2 | `configure_system.sql` ainda usa `'ENABLE'` — deve ser `'PENDING_PASSWORD_CHANGE'` | 🔴 Bloqueante | ✅ Incorporado — FASE 5 item 5.1b |
| B-NEW-3 | Ausência de `CHECK constraint` explícito em `STATUS` de `scos_login.yml` | 🔴 Bloqueante | ✅ Incorporado — FASE 2 item 2.11a |
| A-1 | `CREATE OR REPLACE FUNCTION` com `runOnChange` preserva OID — triggers não são quebrados | ✅ Aprovado | — |
| A-2 | `VARCHAR` para `STATUS` superior a `ENUM` PostgreSQL para evolução não-destrutiva | ✅ Aprovado | — condicionado ao CHECK constraint |
| A-3 | Ordem `tag → tables → functions → triggers → inserts → views` correta; views não dependem de funções customizadas | ✅ Aprovado | Manter exatamente como está |
| R-NEW-1 | `runOnChange` em views não suporta remoção/renomeação de colunas via `OR REPLACE` | ⚠️ Ressalva | ✅ Documentado em §4.5 e FASE 5.5 |
| R-NEW-2 | `DATABASECHANGELOG` perde histórico temporal de mudanças em funções/views (apenas última execução) | ⚠️ Ressalva | Sugestão: adicionar comentário de versão no cabeçalho dos SQLs |

---

### 9.5 — 🏗️ @arquiteto (2ª Revisão)
**Veredicto**: ⚠️ Aprovado com Ressalvas — 1 gap incorporado, ressalvas de 1ª rodada mantidas

| # | Ponto | Classificação | Status |
|---|---|---|---|
| P1 | `runOnChange: true` + `rollback` explícito — combinação válida e segura; rollback não é executado automaticamente na re-execução | ✅ Aprovado | — |
| P2 | Triggers excluídos de `runOnChange` — distinção arquitetural correta (dependência de funções, semântica de mudança intencional) | ✅ Aprovado | — |
| P3 | Impacto no checksum com `runOnChange: true` — comportamento previsível; qualquer mudança no `.sql` (inclusive comentário) dispara re-execução | ✅ Aprovado | Nota informativa |
| P4 | `CREATE OR REPLACE` obrigatório em todos `.sql` de função e view para suportar `runOnChange` | ⚠️ Ressalva | ✅ Incorporado — FASE 3 items 3.1–3.3; FASE 5 items 5.3–5.4; §4.5 |
| P5 | Template 4.5 — estrutura YAML validada em todos os atributos | ✅ Aprovado | — |
| R1 | TestContainers com `install_database.sql` como init script — sem atualização desta rodada | ⚠️ Ressalva mantida | Resolver antes de testes de integração |
| R4 | `ddl-auto: validate` — recomendação: verificar entidades JPA durante FASE 2, não ao final | ⚠️ Ressalva mantida | Verificar incrementalmente durante FASE 2 |

---

### 9.6 — 🎯 @especialista (2ª Revisão)
**Veredicto**: ⚠️ Aprovado com Ressalvas — 1 novo bloqueante descoberto (features ADMIN)

| # | Ponto | Classificação | Status |
|---|---|---|---|
| B-3a | Naming de features ADMIN (`ORGANIZATION_*`) diverge de `x-authorize` do OpenAPI (`GET_EMPLOYEE_LOGIN`, `CREATE_PROFILE`, etc.) — ADMIN pode ser inserido sem acesso funcional | 🔴 Bloqueante FASE 5 | ✅ Incorporado — aguarda decisão do usuário |
| R-1a | `PENDING_PASSWORD_CHANGE` ausente no ciclo de vida em `login-business-rules.md` | ⚠️ Ressalva | Atualizar doc de regras antes de FASE 5 |
| R-1c | Fluxo `PENDING_PASSWORD_CHANGE → ACTIVE` não documentado no `ChangePasswordUseCase` | ⚠️ Ressalva | Detalhar regra de transição de status na troca de senha |
| A-1b | OpenAPI **não precisa** ser atualizado — `PENDING_PASSWORD_CHANGE` já está presente na linha 722 do enum `LoginStatus` | ✅ Aprovado | — |
| A-3b | CNPJ `'00000000000191'` — dígitos verificadores validados matematicamente ✅ (CNPJ da União Federal) | ✅ Aprovado | — |
| A-4 | Ordem de rollback do insert (Login → Employee → `scos_profile_permission` → Profile → Position → Department → Company) respeita todas as FKs | ✅ Aprovado | — |
| S | Invalidação do cache Redis ao modificar views em produção — documentar `FLUSHDB` no processo de deploy | 💡 Sugestão | ✅ Incorporado — FASE 5 item 5.6 |

---

### 9.7 — 🗻️ @arquiteto (3ª Revisão)
**Veredicto**: ⚠️ Aprovado com Ressalvas — 3 novos bloqueantes identificados

| # | Ponto | Classificação | Status |
|---|---|---|---|
| N1 | Item 1.1a ambiguidade: `bootstrap.yml` é o único arquivo com `scos.liquibase.change-log` | ⚠️ Ressalva | ✅ Incorporado — FASE 1.1a |
| N2 | Placeholder `${scos.liquibase.enabled:true}` em `bootstrap.yml` — default incorreto, deveria ser `:false` | ⚠️ Ressalva | ✅ Incorporado — FASE 1.1a |
| N3 | `LiquibaseAutoConfiguration` pode sobrescrever o `LiquibaseConfig` custom com path `.yaml` antigo | 🔴 Bloqueante | ✅ Incorporado — FASE 1.6 |
| N4 | `property.yml` omitido da lista de includes do FASE 6.2; sem ele, variáveis Liquibase ficam sem resolução | 🔴 Bloqueante | ✅ Incorporado — FASE 1.4 e 6.2 |
| N5 | TestContainers sem `install_database.sql` como init script — escalado a bloqueante (Spring Boot 4.x + `ddl-auto: validate`) | 🔴 Bloqueante | ✅ Incorporado — FASE 1.7 |
| N6 | `LocalDateTime` incompatível com `TIMESTAMPTZ` no Hibernate strict `validate` | ⚠️ Ressalva | ✅ Incorporado — Checklist §6 |
| N7 | `procedure.yml` com `databaseChangeLog: []` emite WARN no Liquibase 4.25+ | 💡 Sugestão | ✅ Incorporado — FASE 6.4 |
| N8 | Item 1.4 deveria listar `property.yml` como primeiro include | ⚠️ Ressalva | ✅ Incorporado — FASE 1.4 |
| N9 | Exemplo do `api-development-guidelines.md` contém a violação 1 — adicionar nota de alerta | ⚠️ Ressalva | ✅ Incorporado — FASE 6.1 |
| N10 | `v1.0.0.yml` com changeSet de tag + includes — ordem correta | ✅ Aprovado | — |

---

### 9.8 — 🗤️🏽 @dba (3ª Revisão)
**Veredicto**: ⚠️ Aprovado com Ressalvas — 3 novos bloqueantes identificados

| # | Ponto | Classificação | Status |
|---|---|---|---|
| F1+F2 | Path changes vs `DATABASECHANGELOG` + rename `trigger/` → `triggers/` — clean install obrigatória, não documentada | 🔴 Bloqueante | ✅ Incorporado — Checklist §6 pré-condição + §7 Riscos |
| F3 | Remoção de `init.yml` — arquivos de include não são registrados em `DATABASECHANGELOG`; remoção segura | ✅ Aprovado | — |
| F4.1 | Ordem `v1.0.0 → function → triggers → view → procedure` — correta; triggers dependem de functions | ✅ Aprovado | — |
| F4.2 | `procedure.yml` com `databaseChangeLog: []` — NPE em Liquibase < 4.8 | ⚠️ Ressalva | ✅ Incorporado — FASE 6.4 |
| F4.3 | `liquibase:releaseLocks` ausente do checklist | ⚠️ Ressalva | ✅ Incorporado — Checklist §6 |
| F5.1 | `cycle: false` ausente nos `createSequence` | ⚠️ Ressalva | ✅ Incorporado — §4.6 |
| F5.2 | `indexType: gin` ausente — Liquibase cria BTREE silenciosamente | 🔴 Bloqueante | ✅ Incorporado — §4.8 |
| F5.3 | `dbms: postgresql` faltando no `createIndex` GIN | ⚠️ Ressalva | ✅ Incorporado — §4.8 |
| F5.4 | `schemaName: scos` ausente em `createSequence`; `defaultValueSequenceNext` deve ser `scos.SEQ_*` | 🔴 Bloqueante | ✅ Incorporado — §4.6 + §7 Riscos |
| F6.1 | Schema do `DATABASECHANGELOG` depende do `currentSchema` da connection string | ⚠️ Ressalva | Verificar `application.yml` — sem ação bloqueante |
| F6.2 | `NOW()` com `TIMESTAMPTZ` — compatível | ✅ Aprovado | — |
| F6.3 | `runOnChange` + lock em pods paralelos — protegido via `DATABASECHANGELOGLOCK` | ✅ Aprovado | Nota informativa para ambiente cloud |

---

### 9.9 — 🎯 @especialista (3ª Revisão)
**Veredicto**: ⚠️ Aprovado com Ressalvas — 2 novos bloqueantes na FASE 5

| # | Ponto | Classificação | Status |
|---|---|---|---|
| 1 | `configure_system.sql` em `v1.0.0/insert/` — localização correta para seeds versionados | ✅ Aprovado | — |
| 2 | Remoção de `init.yml` — sem impacto funcional | ✅ Aprovado | — |
| 3 | `procedure/` vazio — sem impacto funcional; sintaxe `[]` pode ser rejeitada | ⚠️ Ressalva | ✅ Incorporado — FASE 6.4 |
| 4 | `configure_system.sql` atual é stub de 3 linhas — seed completo não foi escrito | 🔴 Bloqueante | ✅ Incorporado — FASE 5 item 5.0 |
| 5 | Validação final do seed dependente do seed completo (order FK: company → dept → pos → perm → profile → employee → login → profile_perm) | 🔴 Bloqueante | ✅ Incorporado — FASE 5 item 5.0 |
| 6 | Dependência `function → triggers` deve ser comentada no `db.changelog-master.yml` | ⚠️ Ressalva | ✅ Incorporado — FASE 6.2 |
| 7 | **B-3a DESBLOQUEADO**: `api-development-guidelines.md` documenta Opção B (Hierarquia `ORGANIZATION_*`) como mecanismo de features; lista completa disponível | 💡 Decisão disponível | Aguarda confirmação do usuário — ver [PONTO DE PARADA] |
| 8 | Inventário de `scos_permission` (~47 códigos `x-authorize`) ausente do plano | ⚠️ Ressalva | ✅ Incorporado — FASE 5 item 5.0a |

---

## [PONTO DE PARADA] — 3ª Revisão dos Agentes

**O plano acumula: 22 violações originais + 10 (1ª rodada) + 8 (2ª rodada) + 14 (3ª rodada) = 54 pontos de melhoria.**

### Decisões tomadas pelo usuário (2026-02-28):

1. **[CRÍTICO — @especialista B2]** ✅ `LOGIN.STATUS` = `'PENDING_PASSWORD_CHANGE'`
2. **[@arquiteto S2]** ✅ `runOnChange: true` adotado para funções e views SQL

### Itens incorporados da 3ª rodada (todos no plano):

| # | Origem | Item | Localização |
|---|---|---|---|
| N3 | @arquiteto | Verificar/excluir `LiquibaseAutoConfiguration` do Spring Boot | FASE 1.6 |
| N4 | @arquiteto | `property.yml` como primeiro include no master | FASE 1.4 e 6.2 |
| N5 | @arquiteto | TestContainers com `install_database.sql` como init script (escalado a bloqueante) | FASE 1.7 |
| N1/N2 | @arquiteto | Corrigir item 1.1a + placeholder default `:false` em `bootstrap.yml` | FASE 1.1a |
| N6 | @arquiteto | `LocalDateTime` vs `TIMESTAMPTZ` — verificar entidades JPA | Checklist §6 |
| F1/F2 | @dba | Pré-condição de clean install documentada | Checklist §6 + §7 |
| F4.2/N7 | @dba/@arquiteto | `procedure.yml` sem `databaseChangeLog: []` | FASE 6.4 |
| F4.3 | @dba | `liquibase:releaseLocks` no checklist | Checklist §6 |
| F5.1 | @dba | `cycle: false` nos `createSequence` | §4.6 |
| F5.2 | @dba | `indexType: gin` no `createIndex` GIN | §4.8 |
| F5.3 | @dba | `dbms: postgresql` no `createIndex` GIN | §4.8 |
| F5.4 | @dba | `schemaName: scos` nos `createSequence`; `scos.SEQ_*` no `defaultValueSequenceNext` | §4.6 + §7 |
| 4/5 | @especialista | `configure_system.sql` é stub — seed completo deve ser escrito | FASE 5 item 5.0 |
| 7/8 | @especialista | B-3a desbloqueável + inventário de permissões | FASE 5 items 5.0a/5.1d |
| N9 | @arquiteto | Nota de alerta: exemplo do guideline tem violação 1 | FASE 6.1 |
| 6 | @especialista | Comentário de dependência `function → triggers` no master | FASE 6.2 |

### Decisão que requer resposta do usuário antes da FASE 5:

**[@especialista §9.9 ponto 7 — B-3a]** O `api-development-guidelines.md` documenta o mecanismo de features como **Opção B — Hierarquia `ORGANIZATION_*`**. Com base nisso, a @especialista propõe a seguinte lista completa de features para o perfil ADMIN:

```
ARRAY[
  'ORGANIZATION_ADMINISTRATION',
  'ORGANIZATION_VIEW',
  'ORGANIZATION_MANAGEMENT',
  'ORGANIZATION_COMPANY_VIEW',
  'ORGANIZATION_COMPANY_MANAGEMENT',
  'ORGANIZATION_EMPLOYEE_VIEW',
  'ORGANIZATION_EMPLOYEE_MANAGEMENT',
  'ORGANIZATION_EMPLOYEE_LOGIN_MANAGEMENT',
  'ORGANIZATION_DEPARTMENT_VIEW',
  'ORGANIZATION_DEPARTMENT_MANAGEMENT',
  'ORGANIZATION_POSITION_VIEW',
  'ORGANIZATION_POSITION_MANAGEMENT',
  'ORGANIZATION_PROFILE_VIEW',
  'ORGANIZATION_PROFILE_MANAGEMENT'
]
```

> **Confirma esta lista para o seed do perfil ADMIN?** (Substitui a lista parcial anterior de R2)

### Status de execução por fase:

| Fase | Status | Bloqueio |
|---|---|---|
| FASE 1 | ✅ Pronta para iniciar | Nenhum (após incorporação dos itens N3, N4, N5) |
| FASE 2 | ✅ Pronta para iniciar | Nenhum |
| FASE 3 | ✅ Pronta para iniciar | Nenhum |
| FASE 4 | ✅ Pronta para iniciar | Nenhum |
| FASE 5 | 🔴 BLOQUEADA | (1) seed completo precisa ser escrito (item 5.0); (2) confirmação da lista de features B-3a |
| FASE 6 | ✅ Pronta para iniciar | Nenhum |
````
