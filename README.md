# SCOS Organization - Sistema de Gestão de Organizações

![Java](https://img.shields.io/badge/Java-25-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.x-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18+-blue)
![Architecture](https://img.shields.io/badge/Architecture-Hexagonal%20%2B%20Clean-orange)
![DDD](https://img.shields.io/badge/Design-DDD-purple)
![License](https://img.shields.io/badge/License-Apache%202.0-yellowgreen)

## 📋 Índice

- [Visão Geral](#-visão-geral)
- [Arquitetura](#-arquitetura)
- [Estrutura do Projeto](#-estrutura-do-projeto)
- [Módulos](#-módulos)
- [Conceitos de Domínio](#-conceitos-de-domínio)
- [Fluxo de Dados](#-fluxo-de-dados)
- [Tecnologias](#-tecnologias)
- [Configuração](#-configuração)
- [Exemplos de Uso](#-exemplos-de-uso)

---

## 🎯 Visão Geral

**SCOS Organization** (SawCunha Open System - Organization) é um sistema modular de gestão de organizações empresariais que oferece:

- ✅ **Gestão de Empresas**: Criação e administração de matrizes e filiais
- ✅ **Hierarquia Multinível**: Suporte a estruturas organizacionais complexas
- ✅ **Gestão de Usuários**: Controle de usuários de negócio com roles e permissões
- ✅ **Integração com IdP**: Sincronização com Keycloak para autenticação centralizada
- ✅ **Configuração Flexível**: Personalização por empresa (timezone, idioma)
- ✅ **Auditoria Completa**: Rastreabilidade de todas as operações
- ✅ **Eventos de Domínio**: Comunicação assíncrona via Kafka

### Casos de Uso Principais

```
📊 Cenários Suportados:
├── Empresa matriz com múltiplas filiais
├── Filiais com sub-filiais (até 5 níveis)
├── Usuários com acesso a múltiplas empresas
├── Sincronização automática com Keycloak
└── Configurações específicas por empresa
```

---

## 🏛️ Arquitetura

O sistema foi construído seguindo os princípios de **Clean Architecture**, **Hexagonal Architecture (Ports & Adapters)** e **Domain-Driven Design (DDD)**.

### Princípios Fundamentais

#### 1. Separação de Responsabilidades

Cada camada possui responsabilidades claras e bem definidas:

```
┌─────────────────────────────────────────────┐
│           API Layer (REST)                  │  ← Apresentação
├─────────────────────────────────────────────┤
│       Application Layer (Use Cases)         │  ← Orquestração + Ports
├─────────────────────────────────────────────┤
│         Domain Layer (Negócio)              │  ← Regras de Negócio + Repositories
├─────────────────────────────────────────────┤
│    Infrastructure Layer (Técnico)           │  ← Adapters + Configurações
└─────────────────────────────────────────────┘
```

#### 2. Inversão de Dependências

As dependências sempre apontam **para dentro**, em direção ao domínio:

```
Infrastructure ──────┐
                     ↓
API ────> Application ────> Domain (núcleo isolado)
```

**Regras:**
- ✅ **Domain**: Não depende de ninguém
- ✅ **Application**: Depende apenas de Domain, define **Ports** (interfaces)
- ✅ **Infrastructure**: Implementa os **Adapters** para as **Ports** da Application
- ✅ **API**: Depende de Application

#### 3. Repositories vs Ports

**Diferença fundamental:**

| Conceito | Camada | Implementação | Propósito |
|----------|--------|---------------|-----------|
| **Repository** | Domain (interface) | Spring Data JPA (automático) | Acesso a dados do próprio domínio |
| **Port** | Application (interface) | Infrastructure (Adapter) | Integrações externas (Keycloak, Email, Kafka) |

```java
// ✅ Repository - Interface no DOMAIN
public interface CompanyRepository extends JpaRepository<Company, UUID> {
    // Spring Data JPA implementa automaticamente
}

// ✅ Port - Interface na APPLICATION
public interface IdentityProviderPort {
    String createUser(String email, String name);
}

// ✅ Adapter - Implementação na INFRASTRUCTURE
@Component
public class KeycloakAdapter implements IdentityProviderPort {
    // Implementação concreta
}
```

#### 4. Pragmatismo sobre Purismo

Optamos por uma abordagem pragmática para evitar complexidade desnecessária:

- **Entities JPA = Domain Entities**: Sem duplicação de código
- **Repositories**: Interfaces no Domain, implementação automática via Spring Data JPA
- **Value Objects**: Usamos `@Embeddable` para reutilização
- **Validações Simples**: Bean Validation na API, regras complexas no Domain Service

---

## 📁 Estrutura do Projeto

### Visão Hierárquica

```
scos-organization/
│
├── 📦 flow-organization-domain/          # Camada de Domínio
│   └── Regras de negócio, entidades, repositories (interfaces)
│
├── 📦 flow-organization-application/     # Camada de Aplicação
│   └── Casos de uso, DTOs, Ports (interfaces para integração)
│
├── 📦 flow-organization-infrastructure/  # Camada de Infraestrutura
│   └── Adapters (implementam Ports), mensageria, configurações
│
├── 📦 flow-organization-api/             # Camada de API REST
│   └── Controllers, validações, mapeamento de requisições
│
├── 📦 flow-organization-boot/            # Módulo de Inicialização
│   └── Spring Boot application, migrations, configurações
│
└── pom.xml                                # Parent POM
```

### Dependências entre Módulos

```
flow-organization-boot
    ├─→ flow-organization-api
    │       └─→ flow-organization-application
    │               └─→ flow-organization-domain
    │
    └─→ flow-organization-infrastructure
            └─→ flow-organization-application
                    └─→ flow-organization-domain
```

**Importante**: Infrastructure **NÃO** implementa interfaces do Domain. Infrastructure implementa **Ports** da Application.

---

## 📦 Módulos

### 1️⃣ Domain Layer (flow-organization-domain)

**Responsabilidade**: Contém o **coração do sistema** - as regras de negócio puras.

**NÃO contém**: Nenhuma referência a frameworks externos, exceto JPA (pragmatismo).

#### Estrutura Detalhada

```
domain/
├── model/                          # Entidades e Value Objects
│   ├── company/
│   │   ├── Company.java           # @Entity - Aggregate Root
│   │   ├── CompanyType.java       # Enum: HEADQUARTERS, BRANCH
│   │   ├── CompanyStatus.java     # Enum: ACTIVE, INACTIVE
│   │   └── CompanyConfiguration.java  # @Embeddable
│   │
│   ├── branch/
│   │   └── Branch.java            # @Entity - Filial com hierarquia
│   │
│   ├── user/
│   │   ├── BusinessUser.java      # @Entity - Usuário de negócio
│   │   ├── UserRole.java          # Enum: ADMIN, MANAGER
│   │   └── UserPermissions.java   # @Embeddable
│   │
│   └── shared/                    # Value Objects compartilhados
│       ├── Address.java           # @Embeddable - Endereço
│       ├── TaxIdentifier.java     # @Embeddable - CNPJ com validação
│       ├── Contact.java           # @Embeddable - Contatos
│       └── AuditInfo.java         # @Embeddable - Auditoria
│
├── repository/                     # Interfaces (Spring Data JPA)
│   ├── CompanyRepository.java
│   ├── BranchRepository.java
│   └── BusinessUserRepository.java
│
├── service/                        # Serviços de Domínio
│   ├── CompanyDomainService.java
│   ├── BranchHierarchyService.java
│   └── UserAuthorizationService.java
│
├── specification/                  # Criteria API
│   ├── CompanySpecification.java
│   └── BranchSpecification.java
│
├── event/                          # Eventos de Domínio
│   ├── CompanyCreatedEvent.java
│   ├── BranchActivatedEvent.java
│   └── UserAssignedToCompanyEvent.java
│
└── exception/                      # Exceções de Domínio
    ├── CompanyNotFoundException.java
    ├── DuplicateCompanyException.java
    └── InvalidBranchHierarchyException.java
```

#### O que tem no Domain

| Componente | Descrição | Exemplo |
|------------|-----------|---------|
| **Entity** | Conceitos centrais com identidade | `Company`, `Branch` |
| **Value Object** | Conceitos imutáveis sem identidade | `TaxIdentifier`, `Address` |
| **Repository Interface** | Contrato de persistência | `CompanyRepository` |
| **Domain Service** | Lógica multi-entidade | Validar CNPJ duplicado |
| **Specification** | Queries reutilizáveis | Filtros dinâmicos |
| **Domain Event** | POJOs para eventos | `CompanyCreatedEvent` (classe disponível **para histórico/compatibilidade**, mas **NÃO** publicada em runtime para qualquer operação de `Company` ou sub‑recursos; operações são audit‑only) |
| **Exception** | Exceções de negócio | `CompanyNotFoundException` |

#### O que NÃO tem no Domain

- ❌ Implementações de Repositories (Spring Data faz automaticamente)
- ❌ Configurações de Spring
- ❌ Chamadas HTTP, Kafka, etc
- ❌ DTOs de API
- ❌ Qualquer dependência de Infrastructure

#### Exemplo: Entity com Lógica de Negócio

```java
@Entity
@Table(name = "companies")
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    private String name;
    
    @Enumerated(EnumType.STRING)
    private CompanyStatus status;
    
    // Lógica de negócio na própria entidade
    public void activate() {
        if (this.status == CompanyStatus.ACTIVE) {
            throw new IllegalStateException("Already active");
        }
        this.status = CompanyStatus.ACTIVE;
    }
}
```

#### Exemplo: Domain Service

```java
@Service
public class CompanyDomainService {
    private final CompanyRepository repository;
    
    // Lógica que envolve consulta ao banco
    public void validateUniqueTaxIdentifier(TaxIdentifier cnpj) {
        if (repository.existsByTaxIdentifier(cnpj)) {
            throw new DuplicateCompanyException();
        }
    }
}
```

#### Exemplo: Repository

```java
@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID> {
    
    Optional<Company> findByTaxIdentifier(TaxIdentifier taxIdentifier);
    
    boolean existsByTaxIdentifier(TaxIdentifier taxIdentifier);
    
    @Query("SELECT c FROM Company c WHERE c.type = 'HEADQUARTERS'")
    List<Company> findActiveHeadquarters();
}
```

---

### 2️⃣ Application Layer (flow-organization-application)

**Responsabilidade**: Orquestra os **casos de uso** e define **Ports** para integrações externas.

#### Estrutura Detalhada

```
application/
├── usecase/                        # Casos de Uso
│   ├── company/
│   │   ├── CreateCompanyUseCase.java
│   │   ├── UpdateCompanyUseCase.java
│   │   ├── ActivateCompanyUseCase.java
│   │   └── ListCompaniesUseCase.java
│   │
│   ├── branch/
│   │   ├── CreateBranchUseCase.java
│   │   └── GetBranchHierarchyUseCase.java
│   │
│   └── user/
│       ├── RegisterBusinessUserUseCase.java
│       └── AssignUserToCompanyUseCase.java
│
├── dto/                            # DTOs de Aplicação
│   ├── company/
│   │   ├── CreateCompanyDTO.java
│   │   ├── UpdateCompanyDTO.java
│   │   └── CompanyDetailDTO.java
│   │
│   └── user/
│       ├── RegisterUserDTO.java
│       └── UserDetailDTO.java
│
├── mapper/                         # Entity ↔ DTO
│   ├── CompanyMapper.java
│   ├── BranchMapper.java
│   └── UserMapper.java
│
├── port/                           # 🔑 PORTS (Interfaces)
│   ├── IdentityProviderPort.java
│   ├── NotificationPort.java
│   ├── EventPublisherPort.java
│   └── AuditPort.java
│
└── validator/                      # Validadores
    ├── CompanyValidator.java
    └── BranchValidator.java
```

#### Ports (Hexagonal Architecture)

**Ports são INTERFACES definidas na Application Layer:**

```java
// Port - Define O QUE precisa ser feito
package br.com.sawcunha.scos.organization.application.port;

public interface IdentityProviderPort {
    String createUser(String email, String name, String password);
    void updateUser(String externalId, String name);
    void deleteUser(String externalId);
}
```

**Por que Ports na Application?**
- Use Cases precisam de integrações externas (Keycloak, Email, Kafka)
- Mas não podem depender de detalhes técnicos
- Ports definem **o que** precisa, Infrastructure define **como** fazer

#### Exemplo: Use Case

```java
@Service
public class CreateCompanyUseCase {
    private final CompanyRepository repository;
    private final CompanyDomainService domainService;
    private final CompanyMapper mapper;
    private final EventPublisherPort eventPublisher;
    
    @Transactional
    public CompanyDetailDTO execute(CreateCompanyDTO dto) {
        // 1. Validar com Domain Service
        domainService.validateUniqueTaxIdentifier(dto.taxIdentifier());
        
        // 2. Converter e persistir
        Company company = mapper.toEntity(dto);
        Company saved = repository.save(company);
        
        // 3. Registrar auditoria (POLÍTICA DO PROJETO: não publicar evento Kafka nem sincronizar com Keycloak para CREATE/UPDATE de Company)
        //    eventPublisher.publish(new CompanyCreatedEvent(saved.getId())); // **não usar** para criação/edição de Company
        
        // 4. Retornar DTO
        return mapper.toDetailDTO(saved);
    }
}
```

---

### 3️⃣ Infrastructure Layer (flow-organization-infrastructure)

**Responsabilidade**: Implementa **Adapters** para as **Ports** e fornece configurações técnicas.

**Regra de Ouro**: Infrastructure **implementa Ports da Application**, **NÃO implementa Repositories do Domain**.

#### Estrutura Detalhada

```
infrastructure/
├── adapter/                        # 🔌 ADAPTERS (implementam Ports)
│   ├── keycloak/
│   │   ├── KeycloakAdapter.java   # implements IdentityProviderPort
│   │   ├── KeycloakClient.java
│   │   └── KeycloakMapper.java
│   │
│   ├── notification/
│   │   ├── EmailNotificationAdapter.java
│   │   └── SmsNotificationAdapter.java
│   │
│   └── audit/
│       └── DatabaseAuditAdapter.java
│
├── messaging/                      # Mensageria
│   ├── publisher/
│   │   └── KafkaEventPublisher.java
│   │
│   ├── listener/
│   │   ├── CompanyEventListener.java
│   │   └── UserEventListener.java
│   │
│   └── config/
│       └── KafkaConfig.java
│
├── config/                         # Configurações Spring
│   ├── JpaConfig.java
│   ├── SecurityConfig.java
│   ├── KeycloakConfig.java
│   └── CacheConfig.java
│
└── interceptor/
    └── AuditInterceptor.java
```

#### Adapters (Implementam Ports)

```java
// Adapter - Implementa COMO fazer
package br.com.sawcunha.scos.organization.infrastructure.adapter.keycloak;

import br.com.sawcunha.scos.organization.application.port.IdentityProviderPort;

@Component
public class KeycloakAdapter implements IdentityProviderPort {
    
    private final KeycloakClient client;
    
    @Override
    public String createUser(String email, String name, String password) {
        // Implementação técnica usando Keycloak Admin API
        return client.createUser(email, name);
    }
}
```

#### O que Infrastructure FAZ

- ✅ Implementa **Ports** da Application (via Adapters)
- ✅ Configura Spring Boot
- ✅ Configura JPA, Security, Kafka
- ✅ Publica/consome eventos
- ✅ Interceptors, Filters

#### O que Infrastructure NÃO FAZ

- ❌ **NÃO implementa Repositories do Domain** (Spring Data faz automaticamente)
- ❌ NÃO contém regras de negócio
- ❌ NÃO conhece casos de uso específicos

---

### Esclarecimento: Repositories vs Ports

```
┌─────────────────────────────────────────────────────────────┐
│                    DIFERENÇA FUNDAMENTAL                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  REPOSITORIES (Domain)                                      │
│  ├── Interface: Domain Layer                                │
│  ├── Implementação: Spring Data JPA (automática)            │
│  ├── Propósito: Persistência de entidades do domínio        │
│  └── Exemplo: CompanyRepository.save(company)               │
│                                                             │
│  PORTS (Application)                                        │
│  ├── Interface: Application Layer                           │
│  ├── Implementação: Infrastructure (Adapters)               │
│  ├── Propósito: Integrações externas ao domínio             │
│  └── Exemplo: IdentityProviderPort.createUser()             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**Exemplo Prático:**

```java
@Service
public class CreateCompanyUseCase {
    
    // ✅ Repository do Domain (Spring Data implementa)
    private final CompanyRepository companyRepository;
    
    // ✅ Port da Application (Infrastructure implementa)
    private final IdentityProviderPort identityProvider;
    
    // ✅ Port da Application (Infrastructure implementa)
    private final EventPublisherPort eventPublisher;
    
    @Transactional
    public CompanyDetailDTO execute(CreateCompanyDTO dto) {
        // Repository: persistência
        Company company = companyRepository.save(...);
        
        // Port: integração externa
        identityProvider.createUser(...);
        
        // Port: publicar evento
        // **POLÍTICA DO PROJETO**: não publicar evento Kafka nem sincronizar com Keycloak para CREATE/UPDATE de `Company`.
        // eventPublisher.publish(new CompanyCreatedEvent(...)); // não usar para criação/edição de Company
        
        return ...;
    }
}
```

---

### 4️⃣ API Layer (flow-organization-api)

**Responsabilidade**: Expor **endpoints REST** e validar entradas.

#### Estrutura Detalhada

```
api/
├── controller/                     # REST Controllers
│   ├── CompanyController.java
│   ├── BranchController.java
│   └── UserController.java
│
├── request/                        # DTOs de Requisição
│   ├── CreateCompanyRequest.java
│   └── UpdateCompanyRequest.java
│
├── response/                       # DTOs de Resposta
│   ├── CompanyResponse.java
│   └── ApiErrorResponse.java
│
├── mapper/                         # Request/Response ↔ DTO
│   ├── CompanyApiMapper.java
│   └── UserApiMapper.java
│
├── exception/
│   └── GlobalExceptionHandler.java
│
├── security/
│   ├── JwtAuthenticationFilter.java
│   └── SecurityContextHolder.java
│
└── validation/
    ├── ValidCNPJ.java
    └── CNPJValidator.java
```

#### Exemplo: Controller

```java
@RestController
@RequestMapping("/api/v1/companies")
public class CompanyController {
    private final CreateCompanyUseCase createUseCase;
    private final CompanyApiMapper mapper;
    
    @PostMapping
    @ResponseStatus(CREATED)
    public CompanyResponse create(@Valid @RequestBody CreateCompanyRequest request) {
        CreateCompanyDTO dto = mapper.toDTO(request);
        CompanyDetailDTO result = createUseCase.execute(dto);
        return mapper.toResponse(result);
    }
}
```

---

### 5️⃣ Boot Module (flow-organization-boot)

**Responsabilidade**: **Inicializar** a aplicação e agregar todos os módulos.

```
boot/
├── src/main/java/
│   └── ScosOrganizationApplication.java
│
└── src/main/resources/
    ├── application.yml
    ├── application-dev.yml
    ├── application-prod.yml
    │
    └── db/changelog/                # Liquibase Changelogs
        ├── db.changelog-master.xml
        ├── changelogs/
        │   ├── 001-create-company-table.xml
        │   ├── 002-create-branch-table.xml
        │   ├── 003-create-business-user-table.xml
        │   └── 004-create-indexes.xml
        └── data/
            └── seed-data.xml
```

---

## 🧩 Conceitos de Domínio

### Agregados Principais

#### 1. Company (Matriz/Filial)

```
Company
├── Atributos
│   ├── id: UUID
│   ├── name: String
│   ├── taxIdentifier: TaxIdentifier (CNPJ)
│   ├── type: CompanyType (HEADQUARTERS | BRANCH)
│   ├── status: CompanyStatus (ACTIVE | INACTIVE)
│   ├── configuration: CompanyConfiguration
│   └── branches: List<Branch>
│
└── Regras de Negócio
    ├── CNPJ deve ser único
    ├── Matriz pode ter múltiplas filiais
    ├── Matriz não pode ser desativada com filiais ativas
    └── Filiais precisam de matriz ativa
```

#### 2. Branch (Filial)

```
Branch
├── Atributos
│   ├── id: UUID
│   ├── name: String
│   ├── headquarters: Company
│   ├── parentBranch: Branch (nullable)
│   └── subBranches: List<Branch>
│
└── Regras de Negócio
    ├── Máximo de 5 níveis de hierarquia
    ├── Não pode haver referência circular
    ├── Deve estar vinculada a uma matriz
    └── Pode ter filiais subordinadas
```

#### 3. BusinessUser (Usuário)

```
BusinessUser
├── Atributos
│   ├── id: UUID
│   ├── name: String
│   ├── email: String (único)
│   ├── externalId: String (Keycloak)
│   ├── companies: Set<Company>
│   ├── roles: Set<UserRole>
│   └── permissions: UserPermissions
│
└── Regras de Negócio
    ├── Email deve ser único
    ├── Sincronizado com Keycloak
    ├── Acesso a múltiplas empresas
    └── Permissões por role
```

### Value Objects

| Value Object    | Propósito         | Validações             |
|-----------------|-------------------|------------------------|
| `TaxIdentifier` | CNPJ validado     | 14 dígitos, algoritmo  |
| `Address`       | Endereço completo | CEP, UF obrigatórios   |
| `Contact`       | Dados de contato  | Email válido           |
| `AuditInfo`     | Rastreabilidade   | Timestamps automáticos |

### Enums

```java
// Tipo de empresa
enum CompanyType {
    HEADQUARTERS,  // Matriz
    BRANCH         // Filial
}

// Status da empresa
enum CompanyStatus {
    ACTIVE,
    INACTIVE,
    PENDING_APPROVAL,
    SUSPENDED
}

// Roles de usuário
enum UserRole {
    ADMIN,      // Administrador global
    MANAGER,    // Gerente de empresa
    OPERATOR,   // Operador
    VIEWER      // Visualizador
}
```

---

## 🔄 Fluxo de Dados

### Fluxo Completo: Criar Empresa

```
1. Cliente HTTP
   POST /api/v1/companies
   Body: { "name": "Acme", "taxIdentifier": "12345678000190" }
   
   ↓

2. CompanyController (API Layer)
   - Valida Request
   - Mapeia → DTO
   - Chama Use Case
   
   ↓

3. CreateCompanyUseCase (Application Layer)
   - Valida com Domain Service
   - Converte DTO → Entity
   - Persiste via Repository (Domain)
   - Chama Ports (Infrastructure implementa)
   
   ↓

4. CompanyDomainService (Domain Layer)
   - Valida CNPJ duplicado
   - CompanyRepository.existsByTaxIdentifier()
   
   ↓

5. Persistência
   - CompanyRepository.save() (Spring Data JPA)
   
   ↓

6. Integrações (via Ports)
   - IdentityProviderPort → KeycloakAdapter
   - EventPublisherPort → KafkaEventPublisher
   
   ↓

7. Eventos Assíncronos
   - CompanyEventListener consome
   - Envia email, auditoria
```

### Decisões Arquiteturais

| Situação                          | Onde colocar         | Camada         |
|-----------------------------------|----------------------|----------------|
| Validação **uma entidade**        | Método na Entity     | Domain         |
| Validação **múltiplas entidades** | Domain Service       | Domain         |
| **Acesso a dados** do domínio     | Repository Interface | Domain         |
| **Orquestração**                  | Use Case             | Application    |
| **Integração externa**            | Port (interface)     | Application    |
| **Implementação integração**      | Adapter              | Infrastructure |

---

## 🛠️ Tecnologias

### Core

| Tecnologia      | Versão | Uso            |
|-----------------|--------|----------------|
| Java            | 25     | Linguagem base |
| Spring Boot     | 4.x    | Framework      |
| Spring Data JPA | 4.x    | Persistência   |
| Hibernate       | 7.x    | ORM            |
| PostgreSQL      | 18+    | Banco de dados |

### Integrações

| Tecnologia   | Uso                                      |
|--------------|------------------------------------------|
| Keycloak     | Autenticação/autorização                 |
| Kafka        | Mensageria e eventos                     |
| Liquibase    | Migrations e controle de versão do banco |
| MapStruct    | Mapeamento                               |
| Lombok       | Redução boilerplate                      |

### Testes

| Tecnologia     | Uso               |
|----------------|-------------------|
| JUnit 5        | Testes unitários  |
| Mockito        | Mocks             |
| Testcontainers | Testes integração |
| REST Assured   | Testes API        |

---

## ⚙️ Configuração

### Pré-requisitos

```
- Java 25
- Maven 3.9+
- PostgreSQL 18+
- Keycloak 26+
- Kafka 3.x (opcional)
```

### Variáveis de Ambiente

```bash
# Database
DB_USERNAME=postgres
DB_PASSWORD=postgres
DB_URL=jdbc:postgresql://localhost:5432/scos_organization

# Keycloak
KEYCLOAK_SERVER_URL=http://localhost:8080
KEYCLOAK_REALM=scos
KEYCLOAK_CLIENT_ID=scos-organization
KEYCLOAK_CLIENT_SECRET=your-secret

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

### Executar o Projeto

```bash
# 1. Compilar
mvn clean install

# 2. Rodar migrations (Liquibase)
mvn liquibase:update

# 3. Iniciar aplicação
mvn spring-boot:run -pl flow-organization-boot

# Ou via JAR
java -jar flow-organization-boot/target/flow-organization-boot-1.0.0.jar
```

### Perfis de Ambiente

```bash
# Desenvolvimento
mvn spring-boot:run -Dspring.profiles.active=dev

# Produção
java -jar app.jar --spring.profiles.active=prod
```

### Configuração application.yml (exemplo)

```yaml
spring:
  application:
    name: scos-organization
  
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/scos_organization}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: none  # Liquibase gerencia o schema
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        default_schema: public
        dialect: org.hibernate.dialect.PostgreSQLDialect

  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.xml
    default-schema: public
    drop-first: false  # CUIDADO: true apaga o banco antes de aplicar

  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

keycloak:
  server-url: ${KEYCLOAK_SERVER_URL:http://localhost:8080}
  realm: ${KEYCLOAK_REALM:scos}
  client-id: ${KEYCLOAK_CLIENT_ID:scos-organization}
  client-secret: ${KEYCLOAK_CLIENT_SECRET}

logging:
  level:
    br.com.sawcunha.scos: DEBUG
    org.hibernate.SQL: DEBUG
    liquibase: INFO
```

### Exemplo de Changelog Liquibase

**db.changelog-master.xml**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">

    <include file="db/changelog/changelogs/001-create-company-table.xml"/>
    <include file="db/changelog/changelogs/002-create-branch-table.xml"/>
    <include file="db/changelog/changelogs/003-create-business-user-table.xml"/>
    <include file="db/changelog/changelogs/004-create-indexes.xml"/>
    
    <!-- Dados iniciais (apenas em dev) -->
    <include file="db/changelog/data/seed-data.xml" context="dev"/>
</databaseChangeLog>
```

**001-create-company-table.xml**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">

    <changeSet id="001-create-company-table" author="scos-team">
        <createTable tableName="companies">
            <column name="id" type="uuid">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="name" type="varchar(200)">
                <constraints nullable="false"/>
            </column>
            <column name="trade_name" type="varchar(200)"/>
            <column name="tax_identifier" type="varchar(14)">
                <constraints nullable="false" unique="true"/>
            </column>
            <column name="type" type="varchar(20)">
                <constraints nullable="false"/>
            </column>
            <column name="status" type="varchar(20)">
                <constraints nullable="false"/>
            </column>
            
            <!-- Address (Embeddable) -->
            <column name="street" type="varchar(200)"/>
            <column name="number" type="varchar(20)"/>
            <column name="complement" type="varchar(100)"/>
            <column name="neighborhood" type="varchar(100)"/>
            <column name="city" type="varchar(100)"/>
            <column name="state" type="varchar(2)"/>
            <column name="zip_code" type="varchar(10)"/>
            <column name="country" type="varchar(50)"/>
            
            <!-- Configuration (Embeddable) -->
            <column name="timezone" type="varchar(50)"/>
            <column name="default_language" type="varchar(10)"/>

            
            <!-- Audit Info (Embeddable) -->
            <column name="created_at" type="timestamp" defaultValueComputed="CURRENT_TIMESTAMP">
                <constraints nullable="false"/>
            </column>
            <column name="updated_at" type="timestamp"/>
            <column name="created_by" type="varchar(100)"/>
            <column name="updated_by" type="varchar(100)"/>
        </createTable>
        
        <addNotNullConstraint tableName="companies" columnName="created_at"/>
    </changeSet>
</databaseChangeLog>
```

---

## 📚 Exemplos de Uso

### 1. Criar Matriz

```bash
POST /api/v1/companies
Content-Type: application/json
Authorization: Bearer {token}

{
  "name": "Acme Corporation",
  "tradeName": "Acme",
  "taxIdentifier": "12345678000190",
  "type": "HEADQUARTERS",
  "address": {
    "street": "Av. Paulista",
    "number": "1000",
    "city": "São Paulo",
    "state": "SP",
    "zipCode": "01310-100",
    "country": "Brasil"
  },
  "configuration": {
    "timezone": "America/Sao_Paulo",
    "defaultLanguage": "pt-BR"
  }
}
```

**Resposta**: `201 Created`
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "name": "Acme Corporation",
  "type": "HEADQUARTERS",
  "status": "ACTIVE",
  "createdAt": "2024-02-15T10:30:00Z"
}
```

### 2. Criar Filial

```bash
POST /api/v1/branches

{
  "name": "Acme Filial Rio",
  "headquartersId": "123e4567-e89b-12d3-a456-426614174000",
  "taxIdentifier": "98765432000190",
  "address": { ... }
}
```

### 3. Atribuir Usuário a Empresa

```bash
PUT /api/v1/users/{userId}/companies/{companyId}
```

### 4. Buscar Hierarquia de Filiais

```bash
GET /api/v1/branches/{branchId}/hierarchy
```

**Resposta**:
```json
{
  "headquartersId": "...",
  "branches": [
    {
      "id": "...",
      "name": "Filial A",
      "level": 1,
      "subBranches": [
        {
          "id": "...",
          "name": "Sub-Filial A1",
          "level": 2
        }
      ]
    }
  ]
}
```

---

## 📖 Convenções de Código

### Nomenclatura

```java
// Entities: Substantivo singular
Company, Branch, BusinessUser

// Repositories: Entity + Repository
CompanyRepository, BranchRepository

// Domain Services: Entity + DomainService
CompanyDomainService, BranchHierarchyService

// Use Cases: Action + Entity + UseCase
CreateCompanyUseCase, UpdateCompanyUseCase

// DTOs: Action + Entity + DTO
CreateCompanyDTO, CompanyDetailDTO

// Events: Entity + Action + Event
CompanyCreatedEvent, BranchActivatedEvent

// Ports: Purpose + Port
IdentityProviderPort, NotificationPort

// Adapters: Technology + Adapter
KeycloakAdapter, KafkaEventPublisher
```

### Pacotes

```
br.com.sawcunha.scos.organization
├── domain.model.company
├── domain.repository
├── domain.service
├── application.usecase.company
├── application.dto.company
├── application.port
├── infrastructure.adapter.keycloak
├── infrastructure.messaging
└── api.controller
```

---

## 🎯 Benefícios da Arquitetura

### ✅ Testabilidade

```
- Domain: Testável sem infraestrutura
- Application: Testável com mocks de Ports
- Adapters: Testáveis isoladamente
- API: Testes de contrato
```

### ✅ Manutenibilidade

```
- Mudanças isoladas em camadas
- Regras de negócio centralizadas
- Baixo acoplamento
- Ports permitem trocar implementações
```

### ✅ Escalabilidade

```
- Evolução para microsserviços
- Eventos assíncronos
- Stateless
- Escala horizontal
```

### ✅ Independência

```
- Trocar Keycloak: Apenas KeycloakAdapter
- Trocar Kafka: Apenas Messaging
- Trocar PostgreSQL: Apenas config
- Domain permanece intocado
```

---

## 📊 Diagrama de Arquitetura

```
┌─────────────────────────────────────────────────────────┐
│                    API Layer                            │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐               │
│  │ Company  │  │  Branch  │  │   User   │               │
│  │Controller│  │Controller│  │Controller│               │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘               │
└───────┼─────────────┼─────────────┼─────────────────────┘
        │             │             │
        ↓             ↓             ↓
┌─────────────────────────────────────────────────────────┐
│                Application Layer                        │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐         │
│  │  Use Cases │  │    DTOs    │  │   Mappers  │         │
│  └────────────┘  └────────────┘  └────────────┘         │
│                                                         │
│  🔌 PORTS (Interfaces)                                  │
│  ┌─────────────────┐  ┌──────────────┐                  │
│  │IdentityProvider │  │EventPublisher│                  │
│  │      Port       │  │     Port     │                  │
│  └────────┬────────┘  └──────┬───────┘                  │
└───────────┼────────────────┼────────────────────────────┘
            │                │
            ↓                ↓
┌─────────────────────────────────────────────────────────┐
│              Infrastructure Layer                       │
│  🔌 ADAPTERS (Implementações)                           │
│  ┌─────────────┐  ┌──────────────┐                      │
│  │  Keycloak   │  │    Kafka     │                      │
│  │   Adapter   │  │EventPublisher│                      │
│  └─────────────┘  └──────────────┘                      │
│                                                         │
│  ⚙️ Configurações Spring                                │
└─────────────────────────────────────────────────────────┘
            ↓
┌─────────────────────────────────────────────────────────┐
│                  Domain Layer                           │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐               │
│  │Entities  │  │Services  │  │Events    │               │
│  └──────────┘  └──────────┘  └──────────┘               │
│                                                         │
│  📁 REPOSITORY INTERFACES                               │
│  (Spring Data JPA implementa automaticamente)           │
│  ┌────────────────────────────────────────┐             │
│  │  CompanyRepository, BranchRepository   │             │
│  └────────────────────────────────────────┘             │
└─────────────────────────────────────────────────────────┘
```

---

## 📝 Resumo da Arquitetura

### Responsabilidades por Camada

| Camada             | Contém                                                                  | NÃO Contém                                |
|--------------------|-------------------------------------------------------------------------|-------------------------------------------|
| **Domain**         | Entities, Value Objects, Repository Interfaces, Domain Services, Events | Implementações técnicas, Configs          |
| **Application**    | Use Cases, DTOs, Mappers, **Port Interfaces**, Validators               | Implementações de Ports, Regras complexas |
| **Infrastructure** | **Adapters (implementam Ports)**, Configs Spring, Messaging             | Repositories, Regras de negócio           |
| **API**            | Controllers, Request/Response DTOs, Exception Handlers                  | Lógica de negócio, Orquestração           |

### Implementação de Interfaces

```
┌─────────────────────────────────────────────────────┐
│         QUEM IMPLEMENTA O QUÊ?                      │
├─────────────────────────────────────────────────────┤
│                                                     │
│  Repository Interfaces (Domain)                     │
│  └─→ Implementado por: Spring Data JPA (automático) │
│                                                     │
│  Port Interfaces (Application)                      │
│  └─→ Implementado por: Adapters (Infrastructure)    │
│                                                     │
└─────────────────────────────────────────────────────┘
```

---

## 📝 Licença

Este projeto está licenciado sob a **Apache License 2.0** - veja o arquivo [LICENSE](LICENSE) para detalhes.

```
Copyright 2026 SawCunha Open System

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

## 👥 Contribuindo

Contribuições são bem-vindas! Por favor:

1. Fork o projeto
2. Crie uma branch (`git checkout -b feature/AmazingFeature`)
3. Commit suas mudanças (`git commit -m 'Add some AmazingFeature'`)
4. Push para a branch (`git push origin feature/AmazingFeature`)
5. Abra um Pull Request

---

## 📞 Contato

**SCOS Team** - SawCunha Open System

- Website: [www.sacunhaos.com.br](https://www.sacunhaos.com.br)

---

**Construído com ❤️ seguindo princípios de Clean Architecture, Hexagonal Architecture (Ports & Adapters) e DDD**
