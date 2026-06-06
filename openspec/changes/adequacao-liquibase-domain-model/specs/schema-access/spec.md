## ADDED Requirements

### Requirement: SCOS_PROFILE corrigido
O sistema SHALL ter `SCOS_PROFILE` sem coluna `FEATURES TEXT[]`, sem índice GIN em FEATURES, com `CODE VARCHAR(30)` (não 50), `DESCRIPTION VARCHAR(30)` (não 100), `UPDATED_AT TIMESTAMPTZ NOT NULL`, `USER_AT VARCHAR(255) NOT NULL`.

#### Scenario: FEATURES ausente
- **WHEN** tabela é criada
- **THEN** coluna `FEATURES` não existe em `SCOS_PROFILE`

#### Scenario: Tamanhos corretos
- **WHEN** tabela é criada
- **THEN** `CODE` é `VARCHAR(30)` e `DESCRIPTION` é `VARCHAR(30)`

### Requirement: SCOS_LOGIN reescrito
O sistema SHALL ter `SCOS_LOGIN` com `EMPLOYEE_ID BIGINT FK NULL` (nullable — permite usuários externos e contas de serviço), sem colunas `PASSWORD`, `SALT`, `DATE_LAST_CHANGE_PASSWORD`, com `TYPE VARCHAR(50) NOT NULL` (valores: `EMPLOYEE`, `EXTERNAL`, `SERVICE`), `LOGIN VARCHAR(255) NOT NULL`, `STATUS VARCHAR(50) NOT NULL`, `KEYCLOAK_ID UUID NULL`, `UPDATED_AT TIMESTAMPTZ NOT NULL`, `USER_AT VARCHAR(255) NOT NULL`.

#### Scenario: EMPLOYEE_ID nullable
- **WHEN** INSERT em SCOS_LOGIN é feito sem EMPLOYEE_ID
- **THEN** banco aceita o registro (EMPLOYEE_ID NULL é válido)

#### Scenario: PASSWORD e SALT ausentes
- **WHEN** tabela é criada
- **THEN** colunas `PASSWORD` e `SALT` não existem em `SCOS_LOGIN`

#### Scenario: TYPE presente e NOT NULL
- **WHEN** tabela é criada
- **THEN** coluna `TYPE VARCHAR(50) NOT NULL` existe

#### Scenario: IDX_KEYCLOAK_ID_SCOS_LOGIN criado
- **WHEN** tabela é criada
- **THEN** índice `IDX_KEYCLOAK_ID_SCOS_LOGIN` existe em `KEYCLOAK_ID`

#### Scenario: LOGIN com tamanho correto
- **WHEN** tabela é criada
- **THEN** coluna `LOGIN` é `VARCHAR(255)` (não VARCHAR(100))
