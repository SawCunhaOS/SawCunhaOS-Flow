## ADDED Requirements

### Requirement: Tabelas de motivo pré-cadastrado por tipo de transição
O sistema SHALL ter as tabelas `SCOS_REASON_ACTIVATE`, `SCOS_REASON_INACTIVATE`, `SCOS_REASON_DISABLE` e `SCOS_REASON_ENABLE`, cada uma com `<NOME>_ID BIGINT PK` (identity), `CODE VARCHAR(30) NOT NULL` (UK composta com `ENTITY_TYPE`), `DESCRIPTION VARCHAR(255) NOT NULL`, `ENTITY_TYPE VARCHAR(20) NOT NULL` restrito a `COMPANY`/`EMPLOYEE`/`LOGIN` via `CHECK`, `ACTIVE BOOLEAN NOT NULL DEFAULT TRUE`, campos de auditoria NOT NULL. `DELETE` SHALL ser bloqueado por trigger em cada uma. Cada tabela tem índice parcial `(ENTITY_TYPE) WHERE ACTIVE = TRUE`.

#### Scenario: Quatro tabelas de motivo existem
- **WHEN** `liquibase update` é executado em banco limpo
- **THEN** `SCOS_REASON_ACTIVATE`, `SCOS_REASON_INACTIVATE`, `SCOS_REASON_DISABLE` e `SCOS_REASON_ENABLE` existem com o schema especificado

#### Scenario: ENTITY_TYPE restrito ao vocabulário fechado
- **WHEN** INSERT em qualquer uma das 4 tabelas usa `ENTITY_TYPE = 'SUPPLIER'`
- **THEN** banco rejeita por violação do `CHECK` correspondente

#### Scenario: DELETE bloqueado nas 4 tabelas
- **WHEN** `DELETE` é tentado em qualquer uma das 4 tabelas
- **THEN** banco rejeita via trigger `TRG_BLOCK_DELETE_REASON_*`

### Requirement: SCOS_COMPANY_STATUS_HISTORY histórico imutável de status
O sistema SHALL ter `SCOS_COMPANY_STATUS_HISTORY` com `COMPANY_STATUS_HISTORY_ID BIGINT PK`, `COMPANY_ID BIGINT FK NOT NULL → SCOS_COMPANY`, `STATUS VARCHAR(20) NOT NULL` restrito a `ACTIVE`/`INACTIVE`/`DISABLED`, `PREVIOUS_STATUS VARCHAR(20) NULL` restrito ao mesmo vocabulário, `REASON_ACTIVATE_ID`/`REASON_INACTIVATE_ID`/`REASON_DISABLE_ID`/`REASON_ENABLE_ID BIGINT FK NULL`, `OBSERVATION TEXT NULL`, `CREATED_AT TIMESTAMPTZ NOT NULL`, `USER_AT VARCHAR(255) NOT NULL`. Sem `UPDATED_AT` — tabela imutável, apenas inserção. Índice `IDX_COMPANY_STATUS_HISTORY_CURRENT` em `(COMPANY_ID, CREATED_AT DESC)`.

#### Scenario: Tabela imutável
- **WHEN** tabela é criada
- **THEN** coluna `UPDATED_AT` não existe em `SCOS_COMPANY_STATUS_HISTORY`

#### Scenario: STATUS e PREVIOUS_STATUS restritos ao vocabulário fechado
- **WHEN** INSERT usa `STATUS = 'DELETED'` ou `PREVIOUS_STATUS = 'DELETED'`
- **THEN** banco rejeita por violação de `CHK_COMPANY_STATUS_HISTORY_STATUS`/`_PREVIOUS`

### Requirement: SCOS_EMPLOYEE_STATUS_HISTORY histórico imutável de status
O sistema SHALL ter `SCOS_EMPLOYEE_STATUS_HISTORY` com a mesma estrutura de `SCOS_COMPANY_STATUS_HISTORY`, trocando `COMPANY_ID` por `EMPLOYEE_ID FK NOT NULL → SCOS_EMPLOYEE`. `STATUS`/`PREVIOUS_STATUS` restritos a `ACTIVE`/`INACTIVE`/`DISABLED`. Índice `IDX_EMPLOYEE_STATUS_HISTORY_CURRENT` em `(EMPLOYEE_ID, CREATED_AT DESC)`.

#### Scenario: Tabela imutável
- **WHEN** tabela é criada
- **THEN** coluna `UPDATED_AT` não existe em `SCOS_EMPLOYEE_STATUS_HISTORY`

### Requirement: SCOS_LOGIN_STATUS_HISTORY histórico imutável de status com BLOCKED
O sistema SHALL ter `SCOS_LOGIN_STATUS_HISTORY` com a mesma estrutura das demais, trocando `COMPANY_ID` por `LOGIN_ID FK NOT NULL → SCOS_LOGIN`. **`STATUS`/`PREVIOUS_STATUS` usam `BLOCKED` no lugar de `DISABLED`** — restritos a `ACTIVE`/`INACTIVE`/`BLOCKED`, consistente com `SCOS_LOGIN.STATUS`. Índice `IDX_LOGIN_STATUS_HISTORY_CURRENT` em `(LOGIN_ID, CREATED_AT DESC)`.

#### Scenario: BLOCKED aceito, DISABLED rejeitado
- **WHEN** INSERT em `SCOS_LOGIN_STATUS_HISTORY` usa `STATUS = 'BLOCKED'`
- **THEN** banco aceita
- **WHEN** INSERT usa `STATUS = 'DISABLED'`
- **THEN** banco rejeita por violação de `CHK_LOGIN_STATUS_HISTORY_STATUS`

### Requirement: Trigger deriva PREVIOUS_STATUS e valida ENTITY_TYPE do motivo
O sistema SHALL, via trigger `BEFORE INSERT` em cada uma das 3 tabelas de histórico, (1) derivar automaticamente `PREVIOUS_STATUS` a partir do último registro real da mesma entidade (`ORDER BY created_at DESC LIMIT 1`) — a aplicação nunca informa esse valor — e (2) validar que a FK de motivo preenchida (`REASON_ACTIVATE_ID`/`REASON_INACTIVATE_ID`/`REASON_DISABLE_ID`/`REASON_ENABLE_ID`) referencia um motivo cujo `ENTITY_TYPE` corresponde à entidade da tabela de histórico, rejeitando com exceção caso contrário.

#### Scenario: PREVIOUS_STATUS derivado automaticamente
- **WHEN** um `SCOS_EMPLOYEE_STATUS_HISTORY` é inserido para um funcionário que já tem histórico prévio, sem informar `PREVIOUS_STATUS`
- **THEN** trigger preenche `PREVIOUS_STATUS` com o `STATUS` do último registro real daquele funcionário

#### Scenario: Motivo de entidade errada é rejeitado
- **WHEN** um `SCOS_COMPANY_STATUS_HISTORY` é inserido referenciando um `REASON_ACTIVATE_ID` cujo `ENTITY_TYPE = 'EMPLOYEE'`
- **THEN** trigger rejeita com exceção

### Requirement: Trigger sincroniza STATUS cache na tabela principal
O sistema SHALL, via trigger `AFTER INSERT` em cada uma das 3 tabelas de histórico, propagar `NEW.STATUS` para a coluna `STATUS` (cache) da tabela principal correspondente (`SCOS_COMPANY`, `SCOS_EMPLOYEE`, `SCOS_LOGIN`), atualizando também `UPDATED_AT` com o `CREATED_AT` do histórico. A aplicação SHALL sempre inserir no histórico primeiro — nunca fazer `UPDATE` direto de `STATUS` na tabela principal.

#### Scenario: STATUS da empresa sincronizado após INSERT no histórico
- **WHEN** um registro é inserido em `SCOS_COMPANY_STATUS_HISTORY` com `STATUS = 'INACTIVE'`
- **THEN** `SCOS_COMPANY.STATUS` daquela empresa passa a ser `'INACTIVE'`

### Requirement: CHECK garante coerência entre transição de status e motivo
O sistema SHALL impedir, via `CHECK`, combinações de `PREVIOUS_STATUS`/`STATUS`/motivo fora das transições válidas: `(nenhum) → ACTIVE` exige `REASON_ACTIVATE_ID`; `INACTIVE → ACTIVE` exige `REASON_ACTIVATE_ID`; `DISABLED|BLOCKED → ACTIVE` exige `REASON_ENABLE_ID`; `ACTIVE → INACTIVE` exige `REASON_INACTIVATE_ID`; `DISABLED|BLOCKED → INACTIVE` exige `REASON_INACTIVATE_ID`; `ACTIVE → DISABLED|BLOCKED` exige `REASON_DISABLE_ID`. `INACTIVE → DISABLED|BLOCKED` não é transição válida.

#### Scenario: Transição sem motivo obrigatório é rejeitada
- **WHEN** um `SCOS_EMPLOYEE_STATUS_HISTORY` é inserido com `PREVIOUS_STATUS = 'ACTIVE'`, `STATUS = 'DISABLED'` e `REASON_DISABLE_ID` nulo
- **THEN** banco rejeita por violação do `CHECK` `CHK_EMPLOYEE_STATUS_TRANSITION`

#### Scenario: Transição inválida é rejeitada mesmo com motivo preenchido
- **WHEN** um histórico é inserido com `PREVIOUS_STATUS = 'INACTIVE'` e `STATUS = 'DISABLED'`
- **THEN** banco rejeita — essa transição não existe na tabela de transições válidas
