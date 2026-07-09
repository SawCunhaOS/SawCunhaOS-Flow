## 1. Contrato da permissão (interface + enum + i18n)

- [x] 1.1 Fechar o conjunto final de métodos da interface `ScosPermission` (`getPermission`, `getCodeDescription`, `getGroup`, `getSubGroup`, `getActive`, `getVersion`, `getUpdatedAt`); remover `getDescriptionPtBr`/`getDescriptionEng`
- [x] 1.2 Refatorar as ~110 constantes de `ScosOrganizationPermission` para carregar `codeDescription`, `group`, `subGroup`, `version`, `updatedAt`, `active` — sem strings pt/en inline; garantir `active` não-nulo
- [x] 1.3 Criar `messages_permission.properties` e `messages_permission_en.properties` (módulo infrastructure), chaveados por `codeDescription`
- [x] 1.4 Declarar `MessageSource` dedicado e qualificado para o bundle de permissão (sem colidir com o primário)

## 2. Registro no startup (consumidor)

- [x] 2.1 `ScosSystemRegistrationService`: resolver `codeDescription` → pt/en via `MessageSource`
- [x] 2.2 `ScosSystemRegistrationService`: preencher o proto `Resource` completo (`code`, `description_pt`, `description_en`, `group`, `sub_group`, `version`, `updated_at` ISO-8601, `active`)

## 3. Contrato gRPC

- [x] 3.1 `registry.proto`: renomear `updateAt`→`updated_at` no `Resource`; manter `description_pt`/`description_en`
- [x] 3.2 Regenerar stubs em grpc-proto, grpc-boot e starter
- [x] 3.3 `RegistreServiceImpl`: mapear campos novos e parsear `updated_at` (string ISO-8601) → `LocalDate`

## 4. Propagação na cadeia servidora

- [x] 4.1 `RegistryResourceInput` (+campos) e `RegistryResourceUseCaseBean` (pass-through de `group`/`subGroup`/`version`/`updatedAt`)
- [x] 4.2 `RegisterResourceInput` (domain) +campos

## 5. Banco e entidade

- [x] 5.1 `scos_resource.yml`: adicionar colunas `RESOURCE_GROUP` (`varchar(100)`), `SUB_GROUP` (`varchar(100)`), `VERSION` (`varchar(20)`), `DEFINITION_UPDATED_AT` (`date`); definir nulabilidade
- [x] 5.2 Entidade `Resource`: mapear `resourceGroup`, `subGroup`, `version`, `definitionUpdatedAt`

## 6. Upsert nativo condicional

- [x] 6.1 `ResourceRepository`: adicionar `upsert(...)` `@Modifying @Query(nativeQuery = true)` com `INSERT ... ON CONFLICT (CODE) DO UPDATE ... WHERE active/definition_updated_at IS DISTINCT FROM EXCLUDED`; remover `findIdByCodeAndSystemCode` (dead)
- [x] 6.2 `ResourceServiceBean`: trocar `findByCode().get()` por `orElseThrow(() -> new ScosException(SCOS_SYSTEM_001, systemCode))`
- [x] 6.3 `ResourceServiceBean`: chamar `upsert` (colunas de auditoria no SQL) no lugar de `merge`/`update`
- [x] 6.4 Garantir fronteira `@Transactional(rollbackFor = ScosException.class)` cobrindo o lote inteiro (batch atômico)

## 7. Propagação do erro entre módulos

- [x] 7.1 `ExceptionCodeError`: adicionar `SCOS_SYSTEM_001` (404, `SCOS_TITLE_NOT_FOUND`)
- [x] 7.2 Adicionar mensagem `SCOS_SYSTEM_001` em `scos_message_organization.properties` e `_en.properties`
- [x] 7.3 `GrpcGlobalExceptionHandler.handleScosException`: mapear `code`→`ExceptionCodeError.httpCode`→`Status` (404 `NOT_FOUND`, 409 `ALREADY_EXISTS`, 422 `FAILED_PRECONDITION`, 401 `UNAUTHENTICATED`, 400 `INVALID_ARGUMENT`, resto `INTERNAL`); anexar `code` no trailer `Metadata`; passar `ex.getArgs()` ao `getMessage`
- [x] 7.4 `ScosSystemRegistrationService` (client): ler `description` + `code` do trailer e logar o motivo real

## 8. Testes

> Decisão do usuário: **só Mockito** (estilo atual do projeto — sem Testcontainers). Cenários que exigem
> executar o SQL nativo Postgres (persistência real, gatilho condicional) ou servidor gRPC in-process
> ficam fora do escopo unit e estão anotados abaixo.

- [x] 8.1 `ResourceServiceBeanTest`: verifica que os metadados (`group`/`subGroup`/`version`/`definitionUpdatedAt`) são passados ao `upsert` (persistência real no banco não é unit-testável — requer integração)
- [x] 8.2 `ResourceServiceBeanTest`: `active=false` é passado ao `upsert` (soft-disable na origem)
- [ ] 8.3 Re-registro sem mudança NÃO bumpa `UPDATED_AT` — **não coberto**: gatilho condicional vive no SQL `DO UPDATE ... WHERE`, exige teste de integração (Testcontainers) fora deste escopo
- [~] 8.4 `ResourceServiceBeanTest` prova o throw `SCOS_SYSTEM_001` (+arg) para sistema inexistente; o mapeamento `NOT_FOUND`+trailer no `GrpcGlobalExceptionHandler` **não** tem teste unit (grpc-boot sem infra de teste) — validado por build/design
- [x] 8.5 Não-impacto no login confirmado por inspeção: `vw_authority_response.sql`/`vw_login_context.sql` não foram tocados (só usam o array `permission`)
- [x] 8.6 `RegistryResourcesUseCaseBeanTest`: pass-through de metadados + `systemCode` do principal e propagação de `ScosException` (rollback do lote)

## 9. Validação final

- [x] 9.1 Compilar o projeto inteiro (`mvn -DskipTests install` — exit 0; stubs do proto regenerados; interface ↔ enum ↔ registration service alinhados)
- [x] 9.2 `openspec validate permission-metadata-registry-enrichment --strict` — valid
