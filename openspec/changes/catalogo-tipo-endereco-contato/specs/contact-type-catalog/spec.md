## ADDED Requirements

### Requirement: CRUD de referência ContactType
O sistema SHALL prover um catálogo de referência `ContactType` com os campos `code`, `description`, `entityType` (`COMPANY`/`EMPLOYEE`) e `active`, sem operação de exclusão física — o banco bloqueia `DELETE` via trigger.

#### Scenario: Criar tipo de contato
- **WHEN** um cliente autorizado envia `POST /v1/contact-types` com `code`, `description` e `entityType` válidos
- **THEN** o sistema cria o registro com `active=true` e retorna `201 Created`

#### Scenario: Listar tipos de contato com paginação
- **WHEN** um cliente autorizado envia `GET /v1/contact-types` com `paginationFilter`
- **THEN** o sistema retorna `200 OK` com `GetAllContactTypesResponse` (`data: array`, `paginatedDTO`)

#### Scenario: Filtrar tipos de contato por entityType
- **WHEN** um cliente autorizado envia `GET /v1/contact-types?entityType=COMPANY`
- **THEN** o sistema retorna só os registros com `entityType=COMPANY`

#### Scenario: Buscar tipo de contato por id
- **WHEN** um cliente autorizado envia `GET /v1/contact-types/{id}` para um id existente
- **THEN** o sistema retorna `200 OK` com `GetContactTypeResponse { data }`

#### Scenario: Atualizar tipo de contato
- **WHEN** um cliente autorizado envia `PUT /v1/contact-types/{id}` com `code`/`description`/`entityType` novos
- **THEN** o sistema atualiza o registro e retorna `204 No Content`

#### Scenario: Desativar tipo de contato
- **WHEN** um cliente autorizado envia `PUT /v1/contact-types/{id}/disable`
- **THEN** o sistema marca `active=false` e retorna `204 No Content`

#### Scenario: Reativar tipo de contato
- **WHEN** um cliente autorizado envia `PUT /v1/contact-types/{id}/enable`
- **THEN** o sistema marca `active=true` e retorna `204 No Content`

#### Scenario: DELETE não é oferecido
- **WHEN** o contrato OpenAPI de `ContactType` é inspecionado
- **THEN** não existe operação `DELETE` para `/v1/contact-types/{id}`

### Requirement: entityType de ContactType é enum próprio de 2 valores
`ContactType.entityType` SHALL ser um enum OpenAPI com exatamente 2 valores (`COMPANY`, `EMPLOYEE`), distinto do enum de 3 valores (`COMPANY`/`EMPLOYEE`/`LOGIN`) usado pelos catálogos `Reason*`, refletindo o `CHECK` real do banco `chk_contact_type_entity_type`.

#### Scenario: LOGIN é rejeitado pelo contrato
- **WHEN** um cliente envia `POST /v1/contact-types` com `entityType=LOGIN`
- **THEN** o sistema rejeita a requisição com `4XX` de validação de schema, antes de qualquer chamada ao banco

### Requirement: Autorização do catálogo ContactType
Cada operação de `ContactType` SHALL exigir a permissão correspondente em `ScosOrganizationPermission`: `GET_CONTACT_TYPE`, `CREATE_CONTACT_TYPE`, `UPDATE_CONTACT_TYPE`, `ENABLE_CONTACT_TYPE`, `DISABLE_CONTACT_TYPE`.

#### Scenario: Requisição sem permissão é rejeitada
- **WHEN** um cliente sem a permissão `CREATE_CONTACT_TYPE` envia `POST /v1/contact-types`
- **THEN** o sistema retorna `4XX` de autorização

### Requirement: CompanyContact e EmployeeContact usam contactTypeId
`CompanyContact`, `EmployeeContact` e seus schemas `Create*ContactRequest`/`Update*ContactRequest` SHALL expor `contactTypeId: integer (format: int64)`, obrigatório, referenciando `ContactType`, no lugar do campo `type: string` livre.

#### Scenario: Criar contato de empresa com contactTypeId
- **WHEN** um cliente autorizado envia `POST /v1/companies/{companyId}/contacts` com `contactTypeId` referenciando um `ContactType` ativo com `entityType=COMPANY`
- **THEN** o sistema cria o registro associando o `contactTypeId` informado

#### Scenario: Criar contato de funcionário com contactTypeId
- **WHEN** um cliente autorizado envia `POST /v1/employees/{employeeId}/contacts` com `contactTypeId` referenciando um `ContactType` ativo com `entityType=EMPLOYEE`
- **THEN** o sistema cria o registro associando o `contactTypeId` informado

#### Scenario: contactTypeId ausente é rejeitado
- **WHEN** um cliente envia `POST /v1/companies/{companyId}/contacts` sem `contactTypeId`
- **THEN** o sistema retorna `4XX` de validação de contrato

#### Scenario: Campo type não existe mais no contrato
- **WHEN** os schemas `CompanyContact`/`EmployeeContact`/`CreateCompanyContactRequest`/`CreateEmployeeContactRequest` são inspecionados
- **THEN** nenhum deles contém um campo `type: string`
