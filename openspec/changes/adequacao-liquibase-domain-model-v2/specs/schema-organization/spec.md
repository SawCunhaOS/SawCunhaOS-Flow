## MODIFIED Requirements

### Requirement: SCOS_DEPARTMENT corrigido
O sistema SHALL ter `SCOS_DEPARTMENT` com `DESCRIPTION VARCHAR(255) NOT NULL` (não 30), `UPDATED_AT TIMESTAMPTZ NOT NULL`, `USER_AT VARCHAR(255) NOT NULL`.

#### Scenario: Tamanho de DESCRIPTION correto
- **WHEN** tabela é criada
- **THEN** coluna `DESCRIPTION` tem tipo `VARCHAR(255)`

### Requirement: SCOS_POSITION corrigido
O sistema SHALL ter `SCOS_POSITION` com `DESCRIPTION VARCHAR(255) NOT NULL` (não 30), `IS_TRUST_POSITION BOOLEAN NOT NULL DEFAULT FALSE` (cargo de confiança — afeta isenção de controle de jornada), `CODE VARCHAR(30) NOT NULL` com UK **composta com `DEPARTMENT_ID`** (mesmo código pode existir em departamentos diferentes), `UPDATED_AT TIMESTAMPTZ NOT NULL`, `USER_AT VARCHAR(255) NOT NULL`.

#### Scenario: Tamanho de DESCRIPTION correto
- **WHEN** tabela é criada
- **THEN** coluna `DESCRIPTION` tem tipo `VARCHAR(255)`

#### Scenario: CODE repetido em departamentos diferentes é aceito
- **WHEN** `CODE = 'ANALISTA_SR'` é inserido para `DEPARTMENT_ID = 1` e depois para `DEPARTMENT_ID = 2`
- **THEN** ambas as inserções são aceitas

#### Scenario: CODE repetido no mesmo departamento é rejeitado
- **WHEN** `CODE = 'ANALISTA_SR'` é inserido duas vezes para o mesmo `DEPARTMENT_ID`
- **THEN** a segunda inserção falha por violação da UK composta

### Requirement: SCOS_COMPANY corrigido
O sistema SHALL ter `SCOS_COMPANY` sem coluna `ACTIVE`, com `NAME VARCHAR(250)`, `NAME_TREATMENT VARCHAR(100)`, `STATUS VARCHAR(20) NOT NULL` restrito a `ACTIVE`/`INACTIVE`/`DISABLED` via `CHECK`, `LEGAL_NATURE_ID BIGINT FK NULL → SCOS_LEGAL_NATURE`, `CNAE_PRINCIPAL_ID BIGINT FK NULL → SCOS_CNAE`, `STATE_REGISTRATION VARCHAR(20) NULL` (aceita `ISENTO`), `MUNICIPAL_REGISTRATION VARCHAR(20) NULL`, `UPDATED_AT TIMESTAMPTZ NOT NULL`, `USER_AT VARCHAR(255) NOT NULL`. Índices em `LEGAL_NATURE_ID` e `CNAE_PRINCIPAL_ID`.

#### Scenario: ACTIVE ausente
- **WHEN** tabela é criada
- **THEN** coluna `ACTIVE` não existe em `SCOS_COMPANY`

#### Scenario: STATUS presente, NOT NULL e restrito ao vocabulário fechado
- **WHEN** tabela é criada
- **THEN** coluna `STATUS VARCHAR(20) NOT NULL` existe
- **WHEN** INSERT usa `STATUS = 'DELETED'`
- **THEN** banco rejeita por violação de `CHK_COMPANY_STATUS`

#### Scenario: LEGAL_NATURE_ID e CNAE_PRINCIPAL_ID opcionais
- **WHEN** empresa é inserida sem `LEGAL_NATURE_ID` nem `CNAE_PRINCIPAL_ID`
- **THEN** banco aceita — ambos são nullable

#### Scenario: Índices de FK criados
- **WHEN** tabela é criada
- **THEN** índices existem em `LEGAL_NATURE_ID` e em `CNAE_PRINCIPAL_ID`

### Requirement: SCOS_COMPANY_ADDRESS com PK composta
O sistema SHALL ter `SCOS_COMPANY_ADDRESS` com PK composta `(COMPANY_ID_ADDRESS BIGINT, COMPANY_ID BIGINT FK)`, sem surrogate key, sem sequence. `ADDRESS_TYPE_ID BIGINT FK NOT NULL → SCOS_ADDRESS_TYPE` (UK composta com `COMPANY_ID`) no lugar de coluna `TYPE` em texto livre. `GEOLOCATION GEOMETRY(POINT) NOT NULL`, `UPDATED_AT TIMESTAMPTZ NOT NULL`, `USER_AT VARCHAR(255) NOT NULL`. Índice em `ADDRESS_TYPE_ID`.

#### Scenario: PK composta correta
- **WHEN** tabela é criada
- **THEN** PK é composta por `COMPANY_ID_ADDRESS` e `COMPANY_ID`; não existe coluna `COMPANY_ADDRESS_ID`

#### Scenario: ADDRESS_TYPE_ID substitui TYPE texto
- **WHEN** tabela é criada
- **THEN** coluna `ADDRESS_TYPE_ID BIGINT FK` existe referenciando `SCOS_ADDRESS_TYPE`; não existe coluna `TYPE` em texto livre

#### Scenario: GEOLOCATION NOT NULL
- **WHEN** tabela é criada
- **THEN** coluna `GEOLOCATION` tem constraint NOT NULL

### Requirement: SCOS_COMPANY_CONTACT corrigido
O sistema SHALL ter `SCOS_COMPANY_CONTACT` com PK `COMPANY_ID_CONTACT` (não `CONTACT_ID`), com UK em `PHONE`, `EMAIL` e `CONTACT_TYPE_ID` (todas compostas com `COMPANY_ID`), `CONTACT_TYPE_ID BIGINT FK NOT NULL → SCOS_CONTACT_TYPE` no lugar de coluna `TYPE` em texto livre, `UPDATED_AT TIMESTAMPTZ NOT NULL`, `USER_AT VARCHAR(255) NOT NULL`. Índice em `CONTACT_TYPE_ID`.

#### Scenario: PK com nome correto
- **WHEN** tabela é criada
- **THEN** PK se chama `COMPANY_ID_CONTACT`, não `CONTACT_ID`

#### Scenario: CONTACT_TYPE_ID substitui TYPE texto
- **WHEN** tabela é criada
- **THEN** coluna `CONTACT_TYPE_ID BIGINT FK` existe referenciando `SCOS_CONTACT_TYPE`; não existe coluna `TYPE` em texto livre

### Requirement: SCOS_EMPLOYEE corrigido
O sistema SHALL ter `SCOS_EMPLOYEE` com `COMPANY_ID FK NOT NULL`, `POSITION_ID FK NOT NULL`, `STATUS VARCHAR(20) NOT NULL` restrito a `ACTIVE`/`INACTIVE`/`DISABLED` via `CHECK`, `CONTRACT_TYPE VARCHAR(20) NOT NULL` restrito a `CLT`/`PJ`/`ESTAGIO`/`TEMPORARIO` via `CHECK`, `PROBATION_END_DATE DATE NULL` (quando preenchido, deve ser posterior a `DATE_OF_HIRING`), sem coluna `ACTIVE`, `NAME VARCHAR(250)`, `NAME_TREATMENT VARCHAR(100)`, `EMAIL VARCHAR(255) UK NOT NULL`, `UPDATED_AT TIMESTAMPTZ NOT NULL`, `USER_AT VARCHAR(255) NOT NULL`.

#### Scenario: COMPANY_ID e POSITION_ID NOT NULL
- **WHEN** INSERT em SCOS_EMPLOYEE é tentado com COMPANY_ID NULL
- **THEN** banco rejeita com violação de NOT NULL

#### Scenario: STATUS presente e restrito ao vocabulário fechado
- **WHEN** tabela é criada
- **THEN** coluna `STATUS VARCHAR(20) NOT NULL` existe
- **WHEN** INSERT usa `STATUS = 'DELETED'`
- **THEN** banco rejeita por violação de `CHK_EMPLOYEE_STATUS`

#### Scenario: CONTRACT_TYPE restrito ao vocabulário fechado
- **WHEN** INSERT usa `CONTRACT_TYPE = 'FREELANCER'`
- **THEN** banco rejeita por violação de `CHK_EMPLOYEE_CONTRACT_TYPE`

#### Scenario: ACTIVE ausente
- **WHEN** tabela é criada
- **THEN** coluna `ACTIVE` não existe em `SCOS_EMPLOYEE`

#### Scenario: IDX_STATUS_SCOS_EMPLOYEE criado
- **WHEN** tabela é criada
- **THEN** índice `IDX_STATUS_SCOS_EMPLOYEE` existe em `STATUS`

### Requirement: SCOS_EMPLOYEE_ADDRESS corrigido
O sistema SHALL ter `SCOS_EMPLOYEE_ADDRESS` com PK composta `(EMPLOYEE_ID_ADDRESS BIGINT, EMPLOYEE_ID BIGINT FK)` — coluna do endereço externo chamada `EMPLOYEE_ID_ADDRESS` (não `ADDRESS_ID`). `ADDRESS_TYPE_ID BIGINT FK NOT NULL → SCOS_ADDRESS_TYPE` (UK composta com `EMPLOYEE_ID`) no lugar de coluna `TYPE` em texto livre. `GEOLOCATION GEOMETRY(POINT) NOT NULL`, `UPDATED_AT TIMESTAMPTZ NOT NULL`, `USER_AT VARCHAR(255) NOT NULL`. Índice em `ADDRESS_TYPE_ID`.

#### Scenario: Coluna com nome correto
- **WHEN** tabela é criada
- **THEN** coluna se chama `EMPLOYEE_ID_ADDRESS`, não `ADDRESS_ID`

#### Scenario: ADDRESS_TYPE_ID substitui TYPE texto
- **WHEN** tabela é criada
- **THEN** coluna `ADDRESS_TYPE_ID BIGINT FK` existe referenciando `SCOS_ADDRESS_TYPE`; não existe coluna `TYPE` em texto livre

### Requirement: SCOS_EMPLOYEE_CONTACT corrigido
O sistema SHALL ter `SCOS_EMPLOYEE_CONTACT` com PK `EMPLOYEE_ID_CONTACT` (não `CONTACT_ID`), UK em `PHONE` e `CONTACT_TYPE_ID` (compostas com `EMPLOYEE_ID`), `CONTACT_TYPE_ID BIGINT FK NOT NULL → SCOS_CONTACT_TYPE` no lugar de coluna `TYPE` em texto livre, `UPDATED_AT TIMESTAMPTZ NOT NULL`, `USER_AT VARCHAR(255) NOT NULL`. Índice em `CONTACT_TYPE_ID`.

#### Scenario: PK com nome correto
- **WHEN** tabela é criada
- **THEN** PK se chama `EMPLOYEE_ID_CONTACT`

#### Scenario: CONTACT_TYPE_ID substitui TYPE texto
- **WHEN** tabela é criada
- **THEN** coluna `CONTACT_TYPE_ID BIGINT FK` existe referenciando `SCOS_CONTACT_TYPE`; não existe coluna `TYPE` em texto livre

## ADDED Requirements

### Requirement: SCOS_POSITION_WORK_SCHEDULE define horário template do cargo
O sistema SHALL ter `SCOS_POSITION_WORK_SCHEDULE` com `POSITION_WORK_SCHEDULE_ID BIGINT PK` (identity), `POSITION_ID BIGINT FK NOT NULL → SCOS_POSITION` (UK composta com `DAY_OF_WEEK`), `DAY_OF_WEEK VARCHAR(10) NOT NULL` restrito a `MONDAY`..`SUNDAY` via `CHECK`, `START_TIME`/`LUNCH_START`/`LUNCH_END`/`END_TIME TIME NOT NULL`, campos de auditoria NOT NULL. É apenas um template — existe para ser copiado pra `SCOS_EMPLOYEE_WORK_SCHEDULE`, não afeta funcionários já com linha própria.

#### Scenario: Um cargo não pode ter duas linhas pro mesmo dia
- **WHEN** `POSITION_ID = 1` já tem uma linha com `DAY_OF_WEEK = 'MONDAY'`
- **THEN** inserir outra linha com o mesmo `POSITION_ID`/`DAY_OF_WEEK` falha por violação da UK composta

#### Scenario: DAY_OF_WEEK restrito ao vocabulário fechado
- **WHEN** INSERT usa `DAY_OF_WEEK = 'HOLIDAY'`
- **THEN** banco rejeita por violação de `CHK_POSITION_WORK_SCHEDULE_DAY`

### Requirement: SCOS_EMPLOYEE_WORK_SCHEDULE define horário efetivo do funcionário
O sistema SHALL ter `SCOS_EMPLOYEE_WORK_SCHEDULE` com a mesma estrutura de `SCOS_POSITION_WORK_SCHEDULE`, trocando `POSITION_ID` por `EMPLOYEE_ID BIGINT FK NOT NULL → SCOS_EMPLOYEE`. É a fonte de verdade do horário do funcionário, populada por cópia do template (ação da aplicação, não automática) e editável livremente depois — sem relação retroativa com o cargo. Dia sem linha significa **não definido**, não fallback pro template.

#### Scenario: Um funcionário não pode ter duas linhas pro mesmo dia
- **WHEN** `EMPLOYEE_ID = 1` já tem uma linha com `DAY_OF_WEEK = 'MONDAY'`
- **THEN** inserir outra linha com o mesmo `EMPLOYEE_ID`/`DAY_OF_WEEK` falha por violação da UK composta

#### Scenario: DAY_OF_WEEK restrito ao vocabulário fechado
- **WHEN** INSERT usa `DAY_OF_WEEK = 'HOLIDAY'`
- **THEN** banco rejeita por violação de `CHK_EMPLOYEE_WORK_SCHEDULE_DAY`

### Requirement: SCOS_REASON_POSITION_CHANGE cataloga motivos de mudança de cargo
O sistema SHALL ter `SCOS_REASON_POSITION_CHANGE` com `REASON_POSITION_CHANGE_ID BIGINT PK` (identity), `CODE VARCHAR(30) UK NOT NULL`, `DESCRIPTION VARCHAR(255) NOT NULL`, `ACTIVE BOOLEAN NOT NULL DEFAULT TRUE`, campos de auditoria NOT NULL. Sem `ENTITY_TYPE` — aplica-se somente a `EMPLOYEE`. `DELETE` SHALL ser bloqueado por trigger.

#### Scenario: DELETE bloqueado
- **WHEN** `DELETE FROM SCOS_REASON_POSITION_CHANGE` é tentado
- **THEN** banco rejeita via trigger `TRG_BLOCK_DELETE_REASON_POSITION_CHANGE`

### Requirement: SCOS_EMPLOYEE_POSITION_HISTORY registra linha do tempo de cargos, imutável
O sistema SHALL ter `SCOS_EMPLOYEE_POSITION_HISTORY` com `EMPLOYEE_POSITION_HISTORY_ID BIGINT PK`, `EMPLOYEE_ID BIGINT FK NOT NULL → SCOS_EMPLOYEE`, `POSITION_ID BIGINT FK NOT NULL → SCOS_POSITION`, `START_DATE DATE NOT NULL`, `END_DATE DATE NULL` (nulo = cargo atual, preenchido por trigger), `REASON_POSITION_CHANGE_ID BIGINT FK NOT NULL`, `CREATED_AT TIMESTAMPTZ NOT NULL`, `USER_AT VARCHAR(255) NOT NULL`. Sem `UPDATED_AT` — imutável. Índice único parcial `(EMPLOYEE_ID) WHERE END_DATE IS NULL` garante que nunca haja duas linhas abertas simultaneamente para o mesmo funcionário. Índice em `POSITION_ID` para consulta reversa.

#### Scenario: Nunca duas linhas abertas para o mesmo funcionário
- **WHEN** `EMPLOYEE_ID = 1` já tem uma linha com `END_DATE IS NULL`
- **THEN** inserir outra linha com `END_DATE IS NULL` para o mesmo `EMPLOYEE_ID` falha por violação do índice único parcial

#### Scenario: END_DATE posterior a START_DATE
- **WHEN** um registro é inserido com `END_DATE` anterior a `START_DATE`
- **THEN** banco rejeita por violação de constraint de integridade

### Requirement: Trigger fecha automaticamente a linha de cargo anterior
O sistema SHALL, via trigger `BEFORE INSERT` em `SCOS_EMPLOYEE_POSITION_HISTORY`, fechar a linha atualmente aberta do funcionário (`END_DATE = NEW.START_DATE`) antes da nova linha entrar. A aplicação só insere a linha nova, nunca dá `UPDATE` na anterior.

#### Scenario: Linha anterior fechada automaticamente
- **WHEN** um novo registro é inserido para `EMPLOYEE_ID = 1` com `START_DATE = '2026-08-01'`, havendo uma linha aberta anterior
- **THEN** trigger `TRG_CLOSE_PREVIOUS_POSITION` atualiza a linha anterior com `END_DATE = '2026-08-01'`

### Requirement: Trigger sincroniza POSITION_ID cache em SCOS_EMPLOYEE
O sistema SHALL, via trigger `AFTER INSERT` em `SCOS_EMPLOYEE_POSITION_HISTORY`, propagar `NEW.POSITION_ID` para `SCOS_EMPLOYEE.POSITION_ID`. A aplicação SHALL sempre inserir no histórico primeiro — nunca fazer `UPDATE` direto de `POSITION_ID` na tabela principal.

#### Scenario: POSITION_ID sincronizado após INSERT no histórico
- **WHEN** um registro é inserido em `SCOS_EMPLOYEE_POSITION_HISTORY` com `POSITION_ID = 5`
- **THEN** `SCOS_EMPLOYEE.POSITION_ID` daquele funcionário passa a ser `5`
