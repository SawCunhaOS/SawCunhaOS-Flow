# SCOS — Infraestrutura de Desenvolvimento

Cada serviço é isolado em seu próprio `docker-compose` e compartilha a rede `scos_network`.

---

## Pré-requisitos

- Docker Engine 24+
- Docker Compose v2.20+

---

## Configuração Inicial

Copie o arquivo de variáveis e ajuste as senhas antes de subir qualquer serviço:

```bash
cp .env.example .env
# edite .env com as senhas adequadas ao ambiente
```

---

## Rede Compartilhada

Todos os composes declaram `name: scos_network`. O Docker cria a rede na primeira vez e os demais composes reutilizam automaticamente. Não é necessário criá-la manualmente.

---

## Serviços

### Banco de Dados — `docker-compose-database.yml`

| Item | Valor |
|------|-------|
| Imagem | `postgis/postgis:18-3.6-alpine` |
| Porta | `5432` |
| Host (inter-serviços) | `postgresql` |

**O que sobe:**
- PostgreSQL 18 com extensão PostGIS 3.6
- Script de inicialização automática (`20_install_database.sh`) executado na primeira vez, após o `10_postgis.sh` da imagem:
  - Cria extensões `uuid-ossp` e `postgis`
  - Cria schema `scos` com `search_path = scos, public` (inclui `public` para PostGIS types)
  - Cria role `scos_user`
  - Cria banco e role dedicados para o Keycloak

**Variáveis relevantes no `.env`:**

| Variável | Descrição |
|----------|-----------|
| `POSTGRES_USER` | Superusuário do PostgreSQL |
| `POSTGRES_PASSWORD` | Senha do superusuário |
| `POSTGRES_DB` | Banco principal da aplicação |
| `APP_DB_USER` | Usuário que a aplicação usa para conectar (pode ser igual a `POSTGRES_USER`) |
| `APP_DB_PASSWORD` | Senha do usuário da aplicação |
| `KC_DB_USER` | Role criado para o Keycloak |
| `KC_DB_PASSWORD` | Senha do role do Keycloak |
| `KC_DB_NAME` | Banco criado para o Keycloak |

```bash
docker compose -f docker-compose-database.yml up -d
```

---

### Keycloak — `docker-compose-keycloak.yml`

| Item | Valor |
|------|-------|
| Imagem | `quay.io/keycloak/keycloak:26.6.4` |
| Porta HTTP | `7080` |
| Porta HTTPS | `7443` |
| Admin UI (master) | `http://localhost:7080/admin` |

**O que sobe:**
- Keycloak em modo `start` (produção)
- Backend PostgreSQL no mesmo banco (`postgresql:5432`, banco `keycloak`)
- Realm `Scos` importado automaticamente via `--import-realm` na primeira inicialização

> **Dependência:** requer o banco de dados rodando (`docker-compose-database.yml`), pois conecta ao container `postgresql` via `scos_network`.

**Variáveis relevantes no `.env`:**

| Variável | Descrição |
|----------|-----------|
| `KC_DB_USER` | Usuário do banco Keycloak |
| `KC_DB_PASSWORD` | Senha do banco Keycloak |
| `KC_DB_NAME` | Nome do banco Keycloak |
| `KEYCLOAK_ADMIN` | Login do admin master do Keycloak |
| `KEYCLOAK_ADMIN_PASSWORD` | Senha do admin master do Keycloak |

```bash
docker compose -f docker-compose-keycloak.yml up -d
```

#### Acesso ao Admin Console

| Usuário | Senha inicial | Acesso |
|---------|--------------|--------|
| Valor de `KEYCLOAK_ADMIN` no `.env` | Valor de `KEYCLOAK_ADMIN_PASSWORD` | Master realm — gerencia todos os realms |
| `keycloak-admin` | `Keycloak@1234!` | Realm `Scos` — gerencia usuários e clients do realm |

> Todos os usuários são criados com senha temporária. **Na primeira autenticação o Keycloak exige troca obrigatória de senha.**

#### Realm `Scos` — Usuários padrão

| Usuário | Senha inicial | Finalidade |
|---------|--------------|-----------|
| `scos-admin` | `Admin@1234!` | Acesso administrativo via aplicação |
| `scos-api` | `Api@1234!` | Chamadas de API entre serviços |
| `keycloak-admin` | `Keycloak@1234!` | Gerência do realm no Keycloak Admin Console |

> Todas as senhas acima são temporárias. Troca exigida no primeiro login.

#### JWT — Claims retornados

O token de acesso emitido pelo client `Scos` contém apenas:

| Claim | Descrição |
|-------|-----------|
| `preferred_username` | Login do usuário |
| `name` | Nome completo |
| `email` | E-mail |
| `idCompany` | ID da empresa vinculada ao usuário (atributo customizado) |

Scopes padrão do client: `profile`, `email`, `web-origins`. Os scopes `roles` e `acr` foram removidos — o controle de permissões é responsabilidade da aplicação, não do Keycloak.

#### Obter token (Resource Owner Password Grant)

```bash
curl -s -X POST http://localhost:7080/realms/Scos/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=Scos" \
  -d "username=scos-admin" \
  -d "password=<nova_senha>" | jq .
```

---

### Redis — `docker-compose-redis.yml`

| Item | Valor |
|------|-------|
| Imagem Redis | `redis:8.8-alpine` |
| Imagem Sentinel | `redis:8.8-alpine` (modo sentinel via entrypoint) |
| Porta Redis | `6379` |
| Porta Sentinel | `26379` |
| Host (inter-serviços) | `redis` |
| Master Set (Sentinel) | `scos_master` |

**O que sobe:**
- Redis 8.0 standalone com senha obrigatória
- Redis Sentinel monitorando o master `redis:6379`
- Tuning de performance: `maxmemory 256mb`, política `allkeys-lru`, `io-threads 4`

**Variáveis relevantes no `.env`:**

| Variável | Descrição |
|----------|-----------|
| `REDIS_PASSWORD` | Senha de autenticação do Redis |

```bash
docker compose -f docker-compose-redis.yml up -d
```

---

### Monitoramento — `docker-compose-grafana.yml`

| Item | Valor |
|------|-------|
| Imagem Prometheus | `prom/prometheus:v3.12.0` |
| Imagem Grafana | `grafana/grafana:13.1.0` |
| Imagem Zipkin | `openzipkin/zipkin:3.6.1` |
| Porta Prometheus | `9090` |
| Porta Grafana | `3000` |
| Porta Zipkin | `9411` |

**O que sobe:**
- Prometheus coletando métricas das aplicações via `host.docker.internal` (funciona em Linux e macOS)
- Grafana com datasource Prometheus pré-configurado automaticamente via provisioning
- Grafana com volume persistente (`grafana_data`) — dados e dashboards sobrevivem a restarts
- Zipkin para rastreamento distribuído
- Grafana aguarda Prometheus estar healthy antes de subir (`depends_on`)

**Variáveis relevantes no `.env`:**

| Variável | Descrição |
|----------|-----------|
| `GRAFANA_ADMIN_PASSWORD` | Senha do admin do Grafana (usuário: `admin`) |

```bash
docker compose -f docker-compose-grafana.yml up -d
```

---

## Ordem de Inicialização

O banco de dados deve subir primeiro (cria a rede e o banco do Keycloak). Os demais podem subir em paralelo após o banco estar healthy.

```bash
# 1. Banco de dados (obrigatório primeiro)
docker compose -f docker-compose-database.yml up -d

# 2. Demais serviços (após o banco estar healthy)
docker compose -f docker-compose-keycloak.yml up -d
docker compose -f docker-compose-redis.yml up -d
docker compose -f docker-compose-grafana.yml up -d
```

Para verificar se o banco está pronto antes de continuar:

```bash
docker compose -f docker-compose-database.yml ps
# aguardar status "healthy" no container postgresql
```

---

## Parar os Serviços

```bash
docker compose -f docker-compose-database.yml down
docker compose -f docker-compose-keycloak.yml down
docker compose -f docker-compose-redis.yml down
docker compose -f docker-compose-grafana.yml down
```

Para remover volumes (apaga dados):

```bash
docker compose -f docker-compose-database.yml down -v
docker compose -f docker-compose-grafana.yml down -v   # remove grafana_data
```

---

## Endpoints de Saúde

| Serviço | URL |
|---------|-----|
| PostgreSQL | `pg_isready -h localhost -p 5432` |
| Keycloak | `http://localhost:9000/health/ready` |
| Prometheus | `http://localhost:9090/-/healthy` |
| Grafana | `http://localhost:3000/api/health` |
| Zipkin | `http://localhost:9411/health` |

---

## Configuração do Prometheus

Edite `prometheus/prometheus.yml` para adicionar os targets das aplicações:

```yaml
scrape_configs:
  - job_name: 'scos'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:8081']  # porta da aplicação no host
```

---

## Arquivos

```
etc/infra/
├── .env                          # Variáveis locais (não commitado)
├── .env.example                  # Template de variáveis
├── docker-compose-database.yml
├── docker-compose-keycloak.yml
├── docker-compose-redis.yml
├── docker-compose-grafana.yml
├── redis/
│   └── sentinel-entrypoint.sh    # Gera sentinel.conf com variáveis de ambiente e inicia sentinel
├── database/
│   ├── 20_install_database.sh    # Script de init do banco (roda após 10_postgis.sh da imagem)
│   └── postgresql.conf           # Configuração customizada do PostgreSQL
├── keycloak/
│   └── Scos_Realm.json           # Realm importado automaticamente no primeiro start
├── grafana/
│   └── provisioning/
│       └── datasources/
│           └── prometheus.yml    # Datasource Prometheus pré-configurado
└── prometheus/
    └── prometheus.yml            # Configuração de scraping do Prometheus
```
