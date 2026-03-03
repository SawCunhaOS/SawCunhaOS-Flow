# Implementação das APIs de Company

**Data de Criação**: 2026-02-28 16:00:00  
**Última Atualização**: 2026-02-28 18:00:00  
**Status**: ✅ Aprovado — Pronto para Implementação
**ID da Decisão**: ADR-003
**Tipo**: 🆕 Nova Feature

---

## 1️⃣ Solicitação do Usuário

### Requisição Original
```
Preciso criar a implementação das APIs de empresa.
Crie o plano para essa APIs de acordo com esses documentos
```

### Contexto Adicional
- Contrato OpenAPI já definido em `etc/api/organization/ScosOrganization_Company.yml`
- Regras de negócio documentadas em `etc/docs/company-business-rules.md`
- Diretrizes arquiteturais em `etc/architecture/api-development-guidelines.md`
- Entidades de domínio parcialmente existentes (`Company`, `CompanyContact`, `CompanyAddress`, `StatusCompany`)
- Repositórios base existentes (`CompanyRepository`, `CompanyContactRepository`, `CompanyAddressRepository`)
- DTOs de aplicação parcialmente existentes (`UpdateCompanyDTO`)

### Arquivos Mencionados
- `etc/api/organization/ScosOrganization_Company.yml` — contrato OpenAPI completo
- `etc/docs/company-business-rules.md` — regras de negócio e especificação de UseCases
- `etc/architecture/api-development-guidelines.md` — padrões e convenções de código

### Prioridade
- [ ] 🔴 Crítica (produção quebrada)
- [x] 🟠 Alta (feature bloqueante)
- [ ] 🟡 Média (melhoria importante)
- [ ] 🟢 Baixa (nice to have)

---

## 2️⃣ Objetivo do Desenvolvimento

> **Definido por**: 🔧 @dev-senior

### Problema Identificado
As APIs de Company estão definidas no contrato OpenAPI mas **não possuem implementação**. A ausência dos Use Cases, Domain Services, mappers, DTOs completos e o controller impede que os endpoints de `Company`, `CompanyContact` e `CompanyAddress` sejam funcionais.

**Sintomas**:
- Endpoints gerados pelo OpenAPI sem implementação no delegate
- Ausência de `CompanyApiDelegateImp`
- Ausência de Use Cases para Company, CompanyContact e CompanyAddress
- Ausência de `CompanyDomainService`
- DTOs de aplicação incompletos (apenas `UpdateCompanyDTO` existe)
- Ausência de mappers MapStruct para Company

**Causa Raiz**:
Não foi realizada a implementação (application + api) das entidades de Company que já possuem modelos de domínio e repositórios criados.

### Objetivo Principal
Implementar todas as camadas de aplicação e API para os endpoints de `Company`, `CompanyContact` e `CompanyAddress`, seguindo a Clean Architecture, padrões do projeto e as regras de negócio documentadas.

### Objetivos Secundários
- Garantir cobertura de testes ≥ 80% (unitários + integração por camada)
- Respeitar os códigos de erro já definidos em `ExceptionCodeError`
- Implementar validações de domínio: CNPJ único, hierarquia sem ciclos, regra de matriz
- Auditoria audit-only (sem Kafka, sem Keycloak sync)

### Critérios de Sucesso
- [ ] Todos os 17 endpoints do contrato respondem corretamente
- [ ] Regras de negócio críticas cobertas por testes (CNPJ único, ciclo de parent, regra de matriz)
- [ ] Nenhum breakage nos testes existentes de Department/Position
- [ ] Build `mvn clean install` sem erros

### Não-Objetivos (Out of Scope)
- Implementação do módulo `Company Document` (endpoints comentados no OpenAPI)
- Integração Kafka / Keycloak (política: Company é audit-only)

> ✅ **Confirmado @dba (Rodada 2)**: As migrations **já existem** no DDL v1.0.0:
> - `UK_TAX_IDENTIFIER_SCOS_COMPANY` — constraint UNIQUE full já em `scos_company.yml`
> - `IDX_PARENT_COMPANY_ID_SCOS_COMPANY` — index já em `scos_company.yml`
> Não é necessário criar changelogs adicionais.

---

## 3️⃣ Solução Proposta

> **Proposta inicial**: 🔧 @dev-senior

### 3.1 Abordagem Escolhida

**Nome da Abordagem**: Clean Architecture + OpenAPI Delegate Pattern (idêntico ao Department/Position)

**Descrição**:
Seguir exatamente o padrão já estabelecido no projeto:
1. DTOs de aplicação → Use Cases → Domain Services → Repositórios
2. Controller implementa a interface gerada pelo `openapi-generator-maven-plugin`
3. MapStruct para mapeamento entre camadas
4. Domain Service com as validações de negócio específicas de Company

**Justificativa da Escolha**:
Consistência total com os módulos Department e Position, que servem como referência de implementação conforme as diretrizes em `api-development-guidelines.md`.

### 3.2 Arquitetura da Solução

**Componentes Afetados**:
```
scos-organization-domain
├── NOVO:      domain/service/company/CompanyDomainService.java
├── MODIFICAR: repository/company/CompanyRepository.java        (+ existsAnotherActiveRoot, existsByParentCompanyId)
└── MODIFICAR: repository/company/CompanyAddressRepository.java (+ deleteByCompanyIdAndId)

scos-organization-application
├── NOVO:      dto/CreateCompanyDTO.java
├── NOVO:      dto/CompanyDTO.java
├── NOVO:      dto/CreateCompanyContactDTO.java
├── NOVO:      dto/UpdateCompanyContactCommandDTO.java           (envelope: companyId + contactId + UpdateCompanyContactDTO)
├── NOVO:      dto/DeleteCompanyContactCommandDTO.java           (envelope: companyId + contactId)
├── NOVO:      dto/CompanyContactDTO.java
├── NOVO:      dto/CreateCompanyAddressDTO.java
├── NOVO:      dto/UpdateCompanyAddressCommandDTO.java           (envelope: companyId + addressId + UpdateCompanyAddressDTO)
├── NOVO:      dto/DeleteCompanyAddressCommandDTO.java           (envelope: companyId + addressId)
├── NOVO:      dto/CompanyAddressDTO.java
├── NOVO:      dto/UpdateCompanyContactDTO.java
├── NOVO:      dto/UpdateCompanyAddressDTO.java
├── EXISTENTE: dto/UpdateCompanyDTO.java                        (já existe — não recriar)
├── NOVO:      mapper/company/CompanyMapper.java
├── NOVO:      mapper/company/CompanyContactMapper.java
├── NOVO:      mapper/company/CompanyAddressMapper.java
├── NOVO:      usecase/company/CreateCompanyUseCase.java
├── NOVO:      usecase/company/GetCompanyByIdUseCase.java
├── NOVO:      usecase/company/ListCompaniesUseCase.java
├── NOVO:      usecase/company/UpdateCompanyUseCase.java
├── NOVO:      usecase/company/DeleteCompanyUseCase.java
├── NOVO:      usecase/company/ActivateCompanyUseCase.java
├── NOVO:      usecase/company/InactivateCompanyUseCase.java
├── NOVO:      usecase/company/contact/CreateCompanyContactUseCase.java
├── NOVO:      usecase/company/contact/GetCompanyContactByIdUseCase.java
├── NOVO:      usecase/company/contact/ListCompanyContactsUseCase.java
├── NOVO:      usecase/company/contact/UpdateCompanyContactUseCase.java
├── NOVO:      usecase/company/contact/DeleteCompanyContactUseCase.java
├── NOVO:      usecase/company/address/CreateCompanyAddressUseCase.java
├── NOVO:      usecase/company/address/GetCompanyAddressByIdUseCase.java
├── NOVO:      usecase/company/address/ListCompanyAddressesUseCase.java
├── NOVO:      usecase/company/address/UpdateCompanyAddressUseCase.java
└── NOVO:      usecase/company/address/DeleteCompanyAddressUseCase.java

scos-organization-api
└── NOVO:      controller/company/CompanyApiDelegateImp.java

scos-organization-boot (Liquibase)
└── ✅ SEM NOVOS CHANGELOGS — UK_TAX_IDENTIFIER e IDX_PARENT_COMPANY_ID já existem em v1.0.0
```

**Fluxo da Solução**:
```
HTTP Request
  → CompanyApiDelegateImp (scos-organization-api)
      → [UseCase] (scos-organization-application)
          → CompanyDomainService (scos-organization-domain) [validações]
          → CompanyRepository / CompanyContactRepository / CompanyAddressRepository
              → PostgreSQL
```

### 3.3 Design Patterns Utilizados

| Pattern | Onde | Justificativa |
|---------|------|---------------|
| Delegate Pattern | CompanyApiDelegateImp | Interface gerada pelo openapi-generator |
| Use Case (Command) | usecase/company/ | Isolamento de lógica por operação |
| Domain Service | CompanyDomainService | Validações de negócio multi-entidade |
| QueryDSL Predicate | CompanyRepository | Consultas dinâmicas seguras e typesafe |
| MapStruct | CompanyMapper | Mapeamento automático entre camadas |

### 3.4 Conformidade Arquitetural

**Validado por**: 🏗️ @arquiteto *(aguardando validação)*

#### SOLID
| Princípio | Conformidade | Observação |
|-----------|--------------|------------|
| Single Responsibility | ✅ | Cada UseCase tem responsabilidade única |
| Open/Closed | ✅ | Extensão via novas implementações de interface |
| Liskov Substitution | ✅ | Uso de interfaces ScosBaseUseCase |
| Interface Segregation | ✅ | Cada delegate implementa apenas seu contrato |
| Dependency Inversion | ✅ | Injeção via construtor com @RequiredArgsConstructor |

#### Separação de Camadas
- **Respeitada**: ✅ Sim — sem lógica de negócio na controller, sem acesso a BD no controller

#### Acoplamento & Coesão
- **Acoplamento**: 🟢 Baixo — módulos se comunicam por interfaces e DTOs
- **Coesão**: 🟢 Alta — cada classe tem propósito único e claro

---

## 4️⃣ Detalhamento das Implementações

### 4.1 Arquivos — scos-organization-domain

> **Decisão @arquiteto**: Contact e Address são sub-recursos sem regras complexas. Todas as validações ficam em `CompanyDomainService` (única classe). Eliminar classes separadas para evitar over-engineering.

#### `CompanyDomainService.java` *(NOVO)*
```
Localização: scos-organization-domain/src/main/java/.../domain/service/company/

— Validações de Company —
  validateCompanyExistsValidation(Long companyId)                   → SCOS_COMPANY_001 (404)
  validateFoundationDateValidation(LocalDate date)                  → SCOS-002 (400)
  validateTaxIdentifierUniqueValidation(String cnpjNormalized)      → SCOS_COMPANY_002 (409)
  validateTaxIdentifierUniqueOnUpdateValidation(Long id, String cnpj) → SCOS_COMPANY_002 (409)
  validateParentCompanyExistsAndActiveValidation(Long parentId)     → SCOS_COMPANY_001 (404) + status check
  validateCycleInHierarchyValidation(Long companyId, Long parentId) → SCOS_COMPANY_004 (400)
    /* Percorre ancestrais do parentId até a raiz, max 5 níveis;
       se encontrar companyId → ciclo detectado. A trigger de BD
       cobre APENAS auto-referência direta; ciclos transitivos
       são exclusivamente responsabilidade desta validação. */
  validateCompanyHasDependenciesValidation(Long companyId)          → SCOS_COMPANY_003 (409)
  validateRootCompanyConstraintValidation(Long companyId)           → SCOS_COMPANY_005 (409)
  validateCompanyIsActiveForSubResourceValidation(Long companyId)   → regra de status

— Validações de CompanyContact (agrupadas aqui) —
  validateCompanyContactExistsValidation(Long companyId, Long contactId) → SCOS_COMPANY_006 (404)
    /* usa CompanyContactRepository.findCompanyContactByCompany */

— Validações de CompanyAddress (agrupadas aqui) —
  validateCompanyAddressExistsValidation(Long companyId, Long addressId) → SCOS_COMPANY_006 (404)
    /* usa CompanyAddressRepository.findCompanyContactByCompany */
```

#### `CompanyRepository.java` *(MODIFICAR — métodos adicionais)*
```java
// Verifica se existe filial com esse parentId (para validar dependências no delete)
default boolean existsChildByCompanyId(Long companyId) {
    return exists(company.parentCompany.id.eq(companyId)
                          .and(company.status.ne(StatusCompany.DELETED)));
}

// Verifica se existe outra matriz ativa (regra SCOS_COMPANY_005)
// Já existe companyId para excluir a si própria
// existsByStatus(Long companyId, StatusCompany status) JÁ EXISTE no repositório

// Busca a cadeia de ancestrais para detecção de ciclo
// Implementado iterativamente no domain service (não requer query específica,
// usa findNotDeletedById em loop controlado por MAX_DEPTH=5)
```

#### `CompanyAddressRepository.java` *(MODIFICAR — método ausente)*
```java
// Adicionar à interface (padrão já usado em CompanyContactRepository):
void deleteByCompanyIdAndId(final Long companyId, final Long addressId);
```

### 4.2 Novos Arquivos — scos-organization-application

#### DTOs

> **Decisão @arquiteto (Rodada 2 — CONFIRMADO)**: O padrão do projeto usa DTOs **flat com IDs embutidos** — conforme `UpdatePositionDTO` que contém `positionId` diretamente. **NÃO usar envelope** `{id, data}`. Todos os DTOs que precisam de múltiplos IDs devem tê-los embutidos como campos simples.

> **Decisão @especialista + @dba (Rodada 2 — CONFIRMADO)**: `CompanyAddress.number` é `long` na entidade Java; coluna `NUMBER INT` no DDL (PostgreSQL `INT` = 32-bit). Manter `long`; OpenAPI a corrigir para `integer/int64`. `GEOLOCATION` é `nullable=true` no DDL. `CompanyAddress.geolocation` é `Point` (PostGIS); DTOs recebem `latitude`/`longitude` como `Double` e o mapper converte para `Point` via `new GeometryFactory().createPoint(new Coordinate(longitude, latitude))` (**longitude primeiro** — padrão JTS/WKT confirmado).

| Arquivo | Status | Campos Principais |
|---------|--------|-------------------|
| `CreateCompanyDTO` | NOVO | name, nameTreatment, taxIdentifier (String raw, normalizado pelo UseCase), foundationDate, sectorOfActivity, observation, parentCompanyId |
| `CompanyDTO` | NOVO | id, name, nameTreatment, taxIdentifier, foundationDate, sectorOfActivity, observation, dateCreated, status, parentCompanyId, parentCompanyName, createdAt, updatedAt, createdBy, updatedBy |
| `UpdateCompanyDTO` | **EXISTENTE — MODIFICAR** | Adicionar campo `companyId (Long)` — seguindo padrão `UpdatePositionDTO.positionId`. Demais campos: name, nameTreatment, foundationDate, sectorOfActivity, observation, parentCompanyId *(status removido — não muda via PUT base)* |
| `CreateCompanyContactDTO` | NOVO | companyId, type, phone, email, responsiblePerson |
| `UpdateCompanyContactDTO` | NOVO | **Flat com IDs**: companyId, contactId, type, phone, email, responsiblePerson |
| `DeleteCompanyContactDTO` | NOVO | **Flat com IDs**: companyId, contactId |
| `CompanyContactDTO` | NOVO | id, companyId, type, phone, email, responsiblePerson |
| `GetCompanyContactDTO` | NOVO | **Flat com IDs**: companyId, contactId |
| `ListCompanyContactsDTO` | NOVO | **Flat**: companyId, pageable |
| `CreateCompanyAddressDTO` | NOVO | companyId, type, number (long), complement, latitude (Double, nullable), longitude (Double, nullable) |
| `UpdateCompanyAddressDTO` | NOVO | **Flat com IDs**: companyId, addressId, type, number (long), complement, latitude (Double, nullable), longitude (Double, nullable) |
| `DeleteCompanyAddressDTO` | NOVO | **Flat com IDs**: companyId, addressId |
| `GetCompanyAddressDTO` | NOVO | **Flat com IDs**: companyId, addressId |
| `ListCompanyAddressesDTO` | NOVO | **Flat**: companyId, pageable |
| `CompanyAddressDTO` | NOVO | id, companyId, type, number (long), complement, latitude (Double), longitude (Double) |

#### Mappers (MapStruct)
| Arquivo | Status | Mapeamentos |
|---------|--------|-------------|
| `CompanyMapper` | NOVO | `CreateCompanyDTO → Company` (taxIdentifier normalizado → `new Cnpj(cnpj)`), `Company → CompanyDTO` |
| `CompanyContactMapper` | NOVO | `CreateCompanyContactDTO → CompanyContact`, `CompanyContact → CompanyContactDTO` |
| `CompanyAddressMapper` | NOVO | `CreateCompanyAddressDTO → CompanyAddress` (lat/lon → `new GeometryFactory().createPoint(new Coordinate(lon, lat))`), `CompanyAddress → CompanyAddressDTO` (Point → lat/lon extraídos) |

#### Use Cases — Company

> **Confirmado @arquiteto (Rodada 2)**: Padrão **flat DTO com ID embutido** — igual a `UpdatePositionDTO` que possui `positionId`. `UpdateCompanyDTO` (EXISTENTE) receberá o campo `companyId` adicionado. Campo `status` **removido** de `UpdateCompanyDTO` — status só muda via `/enable` e `/disable`.

| UseCase | Input `ScosBaseUseCase<I,O>` | Output | Regras de Domínio Chamadas |
|---------|------------------------------|--------|---------------------------|
| `CreateCompanyUseCase` | `CreateCompanyDTO` | `Long` | validateFoundationDate, validateTaxIdentifierUnique*(cnpj normalizado)*, validateParentCompanyExistsAndActive, validateCycleInHierarchy |
| `GetCompanyByIdUseCase` | `Long id` | `CompanyDTO` | validateCompanyExists |
| `ListCompaniesUseCase` | `Pageable` | `Page<CompanyDTO>` | — |
| `UpdateCompanyUseCase` | `UpdateCompanyDTO` *(companyId embutido — MODIFICAR existente)* | `Void` | validateCompanyExists, validateParentCompanyExistsAndActive, validateCycleInHierarchy |
| `ActivateCompanyUseCase` | `Long id` | `Void` | validateCompanyExists → chama `company.activate()` |
| `InactivateCompanyUseCase` | `Long id` | `Void` | validateCompanyExists, validateRootCompanyConstraint → chama `company.inactivate()` |
| `DeleteCompanyUseCase` | `Long id` | `Void` | validateCompanyExists, validateCompanyHasDependencies, validateRootCompanyConstraint → chama `company.delete()` |

#### Use Cases — CompanyContact

> **Confirmado @arquiteto (Rodada 2)**: IDs embutidos diretamente no DTO flat — sem envelope.

| UseCase | Input `ScosBaseUseCase<I,O>` | Output | Regras |
|---------|------------------------------|--------|--------|
| `CreateCompanyContactUseCase` | `CreateCompanyContactDTO` (companyId embutido) | `Long` | validateCompanyExists, validateCompanyIsActiveForSubResource |
| `GetCompanyContactByIdUseCase` | `GetCompanyContactDTO` (companyId + contactId embutidos) | `CompanyContactDTO` | validateCompanyContactExists |
| `ListCompanyContactsUseCase` | `ListCompanyContactsDTO` (companyId + pageable embutidos) | `Page<CompanyContactDTO>` | validateCompanyExists |
| `UpdateCompanyContactUseCase` | `UpdateCompanyContactDTO` (companyId + contactId + campos embutidos) | `Void` | validateCompanyContactExists |
| `DeleteCompanyContactUseCase` | `DeleteCompanyContactDTO` (companyId + contactId embutidos) | `Void` | validateCompanyContactExists |

#### Use Cases — CompanyAddress

> **Confirmado @especialista + @dba (Rodada 2)**: IDs embutidos diretamente no DTO flat. `GEOLOCATION nullable=true` no DDL — campos `latitude`/`longitude` são `@Nullable Double`. Conversão via `new GeometryFactory().createPoint(new Coordinate(longitude, latitude))` — **longitude primeiro** (padrão JTS). Se ambos forem `null`, `geolocation = null`.

| UseCase | Input `ScosBaseUseCase<I,O>` | Output | Regras |
|---------|------------------------------|--------|--------|
| `CreateCompanyAddressUseCase` | `CreateCompanyAddressDTO` (companyId embutido) | `Long` | validateCompanyExists, validateCompanyIsActiveForSubResource |
| `GetCompanyAddressByIdUseCase` | `GetCompanyAddressDTO` (companyId + addressId embutidos) | `CompanyAddressDTO` | validateCompanyAddressExists |
| `ListCompanyAddressesUseCase` | `ListCompanyAddressesDTO` (companyId + pageable embutidos) | `Page<CompanyAddressDTO>` | validateCompanyExists |
| `UpdateCompanyAddressUseCase` | `UpdateCompanyAddressDTO` (companyId + addressId + campos embutidos) | `Void` | validateCompanyAddressExists |
| `DeleteCompanyAddressUseCase` | `DeleteCompanyAddressDTO` (companyId + addressId embutidos) | `Void` | validateCompanyAddressExists |

### 4.3 Novos Arquivos — scos-organization-api

#### `CompanyApiDelegateImp.java`
```
Localização: scos-organization-api/src/main/java/.../api/controller/company/
Implementa: CompanyApiDelegate (gerado pelo openapi-generator)
Use Cases injetados: todos os 17 use cases listados acima

Endpoints mapeados:
  GET    /v1/company                          → ListCompaniesUseCase
  POST   /v1/company                          → CreateCompanyUseCase
  GET    /v1/company/{id}                     → GetCompanyByIdUseCase
  PUT    /v1/company/{id}                     → UpdateCompanyUseCase
  DELETE /v1/company/{id}                     → DeleteCompanyUseCase
  PUT    /v1/company/{id}/enable              → ActivateCompanyUseCase
  PUT    /v1/company/{id}/disable             → InactivateCompanyUseCase
  GET    /v1/company/{companyId}/contact      → ListCompanyContactsUseCase
  POST   /v1/company/{companyId}/contact      → CreateCompanyContactUseCase
  GET    /v1/company/{companyId}/contact/{id} → GetCompanyContactByIdUseCase
  PUT    /v1/company/{companyId}/contact/{id} → UpdateCompanyContactUseCase
  DELETE /v1/company/{companyId}/contact/{id} → DeleteCompanyContactUseCase
  GET    /v1/company/{companyId}/address      → ListCompanyAddressesUseCase
  POST   /v1/company/{companyId}/address      → CreateCompanyAddressUseCase
  GET    /v1/company/{companyId}/address/{id} → GetCompanyAddressByIdUseCase
  PUT    /v1/company/{companyId}/address/{id} → UpdateCompanyAddressUseCase
  DELETE /v1/company/{companyId}/address/{id} → DeleteCompanyAddressUseCase
```

---

## 5️⃣ Regras de Negócio Críticas — Detalhamento

### 5.1 Unicidade de CNPJ (`SCOS_COMPANY_002`)
- `CreateCompanyUseCase` **normaliza o CNPJ** (remove `.`, `/`, `-`) antes de chamar `existsByTaxIdentifier`. Isso garante consistência com a trigger `trg_remove_formatting_tax_identifier_company` que normaliza no BD — sem isso, CNPJs formatados distintos mas numericamente iguais passariam na checagem Java.
- A normalização ocorre no Use Case (não na API) para manter a lógica de negócio na camada correta.
- No **update**, o campo `taxIdentifier` é completamente ignorado — qualquer tentativa de mudança deve retornar 422 `SCOS_COMPANY_006`.
- **Padrão de normalização**: `cnpj.replaceAll("[^0-9]", "")`
- A integridade no BD é garantida pelo `UNIQUE INDEX` (ver Fase 0 — Migrations).

### 5.2 Hierarquia sem Ciclos (`SCOS_COMPANY_004`)
- A trigger `check_company_parent_company` no BD **cobre somente auto-referência direta** (`A.parentId = A.id`). Ciclos transitivos (A→B→C→A) **não são bloqueados pelo BD** — obrigatoriamente implementados na aplicação.
- **Algoritmo em `CompanyDomainService.validateCycleInHierarchyValidation`**:
  ```
  current = newParentId
  depth = 0
  MAX_DEPTH = 5
  while (current != null AND depth < MAX_DEPTH):
    parent = companyRepository.findNotDeletedById(current)
                .orElseThrow(SCOS_COMPANY_001)
    if (parent.id == companyId) → throw SCOS_COMPANY_004
    current = parent.parentCompany?.id
    depth++
  ```
- Complexidade: O(n) onde n ≤ MAX_DEPTH (5) — custo fixo e baixo.

### 5.3 Regra da Matriz (`SCOS_COMPANY_005`)
- Uma Company é **matriz** quando `parentCompanyId = null`.
- **Inativar** ou **deletar** uma matriz só é permitido se existir **outra** matriz com `status = ACTIVE`.
- Usar `CompanyRepository.existsByStatus(companyId, StatusCompany.ACTIVE)` para verificar.

### 5.4 Regra de Dependências (`SCOS_COMPANY_003`)
- **Delete** deve verificar: `existsByParentCompanyId(id)` (filiais), employees ativos, departments ativos.
- Se existirem → soft-delete (`status = DELETED`), não pode hard-delete.

### 5.5 Data de Fundação — Não Futura (`SCOS-002`)
- `foundationDate` deve ser ≤ `LocalDate.now()`.

### 5.6 Company Inativa Bloqueia Recursos Dependentes
- `POST /v1/company/{companyId}/contact` deve verificar se a company está `ACTIVE`.
- `POST /v1/company/{companyId}/address` idem.

---

## 6️⃣ Testes

### 6.1 Estrutura de Testes

#### Unitários — scos-organization-domain
- `CompanyDomainServiceTest` — cobrir todos os métodos de validação (happy path + cada exceção)
- `CompanyTest` — métodos de domínio da entidade (`activate`, `deactivate`, `softDelete`)

#### Unitários — scos-organization-application
- `CreateCompanyUseCaseTest` — incluir: CNPJ duplicado, foundationDate futura, parentCompany inativa, ciclo
- `UpdateCompanyUseCaseTest` — CNPJ imutável, company não encontrada, ciclo
- `DeleteCompanyUseCaseTest` — conflito por dependências, regra da matriz
- `ActivateCompanyUseCaseTest` / `InactivateCompanyUseCaseTest` — transições de status
- `CreateCompanyContactUseCaseTest`
- `CreateCompanyAddressUseCaseTest`

#### Integração — scos-organization-application (TestContainers)
- `CreateCompanyUseCaseIT` — fluxo completo de criação
- `CompanyHierarchyIT` — criação de matrix + filial, detecção de ciclo

#### API — scos-organization-api (MockMvc ou Spring Boot Test)
- `CompanyApiDelegateImpTest` — verificar mapeamento de request/response para cada endpoint

### 6.2 Padrão de Testes
Todos os testes devem seguir o padrão `Given / When / Then` com AssertJ.

---

## 7️⃣ Impactos da Alteração

### 7.1 Módulos Afetados
| Módulo | Tipo de Impacto | Severidade | Descrição |
|--------|----------------|------------|-----------|
| `scos-organization-domain` | Adição | 🟢 | Novo CompanyDomainService |
| `scos-organization-application` | Adição | 🟡 | ~28 novos arquivos (DTOs, mappers, use cases) |
| `scos-organization-api` | Adição | 🟢 | Novo CompanyApiDelegateImp |
| `scos-organization-infrastructure` | Verificação | 🟢 | Confirmar se InfraRepository já existe |
| Testes existentes | Neutro | 🟢 | Sem alteração em Department/Position |

### 7.2 Breaking Changes
- **Identificados**: ❌ Não — apenas adição de novos componentes

### 7.3 Banco de Dados
- **Impacto de schema**: ❌ Não — tabelas `SCOS_COMPANY`, `SCOS_COMPANY_CONTACT`, `SCOS_COMPANY_ADDRESS` já existem
- **Confirmado @dba (Rodada 2)**:
  - PK de `SCOS_COMPANY_ADDRESS`: `COMPANY_ADDRESS_ID` BIGINT via `SEQ_COMPANY_ADDRESS_ID` *(PUML estava errado; Java correto)*
  - `UK_TAX_IDENTIFIER_SCOS_COMPANY`: constraint UNIQUE full já existe *(sem `WHERE` parcial — CNPJs de empresas deletadas bloqueiam reutilização)*
  - `IDX_PARENT_COMPANY_ID_SCOS_COMPANY`: index já existe em v1.0.0
  - `GEOLOCATION`: `nullable=true` no DDL — lat/lon opcionais
  - `NUMBER`: coluna `INT` no DDL; Java usa `long` (mapeamento funcional para valores ≤ 2^31)

### 7.4 Checklist de Implementação
- [ ] `CompanyDomainService` com todos os métodos de validação
- [ ] `CompanyContactDomainService`
- [ ] `CompanyAddressDomainService`
- [ ] Revisão de `CompanyRepository` (métodos adicionais)
- [ ] Revisão de `CompanyContactRepository`
- [ ] Revisão de `CompanyAddressRepository`
- [ ] `CreateCompanyDTO` e demais DTOs (7 novos)
- [ ] `CompanyMapper`, `CompanyContactMapper`, `CompanyAddressMapper`
- [ ] 7 Use Cases de Company
- [ ] 5 Use Cases de CompanyContact
- [ ] 5 Use Cases de CompanyAddress
- [ ] `CompanyApiDelegateImp` (17 endpoints)
- [ ] Testes unitários (≥ 80% cobertura)
- [ ] Testes de integração
- [ ] Build `mvn clean install` passando

---

## 8️⃣ Ordem de Implementação (TDD)

> Seguindo o fluxo: Teste → Domínio → Application → API

> ✅ **Confirmado @dba (Rodada 2)**: Fase 0 de Migrations **eliminada** — `UK_TAX_IDENTIFIER_SCOS_COMPANY` e `IDX_PARENT_COMPANY_ID_SCOS_COMPANY` já existem no DDL v1.0.0. Sem changelogs novos necessários.

### Fase 1 — Domain Layer
1. *(Entidade `Company` já possui todos os métodos necessários — apenas confirmar completude)*
2. Adicionar `existsChildByCompanyId` em `CompanyRepository`
3. Adicionar `deleteByCompanyIdAndId` em `CompanyAddressRepository`
4. Criar `CompanyDomainService` *(único, consolida validações de Company + Contact + Address)*
5. **Testes unitários** do `CompanyDomainService` (happy path + todas as exceções)

### Fase 2 — Application Layer
6. Adicionar `companyId` em `UpdateCompanyDTO` *(MODIFICAR existente — padrão flat DTO)*
7. Criar DTOs: `CreateCompanyDTO`, `CompanyDTO` e demais DTOs de Contact e Address (flat com IDs embutidos)
8. Criar `CompanyMapper`, `CompanyContactMapper`, `CompanyAddressMapper` *(com conversão lat/lon ↔ Point)*
9. Criar Use Cases de Company (Create, Get, List, Update, Delete, Activate, Inactivate)
10. Criar Use Cases de CompanyContact (Create, Get, List, Update, Delete)
11. Criar Use Cases de CompanyAddress (Create, Get, List, Update, Delete)
12. **Testes unitários** dos use cases

### Fase 3 — API Layer
13. Criar `CompanyApiDelegateImp` com todos os 17 endpoints
14. **Testes de API** (MockMVC)

### Fase 4 — Integração e Validação
15. Testes de integração (TestContainers com PostGIS — imagem `postgis/postgis:18-3.5`)
16. Executar `mvn clean install` e garantir build verde

---

## 9️⃣ Revisado Por

### 9.1 Validações dos Agentes

#### 🔧 Dev Senior
- **Data**: 2026-02-28 18:00
- **Decisão**: ✅ Plano finalizado com todas as correções das Rodadas 1 e 2
- **Comentários**:
  - Fase 0 (Migrations) eliminada — ambos os artefatos já existem no DDL v1.0.0
  - Padrão flat DTO confirmado e aplicado — sem envelope, IDs embutidos
  - `UpdateCompanyDTO` (EXISTENTE) será modificado para adicionar `companyId`
  - GEOLOCATION `nullable=true` no DDL — lat/lon opcionais nos DTOs
  - `new Coordinate(longitude, latitude)` — longitude primeiro (padrão JTS/WKT confirmado)
  - PK de `SCOS_COMPANY_ADDRESS` confirmada como `COMPANY_ADDRESS_ID` (PUML era inconsistente)
  - Fases renumeradas: 4 fases (Domínio → Application → API → Integração)

---

#### 🏗️ Arquiteto
- **Data**: 2026-02-28 16:30 (Rodada 1) | 2026-02-28 18:00 (Rodada 2)
- **Decisão**: ✅ **APROVADO**
- **Itens validados Rodada 2**:
  - ✅ Padrão **flat DTO com IDs embutidos** confirmado — consistente com `UpdatePositionDTO.positionId`
  - ✅ `UpdateCompanyDTO` (EXISTENTE) será modificado para adicionar `companyId` — sem envelope
  - ✅ `ScosBaseUseCase<UpdateCompanyDTO, Void>` — compatível com a interface do projeto
  - ✅ Nomes dos DTOs flat ajustados (`UpdateCompanyContactDTO` inclui companyId + contactId embutidos)

---

#### 🎧 Especialista
- **Data**: 2026-02-28 16:30 (Rodada 1) | 2026-02-28 18:00 (Rodada 2)
- **Decisão**: ✅ **APROVADO**
- **Itens validados Rodada 2**:
  - ✅ Conversão JTS: `new Coordinate(longitude, latitude)` — **longitude primeiro** é a ordem correta (x=lon, y=lat no PostGIS/WKT)
  - ✅ `GEOLOCATION nullable=true` — lat/lon como `@Nullable Double`; se ambos null, salvar `null` em `Point`
  - ✅ Todos os pontos de risco técnico documentados e resolvidos

---

#### 🗄️ DBA
- **Data**: 2026-02-28 16:30 (Rodada 1) | 2026-02-28 18:00 (Rodada 2)
- **Decisão**: ✅ **APROVADO**
- **Itens confirmados Rodada 2**:
  - ✅ PK de `SCOS_COMPANY_ADDRESS`: `COMPANY_ADDRESS_ID` BIGINT via `SEQ_COMPANY_ADDRESS_ID` *(inspecionado `scos_company_address.yml`)* — PUML estava inconsistente; entidade Java estava correta
  - ✅ `UK_TAX_IDENTIFIER_SCOS_COMPANY` **já existe** como constraint UNIQUE **sem filtro partial** — CNPJs de empresas DELETED **não podem ser reutilizados** (comportamento atual aceito; conversão para partial index seria uma decisão de negócio futura, não escopo desta implementação)
  - ✅ `IDX_PARENT_COMPANY_ID_SCOS_COMPANY` **já existe** em `scos_company.yml` — Fase 0 de migrations eliminada
  - ✅ `GEOLOCATION`: `nullable: true` no DDL — endereços sem coordenadas são suportados
  - ⚠️ **Observação**: `NUMBER INT` no DDL vs `long` no Java — mapeamento funcional para valores de número de imovél (max ~2 bilhões); sem impacto operacional

---

### 9.2 Decisão Final Consolidada

**Consolidado por**: 🔧 @dev-senior  
**Data**: 2026-02-28 18:00  
**Status Final**: ✅ **APROVADO PARA IMPLEMENTAÇÃO**

#### Resumo das Validações
| Agente | Rodada 1 | Rodada 2 | Status |
|--------|----------|----------|--------|
| Dev Senior | ✅ | ✅ | Aprovado |
| Arquiteto | ⚠️ | ✅ | Aprovado |
| Especialista | ⚠️ | ✅ | Aprovado |
| DBA | ⚠️ | ✅ | Aprovado |

### 9.3 Todos os Ajustes (Status Final)

| # | Ajuste | Status |
|---|--------|--------|
| 1 | Aprovação explícita do usuário | 🔄 Aguardando |
| 2 | Domain Services consolidados em `CompanyDomainService` | ✅ Resolvido |
| 3 | Padrão flat DTO com IDs embutidos (sem envelope) | ✅ Resolvido |
| 4 | `UpdateCompanyDTO` marcado como EXISTENTE — adicionar `companyId` | ✅ Resolvido |
| 5 | Migrations: `UK_TAX_IDENTIFIER` e `IDX_PARENT_COMPANY_ID` já existem | ✅ Resolvido |
| 6 | PK de `SCOS_COMPANY_ADDRESS`: `COMPANY_ADDRESS_ID` autoincrement | ✅ Resolvido |
| 7 | Tipo `NUMBER`: mantido como `long` na Java; coluna `INT` no DDL | ✅ Resolvido |
| 8 | DTOs address: lat/lon como `@Nullable Double`, mapper converte para `Point` (lon primeiro) | ✅ Resolvido |
| 9 | Normalização CNPJ no Use Case: `replaceAll("[^0-9]", "")` | ✅ Resolvido |
| 10 | `deleteByCompanyIdAndId` em `CompanyAddressRepository` | ✅ Resolvido |
| 11 | Fase 0 eliminada — ambas as migrations já existem no DDL v1.0.0 | ✅ Resolvido |
| 12 | `GEOLOCATION nullable=true` no DDL — campos lat/lon opcionais | ✅ Resolvido |
| 13 | Conversão JTS: `new Coordinate(longitude, latitude)` — longitude primeiro | ✅ Resolvido |

---

## ✅ Checklist de Implementação

### Pré-Implementação
- [ ] Aprovação explícita do usuário — **[PONTO DE PARADA]**

### Fase 1 — Domain Layer
- [ ] `CompanyRepository` adicionado: `existsChildByCompanyId`
- [ ] `CompanyAddressRepository` adicionado: `deleteByCompanyIdAndId`
- [ ] `CompanyDomainService` criado com todos os métodos de validação
- [ ] Testes unitários de `CompanyDomainService` (≥ 80%)

### Fase 2 — Application Layer
- [ ] `UpdateCompanyDTO` modificado: adicionar campo `companyId`
- [ ] DTOs de Company criados (`CreateCompanyDTO`, `CompanyDTO`)
- [ ] DTOs de Contact criados (flat com IDs embutidos)
- [ ] DTOs de Address criados (flat com IDs embutidos; `number` como `long`; lat/lon como `@Nullable Double`)
- [ ] Mappers criados (com conversão lat/lon → Point e Point → lat/lon)
- [ ] Use Cases de Company criados e testados (7 use cases)
- [ ] Use Cases de CompanyContact criados e testados (5 use cases)
- [ ] Use Cases de CompanyAddress criados e testados (5 use cases)

### Fase 3 — API Layer
- [ ] `CompanyApiDelegateImp` criado (17 endpoints)
- [ ] Testes de API (MockMVC)

### Fase 4 — Integração e Validação
- [ ] Testes de integração (TestContainers com PostGIS — imagem `postgis/postgis:18-3.5`)
- [ ] `mvn clean install` passando
- [ ] Cobertura ≥ 80%

---

**Fim do Documento**
