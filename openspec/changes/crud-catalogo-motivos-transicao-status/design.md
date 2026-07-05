## Context

As 4 entidades JPA (`ReasonActivate`/`ReasonInactivate`/`ReasonDisable`/`ReasonEnable`) e seus repositórios vazios já existem em `scos-organization-domain/.../access/status/internal/`, junto com o enum compartilhado `EntityType` (3 valores: `COMPANY`/`EMPLOYEE`/`LOGIN`) e as 3 entidades `*StatusHistory`. O contrato OpenAPI dos 24 endpoints já está fechado. Falta toda a camada de código (domain service, use case, delegate, permissão, exceções). O padrão de referência é `crud-cargo-position` (Department/Position), já implementado.

## Goals / Non-Goals

**Goals:**
- Implementar os 24 endpoints (6 por catálogo × 4 catálogos) sobre as entidades/tabelas já existentes
- Reaproveitar exatamente o padrão delegate → usecase → specification/service → mapper → repository já validado por Department/Position
- Cobrir as 16 mensagens de erro (404/409/422 × 4 catálogos) via `ExceptionCodeError` + bundle i18n

**Non-Goals:**
- Não implementa `reasonId` nos fluxos `enable`/`disable`/`block`/`unblock` de Company/Employee/Login (capability `status-transition-contract`, fora de escopo)
- Não implementa `GET .../status-history` (capability `status-history-query`, fora de escopo)
- Não corrige a doc 05 (`entityType` 2→3 valores) — só o código segue o contrato
- Não cria testes de integração com Testcontainers nesta etapa

## Decisions

| Decisão | Escolha | Alternativa descartada | Motivo |
|---|---|---|---|
| Pacote das 4 entidades | `access/status/{dto,service,specification}` compartilhado, classes por entidade | Pacote próprio por entidade (`access/status/reasonactivate/...`) | `internal/` já bundla as 4 entidades (+ `EntityType`, +3 `StatusHistory`) na mesma granularidade hoje |
| `entityType` — 2 vs. 3 valores | 3 valores (`COMPANY`/`EMPLOYEE`/`LOGIN`), conforme contrato YAML (`ReasonEntityType`) e `EntityType.java` | 2 valores conforme doc 05 | Confirmado com usuário: contrato prevalece sobre doc (regra do projeto — contrato antes do código); doc 05 está desatualizada |
| Unicidade de `code` | Escopo por catálogo **e** por `entityType` (4 tabelas independentes, `existsByCodeAndEntityType`/`existsByCodeAndEntityTypeAndNotId` por repositório — mesmo `code` é permitido em `entityType`s diferentes dentro do mesmo catálogo) | Unicidade só por catálogo (code global dentro da tabela, ignorando `entityType`); unicidade cross-catálogo | `code` e `entityType` juntos formam a chave natural do motivo (mesmo padrão de `AddressType`/`ContactType`); doc 05 e schema físico tratam os 4 catálogos como tabelas distintas |
| `disable` de motivo referenciado em histórico | Bloqueia só novo uso, não quebra histórico existente (FK em `*StatusHistory`) | Impedir disable se houver qualquer referência histórica | Mesma regra já adotada nos catálogos de endereço/contato — motivo inativo continua legível no histórico |
| Categorias de erro (`SCOS_TITLE_*`) | Reaproveitar as 7 categorias já existentes (`NOT_FOUND`/`CONFLICT`/`BUSINESS_RULE_VIOLATION`/etc.) | Criar categorias novas por catálogo | Construtor de 3 args de `ExceptionCodeError` já entrega code/httpCode/title — nenhum caso novo de categoria aparece nesses 24 endpoints |

## Risks / Trade-offs

- [Risco] DTOs OpenAPI gerados (`ReasonEntityType`) podem não refletir os 3 valores no momento da escrita do `ApiMapper`, causando mismatch de enum em build → Mitigação: conferir o gerado antes de escrever cada `ApiMapper`; se divergir, é bug no gerador/contrato, não decisão de design
- [Risco] `enable`/`disable` não-idempotente gerar comportamento inconsistente entre os 4 catálogos → Mitigação: mesma regra em todos os 4 `ServiceBean` (422 se já no estado alvo), coberta por `ServiceBeanTest`
- [Trade-off] Duplicação de código entre os 4 catálogos (mesma estrutura repetida 4×) em vez de abstração genérica única → aceito: mesma decisão já tomada por Department/Position/AddressType/ContactType; abstração prematura custaria mais do que a repetição controlada

## Migration Plan

Não aplicável — sem mudança de schema (tabelas já existem) nem de contrato OpenAPI. Deploy é adição pura de código; sem estado a migrar, sem rollback especial além do reverter do deploy.

## Open Questions

Nenhuma em aberto — divergência doc vs. contrato já resolvida com o usuário (contrato prevalece).
