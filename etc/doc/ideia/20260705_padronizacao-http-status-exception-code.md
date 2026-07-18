# Padronização de HTTP Status e Título de Erro por ExceptionCodeError

**Data**: 2026-07-05
**Status**: 🔄 Em Análise
**Tipo**: 🐛 Bug Fix

---

## ⚠️ Princípio SRP — Uma Funcionalidade por Ideia

- **Nome da funcionalidade**: `padronizacao-http-status-exception-code`
- **Resumo em uma frase**: Fazer `ExceptionCodeError` (organization) atribuir o `httpCode` e o `title` (RFC 9457) corretos por código de erro, em vez do valor fixo 400/"Error" herdado do default de `ExceptionCode`.

> `title` entrou no escopo desta mesma ideia (não uma nova) porque usa exatamente o mesmo mecanismo — mesmo campo de enum, mesmo construtor, mesma mudança de arquivo — que o `httpCode`. Separar em 2 ideias criaria 2 changes tocando a mesma assinatura de construtor em sequência, sem necessidade.

**Checklist SRP**:
- [x] Esta ideia cobre exatamente uma funcionalidade
- [x] Não mistura features independentes no mesmo arquivo
- [x] O nome é específico (não genérico como "melhorar módulo X")

---

## 1️⃣ Visão

### Problema

A interface `ExceptionCode` (foundation, `br.com.sawcunhaos.foundation.utils.specification.ExceptionCode`) já expõe `default int getHttpCode() { return 400; }`. O pipeline até a resposta HTTP já está todo plugado:

```
ScosException(ExceptionCode code)
  → this.httpCode = code.getHttpCode()
ExceptionsHandler.handleScosException()
  → resolveHttpCode(exception.getHttpCode())
  → ResponseEntity.status(...)
```

Mas `ExceptionCodeError` (`flow-organization-shared`) hoje tem:
```java
@Override
public int getHttpCode() {
    return ExceptionCode.super.getHttpCode();
}
```
Um override que só repassa o default — **todo** `ScosException` lançado no módulo organization retorna HTTP `400`, independente de o código ser um "não encontrado" (deveria ser 404), um "código duplicado" (deveria ser 409) ou uma regra de negócio violada (deveria ser 422). Isso já foi confirmado lendo `ExceptionsHandler.java` (foundation) — o `resolveHttpCode` de fato usa esse valor pra montar a resposta, não é um método morto.

Os comentários já presentes no próprio enum (`/** ... HTTP 422. */` em `SCOS_DEPARTMENT_004/005/006`, `SCOS_POSITION_004/005`, `SCOS_LOGIN_013`) documentam a intenção correta — só nunca foi implementada.

O mesmo padrão de gap existe pro `title` RFC 9457. `ExceptionCode.getTitle()` (foundation) tem default `"Error"`. `ExceptionsHandler` (foundation) já ganhou um `resolveTitle(String title)`:
```java
private String resolveTitle(String title) {
    try {
        return localeService.getMessage(title);
    } catch (Exception e) {
        return "Business Error";
    }
}
```
Ou seja: `getTitle()` deixou de ser texto literal fixo e passou a ser tratado como **chave de mensagem** (mesmo mecanismo do `detail`, via `LocaleService.getMessage(key)`), resolvida por locale. `handleScosNoRollbackException` já chama `resolveTitle(exception.getTitle())`; `handleScosException` (o handler usado por todo `ScosException` comum — é o que o organization sempre usa) ainda hardcoda `"Business Error"` direto, mas esse fio está sendo ligado pelo usuário em paralelo no repositório `SawCunhaOS-Foundation` — não é risco pendente desta ideia.

`ExceptionCodeError` hoje não sobrescreve `getTitle()` — todo código cai no default `"Error"` (e, mesmo depois de o foundation ligar o fio, cairia no fallback `"Business Error"` por `NoSuchMessageException`, já que nenhuma chave de título existe no bundle organization hoje).

### Objetivo

Cada constante de `ExceptionCodeError` passa a carregar:
- seu `httpCode` correto, coerente com a operação que a lança (`404` não encontrado, `409` conflito de unicidade, `422` regra de negócio violada, `502`/`500` falha de integração externa, `401` rejeição de autenticação)
- seu `title` (chave de mensagem RFC 9457), agrupado por categoria de HTTP status — não uma chave por código individual — com tradução PT/EN em `scos_message_organization.properties`/`_en.properties`

### Fora de Escopo

- Remover os 3 códigos órfãos `SCOS_LOGIN_001/002/003` (sem mensagem PT/EN, sem throw site) — decisão do usuário: manter reservados, só atribuir `httpCode`/`title` default (categoria `GENERIC`).
- Alterar `scos-foundation` (`ExceptionCode`/`ScosExceptionCode`/`ScosException`/`ExceptionsHandler`) — já em andamento pelo usuário em paralelo nesse outro repositório; esta ideia assume que o fio (`handleScosException` → `resolveTitle`) vai existir e cobre só o lado organization (dado a fornecer: `httpCode` + `title`-como-chave + entradas no bundle de mensagens).
- `ScosExceptionCode` (foundation) continuar sem `httpCode`/`title` próprios pros seus códigos (`GENERIC`, `ACCESS_DENIED`, `TOKEN_NOT_PROVIDED`) — os handlers que os usam já fixam status/título manualmente, não dependem do padrão aqui descrito. Fora de escopo por ser outro repositório.
- Os novos códigos de `SCOS_ADDRESS_TYPE_*`/`SCOS_CONTACT_TYPE_*`/`SCOS_REASON_*_*` das ideias `20260705_crud-catalogo-tipo-endereco-contato` e `20260705_crud-catalogo-motivos-transicao-status` — essas duas já preveem a coluna HTTP nas suas próprias tabelas de mensagem; quando forem implementadas, seus construtores de enum já devem nascer com `httpCode` e `title` (esta change é pré-requisito de ordem, ver Riscos).

---

## 2️⃣ Requisitos

### Funcionais
- [ ] **RF-01**: `ExceptionCodeError` ganha campo `private final int httpCode` (argumento do construtor), substituindo o override manual de `getHttpCode()`
- [ ] **RF-02**: Todas as 29 constantes existentes recebem o `httpCode` correto (tabela abaixo)
- [ ] **RF-03**: `ExceptionCodeError` ganha campo `private final String title` (argumento do construtor) — valor é uma **chave de mensagem** (não texto literal), uma das 7 categorias (`SCOS_TITLE_NOT_FOUND`, `SCOS_TITLE_CONFLICT`, `SCOS_TITLE_BUSINESS_RULE_VIOLATION`, `SCOS_TITLE_EXTERNAL_INTEGRATION_FAILURE`, `SCOS_TITLE_INTERNAL_ERROR`, `SCOS_TITLE_UNAUTHORIZED`, `SCOS_TITLE_GENERIC`)
- [ ] **RF-04**: As 7 chaves de título recebem tradução PT-BR e EN em `scos_message_organization.properties`/`_en.properties`

### Não-Funcionais
- [ ] **RNF-01**: Nenhuma mudança de comportamento fora do status HTTP e do título retornados — código/mensagem/args continuam iguais
- [ ] **RNF-02**: Compatível com `@Getter` do Lombok (mesmo padrão já usado por `title` em `ScosExceptionCode` — campos novos geram `getHttpCode()`/`getTitle()` automaticamente, satisfazem a interface sem `@Override` manual)
- [ ] **RNF-03**: `title` segue o mesmo mecanismo de i18n do `code`/`detail` (chave resolvida via `LocaleService.getMessage(key)`), não texto hardcoded — consistente com `resolveTitle` já implementado no foundation

---

## 3️⃣ Arquitetura

### Componentes Afetados
```
scos-organization-shared
├── exception/ExceptionCodeError.java: modificação
│   (novos campos httpCode + title; +2 argumentos em todas as 29 constantes; remove override manual de getHttpCode())
├── resources/scos_message_organization.properties: modificação (+7 chaves SCOS_TITLE_*)
└── resources/scos_message_organization_en.properties: modificação (+7 chaves SCOS_TITLE_*)
```
Nenhum outro arquivo de código muda — `ScosException`/`ExceptionsHandler` (foundation) já leem `getHttpCode()`/`getTitle()` corretamente (o fio do `title` no `handleScosException` está sendo ligado pelo usuário em paralelo nesse outro repositório).

### Fluxo Principal (sem mudança de fluxo, só de dado)
```
Domain lança new ScosException(SCOS_DEPARTMENT_001)
  → ScosException.httpCode = SCOS_DEPARTMENT_001.getHttpCode()   // hoje: 400 sempre | depois: 404
  → ScosException.title (via getTitle()) = "SCOS_TITLE_NOT_FOUND" // hoje: "Error" sempre | depois: chave por categoria
ExceptionsHandler.handleScosException()
  → resolveHttpCode(exception.getHttpCode())        → 404
  → resolveTitle(exception.getTitle())              → localeService.getMessage("SCOS_TITLE_NOT_FOUND") → "Recurso não encontrado" / "Resource not found"
  → ResponseEntity.status(404).body({ title: "Recurso não encontrado", ... })
```

### Decisões Técnicas
| Decisão | Escolha | Alternativa Descartada | Motivo |
|---------|---------|------------------------|--------|
| Tipo do campo `httpCode` | `int httpCode` | `org.springframework.http.HttpStatus httpCode` | Interface `ExceptionCode.getHttpCode()` retorna `int`; `HttpStatus` exigiria getter manual (`.value()`) em vez de deixar o Lombok `@Getter` satisfazer a interface automaticamente — mesmo padrão que `ScosException` (foundation) já usa (`private final int httpCode`) |
| Onde declarar os valores | Argumentos do construtor de cada constante (`@AllArgsConstructor`) | Métodos `switch` centrais em `getHttpCode()`/`getTitle()` | Mesmo padrão já usado por `title` em `ScosExceptionCode` (foundation) — 1 campo por constante, sem lógica condicional |
| Manter override manual atual de `getHttpCode()` | Remover — deletar o método escrito à mão | Manter e delegar ao campo dentro do método | Com o campo declarado, `@Getter` já gera o método; manter o override manual seria código morto duplicado |
| Granularidade do `title` | 7 chaves compartilhadas por categoria de HTTP status (`SCOS_TITLE_NOT_FOUND`, `SCOS_TITLE_CONFLICT`, `SCOS_TITLE_BUSINESS_RULE_VIOLATION`, `SCOS_TITLE_EXTERNAL_INTEGRATION_FAILURE`, `SCOS_TITLE_INTERNAL_ERROR`, `SCOS_TITLE_UNAUTHORIZED`, `SCOS_TITLE_GENERIC`) | 1 chave de título por código individual (29 chaves) | Confirmado com o usuário — padrão RFC 9457 clássico: `title` é a categoria do problema, `detail` é a instância específica; 7 chaves compartilhadas evitam duplicação de manutenção |
| Valor de `title` = literal ou chave | Chave de mensagem (resolvida via `LocaleService.getMessage`) | Texto literal fixo (como `ScosExceptionCode.title` faz hoje) | `resolveTitle` do foundation já trata o valor como chave (`localeService.getMessage(title)`); usar literal quebraria o mecanismo de i18n recém-ligado — texto literal fixo nem seria traduzido por locale |

### Banco de Dados
- **Impacto**: ❌ Não

### Mensagens de Título (chave + PT-BR + EN)

> `title` é chave de mensagem, resolvida por `localeService.getMessage(key)` — mesmo bundle `scos_message_organization.properties`/`_en.properties` usado pelos códigos `SCOS_*_00N`.

| Chave | PT-BR | EN | Usada por (HTTP) |
|---|---|---|---|
| `SCOS_TITLE_NOT_FOUND` | Recurso não encontrado | Resource not found | 404 |
| `SCOS_TITLE_CONFLICT` | Conflito de dados | Data conflict | 409 |
| `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | Regra de negócio violada | Business rule violation | 422 |
| `SCOS_TITLE_EXTERNAL_INTEGRATION_FAILURE` | Falha de integração externa | External integration failure | 502 |
| `SCOS_TITLE_INTERNAL_ERROR` | Erro interno | Internal error | 500 |
| `SCOS_TITLE_UNAUTHORIZED` | Não autorizado | Unauthorized | 401 |
| `SCOS_TITLE_GENERIC` | Erro | Error | 400 (default/órfãos) |

### Tabela de HTTP Status + Título por Código (proposta completa)

| Código | HTTP hoje | HTTP proposto | Title proposto | Base |
|---|---|---|---|---|
| `SCOS_CONFIGURATION_001` | 400 | **404** | `SCOS_TITLE_NOT_FOUND` | "Key informada não existe" |
| `SCOS_CONFIGURATION_002` | 400 | **404** | `SCOS_TITLE_NOT_FOUND` | "Configuração não está cadastrada" |
| `SCOS_DEPARTMENT_001` | 400 | **404** | `SCOS_TITLE_NOT_FOUND` | `orElseThrow` — não encontrado |
| `SCOS_DEPARTMENT_002` | 400 | **409** | `SCOS_TITLE_CONFLICT` | `existsByCode` — código duplicado |
| `SCOS_DEPARTMENT_003` | 400 | **422** | `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | disable bloqueado — posições ativas vinculadas |
| `SCOS_DEPARTMENT_004` | 400 | **422** | `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | já ativo (comentário já dizia HTTP 422) |
| `SCOS_DEPARTMENT_005` | 400 | **422** | `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | já inativo (comentário já dizia HTTP 422) |
| `SCOS_DEPARTMENT_006` | 400 | **422** | `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | departamento inativo bloqueia Position (comentário já dizia HTTP 422) |
| `SCOS_POSITION_001` | 400 | **404** | `SCOS_TITLE_NOT_FOUND` | `orElseThrow` — não encontrado |
| `SCOS_POSITION_002` | 400 | **409** | `SCOS_TITLE_CONFLICT` | `existsByCode` — código duplicado |
| `SCOS_POSITION_003` | 400 | **422** | `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | disable bloqueado — funcionários ativos vinculados |
| `SCOS_POSITION_004` | 400 | **422** | `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | já ativa (comentário já dizia HTTP 422) |
| `SCOS_POSITION_005` | 400 | **422** | `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | já inativa (comentário já dizia HTTP 422) |
| `SCOS_COMPANY_001` | 400 | **404** | `SCOS_TITLE_NOT_FOUND` | "empresa informada não existe" |
| `SCOS_COMPANY_002` | 400 | **409** | `SCOS_TITLE_CONFLICT` | "já existe empresa com esse CNPJ" |
| `SCOS_COMPANY_003` | 400 | **422** | `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | block bloqueado por colaboradores ativos — **sem throw site hoje** (spec `error-message-catalog` já documenta como reservado p/ implementação futura) |
| `SCOS_COMPANY_004` | 400 | **422** | `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | ciclo detectado na hierarquia de empresas |
| `SCOS_COMPANY_005` | 400 | **422** | `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | não permite inativar última matriz ativa |
| `SCOS_COMPANY_006` | 400 | **422** | `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | não permite bloquear única empresa ativa — **sem throw site hoje** (idem COMPANY_003) |
| `SCOS_COMPANY_007` | 400 | **422** | `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | transição de status inválida (`activate`/`inactivate`/`disable`/`enable`) |
| `SCOS_EMPLOYEE_001` | 400 | **422** | `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | transição de status inválida |
| `SCOS_USER_001` | 400 | **502** | `SCOS_TITLE_EXTERNAL_INTEGRATION_FAILURE` | falha ao criar usuário no Keycloak (integração externa) — confirmado com usuário |
| `SCOS_USER_002` | 400 | **502** | `SCOS_TITLE_EXTERNAL_INTEGRATION_FAILURE` | falha ao atualizar usuário no Keycloak — idem |
| `SCOS_USER_003` | 400 | **502** | `SCOS_TITLE_EXTERNAL_INTEGRATION_FAILURE` | falha ao excluir usuário no Keycloak — idem |
| `SCOS_USER_004` | 400 | **500** | `SCOS_TITLE_INTERNAL_ERROR` | erro desconhecido ao processar usuário — confirmado com usuário |
| `SCOS_AUTHORITY_001` | 400 | **404** | `SCOS_TITLE_NOT_FOUND` | "Login informado não existe" |
| `SCOS_LOGIN_001` | 400 | **400** (mantido) | `SCOS_TITLE_GENERIC` | órfão — sem mensagem PT/EN, sem throw site; decisão do usuário: manter reservado, sem status/título dedicado |
| `SCOS_LOGIN_002` | 400 | **400** (mantido) | `SCOS_TITLE_GENERIC` | idem |
| `SCOS_LOGIN_003` | 400 | **400** (mantido) | `SCOS_TITLE_GENERIC` | idem |
| `SCOS_LOGIN_010` | 400 | **401** | `SCOS_TITLE_UNAUTHORIZED` | login inativo — rejeição em tentativa de autenticação; **sem throw site hoje** (`LoginInactiveRule` não tem consumidor no repo) — confirmado com usuário |
| `SCOS_LOGIN_011` | 400 | **401** | `SCOS_TITLE_UNAUTHORIZED` | login bloqueado — idem (`LoginBlockedRule`) — confirmado com usuário |
| `SCOS_LOGIN_013` | 400 | **422** | `SCOS_TITLE_BUSINESS_RULE_VIOLATION` | transição de status inválida (comentário já dizia HTTP 422) |

---

## 4️⃣ Implementação

### Arquivos

**Modificados**:
- `scos-organization-shared/.../exception/ExceptionCodeError.java` — novos campos `httpCode` + `title`, argumentos em cada uma das 29 constantes, remove override manual de `getHttpCode()`
- `scos-organization-shared/src/main/resources/scos_message_organization.properties` — +7 chaves `SCOS_TITLE_*` (PT-BR)
- `scos-organization-shared/src/main/resources/scos_message_organization_en.properties` — +7 chaves `SCOS_TITLE_*` (EN)

### Tarefas
- [ ] **T-01**: Adicionar campo `private final int httpCode` e atualizar as 29 constantes com o valor da tabela acima
- [ ] **T-02**: Adicionar campo `private final String title` e atualizar as 29 constantes com a chave de categoria correspondente
- [ ] **T-03**: Remover o override manual de `getHttpCode()` (campos + `@Getter` já cobrem `getHttpCode()`/`getTitle()`)
- [ ] **T-04**: Adicionar as 7 chaves `SCOS_TITLE_*` em `scos_message_organization.properties` e `_en.properties`
- [ ] **T-05**: Rodar suíte de testes existente (`DepartmentServiceBeanTest`, `PositionServiceBeanTest`) — eles hoje comparam só `code` via `hasFieldOrPropertyWithValue("code", ...)`, não `httpCode`/`title`; considerar se vale adicionar asserção nesses testes já existentes (não é pré-requisito, mas fortalece a mudança)

### Riscos e Edge Cases
1. **Ordem entre changes**: as ideias `crud-catalogo-tipo-endereco-contato` e `crud-catalogo-motivos-transicao-status` também vão adicionar constantes a `ExceptionCodeError`. Se esta change (que muda a assinatura do construtor) for implementada depois daquelas, os novos códigos delas nascem sem `httpCode`/`title` e precisam de retrabalho. Recomenda-se implementar esta change **antes** ou **junto**.
2. Qualquer cliente (mesmo interno/dev) que dependa do 400/"Error" atual pra qualquer um desses 29 códigos passa a receber outro status/título — é a correção do bug, não uma mudança de contrato de dado (`code`/mensagem continuam os mesmos), mas status e título HTTP mudam de fato.
3. `SCOS_COMPANY_003/006` e `SCOS_LOGIN_010/011` recebem status/título atribuídos mesmo sem throw site hoje — decisão consciente de já deixar correto pra quando forem implementados, evitando esquecer depois.
4. **Dependência externa**: o efeito real do `title` na resposta HTTP depende de `handleScosException` (foundation) trocar `"Business Error"` por `resolveTitle(exception.getTitle())` — mudança em andamento pelo usuário em paralelo, fora deste repositório. Sem ela, o `title` calculado aqui fica pronto no dado mas não aparece na resposta.

---

## 📎 Referências
- `ExceptionCode.java` (foundation) — `default int getHttpCode() { return 400; }` / `default String getTitle() { return "Error"; }`
- `ExceptionsHandler.java` (foundation) — `handleScosException`/`handleScosNoRollbackException` usam `exception.getHttpCode()`/`exception.getTitle()` via `resolveHttpCode`/`resolveTitle`
- `openspec/specs/error-message-catalog/spec.md` — spec vigente de qualidade de mensagens (texto PT/EN), não cobre HTTP status/título
- Change arquivado `correcao-catalogo-mensagens-erro` (2026-07-05) — origem do catálogo de mensagens atual
- Ideias irmãs que adicionam códigos novos ao mesmo enum: `20260705_crud-catalogo-tipo-endereco-contato.md`, `20260705_crud-catalogo-motivos-transicao-status.md`

---
