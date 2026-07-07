## Why

Os 2 módulos deployáveis (`scos-organization-boot`, `scos-organization-grpc-boot`) não têm o bloco `<image>` do `spring-boot-maven-plugin` configurado (build de imagem só ad-hoc, sem nome/builder/tuning) e têm topologia de infra (`datasource.url`, `keycloak.issuer-uri`, `cache.sentinels`, `registry.host`), pool, toggles, tuning operacional e o diretório de log **hardcoded** — `localhost`/valores fixos que, dentro de um container, apontam pro próprio container. Isso bloqueia rodar a mesma imagem em docker (ambiente k6) e em **deploy on-premise por cliente** (cada cliente com Postgres/Keycloak/Redis/registry próprios) sem rebuild nem edição de arquivo.

## What Changes

- **Build de imagem**: bloco `<image>` no `spring-boot-maven-plugin` dos 2 poms — nome fixo (`:local`), builder `paketobuildpacks/builder-jammy-tiny` pinado, `pullPolicy=IF_NOT_PRESENT`, `BP_JVM_VERSION=25`, CDS e thread-count parametrizáveis via propriedade Maven
- **Externalização de config** via placeholder `${VAR:default}` (default = valor de hoje, regressão-zero):
  - Endereços com forma de URL → **env var da string inteira** (`SCOS_DB_URL`, `SCOS_AUDIT_DB_URL`, `SCOS_KEYCLOAK_ISSUER_URI`, `SCOS_REDIS_SENTINELS`) — split host+porta deixaria scheme `http`/db/schema/realm cravados e impediria Sentinel HA
  - Registry (host/port/tls), porta da app, pool principal+auditoria, toggles operacionais, tuning cache/jdempotent/privacy → env var
  - Diagnóstico JPA (`show_sql`/`generate_statistics`/slow-query), `tracing.sampling.probability`, log HTTP (`scos.filter.*`), redis timeout, tomcat → env var (reclassificados de "fixo")
  - `LOG_DIR`/`LOG_LEVEL` do `logback-spring.xml` → env var
- **Correções**: `scos.cache.master` do `grpc-boot` (`inside_flow_master` → `scos_master`); appender `ScosJson` (`FileAppender` → `RollingFileAppender`, hoje `application.json` cresce sem rotação)
- **Segredos** (decisão 2026-07-05): credenciais (`datasource.username`/`password`, `audit.datasource.*`, `cache.password`, `registry.key-access`, `privacy.crypto.secret`) viram env var **com default = valor de hoje** (`${SCOS_DB_PASSWORD:scos#2026}`). Permite cada cliente sobrescrever sua credencial sem rebuild. ⚠️ **Não remove o segredo do git** (default versionado) — só fecha o risco multi-cliente se cada instalação setar a env; hardening real (remover do git / cifrar em repouso) continua na change Jasypt separada
- **Documentação**: `.env.example` com todas as env vars + defaults, agrupadas por categoria
- **Fora de escopo (SRP)**: hardening de segredo (remover default do git, Jasypt `ENC`) → change própria; composição do ambiente k6; CI

## Capabilities

### New Capabilities
- `container-image-build`: configuração de build de imagem OCI via buildpacks (`spring-boot:build-image`) nos módulos deployáveis — nome, builder pinado, env de build, parametrização de CDS/thread-count
- `runtime-config-externalization`: externalização de topologia, pool, toggles, tuning operacional e configuração de log via env var (`${VAR:default}`, default = comportamento atual), permitindo a mesma imagem servir local/docker/on-premise sem rebuild

### Modified Capabilities
<!-- Nenhuma capability de spec existente muda requisito. -->

## Impact

- **Modificados**: `scos-organization-boot/pom.xml`, `.../application.yml`, `.../bootstrap.yml`, `.../logback-spring.xml`; `grpc/scos-organization-grpc-boot/pom.xml`, `.../application.yml`, `.../bootstrap.yml`, `.../logback-spring.xml`
- **Novo**: `.env.example` (raiz ou `etc/`)
- **Sem impacto de banco**; sem mudança de API/contrato
- **Segurança**: segredos agora sobrescrevíveis por env por instalação, mas o **default ainda versionado** — o `.env.example` e a doc devem alertar que prod MUST setar as env de segredo (não confiar no default). Remoção do default do git / cifra em repouso fica pra change Jasypt
