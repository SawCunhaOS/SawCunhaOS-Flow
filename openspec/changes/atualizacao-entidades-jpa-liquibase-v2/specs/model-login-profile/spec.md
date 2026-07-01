## ADDED Requirements

### Requirement: Entidade LoginProfile
O sistema SHALL fornecer `LoginProfile` — PK composta (`loginId` + `profileId`), imutável, padrão `ProfileResource` (`createdAt` `@CreationTimestamp`, `userAt` `NOT NULL`, sem `BaseEntity`), representando perfis adicionais atribuídos a um login além do `Profile` principal.

#### Scenario: PK composta via EmbeddedId
- **WHEN** a entidade `LoginProfile` é inspecionada
- **THEN** deve usar `@EmbeddedId` com `loginId` e `profileId`

#### Scenario: Repositório criado
- **WHEN** o módulo é inspecionado
- **THEN** deve existir `LoginProfileRepository` estendendo `BaseJpaRepository<LoginProfile, LoginProfilePk>` + `JpaSpecificationExecutor` + `QuerydslPredicateExecutor`
