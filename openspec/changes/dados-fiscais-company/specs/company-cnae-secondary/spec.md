## ADDED Requirements

### Requirement: Associação N:N entre Company e Cnae secundário
O sistema SHALL prover `CompanyCnaeSecondary` como associação pura N:N (PK composta `companyId`+`cnaeId`) entre `Company` e `Cnae`, sem operação de atualização — apenas criação e remoção do par.

#### Scenario: Adicionar CNAE secundário existente
- **WHEN** um cliente autorizado envia `POST /v1/companies/{companyId}/cnaes-secondary/{cnaeId}` para `companyId` e `cnaeId` existentes, ainda não associados
- **THEN** o sistema cria a associação e retorna `201 Created`

#### Scenario: Adicionar CNAE secundário já associado
- **WHEN** um cliente autorizado envia `POST /v1/companies/{companyId}/cnaes-secondary/{cnaeId}` para um par já existente
- **THEN** o sistema rejeita a duplicidade e retorna `4XX`

#### Scenario: Adicionar CNAE secundário com companyId ou cnaeId inexistente
- **WHEN** um cliente autorizado envia `POST /v1/companies/{companyId}/cnaes-secondary/{cnaeId}` referenciando `companyId` ou `cnaeId` inexistente
- **THEN** o sistema retorna `4XX`

#### Scenario: Remover CNAE secundário
- **WHEN** um cliente autorizado envia `DELETE /v1/companies/{companyId}/cnaes-secondary/{cnaeId}` para uma associação existente
- **THEN** o sistema remove a associação e retorna `204 No Content`

#### Scenario: Listar CNAEs secundários com paginação
- **WHEN** um cliente autorizado envia `GET /v1/companies/{companyId}/cnaes-secondary` com `paginationFilter`
- **THEN** o sistema retorna `200 OK` com a lista paginada de CNAEs secundários da empresa

### Requirement: Autorização de CompanyCnaeSecondary
Cada operação SHALL exigir a permissão correspondente em `ScosOrganizationPermission`: `GET_COMPANY_CNAE_SECONDARY`, `CREATE_COMPANY_CNAE_SECONDARY`, `DELETE_COMPANY_CNAE_SECONDARY`. Não existe permissão de `UPDATE` — a associação não suporta essa operação.

#### Scenario: Requisição sem permissão é rejeitada
- **WHEN** um cliente sem a permissão `CREATE_COMPANY_CNAE_SECONDARY` envia `POST /v1/companies/{companyId}/cnaes-secondary/{cnaeId}`
- **THEN** o sistema retorna `4XX` de autorização
