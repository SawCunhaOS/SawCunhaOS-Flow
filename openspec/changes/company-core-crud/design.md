## Context

O agregado `Company` (entidade + `Cnpj` value object + guardas de transição de status) e o `CompanyRepository` já existem no `scos-organization-domain`; as tabelas `SCOS_COMPANY` e `SCOS_COMPANY_STATUS_HISTORY` já estão criadas via Liquibase. Falta apenas a camada de aplicação/API que expõe o CRUD cadastral (UC-001..005). O projeto segue as convenções SCOS: delegate pattern (`XxxApiDelegate`), service como interface pública + Bean package-private, erros via `ScosException`/`ExceptionsHandler` (RFC 9457), e fronteira transacional padronizada (change `padronizacao-transactional-camadas`). Recursos análogos já implementados servem de referência direta: `cnae-api`, `legal-nature-api`, e o par `ContactTypeServiceBean`/`CreateContactTypeUseCaseBean`.

## Goals / Non-Goals

**Goals:**
- Expor `POST`/`PUT`/`GET id`/`GET lista` de `/v1/companies` (UC-001..005) retornando os status HTTP e códigos de erro de `etc/doc/usecase/01-empresa.md`.
- Posicionar cada validação na camada correta: contrato (formato/DV), domain service (unicidade/FK/negócio), agregado (estado).
- Reusar o service de `ReasonActivate` para validar `reasonActivateId`.
- Cobertura de integração (Testcontainers) verde para todos os UC-S/UC-E.

**Non-Goals:**
- Transições de status e histórico (enable/disable/block/unblock, status-history) — feature própria.
- Hierarquia (hierarchy/branches), contatos e endereços da empresa.
- Regras-alvo não implementadas `SCOS_COMPANY_004/005/006` (ciclo de hierarquia, última matriz ativa, única empresa ativa).
- Qualquer mudança de schema de banco.

## Decisions

**1. Validação distribuída por camada (a validação mora onde moram os dados que ela precisa).**
- Contrato (YAML → bean-validation + validator): presença/vazio/tamanho, `taxIdentifier` 14 alfanum + DV (`x-is-cnpj`), `foundationDate` não-futura. Falha barata antes do banco.
- Domain service (`CompanyServiceBean`): unicidade de `taxIdentifier`, existência/estado de FKs, compatibilidade de motivo, profundidade de hierarquia. Precisa de repositório/outros agregados.
- Agregado (`Company`): guardas de estado (já implementadas).
- UseCase Bean: só orquestra (map DTO → chama service → map back). Nunca valida.
- *Alternativa descartada:* validar tudo no UseCase — acopla regra a orquestração e quebra o padrão SCOS existente.

**2. Service como interface pública + Bean package-private.** `CompanyService` (specification) + `CompanyServiceBean` (`@Service`, package-private), espelhando `ContactTypeServiceBean`. Métodos de escrita `@Transactional`; leitura `@Transactional(readOnly = true)`.
- *Alternativa descartada:* lógica no delegate/usecase — viola SRP e o padrão do projeto.

**3. Reuso do service de `ReasonActivate`** para checar existência + `ACTIVE=true` + `entityType=COMPANY`, em vez de reimplementar a checagem no `CompanyServiceBean`. DRY; a regra já está centralizada.

**4. Idempotência apenas no POST** via `@JdempotentRequestPayload` sobre `taxIdentifier` (já marcado no contrato). PUT não é idempotente por esse mecanismo — atualização é naturalmente repetível mas não deve mascarar conflitos de unicidade.

**5. Ordem de avaliação de erros fixada:** formato (`400`) → unicidade (`409`) → existência de FK (`404`) → regra de negócio (`422`), conforme `01-empresa.md`. É contrato observável (testado), não detalhe interno.

## Risks / Trade-offs

- **Ordem de erro divergente do documentado** → testes de integração asseguram a sequência `400 → 409 → 404 → 422` por cenário.
- **`reasonActivateId` sem código de erro dedicado** (só `404`/`422` textual) → alinhar mensagens ao bundle i18n existente; não inventar código novo.
- **`ScosOrganizationPermission` desatualizado** (00-indice §3.6) → Spring Security valida por string em `hasAuthority`, então não bloqueia; adicionar `CREATE_COMPANY`/`UPDATE_COMPANY`/`GET_COMPANY` ao enum por consistência se ausentes.
- **Profundidade de hierarquia** exige percorrer a cadeia de `parentCompanyId` → limitar leitura ao necessário e cobrir o caso de borda `= COMPANY_HIERARCHY_MAX_DEPTH`.
- **`sectorOfActivity`** é texto livre (`VARCHAR(100)`), não enum — não confundir com `cnaePrincipalId`/`legalNatureId`; validar só presença + tamanho.

## Migration Plan

Não há migração de dados nem mudança de schema. Deploy é aditivo: novos beans/endpoints. Rollback = reverter o artefato (endpoints deixam de responder, sem estado residual). Sem feature flag necessária.

## Open Questions

Nenhuma. A dúvida sobre a fonte da verdade de `sectorOfActivity` foi resolvida (texto livre `VARCHAR(100)`, sem lista fixa) e `01-empresa.md` foi corrigido.
