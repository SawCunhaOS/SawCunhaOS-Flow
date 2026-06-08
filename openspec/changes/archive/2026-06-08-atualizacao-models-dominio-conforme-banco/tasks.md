## 1. Enum StatusCompany — executar primeiro para não quebrar compilação

- [x] 1.1 Renomear `StatusCompany.DELETED` para `DISABLED` e ajustar `displayName`
- [x] 1.2 Atualizar `CompanyRepository`: substituir todas as ocorrências de `StatusCompany.DELETED` por `StatusCompany.DISABLED`
- [x] 1.3 Atualizar `CompanyAddressRepository`: substituir `StatusCompany.DELETED` por `StatusCompany.DISABLED`
- [x] 1.4 Atualizar `CompanyContactRepository`: substituir `StatusCompany.DELETED` por `StatusCompany.DISABLED`

## 2. Refatorar Company

- [x] 2.1 Remover campos `dateCreated` e `active` de `Company`
- [x] 2.2 Ajustar `isActive()`: operar apenas sobre `status`, sem referência a `active`
- [x] 2.3 Ajustar `inactivate()` e `activate()`: remover referências a `active`
- [x] 2.4 Renomear método `delete()` para `disable()` e atualizar lógica para `StatusCompany.DISABLED`

## 3. Refatorar CompanyContact

- [x] 3.1 Corrigir coluna PK: `CONTACT_ID` → `COMPANY_ID_CONTACT`
- [x] 3.2 Remover campo `responsiblePerson`

## 4. Refatorar CompanyAddress para PK composta

- [x] 4.1 Criar classe `CompanyAddressPk` (`@Embeddable`) com campos `companyIdAddress` (coluna `COMPANY_ID_ADDRESS`) e `companyId` (coluna `COMPANY_ID`)
- [x] 4.2 Refatorar `CompanyAddress`: substituir PK simples por `@EmbeddedId CompanyAddressPk`; remover campo `addressId` separado
- [x] 4.3 Atualizar `CompanyAddressRepository`: mudar para `BaseJpaRepository<CompanyAddress, CompanyAddressPk>`
- [x] 4.4 Revisar e corrigir métodos `findCompanyContactByCompany` e `deleteByCompanyIdAndId` para nova PK

## 5. Refatorar Employee

- [x] 5.1 Criar enum `StatusEmployee` com valores `ACTIVE`, `INACTIVE`, `DISABLED` (`@Getter`, `displayName`)
- [x] 5.2 Remover campos `dateCreated` e `active` de `Employee`
- [x] 5.3 Adicionar campo `status` do tipo `StatusEmployee` com `@Enumerated(EnumType.STRING)`, coluna `STATUS`
- [x] 5.4 Adicionar métodos de negócio `activate()`, `inactivate()` e `disable()` em `Employee`
- [x] 5.5 Alterar `@Auditable` para `@Auditable(auditRead = true)` em `Employee`

## 6. Corrigir EmployeeContact e EmployeeAddressPk

- [x] 6.1 Corrigir coluna PK em `EmployeeContact`: `CONTACT_ID` → `EMPLOYEE_ID_CONTACT`
- [x] 6.2 Alterar `@Auditable` para `@Auditable(auditRead = true)` em `EmployeeContact`
- [x] 6.3 Alterar `@Auditable` para `@Auditable(auditRead = true)` em `EmployeeAddress`
- [x] 6.4 Renomear campo `addressId` → `employeeIdAddress` e coluna `ADDRESS_ID` → `EMPLOYEE_ID_ADDRESS` em `EmployeeAddressPk`

## 7. Refatorar Login e Profile

- [x] 7.1 Criar enum `LoginType` com valores `EMPLOYEE`, `EXTERNAL`, `SERVICE` (`@Getter`, `displayName`)
- [x] 7.2 Remover campos `password`, `salt`, `dateCreated`, `dateLastChangePassword` de `Login`
- [x] 7.3 Adicionar campo `type` do tipo `LoginType` com `@Enumerated(EnumType.STRING)`, coluna `TYPE`
- [x] 7.4 Alterar `@Auditable` para `@Auditable(auditRead = true)` em `Login`
- [x] 7.5 Remover campo `features` de `Profile`

## 8. Reestruturar PartnersConfiguration

- [x] 8.1 Alterar PK de `BIGINT @GeneratedValue` para `String @Id` sem `@GeneratedValue`, coluna `CONFIGURATION_ID`
- [x] 8.2 Remover campos `key` (`CONFIGURATION_KEY`), `description` (`DESCRIPTION`) e `defaultValue` (`DEFAULT_VALUE`)
- [x] 8.3 Adicionar campo `value` (String, coluna `VALUE`)
- [x] 8.4 Adicionar campo `type` (String, coluna `TYPE`)
- [x] 8.5 Remover método `getValueOrDefaultValue()`

## 9. Criar entidades System e Resource

- [x] 9.1 Criar pacote `model/system`
- [x] 9.2 Criar entidade `System` (`@Auditable`, estende `BaseEntity`, PK UUID, campos: `code`, `description`, `secretKey`, `status`)
- [x] 9.3 Criar entidade `Resource` (`@Auditable`, estende `BaseEntity`, PK UUID, `@ManyToOne System`, campos: `code`, `description`, `active`)
- [x] 9.4 Criar pacote `repository/system`
- [x] 9.5 Criar `SystemRepository` (`BaseJpaRepository<System, UUID>` + `JpaSpecificationExecutor` + `QuerydslPredicateExecutor`)
- [x] 9.6 Criar `ResourceRepository` (`BaseJpaRepository<Resource, UUID>` + `JpaSpecificationExecutor` + `QuerydslPredicateExecutor`)

## 10. Criar entidade ProfileResource

- [x] 10.1 Criar pacote `model/permission`
- [x] 10.2 Criar `ProfileResourcePk` (`@Embeddable`, campos: `profileId` Long coluna `PROFILE_ID`, `resourceId` UUID coluna `RESOURCE_ID`)
- [x] 10.3 Criar entidade `ProfileResource` (`@Auditable`, SEM herança `BaseEntity`, `@EmbeddedId ProfileResourcePk`, `createdAt` `@CreationTimestamp`, `userAt`)
- [x] 10.4 Criar pacote `repository/permission`
- [x] 10.5 Criar `ProfileResourceRepository` (`BaseJpaRepository<ProfileResource, ProfileResourcePk>` + `JpaSpecificationExecutor` + `QuerydslPredicateExecutor`)

## 11. Criar entidades de Integração

- [x] 11.1 Criar pacote `model/integration`
- [x] 11.2 Criar entidade `IntegrationKeycloak` (`@Auditable(auditRead = true)`, estende `BaseEntity`, PK UUID, todos os campos incluindo `request` com `@Type(JsonBinaryType.class)`, `dateStart`/`dateEnd` como `OffsetDateTime`)
- [x] 11.3 Criar entidade `IntegrationKeycloakLog` (`@Auditable`, SEM herança `BaseEntity`, PK UUID, `@ManyToOne IntegrationKeycloak`, `success`, `response`, `createdAt` `@CreationTimestamp`, `userAt`)
- [x] 11.4 Criar entidade `IntegrationMessageInvalid` (`@Auditable`, SEM herança `BaseEntity`, PK UUID, `messageInvalid`, `createdAt` `@CreationTimestamp`, `userAt`)
- [x] 11.5 Criar pacote `repository/integration`
- [x] 11.6 Criar `IntegrationKeycloakRepository` (`BaseJpaRepository<IntegrationKeycloak, UUID>` + `JpaSpecificationExecutor` + `QuerydslPredicateExecutor`)
- [x] 11.7 Criar `IntegrationKeycloakLogRepository` (`BaseJpaRepository<IntegrationKeycloakLog, UUID>` + `JpaSpecificationExecutor` + `QuerydslPredicateExecutor`)
- [x] 11.8 Criar `IntegrationMessageInvalidRepository` (`BaseJpaRepository<IntegrationMessageInvalid, UUID>` + `JpaSpecificationExecutor` + `QuerydslPredicateExecutor`)
