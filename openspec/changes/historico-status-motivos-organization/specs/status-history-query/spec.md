## ADDED Requirements

### Requirement: Consulta paginada ao histórico de status
`Company`, `Employee` e `Login` SHALL expor `GET .../{id}/status-history` (paginado, somente leitura), retornando `status`, `previousStatus`, o motivo associado (qualquer um dos 4 FKs de motivo que estiver preenchido), `observation`, `userAt` e `createdAt`.

#### Scenario: Consultar histórico de status de uma empresa
- **WHEN** um cliente autorizado envia `GET /companies/{id}/status-history` com `paginationFilter`
- **THEN** o sistema retorna `200 OK` com `data: array` (cada item com `status`, `previousStatus`, motivo, `observation`, `userAt`, `createdAt`) e `paginatedDTO`

#### Scenario: Histórico reflete transições anteriores
- **WHEN** uma empresa passou por `activate` → `disable` → `enable`
- **THEN** `GET /companies/{id}/status-history` retorna 3 linhas, uma por transição, na ordem cronológica

### Requirement: status-history é somente leitura
`GET .../status-history` SHALL ser a única operação exposta para as tabelas `*_STATUS_HISTORY` — não deve existir nenhum `POST`/`PUT`/`DELETE` direto sobre o histórico, que é populado exclusivamente como efeito colateral das rotas de transição.

#### Scenario: Nenhuma rota de escrita para status-history
- **WHEN** o contrato de `Company`/`Employee`/`Login` é inspecionado
- **THEN** só existe `GET .../status-history`, sem `POST`/`PUT`/`DELETE` equivalente
