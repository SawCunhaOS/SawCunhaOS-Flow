## ADDED Requirements

### Requirement: CRUD de referência AddressType
O sistema SHALL prover um catálogo de referência `AddressType` com os campos `code`, `description`, `entityType` (`COMPANY`/`EMPLOYEE`) e `active`, sem operação de exclusão física — o banco bloqueia `DELETE` via trigger.

#### Scenario: Criar tipo de endereço
- **WHEN** um cliente autorizado envia `POST /v1/address-types` com `code`, `description` e `entityType` válidos
- **THEN** o sistema cria o registro com `active=true` e retorna `201 Created`

#### Scenario: Listar tipos de endereço com paginação
- **WHEN** um cliente autorizado envia `GET /v1/address-types` com `paginationFilter`
- **THEN** o sistema retorna `200 OK` com `GetAllAddressTypesResponse` (`data: array`, `paginatedDTO`)

#### Scenario: Filtrar tipos de endereço por entityType
- **WHEN** um cliente autorizado envia `GET /v1/address-types?entityType=EMPLOYEE`
- **THEN** o sistema retorna só os registros com `entityType=EMPLOYEE`

#### Scenario: Buscar tipo de endereço por id
- **WHEN** um cliente autorizado envia `GET /v1/address-types/{id}` para um id existente
- **THEN** o sistema retorna `200 OK` com `GetAddressTypeResponse { data }`

#### Scenario: Atualizar tipo de endereço
- **WHEN** um cliente autorizado envia `PUT /v1/address-types/{id}` com `code`/`description`/`entityType` novos
- **THEN** o sistema atualiza o registro e retorna `204 No Content`

#### Scenario: Desativar tipo de endereço
- **WHEN** um cliente autorizado envia `PUT /v1/address-types/{id}/disable`
- **THEN** o sistema marca `active=false` e retorna `204 No Content`

#### Scenario: Reativar tipo de endereço
- **WHEN** um cliente autorizado envia `PUT /v1/address-types/{id}/enable`
- **THEN** o sistema marca `active=true` e retorna `204 No Content`

#### Scenario: DELETE não é oferecido
- **WHEN** o contrato OpenAPI de `AddressType` é inspecionado
- **THEN** não existe operação `DELETE` para `/v1/address-types/{id}`

### Requirement: entityType de AddressType é enum próprio de 2 valores
`AddressType.entityType` SHALL ser um enum OpenAPI com exatamente 2 valores (`COMPANY`, `EMPLOYEE`), distinto do enum de 3 valores (`COMPANY`/`EMPLOYEE`/`LOGIN`) usado pelos catálogos `Reason*`, refletindo o `CHECK` real do banco `chk_address_type_entity_type`.

#### Scenario: LOGIN é rejeitado pelo contrato
- **WHEN** um cliente envia `POST /v1/address-types` com `entityType=LOGIN`
- **THEN** o sistema rejeita a requisição com `4XX` de validação de schema, antes de qualquer chamada ao banco

### Requirement: Autorização do catálogo AddressType
Cada operação de `AddressType` SHALL exigir a permissão correspondente em `ScosOrganizationPermission`: `GET_ADDRESS_TYPE`, `CREATE_ADDRESS_TYPE`, `UPDATE_ADDRESS_TYPE`, `ENABLE_ADDRESS_TYPE`, `DISABLE_ADDRESS_TYPE`.

#### Scenario: Requisição sem permissão é rejeitada
- **WHEN** um cliente sem a permissão `CREATE_ADDRESS_TYPE` envia `POST /v1/address-types`
- **THEN** o sistema retorna `4XX` de autorização

### Requirement: CompanyAddress e EmployeeAddress usam addressTypeId
`CompanyAddress`, `EmployeeAddress` e seus schemas `Create*AddressRequest`/`Update*AddressRequest` SHALL expor `addressTypeId: integer (format: int64)`, obrigatório, referenciando `AddressType`, no lugar do campo `type: string` livre.

#### Scenario: Criar endereço de empresa com addressTypeId
- **WHEN** um cliente autorizado envia `POST /v1/companies/{companyId}/addresses` com `addressTypeId` referenciando um `AddressType` ativo com `entityType=COMPANY`
- **THEN** o sistema cria o registro associando o `addressTypeId` informado

#### Scenario: Criar endereço de funcionário com addressTypeId
- **WHEN** um cliente autorizado envia `POST /v1/employees/{employeeId}/addresses` com `addressTypeId` referenciando um `AddressType` ativo com `entityType=EMPLOYEE`
- **THEN** o sistema cria o registro associando o `addressTypeId` informado

#### Scenario: addressTypeId ausente é rejeitado
- **WHEN** um cliente envia `POST /v1/companies/{companyId}/addresses` sem `addressTypeId`
- **THEN** o sistema retorna `4XX` de validação de contrato

#### Scenario: Campo type não existe mais no contrato
- **WHEN** os schemas `CompanyAddress`/`EmployeeAddress`/`CreateCompanyAddressRequest`/`CreateEmployeeAddressRequest` são inspecionados
- **THEN** nenhum deles contém um campo `type: string`

### Requirement: EmployeeAddress.number usa int32
`EmployeeAddress.number` SHALL ser tipado como `integer (format: int32)`, alinhado à coluna `INT` do banco e ao mesmo campo em `CompanyAddress`.

#### Scenario: Schema number com formato correto
- **WHEN** o schema `EmployeeAddress` é inspecionado
- **THEN** o campo `number` tem `type: integer` e `format: int32`
