## ADDED Requirements

### Requirement: Regressão-zero sem env var

Toda property externalizada SHALL usar o padrão `${VAR:default}` com default idêntico ao valor de hoje. Subir qualquer módulo sem setar nenhuma env var MUST reproduzir o comportamento atual.

#### Scenario: Subir local sem env

- **WHEN** o módulo sobe sem nenhuma env var `SCOS_*`/`LOG_*` setada
- **THEN** cada property resolve para o valor hoje presente no `application.yml`/`bootstrap.yml`/`logback-spring.xml`
- **AND** o comportamento observável é idêntico ao atual

### Requirement: Endereços em forma de URL externalizados como string inteira

Properties cujo valor tem forma de URL SHALL ser externalizadas como uma única env var contendo a string completa, não fatiadas em host+porta. Isso cobre scheme, db, schema e realm, e preserva listas (Sentinel HA).

#### Scenario: Datasource principal e de auditoria

- **WHEN** `SCOS_DB_URL` e `SCOS_AUDIT_DB_URL` estão setadas
- **THEN** `scos.datasource.url` e `scos.audit.datasource.url` usam esses valores
- **AND** sem elas, ambas caem em `jdbc:postgresql://localhost:5432/scos?currentSchema=scos`

#### Scenario: Schema controlado por knob único

- **WHEN** `SCOS_DB_SCHEMA=cliente_x` está setada, com `SCOS_DB_URL` no default
- **THEN** o `currentSchema` da URL e o `default-schema` do Liquibase (principal e auditoria) usam `cliente_x`
- **AND** sem ela, todos usam `scos` (URL e Liquibase sempre com o mesmo schema)

#### Scenario: Keycloak issuer com scheme HTTPS

- **WHEN** `SCOS_KEYCLOAK_ISSUER_URI=https://kc.cliente/realms/Foo` está setada
- **THEN** o resource server e o client OAuth2 usam esse issuer, com `https` e realm próprios
- **AND** sem ela o default é `http://localhost:7080/realms/Scos`

#### Scenario: Redis Sentinel em alta disponibilidade

- **WHEN** `SCOS_REDIS_SENTINELS=h1:26379,h2:26379,h3:26379` está setada
- **THEN** `scos.cache.sentinels` recebe a lista completa de nós
- **AND** sem ela o default é `localhost:26379`

### Requirement: Registry e porta da aplicação externalizados

O host, a porta e o toggle de TLS do registry (`bootstrap.yml`, só `boot`), e a porta HTTP/gRPC da própria aplicação SHALL ser externalizados.

#### Scenario: Registry configurável

- **WHEN** `SCOS_REGISTRY_HOST`, `SCOS_REGISTRY_PORT`, `SCOS_REGISTRY_TLS_ENABLED` estão setadas
- **THEN** `scos.registry.host`/`port`/`tls-enabled` usam esses valores
- **AND** sem elas caem em `localhost`/`8090`/`false`

#### Scenario: Porta da app remapeável

- **WHEN** `SCOS_PORT` está setada
- **THEN** a app expõe essa porta
- **AND** sem ela o default é `8081` (boot) / `8090` (grpc-boot)

### Requirement: Pool de conexão externalizado (principal e auditoria)

Timeouts, min-idle e max-pool-size dos datasources principal e de auditoria SHALL ser externalizados nos 2 módulos, para ajuste por tamanho de instalação.

#### Scenario: Reduzir pool em instalação pequena

- **WHEN** `SCOS_DB_MAX_POOL_SIZE=10` e `SCOS_DB_MIN_IDLE=2` estão setadas
- **THEN** o HikariCP principal usa esses limites
- **AND** sem elas os defaults são `100`/`25`

#### Scenario: Pool de auditoria independente

- **WHEN** `SCOS_AUDIT_DB_MAX_POOL_SIZE` está setada
- **THEN** o datasource de auditoria usa esse limite, distinto do principal

### Requirement: Toggles e tuning operacional externalizados

Os toggles (`audit.enabled`, `cache.enable`, `liquibase.enabled`, `privacy.enabled`, `privacy.strict`, `jdempotent.enabled`, `registry.tls-enabled`) e o tuning fino (cache TTL/database, jdempotent timeouts, privacy max-payload) SHALL ser externalizados, respeitando os defaults por módulo.

#### Scenario: Default por módulo preservado

- **WHEN** nenhum toggle é setado
- **THEN** `cache.enable` resolve `true` no boot e `false` no grpc-boot
- **AND** `liquibase.enabled` resolve `true` no boot e `false` no grpc-boot

#### Scenario: jdempotent só existe no boot

- **WHEN** as env `SCOS_JDEMPOTENT_*` são documentadas
- **THEN** aplicam-se apenas ao `boot`, pois o grpc-boot não tem jdempotent

### Requirement: Diagnóstico JPA e observability externalizados

Os toggles de diagnóstico do Hibernate (`show_sql`/`format_sql`, `generate_statistics`, slow-query threshold) e o `tracing.sampling.probability` SHALL ser externalizados, mantendo o default de hoje. Estes deixam de ser tratados como configuração fixa de arquitetura.

#### Scenario: Desligar SQL logging em produção

- **WHEN** `SCOS_JPA_SHOW_SQL=false` está setada
- **THEN** o Hibernate não loga nem formata SQL
- **AND** sem ela o default é `true` (comportamento de hoje)

#### Scenario: Reduzir sampling de trace

- **WHEN** `SCOS_TRACING_SAMPLING=0.1` está setada
- **THEN** o tracing amostra 10% das requisições
- **AND** sem ela o default é `1.0`

### Requirement: Log HTTP e tuning extra externalizados

Os toggles de log de request/response (`scos.filter.*`), o timeout do Redis e o tuning do Tomcat (só boot) SHALL ser externalizados.

#### Scenario: Ligar log de request body para debug

- **WHEN** `SCOS_LOG_SHOW_REQUEST_BODY=true` está setada
- **THEN** o filtro loga o corpo da request (mascarado pelo privacy)
- **AND** sem ela o default é `false`

### Requirement: Diretório e nível de log externalizados

O diretório de gravação de log (`LOG_DIR`) e o nível do root logger (`LOG_LEVEL`) do `logback-spring.xml` SHALL ser externalizados nos 2 módulos, para permitir apontar o log a um caminho persistente sem editar arquivo.

#### Scenario: Apontar log para caminho persistente

- **WHEN** `LOG_DIR=/var/log/scos` está setada
- **THEN** os appenders de arquivo gravam nesse diretório
- **AND** sem ela o default é `logs`

#### Scenario: Ajustar nível de log

- **WHEN** `LOG_LEVEL=WARN` está setada
- **THEN** o root logger opera em WARN
- **AND** sem ela o default é `INFO`

### Requirement: Correções de configuração pré-existentes

O nome do master set do Sentinel no `grpc-boot` SHALL ser corrigido para o valor da infra padrão, e o appender de log JSON SHALL rotacionar de fato.

#### Scenario: cache.master corrigido no grpc-boot

- **WHEN** o `grpc-boot` habilita cache
- **THEN** `scos.cache.master` é `scos_master`, batendo com a infra padrão (não `inside_flow_master`)

#### Scenario: Log JSON rotaciona

- **WHEN** a aplicação roda por vários dias
- **THEN** `application.json` é rotacionado por tempo e limitado por `totalSizeCap`/`maxHistory`, sem crescer ilimitado

### Requirement: Credenciais externalizadas como env var com default

Credenciais e chaves (`datasource.username`/`password`, `audit.datasource.username`/`password`, `cache.password`, `registry.key-access`, `privacy.crypto.secret`) SHALL ser externalizadas como env var no padrão `${VAR:default}`, com default = valor de hoje, permitindo que cada instalação sobrescreva sua credencial sem rebuild. O default MUST preservar regressão-zero.

#### Scenario: Sobrescrever credencial por instalação

- **WHEN** `SCOS_DB_PASSWORD` e `SCOS_DB_USERNAME` estão setadas
- **THEN** o datasource principal usa essas credenciais
- **AND** sem elas caem no valor de hoje (`scos`/`scos#2026`)

#### Scenario: Chave de privacy por instalação

- **WHEN** `SCOS_PRIVACY_CRYPTO_SECRET` está setada
- **THEN** `scos.privacy.crypto.secret` usa esse valor
- **AND** sem ela cai no default de hoje

### Requirement: Segredo com default deve alertar risco de valor versionado

Como o default do segredo permanece no yaml versionado, a documentação (`.env.example` + doc de properties) SHALL alertar que ambientes de produção MUST setar as env vars de segredo e NÃO confiar no default. O hardening (remover default do git, cifra em repouso via Jasypt) permanece fora do escopo desta change.

#### Scenario: Documentação alerta sobre default versionado

- **WHEN** o `.env.example` é gerado
- **THEN** as env vars de segredo são marcadas como "obrigatório setar em produção — default é apenas de dev, versionado"
- **AND** apontam a change de Jasypt como hardening complementar
