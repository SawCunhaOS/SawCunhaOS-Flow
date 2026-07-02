## 1. Contrato OpenAPI — Company

- [x] 1.1 Adicionar `legalNatureId`, `cnaePrincipalId`, `stateRegistration`, `municipalRegistration` a `CompanyOutput`, `CreateCompanyRequest`, `UpdateCompanyRequest` em `ScosOrganization_Company.yml`
- [x] 1.2 Criar schemas `LegalNature`, `CreateLegalNatureRequest`, `UpdateLegalNatureRequest`, `GetLegalNatureResponse`, `GetAllLegalNaturesResponse`
- [x] 1.3 Criar paths `GET/POST /v1/legal-natures`, `GET/PUT/DELETE /v1/legal-natures/{id}` com `x-authorize`
- [x] 1.4 Criar schemas `Cnae`, `CreateCnaeRequest`, `UpdateCnaeRequest`, `GetCnaeResponse`, `GetAllCnaesResponse`
- [x] 1.5 Criar paths `GET/POST /v1/cnaes`, `GET/PUT/DELETE /v1/cnaes/{id}` com `x-authorize`
- [x] 1.6 Criar schema `CompanyCnaeSecondary` e `GetAllCompanyCnaeSecondaryResponse`
- [x] 1.7 Criar paths `GET/POST/DELETE /v1/companies/{companyId}/cnaes-secondary[/{cnaeId}]` com `x-authorize`
