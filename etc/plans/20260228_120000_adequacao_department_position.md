# Adequação do Fluxo Department & Position — Conformidade com Padrões do Projeto

**Data de Criação**: 2026-02-28 12:00:00  
**Status**: ⚠️ 2ª Rodada Aprovada c/ Ressalvas Incorporadas  
**ID da Decisão**: ADR-003  
**Tipo**: 🔧 Refatoração

---

## 1️⃣ Solicitação do Usuário

### Requisição Original
```
Preciso que seja revisado a estrutura do projeto e validado se ele segue o padrão estabelecido.
Principalmente para o fluxo de Department e Position.
Após isso crie um plano de adequação.
```

### Arquivos Mencionados
- `etc/architecture/api-development-guidelines.md` — diretrizes oficiais
- `etc/docs/department-position-business-rules.md` — regras de negócio
- `etc/database/department-position.puml` — modelo de banco de dados

### Prioridade
- [x] 🟡 Média (melhoria importante — inconsistências estruturais com os padrões)

---

## 2️⃣ Objetivo do Desenvolvimento

> **Definido por**: 🔧 @dev-senior

### Problema Identificado
Após revisão completa do fluxo Department & Position, foram identificadas **9 categorias de não-conformidades** em relação às diretrizes em `etc/architecture/api-development-guidelines.md` e às regras de negócio em `etc/docs/department-position-business-rules.md`.

**Sintomas**:
- Entidades sem campo `active` nem anotação `@Auditable`
- Changelogs do Liquibase sem coluna `ACTIVE` e com tamanhos de campo incorretos
- `PositionDomainService` não valida se o Department referenciado está **ativo**
- DTOs de resposta não expõem o campo `active`
- `DepartmentApiDelegateImp` usa enum de ordenação de Position (`PositionOrder`) em vez de um enum próprio
- `DeleteDepartmentUseCase` não implementa `ScosBaseUseCase`
- Colunas `@Column` sem `nullable = false` nas entidades

**Causa Raiz**:
Implementação incremental sem checklist de conformidade completa. Campos de ciclo de vida (`active`) e contrato de dados nunca foram adicionados.

### Objetivo Principal
Corrigir todas as não-conformidades para que o fluxo Department & Position esteja 100% alinhado com os padrões arquiteturais e regras de negócio definidos.

### Objetivos Secundários
- Garantir consistência entre entidades, banco de dados, DTOs e contrato OpenAPI
- Adicionar validação de `active` na regra de referência de Department por Position
- Padronizar os Use Cases de Delete com `ScosBaseUseCase`
- Criar enum de ordenação próprio para Department

### Critérios de Sucesso
- [ ] Entidades `Department` e `Position` com campo `active`, `@Auditable` e `nullable = false`
- [ ] Changelogs Liquibase com coluna `ACTIVE` e tamanhos corretos de `CODE`/`DESCRIPTION`
- [ ] `PositionDomainService` valida Department ativo antes de criar/atualizar Position
- [ ] `DepartmentDTO` e `PositionDTO` expõem campo `active`
- [ ] `DepartmentApiDelegateImp` usa `DepartmentOrder` (novo enum)
- [ ] `DeleteDepartmentUseCase` implementa `ScosBaseUseCase<Long, Void>`
- [ ] Todos os testes existentes continuam passando após as alterações

### Não-Objetivos (Out of Scope)
- Implementação de endpoints de ativação/inativação (PATCH `/activate`, `/deactivate`) — decisão futura
- Migração de dados existentes (escopo de @dba em execução separada)
- Alterações em outros domínios (Company, Employee, Login)

---

## 3️⃣ Solução Proposta

> **Proposta inicial**: 🔧 @dev-senior  
> **Refinamentos**: 🏗️ @arquiteto | 🎯 @especialista | 🗄️ @dba

### 3.1 Abordagem Escolhida

**Nome da Abordagem**: Adequação Incremental por Camada (Domain → DB → Application → API)

**Descrição**:
Corrigir as não-conformidades camada a camada, da mais interna para a mais externa, garantindo que nenhuma camada superior seja quebrada antes de a inferior estar correta.

**Justificativa da Escolha**:
Segue o princípio Clean Architecture — mudanças no domínio são a fonte de verdade para as camadas superiores.

---

### 3.2 Inventário Completo de Não-Conformidades

#### NC-01 — `Department` sem campo `active` e sem `@Auditable`
| Atributo | Valor |
|----------|-------|
| **Arquivo** | `scos-organization-domain/.../model/department/Department.java` |
| **Severidade** | 🔴 Alta |
| **Referência** | `api-development-guidelines.md` — Estrutura de Entidades; `department-position-business-rules.md` §2.1 |
| **Descrição** | Falta campo `active: boolean (NOT NULL, default=true)`, anotação `@Auditable` na classe, e `nullable = false` nos campos `CODE` e `DESCRIPTION`. Também faltam métodos de domínio `activate()` e `deactivate()` conforme padrão `Company`. |

**Fix necessário**:
```java
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "SCOS_DEPARTMENT")
@Auditable                                          // ADICIONAR
public class Department extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DEPARTMENT_ID")
    private Long id;

    @Column(name = "CODE", nullable = false)        // ADICIONAR nullable = false
    private String code;

    @Column(name = "DESCRIPTION", nullable = false) // ADICIONAR nullable = false
    private String description;

    @Builder.Default
    @Column(name = "ACTIVE", nullable = false)      // ADICIONAR campo + coluna
    private boolean active = true;

    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    private Set<Position> positions;

    // ADICIONAR métodos de domínio — preparados para futuros endpoints
    // DECISÃO: active = true é o default; operations (UPDATE, GET, DELETE) PERMITIDAS em Department inativo
    public void activate() {
        if (this.active) throw new ScosException(SCOS_DEPARTMENT_004); // "já está ativo"
        this.active = true;
    }

    public void deactivate() {
        if (!this.active) throw new ScosException(SCOS_DEPARTMENT_005); // "já está inativo"
        this.active = false;
    }
}
```

---

#### NC-02 — `Position` sem campo `active` e sem `@Auditable`
| Atributo | Valor |
|----------|-------|
| **Arquivo** | `scos-organization-domain/.../model/department/Position.java` |
| **Severidade** | 🔴 Alta |
| **Referência** | `department-position-business-rules.md` §2.2 |
| **Descrição** | Mesmo problema da NC-01, aplicado a `Position`. |

**Fix necessário**: Adicionar `@Auditable`, `@Builder.Default private boolean active = true`, `nullable = false` nos campos e métodos `activate()` / `deactivate()` com os erros `SCOS_POSITION_004` ("já ativo") e `SCOS_POSITION_005` ("já inativo").

---

#### NC-03 — `CreateDepartmentUseCase` não seta `active = true` na criação
| Atributo | Valor |
|----------|-------|
| **Arquivo** | `scos-organization-application/.../usecase/department/CreateDepartmentUseCase.java` |
| **Severidade** | 🟠 Alta |
| **Referência** | Business rules: `active` default=true |
| **Descrição** | O mapper `toDepartment(CreateDepartmentDTO)` não definirá `active = true` automaticamente. É necessário setar `active = true` explicitamente após mapear, ou garantir que o valor default seja definido na entidade via `@Builder.Default`. |

**Fix necessário**:
```java
Department department = departmentMapper.toDepartment(createDepartmentDTO);
department.setActive(true);  // ADICIONAR
```
> **Alternativa preferida**: Adicionar `@Builder.Default private boolean active = true;` na entidade.

---

#### NC-04 — `CreatePositionUseCase` não seta `active = true` na criação
| Atributo | Valor |
|----------|-------|
| **Arquivo** | `scos-organization-application/.../usecase/position/CreatePositionUseCase.java` |
| **Severidade** | 🟠 Alta |
| **Referência** | Business rules: `active` default=true |
| **Descrição** | Mesmo problema da NC-03, aplicado a `Position`. |

---

#### NC-05 — Changelogs Liquibase sem coluna `ACTIVE` e tamanhos errados
| Atributo | Valor |
|----------|-------|
| **Arquivo** | `scos-organization-boot/.../tables/scos_department.yml` e `scos_position.yml` |
| **Severidade** | 🔴 Crítica (impacto em banco) |
| **Referência** | `department-position-business-rules.md` §2.1 / §2.2; `etc/database/department-position.puml` |
| **Descrição** | Ambas as tabelas não têm coluna `ACTIVE`. Além disso, `DESCRIPTION` está como `varchar(100)` (deveria ser `varchar(500)`) e `CODE` como `varchar(50)` (deveria ser `varchar(30)`). |

**Delegando para @dba**: Validar a estratégia de migração para adicionar coluna `ACTIVE` e alterar tamanhos de campo sem inconsistência de dados.

**Fix necessário** — novo changeSet em ambos:
```yaml
- changeSet:
    id: SGC_2802202600001
    author: Samuel.Cunha
    changes:
      - addColumn:
          tableName: SCOS_DEPARTMENT
          columns:
            - column:
                name: ACTIVE
                type: BOOLEAN
                defaultValueBoolean: true
                constraints:
                  nullable: false
      - modifyDataType:
          tableName: SCOS_DEPARTMENT
          columnName: DESCRIPTION
          newDataType: varchar(500)
      - modifyDataType:
          tableName: SCOS_DEPARTMENT
          columnName: CODE
          newDataType: varchar(30)
    rollback:
      - dropColumn:
          tableName: SCOS_DEPARTMENT
          columnName: ACTIVE
```
*(Mesmo padrão para `SCOS_POSITION`)*

---

#### NC-06 — `PositionDomainService.validateDepartmentExistsValidation` não verifica `active`
| Atributo | Valor |
|----------|-------|
| **Arquivo** | `scos-organization-domain/.../service/department/PositionDomainService.java` |
| **Severidade** | 🔴 Alta (violação de regra de negócio crítica) |
| **Referência** | `department-position-business-rules.md` §4: "departmentId: deve existir e estar **ativo**" |
| **Descrição** | O método usa apenas `departmentRepository.existsById(departmentId)`, sem checar se o Department está ativo. Uma Position pode ser criada com referência a um Department inativo. |

**Decisão arquitetural (A-ARQ-01)**: `PositionDomainService` NÃO injeta `DepartmentRepository` diretamente. Delega para `DepartmentDomainService`.

**Fix necessário**:
1. Adicionar método no `DepartmentRepository`:
```java
default boolean existsByIdAndActive(Long departmentId) {
    BooleanBuilder b = new BooleanBuilder();
    b.and(qDepartment.id.eq(departmentId))
     .and(qDepartment.active.isTrue());
    return exists(b.getValue());
}
```
2. Adicionar método em `DepartmentDomainService` (dono do `DepartmentRepository`) — **implementação em dois passos** (A-ARQ-06):
```java
public void validateDepartmentExistsAndActiveValidation(@NonNull Long departmentId) {
    log.info("Validating department exists and is active: {}", departmentId);
    // Passo 1: garante que existe → SCOS_DEPARTMENT_001 (404) se não houver
    validateDepartmentExistsValidation(departmentId);
    // Passo 2: garante que está ativo → SCOS_DEPARTMENT_006 (422) se inativo
    if (!departmentRepository.existsByIdAndActive(departmentId)) {
        throw new ScosException(ExceptionCodeError.SCOS_DEPARTMENT_006);
    }
}
```

> ⚠️ **A-ARQ-06 (CRÍTICO)**: Sem os dois passos, `existsByIdAndActive()` retorna `false` tanto para Department inexistente quanto para inativo — ambos lançariam `SCOS_DEPARTMENT_006` (422), mas Department inexistente deve lançar `SCOS_DEPARTMENT_001` (404).
3. `PositionDomainService` (B-ESP-02):
   - **Remover** `private final DepartmentRepository departmentRepository` do construtor
   - **Remover** o método `validateDepartmentExistsValidation` local
   - **Adicionar** `private final DepartmentDomainService departmentDomainService`
   - **Adicionar** método delegador:
```java
// PositionDomainService
private final DepartmentDomainService departmentDomainService; // substituiu DepartmentRepository

public void validateDepartmentExistsAndActiveValidation(@NonNull Long departmentId) {
    departmentDomainService.validateDepartmentExistsAndActiveValidation(departmentId);
}
```
4. Atualizar chamadas em `CreatePositionUseCase` e `UpdatePositionUseCase`.

> **Nota**: `SCOS_DEPARTMENT_006` = "Department inativo" (HTTP 422). Diferente de `SCOS_DEPARTMENT_001` = "não encontrado" (HTTP 404). `SCOS_DEPARTMENT_001` continua sendo usado apenas para verificação de existência pura.

---

#### NC-07 — `DepartmentDTO` e `PositionDTO` sem campo `active`
| Atributo | Valor |
|----------|-------|
| **Arquivo** | `scos-organization-application/.../dto/DepartmentDTO.java` e `PositionDTO.java` |
| **Severidade** | 🟡 Média |
| **Referência** | Business rules §2.1 / §2.2 — campo `active` faz parte do contrato de resposta |
| **Descrição** | Os DTOs de resposta não expõem o campo `active`. |

**Fix necessário**: Adicionar `private boolean active;` em ambos os DTOs.

---

#### NC-08 — `DepartmentApiDelegateImp` usa `PositionOrder` em vez de `DepartmentOrder`
| Atributo | Valor |
|----------|-------|
| **Arquivo** | `scos-organization-api/.../controller/department/DepartmentApiDelegateImp.java` |
| **Severidade** | 🟡 Média (contaminação entre domínios) |
| **Referência** | `api-development-guidelines.md` — boas práticas de separação |
| **Descrição** | O método `getAllDepartments` passa `PositionOrder.ID` para `createPageable`, usando o enum de ordenação de outro domínio. |

**Fix necessário**: Criar `DepartmentOrder.java` em `scos-organization-api/.../enumeration/` com valores `ID` e `CODE`, e substituir a referência no controller.

---

#### NC-09 — `DeleteDepartmentUseCase` não implementa `ScosBaseUseCase`
| Atributo | Valor |
|----------|-------|
| **Arquivo** | `scos-organization-application/.../usecase/department/DeleteDepartmentUseCase.java` |
| **Severidade** | 🟡 Média (inconsistência com padrão) |
| **Referência** | `api-development-guidelines.md` — Estrutura de Use Cases |
| **Descrição** | `DeleteDepartmentUseCase` não implementa `ScosBaseUseCase<Long, Void>`, contrário ao padrão dos outros Use Cases (`CreateDepartmentUseCase`, `DeletePositionUseCase`, etc.). |

**Fix necessário**:
```java
public class DeleteDepartmentUseCase implements ScosBaseUseCase<Long, Void> {
    @Override
    public Void execute(@NonNull Long departmentId) { ... }
}
```

---

#### NC-10 — Resposta GET Department/Position não expõe `active` no contrato OpenAPI
| Atributo | Valor |
|----------|-------|
| **Arquivo** | `etc/api/organization/ScosOrganization_Department-Position.yml` |
| **Severidade** | 🟡 Média |
| **Referência** | Business rules — campo `active` é parte da entidade exposta |
| **Descrição** | Os schemas `Department` e `Position` nos responses da OpenAPI não incluem o campo `active`. O controller não propaga o campo `active` do DTO para o response. |

**Fix necessário**: Adicionar `active: boolean` nos schemas OpenAPI e propagar no controller.

---

### 3.3 Arquitetura da Solução

**Componentes Afetados**:
```
scos-organization-domain
├── model/department/Department.java       → +active, +@Auditable, +@Builder.Default, +nullable
├── model/department/Position.java         → +active, +@Auditable, +@Builder.Default, +nullable
├── repository/department/DepartmentRepository.java → +existsByIdAndActive()
└── service/department/PositionDomainService.java  → atualizar validateDepartmentExists*

scos-organization-application
├── dto/DepartmentDTO.java                 → +active
├── dto/PositionDTO.java                   → +active
├── usecase/department/CreateDepartmentUseCase.java → garantir active=true
├── usecase/department/DeleteDepartmentUseCase.java → implements ScosBaseUseCase
└── usecase/position/CreatePositionUseCase.java    → garantir active=true

scos-organization-api
├── controller/department/DepartmentApiDelegateImp.java → usar DepartmentOrder, +active no response
└── enumeration/DepartmentOrder.java       → CRIAR

scos-organization-boot
├── db/changelog/v1/0/0/tables/scos_department.yml → novo changeSet: +ACTIVE, corrigir varchars
└── db/changelog/v1/0/0/tables/scos_position.yml  → novo changeSet: +ACTIVE, corrigir varchars

etc/api/organization/ScosOrganization_Department-Position.yml → +active nos schemas
```

**Fluxo da Solução**:
```
1. [Domain] Corrigir entidades (NC-01, NC-02)
2. [Domain] Atualizar DepartmentRepository (NC-06)
3. [Domain] Atualizar PositionDomainService (NC-06)
4. [Application] Corrigir DTOs (NC-07)
5. [Application] Garantir active=true na criação (NC-03, NC-04)
6. [Application] DeleteDepartmentUseCase implementar ScosBaseUseCase (NC-09)
7. [API] Criar DepartmentOrder, corrigir DepartmentApiDelegateImp (NC-08, NC-10)
8. [API] Atualizar OpenAPI schemas (NC-10)
9. [DB] Novos changeSets Liquibase (NC-05)
10. [Testes] Atualizar/adicionar testes para cobrir active e validação de Department ativo
```

### 3.4 Design Patterns Utilizados

| Pattern | Onde | Justificativa |
|---------|------|---------------|
| Domain Entity Methods | `Department`, `Position` | Lógica de negócio (`activate`/`deactivate`) encapsulada na entidade; preparados para futuros endpoints |
| Repository Method | `DepartmentRepository` | QueryDSL para verificação de existência + estado ativo |
| Domain Service Delegation | `PositionDomainService → DepartmentDomainService` | Responsabilidade de validação de estado do Department permanece no `DepartmentDomainService` |
| ScosBaseUseCase | `DeleteDepartmentUseCase` | Padronização da interface de Use Cases |

---

### 3.5 Conformidade Arquitetural

**Validado por**: 🏗️ @arquiteto

#### SOLID
| Princípio | Conformidade (após fix) | Observação |
|-----------|------------------------|------------|
| Single Responsibility | ✅ | Cada classe com responsabilidade única |
| Open/Closed | ✅ | Novo método no repositório sem alterar existentes |
| Liskov Substitution | ✅ | DeleteDepartmentUseCase passa a implementar a interface corretamente |
| Interface Segregation | ✅ | ScosBaseUseCase é pequena e focada |
| Dependency Inversion | ✅ | Injeção via construtor mantida |

#### Separação de Camadas
- **Respeitada**: ✅ Sim — todas as correções respeitam Clean Architecture

### 3.6 Validação Técnica

**Validado por**: 🎯 @especialista

#### Edge Cases Identificados
1. **Department inativo referenciado em Position existente**: Positions já criadas com Department inativo continuam existindo — o bloqueio ocorre apenas em novas criações/atualizações de Position.
2. **`@Builder.Default` garantido**: `@Builder.Default private boolean active = true` nas entidades. `mapDepartmentFromId` no `PositionMapper` usa `Department.builder().id(id).build()` para aproveitar o default (não `new Department()`).
3. **operações sobre Department inativo**: ✅ DECIDIDO — `UPDATE`, `GET`, `DELETE` em Department inativo são **permitidas**. Apenas criação/atualização de Position que referencia Department inativo é bloqueada.
4. **`activate()`/`deactivate()` sem endpoints**: ✅ DECIDIDO — métodos adicionados nas entidades agora, preparados para futuros endpoints. Não são dead code — estrutura de domínio antecipa o comportamento futuro.
5. **Rollback do changeSet ACTIVE**: O rollback completo inclui reversão de todos os `modifyDataType` (não apenas `dropColumn`).

### 3.7 Validação de Banco de Dados

**Validado por**: 🗄️ @dba

**Impacto em BD**: ✅ Sim

#### Banco de Dados
- **SGBD**: PostgreSQL 18+
- **Versão**: 18+

#### Mudanças de Schema

**SCOS_DEPARTMENT — ANTES**:
```sql
CODE        VARCHAR(50) NOT NULL
DESCRIPTION VARCHAR(100) NOT NULL
-- sem coluna ACTIVE
```

**SCOS_DEPARTMENT — DEPOIS**:
```sql
CODE        VARCHAR(30) NOT NULL
DESCRIPTION VARCHAR(500) NOT NULL
ACTIVE      BOOLEAN NOT NULL DEFAULT TRUE
```

**SCOS_POSITION — ANTES**:
```sql
CODE        VARCHAR(50) NOT NULL
DESCRIPTION VARCHAR(100) NOT NULL
-- sem coluna ACTIVE
```

**SCOS_POSITION — DEPOIS**:
```sql
CODE        VARCHAR(30) NOT NULL
DESCRIPTION VARCHAR(500) NOT NULL
ACTIVE      BOOLEAN NOT NULL DEFAULT TRUE
```

#### ⚠️ Atenção @dba
- `modifyDataType` em `CODE` de varchar(50) para varchar(30) pode **truncar dados existentes** se houver códigos com mais de 30 caracteres. Verificar ambiente de produção antes de aplicar.
- `modifyDataType` em `DESCRIPTION` de varchar(100) para varchar(500) é expansão segura (sem perda de dados).
- Coluna `ACTIVE` com `defaultValueBoolean: true` garante que registros existentes sejam marcados como ativos automaticamente.

---

## 4️⃣ Plano de Execução

> Delegando para **@arquiteto**: Validar se a ordem de execução e a separação de responsabilidades seguem as diretrizes em `etc/architecture/`.  
> Delegando para **@dba**: Validar se os changeSets estão corretos e se a estratégia de migração é segura para produção.  
> Delegando para **@especialista**: Verificar se todos os edge cases estão cobertos pelos testes.

### Etapas

| # | Módulo | Arquivo(s) | NC | Tipo | Estimativa |
|---|--------|------------|-----|------|-----------|
| 1 | domain | `Department.java` | NC-01 | Refatoração Entidade | 30min |
| 2 | domain | `Position.java` | NC-02 | Refatoração Entidade | 20min |
| 3 | domain | `DepartmentRepository.java` | NC-06 | Novo método | 15min |
| 4 | domain | `PositionDomainService.java` | NC-06 | Atualizar validação | 15min |
| 5 | application | `DepartmentDTO.java`, `PositionDTO.java` | NC-07 | +campo active | 10min |
| 6 | application | `CreateDepartmentUseCase.java` | NC-03 | Garantir active=true | 10min |
| 7 | application | `CreatePositionUseCase.java` | NC-04 | Garantir active=true | 10min |
| 8 | application | `DeleteDepartmentUseCase.java` | NC-09 | Impl. ScosBaseUseCase | 10min |
| 9 | api | `DepartmentOrder.java` (CRIAR) | NC-08 | Novo enum | 10min |
| 10 | api | `DepartmentApiDelegateImp.java` | NC-08, NC-10 | Corrigir orderEnum, +active | 20min |
| 11 | api | `ScosOrganization_Department-Position.yml` | NC-10 | +active nos schemas | 15min |
| 12 | boot | `scos_department.yml` | NC-05 | Novo changeSet Liquibase | 20min |
| 13 | boot | `scos_position.yml` | NC-05 | Novo changeSet Liquibase | 20min |
| 14 | testes | Use Case tests + Domain Service tests | — | Atualizar/adicionar testes | 60min |

**Estimativa Total**: ~4h de desenvolvimento

---

## 5️⃣ Riscos e Mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|-------|--------------|---------|-----------|
| Truncamento de dados em `CODE` (50→30 chars) | 🟡 Média | 🔴 Alto | @dba verifica dados antes de aplicar |
| Testes quebrarem por mudança em entidade | 🟢 Baixa | 🟡 Médio | Rodar suite completa após cada etapa |
| Quebra de contrato da API (campo `active` novo) | 🟢 Baixa | 🟡 Médio | Campo aditivo — compatível retroativamente |

---

## 6️⃣ Testes Requeridos

> **Verificar por @especialista**

### Testes a Atualizar

| Arquivo de Teste | Motivo da Atualização |
|------------------|----------------------|
| `CreateDepartmentUseCaseIntegrationTest` | Verificar que `active = true` é definido na criação |
| `UpdateDepartmentUseCaseTest` | Verificar que `active` da entidade é preservado no update |
| `UpdateDepartmentUseCaseTest` | Verificar que Department inativo **pode** ser atualizado (DECISÃO 1) |
| `CreatePositionUseCaseIntegrationTest` | Verificar que `active = true` é definido na criação |
| `UpdatePositionUseCaseTest` | Atualizar mock de `validateDepartmentExistsValidation` → `validateDepartmentExistsAndActiveValidation` |
| `UpdatePositionUseCaseTest` | Adicionar cenário: Position com Department inativo → `SCOS_DEPARTMENT_006` (HTTP 422) |
| `UpdatePositionUseCaseTest` | Adicionar cenário: verificar que campo `active` da Position é **preservado** no update (B-ESP-04) |
| `PositionDomainServiceTest` | Substituir todos os mocks de `validateDepartmentExistsValidation` por `validateDepartmentExistsAndActiveValidation` |
| `DepartmentDomainServiceTest` | Adicionar cenários para `validateDepartmentExistsAndActiveValidation` |

### Novos Testes a Criar

| Arquivo de Teste | Cenário |
|------------------|---------|
| `CreatePositionUseCaseIntegrationTest` | Position com Department inativo → `SCOS_DEPARTMENT_006` (422) |
| `CreatePositionUseCaseIntegrationTest` | Position com Department **inexistente** → `SCOS_DEPARTMENT_001` (404) (B-ESP-03) |
| `UpdatePositionUseCaseTest` | Position com Department inativo → `SCOS_DEPARTMENT_006` (422) |
| `UpdatePositionUseCaseTest` | Position com Department **inexistente** → `SCOS_DEPARTMENT_001` (404) (B-ESP-03) |
| `DepartmentDomainServiceTest` | `activate()` em Department já ativo → `SCOS_DEPARTMENT_004` |
| `DepartmentDomainServiceTest` | `deactivate()` em Department já inativo → `SCOS_DEPARTMENT_005` |
| `PositionDomainServiceTest` | `activate()` em Position já ativa → `SCOS_POSITION_004` |
| `PositionDomainServiceTest` | `deactivate()` em Position já inativa → `SCOS_POSITION_005` |
| `DeleteDepartmentUseCaseTest` | Verificar conformidade com interface `ScosBaseUseCase` |

---

---

## 7️⃣ Validações dos Subagentes

### 🏗️ @arquiteto — Veredicto: ⚠️ APROVADO COM RESSALVAS

Ajustes obrigatórios identificados:

**A-ARQ-01 (BLOQUEANTE)**: NC-06 — `PositionDomainService` **NÃO deve** injetar `DepartmentRepository` diretamente. A validação de "Department existe e está ativo" é invariante do domínio Department. A solução correta é:
1. Adicionar `validateDepartmentExistsAndActiveValidation()` em `DepartmentDomainService`
2. `PositionDomainService` injeta `DepartmentDomainService` e delega a chamada

**A-ARQ-02 (BLOQUEANTE)**: NC-03/04 — Adotar exclusivamente `@Builder.Default private boolean active = true` na entidade como solução. Remover a alternativa de setter no Use Case.

**A-ARQ-03 (BLOQUEANTE)**: ✅ **DECIDIDO** — Operações (`UPDATE`, `GET`, `DELETE`) sobre Department **inativo** são **PERMITIDAS**. Apenas criação/atualização de Position que referencia Department inativo é bloqueada.

**A-ARQ-04 (BLOQUEANTE)**: ✅ **INCORPORADO** — Novo código `SCOS_DEPARTMENT_006` criado para "Department inativo" (HTTP 422). `SCOS_DEPARTMENT_001` permanece exclusivo para "não encontrado" (HTTP 404).

**A-ARQ-05 (DECISÃO DO TIME)**: ✅ **DECIDIDO** — Métodos `activate()`/`deactivate()` são adicionados agora nas entidades, preparados para futuros endpoints. Códigos: `SCOS_DEPARTMENT_004` ("já ativo"), `SCOS_DEPARTMENT_005` ("já inativo"). Mesmo padrão para Position: `SCOS_POSITION_004` e `SCOS_POSITION_005`.

---

### 🎯 @especialista — Veredicto: ⚠️ APROVADO COM RESSALVAS

Ajustes obrigatórios identificados:

**A-ESP-01 (BLOQUEANTE)**: `UpdatePositionUseCase` também chama `validateDepartmentExistsValidation` sem verificar `active` — **omitido da tabela de execução (etapa 7)**. Deve ser adicionado explicitamente como item de correção na NC-06.

**A-ESP-02 (BLOQUEANTE)**: Erro para "Department inativo" deve ter código distinto de `SCOS_DEPARTMENT_001` e HTTP status 422 (não 404). Confirmado pelo @arquiteto.

**A-ESP-03 (BLOQUEANTE)**: `SCOS_DEPARTMENT_004` (e outros novos se mantidos) devem ser registrados em `ExceptionCodeError` e no error catalog antes da codificação.

**A-ESP-04 (OBRIGATÓRIO)**: Adicionar à seção de testes: `UpdatePositionUseCaseTest` — cenário Position com Department inativo → deve lançar erro específico.

**A-ESP-05 (OBRIGATÓRIO)**: Listar explicitamente todos os testes que referenciam `validateDepartmentExistsValidation` e precisarão atualizar o mock para o novo nome `validateDepartmentExistsAndActiveValidation`.

**A-ESP-06 (IMPORTANTE)**: Alterar `mapDepartmentFromId` no `PositionMapper` para usar `Department.builder().id(departmentId).build()` (aproveita `@Builder.Default`) em vez de `new Department()` + `setId()`.

**A-ESP-07 (IMPORTANTE)**: ✅ **DECIDIDO** — `ListDepartmentsUseCase` e `ListPositionsUseCase` **NÃO** irão filtrar por `active` agora. O filtro será adicionado junto com os endpoints de ativação/inativação em sprint futura.

**A-ESP-08 (IMPORTANTE)**: ✅ **REGISTRADO COMO DÍVIDA TÉCNICA** — `DeleteDepartmentUseCase` e `DeletePositionUseCase` executam hard delete. A regra de negócio §4 recomenda soft-delete. Será implementado junto com os endpoints de ativação/inativação.

---

### 🗄️ @dba — Veredicto: ⚠️ APROVADO COM RESSALVAS

Ajustes obrigatórios identificados:

**A-DBA-01 (OBRIGATÓRIO)**: Rollback dos changeSets está **incompleto** — o `modifyDataType` de `DESCRIPTION` e `CODE` não têm rollback declarado. O rollback completo deve ser:
```yaml
rollback:
  - dropColumn:
      tableName: SCOS_DEPARTMENT
      columnName: ACTIVE
  - modifyDataType:
      tableName: SCOS_DEPARTMENT
      columnName: DESCRIPTION
      newDataType: varchar(100)
  - modifyDataType:
      tableName: SCOS_DEPARTMENT
      columnName: CODE
      newDataType: varchar(50)
```
*(Mesmo padrão para `SCOS_POSITION`)*

**A-DBA-02 (OBRIGATÓRIO)**: Executar query de pré-validação antes do deploy em qualquer ambiente com dados:
```sql
SELECT COUNT(*) FROM SCOS_DEPARTMENT WHERE LENGTH(CODE) > 30;
SELECT COUNT(*) FROM SCOS_POSITION   WHERE LENGTH(CODE) > 30;
```
Se resultado > 0, o changeSet falhará no PostgreSQL.

**A-DBA-03 (OBRIGATÓRIO)**: ID dos changeSets deve usar hora real de criação. Formato correto: `SGC_DDMMYYYYHHMM##`. Proposta: `SGC_2802202612001` (DEPARTMENT) e `SGC_2802202612002` (POSITION).

**A-DBA-04 (OBRIGATÓRIO)**: Atualizar `etc/database/department-position.puml`: adicionar `ACTIVE : BOOLEAN NOT NULL DEFAULT TRUE` e corrigir `DESCRIPTION VARCHAR(100)` → `VARCHAR(500)`.

**A-DBA-05 (RECOMENDADO FORTEMENTE)**: PKs `DEPARTMENT_ID` e `POSITION_ID` usam tipo `serial` (INTEGER/4 bytes) no banco, mas o modelo e Java usam `BIGINT`/`Long`. Adicionar `modifyDataType` para `BIGINT` (momento ideal antes de ter dados em produção).

**A-DBA-06 (RECOMENDADO)**: Adicionar índice explícito na FK `DEPARTMENT_ID` de `SCOS_POSITION` — o PostgreSQL **não cria índice automático em FK**:
```yaml
- createIndex:
    tableName: SCOS_POSITION
    indexName: IDX_DEPARTMENT_ID_SCOS_POSITION
    columns:
      - column:
          name: DEPARTMENT_ID
```

**A-DBA-07 (DECIDIDO)**: Divergência `DESCRIPTION` — adotar `varchar(500)` conforme doc de regras. O `puml` é artefato derivado e deve ser atualizado (A-DBA-04).

---

## 8️⃣ Plano de Execução Revisado

> Plano atualizado após validação dos subagentes. Todos os ajustes obrigatórios incorporados.

| # | Módulo | Arquivo(s) | NC / Ajuste | Tipo | Estimativa |
|---|--------|------------|-------------|------|-----------|
| 1 | domain | `Department.java` | NC-01, A-ARQ-02 | Entidade: +active `@Builder.Default`, +@Auditable, +nullable | 20min |
| 2 | domain | `Position.java` | NC-02, A-ARQ-02 | Entidade: +active `@Builder.Default`, +@Auditable, +nullable | 20min |
| 3 | domain | `ExceptionCodeError.java` | A-ARQ-04/05, NC-06 | +SCOS_DEPARTMENT_004 ("já ativo"), +SCOS_DEPARTMENT_005 ("já inativo"), +SCOS_DEPARTMENT_006 ("inativo"), +SCOS_POSITION_004 ("já ativo"), +SCOS_POSITION_005 ("já inativo") | 15min |
| 4 | domain | `DepartmentRepository.java` | NC-06 | +`existsByIdAndActive()` | 15min |
| 5 | domain | `DepartmentDomainService.java` | NC-06, A-ARQ-01, A-ARQ-06 | +`validateDepartmentExistsAndActiveValidation()` — 2 passos: `validateDepartmentExistsValidation` + `existsByIdAndActive` | 15min |
| 6 | domain | `PositionDomainService.java` | NC-06, A-ARQ-01, B-ESP-02 | Remover `DepartmentRepository` + `validateDepartmentExistsValidation`; injetar `DepartmentDomainService`; adicionar método delegador | 15min |
| 7 | application | `DepartmentDTO.java` | NC-07 | +campo `active` | 5min |
| 8 | application | `PositionDTO.java` | NC-07 | +campo `active` | 5min |
| 9 | application | `DeleteDepartmentUseCase.java` | NC-09 | Implementar `ScosBaseUseCase<Long, Void>` | 10min |
| 10 | application | `UpdatePositionUseCase.java` | NC-06, A-ESP-01 | Atualizar para `validateDepartmentExistsAndActiveValidation` | 10min |
| 11 | application | `PositionMapper.java` | A-ESP-06 | `mapDepartmentFromId` usar `Department.builder().id(id).build()` | 10min |
| 12 | api | `DepartmentOrder.java` (CRIAR) | NC-08 | Novo enum com `ID` e `CODE` | 10min |
| 13 | api | `DepartmentApiDelegateImp.java` | NC-08, NC-10 | Usar `DepartmentOrder`; expor `active` no response | 20min |
| 14 | api | `ScosOrganization_Department-Position.yml` | NC-10 | +`active` nos schemas `Department` e `Position` | 15min |
| 15 | boot | `scos_department.yml` | NC-05, A-DBA-01/03/05/06, N-DBA-01 | Novo changeSet: ACTIVE, varchars, BIGINT PK, `ALTER SEQUENCE SEQ_DEPARTMENT_ID AS bigint`, rollback completo | 30min |
| 16 | boot | `scos_position.yml` | NC-05, A-DBA-01/03/05/06, N-DBA-01 | Mesmo padrão; +createIndex FK; +`ALTER SEQUENCE SEQ_POSITION_ID AS bigint` | 30min |
| 17 | database | `department-position.puml` | A-DBA-04, N-DBA-02 | Atualizar modelo: ACTIVE, `DESCRIPTION VARCHAR(500)` (puml atual: 250, banco atual: 100); PKs BIGINT já corretos no puml | 10min |
| 18 | testes | Department/Position Use Case tests | Todos | Atualizar mocks; +cenários de Department inativo | 90min |

**Estimativa Total Revisada**: ~5,5h de desenvolvimento

---

---

## 9️⃣ Validações dos Subagentes — 2ª Rodada

### 🏗️ @arquiteto — Veredicto: ⚠️ APROVADO COM RESSALVAS (todos os bloqueantes resolvidos)

Todos os ajustes da 1ª rodada confirmados. Novo achado incorporado:

**A-ARQ-06 (BLOQUEANTE → RESOLVIDO)**: `validateDepartmentExistsAndActiveValidation` não pode usar apenas `existsByIdAndActive()` como única guarda. A implementação correta exige dois passos:
- Passo 1: `validateDepartmentExistsValidation()` → lança `SCOS_DEPARTMENT_001` (404) se não existir
- Passo 2: `existsByIdAndActive()` → lança `SCOS_DEPARTMENT_006` (422) apenas se existir mas estiver inativo

**Confirmações adicionais**:
- Sem risco de dependência circular entre `PositionDomainService` → `DepartmentDomainService`
- Dois métodos em `DepartmentDomainService` não violam SRP (consumidores e semânticas diferentes)
- `activate()`/`deactivate()` sem endpoints é arquiteturalmente correto (modelo de domínio completo)

---

### 🎯 @especialista — Veredicto: ⚠️ APROVADO COM RESSALVAS (todos os bloqueantes resolvidos)

Todos os bloqueantes da 1ª rodada confirmados. Novos achados incorporados:

**B-ESP-01 (BLOQUEANTE → RESOLVIDO)**: Mesmo que A-ARQ-06 — lógica de dois passos obrigatória.

**B-ESP-02 (OBRIGATÓRIO → RESOLVIDO)**: Etapa 6 agora declara explicitamente:
- Remover `validateDepartmentExistsValidation` do `PositionDomainService`
- Remover `DepartmentRepository` do construtor de `PositionDomainService`

**B-ESP-03 (OBRIGATÓRIO → RESOLVIDO)**: Seção 6 atualizada com cenários "Department **inexistente** → `SCOS_DEPARTMENT_001` (404)" para `CreatePositionUseCaseIntegrationTest` e `UpdatePositionUseCaseTest`.

**B-ESP-04 (IMPORTANTE → RESOLVIDO)**: `UpdatePositionUseCaseTest` agora inclui cenário de preservação do campo `active` no update.

---

### 🗄️ @dba — Veredicto: ⚠️ APROVADO COM RESSALVAS (changeSets funcionalmente corretos)

Todos os ajustes obrigatórios da 1ª rodada confirmados. Changests aprovados para execução. Novos ajustes incorporados:

**N-DBA-01 (RECOMENDADO → INCORPORADO)**: `modifyDataType` de `serial` para `BIGINT` não atualiza a sequence associada. Adicionado `sql` step em ambos os changeSets:
```yaml
- sql:
    sql: ALTER SEQUENCE SEQ_DEPARTMENT_ID AS bigint;  # etapa 15
- sql:
    sql: ALTER SEQUENCE SEQ_POSITION_ID AS bigint;    # etapa 16
```
Rollback correspondente adiciona `ALTER SEQUENCE ... AS integer`.

**N-DBA-02 (AJUSTE MENOR → INCORPORADO)**: puml atual mostra `DESCRIPTION VARCHAR(250)`, mas banco real tem `VARCHAR(100)`. Etapa 17 corrige para `VARCHAR(500)` diretamente (não 250→500).

**Risco documentado (Q3)**: Rollback de `BIGINT → integer` nas PKs falhará se qualquer ID ultrapassar 2.147.483.647. Validação pré-rollback obrigatória:
```sql
SELECT MAX(DEPARTMENT_ID) FROM SCOS_DEPARTMENT;
SELECT MAX(POSITION_ID)   FROM SCOS_POSITION;
```

---

## [PONTO DE PARADA] ⛔

**2ª rodada de validação concluída. Todos os bloqueantes resolvidos e incorporados. Plano pronto para implementação mediante aprovação do usuário.**

| Ponto | Decisão | Status |
|-------|---------|--------|
| Operações sobre Department inativo (UPDATE/GET/DELETE) | ✅ **PERMITIDAS** | Incorporado |
| `activate()`/`deactivate()` nas entidades agora | ✅ **SIM** — preparar para endpoints futuros | SCOS_DEPARTMENT_004/005, SCOS_POSITION_004/005 |
| Filtro `?active=` na listagem agora | ✅ **NÃO** — apenas junto com endpoints de ativação | Incorporado |
| `validateDepartmentExistsAndActiveValidation` — 2 passos (A-ARQ-06 / B-ESP-01) | ✅ Corrigido | Incorporado |
| Remoção explícita `validateDepartmentExistsValidation` + `DepartmentRepository` do `PositionDomainService` (B-ESP-02) | ✅ Declarado | Etapa 6 |
| Cenários "Department inexistente → 404" nos testes (B-ESP-03) | ✅ Adicionado | Seção 6 |
| Preservação de `active` no `UpdatePositionUseCaseTest` (B-ESP-04) | ✅ Adicionado | Seção 6 |
| `ALTER SEQUENCE ... AS bigint` nos changeSets (N-DBA-01) | ✅ Incorporado | Etapas 15 e 16 |
| Correção de `DESCRIPTION VARCHAR(500)` no puml (N-DBA-02) | ✅ Incorporado | Etapa 17 |
| Risco de rollback PK documentado | ✅ Documentado | Seção 9 |
