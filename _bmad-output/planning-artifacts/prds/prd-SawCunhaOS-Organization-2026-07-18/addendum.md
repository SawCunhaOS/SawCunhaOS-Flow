# Addendum — PRD SawCunhaOS Fase 1

Conteúdo técnico e decisões de negócio complementares ao PRD (capacidade, não mecanismo), que a Arquitetura e o time precisam ter à mão.

## 1. Decisões Herdadas do Product Brief

### Escopo e Negócio

- **Desacoplamento do CRM:** removido do escopo de lançamento para garantir foco total nos motores de governança e mensageria.
- **Controle invisível no back-end:** validação de turno/jornada posicionada estritamente na camada de serviço (API), bloqueando com o código de erro correto no gateway de segurança, para evitar bypass pelo console do navegador.
- **Bloqueio seletivo por cargo:** RH mapeia seletivamente quais Perfis exigem bloqueio (atendentes, operacionais de campo) e quais têm acesso irrestrito por justificativa legal (gerentes, administradores, suporte emergencial).
- **Tags operacionais no calendário:** informativas/filtro apenas em Fase 1; base para automação inteligente em Fase 2 — decisão deliberada, não lacuna.

### Arquitetura e Engenharia

- **Kill Switch:** endpoint de inativação de colaborador no RH dispara evento síncrono para o barramento de segurança, limpando cache de sessões (ex.: Redis) e forçando invalidação imediata de JWT.
- **Cache Geotemporal:** base de endereços/CEP integra APIs públicas (ex.: ViaCEP) com fallback estruturado (segunda opção automática) e cache local para suportar picos sem degradar o banco principal.
- **Dualidade de notificação:** fluxo síncrono (requisições bloqueantes, resposta imediata — tokens/alertas críticos) e fluxo assíncrono (fila de mensageria — rotinas/avisos em lote).

## 2. Achados da Auditoria de Código (2026-07-18) — estado real vs. Brief

Cruzamento de cada pilar do Product Brief contra o código-fonte atual (`flow-organization-*`), feito via subagentes de extração + grep dirigido, para fundamentar a priorização P0/P1/P2 do PRD.

| Pilar do Brief | Estado no código | Evidência |
|---|---|---|
| Governança IAM (bloqueio de turno) | **0%** | Único `OncePerRequestFilter` é `ExceptionHandlerFilter` (tratamento de erro); `PositionWorkSchedule`/`EmployeeWorkSchedule` são entidades JPA burras, sem nenhuma classe comparando `LocalTime.now()` contra o cadastro. |
| Kill Switch | **0%** | Zero ocorrência de `kill/invalidat/revoke/blacklist` em `.java`; nenhuma dependência Redis em nenhum `pom.xml`; nenhuma integração com Keycloak Admin API (só mocks de teste). Login/Employee sem Use Case/Delegate de escrita ainda — só entidades de domínio. |
| Fundação (Company) | **Parcial** | Só `Create/Get/GetAll/Update` como Use Case; `enable/disable/block/unblock/hierarchy/branches/status-history/contacts/addresses` sem Use Case (`CompanyDelegate` javadoc admite "comportamento default"). `Company.java` já tem `activate/inactivate/disable/enable` no domínio, mas órfãos. |
| Fundação (Employee/Login) | **Esqueleto** | Só entidades de domínio + `EmployeePositionQueryServiceBean` (leitura). Sem `EmployeeServiceBean`/`LoginServiceBean`, sem Delegate. |
| Motor Geotemporal (CEP/calendário) | **0%** | Zero ocorrência de `CEP/ViaCEP/Calendar/Holiday/BusinessDay/feriado` em código-fonte. Endereço hoje é só `addressId` (referência a serviço externo). `SCOS_VALIDATION_009` (CEP inválido) existe como mensagem órfã, sem campo que a use. |
| Motor de Notificação Híbrida | **Parcial** | Outbox (Events/Topics) é genérico por arquitetura, mas único consumidor real é a Saga de sincronização de Login com o Keycloak. Zero `NotificationService`/`MessageDispatcher`/`JavaMailSender`/dependência de mensageria real (`spring-kafka`/`spring-rabbit`/`spring-boot-starter-mail`) em nenhum `pom.xml`. `KAFKA` só existe como valor de enum (`OutboxBackend`), sem producer/consumer implementado. |

## 3. Decisões Técnicas Abertas (mecanismo, não capacidade — para Arquitetura decidir)

- **Kill Switch:** cache compartilhado (Redis denylist de JWT por `jti`/`loginId`, consultado no filtro de autenticação) **vs.** revogação ativa via Keycloak Admin API **vs.** combinação das duas. Repositório já usa Redis para jDempotent — reaproveitar a mesma instância é candidato natural, mas não decidido.
- **CEP:** ViaCEP como provedor primário (citado no Brief) + qual provedor de fallback (ex. BrasilAPI, OpenCEP) — não escolhido. TTL de cache local — valor de referência 24h usado no PRD como suposição, a validar.
- **Calendário de feriados:** fonte de dados nacional/estadual/municipal — biblioteca própria vs. serviço externo vs. base própria mantida manualmente. Não decidido.
- **Notificação:** backend de fila para o modo assíncrono — reaproveitar `OutboxBackend` (`PGMQ`/`KAFKA`/`DIRECT_API`, hoje só enum) implica escolher e implementar um producer/consumer real; provedores de Email/SMS (SendGrid, Twilio, SES, etc.) não escolhidos.
- **Checagem de turno:** implementação como `Filter`/`Interceptor`/aspecto AOP — ponto de corte técnico não decidido; precisa coexistir com a `SecurityFilterChain` existente (`ScosHttpSecurityConfiguration`) sem duplicar responsabilidade do filtro de autenticação Keycloak/JWT já em uso.

## 4. Referências de Código (para quem for implementar)

*Caminhos relativos à raiz do monorepo — módulos hoje vivem em `organization/flow-organization-*` (reestruturação de `scos-organization-*`/módulos separados para monólito modular, concluída nesta rodada — achados de auditoria re-verificados e confirmados sob a estrutura nova).*

- `flow-organization-domain/.../corporate/company/internal/Company.java` — métodos de ciclo de vida já existentes, órfãos de Use Case.
- `flow-organization-usecase/.../corporate/company/` — Use Cases hoje implementados (Create/Get/GetAll/Update).
- `flow-organization-domain/.../corporate/position/internal/PositionWorkSchedule.java` e `.../employee/internal/EmployeeWorkSchedule.java` — cadastro de jornada, sem enforcement.
- `flow-security-starter/.../keycloak/WebSecurityConfig.java`, `ScosHttpSecurityConfiguration.java` — cadeia de filtros de segurança atual, ponto de extensão candidato para a checagem de turno.
- `flow-organization-domain/.../outbox/internal/OutboxBackend.java` — enum de backend do Outbox, hoje sem implementação real de Kafka/PGMQ.
- `etc/doc/usecase/00-indice-central.md` a `06-configuracao-outbox.md` — especificação funcional detalhada da Fundação (§4.1 do PRD).
