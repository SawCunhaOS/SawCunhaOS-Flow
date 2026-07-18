---
title: 'PRD: SawCunhaOS (Motor Fundacional) - Fase 1'
status: final
created: '2026-07-18'
updated: '2026-07-18'
---

# PRD: SawCunhaOS (Motor Fundacional) — Fase 1
*Working title — confirma com o Product Brief (`etc/doc/Briefing.md`, status "Alinhado").*

## 0. Propósito do Documento

Este PRD é o norte de produto para a Fase 1 do SawCunhaOS: o motor de governança, ciclo de vida de RH e mensageria para provedores de internet (ISPs) de pequeno/médio porte. É dirigido a Engenharia, QA e ao próprio PM, e serve de insumo direto para Arquitetura e para a quebra em Épicos/Stories.

Este documento **não duplica** a especificação funcional detalhada já existente em `etc/doc/usecase/00-indice-central.md` a `07-mensagens-erro-pt-en.md` (162 endpoints, validados contra o código real de Company, Department, Position, Employee, Login, Profile, Resource, System, Catalog, Reason, Configuration e Outbox). Aquele conjunto de documentos é a fonte de verdade funcional para a **Fundação de Identidade e Organização** (§4.1) e é referenciado, não repetido, aqui.

O valor deste PRD está em cobrir o que **nenhum documento existente especifica ainda**: os três motores diferenciadores do Product Brief — Governança Ativa via API (bloqueio de turno), Kill Switch e Motor Geotemporal — mais o Motor de Notificação Híbrida, todos citados no Brief apenas como visão de 1-2 parágrafos, sem FRs testáveis. Uma auditoria de código feita durante este Discovery (cruzando os 4 pilares do Brief contra o código real) confirmou que 3 dos 4 pilares de Fase 1 estão em 0%–parcial de implementação hoje; este PRD assume o papel de roteiro de construção, não de descrição de sistema pronto.

Escopo: **somente back-end/API** — este repositório não contém front-end; qualquer interface (RH, técnico) é um consumidor externo desta API.

## 1. Visão

SawCunhaOS Fase 1 é o núcleo de governança e comunicação para ISPs brasileiros de pequeno/médio porte. Em um setor onde conformidade trabalhista, segurança lógica e agilidade logística decidem a sobrevivência do negócio, o sistema amarra o controle de acesso técnico à jornada de trabalho ativa de cada colaborador — de forma inescapável, na camada de API, não no front-end.

O sistema neutraliza três passivos concretos do dia a dia de um ISP: colaboradores operacionais acessando dados de assinante fora do expediente (passivo trabalhista + risco de segurança), contas órfãs sobrevivendo ao desligamento de um funcionário (risco de vazamento), e agendamentos técnicos que ignoram feriados municipais (erro de SLA). Ele faz isso com postura Zero Trust: a regra vive no back-end, é imutável ao usuário final, e cada decisão de acesso gera rastro de auditoria.

Esta é, deliberadamente, uma entrega de blindagem interna — não de captação de cliente. Fase 2 (CRM, faturamento inteligente, provisionamento de rede) se apoia diretamente na fundação de identidade, permissão e mensageria construída aqui.

**Modelo de implantação: single-tenant por ISP.** Cada instalação do SawCunhaOS atende a um único ISP — não é uma plataforma SaaS multi-tenant compartilhada entre provedores diferentes. Isso é arquiteturalmente carregado: regras como "última matriz ativa do sistema" e "única empresa ativa do sistema" (FR-1) só fazem sentido nesse modelo, e assumem esse escopo a partir daqui.

## 2. Usuário-Alvo

### 2.1 Jobs To Be Done

- **Gestor de ISP / Analista de RH** — preciso de controle total sobre a jornada da equipe, eliminação de risco de horas extras indevidas, segurança jurídica no desligamento de colaboradores, e garantia de que dados de rede/assinante estão protegidos fora do expediente.
- **Técnico / Operador de Suporte** — preciso de uma interface de trabalho simples, alertas precisos de escala e rota de turno, e a garantia (para mim também) de que o sistema só me deixa operar dentro do meu horário — isso me protege de acusação indevida de acesso fora de hora.
- **Gestor/Admin/Suporte emergencial** *(perfil de acesso irrestrito)* — preciso continuar operando fora do horário regular quando uma emergência real exige, sem ser bloqueado pela mesma regra que protege os perfis operacionais — mas com meu acesso extraordinário sempre registrado.
- **Fase 2 (CRM / Faturamento)** *(consumidor futuro, não usuário direto)* — precisa de uma fundação de identidade, permissão e mensageria estável para se acoplar com segurança, sem reconstruir IAM.

### 2.2 Não-Usuários (v1)

- Cliente final / assinante do ISP — sem Portal do Assinante nesta fase.
- Time comercial/CRM — sem telas de prospecção ou carteira de clientes.
- Financeiro/cobrança — sem emissão de boleto, remessa ou gateway de pagamento.
- NOC/campo de provisionamento de rede — sem integração com Radius, OLT ou roteadores.

### 2.3 Principais Jornadas de Usuário

- **UJ-1. Carla tenta acessar o sistema fora do turno e é barrada.**
  - **Persona + contexto:** Carla, atendente de suporte técnico, perfil `requiresShiftEnforcement=true`. Curiosa sobre um chamado, tenta consultar dados de um assinante às 22h, duas horas após o fim do seu turno.
  - **Entrada:** autenticada (token JWT válido, obtido mais cedo no expediente), chamando `GET /v1/subscribers/{id}` (fora do escopo deste módulo, mas protegido pela mesma regra transversal).
  - **Caminho:** app envia requisição → filtro de governança intercepta antes da lógica de negócio → consulta a Jornada de Trabalho vigente de Carla para o dia da semana atual → horário atual (22h) está fora do intervalo `[startTime, endTime]`.
  - **Clímax:** API retorna `403 Forbidden` (ProblemDetail SCOS padrão); nenhum dado de negócio é processado ou retornado.
  - **Resolução:** Carla vê mensagem de acesso negado por fora de turno; o evento é gravado no log de auditoria imutável (permitido/negado, quem, quando, endpoint).
  - **Edge case:** se Carla tiver uma janela de plantão pré-aprovada cadastrada para aquele horário (FR-12), a chamada é liberada e o log ainda assim é gravado, identificando a liberação como plantão.

- **UJ-2. Marina (RH) desliga um colaborador e todas as sessões dele morrem na hora.**
  - **Persona + contexto:** Marina, analista de RH, acabou de confirmar a demissão de um técnico de campo.
  - **Entrada:** autenticada no console de RH, perfil com `DISABLE_EMPLOYEE`/`BLOCK_EMPLOYEE`.
  - **Caminho:** Marina abre o cadastro do funcionário → seleciona "Desligar" → escolhe motivo do catálogo (`SCOS_REASON_DISABLE`) → confirma.
  - **Clímax:** o sistema muda o status do Employee para `DISABLED`, grava o motivo no histórico imutável, e dispara — de forma síncrona, na mesma transação lógica — a invalidação de todas as sessões/tokens ativos vinculados aos Logins desse funcionário.
  - **Resolução:** em menos de 1 segundo, qualquer requisição feita com o token antigo do ex-funcionário retorna `401`. Marina recebe confirmação de que o desligamento foi propagado.
  - **Edge case:** o ex-funcionário está com uma sessão ativa no app móvel de campo, fazendo uma chamada em andamento — essa chamada em voo é interrompida na próxima checagem de token (ver FR-13, NFR de latência).

- **UJ-3. Marina cadastra uma nova filial e o motor geotemporal resolve o endereço e o calendário regional.**
  - **Persona + contexto:** Marina está abrindo uma nova filial do ISP em outro município.
  - **Entrada:** autenticada, criando uma nova Empresa (Company) do tipo filial.
  - **Caminho:** Marina digita o CEP → sistema consulta o motor geotemporal → endereço é preenchido automaticamente (logradouro, bairro, cidade, UF) → Marina confirma e salva a filial.
  - **Clímax:** a partir do município resolvido, o sistema já sabe quais feriados municipais/estaduais/nacionais afetam agendamentos técnicos dessa filial.
  - **Resolução:** filial criada com endereço validado; agendamentos futuros nessa filial já respeitam o calendário de dias úteis local.
  - **Edge case:** ViaCEP está fora do ar — sistema usa o provedor de fallback (FR-15); se ambos falharem, Marina cadastra o endereço manualmente e o sistema avisa que a filial ficará sem resolução automática de calendário até nova tentativa.

- **UJ-4. O sistema dispara um alerta crítico de segurança na hora, e um lote de avisos operacionais em segundo plano.**
  - **Persona + contexto:** nenhum humano inicia isso diretamente — é o próprio SawCunhaOS reagindo a dois eventos de naturezas diferentes no mesmo dia.
  - **Caminho A (síncrono):** uma tentativa de acesso fora de turno bloqueada dispara, na mesma chamada, um alerta de segurança ao gestor responsável — a chamada de notificação é bloqueante e o sistema só seguirá adiante após confirmar o envio (ou falha) do alerta.
  - **Caminho B (assíncrono):** ao final do dia, um lote de avisos de escala do dia seguinte é publicado no Outbox e processado em segundo plano, sem bloquear nenhuma requisição de usuário.
  - **Clímax:** o alerta crítico chega ao gestor em tempo real; os avisos de rotina chegam aos técnicos sem impacto de performance em nenhuma chamada de API.
  - **Resolução:** nenhuma mensagem é perdida em nenhum dos dois modos — falha de envio (síncrono ou assíncrono) fica registrada e é reprocessável.

- **UJ-5. Roberto (suporte emergencial, acesso irrestrito) atua fora do horário sem ser bloqueado — mas deixa rastro.**
  - **Persona + contexto:** Roberto tem perfil marcado como acesso irrestrito por justificativa legal (função de suporte emergencial 24/7).
  - **Caminho:** Roberto acessa o sistema às 3h da manhã para resolver uma queda de rede.
  - **Clímax:** a checagem de turno (FR-9) identifica que o perfil de Roberto está isento (FR-8) e libera a chamada normalmente.
  - **Resolução:** o acesso, mesmo liberado, gera o mesmo registro de auditoria imutável que qualquer outro (FR-11) — a exceção de turno nunca é uma exceção de rastreabilidade.

- **UJ-6. Marina (RH) solicita um Login para um funcionário recém-admitido, e o supervisor aprova antes que ele possa logar.**
  - **Persona + contexto:** Marina acabou de admitir um novo técnico de campo e precisa liberar o acesso dele ao sistema.
  - **Entrada:** Marina autenticada no console de RH, cria o Login para o novo Funcionário.
  - **Caminho:** Login é criado em `PENDING_APPROVAL` → sistema identifica o supervisor direto do Funcionário → dispara notificação síncrona ao supervisor → supervisor recebe o alerta e aprova pela sua sessão.
  - **Clímax:** ao aprovar, o Login transita para `ACTIVE` e a Saga Keycloak sincroniza normalmente — só a partir daí o novo técnico consegue autenticar.
  - **Resolução:** decisão do supervisor (aprovação) fica registrada em auditoria imutável; o técnico recebe acesso liberado.
  - **Edge case:** se o Funcionário não tiver supervisor cadastrado, a notificação e a aprovação recaem sobre o gerente do Departamento.

- **UJ-7. Marina monta uma filial nova do zero — empresa, departamento, cargo e primeiro funcionário.**
  - **Persona + contexto:** Marina está estruturando a operação de uma filial recém-aberta.
  - **Caminho:** cadastra a Empresa como filial de uma matriz ativa (FR-1) → cria o Departamento de Operações (FR-3) → cria o Cargo de Técnico de Campo vinculado a esse Departamento, já com o template de Jornada de Trabalho (FR-4) → admite o primeiro Funcionário nesse Cargo (FR-5), que copia a Jornada do Cargo → cria o Login do Funcionário (FR-23), pendente de aprovação do supervisor.
  - **Clímax:** cada passo depende do anterior estar ativo (Depto ativo pro Cargo, Cargo ativo pro Funcionário) — a cadeia inteira só fecha quando todos os elos estão certos.
  - **Resolução:** filial operacional, com hierarquia de Empresa, organização interna e primeiro colaborador com Login em aprovação.

- **UJ-8. Um técnico volta de licença e seu Login precisa ser reativado.**
  - **Persona + contexto:** Carla ficou 60 dias afastada (licença médica); seu Login está `INACTIVE`.
  - **Caminho:** RH aciona `enable` no Login de Carla (FR-24) → mesmo mecanismo de FR-23 é acionado: `PENDING_APPROVAL`, notificação ao supervisor de Carla.
  - **Clímax:** supervisor aprova a reativação → Login volta a `ACTIVE`.
  - **Resolução:** Carla autentica normalmente de novo, sujeita à mesma checagem de turno de sempre (FR-9).

- **UJ-9. Carla é promovida e precisa de um Perfil com mais acesso.**
  - **Persona + contexto:** Carla passa a atender chamados de rede, além de suporte — precisa do Perfil adicional "Operador de Rede".
  - **Caminho:** RH ou o próprio supervisor solicita a adição do Perfil adicional ao Login de Carla (FR-25).
  - **Clímax:** enquanto a solicitação aguarda aprovação, **Carla continua trabalhando normalmente com o Perfil que já tinha** — nada é bloqueado.
  - **Resolução:** supervisor aprova → Perfil adicional passa a valer (sujeito à defasagem de até 30 min das views de autoridade); Carla ganha o novo acesso sem nunca ter ficado sem o antigo.

- **UJ-10. Um supervisor rejeita uma solicitação de Login.**
  - **Persona + contexto:** RH solicita Login para um prestador temporário; o supervisor considera que o cargo não deveria ter acesso ao sistema ainda.
  - **Caminho:** Login em `PENDING_APPROVAL` → supervisor abre a solicitação e escolhe rejeitar, em vez de aprovar (FR-23).
  - **Clímax:** Login transita para `REJECTED` — estado terminal, nunca vira `ACTIVE`.
  - **Resolução:** RH é notificado da rejeição; se o acesso ainda for necessário, precisa abrir uma nova solicitação do zero — a rejeição não é editável para virar aprovação depois.

- **UJ-11. Ninguém responde a tempo, e a solicitação escala sozinha.**
  - **Persona + contexto:** o supervisor de um novo Funcionário está de férias e não vê a notificação de aprovação de Login.
  - **Caminho:** passa 1 dia útil sem decisão (FR-23) → sistema escala automaticamente para o gerente do Departamento, mesmo o Funcionário tendo supervisor definido.
  - **Clímax:** gerente recebe a notificação e decide no lugar do supervisor original.
  - **Resolução:** novo Funcionário não fica bloqueado indefinidamente por causa da ausência de uma pessoa; o atraso fica registrado na contra-métrica SM-C3.

- **UJ-12. Um sistema de monitoramento externo precisa de um Login próprio, sem funcionário por trás.**
  - **Persona + contexto:** a equipe de TI está integrando uma ferramenta de monitoramento de rede que precisa consumir a API continuamente.
  - **Caminho:** TI cria um Login `SERVICE` sem `employeeId` (FR-26) → entra em `PENDING_APPROVAL` → notificação vai para todos os Logins que detêm `APPROVE_SYSTEM_ACCESS` (não para um supervisor, pois não há Funcionário).
  - **Clímax:** qualquer um do grupo aprova → Login `SERVICE` fica `ACTIVE`.
  - **Resolução:** integração começa a funcionar; se ninguém do grupo agir em 1 dia útil, o sistema renotifica o grupo inteiro (sem hierarquia pra escalar).

- **UJ-13. TI cria um novo Perfil de acesso restrito para um auditor externo.**
  - **Persona + contexto:** a empresa contratou uma auditoria externa temporária, que precisa de um Perfil novo com Recursos bem limitados (só leitura de relatórios).
  - **Caminho:** um membro de TI cria o Perfil "Auditor Externo" com os Recursos escolhidos (FR-27) → Perfil fica pendente, não pode ser atribuído a nenhum Login ainda.
  - **Clímax:** outro Login com `APPROVE_SYSTEM_ACCESS` aprova o novo Perfil.
  - **Resolução:** Perfil "Auditor Externo" pode agora ser atribuído a um Login; qualquer edição futura nos Recursos desse Perfil (ex.: adicionar mais um relatório) passa pela mesma aprovação.

- **UJ-14. Um funcionário suspenso é reintegrado, mudando de cargo.**
  - **Persona + contexto:** um funcionário `DISABLED` (bloqueado por uma investigação interna) é liberado e a empresa decide reintegrá-lo em um cargo diferente, em outra filial.
  - **Caminho:** RH usa `rehire` (FR-5, agora aceitando origem `DISABLED`) informando novo Cargo e nova Empresa/filial — não usa o `unblock` simples, que não permite reatribuição.
  - **Clímax:** funcionário reintegrado já no cargo/filial certos, com nova entrada no histórico de posição.
  - **Resolução:** trigger fecha automaticamente a posição anterior aberta; funcionário segue o fluxo normal a partir daí (Login segue como estava, sujeito a nova aprovação só se também precisar de mudança de Perfil).

- **UJ-15. O mecanismo do Kill Switch está fora do ar bem na hora de um desligamento.**
  - **Persona + contexto:** Marina desliga um funcionário exatamente no momento em que o componente de invalidação de sessão está indisponível.
  - **Caminho:** transição de status no banco (`DISABLED`) acontece normalmente, sem depender do mecanismo indisponível (FR-13) → sistema aciona retry e alerta obrigatório.
  - **Clímax:** enquanto a invalidação não propaga, qualquer checagem de token que não conseguir confirmar o estado falha **fechado** — nega o acesso em vez de liberar por incerteza.
  - **Resolução:** ex-funcionário não consegue mais usar o sistema mesmo com o componente de invalidação fora do ar; equipe de engenharia é alertada para agir sobre a indisponibilidade.

- **UJ-16. Uma tentativa de criar ciclo na hierarquia de empresas é barrada.**
  - **Persona + contexto:** por engano, alguém tenta relacionar uma matriz como filial da sua própria filial.
  - **Caminho:** requisição de cadastro/edição de hierarquia (FR-1) é avaliada → sistema detecta que a nova relação criaria um ciclo.
  - **Clímax:** requisição rejeitada com `SCOS_COMPANY_004`, nenhuma alteração aplicada.
  - **Resolução:** hierarquia de empresas permanece uma árvore válida, sem loops.

- **UJ-17. Carla sai de férias, e o sistema mesmo lembra de pedir a reativação quando ela volta.**
  - **Persona + contexto:** Carla vai tirar 20 dias de férias a partir de segunda-feira.
  - **Caminho:** RH desativa o Funcionário/Login de Carla com Motivo "Férias" e data prevista de retorno (FR-28) → Login de Carla é desativado, Kill Switch dispara normalmente → no dia previsto de retorno, o sistema abre sozinho a solicitação de reativação do Login.
  - **Clímax:** supervisor de Carla recebe a notificação e aprova, exatamente como qualquer reativação (FR-24) — a diferença é que ninguém em RH precisou lembrar de abrir o pedido.
  - **Resolução:** Login de Carla volta a `ACTIVE` assim que aprovado; se as férias forem prorrogadas, RH atualiza a data antes que a reativação automática dispare.

## 3. Glossário

- **Empresa (Company)** — Pessoa jurídica do ISP; pode ser matriz (`parentCompanyId=null`) ou filial. Estados: `ACTIVE ⇄ INACTIVE ⇄ DISABLED`.
- **Filial (Branch)** — Empresa cujo `parentCompanyId` aponta para outra Empresa (a matriz).
- **Departamento** — Unidade organizacional; agrupa Cargos.
- **Cargo (Position)** — Função dentro de um Departamento; carrega o template de Jornada de Trabalho.
- **Funcionário (Employee)** — Colaborador vinculado a Empresa e Cargo. Estados: `ACTIVE ⇄ INACTIVE ⇄ DISABLED`.
- **Login** — Credencial de acesso vinculada (ou não) a um Funcionário; sincronizada com o Keycloak via Saga/Outbox. Estados: `PENDING_APPROVAL → ACTIVE ⇄ INACTIVE ⇄ BLOCKED`, com `REJECTED` como estado terminal alternativo a partir de `PENDING_APPROVAL`.
- **Perfil (Profile)** — Conjunto de Recursos/permissões atribuível a um Login (principal ou adicional).
- **Recurso (Resource)** — Menor unidade de permissão (`x-authorize`), somente leitura nesta API (fonte: scos-registry via gRPC).
- **Jornada de Trabalho (Work Schedule)** — Horário (`startTime`, `lunchStart`, `lunchEnd`, `endTime`) por dia da semana; existe como template no Cargo e como registro efetivo no Funcionário (cópia feita na admissão, sem vínculo futuro).
- **Acesso Irrestrito** — Perfil isento da checagem de turno, por justificativa legal (gerência, administração, suporte emergencial).
- **Plantão / Acesso Extraordinário** — Janela de acesso fora da Jornada de Trabalho, pré-aprovada e cadastrada antecipadamente (FR-12), sempre registrada em auditoria.
- **Supervisor** — Funcionário registrado como responsável direto por outro Funcionário; nível 1 da cadeia de aprovação (FR-23/FR-24/FR-25).
- **Gerente do Departamento** — Nível 2 da cadeia de aprovação, usado quando o Supervisor está ausente ou é a própria pessoa que abriu a solicitação (FR-23/FR-24/FR-25).
- **PENDING_APPROVAL** — Estado do Login entre a criação/solicitação de reativação e a decisão do aprovador; não autentica nem é aceito em nenhuma chamada de API.
- **Login EXTERNAL/SERVICE** — Login sem Funcionário vinculado (`employeeId` nulo); aprovado diretamente por `APPROVE_SYSTEM_ACCESS` (FR-26), sem passar por Supervisor/Gerente.
- **APPROVE_SYSTEM_ACCESS** — Permissão dedicada, atribuída explicitamente a Logins específicos (não a Departamento/Cargo/hierarquia). Dupla função: (1) aprovador direto de Login sem Funcionário (FR-26) e de criação/edição de Perfil (FR-27); (2) nível 3 — fallback de última instância — da cadeia de aprovação de FR-23/24/25, quando Supervisor e Gerente estão ausentes ou em conflito de interesse com o solicitante.
- **Segregação de função (four-eyes)** — Princípio transversal a toda aprovação do sistema: quem abre uma solicitação nunca pode ser quem a aprova, em nenhum nível da cadeia (FR-23 a FR-27).
- **Kill Switch** — Mecanismo de invalidação síncrona de todas as sessões/tokens de um Login ao seu desligamento/bloqueio.
- **Motor Geotemporal** — Subsistema de resolução de endereço por CEP + calendário de dias úteis/feriados (nacional/estadual/municipal) + tags operacionais.
- **Tag Operacional** — Rótulo customizado em uma data do calendário (ex.: "Janela de Manutenção"); em Fase 1, apenas informativa/filtro de relatório, sem automação.
- **Motor de Notificação Híbrida** — Despachante de mensagens com modo síncrono (bloqueante) e assíncrono (fila via Outbox), multi-canal (Email/SMS/App/Web).
- **Outbox (Events/Topics)** — Infraestrutura genérica de eventos assíncronos já existente; hoje seu único consumidor real é a Saga de sincronização de Login com o Keycloak.
- **Motivo (Reason)** — Catálogo obrigatório em transições de status de Empresa/Funcionário/Login (Activate/Inactivate/Disable/Enable) e em troca de Cargo. Inclui "Férias" e "Licença Médica" como Motivos de `disable` (FR-28).
- **Data prevista de retorno** — Campo opcional em uma transição de `disable` com Motivo "Férias"/"Licença Médica"; ao chegar essa data, o sistema abre sozinho a solicitação de reativação do Login (FR-28), ainda sujeita a aprovação normal (FR-24).
- **Zero Trust** — Postura de segurança em que nenhuma regra de acesso depende de confiança no cliente/front-end; toda decisão é tomada e imposta no back-end.

## 4. Features

### 4.1 Fundação de Identidade e Organização — **Etapa 1 (P0)**
**Descrição:** cadastro e ciclo de vida de Empresa, Departamento, Cargo, Funcionário, Login, Perfil e Recurso — a base sobre a qual as demais features atuam. **Especificação funcional detalhada (162 endpoints, regras de campo, códigos de erro) vive em `etc/doc/usecase/00` a `06` e não é repetida aqui.** Realiza a base de todas as Jornadas (UJ-1 a UJ-17) — todas dependem de Empresa/Departamento/Cargo/Funcionário/Login/Perfil existirem. Realiza diretamente UJ-6 a UJ-14, UJ-16, UJ-17.

**Estado atual (auditoria de código em `flow-organization-usecase`/`api`, 2026-07-18):**
- ✅ **Completo:** Department, Position (Create/Update/Enable/Disable/Find/FindAll), Legal Nature, CNAE, os 4 catálogos de Reason (Activate/Inactivate/Disable/Enable), Address Type, Contact Type.
- ⚠️ **Parcial:** Company tem apenas Create/Update/Find/FindAll — `enable/disable/block/unblock/hierarchy/branches/status-history/contacts/addresses` ainda não têm Use Case (`CompanyDelegate` documenta "comportamento default"); regras de ciclo de hierarquia, última matriz ativa e última empresa ativa (`SCOS_COMPANY_004/005/006`) têm mensagem de erro definida mas nenhuma validação implementada.
- ❌ **Zero:** Employee (nenhum Use Case, nenhum Delegate — só esqueleto de domínio + um serviço de consulta), Login (idem), Position/Employee Work Schedule (só entidade JPA), Reason Position Change (idem).

Ou seja: cadastros simples/catálogo já estão prontos; o que falta de fato é o núcleo de RH (Employee, Login inteiros) e as partes de Company/Work Schedule com transição de estado ou relacionamento mais complexo. `[ASSUMPTION: completar esta fundação é pré-requisito bloqueante para 4.2 e 4.3, não um pilar paralelo — é isso que o Brief chama de "sustentação".]`

**Requisitos Funcionais:**

#### FR-1: Cadastro hierárquico de Empresa
RH pode cadastrar uma Empresa como matriz ou filial de outra Empresa ativa, com CNPJ único.
**Consequences (testable):**
- Profundidade de hierarquia é limitada por `COMPANY_HIERARCHY_MAX_DEPTH` (configurável).
- Sistema rejeita ciclo na hierarquia (mãe não pode descender da própria filial). `[NOTE FOR PM: auto-referência direta (COMPANY_ID = PARENT_COMPANY_ID) já é bloqueada por trigger de banco hoje; o que falta é ciclo indireto (A → B → A) — código de erro existe (SCOS_COMPANY_004), validação não. Hoje isso é estruturalmente improvável porque parentCompanyId não é editável após a criação, mas a regra deve existir antes de qualquer endpoint futuro de edição de hierarquia.]`
- Sistema impede inativar a última matriz ativa do sistema e bloquear a única empresa ativa do sistema. `[NOTE FOR PM: hoje não implementado — SCOS_COMPANY_005/006.]`

#### FR-2: Ciclo de vida completo de Empresa
RH pode ativar/inativar/bloquear/desbloquear uma Empresa, sempre com motivo obrigatório do catálogo e histórico imutável.
**Consequences (testable):**
- Toda transição grava linha em histórico de status. `[NOTE FOR PM: "nunca editável/removível" é hoje garantia de convenção de aplicação, não proteção física de banco — ver §10, requisito de trigger de bloqueio físico para as tabelas novas desta feature.]`
- `disable` é rejeitado se existir filial ativa **em qualquer nível abaixo dela na hierarquia** (não só filial direta) — vale para toda transição que tira a Empresa do estado `ACTIVE` (`disable` e `block`), não só uma das duas. `[NOTE FOR PM: revisão de edge-case encontrou 2 lacunas na formulação original — (1) a regra só cobria a transição que bloqueia por completo, deixando `disable`/inativação livre para órfã uma filial `ACTIVE` sob matriz `INACTIVE`; (2) a checagem só olhava filial de 1º nível, deixando hierarquias de 3+ níveis furarem a regra. Ambas corrigidas aqui. Esta continua sendo a regra com menos lastro na auditoria — está na especificação (etc/doc/usecase/01-empresa.md), mas não tem sequer um código de erro reservado no enum, diferente das regras de ciclo/última-matriz/última-empresa (que ao menos têm SCOS_COMPANY_004/005/006 reservados). Vai precisar de um código novo.]`

#### FR-3: Cadastro de Departamento e Cargo
RH pode cadastrar Departamentos e Cargos vinculados a um Departamento ativo, cada um com ciclo enable/disable.
**Consequences (testable):**
- Criar/atualizar Cargo referenciando Departamento inativo é rejeitado. ✅ *Confirmado implementado e funcional (`PositionServiceBean`).*
- Desativar Departamento com Cargo ativo vinculado é rejeitado; mesma regra para Cargo com Funcionário ativo vinculado. ✅ *Confirmado implementado — a checagem de Funcionário ativo funciona mesmo sem nenhum Use Case de Employee existir, via `EmployeePositionQueryServiceBean`/`EmployeeQueryRepository` (repositório de leitura direto, contornando a camada de aplicação inexistente de Employee).*

#### FR-4: Jornada de Trabalho por Cargo, copiada ao Funcionário
RH pode definir um template de Jornada de Trabalho por Cargo e por dia da semana; a admissão de um Funcionário copia esse template para seu registro efetivo.
**Consequences (testable):**
- Validação cronológica estrita: `startTime < lunchStart < lunchEnd < endTime`. `[NOTE FOR PM: FR-4 inteiro é hoje 0% implementado — Position/EmployeeWorkSchedule são só entidades JPA, sem Use Case, sem Delegate, sem nenhuma validação escrita em lugar nenhum (nem essa checagem cronológica). Confirmado como regra de especificação (etc/doc/usecase/02), não como comportamento atual.]`
- Editar o template do Cargo depois da cópia **não** altera a Jornada já copiada de Funcionários existentes — mudança futura exige atualização explícita por Funcionário. *(Confirmado com o usuário como comportamento intencional.)*
- Dia da semana sem registro = "não definido" para aquele Funcionário (não herda template automaticamente depois da cópia inicial).

#### FR-5: Ciclo de vida completo de Funcionário
RH pode admitir, ativar/inativar/bloquear/desbloquear e recontratar um Funcionário, com validações de idade mínima, domínio de e-mail corporativo e CPF.
**Consequences (testable):**
- `EMPLOYEE_MIN_AGE` e `EMPLOYEE_EMAIL_DOMAIN` são lidos de Configuration (não hardcoded).
- Nova posição fecha automaticamente a posição anterior aberta (sem gap nem sobreposição de datas).
- **Recontratação (`rehire`) passa a aceitar Funcionário `INACTIVE` ou `DISABLED` como estado de origem** — decisão desta rodada, corrigindo uma lacuna identificada: hoje só `INACTIVE` tem o fluxo rico de reatribuição (empresa/cargo/supervisor); `DISABLED` só tinha `unblock` simples, sem poder reatribuir nada, mesmo sendo um cenário plausível (ex.: reintegração após suspensão, com mudança de cargo/filial). `unblock` continua existindo como caminho rápido para reintegração sem reatribuição (mesma empresa/cargo/supervisor de antes do bloqueio).
- `[NOTE FOR PM: o YAML atual (etc/api/organization/ScosOrganization_Employee.yml) tem um defeito de contrato conhecido no path /v1/employees/rehire — declara GET/PUT que são cópias acidentais dos métodos de /{id} (achado 3.5 do doc etc/doc/usecase/00-indice-central.md). Como esta FR altera regra de negócio exatamente desse endpoint, corrigir o defeito de spec deve andar junto da implementação, não ficar pra depois.]`

#### FR-6: Login vinculado a Funcionário, sincronizado com Keycloak
RH pode criar um Login para um Funcionário; a partir da aprovação (FR-23), o Login fica `ACTIVE` e sincroniza com o Keycloak em segundo plano via Saga/Outbox.
**Consequences (testable):**
- Falha de sincronização com Keycloak não impede o Login de existir no SawCunhaOS (fire-and-forget), mas fica registrada e reprocessável no Outbox.
- Bloquear/desbloquear/ativar/inativar um Login gera histórico imutável e nova Saga de atualização.
- Permissões de transição de status são simétricas por ação — `BLOCK_LOGIN` e `UNBLOCK_LOGIN` separadas, no mesmo padrão já usado em `ENABLE_LOGIN`/`DISABLE_LOGIN` e em `BLOCK_COMPANY`/`UNBLOCK_COMPANY`. `[NOTE FOR PM: hoje o código usa uma permissão única (UPDATE_LOGIN_STATUS) para block+unblock — inconsistente com o próprio precedente de Company. Corrigir para granularidade simétrica. Isso substitui um x-authorize já publicado em etc/api/organization/ScosOrganization_Login.yml — é mudança breaking de contrato, sujeita à política de versionamento de §11 (convivência mínima de 12 meses) se a API já tiver consumidor em produção; se ainda não tiver, pode ser corrigido direto na v1 sem versionar.]`

#### FR-7: Perfis e Recursos controlam autorização granular
Um Login tem um Perfil principal e pode ter Perfis adicionais; cada Perfil agrega Recursos (permissões).
**Consequences (testable):**
- Toda operação de API exige `x-authorize` mapeado 1:1 a uma constante de permissão (garantido por teste de contrato já existente, `PermissionsConsistencyTest`).
- `[NOTE FOR PM: bug conhecido — Perfis adicionais hoje não somam permissões na view de autoridade (vw_login_context só faz JOIN com o perfil principal). Correção de view necessária antes de qualquer feature que dependa de perfil adicional funcionar corretamente.]`

#### FR-23: Aprovação obrigatória para criação de Login
Todo Login recém-criado exige aprovação de um aprovador válido — nunca a própria pessoa que abriu a solicitação — antes de se tornar utilizável. Realiza uma extensão de UJ-6.

`[NOTE FOR PM: esta FR reverte, de forma deliberada, uma decisão já documentada em etc/doc/usecase/00-indice-central.md §1 ("Login não tem mais PENDING... a criação é fire-and-forget: nasce ACTIVE imediatamente"). É mudança consciente de produto desta rodada, não descuido — mas é breaking change de contrato (o efeito colateral de POST /v1/logins deixa de ser "ACTIVE imediato"). Precisa: (a) atualizar o doc de usecase 00/04 para refletir o novo comportamento, (b) tratar como mudança de versão de API conforme a política de §11 caso a API já tenha consumidores em produção. O mesmo vale para FR-27, que introduz estado pendente em Perfil (hoje documentado como simples ACTIVE=true/false).]`

**Consequences (testable):**
- Login criado entra em estado `PENDING_APPROVAL` — não autentica nem é aceito em nenhuma chamada de API enquanto pendente.
- **Cadeia de resolução do aprovador** (cada nível pulado se ausente OU se coincidir com quem abriu a solicitação):
  1. Supervisor direto do Funcionário.
  2. Gerente do Departamento do Funcionário.
  3. Qualquer Login com a permissão universal `APPROVE_SYSTEM_ACCESS` (tabela de aprovadores de última instância, mesmo grupo de FR-26/FR-27) — fallback final quando nem supervisor nem gerente servem.
- **Princípio de segregação de função (four-eyes):** quem abre a solicitação nunca pode ser o aprovador da mesma solicitação, em nenhum nível da cadeia acima — vale mesmo que essa pessoa seja o supervisor, o gerente, ou membro do grupo `APPROVE_SYSTEM_ACCESS`.
- **O gate de aprovação não depende de notificação para funcionar.** Sistema expõe uma consulta de solicitações pendentes (`GET`, filtrável por aprovador) — o aprovador pode agir a qualquer momento consultando essa lista, mesmo sem nenhum aviso proativo. Notificar o aprovador assim que o Login entra em `PENDING_APPROVAL`, via modo síncrono do Motor de Notificação (FR-19), é um reforço de UX, não pré-requisito funcional. `[NOTE FOR PM: decisão desta rodada — resolve a inversão de prioridade apontada na revisão (FR-23 é pré-requisito da Fundação/P0, FR-19 é P2); o fluxo de aprovação funciona por consulta direta desde o primeiro dia, ganhando notificação proativa quando o Motor de Notificação for entregue.]`
- Aprovador aprova ou rejeita através de ação dedicada na API, protegida por permissão própria (`APPROVE_LOGIN`); a própria API rejeita a tentativa se quem chamar for o solicitante original — **exceto** no caso de última instância descrito abaixo.
- Aprovação → Login transita para `ACTIVE` e segue o fluxo normal de FR-6 (Saga Keycloak).
- Rejeição → `[ASSUMPTION: Login transita para estado terminal REJECTED, não reutilizável — RH deve abrir nova solicitação se ainda necessário]`; RH é notificado da rejeição.
- Toda decisão (aprovação ou rejeição) gera registro de auditoria imutável — quem decidiu, quando, e o resultado.
- **Status do Funcionário é reverificado no momento da decisão, não só no momento da criação da solicitação.** Se o Funcionário tiver sido desligado (`DISABLED`/`INACTIVE`) enquanto seu Login segue `PENDING_APPROVAL`, a aprovação é bloqueada automaticamente e a solicitação cai para `REJECTED` — impede aprovar acesso para quem já não deveria mais tê-lo.
- **SLA de 1 dia útil por nível.** Sem decisão dentro desse prazo, a solicitação escala automaticamente para o próximo nível da cadeia (supervisor → gerente → grupo `APPROVE_SYSTEM_ACCESS`); esgotada a cadeia, o grupo universal é renotificado por inteiro (mesmo padrão de FR-26).
- **Mitigação primária:** o grupo `APPROVE_SYSTEM_ACCESS` deve ter sempre, por regra operacional, ao menos 2 membros — evitando na prática que o solicitante seja a única pessoa apta em qualquer nível.
- **Mitigação de última instância:** se, ainda assim, ninguém além do próprio solicitante estiver disponível para aprovar em nenhum nível da cadeia, o solicitante pode aprovar sua própria solicitação como exceção — mas esse evento é **obrigatoriamente marcado no registro de auditoria** como "aprovação por exceção (auto-aprovação)", tornando o desvio da segregação de função sempre visível e rastreável, nunca silencioso.

#### FR-24: Aprovação obrigatória para reativação de Login
Reativar um Login — via `enable` (`INACTIVE`→`ACTIVE`) ou via `unblock` (`BLOCKED`→`ACTIVE`) — exige a mesma aprovação de FR-23 antes de o Login voltar a ser utilizável.
**Consequences (testable):**
- Mesma cadeia de resolução de aprovador de 3 níveis de FR-23 (Supervisor → Gerente do Departamento → grupo `APPROVE_SYSTEM_ACCESS`), mesma segregação de função, mesmo mecanismo de estado pendente/consulta/notificação, e mesmo SLA de 1 dia útil por nível com escalonamento — sem exceção, é literalmente a mesma resolução, não uma versão reduzida.
- Login permanece no seu estado atual (`INACTIVE` ou `BLOCKED`) até a aprovação — nunca transita para `ACTIVE` sem ela.
- `[NOTE FOR PM: assimetria intencional em relação ao Kill Switch — desligar/bloquear (FR-13) é automático e instantâneo (segurança); reativar (FR-24) exige aval humano (governança). Uma coisa nunca bloqueia a outra.]`
- **Diferença deliberada em relação a FR-28:** aqui a escalada de SLA é indefinida (supervisor → gerente → grupo universal, renotificando até alguém decidir), porque a solicitação foi aberta por uma pessoa que genuinamente quer o acesso de volta. Em FR-28, a solicitação nasce de um gatilho automático do sistema (data prevista de retorno), por isso tem cancelamento automático em vez de escalonamento indefinido — políticas diferentes por terem origem diferente, não inconsistência.

#### FR-25: Aprovação obrigatória para mudança de Perfil de um Login
Toda mudança que aumenta o acesso de um Login existente — troca do Perfil principal ou adição de Perfil adicional — exige a mesma aprovação de FR-23 antes de ter efeito.
**Consequences (testable):**
- Solicitação de troca de Perfil principal ou de adição de Perfil adicional entra em estado de aprovação pendente — mas, diferente de FR-23/24, **o Login continua ACTIVE e operando com o(s) Perfil(is) atual(is)** enquanto aguarda; a mudança só é aplicada após aprovação, nunca bloqueia o acesso já existente.
- Mesma cadeia de resolução de aprovador de 3 níveis de FR-23 (Supervisor → Gerente do Departamento → grupo `APPROVE_SYSTEM_ACCESS`), mesma segregação de função, mesmo mecanismo de notificação/consulta e mesmo SLA de 1 dia útil com escalonamento.
- Aprovação → mudança de Perfil se aplica normalmente. Rejeição → Login mantém o(s) Perfil(is) anterior(es), sem alteração.
- Toda decisão gera registro de auditoria imutável (mesmo requisito de proteção física de banco do §10).
- `[ASSUMPTION: remover um Perfil adicional (reduzir acesso, não aumentar) não exige aprovação — só concessão de acesso passa pelo gate, nunca revogação.]`
- Alterar quais Recursos um Perfil contém (`PUT /v1/profiles/{id}/resources`) **não** é coberto por esta FR — afeta todos os Logins daquele Perfil, não um usuário específico; coberto por FR-27, com aprovador diferente.

#### FR-26: Aprovação obrigatória para Login sem vínculo a Funcionário
Login criado sem Funcionário associado (tipos `EXTERNAL`/`SERVICE`) segue o mesmo mecanismo de aprovação de FR-23/24, mas o aprovador não é resolvido por hierarquia (não há Funcionário, logo não há supervisor) — é qualquer Login designado explicitamente com a permissão de aprovação de acesso de sistema.
**Consequences (testable):**
- Login sem Funcionário vinculado entra em `PENDING_APPROVAL` na criação e a cada reativação (`enable`/`unblock`), mesmo padrão de estado de FR-23/24.
- Aprovador: qualquer Login que detenha a permissão dedicada `APPROVE_SYSTEM_ACCESS`, atribuída explicitamente pelo RH/Admin a Logins específicos — **não** derivada de Departamento, Cargo ou qualquer hierarquia (design escolhido nesta rodada: designação direta de indivíduos, não de área/cargo).
- Notificação enviada simultaneamente a todos os Logins com essa permissão; qualquer um deles pode decidir, **exceto quem abriu a solicitação** (mesmo princípio de segregação de função de FR-23) — precisa ser outro membro do grupo.
- SLA de 1 dia útil; sem decisão no prazo, sistema renotifica todo o grupo novamente (escalonamento horizontal — sem hierarquia adicional para escalar). Atraso registrado na contra-métrica SM-C3.
- Mesma auditoria imutável de FR-23. **Herda também a mesma válvula de última instância de FR-23:** se o grupo `APPROVE_SYSTEM_ACCESS` cair para 1 único membro disponível (violação da regra operacional de 2+), esse membro pode aprovar sua própria solicitação como exceção, sempre com o mesmo registro obrigatório de "aprovação por exceção (auto-aprovação)" — sem essa válvula, um Login `SERVICE` ou Perfil novo ficaria preso em `PENDING_APPROVAL` para sempre nesse cenário.

#### FR-27: Aprovação obrigatória para criação e edição de Perfil
Criar um novo Perfil ou alterar quais Recursos um Perfil existente contém exige aprovação do mesmo grupo de FR-26, antes de ter efeito — dado o impacto de segurança sobre todos os Logins que usam aquele Perfil.
**Consequences (testable):**
- Perfil novo entra em estado pendente — não pode ser atribuído a nenhum Login enquanto não aprovado.
- Edição dos Recursos de um Perfil existente: Logins que já usam esse Perfil continuam com o conjunto de Recursos **anterior** em vigor até a mudança ser aprovada — nunca aplica mudança de permissão sem aprovação prévia (mesma lógica de "não perde acesso existente" de FR-25).
- Aprovador: mesmo grupo de Logins com `APPROVE_SYSTEM_ACCESS` (FR-26), **exceto quem abriu a solicitação** (mesma segregação de função de FR-23).
- Mesmo SLA de 1 dia útil e mesmo escalonamento horizontal (renotificação do grupo).
- Mesma auditoria imutável, incluindo a mesma válvula de última instância (auto-aprovação de exceção) de FR-26.
- Fecha a lacuna deixada explicitamente fora de FR-25.

#### FR-28: Desativação temporária por Férias/Licença Médica com retorno assistido
RH pode colocar um Funcionário em licença temporária (férias ou licença médica) reaproveitando o mecanismo de disable/enable já existente (FR-5, FR-6) — sem estado novo — usando dois Motivos dedicados e uma data prevista de retorno; ao chegar essa data, o sistema abre sozinho a solicitação de reativação, que ainda passa pela aprovação normal.
**Consequences (testable):**
- Dois novos Motivos cadastrados no catálogo **`SCOS_REASON_INACTIVATE`** (o catálogo correto da transição `disable`, que leva a `INACTIVE` — não confundir com `SCOS_REASON_DISABLE`, que é o catálogo da transição `block`): "Férias" e "Licença Médica". Reaproveita 100% do mecanismo de disable/enable de Funcionário e Login já especificado (FR-5, FR-6), sem introduzir status novo.
- Transição de `disable` com um desses dois Motivos aceita um campo opcional de **data prevista de retorno**.
- Login vinculado ao Funcionário é desativado junto, disparando o Kill Switch normalmente (FR-13) — mesma regra de qualquer `disable`, sem exceção por ser licença.
- **Na data prevista de retorno, o sistema abre automaticamente a solicitação de reativação do Login** (mesmo fluxo de FR-24) — RH não precisa lembrar de reativar manualmente. **Exceto se o Funcionário já tiver sido desligado por outro motivo nesse meio-tempo** (`block`, ou `disable` com Motivo diferente de Férias/Licença Médica) — nesse caso o gatilho automático não dispara; o Funcionário já não está mais em licença, está desligado, e reativar seguiria o fluxo normal de FR-24 se e quando alguém pedir, não automaticamente.
- Mesmo com o gatilho automático, a reativação **continua exigindo aprovação humana normal** (supervisor, com a cadeia de fallback de FR-23) antes de o Login voltar a `ACTIVE` — o sistema abre a solicitação, mas não aprova sozinho.
- `[ASSUMPTION: se a licença for prorrogada antes da data prevista, RH atualiza a data de retorno (ou registra novo disable com nova data); o sistema não tem como inferir sozinho que a licença foi estendida.]`
- **Cancelamento automático se não decidido em `[ASSUMPTION: 5 dias úteis, valor de referência a confirmar]`** — diferente das demais aprovações (que seguem escalando indefinidamente), a solicitação de retorno de licença/férias é cancelada automaticamente se ninguém decidir nesse prazo, já que sua origem é o sistema, não uma pessoa pedindo acesso de volta. RH precisa então abrir manualmente a reativação quando a pessoa de fato retornar.

**NFRs específicos da feature:**
- O gate de aprovação (FR-23 a FR-27) não pode ser contornado por nenhuma outra rota de criação/reativação/mudança de Login ou Perfil — mesma premissa Zero Trust de §4.2 (nenhum bypass client-side ou de fluxo alternativo).
- Views de autoridade (`vw_login_context`, `vw_authority_response`) são materializadas e atualizadas a cada 30 min via `pg_cron` — qualquer feature que dependa de troca de perfil em tempo real (incluindo 4.2, FR-25 e FR-27) precisa considerar essa defasagem conhecida.

---

### 4.2 Governança Ativa via API (Zero Trust por Turno) — **Etapa 1 (P0)**
**Descrição:** o sistema impõe, na camada de serviço/API — nunca no front-end —, uma janela de acesso para Perfis operacionais marcados como sujeitos a bloqueio de turno. Fora da janela, a requisição é negada antes de qualquer lógica de negócio executar. Realiza UJ-1, UJ-5.

#### FR-8: Classificação de Perfis sujeitos a bloqueio de turno
RH pode marcar quais Perfis exigem bloqueio de turno e quais têm acesso irrestrito por justificativa legal (gerência, administração, suporte emergencial).
**Consequences (testable):**
- Perfil sem a marcação mantém o comportamento atual (sem checagem de turno).
- Classificação de acesso irrestrito é uma flag simples e permanente por Perfil — sem aprovação ou expiração por caso. Não é um mecanismo de exceção pontual: cargo/Perfil legalmente justificado (gerência, administração, suporte emergencial) fica isento de forma contínua.
- Qualquer necessidade de exceção pontual para um Perfil normalmente restrito (não isento) segue pelo mecanismo de plantão pré-aprovado (FR-12) — não duplica lógica aqui.
- Mudança de classificação de um Perfil se aplica a todos os Logins vinculados a ele, respeitando a defasagem de até 30 min das views de autoridade (FR-7).

#### FR-9: Bloqueio de requisição fora da Jornada de Trabalho
Toda requisição de API autenticada, originada de um Login cujo Perfil exige bloqueio de turno, é avaliada contra a Jornada de Trabalho vigente do Funcionário antes da lógica de negócio executar.
**Consequences (testable):**
- Fora do intervalo `[startTime, endTime]` do dia da semana corrente (excluindo o intervalo de almoço) → `403 Forbidden`, corpo `ProblemDetail` padrão SCOS, nenhum dado de negócio processado.
- Dentro do intervalo → requisição segue o fluxo normal, sem latência perceptível adicional.

#### FR-10: Comportamento fail-closed sem Jornada cadastrada
Login de Funcionário sujeito a bloqueio de turno sem Jornada de Trabalho cadastrada para o dia da semana corrente é tratado como fora de turno.
**Consequences (testable):**
- Ausência de registro de Jornada para o dia = bloqueio, nunca liberação por omissão. `[ASSUMPTION: fail-closed é a postura correta para um motor Zero Trust — confirmar com stakeholder de segurança.]`

#### FR-11: Auditoria imutável de toda decisão de turno
Toda avaliação de bloqueio de turno — permitida ou negada — gera um registro de auditoria imutável (quem, quando, endpoint, decisão, dentro/fora da janela).
**Consequences (testable):**
- Realiza a métrica "Rastreabilidade Trabalhista" (SM-2): 100% das avaliações geram log, sem exceção — inclusive as liberadas por acesso irrestrito ou plantão.
- Log não é editável nem removível por nenhuma API deste sistema.
- Registro é retido por, no mínimo, **5 anos após o desligamento do Funcionário** vinculado ao Login (alinhado à prescrição trabalhista do art. 7º XXIX da CF/88 — 5 anos, limitada a 2 anos após extinção do contrato). **Decisão definitiva confirmada nesta rodada** — não é mais pendência de sign-off.

#### FR-12: Acesso extraordinário de plantão via escala pré-aprovada
RH/gestor pode cadastrar previamente uma janela de plantão (início/fim) para um Login/Funcionário específico — uma segunda janela de acesso válida além da Jornada de Trabalho regular (FR-9), no mesmo padrão de cadastro por período já usado em Work Schedule (FR-4).
**Consequences (testable):**
- Requisição dentro de uma janela de plantão cadastrada é liberada mesmo fora da Jornada de Trabalho regular.
- Toda liberação por plantão permanece sujeita a FR-11 (auditoria imutável), com o registro identificando explicitamente que a liberação ocorreu via janela de plantão — não via jornada regular nem via acesso irrestrito.
- Plantão não cadastrado = sem liberação. Não há aprovação em tempo real nem flag manual nesta fase — emergência não prevista fora de qualquer janela cadastrada é tratada como acesso irrestrito (FR-8) ou fica bloqueada.

**NFRs específicos da feature:**
- A checagem de turno é síncrona e bloqueante em toda requisição sujeita a ela — deve adicionar latência desprezível `[ASSUMPTION: p95 < 10ms]`.
- Nenhum mecanismo de bypass client-side é possível — a regra não pode ser satisfeita ou contornada por manipulação de request no front-end/Postman/curl (é a premissa central do Brief, seção "What Makes This Different").

---

### 4.3 Ciclo de Vida Integrado (Kill Switch) — **Etapa 1 (P0)**
**Descrição:** ao Funcionário ser desligado ou Login ser bloqueado/desabilitado, todas as sessões/tokens ativos vinculados são invalidados de forma síncrona — não há janela de tolerância até a expiração natural do token. Realiza UJ-2, UJ-15.

#### FR-13: Invalidação síncrona de sessões ao desligamento/bloqueio
Ao Funcionário transitar para `DISABLED` ou Login transitar para `BLOCKED`/`INACTIVE`, o sistema invalida, de forma síncrona, todas as sessões/tokens JWT ativos vinculados a esse Login.
**Consequences (testable):**
- Qualquer requisição feita com o token antigo retorna `401` em menos de 1 segundo após a transição de status ser confirmada (Success Criteria original do Brief).
- A invalidação faz parte do mesmo fluxo de negócio que muda o status — não pode depender de uma fila assíncrona best-effort.
- A transição de status (`DISABLED`/`BLOCKED`) no banco **sempre sucede**, mesmo se o mecanismo de invalidação de sessão estiver indisponível no momento — RH nunca fica impedido de desligar/bloquear um colaborador por uma dependência externa fora do ar.
- Indisponibilidade do mecanismo de invalidação no momento da transição aciona retry automático + alerta obrigatório até a propagação ser confirmada.
- Enquanto a invalidação não propagar (ou se o próprio mecanismo de checagem estiver inacessível na entrada), toda validação de token que não conseguir confirmar o estado falha **fechado** (nega o acesso) — nunca libera por "não conseguiu verificar", mesma postura fail-closed já adotada em FR-10.
- `[ASSUMPTION: o mecanismo concreto (ex.: denylist de token em cache compartilhado, ou revogação via Admin API do Keycloak) é decisão de arquitetura — ver addendum.md. Hoje não existe nenhuma peça desse mecanismo no código: sem cache compartilhado de sessão, sem chamada de revogação ao Keycloak.]`

#### FR-14: Reativação não restaura sessões antigas
Reativar (`enable`/`unblock`) um Funcionário ou Login não restaura nenhuma sessão anterior; o usuário deve autenticar novamente.
**Consequences (testable):**
- Reativar Funcionário não reverte automaticamente o status `INACTIVE`/`BLOCKED` de Logins vinculados — ação separada (comportamento já confirmado no doc de usecase 00, mantido aqui como requisito explícito).

**NFRs específicos da feature:**
- Invalidação fim-a-fim (transição de status → efeito no próximo request) em menos de 1 segundo, sob carga normal do sistema.
- Monitoramento/alerta de indisponibilidade do mecanismo de invalidação é requisito, não opcional — ver Riscos e Mitigações, §9.

---

### 4.4 Motor Geotemporal — **Etapa 2 (P1)**
**Descrição:** resolução de endereço a partir de CEP (com fallback e cache) e calendário de dias úteis/feriados nacional, estadual e municipal, com tags operacionais customizadas. Realiza UJ-3.

#### FR-15: Resolução de endereço por CEP com fallback
Sistema resolve endereço a partir de um CEP informado, consultando um provedor externo primário com fallback para um segundo provedor em caso de indisponibilidade.
**Consequences (testable):**
- Indisponibilidade do provedor primário aciona automaticamente o fallback, sem exigir nova ação do usuário.
- Falha de ambos os provedores permite cadastro manual do endereço, com aviso explícito ao usuário.
- `[ASSUMPTION: provedor primário é ViaCEP (citado no Brief); provedor de fallback e política de timeout são decisão de arquitetura — ver addendum.md.]`

#### FR-16: Cache de consulta de CEP
Resultado de consulta de CEP é cacheado para suportar picos de consulta sem degradar o serviço/tabela de origem.
**Consequences (testable):**
- Consulta repetida do mesmo CEP dentro do TTL não gera nova chamada ao provedor externo.
- `[ASSUMPTION: TTL configurável, valor inicial de referência 24h — a validar com Arquitetura.]`

#### FR-17: Calendário de dias úteis nacional/estadual/municipal
Sistema mantém e expõe consulta "é dia útil em {município/UF} na data X", combinando feriados nacionais, estaduais e municipais.
**Consequences (testable):**
- Consulta retorna corretamente feriados municipais que não afetam outros municípios do mesmo estado.
- Zero divergência de feriado local reportada nos ISPs de teste (SM-4, meta original do Brief).

#### FR-18: Tags operacionais customizadas em datas
Usuário pode criar tags customizadas em datas do calendário (ex.: "Janela de Manutenção").
**Consequences (testable):**
- Tag é usada apenas como rótulo informativo e filtro de relatório nesta fase.

**Out of Scope (FR-18):**
- Qualquer automação disparada por tag (ex.: bloquear agendamento automaticamente numa janela marcada) — decisão de escopo já registrada no Brief, reservada para Fase 2.

**NFRs específicos da feature:**
- Resolução de CEP deve suportar alta concorrência sem degradar o banco principal `[ASSUMPTION: p95 < 300ms sob carga de pico]`.

---

### 4.5 Motor de Notificação Híbrida — **Etapa 3 (P2)**
**Descrição:** despachante de mensagens desacoplado, com modo síncrono (bloqueante, para alertas críticos e tokens de validação) e modo assíncrono (fila, reaproveitando a infraestrutura de Outbox já existente, para rotinas em lote), multi-canal. Realiza UJ-4.

#### FR-19: Envio síncrono bloqueante
Sistema oferece um modo de envio síncrono: a chamada bloqueia até confirmar sucesso ou falha do envio, usado para alertas de segurança e tokens de validação.
**Consequences (testable):**
- O chamador só recebe resposta depois que o envio (ou a falha registrada) é confirmado — nunca "dispara e esquece" nesse modo.

#### FR-20: Envio assíncrono via fila
Sistema oferece um modo assíncrono, publicando o evento na infraestrutura de Outbox existente (Events/Topics) para processamento em segundo plano, usado em rotinas e avisos em lote.
**Consequences (testable):**
- Publicação do evento não bloqueia a requisição que a originou.
- `[NOTE FOR PM: hoje o Outbox é genérico na arquitetura mas seu único consumidor real é a Saga de sincronização de Login com o Keycloak — este FR exige um novo consumidor/dispatcher de notificação, não apenas reuso passivo.]`

#### FR-21: Suporte multi-canal
Sistema despacha notificações pelos canais Email, SMS, notificação In-App e Web.
**Consequences (testable):**
- Escolha de provedor externo de Email/SMS é decisão de arquitetura (custo, SLA, presença no Brasil) — não fixada neste PRD. Ver `addendum.md`.

#### FR-22: Zero perda de mensagem
Toda falha de envio, síncrono ou assíncrono, é registrada e reprocessável.
**Consequences (testable):**
- Reaproveita o mecanismo de dead-letter/retry já existente no Outbox Events.
- Realiza a métrica "Segurança de Notificação" (SM-5): zero mensagens perdidas ou descartadas.

**NFRs específicos da feature:**
- Modo síncrono não pode adicionar latência perceptível ao fluxo que o dispara `[ASSUMPTION: orçamento de latência a definir com Arquitetura]`.

## 5. NFRs Transversais

- **Performance:** checagem de turno síncrona `[ASSUMPTION: p95 < 10ms]` (FR-9); resolução de CEP `[ASSUMPTION: p95 < 300ms]` sob pico (FR-15/16); Kill Switch fim-a-fim < 1s (FR-13).
- **Segurança:** Zero Trust — toda regra de acesso vive no back-end, nunca depende de confiança no cliente; token revogado nunca é aceito novamente, mesmo dentro da validade original; PII (CPF, e-mail, endereço) mascarada em log via `scos-foundation-privacy`; nenhum segredo literal em YAML (Jasypt).
- **Confiabilidade:** zero perda de mensagem (FR-22); views de autoridade (`vw_login_context`, `vw_authority_response`) são materializadas com defasagem conhecida de até 30 min — qualquer feature de segurança crítica que dependa de Perfil/permissão (FR-8) precisa considerar essa janela ou propor invalidação ativa de cache (ver Riscos, §9).
- **Observabilidade:** toda transição de status sensível e toda decisão de bloqueio de turno reaproveita o padrão de auditoria imutável já existente na Fundação — não é infraestrutura nova (ver §10).

## 6. Restrições e Guardrails

- **Segurança:** nenhuma regra de acesso pode ser satisfeita ou contornada por manipulação de requisição no cliente (front-end, Postman, curl) — premissa arquitetural, testável via chamada direta à API ignorando qualquer client.
- **Privacidade:** CPF, e-mail e endereço de Funcionário são PII — seguem a política de mascaramento já adotada (`%mask`/`%maskmdc`); nenhuma PII em texto puro em log de auditoria de acesso.
- **Custo:** sem orçamento de infraestrutura definido neste PRD — provedores externos (CEP, Email/SMS) devem ser avaliados por custo pela Arquitetura antes da escolha final (ver `addendum.md`).

## 7. Conformidade Trabalhista e Regulatória

*Seção adicionada por ser o motivador central do Product Brief — não é um cluster padrão de template.*

- **CLT:** o bloqueio de turno (§4.2) existe especificamente para eliminar o passivo de horas extras não autorizadas; a rastreabilidade (FR-11) serve de evidência em eventual disputa trabalhista.
- **LGPD:** dados de Funcionário (CPF, e-mail, endereço, contato) são dados pessoais; tratamento deve ter base legal (execução de contrato de trabalho) e minimização.
- **Retenção de log de auditoria:** mínimo de 5 anos após o desligamento do Funcionário, alinhado ao art. 7º XXIX CF/88 (ver FR-11). Decisão definitiva.

## 8. Integrações e Dependências

- **Keycloak (OAuth2/JWT)** — Identity Provider já em uso; Kill Switch (FR-13) é candidato a depender de sua Admin API ou de um cache paralelo — decisão de arquitetura.
- **Redis** — hoje usado para cache/idempotência (jDempotent); Kill Switch e cache de CEP são candidatos naturais a reaproveitá-lo, mas nenhuma das duas features usa Redis ainda no código atual.
- **ViaCEP (ou equivalente)** — dependência externa nova, com fallback obrigatório (FR-15).
- **scos-registry (gRPC)** — fonte de verdade de Recurso, já integrada, somente leitura.
- **Outbox (PGMQ/Kafka/Direct API)** — infraestrutura assíncrona já existente, reaproveitada por §4.5; hoje só consumida pela Saga Keycloak.
- **Fase 2 (CRM, Faturamento)** — consumidor downstream direto desta fundação; qualquer breaking change em contrato de API/permissão aqui tem custo amplificado depois.

## 9. Riscos e Mitigações

- **Bloqueio de turno mal calibrado barra emergência real** → mitigado por Perfil de acesso irrestrito (FR-8) + janela de plantão pré-aprovada (FR-12).
- **Kill Switch falha silenciosamente** (componente de invalidação indisponível, sessão continua viva) → mitigado: transição de status no banco nunca é bloqueada pela dependência; toda validação de token que não conseguir confirmar o estado falha fechado; indisponibilidade aciona retry + alerta obrigatório (FR-13).
- **Defasagem de até 30 min nas views de autoridade** pode atrasar a aplicação de nova classificação de Perfil (FR-8) → considerar invalidação ativa de cache para mudanças de segurança crítica, não só refresh periódico.
- **Dependência de ViaCEP externo** (indisponibilidade) → mitigado por fallback + cache (FR-15/16).
- **3 dos 4 pilares de Fase 1 partem de 0% de implementação** (confirmado nesta auditoria de código) → mitigado pela divisão em Etapas (§13): só a Etapa 1/P0 (segurança/passivo trabalhista) está em construção agora; Geotemporal (Etapa 2) e Notificação (Etapa 3) são entregas futuras separadas, não bloqueiam nem são bloqueadas pela Etapa 1.
- **Governança de Turno (§4.2, P0) depende de Jornada de Trabalho (FR-4), que é 0% implementada** — a checagem de turno não tem dado nenhum pra avaliar sem isso. FR-4 deve ser tratado como dependência bloqueadora de §4.2, com prioridade de construção equivalente, não como item secundário da Fundação.
- **Kill Switch (§4.3, P0) depende de Employee e Login terem Use Case/Delegate reais (FR-5/FR-6), hoje 0% implementados** — mesma natureza da dependência acima (FR-4→§4.2), mas para o outro pilar P0; sem essa base, não há o que desligar/invalidar.
- **Ex-Funcionário pode receber Login aprovado por engano:** se o Funcionário for desligado enquanto seu próprio Login ainda está `PENDING_APPROVAL` (FR-23), nada impede hoje que o aprovador aprove um acesso para alguém que já não deveria mais tê-lo → mitigação a construir: checagem do status do Funcionário no momento da decisão de aprovação, não só no momento da criação da solicitação.
- **SLA de 1 dia útil (FR-23 a FR-28) depende do calendário de dias úteis (FR-17, Motor Geotemporal, Etapa 2)** para saber o que conta como "dia útil" — mas FR-17 só existe na Etapa 2, uma entrega futura separada. **Para toda a Etapa 1 (a única em construção agora), "1 dia útil" deve ser tratado como "1 dia corrido"** — não é uma lacuna temporária de poucos dias, é o comportamento real enquanto a Etapa 2 não for entregue.
- **Bootstrap do grupo `APPROVE_SYSTEM_ACCESS`:** a permissão em si só pode ser concedida através de um Perfil aprovado (FR-27) por alguém que já tenha `APPROVE_SYSTEM_ACCESS` — paradoxo de ovo-e-galinha na primeira vez. Mitigação: o(s) primeiro(s) membro(s) do grupo devem ser semeados diretamente via seed/migração de banco na implantação inicial, nunca através do próprio fluxo de aprovação da API.

## 10. Trilha de Auditoria

- Toda transição de status sensível (Empresa/Funcionário/Login) e toda decisão de bloqueio de turno (FR-11) usa o padrão de auditoria já estabelecido pela Fundação (`@Auditable`, tabelas `*_STATUS_HISTORY`) — extensão do padrão existente à nova decisão de turno, não infraestrutura nova.
- **Nenhum endpoint desta API permite editar ou remover um registro de auditoria hoje** — mas essa garantia é hoje de **convenção de aplicação** (nenhum Use Case chama update/delete nessas tabelas), **não de proteção física de banco**. `[NOTE FOR PM: verificado em auditoria de código — `CompanyStatusHistory` só tem trigger `BEFORE INSERT`; o repositório (`BaseJpaRepository`) expõe update/delete genéricos que a camada de aplicação simplesmente não invoca. Diferente de `SCOS_REASON_*`/Catalog/Outbox, que já têm `fn_block_delete` bloqueando DELETE físico a nível de trigger. Requisito para as novas tabelas de histórico (FR-11, FR-23/24): aplicar a mesma trigger de bloqueio físico (`fn_block_delete` ou equivalente `BEFORE UPDATE/DELETE`) desde a primeira versão — não repetir a lacuna das tabelas de status history existentes.]`

## 11. Contratos de API / Superfície Pública

- Esta API é a fundação sobre a qual Fase 2 (CRM, Faturamento) será construída — qualquer mudança breaking em contrato (`x-authorize`, shape de resposta, remoção de campo) tem custo amplificado em consumidores futuros.
- Contrato-primeiro já é convenção do projeto (YAML antes de código) — manter.
- **Política de versionamento/depreciação:** versionamento por path já existe na convenção do projeto (`/v1/...`). Formaliza-se: toda mudança breaking sobe a versão de path (`/v2/...`); a versão anterior permanece funcional por **no mínimo 12 meses** após o lançamento da nova, com aviso prévio aos consumidores (incluindo Fase 2). Mudança não-breaking (campo opcional novo, endpoint novo) não exige nova versão.

## 12. Não-Objetivos (Explícitos)

- SawCunhaOS Fase 1 **não é** um CRM — sem prospecção, funil de vendas ou carteira de clientes.
- **não é** um módulo financeiro — sem emissão/baixa de boleto, remessa bancária ou gateway de pagamento.
- **não faz** provisionamento de rede — sem integração com Radius, gerência de OLT ou configuração automática de roteadores.
- **não é** um Portal do Assinante — sem autoatendimento, fatura ou abertura de chamado para o cliente final.
- **não substitui** o Keycloak como Identity Provider — consome-o (JWT, Admin API), não o reimplementa.
- **não inclui** front-end/UI própria neste repositório — é motor de API para consumo externo.
- Tags operacionais em Fase 1 **não disparam automação** — são apenas rótulo e filtro (decisão de escopo já registrada no Brief).

## 13. Escopo do MVP (Fase 1)

**Decisão desta rodada: P0/P1/P2 deixam de ser só ordem de construção e viram corte real de escopo por release.** Fase 1 (esta PRD, no sentido do Product Brief) se divide em 3 entregas sequenciais — **Etapa 1, 2 e 3** — cada uma um MVP fechado e utilizável, não uma fatia técnica incompleta. `[NOTE FOR PM: "Etapa" aqui é subdivisão interna desta Fase 1 — não confundir com a "Fase 2" do Product Brief (CRM/Faturamento/Provisionamento), que é um produto totalmente separado e permanece fora do escopo de todas as 3 Etapas abaixo.]`

### 13.1 Etapa 1 (P0) — única em construção agora
- Fundação de Identidade e Organização completa (§4.1) — incluindo fechar os gaps hoje existentes em Company (enable/disable/hierarquia), implementar Employee/Login além do esqueleto de domínio, **e todo o sistema de aprovação de acesso (FR-23 a FR-28)**, que é pré-requisito para qualquer Login existir de forma governada.
- Governança Ativa via API / bloqueio de turno (§4.2).
- Kill Switch (§4.3).

Este é o MVP real: entrega sozinho os 3 passivos centrais do Product Brief (horas extras indevidas, contas órfãs, e a governança/rastreabilidade que sustenta os dois). Geotemporal e Notificação Híbrida **não são necessários** para essa entrega funcionar — a decisão já tomada de desacoplar a aprovação de Login da notificação (FR-23, ver nota da feature) garante isso: o fluxo de aprovação funciona por consulta direta mesmo sem nenhum motor de notificação existir.

### 13.2 Etapa 2 (P1) — segunda entrega
- Motor Geotemporal (§4.4) — CEP, calendário de dias úteis/feriados, tags operacionais.

### 13.3 Etapa 3 (P2) — terceira entrega
- Motor de Notificação Híbrida (§4.5) — despachante síncrono/assíncrono, multi-canal.

### 13.4 Fora de Escopo desta Fase 1 (nenhuma das 3 Etapas)
- CRM, Módulo Financeiro, Provisionamento de Rede, Portal do Assinante — Fase 2 do Product Brief, produto separado (ver Brief, §7 Scope).
- Automação disparada por tag operacional — deferida para a Fase 2 do Brief.
- Front-end/UI de qualquer tipo — fora do escopo deste PRD (decisão desta rodada, §0).

## 14. Métricas de Sucesso

*Herdadas diretamente da tabela de Success Criteria do Product Brief, com FRs de validação.*

**Primárias**
- **SM-1: Governança de APIs** — 100% das tentativas de chamada fora do turno, por perfis sujeitos a bloqueio, retornam `403`. Valida FR-9, FR-10.
- **SM-2: Rastreabilidade Trabalhista** — 100% dos acessos avaliados por turno (permitidos ou negados) geram log imutável de auditoria. Valida FR-11.
- **SM-3: Offboarding em Tempo Real** — invalidação síncrona de 100% dos tokens/sessões ativos de um Login/Funcionário desligado, em menos de 1 segundo, **enquanto o mecanismo de invalidação estiver disponível**; indisponibilidades seguem o comportamento fail-closed de FR-13 (nega acesso por incerteza) e são contadas à parte, não misturadas nesta métrica. Valida FR-13.

**Secundárias**
- **SM-4: Precisão Geotemporal** — zero divergência de feriado municipal reportada nos ISPs de teste. Valida FR-17. *(Métrica de ausência de reclamação — só é mensurável a partir da Etapa 2, quando FR-17 existir; até lá não há o que medir.)*
- **SM-5: Segurança de Notificação** — zero mensagens perdidas ou descartadas pelo despachante, em qualquer modo. Valida FR-19, FR-20, FR-22. *(Mesma ressalva — só mensurável a partir da Etapa 3, quando o Motor de Notificação existir.)*
- **SM-6: Cobertura de Aprovação de Acesso** — 100% dos Logins criados, reativados, ou com mudança de Perfil (troca de principal ou adição de adicional), 100% dos Logins sem Funcionário vinculado, e 100% dos Perfis criados/editados, têm registro de decisão (aprovação/rejeição) do aprovador correto (supervisor/gerente para Logins vinculados a Funcionário; grupo `APPROVE_SYSTEM_ACCESS` para Login órfão e Perfil) antes de a mudança ter efeito. **"Zero exceção" inclui as auto-aprovações por exceção de última instância (FR-23/26/27) — elas contam como decisão registrada, só que marcadas separadamente na auditoria, nunca como ausência de decisão.** Valida FR-23 a FR-27.

**Contra-métricas (não otimizar)**
- **SM-C1**: Taxa de falso-positivo de bloqueio de turno (acesso legítimo de emergência barrado indevidamente) — contrabalança SM-1; otimizar SM-1 às cegas empurra Perfis legítimos para "acesso irrestrito" só para escapar do bloqueio, esvaziando o propósito da feature.
- **SM-C2**: Volume de reenvio/retry de notificação por mensagem — contrabalança SM-5; "zero perda" não deve ser alcançado por retry agressivo que sature o canal externo (Email/SMS) ou gere spam ao destinatário.
- **SM-C3**: Taxa de escalonamento/renotificação por estouro do SLA de 1 dia útil (FR-23 a FR-27) e tempo médio/máximo real em aprovação pendente — contrabalança SM-6; garante que o gate de aprovação não vire gargalo operacional mesmo com o SLA definido.

## 15. Questões em Aberto

Nenhuma questão em aberto no momento — todas as levantadas ao longo deste PRD foram resolvidas (ver decisões em §7, §11, FR-23 e FR-28). Suposições que ainda carregam um valor de referência (não uma pendência de decisão) estão listadas no Índice de Suposições (§16).

## 16. Índice de Suposições

- §2.1 — JTBD de Fase 2 tratado como consumidor futuro, não usuário direto desta Fase 1.
- §4.1 — Completar a Fundação (Employee/Login além do esqueleto, gaps de Company) é pré-requisito bloqueante para §4.2/4.3, não pilar paralelo.
- §4.2, FR-10 — Comportamento fail-closed (bloquear por padrão sem Jornada cadastrada) assumido como postura correta de um motor Zero Trust.
- §4.2, NFR — Latência da checagem de turno assumida em p95 < 10ms.
- §4.3, FR-13 — Mecanismo concreto de invalidação de sessão (cache compartilhado vs. Admin API do Keycloak) não decidido aqui; hoje nenhuma peça existe no código.
- §4.4, FR-15 — Provedor primário assumido como ViaCEP (citado no Brief); fallback e timeout não definidos.
- §4.4, FR-16 — TTL de cache de CEP assumido em 24h como valor de referência.
- §4.1, FR-23 — Rejeição de solicitação de Login assumida como estado terminal `REJECTED` (não reaproveitável); RH deve abrir nova solicitação.
- §4.1, FR-25 — Remover Perfil adicional (reduzir acesso) assumido como não sujeito a aprovação; só concessão de acesso passa pelo gate.
- §4.4, NFR — Meta de performance de CEP assumida em p95 < 300ms sob pico.
- §4.1, FR-28 — Cancelamento automático da reativação pós-licença/férias assumido em 5 dias úteis sem decisão; valor de referência a confirmar.
- §4.5, NFR — Orçamento de latência do modo síncrono de notificação não definido, marcado para Arquitetura.

---
*Decisões técnicas de implementação (mecanismo de Kill Switch, provedor de CEP, backend de mensageria, cache) vivem em `addendum.md` — este PRD descreve capacidade, não mecanismo.*
