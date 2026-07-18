# Diagramas de Arquitetura — Etapa 1 (P0)

Companion visual de `_bmad-output/planning-artifacts/architecture/architecture-SawCunhaOS-Organization-2026-07-18/ARCHITECTURE-SPINE.md`. Cada diagrama tem um parágrafo de contexto em linguagem simples antes do Mermaid técnico — serve tanto para quem vai codar quanto para quem só precisa entender o desenho geral.

Escopo: só a Etapa 1 (Fundação, Governança de Turno, Kill Switch). Motor Geotemporal e Notificação Híbrida (Etapas 2 e 3) não aparecem aqui — ver `Deferred` na spine.

## 1. Contexto — quem interage com o sistema

RH cadastra e desliga gente; o Técnico é quem sofre o bloqueio de turno; Supervisor/Gerente/Grupo de TI aprovam acesso; Keycloak e Redis são os dois sistemas externos que a Etapa 1 já usa hoje, sem infraestrutura nova.

```mermaid
graph TB
  RH["RH / Administrador"]
  Tecnico["Técnico / Operador"]
  Aprovador["Supervisor · Gerente · Grupo TI"]

  subgraph SawCunhaOS["SawCunhaOS — Motor Fundacional (Etapa 1)"]
    API["API REST — flow-organization-boot"]
  end

  Keycloak[("Keycloak — Identity Provider")]
  Redis[("Redis — denylist Kill Switch")]
  Postgres[("PostgreSQL — dados de identidade e aprovação")]

  RH -->|"cadastra Empresa/Depto/Cargo/Funcionário, desliga, aprova"| API
  Tecnico -->|"chamadas de API dentro/fora do turno"| API
  Aprovador -->|"aprova ou rejeita solicitação de Login/Perfil"| API

  API -->|"valida/emite token"| Keycloak
  API -->|"consulta/grava denylist"| Redis
  API -->|"lê/grava"| Postgres
  API -.->|"Kill Switch: invalida sessão"| Keycloak
```

## 2. Módulos e dependências

A regra de dependência é uma via de mão única: `api` nunca enxerga `domain` diretamente — quem traduz é o `usecase`. `flow-security-starter` é biblioteca reusável entre projetos SCOS; por isso a lógica de bloqueio de turno (específica deste produto) mora em `flow-organization-infrastructure`, não dentro do starter (AD-3).

```mermaid
graph LR
  boot["flow-organization-boot\n(composition root REST)"] --> api
  boot --> usecase
  boot --> domain
  boot --> infrastructure
  api["flow-organization-api\n(Delegates)"] --> usecase["flow-organization-usecase\n(orquestração)"]
  usecase --> domain["flow-organization-domain\n(entidades, regra)"]
  usecase --> infrastructure["flow-organization-infrastructure\n(permissões, Filter de turno)"]
  domain --> shared["flow-organization-shared\n(ExceptionCodeError)"]
  infrastructure -.->|"registra Filter no ponto de extensão"| security["flow-security-starter\n(biblioteca reusável — SEM lógica de turno)"]
  api -.->|"exclusão intencional — nunca"| domain

  style security fill:#00000000,stroke-dasharray: 4 3
```

## 3. Modelo de dados — Etapa 1

Foco nas entidades e relações que a Etapa 1 usa ou adiciona. `LoginApprovalRequest` é a peça nova central (AD-4); `Department.managerId` fecha o nível 2 da cadeia de aprovação (AD-5); `Employee.supervisor` já existia. Atributo que é ele mesmo uma decisão de arquitetura (ex. `escalationPolicy`) tem explicação na spine, não aqui — este diagrama só mostra nomes e relações.

```mermaid
erDiagram
  COMPANY ||--o{ COMPANY : "matriz / filial (parentCompanyId)"
  DEPARTMENT }o--|| EMPLOYEE : "managerId (novo — AD-5)"
  DEPARTMENT ||--o{ POSITION : agrupa
  POSITION ||--o{ EMPLOYEE : ocupa
  EMPLOYEE }o--|| EMPLOYEE : "supervisorId (já existente)"
  EMPLOYEE ||--o{ LOGIN : possui
  LOGIN }o--|| PROFILE : "perfil principal"
  LOGIN ||--o{ LOGIN_PROFILE : "perfis adicionais"
  PROFILE ||--o{ PROFILE_RESOURCE : agrega
  PROFILE_RESOURCE }o--|| RESOURCE : referencia
  LOGIN ||--o{ LOGIN_STATUS_HISTORY : historico
  LOGIN ||--o{ LOGIN_APPROVAL_REQUEST : "0..N — novo (AD-4)"
  LOGIN ||--o{ SHIFT_ENFORCEMENT_LOG : "0..N — novo (FR-11)"
  EMPLOYEE ||--o{ SHIFT_ENFORCEMENT_LOG : "0..N (nullable — Login EXTERNAL/SERVICE não tem)"

  LOGIN_APPROVAL_REQUEST {
    bigint login_id FK
    bigint requested_by_login_id "nullable — null = sistema (FR-28)"
    bigint decided_by_login_id "nullable ate decisao"
    varchar current_level "SUPERVISOR MANAGER SYSTEM_ACCESS_GROUP"
    varchar escalation_policy "INDEFINITE AUTO_CANCEL"
    varchar status "PENDING APPROVED REJECTED CANCELLED"
    boolean is_exception_self_approval
    timestamptz sla_deadline
  }

  SHIFT_ENFORCEMENT_LOG {
    bigint login_id FK
    bigint employee_id FK "nullable"
    varchar endpoint
    varchar decision "ALLOWED DENIED"
    boolean within_schedule
    varchar allowed_via "REGULAR_SCHEDULE ON_CALL_WINDOW UNRESTRICTED_PROFILE — null se DENIED"
    timestamptz request_timestamp
  }
```

## 4. Fluxo — criação e aprovação de Login

Ninguém precisa ser avisado pra o fluxo funcionar: o aprovador pode consultar a lista de pendências a qualquer momento (decisão que resolveu a inversão de prioridade entre FR-23 e o Motor de Notificação, P2). A notificação, quando existir, é reforço — não pré-requisito.

```mermaid
sequenceDiagram
  actor RH
  participant API as flow-organization-boot
  participant LAR as LoginApprovalRequest
  actor Supervisor

  RH->>API: POST /v1/employees/{id}/logins
  API->>LAR: cria (current_level=SUPERVISOR, status=PENDING)
  API->>API: grava SCOS_LOGIN_STATUS_HISTORY (status=PENDING_APPROVAL)
  Note over API: trigger existente trg_sync_login_status<br/>sincroniza Login.status automaticamente
  API-->>RH: 202 (Login criado, PENDING_APPROVAL)

  Note over Supervisor,API: Sem notificação, Supervisor consulta pendências quando quiser
  Supervisor->>API: GET /v1/login-approval-requests?aprovador=eu
  API-->>Supervisor: lista com esta solicitação

  Supervisor->>API: POST /v1/login-approval-requests/{id}/approve
  API->>API: valida: Supervisor != solicitante (segregação de função)
  API->>API: valida: Funcionário ainda ativo (reverificação no momento da decisão)
  API->>LAR: status=APPROVED, decided_by, decided_at
  API->>API: grava SCOS_LOGIN_STATUS_HISTORY (status=ACTIVE)
  Note over API: trigger sincroniza Login.status=ACTIVE
  API-->>Supervisor: 200 (aprovado)
```

## 5. Fluxo — bloqueio de turno (Zero Trust)

O filtro novo entra depois de saber quem é o usuário (JWT já validado) e antes de qualquer efeito de negócio — inclusive antes do filtro de idempotência, pra uma chamada barrada nunca consumir uma chave de idempotência à toa (AD-2).

```mermaid
sequenceDiagram
  actor Tecnico
  participant JWT as Filtro JWT (existente)
  participant Turno as ShiftEnforcementFilter (novo — AD-2/AD-3)
  participant Idem as Filtro jDempotent (existente)
  participant Delegate

  Tecnico->>JWT: requisição + Bearer token
  JWT->>JWT: valida assinatura, resolve Login
  alt token inválido
    JWT-->>Tecnico: 401
  else token válido
    JWT->>Turno: segue a chain
    Turno->>Turno: Perfil exige bloqueio de turno?
    alt Perfil isento (acesso irrestrito) ou dentro da Jornada/Plantão
      Turno->>Idem: segue a chain
      Idem->>Delegate: segue a chain
      Delegate-->>Tecnico: 200 (resposta normal)
    else fora da Jornada e sem plantão
      Turno-->>Tecnico: 403 Forbidden (nunca chega no jDempotent)
      Turno->>Turno: grava log de auditoria imutável (FR-11)
    end
  end
```

## 6. Fluxo — Kill Switch

O commit no banco nunca espera o Redis. Se a escrita no denylist falhar, o Use Case aciona retry/alerta — e enquanto não propagar, qualquer checagem que não confirmar o estado nega por padrão (fail-closed), nunca libera por incerteza.

```mermaid
sequenceDiagram
  actor RH
  participant API as flow-organization-boot
  participant DB as PostgreSQL
  participant Redis
  participant Keycloak

  RH->>API: PUT /v1/employees/{id}/block
  API->>DB: UPDATE status=DISABLED + INSERT status_history
  DB-->>API: commit confirmado
  par Escrita síncrona no denylist
    API->>Redis: SET denylist:{jti|loginId} TTL=validade restante
    Redis-->>API: OK
  and Chamada em paralelo (defesa em profundidade)
    API->>Keycloak: revoke session (Admin API)
  end
  alt Redis falhou
    API->>API: retry automático + alerta obrigatório
  end
  API-->>RH: 200 (desligamento confirmado)

  Note over Tecnico: próxima requisição do ex-funcionário
  actor Tecnico as Ex-funcionário
  Tecnico->>API: requisição com token antigo
  API->>Redis: consulta denylist
  alt Redis respondeu e token está na lista
    API-->>Tecnico: 401 (revogado)
  else Redis inacessível (não conseguiu confirmar)
    API-->>Tecnico: 401 (fail-closed — nega por incerteza)
  end
```

## 7. Máquina de estados — LoginApprovalRequest

`Cancelado` só existe na política `AUTO_CANCEL` (retorno automático de licença/férias, FR-28) — o escalonamento indefinido (FR-23/24/25/26/27) nunca cancela sozinho, só renotifica.

```mermaid
stateDiagram-v2
  note right of [*]: Estados = valores literais de CURRENT_LEVEL/STATUS no schema (inglês) — não traduzir no código
  [*] --> SUPERVISOR: request created — nível inicial resolvido no ato
  SUPERVISOR --> MANAGER: SLA estourado ou supervisor ausente/conflito
  MANAGER --> SYSTEM_ACCESS_GROUP: SLA estourado ou gerente ausente/conflito
  SYSTEM_ACCESS_GROUP --> SYSTEM_ACCESS_GROUP: SLA estourado, policy=INDEFINITE (renotifica)

  SUPERVISOR --> APPROVED
  MANAGER --> APPROVED
  SYSTEM_ACCESS_GROUP --> APPROVED
  SUPERVISOR --> REJECTED
  MANAGER --> REJECTED
  SYSTEM_ACCESS_GROUP --> REJECTED
  SUPERVISOR --> CANCELLED: SLA estourado, policy=AUTO_CANCEL (só FR-28)
  MANAGER --> CANCELLED: SLA estourado, policy=AUTO_CANCEL (só FR-28)

  APPROVED --> [*]
  REJECTED --> [*]
  CANCELLED --> [*]
```

## 8. Deploy e infraestrutura

Nada disto é novo — Redis, Keycloak e PostgreSQL já estão provisionados e em uso por outras partes do sistema hoje. A Etapa 1 reaproveita, não adiciona.

```mermaid
graph TB
  subgraph Runtime["Ambiente de execução"]
    boot["flow-organization-boot\n(API REST — porta HTTP)"]
    grpcboot["flow-organization-grpc-boot\n(servidor gRPC — fora do escopo desta Etapa)"]
  end

  Keycloak[("Keycloak\nOAuth2/JWT + Admin API")]
  Redis[("Redis\ncache jDempotent + denylist Kill Switch")]
  Postgres[("PostgreSQL 18+\nschema scos")]

  boot --> Postgres
  boot --> Redis
  boot --> Keycloak
  grpcboot -.->|fora do escopo| Postgres

  style grpcboot fill:#00000000,stroke-dasharray: 4 3
```

---

*Fonte de verdade das decisões: `ARCHITECTURE-SPINE.md` (AD-1 a AD-6). Este arquivo é ilustração — se um diagrama e a spine discordarem, a spine vence.*
