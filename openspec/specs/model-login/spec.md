## ADDED Requirements

### Requirement: Login mapeado conforme schema do banco
A entidade `Login` SHALL mapear exatamente as colunas da tabela `SCOS_LOGIN`. Os campos `password`, `salt`, `dateCreated` e `dateLastChangePassword` não existem no banco e MUST ser removidos. O campo `TYPE` (`VARCHAR(50)`) existe no banco e MUST ser adicionado como `LoginType` com `@Enumerated(EnumType.STRING)`.

#### Scenario: Campos obsoletos removidos de Login
- **WHEN** a entidade `Login` é inspecionada
- **THEN** os campos `password`, `salt`, `dateCreated` e `dateLastChangePassword` não devem existir

#### Scenario: Campo type presente e tipado
- **WHEN** a entidade `Login` é inspecionada
- **THEN** deve existir campo `type` do tipo `LoginType` anotado com `@Enumerated(EnumType.STRING)` mapeando coluna `TYPE`

### Requirement: LoginType com valores corretos
O enum `LoginType` SHALL conter exatamente os valores `EMPLOYEE`, `EXTERNAL` e `SERVICE`, seguindo o padrão de enum do projeto com `@Getter` e `displayName`.

#### Scenario: Enum com três valores
- **WHEN** o enum `LoginType` é inspecionado
- **THEN** deve conter os valores `EMPLOYEE`, `EXTERNAL` e `SERVICE` e nenhum outro

### Requirement: Login com auditoria de leitura LGPD habilitada
A entidade `Login` SHALL ser anotada com `@Auditable(auditRead = true)` por conter login (e-mail) e `keycloakId`, dados vinculados à identidade pessoal do usuário.

#### Scenario: Anotação auditRead presente em Login
- **WHEN** a entidade `Login` é inspecionada
- **THEN** deve estar anotada com `@Auditable(auditRead = true)`

### Requirement: Profile sem campo features
A entidade `Profile` SHALL ter o campo `features` (`FEATURES text[]`) removido, pois esta coluna não existe na tabela `SCOS_PROFILE` do banco.

#### Scenario: Campo features removido de Profile
- **WHEN** a entidade `Profile` é inspecionada
- **THEN** o campo `features` não deve existir na classe
