## ADDED Requirements

### Requirement: Entidades AddressType e ContactType
O sistema SHALL fornecer `AddressType` e `ContactType` — entidades de referência estendendo `BaseEntity`, anotadas com `@Auditable`, com `code`, `description`, `entityType` (reaproveita o enum `EntityType` de `model-status-history`) e `active`.

#### Scenario: Entidades estendem BaseEntity
- **WHEN** `AddressType` ou `ContactType` são inspecionadas
- **THEN** ambas devem estender `BaseEntity` e estar anotadas com `@Auditable`

#### Scenario: Repositórios criados
- **WHEN** o módulo é inspecionado
- **THEN** deve existir `AddressTypeRepository` e `ContactTypeRepository`, cada um estendendo `BaseJpaRepository<T, Long>` + `JpaSpecificationExecutor<T>` + `QuerydslPredicateExecutor<T>`

### Requirement: EmployeeAddress e CompanyAddress usam AddressType tipado
`EmployeeAddress` e `CompanyAddress` SHALL substituir o campo `type: String` por `addressType: AddressType` (`@ManyToOne`, FK `ADDRESS_TYPE_ID`).

#### Scenario: Campo type removido
- **WHEN** `EmployeeAddress` ou `CompanyAddress` são inspecionadas
- **THEN** não deve existir campo `type` do tipo `String`; deve existir `addressType` do tipo `AddressType`

### Requirement: EmployeeContact e CompanyContact usam ContactType tipado
`EmployeeContact` e `CompanyContact` SHALL substituir o campo `type: String` por `contactType: ContactType` (`@ManyToOne`, FK `CONTACT_TYPE_ID`).

#### Scenario: Campo type removido
- **WHEN** `EmployeeContact` ou `CompanyContact` são inspecionadas
- **THEN** não deve existir campo `type` do tipo `String`; deve existir `contactType` do tipo `ContactType`
