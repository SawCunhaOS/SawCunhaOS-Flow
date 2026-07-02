## Context

`SCOS_COMPANY` (schema v2, change `adequacao-liquibase-domain-model-v2`, já aplicada) ganhou 4 colunas fiscais e 3 tabelas novas de catálogo/associação. O contrato OpenAPI (`etc/api/organization/ScosOrganization_Company.yml`) ainda reflete o schema v1 e não expõe nada disso. Este change é só o contrato — a camada JPA já foi sincronizada em `atualizacao-entidades-jpa-liquibase-v2` (completa).

## Goals / Non-Goals

**Goals:**
- `Company` expõe `legalNatureId`, `cnaePrincipalId`, `stateRegistration`, `municipalRegistration`
- `LegalNature`/`Cnae` têm CRUD de referência mínimo, compatível com o schema real do banco (sem colunas de auditoria)
- `CompanyCnaeSecondary` tem sub-recurso de associação N:N sem update

**Non-Goals:**
- Seed de dados oficiais IBGE — decidido fora de escopo desde a change Liquibase v2
- Qualquer alteração de schema de banco — nenhuma migration nova nesta change
- Implementação de código (permissões, use case, domain, delegates, testes) — fora de escopo desta change; entra em change futura separada

## Decisions

**`LegalNature`/`Cnae` sem campos de auditoria.** O banco (`SCOS_LEGAL_NATURE`/`SCOS_CNAE`) só tem `CODE`/`DESCRIPTION`/`CREATED_AT` — sem `UPDATED_AT`/`USER_AT`/`ACTIVE`. Alternativa descartada: abrir migration nova para igualar ao padrão `Department`/`ReasonX` (com `active`, soft-delete). Rejeitada para não abrir change de banco só para isso — o schema mínimo já existente é aceito como está.

**`DELETE` físico real em `LegalNature`/`Cnae`.** Como não existe estado inativo (`active`), não há como fazer soft-delete. `DELETE /v1/legal-natures/{id}` remove a linha de verdade; se houver referência em `Company.legalNatureId` ou `CompanyCnaeSecondary`, a constraint de FK do banco rejeita — o use case deve traduzir isso para um `4XX` de negócio, não deixar subir como erro genérico de banco.

**Catálogos vivem dentro de `ScosOrganization_Company.yml`, não em arquivo próprio.** Alternativa descartada: arquivo dedicado (padrão usado para o catálogo compartilhado `AddressType`/`ContactType`, ver change `catalogo-tipo-endereco-contato`). Rejeitada porque `LegalNature`/`Cnae` são exclusivos de `Company` — nenhum outro agregado os referencia, então não há motivo para extrair para um arquivo central.

**`CompanyCnaeSecondary` sem `PUT`.** É uma associação pura (`companyId`+`cnaeId`, PK composta) — não há nada para atualizar num relacionamento N:N; a operação é sempre criar ou remover o par.

**Todas as listas usam `paginationFilter`/`paginatedDTO`, mesmo os catálogos pequenos.** Consistência com o resto do contrato (nenhuma lista do `ScosOrganization_Company.yml` atual foge da paginação) — não abrir uma exceção aqui só porque o volume esperado é baixo.

## Risks / Trade-offs

- [Risco] `DELETE /v1/legal-natures/{id}` (ou `/v1/cnaes/{id}`) num registro referenciado → erro genérico de FK do Postgres. **Mitigação**: use case valida referência antes do delete e devolve `ScosException` com `4XX` amigável (tarefa explícita em `tasks.md`).
- [Risco] `cnaePrincipalId` de `Company` pode coincidir com um `Cnae` já cadastrado como secundário em `CompanyCnaeSecondary` — nenhuma constraint de banco impede. **Mitigação**: não bloqueado nesta rodada; documentado como possível validação futura de use case, fora do escopo do contrato.
- [Trade-off] Catálogos sem soft-delete/`active` significam que qualquer "desativação" futura de uma natureza jurídica ou CNAE exigiria uma migration nova — aceito conscientemente para não abrir change de banco agora.

## Migration Plan

Não aplicável — nenhuma mudança de schema de banco. O rollout desta change é só de contrato (YAML); código (use cases/permissions) fica para uma change futura, sem necessidade de estratégia de migração de dados.

## Open Questions

Nenhuma pendente — todas as decisões de arquitetura já foram fechadas na fase de ideia (`etc/doc/ideia/20260701_dados-fiscais-company.md`).
