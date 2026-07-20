# Product Brief: SawCunhaOS (Motor Fundacional)

* **Status:** Alinhado (Pronto para PRD)
* **Empresa:** SawCunhaOS
* **Versão:** 1.0 (Fase 1)
* **Data de Criação:** 16 de julho de 2026
* **Data de Atualização:** 16 de julho de 2026

---

## 1. Executive Summary

A SawCunhaOS (Fase 1) é o núcleo de governança e comunicação projetado especificamente para provedores de internet (ISPs) de pequeno e médio porte. Em um setor dinâmico onde a conformidade com as leis trabalhistas brasileiras, a segurança lógica de dados e a agilidade logística ditam a sobrevivência do negócio, esta entrega inicial estabelece uma fundação operacional inabalável. O sistema resolve a fragmentação das operações ao amarrar o controle de acessos técnicos diretamente à jornada de trabalho ativa de cada colaborador.

Esta primeira versão não foca na captação de clientes, mas sim na blindagem interna e estrutural do provedor. Ao integrar em um só ecossistema a segurança no nível de APIs (bloqueio por turno de RH), a inteligência geotemporal de alta concorrência e um motor de notificações híbrido (síncrono e assíncrono), a SawCunhaOS neutraliza passivos trabalhistas e riscos patrimoniais. Essa arquitetura robusta servirá como sustentação direta para a acoplagem segura dos futuros módulos de CRM e faturamento na Fase 2.

---

## 2. The Problem

Provedores de internet de pequeno e médio porte sofrem com o desperdício de receita devido à falta de governança interna e a processos de comunicação desorganizados. No cenário atual, os seguintes pontos de atrito críticos minam a operação do ISP:

* **Vulnerabilidade Trabalhista e de Segurança:** A ausência de controle sobre os horários de acesso ao sistema faz com que funcionários realizem atividades operacionais fora do expediente (gerando horas extras indesejadas e passivos judiciais). Além disso, o acesso a dados de assinantes fora do turno expõe o provedor a riscos de segurança da informação.
* **Offboarding Frágil:** No momento do desligamento de um colaborador, a demora para desativar manualmente suas contas em sistemas satélites deixa credenciais ativas (contas órfãs), gerando graves riscos de vazamento ou uso malicioso de dados corporativos.
* **Inconsistência Geotemporal:** A falta de sincronização nativa com calendários municipais e locais gera agendamentos técnicos ineficientes e erros de faturamento, descumprindo os prazos regulamentares de SLA estabelecidos com o cliente final.

---

## 3. The Solution

A SawCunhaOS atua como o sistema nervoso central e de segurança do ISP. A solução automatiza e consolida as regras de negócio diretamente no back-end da aplicação:

* **Governança Ativa via API:** O sistema bloqueia requisições de API de perfis operacionais fora do horário de trabalho regular, registrando de forma imutável os acessos de equipes autorizadas de plantão.
* **Ciclo de Vida Integrado (Kill Switch):** A gestão do funcionário no RH governa seus direitos de acesso. Se o colaborador for desligado no sistema, todas as suas sessões e tokens ativos são invalidados imediatamente (em menos de um segundo).
* **Motor Geotemporal de Alta Performance:** Uma infraestrutura rápida de busca de CEP integrada a calendários automatizados nacionais, estaduais e municipais, permitindo também a aplicação de tags corporativas customizadas.
* **Motor de Notificações Híbrido:** Um despachante de mensagens desacoplado que opera de forma síncrona (para alertas emergenciais e de segurança) e assíncrona (com filas de mensageria para rotinas e avisos operacionais).

---

## 4. What Makes This Different

Diferente de ferramentas de RH tradicionais ou cadastros passivos de sistema, a SawCunhaOS adota uma postura Zero Trust. O sistema diferencia-se no mercado ao impor regras de jornada diretamente na camada de APIs e serviços do provedor, eliminando a possibilidade de burla por parte do usuário (como o uso de ferramentas de requisição direta ou manipulação de horários no front-end). A combinação entre escala de trabalho parametrizada, desligamento reativo em tempo real e inteligência geográfica de calendário transforma regras complexas em ações de back-end automatizadas e blindadas contra falhas humanas.

---

## 5. Who This Serves

* **Primário (Gestores de ISP e Analistas de RH):** Necessitam de controle total sobre a jornada da equipe, eliminação de riscos de horas extras indevidas, segurança jurídica em processos demissionais e garantia de dados de rede protegidos.
* **Secundário (Técnicos e Operadores de Suporte):** Precisam de uma interface de trabalho simplificada, alertas precisos de escalas e rotas de turno, além da garantia de que o sistema operará estritamente dentro de seu horário regular de trabalho.

---

## 6. Success Criteria

| Sinal de Sucesso (Foco de Negócio) | Como Medir (Métrica) | Meta / Limiar de Aceitação | Responsável |
| :--- | :--- | :--- | :--- |
| **Governança de APIs** | Tentativas de requisições de API fora da jornada por perfis operacionais bloqueados. | 100% das chamadas fora do turno retornam erro HTTP 403 (Forbidden). | Engenharia (Dev) |
| **Rastreabilidade Trabalhista** | Acessos extraordinários de plantão registrados em banco. | 100% dos acessos permitidos fora da jornada geram logs imutáveis de auditoria. | Engenharia (Dev) |
| **Offboarding em Tempo Real** | Propagação imediata do comando Kill Switch pós-desligamento. | Invalidação síncrona absoluta de todos os tokens de sessão ativos de usuários demitidos. | Engenharia (Dev) |
| **Precisão Geotemporal** | Divergência ou erro de agendamentos causados por feriados municipais locais. | Zero ocorrências de conflitos de feriados locais reportadas nos provedores de teste. | Garantia de Qualidade (QA) |
| **Segurança de Notificação** | Perda de requisições de mensageria processadas pelo motor híbrido. | Zero mensagens perdidas ou descartadas pelo despachante de envio. | Engenharia (Dev) |

---

## 7. Scope

| Escopo Incluso (In - Fase 1) | Escopo Excluído (Out - Fase 1) |
| :--- | :--- |
| **IAM & API Gateway:** Controle seletivo de acessos a APIs do sistema por perfil, escala de turno e logs imutáveis de auditoria para acessos extraordinários. | **Gestão de Clientes (CRM):** Sem telas de prospecção, histórico ativo de vendas, negociações ou controle direto de carteira de clientes nesta versão. |
| **Ciclo de Vida (RH):** Módulos para admissão de dados, divisão de departamentos corporativos, horários de escalas de trabalho e fluxo de desligamento com Kill Switch integrado. | **Módulo Financeiro:** Emissão e baixa automática de boletos, remessa bancária de faturamento e integração de gateway de pagamento postergados. |
| **Motor Geotemporal:** Cadastro integrado a barramentos de CEP externos, calendário de dias úteis (nacional, estadual e municipal) e parametrização de tags operacionais corporativas. | **Provisionamento de Rede:** Sem integrações ativas com servidores Radius, sistemas de gerência de OLTs ou configurações automáticas de roteadores. |
| **Mensageria Híbrida:** Infraestrutura de recepção e disparo de notificações (E-mail, SMS, App, Web) em modos síncrono e assíncrono. | **Portal do Assinante:** Sem telas de autoatendimento, faturas ou abertura de chamados do cliente final nesta entrega inicial. |

---

## 8. Vision

Em 2 a 3 anos, a SawCunhaOS evoluirá para se consolidar como a plataforma líder em governança, provisionamento e inteligência operacional para ISPs de médio porte no Brasil. Ao expandir esta fundação rigorosa de segurança, o sistema passará a comportar o CRM de alta conversão, faturamento inteligente por WhatsApp e provisionamento automatizado de rede diretamente no hardware da planta de fibra (OLTs). A plataforma se tornará sinônimo de conformidade legal e eficiência em larga escala na indústria de telecomunicações.

---

## 9. Addendum (Anexos e Decisões)

### Decisões de Escopo e Diretrizes de Negócio (Paper Trail)
* **Desacoplamento do CRM:** O CRM foi oficialmente removido do escopo de lançamento para garantir foco total nos motores de governança e mensageria (evitando sobrecarga do time e garantindo alta qualidade na base).
* **Controle Invisível no Back-end:** A validação de turnos e jornadas foi posicionada estritamente na camada de serviço (APIs), bloqueando requisições com o código de erro correto no gateway de segurança para evitar bypass pelo console do navegador.
* **Bloqueio Seletivo por Cargo:** O RH mapeará seletivamente quais perfis exigem bloqueio (atendentes, operacionais de campo) e quais possuem acesso irrestrito por justificativa legal (gerentes, administradores, suporte emergencial).
* **Tags Operacionais no Calendário:** As tags criadas nas datas (ex: "Janela de Manutenção") funcionarão apenas como tags informativas de rotulagem e filtros de relatórios na Fase 1, servindo de base para as automações inteligentes programadas para a Fase 2.

### Diretrizes de Arquitetura e Engenharia (Addendum Técnico)
* **Mecanismo Kill Switch:** O endpoint de inativação de colaborador no módulo de RH disparará um evento síncrono para o barramento de segurança, limpando o cache de sessões (ex: Redis) e forçando a invalidação imediata de JWTs (Tokens de Acesso Web).
* **Cache Geotemporal:** A base de endereços e CEPs integrará APIs públicas (ex: ViaCEP) com mecanismo de fallback estruturado (segunda opção automática), além de cache local de pesquisas para suportar picos de consulta sem degradação do banco principal.
* **Dualidade de Notificação:** O despachante de mensagens oferecerá um fluxo síncrono (requisições bloqueantes com tempo de resposta imediato, ideal para tokens de validação e alertas de falhas críticas de infraestrutura) e um fluxo assíncrono (com filas de mensageria para processamento e entrega em segundo plano, ideal para rotinas diárias e avisos corporativos em lote).
