# Jornada de Trabalho — Cargo e Funcionário

**Data**: 2026-07-01
**Status**: 🔄 Em Análise
**Tipo**: 🆕 Nova Feature

---

## ⚠️ Princípio SRP — Uma Funcionalidade por Ideia

> **Regra**: Uma ideia = uma funcionalidade. Features independentes → arquivos separados.

- **Nome da funcionalidade**: `jornada-trabalho-position-employee`
- **Resumo em uma frase**: Expor CRUD de jornada de trabalho template por cargo (`PositionWorkSchedule`) e efetiva por funcionário (`EmployeeWorkSchedule`), tabelas novas do schema v2 sem nenhum endpoint hoje.

**Checklist SRP**:
- [x] Esta ideia cobre exatamente uma funcionalidade — jornada de trabalho, template + efetiva, que são as duas faces da mesma tabela horária (dia da semana + 4 horários)
- [x] Não mistura com histórico de cargo/contrato, histórico de status, fiscal, catálogo de endereço/contato, outbox ou perfil — ideias próprias
- [x] O nome é específico

---

## 1️⃣ Visão

### Problema
`SCOS_POSITION_WORK_SCHEDULE` (template por cargo) e `SCOS_EMPLOYEE_WORK_SCHEDULE` (efetiva por funcionário, copiada do template via código/use case — não é ação exposta por API) são tabelas novas do schema v2. Nenhum endpoint existe hoje para nenhuma das duas.

### Objetivo
Existe CRUD completo de horário por dia da semana tanto no template do cargo quanto na jornada efetiva do funcionário — permitindo cadastrar/atualizar os horários do funcionário diretamente via API, independente da cópia inicial (que é feita internamente pelo código, fora do escopo desta ideia).

### Fora de Escopo
- Mecanismo de cópia automática template→efetivo (acontece via código/use case, ex.: na contratação ou atribuição de cargo) — não é endpoint público
- Histórico de cargo/contrato, histórico de status, dados fiscais, catálogo de endereço/contato, outbox, perfil adicional — ideias próprias

---

## 2️⃣ Requisitos

### Funcionais
- [ ] **RF-01**: Criar CRUD de `PositionWorkSchedule` — `GET /positions/{positionId}/work-schedule` (lista os até 7 dias cadastrados), `POST /positions/{positionId}/work-schedule` (cria um dia), `PUT /positions/{positionId}/work-schedule/{dayOfWeek}` (atualiza), `DELETE /positions/{positionId}/work-schedule/{dayOfWeek}` (remove o dia). Schema: `dayOfWeek` (enum `MONDAY`..`SUNDAY`), `startTime`, `lunchStart`, `lunchEnd`, `endTime`
- [ ] **RF-02**: Criar CRUD de `EmployeeWorkSchedule` — mesmo padrão de rotas/schema, sob `/employees/{employeeId}/work-schedule`
- [ ] **RF-03**: Validar (documentar como regra, aplicada no use case) `startTime < lunchStart < lunchEnd < endTime` — mesma restrição de integridade das duas tabelas
- [ ] **RF-04** (achado na revisão): Toda rota nova precisa de `x-authorize` + entrada correspondente em `ScosOrganizationPermission` — `GET/CREATE/UPDATE/DELETE_POSITION_WORK_SCHEDULE`, mesmo conjunto para `EMPLOYEE_WORK_SCHEDULE`

### Não-Funcionais
- [ ] **RNF-01**: Nenhuma alteração de schema no banco — schema v2 já existe
- [ ] **RNF-02**: `dayOfWeek` como chave do sub-recurso (não `id` sequencial) — UK composta `(positionId/employeeId, dayOfWeek)` no banco já garante um registro por dia
- [ ] **RNF-03** (achado na revisão, decidido com o usuário): `GET .../work-schedule` retorna array direto (`WorkScheduleOutput[]`), **sem** `paginationFilter`/`paginatedDTO` — exceção deliberada ao padrão do resto do contrato (que pagina até listas pequenas), justificada pelo teto real de 7 registros garantido por UK composta no banco. `CreateXRequest`/`UpdateXRequest` continuam seguindo o padrão SCOS normal (`x-required-message`/`x-empty-message`)

---

## 3️⃣ Arquitetura

### Componentes Afetados
```
etc/api/organization/
└── ScosOrganization_Department-Position.yml
    └── paths novos: /v1/positions/{positionId}/work-schedule*

etc/api/organization/
└── ScosOrganization_Employee.yml
    └── paths novos: /v1/employees/{employeeId}/work-schedule*
    └── enum compartilhado: DayOfWeek (MONDAY..SUNDAY) — mesmo enum usado nos dois arquivos, decidir local canônico
```

### Detalhamento de Campos por Arquivo YAML

| Arquivo YAML | Schema/Path | Campo | Tipo | Nota |
|---|---|---|---|---|
| `ScosOrganization_Department-Position.yml` | `PositionWorkSchedule` + `Create`/`UpdatePositionWorkScheduleRequest` (novos) | `dayOfWeek`, `startTime`, `lunchStart`, `lunchEnd`, `endTime` | `enum DayOfWeek` / `string (format: time)` × 4 | UK composta `(positionId, dayOfWeek)` |
| `ScosOrganization_Department-Position.yml` | `GET/POST /v1/positions/{positionId}/work-schedule`, `PUT/DELETE .../{dayOfWeek}` (novos) | — | — | `GET` retorna array direto (`WorkScheduleOutput[]`), sem paginação — exceção deliberada (RNF-03) |
| `ScosOrganization_Employee.yml` | `EmployeeWorkSchedule` + `Create`/`UpdateEmployeeWorkScheduleRequest` (novos) | `dayOfWeek`, `startTime`, `lunchStart`, `lunchEnd`, `endTime` | idem `PositionWorkSchedule` | UK composta `(employeeId, dayOfWeek)` |
| `ScosOrganization_Employee.yml` | `GET/POST /v1/employees/{employeeId}/work-schedule`, `PUT/DELETE .../{dayOfWeek}` (novos) | — | — | mesmo padrão, sem paginação |
| local a decidir (`/propose`, provável `ScosComponents.yml`) | `DayOfWeek` (enum novo) | `MONDAY`..`SUNDAY` | enum | compartilhado entre os dois arquivos |

### Fluxo Principal
```
POST /employees/{employeeId}/work-schedule { dayOfWeek: TUESDAY, startTime, lunchStart, lunchEnd, endTime }
  → valida startTime < lunchStart < lunchEnd < endTime
  → valida não existe já um registro pra esse (employeeId, dayOfWeek)
  → usecase cria SCOS_EMPLOYEE_WORK_SCHEDULE
```

### Decisões Técnicas
| Decisão | Escolha | Alternativa Descartada | Motivo |
|---------|---------|------------------------|--------|
| Cópia template→efetivo | Feita via código (use case), sem endpoint público de "copiar" | Endpoint explícito `POST .../copy-from-position` | Decidido com o usuário — API expõe só cadastro/atualização direta da jornada do funcionário |
| Chave do sub-recurso | `dayOfWeek` (não precisa de `id` próprio na rota) | `id` sequencial da tabela | UK composta já é natural — `dayOfWeek` é único por cargo/funcionário |
| Paginação em `GET .../work-schedule` | Sem paginação — array direto | `paginationFilter` + `paginatedDTO` (padrão do resto do contrato) | Decidido com o usuário — teto de 7 registros garantido por UK composta torna paginação sem propósito real, única exceção deliberada entre as 7 ideias |

### Banco de Dados
- **Impacto**: ❌ Não — schema já existe (`adequacao-liquibase-domain-model-v2`, completo)

---

## 4️⃣ Implementação

*Detalhamento de arquivos/tarefas fica para a fase de `/openspec-propose`.*

### Riscos e Edge Cases
1. `EmployeeWorkSchedule` não tem histórico — mudança sobrescreve via `UPDATE`; se auditoria de jornada anterior for necessária no futuro, seria uma ideia separada (tabela não suporta hoje)
2. Dia sem linha cadastrada significa "não definido", não "segue o cargo" — API precisa deixar isso claro na documentação/descrição das rotas, para não ser interpretado como fallback automático
3. Local canônico do enum `DayOfWeek` (compartilhado entre os dois arquivos YAML) — decidir no `/propose` (provavelmente `ScosComponents.yml`)

---

## 📎 Referências
- Domain model: `etc/database/domain_model.md`
- Migrations v2: `openspec/changes/adequacao-liquibase-domain-model-v2/`

---
