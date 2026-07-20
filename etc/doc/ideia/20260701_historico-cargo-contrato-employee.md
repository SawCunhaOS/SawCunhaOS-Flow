# Histórico de Cargo e Dados de Contrato do Funcionário

**Data**: 2026-07-01
**Status**: 🔄 Em Análise
**Tipo**: 🆕 Nova Feature

---

## ⚠️ Princípio SRP — Uma Funcionalidade por Ideia

> **Regra**: Uma ideia = uma funcionalidade. Features independentes → arquivos separados.

- **Nome da funcionalidade**: `historico-cargo-contrato-employee`
- **Resumo em uma frase**: Expor no contrato de `Employee`/`Position` os novos dados de contrato (`contractType`, `probationEndDate`), cargo de confiança (`isTrustPosition`) e o histórico de cargo com motivo obrigatório (`EmployeePositionHistory`/`ReasonPositionChange`).

**Checklist SRP**:
- [x] Esta ideia cobre exatamente uma funcionalidade — dados de contrato/cargo do funcionário e seu histórico, que nascem juntos na mesma mudança de domínio (`transfer` passa a exigir motivo e gerar histórico)
- [x] Não mistura com histórico de status (Company/Employee/Login), fiscal, catálogo de endereço/contato, jornada, outbox ou perfil — ideias próprias
- [x] O nome é específico

---

## 1️⃣ Visão

### Problema
O schema v2 adicionou `CONTRACT_TYPE` e `PROBATION_END_DATE` em `SCOS_EMPLOYEE`, `IS_TRUST_POSITION` em `SCOS_POSITION`, e criou `SCOS_REASON_POSITION_CHANGE` + `SCOS_EMPLOYEE_POSITION_HISTORY` (histórico imutável de cargo, fechado automaticamente por trigger). Nenhum desses campos/tabelas está no contrato hoje. Além disso, `TransferEmployeeRequest` (`PATCH /employees/{id}/transfer`) não tem `reasonPositionChangeId`, mas a tabela de histórico exige motivo `NOT NULL` sempre que uma nova atribuição de cargo é criada.

### Objetivo
`Employee` expõe `contractType`/`probationEndDate`; `Position` expõe `isTrustPosition`; existe CRUD de referência para `ReasonPositionChange`; `/transfer` exige `reasonPositionChangeId` sempre; existe endpoint de consulta ao histórico de cargo do funcionário.

### Fora de Escopo
- Histórico de status (Company/Employee/Login) — ideia própria, embora compartilhe o espírito "motivo obrigatório + histórico imutável"
- Jornada de trabalho (`PositionWorkSchedule`/`EmployeeWorkSchedule`) — ideia própria, mesmo estando no mesmo bounded context de `Position`/`Employee`
- Dados fiscais, catálogo de endereço/contato, outbox, perfil adicional — ideias próprias

---

## 2️⃣ Requisitos

### Funcionais
- [ ] **RF-01**: Adicionar a `Position`/`CreatePositionRequest`/`UpdatePositionRequest`: `isTrustPosition: boolean` (default `false`)
- [ ] **RF-02**: Adicionar a `Employee`/`CreateEmployeeRequest`/`UpdateEmployeeRequest`: `contractType` (enum `CLT`/`PJ`/`ESTAGIO`/`TEMPORARIO`, obrigatório), `probationEndDate` (date, opcional — nulo se não aplicável ou já encerrado)
- [ ] **RF-03**: Criar CRUD de referência `ReasonPositionChange` — `code`, `description`, `active` (sem `entityType`, só se aplica a `EMPLOYEE`). Endpoints: `GET/POST /v1/reason-position-change`, `GET/PUT /v1/reason-position-change/{id}`, `enable`/`disable`
- [ ] **RF-04**: `TransferEmployeeRequest`: adicionar `reasonPositionChangeId` (obrigatório sempre). Todo `/transfer` insere uma nova linha em `EmployeePositionHistory` — mesmo quando `positionId` não muda, fecha a linha aberta e abre outra com o mesmo cargo e o motivo informado (decidido com o usuário: toda transferência vira um marco na linha do tempo do funcionário, não só mudança de cargo)
- [ ] **RF-05**: Criar `GET /employees/{id}/position-history` — paginado, retorna `positionId`, `startDate`, `endDate` (nulo = atual), `reasonPositionChangeId`, `createdAt`, `userAt`
- [ ] **RF-06** (achado na revisão): Toda rota nova precisa de `x-authorize` + entrada correspondente em `ScosOrganizationPermission` — `GET/CREATE/UPDATE_REASON_POSITION_CHANGE`, `ENABLE/DISABLE_REASON_POSITION_CHANGE`, `GET_EMPLOYEE_POSITION_HISTORY`

### Não-Funcionais
- [ ] **RNF-01**: Nenhuma alteração de schema no banco — schema v2 já existe
- [ ] **RNF-02**: `x-required-message`/`x-empty-message` seguem convenção SCOS já usada nos demais campos obrigatórios do arquivo
- [ ] **RNF-03** (achado na revisão): DTOs seguem o padrão SCOS do restante do arquivo — `CreateReasonPositionChangeRequest`/`UpdateReasonPositionChangeRequest`, `GetReasonPositionChangeResponse { data }`, `GetAllReasonPositionChangeResponse { data: array, paginatedDTO }`. `GET /v1/reason-position-change` e `GET /v1/employees/{id}/position-history` usam `paginationFilter` (query, obrigatório) — sem exceção nenhuma lista do contrato foge da paginação

---

## 3️⃣ Arquitetura

### Componentes Afetados
```
etc/api/organization/
└── ScosOrganization_Department-Position.yml
    ├── Position/CreatePositionRequest/UpdatePositionRequest (mod — isTrustPosition)
    └── paths novos: /v1/reason-position-change*

etc/api/organization/
└── ScosOrganization_Employee.yml
    ├── Employee/CreateEmployeeRequest/UpdateEmployeeRequest (mod — contractType, probationEndDate)
    ├── TransferEmployeeRequest (mod — reasonPositionChangeId)
    ├── novo enum: EmployeeContractType
    └── path novo: GET /v1/employees/{id}/position-history
```

### Detalhamento de Campos por Arquivo YAML

| Arquivo YAML | Schema/Path | Campo | Tipo | Nota |
|---|---|---|---|---|
| `ScosOrganization_Department-Position.yml` | `Position`, `CreatePositionRequest`, `UpdatePositionRequest` (mod) | `isTrustPosition` | `boolean` | default `false` |
| `ScosOrganization_Department-Position.yml` | `ReasonPositionChange` (novo) | `code`, `description`, `active` | string / string / boolean | sem `entityType` — só se aplica a `EMPLOYEE` |
| `ScosOrganization_Department-Position.yml` | `CreateReasonPositionChangeRequest`/`UpdateReasonPositionChangeRequest` (novos) | `code`, `description` | string / string | `active` via `enable`/`disable` |
| `ScosOrganization_Department-Position.yml` | `GetReasonPositionChangeResponse`/`GetAllReasonPositionChangeResponse` (novos) | `data`, `paginatedDTO` | `$ref` / `$ref ScosPaginated` | padrão SCOS |
| `ScosOrganization_Department-Position.yml` | `GET/POST /v1/reason-position-change`, `GET/PUT /v1/reason-position-change/{id}`, `enable`/`disable` (novos) | — | — | mesmo padrão de `AddressType` |
| `ScosOrganization_Employee.yml` | `Employee`, `CreateEmployeeRequest`, `UpdateEmployeeRequest` (mod) | `contractType` | `enum EmployeeContractType` (`CLT`/`PJ`/`ESTAGIO`/`TEMPORARIO`, novo) | obrigatório |
| `ScosOrganization_Employee.yml` | `Employee`, `CreateEmployeeRequest`, `UpdateEmployeeRequest` (mod) | `probationEndDate` | `string (format: date)` | opcional, nulo se n/a ou encerrado |
| `ScosOrganization_Employee.yml` | `TransferEmployeeRequest` (mod) | `reasonPositionChangeId` | `integer (format: int64)` | obrigatório sempre (RF-04) |
| `ScosOrganization_Employee.yml` | `GET /v1/employees/{id}/position-history` (novo) → `EmployeePositionHistory`/resposta paginada | `positionId`, `startDate`, `endDate`, `reasonPositionChangeId`, `createdAt`, `userAt` | int64 / date / date (nullable) / int64 / datetime / string | `endDate` nulo = linha atual; paginado |

### Fluxo Principal
```
PATCH /employees/{id}/transfer { positionId?, companyId?, supervisorId?, dateOfTransfer, reasonPositionChangeId }
  → sempre exige reasonPositionChangeId
  → usecase sempre insere EmployeePositionHistory: fecha a linha aberta (trigger TRG_CLOSE_PREVIOUS_POSITION),
    abre nova linha com positionId (o novo, se veio no request; o mesmo de antes, se não veio) e o
    reasonPositionChangeId informado — sincroniza SCOS_EMPLOYEE.POSITION_ID mesmo quando o valor não mudou
  → demais campos (companyId, supervisorId) atualizam Employee diretamente, sem entrar no histórico de cargo
```

### Decisões Técnicas
| Decisão | Escolha | Alternativa Descartada | Motivo |
|---------|---------|------------------------|--------|
| `reasonPositionChangeId` em `/transfer` | Obrigatório sempre, e sempre gera linha de histórico (mesmo sem mudar `positionId`) | Só insere histórico quando `positionId` muda | Decidido com o usuário — toda transferência (empresa/supervisor/cargo) vira marco na linha do tempo do funcionário |
| Consulta de histórico de cargo | Expõe `GET .../position-history` | Só banco, sem endpoint | Decidido com o usuário — espelha decisão do histórico de status |

### Banco de Dados
- **Impacto**: ❌ Não — schema já existe (`adequacao-liquibase-domain-model-v2`, completo)

---

## 4️⃣ Implementação

*Detalhamento de arquivos/tarefas fica para a fase de `/openspec-propose`.*

### Riscos e Edge Cases
1. Resolvido: todo `/transfer` gera linha em `EMPLOYEE_POSITION_HISTORY`, mesmo sem mudar `positionId` — o índice único parcial (`EMPLOYEE_ID` filtrado por `END_DATE IS NULL`) segue garantindo uma única linha aberta por vez, então o comportamento é seguro a nível de banco. Efeito colateral aceito: o histórico de cargo passa a registrar toda transferência (inclusive as que só mudam empresa/supervisor), não só mudança de cargo em si — nomear isso claramente na descrição do endpoint pra não confundir consumidores da API
2. `probationEndDate` deve ser posterior a `DATE_OF_HIRING` (restrição de integridade do banco) — contrato não valida isso sozinho, é regra de use case/banco
3. `PROBATION_END_DATE` não aplicável a `PJ` — contrato aceita nulo, mas não impede preenchimento incoerente com `contractType=PJ`; considerar validação client-side

---

## 📎 Referências
- Domain model: `etc/database/domain_model.md`
- Migrations v2: `openspec/changes/adequacao-liquibase-domain-model-v2/`
- Sync JPA (completo, já criou `EmployeeContractType`/`ReasonPositionChange`/`EmployeePositionHistory` no domínio): `openspec/changes/atualizacao-entidades-jpa-liquibase-v2/`

---
