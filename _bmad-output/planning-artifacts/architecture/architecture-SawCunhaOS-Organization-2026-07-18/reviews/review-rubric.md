---
review-of: _bmad-output/planning-artifacts/architecture/architecture-SawCunhaOS-Organization-2026-07-18/ARCHITECTURE-SPINE.md
type: rubric-walk
reviewer: rubric-walker (subagent)
date: '2026-07-18'
verdict: needs-revision
---

# Rubric Walk — Architecture Spine (SawCunhaOS Motor Fundacional, Etapa 1/P0)

## Veredito

A spine acerta o mecanismo nos dois pilares onde ela de fato se aprofunda (Kill Switch e
bloqueio de turno) e ratifica corretamente boa parte do brownfield (paradigma DDD em
camadas, `Employee.supervisor`, `fn_block_delete`, stack/versões). Mas ela **reivindica
cobertura que não entrega**: o frontmatter (`binds: ['FR-1..FR-13', ...]`) e o próprio
`scope` incluem Company/Department/Position/Employee/Login/Profile, porém nenhuma FR de
FR-1 a FR-8 recebe AD, linha na Capability Map ou menção em Deferred — incluindo pontos
com risco real de divergência (cascata de `disable` na hierarquia de Company, classificação
de Perfil isento que a própria AD-2/AD-3 depende para funcionar). Pior: uma das seis ADs
(AD-6) se apoia numa premissa que o código contradiz hoje (a view de autoridade não soma
Perfil adicional), e a Design Paradigm describe "boot como único módulo empacotado" quando
o repo já tem dois módulos executáveis (`flow-organization-boot` e `server-fat`, este
último adicionado no mesmo dia da spine). Nenhum desses achados é fatal para os dois ADs
centrais, mas juntos eles deixam builders livres para divergir exatamente nos pontos que
uma spine existe para fechar.

## Achados por severidade

- **Crítico:** 0
- **Alto:** 5
- **Médio:** 3
- **Baixo:** 2

Total: 10 achados. Ver detalhamento abaixo, organizado pelas 7 perguntas do checklist.

---

## 1. Resolve os pontos reais de divergência, sem deixar nenhum de fora?

### [ALTO] FR-1 a FR-8 estão no `scope`/`binds` mas não têm nenhum AD, linha de Capability Map ou entrada em Deferred

`ARCHITECTURE-SPINE.md:7` (scope) e `:11` (`binds: ['FR-1..FR-13', 'FR-23..FR-28']`)
incluem explicitamente "Company/Department/Position/Employee/Login/Profile". Mas os 6 ADs
(`:45-95`), a Capability Map (`:137-145`) e o Deferred (`:147-153`) só endereçam FR-9/10/12
(turno), FR-13 (Kill Switch) e FR-23-28 (aprovação). FR-1 a FR-8 não aparecem em lugar
nenhum do corpo da spine — nem como AD, nem como "decidido pelo paradigma já ratificado",
nem como pergunta em aberto. Isso importa porque pelo menos duas dessas FRs têm risco real
de divergência arquitetural, não são só CRUD repetindo o padrão já ratificado:

- **FR-2** (regra de `disable` bloqueada por qualquer filial `ACTIVE` em **qualquer nível**
  abaixo, não só direta) exige um mecanismo de travessia descendente da hierarquia de
  `Company` que **não existe ainda no código**. Verifiquei
  `organization/flow-organization-domain/.../company/service/CompanyServiceBean.java:230-238`
  — o único método de travessia hoje (`depthOf`) sobe pela cadeia via `parentCompany`, não
  desce para filhos. É uma decisão nova de arquitetura (CTE recursiva no Postgres já em uso
  vs. travessia recursiva em Java, potencialmente N+1) — do mesmo tipo que motivou AD-2/AD-3
  — e a PRD (`prd.md:213`) registra que essa regra já foi corrigida duas vezes por lacunas
  de edge-case, ou seja, é área historicamente propensa a divergência.
- **FR-4** (Jornada de Trabalho) é citada na própria PRD (`prd.md:491`, seção de Riscos)
  como **dependência bloqueadora** de FR-9/10/12 — a mesmíssima Governança de Turno que
  AD-2/AD-3 decidem. A spine nunca menciona `EmployeeWorkSchedule`/`PositionWorkSchedule`
  nem a validação cronológica (`startTime < lunchStart < lunchEnd < endTime`). Como o Filter
  de AD-2 depende inteiramente desses dados existirem e estarem corretos, essa omissão é
  uma lacuna direta no pilar que a spine mais desenvolve.
- **FR-8** (classificação de Perfil como "sujeito a bloqueio de turno" vs. "acesso
  irrestrito") é o dado de entrada que decide se o Filter de AD-2 age ou não — e não é
  mencionado nem no `binds`, nem em nenhuma Rule, nem na Capability Map. Ver também o
  achado de AD-6 abaixo: essa classificação provavelmente atravessa o mesmo mecanismo de
  view de autoridade que está comprovadamente quebrado para Perfil adicional.

**Sugestão:** para cada FR de FR-1 a FR-8, decidir explicitamente "segue o paradigma já
ratificado, sem AD nova" (e dizer isso) ou escrever a AD/Deferred/pergunta em aberto
correspondente. Pelo menos FR-2, FR-4 e FR-8 parecem merecer tratamento explícito.

### [ALTO] FR-14 (Etapa 1 P0, mesma feature de FR-13) não está no `binds` nem em nenhum AD

`prd.md` §4.3 (Kill Switch, **Etapa 1 P0**) contém FR-13 **e** FR-14 ("Reativação não
restaura sessões antigas"). O `binds` da spine (`ARCHITECTURE-SPINE.md:11`) lista só
`FR-1..FR-13`, cortando FR-14 do meio da mesma feature P0. Nenhuma AD, nenhuma linha da
Capability Map, nenhum Deferred menciona FR-14. Na prática, o mecanismo de AD-1 (denylist
Redis com TTL) provavelmente já satisfaz FR-14 por construção (reativar não limpa a entrada
do denylist, e o token antigo já expirou de qualquer forma) — mas a spine nunca faz essa
ligação explícita, deixando um builder implementando `enable`/`unblock` livre para achar
que precisa limpar o denylist manualmente (o que quebraria FR-14).

---

## 2. A Rule de cada AD é aplicável de verdade e previne a divergência descrita?

### [ALTO] AD-1 não define o comportamento fail-closed quando o próprio Redis (denylist) está inacessível

`ARCHITECTURE-SPINE.md:49` define a Rule só para o caminho feliz (denylist consultado no
filtro) e deixa explícito que Keycloak nunca é o mecanismo de bloqueio. Mas a PRD é
enfática e repetida (`prd.md:377`, FR-13; `prd.md:145`, UJ-15) que **toda validação de
token que não conseguir confirmar o estado deve falhar fechado** — inclusive quando o
próprio mecanismo de checagem (aqui, o Redis) está inacessível na entrada. A Rule de AD-1
não diz isso. Como o denylist passa a ser consultado em **toda** requisição autenticada do
sistema (não só nas relacionadas a desligamento), essa omissão tem peso operacional real:
torna o Redis uma dependência síncrona obrigatória para 100% do tráfego autenticado, e sem
a Rule explícita de fail-closed, um builder pode razoavelmente escolher fail-open num
timeout de Redis (escolha pragmática comum) — violando diretamente o Zero Trust da PRD.
Reforça a lacuna: `etc/infra/redis/sentinel-entrypoint.sh` mostra que HA de Redis
(Sentinel) já é uma preocupação operacional viva neste monorepo, o que só aumenta a
importância de a Rule declarar o comportamento sob falha do próprio Redis.

**Sugestão:** adicionar à Rule de AD-1: "se a consulta ao denylist falhar/expirar (Redis
inacessível), a requisição é negada (fail-closed) — nunca liberada por incerteza."

### [ALTO] FR-11 (auditoria da decisão de turno) tem mecanismo ambíguo/contraditório entre a spine e a PRD

A tabela de Consistency Conventions (`ARCHITECTURE-SPINE.md:103`) cita "log de turno" como
uma das "tabela[s] de histórico/auditoria **nova[s]** desta Etapa" que precisa de trigger
`fn_block_delete`. Isso implica uma tabela dedicada nova. Só que:
- A linha "Novas tabelas desta Etapa" (`:102`) só lista `SCOS_LOGIN_APPROVAL_REQUEST` e
  `DEPARTMENT.MANAGER_ID` — sem nenhuma tabela de log de turno.
- O Structural Seed (`:121-135`) não tem nenhuma entidade para esse log.
- A PRD (`prd.md:499`) diz o oposto: FR-11 "usa o padrão de auditoria já estabelecido pela
  Fundação (`@Auditable`, tabelas `*_STATUS_HISTORY`) — extensão do padrão existente ...
  não infraestrutura nova."

O problema de fundo é que `@Auditable` (confirmado via skill `scos-audit-config`: "trilha
de auditoria via `@Auditable` + listener Hibernate") é um mecanismo acoplado a eventos de
persistência JPA (leitura/escrita de entidade) — não encaixa naturalmente numa decisão de
Filter que permite/nega uma chamada a um endpoint fora deste módulo (ex.: `GET
/v1/subscribers/{id}`, citado em UJ-1). A spine nunca resolve qual das duas leituras vale,
nem qual é o mecanismo concreto de persistência do log de turno — uma lacuna real no
próprio pilar (AD-2/AD-3) que a spine mais desenvolve, e que alimenta a métrica primária
SM-2.

### [MÉDIO] AD-4 não modela o caminho de "cancelamento automático" de FR-28 no diagrama de estados

O `stateDiagram-v2` de AD-4 (`ARCHITECTURE-SPINE.md:69-83`) modela escalonamento →
Aprovado/Rejeitado para os 3 níveis, mas FR-28 (`prd.md:317`) define uma política
**diferente**: a solicitação de retorno automático de férias/licença é **cancelada**
automaticamente após N dias úteis (não escala indefinidamente). O texto de AD-4
(`:67`) e o Capability Map (`:145`) mencionam FR-28 como coberto por AD-4, mas o diagrama —
que é o artefato mais visual e fácil de copiar por um builder — não tem esse terceiro
desfecho. Risco baixo-médio de um builder implementar escalonamento indefinido também para
o caso de licença/férias, contrariando a assimetria que a própria PRD (`prd.md:277`)
chama de "diferença deliberada."

---

## 3. Nada em Deferred deixaria dois builders divergirem sem perceber?

O Deferred (`ARCHITECTURE-SPINE.md:147-153`) em si é enxuto e coerente com o que a PRD já
resolveu (Geotemporal/Notificação em etapas futuras; "1 dia útil = 1 dia corrido"; envelope
de infra já provisionado — todos verificados como factualmente corretos, ver seção 4). O
problema não é o que está em Deferred, é o que **deveria estar lá e não está** — os achados
de FR-2/FR-4/FR-8/FR-11/FR-14 acima são exatamente lacunas que, por não aparecerem nem como
AD nem como Deferred, ficam impossíveis de distinguir de "decidido e resolvido" por quem lê
a spine rapidamente. Não há um achado adicional aqui além dos já listados; a lista acima
cobre este critério.

---

## 4. Tecnologia nomeada está atual/verificada, ou é reaproveito claro?

Todas as afirmações de Stack (`ARCHITECTURE-SPINE.md:107-119`) foram verificadas contra o
repo real e **conferem**:

- Java 25, MapStruct 1.6.3, `scos-bom:1.3.1` — confirmados em `pom.xml:91,95,31-32` (raiz).
- PostgreSQL 18+ — confirmado em `etc/infra/database/Dockerfile:1-8`
  (`ghcr.io/pgmq/pg18-pgmq:v1.10.0` + `postgresql-18-postgis-3` + `postgresql-18-cron`,
  este último explicando o refresh via `pg_cron` citado na PRD).
- Redis via jDempotent "já em uso" — confirmado: dependência
  `scos-foundation-jdempotent` presente em
  `organization/flow-organization-boot/pom.xml:74` e propriedades `jdempotent.*` em
  `application.yml:50-57`; infraestrutura Redis (incl. Sentinel) já provisionada em
  `etc/infra/docker-compose-redis.yml` e `etc/infra/redis/sentinel-entrypoint.sh`.
- Keycloak "já integrado" — confirmado, pacote
  `br.com.sawcunhaos.security.starter.keycloak` com `WebSecurityConfig`,
  `JwtAuthConverter` etc.
- Liquibase em `flow-organization-resources` — confirmado (changelogs, triggers, views
  reais encontrados em `organization/flow-organization-resources/src/main/resources/db/changelog/organization/`).

Nenhuma tecnologia nova foi introduzida ou mal identificada. Este critério passa sem
ressalvas.

---

## 5. RATIFICA o código brownfield existente, em vez de contradizê-lo?

### [ALTO] AD-6 se apoia numa premissa que o código contradiz: Perfil adicional não é resolvido pela view de autoridade

AD-6 (`ARCHITECTURE-SPINE.md:91-95`) decide que `APPROVE_SYSTEM_ACCESS` é "atribu[ído]
(principal ou adicional) diretamente aos Logins escolhidos", reusando o "modelo de
permissão já existente." Isso presume que atribuir como Perfil **adicional** funciona igual
a atribuir como principal. Verifiquei o código-fonte da view de autoridade:

- `organization/flow-organization-resources/.../view/vw_login_context.sql:54-61` faz
  `JOIN scos.scos_profile p ON p.profile_id = l.profile_id` — ou seja, só o Perfil
  **principal** do Login (`SCOS_LOGIN.PROFILE_ID`). A tabela `SCOS_LOGIN_PROFILE`
  (Perfis adicionais, entidade `LoginProfile.java` confirmada em
  `organization/flow-organization-domain/.../profile/internal/LoginProfile.java`) **nunca**
  é referenciada nessa view.
- `vw_authority_response.sql:30-53` agrega `permission` estritamente a partir de
  `vw_login_context` — herda a mesma lacuna.

Ou seja: hoje, se `APPROVE_SYSTEM_ACCESS` for atribuído como Perfil **adicional** (o próprio
texto da AD-6 permite essa opção), a permissão **não aparece** em `vw_authority_response`
— e qualquer checagem de autorização que dependa dessa view falharia silenciosamente. Isso
não é hipotético: a própria PRD (`prd.md:247`) já registra esse bug como conhecido ("Perfis
adicionais hoje não somam permissões na view de autoridade ... correção de view necessária
antes de qualquer feature que dependa de perfil adicional funcionar corretamente"). A spine
não menciona essa dependência/pré-requisito em lugar nenhum — nem como AD, nem em Deferred,
nem como risco. Isso também contamina indiretamente FR-8 (ver achado da seção 1): se a
classificação "isento de bloqueio de turno" também passar por essa mesma view, o mesmo bug
se aplica lá.

**Sugestão:** ou (a) AD-6 restringe `APPROVE_SYSTEM_ACCESS` a atribuição só como Perfil
**principal** até a view ser corrigida, ou (b) a correção da view vira pré-requisito
explícito (Deferred ou nova AD) antes de qualquer story que dependa de Perfil adicional.

### [ALTO] "boot ... único módulo empacotado" já não é verdade no repo atual

A Design Paradigm (`ARCHITECTURE-SPINE.md:26`) afirma que `boot` é "o composition root,
Liquibase, **único módulo empacotado**." Verifiquei que isso não é mais verdade:

- `organization/flow-organization-boot/pom.xml` empacota um Spring Boot executável
  (`spring-boot-maven-plugin`, classe main `ScosOrganizationApplication.java`,
  `finalName=ScosOrganizationApplication`, build de imagem `flow-organization-boot:local`).
- `server-fat/pom.xml` **também** empacota um Spring Boot executável independente (classe
  main `ScosFlowApplication.java`, `finalName=ScosFlowServerApplication`, e — por
  coincidência ou cópia — a mesma tag de imagem `flow-organization-boot:local`), depende
  diretamente de `flow-organization-domain/usecase/api/infrastructure/resources`,
  contornando `flow-organization-boot` por completo.
- `server-fat` foi adicionado no commit `7e9925d` ("Adicionando o application server Fat"),
  **no mesmo dia** em que a spine foi escrita (2026-07-18), pouco antes do commit mais
  recente (`8bebf7f`).

Ou seja: existem hoje **dois** módulos empacotáveis/executáveis concorrentes, e a spine não
menciona `server-fat` em lugar nenhum — nem para ratificá-lo como o novo composition root
único (substituindo `flow-organization-boot`), nem para explicar a convivência dos dois.
Isso é uma contradição concreta e verificável entre a spine e o estado real do repo no
mesmo dia da spine, e deixa em aberto uma pergunta estrutural real: onde um builder deve
registrar o novo `ShiftEnforcementFilter`/beans da Etapa 1 — em `flow-organization-boot`
(como o paradigma da spine sugere) ou em `server-fat` (o módulo mais recente, cujo nome
sugere ser o destino final unificado)?

### [MÉDIO] Nome do enum de status do Login citado errado (`StatusLogin` vs. `LoginStatus` real)

AD-4 (`ARCHITECTURE-SPINE.md:67`) e o Structural Seed (`:126`) chamam o enum de status do
Login de "`StatusLogin`". O nome real, verificado em
`organization/flow-organization-domain/.../login/internal/LoginStatus.java:16`, é
**`LoginStatus`** (valores `ACTIVE`, `INACTIVE`, `BLOCKED`). É um erro compreensível — os
enums de Company e Employee de fato seguem o padrão `Status<Entidade>`
(`StatusCompany`, confirmado em `CompanyServiceBean.java:37`; `StatusEmployee`, confirmado
em `Employee.java:92`) — mas Login quebra essa convenção no próprio código, e a spine
herdou a convenção errada exatamente para a entidade que ela está modificando. Baixo risco
de execução (o compilador pega na hora), mas é uma imprecisão factual sobre o brownfield
que a spine deveria ter verificado, já que está prescrevendo uma mudança nesse exato tipo.

**Todas as demais ratificações de brownfield foram verificadas e conferem** (ver seção 7,
"O que já está bom").

---

## 6. Cobre as capacidades do PRD que a guiou?

Cobertura ponto a ponto do PRD (`prd.md`) contra a spine:

| Seção PRD | Etapa | Coberto pela spine? |
| --- | --- | --- |
| §4.1 FR-1/2 (Company hierarquia/ciclo de vida) | P0 | **Não** — ver achado seção 1 |
| §4.1 FR-3 (Department/Position) | P0 | Não precisa de AD (✅ já implementado, sem gap) |
| §4.1 FR-4 (Work Schedule) | P0 | **Não** — ver achado seção 1 |
| §4.1 FR-5 (Employee lifecycle) | P0 | Não coberto, mas parece CRUD seguindo paradigma já ratificado — risco baixo |
| §4.1 FR-6 (Login-Keycloak Saga) | P0 | Implicitamente ratificado (mecanismo já existe) — ok |
| §4.1 FR-7 (Perfil/Recurso) | P0 | **Não** — e o bug de view citado pela própria PRD contamina AD-6 (seção 5) |
| §4.1 FR-23-28 (aprovação) | P0 | Coberto (AD-4, AD-5, AD-6) |
| §4.2 FR-8 (classificação de Perfil) | P0 | **Não** — ver achado seção 1 |
| §4.2 FR-9/10/12 (bloqueio de turno) | P0 | Coberto (AD-2, AD-3) |
| §4.2 FR-11 (auditoria de turno) | P0 | Parcial/ambíguo — ver achado seção 2 |
| §4.3 FR-13 (Kill Switch) | P0 | Coberto (AD-1) |
| §4.3 FR-14 (não restaura sessão) | P0 | **Não** — ver achado seção 1 |
| §4.4/§4.5 (Geotemporal/Notificação) | P1/P2 | Corretamente Deferred |

A spine cobre bem o "núcleo novo" que a PRD pediu para essa rodada de Arquitetura (Kill
Switch e Turno, que eram os itens com "decisão de arquitetura" explicitamente marcada no
`addendum.md:35-39`) — isso é o trabalho mais difícil e ela faz bem. Mas ela também
declara, no seu próprio `scope`/`binds`, cobertura de toda a Fundação de Identidade (FR-1 a
FR-8), e nessa parte fica devendo.

---

## 7. Toda dimensão que essa altitude deveria decidir foi decidida/deferida/virou pergunta aberta?

### Envelope operacional (deploy/ambientes/infra/operação) — checagem especial

O Deferred (`ARCHITECTURE-SPINE.md:153`) trata o envelope de deploy como "já provisionado
... Etapa 1 não introduz infraestrutura nova, só reaproveita." **Isso é verificável e
correto**: `etc/infra/` já tem `docker-compose-redis.yml`, `docker-compose-keycloak.yml`,
`docker-compose-database.yml`, `docker-compose-grafana.yml`, e o Dockerfile do Postgres já
está fixado em uma tag verificada (não `latest`). Não é uma lacuna silenciosa — é uma
deferência justificada e correta.

Duas ressalvas, já cobertas como achados formais acima, mas que pertencem estritamente a
este eixo "operação":
- O comportamento de fail-closed do **Redis em si** (não just do Kill Switch) sob
  indisponibilidade não é declarado (achado ALTO, seção 2) — isso é operação, não só
  mecanismo de feature.
- A existência de **dois** módulos empacotáveis concorrentes (achado ALTO, seção 5) é
  literalmente uma dimensão de "quem é a unidade de deploy" que ficou sem decisão, sem
  menção, e sem pergunta em aberto — exatamente o tipo de silêncio que este critério do
  checklist pede para destacar.

Fora do envelope operacional, não há outras dimensões de "altitude initiative" claramente
silenciadas além das já listadas (FR-1/2/4/8/11/14).

---

## O que já está bom (preservar)

- **AD-1 (Kill Switch)**: mecanismo bem escolhido e corretamente fundamentado — reaproveita
  Redis que já está no classpath/infra (jDempotent), evita reintroduzir round-trip síncrono
  ao Keycloak no caminho quente, e a "Prevents" é concreta e verificável.
- **AD-3 (fronteira starter vs. app)**: é o tipo exato de decisão que uma spine deve conter
  — previne uma classe real de erro (lógica de negócio vazando pra lib compartilhada) com
  uma regra binária e fácil de auditar (`grep` por pacote).
- **AD-5 (`Department.managerId` como FK direta)**: verificado — `Employee.supervisor` é
  de fato uma self-FK simples (`Employee.java:94-96`, `SUPERVISOR_ID`), então o precedente
  citado é real, não inventado, e a decisão de espelhar o mesmo padrão é sólida.
  economia de mecanismo genuína.
- **Convenção de trigger `fn_block_delete`**: verificado contra os changelogs reais —
  `trg_block_delete_reason_*`, `trg_block_delete_address_type`, `_contact_type`,
  `_outbox_topic` existem; nenhum `trg_block_delete_*_status_history` existe. A spine
  descreve essa lacuna com precisão cirúrgica.
- **Seção Stack inteira**: 100% verificada contra `pom.xml`, Dockerfiles e YAMLs reais (ver
  seção 4) — nenhuma tecnologia inventada ou desatualizada.
- **Paradigma DDD em camadas**: a descrição de domain/usecase/api/infrastructure/boot e a
  regra de "api nunca vê domain" batem com o código lido (Company/Employee/Login/Department).

---

## Metodologia

Verificação feita lendo o PRD completo (`prd.md`, 580 linhas) e o `addendum.md`, e
inspecionando diretamente no repositório: `Employee.java`, `Department.java`, `Login.java`,
`LoginStatus.java`, `LoginProfile.java`, `CompanyServiceBean.java`,
`ExceptionHandlerFilter.java`, `ScosHttpSecurityConfiguration.java`, `WebSecurityConfig.java`,
`vw_login_context.sql`, `vw_authority_response.sql`, `ScosOrganizationPermission.java`, os
`pom.xml` de raiz/`flow-organization-boot`/`server-fat`, o `Dockerfile` do Postgres, os
changelogs de trigger em `flow-organization-resources`, e o histórico de commits (`git log`)
para confirmar a ordem temporal da reestruturação para monólito modular e da introdução do
`server-fat`.
