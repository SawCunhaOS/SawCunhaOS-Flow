# CRUD de Cargo (Position)

**Data**: 2026-07-05
**Status**: 🔄 Em Análise
**Tipo**: 🆕 Nova Feature

---

## ⚠️ Princípio SRP — Uma Funcionalidade por Ideia

- **Nome da funcionalidade**: `crud-cargo-position`
- **Resumo em uma frase**: Implementar o CRUD completo de Cargo (Position) — listar, buscar, criar, atualizar, habilitar e desabilitar — seguindo o padrão já aplicado ao Departamento.

**Checklist SRP**:
- [x] Esta ideia cobre exatamente uma funcionalidade (CRUD de Position)
- [x] Não mistura features independentes no mesmo arquivo (Jornada de Trabalho do Cargo e Motivo de Mudança de Cargo já são ideias/changes separadas)
- [x] O nome é específico

---

## 1️⃣ Visão

### Problema
A entidade de domínio `Position` (`scos-organization-domain`) já existe com os métodos `activate()`/`deactivate()` e o `PositionRepository` já tem `existsByCode`, `existsByCodeAndNotId`, `existsByDepartmentId`. Porém, diferente do Departamento (que já tem `dto/`, `service/`, `specification/` e todos os Use Cases), o Position **não tem** camada de serviço de domínio, Use Cases, nem `PositionDelegate` — a pasta `delegate/position/` está vazia e não existe `usecase/corporate/position/`. A permissão `ScosOrganizationPermission` já contém todos os valores necessários (`GET/CREATE/UPDATE/ENABLE/DISABLE_POSITION`).

Aparentemente essa camada existiu em algum momento (task 5.7 da change `adequacao-rest-nivel2-organization`, de 2026-06-10, está marcada `[x]` como concluída — "Adicionar métodos enable/disable no PositionController e criar ActivatePositionUseCase/InactivatePositionUseCase"), mas não está mais presente no código — provavelmente removida durante o refactor de entidades JPA/Liquibase v2 (changes `adequacao-liquibase-domain-model-v2` e `atualizacao-entidades-jpa-liquibase-v2`, ambas de 2026-07-01).

### Objetivo
Implementar as camadas que faltam (domain service, use cases, delegate) para os 6 endpoints de Position definidos em `etc/api/organization/ScosOrganization_Department-Position.yml`:
- `GET /v1/positions` (UC-029)
- `POST /v1/positions` (UC-028)
- `GET /v1/positions/{id}` (UC-030)
- `PUT /v1/positions/{id}` (UC-031)
- `PUT /v1/positions/{id}/enable` (UC-032)
- `PUT /v1/positions/{id}/disable` (UC-033)

Critério de sucesso: os 6 endpoints funcionam de acordo com as regras do Documento 02 (`etc/doc/usecase/02-departamento-cargo.md`), reaproveitando o `DepartmentService` já existente para validar o departamento vinculado.

### Fora de Escopo
- Jornada de Trabalho do Cargo (`/v1/positions/{positionId}/work-schedule*`) — feature própria (`jornada-trabalho-position-employee`, já implementada)
- Motivo de Mudança de Cargo (`/v1/reason-position-change*`) — feature própria, não tratada aqui
- Qualquer alteração em Departamento

---

## 2️⃣ Requisitos

### Funcionais
- [ ] **RF-01**: `GET /v1/positions` — lista paginada, filtrável por `departmentId` e `active`
- [ ] **RF-02**: `GET /v1/positions/{id}` — retorna cargo incluindo dados do departamento; `404` `SCOS_POSITION_001` se não existir
- [ ] **RF-03**: `POST /v1/positions` — cria cargo; valida `code` único (`SCOS_POSITION_002`), `departmentId` existente (`SCOS_DEPARTMENT_001`) e ativo (`SCOS_DEPARTMENT_006`)
- [ ] **RF-04**: `PUT /v1/positions/{id}` — atualiza `code`/`description`/`departmentId`/`isTrustPosition`; mesmas validações do create, `code` único excluindo `{id}`
- [ ] **RF-05**: `PUT /v1/positions/{id}/enable` — `ACTIVE=false→true`; `422` `SCOS_POSITION_004` se já ativo
- [ ] **RF-06**: `PUT /v1/positions/{id}/disable` — `ACTIVE=true→false`; `422` `SCOS_POSITION_005` se já inativo; `422` `SCOS_POSITION_003` se houver funcionário `ACTIVE` vinculado

### Não-Funcionais
- [ ] **RNF-01**: Seguir estritamente o padrão arquitetural já usado em Department (specification interface pública + `@Service` Bean package-private; Use Case interface pública + Bean package-private `@Service`)
- [ ] **RNF-02**: Seguir os padrões de desenvolvimento descrito no projeto.

---

## 3️⃣ Arquitetura

### Componentes Afetados
```
scos-organization-domain/.../corporate/position/
├── dto/PositionInput.java              (novo)
├── dto/PositionOutput.java             (novo, com Department aninhado)
├── service/PositionMapper.java         (novo)
├── service/PositionServiceBean.java    (novo)
└── specification/PositionService.java  (novo)

scos-organization-usecase/.../usecase/corporate/position/
├── CreatePositionUseCase(+Bean).java   (novo)
├── UpdatePositionUseCase(+Bean).java   (novo)
├── FindPositionUseCase(+Bean).java     (novo)
├── FindAllPositionUseCase(+Bean).java  (novo)
├── EnablePositionUseCase(+Bean).java   (novo)
└── DisablePositionUseCase(+Bean).java  (novo)

scos-organization-api/.../delegate/position/
└── PositionDelegate.java               (novo, implements PositionApiDelegate)
```

### Fluxo Principal (exemplo: Create)
```
PositionDelegate.createPosition
  → CreatePositionUseCase.execute(CreatePositionRequest)
    → PositionService.create(PositionInput)
      → valida code único (PositionRepository.existsByCode)
      → DepartmentService.findDepartmentById(departmentId)  [reaproveita SCOS_DEPARTMENT_001]
      → valida department.isActive() → senão SCOS_DEPARTMENT_006
      → persiste Position
```

### Decisões Técnicas (confirmadas com o usuário em 2026-07-05)
| Decisão | Escolha | Alternativa Descartada | Motivo |
|---------|---------|------------------------|--------|
| Checar funcionário ativo vinculado (disable) | Expor método público no agregado Employee (nova `EmployeeService`/specification, ou método público equivalente) com `existsByPositionIdAndStatus(positionId, ACTIVE)`; `PositionService` chama essa API pública | Chamar `EmployeeQueryRepository.existsByPositionId` direto do pacote `.internal` do Employee | Mantém encapsulamento do agregado Employee; método existente também não filtra por status |
| Filtro `active` em `GET /v1/positions` | **Não aplicar** — aceito no contrato mas ignorado na query, mesmo padrão (falho) hoje usado em Department | Aplicar de fato o filtro `active` | Consistência com o padrão existente; corrigir o filtro em Department fica fora de escopo desta ideia |
| `PUT /v1/positions/{id}` com `departmentId` inalterado | Validar **sempre** que o `departmentId` informado existe e está `ACTIVE=true`, tenha mudado ou não | Pular validação quando `departmentId` == atual | Mais simples e seguro — evita manter cargo associado a departamento que foi desativado depois da última atualização |
| Nested Department no `PositionOutput`/`findAll` | Carregar `Position` via `QuerydslPredicateExecutor` (entidade já traz `department` via `@ManyToOne` padrão EAGER) e mapear com MapStruct compondo `DepartmentMapper` | Query JPQL com `new PositionOutput(...)` flat (como Department faz) | Response precisa de `department` aninhado completo; constructor expression flat não suporta objeto aninhado sem subquery adicional |
| `isTrustPosition` nas regras de negócio | Aceitar como campo opcional (default `false`), sem regra de validação própria | — | Documento 02 não lista o campo na tabela de campos do POST, mas o YAML já o define — mismatch entre doc e contrato |

### Banco de Dados
- **Impacto**: ❌ Não (tabela `SCOS_POSITION` e colunas já existem via Liquibase)

---

## 4️⃣ Implementação

### Arquivos
Ver árvore da Seção 3.

### Tarefas
- [ ] **T-01**: Criar `PositionInput`/`PositionOutput` (dto)
- [ ] **T-02**: Criar `PositionMapper` (MapStruct, compondo `DepartmentMapper`)
- [ ] **T-03**: Criar `PositionService` (specification) + `PositionServiceBean`
- [ ] **T-04**: Resolver decisão da checagem de funcionário ativo (ver riscos) e implementar em `disable()`
- [ ] **T-05**: Criar os 6 Use Cases (interface + Bean) em `usecase/corporate/position`
- [ ] **T-06**: Criar `PositionDelegate implements PositionApiDelegate`
- [ ] **T-07**: Testes (unit + integration conforme `tdd-workflow`/`testcontainers-integration`)

### Riscos e Edge Cases
1. **Checagem cruzada de agregado (Employee → Position)** — RESOLVIDO: será exposto método público no agregado Employee (`existsByPositionIdAndStatus`); `PositionService` não acessa `EmployeeQueryRepository`/pacote `.internal` diretamente. Isso implica criar (ou estender) uma `EmployeeService`/specification pública — hoje `employee/specification/` está vazio, então este será o primeiro método público desse agregado. Avaliar durante o apply se vale criar uma interface mínima (`EmployeeService.existsActiveEmployeeInPosition(Long positionId)`) só com esse método, sem puxar todo o CRUD de Employee para o escopo.
2. **`active` filtro no `GET /v1/positions`** — RESOLVIDO: não será aplicado (consistente com Department). `departmentId` deve ser aplicado (exigido pelo UC-029).
3. **`PUT /v1/positions/{id}` com `departmentId` inalterado** — RESOLVIDO: validar sempre.
4. **Mensagens de erro sem texto** (`SCOS_POSITION_004`/`005`): conforme Documento 07, os textos PT/EN já foram definidos nesse documento — só falta aplicar nos arquivos `.properties`, fora do escopo desta ideia (é tarefa de infraestrutura de mensagens, não de código de use case).

---

## 📎 Referências
- `etc/doc/usecase/02-departamento-cargo.md` (Seção 3 — Cargo)
- `etc/api/organization/ScosOrganization_Department-Position.yml`
- `etc/doc/usecase/07-mensagens-erro-pt-en.md`
- Padrão de referência: `scos-organization-domain/.../corporate/department/*`, `scos-organization-usecase/.../corporate/department/*`, `DepartmentDelegate.java`
- Padrão de arquitetura: `etc/architecture/api-development-guidelines.md`

---
