## ADDED Requirements

### Requirement: Contrato da interface ScosPermission com metadados
A interface `ScosPermission` SHALL expor o conjunto final de métodos: `getPermission`, `getCodeDescription` (chave i18n), `getGroup`, `getSubGroup`, `getActive`, `getVersion` e `getUpdatedAt` (`LocalDate`). A interface MUST NOT expor descrições pt/en (`getDescriptionPtBr`/`getDescriptionEng`).

#### Scenario: Interface expõe o conjunto final de métodos
- **WHEN** a interface `ScosPermission` é inspecionada
- **THEN** declara `getPermission`, `getCodeDescription`, `getGroup`, `getSubGroup`, `getActive`, `getVersion`, `getUpdatedAt` e não declara `getDescriptionPtBr`/`getDescriptionEng`

#### Scenario: Projeto compila com interface e enum alinhados
- **WHEN** o módulo é compilado
- **THEN** `ScosOrganizationPermission` implementa todos os métodos de `ScosPermission` e a compilação conclui sem erro

### Requirement: Enum ScosOrganizationPermission carrega metadados sem descrição inline
O enum `ScosOrganizationPermission` SHALL carregar, por constante, `codeDescription` (chave de mensagem), `group`, `subGroup`, `version`, `updatedAt` (`LocalDate`) e `active` (não-nulo). O enum MUST NOT conter strings de descrição pt/en inline nas constantes.

#### Scenario: Constante expõe metadados e chave de descrição
- **WHEN** uma constante do enum é lida
- **THEN** retorna `codeDescription`, `group`, `subGroup`, `version`, `updatedAt` e `active`, e nenhuma string pt/en literal

#### Scenario: active nunca é nulo na origem
- **WHEN** `getActive()` é chamado em qualquer constante
- **THEN** retorna um valor booleano não-nulo

### Requirement: Bundle i18n dedicado para descrição da permissão
O sistema SHALL prover os bundles `messages_permission.properties` e `messages_permission_en.properties` (no módulo do enum), chaveados por `getCodeDescription`, resolvidos por um `MessageSource` dedicado e qualificado — sem colidir com o `MessageSource` primário da aplicação.

#### Scenario: Descrição resolvida por locale
- **WHEN** o registro resolve `codeDescription` de uma permissão nos locales pt e en
- **THEN** retorna o texto do bundle correspondente e o texto en difere do texto pt

#### Scenario: MessageSource de permissão não colide com o primário
- **WHEN** o contexto Spring sobe com o bundle de permissão e o `MessageSource` primário
- **THEN** ambos os beans coexistem e a aplicação inicia sem conflito de bean

### Requirement: Registration service resolve i18n e preenche o proto completo
No startup, `ScosSystemRegistrationService` SHALL resolver `codeDescription` → pt/en via `MessageSource` e preencher o proto `Resource` com `code`, `description_pt`, `description_en`, `group`, `sub_group`, `version`, `updated_at` e `active`. O campo `updated_at` do proto SHALL transportar a data em ISO-8601 (`string`) e ser parseado para `LocalDate` no servidor.

#### Scenario: Proto Resource preenchido com todos os campos
- **WHEN** o registro monta o proto `Resource` de uma permissão
- **THEN** os campos `code`, `description_pt`, `description_en`, `group`, `sub_group`, `version`, `updated_at` e `active` estão preenchidos

#### Scenario: updated_at transportado como ISO-8601 e parseado no servidor
- **WHEN** o servidor recebe `updated_at` como string ISO-8601
- **THEN** converte para `LocalDate` sem erro e persiste o valor correto

### Requirement: Cadeia servidora propaga e persiste os metadados
A cadeia `RegistreServiceImpl` → `RegistryResourceInput` → `RegistryResourceUseCaseBean` → `RegisterResourceInput` → `ResourceServiceBean` SHALL propagar `group`, `subGroup`, `version` e `updatedAt` e persistir esses campos em `SCOS_RESOURCE` tanto no insert quanto no update.

#### Scenario: Insert persiste os metadados
- **WHEN** uma permissão inexistente é registrada
- **THEN** a linha em `SCOS_RESOURCE` grava `RESOURCE_GROUP`, `SUB_GROUP`, `VERSION` e `DEFINITION_UPDATED_AT`

#### Scenario: Update persiste os metadados
- **WHEN** uma permissão já existente é re-registrada com metadados alterados
- **THEN** a linha em `SCOS_RESOURCE` reflete os novos `RESOURCE_GROUP`, `SUB_GROUP`, `VERSION` e `DEFINITION_UPDATED_AT`

### Requirement: Upsert nativo condicional do resource
`ResourceServiceBean.register` SHALL persistir o resource via upsert nativo PostgreSQL (`@Modifying @Query(nativeQuery = true)` em `ResourceRepository`) usando `INSERT ... ON CONFLICT (CODE) DO UPDATE`, substituindo o fluxo `findByCode().get()` + `merge`/`update`. As colunas de auditoria SHALL ser setadas no próprio SQL (`CREATED_AT` e `UPDATED_AT` = `NOW()`, `USER_AT` = parâmetro). O ramo `DO UPDATE` SHALL executar somente quando `ACTIVE` **ou** `DEFINITION_UPDATED_AT` diferirem da linha atual (`IS DISTINCT FROM EXCLUDED`).

#### Scenario: Insert quando o CODE não existe
- **WHEN** o resource com um `CODE` inexistente é registrado
- **THEN** uma linha é inserida com `CREATED_AT`, `UPDATED_AT` e `USER_AT` preenchidos

#### Scenario: Update dispara quando active muda
- **WHEN** um resource existente é re-registrado com `active` diferente do persistido
- **THEN** o `DO UPDATE` executa, atualiza `ACTIVE` e bumpa `UPDATED_AT`

#### Scenario: Update dispara quando a data da definição muda
- **WHEN** um resource existente é re-registrado com `DEFINITION_UPDATED_AT` diferente do persistido
- **THEN** o `DO UPDATE` executa e persiste os novos metadados

#### Scenario: Sem mudança em active nem na data não bumpa UPDATED_AT
- **WHEN** um resource existente é re-registrado com o mesmo `active` e a mesma `DEFINITION_UPDATED_AT`
- **THEN** nenhum UPDATE é aplicado e o `UPDATED_AT` da linha permanece inalterado

### Requirement: Registro do lote é atômico
O registro do lote de resources SHALL ser coberto por `@Transactional(rollbackFor = ScosException.class)` na iteração inteira, de modo que a falha ao registrar qualquer resource faça rollback de todos.

#### Scenario: Falha em um resource reverte o lote inteiro
- **WHEN** o registro de um lote falha com `ScosException` no meio da iteração
- **THEN** nenhum resource do lote é persistido (rollback total)

### Requirement: Desativação soft de permissão
Uma constante do enum re-registrada com `active=false` SHALL marcar `ACTIVE=false` na linha correspondente de `SCOS_RESOURCE`, sem deletar a linha.

#### Scenario: Permissão desativada mantém a linha
- **WHEN** uma permissão existente é re-registrada com `active=false`
- **THEN** a linha em `SCOS_RESOURCE` permanece e `ACTIVE` passa a `false`

### Requirement: Sistema inexistente vira erro de negócio visível
`ResourceServiceBean` SHALL lançar `ScosException(SCOS_SYSTEM_001)` (HTTP 404, `SCOS_TITLE_NOT_FOUND`) quando o `systemCode` informado não corresponder a nenhum `System`, em vez de `NoSuchElementException` via `.get()`. O código `SCOS_SYSTEM_001` SHALL existir em `ExceptionCodeError` com mensagem pt e en.

#### Scenario: systemCode inexistente lança SCOS_SYSTEM_001
- **WHEN** o registro é chamado com um `systemCode` que não existe
- **THEN** é lançada `ScosException` com código `SCOS_SYSTEM_001` (404), não `NoSuchElementException`

### Requirement: Motivo do erro visível entre módulos no gRPC
`GrpcGlobalExceptionHandler` SHALL mapear `ScosException` para um `Status` gRPC de negócio resolvendo `code` → `ExceptionCodeError.httpCode` → `Status` (404 `NOT_FOUND`, 409 `ALREADY_EXISTS`, 422 `FAILED_PRECONDITION`, 401 `UNAUTHENTICATED`, 400 `INVALID_ARGUMENT`, demais `INTERNAL`), anexar o `code` num `io.grpc.Metadata` (trailer) e resolver a mensagem passando `ex.getArgs()`. O client `ScosSystemRegistrationService` SHALL ler `description` + `code` do trailer e logar o motivo real.

#### Scenario: ScosException 404 vira NOT_FOUND com code no trailer
- **WHEN** o servidor lança `ScosException(SCOS_SYSTEM_001)` (404)
- **THEN** o client recebe `Status.NOT_FOUND`, a `description` resolvida e o `code` `SCOS_SYSTEM_001` no trailer `Metadata`

#### Scenario: Client loga o motivo real em vez de mensagem genérica
- **WHEN** o client recebe o erro gRPC de registro
- **THEN** loga a `description` e o `code` do trailer, não "Internal server error"
