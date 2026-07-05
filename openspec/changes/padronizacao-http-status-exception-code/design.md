## Context

`ExceptionCode` (foundation, `br.com.sawcunhaos.foundation.utils.specification.ExceptionCode`) já expõe:
```java
default String getTitle() { return "Error"; }
default int getHttpCode() { return 400; }
```
E o pipeline até a resposta HTTP já está plugado:
```
ScosException(ExceptionCode code)
  → this.httpCode = code.getHttpCode()
  → this.title (via ScosException.getTitle(), delegando pro code)  // usado por handleScosNoRollbackException
ExceptionsHandler.handleScosException() / handleScosNoRollbackException()
  → resolveHttpCode(exception.getHttpCode())   → HttpStatus real da resposta
  → resolveTitle(exception.getTitle())         → localeService.getMessage(title) com fallback "Business Error"
```
`ExceptionCodeError` (`scos-organization-shared`) hoje tem um override de `getHttpCode()` que só repassa o default (`ExceptionCode.super.getHttpCode()`) e não sobrescreve `getTitle()`. Resultado: todo `ScosException` lançado no organization retorna HTTP `400` e título `"Error"`/`"Business Error"`, independente da semântica real do código (não encontrado, conflito, regra de negócio, falha de integração).

Os comentários já presentes no enum (`/** ... HTTP 422. */` em `SCOS_DEPARTMENT_004/005/006`, `SCOS_POSITION_004/005`, `SCOS_LOGIN_013`) documentam a intenção correta desde a origem — nunca foi implementada.

`handleScosException` (o handler usado por todo `ScosException` comum — é o que o organization sempre usa) ainda hardcoda `"Business Error"` em vez de chamar `resolveTitle(exception.getTitle())` como `handleScosNoRollbackException` já faz. Essa mudança está em andamento pelo usuário em paralelo no repositório `SawCunhaOS-Foundation` — fora do escopo deste change.

## Goals / Non-Goals

**Goals:**
- Cada uma das 29 constantes de `ExceptionCodeError` carrega o `httpCode` correto para a operação que a lança
- Cada constante carrega um `title` — chave de mensagem RFC 9457, resolvida via `LocaleService.getMessage(key)`, com tradução PT-BR/EN
- Nenhuma mudança de `code`/mensagem de `detail`/`args` — só os metadados de resposta HTTP mudam

**Non-Goals:**
- Não altera `scos-foundation` (`ExceptionCode`, `ScosException`, `ExceptionsHandler`, `ScosExceptionCode`) — mudança em outro repositório, em andamento pelo usuário em paralelo
- Não remove os códigos órfãos `SCOS_LOGIN_001/002/003` (sem mensagem, sem throw site) — mantidos reservados com `httpCode`/`title` default
- Não adiciona `httpCode`/`title` aos novos códigos de catálogo (`AddressType`/`ContactType`/`Reason*`) de outras ideias/changes — esses nascem já corretos quando forem implementados, desde que esta change seja aplicada antes

## Decisions

### 1. Campo `httpCode`: tipo `int`, não `HttpStatus`
`ExceptionCode.getHttpCode()` retorna `int`. Usar `org.springframework.http.HttpStatus` como tipo de campo exigiria um getter manual (`.value()`), perdendo a geração automática do Lombok `@Getter`. Mesmo padrão que `ScosException` (foundation) já usa internamente (`private final int httpCode`).

### 2. Valores declarados por constante, não por método central
Cada constante recebe `httpCode`/`title` como argumentos do construtor (`@AllArgsConstructor`), não uma lógica de `switch` central dentro de `getHttpCode()`/`getTitle()`. Mesmo padrão já usado por `title` em `ScosExceptionCode` (foundation) — 1 campo por constante, sem branching.

### 3. Remover o override manual de `getHttpCode()`
Com o campo declarado, o `@Getter` do Lombok já gera `getHttpCode()` satisfazendo a interface — o método escrito à mão hoje (`return ExceptionCode.super.getHttpCode();`) vira código morto duplicado e é removido.

### 4. `title` é chave de mensagem, não texto literal
`resolveTitle` (foundation) já trata o valor de `getTitle()` como chave, resolvendo via `localeService.getMessage(title)` — com fallback para `"Business Error"` se a chave não existir no bundle (`NoSuchMessageException`). Usar texto literal fixo (como `ScosExceptionCode.title` faz hoje no foundation) quebraria esse mecanismo de i18n recém-ligado e nunca seria traduzido por locale.

### 5. Granularidade do `title`: 7 chaves compartilhadas por categoria de HTTP status, não 1 por código
| Chave | HTTP | PT-BR | EN |
|---|---|---|---|
| `SCOS_TITLE_NOT_FOUND` | 404 | Recurso não encontrado | Resource not found |
| `SCOS_TITLE_CONFLICT` | 409 | Conflito de dados | Data conflict |
| `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | 422 | Regra de negócio violada | Business rule violation |
| `SCOS_TITLE_EXTERNAL_INTEGRATION_FAILURE` | 502 | Falha de integração externa | External integration failure |
| `SCOS_TITLE_INTERNAL_ERROR` | 500 | Erro interno | Internal error |
| `SCOS_TITLE_UNAUTHORIZED` | 401 | Não autorizado | Unauthorized |
| `SCOS_TITLE_GENERIC` | 400 (default/órfãos) | Erro | Error |

Alternativa descartada: 1 chave de título por código individual (29 chaves). Descartada porque o padrão RFC 9457 clássico trata `title` como a categoria do problema e `detail` como a instância específica — 7 chaves compartilhadas evitam duplicação de manutenção sem perder informação (o `detail`, já granular por código, continua carregando o texto específico).

### 6. Mapeamento completo `httpCode`/`title` por código

| Código | HTTP | Title | Base |
|---|---|---|---|
| `SCOS_CONFIGURATION_001` | 404 | `SCOS_TITLE_NOT_FOUND` | "Key informada não existe" |
| `SCOS_CONFIGURATION_002` | 404 | `SCOS_TITLE_NOT_FOUND` | "Configuração não está cadastrada" |
| `SCOS_DEPARTMENT_001` | 404 | `SCOS_TITLE_NOT_FOUND` | `orElseThrow` — não encontrado |
| `SCOS_DEPARTMENT_002` | 409 | `SCOS_TITLE_CONFLICT` | `existsByCode` — código duplicado |
| `SCOS_DEPARTMENT_003` | 422 | `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | disable bloqueado — posições ativas vinculadas |
| `SCOS_DEPARTMENT_004` | 422 | `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | já ativo |
| `SCOS_DEPARTMENT_005` | 422 | `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | já inativo |
| `SCOS_DEPARTMENT_006` | 422 | `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | departamento inativo bloqueia Position |
| `SCOS_POSITION_001` | 404 | `SCOS_TITLE_NOT_FOUND` | `orElseThrow` — não encontrado |
| `SCOS_POSITION_002` | 409 | `SCOS_TITLE_CONFLICT` | `existsByCode` — código duplicado |
| `SCOS_POSITION_003` | 422 | `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | disable bloqueado — funcionários ativos vinculados |
| `SCOS_POSITION_004` | 422 | `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | já ativa |
| `SCOS_POSITION_005` | 422 | `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | já inativa |
| `SCOS_COMPANY_001` | 404 | `SCOS_TITLE_NOT_FOUND` | "empresa informada não existe" |
| `SCOS_COMPANY_002` | 409 | `SCOS_TITLE_CONFLICT` | "já existe empresa com esse CNPJ" |
| `SCOS_COMPANY_003` | 422 | `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | block bloqueado por colaboradores ativos (sem throw site hoje) |
| `SCOS_COMPANY_004` | 422 | `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | ciclo na hierarquia de empresas |
| `SCOS_COMPANY_005` | 422 | `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | não permite inativar última matriz ativa |
| `SCOS_COMPANY_006` | 422 | `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | não permite bloquear única empresa ativa (sem throw site hoje) |
| `SCOS_COMPANY_007` | 422 | `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | transição de status inválida |
| `SCOS_EMPLOYEE_001` | 422 | `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | transição de status inválida |
| `SCOS_USER_001` | 502 | `SCOS_TITLE_EXTERNAL_INTEGRATION_FAILURE` | falha ao criar usuário no Keycloak |
| `SCOS_USER_002` | 502 | `SCOS_TITLE_EXTERNAL_INTEGRATION_FAILURE` | falha ao atualizar usuário no Keycloak |
| `SCOS_USER_003` | 502 | `SCOS_TITLE_EXTERNAL_INTEGRATION_FAILURE` | falha ao excluir usuário no Keycloak |
| `SCOS_USER_004` | 500 | `SCOS_TITLE_INTERNAL_ERROR` | erro desconhecido ao processar usuário |
| `SCOS_AUTHORITY_001` | 404 | `SCOS_TITLE_NOT_FOUND` | "Login informado não existe" |
| `SCOS_LOGIN_001` | 400 (mantido) | `SCOS_TITLE_GENERIC` | órfão — sem mensagem, sem throw site |
| `SCOS_LOGIN_002` | 400 (mantido) | `SCOS_TITLE_GENERIC` | idem |
| `SCOS_LOGIN_003` | 400 (mantido) | `SCOS_TITLE_GENERIC` | idem |
| `SCOS_LOGIN_010` | 401 | `SCOS_TITLE_UNAUTHORIZED` | login inativo — rejeição em tentativa de autenticação (sem throw site hoje) |
| `SCOS_LOGIN_011` | 401 | `SCOS_TITLE_UNAUTHORIZED` | login bloqueado — idem |
| `SCOS_LOGIN_013` | 422 | `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | transição de status inválida |

## Risks / Trade-offs

- **[Risco] Ordem entre changes** — as ideias/changes futuras de catálogo (`AddressType`/`ContactType`/`Reason*`) também adicionam constantes a `ExceptionCodeError`. Se implementadas antes desta, seus construtores nascem sem `httpCode`/`title` e precisam de retrabalho. → **Mitigação**: aplicar esta change antes ou junto das changes de catálogo.
- **[Risco] Mudança de contrato de fato (status HTTP)** — clientes (mesmo internos/dev) que dependiam do `400` genérico atual para qualquer um dos 29 códigos passam a receber outro status. → **Mitigação**: é a correção do bug, não uma mudança de payload (`code`/`detail`/`args` continuam iguais); comunicar a squads consumidoras antes do deploy.
- **[Risco] `title` sem efeito visível até o fio do foundation ser ligado** — `handleScosException` ainda hardcoda `"Business Error"`; sem a troca por `resolveTitle(exception.getTitle())` (em andamento pelo usuário, fora deste repo), o `title` calculado aqui fica correto no dado mas não aparece na resposta. → **Mitigação**: nenhuma ação necessária deste lado; apenas ciente da dependência cruzada de repositório antes de considerar a mudança "visível em produção".
- **[Trade-off] Códigos sem throw site hoje** (`SCOS_COMPANY_003/006`, `SCOS_LOGIN_010/011`) recebem `httpCode`/`title` mesmo não sendo lançados por nenhuma classe ainda — decisão consciente de deixar correto agora para não esquecer quando forem implementados, ao custo de dados "não testáveis em produção" até lá.

## Migration Plan

Mudança aditiva de dados dentro de um enum existente — sem migration de banco, sem dependência de deploy coordenado além do já descrito na dependência externa (foundation). Rollback trivial: reverter o commit reverte `httpCode`/`title` para o comportamento atual (400/"Error" fixos).

## Open Questions

Nenhuma — granularidade do `title`, valores de `SCOS_USER_00N`/`SCOS_LOGIN_010/011` e tratamento dos órfãos `SCOS_LOGIN_001/002/003` já foram confirmados com o usuário durante a exploração.
