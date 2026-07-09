## Why

A interface `ScosPermission` já ganhou campos novos (`getCodeDescription`, `getGroup`, `getSubGroup`, `getVersion`, `getUpdatedAt`), mas o resto da cadeia de registro não acompanhou — **o projeto não compila** (`ScosOrganizationPermission` não implementa os novos métodos; `ScosSystemRegistrationService` ainda chama `getDescriptionPtBr/getDescriptionEng`). Além disso, as descrições pt/en estão hardcoded em ~110 constantes do enum (sem i18n), o registro do resource não persiste `group`/`subGroup`/`version`/`updatedAt` (o front não consegue agrupar/atribuir permissões), o upsert atual usa `findByCode().get()` + `merge`/`update` (frágil e não condicional), e qualquer falha vira `Status.INTERNAL "Internal server error"` no gRPC — o motivo real fica escondido entre módulos.

## What Changes

- Fechar o contrato da interface `ScosPermission` e refatorar o enum `ScosOrganizationPermission` para carregar metadados (`codeDescription`, `group`, `subGroup`, `version`, `updatedAt`, `active`) — sem mais strings pt/en inline. **BREAKING** (assinatura da interface e forma das constantes).
- Novo bundle i18n `messages_permission.properties`/`_en` (chaveado por `codeDescription`) + `MessageSource` dedicado; `ScosSystemRegistrationService` resolve pt/en no startup e preenche o proto `Resource` completo.
- Propagar `group`/`subGroup`/`version`/`updatedAt` por toda a cadeia servidora (`RegistreServiceImpl` → `RegistryResourceInput` → `RegisterResourceInput` → `Resource` → `SCOS_RESOURCE`) no insert **e** no update.
- Trocar o insert/update do resource por **upsert nativo condicional** PostgreSQL (`INSERT ... ON CONFLICT (CODE) DO UPDATE ... WHERE active/definition_updated_at IS DISTINCT FROM EXCLUDED`) — 1 query, colunas de auditoria no próprio SQL, e o `DO UPDATE` só dispara quando `active` ou a data da definição muda.
- Registro do lote **atômico** (`@Transactional(rollbackFor = ScosException.class)` cobrindo a iteração inteira) e `sistema inexistente` vira `ScosException(SCOS_SYSTEM_001)` (404) em vez de `NoSuchElementException`.
- **Propagação do motivo de erro entre módulos**: `GrpcGlobalExceptionHandler` mapeia `ScosException` para `Status` de negócio (404 `NOT_FOUND`, 409 `ALREADY_EXISTS`, 422 `FAILED_PRECONDITION`, …) + anexa o `code` no trailer `Metadata`; o client lê `description` + `code` e loga o motivo real.
- Colunas novas em `SCOS_RESOURCE`: `RESOURCE_GROUP`, `SUB_GROUP`, `VERSION`, `DEFINITION_UPDATED_AT` (edição direta do `scos_resource.yml` — sistema não publicado).

## Capabilities

### New Capabilities
- `permission-registry-enrichment`: fluxo de registro da permissão de ponta a ponta — resolução i18n da descrição no startup, propagação dos metadados pelo pipeline gRPC, upsert nativo **condicional** do resource, registro em lote atômico e propagação do motivo de erro entre módulos (gRPC `Status` de negócio + `code` no trailer).

### Modified Capabilities
- `model-system`: a entidade `Resource`/tabela `SCOS_RESOURCE` ganha os campos/colunas `resourceGroup`, `subGroup`, `version`, `definitionUpdatedAt` (a definição da permissão em si — interface `ScosPermission` + enum `ScosOrganizationPermission` — não tem requirement existente e entra na nova capability).

## Impact

- **Código**: `scos-security-starter` (`ScosPermission`, `ScosSystemRegistrationService`, config `MessageSource`); `scos-organization-infrastructure` (`ScosOrganizationPermission`, bundles `messages_permission*`); `grpc-proto` (`registry.proto` — `updateAt`→`updated_at`, regen de stubs); `grpc-boot` (`RegistreServiceImpl`, `GrpcGlobalExceptionHandler`); `scos-organization-usecase` (`RegistryResourceInput`/Bean); `scos-organization-domain` (`RegisterResourceInput`, `Resource`, `ResourceRepository`, `ResourceServiceBean`); `scos-organization-shared` (`ExceptionCodeError`, `scos_message_organization*`); `scos-organization-boot` (`scos_resource.yml`).
- **Banco**: `SCOS_RESOURCE` +4 colunas (edição direta do changelog, sistema não publicado).
- **Contrato gRPC**: `registry.proto` `Resource` renomeia `updateAt`→`updated_at` → regenerar stubs em grpc-proto/grpc-boot/starter.
- **Sem impacto** no login/validação: `vw_authority_response` e `vw_login_context` usam só o array `permission`.
- **Trilha de auditoria**: `@Auditable` não dispara no upsert nativo (aceito — resource é metadado auto-registrado; auditoria fica nas colunas próprias).
