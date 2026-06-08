## ADDED Requirements

### Requirement: PKs BIGINT com GENERATED ALWAYS AS IDENTITY
Todas as PKs do tipo `BIGINT` nas tabelas de organização SHALL usar `GENERATED ALWAYS AS IDENTITY` (via `autoIncrement: true` no Liquibase). Nenhuma sequence explícita (`SEQ_*`) deve ser criada.

#### Scenario: Sem sequences no schema
- **WHEN** `liquibase update` é executado em banco limpo
- **THEN** nenhuma sequence `SEQ_*` existe no schema `scos`

#### Scenario: PK gerada automaticamente
- **WHEN** registro é inserido sem valor de PK
- **THEN** banco gera o ID automaticamente via identity column

### Requirement: SCOS_DEPARTMENT corrigido
O sistema SHALL ter `SCOS_DEPARTMENT` com `DESCRIPTION VARCHAR(30) NOT NULL` (não 500), `UPDATED_AT TIMESTAMPTZ NOT NULL`, `USER_AT VARCHAR(255) NOT NULL`.

#### Scenario: Tamanho de DESCRIPTION correto
- **WHEN** tabela é criada
- **THEN** coluna `DESCRIPTION` tem tipo `VARCHAR(30)`

### Requirement: SCOS_POSITION corrigido
O sistema SHALL ter `SCOS_POSITION` com `DESCRIPTION VARCHAR(30) NOT NULL` (não 500), `UPDATED_AT TIMESTAMPTZ NOT NULL`, `USER_AT VARCHAR(255) NOT NULL`.

#### Scenario: Tamanho de DESCRIPTION correto
- **WHEN** tabela é criada
- **THEN** coluna `DESCRIPTION` tem tipo `VARCHAR(30)`

### Requirement: SCOS_COMPANY corrigido
O sistema SHALL ter `SCOS_COMPANY` sem coluna `ACTIVE`, com `NAME VARCHAR(250)`, `NAME_TREATMENT VARCHAR(100)`, `STATUS VARCHAR(20) NOT NULL`, `UPDATED_AT TIMESTAMPTZ NOT NULL`, `USER_AT VARCHAR(255) NOT NULL`.

#### Scenario: ACTIVE ausente
- **WHEN** tabela é criada
- **THEN** coluna `ACTIVE` não existe em `SCOS_COMPANY`

#### Scenario: STATUS presente e NOT NULL
- **WHEN** tabela é criada
- **THEN** coluna `STATUS VARCHAR(20) NOT NULL` existe

### Requirement: SCOS_COMPANY_ADDRESS com PK composta
O sistema SHALL ter `SCOS_COMPANY_ADDRESS` com PK composta `(COMPANY_ID_ADDRESS BIGINT, COMPANY_ID BIGINT FK)`, sem surrogate key, sem sequence. `GEOLOCATION GEOMETRY(POINT) NOT NULL`, `UPDATED_AT TIMESTAMPTZ NOT NULL`, `USER_AT VARCHAR(255) NOT NULL`.

#### Scenario: PK composta correta
- **WHEN** tabela é criada
- **THEN** PK é composta por `COMPANY_ID_ADDRESS` e `COMPANY_ID`; não existe coluna `COMPANY_ADDRESS_ID`

#### Scenario: GEOLOCATION NOT NULL
- **WHEN** tabela é criada
- **THEN** coluna `GEOLOCATION` tem constraint NOT NULL

### Requirement: SCOS_COMPANY_CONTACT corrigido
O sistema SHALL ter `SCOS_COMPANY_CONTACT` com PK `COMPANY_ID_CONTACT` (não `CONTACT_ID`), sem coluna `RESPONSIBLE_PERSON`, com UK em `PHONE`, `EMAIL` e `TYPE`, `UPDATED_AT TIMESTAMPTZ NOT NULL`, `USER_AT VARCHAR(255) NOT NULL`.

#### Scenario: PK com nome correto
- **WHEN** tabela é criada
- **THEN** PK se chama `COMPANY_ID_CONTACT`, não `CONTACT_ID`

#### Scenario: RESPONSIBLE_PERSON ausente
- **WHEN** tabela é criada
- **THEN** coluna `RESPONSIBLE_PERSON` não existe

### Requirement: SCOS_EMPLOYEE corrigido
O sistema SHALL ter `SCOS_EMPLOYEE` com `COMPANY_ID FK NOT NULL`, `POSITION_ID FK NOT NULL`, `STATUS VARCHAR(20) NOT NULL`, sem coluna `ACTIVE`, `NAME VARCHAR(250)`, `NAME_TREATMENT VARCHAR(100)`, `EMAIL VARCHAR(255) UK NOT NULL`, `UPDATED_AT TIMESTAMPTZ NOT NULL`, `USER_AT VARCHAR(255) NOT NULL`.

#### Scenario: COMPANY_ID e POSITION_ID NOT NULL
- **WHEN** INSERT em SCOS_EMPLOYEE é tentado com COMPANY_ID NULL
- **THEN** banco rejeita com violação de NOT NULL

#### Scenario: STATUS presente
- **WHEN** tabela é criada
- **THEN** coluna `STATUS VARCHAR(20) NOT NULL` existe

#### Scenario: ACTIVE ausente
- **WHEN** tabela é criada
- **THEN** coluna `ACTIVE` não existe em `SCOS_EMPLOYEE`

#### Scenario: IDX_STATUS_SCOS_EMPLOYEE criado
- **WHEN** tabela é criada
- **THEN** índice `IDX_STATUS_SCOS_EMPLOYEE` existe em `STATUS`

### Requirement: SCOS_EMPLOYEE_ADDRESS corrigido
O sistema SHALL ter `SCOS_EMPLOYEE_ADDRESS` com PK composta `(EMPLOYEE_ID_ADDRESS BIGINT, EMPLOYEE_ID BIGINT FK)` — coluna do endereço externo chamada `EMPLOYEE_ID_ADDRESS` (não `ADDRESS_ID`). `GEOLOCATION GEOMETRY(POINT) NOT NULL`, `UPDATED_AT TIMESTAMPTZ NOT NULL`, `USER_AT VARCHAR(255) NOT NULL`.

#### Scenario: Coluna com nome correto
- **WHEN** tabela é criada
- **THEN** coluna se chama `EMPLOYEE_ID_ADDRESS`, não `ADDRESS_ID`

### Requirement: SCOS_EMPLOYEE_CONTACT corrigido
O sistema SHALL ter `SCOS_EMPLOYEE_CONTACT` com PK `EMPLOYEE_ID_CONTACT` (não `CONTACT_ID`), UK em `PHONE` e `TYPE`, `UPDATED_AT TIMESTAMPTZ NOT NULL`, `USER_AT VARCHAR(255) NOT NULL`.

#### Scenario: PK com nome correto
- **WHEN** tabela é criada
- **THEN** PK se chama `EMPLOYEE_ID_CONTACT`

#### Scenario: UKs em PHONE e TYPE
- **WHEN** tabela é criada
- **THEN** UK existe em `PHONE` e UK existe em `TYPE`
