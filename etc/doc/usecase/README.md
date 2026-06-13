# SCOS — SawCunhaOS Organization System

> Sistema central de gestão organizacional, identidade e controle de acesso.  
> Construído com **Java Spring**, **Keycloak** e princípios de **Clean Architecture**, **Hexagonal Architecture** e **DDD**.

---

## Sumário

1. [Visão Geral](#visão-geral)
2. [Conceitos Fundamentais](#conceitos-fundamentais)
3. [Arquitetura](#arquitetura)
4. [scos-security-starter](#scos-security-starter)
5. [Fluxo de Autenticação](#fluxo-de-autenticação)
6. [APIs — Referência e Use Cases](#apis--referência-e-use-cases)
   - [Company API](#company-api)
   - [Department API](#department-api)
   - [Position API](#position-api)
   - [Employee API](#employee-api)
   - [Login API](#login-api)
   - [Profile API](#profile-api)
   - [Resource API](#resource-api)
   - [Configuration API](#configuration-api)
7. [Integração com Sistemas Externos](#integração-com-sistemas-externos)
8. [Padrões de Projeto Aplicados](#padrões-de-projeto-aplicados)

---

## Visão Geral

O SCOS é a **espinha dorsal de identidade e permissões** de uma organização. Ele foi criado para responder a três perguntas fundamentais que qualquer sistema precisa saber sobre um usuário:

```
1. Quem é você?         → autenticação via Keycloak
2. Onde você trabalha?  → empresa e filial no banco SCOS
3. O que pode fazer?    → perfil e permissões no banco SCOS
```

O sistema **não concorre** com os sistemas de negócio da empresa — ele os alimenta. Um sistema de estoque, de streaming, de RH ou qualquer outra aplicação chama o SCOS para validar o token e receber o contexto completo do usuário, sem precisar gerenciar identidade por conta própria.

### O que o SCOS gerencia

- A estrutura organizacional completa: empresa matriz, filiais, departamentos, cargos e funcionários
- Os usuários de acesso: funcionários, parceiros externos e contas de serviço
- Os perfis de permissão e os recursos que cada perfil pode acessar
- A integração com o Keycloak para criação e atualização de usuários
- As configurações gerais do sistema

### O que o SCOS não faz

- Não gerencia senhas — responsabilidade do Keycloak
- Não armazena dados de negócio de outros sistemas
- Não define o que cada permissão significa dentro de cada sistema externo

---

## Conceitos Fundamentais

### Empresa Matriz e Filiais

A `SCOS_COMPANY` representa tanto a empresa matriz quanto suas filiais por meio de auto-referência. O campo `PARENT_COMPANY_ID` nulo indica a matriz; preenchido, indica uma filial vinculada.

```
Empresa Matriz (PARENT_COMPANY_ID = NULL)
  ├── Filial SP
  ├── Filial RJ
  └── Filial MG
```

Cada funcionário está vinculado a exatamente uma empresa ou filial. O contexto da empresa é propagado automaticamente no token do Keycloak via atributos customizados, dispensando consultas adicionais nos sistemas externos.

### Perfil e Permissões

Um **perfil** é um pacote nomeado de permissões — ex: `ADMIN`, `OPERATOR`, `VIEWER`. Em vez de configurar permissões individualmente para cada usuário, o administrador monta os perfis uma vez e atribui ao usuário.

As **permissões** (resources) são definidas pelos próprios sistemas externos. Cada sistema se cadastra no SCOS via gRPC e registra as permissões que possui — ex: `PRODUCT_READ`, `ORDER_APPROVE`, `VIDEO_WATCH`. O SCOS não precisa entender o que cada permissão significa; apenas garante que o usuário certo tenha as permissões certas.

```
Sistema de Estoque registra:   PRODUCT_READ, PRODUCT_CREATE, ORDER_APPROVE
Sistema de Streaming registra: VIDEO_WATCH, VIDEO_UPLOAD, PLAYLIST_MANAGE

Perfil "Operador" recebe:      PRODUCT_READ, PRODUCT_CREATE, VIDEO_WATCH
```

### Tipos de Login

O SCOS suporta três tipos de usuário de acesso:

| Tipo | Descrição | Vínculo com Funcionário |
|---|---|---|
| `EMPLOYEE` | Funcionário da organização | Obrigatório |
| `EXTERNAL` | Parceiro ou cliente externo | Não possui |
| `SERVICE` | Conta técnica sistema-para-sistema | Não possui |

### Token e Contexto

O Keycloak emite o token após autenticação. O token carrega atributos customizados injetados pelo SCOS no momento da criação do usuário:

```json
{
  "sub": "uuid-keycloak",
  "name": "João Silva",
  "email": "joao@empresa.com",
  "company_id": "123",
  "company_name": "Empresa X",
  "branch_id": "456",
  "branch_name": "Filial SP",
  "employee_id": "789"
}
```

Com esses dados no token, os sistemas externos recebem o contexto completo do usuário sem fazer consultas adicionais ao SCOS.

---

## Arquitetura

O SCOS é composto por dois serviços independentes que compartilham o mesmo banco de dados:

```
┌──────────────────────────────────────────────────────────────┐
│                      SCOS Organization                        │
│                                                              │
│  ┌──────────────────────────┐  ┌───────────────────────────┐ │
│  │   scos-organization      │  │      scos-registry        │ │
│  │   REST API               │  │      gRPC API             │ │
│  │                          │  │                           │ │
│  │  Gestão da organização   │  │  Registro e permissões    │ │
│  │                          │  │                           │ │
│  │  • Empresa               │  │  • Cadastra sistemas      │ │
│  │  • Funcionário           │  │  • Registra recursos      │ │
│  │  • Login                 │  │  • Valida token           │ │
│  │  • Perfil                │  │  • Retorna contexto       │ │
│  │  • Configuração          │  │                           │ │
│  └────────────┬─────────────┘  └─────────────┬─────────────┘ │
│               │                              │               │
│               └──────────────┬───────────────┘               │
│                              ▼                               │
│                   ┌──────────────────┐                       │
│                   │   PostgreSQL     │                       │
│                   └──────────────────┘                       │
└──────────────────────────────────────────────────────────────┘
                              │
                   ┌──────────┘
                   ▼
         ┌──────────────────┐
         │    Keycloak      │
         │  Autenticação    │
         └──────────────────┘
```

### scos-organization

Responsável por toda a gestão da organização. Expõe a API REST documentada neste repositório. Também executa as migrations do banco via Liquibase — deve sempre subir antes do `scos-registry`.

### scos-registry

Responsável pelo registro de sistemas externos, seus recursos e pela central de permissões. Expõe uma API gRPC consumida pelos sistemas externos. É a peça que responde: *"esse usuário tem acesso a esse recurso?"*. Não executa migrations — sobe assumindo que o banco já está atualizado pelo `scos-organization`.

### Estrutura de Módulos Maven

```
scos-organization-parent              ← parent pom
├── scos-organization-domain          ← entidades, value objects, interfaces (sem frameworks)
├── scos-organization-application     ← casos de uso e ports
├── scos-organization-infrastructure  ← JPA, Keycloak, mensageria
├── scos-organization-api             ← adaptador REST  (scos-organization)
├── scos-organization-grpc            ← adaptador gRPC  (scos-registry)
├── scos-organization-boot            ← scos-organization + Liquibase
├── scos-registry-boot                ← scos-registry
└── scos-security-starter             ← biblioteca de segurança para projetos externos
```

---

## scos-security-starter

O `scos-security-starter` é a **biblioteca de segurança do ecossistema SCOS**. Todo projeto externo que precise autenticar usuários e validar permissões importa esse starter — sem precisar escrever nenhuma linha de código de segurança.

### Por que existe

O SCOS utiliza Keycloak para autenticação e o `scos-registry` via gRPC para validação de permissões. Esse conhecimento é específico do ecossistema SCOS e não pertence a um projeto externo nem a uma biblioteca genérica como o Foundation.

```
SawCunhaOS-Foundation           scos-security-starter
──────────────────────          ──────────────────────────────────
Paginação                       Configuração Spring Security
Tratamento de erros             Validação JWT do Keycloak
Utils genéricos                 Chamada gRPC ao scos-registry
Base configs Spring             Interceptador de token
                                Context do usuário autenticado
                                Anotações de autorização
```

> O Foundation é agnóstico — não sabe como o projeto autentica.  
> O starter é específico — sabe tudo sobre autenticação e permissões no SCOS.

---

### Estrutura interna do starter

```
scos-security-starter
│
├── autoconfigure
│     └── ScosSecurityAutoConfiguration
│           Configura o Spring Security automaticamente
│           quando o starter é importado no projeto
│
├── jwt
│     ├── KeycloakTokenValidator
│     │     Valida assinatura, expiração e formato do token JWT
│     └── KeycloakClaimsExtractor
│           Extrai claims do token: usuário, empresa, filial
│
├── grpc
│     ├── ScosRegistryClient
│     │     Client gRPC que chama o scos-registry
│     │     Envia o token e recebe o contexto de permissões
│     └── ScosTokenInterceptor
│           Intercepta cada requisição, lê o token do header
│           Authorization e popula o ScosSecurityContext
│
├── permission
│     ├── ScosPermissionEvaluator
│     │     Verifica se o usuário autenticado possui
│     │     determinada permissão no contexto atual
│     └── @RequiresPermission("PRODUCT_READ")
│           Anotação para proteger métodos ou endpoints
│
└── context
      └── ScosSecurityContext
            Objeto disponível em qualquer ponto do projeto
            com os dados do usuário autenticado:
            loginId, name, companyId, branchId, permissions[]
```

---

### O que o ScosSecurityContext carrega

```java
ScosSecurityContext {
    Long    loginId
    String  login
    String  name
    String  nameTreatment
    String  type              // EMPLOYEE | EXTERNAL | SERVICE
    String  status            // ACTIVE | BLOCKED
    Long    companyId
    String  companyName
    Long    branchId
    String  branchName
    Long    employeeId
    String  profileCode
    List<String> permissions  // ["PRODUCT_READ", "ORDER_APPROVE"]
}
```

---

### Como um projeto externo usa

**1. Importar o starter**

```xml
<dependency>
    <groupId>com.sawcunha.scos</groupId>
    <artifactId>scos-security-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

**2. Configurar o endpoint do scos-registry**

```yaml
scos:
  registry:
    host: scos-registry.internal
    port: 9090
    system-id: ${SCOS_SYSTEM_ID}
    secret-key: ${SCOS_SECRET_KEY}
```

**3. Proteger endpoints com anotação**

```java
@GetMapping("/products")
@RequiresPermission("PRODUCT_READ")
public List<Product> listProducts() {
    // só chega aqui se o usuário tem PRODUCT_READ
}
```

**4. Acessar o contexto do usuário**

```java
@GetMapping("/products")
@RequiresPermission("PRODUCT_READ")
public List<Product> listProducts(ScosSecurityContext context) {
    // filtra os produtos pela filial do usuário autenticado
    return productService.findByBranch(context.getBranchId());
}
```

---

### Versionamento

O starter e o scos-registry são **versionados juntos**. Quando o contrato gRPC muda, apenas o starter precisa ser republicado — os projetos externos atualizam somente essa dependência.

```
scos-security-starter:1.0.0  →  compatível com  →  scos-registry:1.0.0
scos-security-starter:1.1.0  →  compatível com  →  scos-registry:1.1.0
```

---

### Relação com o Foundation

O Foundation continua existindo como biblioteca de utilitários genéricos. O starter pode importar o Foundation, mas o Foundation não sabe nada sobre o starter.

```
scos-security-starter  →  importa  →  foundation-core (utils, paginação, erros)
Foundation             →  não conhece  →  scos-security-starter
```

---

## Fluxo de Autenticação

### 1. Criação de usuário

```
Admin cria login no scos-organization (POST /v1/logins)
    │
    ▼
SCOS salva login com status PENDING
    │
    ▼
Saga Keycloak inicia
    │
    ├── Enfileira operação (SCOS_INTEGRATION_KEYCLOAK: PENDING)
    ├── Processa e chama API do Keycloak
    ├── Registra resultado (SCOS_INTEGRATION_KEYCLOAK_LOG)
    └── Finaliza — salva KEYCLOAK_ID no login (SUCCESS)
            ou reprocessa até MAX_RETRIES (ERROR)
```

### 2. Login do usuário

```
Usuário informa login e senha
    │
    ▼
Keycloak valida credenciais e emite token JWT
    │
    ▼
Token chega no sistema externo (ex: Estoque)
    │
    ▼
Sistema externo chama scos-registry via gRPC com o token no header
    │
    ▼
scos-registry valida token com Keycloak
    │
    ▼
scos-registry busca permissões no banco
    │
    ▼
Retorna: dados do usuário + empresa + filial + permissões
    │
    ▼
Sistema externo aplica o contexto
```

### 3. Atualização de dados da empresa/filial

```
Admin atualiza empresa ou transfere funcionário no scos-organization
    │
    ▼
scos-organization dispara Saga Keycloak com TYPE=UPDATE
    │
    ▼
Atributos do usuário atualizados no Keycloak
    │
    ▼
Próximo token já carrega os dados novos
```

---

## APIs — Referência e Use Cases

---

### Company API

**Ideia:** gerenciar a estrutura societária da organização — empresa matriz, filiais e seus dados de contato e endereço.

**Base path:** `GET|POST /v1/companies`

#### Use Cases

**UC-001 — Cadastrar empresa matriz**
> O administrador do SCOS registra a empresa principal. É o ponto de partida de toda a estrutura organizacional. Sem uma empresa cadastrada, não é possível vincular departamentos, cargos ou funcionários.

**UC-002 — Cadastrar filial**
> O administrador registra uma filial vinculada à matriz via `parentCompanyId`. Uma filial segue as mesmas regras da matriz mas possui CNPJ, endereço e contatos próprios.

**UC-003 — Consultar hierarquia de empresas**
> O sistema ou o administrador precisa visualizar a estrutura completa — matriz e todas as filiais vinculadas — em formato de árvore. Usado para seleção de empresa/filial em formulários ou relatórios organizacionais.

**UC-004 — Ativar e inativar empresa**
> Quando uma filial é encerrada ou temporariamente suspensa, o administrador a inativa. Funcionários de uma empresa inativa não conseguem fazer login enquanto o vínculo não for atualizado.

**UC-005 — Gerenciar contatos da empresa**
> Cada empresa pode ter múltiplos contatos — comercial, financeiro, suporte. São usados para comunicação interna e não impactam o acesso ao sistema.

**UC-006 — Gerenciar endereços da empresa**
> Os endereços são referenciados de um sistema externo de endereços via `addressId`. O SCOS guarda apenas o vínculo e as coordenadas geográficas para uso em contextos que necessitem de localização.

---

### Department API

**Ideia:** organizar a empresa em áreas funcionais que agrupam os cargos.

**Base path:** `GET|POST /v1/departments`

#### Use Cases

**UC-010 — Cadastrar departamento**
> O administrador cria os departamentos da organização — ex: Tecnologia, Financeiro, Recursos Humanos. Um departamento é necessário antes de criar qualquer cargo.

**UC-011 — Ativar e inativar departamento**
> Quando um departamento é reestruturado ou extinto, o administrador o inativa. Cargos de um departamento inativo não podem ser atribuídos a novos funcionários.

**UC-012 — Listar departamentos para seleção**
> Interfaces administrativas exibem a lista de departamentos ativos para que o administrador selecione ao cadastrar um cargo ou filtrar funcionários por área.

---

### Position API

**Ideia:** definir os cargos existentes dentro de cada departamento.

**Base path:** `GET|POST /v1/positions`

#### Use Cases

**UC-020 — Cadastrar cargo**
> O administrador cria os cargos da organização vinculados a um departamento — ex: Analista Sênior (TI), Gerente Financeiro. Um cargo é necessário antes de registrar um funcionário.

**UC-021 — Ativar e inativar cargo**
> Quando um cargo é extinto ou renomeado, o administrador o inativa e cria um novo. O cargo inativo é mantido no histórico dos funcionários que o ocuparam.

**UC-022 — Listar cargos por departamento**
> Ao cadastrar um funcionário, a interface filtra os cargos disponíveis pelo departamento selecionado, reduzindo erros de vínculo.

---

### Employee API

**Ideia:** gerenciar os funcionários da organização com seus dados pessoais, hierarquia, contatos e endereços.

**Base path:** `GET|POST /v1/employees`

#### Use Cases

**UC-030 — Registrar funcionário**
> O RH cadastra um novo funcionário informando cargo, empresa/filial e supervisor direto. O CPF e e-mail são únicos no sistema. O supervisor é opcional — funcionários no topo da hierarquia não possuem.

**UC-031 — Transferir funcionário**
> O RH move um funcionário para outra empresa, filial ou cargo. A transferência é feita via `PATCH /v1/employees/{id}/transfer` informando apenas os campos que mudaram. O histórico anterior é preservado no banco.

**UC-032 — Consultar hierarquia do funcionário**
> O sistema ou o gestor precisa visualizar a cadeia completa de supervisores de um funcionário — usado em aprovações de fluxo ou relatórios de estrutura organizacional.

**UC-033 — Ativar e inativar funcionário**
> Quando um funcionário é desligado ou afastado, o administrador o inativa. O login vinculado a um funcionário inativo é automaticamente bloqueado.

**UC-034 — Gerenciar contatos do funcionário**
> O RH mantém os telefones de contato do funcionário para comunicações internas. Um funcionário pode ter múltiplos contatos de tipos diferentes — celular, trabalho, residencial.

**UC-035 — Gerenciar endereços do funcionário**
> O RH vincula endereços do funcionário referenciados de um sistema externo. Útil para registros de localização, benefícios por região ou entrega de documentos.

---

### Login API

**Ideia:** gerenciar os usuários de acesso ao sistema. A senha é controlada pelo Keycloak — o SCOS controla apenas o vínculo, o tipo e o perfil de cada login.

**Base path:** `GET|POST /v1/logins` e `GET|POST /v1/employees/{id}/logins`

#### Use Cases

**UC-040 — Criar login para funcionário**
> O administrador cria um acesso para um funcionário já cadastrado. O sistema valida o vínculo, cria o registro com status `PENDING` e inicia a Saga de integração com o Keycloak para criar o usuário na plataforma de autenticação.

**UC-041 — Criar login externo ou de serviço**
> Um parceiro externo ou conta técnica recebe acesso ao sistema via `POST /v1/logins` sem precisar de vínculo com funcionário. O tipo (`EXTERNAL` ou `SERVICE`) é informado no corpo da requisição.

**UC-042 — Consultar contexto do usuário autenticado**
> Via `GET /v1/logins/me`, o sistema retorna todos os dados do usuário autenticado — nome, empresa, filial, cargo, perfil e lista de permissões. Usado na inicialização de interfaces para montar o menu e controlar o que o usuário vê.

**UC-043 — Bloquear e desbloquear login**
> O administrador bloqueia o acesso de um usuário sem precisar removê-lo — útil em situações de segurança, afastamento temporário ou revisão de permissões. O desbloqueio restaura o acesso imediatamente.

**UC-044 — Atribuir perfil ao login**
> Quando as responsabilidades de um usuário mudam, o administrador atualiza o perfil via `PUT /v1/logins/{id}/profile/{profileId}`. As novas permissões entram em vigor no próximo acesso do usuário.

**UC-045 — Buscar login pelo ID do Keycloak**
> Após a Saga de integração concluir, o sistema sincroniza o `KEYCLOAK_ID` retornado pelo Keycloak com o registro interno do SCOS. Também usado para reconciliar registros em caso de reprocessamento.

---

### Profile API

**Ideia:** gerenciar os perfis de acesso que agrupam permissões — evita configurar permissões individualmente para cada usuário.

**Base path:** `GET|POST /v1/profiles`

#### Use Cases

**UC-050 — Criar perfil de acesso**
> O administrador cria perfis com nomes claros e objetivos — ex: `ADMIN`, `OPERATOR`, `AUDITOR`, `VIEWER`. Um perfil sem recursos atribuídos não concede nenhum acesso.

**UC-051 — Atribuir recursos ao perfil**
> Via `PUT /v1/profiles/{id}/resources`, o administrador define quais permissões o perfil possui — ex: o perfil `OPERATOR` recebe `PRODUCT_READ`, `PRODUCT_CREATE` e `ORDER_READ`. A lista completa é substituída a cada atualização.

**UC-052 — Consultar permissões de um perfil**
> Via `GET /v1/profiles/{id}/resources`, o administrador visualiza todas as permissões atribuídas a um perfil para auditoria ou planejamento de acesso.

**UC-053 — Ativar e inativar perfil**
> Quando um perfil não deve mais ser atribuído a novos usuários, o administrador o inativa. Usuários que já possuem o perfil não são afetados imediatamente — a inativação impede apenas novas atribuições.

---

### Resource API

**Ideia:** catálogo das permissões disponíveis nos sistemas externos cadastrados. Cada sistema define seus próprios recursos ao se registrar no `scos-registry` via gRPC.

**Base path:** `GET /v1/resources`

#### Use Cases

**UC-060 — Listar recursos disponíveis**
> O administrador consulta todos os recursos cadastrados para montar ou auditar perfis. Pode filtrar por `systemId` para ver apenas os recursos de um sistema específico — ex: todos os recursos do Sistema de Estoque.

**UC-061 — Selecionar recursos ao configurar perfil**
> Ao editar um perfil via `PUT /v1/profiles/{id}/resources`, a interface lista os recursos disponíveis via `GET /v1/resources` para que o administrador escolha quais atribuir. Os `resourceId` selecionados são enviados no corpo da requisição.

> **Nota:** o cadastro de novos recursos é feito pelos sistemas externos via `scos-registry` (gRPC) — não pela API REST. A Resource API do `scos-organization` é somente leitura.

---

### Configuration API

**Ideia:** gerenciar parâmetros de comportamento do sistema SCOS sem necessidade de reimplantação.

**Base path:** `GET|PUT /v1/configurations`

#### Use Cases

**UC-070 — Consultar configuração do sistema**
> O administrador ou o próprio sistema consulta uma configuração específica pelo identificador — ex: `TOKEN_EXPIRY_MINUTES`, `MAX_LOGIN_ATTEMPTS`, `KEYCLOAK_REALM`.

**UC-071 — Atualizar configuração em tempo real**
> O administrador altera o valor de uma configuração sem precisar reiniciar o sistema. As configurações são interpretadas conforme seu tipo declarado: `STRING`, `INTEGER`, `BOOLEAN` ou `JSON`.

**UC-072 — Listar todas as configurações**
> Para auditoria ou revisão, o administrador consulta todas as configurações ativas do sistema com seus valores e tipos.

---

## Integração com Sistemas Externos

Todo sistema que queira usar o SCOS como central de permissões precisa seguir quatro passos:

### Passo 1 — Registrar o sistema no scos-registry via gRPC

```
RegisterSystem(
  code: "STOCK_SYSTEM",
  description: "Sistema de Gestão de Estoque"
)
→ Recebe: systemId (UUID) + secretKey
```

### Passo 2 — Cadastrar os recursos do sistema

```
RegisterResource(
  systemId: "uuid-do-sistema",
  code: "PRODUCT_READ",
  description: "Visualizar produtos"
)
RegisterResource(
  systemId: "uuid-do-sistema",
  code: "ORDER_APPROVE",
  description: "Aprovar pedidos"
)
```

### Passo 3 — Importar o scos-security-starter

```xml
<dependency>
    <groupId>com.sawcunha.scos</groupId>
    <artifactId>scos-security-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

Configurar no `application.yml`:

```yaml
scos:
  registry:
    host: scos-registry.internal
    port: 9090
    system-id: ${SCOS_SYSTEM_ID}
    secret-key: ${SCOS_SECRET_KEY}
```

### Passo 4 — Proteger os endpoints

```java
@GetMapping("/products")
@RequiresPermission("PRODUCT_READ")
public List<Product> listProducts(ScosSecurityContext context) {
    return productService.findByBranch(context.getBranchId());
}
```

O starter intercepta o token automaticamente, chama o `scos-registry` e popula o `ScosSecurityContext`. O projeto externo não precisa gerenciar autenticação — apenas declarar quais permissões cada endpoint exige.

---

## Padrões de Projeto Aplicados

| Padrão | Onde se aplica |
|---|---|
| **Clean Architecture** | Separação em domain, application, infrastructure, api, boot |
| **Hexagonal (Ports & Adapters)** | Ports no application, adapters em api e infrastructure |
| **DDD — Bounded Contexts** | `scos-organization` = Organization Management / `scos-registry` = Permission Registry |
| **DDD — Aggregates e Value Objects** | Entidades e validações no módulo domain |
| **Outbox Pattern** | `SCOS_INTEGRATION_KEYCLOAK` como tabela de saída garantida |
| **Saga Pattern** | Fluxo de criação de usuário no Keycloak com compensações |
| **Anti-Corruption Layer** | Adaptador do Keycloak isolado na infrastructure |
| **Repository Pattern** | Interfaces no domain, implementações JPA na infrastructure |

---

## Informações Técnicas

| Item | Tecnologia |
|---|---|
| Linguagem | Java |
| Framework | Spring Boot |
| Autenticação | Keycloak (JWT / Bearer) |
| API REST | Spring Web |
| API gRPC | Spring gRPC |
| Banco de dados | PostgreSQL |
| Migrations | Liquibase |
| Arquitetura | Clean Architecture + Hexagonal + DDD |
| Biblioteca de segurança | scos-security-starter (publicada junto ao projeto) |

---

## Licença

Apache-2.0 — [https://www.apache.org/licenses/LICENSE-2.0](https://www.apache.org/licenses/LICENSE-2.0)

---

*Desenvolvido por Samuel Cunha — [github.com/SawCunhaOS](https://github.com/SawCunhaOS)*
