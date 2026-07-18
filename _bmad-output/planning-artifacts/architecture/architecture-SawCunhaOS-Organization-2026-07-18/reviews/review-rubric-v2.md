---
review-of: _bmad-output/planning-artifacts/architecture/architecture-SawCunhaOS-Organization-2026-07-18/ARCHITECTURE-SPINE.md
type: rubric-walk
reviewer: rubric-walker (subagent) — rodada 2, pós-implementação de schema
date: '2026-07-18'
verdict: needs-revision
previous-review: reviews/review-rubric.md (v1, needs-revision, 10 achados)
---

# Rubric Walk — Architecture Spine (SawCunhaOS Motor Fundacional, Etapa 1/P0) — Rodada 2

## Veredito

A verificação especial desta rodada — Structural Seed vs. schema real (Liquibase) — **passa
no essencial**: os cinco artefatos citados (`scos_login_approval_request.yml`,
`scos_department_manager.yml`, `checks.yml`, o trigger `trg_block_delete_login_approval_request`
+ `triggers.yml`, e o índice `login_approval_request.yml`) existem de fato, todo nome de
tabela/coluna citado na spine bate literalmente com o YAML real, a ordem de inclusão em
`tables.yml` respeita a dependência de FK (Department→Employee na Layer 4,
LoginApprovalRequest→Login na Layer 6), e a alegação mais arriscada da rodada — que
`trg_sync_login_status`/`fn_sync_login_status` já existe e é reaproveitado sem mecanismo novo
— foi verificada linha a linha e está correta. O companion de diagramas
(`etc/architecture/etapa-1-diagramas.md`) existe e reflete os mesmos nomes de campo do YAML
sem contradição.

Mas a verificação encontrou um **problema real dentro do próprio Structural Seed**: a
Convenção "Imutabilidade de histórico/auditoria" manda `LoginApprovalRequest` levar trigger
`BEFORE UPDATE/DELETE` "mesmo padrão de `fn_block_delete` já usado" — só que **nenhum** dos 9
usos existentes de `fn_block_delete` no repo (incluindo o novo trigger que a própria spine
acabou de citar) bloqueia UPDATE, e bloquear UPDATE em `LoginApprovalRequest` quebraria o
próprio fluxo que a AD-4 descreve (a entidade *precisa* de UPDATE para registrar decisão e
escalonamento). O código já implementou corretamente só `BEFORE DELETE`; é o texto da spine
que está errado/largo demais — achado Alto.

Fora da verificação especial, esta rodada corrigiu de fato vários achados Altos da v1 (nome
correto de `LoginStatus`, `FR-14` agora no `binds`, fail-closed do Redis no lado da leitura,
premissa de `vw_login_context`/Perfil adicional agora tratada como pré-requisito em AD-4/AD-6,
caminho `Cancelado` de FR-28 agora no diagrama de estados). Mas **dois achados Altos da v1
seguem abertos e intocados**, e eu confirmei que ainda são reais no código de hoje: o módulo
`server-fat` continua existindo, registrado no `pom.xml` raiz e empacotável, sem nenhuma
menção na spine (que só fala de `boot`/`grpc-boot`); e o mecanismo de bloqueio em cascata de
FR-2 (desligar Empresa se **qualquer** filial ativa em **qualquer** nível) ainda não existe em
código (`CompanyServiceBean.depthOf` só sobe a cadeia, nunca desce) e a spine continua tratando
FR-1/FR-2 como "CRUD já ratificado, sem AD nova" sem mencionar essa lacuna.

## Achados por severidade

- **Crítico:** 0
- **Alto:** 3
- **Médio:** 4
- **Baixo:** 3

Total: 10 achados novos/reabertos nesta rodada (mais um apêndice de achados da v1 já
corrigidos, para rastreabilidade).

---

## Parte 1 — Verificação especial: Structural Seed vs. schema real

### [Confirmado ✅] Nomes de tabela/coluna citados na spine batem com os arquivos reais

Cruzei cada nome citado em AD-4 (`ARCHITECTURE-SPINE.md:68`), AD-5 (`:95`), Consistency
Conventions (`:108`) e Structural Seed (`:132-138`) contra os arquivos reais:

| Citado na spine | Arquivo real | Confere? |
| --- | --- | --- |
| `LoginApprovalRequest` FK para `Login`, colunas de SLA, `isExceptionSelfApproval`, `escalationPolicy` | `scos_login_approval_request.yml:12-102` (`LOGIN_ID`, `SLA_DEADLINE`, `IS_EXCEPTION_SELF_APPROVAL`, `ESCALATION_POLICY`) | ✅ |
| `CURRENT_LEVEL` = `SUPERVISOR`/`MANAGER`/`SYSTEM_ACCESS_GROUP` | `checks.yml:32` (`chk_login_approval_request_level`) | ✅ |
| `escalationPolicy` = `INDEFINITE`/`AUTO_CANCEL` | `checks.yml:33` (`chk_login_approval_request_policy`) | ✅ |
| `Login.status` ganha `PENDING_APPROVAL`/`REJECTED`, enum real `LoginStatus` | `checks.yml:16` (`chk_login_status`) e `LoginStatus.java:16-19` (hoje só `ACTIVE/INACTIVE/BLOCKED` — corretamente listado em "Java ainda por implementar", não no schema) | ✅ |
| `Department.MANAGER_ID` (FK Employee) | `scos_department_manager.yml:12-17` (`MANAGER_ID` → `SCOS_EMPLOYEE(EMPLOYEE_ID)`) | ✅ |
| Trigger reaproveita `fn_block_delete()` | `trg_block_delete_login_approval_request.sql:1-5` | ✅ |
| Índice de FK + índice parcial de SLA | `v1.0.0/indexes/login_approval_request.yml` — `idx_..._login_id` (FK) e `idx_..._sla_scan` (parcial, `WHERE status = 'PENDING'`) | ✅ |

Nenhum nome inventado encontrado.

### [Confirmado ✅] Ordem de dependência em `tables.yml`/`indexes.yml`/`triggers.yml`

- `tables.yml` (`:76-82` no arquivo real) inclui `scos_employee.yml` na **Layer 4** e
  `scos_department_manager.yml` logo em seguida, **na mesma Layer 4** — depois de `Employee`
  existir, como a FK exige. Correto.
- `scos_login_approval_request.yml` está na **Layer 6** (`:111-116`), depois de
  `scos_login.yml` (Layer 5) — também correto para a FK `LOGIN_ID`.
- Ordem do changelog master (`db.changelog-master.yml`): `v1.0.0.yml` (tabelas+índices) →
  `checks.yml` → `function.yml` → `triggers.yml` → `view.yml`. Isso significa que
  `checks.yml` roda **depois** da tabela existir (ok) e `triggers.yml` roda **depois** de
  `function.yml` (ok — `fn_block_delete()` já existe quando o trigger é criado).

### [ALTO] A Convenção "Imutabilidade de histórico/auditoria" prescreve `BEFORE UPDATE/DELETE` para `LoginApprovalRequest`, mas isso é factualmente errado e quebraria o próprio fluxo da AD-4

`ARCHITECTURE-SPINE.md:109`:
> "Toda tabela de histórico/auditoria nova desta Etapa (`LoginApprovalRequest`, log de turno)
> leva trigger física `BEFORE UPDATE/DELETE` (mesmo padrão de `fn_block_delete` já usado em
> `SCOS_REASON_*`/Catalog/Outbox) — **não** repetir a lacuna hoje existente em
> `*_STATUS_HISTORY`."

Duas coisas erradas nessa frase, verificadas contra o código real:

1. **"Mesmo padrão de `fn_block_delete`" não é `BEFORE UPDATE/DELETE`.** Conferi os 9 usos
   existentes de `fn_block_delete()` no repo (`triggers/trg_block_delete_*.sql` — 
   `address_type`, `contact_type`, `outbox_topic`, `reason_activate/disable/enable/inactivate/position_change`,
   e o novo `login_approval_request`): **todos, sem exceção, são `BEFORE DELETE`** — nenhum é
   `BEFORE UPDATE`. O "mesmo padrão" citado como precedente simplesmente não existe; é
   `BEFORE DELETE`, ponto.
2. **Bloquear UPDATE em `LoginApprovalRequest` quebraria a própria AD-4.** A entidade
   **precisa** de UPDATE para funcionar — é assim que `CURRENT_LEVEL` escala
   (`SUPERVISOR`→`MANAGER`→`SYSTEM_ACCESS_GROUP`), que `STATUS` transiciona
   (`PENDING`→`APPROVED`/`REJECTED`/`CANCELLED`), e que `DECIDED_BY_LOGIN_ID`/`DECIDED_AT` são
   preenchidos. `LoginApprovalRequest` é uma **entidade de estado mutável em andamento**, não
   um log de auditoria append-only como `*_STATUS_HISTORY` — a própria spine trata os dois
   como se fossem a mesma coisa nessa frase, quando não são.

O código já fez a coisa certa (`trg_block_delete_login_approval_request.sql:2` — `BEFORE
DELETE`, nada de UPDATE), então isso **não é uma bomba-relógio de implementação** — mas é uma
imprecisão real no texto da spine que:
- Confunde quem for implementar a próxima tabela de auditoria genuína ("log de turno", ainda
  não construída, citada na mesma frase) sobre qual é de fato o padrão a copiar.
- Deixa a spine dizendo uma coisa que, se um builder futuro seguir ao pé da letra para
  `LoginApprovalRequest` (ex. numa migração futura "para cumprir a Convenção"), quebra o
  fluxo de aprovação.

**Sugestão:** separar a linha em duas: (a) tabelas de auditoria pura, append-only
(`*_STATUS_HISTORY` hoje, "log de turno" futuro) levam `BEFORE DELETE` via `fn_block_delete`
— não `UPDATE/DELETE`, porque nenhuma tabela no repo bloqueia UPDATE hoje, nem deveria, já
que nem o próprio padrão faz isso; (b) `LoginApprovalRequest` é uma entidade de estado
mutável — só `DELETE` é bloqueado (proteger o histórico de decisão), e isso já está
implementado corretamente. Se a intenção for genuinamente impedir UPDATE de campos já
decididos (ex. não deixar reabrir uma solicitação `APPROVED`), isso é uma regra de **domínio**
(validação no Use Case/entidade), não uma trigger física de banco — e merece sua própria
frase, não ser emprestada da linha de imutabilidade.

### [Confirmado ✅] AD-4 / `trg_sync_login_status` / `fn_sync_login_status`

Li os dois arquivos reais:
- `trg_sync_login_status.sql:1-5` — `AFTER INSERT ON scos.SCOS_LOGIN_STATUS_HISTORY ...
  EXECUTE FUNCTION scos.fn_sync_login_status()`.
- `fn_sync_login_status.sql:1-12` — faz exatamente `UPDATE scos.scos_login SET status =
  NEW.status, updated_at = NEW.created_at WHERE login_id = NEW.login_id`.

A frase da spine (`ARCHITECTURE-SPINE.md:69`) — "toda transição de `LoginApprovalRequest`
grava uma linha em `SCOS_LOGIN_STATUS_HISTORY` ... e o trigger de banco já existente
(`trg_sync_login_status`/`fn_sync_login_status`) atualiza `SCOS_LOGIN.status`
automaticamente a partir dela. Nenhum mecanismo de sincronização novo é criado" — está
**tecnicamente correta e precisa**. É a mesma leitura de v1 (já confirmada lá para os demais
usos de `fn_sync_*`), reconfirmada aqui especificamente para `login_status`.

### [Confirmado ✅] Companion de diagramas existe e não contradiz a spine

`etc/architecture/etapa-1-diagramas.md` existe (233 linhas, 8 diagramas Mermaid). O diagrama
ER (seção 3) usa exatamente os mesmos nomes de campo do YAML real
(`requested_by_login_id`, `decided_by_login_id`, `current_level`, `escalation_policy`,
`status`, `is_exception_self_approval`, `sla_deadline`), a máquina de estados (seção 7)
replica a mesma da spine incluindo o caminho `Cancelado` de FR-28, e o próprio arquivo
termina com "se um diagrama e a spine discordarem, a spine vence" — hierarquia de verdade
correta. Nenhuma contradição encontrada.

### [MÉDIO] Nova FK quebra a convenção de nomenclatura `FK_<COL>_SCOS_<T>` que a própria spine diz estar "já em vigor"

`ARCHITECTURE-SPINE.md:107` afirma a convenção `FK_<COL>_SCOS_<T>` como "já em vigor — não
redefinido aqui". Levantei **todas** as `foreignKeyName` do repo (~50 ocorrências, todas as
tabelas em `v1.0.0/tables/*.yml`): sem nenhuma exceção, o padrão preserva o nome completo da
coluna (`FK_SUPERVISOR_ID_SCOS_EMPLOYEE`, `FK_MANAGER_ID_SCOS_DEPARTMENT`,
`FK_REASON_ACTIVATE_ID_SCOS_EMPLOYEE_STATUS_HISTORY` etc.). Mas em
`scos_login_approval_request.yml:31,38` as duas FKs para `Login` que não são a principal
usam nome **abreviado**, cortando o sufixo `_LOGIN_ID`:

- Coluna `REQUESTED_BY_LOGIN_ID` → `FK_REQUESTED_BY_SCOS_LOGIN_APPROVAL_REQUEST` (deveria ser
  `FK_REQUESTED_BY_LOGIN_ID_SCOS_LOGIN_APPROVAL_REQUEST`)
- Coluna `DECIDED_BY_LOGIN_ID` → `FK_DECIDED_BY_SCOS_LOGIN_APPROVAL_REQUEST` (deveria ser
  `FK_DECIDED_BY_LOGIN_ID_SCOS_LOGIN_APPROVAL_REQUEST`)

Não é limite de 63 caracteres do Postgres (os nomes completos teriam 50 e 52 caracteres,
verificado por contagem direta). É a única tabela do repo inteiro que quebra essa convenção.
Como a spine ratifica a convenção como já estável, ela deveria ter pego essa divergência no
próprio Structural Seed que está descrevendo — é exatamente o tipo de detalhe que, se
replicado por um builder futuro criando outra FK auto-referenciada a `Login` (ex. um segundo
aprovador dedicado), gera inconsistência silenciosa sobre qual dos dois estilos seguir.

**Sugestão:** renomear as duas constraints para o nome completo antes de tratar o schema
como definitivamente fechado, ou registrar a abreviação como exceção deliberada na Convenção
(mas hoje não há nenhuma justificativa registrada para o desvio).

### [BAIXO] Structural Seed subestima o que mudou em `checks.yml`

A linha do Structural Seed (`ARCHITECTURE-SPINE.md:136`) descreve a mudança em `checks.yml`
como "ALTER: `chk_login_status(_history)` ganham `PENDING_APPROVAL`/`REJECTED`" — mas o mesmo
changeset real (`checks.yml:31-34`) também adiciona três constraints novas
(`chk_login_approval_request_level/policy/status`) para a tabela nova. Não é risco de
divergência (AD-4 já especifica esse vocabulário em prosa), só uma lacuna de completude no
inventário do Structural Seed.

### [BAIXO] Índice de `MANAGER_ID` não é mencionado no Structural Seed

`scos_department_manager.yml:18-24` cria `IDX_MANAGER_ID_SCOS_DEPARTMENT` inline junto do
`addColumn` — segue corretamente o precedente real do repo (índices de suporte a FK ficam
inline na própria tabela, ex. `IDX_SUPERVISOR_ID_SCOS_EMPLOYEE`; só índices "curados" de
hot-path ganham arquivo dedicado em `v1.0.0/indexes/`). O padrão foi seguido certo — só não
está listado na linha do Structural Seed que descreve esse arquivo (`:135`).

### [BAIXO] `checks.yml` foi editado com o mesmo ID de changeset antigo (`20260701-...-022`), enquanto tabelas/triggers novos ganharam ID novo (`20260718-...`)

Consistente com a nota "projeto pré-produção, sem migração incremental" do próprio
Structural Seed — mas vale registrar explicitamente que essa mistura (novo objeto = novo ID;
extensão de objeto existente = edita o ID antigo) é a prática real, para não confundir quem
comparar os dois estilos lado a lado achando inconsistência de processo.

---

## Parte 2 — Checklist geral (6 perguntas)

### 1. Resolve os pontos reais de divergência, sem deixar nenhum de fora?

- **[ALTO — reaberto de v1, ainda real]** `server-fat` continua existindo, é um segundo
  composition root Spring Boot completo (`server-fat/pom.xml` → `finalName:
  ScosFlowServerApplication`, `spring-boot-maven-plugin`, dependência direta de
  `flow-organization-{domain,usecase,api,infrastructure,resources}`), está registrado como
  `<module>` no `pom.xml` raiz, e não recebeu nenhum commit desde que foi criado (mesmo dia
  da v1 da spine). A Design Paradigm atual (`ARCHITECTURE-SPINE.md:26`) já foi revisada para
  citar **dois** composition roots — mas são `flow-organization-boot` e
  `flow-organization-grpc-boot`, não `server-fat`. A revisão desta rodada resolveu a *forma*
  do achado de v1 (agora fala de dois módulos) sem resolver o *conteúdo* (o segundo módulo
  real e não mencionado continua sendo outro). Um builder que precise decidir onde registrar
  o `ShiftEnforcementFilter`/beans novos ainda não tem resposta da spine sobre por que
  `server-fat` existe ou se compete com `flow-organization-boot`.
- **[ALTO — reaberto de v1, ainda real]** FR-2 (bloqueio de `disable` de Empresa por filial
  ativa em qualquer nível da hierarquia) continua sem mecanismo de travessia descendente no
  código: `CompanyServiceBean.depthOf` (`:230`, `organization/flow-organization-domain/...`)
  só sobe via `parentCompany`; não há `WITH RECURSIVE`, nem busca de descendentes, em
  `CompanyServiceBean` nem em `UpdateCompanyUseCaseBean`. A spine continua tratando FR-1/FR-2
  como "CRUD+status já ratificado ... sem AD nova" (`:161`) sem mencionar essa lacuna
  especificamente — mesmo achado de v1, ainda sem resposta.
- **[MÉDIO — reaberto de v1, ainda real]** FR-11 (auditoria imutável de decisão de turno)
  continua ambíguo: a Capability Map (`:165`) aponta para "Nova tabela de log de turno", e a
  Convenção de imutabilidade (`:109`) trata essa tabela futura como já decidida ("leva
  trigger física..."), mas nenhuma tabela desse tipo existe no Structural Seed e o PRD
  (`prd.md:499`) diz o oposto — que FR-11 deveria reusar o padrão `@Auditable`/`*_STATUS_HISTORY`
  já existente, sem infraestrutura nova. A spine ainda não resolve qual dos dois mecanismos
  vale.

### 2. A Rule de cada AD é aplicável de verdade e previne a divergência descrita?

- Ver achado Alto da Parte 1 (Convenção de imutabilidade para `LoginApprovalRequest`) — a
  única Rule/Convenção que falha neste critério nesta rodada.
- AD-1, AD-2, AD-5, AD-6: Rules verificadas e aplicáveis (ver Parte 1 e itens "já bom"
  abaixo). AD-1 em particular já resolve o achado de fail-closed que v1 tinha marcado como
  Alto (`:49`, "se o filtro de autenticação não conseguir consultar o Redis ... negada por
  padrão").

### 3. Nada em Deferred deixaria dois builders divergirem sem perceber?

O `Deferred` (`:171-178`) continua enxuto e cada item foi checado contra o PRD:
- "SLA de 1 dia útil = 1 dia corrido" (`:174`) bate palavra por palavra com `prd.md:494`.
- "Defasagem de até 30 min" (`:175`) é consistente com o comentário real em
  `vw_login_context.sql:22` (`Refresh: pg_cron a cada 30 minutos`).
- Nenhum item novo do Deferred introduz ambiguidade. O problema, como na v1, não é o que
  está lá — é o que falta (server-fat, cascata de FR-2, FR-11), já listado acima.

### 4. Tecnologia nomeada está atual/verificada?

Sem mudança de Stack nesta rodada (mesma seção da v1, já verificada). Nenhuma tecnologia
nova introduzida no Structural Seed — apenas Liquibase/Postgres, já cobertos.

### 5. Ratifica o brownfield em vez de contradizê-lo?

- **[MÉDIO — novo]** AD-3 justifica a fronteira Filter-fora-do-starter dizendo que
  `flow-security-starter` "é biblioteca reusável entre projetos SCOS" (`:60`, `:37` do
  diagrama de módulos). Verifiquei `flow-security-starter/pom.xml:92-95`: o módulo já
  depende de `flow-organization-grpc-proto`, cujo `authority.proto` define
  `AuthorityResponse` com campos `employee_id`, `company_id`, `branch_id` — conceitos
  específicos **deste** produto, não genéricos entre projetos SCOS. A Rule de AD-3 continua
  válida e aplicável (o Filter de turno realmente não deve entrar no starter,
  independentemente disso), mas a premissa retórica de que o starter hoje é
  "produto-agnóstico" já não é 100% verdade — vale uma frase reconhecendo essa fissura
  existente, para não sugerir a um builder futuro que a fronteira é mais estanque do que é
  na prática.
- Todo o resto ratifica corretamente: `Employee.supervisor` (self-FK real, `Employee.java:94-96`,
  usado como precedente de AD-5), `fn_block_delete`/padrão de trigger (Parte 1), premissa de
  bug em `vw_login_context` de AD-6 (`vw_login_context.sql:54-61` — só faz `JOIN` com
  `l.profile_id`, nunca referencia `SCOS_LOGIN_PROFILE`; confirmado também pelo próprio PRD,
  `prd.md:247`), `ExceptionHandlerFilter`/`addFilterBefore` como precedente real de AD-2
  (`ScosHttpSecurityConfiguration.java:48`), e enum `LoginStatus` (não mais citado como
  "StatusLogin" — achado Médio da v1 corrigido).

### 6. Toda dimensão que esta altitude deveria decidir foi decidida/deferida/ou virou pergunta aberta?

- Envelope operacional: sem mudança desde v1, ainda correto (infra já provisionada,
  reaproveitada).
- A dimensão "qual é a unidade de deploy/composition root" segue sendo a lacuna real deste
  critério (ver achado Alto de `server-fat` acima) — é uma decisão de altitude *initiative*
  por definição (afeta todo builder que precisa saber onde registrar algo), e continua sem
  decisão, sem menção, e sem pergunta em aberto formal.

---

## Apêndice — Achados da v1 confirmados corrigidos nesta rodada

Para rastreabilidade (comparado com `reviews/review-rubric.md`):

- ✅ Nome do enum `LoginStatus` (era citado como `StatusLogin`) — corrigido em `AD-4` e
  Structural Seed, e bate com `LoginStatus.java:16`.
- ✅ `FR-14` fora do `binds` — agora incluído (`binds: ['FR-1..FR-14', 'FR-23..FR-28']`).
- ✅ AD-1 sem fail-closed explícito na leitura — agora explícito (`:49`).
- ✅ Bug de `vw_login_context`/Perfil adicional contaminando AD-6 sem ser mencionado — agora
  é pré-requisito de entrega explícito em AD-6 (`:101`).
- ✅ Diagrama de estados de AD-4 sem o caminho `Cancelado` de FR-28 — presente agora em ambas
  as versões do diagrama (spine e companion).
- ✅ FR-4/FR-8 ausentes do `binds`/Capability Map — agora têm linha própria, com "sem AD
  nova" justificado (paradigma existente resolve).

## O que já está bom (preservar)

- Todo o Structural Seed desta rodada é factualmente preciso quanto a nomes, exceto os dois
  achados acima (Convenção de imutabilidade, nomenclatura de FK) — é um nível de fidelidade
  ao código real notavelmente alto para uma spine.
- AD-4's reaproveitamento de `trg_sync_login_status` é a decisão mais frágil-parecendo da
  rodada (fácil de errar) e foi descrita com exatidão cirúrgica.
- A ordenação de dependência em `tables.yml` (Layer 4/6) mostra disciplina real de quem
  escreveu o schema — nada a corrigir aí.
- O companion de diagramas é um artefato de alta qualidade, consistente com a spine e
  explícito sobre hierarquia de verdade.

## Metodologia

Leitura completa da spine atual e comparação com a v1 (`reviews/review-rubric.md`).
Verificação direta no repositório: os 5 arquivos da verificação especial
(`scos_login_approval_request.yml`, `scos_department_manager.yml`, `checks.yml`,
`trg_block_delete_login_approval_request.sql` + `triggers.yml`,
`v1.0.0/indexes/login_approval_request.yml`), mais `trg_sync_login_status.sql`,
`fn_sync_login_status.sql`, `fn_block_delete.sql` e todos os 9 usos de
`fn_block_delete()` no repo, `tables.yml`/`indexes.yml`/`db.changelog-master.yml`/`v1.0.0.yml`
(ordem de changelog), `LoginStatus.java`, `Employee.java`, `Department.java`,
`vw_login_context.sql`, `etc/architecture/etapa-1-diagramas.md`, `flow-security-starter/pom.xml`
+ `authority.proto`, `server-fat/pom.xml` + `pom.xml` raiz, `CompanyServiceBean.java` +
`UpdateCompanyUseCaseBean.java`, e `prd.md` (seções FR-1 a FR-28, §9 Riscos, §10 Auditoria).
Rodei também `lint_spine.py` (passagem mecânica): 0 achados — placeholders, IDs duplicados,
Binds/Prevents/Rule ausentes e versões de Stack não fixadas já estavam todos ok antes da
revisão semântica.
