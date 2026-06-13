## Why

A `scos-organization-application` está vazia e `ScosOrganization_Configuration.yml` ainda não está no `openapi-generator-maven-plugin`. Nenhum endpoint da Configuration API funciona, apesar do domain (`PartnersConfiguration`) e da spec OpenAPI já existirem. Implementar esta API primeiro valida a stack completa (REST → Delegate → UseCase → Domain → Banco) antes das APIs com dependências mais complexas.

## What Changes

- Adicionar `ScosOrganization_Configuration.yml` ao `openapi-generator-maven-plugin` no `scos-organization-api/pom.xml` — gera `ConfigurationApi`, `ConfigurationApiController`, `ConfigurationApiDelegate` e DTOs
- Criar `PartnersConfigurationRepository` no `scos-organization-domain` (Spring Data + QueryDSL)
- Criar 3 interfaces de input port em `scos-organization-application/port/in/configuration/`: `ListConfigurationsUseCase`, `GetConfigurationUseCase`, `UpdateConfigurationUseCase`
- Implementar os 3 use cases em `scos-organization-application/usecase/configuration/`
- Criar `ConfigurationApiDelegateImpl` em `scos-organization-api` (implementa `ConfigurationApiDelegate` gerado)
- Mascaramento de valores sensíveis: IDs com sufixo `_PASSWORD`, `_SECRET`, `_TOKEN`, `_KEY` têm `value` substituído por `****` nas respostas
- Validação de tipo JSON no UC-079: `ObjectMapper.readTree(value)` — se não lançar exceção, valor é aceito

## Capabilities

### New Capabilities

- `configuration-api`: Expõe os 3 endpoints da Configuration API (UC-077 lista paginada, UC-078 consulta por ID, UC-079 atualização de valor) com mascaramento de sensíveis e validação de tipo.

### Modified Capabilities

- `schema-configuration`: Sem mudança de requirement — tabela `SCOS_CONFIGURATION` já existe com schema correto.
- `model-configuration`: Sem mudança de requirement — `PartnersConfiguration` já está reestruturada conforme spec.

## Impact

- `scos-organization-api/pom.xml`: novo execution `ScosOrganization_Configuration` no generator
- `scos-organization-api/src/`: novo `ConfigurationApiDelegateImpl`
- `scos-organization-domain/src/`: novo `PartnersConfigurationRepository`
- `scos-organization-application/src/`: 3 interfaces port/in + 3 implementações usecase
- Sem impacto em Liquibase (tabela já existe)
- Sem impacto em outras APIs
