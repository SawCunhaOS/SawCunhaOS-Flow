## Context

O módulo `scos-organization-domain` contém as entidades JPA e repositórios que mapeiam o schema PostgreSQL gerenciado por Liquibase. As migrations v2 (`adequacao-liquibase-domain-model-v2`) já estão corretas e são a fonte da verdade — o gap está inteiramente no código Java, que ainda reflete o schema v1.

**Padrões do projeto** que devem ser seguidos integralmente (nenhum padrão novo é introduzido):
- Entidades com `CREATED_AT + UPDATED_AT + USER_AT` (todos `NOT NULL`) → estender `BaseEntity` (`scos-foundation-utils`)
- Entidades imutáveis (sem `UPDATED_AT`) → declarar `createdAt` (`@CreationTimestamp`) e `userAt` diretamente, sem herança — padrão `ProfileResource`
- Tabelas com formato ainda mais mínimo (sem `USER_AT`, sem `UPDATED_AT`) → mapeamento próprio, sem imitar nenhum dos dois padrões acima
- Todas as entidades → `@Auditable` + Lombok (`@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`)
- Entidades com dados pessoais (LGPD) → `@Auditable(auditRead = true)`
- PK composta → classe `@Embeddable` separada com `@EmbeddedId`
- Repositórios → `interface @Repository` estendendo `BaseJpaRepository<T,ID> + JpaSpecificationExecutor<T> + QuerydslPredicateExecutor<T>`, sem classe de implementação

## Goals / Non-Goals

**Goals:**
- Sincronizar 100% dos models JPA com o schema Liquibase v2 existente
- Redesenhar `STATUS` de `Company`/`Employee`/`Login` como cache sincronizado por trigger, com histórico auditável como fonte de verdade
- Criar todas as entidades/repositórios ausentes (histórico de status e cargo, outbox, fiscal, catálogo de tipos, `LoginProfile`)
- Remover entidades/enums/regras que violam o vocabulário fechado do novo schema
- Zero alteração no schema do banco

**Non-Goals:**
- Modificar migrations Liquibase — já feitas, fonte da verdade
- Implementar casos de uso que consumirão as entidades novas (outbox, histórico, cadastro de motivos/tipos, dados fiscais) — ficam para ideias futuras
- Renomear o campo público `keycloakId` nos contratos OpenAPI/gRPC — decisão de contrato separada
- Seed de dados oficiais (`SCOS_LEGAL_NATURE`/`SCOS_CNAE`) e migração de dados — fora de escopo (banco recriado do zero)

## Decisions

### D-01: Layout de pacotes

```
domain/
├── access/
│   ├── login/internal/           Login (mod), LoginStatus (mod), LoginType,
│   │                             VwAuthorityResponse (mod), rules/*
│   ├── profile/internal/         Profile, LoginProfile (novo)
│   ├── status/internal/          EntityType (novo enum compartilhado),
│   │                             ReasonActivate/Inactivate/Disable/Enable (novos),
│   │                             CompanyStatusHistory/EmployeeStatusHistory/LoginStatusHistory (novos)
│   ├── resource/internal/        Resource, ProfileResource — inalterados
│   ├── system/internal/          ScosSystem — inalterado
│   └── integration/internal/     IntegrationKeycloak* — REMOVIDO
├── corporate/
│   ├── company/internal/         Company (mod), StatusCompany (mod), CompanyAddress (mod),
│   │                             CompanyContact (mod), LegalNature/Cnae/CompanyCnaeSecondary (novos)
│   ├── employee/internal/        Employee (mod), StatusEmployee (mod), EmployeeContractType (novo enum),
│   │                             EmployeeAddress (mod), EmployeeContact (mod),
│   │                             ReasonPositionChange/EmployeePositionHistory (novos)
│   ├── position/internal/        Position (mod), PositionWorkSchedule (novo), DayOfWeek (novo enum)
│   ├── employee/internal/        EmployeeWorkSchedule (novo — junto de Employee, mesma regra de "entidade vive
│   │                             no pacote do dono do FK" usada em Position/PositionWorkSchedule)
│   ├── department/internal/      Department — inalterado
│   └── catalog/internal/         AddressType, ContactType (novos — pacote próprio, não access/resource,
│                                 para não confundir com o conceito de "Resource" de permissão)
└── outbox/internal/              OutboxTopic, OutboxEvent, OutboxEventLog, OutboxEventDeadLetter (novo pacote)
```

**Alternativa descartada**: colocar `AddressType`/`ContactType` em `access/resource/internal` (sugestão inicial da ideia). Descartado porque esse pacote já tem um conceito chamado "Resource" (permissão/endpoint), e reaproveitar o nome para "tipo de referência de endereço/contato" geraria confusão de domínio. `corporate/catalog/internal` é mais claro e o acoplamento cruzado (`catalog` → `access/status` para importar `EntityType`) já existe em outro sentido hoje (`Employee` → `Login`, `Login` → `Employee`).

### D-02: `STATUS` como cache — métodos de domínio retornam o registro de histórico, não persistem

`Company`/`Employee`/`Login` continuam com o campo `status` (cache), mas os métodos de transição deixam de mutá-lo diretamente. Cada método:
1. Valida a transição client-side (replica exatamente `CHK_*_STATUS_TRANSITION` — ver tabela abaixo)
2. Retorna uma instância *não persistida* de `CompanyStatusHistory`/`EmployeeStatusHistory`/`LoginStatusHistory` com `status` e a FK de motivo corretos
3. **Não seta `previousStatus`** — é derivado pela trigger `TRG_BEFORE_INSERT_*_STATUS_HISTORY` no INSERT
4. **Não seta `status` na entidade principal** — é sincronizado pela trigger `TRG_SYNC_*_STATUS` após o INSERT do histórico

A entidade não tem acesso a repositório (pureza de agregado); quem persiste o histórico retornado é o usecase que chamar o método — implementação desse usecase é não-goal desta mudança.

Tabela de transição (`SCOS_LOGIN` usa `BLOCKED` no lugar de `DISABLED`, mesma lógica):

| Método | Transição válida | Motivo exigido | Guard (senão) |
|---|---|---|---|
| `activate(reasonActivateId)` | `INACTIVE → ACTIVE` | `REASON_ACTIVATE_ID` | lança se `status != INACTIVE` |
| `inactivate(reasonInactivateId)` | `ACTIVE → INACTIVE` ou `DISABLED/BLOCKED → INACTIVE` | `REASON_INACTIVATE_ID` | lança se `status == INACTIVE` |
| `disable(reasonDisableId)` | `ACTIVE → DISABLED/BLOCKED` | `REASON_DISABLE_ID` | lança se `status != ACTIVE` |
| `enable(reasonEnableId)` | `DISABLED/BLOCKED → ACTIVE` | `REASON_ENABLE_ID` | lança se `status != DISABLED/BLOCKED` |

`INACTIVE → DISABLED/BLOCKED` não é transição válida (premissa assumida no `domain_model.md` — não é coerente bloquear temporariamente algo já encerrado definitivamente); `disable()`/`enable()` guards refletem isso.

**Alternativa descartada**: manter `status` mutável diretamente e só mapear as entidades novas de histórico sem integrar aos métodos de negócio. Descartada porque adiaria a mesma refatoração para quando os usecases forem implementados, e deixaria os métodos atuais (`activate`/`inactivate`/`disable`) referenciando `DELETED`, que não existe mais.

### D-03: Remoção de `delete()` e `DELETED`

`INACTIVE` passa a ser o estado terminal — não existe mais soft-delete distinto de inativação. `Company.delete()`/`Employee.delete()` são removidos (sem substituto — `inactivate()` cobre o caso). Capability `status-deleted` é retirada (`REMOVED Requirements`).

### D-04: `LoginStatus.LOCKED` → `BLOCKED`, `LoginDeletedRule` removida

Alinhamento estrito ao `CHECK` (`ACTIVE/INACTIVE/BLOCKED`). `LoginLockedRule` é renomeada para `LoginBlockedRule` (mesma mensagem `SCOS_LOGIN_011`, já fala em "bloqueado"). `LoginDeletedRule` é removida — `SCOS_LOGIN_012` some de `ExceptionCodeError` e dos dois bundles i18n. `@ScosRule`: `LoginInactiveRule` (`2`→`1`), `LoginBlockedRule` (`3`→`2`) — renumeração contígua, sem lacuna.

### D-05: Entidades novas sem `UPDATED_AT` seguem o padrão `ProfileResource`, adaptado a PK simples quando não há PK composta

`ProfileResource` usa `@EmbeddedId` porque sua PK é composta. As três tabelas de histórico de status, `EmployeePositionHistory`, `OutboxEventLog` e `OutboxEventDeadLetter` têm PK simples auto-gerada — usam o mesmo padrão (`createdAt` `@CreationTimestamp`, `userAt` direto, sem `BaseEntity`), só com `@Id @GeneratedValue` em vez de `@EmbeddedId`. Não é um padrão novo, é a mesma receita aplicada a uma PK diferente — não se cria uma superclasse `ImmutableEntity` (mantém a mesma decisão da rodada anterior de não introduzir abstração nova para duas variações de shape).

`CompanyCnaeSecondary` e `LoginProfile` têm PK composta como `ProfileResource` — usam `@EmbeddedId` idêntico.

### D-06: `OutboxEvent` não estende `BaseEntity` — `UPDATED_AT`/`USER_AT` nullable

`SCOS_OUTBOX_EVENT.UPDATED_AT`/`USER_AT` são `NULL` (só `CREATED_AT` tem `DEFAULT NOW()` e é populado por trigger de sincronização futura/worker). `BaseEntity` assume os três campos `NOT NULL` — não se encaixa. `OutboxEvent` mapeia os três campos diretamente: `createdAt` (`@CreationTimestamp`), `updatedAt` (campo simples, sem anotação — atualizado pelo usecase/worker que ainda não existe), `userAt` (campo simples nullable). `OutboxTopic`, ao contrário, tem os três campos `NOT NULL` — estende `BaseEntity` normalmente.

### D-07: `LegalNature`/`Cnae` — entidade mínima própria, nem `BaseEntity` nem `ProfileResource`

`SCOS_LEGAL_NATURE`/`SCOS_CNAE` só têm `ID`/`CODE`/`DESCRIPTION`/`CREATED_AT` — nem `UPDATED_AT` nem `USER_AT`. Não se encaixam em nenhum padrão existente (ambos assumem pelo menos `userAt`). Mapeamento próprio: `@Id @GeneratedValue`, `code`, `description`, `createdAt` (`@CreationTimestamp`), sem `userAt`. Tabelas de seed puro (IBGE), sem necessidade de rastrear autor.

### D-08: `EntityType` compartilhado entre histórico de status e catálogo de tipo, apesar de vocabulário de `CHECK` diferente

`EntityType` (usado por `Reason*`) tem 3 valores (`COMPANY/EMPLOYEE/LOGIN`); `AddressType`/`ContactType` só permitem 2 no `CHECK` do banco (`COMPANY/EMPLOYEE`). Reaproveitar o mesmo enum Java para os dois (em vez de criar um segundo enum de 2 valores) é seguro nesta rodada porque nenhum usecase ainda escreve nessas tabelas — o mismatch (Java permite `LOGIN`, banco rejeitaria) só se tornaria um risco real quando o usecase de cadastro de tipo for implementado, e nesse momento o guard client-side deve ser adicionado junto (mesmo cuidado do item de risco sobre `CHK_*_STATUS_TRANSITION`).

**Alternativa descartada**: criar um enum `EntityType` separado por contexto (`EntityType` e `AddressContactEntityType`). Descartada por ser duplicação de vocabulário para um risco que só existe quando o usecase correspondente for escrito — decisão revisitável nessa ideia futura.

### D-09: `Employee.position` continua cache; sem método `changePosition()` nesta rodada

O domain model documenta `SCOS_EMPLOYEE.POSITION_ID` como cache sincronizado por trigger a partir de `SCOS_EMPLOYEE_POSITION_HISTORY`. Esta mudança cria a entidade `EmployeePositionHistory` e o repositório, mas **não** adiciona um método de domínio equivalente a `changePosition()` em `Employee` — o usecase que vai popular o histórico de cargo é não-goal explícito (fica para ideia futura, junto com os demais usecases de outbox/fiscal/catálogo). `Employee.position` permanece com setter Lombok padrão.

## Risks / Trade-offs

- **Quebra de compilação por assinatura pública mudada** → `activate/inactivate/disable` de `Company`/`Employee`/`Login` mudam de `void` sem parâmetro para retornar um histórico e exigir `reasonId`. *Mitigação*: nenhum caller existe hoje fora do próprio módulo `domain` (confirmado por busca) — impacto real zero nesta rodada.
- **Guard client-side dessincronizado do `CHECK` de banco** → se a tabela de transição implementada em Java divergir de `CHK_*_STATUS_TRANSITION`, o banco rejeita com exceção genérica do Postgres em vez de `ScosException` amigável. *Mitigação*: tabela de transição replicada exatamente da seção "Triggers" do `domain_model.md` (D-02); testes unitários devem cobrir as 6 transições válidas + as inválidas.
- **`EntityType` de 3 valores usado onde o `CHECK` só aceita 2** (D-08) → risco adiado, não eliminado. *Mitigação*: documentado como decisão explícita; guard client-side entra junto do usecase futuro de cadastro de `AddressType`/`ContactType`.
- **`SCOS_LOGIN_012` removida** → se alguma configuração externa (dashboard, alerta) referenciar o código, quebra silenciosamente. *Mitigação*: código nunca foi exposto publicamente (só usado internamente pela regra removida); baixo risco.

## Migration Plan

Nenhuma migration de banco necessária. Ordem sugerida de execução (evita quebra de compilação em cascata):

1. Enums primeiro: `StatusCompany`/`StatusEmployee` (remove `DELETED`), `LoginStatus` (remove `DELETED`/`LOCKED`, adiciona `BLOCKED`), novos enums (`EntityType`, `DayOfWeek`, `EmployeeContractType`, `OutboxBackend`, `OutboxEventStatus`)
2. Entidades de referência sem dependência (`ReasonActivate/Inactivate/Disable/Enable`, `ReasonPositionChange`, `AddressType`, `ContactType`, `LegalNature`, `Cnae`, `OutboxTopic`) + repositórios
3. Redesenho de `Company`/`Employee`/`Login` (remove `delete()`, novos métodos de transição, novos campos) — depende dos enums e das tabelas de motivo já existirem
4. Entidades de histórico/dependentes (`CompanyStatusHistory`/`EmployeeStatusHistory`/`LoginStatusHistory`, `EmployeePositionHistory`, `LoginProfile`, `CompanyCnaeSecondary`, `PositionWorkSchedule`, `EmployeeWorkSchedule`, `OutboxEvent`, `OutboxEventLog`, `OutboxEventDeadLetter`) + repositórios
5. Migração de `type: String` → FK tipada em `EmployeeAddress`/`CompanyAddress`/`EmployeeContact`/`CompanyContact`
6. Remoção de `IntegrationKeycloak*` e regras/mensagens obsoletas (`LoginDeletedRule`, `SCOS_LOGIN_012`)
7. Validar com `spring.jpa.hibernate.ddl-auto: validate` contra o banco v2

Rollback: revert do branch — nenhuma alteração de dados ou schema.

## Open Questions

Nenhuma em aberto — todos os pontos sinalizados como "validar durante o `/propose`" na ideia (`OutboxEvent`/`OutboxEventLog`/`OutboxEventDeadLetter`, `LegalNature`/`Cnae`) foram resolvidos nas decisões D-06 e D-07 acima.
