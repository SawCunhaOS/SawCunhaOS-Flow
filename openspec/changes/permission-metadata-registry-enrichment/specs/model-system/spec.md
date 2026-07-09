## MODIFIED Requirements

### Requirement: Entidade Resource criada conforme schema do banco
A entidade `Resource` SHALL ser criada no pacote `model/system`, mapeando a tabela `SCOS_RESOURCE`. SHALL estender `BaseEntity`, usar `@Auditable` (sem `auditRead`), Lombok completo, PK `UUID` com `@GeneratedValue(strategy = GenerationType.UUID)`. Campos: `system` (FK `@ManyToOne` para `System`, coluna `SYSTEM_ID`), `code` (UK, `VARCHAR(50)`), `descriptionPt` (coluna `DESCRIPTION_PT`, `VARCHAR(255)`), `descriptionEn` (coluna `DESCRIPTION_EN`, `VARCHAR(255)`), `active` (`BOOLEAN`), e os metadados da definição da permissão: `resourceGroup` (coluna `RESOURCE_GROUP`, `VARCHAR(100)`), `subGroup` (coluna `SUB_GROUP`, `VARCHAR(100)`), `version` (coluna `VERSION`, `VARCHAR(20)`) e `definitionUpdatedAt` (coluna `DEFINITION_UPDATED_AT`, `DATE`). A coluna de grupo SHALL chamar-se `RESOURCE_GROUP` (não `GROUP`, palavra reservada SQL).

#### Scenario: Entidade Resource existe com campos corretos
- **WHEN** a entidade `Resource` é inspecionada
- **THEN** deve ter PK UUID, relacionamento `@ManyToOne` para `System`, campos `code`, `descriptionPt`, `descriptionEn`, `active`, `resourceGroup`, `subGroup`, `version`, `definitionUpdatedAt` e estender `BaseEntity`

#### Scenario: Colunas de metadados presentes na tabela
- **WHEN** a tabela `SCOS_RESOURCE` é inspecionada
- **THEN** existem as colunas `RESOURCE_GROUP`, `SUB_GROUP`, `VERSION` e `DEFINITION_UPDATED_AT`

#### Scenario: Repositório ResourceRepository criado
- **WHEN** o pacote `repository/system` é inspecionado
- **THEN** deve existir `ResourceRepository` estendendo `BaseJpaRepository<Resource, UUID>` + `JpaSpecificationExecutor<Resource>` + `QuerydslPredicateExecutor<Resource>`
