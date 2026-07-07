# Configuração por variável de ambiente — SCOS Organization

Referência das properties externalizadas dos módulos deployáveis (`scos-organization-boot`, `scos-organization-grpc-boot`). O arquivo `.env.example` na raiz lista todas as env vars com seus defaults prontas para copiar. Este documento explica **o que setar** e, principalmente, **o que NÃO mexer**.

Padrão: toda property usa `${VAR:default}`, com default = valor de hoje. **Rodar sem setar nada reproduz o comportamento atual** (regressão-zero). Onde o valor tem forma de URL, a env var carrega a string inteira (cobre scheme, porta, db, schema, realm; e listas de Sentinel).

## Categorias (resumo — detalhe e defaults no `.env.example`)

| Categoria | Env vars | Quando mexer |
|---|---|---|
| **Segredos** ⚠️ | `SCOS_DB_USERNAME`/`_PASSWORD`, `SCOS_AUDIT_DB_USERNAME`/`_PASSWORD`, `SCOS_REDIS_PASSWORD`, `SCOS_REGISTRY_KEY_ACCESS`, `SCOS_PRIVACY_CRYPTO_SECRET` | **Sempre em produção** — default versionado é só dev |
| **Topologia (URL)** | `SCOS_DB_URL`, `SCOS_AUDIT_DB_URL`, `SCOS_DB_SCHEMA`, `SCOS_KEYCLOAK_ISSUER_URI`, `SCOS_REDIS_SENTINELS`, `SCOS_REGISTRY_HOST` | Cada instalação/cliente |
| **Portas** | `SCOS_PORT`, `SCOS_REGISTRY_PORT`, `SCOS_REDIS_PORT` | Porta customizada/conflito |
| **Pool (principal + audit)** | `SCOS_DB_*`, `SCOS_AUDIT_DB_*` (timeout/idle/lifetime/min-idle/max-pool) | Ajuste por tamanho da instalação |
| **Toggles** | `SCOS_AUDIT_ENABLED`, `SCOS_CACHE_ENABLE`, `SCOS_LIQUIBASE_ENABLED`, `SCOS_PRIVACY_ENABLED`, `SCOS_PRIVACY_STRICT`, `SCOS_JDEMPOTENT_ENABLED`, `SCOS_REGISTRY_TLS_ENABLED` | Liga/desliga por instalação |
| **Tuning fino** | `SCOS_CACHE_TTL`/`_DATABASE`, `SCOS_JDEMPOTENT_*`, `SCOS_PRIVACY_MAX_PAYLOAD_KB` | Avançado — raramente |
| **Diagnóstico/observability** | `SCOS_JPA_SHOW_SQL`, `SCOS_JPA_GENERATE_STATISTICS`, `SCOS_JPA_SLOW_QUERY_MS`, `SCOS_TRACING_SAMPLING`, `SCOS_LOG_SHOW_REQUEST_*`/`_RESPONSE_*` | **Produção: desligar SQL/stats, reduzir sampling** |
| **Tuning extra (só boot)** | `SCOS_REDIS_TIMEOUT`, `SCOS_TOMCAT_MAX_CONNECTIONS`, `SCOS_TOMCAT_CONNECTION_TIMEOUT` | Infra pequena |
| **Log** | `LOG_DIR`, `LOG_LEVEL` | Container/on-premise: caminho persistente + nível |

## Build de imagem — NÃO são env var (são propriedade Maven, via `-D`)

| Propriedade | Default | Uso |
|---|---|---|
| `bp.cds.enabled` | `true` | `-Dbp.cds.enabled=false` = fallback do bug paketo #581 |
| `bp.jvm.thread.count` | `20` | dimensionar junto do `mem_limit` do container |
| `paketo.builder` | tag pinada | builder buildpacks; **nunca `latest`** |

## O que NÃO vira env var (e por quê)

| Property | Motivo |
|---|---|
| `scos.security.keycloak.client-id`/`grant-type`/`scope`/`resourceId` | Identidade/convenção do client OAuth2 |
| `scos.cache.key-prefix` (`api::` / `grpc::`), `scos.cache.master` (`scos_master`) | Convenção de produto / infra padrão SCOS |
| `scos.registry.system-code`/`system-name`/`system-description` | Identidade do sistema |
| `scos.privacy.masking.default-patterns`, `log.register-converter` | Regra de masking / wiring |
| JPA/Hibernate **estrutural**: `database-platform`, `naming.*`, `jdbc.batch_*`, `query.*`, `ddl-auto: validate` | Decisão de arquitetura do código, não de deploy |
| `management.endpoints.web.exposure.include`, métricas/tags | Estrutura de observability (candidato futuro, não nesta rodada) |
| `spring.grpc.server.*` | Tuning de conexão gRPC (candidato futuro) |

> ✅ **Schema**: controlado por um **knob único `SCOS_DB_SCHEMA` (default `scos`)**. O mesmo valor alimenta o `currentSchema` da URL (via placeholder aninhado no default) **e** o `default-schema` do Liquibase (principal + auditoria), garantindo que os dois sejam **sempre iguais**. Para mudar o schema, sete só `SCOS_DB_SCHEMA`. ⚠️ Se você sobrescrever `SCOS_DB_URL` inteira com outro `currentSchema`, sete `SCOS_DB_SCHEMA` com o mesmo valor (o Liquibase lê o knob, não a URL).

## Segurança — leia antes de subir em produção

- Os segredos têm **default versionado no git** (para regressão-zero). Isso NÃO os protege — só permite override. **Produção MUST setar todas as env de segredo.**
- `SCOS_PRIVACY_STRICT=true` só com `SCOS_PRIVACY_CRYPTO_SECRET` real — senão a aplicação não sobe.
- Hardening (remover default do git, cifra em repouso via Jasypt `ENC`) é change separada.
