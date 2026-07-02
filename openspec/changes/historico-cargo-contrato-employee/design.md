## Context

`SCOS_EMPLOYEE`/`SCOS_POSITION` já foram sincronizados com o schema v2 na camada JPA (`atualizacao-entidades-jpa-liquibase-v2`, completo — `EmployeeContractType`, `ReasonPositionChange`, `EmployeePositionHistory` já existem como entidades de domínio). Este change cobre só a camada de contrato OpenAPI (`etc/api/organization/`), que ainda reflete o modelo v1.

`EmployeePositionHistory` é fechado por trigger de banco (`TRG_CLOSE_PREVIOUS_POSITION`) via um índice único parcial em `EMPLOYEE_ID` filtrado por `END_DATE IS NULL` — garante uma única linha "aberta" por funcionário a qualquer momento.

## Goals / Non-Goals

**Goals:**
- Expor `isTrustPosition` (`Position`), `contractType`/`probationEndDate` (`Employee`) no contrato
- Expor CRUD de referência `ReasonPositionChange`
- Tornar `reasonPositionChangeId` obrigatório em `TransferEmployeeRequest`, sempre gerando uma linha de histórico
- Expor `GET /v1/employees/{id}/position-history` paginado

**Non-Goals:**
- Implementar código (permissões, use case, domain, delegates, testes) — já feito na camada JPA na rodada anterior; o restante (delegates/use case novos, permissões) fica fora de escopo desta change, entra em change futura separada
- Alterar schema de banco (nenhuma migration nova)
- Validar client-side a coerência `probationEndDate`/`contractType=PJ` (fica para o use case, se necessário)

## Decisions

| Decisão | Escolha | Alternativa Descartada | Motivo |
|---------|---------|------------------------|--------|
| `reasonPositionChangeId` em `/transfer` | Obrigatório sempre, e sempre gera linha de histórico (mesmo sem mudar `positionId`) | Só insere histórico quando `positionId` muda | Toda transferência (empresa/supervisor/cargo) vira marco na linha do tempo do funcionário |
| Consulta de histórico de cargo | Expõe `GET /v1/employees/{id}/position-history`, paginado | Só banco, sem endpoint | Auditoria acessível via API, espelha a decisão equivalente do change `historico-status-motivos-organization` |
| `ReasonPositionChange` sem `entityType` | Catálogo dedicado, sem filtro de entidade | Reaproveitar o enum `entityType` de 3 valores dos catálogos `Reason*` de status | `ReasonPositionChange` só se aplica a `EMPLOYEE` — não é um catálogo compartilhado entre agregados, então não precisa da coluna/filtro |
| Localização do catálogo `ReasonPositionChange` | Dentro de `ScosOrganization_Department-Position.yml` | Arquivo próprio ou dentro de `Employee.yml` | Segue o agrupamento já decidido na idea (motivo de mudança de cargo é conceitualmente ligado a `Position`) |

## Risks / Trade-offs

- [Risco] `/transfer` passa a registrar toda transferência no histórico de cargo, inclusive as que só mudam empresa/supervisor, não só mudança de cargo em si → Mitigação: documentar isso claramente na `description` do `operationId` de `/transfer` e de `GET .../position-history`, para não confundir consumidores da API
- [Risco] `probationEndDate` deve ser posterior a `DATE_OF_HIRING` (restrição de integridade do banco) → Mitigação: contrato não valida isso sozinho; use case deve tratar o erro de constraint com uma `ScosException` amigável em vez de deixar o erro genérico do Postgres estourar
- [Risco] `PROBATION_END_DATE` não é aplicável a `contractType=PJ`, mas o contrato aceita `null` sem impedir preenchimento incoerente → Mitigação: considerar validação client-side/use case numa iteração futura; não bloqueante para este change
- [Trade-off] `TransferEmployeeRequest` com `reasonPositionChangeId` obrigatório é breaking change de contrato — aceito diretamente (sistema sem produção, mesma decisão já tomada nas ideias irmãs)

## Migration Plan

Não aplicável — sem alteração de schema de banco. O rollout desta change é só a alteração do contrato OpenAPI; a implementação de código fica para uma change futura separada.

## Open Questions

Nenhuma em aberto — todas as decisões relevantes já foram tomadas com o usuário na fase de ideia (`etc/doc/ideia/20260701_historico-cargo-contrato-employee.md`).
