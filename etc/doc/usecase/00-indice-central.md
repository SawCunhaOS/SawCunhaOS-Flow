# Regras e Casos de Uso por Endpoint — scos-organization

## Índice Central

Este é o documento-guia. Ele reúne tudo que é transversal a múltiplos endpoints — modelo de status, catálogo de códigos de erro reais (extraídos do código-fonte, não inferidos), chaves de configuração e achados críticos da validação cruzada contra a implementação. As regras específicas de cada recurso estão nos documentos de módulo, listados na tabela de navegação ao final.

**Fontes usadas nesta validação:** além dos 8 arquivos OpenAPI e do `domain_model.md`, este documento foi cruzado contra o código-fonte real: `StatusCompany.java`, `StatusEmployee.java`, `LoginStatus.java`, `EmployeeContractType.java`, `ExceptionCodeError.java`, `ScosOrganizationPermission.java`, os arquivos de mensagem (`scos_message_organization.properties`, `scos_message_validation.properties`), as regras de domínio (`LoginBlockedRule`, `LoginInactiveRule`), e as funções/triggers SQL (`fn_close_previous_position`, `fn_validate_*_type`, `fn_block_delete`, `fn_validate_login_profile_not_primary`) e as views `vw_login_context`/`vw_authority_response`.

---

## 1. Modelo de Status

| Entidade | Estados | Transição e permissão | Requer `reasonId` |
| --- | --- | --- | --- |
| Company | `ACTIVE` ⇄ `INACTIVE` ⇄ `DISABLED` | `enable`→`ENABLE_COMPANY`, `disable`→`DISABLE_COMPANY`, `block`→`BLOCK_COMPANY`, `unblock`→`UNBLOCK_COMPANY` | Sim, em todas |
| Employee | `ACTIVE` ⇄ `INACTIVE` ⇄ `DISABLED` | `enable`→`ENABLE_EMPLOYEE`, `disable`→`DISABLE_EMPLOYEE`, `block`→`BLOCK_EMPLOYEE`, `unblock`→`UNBLOCK_EMPLOYEE` | Sim, em todas |
| Login | `ACTIVE` ⇄ `INACTIVE` ⇄ `BLOCKED` | `enable`→`ENABLE_LOGIN`, `disable`→`DISABLE_LOGIN`, `block`/`unblock`→`UPDATE_LOGIN_STATUS` (permissão compartilhada entre as duas ações) | Sim, em todas |
| Department / Position / Profile | `ACTIVE=true` ⇄ `ACTIVE=false` | `enable`/`disable` por recurso | Não — modelo mais simples, sem catálogo de motivo |
| Reason* / Catalog (Address Type, Contact Type) | `ACTIVE=true` ⇄ `ACTIVE=false` | `enable`/`disable` por recurso | Não |

**Todas as três entidades com `reasonId` obrigatório** (Company, Employee, Login) seguem o mesmo desenho: cada transição referencia um catálogo próprio (`SCOS_REASON_ACTIVATE`, `SCOS_REASON_INACTIVATE`, `SCOS_REASON_DISABLE`, `SCOS_REASON_ENABLE`), o motivo deve estar `ACTIVE=true` e ter `entityType` compatível com a entidade, e a transição insere uma linha imutável na tabela de histórico correspondente (`SCOS_COMPANY_STATUS_HISTORY`, `SCOS_EMPLOYEE_STATUS_HISTORY`, `SCOS_LOGIN_STATUS_HISTORY`).

**Sem exclusão física ou lógica irreversível para Company e Employee.** O antigo modelo `DELETED` foi removido — `DISABLED` agora é reversível via `unblock`. Reativar (`enable`) não reverte automaticamente o `INACTIVE`/`BLOCKED` dos Logins vinculados — essa é uma ação separada.

**Login não tem mais `PENDING`.** A criação é fire-and-forget: nasce `ACTIVE` imediatamente; a Saga Keycloak sincroniza em background via Outbox (Seção 6).

**Recontratação (`POST /v1/employees/rehire`)** cobre apenas funcionários `INACTIVE` — não há caminho de recontratação para `DISABLED` por esse endpoint.

---

## 2. Catálogo de Códigos de Erro

### 2.1. Validação de Campo — prefixo `SCOS_VALIDATION_`

> ⚠️ **Correção:** o prefixo correto é `SCOS_VALIDATION_`, não `SCOS-` como constava em versões anteriores deste documento.

| Código | Significado | Onde se aplica |
| --- | --- | --- |
| `SCOS_VALIDATION_001` | Campo presente porém vazio | Todo campo obrigatório do tipo string |
| `SCOS_VALIDATION_002` | Valor abaixo do mínimo | Campos numéricos com piso |
| `SCOS_VALIDATION_003` | Campo obrigatório ausente | Todo campo obrigatório |
| `SCOS_VALIDATION_004` | Valor acima do máximo | Campos numéricos com teto |
| `SCOS_VALIDATION_005` | E-mail em formato inválido | `email` (Contato de Empresa, Funcionário) |
| `SCOS_VALIDATION_006` | Valor fora do intervalo `[min, max]` | Genérico — biblioteca compartilhada; não confirmei em qual campo específico deste módulo é aplicado hoje |
| `SCOS_VALIDATION_007` | Horário fora do formato `HH:mm:ss` ou `HH:mm` | `startTime`, `lunchStart`, `lunchEnd`, `endTime` (Work Schedule) — **aceita ambos os formatos**, não só `HH:mm` |
| `SCOS_VALIDATION_008` | Lista com tamanho fora do intervalo `[min, max]` | Genérico — não confirmei em qual campo específico deste módulo é aplicado hoje |
| `SCOS_VALIDATION_009` | CEP inválido | Definido na biblioteca compartilhada; não encontrei nenhum campo deste módulo que o utilize atualmente (endereços usam `addressId` de serviço externo, sem campo de CEP direto) |
| `SCOS_VALIDATION_010` | CNPJ inválido (dígito verificador) | `taxIdentifier` de Company |
| `SCOS_VALIDATION_011` | CPF inválido (dígito verificador) | `taxIdentifier` de Employee |
| `SCOS_VALIDATION_012` | Documento genérico inválido | Reservado — sem uso identificado neste módulo |

### 2.2. Regras de Negócio — prefixo `SCOS_<MÓDULO>_<NNN>`

Diferente do `SCOS_VALIDATION_`, este catálogo é por módulo e cobre violação de regra de negócio (não formato de campo). HTTP status abaixo é o documentado no Javadoc do próprio enum; onde não há Javadoc, uso a convenção REST padrão (`404` não encontrado, `409` conflito de unicidade, `422` regra de negócio violada) sem que isso seja uma confirmação direta do código-fonte.

> **Nota de transparência sobre a coluna "Confirmado no código":** "Sim (mensagem)" significa que o texto existe no arquivo de mensagens — não que necessariamente uma classe Java lança esse código hoje. Fiz `grep` exaustivo (todo o monorepo, todos os `.java`) especificamente para o cluster `SCOS_COMPANY_003` a `006`, porque a mensagem de `003` ("excluir") já soava como resquício do modelo antigo — e confirmei que nenhum dos quatro é lançado por código Java. Para os demais códigos da tabela, não fiz essa verificação exaustiva individual; trate "Sim (mensagem)" como "o texto existe e é plausível que esteja implementado", não como confirmação de uso ativo.

| Código | Mensagem | HTTP | Confirmado no código |
| --- | --- | --- | --- |
| `SCOS_DEPARTMENT_001` | Departamento informado não existe | 404 | Sim (mensagem) |
| `SCOS_DEPARTMENT_002` | Já existe departamento com esse código | 409 | Sim (mensagem) |
| `SCOS_DEPARTMENT_003` | Não é possível excluir — possui vínculo com Cargo | 422 | Sim (mensagem) |
| `SCOS_DEPARTMENT_004` | Departamento já ativo — impossível ativar novamente | **422** | Sim (Javadoc + HTTP) |
| `SCOS_DEPARTMENT_005` | Departamento já inativo — impossível inativar novamente | **422** | Sim (Javadoc + HTTP) |
| `SCOS_DEPARTMENT_006` | Departamento referenciado está inativo — Cargo não pode ser criado/atualizado | **422** | Sim (Javadoc + HTTP) |
| `SCOS_POSITION_001` | Cargo informado não existe | 404 | Sim (mensagem) |
| `SCOS_POSITION_002` | Já existe cargo com esse código | 409 | Sim (mensagem) |
| `SCOS_POSITION_003` | Não é possível excluir — possui vínculo com Funcionário | 422 | Sim (mensagem) |
| `SCOS_POSITION_004` | Cargo já ativo | **422** | Sim (Javadoc + HTTP) |
| `SCOS_POSITION_005` | Cargo já inativo | **422** | Sim (Javadoc + HTTP) |
| `SCOS_COMPANY_001` | Empresa informada não existe | 404 | Sim (mensagem) |
| `SCOS_COMPANY_002` | Já existe empresa com esse CNPJ | 409 | Sim (mensagem) |
| `SCOS_COMPANY_003` | Não é possível excluir — possui vínculo com colaboradores ou departamentos | 422 | ⚠️ **Mensagem existe, mas nenhuma classe Java lança este código hoje** — mesma situação de 004/005/006 (ver nota abaixo). O texto ("excluir") sugere que é resquício do modelo antigo (antes do `block`/`unblock`); se for reaproveitado, o gatilho mais coerente seria `PUT /v1/companies/{id}/block` |
| `SCOS_COMPANY_004` | Ciclo detectado na hierarquia de empresas | 422 | ⚠️ **Mensagem existe, mas nenhuma classe Java lança este código hoje** — ver Seção 3.1 |
| `SCOS_COMPANY_005` | Não é permitido inativar a última matriz ativa do sistema | 422 | ⚠️ **Não implementado** — ver Seção 3.1 |
| `SCOS_COMPANY_006` | Não é permitido excluir a única empresa ativa do sistema | 422 | ⚠️ **Não implementado** — ver Seção 3.1 |
| `SCOS_COMPANY_007` | Transição de status inválida | 422 | Sim (mensagem) |
| `SCOS_EMPLOYEE_001` | Transição de status inválida | 422 | Sim (mensagem) — único código de Employee; demais violações (CPF duplicado, idade mínima etc.) usam os códigos `SCOS_VALIDATION_`/`SCOS-CONFIGURATION` ou verificação de unicidade genérica, sem código de negócio dedicado |
| `SCOS_CONFIGURATION_001` | Configuração não encontrada pelo id | **400** | Sim (Javadoc + HTTP) — não `404` |
| `SCOS_CONFIGURATION_002` | Valor incompatível com o tipo declarado | **400** | Sim (Javadoc + HTTP) — não `422` |
| `SCOS_USER_001`–`004` | Erros de sincronização com Keycloak (criar/atualizar/excluir/desconhecido) | 502/500 | Sim (mensagem) — usados internamente pela Saga, não retornados diretamente pelos endpoints REST de Login |
| `SCOS_LOGIN_001`–`003` | Reservados (login) | — | Existem no enum, sem mensagem/Javadoc associado |
| `SCOS_LOGIN_010` | Login está inativo | — | Sim (mensagem) — usado pela cadeia de validação de autoridade, gRPC/leitura, fora do escopo deste documento |
| `SCOS_LOGIN_011` | Login está bloqueado | — | Sim (mensagem) — mesmo escopo do 010 |
| `SCOS_LOGIN_013` | Transição de status inválida | **422** | Sim (Javadoc + HTTP) |

---

## 3. Achados Críticos da Validação Cruzada

### 3.1. Regras de Company Não Implementadas (documentar como alvo, não como comportamento atual)

Três regras de negócio têm código de erro e mensagem definidos, mas **nenhuma classe Java as aplica hoje** — busquei exaustivamente por qualquer referência a `SCOS_COMPANY_004`, `005` e `006` fora do enum e do arquivo de mensagens, sem encontrar nenhuma. São regras de negócio válidas e devem ser implementadas; o desenvolvedor não deve assumir que já estão ativas:

- **`SCOS_COMPANY_004`** — impedir ciclo na hierarquia de empresas. Hoje `parentCompanyId` só é definido na criação e nunca é editável (confirmado — não consta em `UpdateCompanyRequest`), então um ciclo é estruturalmente impossível pela API atual. Esta regra provavelmente antecipa um futuro endpoint de edição de hierarquia — vale confirmar com o time se esse endpoint está planejado.
- **`SCOS_COMPANY_005`** — bloquear inativação da última matriz ativa do sistema (empresa com `parentCompanyId IS NULL` e `status=ACTIVE`). Deve ser adicionado como passo de validação em `PUT /v1/companies/{id}/disable`.
- **`SCOS_COMPANY_006`** — bloquear exclusão (bloqueio, `DISABLED`) da única empresa ativa do sistema, de qualquer nível hierárquico. Deve ser adicionado como passo de validação em `PUT /v1/companies/{id}/block`.

### 3.2. Bug Confirmado — Perfis Adicionais Não Somam Permissões na View Atual

`vw_login_context` (materialized view, base de `vw_authority_response` e de `GET /v1/logins/me`) faz `JOIN` apenas com `l.profile_id` (perfil principal do Login). **Não há nenhum `JOIN`/`UNION` com `SCOS_LOGIN_PROFILE`** (perfis adicionais) em nenhuma das duas views.

A regra de negócio correta — confirmada — é que perfis adicionais **devem** somar permissões ao perfil principal. Isso é um **bug de implementação a corrigir na view**, não uma mudança de regra. A correção da view deve unir `SCOS_LOGIN_PROFILE → SCOS_PROFILE_RESOURCE → SCOS_RESOURCE` como uma fonte adicional de `permission`, junto da já existente via perfil principal, antes do `ARRAY_AGG`. Até essa correção ser aplicada, um Login com perfil adicional **não** vê essas permissões refletidas em `GET /v1/logins/me` nem na validação de autoridade.

### 3.3. Defasagem de até 30 Minutos em Permissões

`vw_login_context` e `vw_authority_response` são **materialized views**, atualizadas via `pg_cron` a cada 30 minutos (`scos.refresh_authority_views()`) — não em tempo real. Isso afeta diretamente:

- `GET /v1/logins/me` — pode não refletir imediatamente uma troca de perfil (`PUT /v1/logins/{id}/profile/{profileId}`), atualização de recursos do perfil (`PUT /v1/profiles/{id}/resources`), ou adição/remoção de perfil adicional.
- A validação de autoridade via gRPC (fora do escopo deste documento, mas consome a mesma view).

Qualquer nota anterior dizendo que a mudança "vale na próxima chamada" está incorreta — o intervalo real é de até 30 minutos, dependente do ciclo do `pg_cron`.

### 3.4. Resolução de Empresa/Filial em `GET /v1/logins/me`

A view resolve `companyId`/`companyName` como a **matriz** (usa `COALESCE(parent, próprio)`) e `branchId`/`branchName` como a empresa direta do funcionário. Se o funcionário está na própria matriz, `companyId` e `branchId` apontam para o mesmo registro. Login `EXTERNAL`/`SERVICE` retorna `companyId`, `branchId` e `employeeId` como `null`.

### 3.5. Defeito no Contrato OpenAPI — `/v1/employees/rehire`

O path `/v1/employees/rehire` declara três métodos: `POST` (correto, `REHIRE_EMPLOYEE`), e também `GET` e `PUT` com resumos "Get employee by id" e "Update employee" — que são claramente cópias acidentais dos métodos de `/v1/employees/{id}`. Isso é reportado como defeito de especificação a corrigir pelo time de API; o documento de módulo de Employee **não** documenta esses dois métodos sob esse path, pois não representam comportamento pretendido.

### 3.6. Enum de Permissões Desatualizado (nota de implementação, não de contrato)

`ScosOrganizationPermission.java` (camada de infraestrutura) ainda usa a nomenclatura antiga — `DELETE_COMPANY`, `DELETE_EMPLOYEE`, `DELETE_LOGIN`, `UPDATE_LOGIN_STATUS` — e **não** declara `BLOCK_COMPANY`, `UNBLOCK_COMPANY`, `BLOCK_EMPLOYEE`, `UNBLOCK_EMPLOYEE`, `ENABLE_LOGIN`, `DISABLE_LOGIN`, `REHIRE_EMPLOYEE`, `CREATE_LOGIN_PROFILE`, `GET_LOGIN_PROFILE` ou `DELETE_LOGIN_PROFILE`, embora esses sejam exatamente os valores declarados em `x-authorize` nos YAMLs (confirmado, contrato e documento batem). Spring Security não exige que a string exista num enum para funcionar via `hasAuthority(...)`, então isso não bloqueia a implementação — mas o enum deveria ser atualizado por consistência, caso outro processo dependa dele (ex.: seed de `SCOS_RESOURCE`).

### 3.7. Nota — Proteção Genérica Contra `DELETE` Físico

`fn_block_delete()` é uma trigger genérica aplicada em `SCOS_REASON_ACTIVATE`, `SCOS_REASON_INACTIVATE`, `SCOS_REASON_DISABLE`, `SCOS_REASON_ENABLE`, `SCOS_REASON_POSITION_CHANGE`, `SCOS_ADDRESS_TYPE`, `SCOS_CONTACT_TYPE` e `SCOS_OUTBOX_TOPIC` — bloqueia qualquer `DELETE` SQL direto nessas tabelas, forçando o padrão `UPDATE ... SET active = false`. É proteção de defesa em profundidade a nível de banco — confirma que a ausência de endpoint `DELETE` para esses recursos é intencional, não uma lacuna de API.

### 3.8. Detalhe Confirmado — Fechamento de Posição Anterior

A trigger `fn_close_previous_position` fecha **todas** as linhas em aberto (`end_date IS NULL`) do funcionário ao inserir uma nova, atribuindo `end_date = <start_date do novo registro>` — garantindo que não há gap nem sobreposição entre períodos.

---

## 4. Chaves de Configuração

| Chave | Tipo | Consumida em |
| --- | --- | --- |
| `EMPLOYEE_MIN_AGE` | `INTEGER` | Criação de Funcionário — Documento 03 |
| `COMPANY_HIERARCHY_MAX_DEPTH` | `INTEGER` | Criação de Filial — Documento 01 |
| `LOGIN_INACTIVITY_TIMEOUT_DAYS` | `INTEGER` | Processo de background de inativação de Login — Documento 04 |
| `EMPLOYEE_EMAIL_DOMAIN` | `STRING` | Criação/atualização de Funcionário — Documento 03 |
| `DEFAULT_COMPANY_ID` | `INTEGER` | Atributos Keycloak para Login `EXTERNAL`/`SERVICE` — Documento 04 |

---

## 5. Navegação — Documentos de Módulo

| # | Documento | Cobertura | Endpoints |
| - | --- | --- | --- |
| 01 | Empresa | Company, Legal Nature, CNAE, CNAE Secundário | 34 |
| 02 | Departamento e Cargo | Department, Position, Work Schedule, Reason Position Change | 24 |
| 03 | Funcionário | Employee, Rehire, Work Schedule, Position History | 28 |
| 04 | Login, Perfil, Recurso, Sistema | Login, Profile, Resource, System | 30 |
| 05 | Catálogo e Motivos | Address Type, Contact Type, Reason (4 variações) | 36 |
| 06 | Configuração e Outbox | Configuration, Outbox Events, Outbox Topics | 10 |
| 07 | Mensagens de Erro (PT/EN) | Catálogo bilíngue de `SCOS_VALIDATION_`/`SCOS_*`, extraído dos arquivos de mensagem reais | — |

**Total: 162 endpoints.**

> Nota sobre a estrutura: agrupei por módulo funcional, não um arquivo por endpoint individual (o que geraria 162 arquivos fragmentados, sem contexto compartilhado entre operações do mesmo recurso). Cada documento de módulo cobre seus endpoints com o mesmo nível de detalhe que o documento único anterior — apenas separados para navegação mais fácil. Avise se a intenção era literalmente um arquivo por endpoint.
