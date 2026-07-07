## Context

O contrato OpenAPI de CNAE (`/v1/cnaes`, UC-098…UC-102) já está fechado: delegates gerados (`CnaeApiDelegate`), entidade `Cnae` (`corporate/company/internal/Cnae.java` — `id`, `code`, `description`, `createdAt`, `@Auditable`) e tabela `SCOS_CNAE` (Liquibase) existem. Falta toda a implementação de runtime. O padrão de catálogo já consolidado no módulo é o de `ContactType` (change `crud-catalogo-tipo-endereco-contato`, completo): camada de domínio `dto`/`specification`/`service`, repositório com default methods QueryDSL, Use Cases (interface pública + `@Service` Bean package-private) e `Delegate`.

CNAE difere de `AddressType`/`ContactType` em dois pontos: (1) **não tem status** (`active`) — logo sem `enable`/`disable`; (2) admite **exclusão física guardada**, enquanto os catálogos de tipo são append-only (protegidos por `trg_block_delete_*`).

## Goals / Non-Goals

**Goals:**
- Implementar os 5 endpoints de CNAE respondendo os status corretos (200/201/204/400/404/409/422).
- Garantir unicidade de `code` em `SCOS_CNAE`.
- Impedir exclusão de CNAE ainda vinculado a empresa (principal ou secundário) com erro de domínio explícito (`422 SCOS_CNAE_003`).
- Reutilizar o padrão `ContactTypeService`/`ContactTypeServiceBean` para consistência do módulo.

**Non-Goals:**
- CRUD de Natureza Jurídica (change própria).
- Vínculo de CNAEs secundários da empresa (UC-103/104/105, seção 1.3).
- Status/ativação de CNAE — a entidade não possui `active`.
- Redesenho do contrato OpenAPI — apenas `+maxLength` em `code`/`description` (sem novos endpoints/schemas).

## Decisions

### D1 — Localização em `company/fiscal`, não em `catalog`
A entidade `Cnae` vive em `corporate/company/internal`. A camada de domínio nova (`dto`/`specification`/`service`) fica sob `corporate/company/`, e os Use Cases sob `application/usecase/corporate/company/fiscal/cnae/`. O delegate fica em `api/delegate/company/`.
- **Alternativa descartada**: colocar em `corporate/catalog/` junto de `ContactType`/`AddressType`. Rejeitada porque CNAE é sub-entidade fiscal de Company (referenciada por `cnaePrincipalId`/secundário), não um catálogo de tipo genérico — decisão do usuário.

### D2 — Manter a camada de domínio `Service`/`ServiceBean`
Mesmo sendo um CRUD simples, cria-se `CnaeService` (interface em `specification/`) + `CnaeServiceBean` (impl em `service/`) + `CnaeMapper`, espelhando `ContactType`. Os Use Cases orquestram; a regra de unicidade e a guarda de exclusão residem no `CnaeServiceBean`.
- **Alternativa descartada**: Use Case chamando o repositório direto. Rejeitada por quebrar a convenção SCOS (regra de domínio no domain service) já usada em todo o módulo.

### D3 — Guarda de exclusão na aplicação/domínio, sem trigger de bloqueio
`DELETE` é físico. A guarda faz duas checagens antes de remover:
- `CompanyRepository.existsByCnaePrincipalId(id)` (novo default method QueryDSL sobre `qCompany.cnaePrincipalId`);
- `CompanyCnaeSecondaryRepository.existsByCnaeId(id)` (novo default method sobre `qCompanyCnaeSecondary.id.cnaeId` — a PK composta `CompanyCnaeSecondaryPk` tem o campo `cnaeId`).
Qualquer verdadeiro → `ScosException(SCOS_CNAE_003)` (422).
- **Alternativa descartada**: `trg_block_delete_*` como nos catálogos de tipo. Rejeitada porque esse trigger bloqueia **toda** exclusão física (append-only), enquanto CNAE deve permitir exclusão quando não há vínculo. A guarda condicional pertence à aplicação.

### D4 — Unicidade global de `code`, case-sensitive, sem normalização
`code` é único em toda a tabela `SCOS_CNAE` (não há dimensão `entityType` como em `ContactType`) — respaldado pela constraint `UK_CODE_SCOS_CNAE` já existente. Repositório expõe `existsByCode(String)` e `existsByCodeAndNotId(String, Long)` via `BooleanBuilder`/`qCnae`. Segue o comportamento de `ContactType`: comparação exata (`code.eq`), sem `trim`/`upper` — mantém consistência; normalização não é introduzida aqui.

### D6 — Tamanho de `code` = 30, via migração + `maxLength` no contrato
Fontes divergiam: DB `varchar(10)`, doc UC-099 `≤ 30`, contrato sem `maxLength`. Decisão (usuário): alinhar a **30**.
- Novo changeSet Liquibase `ALTER COLUMN SCOS_CNAE.CODE` de `varchar(10)` → `varchar(30)` (preserva `UK_CODE_SCOS_CNAE`; sem alterar `DESCRIPTION` que já é `varchar(255)`).
- `maxLength: 30` (`code`) e `maxLength: 255` (`description`) nos schemas `CreateCnaeRequest`/`UpdateCnaeRequest` → `@Size` gerado → `400` limpo antes de tocar o banco.
- **Alternativa descartada**: manter `varchar(10)` (mais próximo do estado atual, mas contradiz o UC documentado); ou não validar no contrato (deixaria `code>30` estourar `DataIntegrityViolation`/500). A validação no contrato fecha a lacuna que `AddressType`/`ContactType` deixaram.

### D5 — Códigos de erro reutilizam categorias de título existentes
Novas constantes em `ExceptionCodeError`: `SCOS_CNAE_001` (404, `SCOS_TITLE_NOT_FOUND`), `SCOS_CNAE_002` (409, `SCOS_TITLE_CONFLICT`), `SCOS_CNAE_003` (422, `SCOS_TITLE_BUSINESS_RULE_VIOLATION`). Mensagens de `detail` em `scos_message_organization[_en].properties`. Nenhuma categoria `SCOS_TITLE_*` nova.

## Risks / Trade-offs

- **Guarda de exclusão incompleta (só principal)** → deixaria CNAE órfão referenciado como secundário. Mitigação: a spec exige cenário de teste para ambas as referências; teste de integração Testcontainers cobre principal e secundário.
- **Corrida entre checagem de unicidade e insert concorrente** → dois inserts simultâneos com o mesmo `code` poderiam passar. Mitigação: constraint UNIQUE em `SCOS_CNAE.code` no banco (verificar no `scos_cnae.yml`); a checagem de aplicação dá a mensagem amigável, o UNIQUE garante a integridade.
- **Comparação de `code` case-sensitive** → `1234` e `1234 ` (com espaço) seriam considerados distintos. Aceito por consistência com `ContactType`; normalização fica como decisão global futura, fora desta change.
- **Migração `ALTER COLUMN` em produção** → alargar `varchar(10)→(30)` é operação segura (não trunca dados existentes; tabela sobe vazia por ser de referência). Sem risco de rollback destrutivo.

## Open Questions

- Nenhuma pendente. Constraint `UK_CODE_SCOS_CNAE` confirmada em `scos_cnae.yml`; tamanho de `code` resolvido (30).
