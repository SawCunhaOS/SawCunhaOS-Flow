## 1. Enums — executar primeiro para não quebrar compilação em cascata

- [x] 1.1 `StatusCompany`: remover valor `DELETED` (mantém `ACTIVE`/`INACTIVE`/`DISABLED`)
- [x] 1.2 `StatusEmployee`: remover valor `DELETED` (mantém `ACTIVE`/`INACTIVE`/`DISABLED`)
- [x] 1.3 `LoginStatus`: remover `DELETED`/`LOCKED`, adicionar `BLOCKED` (mantém `ACTIVE`/`INACTIVE`)
- [x] 1.4 Criar pacote `access/status/internal`; criar enum `EntityType` (`COMPANY`, `EMPLOYEE`, `LOGIN`)
- [x] 1.5 Criar enum `DayOfWeek` em `corporate/position/internal` (`MONDAY`..`SUNDAY`)
- [x] 1.6 Criar enum `EmployeeContractType` em `corporate/employee/internal` (`CLT`, `PJ`, `ESTAGIO`, `TEMPORARIO`)
- [x] 1.7 Criar pacote `outbox/internal`; criar enums `OutboxBackend` (`PGMQ`, `KAFKA`, `DIRECT_API`) e `OutboxEventStatus` (`PENDING`, `PROCESSING`, `PROCESSED`, `FAILED`)

## 2. Entidades de referência (sem dependência de outras entidades novas)

- [x] 2.1 Criar `ReasonActivate`, `ReasonInactivate`, `ReasonDisable`, `ReasonEnable` em `access/status/internal` (`BaseEntity`, `@Auditable`, `code`/`description`/`entityType`/`active`)
- [x] 2.2 Criar `ReasonActivateRepository`, `ReasonInactivateRepository`, `ReasonDisableRepository`, `ReasonEnableRepository` (`BaseJpaRepository<T, Long>` + `JpaSpecificationExecutor` + `QuerydslPredicateExecutor`)
- [x] 2.3 Criar `ReasonPositionChange` em `corporate/employee/internal` (`BaseEntity`, `@Auditable`, `code`/`description`/`active`, sem `entityType`) + `ReasonPositionChangeRepository`
- [x] 2.4 Criar pacote `corporate/catalog/internal`; criar `AddressType`, `ContactType` (`BaseEntity`, `@Auditable`, `code`/`description`/`entityType` reaproveitando `EntityType`/`active`) + `AddressTypeRepository`/`ContactTypeRepository`
- [x] 2.5 Criar `LegalNature`, `Cnae` em `corporate/company/internal` (mapeamento próprio: `@Id @GeneratedValue`, `code`, `description`, `createdAt` `@CreationTimestamp`, sem `userAt`, sem `BaseEntity`) — sem repositório dedicado se não houver necessidade de busca fora do `@ManyToOne` (avaliar durante implementação; se precisar paginação/filtro, criar `LegalNatureRepository`/`CnaeRepository` no mesmo padrão dos demais)
- [x] 2.6 Criar `OutboxTopic` em `outbox/internal` (`BaseEntity`, `@Auditable`, PK natural `String topic` sem `@GeneratedValue`, `backend`, `targetSystem` nullable, `defaultMaxRetries` default 3, `active`) + `OutboxTopicRepository`

## 3. Redesenhar Company

- [x] 3.1 Adicionar campos `legalNature` (`@ManyToOne` nullable), `cnaePrincipal` (`@ManyToOne` nullable), `stateRegistration`, `municipalRegistration`
- [x] 3.2 Remover método `delete()`
- [x] 3.3 Redesenhar `activate(reasonActivateId)`: guard `status == INACTIVE`, senão lança `ScosException` (`SCOS_COMPANY_007`); retorna `CompanyStatusHistory` não persistido (`status = ACTIVE`, `reasonActivateId` preenchido, `previousStatus` não setado)
- [x] 3.4 Redesenhar `inactivate(reasonInactivateId)`: guard `status != INACTIVE`, senão lança; retorna `CompanyStatusHistory` (`status = INACTIVE`, `reasonInactivateId` preenchido)
- [x] 3.5 Redesenhar `disable(reasonDisableId)`: guard `status == ACTIVE`, senão lança; retorna `CompanyStatusHistory` (`status = DISABLED`, `reasonDisableId` preenchido)
- [x] 3.6 Criar método `enable(reasonEnableId)`: guard `status == DISABLED`, senão lança; retorna `CompanyStatusHistory` (`status = ACTIVE`, `reasonEnableId` preenchido)
- [x] 3.7 Confirmar `isActive()` inalterado (já opera só sobre `status`)

## 4. Redesenhar Employee

- [x] 4.1 Adicionar campos `contractType` (`EmployeeContractType`), `probationEndDate` (nullable)
- [x] 4.2 Remover método `delete()`
- [x] 4.3 Redesenhar `activate(reasonActivateId)`: guard `status == INACTIVE`, senão lança `ScosException` (`SCOS_EMPLOYEE_001`); retorna `EmployeeStatusHistory` não persistido
- [x] 4.4 Redesenhar `inactivate(reasonInactivateId)`: guard `status != INACTIVE`, senão lança; retorna `EmployeeStatusHistory` (`status = INACTIVE`)
- [x] 4.5 Redesenhar `disable(reasonDisableId)`: guard `status == ACTIVE`, senão lança; retorna `EmployeeStatusHistory` (`status = DISABLED`)
- [x] 4.6 Criar método `enable(reasonEnableId)`: guard `status == DISABLED`, senão lança; retorna `EmployeeStatusHistory` (`status = ACTIVE`)
- [x] 4.7 Corrigir `EmployeeAddress.number` de `long` para `int`
- [x] 4.8 `Position`: adicionar campo `isTrustPosition` (`IS_TRUST_POSITION`, `boolean`, default `false`)

## 5. Redesenhar Login

- [x] 5.1 Renomear `Login.keycloakId` (`UUID`) → `externalId`, coluna `EXTERNAL_ID`
- [x] 5.2 Renomear `VwAuthorityResponse.keycloakId` → `externalId`, coluna `external_id`
- [x] 5.3 Renomear `AuthorityResponseOutput.keycloakId` → `externalId`; confirmar `AuthorityResponseMapper` continua mapeando sem configuração extra (nomes batem)
- [x] 5.4 Criar métodos `activate(reasonActivateId)`, `inactivate(reasonInactivateId)`, `disable(reasonDisableId)`, `enable(reasonEnableId)` em `Login`, mesma lógica de guard de `Company`/`Employee` mas com `BLOCKED` no lugar de `DISABLED`; retornam `LoginStatusHistory` não persistido
- [x] 5.5 Remover `LoginDeletedRule`
- [x] 5.6 Renomear `LoginLockedRule` → `LoginBlockedRule`; trocar checagem para `LoginStatus.BLOCKED` (mantém código `SCOS_LOGIN_011`)
- [x] 5.7 Renumerar `@ScosRule`: `LoginInactiveRule` → `1`, `LoginBlockedRule` → `2`
- [x] 5.8 Remover `SCOS_LOGIN_012` de `ExceptionCodeError`
- [x] 5.9 Remover chave `SCOS_LOGIN_012` de `scos_message_organization.properties` e `scos_message_organization_en.properties`
- [x] 5.10 **Desvio do design**: guard de transição de `Login` (`activate/inactivate/disable/enable`) não deve reusar `SCOS_LOGIN_010`/`011` (mensagens de rejeição de autenticação, semântica diferente) — criado código dedicado `SCOS_LOGIN_013` (mensagem em ambos os bundles), análogo a `SCOS_COMPANY_007`/`SCOS_EMPLOYEE_001`. `SCOS_COMPANY_007` também tinha mensagem desatualizada ("empresa deletada" — conceito removido) e `SCOS_EMPLOYEE_001` não tinha mensagem alguma (gap pré-existente); ambas corrigidas para texto genérico de transição inválida nos dois bundles

## 6. Entidades de histórico e dependentes

- [x] 6.1 Criar `CompanyStatusHistory`, `EmployeeStatusHistory`, `LoginStatusHistory` em `access/status/internal` (imutáveis — `createdAt`/`userAt` diretos, sem `BaseEntity`; FK dona, `status`, `previousStatus` nullable, 4 FKs de motivo nullable)
- [x] 6.2 Criar `CompanyStatusHistoryRepository`, `EmployeeStatusHistoryRepository`, `LoginStatusHistoryRepository`
- [x] 6.3 Criar `EmployeePositionHistory` em `corporate/employee/internal` (imutável — FK `employee`, FK `position`, `startDate`, `endDate` nullable, FK `reasonPositionChange` `NOT NULL`) + `EmployeePositionHistoryRepository`
- [x] 6.4 Criar `PositionWorkSchedule` em `corporate/position/internal` (`BaseEntity`, `@Auditable`, FK `position`, `dayOfWeek`, `startTime`/`lunchStart`/`lunchEnd`/`endTime`) + `PositionWorkScheduleRepository`
- [x] 6.5 Criar `EmployeeWorkSchedule` em `corporate/employee/internal` (mesma estrutura de `PositionWorkSchedule`, FK `employee`) + `EmployeeWorkScheduleRepository`
- [x] 6.6 Criar pacote `access/profile/internal` (já existe) → criar `LoginProfile` (PK composta `loginId`+`profileId`, imutável, padrão `ProfileResource`) + `LoginProfileRepository`
- [x] 6.7 Criar `CompanyCnaeSecondary` em `corporate/company/internal` (PK composta `companyId`+`cnaeId`, imutável, padrão `ProfileResource`) + `CompanyCnaeSecondaryRepository`
- [x] 6.8 Criar `OutboxEvent` em `outbox/internal` (sem `BaseEntity` — `updatedAt`/`userAt` campos simples nullable, `createdAt` `@CreationTimestamp`; `payload`/`responseData` via `@Type(JsonBinaryType.class)`) + `OutboxEventRepository`
- [x] 6.9 Criar `OutboxEventLog`, `OutboxEventDeadLetter` em `outbox/internal` (imutáveis, `userAt` nullable) + `OutboxEventLogRepository`/`OutboxEventDeadLetterRepository`

## 7. Migrar catálogo de tipo em Address/Contact

- [x] 7.1 `CompanyAddress`: substituir `type: String` por `addressType: AddressType` (`@ManyToOne`, FK `ADDRESS_TYPE_ID`)
- [x] 7.2 `EmployeeAddress`: substituir `type: String` por `addressType: AddressType` (`@ManyToOne`, FK `ADDRESS_TYPE_ID`)
- [x] 7.3 `CompanyContact`: substituir `type: String` por `contactType: ContactType` (`@ManyToOne`, FK `CONTACT_TYPE_ID`)
- [x] 7.4 `EmployeeContact`: substituir `type: String` por `contactType: ContactType` (`@ManyToOne`, FK `CONTACT_TYPE_ID`)

## 8. Remover mecanismo de integração Keycloak dedicado

- [x] 8.1 Remover `IntegrationKeycloak`, `IntegrationKeycloakRepository`
- [x] 8.2 Remover `IntegrationKeycloakLog`, `IntegrationKeycloakLogRepository`
- [x] 8.3 Remover `IntegrationMessageInvalid`, `IntegrationMessageInvalidRepository`
- [x] 8.4 Remover pacote `access/integration` se ficar vazio

## 9. Validação final

- [x] 9.1 Compilar `scos-organization-domain` (`mvn -pl scos-organization-domain compile`) — zero erro
- [x] 9.2 Subir a aplicação (ou teste dedicado) com `spring.jpa.hibernate.ddl-auto: validate` contra o banco gerado pela v2 — zero erro de schema-validation
- [x] 9.3 Conferir que nenhuma referência a `StatusCompany.DELETED`/`StatusEmployee.DELETED`/`LoginStatus.DELETED`/`LoginStatus.LOCKED` restou no módulo (`grep`)
- [x] 9.4 Conferir que nenhuma referência a `keycloakId` restou fora dos contratos OpenAPI/gRPC (fora de escopo)
