## ADDED Requirements

### Requirement: Entidades LegalNature e Cnae com mapeamento mínimo próprio
O sistema SHALL fornecer `LegalNature` e `Cnae` — tabelas de seed puro (IBGE) com apenas `ID`/`CODE`/`DESCRIPTION`/`CREATED_AT`, sem `UPDATED_AT` nem `USER_AT`. Não estendem `BaseEntity` nem seguem o padrão `ProfileResource` (que assume `userAt`). Mapeamento próprio: `@Id @GeneratedValue`, `code`, `description`, `createdAt` (`@CreationTimestamp`) — sem campo `userAt`.

#### Scenario: Entidades sem campo userAt
- **WHEN** `LegalNature` ou `Cnae` são inspecionadas
- **THEN** nenhum campo `userAt` deve existir na classe

#### Scenario: Entidades sem updatedAt
- **WHEN** `LegalNature` ou `Cnae` são inspecionadas
- **THEN** nenhum campo `updatedAt` deve existir na classe

### Requirement: Entidade CompanyCnaeSecondary com PK composta
O sistema SHALL fornecer `CompanyCnaeSecondary` — PK composta (`companyId` + `cnaeId`), imutável, padrão `ProfileResource` (`createdAt` `@CreationTimestamp`, `userAt` `NOT NULL`, sem `BaseEntity`).

#### Scenario: PK composta via EmbeddedId
- **WHEN** a entidade `CompanyCnaeSecondary` é inspecionada
- **THEN** deve usar `@EmbeddedId` com `companyId` e `cnaeId`

#### Scenario: Repositório criado
- **WHEN** o módulo é inspecionado
- **THEN** deve existir `CompanyCnaeSecondaryRepository` estendendo `BaseJpaRepository<CompanyCnaeSecondary, CompanyCnaeSecondaryPk>` + `JpaSpecificationExecutor` + `QuerydslPredicateExecutor`
