## ADDED Requirements

### Requirement: HTTP status reflete a semântica da operação que lança o erro
Cada constante de `ExceptionCodeError` SHALL retornar, via `getHttpCode()`, o status HTTP correspondente à semântica da operação que a lança: `404` para recurso não encontrado, `409` para conflito de unicidade, `422` para regra de negócio violada, `502` para falha de integração externa, `500` para erro interno desconhecido, `401` para rejeição de tentativa de autenticação.

#### Scenario: Departamento não encontrado
- **WHEN** o sistema lança `ScosException(SCOS_DEPARTMENT_001)` ao buscar um departamento por id inexistente
- **THEN** a resposta HTTP tem status `404`

#### Scenario: Código de cargo duplicado
- **WHEN** o sistema lança `ScosException(SCOS_POSITION_002)` ao criar um cargo com `code` já cadastrado
- **THEN** a resposta HTTP tem status `409`

#### Scenario: Departamento já ativo
- **WHEN** o sistema lança `ScosException(SCOS_DEPARTMENT_004)` ao tentar ativar um departamento já `ACTIVE`
- **THEN** a resposta HTTP tem status `422`

#### Scenario: Falha ao criar usuário no Keycloak
- **WHEN** o sistema lança `ScosException(SCOS_USER_001)` porque a chamada ao Keycloak falhou
- **THEN** a resposta HTTP tem status `502`

#### Scenario: Erro desconhecido ao processar usuário
- **WHEN** o sistema lança `ScosException(SCOS_USER_004)`
- **THEN** a resposta HTTP tem status `500`

### Requirement: Título RFC 9457 é resolvido por categoria via chave de mensagem i18n
Cada constante de `ExceptionCodeError` SHALL retornar, via `getTitle()`, uma chave de mensagem (não texto literal) correspondente à categoria do seu `httpCode`. As 7 chaves de categoria (`SCOS_TITLE_NOT_FOUND`, `SCOS_TITLE_CONFLICT`, `SCOS_TITLE_BUSINESS_RULE_VIOLATION`, `SCOS_TITLE_EXTERNAL_INTEGRATION_FAILURE`, `SCOS_TITLE_INTERNAL_ERROR`, `SCOS_TITLE_UNAUTHORIZED`, `SCOS_TITLE_GENERIC`) SHALL possuir tradução PT-BR e EN em `scos_message_organization.properties`/`scos_message_organization_en.properties`.

#### Scenario: Título de erro de recurso não encontrado em PT-BR
- **WHEN** o sistema resolve o `title` de `SCOS_DEPARTMENT_001` usando o locale PT-BR
- **THEN** o texto retornado é "Recurso não encontrado"

#### Scenario: Título de erro de recurso não encontrado em EN
- **WHEN** o sistema resolve o `title` de `SCOS_DEPARTMENT_001` usando o locale EN
- **THEN** o texto retornado é "Resource not found", diferente do texto PT-BR da mesma chave

#### Scenario: Dois códigos distintos da mesma categoria compartilham o título
- **WHEN** o sistema resolve o `title` de `SCOS_DEPARTMENT_004` (já ativo) e de `SCOS_POSITION_003` (disable bloqueado)
- **THEN** ambos retornam a mesma chave `SCOS_TITLE_BUSINESS_RULE_VIOLATION` e o mesmo texto localizado, mesmo sendo `code`/`detail` diferentes

### Requirement: Códigos sem throw site hoje recebem httpCode/title coerentes com o alvo documentado
Códigos que ainda não são lançados por nenhuma classe (`SCOS_COMPANY_003`, `SCOS_COMPANY_006`, `SCOS_LOGIN_010`, `SCOS_LOGIN_011`) SHALL, mesmo assim, retornar o `httpCode`/`title` coerente com a operação-alvo documentada, para que já estejam corretos quando a operação for implementada.

#### Scenario: Motivo de bloqueio de empresa ainda não implementado
- **WHEN** um desenvolvedor consulta `SCOS_COMPANY_003.getHttpCode()`/`getTitle()` hoje
- **THEN** o valor retornado é `422`/`SCOS_TITLE_BUSINESS_RULE_VIOLATION`, mesmo nenhuma classe lançando esse código atualmente

### Requirement: Códigos órfãos sem mensagem mantêm o comportamento default
Códigos sem mensagem PT-BR/EN e sem throw site em nenhum lugar do código (`SCOS_LOGIN_001`, `SCOS_LOGIN_002`, `SCOS_LOGIN_003`) SHALL manter o `httpCode` default (`400`) e o `title` genérico (`SCOS_TITLE_GENERIC`), sem receber uma categoria específica inferida.

#### Scenario: Código órfão reservado
- **WHEN** o sistema resolve `httpCode`/`title` de `SCOS_LOGIN_001`
- **THEN** o `httpCode` retornado é `400` e o `title` é `SCOS_TITLE_GENERIC`
