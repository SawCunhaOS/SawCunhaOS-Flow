## Context

O contrato OpenAPI de Natureza Jurídica (`/v1/legal-natures`, UC-093…UC-097) já está fechado: delegates gerados (`LegalNatureApiDelegate`), entidade `LegalNature` (`corporate/company/internal/LegalNature.java` — `id`, `code`, `description`, `createdAt`, `@Auditable`) e tabela `SCOS_LEGAL_NATURE` (Liquibase) existem. Falta toda a implementação de runtime.

Esta change é o gêmeo do `crud-cnae` (já implementado): mesmo padrão de camadas (`dto`/`specification`/`service`, Use Cases interface+Bean, `Delegate`), mesma resolução do conflito de tamanho de `code`, e mesma decisão de guarda de exclusão na aplicação. A única diferença estrutural: a guarda de exclusão tem **um único vínculo** a checar (`SCOS_COMPANY.legalNatureId`), pois não existe tabela de natureza jurídica secundária (o CNAE tinha `SCOS_COMPANY_CNAE_SECONDARY` além do principal).

Assim como o CNAE, `LegalNature` **não estende `BaseEntity`** — só possui `createdAt` (`@CreationTimestamp`); logo o `ServiceBean` não chama `updateAuditInfo` (a trilha de auditoria vem do `@Auditable`).

## Goals / Non-Goals

**Goals:**
- Implementar os 5 endpoints de Natureza Jurídica respondendo os status corretos (200/201/204/400/404/409/422).
- Garantir unicidade de `code` em `SCOS_LEGAL_NATURE`.
- Impedir exclusão de natureza jurídica ainda vinculada a empresa com erro de domínio explícito (`422 SCOS_LEGAL_NATURE_003`).
- Reutilizar o padrão `CnaeService`/`CnaeServiceBean` para consistência do módulo.

**Non-Goals:**
- CRUD de CNAE (change `crud-cnae`).
- Status/ativação de natureza jurídica — a entidade não possui `active`.
- Redesenho do contrato OpenAPI — apenas `+maxLength` em `code`/`description`.

## Decisions

### D1 — Localização em `company/fiscal`, não em `catalog`
A entidade `LegalNature` vive em `corporate/company/internal`. A camada de domínio nova (`dto`/`specification`/`service`) fica sob `corporate/company/`, e os Use Cases sob `application/usecase/corporate/company/fiscal/legalnature/`. O delegate fica em `api/delegate/company/`. Mesmo critério do `crud-cnae`.

### D2 — Manter a camada de domínio `Service`/`ServiceBean`
Cria-se `LegalNatureService` (interface em `specification/`) + `LegalNatureServiceBean` (impl em `service/`) + `LegalNatureMapper`, espelhando `Cnae`. A regra de unicidade e a guarda de exclusão residem no `LegalNatureServiceBean`.

### D3 — Guarda de exclusão na aplicação/domínio, sem trigger de bloqueio
`DELETE` é físico. Antes de remover, checa `CompanyRepository.existsByLegalNatureId(id)` (novo default method QueryDSL sobre `qCompany.legalNature.id` — a associação é `@ManyToOne LegalNature legalNature` com `@JoinColumn(LEGAL_NATURE_ID)`). Verdadeiro → `ScosException(SCOS_LEGAL_NATURE_003)` (422). Como `legalNatureId` é FK **nullable** e opcional, a checagem cobre empresas de qualquer status.
- **Alternativa descartada**: `trg_block_delete_*` como nos catálogos de tipo. Rejeitada porque bloqueia **toda** exclusão física; a guarda condicional pertence à aplicação.

### D4 — Unicidade global de `code`, case-sensitive, sem normalização
`code` é único em toda a tabela `SCOS_LEGAL_NATURE` — respaldado pela constraint `UK_CODE_SCOS_LEGAL_NATURE` já existente. Repositório expõe `existsByCode(String)` e `existsByCodeAndNotId(String, Long)` via `qLegalNature`. Comparação exata (`code.eq`), sem `trim`/`upper` — consistente com `Cnae`/`ContactType`.

### D5 — Códigos de erro reutilizam categorias de título existentes
Novas constantes em `ExceptionCodeError`: `SCOS_LEGAL_NATURE_001` (404), `SCOS_LEGAL_NATURE_002` (409), `SCOS_LEGAL_NATURE_003` (422). Mensagens de `detail` em `scos_message_organization[_en].properties`.

### D6 — Tamanho de `code` = 30, via migração + `maxLength` no contrato
Fontes divergiam: DB `varchar(10)`, doc UC-094 `≤ 30`, contrato sem `maxLength`. Decisão: alinhar a **30** (mesmo precedente do `crud-cnae`, já ratificado).
- Novo changeSet Liquibase `ALTER COLUMN SCOS_LEGAL_NATURE.CODE` de `varchar(10)` → `varchar(30)` (preserva `UK_CODE_SCOS_LEGAL_NATURE`; `DESCRIPTION` já é `varchar(255)`).
- `maxLength: 30` (`code`) e `maxLength: 255` (`description`) nos schemas `CreateLegalNatureRequest`/`UpdateLegalNatureRequest` → `@Size` gerado → `400` limpo antes de tocar o banco.

## Risks / Trade-offs

- **Guarda considerando só status ativos** → excluiria natureza jurídica ainda referenciada por empresa inativa/bloqueada, deixando FK órfã. Mitigação: `existsByLegalNatureId` não filtra por status (cobre qualquer empresa); cenário coberto por unit test.
- **Corrida entre checagem de unicidade e insert concorrente** → mitigada pela constraint UNIQUE `UK_CODE_SCOS_LEGAL_NATURE` (a checagem de aplicação dá mensagem amigável; o UNIQUE garante integridade).
- **Migração `ALTER COLUMN` em produção** → alargar `varchar(10)→(30)` é seguro (não trunca; tabela de referência sobe vazia).

## Open Questions

- Nenhuma pendente. Constraint `UK_CODE_SCOS_LEGAL_NATURE` confirmada; tamanho de `code` resolvido (30, alinhado ao `crud-cnae`).
