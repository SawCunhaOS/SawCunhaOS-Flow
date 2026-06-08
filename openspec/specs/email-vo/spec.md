## ADDED Requirements

### Requirement: Campos de email em entidades JPA usam VO Email
Toda entidade JPA com campo de e-mail SHALL usar o VO `Email` de `scos-foundation` via `@Embedded` + `@AttributeOverride(name = "email", column = @Column(name = "EMAIL"))`. Mapeamento direto como `String` é PROIBIDO.

Entidades afetadas:
- `CompanyContact.email`
- `Employee.email`
- `IntegrationKeycloak.email`

#### Scenario: Email inválido rejeitado na criação de CompanyContact
- **WHEN** `CompanyContact` é criado com `new Email("invalido")`
- **THEN** lança `ScosException` com código `EMAIL_INVALID`

#### Scenario: Email válido aceito
- **WHEN** `CompanyContact` é criado com `new Email("contato@empresa.com")`
- **THEN** objeto criado com sucesso e email armazenado na coluna `EMAIL`

#### Scenario: Carregamento do banco não aciona validação
- **WHEN** Hibernate carrega entidade com email da tabela
- **THEN** VO criado via construtor protegido sem validação (carga de dados existentes não falha)

#### Scenario: Email inválido rejeitado em Employee
- **WHEN** `Employee` é criado com email mal formatado
- **THEN** lança `ScosException` com código `EMAIL_INVALID`

#### Scenario: Email inválido rejeitado em IntegrationKeycloak
- **WHEN** `IntegrationKeycloak` é criado com email mal formatado
- **THEN** lança `ScosException` com código `EMAIL_INVALID`
