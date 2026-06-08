## ADDED Requirements

### Requirement: SCOS_INTEGRATION_KEYCLOAK com retry
O sistema SHALL ter `SCOS_INTEGRATION_KEYCLOAK` com `RETRY_COUNT INT NOT NULL DEFAULT 0` e `MAX_RETRIES INT NOT NULL DEFAULT 3`, além dos campos já existentes. Deve incluir `IDX_STATUS_SCOS_INTEGRATION_KEYCLOAK` em `STATUS` e `IDX_KEYCLOAK_ID_SCOS_INTEGRATION_KEYCLOAK` em `KEYCLOAK_ID`.

#### Scenario: RETRY_COUNT e MAX_RETRIES presentes com default
- **WHEN** registro é inserido sem fornecer RETRY_COUNT e MAX_RETRIES
- **THEN** `RETRY_COUNT` = 0 e `MAX_RETRIES` = 3 são gravados automaticamente

#### Scenario: IDX_STATUS criado
- **WHEN** tabela é criada
- **THEN** índice `IDX_STATUS_SCOS_INTEGRATION_KEYCLOAK` existe em `STATUS`

#### Scenario: IDX_KEYCLOAK_ID criado
- **WHEN** tabela é criada
- **THEN** índice `IDX_KEYCLOAK_ID_SCOS_INTEGRATION_KEYCLOAK` existe em `KEYCLOAK_ID`

### Requirement: SCOS_INTEGRATION_KEYCLOAK_LOG imutável
O sistema SHALL ter `SCOS_INTEGRATION_KEYCLOAK_LOG` sem coluna `UPDATED_AT`. A tabela é imutável — registros apenas inseridos, nunca atualizados. Campos: `INTEGRATION_KEYCLOAK_LOG_ID UUID PK`, `INTEGRATION_KEYCLOAK_ID UUID FK NOT NULL`, `SUCCESS BOOLEAN NOT NULL`, `RESPONSE VARCHAR(5000) NULL`, `CREATED_AT TIMESTAMPTZ NOT NULL DEFAULT NOW()`, `USER_AT VARCHAR(100) NULL`.

#### Scenario: UPDATED_AT ausente
- **WHEN** tabela é criada
- **THEN** coluna `UPDATED_AT` não existe em `SCOS_INTEGRATION_KEYCLOAK_LOG`

#### Scenario: Índice FK presente
- **WHEN** tabela é criada
- **THEN** índice `IDX_INTEGRATION_KEYCLOAK_ID_SCOS_INTEGRATION_KEYCLOAK_LOG` existe

### Requirement: SCOS_INTEGRATION_MESSAGE_INVALID imutável
O sistema SHALL ter `SCOS_INTEGRATION_MESSAGE_INVALID` com `MESSAGE_INVALID VARCHAR(5000) NOT NULL` (não nullable) e sem coluna `UPDATED_AT`. A tabela é imutável — registros apenas inseridos.

#### Scenario: MESSAGE_INVALID NOT NULL
- **WHEN** INSERT é tentado com MESSAGE_INVALID NULL
- **THEN** banco rejeita com violação de NOT NULL

#### Scenario: UPDATED_AT ausente
- **WHEN** tabela é criada
- **THEN** coluna `UPDATED_AT` não existe em `SCOS_INTEGRATION_MESSAGE_INVALID`
