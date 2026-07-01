---
name: scos-jdempotent-config
description: >
  Configurar o módulo scos-foundation-jdempotent num sistema consumidor SCOS — idempotência de requisições
  via Redis (scos.jdempotent.*) e anotações @JdempotentResource/@JdempotentId/@JdempotentRequestPayload.
  Use ao proteger endpoints contra reprocessamento duplicado ou ao configurar o Redis da idempotência.
---

# Configuração — `scos-foundation-jdempotent`

Idempotência de requisições: a mesma chave não reprocessa: a resposta anterior é devolvida. Estado em Redis.

## 1. Dependência

```xml
<dependency>
  <groupId>br.com.sawcunhaos</groupId>
  <artifactId>scos-foundation-jdempotent</artifactId>
</dependency>
```

Auto-configura por `AutoConfiguration.imports`.

## 2. Ativação + Redis

Ligado por `scos.jdempotent.enabled` (`matchIfMissing=true`). Exige Redis.

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
scos:
  jdempotent:
    enabled: true
    cache:
      redis:
        expirationTimeHour: 24
        dialTimeoutSecond: 5
        readTimeoutSecond: 5
        writeTimeoutSecond: 5
        maxRetryCount: 3
        persistReqRes: true     # guarda payload de request/response
```

## 3. Uso

No projeto `scos-organization`, a idempotência de um endpoint é declarada no OpenAPI via
extensões `x-jdempotentresource` + `x-jdempotentrequestpayload`, que o `openapi-generator`
traduz para as anotações correspondentes no código gerado. Para adicionar idempotência a
um endpoint novo:

**No YAML (`etc/api/organization/*.yml`):**
```yaml
post:
  operationId: createCompany
  requestBody:
    content:
      application/json:
        schema:
          $ref: '#/components/schemas/CreateCompanyRequest'
    x-jdempotentrequestpayload: true
  x-jdempotentresource:
    cachePrefix: SCOS_ORGANIZATION_IDP_COMPANY
    ttl: 1
  responses:
    '201':
      $ref: './ScosComponents.yml#/components/responses/201_CREATED'
```

**No delegate (código gerado recebe as anotações; não adicionar manualmente):**
```java
@Component
@RequiredArgsConstructor
public class CompanyDelegate implements CompanyApiDelegate {

    private final CreateCompanyUseCase createCompanyUseCase;

    @Override
    public ResponseEntity<CreateResponse> createCompany(CreateCompanyRequest request,
            Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        return createCompanyUseCase.execute(request);
    }
}
```

**Anotações disponíveis** (para uso fora do fluxo OpenAPI-gerado):
- `@JdempotentResource` — marca o método idempotente.
- `@JdempotentId` — fonte da chave de idempotência (header/param).
- `@JdempotentRequestPayload` — payload considerado na chave.
- `@JdempotentProperty` / `@JdempotentIgnore` — incluir/excluir campos do hash da chave.

## Pegadinhas

- Sem Redis acessível a config falha no startup.
- `scos.jdempotent.enabled=false` desliga o módulo (default ligado por `matchIfMissing`).
- A chave de idempotência deve ser estável por requisição lógica — header `Idempotency-Key` é o padrão.
