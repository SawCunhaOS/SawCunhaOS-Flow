---
stepsCompleted: ['step-01-validate-prerequisites', 'step-02-design-epics', 'step-03-create-stories']
inputDocuments: ['_bmad-output/planning-artifacts/prds/prd-SawCunhaOS-Organization-2026-07-18/prd.md', '_bmad-output/planning-artifacts/prds/prd-SawCunhaOS-Organization-2026-07-18/addendum.md', '_bmad-output/planning-artifacts/architecture/architecture-SawCunhaOS-Organization-2026-07-18/ARCHITECTURE-SPINE.md']
---

# SawCunhaOS-Organization - Epic Breakdown

## Overview

Este documento quebra em épicos e stories o escopo da **Etapa 1 (P0)** do PRD "SawCunhaOS Motor Fundacional — Fase 1": Fundação de Identidade e Organização, Governança Ativa via API (bloqueio de turno / Zero Trust) e Kill Switch. As Etapas 2 (Motor Geotemporal) e 3 (Motor de Notificação Híbrida) — FR-15 a FR-22 — ficam fora deste breakdown por decisão explícita (spine de arquitetura só decidiu para a Etapa 1; essas FRs entram em rodada própria quando a etapa chegar).

**Estado real do código (auditoria + implementação já feita nesta sessão):** schema Liquibase da Etapa 1 já está implementado (`SCOS_LOGIN_APPROVAL_REQUEST`, `SCOS_SHIFT_ENFORCEMENT_LOG`, `DEPARTMENT.MANAGER_ID`, checks e triggers). A camada Java (entidades, Use Cases, Delegates, o Filter de turno) ainda não existe — é o que estes épicos cobrem.

**Epic 0 (adicionado em 2026-07-19):** débito técnico de fundação — não vem de FR do PRD, vem de correção de padrão detectada em auditoria de código. Precede o Epic 1 porque padroniza uma convenção (tipos temporais) que os épicos de Governança de Turno (Epic 5) e Kill Switch (Epic 6) dependem para precisão de janela/latência.

## Requirements Inventory

### Functional Requirements

FR-1: RH pode cadastrar uma Empresa como matriz ou filial de outra Empresa ativa, com CNPJ único e profundidade de hierarquia limitada (`COMPANY_HIERARCHY_MAX_DEPTH`). Sistema rejeita ciclo na hierarquia (auto-referência direta já bloqueada por trigger; ciclo indireto ainda por implementar) e impede inativar a última matriz ativa / bloquear a única empresa ativa do sistema.

FR-2: RH pode ativar/inativar/bloquear/desbloquear uma Empresa, sempre com motivo obrigatório do catálogo e histórico de status. `disable`/`block` são rejeitados se existir filial ativa em **qualquer nível** da subárvore (verificação via CTE recursiva, AD-7 da spine).

FR-3: RH pode cadastrar Departamentos e Cargos vinculados a um Departamento ativo, cada um com ciclo enable/disable. Criar/atualizar Cargo com Departamento inativo é rejeitado; desativar Departamento/Cargo com Cargo/Funcionário ativo vinculado é rejeitado. *(Já implementado e funcional — `PositionServiceBean`, confirmado em auditoria.)*

FR-4: RH pode definir um template de Jornada de Trabalho por Cargo e dia da semana; a admissão de um Funcionário copia esse template para seu registro efetivo (cópia única, sem vínculo futuro — editar o template depois não afeta quem já foi copiado). Validação cronológica estrita: `startTime < lunchStart < lunchEnd < endTime`.

FR-5: RH pode admitir, ativar/inativar/bloquear/desbloquear e recontratar um Funcionário. `EMPLOYEE_MIN_AGE`/`EMPLOYEE_EMAIL_DOMAIN` configuráveis via Configuration. `rehire` aceita Funcionário `INACTIVE` **ou** `DISABLED` como origem (reatribuindo empresa/cargo/supervisor); `unblock` continua existindo como caminho rápido sem reatribuição.

FR-6: RH pode criar um Login para um Funcionário; a partir da aprovação (FR-23) o Login fica `ACTIVE` e sincroniza com o Keycloak via Saga/Outbox. Permissões de transição simétricas: `BLOCK_LOGIN`/`UNBLOCK_LOGIN` separadas (corrigindo a permissão única `UPDATE_LOGIN_STATUS` hoje no código — breaking change sujeito à política de versionamento, ver §11 do PRD).

FR-7: Um Login tem um Perfil principal e pode ter Perfis adicionais; cada Perfil agrega Recursos. Toda operação de API exige `x-authorize` mapeado 1:1 a uma permissão (`PermissionsConsistencyTest` já garante isso). Bug conhecido: Perfis adicionais não somam permissões em `vw_login_context` — correção é pré-requisito de FR-26/27 (AD-6).

FR-8: RH pode marcar quais Perfis exigem bloqueio de turno e quais têm acesso irrestrito (flag simples e permanente, não expira). Mudança de classificação respeita a defasagem de até 30 min das views de autoridade materializadas.

FR-9: Toda requisição de API autenticada, de um Login cujo Perfil exige bloqueio de turno, é avaliada contra a Jornada de Trabalho vigente antes da lógica de negócio. Fora do intervalo `[startTime, endTime]` (exceto almoço) → `403 Forbidden`.

FR-10: Login sujeito a bloqueio de turno sem Jornada cadastrada para o dia da semana corrente é tratado como fora de turno (fail-closed — nunca libera por omissão).

FR-11: Toda avaliação de bloqueio de turno (permitida ou negada) gera registro de auditoria imutável (quem, quando, endpoint, decisão, dentro/fora da janela), retido por no mínimo 5 anos após o desligamento do Funcionário (art. 7º XXIX CF/88). Tabela `SCOS_SHIFT_ENFORCEMENT_LOG` já implementada.

FR-12: RH/gestor pode cadastrar previamente uma janela de plantão (início/fim) por Login/Funcionário. Requisição dentro dessa janela é liberada mesmo fora da Jornada regular — sempre gera o mesmo registro de auditoria de FR-11, identificando a liberação como plantão.

FR-13 (Kill Switch): ao Funcionário ser desligado ou Login bloqueado/desabilitado, todas as sessões/tokens são invalidados via denylist Redis síncrono (chave `jti`/`loginId`, TTL = validade restante), escrito na mesma thread da requisição, imediatamente após o commit. Chamada à Keycloak Admin API em paralelo (defesa em profundidade, não o enforcement). Falha na escrita do Redis nunca bloqueia o commit (retry + alerta obrigatório); falha na leitura (não confirma o estado) = fail-closed.

FR-14: Reativar (`enable`/`unblock`) um Funcionário ou Login não restaura nenhuma sessão anterior — reautenticação obrigatória. Reativar Funcionário não reverte automaticamente o status de Logins vinculados.

FR-23: Todo Login recém-criado exige aprovação de um aprovador válido — nunca a própria pessoa que abriu a solicitação — através de uma cadeia de 3 níveis (Supervisor → Gerente do Departamento → grupo `APPROVE_SYSTEM_ACCESS`), cada nível pulado se ausente ou em conflito com o solicitante. SLA de 1 dia útil por nível com escalonamento automático. Status do Funcionário é reverificado no momento da decisão (não só na criação). Mitigação de última instância: auto-aprovação de exceção sempre marcada em auditoria separada.

FR-24: Reativar um Login (`enable`/`unblock`) exige exatamente a mesma cadeia de aprovação de FR-23 — sem redução de níveis, sem exceção.

FR-25: Toda mudança que aumenta o acesso de um Login existente (troca de Perfil principal ou adição de Perfil adicional) exige a mesma aprovação — mas o Login continua `ACTIVE`, operando com o Perfil atual, enquanto aguarda (nunca bloqueia acesso já existente). Remover Perfil adicional (reduzir acesso) não exige aprovação.

FR-26: Login sem Funcionário vinculado (tipos `EXTERNAL`/`SERVICE`) segue o mesmo mecanismo de estado pendente, mas o aprovador é resolvido direto pelo grupo `APPROVE_SYSTEM_ACCESS` (designação individual explícita, não hierarquia) — mesma válvula de auto-aprovação de exceção de FR-23.

FR-27: Criar um novo Perfil ou alterar quais Recursos um Perfil existente contém exige aprovação do mesmo grupo de FR-26. Logins que já usam o Perfil mantêm o conjunto de Recursos **anterior** em vigor até a mudança ser aprovada.

FR-28: RH pode colocar um Funcionário em licença temporária (férias/licença médica) reaproveitando `disable`/`enable` — dois Motivos dedicados no catálogo `SCOS_REASON_INACTIVATE` (não `SCOS_REASON_DISABLE`) + campo opcional de data prevista de retorno. Na data prevista, o sistema abre sozinho a solicitação de reativação (ainda exige aprovação humana). Cancelamento automático se não decidido em 5 dias úteis (exceto se o Funcionário for desligado por outro motivo nesse meio-tempo).

### NonFunctional Requirements

NFR-1 (Performance): checagem de turno síncrona com latência desprezível (referência: p95 < 10ms); Kill Switch fim-a-fim em menos de 1 segundo, sob carga normal.

NFR-2 (Segurança / Zero Trust): nenhuma regra de acesso pode ser satisfeita ou contornada por manipulação de requisição no cliente (front-end, Postman, curl) — toda decisão vive e é imposta no back-end. Token revogado nunca é aceito novamente, mesmo dentro da validade original.

NFR-3 (Confiabilidade): views de autoridade (`vw_login_context`, `vw_authority_response`) são materializadas com defasagem conhecida de até 30 min (`pg_cron`) — qualquer story que dependa de troca de Perfil em tempo real precisa considerar essa janela.

NFR-4 (Observabilidade / Auditoria): toda transição de status sensível e toda decisão de bloqueio de turno usa o padrão de auditoria já estabelecido (`@Auditable`, tabelas de histórico) — trigger física de bloqueio de `DELETE` (nunca `UPDATE`) em toda tabela nova de decisão/histórico desta Etapa.

NFR-5 (Privacidade / LGPD): CPF, e-mail e endereço de Funcionário são PII — seguem a política de mascaramento já adotada (`%mask`/`%maskmdc`); nenhuma PII em texto puro em log de auditoria.

NFR-6 (Conformidade CLT): retenção do log de auditoria de turno em, no mínimo, 5 anos após o desligamento do Funcionário (art. 7º XXIX CF/88) — decisão definitiva do PRD.

NFR-7 (Segurança de credenciais): nenhum segredo literal em YAML — só via Jasypt/variável de ambiente.

### Additional Requirements

- **Sem starter template** — projeto é brownfield; paradigma DDD tático em camadas (`domain` → `usecase` → `api`, com `infrastructure`/`boot` cross-cutting) já está ratificado no código existente e não deve ser reinventado. Nenhum módulo Maven novo é necessário para a Etapa 1 (módulos existentes: `organization/flow-organization-{domain,usecase,api,infrastructure,boot,grpc-boot,grpc-proto,resources,shared}`, `organization/flow-security-starter`).
- **Schema Liquibase da Etapa 1 já implementado** (não gerar stories de migração/DDL para isto — só a camada Java que consome): tabela `SCOS_LOGIN_APPROVAL_REQUEST`, tabela `SCOS_SHIFT_ENFORCEMENT_LOG`, coluna `DEPARTMENT.MANAGER_ID` (FK `Employee`), `CHECK` constraints (incluindo a correlação `escalation_policy`/`requested_by_login_id`), triggers de bloqueio de `DELETE` (reaproveitando `fn_block_delete()` existente), índices de FK e de varredura de SLA.
- **AD-1 (Kill Switch):** denylist Redis síncrono é o mecanismo real; chamada à Keycloak Admin API é paralela/defesa em profundidade, nunca o enforcement.
- **AD-2/AD-3 (Filter de turno):** novo `ShiftEnforcementFilter`, registrado via `addFilterAfter` do filtro JWT **e** `addFilterBefore` do filtro de idempotência (jDempotent) — vive em `flow-organization-infrastructure`, nunca em `flow-security-starter` (biblioteca reusável entre projetos SCOS).
- **AD-4 (LoginApprovalRequest):** sincronização com `Login.status` (enum real `LoginStatus`) é feita pelo Use Case, que grava — na mesma transação — o `STATUS` em `LoginApprovalRequest` **e** o INSERT em `SCOS_LOGIN_STATUS_HISTORY` (é esse INSERT que aciona o trigger já existente `trg_sync_login_status`; nenhum trigger novo conecta as duas tabelas diretamente). Recálculo de `SLA_DEADLINE` na escalada é responsabilidade de um job agendado, em uma única transação por solicitação.
- **AD-6 (grupo de aprovação de sistema):** `APPROVE_SYSTEM_ACCESS` deve ser atribuído como Perfil **principal**, nunca adicional, enquanto o bug de `vw_login_context` (FR-7) não for corrigido — a correção da view é pré-requisito de entrega, não item independente.
- **AD-7 (hierarquia de Empresa):** verificação de filial ativa em subárvore é `WITH RECURSIVE` em SQL, não caminhada em Java nem estrutura denormalizada nova.
- **Vocabulário de enum é sempre em inglês** no código/schema (`PENDING_APPROVAL`, `SUPERVISOR`, `MANAGER`, `SYSTEM_ACCESS_GROUP`, `INDEFINITE`, `AUTO_CANCEL`, `PENDING`/`APPROVED`/`REJECTED`/`CANCELLED`) — rótulos em PT-BR só em mensagem ao usuário/diagrama, nunca no valor persistido.
- **Convenções de nomenclatura já em vigor** (não redefinir): `SCOS_<ENTIDADE>`, `PK_SCOS_<T>`, `FK_<COL>_SCOS_<T>`, permissão `ACTION_RESOURCE`, erro `SCOS_<MÓDULO>_<NNN>`, `ScosException`/`ExceptionCodeError` (nunca `RuntimeException` cru), `@Transactional(rollbackFor = ScosException.class)` em Use Case de escrita.
- **Open question não resolvida (não bloqueia stories de código, mas bloqueia decisão de deploy/CI):** qual módulo é o deployável real de produção — `server-fat` (composition root próprio, `ScosFlowApplication`, importa `organization` via BOM) ou `flow-organization-boot` standalone. Sinalizar para o time antes de configurar pipeline.
- **AD-8 (Tipos temporais, adicionado em 2026-07-19):** `Instant`↔`TIMESTAMPTZ` (fato/instante), `LocalDate`↔`DATE` (calendário), `LocalTime`↔`TIME` (hora de parede/jornada). `LocalDateTime` proibido no domínio; `.now()` proibido no domínio — hora "agora" só via `Clock` injetável no Service. Formalizado no Epic 0 e replicado em `project-context.md`.
- **AD-9 (Fuso horário por filial, adicionado em 2026-07-19):** `SCOS_COMPANY.TIME_ZONE` (IANA, `NOT NULL DEFAULT 'America/Sao_Paulo'`), `Company.timeZone` (`ZoneId`, `@Builder.Default`), resolução com herança defensiva subindo `parentCompany` (mesmo padrão de `CompanyServiceBean.depthOf()`, não CTE — AD-7 só exige CTE para descer subárvore). Fuso é imutável após criação (D3, Story 0.3) — sem endpoint de alteração nesta Etapa. `ShiftWindowEvaluator` (domain service novo, `domain/shift/`) é o único ponto de conversão `Instant`→hora local; trata turno que cruza meia-noite; limites `[startTime, endTime]` inclusivos.

### UX Design Requirements

N/A — produto é motor de API/back-end; não há front-end no escopo desta Fase 1 (Non-Goal explícito do PRD, §12).

### FR Coverage Map

FR-1: Epic 1 - Cadastro hierárquico de Empresa/filial
FR-2: Epic 1 - Ciclo de vida de Empresa + guarda de filial ativa em subárvore
FR-3: Epic 1 - Departamento e Cargo (já implementado — story de verificação)
FR-4: Epic 1 - Jornada de Trabalho (template + cópia na admissão)
FR-5: Epic 2 - Ciclo de vida completo de Funcionário
FR-28: Epic 2 - Licença/férias (reaproveita disable/enable do Funcionário)
FR-6: Epic 3 - Criação de Login vinculado a Funcionário
FR-7: Epic 3 - Perfil/Recurso + correção do bug de perfil adicional
FR-23: Epic 3 - Aprovação de criação de Login
FR-24: Epic 3 - Aprovação de reativação de Login
FR-25: Epic 3 - Aprovação de mudança de Perfil
FR-26: Epic 4 - Login sem Funcionário (EXTERNAL/SERVICE) + aprovação por grupo de sistema
FR-27: Epic 4 - Criação/edição de Perfil + aprovação por grupo de sistema
FR-8: Epic 5 - Classificação de Perfil sujeito a bloqueio de turno
FR-9: Epic 5 - Bloqueio de requisição fora da Jornada
FR-10: Epic 5 - Fail-closed sem Jornada cadastrada
FR-11: Epic 5 - Auditoria imutável de decisão de turno
FR-12: Epic 5 - Plantão pré-aprovado
FR-13: Epic 6 - Kill Switch (invalidação síncrona)
FR-14: Epic 6 - Reativação não restaura sessão antiga

## Epic List

### Epic 0: Fundação Técnica — Tipos Temporais e Relógio Injetável
Débito técnico transversal, não mapeado a FR do PRD: padroniza a representação de tempo no domínio (`Instant` para instante/`TIMESTAMPTZ`, `LocalDate` para calendário/`DATE`, `LocalTime` para hora de parede/`TIME`, nunca `LocalDateTime`) e introduz `Clock` injetável para que nenhuma regra de negócio dependa de `.now()` estático; modela o fuso horário por filial (inexistente hoje) e entrega um avaliador de janela de turno reutilizável — pré-requisito de confiabilidade e de dado para Epic 5 (Governança de Turno) e Epic 6 (Kill Switch), onde precisão de janela/latência é crítica.
**FRs covered:** Nenhuma — débito técnico (ver AD-8/AD-9 em Additional Requirements).
**Depende de:** nenhum épico anterior (é fundação).
**Nota:** Story 0.1 cobre testes do caminho crítico de autenticação de sistema (interceptor gRPC + cifra + validação de secret) e CI mínimo — pré-requisito de fato para considerar a Onda 2 (secret em repouso) entregue. Story 0.2 cobre a padronização de tipos temporais/Clock. Story 0.3 modela fuso horário por filial e o avaliador de janela de turno reutilizável — **pré-requisito bloqueante das stories de bloqueio de turno da Etapa 1 (Epic 5: FR-9, FR-10, FR-12)**, que hoje não têm de onde ler o fuso da filial.

### Epic 1: Estrutura Organizacional
RH consegue montar e manter toda a estrutura organizacional do ISP: empresas/filiais em hierarquia íntegra (sem ciclos, sem filial órfã ativa em nenhum nível), departamentos, cargos, e o horário de trabalho padrão de cada cargo.
**FRs covered:** FR-1, FR-2, FR-3, FR-4
**Nota:** FR-3 (Departamento/Cargo) já está implementado e funcional — a story correspondente é de verificação/regressão, não construção nova. FR-1/FR-2 são parciais (Company CRUD básico existe; faltam as guardas de hierarquia). FR-4 é 0% implementado.

### Epic 2: Ciclo de Vida do Funcionário
RH consegue admitir um Funcionário (que já nasce com a Jornada de Trabalho do seu Cargo copiada), gerenciar todo o ciclo de vida dele — ativar, inativar, bloquear, desbloquear, recontratar — e colocá-lo em licença médica/férias com retorno assistido.
**FRs covered:** FR-5, FR-28
**Depende de:** Epic 1 (Departamento/Cargo/Jornada precisam existir).

### Epic 3: Login do Funcionário com Aprovação
RH consegue criar um Login para um Funcionário e ele só fica utilizável depois que um Supervisor ou Gerente aprova — nunca o próprio solicitante — com escalonamento automático se ninguém responder a tempo. O mesmo vale para reativar um Login existente ou aumentar o acesso dele com um novo Perfil.
**FRs covered:** FR-6, FR-7, FR-23, FR-24, FR-25
**Depende de:** Epic 2 (Funcionário precisa existir para o Login referenciar).
**Nota:** Login criado só é utilizável de fato quando o fluxo de aprovação (FR-23) está completo — por isso FR-6 e FR-23/24/25 vivem no mesmo épico (entregar Login sem aprovação não seria funcionalidade completa). **Story 3.7 (adicionada numa validação cruzada entre épicos)** fecha o cascade de status Funcionário→Login que as Stories 2.2/2.4 (Epic 2) já esperavam de FR-6/FR-28, mas nenhuma story original do Epic 3 cobria. **Story 3.6 (idem)** cobre FR-27 — tecnicamente mapeado pro Epic 4 (Story 4.2, marcada redundante), mantido aqui por decisão do usuário.

### Epic 4: Acesso de Sistema e Governança de Perfil
A equipe de TI consegue criar Logins de sistema/serviço sem Funcionário por trás, e criar ou editar Perfis de permissão — ambos passando por aprovação de um grupo de TI/Admin designado diretamente, não da hierarquia de RH.
**FRs covered:** FR-26, FR-27
**Depende de:** Epic 3 (mecanismo de Login/Perfil/aprovação já precisa existir; o grupo `APPROVE_SYSTEM_ACCESS` em si é semeado via schema, não criado pelo fluxo).

### Epic 5: Governança de Turno (Zero Trust)
O sistema passa a barrar sozinho qualquer chamada de API de um Perfil sujeito a bloqueio, fora da Jornada de Trabalho vigente — com plantão pré-aprovado como exceção legítima — e cada decisão (permitida ou negada) fica registrada em auditoria imutável.
**FRs covered:** FR-8, FR-9, FR-10, FR-11, FR-12
**Depende de:** Epic 1 (Jornada de Trabalho), Epic 3 (Login/Perfil já precisam existir e estar ativos) e **Epic 0 / Story 0.3 (bloqueante para FR-9, FR-10, FR-12)** — sem fuso horário por filial nem `ShiftWindowEvaluator`, não há como converter `Instant` em hora local da filial para comparar contra a Jornada.

### Epic 6: Kill Switch
Desligar ou bloquear um colaborador mata a sessão dele em menos de 1 segundo, mesmo se o mecanismo de invalidação estiver temporariamente indisponível — e reativar não devolve sessões antigas.
**FRs covered:** FR-13, FR-14
**Depende de:** Epic 2 (Funcionário) e Epic 3 (Login).

### Epic 7: Completude de API — Empresa e Funcionário
Débito técnico transversal, não mapeado a FR do PRD: fecha o buraco entre contrato OpenAPI, plano (épicos/stories) e código para os agregados Company e Employee — endpoints que já estão publicados no YAML (e em alguns casos até têm entidade/repositório prontos) mas nunca ganharam Use Case/Delegate **nem** apareceram em nenhuma story, incluindo dois já em produção declarada `done` (Epic 1). Achado em auditoria de cobertura de API feita após a Story 2.5 (que fechou o mesmo tipo de buraco para leitura de Funcionário).
**FRs covered:** Nenhuma — débito técnico de completude de API (achado de auditoria, mesmo padrão do Epic 0).
**Depende de:** Epic 1 e Epic 2 (ambos já concluídos/em andamento — este épico só adiciona cobertura sobre agregados que já existem, não cria nada novo em termos de domínio).
**Story 7.9 (adicionada numa auditoria de N+1 pedida pelo usuário):** achou 2 consultas N+1 já em produção (`GET /v1/companies` e `GET /v1/departments/{id}/positions`, ambos Epic 1, `done`) — mesmo espírito de auditoria de completude, aqui sobre performance de consulta em vez de rota faltando.

---

## Epic 0: Fundação Técnica — Tipos Temporais e Relógio Injetável

Débito técnico transversal: padroniza a representação de tempo no domínio (`Instant` para instante/`TIMESTAMPTZ`, `LocalDate` para calendário/`DATE`, `LocalTime` para hora de parede/`TIME`, nunca `LocalDateTime`) e introduz `Clock` injetável — pré-requisito de confiabilidade para Epic 5 e Epic 6.

### Story 0.1: Cobertura de Testes do Caminho Crítico de Autenticação de Sistema

Como time de plataforma,
Eu quero cobertura de teste no caminho de autenticação sistema-a-sistema (interceptor gRPC, cifra do secret, validação do secret) e um CI mínimo rodando esses testes automaticamente,
Para que os quatro bugs já identificados por auditoria fiquem provados fechados e não voltem a regredir silenciosamente.

**Pré-requisito de fato para considerar a Onda 2 (secret em repouso) entregue** — hoje o crypto e o schema estão corretos, mas o caminho de validação nunca foi exercitado por teste.

**Acceptance Criteria:**

**Given** `TokenAuthorizationInterceptor`, `SystemSecretCryptoService` e `ScosSystemServiceBean.validateSecretKey` têm hoje **zero teste**
**When** esta story é implementada
**Then** os três ganham cobertura de teste determinística (sem chamada de rede, sem sleep)

**Given** `SystemSecretCryptoService.tagLength` tem default de campo `12` mas default de `@Value` (`"${scos.security.tag-length:128}"`) é `128` — inconsistentes
**When** esta story é implementada
**Then** o default do campo é alinhado para `128`

**Given** `bootstrap.yml` referencia `${scos.security.maser-key}` (typo, falta o "t") em vez de `master-key` — presente em **dois** arquivos (`flow-organization-boot` e `server-fat`, não só um)
**When** esta story é implementada
**Then** ambos são corrigidos para `master-key`

**Given** `ScosSystemServiceBean.validateSecretKey` tinha, no HEAD commitado, condição invertida (lançava exceção quando o secret CONFERIA) e passava `code` em vez de `secretKey` para `matchesSecret` — bug real, mas **já corrigido na árvore de trabalho não commitada** no momento em que esta story foi escrita
**When** esta story é implementada
**Then** o Task correspondente é idempotente: garante o estado correto independente do ponto de partida (HEAD ou working tree), e adiciona os testes que HEAD hoje reprovaria

**Given** achado adicional (fora da lista original de bugs do prompt, confirmado em auditoria própria): `TokenAuthorizationInterceptor` captura `NoSuchElementException`, mas `ScosSystemServiceBean` sempre lança `ScosException` (nunca `NoSuchElementException`) — o catch nunca dispara, e tanto "sistema inexistente" quanto "secret incorreto" hoje vazam sem virar `UNAUTHENTICATED`
**When** esta story é implementada
**Then** o interceptor passa a capturar `ScosException`

**Given** os cenários obrigatórios de `TokenAuthorizationInterceptorTest` (KEY-ACCESS ausente/incorreto, token ausente/Base64 inválido/formato inesperado, code inexistente, caminho feliz)
**When** implementados
**Then** cada cenário negativo prova `UNAUTHENTICATED` + listener noop + `call.close()` chamado, e o caminho feliz prova `SecurityContext` montado e `handler.startCall` invocado

**Given** os cenários obrigatórios de `SystemSecretCryptoServiceTest` (roundtrip, IV/ciphertext diferentes a cada `encrypt()`, `decrypt()` com master-key diferente falha, payload corrompido falha)
**When** implementados
**Then** todos passam sem exceção de teste, com o crypto instanciado sem contexto Spring completo (reflection para os campos `@Value`)

**Given** os cenários obrigatórios de `validateSecretKey` (secret correto autentica, secret incorreto lança `SCOS_SYSTEM_002`, code inexistente lança `SCOS_SYSTEM_001`)
**When** implementados **Then** todos passam, e nenhum teste de grace period/previous-secret é incluído (guarda de escopo — pertence à Story 0.2)

**Given** o teste de wiring do `SecretKeyConverter`
**When** persiste um `ScosSystem` e lê `SECRET_KEY` direto via query nativa
**Then** prova que o valor em coluna é diferente do secret original (cifrado em repouso) **And**, ao recarregar via repositório, o campo em memória volta ao valor original em claro
**And** se falhar por erro de instanciação do converter, adicionar `@Component` a `SecretKeyConverter` é parte desta story

**Given** não existe `.github/workflows` no projeto hoje
**When** esta story é implementada
**Then** `.github/workflows/ci.yml` passa a disparar em push/PR para `feature/**` e `main`, rodando `mvn -B verify` com sucesso — o que exige `-Denforcer.skip=true` (verificado nesta auditoria: o enforcer `RequireUpperBoundDeps` falha hoje em `infrastructure`/`flow-security-starter`, débito pré-existente documentado em `project-context.md`, não desta story) **And** sem gate de JaCoCo/dependency-check (deliberadamente mínimo)

**Given** a guarda de escopo
**When** esta story é implementada
**Then** NÃO cobre `flow-organization-api` (zero testes, vira nota de backlog do Epic 0) **And** NÃO cria teste `*IT`/Testcontainers (vira nota de backlog) **And** NÃO implementa Use Case de rotação de secret **And** NÃO toca em grace period/previous-secret (Story 0.2)

### Story 0.2: Padronizar Tipos Temporais e Introduzir Clock Injetável

Como Desenvolvedor da plataforma,
Eu quero que todo campo de instante no domínio use `Instant` (nunca `LocalDateTime`) e que a hora "agora" venha sempre de um `Clock` injetável,
Para que a persistência corresponda de fato à coluna `TIMESTAMPTZ` real e testes de janela de tempo sejam determinísticos, sem `sleep`.

**Acceptance Criteria:**

**Given** as 14 ocorrências de campo `LocalDateTime` hoje mapeadas contra coluna `TIMESTAMPTZ` no módulo `domain` (`ScosSystem`, `OutboxEvent`, `OutboxEventDeadLetter`, `OutboxEventLog`, `Login`, `CompanyStatusHistory`, `EmployeeStatusHistory`, `EmployeePositionHistory`, `LoginStatusHistory`, `Cnae`, `LegalNature`, `CompanyCnaeSecondary`, `LoginProfile`, `ProfileResource`)
**When** esta story é implementada
**Then** todas passam a `Instant`, preservando nome de campo, nome de coluna e semântica
**And** `PositionWorkSchedule`/`EmployeeWorkSchedule` (`LocalTime`) e `Employee.birthDate`/`dateOfHiring`/`probationEndDate`/`Company.foundationDate`/`EmployeePositionHistory.startDate`/`endDate` (`LocalDate`) permanecem intocados — já corretos

**Given** o campo `Resource.definitionUpdatedAt` hoje é `LocalDate` contra uma coluna que hoje é `DATE` (divergência da premissa original desta story, confirmada contra o changelog)
**When** esta story é implementada
**Then** a coluna `SCOS_RESOURCE.DEFINITION_UPDATED_AT` é migrada para `TIMESTAMPTZ` via novo changeSet Liquibase com rollback, e o campo passa a `Instant`
**And** a mudança de tipo se propaga por `RegisterResourceInput` (domain/dto), `ResourceRepository.upsert` (domain/internal, native query), `RegistryResourceInput` (usecase) e `RegistreServiceImpl` (grpc-boot) — o contrato gRPC (`registry.proto`) continua enviando `updated_at` como `string`, só o parse interno muda de `LocalDate.parse` para conversão em `Instant`

**Given** `ScosSystem.matchesSecret`/`rotateSecret` hoje calculam ou comparam contra `LocalDateTime.now()` diretamente
**When** esta story é implementada
**Then** nenhuma chamada a `.now()` permanece no módulo `domain` (verificável por grep)
**And** o "agora" é sempre recebido via `Clock` injetado no `ScosSystemServiceBean` (bean novo `Clock.systemUTC()` em produção; `Clock.fixed(...)` em teste)

**Given** o bug de semântica em que `PREVIOUS_SECRET_EXPIRES_AT` grava o instante da rotação (não a expiração) e `matchesSecret` soma o grace na leitura
**When** esta story é implementada
**Then** `rotateSecret(String newRawSecret, Instant expiresAt)` passa a receber a expiração já calculada pelo Service (aplicando a política de grace vigente)
**And** `matchesSecret` só compara contra `clock.instant()`, nunca soma duração — mudar a constante de grace no futuro não altera retroativamente secrets já rotacionados

**Given** `secretKey`/`previousSecretKey` trafegam em texto claro em memória (via `SecretKeyConverter`) e `ScosSystem` hoje não tem `@ToString`
**When** esta story é implementada
**Then** `ScosSystem` ganha `@ToString` de classe excluindo os dois campos via `@ToString.Exclude`, para nunca vazar segredo em log de entidade

**Given** os testes existentes que tocam campos afetados (`ResourceServiceBeanTest` no domain, `RegistryResourcesUseCaseBeanTest` no usecase — únicos encontrados; os demais 12 campos não têm teste unitário que os referencie hoje, só `@CreationTimestamp` gerenciado pelo Hibernate)
**When** esta story é implementada
**Then** ambos são atualizados para `Instant` sem mudar a asserção de comportamento
**And** um novo `ScosSystemServiceBeanTest` (hoje inexistente — primeiro teste da classe) cobre o grace period do secret com `Clock.fixed`: válido dentro da janela, inválido após, determinístico, sem `sleep`

**Given** `BaseEntity` (biblioteca externa `scos-foundation-utils`) ainda expõe `createdAt`/`updatedAt` como `LocalDateTime`, herdado por `ScosSystem`/`Login`/`Resource`/etc.
**When** esta story é implementada
**Then** esse resíduo NÃO é corrigido aqui — é débito fora do controle deste projeto, registrado em Dev Notes e em `project-context.md`, não em código

### Story 0.3: Modelar Fuso Horário por Filial e Avaliar Janela de Turno

Como sistema,
Eu quero saber o fuso horário efetivo de cada filial e converter um `Instant` em hora local dela em um único ponto do código,
Para que a futura checagem de bloqueio de turno (Epic 5) compare corretamente contra a Jornada de Trabalho, inclusive quando o turno cruza a meia-noite.

**Acceptance Criteria:**

**Given** o fuso por filial não existe hoje (`SCOS_COMPANY` sem coluna de timezone, `SCOS_CONFIGURATION` é global, sem `ZoneId` no código — README promete a capacidade, código não entrega)
**When** esta story é implementada
**Then** `SCOS_COMPANY` ganha `TIME_ZONE VARCHAR(64)` (IANA, ex. `America/Manaus`), `NOT NULL DEFAULT 'America/Sao_Paulo'`
**And** a coluna entra editando o changeSet **baseline** de `scos_company.yml` (não um `addColumn` corretivo) — decisão do PM porque nenhum ambiente rodou esse changelog ainda; o baseline congela assim que entrar em homologação

**Given** a entidade `Company`
**When** esta story é implementada
**Then** ganha campo `timeZone` (`ZoneId`, `@Builder.Default = America/Sao_Paulo`) com `AttributeConverter` próprio (`@Component`, ao contrário do `SecretKeyConverter` existente — bug separado, não corrigido aqui)

**Given** a decisão de produto D3 (fuso é imutável — mudar reinterpretaria decisões de turno já auditadas)
**When** esta story é implementada
**Then** nenhum endpoint/Use Case expõe alteração de `timeZone` — regra só documentada, sem endpoint nesta Etapa

**Given** uma filial sem fuso próprio (cenário defensivo — hoje sempre preenchido por default, mas o campo pode ser nulo por construção explícita)
**When** o fuso efetivo é resolvido
**Then** sobe a cadeia `parentCompany` (mesmo padrão de `CompanyServiceBean.depthOf()`, não CTE) até achar o primeiro não nulo

**Given** `ShiftWindowEvaluator` (domain service novo, `domain/shift/`)
**When** avalia um `Instant` contra uma Jornada (`startTime`/`lunchStart`/`lunchEnd`/`endTime`, `LocalTime`)
**Then** converte para hora local da filial em um único ponto do código (delegates/use cases futuros não replicam `atZone()`)
**And** os limites `[startTime, endTime]` são inclusivos nas duas pontas
**And** turno que cruza meia-noite (`endTime < startTime`, ex. 22:00→06:00) é tratado corretamente — caso de teste obrigatório
**And** o intervalo de almoço é excluído do turno (decorre de FR-9: "fora do intervalo, exceto almoço")

**Given** a mesma `Instant` avaliada para filiais em fusos diferentes
**When** convertida para hora local de cada uma
**Then** produz horas locais diferentes e, quando aplicável, decisões de turno diferentes — caso de teste obrigatório

**Given** esta story
**When** concluída
**Then** as stories de bloqueio de turno da Etapa 1 (Epic 5: FR-9, FR-10, FR-12) passam a ter dependência explícita e satisfeita nela — sem Filter nem endpoint construído aqui, só o dado e o avaliador reutilizável

### Backlog do Epic 0 (notas, não stories — registrado em 2026-07-19)

Itens identificados durante a auditoria de cobertura de teste (Story 0.1) e explicitamente fora de escopo de qualquer story atual do Epic 0. Não têm story própria ainda — viram uma quando priorizados:

- **Baseline de cobertura do módulo `flow-organization-api`** — hoje zero testes (`project-context.md`, tabela de Testing Rules). Nenhum delegate tem teste, unitário ou de integração.
- **Cobertura ampliada de `flow-organization-infrastructure`** além do único teste hoje existente (`PermissionsConsistencyTest`) — o módulo não tem nenhum outro teste de contrato/config.
- **Suíte de testes `*IT` com Testcontainers fora do padrão `*ControllerTest` já em uso no `boot`** — especificamente, testes de persistência/repositório que hoje só têm alternativa em `@DataJpaTest`+H2 (Story 0.1, escopo D) ou no `boot` completo; não há hoje uma suíte `*IT` intermediária.

---

## Epic 1: Estrutura Organizacional

RH consegue montar e manter toda a estrutura organizacional do ISP: empresas/filiais em hierarquia íntegra (sem ciclos, sem filial órfã ativa em nenhum nível), departamentos, cargos, e o horário de trabalho padrão de cada cargo.

### Story 1.1: Bloquear Ciclo na Hierarquia de Empresa

Como Analista de RH,
Eu quero que o sistema rejeite qualquer ciclo na hierarquia de empresas (direto ou indireto),
Para que a árvore Empresa/Filial nunca fique estruturalmente inválida.

**Acceptance Criteria:**

**Given** Empresa A é filial de B, e B é filial de C
**When** alguém tenta definir C como filial de A (fechando o ciclo)
**Then** o sistema rejeita com `SCOS_COMPANY_004`
**And** nenhuma alteração é aplicada

**Given** Empresa sem nenhuma relação de hierarquia com outra
**When** ela é definida como matriz de uma nova filial
**Then** a operação é aceita normalmente

**Given** a checagem de ciclo é executada
**When** a hierarquia tem múltiplos níveis
**Then** a validação usa CTE recursiva (AD-7 da spine), nunca caminhada em memória Java

### Story 1.2: Ciclo de Vida Completo de Empresa

Como Analista de RH,
Eu quero ativar, inativar, bloquear e desbloquear uma Empresa sempre informando um motivo do catálogo,
Para que toda mudança de status tenha justificativa e rastro auditável.

**Acceptance Criteria:**

**Given** uma Empresa `ACTIVE`
**When** RH aciona `disable` informando um Motivo válido de `SCOS_REASON_INACTIVATE`
**Then** a Empresa passa para `INACTIVE`
**And** uma linha é gravada em `SCOS_COMPANY_STATUS_HISTORY` com o motivo

**Given** uma Empresa `INACTIVE`
**When** RH aciona `block` informando um Motivo válido de `SCOS_REASON_DISABLE`
**Then** a Empresa passa para `DISABLED`
**And** o histórico é gravado

**Given** uma transição sem Motivo informado, ou com Motivo inativo/de tipo incompatível
**When** RH tenta a transição
**Then** o sistema rejeita com o código de validação apropriado

**Given** nenhum dos 4 Use Cases (`enable`/`disable`/`block`/`unblock`) existe hoje para Company
**When** esta story é implementada
**Then** os 4 são criados (Use Case + Delegate), reaproveitando o padrão já usado por Department/Position

### Story 1.3: Guardas de Integridade ao Desativar Empresa

Como Analista de RH,
Eu quero que o sistema impeça inativar a última matriz ativa do sistema, bloquear a única empresa ativa do sistema, e desativar/bloquear qualquer Empresa com filial ativa em qualquer nível abaixo dela,
Para que a operação nunca deixe o ISP sem empresa raiz ativa nem crie filial órfã.

**Acceptance Criteria:**

**Given** existe só uma matriz ativa no sistema
**When** RH tenta inativar essa matriz
**Then** o sistema rejeita com `SCOS_COMPANY_005`

**Given** existe só uma Empresa ativa no sistema (qualquer nível)
**When** RH tenta bloquear essa Empresa
**Then** o sistema rejeita com `SCOS_COMPANY_006`

**Given** uma Empresa tem filial ativa em qualquer nível da subárvore, direto ou indireto
**When** RH tenta `disable` ou `block` essa Empresa
**Then** o sistema rejeita a operação
**And** a checagem reaproveita a mesma CTE recursiva da Story 1.1 (AD-7)

**Given** esta story depende da Story 1.2
**When** as transições `disable`/`block` ainda não existem
**Then** a Story 1.2 deve estar concluída antes desta

### Story 1.4: Departamento e Cargo (Verificação)

Como Analista de RH,
Eu quero cadastrar Departamentos e Cargos vinculados a um Departamento ativo, com ciclo enable/disable,
Para organizar a estrutura interna da empresa.

**Acceptance Criteria:**

**Given** a implementação já existente de `DepartmentServiceBean`/`PositionServiceBean` (confirmada em auditoria de código)
**When** os testes de regressão são executados
**Then** confirmam: criar/atualizar Cargo com Departamento inativo é rejeitado

**Given** um Departamento com Cargo ativo vinculado
**When** RH tenta desativar o Departamento
**Then** o sistema rejeita

**Given** um Cargo com Funcionário ativo vinculado
**When** RH tenta desativar o Cargo
**Then** o sistema rejeita (via `EmployeePositionQueryServiceBean`)

**Given** esta funcionalidade já está implementada
**When** esta story é executada
**Then** nenhum código novo é criado — só verificação/regressão

### Story 1.5: Template de Jornada de Trabalho por Cargo

Como Analista de RH,
Eu quero definir um template de Jornada de Trabalho por Cargo, com horário por dia da semana,
Para que todo Funcionário admitido nesse Cargo já nasça com uma jornada padrão.

**Acceptance Criteria:**

**Given** um Cargo ativo
**When** RH cadastra um template de Jornada para um dia da semana com `startTime < lunchStart < lunchEnd < endTime`
**Then** o template é salvo

**Given** um template com horários fora de ordem (ex.: `lunchStart` depois de `lunchEnd`)
**When** RH tenta salvar
**Then** o sistema rejeita com erro de validação

**Given** hoje só existe a entidade JPA `PositionWorkSchedule`, sem camada de aplicação
**When** esta story é implementada
**Then** cria o Use Case/Delegate do zero
**And** não inclui a cópia para o Funcionário na admissão — isso fica na Story 2.1

---

## Epic 2: Ciclo de Vida do Funcionário

RH consegue admitir um Funcionário (que já nasce com a Jornada de Trabalho do seu Cargo copiada), gerenciar todo o ciclo de vida dele, e colocá-lo em licença médica/férias.

### Story 2.1: Admissão de Funcionário com Cópia de Jornada de Trabalho

Como Analista de RH,
Eu quero admitir um Funcionário vinculado a um Cargo, com validações de idade mínima, domínio de e-mail e CPF, e a Jornada de Trabalho do Cargo copiada automaticamente para o seu registro,
Para que ele já nasça pronto para operar sob as regras corretas.

**Acceptance Criteria:**

**Given** um Cargo ativo com template de Jornada de Trabalho definido (Story 1.5)
**When** RH admite um Funcionário nesse Cargo
**Then** o Funcionário é criado com status `ACTIVE`
**And** a Jornada do Cargo é copiada para o registro efetivo do Funcionário (`EmployeeWorkSchedule`)

**Given** `EMPLOYEE_MIN_AGE` e `EMPLOYEE_EMAIL_DOMAIN` configurados em Configuration
**When** RH tenta admitir um Funcionário que viola essas regras
**Then** o sistema rejeita

**Given** um CPF já cadastrado no sistema
**When** RH tenta admitir novo Funcionário com o mesmo CPF
**Then** o sistema rejeita por duplicidade

**Given** hoje não existe nenhum Use Case/Delegate de Employee (só entidade de domínio + serviço de consulta)
**When** esta story é implementada
**Then** cria a Use Case de criação do zero

### Story 2.2: Ciclo de Vida de Funcionário

Como Analista de RH,
Eu quero ativar, inativar, bloquear e desbloquear um Funcionário sempre informando um motivo,
Para acompanhar o ciclo de vida dele com rastro auditável.

**Acceptance Criteria:**

**Given** um Funcionário `ACTIVE`
**When** RH aciona `disable` com Motivo válido
**Then** o Funcionário vira `INACTIVE`
**And** grava histórico de status

**Given** um Funcionário `INACTIVE`
**When** RH aciona `block` com Motivo válido
**Then** o Funcionário vira `DISABLED`
**And** grava histórico de status

**Given** nenhum dos 4 Use Cases existe hoje para Employee
**When** esta story é implementada
**Then** cria os 4 do zero, mesmo padrão de Company/Department

### Story 2.3: Recontratação de Funcionário (INACTIVE ou DISABLED)

Como Analista de RH,
Eu quero recontratar um Funcionário que estava `INACTIVE` ou `DISABLED`, reatribuindo empresa/cargo/supervisor se necessário,
Para reintegrar alguém em uma posição diferente.

**Acceptance Criteria:**

**Given** um Funcionário `INACTIVE` ou `DISABLED`
**When** RH aciona `rehire` informando novo Cargo/Empresa/supervisor
**Then** o Funcionário volta a `ACTIVE` na nova posição
**And** a posição anterior é fechada automaticamente (histórico de posição)

**Given** um Funcionário já `ACTIVE`
**When** RH tenta `rehire`
**Then** o sistema rejeita

**Given** `unblock` já cobre o caminho rápido sem reatribuição (Story 2.2)
**When** esta story é implementada
**Then** não duplica essa lógica

### Story 2.4: Licença/Férias — Colocar Funcionário em Licença

Como Analista de RH,
Eu quero colocar um Funcionário em licença médica ou férias informando uma data prevista de retorno,
Para registrar o afastamento sem perder o vínculo.

**Acceptance Criteria:**

**Given** um Funcionário `ACTIVE`
**When** RH aciona `disable` com Motivo "Férias" ou "Licença Médica" (catálogo `SCOS_REASON_INACTIVATE`) e informa data prevista de retorno
**Then** o Funcionário e o Login vinculado vão para o estado equivalente a inativo
**And** o Kill Switch dispara (Epic 6)

**Given** um Motivo de disable diferente de Férias/Licença Médica
**When** RH desativa o Funcionário
**Then** nenhuma data de retorno é aceita ou exigida

**Given** o gatilho automático de reativação na data prevista depende de `LoginApprovalRequest` existir
**When** esta story é implementada
**Then** o gatilho e o cancelamento automático em 5 dias úteis ficam para a story final do Epic 3

### Story 2.5: Consulta de Funcionário — Listagem e Detalhe

Como Analista de RH,
Eu quero listar Funcionários com paginação e filtros (empresa, cargo, status) e consultar o detalhe completo de um Funcionário,
Para localizar rapidamente quem eu preciso gerenciar.

**Acceptance Criteria:**

**Given** um Funcionário existente
**When** RH consulta `GET /v1/employees/{id}`
**Then** o sistema retorna os dados completos, com Supervisor/Empresa/Cargo aninhados

**Given** a listagem de Funcionários
**When** RH informa filtros de empresa, cargo e/ou status (todos opcionais, combináveis)
**Then** o sistema retorna só os Funcionários que atendem a todos os filtros informados, com paginação

**Given** o contrato hoje aninha incorretamente `getEmployeeById`/`updateEmployee` sob o path `/v1/employees/rehire` (defeito já sinalizado pelas Stories 2.1/2.3, nunca corrigido)
**When** esta story é implementada
**Then** corrige o path para `/v1/employees/{id}` — sem implementar `updateEmployee` (fora de escopo)

**Given** nenhum Use Case/Delegate de listagem/detalhe existe hoje para Employee (só o primitivo de domínio `EmployeeService.findById`, criado pela Story 2.3 para uso interno do `rehire`)
**When** esta story é implementada
**Then** cria os 2 Use Cases do zero, mesmo padrão de Company/Position (par completo/resumido no mapper, filtros opcionais via QueryDSL)

### Story 2.6: Separar Regras de Montagem de Predicate dos Repositories

Como Desenvolvedor da plataforma,
Eu quero que toda montagem dinâmica de predicate QueryDSL (`BooleanBuilder`) hoje embutida em métodos `default` de Repository — e, num caso, dentro de um Service — viva numa classe `XxxPredicates` dedicada no mesmo pacote `internal`, e que a suíte de testes do módulo `domain` volte a executar de verdade,
Para separar a regra de montagem da regra de pesquisa, com cobertura de teste unitário isolada, sem mudar nenhum comportamento existente e sem depender de workaround manual para rodar `mvn test`.

**Acceptance Criteria:**

**Given** o módulo `domain` hoje reporta `Tests run: 0` ao rodar `mvn test`, porque `maven-surefire-plugin` não tem `<version>` pinada em nenhum `pluginManagement` do monorepo — resolve para `2.17`, a versão mais antiga cacheada localmente, sem provider de JUnit 5
**When** esta story é implementada
**Then** `maven-surefire-plugin` ganha `<version>3.5.4</version>` no `pom.xml` raiz (mesmo bloco onde `maven-failsafe-plugin` já está pinado em `3.2.5`)
**And** a suíte completa volta a executar (`mvn test` deixa de reportar 0 testes silenciosamente), eliminando o workaround manual documentado nas Stories 2.3/2.5

**Given** `AddressTypeRepository`/`ContactTypeRepository` (`catalog/internal`) já têm a montagem extraída para `AddressTypePredicates`/`ContactTypePredicates` nesta sessão de trabalho (working tree, ainda não commitado)
**When** esta story é implementada
**Then** as duas classes ganham cobertura de teste unitário dedicada (`AddressTypePredicatesTest`/`ContactTypePredicatesTest`), sem mudar assinatura ou comportamento do repositório
**And** `AddressTypePredicates` ganha o cabeçalho de licença Apache 2.0 que hoje falta (inconsistência com `ContactTypePredicates`, que já tem)

**Given** os 4 repositórios simétricos de motivo (`ReasonEnableRepository`/`ReasonDisableRepository`/`ReasonActivateRepository`/`ReasonInactivateRepository`, `access/status/internal`) — hoje idênticos em forma, cada um com 3 métodos `default` montando `BooleanBuilder` inline
**When** esta story é implementada
**Then** cada um ganha sua própria `ReasonXPredicates`, mesmo padrão exato de `AddressTypePredicates`, com teste unitário dedicado por classe

**Given** `EmployeeQueryRepository`, `PositionRepository` e `DepartmentRepository` — cada um com métodos `default` que montam `BooleanBuilder` para `exists`/`findOne`/`findAllFiltered`
**When** esta story é implementada
**Then** cada um ganha sua `XxxPredicates` dedicada (`EmployeeQueryPredicates`, `PositionPredicates`, `DepartmentPredicates`), com teste unitário dedicado — `DepartmentRepository.existsByIdAndPositionsActive`, que já usa predicate direto sem `BooleanBuilder`, permanece intocado (fora do critério de escopo desta story)

**Given** `CompanyServiceBean.findAll` — único Service encontrado que monta um `BooleanBuilder` (`QCompany`) diretamente, cruzando a fronteira de `internal/` a partir da camada `service/`
**When** esta story é implementada
**Then** a montagem migra para uma nova `CompanyPredicates` em `corporate/company/internal/`, exposta por um novo método `default Page<Company> findAllFiltered(StatusCompany, String, Pageable)` em `CompanyRepository`
**And** `CompanyServiceBean.findAll` passa só a chamar `companyRepository.findAllFiltered(status, name, pageable)`, sem importar `BooleanBuilder`/`QCompany`
**And** o comportamento observável não muda: sem filtro nenhum, a consulta continua retornando todas as empresas (fallback `id.isNotNull()` quando o predicate está vazio, preservado)

**Given** todas as classes `XxxPredicates` desta story
**When** os testes unitários são escritos
**Then** seguem o mesmo padrão: JUnit 5 puro (sem Spring, sem banco), no pacote `internal` (visibilidade package-private preservada), asserção via AssertJ comparando `predicate.toString()` com o predicate esperado montado no teste — nenhuma dependência de Testcontainers/H2

**Given** nenhuma regra de negócio muda nesta story — é refatoração pura (mesma assinatura pública, mesmo resultado de query)
**When** esta story é implementada
**Then** a suíte de testes já existente (`domain`, `usecase`, `boot`) continua passando sem nenhuma alteração de asserção fora dos arquivos tocados por esta story

---

## Epic 3: Login do Funcionário com Aprovação

RH consegue criar um Login para um Funcionário, e ele só fica utilizável depois que um Supervisor ou Gerente aprova — nunca o próprio solicitante — com escalonamento automático se ninguém responder a tempo. O mesmo vale para reativar um Login existente ou aumentar o acesso dele com um novo Perfil.

### Story 3.1: Criação de Login Vinculado a Funcionário (mecânica base)

Como Analista de RH,
Eu quero criar um Login para um Funcionário,
Para que ele tenha uma credencial de acesso ao sistema.

**Acceptance Criteria:**

**Given** um Funcionário ativo
**When** RH cria um Login para ele
**Then** o Login nasce em estado `PENDING_APPROVAL`
**And** não autentica nem é aceito em nenhuma chamada de API enquanto pendente

**Given** um Login aprovado (Story 3.2)
**When** ele transita para `ACTIVE`
**Then** sincroniza com o Keycloak via Saga/Outbox

**Given** o código hoje usa uma permissão única para block/unblock
**When** esta story é implementada
**Then** `BLOCK_LOGIN` e `UNBLOCK_LOGIN` ficam separadas (schema já implementado — story cria só a camada Java)

**Given** nenhuma story do roadmap implementa consulta de Login
**When** esta story é implementada
**Then** `GET /v1/logins/{id}`, `GET /v1/logins` e `GET /v1/employees/{id}/logins` passam a funcionar
**And** sem isso RH não teria como descobrir o Login criado nem seu status, depois da criação

### Story 3.2: Cadeia de Aprovação de Criação de Login

Como Supervisor ou Gerente do Departamento,
Eu quero aprovar ou rejeitar uma solicitação de Login,
Para garantir que ninguém ganha acesso sem o aval de outra pessoa.

**Acceptance Criteria:**

**Given** um Login em `PENDING_APPROVAL`
**When** o sistema resolve o aprovador
**Then** segue a cadeia Supervisor → Gerente do Departamento → Grupo `APPROVE_SYSTEM_ACCESS`, pulando nível ausente ou em conflito com o solicitante

**Given** um aprovador tenta decidir sua própria solicitação
**When** chama a ação de aprovar
**Then** a API rejeita a tentativa
**And** a única exceção é a válvula de última instância, sempre marcada como "aprovação por exceção" na auditoria

**Given** 1 dia útil sem decisão no nível atual
**When** o SLA estoura
**Then** escala automaticamente para o próximo nível da cadeia

**Given** o Funcionário é desligado enquanto seu Login segue `PENDING_APPROVAL`
**When** o aprovador tenta decidir
**Then** o sistema reverifica o status do Funcionário no momento da decisão
**And** bloqueia a aprovação se o Funcionário já não estiver mais ativo

**Given** nenhuma notificação existe ainda (Motor de Notificação é Etapa 3/P2)
**When** o aprovador quer agir
**Then** consulta solicitações pendentes via `GET` a qualquer momento, sem depender de aviso proativo

**Given** hoje não existe API para definir o Gerente do Departamento (nível 2 da cadeia)
**When** esta story é implementada
**Then** o endpoint de atualização de Departamento já existente passa a aceitar o gerente
**And** sem isso o nível `MANAGER` nunca resolveria de verdade

**Given** RH quer o histórico de decisões de um Login específico, ou saber quem pode decidir hoje no nível `SYSTEM_ACCESS_GROUP`
**When** consulta a API de solicitações de aprovação
**Then** consegue filtrar por Login **And** consegue listar quem tem a permissão `APPROVE_SYSTEM_ACCESS` no momento

**Given** não existe notificação (Etapa 3/P2) e a resposta da API não dizia quem precisa decidir agora
**When** consulta uma solicitação de aprovação
**Then** a resposta inclui o aprovador resolvido para o nível atual (`null` quando o nível é `SYSTEM_ACCESS_GROUP`, que não resolve uma pessoa específica)

### Story 3.3: Aprovação de Reativação de Login

Como Supervisor ou Gerente do Departamento,
Eu quero que reativar um Login exija a mesma aprovação de criação,
Para nunca reativar acesso sem aval humano.

**Acceptance Criteria:**

**Given** um Login `INACTIVE` ou `BLOCKED`
**When** RH aciona `enable` ou `unblock`
**Then** a solicitação entra na mesma cadeia de 3 níveis da Story 3.2, sem redução de níveis

**Given** a solicitação de reativação está pendente
**When** o Login permanece no seu estado atual
**Then** nunca transita para `ACTIVE` sem a aprovação completa

### Story 3.4: Perfis, Recursos e Aprovação de Mudança de Perfil

Como Supervisor ou Gerente do Departamento,
Eu quero aprovar toda mudança que aumenta o acesso de um Login existente,
Para controlar escalada de privilégio.

**Acceptance Criteria:**

**Given** um Login `ACTIVE`
**When** RH ou supervisor solicita troca do Perfil principal ou adição de Perfil adicional
**Then** a solicitação entra em aprovação
**And** o Login continua `ACTIVE`, operando com o Perfil atual, enquanto aguarda

**Given** a aprovação é concedida
**When** a decisão é registrada
**Then** o novo Perfil passa a valer, sujeito à defasagem de até 30 min das views de autoridade

**Given** uma solicitação de remoção de Perfil adicional (redução de acesso)
**When** ela é feita
**Then** não exige aprovação

**Given** o bug conhecido de `vw_login_context` (Perfil adicional não soma permissão)
**When** esta story é implementada
**Then** corrige a view — pré-requisito para Perfil adicional funcionar de verdade

### Story 3.5: Retorno Automático Assistido de Licença/Férias

Como sistema,
Eu quero abrir sozinho a solicitação de reativação de Login na data prevista de retorno de uma licença/férias,
Para que RH não precise lembrar de reativar manualmente.

**Acceptance Criteria:**

**Given** a data prevista de retorno (Story 2.4) chega
**When** o job agendado roda
**Then** abre um `LoginApprovalRequest` com `escalationPolicy=AUTO_CANCEL`
**And** ainda exige aprovação humana, na mesma cadeia, mas sem escalonamento indefinido

**Given** ninguém decide em 5 dias úteis
**When** o prazo estoura
**Then** a solicitação é cancelada automaticamente (`CANCELLED`)
**And** RH precisa reabrir manualmente quando a pessoa de fato retornar

**Given** o Funcionário foi desligado por outro motivo antes da data prevista
**When** a data chegaria
**Then** o gatilho automático não dispara

### Story 3.6: Aprovação de Criação/Alteração de Perfil e Recursos

Como titular da permissão `APPROVE_SYSTEM_ACCESS`,
Eu quero aprovar toda criação de um novo Perfil e toda mudança no conjunto de Recursos de um Perfil existente,
Para controlar escalada de privilégio na origem — antes de qualquer Login sequer poder ser trocado para esse Perfil.

**Acceptance Criteria:**

**Given** RH aciona a criação de um Perfil ou a substituição do conjunto de Recursos de um Perfil existente
**When** a solicitação é enviada
**Then** entra em aprovação de 1 nível só (sem cadeia Supervisor/Gerente, sem SLA — FR-27 não prevê escalonamento, diferente de FR-23)
**And** nada muda de fato (Perfil não existe, Recursos efetivos não mudam) até a decisão

**Given** Logins que já usam um Perfil com mudança de Recursos pendente
**When** a solicitação ainda não foi decidida
**Then** continuam com o conjunto de Recursos anterior em vigor (FR-27, literal)

**Given** a solicitação é aprovada
**When** a decisão é registrada
**Then** o Perfil passa a existir (criação) ou o conjunto de Recursos efetivo é substituído pelo proposto (alteração)

**Given** a solicitação é rejeitada
**When** a decisão é registrada
**Then** nada muda — Perfil proposto nunca existe, ou Recursos efetivos permanecem os anteriores

### Story 3.7: Cascade de Status — Funcionário Desativado/Bloqueado Inativa seus Logins

Como sistema,
Eu quero que desativar ou bloquear um Funcionário inative automaticamente todos os Logins `ACTIVE` vinculados a ele,
Para que ninguém continue com acesso depois que o vínculo com a empresa muda.

**Acceptance Criteria:**

**Given** um Funcionário `ACTIVE` com Logins `ACTIVE` vinculados
**When** RH aciona `disable` ou `block` no Funcionário (Story 2.2, já implementada)
**Then** todos os Logins `ACTIVE` vinculados transitam para `INACTIVE`, disparando Saga Keycloak para cada um
**And** este comportamento já estava documentado em `etc/doc/usecase/03-funcionario.md` e no FR-28 completo, deferido pelas Stories 2.2/2.4 explicitamente pra "a story final do Epic 3"

**Given** um Funcionário reativado
**When** a reativação é confirmada
**Then** nenhum Login vinculado é reativado automaticamente (FR-14) — cada Login segue seu próprio fluxo de aprovação (Story 3.3)

**Given** um Login já `BLOCKED`/`INACTIVE`/`PENDING_APPROVAL`/`REJECTED` vinculado ao Funcionário
**When** o cascade roda
**Then** só os Logins `ACTIVE` são tocados — os demais permanecem como estavam

---

## Epic 4: Acesso de Sistema e Governança de Perfil

A equipe de TI consegue criar Logins de sistema/serviço sem Funcionário por trás, e criar ou editar Perfis de permissão — ambos passando por aprovação de um grupo de TI/Admin designado diretamente, não da hierarquia de RH.

### Story 4.1: Criação de Login de Sistema/Serviço (EXTERNAL/SERVICE)

Como membro de TI,
Eu quero criar um Login sem Funcionário vinculado,
Para que ferramentas ou integrações automatizadas consumam a API com identidade própria.

**Acceptance Criteria:**

**Given** TI cria um Login do tipo `EXTERNAL` ou `SERVICE` (sem `employeeId`)
**When** a solicitação é enviada
**Then** entra em `PENDING_APPROVAL`
**And** o aprovador é resolvido diretamente pelo grupo `APPROVE_SYSTEM_ACCESS`, sem hierarquia e sem supervisor

**Given** um membro do grupo tenta aprovar sua própria solicitação
**When** chama a ação de aprovar
**Then** o sistema rejeita, exceto pela válvula de última instância

**Given** ninguém do grupo decide em 1 dia útil
**When** o SLA estoura
**Then** o sistema renotifica o grupo inteiro, sem hierarquia adicional para escalar

**Given** um Login órfão precisa ser reativado
**When** `enable`/`unblock` é acionado
**Then** segue o mesmo mecanismo de aprovação desta story

### Story 4.2: Criação e Edição de Perfil com Aprovação

**⚠️ Redundante — já coberta pela Story 3.6 (Epic 3).** Ao criar as stories do Epic 3, um gap de FR-27 foi identificado (nenhuma das 5 stories originais do Epic 3 cobria criação/edição de Perfil com aprovação) e resolvido criando a Story 3.6, sem checar que esta Story 4.2 já existia com o mesmo escopo. Confirmado numa validação cruzada entre épicos: são a mesma feature. Decisão do usuário: manter o trabalho de design na Story 3.6 (arquivo `3-6-aprovacao-criacao-alteracao-perfil-recursos.md`) em vez de duplicar aqui — **não implementar esta story**, ela existe só como registro histórico do planejamento original. Ver `_bmad-output/implementation-artifacts/3-6-aprovacao-criacao-alteracao-perfil-recursos.md`.

Como membro de TI,
Eu quero que criar um novo Perfil ou editar os Recursos de um Perfil existente exija aprovação,
Para controlar o impacto de segurança sobre todos os Logins que usam aquele Perfil.

**Acceptance Criteria:**

**Given** um novo Perfil é criado
**When** TI submete a solicitação
**Then** o Perfil fica pendente
**And** não pode ser atribuído a nenhum Login até ser aprovado

**Given** os Recursos de um Perfil existente são alterados
**When** a mudança é submetida
**Then** os Logins que já usam esse Perfil continuam com o conjunto de Recursos anterior até a aprovação

**Given** a aprovação é concedida
**When** a decisão é registrada
**Then** o Perfil (novo ou editado) passa a valer

**Given** o mesmo grupo e mecanismo da Story 4.1
**When** esta story é implementada
**Then** reaproveita a mesma cadeia, válvula de exceção e SLA

---

## Epic 5: Governança de Turno (Zero Trust)

O sistema passa a barrar sozinho qualquer chamada de API de um Perfil sujeito a bloqueio, fora da Jornada de Trabalho vigente — com plantão pré-aprovado como exceção legítima — e cada decisão fica registrada em auditoria imutável.

**Bloqueado por Epic 0 / Story 0.3** (fuso horário por filial + `ShiftWindowEvaluator`) para as stories que efetivamente convertem `Instant` em hora local (5.2, 5.4).

### Story 5.1: Classificação de Perfil para Bloqueio de Turno

Como Analista de RH,
Eu quero marcar quais Perfis exigem bloqueio de turno e quais têm acesso irrestrito,
Para decidir quem fica sujeito à regra e quem tem isenção legal.

**Acceptance Criteria:**

**Given** um Perfil sem marcação
**When** usado por um Login
**Then** mantém o comportamento atual, sem checagem de turno

**Given** um Perfil marcado como "acesso irrestrito"
**When** usado por um Login
**Then** fica isento da checagem de turno de forma contínua — não expira, não é aprovação pontual

**Given** a classificação de um Perfil muda
**When** aplicada
**Then** vale para todos os Logins vinculados àquele Perfil, respeitando a defasagem de até 30 min das views de autoridade

### Story 5.2: Bloqueio de Requisição Fora da Jornada de Trabalho

Como sistema,
Eu quero avaliar toda requisição de um Login sujeito a bloqueio de turno contra a Jornada de Trabalho vigente,
Para negar acesso fora do horário mesmo com sessão válida.

**Acceptance Criteria:**

**Given** um Login cujo Perfil exige bloqueio de turno
**When** faz uma requisição dentro do intervalo `[startTime, endTime]` do dia da semana corrente
**Then** a requisição segue o fluxo normal

**Given** a mesma situação fora do intervalo
**When** a requisição chega
**Then** retorna `403 Forbidden`, nenhum dado de negócio é processado

**Given** o Login não tem Jornada cadastrada para o dia da semana corrente
**When** faz uma requisição
**Then** é tratado como fora de turno (fail-closed)

**Given** a posição do Filter na cadeia de segurança
**When** esta story é implementada
**Then** o `ShiftEnforcementFilter` é registrado via `addFilterAfter` do filtro JWT e `addFilterBefore` do filtro de idempotência (AD-2/AD-3)

### Story 5.3: Auditoria Imutável de Decisão de Turno

Como Analista de RH,
Eu quero que toda avaliação de bloqueio de turno gere um registro de auditoria imutável,
Para ter prova em caso de disputa trabalhista.

**Acceptance Criteria:**

**Given** qualquer avaliação de turno, permitida ou negada
**When** acontece
**Then** grava uma linha em `SCOS_SHIFT_ENFORCEMENT_LOG` com quem, quando, endpoint, decisão, dentro/fora da janela

**Given** um registro já gravado
**When** alguém tenta editar ou deletar via qualquer API
**Then** é bloqueado (trigger `fn_block_delete`, já implementada no schema)

**Given** o Funcionário é desligado
**When** o tempo passa
**Then** o registro é retido por no mínimo 5 anos após o desligamento

### Story 5.4: Plantão Pré-Aprovado

Como RH ou gestor,
Eu quero cadastrar previamente uma janela de plantão para um Login/Funcionário,
Para permitir acesso legítimo fora da Jornada regular sem abrir mão do controle.

**Acceptance Criteria:**

**Given** uma janela de plantão cadastrada (início/fim)
**When** uma requisição chega dentro dessa janela
**Then** é liberada mesmo fora da Jornada de Trabalho regular

**Given** uma liberação por plantão
**When** registrada
**Then** gera o mesmo log de auditoria da Story 5.3, identificando explicitamente que a liberação ocorreu via plantão

**Given** nenhuma janela cadastrada e a requisição está fora da Jornada
**When** ela chega
**Then** é tratada como acesso irrestrito (Story 5.1) ou bloqueada

---

## Epic 6: Kill Switch

Desligar ou bloquear um colaborador mata a sessão dele em menos de 1 segundo, mesmo se o mecanismo de invalidação estiver temporariamente indisponível — e reativar não devolve sessões antigas.

### Story 6.1: Invalidação Síncrona de Sessões (Kill Switch)

Como Analista de RH,
Eu quero que desligar ou bloquear um Funcionário/Login invalide imediatamente todas as sessões dele,
Para eliminar o risco de conta órfã.

**Acceptance Criteria:**

**Given** um Funcionário ou Login transita para `DISABLED`/`BLOCKED`
**When** a transição é confirmada
**Then** o commit no banco sempre sucede primeiro
**And** a escrita no denylist Redis acontece na mesma thread da requisição, imediatamente depois

**Given** a escrita no Redis falha
**When** isso acontece
**Then** o commit permanece válido — RH nunca é bloqueado por essa dependência
**And** o sistema aciona retry automático e alerta obrigatório

**Given** uma requisição chega com token de uma sessão já invalidada
**When** o filtro de autenticação consulta o denylist
**Then** retorna `401` em menos de 1 segundo após a transição de status

**Given** o Redis está inacessível no momento da consulta
**When** a requisição chega
**Then** falha fechado — nega o acesso, nunca libera por incerteza

**Given** a chamada à Keycloak Admin API
**When** ocorre em paralelo à escrita no Redis
**Then** é defesa em profundidade, nunca o mecanismo que efetivamente barra a requisição

### Story 6.2: Reativação Não Restaura Sessões Antigas

Como sistema,
Eu quero que reativar um Funcionário ou Login nunca restaure uma sessão anterior,
Para forçar reautenticação sempre que o acesso volta.

**Acceptance Criteria:**

**Given** um Funcionário ou Login reativado via `enable`/`unblock`
**When** a reativação é confirmada
**Then** nenhuma sessão ou token anterior volta a ser válido — o usuário precisa autenticar novamente

**Given** um Funcionário reativado
**When** isso acontece
**Then** o status dos Logins vinculados não é revertido automaticamente — é ação separada, cada Login segue seu próprio fluxo de aprovação (Epic 3)

---

## Epic 7: Completude de API — Empresa e Funcionário

Débito técnico transversal: fecha o buraco entre contrato OpenAPI, plano (épicos/stories) e código para os agregados Company e Employee. Achado em auditoria de cobertura de API após a Story 2.5 — cada operação abaixo já está publicada no YAML, mas nunca ganhou Use Case/Delegate nem apareceu em nenhuma story anterior, incluindo agregados de um épico já `done` (Epic 1).

### Story 7.1: Consulta de Empresa — Histórico de Status, Hierarquia e Filiais

Como Analista de RH,
Eu quero consultar o histórico de status de uma Empresa, a árvore de hierarquia completa a partir dela, e a lista das filiais diretas,
Para auditar transições passadas e entender a estrutura organizacional sem montar a árvore manualmente.

**Acceptance Criteria:**

**Given** uma Empresa existente **When** RH consulta `GET /v1/companies/{id}/status-history` **Then** retorna paginado, só leitura, populado exclusivamente pelas transições `enable`/`disable`/`block`/`unblock` já existentes (Story 1.2/1.3)

**Given** uma Empresa existente **When** RH consulta `GET /v1/companies/{id}/hierarchy` **Then** retorna a árvore recursiva completa (matriz + filiais em todos os níveis)

**Given** uma Empresa existente **When** RH consulta `GET /v1/companies/{id}/branches` **Then** retorna só as filiais de primeiro nível, paginadas, com filtro opcional de `status`

**Given** nenhuma dessas 3 rotas tem Use Case/Delegate hoje (só o contrato publicado) **When** esta story é implementada **Then** cria os 3 do zero — nenhuma mudança de schema, Liquibase ou permissão (`GET_COMPANY_STATUS_HISTORY`/`GET_COMPANY` já existem)

### Story 7.2: Contato e Endereço de Empresa

Como Analista de RH,
Eu quero cadastrar, consultar, atualizar e remover contatos e endereços de uma Empresa,
Para manter os dados de contato dela completos e corretos.

**Acceptance Criteria:**

**Given** uma Empresa existente **When** RH gerencia contatos via `GET/POST /v1/companies/{companyId}/contacts` e `GET/PUT/DELETE /v1/companies/{companyId}/contacts/{id}` **Then** o CRUD completo funciona, respeitando as UKs compostas já existentes no schema (`PHONE`/`EMAIL`/`CONTACT_TYPE_ID` por `COMPANY_ID`)

**Given** uma Empresa existente **When** RH gerencia endereços via `GET/POST /v1/companies/{companyId}/addresses` e `GET/PUT/DELETE /v1/companies/{companyId}/addresses/{id}` **Then** o CRUD completo funciona — o `addressId` referencia um endereço externo cuja integridade é garantida pela aplicação, não por FK de schema (mesmo padrão já usado pelo endereço do Funcionário admitido na Story 2.1)

**Given** nenhuma dessas 2 famílias de rota (10 operações no total) tem Use Case/Delegate hoje **When** esta story é implementada **Then** cria as camadas do zero, reaproveitando os padrões de catálogo já existentes (`ContactType`/`AddressType`) para os tipos referenciados

### Story 7.3: CNAE Secundário de Empresa

Como Analista de RH,
Eu quero listar, adicionar e remover CNAEs secundários de uma Empresa,
Para registrar todas as atividades econômicas dela além da principal.

**Acceptance Criteria:**

**Given** uma Empresa existente **When** RH lista via `GET /v1/companies/{companyId}/cnaes-secondary`, adiciona via `POST` ou remove via `DELETE /v1/companies/{companyId}/cnaes-secondary/{cnaeId}` **Then** as 3 operações funcionam — sem `PUT`/update individual (não existe no contrato)

**Given** nenhuma das 3 operações tem Use Case/Delegate hoje **When** esta story é implementada **Then** cria as camadas do zero, reaproveitando `Cnae` (CNAE principal, já implementado) como catálogo de referência

### Story 7.4: Catálogo de Motivo de Mudança de Cargo

Como Analista de RH,
Eu quero cadastrar, consultar, atualizar, ativar e inativar motivos de mudança de cargo,
Para que o catálogo usado por `rehire` (Story 2.3) deixe de depender de seed manual no banco.

**Acceptance Criteria:**

**Given** hoje só existem a entidade JPA `ReasonPositionChange` e o repositório (confirmado: a Story 2.3 precisou semear um registro inativo direto no banco de teste porque não existe API para desativar um motivo) **When** esta story é implementada **Then** cria as 6 operações do zero (`getAll`/`get`/`create`/`update`/`enable`/`disable`, path `/v1/reason-position-change`), mesmo padrão exato já usado pelos 4 catálogos irmãos já implementados (`ReasonActivate`/`ReasonInactivate`/`ReasonDisable`/`ReasonEnable`) — `ReasonPositionChange` não tem campo `entityType` (diferente dos 4 irmãos), só `active`

**Given** os 3 registros seed já existentes (`NEW_HIRE`/`PROMOTION`/`TRANSFER`, todos `active=true`) e o registro `ARCHIVED_REASON` adicionado manualmente pela Story 2.3 para testes **When** esta story é implementada **Then** nenhum dado de seed muda — só a camada de aplicação é criada por cima do que já existe

**Given** nenhuma permissão nova é necessária **When** esta story é implementada **Then** reaproveita `GET_REASON_POSITION_CHANGE`/`CREATE_REASON_POSITION_CHANGE`/`UPDATE_REASON_POSITION_CHANGE`/`ENABLE_REASON_POSITION_CHANGE`/`DISABLE_REASON_POSITION_CHANGE`, já cadastradas em `ScosOrganizationPermission`

### Story 7.5: Consulta de Funcionário — Histórico de Status, Hierarquia, Subordinados e Histórico de Cargo

Como Analista de RH,
Eu quero consultar o histórico de status, a cadeia de supervisores, os subordinados diretos e o histórico de cargos de um Funcionário,
Para auditar sua trajetória sem precisar reconstruir tudo manualmente no banco.

**Acceptance Criteria:**

**Given** um Funcionário existente **When** RH consulta `GET /v1/employees/{id}/status-history` **Then** retorna paginado, populado pelas transições `enable`/`disable`/`block`/`unblock`/`rehire` já existentes (Stories 2.2/2.3) — permissão `GET_EMPLOYEE_STATUS_HISTORY` já existe

**Given** um Funcionário existente **When** RH consulta `GET /v1/employees/{id}/hierarchy` **Then** retorna a cadeia de supervisores em árvore ascendente

**Given** um Funcionário existente **When** RH consulta `GET /v1/employees/{id}/subordinates` **Then** retorna, paginado, os Funcionários que reportam diretamente a ele

**Given** um Funcionário existente **When** RH consulta `GET /v1/employees/{id}/position-history` **Then** retorna paginado, do mais recente para o mais antigo, o histórico já gravado por `create` (Story 2.1) e `rehire` (Story 2.3) via `EmployeePositionHistory` — a linha com `endDate` nulo é a atribuição vigente

**Given** nenhuma dessas 4 rotas tem Use Case/Delegate hoje **When** esta story é implementada **Then** cria as 4 do zero — nenhuma mudança de schema, Liquibase ou permissão

### Story 7.6: Transferência de Funcionário

Como Analista de RH,
Eu quero transferir um Funcionário para outra empresa, filial, cargo ou supervisor sem passar pelo fluxo de `rehire` (que exige `INACTIVE`),
Para reorganizar um Funcionário `ACTIVE` sem precisar desativá-lo antes.

**Acceptance Criteria:**

**Given** um Funcionário `ACTIVE` **When** RH aciona `PATCH /v1/employees/{id}/transfer` informando nova empresa/cargo/supervisor **Then** os vínculos são atualizados **And** uma nova linha de `EmployeePositionHistory` é gravada quando o cargo muda, fechando a anterior (mesmo trigger `trg_close_previous_position` já usado por `rehire`, Story 2.3)

**Given** a descrição do contrato menciona "Mudança de empresa dispara Saga Keycloak TYPE=UPDATE" **When** esta story é implementada **Then** o disparo da Saga/Outbox para o Keycloak **não** é implementado aqui — o mecanismo de Saga/Outbox em si só existe a partir do Epic 3 (Login), ainda não construído; mesmo padrão de guarda de escopo já usado pela Story 2.2 para o cascade de Login em `disable`/`block`

**Given** nenhum Use Case/Delegate existe hoje para esta rota **When** esta story é implementada **Then** cria do zero, reaproveitando os privados já existentes em `EmployeeServiceBean` (`findActiveCompanyOrThrow`/`findActivePositionOrThrow`/`resolveActiveSupervisor`) — sem tocar em status, diferente de `rehire`

### Story 7.7: Contato e Endereço de Funcionário

Como Analista de RH,
Eu quero cadastrar, consultar, atualizar e remover contatos e endereços de um Funcionário,
Para manter os dados de contato dele completos e corretos, além dos já semeados na admissão.

**Acceptance Criteria:**

**Given** um Funcionário existente **When** RH gerencia contatos via `GET/POST /v1/employees/{employeeId}/contacts` e `GET/PUT/DELETE /v1/employees/{employeeId}/contacts/{id}` **Then** o CRUD completo funciona

**Given** um Funcionário existente **When** RH gerencia endereços via `GET/POST /v1/employees/{employeeId}/addresses` e `GET/PUT/DELETE /v1/employees/{employeeId}/addresses/{id}` **Then** o CRUD completo funciona

**Given** nenhuma dessas 2 famílias de rota tem Use Case/Delegate hoje **When** esta story é implementada **Then** cria as camadas do zero, mesmo padrão da Story 7.2 (Company Contact/Address) — considerar extrair um Use Case genérico só se a duplicação entre as duas stories ficar evidente na implementação, não decidir isso agora no plano

### Story 7.8: Jornada de Trabalho Efetiva do Funcionário — API Completa

Como Analista de RH,
Eu quero consultar, criar, atualizar e remover a jornada de trabalho efetiva de um Funcionário, dia a dia,
Para ajustar o horário dele depois da cópia inicial feita na admissão (Story 2.1), sem depender de acesso direto ao banco.

**Acceptance Criteria:**

**Given** a Story 2.1 já grava `EmployeeWorkSchedule` (cópia do template do Cargo na admissão), mas nenhuma rota própria existe para gerenciar esse registro depois **When** RH consulta `GET /v1/employees/{employeeId}/work-schedule` **Then** retorna array direto (sem paginação — teto real de 7 registros, um por dia da semana), sem inferir o template do Cargo como fallback para dia sem registro

**Given** um Funcionário existente **When** RH cria via `POST` (dia da semana ainda sem registro) ou atualiza/remove via `PUT`/`DELETE /v1/employees/{employeeId}/work-schedule/{dayOfWeek}` **Then** as 3 operações funcionam, reaproveitando a mesma validação cronológica estrita já usada por `PositionWorkSchedule` (Story 1.5): `startTime < lunchStart < lunchEnd < endTime`

**Given** nenhuma das 4 operações tem Use Case/Delegate hoje (só a entidade/repositório, escritos internamente pela Story 2.1) **When** esta story é implementada **Then** cria as 4 do zero — nenhuma mudança de schema, Liquibase ou permissão (`GET_EMPLOYEE_WORK_SCHEDULE`/`CREATE_EMPLOYEE_WORK_SCHEDULE`/`UPDATE_EMPLOYEE_WORK_SCHEDULE`/`DELETE_EMPLOYEE_WORK_SCHEDULE` já existem)

---

### Story 7.9: Corrigir Consultas N+1 em Listagem de Empresa e Cargo

Como Analista de RH,
Eu quero que listar Empresas e Cargos não faça 1 consulta extra ao banco por linha da página,
Para que a listagem continue rápida conforme o volume de dados cresce, e para que `GET /v1/departments/{departmentId}/positions` pare de devolver `department` sempre `null` (bug de contrato, achado junto).

**Acceptance Criteria:**

**Given** `GET /v1/companies` **When** a página tem Empresas com `parentCompanyId` preenchido (filiais) **Then** a consulta não dispara 1 SELECT extra por filial pra resolver o nome/CNPJ da matriz — dado que a resposta de lista (`Companies`) nem expõe isso, hoje descartado depois de buscado

**Given** `GET /v1/departments/{departmentId}/positions` **When** a página é montada **Then** cada Cargo retorna o `department` populado (hoje sempre `null`, quebrando o contrato) **And** isso não custa 1 consulta por Cargo — só 1 consulta total, já que todos os Cargos da página pertencem ao mesmo Departamento (`departmentId` já é filtro obrigatório)

**Given** `GET /v1/companies/{id}` e `GET /v1/positions/{id}` (detalhe, 1 linha) **When** consultados **Then** continuam retornando `parentCompany`/`department` completos, sem nenhuma mudança de comportamento — só a listagem muda
