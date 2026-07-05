## Why

`ExceptionCode` (foundation) já expõe `getHttpCode()` (default 400) e `getTitle()` (default `"Error"`, tratado pelo `ExceptionsHandler.resolveTitle` como chave de mensagem i18n) — o pipeline até a resposta RFC 9457 já está plugado (`ScosException` → `resolveHttpCode`/`resolveTitle` → `ResponseEntity`). Mas `ExceptionCodeError` (organization) tem um override de `getHttpCode()` que só repassa o default, e não sobrescreve `getTitle()`. Resultado: **todo** `ScosException` lançado pelo organization retorna HTTP `400` e título `"Error"`/`"Business Error"`, mesmo quando o código é um "não encontrado" (404), um "código duplicado" (409) ou uma regra de negócio violada (422). Os comentários já presentes no enum (`/** ... HTTP 422. */`) documentam a intenção correta — nunca foi implementada.

## What Changes

- `ExceptionCodeError` ganha 2 campos por constante: `httpCode` (int) e `title` (chave de mensagem, categórica)
- Remove o override manual de `getHttpCode()` — passa a ser gerado pelo Lombok `@Getter` a partir do campo
- As 29 constantes existentes recebem `httpCode` correto: `404` (não encontrado), `409` (conflito de unicidade), `422` (regra de negócio violada), `502` (falha de integração externa — Keycloak), `500` (erro interno), `401` (rejeição de autenticação); 3 códigos órfãos (`SCOS_LOGIN_001/002/003`, sem mensagem e sem throw site) mantêm o default `400`
- As 29 constantes recebem `title` — uma de 7 chaves compartilhadas por categoria de HTTP status (`SCOS_TITLE_NOT_FOUND`, `SCOS_TITLE_CONFLICT`, `SCOS_TITLE_BUSINESS_RULE_VIOLATION`, `SCOS_TITLE_EXTERNAL_INTEGRATION_FAILURE`, `SCOS_TITLE_INTERNAL_ERROR`, `SCOS_TITLE_UNAUTHORIZED`, `SCOS_TITLE_GENERIC`), não uma chave por código individual
- As 7 chaves de título ganham tradução PT-BR e EN em `scos_message_organization.properties`/`_en.properties`

## Capabilities

### New Capabilities
- `error-problem-details`: `ExceptionCodeError` (organization) atribui `httpCode` e `title` (RFC 9457, resolvido via i18n) coerentes com a semântica de cada código de erro de negócio, substituindo o `400`/`"Error"` fixo herdado do default de `ExceptionCode`

### Modified Capabilities
_Nenhuma — `error-message-catalog` (spec existente) cobre qualidade de tradução do `detail` (PT/EN completo, verbo correto); não normatiza `httpCode`/`title`, então nenhum requisito dela muda aqui._

## Impact

- `scos-organization-shared/.../exception/ExceptionCodeError.java` — novos campos `httpCode`/`title`, argumentos em todas as 29 constantes, remove override manual
- `scos-organization-shared/src/main/resources/scos_message_organization.properties` — +7 chaves `SCOS_TITLE_*`
- `scos-organization-shared/src/main/resources/scos_message_organization_en.properties` — +7 chaves `SCOS_TITLE_*`
- Sem impacto de banco
- Dependência externa (fora deste repositório): o `title` só aparece de fato na resposta HTTP quando `ExceptionsHandler.handleScosException` (foundation, `SawCunhaOS-Foundation`) trocar o literal `"Business Error"` por `resolveTitle(exception.getTitle())` — mudança em andamento pelo usuário em paralelo, fora do escopo deste change
- Pré-requisito de ordem: as changes futuras que adicionarem novos códigos a `ExceptionCodeError` (catálogo `AddressType`/`ContactType`, catálogo `Reason*`) devem ser implementadas depois desta, para que seus construtores já nasçam com `httpCode`/`title`
- Clientes que dependiam do `400` genérico atual para qualquer um dos 29 códigos passam a receber o status correto — correção de bug, não mudança de contrato de dado (`code`/mensagem de `detail` continuam iguais)
