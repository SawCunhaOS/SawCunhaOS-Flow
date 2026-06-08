## ADDED Requirements

### Requirement: Company mapeado conforme schema do banco
A entidade `Company` SHALL mapear exatamente as colunas da tabela `SCOS_COMPANY`. Os campos `dateCreated` e `active` não existem no banco e MUST ser removidos. O campo `STATUS` existe e já está mapeado via `StatusCompany`.

#### Scenario: Campos obsoletos removidos de Company
- **WHEN** a entidade `Company` é inspecionada
- **THEN** os campos `dateCreated` e `active` não devem existir na classe

### Requirement: StatusCompany com DISABLED em vez de DELETED
O enum `StatusCompany` SHALL conter `DISABLED` em vez de `DELETED`, alinhando com o schema do banco (`ACTIVE/INACTIVE/DISABLED`). O método `delete()` da entidade MUST ser renomeado para `disable()`.

#### Scenario: Enum sem DELETED
- **WHEN** o enum `StatusCompany` é inspecionado
- **THEN** não deve existir o valor `DELETED`

#### Scenario: Enum com DISABLED
- **WHEN** o enum `StatusCompany` é inspecionado
- **THEN** deve existir o valor `DISABLED`

#### Scenario: Método disable em Company
- **WHEN** a entidade `Company` é inspecionada
- **THEN** deve existir método `disable()` e não deve existir método `delete()`

### Requirement: Company com métodos de negócio sem referência a active
Os métodos `isActive()`, `inactivate()` e `activate()` da entidade `Company` MUST ser ajustados para operar apenas sobre `status`, sem referenciar o campo `active` removido.

#### Scenario: isActive baseado apenas em status
- **WHEN** `isActive()` é chamado
- **THEN** retorna `true` somente se `status == StatusCompany.ACTIVE`

### Requirement: CompanyAddress com PK composta
A entidade `CompanyAddress` SHALL usar PK composta via `@EmbeddedId CompanyAddressPk`, onde `companyIdAddress` (coluna `COMPANY_ID_ADDRESS`, ID externo sem auto-geração) e `companyId` (coluna `COMPANY_ID`, FK) formam a chave. Não deve existir PK simples auto-gerada.

#### Scenario: PK composta presente em CompanyAddress
- **WHEN** a entidade `CompanyAddress` é inspecionada
- **THEN** deve usar `@EmbeddedId` do tipo `CompanyAddressPk`, sem campo `@Id` com `@GeneratedValue`

#### Scenario: CompanyAddressPk com campos corretos
- **WHEN** a classe `CompanyAddressPk` é inspecionada
- **THEN** deve ter `companyIdAddress` (coluna `COMPANY_ID_ADDRESS`) e `companyId` (coluna `COMPANY_ID`)

### Requirement: CompanyAddressRepository atualizado para PK composta
O repositório `CompanyAddressRepository` SHALL usar `BaseJpaRepository<CompanyAddress, CompanyAddressPk>` e os métodos de busca e deleção MUST ser revisados para operar com a nova PK.

#### Scenario: Tipo genérico do repositório correto
- **WHEN** `CompanyAddressRepository` é inspecionado
- **THEN** deve estender `BaseJpaRepository<CompanyAddress, CompanyAddressPk>`

### Requirement: CompanyContact com PK correta
A entidade `CompanyContact` SHALL mapear a coluna PK como `COMPANY_ID_CONTACT` (não `CONTACT_ID`) e SHALL remover o campo `responsiblePerson` que não existe no banco.

#### Scenario: Nome da coluna PK correto em CompanyContact
- **WHEN** a entidade `CompanyContact` é inspecionada
- **THEN** a coluna PK deve ser nomeada `COMPANY_ID_CONTACT`

#### Scenario: Campo responsiblePerson removido
- **WHEN** a entidade `CompanyContact` é inspecionada
- **THEN** o campo `responsiblePerson` não deve existir

### Requirement: CompanyRepository sem referência a StatusCompany.DELETED
O repositório `CompanyRepository` SHALL substituir todas as ocorrências de `StatusCompany.DELETED` por `StatusCompany.DISABLED` nos predicados QueryDSL.

#### Scenario: Predicate usa DISABLED
- **WHEN** `CompanyRepository` é inspecionado
- **THEN** nenhuma ocorrência de `StatusCompany.DELETED` deve existir
