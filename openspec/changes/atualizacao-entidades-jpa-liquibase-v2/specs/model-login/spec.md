## MODIFIED Requirements

### Requirement: Login mapeado conforme schema do banco
A entidade `Login` SHALL mapear exatamente as colunas da tabela `SCOS_LOGIN`. O campo `keycloakId` (`UUID`, coluna `KEYCLOAK_ID`) MUST ser renomeado para `externalId` (coluna `EXTERNAL_ID`), acompanhando o rename da coluna no schema v2. O campo `TYPE` continua mapeado como `LoginType`.

#### Scenario: Campo externalId presente, keycloakId ausente
- **WHEN** a entidade `Login` é inspecionada
- **THEN** deve existir campo `externalId` (`UUID`, coluna `EXTERNAL_ID`); não deve existir campo `keycloakId`

#### Scenario: VwAuthorityResponse acompanha o rename
- **WHEN** a entidade `VwAuthorityResponse` é inspecionada
- **THEN** o campo antes chamado `keycloakId` deve se chamar `externalId`, mapeando a coluna `external_id` da view

#### Scenario: AuthorityResponseOutput e mapper acompanham o rename
- **WHEN** `AuthorityResponseOutput` e `AuthorityResponseMapper` são inspecionados
- **THEN** o campo `externalId` substitui `keycloakId` em ambos, sem necessidade de mapeamento manual adicional (nomes batem)

### Requirement: LoginStatus sem DELETED/LOCKED, com BLOCKED
O enum `LoginStatus` SHALL conter exatamente `ACTIVE`, `INACTIVE` e `BLOCKED` — `DELETED` e `LOCKED` não existem mais, pois violam o `CHECK` de vocabulário fechado do schema v2 (`ACTIVE/INACTIVE/BLOCKED`).

#### Scenario: Enum com três valores
- **WHEN** o enum `LoginStatus` é inspecionado
- **THEN** deve conter exatamente `ACTIVE`, `INACTIVE`, `BLOCKED`

#### Scenario: LoginDeletedRule removida
- **WHEN** o pacote `access/login/internal/rules` é inspecionado
- **THEN** não deve existir a classe `LoginDeletedRule`

#### Scenario: LoginLockedRule renomeada para LoginBlockedRule
- **WHEN** o pacote `access/login/internal/rules` é inspecionado
- **THEN** não deve existir `LoginLockedRule`; deve existir `LoginBlockedRule` validando `context.equals(LoginStatus.BLOCKED)` com o mesmo código de erro `SCOS_LOGIN_011`

#### Scenario: Regras remanescentes renumeradas sem lacuna
- **WHEN** `@ScosRule` das regras remanescentes de `Login` é inspecionado
- **THEN** `LoginInactiveRule` deve ser `@ScosRule(1)` e `LoginBlockedRule` deve ser `@ScosRule(2)`

### Requirement: Login com métodos de negócio baseados em histórico de status
Os métodos `activate(reasonActivateId)`, `inactivate(reasonInactivateId)`, `disable(reasonDisableId)` e `enable(reasonEnableId)` da entidade `Login` SHALL validar a transição client-side e retornar uma instância não persistida de `LoginStatusHistory` em vez de mutar `status` diretamente. `disable`/`enable` operam sobre `BLOCKED` (não `DISABLED`).

#### Scenario: disable retorna histórico com status BLOCKED
- **WHEN** `login.disable(reasonDisableId)` é chamado com `status == ACTIVE`
- **THEN** retorna `LoginStatusHistory` com `status = BLOCKED` e `reasonDisableId` preenchido

#### Scenario: enable exige status BLOCKED
- **WHEN** `login.enable(reasonEnableId)` é chamado com `status != BLOCKED`
- **THEN** lança `ScosException`

## REMOVED Requirements

### Requirement: SCOS_LOGIN_012 (código de erro de Login deletado)
**Reason**: `LoginStatus.DELETED` foi removido — o vocabulário fechado do schema v2 (`ACTIVE/INACTIVE/BLOCKED`) não admite mais um estado de exclusão lógica distinto para `Login`. A regra `LoginDeletedRule` que emitia este código é removida junto.
**Migration**: Nenhuma — código nunca foi exposto publicamente. Remover `SCOS_LOGIN_012` de `ExceptionCodeError` e dos bundles `scos_message_organization.properties`/`scos_message_organization_en.properties`.
