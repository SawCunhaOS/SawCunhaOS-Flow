---
title: 'Edge Case Hunter — Revisão de PRD: SawCunhaOS (Motor Fundacional) - Fase 1'
method: bmad-review-edge-case-hunter
target: prd.md
created: '2026-07-18'
---

# Edge Case Hunter — PRD SawCunhaOS Motor Fundacional Fase 1

## Método

Rastreamento exaustivo de caminhos: cada FR e cada Jornada de Usuário (UJ) do PRD foi percorrida, mapeando estados/transições declarados (Company: `ACTIVE⇄INACTIVE⇄DISABLED`; Employee: `ACTIVE⇄INACTIVE⇄DISABLED`; Login: `PENDING_APPROVAL→ACTIVE⇄INACTIVE⇄BLOCKED`/`REJECTED`) e a cadeia de aprovação de 3 níveis (Supervisor → Gerente → `APPROVE_SYSTEM_ACCESS`). São reportados **apenas** os ramos/condições de contorno sem tratamento explícito no texto do PRD. Gaps já auto-declarados pelo autor (`[NOTE FOR PM: ...]`, `[ASSUMPTION: ...]`) não são reportados de novo — só ângulos novos que esses avisos não cobrem. Não há classificação de severidade nem comentário de qualidade: cada item é (localização → condição de disparo → o que falta → consequência potencial).

Total de achados: **24** edge cases não tratados, distribuídos nos 4 focos solicitados (19 itens) + 3 achados adicionais da varredura completa de FR-1 a FR-22 e UJ-1 a UJ-17.

---

## Foco 1 — Interações entre FR-23 a FR-28 (cadeia de aprovação)

### EC-01 — Funcionário desligado enquanto o próprio Login está `PENDING_APPROVAL`
**Localização:** FR-23, FR-6, UJ-6, UJ-7
**Condição de disparo:** Marina cria o Login de um Funcionário (FR-23, estado `PENDING_APPROVAL`); antes de qualquer aprovador decidir, o mesmo Funcionário é desligado (`Employee → DISABLED`, motivo demissão/investigação).
**Lacuna:** Nenhum FR (23, 6, 13) descreve o que acontece com uma solicitação de Login **ainda não aprovada** quando o Funcionário-alvo deixa de existir como ativo. Não há regra de cancelamento automático, nem de bloqueio da aprovação, nem de notificação ao aprovador de que o contexto mudou.
**Consequência potencial:** Um supervisor pode aprovar um Login para um Funcionário que já não deveria ter acesso, criando um Login `ACTIVE` que precisa ser imediatamente desfeito por um segundo processo manual — ou o aprovador aprova sem saber que o Funcionário foi desligado nesse intervalo.

### EC-02 — Aprovador fica indisponível *depois* de a cadeia já ter sido resolvida e notificada
**Localização:** FR-23 (resolução da cadeia), FR-24, FR-25
**Condição de disparo:** Supervisor de um Funcionário é resolvido e notificado no instante T; em T+2h (ainda dentro do SLA de 1 dia útil) esse mesmo supervisor é desligado/bloqueado.
**Lacuna:** O texto define "cada nível pulado se ausente" como condição de **resolução** da cadeia, mas não diz se essa checagem é reavaliada continuamente enquanto a solicitação está pendente ou só uma vez, no momento da criação. Não há regra de re-resolução/escalonamento imediato quando o aprovador designado deixa de existir/fica indisponível *depois* de já ter sido notificado.
**Consequência potencial:** A solicitação fica presa esperando decisão de alguém que não pode mais decidir, consumindo o SLA de 1 dia útil inteiro antes de escalar — atraso evitável não coberto pela contra-métrica SM-C3 como um caso distinto de "ausência detectável antecipadamente".

### EC-03 — Duas solicitações concorrentes de mudança de Perfil (FR-25) para o mesmo Login
**Localização:** FR-25
**Condição de disparo:** RH solicita adição do Perfil X ao Login de Carla; antes da decisão, o supervisor de Carla solicita adição do Perfil Y ao mesmo Login.
**Lacuna:** FR-25 não define se múltiplas solicitações de mudança de Perfil podem coexistir pendentes para o mesmo Login, se há serialização/lock, nem o que acontece se uma é aprovada e a outra rejeitada tendo sido calculadas sobre o mesmo "Perfil anterior" como baseline.
**Consequência potencial:** Aplicação de uma das mudanças pode reverter/sobrescrever efeitos da outra dependendo da ordem de aprovação, sem que isso seja auditável como conflito.

### EC-04 — Corrida entre decisão tardia do aprovador original e o escalonamento automático de SLA
**Localização:** FR-23, FR-24, FR-25 ("SLA de 1 dia útil por nível... escala automaticamente")
**Condição de disparo:** Supervisor decide (aprova/rejeita) exatamente no instante em que o job de escalonamento de SLA dispara e notifica o Gerente.
**Lacuna:** Não há regra de exclusão mútua entre "decisão manual chegando" e "escalonamento automático disparando" no mesmo instante-limite.
**Consequência potencial:** Duas decisões (supervisor e gerente) podem ser registradas para a mesma solicitação, ou o sistema pode aceitar a decisão tardia do supervisor depois de já ter escalado — resultado indefinido de qual decisão prevalece.

### EC-05 — Mudança de Departamento/Supervisor do Funcionário enquanto há solicitação pendente
**Localização:** FR-23, FR-24, FR-25
**Condição de disparo:** Um Funcionário é transferido para outro Departamento (novo supervisor) enquanto uma solicitação de Login/reativação/Perfil dele ainda está `PENDING_APPROVAL`, resolvida contra o supervisor **antigo**.
**Lacuna:** Nenhum FR trata a transferência de Funcionário como gatilho de re-resolução do aprovador de uma solicitação já em curso.
**Consequência potencial:** Aprovador notificado deixa de ser a pessoa hierarquicamente correta (o antigo supervisor pode nem enxergar mais o Funcionário em sua equipe), sem que o sistema perceba a inconsistência.

### EC-06 — FR-26/FR-27 não têm a válvula de escape de "auto-aprovação por exceção" que FR-23 tem
**Localização:** FR-26, FR-27 (comparado a FR-23, mitigação de última instância)
**Condição de disparo:** Grupo `APPROVE_SYSTEM_ACCESS` fica reduzido, na prática, a uma única pessoa disponível (o próprio solicitante) — por exemplo, o outro membro está de licença (FR-28, Login desativado) — violando a mitigação primária de "sempre ≥2 membros".
**Lacuna:** FR-23 prevê explicitamente auto-aprovação auditada como última instância. FR-26 e FR-27 dependem do **mesmo grupo** e do mesmo princípio de segregação de função, mas o texto de FR-26/FR-27 não estende essa válvula de escape — apenas diz "renotifica o grupo inteiro" indefinidamente.
**Consequência potencial:** Um Login `SERVICE`/`EXTERNAL` (FR-26) ou uma solicitação de novo/edição de Perfil (FR-27) pode ficar presa em `PENDING_APPROVAL` para sempre, sem qualquer caminho de saída, se a regra operacional de "≥2 membros" for violada mesmo que temporariamente.

### EC-07 — Ambiguidade sobre se `Employee → DISABLED` sempre também desativa o Login (regra geral vs. regra específica de FR-28)
**Localização:** FR-13, FR-14, FR-28, UJ-2, UJ-14
**Condição de disparo:** Comparar UJ-14 ("funcionário reintegrado... Login segue como estava" — implica que o Login nunca mudou de status durante o `DISABLED` por investigação) com FR-28 ("Login vinculado ao Funcionário é desativado junto, disparando o Kill Switch normalmente" — que fala especificamente de licença/férias).
**Lacuna:** O PRD não deixa explícito se a transição `Employee → DISABLED` **sempre** transiciona também o Login para `INACTIVE`/`BLOCKED` (regra geral, e FR-28 apenas reafirma isso) ou se essa transição de Login é uma ação **exclusiva** do fluxo de licença/férias (FR-28), enquanto um `DISABLED` por outro motivo (ex.: investigação, UJ-14) apenas mata sessões (FR-13) sem tocar no status persistido do Login.
**Consequência potencial:** Implementação pode aplicar a regra de forma inconsistente entre motivos de desligamento — em um caso a reintegração do Funcionário exige nova aprovação de Login (FR-24) e no outro não, sem que isso seja uma decisão de produto deliberada.

### EC-22 — Reabertura de solicitação de Login rejeitada (`REJECTED`) — novo registro ou reaproveitamento?
**Localização:** FR-23, UJ-10 ("RH deve abrir nova solicitação do zero — a rejeição não é editável para virar aprovação depois")
**Condição de disparo:** RH precisa, de fato, liberar acesso para o mesmo Funcionário depois de uma rejeição anterior.
**Lacuna:** Não fica claro se "abrir uma nova solicitação do zero" significa criar um **novo registro de Login** (o que pode colidir com regra de unicidade — um Login por Funcionário, ou username único) ou reabrir/reciclar o mesmo registro `REJECTED` de volta para `PENDING_APPROVAL`.
**Consequência potencial:** Tentativa de recriar Login para o mesmo Funcionário pode falhar por violação de unicidade, ou, se permitida, deixar múltiplos registros de Login histórico (um `REJECTED`, outro `ACTIVE`) sem relação declarada entre eles.

### EC-24 — FR-25 não declara um estado mínimo exigido do Login para aceitar solicitação de mudança de Perfil
**Localização:** FR-25
**Condição de disparo:** Alguém solicita mudança de Perfil (FR-25) para um Login que está `PENDING_APPROVAL` (ainda nem passou por FR-23), `INACTIVE`, `BLOCKED` ou `REJECTED`.
**Lacuna:** FR-25 assume implicitamente "Login continua ACTIVE" durante a espera, mas não declara que o Login **precisa estar** `ACTIVE` no momento de abertura da solicitação para ela fazer sentido/ser aceita.
**Consequência potencial:** Sistema pode aceitar uma solicitação de troca de Perfil para um Login que nem sequer está operante, gerando um estado pendente sem propósito prático ou conflitando com uma futura aprovação de reativação (FR-24) do mesmo Login.

---

## Foco 2 — FR-28 (licença/férias com retorno assistido)

### EC-08 — Funcionário desligado por outro motivo enquanto está com retorno de licença agendado
**Localização:** FR-28
**Condição de disparo:** Funcionário está `DISABLED` por "Férias" com data prevista de retorno em 15 dias; em 5 dias, a empresa decide demiti-lo definitivamente (novo evento de `disable`, motivo diferente, ex.: "Desligamento").
**Lacuna:** FR-28 não trata o caso de um **segundo** evento de disable, com motivo diferente, sobrepondo-se a uma licença já em andamento com retorno agendado. Não fica dito se a data de retorno agendada é cancelada/sobrescrita pelo novo evento.
**Consequência potencial:** Na data originalmente prevista de retorno da licença, o sistema pode abrir automaticamente uma solicitação de reativação de Login para um Funcionário que, na verdade, já foi desligado definitivamente por outro motivo — reabrindo acesso que deveria permanecer fechado.

### EC-09 — Data prevista de retorno retroativa (no passado) no momento do cadastro
**Localização:** FR-28 ("Transição de disable... aceita um campo opcional de data prevista de retorno")
**Condição de disparo:** RH cadastra o `disable` de licença/férias já atrasado (lançamento tardio no sistema) informando uma data prevista de retorno que já passou.
**Lacuna:** Nenhuma validação de range é mencionada para esse campo (ex.: deve ser posterior à data do próprio `disable`, ou posterior à data atual). Não fica claro se o sistema aceita a data retroativa e dispara a reativação automática **imediatamente** (já que a "data" já chegou) ou rejeita o cadastro.
**Consequência potencial:** Cadastro tardio de licença já entra em fluxo de reativação automática assim que salvo, sem o intervalo de licença de fato ter sido respeitado — comportamento surpresa não coberto por nenhuma regra declarada.

### EC-10 — Prorrogação da licença depois que a reativação automática já foi aberta
**Localização:** FR-28 (`[ASSUMPTION: se a licença for prorrogada antes da data prevista, RH atualiza a data...]`)
**Condição de disparo:** A data prevista de retorno chega e o sistema já abriu a solicitação automática de reativação (`PENDING_APPROVAL`); só depois disso, RH percebe que a licença foi prorrogada.
**Lacuna:** A suposição documentada cobre apenas a atualização **antes** da data prevista (antes de a solicitação abrir). Não há instrução sobre como cancelar/retirar (`withdraw`) uma solicitação de reativação automática já aberta e pendente quando a prorrogação é percebida depois.
**Consequência potencial:** A solicitação de reativação fica pendente e pode ser aprovada por engano por um supervisor que não sabe da prorrogação, reativando o Login de alguém que continua de licença.

### EC-11 — Corrida entre retorno antecipado manual e o job agendado da data prevista de retorno
**Localização:** FR-28
**Condição de disparo:** RH reativa manualmente o Funcionário/Login antes da data prevista de retorno (retorno antecipado real), mas o campo de "data prevista de retorno" não é limpo/cancelado.
**Lacuna:** Não há regra dizendo que uma reativação manual antecipada deve cancelar o gatilho automático agendado para a data prevista original.
**Consequência potencial:** Na data que originalmente seria de retorno, o sistema tenta abrir uma nova solicitação de reativação para um Login que já está `ACTIVE` há dias — comportamento indefinido (idempotência do gatilho não especificada).

### EC-12 — Cargo do Funcionário é desativado enquanto ele está de licença (FR-3 x FR-5 x FR-28)
**Localização:** FR-3 ("Desativar... Cargo com Funcionário **ativo** vinculado é rejeitado"), FR-28
**Condição de disparo:** Funcionário está `DISABLED` por licença/férias (FR-28); nesse meio-tempo, RH desativa o Cargo dele, pois, tecnicamente, ele não está mais `ACTIVE` — logo a guarda de FR-3 ("Funcionário ativo vinculado") não impede a desativação do Cargo.
**Lacuna:** FR-3 e FR-28 não se referenciam mutuamente; a guarda de FR-3 usa literalmente o estado `ACTIVE`, deixando destravada a desativação do Cargo de alguém que está apenas temporariamente afastado.
**Consequência potencial:** Na data prevista de retorno, o sistema abre automaticamente a solicitação de reativação de Login (FR-28) para um Funcionário cujo Cargo já não existe mais como ativo — aprovação segue em frente sem checar essa inconsistência organizacional.

---

## Foco 3 — FR-1 (hierarquia) e FR-2 (ciclo de vida de Empresa)

### EC-13 — A guarda de "filial ativa" cobre só a transição de bloqueio/disable, não a de inativação
**Localização:** FR-1 (SCOS_COMPANY_005/006), FR-2 ("disable é rejeitado se existir filial direta ativa")
**Condição de disparo:** Existem múltiplas matrizes ativas no sistema (logo, SCOS_COMPANY_005 — "última matriz ativa" — não se aplica); uma dessas matrizes tem uma filial direta ativa; alguém **inativa** essa matriz (não bloqueia/desabilita).
**Lacuna:** A única regra que impede a operação de uma matriz com filial ativa embaixo é a de FR-2, e ela é explicitamente escopada à transição de **disable/bloqueio** ("disable é rejeitado se existir filial direta ativa") — nada no FR-1 nem no FR-2 impede a transição de **inativação** da mesma matriz nas mesmas condições.
**Consequência potencial:** Uma matriz com filial ativa pode ser livremente inativada (transição mais "leve" que bloquear), deixando uma filial `ACTIVE` operando sob uma matriz `INACTIVE` — o mesmo problema estrutural que a regra de FR-2 tenta evitar, alcançado por uma porta que ela não cobre.

### EC-14 — As duas guardas de "último ativo" (FR-1) têm escopos de transição diferentes e não se sobrepõem com a guarda de filial ativa (FR-2)
**Localização:** FR-1 (SCOS_COMPANY_005 = guarda de **inativar**; SCOS_COMPANY_006 = guarda de **bloquear**), FR-2 (guarda de **disable**/bloqueio por filial ativa)
**Condição de disparo:** Uma Empresa é, ao mesmo tempo, a única empresa ativa do sistema (dispara SCOS_COMPANY_006 ao tentar bloquear) mas **não** é uma matriz (logo SCOS_COMPANY_005 não se aplica a ela por definição) — situação side-case do modelo de hierarquia.
**Lacuna:** O PRD não deixa explícito o que acontece quando as três guardas (005 último-matriz / 006 única-ativa / FR-2 filial-ativa) endereçam a **mesma** empresa sob transições diferentes sem nenhuma delas realmente se sobrepor — cada uma cobre uma combinação (transição × condição) e não há uma tabela de decisão consolidada confirmando que todas as combinações relevantes (inativar/bloquear × matriz/filial × tem-filial-ativa/é-única-ativa) estão cobertas.
**Consequência potencial:** Ao implementar as três regras separadamente (como sugerido pelo texto, com três códigos de erro distintos), é fácil deixar uma combinação sem guarda nenhuma — como de fato ocorre em EC-13 — sem que isso seja percebido, pois cada regra individualmente "parece" cobrir seu próprio caso.

### EC-15 — A guarda de "filial ativa" olha apenas o nível direto, não a hierarquia inteira
**Localização:** FR-2 ("disable é rejeitado se existir filial **direta** ativa"), FR-1 (`COMPANY_HIERARCHY_MAX_DEPTH` configurável, hierarquia com múltiplos níveis)
**Condição de disparo:** Hierarquia de 3+ níveis: Matriz A → Filial B (`INACTIVE`) → Filial C (`ACTIVE`, filha de B). Alguém tenta desabilitar/bloquear a Matriz A.
**Lacuna:** A guarda de FR-2 verifica explicitamente apenas filiais **diretas** de A. Como B (filha direta de A) está `INACTIVE`, a checagem passa, mesmo com C (neta de A, filha de B) `ACTIVE`.
**Consequência potencial:** Matriz do topo é desabilitada com uma filial de segundo nível ainda operando ativamente por baixo, em hierarquias com profundidade > 2 (explicitamente permitidas pelo `COMPANY_HIERARCHY_MAX_DEPTH` configurável).

### EC-16 — Reativação (enable/unblock) de filial não valida se a matriz-pai está atualmente ativa
**Localização:** FR-1 ("RH pode cadastrar uma Empresa como... filial de outra Empresa **ativa**")
**Condição de disparo:** Uma Filial B foi criada sob Matriz A ativa; depois, Matriz A é inativada/desabilitada; alguém tenta reativar (`enable`/`unblock`) a Filial B, que estava `INACTIVE`/`DISABLED` por outro motivo.
**Lacuna:** A exigência de "empresa pai ativa" é declarada apenas para a operação de **cadastro** (criação) da hierarquia — nenhum FR menciona essa mesma checagem sendo reaplicada no momento de reativar uma filial já existente.
**Consequência potencial:** Uma filial pode voltar a `ACTIVE` enquanto sua matriz permanece `INACTIVE`/`DISABLED`, criando a mesma situação estrutural (filha ativa sob pai não-ativo) que a regra de criação tenta evitar, alcançada pela porta de reativação.

---

## Foco 4 — FR-9/FR-10 (bloqueio de turno) x FR-12 (plantão pré-aprovado)

### EC-17 — Sobreposição entre janela de plantão e Jornada regular (incluindo o intervalo de almoço) sem regra de precedência
**Localização:** FR-9 ("fora do intervalo... excluindo o intervalo de almoço"), FR-12 ("liberado mesmo fora da Jornada de Trabalho regular")
**Condição de disparo:** Jornada regular é 08h–18h com almoço 12h–13h; uma janela de plantão é cadastrada das 10h às 14h (sobrepondo parte da Jornada regular e todo o intervalo de almoço). Requisição chega às 12h30 (dentro do almoço da Jornada regular, mas também dentro da janela de plantão).
**Lacuna:** Não fica definido se o plantão é pensado apenas para cobrir horários **totalmente fora** da Jornada regular, ou se pode legitimamente "tapar" o buraco do intervalo de almoço quando as janelas se sobrepõem. Também não há regra de qual rótulo de auditoria prevalece (FR-11/FR-12 exigem identificar explicitamente a liberação como "via plantão" e não "via jornada regular") quando **ambas** as condições são simultaneamente verdadeiras.
**Consequência potencial:** Comportamento e atribuição de auditoria ficam a critério da ordem de implementação dos dois checks, podendo registrar a liberação com o motivo errado (jornada regular em vez de plantão, ou vice-versa) quando as janelas coincidem.

### EC-18 — Plantão cadastrado em dia sem Jornada regular (fail-closed do FR-10) — independência não confirmada
**Localização:** FR-10 ("ausência de registro de Jornada... = bloqueio"), FR-12 ("no mesmo padrão de cadastro por período já usado em Work Schedule")
**Condição de disparo:** Nenhuma Jornada de Trabalho está cadastrada para o Funcionário no dia da semana corrente (aplicando o fail-closed do FR-10); no entanto, há uma janela de plantão cadastrada e válida para esse mesmo dia/horário.
**Lacuna:** O PRD não afirma explicitamente que a avaliação do plantão (FR-12) é totalmente independente da existência de um registro de Jornada regular para aquele dia. A frase "no mesmo padrão de cadastro... já usado em Work Schedule" reforça a semelhança estrutural entre os dois mecanismos, o que pode levar a uma implementação que acopla a checagem de plantão à checagem de Jornada regular (ex.: só avalia plantão se já existir uma Jornada base para o dia).
**Consequência potencial:** Um plantão legitimamente cadastrado pode ser negado por causa do fail-closed do FR-10, se o motor de decisão avaliar "Jornada ausente → bloqueado" antes de sequer considerar a janela de plantão independente.

### EC-19 — Janela de plantão que cruza a virada do dia (ex.: 22h–06h)
**Localização:** FR-12 ("janela de plantão (início/fim)... no mesmo padrão de Work Schedule")
**Condição de disparo:** Plantão noturno cadastrado das 22h de um dia às 06h do dia seguinte, potencialmente atravessando também a virada de dia-da-semana usada como chave de busca da Jornada regular (FR-9 usa "dia da semana corrente").
**Lacuna:** Não fica claro se o modelo de plantão suporta início/fim atravessando a meia-noite/dia-da-semana, ou se — seguindo estritamente o "mesmo padrão" de Work Schedule (que é por dia da semana) — uma janela assim precisaria ser cadastrada como dois registros separados (23:59 do dia 1 + 00:00 do dia 2), sem essa orientação estar no texto.
**Consequência potencial:** Plantão noturno real cadastrado como um único intervalo pode ser parcialmente ou totalmente rejeitado/ignorado pela checagem, dependendo de como o motor resolve "dia da semana corrente" no instante exato da virada.

---

## Achados adicionais (varredura completa FR-1 a FR-22, UJ-1 a UJ-17)

### EC-20 — SLA de "1 dia útil" (FR-23 a FR-28) depende do calendário de dias úteis (FR-17), que é P1 e não tem ordem de construção definida antes das aprovações P0/Fundação
**Localização:** FR-23–FR-28 ("SLA de 1 dia útil"), FR-17 (Motor Geotemporal, P1), §9 Riscos e Mitigações
**Condição de disparo:** Calcular quando 1 "dia útil" se esgota para fins de escalonamento de aprovação exige saber quais dias são feriado (nacional/estadual/municipal) — dado que só o Motor Geotemporal (§4.4, prioridade P1) fornece.
**Lacuna:** §9 já lista explicitamente a dependência de FR-4→§4.2 como risco de ordem de construção, mas não lista esta segunda dependência oculta: FR-23–28 (dentro da Fundação, §4.1) dependem de FR-17 (§4.4, P1) para calcular corretamente "dia útil". Também não fica definido de qual calendário municipal a contagem deve partir quando solicitante, aprovador e Funcionário estão em filiais de municípios diferentes.
**Consequência potencial:** Sistema de aprovação pode ser construído antes do calendário municipal existir, forçando um cálculo provisório ingênuo (ex.: só exclui sábado/domingo) que sub ou superestima prazos de SLA e desalinha a contra-métrica SM-C3.

### EC-21 — Retenção de auditoria (FR-11) é ancorada em "desligamento do Funcionário", mas Logins EXTERNAL/SERVICE (FR-26) não têm Funcionário
**Localização:** FR-11 ("retido por, no mínimo, 5 anos após o desligamento do Funcionário vinculado ao Login"), FR-26 (Login sem `employeeId`)
**Condição de disparo:** Um Login `SERVICE`/`EXTERNAL` (sem Funcionário vinculado, FR-26) acumula anos de registros de auditoria de decisões de turno/aprovação.
**Lacuna:** A regra de retenção de FR-11 usa exclusivamente "desligamento do Funcionário vinculado ao Login" como o evento-âncora da contagem dos 5 anos. Não existe evento equivalente definido para Logins sem Funcionário algum.
**Consequência potencial:** Registros de auditoria de Logins de sistema/integração não têm política de retenção definida — podem ser retidos indefinidamente (custo/LGPD) ou, pior, purgados por uma implementação que assume erroneamente "sem Funcionário = sem regra = pode expirar por padrão genérico".

### EC-23 — Validação cronológica de Jornada (FR-4) não esclarece se o intervalo de almoço é obrigatório
**Localização:** FR-4 ("Validação cronológica estrita: startTime < lunchStart < lunchEnd < endTime")
**Condição de disparo:** Cargo/Funcionário com turno curto (ex.: 4 horas) que legitimamente não tem intervalo de almoço.
**Lacuna:** A regra de validação é apresentada como estrita e obrigatória (`<` em cadeia), sem menção a `lunchStart`/`lunchEnd` opcionais/nulos para turnos sem almoço.
**Consequência potencial:** Turnos parciais sem intervalo de almoço ficam impossíveis de cadastrar corretamente — força um valor artificial de almoço só para satisfazer a validação, contaminando o dado usado depois pelo próprio FR-9 (exclusão do intervalo de almoço no cálculo de bloqueio de turno).

---

## Resumo por foco

| Foco | Itens | IDs |
|---|---|---|
| 1. Interações FR-23–FR-28 | 9 | EC-01, EC-02, EC-03, EC-04, EC-05, EC-06, EC-07, EC-22, EC-24 |
| 2. FR-28 licença/férias | 5 | EC-08, EC-09, EC-10, EC-11, EC-12 |
| 3. FR-1/FR-2 hierarquia de Empresa | 4 | EC-13, EC-14, EC-15, EC-16 |
| 4. FR-9/FR-10 x FR-12 (turno x plantão) | 3 | EC-17, EC-18, EC-19 |
| Achados adicionais (varredura completa) | 3 | EC-20, EC-21, EC-23 |
| **Total** | **24** | |
