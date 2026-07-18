# Configuração de Build de Imagem Docker (Buildpacks) e Externalização de Properties

**Data**: 2026-07-05
**Status**: 🔄 Em Análise (revalidada 2026-07-05 — ver §5 Revalidação)
**Tipo**: 🔧 Refatoração

---

## ⚠️ Princípio SRP — Uma Funcionalidade por Ideia

> **Regra**: Uma ideia = uma funcionalidade. Features independentes → arquivos separados.

- **Nome da funcionalidade**: `config-build-imagem-e-properties-docker`
- **Resumo em uma frase**: Configurar `spring-boot-maven-plugin` (buildpacks) em `flow-organization-boot` e `flow-organization-grpc-boot` pra gerar imagem local com nome/builder fixos, e maximizar o que é configurável via env var (`${VAR:default}`, default = comportamento de hoje) — topologia de rede, portas e tuning operacional — pra uma **mesma imagem** servir dev local, docker, e instalação on-premise de qualquer cliente sem rebuild nem edição de arquivo.

> **Contexto confirmado com o usuário**: "diversos clientes" aqui significa **deploy separado por cliente** (on-premise/whitelabel) — cada cliente tem sua própria instância de Postgres/Keycloak/Redis/registry. Além disso, o ambiente de destino pode ter **restrição de quantas aplicações sobem** (infra pequena/compartilhada) — por isso o objetivo é maximizar o que dá pra reconfigurar via env var (host, porta, pool, timeout, toggle) sem precisar de imagem diferente por cliente. Isso valida o mecanismo de placeholder+env var (padrão 12-factor): cada instalação seta suas próprias env vars, sem depender de um serviço central. Ver seção de Decisões pra detalhe sobre Config Server.

**Checklist SRP**:
- [x] Esta ideia cobre exatamente uma funcionalidade
- [x] Não mistura features independentes no mesmo arquivo
- [x] O nome é específico (não genérico como "melhorar módulo X")

---

## 1️⃣ Visão

### Problema
Nenhum dos 2 módulos deployáveis (`flow-organization-boot`, `flow-organization-grpc-boot`) tem o bloco `<image>` configurado no `spring-boot-maven-plugin` — `mvn spring-boot:build-image` funciona só de forma ad-hoc, sem nome/builder/tuning definidos. Além disso, `application.yml`/`bootstrap.yml` de ambos os módulos têm `datasource.url`, `security.keycloak.issuer-uri`, `cache.sentinels` e (só no `boot`) `registry.host` hardcoded pra `localhost` — dentro de um container, `localhost` aponta pro próprio container, não pros serviços de infra reais. Isso bloqueia qualquer cenário que precise rodar a imagem containerizada apontando pra infra real: ambiente de testes k6 ([[suite-testes-k6-ambiente-integracao]]) hoje, e **deploy on-premise por cliente** amanhã — cada cliente com seu próprio Postgres/Keycloak/Redis/registry.

### Objetivo
- `pom.xml` de `flow-organization-boot` e `flow-organization-grpc-boot` com `<image>` configurado: nome fixo, builder pinado, `pullPolicy`, env de build tunados pra performance
- `application.yml`/`bootstrap.yml` dos 2 módulos com **topologia (host+porta de DB/Keycloak/Redis/Registry), porta da própria app, pool de conexão, toggles e tuning operacional** como placeholder `${VAR:default}` — mesmo arquivo funciona local (sem setar nada, cai no default de hoje), em docker, ou numa instalação on-premise pequena/restrita, só setando env var
- Corrigir a inconsistência encontrada em `scos.cache.master` do `grpc-boot`
- Deixar explícito o que **não** vira placeholder (identidade do sistema, convenções de arquitetura JPA/Hibernate) e o que é segredo (tratado em ideia própria, Jasypt)

### Fora de Escopo
- Composição do ambiente de teste (compose de apps, seed, scripts k6) — outra ideia: [[suite-testes-k6-ambiente-integracao]]
- Ajuste do `Scos_Realm.json` (login `scos-api`) — parte da ideia do ambiente k6, não desta
- CI pipeline

---

## 2️⃣ Requisitos

### Funcionais
- [ ] **RF-01**: `<image><name>` fixo em ambos os poms (ex: `scos-organization-boot:local`, `scos-organization-grpc-boot:local`)
- [ ] **RF-02**: `<image><builder>` pinado em tag exata (`paketobuildpacks/builder-jammy-tiny:<tag>`, nunca `latest`), `<pullPolicy>IF_NOT_PRESENT</pullPolicy>`
- [ ] **RF-03**: `<image><env>` com `BP_JVM_VERSION=25` fixo
- [ ] **RF-04**: `<image><env><BP_JVM_CDS_ENABLED>` parametrizado via propriedade Maven (`${bp.cds.enabled}`, default `true`) — permite `-Dbp.cds.enabled=false` via linha de comando sem editar o pom. Contrato consumido pelo runner do ambiente k6 pro fallback automático (bug [#581](https://github.com/paketo-buildpacks/spring-boot/issues/581))
- [ ] **RF-05**: `<image><env><BPL_JVM_THREAD_COUNT>` parametrizado via propriedade Maven (`${bp.jvm.thread.count}`, default sensato pra 1 CPU) — dimensionado junto do `mem_limit` de quem for rodar o container
- [ ] **RF-06** *(revisado — ver §5)*: Endereços de infra viram placeholder nos 2 módulos. **Propriedades com forma de URL viram env var da string inteira** (não split host+porta), porque o split deixa scheme/db/schema/realm cravados: `SCOS_DB_URL` (cobre host+porta+db+schema), `SCOS_KEYCLOAK_ISSUER_URI` (cobre scheme+host+porta+realm — `http` cravado quebraria Keycloak prod HTTPS), `SCOS_REDIS_SENTINELS` (lista `h:p,h:p` — split host+porta impede Sentinel HA). Registry, que já é host/port separados no yaml, mantém split: `SCOS_REGISTRY_HOST`/`SCOS_REGISTRY_PORT` (`bootstrap.yml`, só `boot`)
- [ ] **RF-07**: Porta da própria aplicação vira placeholder: `scos.port` usa `${SCOS_PORT:8081}` (boot) / `${SCOS_PORT:8090}` (grpc-boot)
- [ ] **RF-08**: `scos.registry.tls-enabled` vira placeholder `${SCOS_REGISTRY_TLS_ENABLED:false}` (`bootstrap.yml`, só `boot`) — comentário original já indicava `# true em produção`
- [ ] **RF-09**: Pool de conexão do datasource principal e de auditoria viram placeholder (timeouts, min-idle, max-pool-size) nos 2 módulos — `SCOS_DB_CONN_TIMEOUT`, `SCOS_DB_IDLE_TIMEOUT`, `SCOS_DB_MAX_LIFETIME`, `SCOS_DB_MIN_IDLE`, `SCOS_DB_MAX_POOL_SIZE` (principal) e os mesmos com prefixo `SCOS_AUDIT_DB_*` (auditoria)
- [ ] **RF-10**: Toggles operacionais viram placeholder: `SCOS_AUDIT_ENABLED`, `SCOS_CACHE_ENABLE`, `SCOS_LIQUIBASE_ENABLED`, `SCOS_PRIVACY_ENABLED`, `SCOS_PRIVACY_STRICT`, `SCOS_JDEMPOTENT_ENABLED` (jdempotent só existe no `boot`)
- [ ] **RF-11**: Tuning fino vira placeholder: cache (`SCOS_CACHE_TTL`, `SCOS_CACHE_DATABASE`), jdempotent (`SCOS_JDEMPOTENT_EXPIRATION_HOUR`, `SCOS_JDEMPOTENT_DIAL_TIMEOUT_SEC`, `SCOS_JDEMPOTENT_READ_TIMEOUT_SEC`, `SCOS_JDEMPOTENT_WRITE_TIMEOUT_SEC`, `SCOS_JDEMPOTENT_MAX_RETRY`, `SCOS_JDEMPOTENT_EXPIRE_TIMEOUT_HOUR`), privacy (`SCOS_PRIVACY_MAX_PAYLOAD_KB`)
- [ ] **RF-12**: Corrigir `scos.cache.master` em `grpc/scos-organization-grpc-boot/src/main/resources/application.yml` — hoje `inside_flow_master`, não bate com `REDIS_MASTER_SET=scos_master` de `etc/infra/docker-compose-redis.yml` (bug pré-existente, dormente porque `cache.enable: false` nesse módulo hoje). Esse valor não vira placeholder — é constante, só corrige o valor errado
- [ ] **RF-13**: Documentar (README próprio ou seção deste repositório) a tabela completa properties → env var, agrupada por categoria (topologia, porta, pool, toggle, tuning, fixo, segredo), pra qualquer consumidor (runner k6, deploy on-premise futuro) saber exatamente o que setar e o que não mexer

#### Requisitos adicionados na revalidação (2026-07-05)

- [ ] **RF-14**: `LOG_DIR` do `logback-spring.xml` (hoje `<property value="logs">`, fixo e relativo) vira placeholder `${LOG_DIR:logs}` nos 2 módulos. Motivo: caminho fixo fura o RNF-01 — cliente on-premise que precise gravar em `/var/log/scos` teria que editar o XML e rebuildar. `logback-spring.xml` é Spring-aware → resolve env var. Adicionar também `LOG_LEVEL` (`<root level="${LOG_LEVEL:INFO}">`) — nível de log varia por install (prod quer WARN, debug pontual quer DEBUG) sem rebuild
- [ ] **RF-15**: Corrigir appender `ScosJson` no `logback-spring.xml` — hoje é `ch.qos.logback.core.FileAppender` com um `<rollingPolicy>` filho que o `FileAppender` **ignora silenciosamente** → `application.json` cresce ilimitado (sem `maxHistory`/`totalSizeCap`), enche disco numa instalação long-running. Trocar por `RollingFileAppender` (igual ao `ScosLog`). Bug pré-existente, independente do env; entra por estar na área de log revalidada
- [ ] **RF-16**: Toggles de diagnóstico JPA/Hibernate viram placeholder `SCOS_JPA_SHOW_SQL` (`show_sql`+`format_sql`), `SCOS_JPA_GENERATE_STATISTICS` (`generate_statistics`), `SCOS_JPA_SLOW_QUERY_MS` (`LOG_QUERIES_SLOWER_THAN_MS`), **default = valor de hoje (`true`/`true`/`500`)**. Reclassificação: hoje estão na categoria "fixo/arquitetura", mas **não são arquitetura** — são diagnóstico de dev ligado. Em prod on-premise: loga todo SQL (volume + PII potencial no SQL) e calcula estatísticas (perf). Manter default atual preserva regressão-zero (RNF-01); a mudança é só permitir desligar por env
- [ ] **RF-17**: `management.tracing.sampling.probability` vira `SCOS_TRACING_SAMPLING` (default `1.0`). Exceção consciente ao "management.* fica fixo": 100% de sampling em prod é custo/perf real e é o parâmetro de observability que mais varia por install (dev `1.0`, prod `0.1`). Restante de `management.*` permanece fixo
- [ ] **RF-18**: Tuning operacional extra vira placeholder: `SCOS_REDIS_TIMEOUT` (`spring.data.redis.timeout`, default `5000ms`, só `boot`) e Tomcat do `boot` (`SCOS_TOMCAT_MAX_CONNECTIONS` default `5000`, `SCOS_TOMCAT_CONNECTION_TIMEOUT` default `120000`) — mesmo motivo do pool: "infra pequena" reduz sem rebuild
- [ ] **RF-19**: Toggles de log HTTP viram placeholder `SCOS_LOG_SHOW_REQUEST_BODY`/`SCOS_LOG_SHOW_REQUEST_HEADERS`/`SCOS_LOG_SHOW_RESPONSE_BODY` (`scos.filter.*`, default `false` nos 2). ⚠ Ligar body/header logging expõe PII (mascarada pelo privacy, mas volume+risco). É toggle operacional de debug — cai na categoria "toggles" que a v1 não cobriu

### Não-Funcionais
- [ ] **RNF-01**: Rodar local sem setar nenhuma env var reproduz exatamente o comportamento de hoje — todo default de placeholder é o valor atual do `application.yml`/`bootstrap.yml`. Qualquer outro alvo (docker, on-premise) só requer setar as env vars relevantes, sem editar arquivo
- [ ] **RNF-02**: Build de imagem funciona isoladamente por módulo (`mvn -pl scos-organization-boot spring-boot:build-image`), sem precisar buildar o reactor inteiro
- [ ] **RNF-03**: Nenhuma propriedade de identidade de sistema (`registry.system-code`, `keycloak.client-id` etc.), convenção de arquitetura (JPA/Hibernate, naming strategy) ou segredo vira placeholder nesta ideia — mantém escopo fechado em topologia + tuning operacional

---

## 3️⃣ Arquitetura

### Componentes Afetados
```
scos-organization-boot/pom.xml
└── spring-boot-maven-plugin: adiciona bloco <image>

scos-organization-boot/src/main/resources/application.yml
└── host+porta (DB/Keycloak/Redis), scos.port, pool de conexão,
    toggles, tuning de cache/jdempotent/privacy: valor fixo → ${VAR:default}

scos-organization-boot/src/main/resources/bootstrap.yml
└── scos.registry.host/port/tls-enabled: valor fixo → ${VAR:default}

grpc/scos-organization-grpc-boot/pom.xml
└── spring-boot-maven-plugin: adiciona bloco <image>

grpc/scos-organization-grpc-boot/src/main/resources/application.yml
├── mesmos placeholders que o boot (sem jdempotent, que não existe nesse módulo)
└── scos.cache.master: inside_flow_master → scos_master (correção, valor fixo)

# Adicionados na revalidação 2026-07-05:
scos-organization-boot/src/main/resources/bootstrap.yml
grpc/scos-organization-grpc-boot/src/main/resources/bootstrap.yml
└── ATENÇÃO: JPA show_sql/generate_statistics/slow-query, tracing.sampling,
    scos.filter.*, redis.timeout, tomcat.* NÃO ficam no application.yml (raw scos.*)
    e sim no bootstrap.yml (bindings + management + jpa). Placeholder vai aqui (RF-16..19)

scos-organization-boot/src/main/resources/logback-spring.xml
grpc/scos-organization-grpc-boot/src/main/resources/logback-spring.xml
├── LOG_DIR fixo → ${LOG_DIR:logs}; <root level> → ${LOG_LEVEL:INFO} (RF-14)
└── ScosJson: FileAppender → RollingFileAppender (RF-15, bug rotação morta)
```

### Fluxo Principal
```
mvn spring-boot:build-image [-Dbp.cds.enabled=false]
  → imagem local (scos-organization-boot:local | scos-organization-grpc-boot:local)

Rodando local (nada setado)       → todo placeholder cai no default de hoje, comportamento idêntico
Rodando em docker (env setada)    → SCOS_DB_HOST=postgresql, SCOS_KEYCLOAK_HOST=keycloak,
                                     SCOS_REDIS_HOST=redis, SCOS_REGISTRY_HOST=<service grpc-boot>
Rodando on-premise (cliente X,     → mesmas env vars de topologia + ajuste fino de pool/timeout/
  infra pequena/restrita)            toggle conforme o tamanho da instalação daquele cliente
                                     (não tem "valor certo" único — cada instalação define o seu)
  → mesma imagem, mesmo application.yml/bootstrap.yml, comportamento muda só pela env var presente
```

### Properties — Tabela Completa por Categoria (placeholder `${VAR:default}`, default = valor de hoje)

Escopo confirmado: **topologia + tuning operacional** vira placeholder (inclusive portas); identidade de sistema, convenção de arquitetura (JPA/Hibernate) fica fixa; segredo vai pra ideia própria (Jasypt).

> ⚠ **Revisado 2026-07-05 (RF-06)**: a v1 fatiava url/issuer-uri/sentinels em host+porta. Split deixa **scheme (`http` → quebra Keycloak prod HTTPS), db name, schema e realm** cravados, e impede Sentinel HA (lista). Corrigido abaixo: propriedade com forma de URL → **1 env var da string inteira**. Registry, que já é host/port separado no yaml, mantém split.

**1. Endereços de infra (forma de URL → string inteira) — muda a cada instalação**

| Propriedade | Presente em | Env var | Default (hoje) |
|---|---|---|---|
| `scos.datasource.url` | boot, grpc-boot | `SCOS_DB_URL` | `jdbc:postgresql://localhost:5432/scos?currentSchema=scos` |
| `scos.audit.datasource.url` | boot, grpc-boot | `SCOS_AUDIT_DB_URL` (mesmo valor hoje; env própria pra permitir DB de auditoria separado) | idem acima |
| `scos.security.keycloak.issuer-uri` | boot, grpc-boot | `SCOS_KEYCLOAK_ISSUER_URI` | `http://localhost:7080/realms/Scos` |
| `scos.cache.sentinels` | boot, grpc-boot | `SCOS_REDIS_SENTINELS` (lista `h:p,h:p` — HA) | `localhost:26379` |
| `scos.registry.host` | boot (`bootstrap.yml`) | `SCOS_REGISTRY_HOST` | `localhost` |

**2. Portas próprias / registry (registry já é host+port separado; app expõe porta) **

| Propriedade | Presente em | Env var | Default (hoje) |
|---|---|---|---|
| `scos.registry.port` | boot (`bootstrap.yml`) | `SCOS_REGISTRY_PORT` | `8090` |
| `scos.cache.port` | boot, grpc-boot | `SCOS_REDIS_PORT` | `26379` (redundante c/ sentinel; manter default) |
| `scos.port` | boot / grpc-boot | `SCOS_PORT` | `8081` (boot) / `8090` (grpc-boot) |

> Nota acoplamento (era risco #5 do split): com url inteira, `SCOS_DB_URL` e `SCOS_AUDIT_DB_URL` são env vars distintas. Setar só uma deixa a outra em `localhost` dentro do container. Documentar juntas no `.env.example`. O schema (`currentSchema=scos`) agora entra embutido na url — **mas `default-schema: scos` do Liquibase (`bootstrap.yml`, principal + audit) continua fixo**; se um cliente mudar de schema via `SCOS_DB_URL`, o Liquibase desalinha. Decidir: ou schema fica convenção fixa (não mexer na url), ou `default-schema` também vira env acoplada. Ver risco #16.

**3. Pool de conexão (datasource) — ajuste fino por tamanho de instalação**

| Propriedade | Presente em | Env var | Default (hoje) |
|---|---|---|---|
| `scos.datasource.connection-timeout` | boot, grpc-boot | `SCOS_DB_CONN_TIMEOUT` | `600000` |
| `scos.datasource.idle-timeout` | boot, grpc-boot | `SCOS_DB_IDLE_TIMEOUT` | `150000` |
| `scos.datasource.max-lifetime` | boot, grpc-boot | `SCOS_DB_MAX_LIFETIME` | `1800000` |
| `scos.datasource.minimum-idle` | boot, grpc-boot | `SCOS_DB_MIN_IDLE` | `25` |
| `scos.datasource.maximum-pool-size` | boot, grpc-boot | `SCOS_DB_MAX_POOL_SIZE` | `100` |
| `scos.audit.datasource.connection-timeout` | boot, grpc-boot | `SCOS_AUDIT_DB_CONN_TIMEOUT` | `100000` |
| `scos.audit.datasource.idle-timeout` | boot, grpc-boot | `SCOS_AUDIT_DB_IDLE_TIMEOUT` | `150000` |
| `scos.audit.datasource.max-lifetime` | boot, grpc-boot | `SCOS_AUDIT_DB_MAX_LIFETIME` | `300000` |
| `scos.audit.datasource.minimum-idle` | boot, grpc-boot | `SCOS_AUDIT_DB_MIN_IDLE` | `10` |
| `scos.audit.datasource.maximum-pool-size` | boot, grpc-boot | `SCOS_AUDIT_DB_MAX_POOL_SIZE` | `30` |

> Pool grande (`maximum-pool-size: 100`) é dimensionado pra uma instalação de porte médio/grande. Cliente com infra pequena provavelmente precisa reduzir — é exatamente o caso que motivou virar placeholder.

**4. Toggles — liga/desliga por instalação**

| Propriedade | Presente em | Env var | Default (hoje) |
|---|---|---|---|
| `scos.audit.enabled` | boot, grpc-boot | `SCOS_AUDIT_ENABLED` | `true` |
| `scos.cache.enable` | boot, grpc-boot | `SCOS_CACHE_ENABLE` | `true` (boot) / `false` (grpc-boot) |
| `scos.liquibase.enabled` | boot, grpc-boot | `SCOS_LIQUIBASE_ENABLED` | `true` (boot) / `false` (grpc-boot) |
| `scos.privacy.enabled` | boot, grpc-boot | `SCOS_PRIVACY_ENABLED` | `true` |
| `scos.privacy.strict` | boot, grpc-boot | `SCOS_PRIVACY_STRICT` | `false` (comentário original: "prod: error if hash/encrypt has no key") |
| `scos.jdempotent.enabled` | boot (não existe no grpc-boot) | `SCOS_JDEMPOTENT_ENABLED` | `true` |
| `scos.registry.tls-enabled` | boot (`bootstrap.yml`) | `SCOS_REGISTRY_TLS_ENABLED` | `false` (comentário original: "true em produção") |

**5. Tuning fino — cache, jdempotent, privacy**

| Propriedade | Presente em | Env var | Default (hoje) |
|---|---|---|---|
| `scos.cache.time-to-live` | boot, grpc-boot | `SCOS_CACHE_TTL` | `12000000` |
| `scos.cache.database` | boot, grpc-boot | `SCOS_CACHE_DATABASE` | `0` |
| `scos.jdempotent.expiration-time-hour` | boot | `SCOS_JDEMPOTENT_EXPIRATION_HOUR` | `2` |
| `scos.jdempotent.dial-timeout-second` | boot | `SCOS_JDEMPOTENT_DIAL_TIMEOUT_SEC` | `3` |
| `scos.jdempotent.read-timeout-second` | boot | `SCOS_JDEMPOTENT_READ_TIMEOUT_SEC` | `3` |
| `scos.jdempotent.write-timeout-second` | boot | `SCOS_JDEMPOTENT_WRITE_TIMEOUT_SEC` | `3` |
| `scos.jdempotent.max-retry-count` | boot | `SCOS_JDEMPOTENT_MAX_RETRY` | `3` |
| `scos.jdempotent.expire-timeout-hour` | boot | `SCOS_JDEMPOTENT_EXPIRE_TIMEOUT_HOUR` | `3` |
| `scos.privacy.max-payload-kb` | boot, grpc-boot | `SCOS_PRIVACY_MAX_PAYLOAD_KB` | `64` |

**5b. Log — diretório e nível (adicionado na revalidação, `logback-spring.xml`)**

| Propriedade | Presente em | Env var | Default (hoje) |
|---|---|---|---|
| `LOG_DIR` (property do logback) | boot, grpc-boot | `LOG_DIR` | `logs` (relativo — vira absoluto por install) |
| `<root level>` | boot, grpc-boot | `LOG_LEVEL` | `INFO` |

**5c. Diagnóstico JPA / observability — reclassificado de "fixo" p/ toggle**

| Propriedade | Presente em | Env var | Default (hoje) |
|---|---|---|---|
| `hibernate.show_sql` + `format_sql` | boot, grpc-boot | `SCOS_JPA_SHOW_SQL` | `true` |
| `hibernate.generate_statistics` | boot, grpc-boot | `SCOS_JPA_GENERATE_STATISTICS` | `true` |
| `LOG_QUERIES_SLOWER_THAN_MS` | boot, grpc-boot | `SCOS_JPA_SLOW_QUERY_MS` | `500` |
| `management.tracing.sampling.probability` | boot, grpc-boot | `SCOS_TRACING_SAMPLING` | `1.0` |
| `scos.filter.show-request-body` | boot, grpc-boot | `SCOS_LOG_SHOW_REQUEST_BODY` | `false` |
| `scos.filter.show-request-headers` | boot, grpc-boot | `SCOS_LOG_SHOW_REQUEST_HEADERS` | `false` |
| `scos.filter.show-response-body` | boot, grpc-boot | `SCOS_LOG_SHOW_RESPONSE_BODY` | `false` |

**5d. Tuning operacional extra (adicionado na revalidação)**

| Propriedade | Presente em | Env var | Default (hoje) |
|---|---|---|---|
| `spring.data.redis.timeout` | boot | `SCOS_REDIS_TIMEOUT` | `5000ms` |
| `server.tomcat.max-connections` | boot | `SCOS_TOMCAT_MAX_CONNECTIONS` | `5000` |
| `server.tomcat.connection-timeout` | boot | `SCOS_TOMCAT_CONNECTION_TIMEOUT` | `120000` |

**Total: reavaliar contagem.** A v1 dizia "34 env vars" com granularidade host+porta. A revalidação: (a) **reduz** 3 pares host+porta a 3 env vars de URL inteira (`SCOS_DB_URL`, `SCOS_KEYCLOAK_ISSUER_URI`, `SCOS_REDIS_SENTINELS`) — menos vars, mais cobertura; (b) **adiciona** log, diagnóstico JPA/observability e tuning extra (RF-14..RF-19). Recontar no propose, não fixar número aqui. Invariante mantida: todo placeholder tem default = valor de hoje → rodar sem env reproduz comportamento atual.

**6. Fica fixo (não vira placeholder nesta ideia) — e por quê**

| Propriedade | Presente em | Motivo |
|---|---|---|
| `scos.security.keycloak.client-id`/`authorization-grant-type`/`scope`/`resourceId` | boot, grpc-boot | Identidade/convenção do client OAuth2, não topologia nem tuning |
| `scos.cache.key-prefix` | boot, grpc-boot | Namespace de chave — convenção de produto, não depende de instalação (cada cliente tem seu próprio Redis, sem risco de colisão) |
| `scos.cache.master` | boot, grpc-boot | Nome do master set do Sentinel é convenção fixa da infra padrão SCOS — só corrige o valor errado no `grpc-boot` (RF-12) |
| `scos.registry.system-code`/`system-name`/`system-description` | boot | Identidade do sistema, não endereço |
| `scos.privacy.masking.default-patterns`/`log.register-converter` | boot, grpc-boot | Regra de masking e wiring — convenção de produto, não posição de ambiente |
| JPA/Hibernate **estrutural** (`database-platform`, `naming.*`, `jdbc.batch_size`, `query.*`, `ddl-auto: validate` etc.) | boot, grpc-boot | Decisão de arquitetura do código, não de deploy. **Exceção (RF-16)**: `show_sql`/`format_sql`/`generate_statistics`/`LOG_QUERIES_SLOWER_THAN_MS` **saíram** desta linha — são diagnóstico, não arquitetura → viraram toggle |
| `management.*` (actuator exposure, métricas, tags) | boot, grpc-boot | Estrutura de observability fica fixa. **Exceção (RF-17)**: `tracing.sampling.probability` virou `SCOS_TRACING_SAMPLING` — único parâmetro que varia forte por install. `management.endpoints.web.exposure.include: '*'` **fica fixo por ora**, mas anotado como candidato (expõe heapdump/env — postura de segurança por install) |
| `spring.grpc.server.*` (`bootstrap.yml`) | boot, grpc-boot | Tuning de conexão gRPC já documentado com comentário no próprio arquivo; não fazia parte do pedido original (datasource/cache/jdempotent/privacy/registry) — candidato a entrar numa rodada futura se precisar |

Curiosidade sem relação com esta ideia: `hikari.pool-name: Inside_Flow-Fat` e a tag `management.metrics.tags.application: Inside_Flow-Fat` usam um nome antigo (`Inside_Flow`), diferente de `ScosOrganizationBoot`/`ScosOrganizationGrpcBoot` usado em `spring.application.name` — provável resíduo de nome anterior do projeto, não mexi por não ser o escopo pedido aqui.

### Segredos — reclassificados após confirmar deploy on-premise por cliente

Com "diversos clientes" confirmado como **deploy separado por cliente**, os segredos abaixo deixam de ser só "má prática genérica" e passam a ser **bloqueio real de produção**: cada cliente precisa da sua própria credencial, e hoje todas estão fixas no yaml versionado, iguais pra qualquer instalação.

| Propriedade | Presente em | Motivo de agora importar mais |
|---|---|---|
| `scos.datasource.username`/`password` | boot, grpc-boot | Cada cliente tem seu próprio Postgres — credencial não pode ser a mesma pra todo mundo |
| `scos.audit.datasource.username`/`password` | boot, grpc-boot | Mesmo motivo, mesma instância física do datasource principal |
| `scos.cache.password` | boot, grpc-boot | Cada cliente tem seu próprio Redis |
| `scos.registry.key-access` | boot | Hoje `ABLABLABLA` (valor claramente fake) — autenticação entre `organization-boot` e o registry, precisa ser real e distinta por instalação |
| `scos.privacy.crypto.secret` | boot, grpc-boot | Chave de criptografia da privacy — **mais crítico ainda**: se todo cliente usar a mesma chave hardcoded no jar/imagem, dado cifrado de um cliente pode virar vetor de ataque pra outro. Não é só "poderia variar", é risco de segurança concreto num modelo multi-cliente |

**Por que isso não vira RF/placeholder simples nesta ideia**: a skill `spring-security-scos` já documenta o padrão SCOS pra isso — **segredo se resolve com Jasypt (`ENC(...)` no yaml), não com env var em texto plano**. Mecanismo diferente do placeholder `${VAR:default}` usado pros hosts (que não são segredo). Misturar os dois nesta ideia quebraria SRP. **Tratado em ideia própria**: [[config-jasypt-segredos-organization]] — decide como cada cliente injeta sua chave/senha (Jasypt master password via env var + valor `ENC(...)`, por instalação).

### Decisões Técnicas
| Decisão | Escolha | Alternativa Descartada | Motivo |
|---------|---------|------------------------|--------|
| Build de imagem | `spring-boot:build-image` (buildpacks) | Dockerfile manual / Jib | Zero Dockerfile pra manter; plugin já presente nos 2 poms |
| Escopo de imagens | `organization-boot` + `organization-grpc-boot` | Incluir `security-starter` | `security-starter` tem `spring-boot-maven-plugin` com `skip=true` — é lib embarcada, não serviço deployável |
| Nome da imagem | `<image><name>` explícito no pom | Default do plugin | Nome previsível pra quem for referenciar a imagem em qualquer compose |
| Builder buildpacks | `jammy-tiny` | `jammy-base` | Imagem final menor. Trade-off aceito: sem shell/curl no run image — quem orquestrar precisa health-check via HTTP externo, não Docker `HEALTHCHECK` nativo |
| CDS + AOT | Parametrizável com default `true`, fallback via `-D` | Fixo em `false` | Ganho de startup (~2x, per issue #581) vale tentar; bug conhecido pro stack Spring Boot 4.0.1 + Java 25 + buildpack 5.35.0 exige capacidade de desabilitar sem editar pom |
| Memória / thread count | Parametrizável (`BPL_JVM_THREAD_COUNT`) | Fixo | Quem sobe o container decide `mem_limit`; thread count do memory calculator precisa acompanhar |
| Versão JDK no build | `BP_JVM_VERSION=25` explícito | Deixar buildpack auto-detectar | Java 25 é recente (suporte Paketo confirmado poucos dias após GA) — fixar evita ambiguidade |
| Hostnames em runtime | Placeholder `${VAR:localhost}` direto no `application.yml`/`bootstrap.yml`, default = comportamento local de hoje | Env var via relaxed binding sobrescrevendo a property inteira / profile `application-docker.yml` | Precisa rodar local OU docker OU on-premise sem editar arquivo em nenhum caso; placeholder com default cobre todos no mesmo arquivo e documenta o env var direto no yaml (auto-descritivo) |
| Escopo do placeholder | Topologia (host+porta) **e** tuning operacional (pool, toggle, timeout) | Só topologia (host) | Confirmado com o usuário: ambiente de destino pode ter poucas máquinas/recursos — mesma imagem precisa se ajustar (ex: pool menor) sem rebuild, não só apontar pro host certo |
| Portas | Viram placeholder (`SCOS_DB_PORT`, `SCOS_KEYCLOAK_PORT`, `SCOS_REDIS_PORT`, `SCOS_REGISTRY_PORT`, `SCOS_PORT`) | Só host, porta fixa por convenção | Confirmado com o usuário — instalação pequena pode ter porta customizada/conflito, precisa remapear sem rebuild |
| Modelo de multi-cliente (contexto, não implementação) | Deploy separado por cliente (on-premise/whitelabel), confirmado com o usuário | Multi-tenant em 1 deploy só (`idCompany`) | Cada cliente = infra própria (DB/Keycloak/Redis/registry distintos). Valida o mecanismo de env var por instalação: não depende de conectividade com serviço central, funciona até em cliente sem acesso à internet |
| Config Server (`spring-cloud-starter-config`, hoje `enabled: false`) | Não virar dependência desta ideia; placeholder+env var funciona com ou sem ele | Esperar decisão do Config Server antes de desenhar o mecanismo | Usuário confirmou que o Config Server "pode ser trocado e nem chegar em produção" — incerto. Placeholder+env var é compatível nos dois futuros: se o Config Server for ativado depois, ele resolve `scos.*` antes do default local (Spring aplica property sources externas antes do `application.yml` do próprio jar) e simplesmente sobrescreve; se for descartado, o mecanismo já funciona standalone |

### Banco de Dados
- **Impacto**: ❌ Não

---

## 4️⃣ Implementação

### Arquivos

**Modificados**:
- `scos-organization-boot/pom.xml` — bloco `<image>` no `spring-boot-maven-plugin`
- `scos-organization-boot/src/main/resources/application.yml` — topologia, portas, pool, toggles e tuning viram placeholder
- `scos-organization-boot/src/main/resources/bootstrap.yml` — `scos.registry.host`/`port`/`tls-enabled` + (revalidação) JPA diag, tracing.sampling, `scos.filter.*`, redis.timeout, tomcat viram placeholder
- `grpc/scos-organization-grpc-boot/pom.xml` — bloco `<image>` no `spring-boot-maven-plugin`
- `grpc/scos-organization-grpc-boot/src/main/resources/application.yml` — mesmo padrão do boot (sem jdempotent) + corrige `scos.cache.master`
- `grpc/scos-organization-grpc-boot/src/main/resources/bootstrap.yml` — (revalidação) JPA diag, tracing.sampling, `scos.filter.*` viram placeholder
- `scos-organization-boot/src/main/resources/logback-spring.xml` — (revalidação) `LOG_DIR`/`LOG_LEVEL` env + corrige appender `ScosJson`
- `grpc/scos-organization-grpc-boot/src/main/resources/logback-spring.xml` — idem

### Tarefas
- [ ] **T-01**: Configurar `<image>` no pom do `flow-organization-boot`
- [ ] **T-02**: Configurar `<image>` no pom do `flow-organization-grpc-boot`
- [ ] **T-03**: `application.yml` do `boot` — placeholder de topologia+porta (DB, Keycloak, Redis)
- [ ] **T-04**: `application.yml` do `boot` — placeholder de pool de conexão (principal + auditoria)
- [ ] **T-05**: `application.yml` do `boot` — placeholder de toggles e tuning fino (cache, jdempotent, privacy)
- [ ] **T-06**: Repetir T-03/T-04/T-05 em `application.yml` do `grpc-boot` (sem jdempotent)
- [ ] **T-07**: Corrigir `scos.cache.master` no `application.yml` do `grpc-boot`
- [ ] **T-08**: `bootstrap.yml` do `boot` — placeholder de `scos.registry.host`/`port`/`tls-enabled`
- [ ] **T-09**: Documentar tabela completa de properties por categoria (README ou doc própria), deixando claro que segredos são tratados em ideia separada (Jasypt)
- [ ] **T-10**: Validar build local (`mvn spring-boot:build-image`) nos 2 módulos, com e sem `bp.cds.enabled`
- [ ] **T-11**: Validar que subir local sem nenhuma env var setada reproduz exatamente o comportamento de hoje (regressão zero) — todas as propriedades, não só as de host
- [ ] **T-12** *(revalidação)*: Trocar split host+porta por env var de URL inteira em `datasource.url`, `audit.datasource.url`, `keycloak.issuer-uri`, `cache.sentinels` (RF-06 revisado); decidir schema/`default-schema` do Liquibase (fixo vs env acoplada)
- [ ] **T-13** *(revalidação)*: `logback-spring.xml` dos 2 módulos — `LOG_DIR`/`LOG_LEVEL` viram env (RF-14) e corrigir `ScosJson` FileAppender→RollingFileAppender (RF-15)
- [ ] **T-14** *(revalidação)*: `bootstrap.yml` dos 2 módulos — placeholder de diagnóstico JPA + tracing.sampling + `scos.filter.*` (RF-16, RF-17, RF-19)
- [ ] **T-15** *(revalidação)*: `bootstrap.yml` do `boot` — placeholder de redis.timeout + tomcat (RF-18)
- [ ] **T-16** *(revalidação)*: Gerar `.env.example` com TODAS as env vars e defaults (risco #9) — inclui validação `SCOS_DB_MIN_IDLE ≤ SCOS_DB_MAX_POOL_SIZE` (risco #11) e alerta strict+segredo-fake (risco #12)

### Riscos e Edge Cases
1. `spring-boot:build-image` baixa a imagem do builder na primeira execução — primeira run local é mais lenta que as seguintes (cache local do Docker reaproveita depois)
2. `jammy-tiny` run image não tem shell/curl/wget — nenhum `docker exec -it ... sh` funciona pra debug manual. Quem orquestrar a imagem (ex: runner do ambiente k6) precisa checar saúde via HTTP externo (`GET /actuator/health`), não via `HEALTHCHECK` nativo do compose
3. Fallback de build com/sem CDS dobra o tempo no pior caso (2 tentativas, uma falhando) — aceitável pra build local pontual, mas vale medir antes de assumir como padrão de todo build
4. `process-aot` já roda no pom (execução existente), mas não há confirmação de que o jar resultante é executado com `-Dspring.aot.enabled=true` hoje — validar isso antes de contar com o ganho combinado de AOT+CDS, senão o "melhor desempenho" esperado não se realiza de fato
5. `SCOS_DB_HOST` é reaproveitada entre `datasource.url` e `audit.datasource.url`, e entre `boot`/`grpc-boot` — setar em só um container faz o outro (ou a datasource de auditoria) continuar em `localhost` de dentro do container, gerando falha de conexão só parcial e mais difícil de diagnosticar
6. `scos.cache.master` errado no `grpc-boot` é bug pré-existente e independente desta mudança — hoje inofensivo só porque `cache.enable: false` nesse módulo; se alguém habilitar cache no `grpc-boot` sem saber disso, quebra
7. **Config Server incerto**: se alguém decidir ativar `spring.cloud.config.enabled: true` no futuro sem saber que os placeholders locais já cobrem o mesmo espaço, pode gerar confusão sobre "qual é a fonte de verdade" do hostname (Config Server vs env var local). Documentar essa relação (RF-07/T-07) evita retrabalho
8. Segredos (`datasource.password`, `cache.password`, `registry.key-access`, `privacy.crypto.secret`) continuam hardcoded e iguais em qualquer instalação até a ideia de Jasypt ser feita — isso **é** um bloqueio real de produção pro modelo multi-cliente confirmado, só não é resolvido aqui por ser mecanismo diferente (SRP). Não tratar como "só nota de rodapé": priorizar a ideia de segredos logo em seguida a esta
9. 34 env vars é bastante coisa pra uma instalação on-premise configurar de cabeça — sem um `.env.example` (ou equivalente) documentando todas de uma vez com os defaults, risco real de esquecer alguma e a app subir com comportamento parcialmente errado (ex: pool de auditoria não ajustado, só o principal)
10. `scos.cache.database`/`scos.cache.port` virarem placeholder é baixo risco, mas `scos.jdempotent.*` e `scos.privacy.max-payload-kb` são tuning fino que poucas instalações vão realmente precisar mudar — documentar isso deixa claro que "dá pra mudar" não é o mesmo que "precisa mudar", pra não gerar sobre-configuração desnecessária em instalação simples
11. Reduzir `SCOS_DB_MAX_POOL_SIZE` numa instalação pequena sem também revisar `SCOS_DB_MIN_IDLE` pode deixar `minimum-idle > maximum-pool-size`, erro de configuração do HikariCP na subida — vale validar essa relação (não só documentar os 2 como independentes)

#### Riscos adicionados na revalidação (2026-07-05)

12. **`SCOS_PRIVACY_STRICT=true` + segredo fake = boot falha.** Enquanto a ideia do Jasypt ([[config-jasypt-segredos-organization]]) não landar, `privacy.crypto.secret` é `ASDASDASDA` (fake). Ligar strict por env antes disso → "error if hash/encrypt has no key" na subida. Acoplamento entre esta ideia e a de segredos: documentar que `SCOS_PRIVACY_STRICT=true` só depois de chave real
13. **`LOG_DIR` relativo em container.** `logs` relativo resolve pro CWD do run image buildpack (`/workspace`) — logs efêmeros (morrem com container) e volume-mount imprevisível. Tornar env (RF-14) resolve o "não editar arquivo", mas o operador ainda precisa apontar `LOG_DIR` pra um caminho com volume, senão perde log. Alternativa a discutir no propose: em container, desligar os 2 FileAppender e deixar só Console (stdout) → orquestrador coleta (12-factor)
14. **`ScosJson` sem rotação (RF-15).** `application.json` cresce ilimitado hoje (FileAppender ignora rollingPolicy). Instalação on-premise long-running enche disco silenciosamente. Confirmar em runtime antes de assumir só pela leitura do XML
15. **Diag JPA ligado por default é perf/PII, mas mudar default quebra regressão-zero.** RF-16 mantém default `true` (comportamento de hoje) de propósito. Consequência: on-premise que não setar `SCOS_JPA_SHOW_SQL=false` continua logando todo SQL em prod. Documentar forte no `.env.example` que prod deve desligar — "default seguro pra regressão" ≠ "default bom pra prod"
16. **Schema na URL vs `default-schema` do Liquibase.** Com `SCOS_DB_URL` cobrindo `currentSchema=scos`, o schema vira configurável — mas `scos.liquibase.default-schema: scos` e `scos.audit.liquibase.default-schema: scos` (bootstrap.yml, boot) continuam fixos. Cliente que mude schema pela URL sem alinhar o Liquibase → migration no schema errado. Decidir no propose: schema é convenção fixa (e então NÃO expor na url) ou vira env acoplada aos 3 pontos

---

## 5️⃣ Revalidação (2026-07-05)

Releitura dos 4 yaml + 2 `logback-spring.xml` reais contra a v1. Objetivo: (a) achar item que dá pra externalizar e a v1 não cobriu; (b) achar item passando que não deveria; (c) olhar onde o log é salvo.

**Confirma da v1**: layout (raw `scos.*` em `application.yml`, bindings+registry em `bootstrap.yml`) correto; registry host/port/tls em `bootstrap.yml` correto; segredos fora do escopo (Jasypt) correto.

**Faltando (dá pra add) → RF-14..RF-19**:
- `LOG_DIR`/`LOG_LEVEL` (log fixo fura RNF-01) — headline do pedido
- Diag JPA `show_sql`/`generate_statistics`/slow-query (dev ligado em prod = perf+PII, estava mal-classificado como "arquitetura fixa")
- `tracing.sampling.probability` 1.0 (custo em prod)
- `redis.timeout`, `tomcat.max-connections/timeout` (tuning "infra pequena")
- `scos.filter.*` (toggle log HTTP)

**Passando com shape errado (não é item a mais, é corrigir o corte) → RF-06 revisado**:
- Split host+porta de `datasource.url`/`issuer-uri`/`sentinels` deixa **scheme `http`** (quebra Keycloak prod HTTPS), **db/schema, realm** cravados e impede **Sentinel HA**. Corrigido: URL-shaped → 1 env var da string inteira.

**Bug latente na área de log (não-env, mas revalidado) → RF-15**:
- `ScosJson` é `FileAppender` com `<rollingPolicy>` ignorado → `application.json` sem rotação, cresce ilimitado.

**Acoplamentos novos**: strict+segredo-fake=boot fail (#12), LOG_DIR relativo efêmero (#13), schema url vs Liquibase (#16).

---

## 📎 Referências
- `scos-organization-boot/src/main/resources/application.yml`, `bootstrap.yml`, `logback-spring.xml`
- `grpc/scos-organization-grpc-boot/src/main/resources/application.yml`, `bootstrap.yml`, `logback-spring.xml`
- `etc/infra/docker-compose-database.yml`, `docker-compose-keycloak.yml`, `docker-compose-redis.yml`
- [paketo-buildpacks/spring-boot#581](https://github.com/paketo-buildpacks/spring-boot/issues/581) — bug CDS+AOT Java 25 / Spring Boot 4.0.1
- `.claude/skills/spring-security-scos/references/resource-server.md` — convenção SCOS de Jasypt pra segredos ("issuer URIs, client secrets, and keys externalized (env) / Jasypt-encrypted")

---
