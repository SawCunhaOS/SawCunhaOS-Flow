## MODIFIED Requirements

### Requirement: StatusCompany sem DELETED, sem método delete()
O enum `StatusCompany` SHALL conter exatamente `ACTIVE`, `INACTIVE` e `DISABLED` — o valor `DELETED` (reintroduzido por uma capability posterior para soft-delete permanente, agora retirada) não existe mais, pois viola o `CHECK` de vocabulário fechado do schema v2. O método `delete()` da entidade `Company` MUST ser removido — `INACTIVE` é o estado terminal definitivo, não há substituto.

#### Scenario: Enum sem DELETED
- **WHEN** o enum `StatusCompany` é inspecionado
- **THEN** não deve existir o valor `DELETED`

#### Scenario: Enum com DISABLED
- **WHEN** o enum `StatusCompany` é inspecionado
- **THEN** deve existir o valor `DISABLED`

#### Scenario: Método delete ausente
- **WHEN** a entidade `Company` é inspecionada
- **THEN** não deve existir método `delete()`

### Requirement: Company com métodos de negócio baseados em histórico de status
Os métodos `activate(reasonActivateId)`, `inactivate(reasonInactivateId)`, `disable(reasonDisableId)` e `enable(reasonEnableId)` da entidade `Company` SHALL validar a transição client-side e retornar uma instância não persistida de `CompanyStatusHistory` em vez de mutar `status` diretamente. `isActive()` continua operando apenas sobre `status`.

#### Scenario: isActive baseado apenas em status
- **WHEN** `isActive()` é chamado
- **THEN** retorna `true` somente se `status == StatusCompany.ACTIVE`

#### Scenario: activate retorna histórico sem mutar status
- **WHEN** `company.activate(reasonActivateId)` é chamado com `status == INACTIVE`
- **THEN** retorna `CompanyStatusHistory` com `status = ACTIVE` e `reasonActivateId` preenchido; `company.getStatus()` continua `INACTIVE` até o histórico ser persistido e a trigger sincronizar

#### Scenario: disable exige status ACTIVE
- **WHEN** `company.disable(reasonDisableId)` é chamado com `status != ACTIVE`
- **THEN** lança `ScosException`

## ADDED Requirements

### Requirement: Company com dados fiscais
A entidade `Company` SHALL incluir os campos `legalNature` (`@ManyToOne` para `LegalNature`, FK `LEGAL_NATURE_ID`, nullable), `cnaePrincipal` (`@ManyToOne` para `Cnae`, FK `CNAE_PRINCIPAL_ID`, nullable), `stateRegistration` (`STATE_REGISTRATION`, nullable) e `municipalRegistration` (`MUNICIPAL_REGISTRATION`, nullable).

#### Scenario: Campos fiscais presentes e nullable
- **WHEN** a entidade `Company` é inspecionada
- **THEN** deve conter `legalNature`, `cnaePrincipal`, `stateRegistration` e `municipalRegistration`, todos aceitando `null`
