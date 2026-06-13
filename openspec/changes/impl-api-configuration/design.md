## Context

A entidade `PartnersConfiguration` e a tabela `SCOS_CONFIGURATION` já existem com schema correto (PK String, campos `value` e `type`). O `ScosOrganization_Configuration.yml` está definido mas não está no `openapi-generator-maven-plugin`. O `scos-organization-application` está vazio. Não existe `PartnersConfigurationRepository`. O padrão de extensão do projeto é implementar `{Resource}ApiDelegate` gerado — o Controller e a interface `Api` são gerados e não devem ser tocados.

## Goals / Non-Goals

**Goals:**
- Habilitar os 3 endpoints da Configuration API via delegate pattern
- Criar `PartnersConfigurationRepository` no domain
- Mascarar valores sensíveis com base em convenção de nomenclatura de ID
- Validar compatibilidade de tipo ao atualizar configuração
- Adicionar `SCOS_CONFIGURATION_*` em `ExceptionCodeError`

**Non-Goals:**
- Criação de configurações via API (tabela populada manualmente ou via migration)
- Keycloak / autenticação (tratado pela Saga em change futura)
- Invalidação cross-service de cache (fora do escopo do scos-organization)

## Decisions

### D-01: OpenAPI Generator Delegate Pattern

O `pom.xml` gerará `ConfigurationApi` (interface com `@ScosRequestGET/PUT`, `@PreAuthorize`, `@JdempotentResource`), `ConfigurationApiController` (`@ScosController` que injeta o delegate) e `ConfigurationApiDelegate` (interface com default `throw MethodNotImplementedException()`). Nosso código implementa apenas `ConfigurationApiDelegateImpl` com `@Service`.

**Alternativa descartada**: Controller manual. Motivo: cache (`SCOS_ORGANIZATION_CONFIGURATION`), segurança (`@PreAuthorize`) e idempotência (`@JdempotentResource`) já estão declarados no YML — o generator os aplica automaticamente.

### D-02: PartnersConfigurationRepository com PK String

`PartnersConfigurationRepository extends BaseJpaRepository<PartnersConfiguration, String>`. PK é `String` (não `Long`) porque `CONFIGURATION_ID` é `VARCHAR(50)`. Métodos: `findAll(Pageable)`, `findById(String)`, `save(PartnersConfiguration)`.

**Alternativa descartada**: QueryDSL predicado. Não necessário — filtros de listagem são simples (sem critério de busca textual no UC-077).

### D-03: Mascaramento por Convenção de Sufixo

Configurações sensíveis são identificadas pelo sufixo do `id`: `_PASSWORD`, `_SECRET`, `_TOKEN`, `_KEY`. O mascaramento (`****`) acontece no use case, antes de montar o DTO de resposta.

**Alternativa descartada**: Coluna `SENSITIVE BOOLEAN` no banco. Exigiria Liquibase migration e complexidade adicional para um critério que pode ser inferido por nomenclatura.

### D-04: Validação de Tipo JSON via Parse

Para `type = JSON`, `UpdateConfigurationUseCaseImpl` chama `objectMapper.readTree(value)`. Se lançar `JsonProcessingException`, lança `ScosException(SCOS_CONFIGURATION_002, 422)`. Aceita qualquer JSON válido (objeto, array, primitivo).

**Alternativa descartada**: Restringir a JSON objeto `{}`. Menos flexível sem justificativa nos UCs.

### D-05: Exception Codes

Adicionar em `ExceptionCodeError`:
- `SCOS_CONFIGURATION_001` — configuração não encontrada (HTTP 404, UC-078 e UC-079)
- `SCOS_CONFIGURATION_002` — valor incompatível com o tipo declarado (HTTP 422, UC-079)

### D-06: Mapping Manual no Delegate

`ConfigurationApiDelegateImpl` mapeia DTOs gerados ↔ domain objects manualmente (sem MapStruct). Consistente com o padrão existente em `DepartmentApiDelegate`.

## Risks / Trade-offs

| Risco | Mitigação |
|-------|-----------|
| Nova chave sensível sem sufixo convencional passa sem mascaramento | Documentar convenção; `ConfigurationKey` enum pode ser populado como validação extra no futuro |
| `ConfigurationKey` enum vazio pode confundir desenvolvedores futuros | Manter vazio por ora; popular junto com uso concreto em Saga/futura change |
| Cache `SCOS_ORGANIZATION_CONFIGURATION` invalidado pelo PUT gerado — testar se `@ScosRequestPUT(nameCache=...)` faz invalidação ou população | Verificar comportamento via teste de integração (T-07); se invalidação não ocorrer, adicionar manualmente no use case |
