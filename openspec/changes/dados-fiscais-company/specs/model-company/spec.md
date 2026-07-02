## ADDED Requirements

### Requirement: Company expõe dados fiscais no contrato público
`CompanyOutput`, `CreateCompanyRequest` e `UpdateCompanyRequest` SHALL expor os campos `legalNatureId` (nullable), `cnaePrincipalId` (nullable), `stateRegistration` (nullable) e `municipalRegistration` (nullable), correspondentes às colunas `LEGAL_NATURE_ID`, `CNAE_PRINCIPAL_ID`, `STATE_REGISTRATION` e `MUNICIPAL_REGISTRATION` de `SCOS_COMPANY`.

#### Scenario: Criar empresa sem dados fiscais
- **WHEN** um cliente autorizado envia `POST /v1/companies` sem `legalNatureId`, `cnaePrincipalId`, `stateRegistration` nem `municipalRegistration`
- **THEN** o sistema cria a empresa normalmente com esses campos nulos

#### Scenario: Criar empresa com dados fiscais completos
- **WHEN** um cliente autorizado envia `POST /v1/companies` com `legalNatureId` e `cnaePrincipalId` referenciando registros existentes, e `stateRegistration`/`municipalRegistration` preenchidos
- **THEN** o sistema cria a empresa persistindo os 4 campos

#### Scenario: stateRegistration aceita ISENTO
- **WHEN** um cliente autorizado envia `POST /v1/companies` ou `PUT /v1/companies/{id}` com `stateRegistration` igual a `"ISENTO"`
- **THEN** o sistema aceita o valor e persiste normalmente

#### Scenario: legalNatureId ou cnaePrincipalId inexistente é rejeitado
- **WHEN** um cliente autorizado envia `POST /v1/companies` ou `PUT /v1/companies/{id}` com `legalNatureId` ou `cnaePrincipalId` que não existem no catálogo correspondente
- **THEN** o sistema retorna `4XX`

#### Scenario: GetCompanyResponse retorna dados fiscais
- **WHEN** um cliente autorizado envia `GET /v1/companies/{id}` para uma empresa com dados fiscais preenchidos
- **THEN** a resposta `CompanyOutput` inclui `legalNatureId`, `cnaePrincipalId`, `stateRegistration` e `municipalRegistration`

### Requirement: Company gerencia CNAEs secundários via sub-recurso
O sistema SHALL prover um sub-recurso `CompanyCnaeSecondary` sob `Company` para associar/desassociar CNAEs secundários, distinto do `cnaePrincipalId` único.

#### Scenario: Listar CNAEs secundários de uma empresa
- **WHEN** um cliente autorizado envia `GET /v1/companies/{companyId}/cnaes-secondary` com `paginationFilter`
- **THEN** o sistema retorna `200 OK` com a lista paginada de CNAEs secundários associados

#### Scenario: Adicionar CNAE secundário
- **WHEN** um cliente autorizado envia `POST /v1/companies/{companyId}/cnaes-secondary/{cnaeId}` para um `cnaeId` existente ainda não associado
- **THEN** o sistema cria a associação e retorna `201 Created`

#### Scenario: Remover CNAE secundário
- **WHEN** um cliente autorizado envia `DELETE /v1/companies/{companyId}/cnaes-secondary/{cnaeId}` para uma associação existente
- **THEN** o sistema remove a associação e retorna `204 No Content`
