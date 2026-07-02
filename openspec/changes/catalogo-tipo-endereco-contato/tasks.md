## 1. Contrato OpenAPI — catálogo novo

- [x] 1.1 Criar `etc/api/organization/ScosOrganization_Catalog.yml` com schemas `AddressType`, `ContactType`, enum `CatalogEntityType` (2 valores: `COMPANY`/`EMPLOYEE`)
- [x] 1.2 Adicionar `CreateAddressTypeRequest`/`UpdateAddressTypeRequest`, `GetAddressTypeResponse`/`GetAllAddressTypesResponse` (mesmo pra `ContactType`)
- [x] 1.3 Adicionar paths `GET/POST /v1/address-types`, `GET/PUT /v1/address-types/{id}`, `PUT /v1/address-types/{id}/enable`, `PUT /v1/address-types/{id}/disable` (sem `DELETE`)
- [x] 1.4 Repetir 1.3 para `/v1/contact-types`
- [x] 1.5 Adicionar `x-authorize` em cada path novo: `GET/CREATE/UPDATE_ADDRESS_TYPE`, `ENABLE/DISABLE_ADDRESS_TYPE`, mesmo conjunto para `CONTACT_TYPE`

## 2. Contrato OpenAPI — Company

- [x] 2.1 Em `ScosOrganization_Company.yml`, remover `type: string` de `CompanyAddress`/`CreateCompanyAddressRequest`/`UpdateCompanyAddressRequest`, adicionar `addressTypeId: integer (format: int64)` obrigatório (`$ref` cross-file pro enum se necessário)
- [x] 2.2 Remover `type: string` de `CompanyContact`/`CreateCompanyContactRequest`/`UpdateCompanyContactRequest`, adicionar `contactTypeId: integer (format: int64)` obrigatório

## 3. Contrato OpenAPI — Employee

- [x] 3.1 Em `ScosOrganization_Employee.yml`, remover `type: string` de `EmployeeAddress`/`CreateEmployeeAddressRequest`/`UpdateEmployeeAddressRequest`, adicionar `addressTypeId: integer (format: int64)` obrigatório
- [x] 3.2 Corrigir `EmployeeAddress.number` para `integer (format: int32)`
- [x] 3.3 Remover `type: string` de `EmployeeContact`/`CreateEmployeeContactRequest`/`UpdateEmployeeContactRequest`, adicionar `contactTypeId: integer (format: int64)` obrigatório
