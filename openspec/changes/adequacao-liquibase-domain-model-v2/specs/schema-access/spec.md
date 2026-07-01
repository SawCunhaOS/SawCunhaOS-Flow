## MODIFIED Requirements

### Requirement: SCOS_LOGIN reescrito
O sistema SHALL ter `SCOS_LOGIN` com `EMPLOYEE_ID BIGINT FK NULL` (nullable — permite usuários externos e contas de serviço), sem colunas `PASSWORD`, `SALT`, `DATE_LAST_CHANGE_PASSWORD`, com `TYPE VARCHAR(50) NOT NULL` restrito a `EMPLOYEE`/`EXTERNAL`/`SERVICE` via `CHECK`, `LOGIN VARCHAR(255) NOT NULL`, `STATUS VARCHAR(50) NOT NULL` restrito a `ACTIVE`/`INACTIVE`/`BLOCKED` via `CHECK`, `EXTERNAL_ID UUID NULL` (renomeado de `KEYCLOAK_ID` — ID do usuário no provedor de identidade externo, hoje Keycloak; nome genérico permite trocar de provedor sem renomear coluna), `UPDATED_AT TIMESTAMPTZ NOT NULL`, `USER_AT VARCHAR(255) NOT NULL`.

#### Scenario: EMPLOYEE_ID nullable
- **WHEN** INSERT em SCOS_LOGIN é feito sem EMPLOYEE_ID
- **THEN** banco aceita o registro (EMPLOYEE_ID NULL é válido)

#### Scenario: PASSWORD e SALT ausentes
- **WHEN** tabela é criada
- **THEN** colunas `PASSWORD` e `SALT` não existem em `SCOS_LOGIN`

#### Scenario: TYPE presente, NOT NULL e restrito ao vocabulário fechado
- **WHEN** tabela é criada
- **THEN** coluna `TYPE VARCHAR(50) NOT NULL` existe
- **WHEN** INSERT usa `TYPE = 'ADMIN'`
- **THEN** banco rejeita por violação de `CHK_LOGIN_TYPE`

#### Scenario: STATUS restrito ao vocabulário fechado (com BLOCKED, não DISABLED)
- **WHEN** INSERT usa `STATUS = 'DISABLED'`
- **THEN** banco rejeita por violação de `CHK_LOGIN_STATUS` — o terceiro estado de LOGIN é `BLOCKED`, não `DISABLED`

#### Scenario: KEYCLOAK_ID renomeado para EXTERNAL_ID
- **WHEN** tabela é criada
- **THEN** coluna `EXTERNAL_ID UUID NULL` existe; coluna `KEYCLOAK_ID` não existe

#### Scenario: IDX_EXTERNAL_ID_SCOS_LOGIN criado
- **WHEN** tabela é criada
- **THEN** índice `IDX_EXTERNAL_ID_SCOS_LOGIN` existe em `EXTERNAL_ID` (substitui `IDX_KEYCLOAK_ID_SCOS_LOGIN`)

#### Scenario: LOGIN com tamanho correto
- **WHEN** tabela é criada
- **THEN** coluna `LOGIN` é `VARCHAR(255)` (não VARCHAR(100))

## ADDED Requirements

### Requirement: SCOS_LOGIN_PROFILE registra perfis adicionais de um login
O sistema SHALL ter `SCOS_LOGIN_PROFILE` com PK composta `(LOGIN_ID BIGINT FK → SCOS_LOGIN, PROFILE_ID BIGINT FK → SCOS_PROFILE)`, `CREATED_AT TIMESTAMPTZ NOT NULL`, `USER_AT VARCHAR(255) NOT NULL`. Sem `UPDATED_AT` — o vínculo é criado ou removido, não atualizado. Representa perfis **adicionais** além do principal (`SCOS_LOGIN.PROFILE_ID`); "todos os perfis de um login" é a união entre o principal e as linhas aqui. Índice `IDX_LOGIN_PROFILE_PROFILE_ID` em `PROFILE_ID` para consulta reversa.

#### Scenario: Mesmo par não pode se repetir
- **WHEN** o mesmo par `(LOGIN_ID, PROFILE_ID)` é inserido duas vezes
- **THEN** a segunda inserção falha por violação da PK composta

#### Scenario: Índice reverso criado
- **WHEN** tabela é criada
- **THEN** `IDX_LOGIN_PROFILE_PROFILE_ID` existe em `PROFILE_ID`

### Requirement: Trigger impede duplicar o perfil principal como adicional
O sistema SHALL, via trigger `BEFORE INSERT OR UPDATE` em `SCOS_LOGIN_PROFILE`, rejeitar a gravação de um `PROFILE_ID` que já é o perfil principal do login (`SCOS_LOGIN.PROFILE_ID`) — sem isso, o mesmo perfil apareceria duplicado (principal e adicional), quebrando a regra de união usada para listar todos os perfis.

#### Scenario: Perfil principal não pode ser duplicado como adicional
- **WHEN** um `SCOS_LOGIN_PROFILE` é inserido com `PROFILE_ID` igual ao `PROFILE_ID` principal do mesmo `LOGIN_ID`
- **THEN** trigger `TRG_VALIDATE_LOGIN_PROFILE_NOT_PRIMARY` rejeita com exceção
