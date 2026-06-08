## ADDED Requirements

### Requirement: Entidade IntegrationKeycloak criada conforme schema do banco
A entidade `IntegrationKeycloak` SHALL ser criada no pacote `model/integration`, mapeando a tabela `SCOS_INTEGRATION_KEYCLOAK`. SHALL estender `BaseEntity` (tem `CREATED_AT + UPDATED_AT + USER_AT`, ambos nullable no banco — `BaseEntity` gerencia automaticamente). SHALL usar `@Auditable(auditRead = true)` por conter `email` e `username` (dados pessoais — LGPD). Lombok completo. PK `UUID` com `@GeneratedValue(strategy = GenerationType.UUID)`.

Campos: `keycloakId` (UUID null), `realm`, `email`, `username`, `requesting`, `type` (String, coluna `TYPE`), `status` (String, coluna `STATUS`), `dateStart` (OffsetDateTime null, coluna `DATE_START`), `dateEnd` (OffsetDateTime null, coluna `DATE_END`), `message` (String null), `request` (String com `@Type(JsonBinaryType.class)`, coluna `REQUEST`), `retryCount` (int default 0), `maxRetries` (int default 3).

#### Scenario: Entidade IntegrationKeycloak existe com todos os campos
- **WHEN** a entidade `IntegrationKeycloak` é inspecionada
- **THEN** deve ter todos os campos listados, PK UUID, estender `BaseEntity` e `@Auditable(auditRead = true)`

#### Scenario: Campo REQUEST mapeado como JSONB
- **WHEN** o campo `request` de `IntegrationKeycloak` é inspecionado
- **THEN** deve estar anotado com `@Type(JsonBinaryType.class)` e ser do tipo `String`

#### Scenario: Repositório IntegrationKeycloakRepository criado
- **WHEN** o pacote `repository/integration` é inspecionado
- **THEN** deve existir `IntegrationKeycloakRepository` estendendo `BaseJpaRepository<IntegrationKeycloak, UUID>` + `JpaSpecificationExecutor<IntegrationKeycloak>` + `QuerydslPredicateExecutor<IntegrationKeycloak>`

### Requirement: Entidade IntegrationKeycloakLog criada como tabela imutável
A entidade `IntegrationKeycloakLog` SHALL ser criada no pacote `model/integration`, mapeando `SCOS_INTEGRATION_KEYCLOAK_LOG`. A tabela NÃO possui `UPDATED_AT` — MUST NOT estender `BaseEntity`. SHALL declarar `createdAt` (`@CreationTimestamp`, coluna `CREATED_AT`) e `userAt` (coluna `USER_AT`) diretamente. SHALL usar `@Auditable` (sem `auditRead` — sem PII direto). Lombok completo. PK `UUID` com `@GeneratedValue(strategy = GenerationType.UUID)`.

Campos: `integrationKeycloak` (`@ManyToOne` para `IntegrationKeycloak`, coluna `INTEGRATION_KEYCLOAK_ID`), `success` (boolean), `response` (String null).

#### Scenario: IntegrationKeycloakLog não estende BaseEntity
- **WHEN** a entidade `IntegrationKeycloakLog` é inspecionada
- **THEN** não deve estender `BaseEntity` e deve ter `createdAt` com `@CreationTimestamp`

#### Scenario: Repositório IntegrationKeycloakLogRepository criado
- **WHEN** o pacote `repository/integration` é inspecionado
- **THEN** deve existir `IntegrationKeycloakLogRepository` estendendo `BaseJpaRepository<IntegrationKeycloakLog, UUID>` + `JpaSpecificationExecutor<IntegrationKeycloakLog>` + `QuerydslPredicateExecutor<IntegrationKeycloakLog>`

### Requirement: Entidade IntegrationMessageInvalid criada como tabela imutável
A entidade `IntegrationMessageInvalid` SHALL ser criada no pacote `model/integration`, mapeando `SCOS_INTEGRATION_MESSAGE_INVALID`. A tabela NÃO possui `UPDATED_AT` — MUST NOT estender `BaseEntity`. SHALL declarar `createdAt` e `userAt` diretamente. SHALL usar `@Auditable` (sem `auditRead`). Lombok completo. PK `UUID` com `@GeneratedValue(strategy = GenerationType.UUID)`.

Campos: `messageInvalid` (String, coluna `MESSAGE_INVALID`, `VARCHAR(5000)`).

#### Scenario: IntegrationMessageInvalid não estende BaseEntity
- **WHEN** a entidade `IntegrationMessageInvalid` é inspecionada
- **THEN** não deve estender `BaseEntity` e deve ter `createdAt` com `@CreationTimestamp`

#### Scenario: Repositório IntegrationMessageInvalidRepository criado
- **WHEN** o pacote `repository/integration` é inspecionado
- **THEN** deve existir `IntegrationMessageInvalidRepository` estendendo `BaseJpaRepository<IntegrationMessageInvalid, UUID>` + `JpaSpecificationExecutor<IntegrationMessageInvalid>` + `QuerydslPredicateExecutor<IntegrationMessageInvalid>`
