## ADDED Requirements

### Requirement: PartnersConfigurationRepository existe no domain
O sistema SHALL ter `PartnersConfigurationRepository extends BaseJpaRepository<PartnersConfiguration, String>` no módulo `scos-organization-domain`, com métodos `findAll(Pageable)`, `findById(String)` e `save(PartnersConfiguration)`.

#### Scenario: Repository encontra configuração existente
- **WHEN** `findById("TOKEN_EXPIRY_MINUTES")` é chamado e o registro existe
- **THEN** retorna `Optional<PartnersConfiguration>` com o registro

#### Scenario: Repository retorna vazio para ID inexistente
- **WHEN** `findById("NAO_EXISTE")` é chamado
- **THEN** retorna `Optional.empty()`

### Requirement: GET /v1/configurations retorna lista paginada com mascaramento
O sistema SHALL retornar lista paginada de `PartnersConfiguration` na rota `GET /v1/configurations`, com valores de configurações sensíveis mascarados por `****`. Paginação é obrigatória.

#### Scenario: Listagem com paginação padrão
- **WHEN** `GET /v1/configurations` é chamado com `page=0&size=10`
- **THEN** retorna HTTP 200 com `data[]` e `paginatedDTO` preenchidos

#### Scenario: Mascaramento de valor sensível na listagem
- **WHEN** existe configuração com `id = "JWT_SECRET_KEY"` e `value = "super-secret"`
- **THEN** a resposta contém `value = "****"` para essa configuração

#### Scenario: Configuração não-sensível não é mascarada
- **WHEN** existe configuração com `id = "TOKEN_EXPIRY_MINUTES"` e `value = "30"`
- **THEN** a resposta contém `value = "30"` sem mascaramento

### Requirement: GET /v1/configurations/{id} retorna 404 se não existir
O sistema SHALL retornar HTTP 404 com `SCOS_CONFIGURATION_001` quando o `id` informado não existe em `SCOS_CONFIGURATION`. Quando existe, retorna a configuração com mascaramento se sensível.

#### Scenario: Configuração encontrada
- **WHEN** `GET /v1/configurations/TOKEN_EXPIRY_MINUTES` é chamado e o registro existe
- **THEN** retorna HTTP 200 com `data.id = "TOKEN_EXPIRY_MINUTES"`

#### Scenario: Configuração não encontrada
- **WHEN** `GET /v1/configurations/NAO_EXISTE` é chamado
- **THEN** retorna HTTP 404 com `codeError = "SCOS_CONFIGURATION_001"`

#### Scenario: Mascaramento por ID na consulta individual
- **WHEN** `GET /v1/configurations/DB_PASSWORD` é chamado e o registro existe
- **THEN** retorna HTTP 200 com `data.value = "****"`

### Requirement: PUT /v1/configurations/{id} valida tipo e retorna 404 se não existir
O sistema SHALL rejeitar com HTTP 422 (`SCOS_CONFIGURATION_002`) quando o `value` enviado é incompatível com o `type` declarado na configuração. SHALL retornar HTTP 404 (`SCOS_CONFIGURATION_001`) se o `id` não existir. SHALL persistir o novo valor e invalidar o cache `SCOS_ORGANIZATION_CONFIGURATION` quando bem-sucedido.

#### Scenario: Atualização bem-sucedida
- **WHEN** `PUT /v1/configurations/TOKEN_EXPIRY_MINUTES` é chamado com `value = "60"` e `type = INTEGER`
- **THEN** retorna HTTP 204 e o valor é persistido

#### Scenario: Valor incompatível com tipo INTEGER
- **WHEN** `PUT /v1/configurations/TOKEN_EXPIRY_MINUTES` é chamado com `value = "nao-e-numero"` e `type = INTEGER`
- **THEN** retorna HTTP 422 com `codeError = "SCOS_CONFIGURATION_002"`

#### Scenario: Valor incompatível com tipo BOOLEAN
- **WHEN** `PUT /v1/configurations/FEATURE_FLAG` é chamado com `value = "sim"` e `type = BOOLEAN`
- **THEN** retorna HTTP 422 com `codeError = "SCOS_CONFIGURATION_002"` (valor válido seria "true" ou "false")

#### Scenario: Valor JSON inválido
- **WHEN** `PUT /v1/configurations/COMPLEX_CONFIG` é chamado com `value = "{invalido"` e `type = JSON`
- **THEN** retorna HTTP 422 com `codeError = "SCOS_CONFIGURATION_002"`

#### Scenario: Configuração não encontrada na atualização
- **WHEN** `PUT /v1/configurations/NAO_EXISTE` é chamado
- **THEN** retorna HTTP 404 com `codeError = "SCOS_CONFIGURATION_001"`

### Requirement: Identificação de configuração sensível por sufixo de ID
O sistema SHALL considerar uma configuração sensível quando seu `id` terminar com `_PASSWORD`, `_SECRET`, `_TOKEN` ou `_KEY` (case-insensitive). O mascaramento substitui o `value` por `"****"` na resposta.

#### Scenario: ID com sufixo _PASSWORD é sensível
- **WHEN** `id = "DB_PASSWORD"` está presente em qualquer resposta
- **THEN** `value = "****"`

#### Scenario: ID com sufixo _KEY é sensível
- **WHEN** `id = "API_SIGNING_KEY"` está presente em qualquer resposta
- **THEN** `value = "****"`

#### Scenario: ID sem sufixo sensível não é mascarado
- **WHEN** `id = "MAX_RETRIES"` está presente em qualquer resposta
- **THEN** `value` contém o valor real armazenado

### Requirement: ExceptionCodeError contém códigos de Configuration
O enum `ExceptionCodeError` SHALL conter `SCOS_CONFIGURATION_001` (não encontrado, HTTP 404) e `SCOS_CONFIGURATION_002` (valor incompatível com tipo, HTTP 422).

#### Scenario: Código SCOS_CONFIGURATION_001 existe
- **WHEN** `ExceptionCodeError.SCOS_CONFIGURATION_001` é referenciado
- **THEN** compila sem erro e retorna code `"SCOS_CONFIGURATION_001"`

#### Scenario: Código SCOS_CONFIGURATION_002 existe
- **WHEN** `ExceptionCodeError.SCOS_CONFIGURATION_002` é referenciado
- **THEN** compila sem erro e retorna code `"SCOS_CONFIGURATION_002"`
