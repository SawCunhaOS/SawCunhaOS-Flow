## ADDED Requirements

### Requirement: SCOS_SYSTEM migration
O sistema SHALL criar a tabela `SCOS_SYSTEM` com `SYSTEM_ID UUID PK` gerado por `gen_random_uuid()`, `CODE VARCHAR(25) UK NOT NULL`, `DESCRIPTION VARCHAR(200) NOT NULL`, `SECRET_KEY TEXT NOT NULL`, `STATUS VARCHAR(50) NOT NULL`, `CREATED_AT TIMESTAMPTZ NOT NULL DEFAULT NOW()`, `UPDATED_AT TIMESTAMPTZ NOT NULL`, `USER_AT VARCHAR(255) NOT NULL`. O changeSet deve incluir rollback funcional.

#### Scenario: Tabela criada corretamente
- **WHEN** `liquibase update` é executado em banco limpo
- **THEN** `SCOS_SYSTEM` existe com UUID PK, UK em CODE, e todos os campos NOT NULL conforme especificado

#### Scenario: Rollback limpo
- **WHEN** `liquibase rollback` é executado
- **THEN** `SCOS_SYSTEM` é removida sem erros

### Requirement: SCOS_RESOURCE migration
O sistema SHALL criar a tabela `SCOS_RESOURCE` com `RESOURCE_ID UUID PK` gerado por `gen_random_uuid()`, `SYSTEM_ID UUID FK NOT NULL → SCOS_SYSTEM`, `CODE VARCHAR(50) UK NOT NULL`, `DESCRIPTION VARCHAR(255) NOT NULL`, `ACTIVE BOOLEAN NOT NULL DEFAULT TRUE`, campos de auditoria NOT NULL. Deve incluir índice `IDX_SYSTEM_ID_SCOS_RESOURCE` em `SYSTEM_ID`.

#### Scenario: FK para SCOS_SYSTEM validada
- **WHEN** `liquibase update` é executado após SCOS_SYSTEM
- **THEN** FK `FK_SYSTEM_ID_SCOS_RESOURCE` existe e aponta para `SCOS_SYSTEM.SYSTEM_ID`

#### Scenario: Índice criado
- **WHEN** tabela é criada
- **THEN** `IDX_SYSTEM_ID_SCOS_RESOURCE` existe em `SYSTEM_ID`

### Requirement: SCOS_PROFILE_RESOURCE migration
O sistema SHALL criar a tabela `SCOS_PROFILE_RESOURCE` com PK composta `(PROFILE_ID BIGINT FK → SCOS_PROFILE, RESOURCE_ID UUID FK → SCOS_RESOURCE)`, `CREATED_AT TIMESTAMPTZ NOT NULL DEFAULT NOW()`, `USER_AT VARCHAR(255) NOT NULL`. Sem `UPDATED_AT` (vínculo imutável). Deve incluir índice `IDX_PROFILE_ID_SCOS_PROFILE_RESOURCE` em `PROFILE_ID` e `IDX_RESOURCE_ID_SCOS_PROFILE_RESOURCE` em `RESOURCE_ID`.

#### Scenario: PK composta impede duplicata
- **WHEN** mesmo par (PROFILE_ID, RESOURCE_ID) é inserido duas vezes
- **THEN** segunda inserção falha por violação de PK

#### Scenario: Índice inverso criado
- **WHEN** tabela é criada
- **THEN** `IDX_RESOURCE_ID_SCOS_PROFILE_RESOURCE` existe em `RESOURCE_ID`

### Requirement: SCOS_PERMISSION removida
O sistema SHALL remover o arquivo `scos_permission.yml` e sua referência em `tables.yml`. A tabela `SCOS_PERMISSION` não deve existir no schema.

#### Scenario: Tabela ausente após migration
- **WHEN** `liquibase update` é executado em banco limpo
- **THEN** tabela `SCOS_PERMISSION` não existe no banco
