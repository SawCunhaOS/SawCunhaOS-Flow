## ADDED Requirements

### Requirement: CompanyContact armazena responsável pelo contato
`CompanyContact` SHALL incluir campo `responsiblePerson` mapeado para coluna `RESPONSIBLE_PERSON VARCHAR(255) NOT NULL` na tabela `SCOS_COMPANY_CONTACT`. Este campo identifica o nome da pessoa responsável pelo contato na empresa.

#### Scenario: Criação de contato com responsável
- **WHEN** `CompanyContact` é persistido com `responsiblePerson` preenchido
- **THEN** valor é armazenado na coluna `RESPONSIBLE_PERSON`

#### Scenario: Coluna RESPONSIBLE_PERSON existe no banco
- **WHEN** migration Liquibase `scos_company_contact.yml` é aplicada
- **THEN** tabela `SCOS_COMPANY_CONTACT` contém coluna `RESPONSIBLE_PERSON VARCHAR(255) NOT NULL`

#### Scenario: domain_model.md reflete campo RESPONSIBLE_PERSON
- **WHEN** `etc/database/domain_model.md` é consultado para tabela `SCOS_COMPANY_CONTACT`
- **THEN** campo `RESPONSIBLE_PERSON` aparece com tipo `VARCHAR(255)` e restrição `NOT NULL`
