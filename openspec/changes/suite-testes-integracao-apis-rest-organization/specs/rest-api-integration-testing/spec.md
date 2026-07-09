## ADDED Requirements

### Requirement: Cobertura de IT por delegate REST implementado

O módulo Organization SHALL possuir exatamente uma classe de teste de integração
full-stack por delegate REST implementado. Cada classe MUST estender
`ScosOrganizationTestUtil` e exercitar o caminho completo MockMvc → filtros JWT →
`@PreAuthorize` → delegate → use case → domain → Postgres real, replicando a matriz de
cenários do exemplar `DepartmentControllerTest` conforme o shape da API.

Delegates cobertos: AddressType, ContactType, Position, ReasonActivate, ReasonInactivate,
ReasonEnable, ReasonDisable, Cnae, LegalNature e Configuration (Department já coberto).

#### Scenario: Suíte completa executa verde

- **WHEN** `mvn verify` é executado no módulo
- **THEN** toda a suíte de IT roda sobre a mesma infra singleton e passa, cobrindo cada
  operação de cada delegate listado

#### Scenario: Um delegate implementado sem IT correspondente

- **WHEN** um delegate REST da lista não possui classe de IT
- **THEN** a cobertura é considerada incompleta e a change não está pronta para arquivar

### Requirement: Matriz de cenários — Shape A (enable/disable)

A IT de cada delegate de shape A SHALL cobrir GET (lista + por id), POST, PUT, `enable` e `disable`.
Aplica-se a AddressType, ContactType, Position, ReasonActivate, ReasonInactivate, ReasonEnable e
ReasonDisable. Por operação MUST existir: happy path, 401 (sem token), 401 (token fora do JWKS),
403 (sem permissão via `stubValidateAuthorityWithoutPermissions`), 404 (id inexistente), 409 (code
duplicado), 422 (já ativo / já inativo), 400 (validação de campos) e idempotência no POST.

#### Scenario: Happy path de criação

- **WHEN** um POST é feito com token válido, permissão e payload válido usando `code` único
- **THEN** a API retorna 201 com `data.id` preenchido

#### Scenario: Reativação de recurso já ativo

- **WHEN** `enable` é chamado sobre um recurso já ativo
- **THEN** a API retorna 422 com o `code` da família do agregado para "já ativo"

#### Scenario: Inativação de recurso já inativo

- **WHEN** `disable` é chamado sobre um recurso já inativo
- **THEN** a API retorna 422 com o `code` da família do agregado para "já inativo"

### Requirement: Matriz de cenários — Shape B (DELETE)

Para os delegates com exclusão física (Cnae, LegalNature) a IT SHALL cobrir GET (lista +
por id), POST, PUT e `DELETE`. MUST validar 404 (`SCOS_CNAE_001` / `SCOS_LEGAL_NATURE_001`),
409 (`_002`, code duplicado) e 422 (`_003`, exclusão com vínculo FK), além dos cenários de
segurança e validação.

#### Scenario: Exclusão de registro sem vínculo

- **WHEN** `DELETE` é feito sobre um registro criado no próprio teste e sem vínculo
- **THEN** a API retorna 204 e o registro deixa de ser retornado no GET

#### Scenario: Exclusão de registro com vínculo FK

- **WHEN** `DELETE` é feito sobre o cnae/legal_nature do seed referenciado por uma company ativa
- **THEN** a API retorna 422 com o `code` `_003` do agregado

### Requirement: Matriz de cenários — Shape C (Configuration)

Para o delegate Configuration a IT SHALL cobrir GET all, GET keys, GET by id (chave string)
e PUT update. NÃO deve testar create nem delete (delegate não expõe). MUST validar 401, 403,
404 e erros de validação no update.

#### Scenario: Atualização de configuração existente

- **WHEN** um PUT é feito sobre uma chave existente com token válido e permissão
- **THEN** a API retorna sucesso e o GET subsequente reflete o novo valor

#### Scenario: Consulta de chave inexistente

- **WHEN** um GET by id é feito com uma chave que não existe
- **THEN** a API retorna 404 no contrato de erro RFC 9457

### Requirement: Validação do contrato de erro RFC 9457

Toda resposta de erro exercitada nas ITs SHALL ser validada contra o contrato RFC 9457
ProblemDetail: `type`, `title`, `status`, `detail`, `instance`, `code`, `requestId` e
`timestamp`. Em falhas de validação de campos MUST validar também o array `errors`. Os
valores de `code`, `title` e `detail` MUST ser lidos das fontes canônicas
(`ExceptionCodeError`, bundle de mensagens) e não presumidos.

#### Scenario: Erro de acesso negado

- **WHEN** uma requisição autenticada sem a permissão necessária é feita
- **THEN** a resposta 403 contém `status=403`, `code=SCOS-004` e os campos `type`, `title`,
  `detail`, `instance` preenchidos

#### Scenario: Erro de validação de campos

- **WHEN** um POST/PUT é feito com payload que viola as validações de campo
- **THEN** a resposta 400 contém `code=SCOS-001` e `errors[]` não vazio

### Requirement: Reuso de infra singleton e isolamento por método

As ITs SHALL reusar a infra singleton existente (`ScosOrganizationTestUtil`) sem declarar
containers próprios nem recriar o contexto Spring. O isolamento entre métodos MUST ser
garantido pelo `@Sql` herdado (setup BEFORE / TRUNCATE RESTART IDENTITY AFTER). Como o cache
de idempotência (Redis) não é resetado entre métodos, cada POST bem-sucedido MUST usar um
`code` único no método. A suíte MUST rodar em JVM único (`forkCount=1`/`reuseForks=true`).

#### Scenario: Duas classes de IT compartilham a mesma infra

- **WHEN** duas ou mais classes de IT executam na mesma JVM
- **THEN** os containers Postgres/Redis sobem uma única vez e o contexto Spring é reaproveitado

#### Scenario: POST idempotente no mesmo agregado

- **WHEN** o mesmo payload de POST é enviado duas vezes seguidas
- **THEN** o jDempotent devolve a resposta em cache e a segunda chamada não retorna 409 de code duplicado
