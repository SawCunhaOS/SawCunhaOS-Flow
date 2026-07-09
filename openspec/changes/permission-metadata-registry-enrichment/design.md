## Context

O registro de permissões é feito no startup: o consumidor (`scos-security-starter`) lê o enum `ScosOrganizationPermission`, monta o proto `Resource` e chama o servidor via gRPC (`registryResources`), que persiste em `SCOS_RESOURCE`. A interface `ScosPermission` já foi editada com métodos novos (`getCodeDescription`, `getGroup`, `getSubGroup`, `getVersion`, `getUpdatedAt`), quebrando a compilação porque o enum e o `ScosSystemRegistrationService` não acompanharam.

Estado atual relevante:
- `ResourceServiceBean.register` faz `scosSystemRepository.findByCode(...).get()` (estoura `NoSuchElementException` se o sistema não existe), depois `findIdByCodeAndSystemCode` + `merge`/`update` (JPA).
- `SCOS_RESOURCE` tem `UK_CODE_SCOS_RESOURCE` **só em `CODE`**; `CREATED_AT` default `NOW()`; `UPDATED_AT`/`USER_AT` são `NOT NULL` **sem default**.
- `GrpcGlobalExceptionHandler.handleScosException` devolve **sempre** `Status.INTERNAL` — o motivo do erro não chega ao client.
- `ScosException` carrega só `code` (string) + `args`; o `httpCode` mora no enum `ExceptionCodeError` (módulo `shared`).
- Descrições pt/en estão hardcoded nas ~110 constantes do enum; erros já usam bundle i18n (`scos_message_organization*`).

Restrições: sistema não publicado (pode editar `scos_resource.yml` direto, sem migration incremental); login/validação usam só o array `permission` das views (`vw_authority_response`, `vw_login_context`) — não podem ser afetados.

## Goals / Non-Goals

**Goals:**
- Restaurar a compilação alinhando interface ↔ enum ↔ registration service.
- Persistir metadados completos da permissão (`group`, `subGroup`, `version`, `updatedAt`, descrição pt/en resolvida) em `SCOS_RESOURCE`, no insert e no update.
- Substituir o insert/update do resource por **upsert nativo condicional** (1 query), atômico no lote.
- Tornar o motivo do erro **visível entre módulos** no gRPC.
- Mover descrições da permissão para bundle i18n dedicado.

**Non-Goals:**
- Endpoint REST de listagem de resources agrupados para o front (arquivo/change próprio).
- Reconciliar permissões deletadas do enum (registry só faz upsert; política: manter constante com `active=false`).
- Alterar login/validação — array `permission` inalterado.
- Manter trilha `@Auditable` no upsert nativo (aceito perder — resource é metadado auto-registrado).

## Decisions

**D1 — Upsert nativo condicional (`INSERT ... ON CONFLICT (CODE) DO UPDATE ... WHERE`).**
`@Modifying @Query(nativeQuery = true)` em `ResourceRepository.upsert(...)`, chamado por `ResourceServiceBean`. Conflito em `(CODE)` (constraint `UK_CODE_SCOS_RESOURCE`; `CODE` é globalmente único). Colunas de auditoria no próprio SQL (`CREATED_AT`/`UPDATED_AT = NOW()`, `USER_AT = :userAt`). O ramo `DO UPDATE` traz `WHERE SCOS_RESOURCE.ACTIVE IS DISTINCT FROM EXCLUDED.ACTIVE OR SCOS_RESOURCE.DEFINITION_UPDATED_AT IS DISTINCT FROM EXCLUDED.DEFINITION_UPDATED_AT`.
- *Alternativa descartada*: `findId` + `merge`/`update` (JPA) — 2+ idas ao banco, não condicional, e todo restart re-grava o lote inteiro bumpando `UPDATED_AT`/gerando dead tuples. `IS DISTINCT FROM` cobre null-safety.
- *Alternativa descartada*: `ON CONFLICT (CODE, SYSTEM_ID)` — não há constraint composta.

**D2 — Convenção do gatilho de update: `updatedAt` bumpa em qualquer mudança da definição.**
O `WHERE` do upsert olha só `active` e `definition_updated_at`. Logo, editar descrição/grupo/subgrupo/version **sem** bumpar `updatedAt` não persiste. Convenção oficial: toda alteração da definição da permissão incrementa `updatedAt` na constante do enum.
- *Alternativa considerada*: comparar todos os campos com `IS DISTINCT FROM` no `WHERE`. Descartada por acoplar o SQL a cada campo e por o requisito ser explicitamente "atualiza só se `active` ou a data mudarem".

**D3 — Resolução i18n no consumidor.**
`ScosSystemRegistrationService` resolve `codeDescription` → pt/en via `MessageSource` no startup e envia o texto resolvido no proto (`description_pt`/`description_en`). Bundle novo `messages_permission*` no módulo do enum (`infrastructure`), com `MessageSource` **qualificado** (não colide com o primário).
- *Alternativa descartada*: registry guardar só a chave e o front resolver — o consumidor é dono do i18n (igual às mensagens de erro) e o registry fica com texto pesquisável.

**D4 — Colunas próprias `VERSION` + `DEFINITION_UPDATED_AT`.**
Versionamento da **definição** ≠ auditoria de linha (`UPDATED_AT`). Colunas separadas evitam corromper o significado do audit. Coluna de grupo é `RESOURCE_GROUP` (`GROUP` é reservado SQL).

**D5 — Propagação do motivo de erro no gRPC.**
`GrpcGlobalExceptionHandler.handleScosException` resolve `ex.getCode()` → `ExceptionCodeError` (via `valueOf`, pois o nome da constante == `code`) → `httpCode` → `Status` de negócio: 404→`NOT_FOUND`, 409→`ALREADY_EXISTS`, 422→`FAILED_PRECONDITION`, 401→`UNAUTHENTICATED`, 400→`INVALID_ARGUMENT`, resto→`INTERNAL`. Anexa o `code` num `io.grpc.Metadata` (trailer) e passa `ex.getArgs()` ao `getMessage`. Sistema inexistente vira `ScosException(SCOS_SYSTEM_001)` (404) — novo código no enum + mensagem pt/en. Client (`ScosSystemRegistrationService`) lê `description` + `code` do trailer e loga o motivo real.
- *Alternativa descartada*: manter `Status.INTERNAL` com descrição — o client não distingue not-found/conflito/regra de negócio.

**D6 — Registro em lote atômico.**
A fronteira transacional sobe para o registro do lote (`@Transactional(rollbackFor = ScosException.class)` cobrindo a iteração inteira): falha em 1 resource faz rollback de todos, evitando estado parcial.

## Risks / Trade-offs

- **[Conditional Upsert cego a descrição/grupo/version]** editar esses campos sem bumpar `updatedAt` não persiste → **Mitigação**: convenção D2 documentada + teste que prova o gatilho; alternativa (comparar todos os campos) registrada.
- **[`MessageSource` cross-módulo]** bundle vive no consumidor, starter é genérico → **Mitigação**: bean `MessageSource` qualificado por basename `messages_permission`, sem colidir com o primário; definir na implementação quem declara o bean.
- **[Renomear `updateAt`→`updated_at` no proto]** quebra stubs → **Mitigação**: regenerar em grpc-proto/grpc-boot/starter num único passo; proto3 sem Date → transportar ISO-8601 (`string`) e parsear para `LocalDate` no servidor.
- **[`@Modifying @Query` nativo no `BaseJpaRepository` (hypersistence)]** conviver com `merge`/`update` da lib → **Mitigação**: validar em teste de integração; `@Modifying(clearAutomatically = true)` se houver leitura da entidade na mesma transação após o upsert.
- **[Trilha `@Auditable` não dispara no upsert nativo]** → **Aceito**: resource é metadado auto-registrado; auditoria fica nas colunas próprias (RNF-04).
- **[Null-safety]** `getActive()` (`Boolean`) × proto `active` (`bool`) × `Resource.active` (`boolean`) → **Mitigação**: garantir não-nulo na origem (enum).
- **[Deletar constante do enum]** deixa linha `ACTIVE=true` órfã → **Mitigação**: política de manter constante com `active=false` (registry só faz upsert).

## Migration Plan

1. `scos_resource.yml`: adicionar `RESOURCE_GROUP`, `SUB_GROUP`, `VERSION`, `DEFINITION_UPDATED_AT` (edição direta — sistema não publicado; drop/recreate do schema no ambiente de dev).
2. Fechar interface → refatorar enum + bundles → `MessageSource` → registration service → proto/stubs → cadeia usecase/domain → upsert/handler.
3. Rollback: reverter changelog (`SCOS_RESOURCE` volta ao schema anterior) e o proto (`updated_at`→`updateAt`); mudança é pré-publicação, sem dado produtivo a migrar.

## Open Questions

- Nulabilidade final de `RESOURCE_GROUP`/`SUB_GROUP`/`VERSION`/`DEFINITION_UPDATED_AT` (provável `nullable: false` com valor em todas as constantes) — confirmar na implementação.
- Quem declara o bean `MessageSource` de permissão (starter via propriedade de basename × consumidor expõe o bean).
