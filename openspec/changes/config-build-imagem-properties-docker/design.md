## Context

Dois módulos deployáveis (`scos-organization-boot` HTTP, `scos-organization-grpc-boot` gRPC) precisam rodar como **uma mesma imagem** em três alvos: local (dev), docker (ambiente de testes k6) e **on-premise por cliente** (whitelabel — cada cliente com Postgres/Keycloak/Redis/registry próprios, infra possivelmente pequena/restrita). Hoje:

- Nenhum pom tem bloco `<image>` no `spring-boot-maven-plugin` → `build-image` só ad-hoc.
- Layout dos yaml: `application.yml` guarda valores brutos sob `scos.*`; `bootstrap.yml` faz o binding (`spring.datasource.url: ${scos.datasource.url}`), mais registry, management, JPA e liquibase. `logback-spring.xml` tem `LOG_DIR` fixo.
- Topologia/pool/toggles/tuning/log estão hardcoded em `localhost`/valores de dev.

Base 12-factor: cada instalação seta suas env vars, sem depender de serviço central (`spring.cloud.config.enabled: false`, futuro incerto).

## Goals / Non-Goals

**Goals:**
- Build de imagem reproduzível e nomeado por módulo, parametrizável (CDS/threads) sem editar pom.
- Mesma imagem + mesmo yaml servem local/docker/on-premise só variando env var; sem env = comportamento de hoje byte-a-byte.
- Externalizar topologia, pool, toggles, tuning operacional, diagnóstico JPA/observability e configuração de log.
- Corrigir `cache.master` do grpc-boot e a rotação morta do appender JSON.

**Non-Goals:**
- **Hardening de segredo**: remover o default do git ou cifrar em repouso (Jasypt `ENC`) — change separada. Esta change externaliza o segredo como env-com-default (override), não o protege no repositório.
- Composição do ambiente k6 (compose de apps, seed, scripts), ajuste do realm, CI.
- Externalizar identidade de sistema (`registry.system-code`, `keycloak.client-id`), convenção JPA estrutural (`ddl-auto`, naming, batch), `management.*` além de sampling.

## Decisions

**D1 — Build via buildpacks (`spring-boot:build-image`), não Dockerfile/Jib.** Zero Dockerfile pra manter; plugin já presente. Builder `jammy-tiny` (imagem menor); trade-off: run image sem shell/curl → health-check via HTTP externo (`GET /actuator/health`), não `HEALTHCHECK` nativo.

**D2 — CDS parametrizável com default `true`, fallback via `-Dbp.cds.enabled=false`.** Ganho de startup (~2x, issue #581) vale tentar, mas bug conhecido (Spring Boot 4.0.1 + Java 25 + buildpack 5.35.0) exige desabilitar sem editar pom. Descartado fixar em `false` (perde o ganho no caso bom).

**D3 — Placeholder `${VAR:default}` direto no yaml, default = valor de hoje.** Cobre local/docker/on-premise no mesmo arquivo, auto-descritivo. Descartado profile `application-docker.yml` (exigiria alternar profile) e sobrescrita relaxed-binding da property inteira (menos explícito). Compatível com Config Server futuro: se ativado, property sources externas resolvem `scos.*` antes do default do jar.

**D4 — Endereços em forma de URL → env var da string inteira, não split host+porta.** Revalidação (2026-07-05): split deixaria `scheme` (`http` quebra Keycloak prod HTTPS), db, schema e realm cravados, e quebraria Sentinel HA (lista de nós). `SCOS_DB_URL`/`SCOS_AUDIT_DB_URL`/`SCOS_KEYCLOAK_ISSUER_URI`/`SCOS_REDIS_SENTINELS` = 1 var cada, mais cobertura e menos vars que o split original. Registry mantém host/port (já separados no yaml). `SCOS_AUDIT_DB_URL` é var própria (mesmo default de hoje) pra permitir DB de auditoria separado por instalação.

**D5 — Diagnóstico JPA e tracing sampling reclassificados de "fixo" para toggle.** `show_sql`/`format_sql`/`generate_statistics`/slow-query e `tracing.sampling.probability: 1.0` são diagnóstico de dev ligado, não arquitetura — em prod on-premise são perf/PII/custo. Viram env com default de hoje (regressão-zero); a mudança é só permitir desligar. `management.endpoints.web.exposure.include: '*'` fica fixo por ora (anotado como candidato de segurança).

**D6 — `LOG_DIR`/`LOG_LEVEL` viram env; appender `ScosJson` corrigido.** `logback-spring.xml` é Spring-aware → resolve env var. Hoje `LOG_DIR=logs` fixo/relativo fura o objetivo (não editar arquivo). `ScosJson` é `FileAppender` com `<rollingPolicy>` ignorado → `application.json` cresce ilimitado; troca para `RollingFileAppender`. Em container, a discussão "só stdout vs FileAppender" fica registrada como open question, sem mudar o default agora.

**D7 — Segredos externalizados como env-com-default (revisado 2026-07-05 por decisão do usuário).** Todas as credenciais (`datasource`/`audit` username+password, `cache.password`, `registry.key-access`, `privacy.crypto.secret`) viram `${VAR:default}` com default = valor de hoje. Escolhido sobre "env sem default" (que removeria o segredo do git, ganho de segurança real, mas quebraria regressão-zero) e sobre Jasypt-only (mais seguro, porém mecanismo separado). **Trade-off explícito**: o default versionado NÃO fecha o risco de segredo compartilhado entre clientes — só permite override; o risco só é mitigado se cada instalação setar a env. Hardening (remover default / cifra em repouso via Jasypt `ENC`) fica em change própria; esta apenas habilita o override 12-factor. `.env.example` e doc DEVEM alertar "prod obrigatoriamente seta as env de segredo".

## Risks / Trade-offs

- **`SCOS_PRIVACY_STRICT=true` + `crypto.secret` fake → boot falha** ("error if hash/encrypt has no key"). → Agora `SCOS_PRIVACY_CRYPTO_SECRET` é setável por env; documentar que ligar strict exige setar a chave real junto.
- **Segredo com default versionado dá falsa sensação de segurança** → operador pode achar que "virou env" = seguro, mas o default no git continua compartilhado. → `.env.example` marca segredos como obrigatórios em prod; não confiar no default. Change Jasypt faz o hardening.
- **`SCOS_DB_URL` e `SCOS_AUDIT_DB_URL` são vars distintas** → setar só uma deixa a outra em `localhost` dentro do container, falha parcial difícil de diagnosticar. → `.env.example` documenta as duas juntas.
- **Schema na URL vs `default-schema` do Liquibase** → resolvido com knob único `SCOS_DB_SCHEMA` alimentando os dois (OQ1). Risco residual: sobrescrever `SCOS_DB_URL` inteira com outro `currentSchema` sem setar `SCOS_DB_SCHEMA` junto → documentado no `.env.example`/doc.
- **`minimum-idle > maximum-pool-size`** ao reduzir só o pool máximo → HikariCP falha na subida. → `.env.example` valida a relação.
- **`LOG_DIR` relativo em container** resolve pro CWD do run image (efêmero) → operador precisa apontar volume. → Documentar; avaliar stdout-only (OQ2).
- **`application.json` sem rotação hoje** enche disco silenciosamente em on-premise long-running. → RF-15 corrige; confirmar em runtime.
- **34+ env vars é muito pra configurar de cabeça** → `.env.example` completo é obrigatório, não opcional.
- **CDS fallback dobra tempo no pior caso** (2 builds, um falhando) → aceitável pra build pontual; medir antes de assumir padrão.

## Migration Plan

1. Aplicar `<image>` nos 2 poms; validar `build-image` isolado por módulo, com e sem `-Dbp.cds.enabled`.
2. Externalizar yaml (`application.yml` valores brutos, `bootstrap.yml` diag/tracing/filter/tomcat/redis-timeout) e `logback-spring.xml`.
3. Corrigir `cache.master` (grpc-boot) e appender `ScosJson` (ambos).
4. Gerar `.env.example`.
5. **Gate de regressão**: subir os 2 módulos sem nenhuma env var e comparar comportamento com o baseline atual (todas as properties, não só host).
6. Rollback: mudanças são só em config declarativa (yaml/pom/xml) — reverter os arquivos restaura o estado anterior; sem migração de dados.

## Open Questions

- **OQ1 (RESOLVIDA 2026-07-05)**: Schema vira **knob único `SCOS_DB_SCHEMA` (default `scos`)**, usado no `currentSchema` da URL (placeholder aninhado no default) e no `default-schema` do Liquibase (principal + auditoria) — garantia de que os dois são sempre iguais. Decisão do usuário: "ambos têm que ser o mesmo, por default a aplicação usa `scos`".
- **OQ2**: Em container, desligar os 2 FileAppender e deixar só Console/stdout (12-factor) ou manter arquivo + `LOG_DIR`? Default proposto: manter arquivo agora, só tornar `LOG_DIR` configurável.
- **OQ3**: `management.endpoints.web.exposure.include` continua `'*'` ou vira env por postura de segurança? Default proposto: fixo nesta change, anotado.
