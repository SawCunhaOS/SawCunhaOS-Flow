# Atualização dos Models de Domínio Conforme Schema do Banco

**Data**: 2026-06-08  
**Status**: 🔄 Em Análise  
**Tipo**: 🔧 Refatoração

---

## ⚠️ Princípio SRP — Uma Funcionalidade por Ideia

> **Regra**: Uma ideia = uma funcionalidade. Features independentes → arquivos separados.

- **Nome da funcionalidade**: `atualizacao-models-dominio-conforme-banco`
- **Resumo em uma frase**: Sincronizar as entidades JPA e repositórios do módulo `flow-organization-domain` com o schema real do banco de dados definido nas migrations Liquibase e no `domain_model.md`.

**Checklist SRP**:
- [x] Esta ideia cobre exatamente uma funcionalidade
- [x] Não mistura features independentes no mesmo arquivo
- [x] O nome é específico (não genérico como "melhorar módulo X")

---

## 1️⃣ Visão

### Problema
As entidades JPA no pacote `domain/model` foram criadas antes das migrations Liquibase serem finalizadas. O resultado é uma divergência significativa: campos que não existem no banco estão mapeados nas entidades, campos que existem no banco estão ausentes nas entidades, nomes de colunas PK incorretos, e seis tabelas do schema não possuem nenhuma entidade JPA correspondente. Isso faz o Hibernate gerar SQL inválido e impossibilita o uso correto das tabelas de integração, permissões e sistema.

### Objetivo
Todos os models JPA e repositórios do pacote `flow-organization-domain` devem refletir exatamente o schema definido no Liquibase (`v1.0.0`) e no `domain_model.md`. O critério de sucesso é: nenhum campo mapeado que não exista no banco, nenhum campo do banco relevante sem mapeamento, zero erro de schema-validation do Hibernate.

### Fora de Escopo
- Alterações nas migrations Liquibase — elas são a fonte da verdade, não serão modificadas
- Alterações em camadas fora de `domain/model` e `domain/repository` (application, infrastructure, api)
- Lógica de negócio além do ajuste nos métodos que dependem dos campos removidos/adicionados

---

## 2️⃣ Requisitos

### Funcionais
- [ ] **RF-01**: Remover campos sem correspondência no banco de todos os models existentes
- [ ] **RF-02**: Adicionar campos presentes no banco que estão ausentes nos models
- [ ] **RF-03**: Corrigir nomes de colunas PK divergentes
- [ ] **RF-04**: Refatorar `CompanyAddress` para usar PK composta (`CompanyAddressPk`)
- [ ] **RF-05**: Corrigir enum `StatusCompany` (DELETED → DISABLED)
- [ ] **RF-06**: Criar enum `StatusEmployee` (ACTIVE/INACTIVE/DISABLED)
- [ ] **RF-07**: Criar enum `LoginType` (EMPLOYEE/EXTERNAL/SERVICE)
- [ ] **RF-08**: Criar as 6 entidades JPA ausentes com seus repositórios
- [ ] **RF-09**: Ajustar métodos de negócio dos models afetados pelos campos removidos/adicionados
- [ ] **RF-10**: Atualizar repositórios impactados pelas mudanças de PK e enum

### Não-Funcionais
- [ ] **RNF-01**: Nenhuma alteração de schema no banco — só o código Java muda
- [ ] **RNF-02**: Hibernate schema-validation deve passar sem erros após as mudanças

---

## 🔒 Padrões Obrigatórios do Projeto

> **Regra**: Toda implementação DEVE seguir os padrões já estabelecidos no projeto. Nenhum novo padrão pode ser introduzido. Em caso de dúvida, consultar as entidades e repositórios existentes como referência.

### Entidades JPA

| Padrão | Regra | Referência |
|---|---|---|
| **Herança** | Toda entidade com `CREATED_AT` + `UPDATED_AT` + `USER_AT` DEVE estender `BaseEntity` de `scos-foundation` | `Company`, `Employee`, `Department` |
| **Entidades imutáveis** | Entidades sem `UPDATED_AT` NÃO DEVEM estender `BaseEntity` — declarar `CREATED_AT` e `USER_AT` diretamente | `ProfileResource`, `IntegrationKeycloakLog`, `IntegrationMessageInvalid` |
| **Auditoria** | DEVE usar `@Auditable` de `scos-foundation` em todas as entidades | `Company`, `Login`, `Profile` |
| **LGPD — auditRead** | Entidades com dados pessoais DEVEM usar `@Auditable(auditRead = true)` — ativa rastreio de leitura via Hibernate `PostLoad` (emite `ActionType.SELECT` na trilha de auditoria). Entidades sem dados pessoais usam `@Auditable` padrão (`auditRead = false`). | Ver tabela abaixo |
| **Lombok** | DEVE usar `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor` — sem exceções | Todos os models |
| **PK composta** | DEVE usar classe `@Embeddable` separada com `@EmbeddedId` na entidade | `EmployeeAddress` + `EmployeeAddressPk` |
| **Value Objects** | DEVE usar VOs da foundation (`Cnpj`, `Cpf`, `Email`) com `@Embedded` + `@AttributeOverride` — nunca mapear como String diretamente | `Company.taxIdentifier`, `Employee.taxIdentifier`, `Employee.email`, `CompanyContact.email`, `IntegrationKeycloak.email` |
| **Enums** | DEVE usar `@Enumerated(EnumType.STRING)`. Enum em arquivo próprio com `@Getter` e `displayName` | `StatusCompany` |
| **JSONB** | DEVE usar `@Type(JsonBinaryType.class)` do Hypersistence Utils para campos `JSONB` | — (novo em `IntegrationKeycloak`) |

#### Classificação LGPD por Entidade

| Entidade | Dados Pessoais | `auditRead` |
|---|---|---|
| `Employee` | CPF (`taxIdentifier`), e-mail, nome, data nascimento, geolocalização | `true` |
| `EmployeeContact` | Telefone | `true` |
| `EmployeeAddress` | Geolocalização, número, complemento | `true` |
| `Login` | Login (e-mail), `keycloakId` | `true` |
| `IntegrationKeycloak` | E-mail, username | `true` |
| `Company` | Apenas dados corporativos (CNPJ, razão social) | `false` |
| `CompanyContact` | Telefone e e-mail corporativo | `false` |
| `CompanyAddress` | Endereço corporativo | `false` |
| `Department` | Sem dados pessoais | `false` |
| `Position` | Sem dados pessoais | `false` |
| `Profile` | Sem dados pessoais | `false` |
| `System` | Sem dados pessoais | `false` |
| `Resource` | Sem dados pessoais | `false` |
| `ProfileResource` | Sem dados pessoais | `false` |
| `IntegrationKeycloakLog` | Resposta técnica, sem PII direto | `false` |
| `IntegrationMessageInvalid` | Payload desconhecido — tratar com cautela | `false` |
| `PartnersConfiguration` | Sem dados pessoais | `false` |

### Repositórios

| Padrão | Regra | Referência |
|---|---|---|
| **Interface** | DEVE ser `interface` anotada com `@Repository` | Todos os repositórios |
| **Herança** | DEVE estender `BaseJpaRepository<T, ID>` + `JpaSpecificationExecutor<T>` + `QuerydslPredicateExecutor<T>` | `CompanyRepository`, `LoginRepository` |
| **QueryDSL** | Predicados DEVEM usar campo estático do Q-type declarado na própria interface | `QCompany company = QCompany.company` em `CompanyRepository` |
| **Queries derivadas** | Queries simples DEVEM usar derivação por nome de método do Spring Data | `findByLogin`, `findByCode` |
| **Sem implementação** | Repositórios NÃO DEVEM ter classe de implementação — apenas `default` methods para predicados compostos | `CompanyRepository.findAllNotDeleted` |

---

## 3️⃣ Arquitetura

### Componentes Afetados
```
scos-organization-domain/src/main/java/.../domain/
├── model/
│   ├── company/
│   │   ├── Company.java              : modificação
│   │   ├── CompanyAddress.java       : modificação (PK composta)
│   │   ├── CompanyAddressPk.java     : adição (nova classe embeddable)
│   │   ├── CompanyContact.java       : modificação
│   │   └── StatusCompany.java        : modificação (DELETED → DISABLED)
│   ├── employee/
│   │   ├── Employee.java             : modificação
│   │   ├── EmployeeAddressPk.java    : modificação (renomear campo)
│   │   ├── EmployeeContact.java      : modificação
│   │   └── StatusEmployee.java       : adição (novo enum)
│   ├── login/
│   │   ├── Login.java                : modificação
│   │   ├── LoginType.java            : adição (novo enum)
│   │   └── Profile.java              : modificação
│   ├── configuration/
│   │   └── PartnersConfiguration.java: modificação (reestruturação completa)
│   ├── system/                       : adição (novo pacote)
│   │   ├── System.java               : adição
│   │   └── Resource.java             : adição
│   ├── permission/                   : adição (novo pacote)
│   │   ├── ProfileResource.java      : adição
│   │   └── ProfileResourcePk.java    : adição (PK composta)
│   └── integration/                  : adição (novo pacote)
│       ├── IntegrationKeycloak.java  : adição
│       ├── IntegrationKeycloakLog.java: adição
│       └── IntegrationMessageInvalid.java: adição
└── repository/
    ├── company/
    │   ├── CompanyRepository.java    : modificação (StatusCompany.DELETED → DISABLED)
    │   ├── CompanyAddressRepository.java: modificação (PK Long → CompanyAddressPk)
    │   └── CompanyContactRepository.java: modificação (PK coluna corrigida)
    ├── system/                       : adição
    │   ├── SystemRepository.java     : adição
    │   └── ResourceRepository.java   : adição
    ├── permission/                   : adição
    │   └── ProfileResourceRepository.java: adição
    └── integration/                  : adição
        ├── IntegrationKeycloakRepository.java   : adição
        ├── IntegrationKeycloakLogRepository.java: adição
        └── IntegrationMessageInvalidRepository.java: adição
```

### Divergências Detalhadas por Entidade

#### Company
| Situação | Campo no Model | Coluna no Banco |
|---|---|---|
| Remover | `dateCreated` → `DATE_CREATED` | não existe no banco |
| Remover | `active` → `ACTIVE` | não existe no banco |
| Corrigir enum | Apenas `DISABLED` | `StatusCompany` deve ter `ACTIVE`, `INACTIVE`, `DISABLED`, `DELETED` |

**Semântica de status**: `DISABLED` = desativado, pode ser reativado. `DELETED` = soft delete permanente, não pode retornar a `ACTIVE`/`INACTIVE`.  
**Impacto em métodos**: `isActive()`, `inactivate()`, `activate()`, `delete()` → remover refs a `active`, `disable()` para desativar, `delete()` para deleção lógica. Guards de `activate()`/`inactivate()` bloqueiam tanto `DISABLED` quanto `DELETED`.

#### Employee
| Situação | Campo no Model | Coluna no Banco |
|---|---|---|
| Remover | `dateCreated` → `DATE_CREATED` | não existe no banco |
| Remover | `active` → `ACTIVE` | não existe no banco |
| Adicionar | ausente | `STATUS` VARCHAR(20) → `StatusEmployee` (ACTIVE/INACTIVE/DISABLED/DELETED) |

**Impacto em métodos**: Adicionar métodos `activate()`, `inactivate()`, `disable()`, `delete()` baseados em `StatusEmployee`. Guards bloqueiam transições a partir de `DISABLED` e `DELETED`.

#### Login
| Situação | Campo no Model | Coluna no Banco |
|---|---|---|
| Remover | `password` → `PASSWORD` | não existe no banco |
| Remover | `salt` → `SALT` | não existe no banco |
| Remover | `dateCreated` → `DATE_CREATED` | não existe no banco |
| Remover | `dateLastChangePassword` → `DATE_LAST_CHANGE_PASSWORD` | não existe no banco |
| Adicionar | ausente | `TYPE` VARCHAR(50) → `LoginType` |

#### Profile
| Situação | Campo no Model | Coluna no Banco |
|---|---|---|
| Remover | `features` → `FEATURES` (text[]) | não existe no banco |

#### CompanyContact
| Situação | Campo no Model | Coluna no Banco |
|---|---|---|
| Corrigir PK | `CONTACT_ID` | deve ser `COMPANY_ID_CONTACT` |
| Adicionar | ausente no model | `RESPONSIBLE_PERSON` VARCHAR(255) NOT NULL — responsável pelo contato |
| Corrigir mapeamento | `email` como `String` | deve usar VO `Email` com `@Embedded @AttributeOverride` |

#### CompanyAddress
| Situação | Campo no Model | Coluna no Banco |
|---|---|---|
| Reestruturar PK | PK simples auto-gen `COMPANY_ADDRESS_ID` | PK composta: `COMPANY_ID_ADDRESS` (externo, sem auto-gen) + `COMPANY_ID` (FK) |
| Criar | — | `CompanyAddressPk` com `companyIdAddress` + `companyId` |

**Impacto no repositório**: `BaseJpaRepository<CompanyAddress, Long>` → `BaseJpaRepository<CompanyAddress, CompanyAddressPk>`. Métodos `findCompanyContactByCompany` e `deleteByCompanyIdAndId` precisam ser revisados.

#### EmployeeContact
| Situação | Campo no Model | Coluna no Banco |
|---|---|---|
| Corrigir PK | `CONTACT_ID` | deve ser `EMPLOYEE_ID_CONTACT` |

#### EmployeeAddressPk
| Situação | Campo no Model | Coluna no Banco |
|---|---|---|
| Corrigir | `addressId` → `ADDRESS_ID` | deve ser `employeeIdAddress` → `EMPLOYEE_ID_ADDRESS` |

#### PartnersConfiguration (SCOS_CONFIGURATION)
| Situação | Campo no Model | Coluna no Banco |
|---|---|---|
| Corrigir PK | BIGINT auto-gen | VARCHAR(50) não auto-gen (`CONFIGURATION_ID`) |
| Remover | `key` → `CONFIGURATION_KEY` | não existe no banco |
| Remover | `description` → `DESCRIPTION` | não existe no banco |
| Remover | `defaultValue` → `DEFAULT_VALUE` | não existe no banco |
| Adicionar | ausente | `VALUE` TEXT |
| Adicionar | ausente | `TYPE` VARCHAR(50) (STRING/INTEGER/BOOLEAN/JSON) |

#### Entidades Ausentes (criar do zero)

**System** → `SCOS_SYSTEM`
- UUID PK, CODE (UK), DESCRIPTION, SECRET_KEY (TEXT), STATUS (ACTIVE/INACTIVE)

**Resource** → `SCOS_RESOURCE`  
- UUID PK, SYSTEM_ID (FK), CODE (UK), DESCRIPTION, ACTIVE

**ProfileResource** → `SCOS_PROFILE_RESOURCE`
- PK composta: PROFILE_ID (FK BIGINT) + RESOURCE_ID (FK UUID)
- Sem UPDATED_AT (vínculo imutável — criado ou removido)

**IntegrationKeycloak** → `SCOS_INTEGRATION_KEYCLOAK`
- UUID PK, KEYCLOAK_ID (UUID null), REALM, EMAIL, USERNAME, REQUESTING
- TYPE (CREATE/UPDATE/DELETE/RESET_PASSWORD), STATUS (PENDING/PROCESSING/SUCCESS/ERROR)
- DATE_START, DATE_END (TIMESTAMPTZ), MESSAGE, REQUEST (JSONB)
- RETRY_COUNT (INT default 0), MAX_RETRIES (INT default 3)

**IntegrationKeycloakLog** → `SCOS_INTEGRATION_KEYCLOAK_LOG`
- UUID PK, INTEGRATION_KEYCLOAK_ID (FK), SUCCESS (BOOLEAN), RESPONSE
- Tabela imutável — sem UPDATED_AT

**IntegrationMessageInvalid** → `SCOS_INTEGRATION_MESSAGE_INVALID`
- UUID PK, MESSAGE_INVALID (VARCHAR 5000)
- Tabela imutável — sem UPDATED_AT

### Banco de Dados
- **Impacto**: ❌ Não — nenhuma migration nova. Apenas código Java.

---

## 4️⃣ Implementação

### Arquivos

**Novos**:
- `model/company/CompanyAddressPk.java` — embeddable para PK composta de CompanyAddress
- `model/employee/StatusEmployee.java` — enum ACTIVE/INACTIVE/DISABLED
- `model/login/LoginType.java` — enum EMPLOYEE/EXTERNAL/SERVICE
- `model/system/System.java` — entidade SCOS_SYSTEM
- `model/system/Resource.java` — entidade SCOS_RESOURCE
- `model/permission/ProfileResource.java` — entidade SCOS_PROFILE_RESOURCE
- `model/permission/ProfileResourcePk.java` — embeddable PK composta
- `model/integration/IntegrationKeycloak.java` — entidade SCOS_INTEGRATION_KEYCLOAK
- `model/integration/IntegrationKeycloakLog.java` — entidade SCOS_INTEGRATION_KEYCLOAK_LOG
- `model/integration/IntegrationMessageInvalid.java` — entidade SCOS_INTEGRATION_MESSAGE_INVALID
- `repository/system/SystemRepository.java` — porta de saída para SCOS_SYSTEM
- `repository/system/ResourceRepository.java` — porta de saída para SCOS_RESOURCE
- `repository/permission/ProfileResourceRepository.java` — porta de saída para SCOS_PROFILE_RESOURCE
- `repository/integration/IntegrationKeycloakRepository.java` — porta de saída para SCOS_INTEGRATION_KEYCLOAK
- `repository/integration/IntegrationKeycloakLogRepository.java` — porta de saída para SCOS_INTEGRATION_KEYCLOAK_LOG
- `repository/integration/IntegrationMessageInvalidRepository.java` — porta de saída para SCOS_INTEGRATION_MESSAGE_INVALID

**Modificados**:
- `model/company/Company.java` — remover dateCreated/active, ajustar métodos de negócio
- `model/company/CompanyAddress.java` — PK simples → PK composta (CompanyAddressPk)
- `model/company/CompanyContact.java` — corrigir nome coluna PK, remover responsiblePerson
- `model/company/StatusCompany.java` — DELETED → DISABLED, ajustar displayName
- `model/employee/Employee.java` — remover dateCreated/active, adicionar status (StatusEmployee)
- `model/employee/EmployeeAddressPk.java` — renomear addressId → employeeIdAddress / ADDRESS_ID → EMPLOYEE_ID_ADDRESS
- `model/employee/EmployeeContact.java` — corrigir nome coluna PK
- `model/login/Login.java` — remover password/salt/dateCreated/dateLastChangePassword, adicionar type (LoginType)
- `model/login/Profile.java` — remover features
- `model/configuration/PartnersConfiguration.java` — reestruturação completa do PK e campos
- `repository/company/CompanyRepository.java` — StatusCompany.DELETED → DISABLED em todos os predicados
- `repository/company/CompanyAddressRepository.java` — PK Long → CompanyAddressPk, revisar métodos
- `repository/company/CompanyContactRepository.java` — revisar após correção de PK

### Tarefas
- [ ] **T-01**: Corrigir `StatusCompany` (DELETED → DISABLED) e ajustar `Company` + repositórios dependentes
- [ ] **T-02**: Refatorar `Company` — remover campos/métodos obsoletos
- [ ] **T-03**: Refatorar `Employee` — remover campos obsoletos, adicionar `StatusEmployee` e métodos
- [ ] **T-04**: Refatorar `Login` — remover campos obsoletos, adicionar `LoginType`
- [ ] **T-05**: Refatorar `Profile` — remover `features`
- [ ] **T-06**: Refatorar `CompanyContact` — corrigir PK, remover `responsiblePerson`
- [ ] **T-07**: Refatorar `CompanyAddress` — criar `CompanyAddressPk`, migrar para PK composta
- [ ] **T-08**: Atualizar `CompanyAddressRepository` para nova PK
- [ ] **T-09**: Corrigir `EmployeeContact` — corrigir nome coluna PK
- [ ] **T-10**: Corrigir `EmployeeAddressPk` — renomear `addressId` → `employeeIdAddress`
- [ ] **T-11**: Reestruturar `PartnersConfiguration` — novo PK VARCHAR(50), campos corretos
- [ ] **T-12**: Criar `System` + `Resource` + `SystemRepository` + `ResourceRepository`
- [ ] **T-13**: Criar `ProfileResource` + `ProfileResourcePk` + `ProfileResourceRepository`
- [ ] **T-14**: Criar `IntegrationKeycloak` + `IntegrationKeycloakLog` + repositórios
- [ ] **T-15**: Criar `IntegrationMessageInvalid` + repositório

### Riscos e Edge Cases
1. `CompanyAddressRepository` usa `qCompanyAddress.id.eq(companyAddressId)` com Long — após mudança para PK composta, o predicado QueryDSL muda. Verificar o Q-type gerado.
2. `deleteByCompanyIdAndId` em `CompanyAddressRepository` e `CompanyContactRepository` — com a mudança de PK, Spring Data pode não conseguir derivar a query automaticamente. Pode ser necessário `@Query` explícita ou `deleteById(new CompanyAddressPk(...))`.
3. `StatusCompany.DELETED` é referenciado em múltiplos predicados nos repositórios — T-01 deve ser feito antes dos demais para não quebrar compilação dos repositórios.
4. `PartnersConfiguration` tem `getValueOrDefaultValue()` que depende de `defaultValue` — remover o campo quebra o método. Simplificar para retornar apenas `value`.
5. `IntegrationKeycloak` usa `JSONB` (PostgreSQL) para `REQUEST` — mapear como `String` ou usar `@Type` do Hypersistence Utils para `JsonBinaryType`.

---

## 📎 Referências
- `etc/database/domain_model.md` — fonte da verdade do schema
- `scos-organization-boot/src/main/resources/db/changelog/v1.0.0/tables/` — migrations Liquibase por tabela
- `openspec/changes/adequacao-liquibase-domain-model/` — change anterior que alinhou o Liquibase ao domain model

---
