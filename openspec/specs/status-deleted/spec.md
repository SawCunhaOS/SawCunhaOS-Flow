## ADDED Requirements

### Requirement: StatusCompany inclui DELETED para soft delete permanente
`StatusCompany` SHALL incluir o valor `DELETED` representando exclusão lógica permanente. `DISABLED` permanece para desativação reversível. Entidade com status `DELETED` NÃO PODE transitar para `ACTIVE` ou `INACTIVE`.

#### Scenario: Transição de ACTIVE para DELETED
- **WHEN** `company.delete()` é chamado com status `ACTIVE`
- **THEN** status muda para `DELETED`

#### Scenario: Tentativa de ativar empresa DELETED
- **WHEN** `company.activate()` é chamado com status `DELETED`
- **THEN** lança `ScosException` com código `SCOS_COMPANY_007`

#### Scenario: Tentativa de inativar empresa DELETED
- **WHEN** `company.inactivate()` é chamado com status `DELETED`
- **THEN** lança `ScosException` com código `SCOS_COMPANY_007`

### Requirement: StatusEmployee inclui DELETED para soft delete permanente
`StatusEmployee` SHALL incluir o valor `DELETED` representando exclusão lógica permanente. Entidade com status `DELETED` NÃO PODE transitar para `ACTIVE` ou `INACTIVE`.

#### Scenario: Transição de ACTIVE para DELETED em Employee
- **WHEN** `employee.delete()` é chamado com status `ACTIVE`
- **THEN** status muda para `DELETED`

#### Scenario: Tentativa de ativar funcionário DELETED
- **WHEN** `employee.activate()` é chamado com status `DELETED`
- **THEN** lança `ScosException`

#### Scenario: DISABLED continua sendo desativação reversível
- **WHEN** empresa tem status `DISABLED`
- **THEN** `company.activate()` é bloqueado (mesmo comportamento do DELETED para activate/inactivate)
