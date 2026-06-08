## ADDED Requirements

### Requirement: PartnersConfiguration reestruturada conforme schema do banco
A entidade `PartnersConfiguration` SHALL mapear exatamente as colunas da tabela `SCOS_CONFIGURATION`. O PK deve ser `VARCHAR(50)` não auto-gerado (mapeado como `String` com `@Id` sem `@GeneratedValue`). Os campos `key` (`CONFIGURATION_KEY`), `description` (`DESCRIPTION`) e `defaultValue` (`DEFAULT_VALUE`) não existem no banco e MUST ser removidos. Os campos `value` (`VALUE TEXT`) e `type` (`TYPE VARCHAR(50)`) devem ser adicionados.

#### Scenario: PK como String não auto-gerada
- **WHEN** a entidade `PartnersConfiguration` é inspecionada
- **THEN** deve ter campo `id` do tipo `String` com `@Id` sem `@GeneratedValue`, mapeando coluna `CONFIGURATION_ID`

#### Scenario: Campos obsoletos removidos
- **WHEN** a entidade `PartnersConfiguration` é inspecionada
- **THEN** os campos `key`, `description` e `defaultValue` não devem existir

#### Scenario: Campos value e type presentes
- **WHEN** a entidade `PartnersConfiguration` é inspecionada
- **THEN** devem existir campo `value` (coluna `VALUE`, tipo `String`) e campo `type` (coluna `TYPE`, tipo `String`)

### Requirement: PartnersConfiguration sem método getValueOrDefaultValue
O método `getValueOrDefaultValue()` depende do campo `defaultValue` removido e MUST ser removido da entidade.

#### Scenario: Método removido
- **WHEN** a entidade `PartnersConfiguration` é inspecionada
- **THEN** o método `getValueOrDefaultValue()` não deve existir
