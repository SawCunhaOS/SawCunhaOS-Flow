## ADDED Requirements

### Requirement: Entidade ProfileResource criada com PK composta
A entidade `ProfileResource` SHALL ser criada no pacote `model/permission`, mapeando a tabela `SCOS_PROFILE_RESOURCE`. A tabela NÃO possui `UPDATED_AT`, portanto a entidade MUST NOT estender `BaseEntity` — deve declarar `createdAt` (`@CreationTimestamp`, coluna `CREATED_AT`) e `userAt` (coluna `USER_AT`) diretamente. SHALL usar `@Auditable` (sem `auditRead`), Lombok completo, e PK composta via `@EmbeddedId ProfileResourcePk`.

A classe `ProfileResourcePk` SHALL ser `@Embeddable` com campos `profileId` (`BIGINT`, coluna `PROFILE_ID`) e `resourceId` (`UUID`, coluna `RESOURCE_ID`).

#### Scenario: Entidade ProfileResource não estende BaseEntity
- **WHEN** a entidade `ProfileResource` é inspecionada
- **THEN** não deve estender `BaseEntity` e deve ter campo `createdAt` com `@CreationTimestamp` e campo `userAt`

#### Scenario: PK composta ProfileResourcePk presente
- **WHEN** a entidade `ProfileResource` é inspecionada
- **THEN** deve usar `@EmbeddedId` do tipo `ProfileResourcePk` com campos `profileId` (Long) e `resourceId` (UUID)

#### Scenario: Repositório ProfileResourceRepository criado
- **WHEN** o pacote `repository/permission` é inspecionado
- **THEN** deve existir `ProfileResourceRepository` estendendo `BaseJpaRepository<ProfileResource, ProfileResourcePk>` + `JpaSpecificationExecutor<ProfileResource>` + `QuerydslPredicateExecutor<ProfileResource>`
