## ADDED Requirements

### Requirement: Entidade System criada conforme schema do banco
A entidade `System` SHALL ser criada no pacote `model/system`, mapeando a tabela `SCOS_SYSTEM`. SHALL estender `BaseEntity` (tem `CREATED_AT + UPDATED_AT + USER_AT`), usar `@Auditable` (sem `auditRead` — sem PII), Lombok completo, e PK `UUID` com `@GeneratedValue(strategy = GenerationType.UUID)`. Campos: `code` (UK, `VARCHAR(25)`), `description` (`VARCHAR(200)`), `secretKey` (`TEXT`, coluna `SECRET_KEY`), `status` (`VARCHAR(50)`, mapeado como `String`).

#### Scenario: Entidade System existe com campos corretos
- **WHEN** a entidade `System` é inspecionada
- **THEN** deve ter PK UUID, campos `code`, `description`, `secretKey`, `status` e estender `BaseEntity`

#### Scenario: Repositório SystemRepository criado
- **WHEN** o pacote `repository/system` é inspecionado
- **THEN** deve existir `SystemRepository` estendendo `BaseJpaRepository<System, UUID>` + `JpaSpecificationExecutor<System>` + `QuerydslPredicateExecutor<System>`

### Requirement: Entidade Resource criada conforme schema do banco
A entidade `Resource` SHALL ser criada no pacote `model/system`, mapeando a tabela `SCOS_RESOURCE`. SHALL estender `BaseEntity`, usar `@Auditable` (sem `auditRead`), Lombok completo, PK `UUID` com `@GeneratedValue(strategy = GenerationType.UUID)`. Campos: `system` (FK `@ManyToOne` para `System`, coluna `SYSTEM_ID`), `code` (UK, `VARCHAR(50)`), `description` (`VARCHAR(255)`), `active` (`BOOLEAN`).

#### Scenario: Entidade Resource existe com campos corretos
- **WHEN** a entidade `Resource` é inspecionada
- **THEN** deve ter PK UUID, relacionamento `@ManyToOne` para `System`, campos `code`, `description`, `active` e estender `BaseEntity`

#### Scenario: Repositório ResourceRepository criado
- **WHEN** o pacote `repository/system` é inspecionado
- **THEN** deve existir `ResourceRepository` estendendo `BaseJpaRepository<Resource, UUID>` + `JpaSpecificationExecutor<Resource>` + `QuerydslPredicateExecutor<Resource>`
