## 1. Build de imagem (buildpacks)

- [x] 1.1 Adicionar bloco `<image>` no `spring-boot-maven-plugin` de `scos-organization-boot/pom.xml` (nome `scos-organization-boot:local`, builder `paketobuildpacks/builder-jammy-tiny:<tag>`, `pullPolicy=IF_NOT_PRESENT`, `BP_JVM_VERSION=25`)
- [x] 1.2 Parametrizar CDS (`BP_JVM_CDS_ENABLED=${bp.cds.enabled}`, default `true`) e thread-count (`BPL_JVM_THREAD_COUNT=${bp.jvm.thread.count}`, default 1-CPU) via propriedade Maven no pom do `boot`
- [x] 1.3 Repetir 1.1 e 1.2 em `grpc/scos-organization-grpc-boot/pom.xml` (nome `scos-organization-grpc-boot:local`)

## 2. Externalização — endereços em forma de URL (string inteira)

- [x] 2.1 `boot/application.yml`: `scos.datasource.url` → `${SCOS_DB_URL:jdbc:postgresql://localhost:5432/scos?currentSchema=scos}`; `scos.audit.datasource.url` → `${SCOS_AUDIT_DB_URL:...}`
- [x] 2.2 `boot/application.yml`: `scos.security.keycloak.issuer-uri` → `${SCOS_KEYCLOAK_ISSUER_URI:http://localhost:7080/realms/Scos}`; `scos.cache.sentinels` → `${SCOS_REDIS_SENTINELS:localhost:26379}`
- [x] 2.3 Repetir 2.1 e 2.2 em `grpc-boot/application.yml`
- [x] 2.4 Schema como knob único `SCOS_DB_SCHEMA:scos` (OQ1 resolvida): `currentSchema` da URL (aninhado no default, 2 módulos) + `default-schema` do Liquibase principal e auditoria (`boot/bootstrap.yml`) leem o mesmo knob

## 3. Externalização — registry, porta, pool, toggles, tuning (application.yml)

- [x] 3.1 `boot/bootstrap.yml`: `scos.registry.host`/`port`/`tls-enabled` → `${SCOS_REGISTRY_HOST:localhost}`/`${SCOS_REGISTRY_PORT:8090}`/`${SCOS_REGISTRY_TLS_ENABLED:false}`
- [x] 3.2 `boot/application.yml`: `scos.port` → `${SCOS_PORT:8081}`; `scos.cache.port` → `${SCOS_REDIS_PORT:26379}`
- [x] 3.3 `boot/application.yml`: pool principal (`SCOS_DB_CONN_TIMEOUT`/`_IDLE_TIMEOUT`/`_MAX_LIFETIME`/`_MIN_IDLE`/`_MAX_POOL_SIZE`) e auditoria (`SCOS_AUDIT_DB_*`), defaults = valores de hoje
- [x] 3.4 `boot/application.yml`: toggles (`SCOS_AUDIT_ENABLED`, `SCOS_CACHE_ENABLE`, `SCOS_LIQUIBASE_ENABLED`, `SCOS_PRIVACY_ENABLED`, `SCOS_PRIVACY_STRICT`, `SCOS_JDEMPOTENT_ENABLED`)
- [x] 3.5 `boot/application.yml`: tuning fino cache (`SCOS_CACHE_TTL`, `SCOS_CACHE_DATABASE`), jdempotent (`SCOS_JDEMPOTENT_*`), privacy (`SCOS_PRIVACY_MAX_PAYLOAD_KB`)
- [x] 3.6 Repetir 3.2–3.5 em `grpc-boot/application.yml`, respeitando defaults por módulo (`SCOS_PORT:8090`, `SCOS_CACHE_ENABLE:false`, `SCOS_LIQUIBASE_ENABLED:false`), sem jdempotent

## 4. Externalização — diagnóstico JPA, tracing, log HTTP, tuning extra (bootstrap.yml)

- [x] 4.1 `boot/bootstrap.yml`: JPA diag (`SCOS_JPA_SHOW_SQL:true` para `show_sql`+`format_sql`, `SCOS_JPA_GENERATE_STATISTICS:true`, `SCOS_JPA_SLOW_QUERY_MS:500`)
- [x] 4.2 `boot/bootstrap.yml`: `management.tracing.sampling.probability` → `${SCOS_TRACING_SAMPLING:1.0}`; `scos.filter.*` → `SCOS_LOG_SHOW_REQUEST_BODY`/`_REQUEST_HEADERS`/`_RESPONSE_BODY` (default `false`)
- [x] 4.3 `boot/bootstrap.yml`: `spring.data.redis.timeout` → `${SCOS_REDIS_TIMEOUT:5000ms}`; tomcat `SCOS_TOMCAT_MAX_CONNECTIONS:5000`/`SCOS_TOMCAT_CONNECTION_TIMEOUT:120000`
- [x] 4.4 Repetir 4.1 e 4.2 em `grpc-boot/bootstrap.yml` (sem tomcat/redis-timeout, que não existem nesse módulo)

## 5. Correções de configuração

- [x] 5.1 `grpc-boot/application.yml`: `scos.cache.master` `inside_flow_master` → `scos_master`
- [x] 5.2 `boot/logback-spring.xml`: appender `ScosJson` de `FileAppender` → `RollingFileAppender` (rotação de fato)
- [x] 5.3 Repetir 5.2 em `grpc-boot/logback-spring.xml`

## 6. Externalização — log (logback-spring.xml)

- [x] 6.1 `boot/logback-spring.xml`: `LOG_DIR` → `${LOG_DIR:logs}`; `<root level>` → `${LOG_LEVEL:INFO}`
- [x] 6.2 Repetir 6.1 em `grpc-boot/logback-spring.xml`

## 7. Documentação

- [x] 7.1 Criar `.env.example` com TODAS as env vars agrupadas por categoria (topologia, porta, pool, toggle, tuning, diag/observability, log) e defaults = valor de hoje
- [x] 7.2 No `.env.example`, marcar segredos como **obrigatórios em produção** (default é só de dev, versionado no git — não confiar); alertar `SCOS_DB_MIN_IDLE ≤ SCOS_DB_MAX_POOL_SIZE` e `SCOS_PRIVACY_STRICT=true` só com `SCOS_PRIVACY_CRYPTO_SECRET` real; apontar change Jasypt como hardening
- [x] 7.3 Documentar tabela properties → env var por categoria (README/doc), deixando claro o que não mexer (identidade, JPA estrutural, segredo)

## 9. Externalização — segredos (env com default = valor de hoje)

- [x] 9.1 `boot/application.yml`: `scos.datasource.username`/`password` → `${SCOS_DB_USERNAME:scos}`/`${SCOS_DB_PASSWORD:scos#2026}`; `scos.audit.datasource.username`/`password` → `${SCOS_AUDIT_DB_USERNAME:scos}`/`${SCOS_AUDIT_DB_PASSWORD:scos#2026}`
- [x] 9.2 `boot/application.yml`: `scos.cache.password` → `${SCOS_REDIS_PASSWORD:scos#2026}`; `scos.privacy.crypto.secret` → `${SCOS_PRIVACY_CRYPTO_SECRET:ASDASDASDA}`
- [x] 9.3 `boot/bootstrap.yml`: `scos.registry.key-access` → `${SCOS_REGISTRY_KEY_ACCESS:ABLABLABLA}`
- [x] 9.4 Repetir 9.1 e 9.2 em `grpc-boot/application.yml` (sem registry.key-access, que não existe nesse módulo)

## 8. Validação

- [ ] 8.1 `mvn -pl scos-organization-boot spring-boot:build-image` e idem grpc-boot, com e sem `-Dbp.cds.enabled=false` — **pendente: exige Docker daemon + pull do builder** (estático: poms passam `mvn validate` offline)
- [ ] 8.2 Subir os 2 módulos sem nenhuma env var e confirmar comportamento idêntico ao baseline atual (regressão-zero, todas as properties) — **pendente: exige Postgres/Keycloak/Redis** (estático: YAML/logback parseiam OK)
- [ ] 8.3 Subir com env var de topologia setada (`SCOS_DB_URL`, `SCOS_KEYCLOAK_ISSUER_URI`, `SCOS_REDIS_SENTINELS`) e confirmar override — **pendente: exige infra**
- [ ] 8.4 Confirmar rotação de `application.json` após a correção do appender (não cresce ilimitado) — **pendente: exige runtime prolongado**
