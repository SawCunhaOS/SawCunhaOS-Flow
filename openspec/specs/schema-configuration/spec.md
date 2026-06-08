## ADDED Requirements

### Requirement: SCOS_CONFIGURATION com PK textual
O sistema SHALL ter `SCOS_CONFIGURATION` com `CONFIGURATION_ID VARCHAR(50) PK` (chave textual, sem auto-increment), `VALUE TEXT NOT NULL`, `TYPE VARCHAR(50) NOT NULL` (valores: `STRING`, `INTEGER`, `BOOLEAN`, `JSON`), `CREATED_AT TIMESTAMPTZ NOT NULL DEFAULT NOW()`, `UPDATED_AT TIMESTAMPTZ NOT NULL`, `USER_AT VARCHAR(255) NOT NULL`. Sem sequence, sem colunas `CONFIGURATION_KEY`, `DESCRIPTION`, `CONFIGURATION_VALUE`, `DEFAULT_VALUE`.

#### Scenario: PK textual correta
- **WHEN** tabela é criada
- **THEN** `CONFIGURATION_ID` é do tipo `VARCHAR(50)` e é a PK; sem sequence associada

#### Scenario: Colunas antigas ausentes
- **WHEN** tabela é criada
- **THEN** colunas `CONFIGURATION_KEY`, `DESCRIPTION`, `CONFIGURATION_VALUE`, `DEFAULT_VALUE` não existem

#### Scenario: VALUE e TYPE NOT NULL
- **WHEN** INSERT é tentado sem VALUE ou sem TYPE
- **THEN** banco rejeita com violação de NOT NULL

#### Scenario: INSERT requer chave explícita
- **WHEN** INSERT é feito sem fornecer CONFIGURATION_ID
- **THEN** banco rejeita (sem geração automática de PK textual)
