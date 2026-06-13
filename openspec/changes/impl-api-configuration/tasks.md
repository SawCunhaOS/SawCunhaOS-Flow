## 1. Pré-requisitos

- [x] 1.1 Adicionar execution `ScosOrganization_Configuration` no `openapi-generator-maven-plugin` do `scos-organization-api/pom.xml`
- [x] 1.2 Adicionar `SCOS_CONFIGURATION_001` e `SCOS_CONFIGURATION_002` em `ExceptionCodeError` no domain
- [x] 1.3 Criar `PartnersConfigurationRepository extends BaseJpaRepository<PartnersConfiguration, String>` em `scos-organization-domain/repository/configuration/`

## 2. Application — Input Ports

- [x] 2.1 Criar interface `ListConfigurationsUseCase` em `port/in/configuration/` com método `execute(Pageable): Page<PartnersConfiguration>`
- [x] 2.2 Criar interface `GetConfigurationUseCase` em `port/in/configuration/` com método `execute(String id): PartnersConfiguration`
- [x] 2.3 Criar interface `UpdateConfigurationUseCase` em `port/in/configuration/` com método `execute(String id, String value): void`

## 3. Application — Use Cases

- [x] 3.1 Implementar `ListConfigurationsUseCaseImpl` — delega para `PartnersConfigurationRepository.findAll(Pageable)`; mascaramento por sufixo `_PASSWORD/_SECRET/_TOKEN/_KEY`
- [x] 3.2 Implementar `GetConfigurationUseCaseImpl` — `findById(id)` → lança `ScosException(SCOS_CONFIGURATION_001, 404)` se vazio; mascaramento se sensível
- [x] 3.3 Implementar `UpdateConfigurationUseCaseImpl` — `findById(id)` → 404 se não existe; valida `type`: STRING (sempre ok), INTEGER (`Long.parseLong`), BOOLEAN (`"true"/"false"` case-insensitive), JSON (`ObjectMapper.readTree`) → 422 `SCOS_CONFIGURATION_002` se inválido; persiste via `save`

## 4. API Layer — Delegate

- [x] 4.1 Regenerar sources: `mvn generate-sources -pl scos-organization-api` para gerar `ConfigurationApi`, `ConfigurationApiController`, `ConfigurationApiDelegate` e DTOs
- [x] 4.2 Criar `ConfigurationApiDelegateImpl` em `scos-organization-api` implementando `ConfigurationApiDelegate` gerado — mapeia DTOs ↔ use cases; injeta `ListConfigurationsUseCase`, `GetConfigurationUseCase`, `UpdateConfigurationUseCase`

## 5. Testes

- [x] 5.1 Unit tests dos 3 use cases via `/tdd-workflow` — application layer é Java puro sem Spring; mockar `PartnersConfigurationRepository`; cobrir cenários de 404, 422, mascaramento e tipos de validação
- [ ] 5.2 Teste de integração com Testcontainers via `/testcontainers-integration` — fluxo completo `PUT → GET → valida resposta`; verificar cache invalidado no PUT e populado no GET
