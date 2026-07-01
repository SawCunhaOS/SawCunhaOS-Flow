## ADDED Requirements

### Requirement: SCOS_ADDRESS_TYPE cataloga tipos de endereço por entidade
O sistema SHALL ter `SCOS_ADDRESS_TYPE` com `ADDRESS_TYPE_ID BIGINT PK` (identity), `CODE VARCHAR(30) NOT NULL` (UK composta com `ENTITY_TYPE`), `DESCRIPTION VARCHAR(255) NOT NULL`, `ENTITY_TYPE VARCHAR(20) NOT NULL` restrito a `COMPANY`/`EMPLOYEE` via `CHECK`, `ACTIVE BOOLEAN NOT NULL DEFAULT TRUE`, campos de auditoria NOT NULL. `DELETE` SHALL ser bloqueado por trigger — único mecanismo de remoção é `ACTIVE = false`. Índice parcial `IDX_ADDRESS_TYPE_ENTITY_TYPE` em `(ENTITY_TYPE) WHERE ACTIVE = TRUE`.

#### Scenario: Mesmo CODE permitido em ENTITY_TYPE diferentes
- **WHEN** `CODE = 'HOME'` é inserido para `ENTITY_TYPE = 'EMPLOYEE'` e depois para `ENTITY_TYPE = 'COMPANY'`
- **THEN** ambas as inserções são aceitas — UK é composta com `ENTITY_TYPE`

#### Scenario: ENTITY_TYPE restrito ao vocabulário fechado
- **WHEN** INSERT usa `ENTITY_TYPE = 'SUPPLIER'`
- **THEN** banco rejeita por violação do `CHECK` `CHK_ADDRESS_TYPE_ENTITY_TYPE`

#### Scenario: DELETE bloqueado
- **WHEN** `DELETE FROM SCOS_ADDRESS_TYPE` é tentado
- **THEN** banco rejeita via trigger `TRG_BLOCK_DELETE_ADDRESS_TYPE`

### Requirement: SCOS_CONTACT_TYPE cataloga tipos de contato por entidade
O sistema SHALL ter `SCOS_CONTACT_TYPE` com a mesma estrutura de `SCOS_ADDRESS_TYPE`: `CONTACT_TYPE_ID BIGINT PK`, `CODE VARCHAR(30) NOT NULL` (UK composta com `ENTITY_TYPE`), `DESCRIPTION VARCHAR(255) NOT NULL`, `ENTITY_TYPE VARCHAR(20) NOT NULL` restrito a `COMPANY`/`EMPLOYEE`, `ACTIVE BOOLEAN NOT NULL DEFAULT TRUE`, `DELETE` bloqueado por trigger, índice parcial `IDX_CONTACT_TYPE_ENTITY_TYPE` em `(ENTITY_TYPE) WHERE ACTIVE = TRUE`.

#### Scenario: ENTITY_TYPE restrito ao vocabulário fechado
- **WHEN** INSERT usa `ENTITY_TYPE = 'SUPPLIER'`
- **THEN** banco rejeita por violação do `CHECK` `CHK_CONTACT_TYPE_ENTITY_TYPE`

#### Scenario: DELETE bloqueado
- **WHEN** `DELETE FROM SCOS_CONTACT_TYPE` é tentado
- **THEN** banco rejeita via trigger `TRG_BLOCK_DELETE_CONTACT_TYPE`

### Requirement: Trigger valida ENTITY_TYPE do tipo referenciado nas tabelas consumidoras
O sistema SHALL impedir, via trigger `BEFORE INSERT OR UPDATE OF <fk_column>`, que `SCOS_EMPLOYEE_ADDRESS`/`SCOS_COMPANY_ADDRESS` referenciem um `ADDRESS_TYPE_ID` cujo `ENTITY_TYPE` não corresponda à tabela, e que `SCOS_EMPLOYEE_CONTACT`/`SCOS_COMPANY_CONTACT` referenciem um `CONTACT_TYPE_ID` com `ENTITY_TYPE` incompatível. A FK sozinha não distingue isso, pois a tabela de tipo é compartilhada entre `COMPANY` e `EMPLOYEE`.

#### Scenario: Endereço de funcionário não aceita tipo de empresa
- **WHEN** um `SCOS_EMPLOYEE_ADDRESS` é inserido com `ADDRESS_TYPE_ID` de um tipo cujo `ENTITY_TYPE = 'COMPANY'`
- **THEN** trigger `TRG_VALIDATE_EMPLOYEE_ADDRESS_TYPE` rejeita com exceção

#### Scenario: Contato de empresa não aceita tipo de funcionário
- **WHEN** um `SCOS_COMPANY_CONTACT` é inserido com `CONTACT_TYPE_ID` de um tipo cujo `ENTITY_TYPE = 'EMPLOYEE'`
- **THEN** trigger `TRG_VALIDATE_COMPANY_CONTACT_TYPE` rejeita com exceção
