## ADDED Requirements

### Requirement: Criação de empresa matriz ou filial

O sistema SHALL expor `POST /v1/companies` (permissão `CREATE_COMPANY`) para cadastrar empresa matriz (`parentCompanyId` nulo) ou filial. A validação de formato, presença e dígito verificador do CNPJ Alfa (`taxIdentifier`, `^[A-Z0-9]{12}[0-9]{2}$`) MUST ocorrer na camada de contrato, antes de qualquer acesso ao banco. As regras que dependem do banco ou de outros agregados (unicidade, FKs, compatibilidade de motivo, hierarquia) MUST ser avaliadas no domain service, nesta ordem: formato (`400`) → unicidade (`409`) → existência de FK (`404`) → regra de negócio (`422`). A criação SHALL persistir a empresa com status `ACTIVE` e inserir a primeira linha em `SCOS_COMPANY_STATUS_HISTORY` com `newStatus=ACTIVE` e o `reasonActivateId` informado, na mesma transação. `sectorOfActivity` é texto livre `VARCHAR(100)` obrigatório (sem lista fixa), distinto de `cnaePrincipalId` e `legalNatureId`.

#### Scenario: Criação de empresa matriz com dados válidos
- **WHEN** um cliente autorizado envia `POST /v1/companies` com campos obrigatórios válidos, CNPJ Alfa com DV correto e `parentCompanyId` nulo
- **THEN** o sistema responde `201` e persiste a empresa com status `ACTIVE`

#### Scenario: Criação registra histórico de status inicial
- **WHEN** uma empresa é criada com sucesso
- **THEN** existe uma linha em `SCOS_COMPANY_STATUS_HISTORY` com `newStatus=ACTIVE` e o `reasonActivateId` informado

#### Scenario: Criação de filial com empresa mãe ativa
- **WHEN** o cliente envia `POST /v1/companies` com `parentCompanyId` de uma empresa `ACTIVE` e profundidade resultante dentro de `COMPANY_HIERARCHY_MAX_DEPTH`
- **THEN** o sistema responde `201`

#### Scenario: taxIdentifier ausente
- **WHEN** o cliente envia `POST /v1/companies` sem `taxIdentifier`
- **THEN** o sistema responde `400` com código `SCOS_VALIDATION_003`

#### Scenario: taxIdentifier com dígito verificador inválido
- **WHEN** o cliente envia `POST /v1/companies` com `taxIdentifier` de DV inválido
- **THEN** o sistema responde `400` com código `SCOS_VALIDATION_010`

#### Scenario: taxIdentifier já cadastrado
- **WHEN** o cliente envia `POST /v1/companies` com um `taxIdentifier` já existente em qualquer status
- **THEN** o sistema responde `409` com código `SCOS_COMPANY_002`

#### Scenario: reasonActivateId inexistente
- **WHEN** o cliente envia `POST /v1/companies` com `reasonActivateId` que não existe
- **THEN** o sistema responde `404`

#### Scenario: reasonActivateId inativo
- **WHEN** o cliente envia `POST /v1/companies` com `reasonActivateId` que tem `ACTIVE=false`
- **THEN** o sistema responde `422`

#### Scenario: reasonActivateId incompatível com a entidade
- **WHEN** o cliente envia `POST /v1/companies` com `reasonActivateId` cujo `entityType` é `EMPLOYEE`
- **THEN** o sistema responde `422`

#### Scenario: empresa mãe inexistente
- **WHEN** o cliente envia `POST /v1/companies` com `parentCompanyId` que não existe
- **THEN** o sistema responde `404`

#### Scenario: empresa mãe não está ativa
- **WHEN** o cliente envia `POST /v1/companies` com `parentCompanyId` de uma empresa `INACTIVE` ou `DISABLED`
- **THEN** o sistema responde `422`

#### Scenario: profundidade de hierarquia excedida
- **WHEN** o cliente envia `POST /v1/companies` com `parentCompanyId` cuja profundidade resultante excede `COMPANY_HIERARCHY_MAX_DEPTH`
- **THEN** o sistema responde `422`

### Requirement: Atualização de dados cadastrais da empresa

O sistema SHALL expor `PUT /v1/companies/{id}` (permissão `UPDATE_COMPANY`) para atualizar os dados cadastrais de uma empresa existente. O `parentCompanyId` MUST NOT ser editável (ausente do contrato de atualização). A unicidade de `taxIdentifier` MUST excluir o próprio `{id}`. Não há inserção em `SCOS_COMPANY_STATUS_HISTORY` (não é transição de status). Resposta de sucesso SHALL ser `204`.

#### Scenario: Atualização com dados válidos
- **WHEN** um cliente autorizado envia `PUT /v1/companies/{id}` para uma empresa existente com campos obrigatórios válidos
- **THEN** o sistema responde `204` e persiste os novos dados cadastrais

#### Scenario: Atualização não altera hierarquia
- **WHEN** uma empresa com `parentCompanyId` definido é atualizada
- **THEN** o `parentCompanyId` permanece inalterado

#### Scenario: Campo obrigatório ausente ou vazio
- **WHEN** o cliente envia `PUT /v1/companies/{id}` com um campo obrigatório ausente ou vazio
- **THEN** o sistema responde `400` com código `SCOS_VALIDATION_003` ou `SCOS_VALIDATION_001`

#### Scenario: taxIdentifier duplicado em outra empresa
- **WHEN** o cliente envia `PUT /v1/companies/{id}` com um `taxIdentifier` que já pertence a outra empresa
- **THEN** o sistema responde `409` com código `SCOS_COMPANY_002`

#### Scenario: empresa a atualizar não existe
- **WHEN** o cliente envia `PUT /v1/companies/{id}` para um `{id}` inexistente
- **THEN** o sistema responde `404` com código `SCOS_COMPANY_001`

### Requirement: Consulta de empresa por id

O sistema SHALL expor `GET /v1/companies/{id}` (permissão `GET_COMPANY`) retornando o objeto `Company` completo (incluindo `parentCompany`, status e dados fiscais) sob a chave `data`.

#### Scenario: Empresa encontrada
- **WHEN** um cliente autorizado envia `GET /v1/companies/{id}` para uma empresa existente
- **THEN** o sistema responde `200` com o `Company` completo sob `data`

#### Scenario: Empresa não encontrada
- **WHEN** o cliente envia `GET /v1/companies/{id}` para um `{id}` inexistente
- **THEN** o sistema responde `404` com código `SCOS_COMPANY_001`

### Requirement: Listagem paginada de empresas

O sistema SHALL expor `GET /v1/companies` (permissão `GET_COMPANY`) retornando uma lista paginada de empresas no formato-resumo `Companies` (id, name, nameTreatment, sectorOfActivity, status) sob `data`, acompanhada de `paginatedDTO`. A listagem SHALL aceitar filtros opcionais por status e/ou nome.

#### Scenario: Listagem com paginação padrão
- **WHEN** um cliente autorizado envia `GET /v1/companies` sem filtros
- **THEN** o sistema responde `200` com o array-resumo `Companies` sob `data` e o objeto `paginatedDTO`

#### Scenario: Filtro por status e/ou nome
- **WHEN** o cliente envia `GET /v1/companies` com filtro de status e/ou nome
- **THEN** o sistema retorna apenas os registros correspondentes ao filtro

#### Scenario: Token ausente ou expirado
- **WHEN** o cliente envia `GET /v1/companies` sem token válido
- **THEN** o sistema responde `401`

### Requirement: Idempotência da criação de empresa

A criação (`POST /v1/companies`) SHALL ser idempotente por meio de `@JdempotentRequestPayload` sobre o `taxIdentifier` (Redis). Uma requisição repetida com a mesma chave de idempotência MUST retornar a resposta original sem reprocessar o efeito. A atualização (`PUT`) não usa este mecanismo.

#### Scenario: Requisição de criação repetida
- **WHEN** duas requisições `POST /v1/companies` idênticas chegam com a mesma chave de idempotência (`taxIdentifier`)
- **THEN** a segunda retorna a resposta da primeira sem criar uma empresa duplicada nem inserir histórico adicional
