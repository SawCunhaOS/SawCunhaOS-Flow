# Suíte de Testes k6 contra Ambiente Docker Completo (Funcional + Carga)

**Data**: 2026-07-05
**Status**: 🔄 Em Análise
**Tipo**: 🆕 Nova Feature

---

## ⚠️ Princípio SRP — Uma Funcionalidade por Ideia

> **Regra**: Uma ideia = uma funcionalidade. Features independentes → arquivos separados.

- **Nome da funcionalidade**: `suite-testes-k6-ambiente-integracao`
- **Resumo em uma frase**: Runner local que builda as imagens Docker do organization-boot e grpc-boot, sobe infra completa (Postgres, Keycloak, Redis) com dados base, e roda k6 (cenários funcionais + carga, REST e gRPC) contra esse ambiente real.
- **Depende de**: [[config-build-imagem-e-properties-docker]] — pom configurado (`<image>`) e properties documentadas via env var são pré-requisito consumido aqui, não implementado nesta ideia

**Checklist SRP**:
- [x] Esta ideia cobre exatamente uma funcionalidade
- [x] Não mistura features independentes no mesmo arquivo
- [x] O nome é específico (não genérico como "melhorar módulo X")

---

## 1️⃣ Visão

### Problema
Repositório hoje não tem nenhum teste (`src/test` = 0 arquivos em todo o projeto) nem pipeline CI. Não existe forma de validar fluxo de negócio ponta-a-ponta nem comportamento sob carga contra a topologia real (REST + gRPC + Keycloak + Postgres + Redis rodando juntos, como em produção).

### Objetivo
Runner bash reproduzível localmente que:
1. Builda imagens `scos-organization-boot` e `scos-organization-grpc-boot`
2. Sobe infra completa (reuso dos composes existentes de `etc/infra/` + novo compose de apps)
3. Popula `seed_data.sql` (dados base) após o schema (Liquibase roda no start da app)
4. Roda k6 — cenários funcionais (checks de contrato, REST e gRPC) e cenários de carga (thresholds p95/p99)
5. Derruba tudo com reset total (`down -v`) ao final, garantindo isolamento entre execuções

Critério de sucesso: `runner.sh` executa do zero ao fim sem intervenção manual além do `.env` já configurado (per `etc/infra/README.md`), e falha o processo se algum check/threshold k6 quebrar.

### Fora de Escopo
- **CI pipeline** — repositório não tem GitHub Actions/GitLab CI hoje; integrar isso é mudança futura separada
- **Observabilidade (Grafana/Prometheus/Zipkin) no ambiente k6** — k6 já gera summary/thresholds próprios; fica fora por agora
- **Testes unitários/testcontainers JVM** — assunto de outra ideia, não se mistura aqui

---

## 2️⃣ Requisitos

### Funcionais
- [ ] **RF-01**: Runner builda imagem `scos-organization-boot` via `mvn spring-boot:build-image`
- [ ] **RF-02**: Runner builda imagem `scos-organization-grpc-boot` via `mvn spring-boot:build-image`
- [ ] **RF-03**: Novo compose `etc/tests/k6/docker-compose-apps.yml` sobe as 2 imagens na rede `scos_network`
- [ ] **RF-04**: Runner sobe infra completa combinando `-f docker-compose-database.yml -f docker-compose-keycloak.yml -f docker-compose-redis.yml -f etc/tests/k6/docker-compose-apps.yml` (reuso dos composes existentes de `etc/infra/`, sem duplicar configuração)
- [ ] **RF-05**: Runner aguarda todos os containers ficarem prontos antes de seguir — infra (`postgresql`, `keycloak`, `redis`) via `HEALTHCHECK` nativo do compose; apps via polling HTTP (`GET /actuator/health`), já que a imagem usa builder `jammy-tiny` sem shell/curl (ver [[config-build-imagem-e-properties-docker]])
- [ ] **RF-06**: Runner executa `seed_data.sql` via `docker compose exec -T postgresql psql -U ... -f -` após o schema (Liquibase, automático no start do boot) e antes do k6
- [ ] **RF-07**: Scripts k6 organizados em `etc/tests/k6/functional/*.js` e `etc/tests/k6/load/*.js`
- [ ] **RF-08**: Cenários funcionais cobrem fluxo de negócio ponta-a-ponta via REST (empresa → funcionário → login → permissão) com checks rígidos
- [ ] **RF-09**: Cenário funcional gRPC usa `k6/net/grpc` carregando os `.proto` direto de `grpc/scos-organization-grpc-proto/src/main/proto` (`authority.proto`, `registry.proto`) — sem depender de server reflection
- [ ] **RF-10**: Cenários de carga usam executor `ramping-vus`/`constant-arrival-rate` com thresholds de p95/p99
- [ ] **RF-11**: Runner finaliza com `docker compose down -v` (reset total de volume — isolamento de dado entre execuções)
- [ ] **RF-12**: `etc/infra/keycloak/Scos_Realm.json` — remover `temporary`/`requiredActions: ["UPDATE_PASSWORD"]` do usuário `scos-api` (usado pelo k6 pra obter token). `scos-admin` mantém como está
- [ ] **RF-13**: `docker-compose-apps.yml` define, pros 2 containers de app, os env vars documentados em [[config-build-imagem-e-properties-docker]] (`SCOS_DATASOURCE_URL`, `SCOS_AUDIT_DATASOURCE_URL`, `SCOS_SECURITY_KEYCLOAK_ISSUER_URI`, `SCOS_CACHE_SENTINELS`) apontando pros hostnames do compose (`postgresql`, `keycloak`, `redis`)
- [ ] **RF-14**: `docker-compose-apps.yml` referencia as imagens `scos-organization-boot:local` / `scos-organization-grpc-boot:local` geradas por [[config-build-imagem-e-properties-docker]], e define `mem_limit` explícito pros 2 containers (valor combinado com `BPL_JVM_THREAD_COUNT` daquela ideia)

### Não-Funcionais
- [ ] **RNF-01**: Execução 100% local por enquanto (sem dependência de CI)
- [ ] **RNF-02**: Script k6 obtém token JWT do Keycloak (Resource Owner Password Grant) no `setup()` e reusa entre iterações/VUs

---

## 3️⃣ Arquitetura

### Componentes Afetados
```
etc/tests/k6/ (novo)
├── runner.sh                      — orquestra build → up → wait → seed → k6 → down
├── docker-compose-apps.yml        — sobe scos-organization-boot + scos-organization-grpc-boot
├── functional/
│   ├── organization-flow.test.js  — fluxo negócio REST (empresa→funcionário→login→permissão)
│   └── grpc-flow.test.js          — chamadas unary/streaming via k6/net/grpc
└── load/
    └── organization-load.test.js  — ramping-vus/constant-arrival-rate + thresholds
```

### Fluxo Principal
```
mvn spring-boot:build-image (boot + grpc-boot)
  → docker compose up -d (db + keycloak + redis + apps)
  → wait healthy (todos containers)
  → seed_data.sql via psql exec (dados base)
  → k6 run (scenarios funcionais REST+gRPC, scenarios carga)
  → docker compose down -v (reset total)
```

### Decisões Técnicas
| Decisão | Escolha | Alternativa Descartada | Motivo |
|---------|---------|------------------------|--------|
| Onde roda | Local (`docker compose` manual) | CI pipeline | Repo não tem CI hoje (nenhum `.github/workflows` ou `.gitlab-ci`); CI fica pra mudança futura |
| Isolamento de dado | Reset total (`down -v`) por run | Dado único por run (UUID/timestamp) | Mais previsível, evita colisão de UK sem lógica extra nos scripts k6 |
| gRPC no k6 | `k6/net/grpc` carregando `.proto` direto | Server reflection | `grpc-boot` não tem reflection-service configurado; carregar `.proto` é mais simples e não exige mudança no serviço |
| Reuso de compose | Múltiplos `-f` (reusa composes existentes) | Compose novo standalone duplicando tudo | Evita duplicar config de db/keycloak/redis já mantida em `etc/infra/` |
| Execução do seed | `docker compose exec psql -f -` | `psql` client no host | Sem dependência nova na máquina; reusa container já healthy |
| Observabilidade | Fora de escopo por agora | Incluir Grafana/Prometheus/Zipkin | k6 já gera summary/thresholds próprios; simplifica escopo inicial. Também evita `down -v` remover `grafana_data` por engano |
| Localização dos scripts | `etc/tests/k6/` | `etc/infra/k6/` | Sinaliza suíte de teste, não infra de dev |
| Estrutura dos scripts | Pastas separadas `functional/` e `load/` | 1 arquivo com múltiplos `scenarios` | Mais organizado se a suíte crescer |
| Objetivo do k6 | Funcional + carga (ambos) | Só um dos dois | Mesmo runner cobre validação de contrato e teste de carga contra a topologia real |
| Login k6 no Keycloak | Remover `temporary`/`UPDATE_PASSWORD` do `scos-api` no realm JSON | Runner resetar senha via Admin API / usuário dedicado novo | `scos-api` já é o usuário semântico "chamadas de API entre serviços" (per `etc/infra/README.md`); mais simples que criar usuário novo ou automatizar reset via Admin API |

> Decisões sobre **build de imagem** (buildpacks, escopo de módulos, nome/builder/CDS/memória) e **externalização de properties** (env vars, relaxed binding) foram movidas pra [[config-build-imagem-e-properties-docker]] — essa ideia só consome o resultado (imagem pronta + env vars documentados).

### Banco de Dados
- **Impacto**: ❌ Não
- Usa `seed_data.sql` já existente (`etc/database/seed_data.sql`), sem migration nova

---

## 4️⃣ Implementação

### Arquivos

**Novos**:
- `etc/tests/k6/runner.sh` — orquestra build→up→wait→seed→k6→down
- `etc/tests/k6/docker-compose-apps.yml` — sobe as 2 imagens de app na rede `scos_network`
- `etc/tests/k6/functional/organization-flow.test.js` — fluxo negócio REST
- `etc/tests/k6/functional/grpc-flow.test.js` — chamadas gRPC via `k6/net/grpc`
- `etc/tests/k6/load/organization-load.test.js` — cenário de carga
- `etc/tests/k6/README.md` — como rodar localmente

**Modificados**:
- `etc/infra/keycloak/Scos_Realm.json` — RF-12 (login `scos-api` sem `UPDATE_PASSWORD`)

### Tarefas
- [ ] **T-01**: Criar `runner.sh`
- [ ] **T-02**: Criar `docker-compose-apps.yml`
- [ ] **T-03**: Criar cenário funcional REST
- [ ] **T-04**: Criar cenário funcional gRPC
- [ ] **T-05**: Criar cenário de carga
- [ ] **T-06**: Documentar em README como rodar localmente
- [ ] **T-07** (futuro, fora de escopo): integrar em CI quando pipeline existir

### Riscos e Edge Cases
1. `seed_data.sql` depende do realm Keycloak já importado com `EXTERNAL_ID` fixos (`scos-admin`, `scos-api`) — se a ordem de subida errar (seed antes do Keycloak estar healthy), lookup/FK falha
2. `k6/net/grpc` exige que `authority.proto`/`registry.proto` não importem nada fora do módulo proto sem caminho resolvível — checar imports antes de escrever o cenário
3. Se Grafana subir junto por engano, `down -v` remove `grafana_data` também — reforça decisão de não incluir Grafana no compose do k6
4. Token JWT pode expirar em runs de carga longos — script k6 precisa refresh ou token com TTL suficiente pro `setup()`
5. **Atenção (segurança)**: `Scos_Realm.json` é o mesmo arquivo usado pelo `docker-compose-keycloak.yml` de dev normal (não é um realm exclusivo do k6). Remover `temporary`/`UPDATE_PASSWORD` do `scos-api` afeta qualquer ambiente que importe esse realm, não só o ambiente k6 — senha `Api@1234!` passa a valer permanentemente sem forçar troca. Validar se isso é aceitável antes de aplicar, ou avaliar duplicar o realm só pro ambiente de teste

> Riscos específicos de build de imagem (buildpacks, CDS/AOT, builder sem shell) e de properties (relaxed binding) estão em [[config-build-imagem-e-properties-docker]].

---

## 📎 Referências
- `etc/infra/README.md` — composes existentes, ordem de inicialização, endpoints de saúde
- `etc/database/seed_data.sql` — dados base
- `grpc/scos-organization-grpc-proto/src/main/proto/` — `authority.proto`, `registry.proto`

---
