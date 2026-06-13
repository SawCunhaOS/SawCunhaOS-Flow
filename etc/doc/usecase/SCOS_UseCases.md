# SCOS — Use Cases de API

> Documento de referência para construção e atualização das APIs.  
> Cada use case descreve endpoint, permissão, cache, idempotência, regras de negócio e dependências.

---

## Convenções

### Símbolos

| Símbolo | Significado |
|---|---|
| 📄 | Endpoint com paginação obrigatória |
| ⚡ | Dispara processo assíncrono via Saga Keycloak |

### Headers padrão

Todos os endpoints aceitam os headers:

| Header | Obrigatório | Descrição |
|---|---|---|
| `Authorization` | Sim | Bearer token JWT emitido pelo Keycloak |
| `X-Request-ID` | Não | UUID para rastreio da requisição e chave de idempotência |
| `Accept-Language` | Não | Idioma das mensagens de retorno — ex: `pt-BR` |

### Autorização

Cada use case declara a **Permissão** exigida (`x-authorize` no contrato). O próprio SCOS consome seu modelo de recursos — as permissões abaixo são recursos cadastrados no `scos-registry` para o sistema `SCOS_ORGANIZATION`. Usuário sem a permissão recebe `403`.

### Cache

Cada use case declara o comportamento de **Cache** (`x-cache` no contrato):

- Em endpoints **GET** — o nome indica o cache onde a resposta é armazenada e lida
- Em endpoints de **escrita** (POST/PUT/PATCH/DELETE) — os nomes indicam os caches **invalidados** pela operação

Padrão de nomenclatura: `SCOS_ORGANIZATION_{ENTIDADE}`.

### Idempotência

Endpoints de criação e atualização declaram **Idempotência** (`x-jdempotentresource` no contrato). A chave é composta pelo `X-Request-ID` + campos marcados do payload, com **TTL de 1 minuto** — requisições repetidas dentro da janela retornam a resposta original sem reexecutar.

Padrão de nomenclatura: `SCOS_ORGANIZATION_IDP_{ENTIDADE}`.

### Transições de status

Todas as entidades seguem o mesmo ciclo de vida:

```
ACTIVE ──── disable ────► INACTIVE
INACTIVE ── enable  ────► ACTIVE
INACTIVE ── delete  ────► DISABLED (soft delete — registro permanece no banco)
DISABLED ──────────────── não pode ser reativado
```

Exceção — `SCOS_LOGIN` possui estados adicionais:

```
PENDING  → aguardando Saga Keycloak concluir
ACTIVE   → acesso liberado
INACTIVE → inativado administrativamente (ex: funcionário inativado)
BLOCKED  → bloqueado por ação de segurança — pode ser desbloqueado
```

### Códigos HTTP e formato único de erro

| Código | Quando ocorre |
|---|---|
| 200 | Consulta bem-sucedida |
| 201 | Criação bem-sucedida — retorna `{ data: { id } }` |
| 204 | Atualização ou exclusão bem-sucedida — sem body |
| 400 | Requisição inválida — campos faltando ou formato errado |
| 401 | Token ausente, inválido ou expirado |
| 403 | Usuário autenticado sem a permissão exigida |
| 404 | Recurso não encontrado |
| 409 | Conflito de unicidade — ex: CNPJ ou e-mail já cadastrado |
| 422 | Violação de regra de negócio — ex: ativar empresa já ativa |

**Todos os erros — independente do código — retornam o mesmo formato** `ExceptionResponse`:

```json
{
  "data": {
    "message": "Descrição legível do erro",
    "codeError": "SCOS-XXX",
    "validationErrors": [
      { "attribute": "campo", "message": "motivo" }
    ]
  }
}
```

Por isso o contrato OpenAPI agrupa as respostas em `4XX` e `5XX` — o formato é único. O código HTTP real vem no status da resposta e o `codeError` identifica o erro específico para tratamento programático. Os códigos listados nas regras de negócio de cada UC indicam o **status HTTP esperado** em cada situação.

---

## Fases de Implementação

A ordem respeita as dependências entre entidades e isola a complexidade da integração Keycloak.

| Fase | Escopo | Use Cases | Pré-requisito |
|---|---|---|---|
| **1 — Fundação** | Configuration, Department, Position | UC-077 a 079, UC-021 a 034 | Infra base (banco, Keycloak, cache) |
| **2 — Organização** | Company + contatos + endereços | UC-001 a UC-020 | Fase 1 |
| **3 — Pessoas** | Employee + contatos + endereços | UC-035 a 053, UC-080 | Fases 1 e 2 |
| **4 — Acesso** | Profile base + Login + Saga Keycloak + Integration | UC-067 a 073, UC-054 a 066, UC-081 a 084 | Fase 3 |
| **5 — Permissões** | Resource + recursos do perfil + /me completo | UC-076, UC-074, UC-075 | Fase 4 + scos-registry operacional |

### Fase 1 — Fundação

**Objetivo:** validar a stack completa — REST, banco, cache, idempotência e autorização — com as entidades mais simples e sem dependências entre si.

**Entregável:** CRUDs de Configuration, Department e Position funcionando com todos os padrões transversais (paginação, cache, idempotência, `ExceptionResponse`, permissões).

### Fase 2 — Organização

**Objetivo:** hierarquia de empresas (matriz/filial via auto-referência), contatos e integração com o sistema externo de endereços.

**Entregável:** árvore de empresas navegável, vínculo de endereço externo validado com chave composta.

### Fase 3 — Pessoas

**Objetivo:** funcionários com hierarquia de supervisores, transferência entre empresas e cargos.

**Entregável:** ciclo de vida completo do funcionário. O `PATCH /transfer` é implementado sem o disparo da Saga nesta fase — o disparo é ligado na Fase 4.

### Fase 4 — Acesso

**Objetivo:** a fase mais complexa — perfis, logins e a Saga assíncrona de integração com o Keycloak (Outbox, retry, dead letter, compensações).

**Entregável:** criação de login dispara a Saga; bloqueio, desbloqueio e exclusão sincronizados com o Keycloak; `GET /v1/logins/me` parcial (sem lista de permissões). Disparo da Saga no `PATCH /transfer` é ativado. APIs operacionais de integração disponíveis — consulta da fila, reprocessamento de falhas e dead letter.

### Fase 5 — Permissões

**Objetivo:** fechar o ciclo de autorização integrando com o catálogo de recursos populado pelo `scos-registry`.

**Entregável:** atribuição de recursos a perfis, `GET /v1/logins/me` completo com permissões, e o próprio SCOS consumindo suas permissões (`x-authorize` ativo em produção).

---

## Company API — Fase 2

---

### UC-001 — Cadastrar empresa matriz

**Endpoint:** `POST /v1/companies`
**Permissão:** `CREATE_COMPANY`
**Cache:** invalida `SCOS_ORGANIZATION_COMPANY`
**Idempotência:** `SCOS_ORGANIZATION_IDP_COMPANY`

**Descrição:** Registra a empresa principal da organização. É o ponto de partida de toda a estrutura — sem uma matriz cadastrada não é possível criar filiais, vincular funcionários ou emitir logins. O campo `parentCompanyId` deve ser omitido ou nulo para indicar que é uma matriz.

**Regras de negócio:**
- `parentCompanyId` deve ser nulo ou ausente
- CNPJ deve ser único no sistema — retorna `409` se já cadastrado
- Data de fundação não pode ser futura
- Setor de atividade é obrigatório
- Criada com status `ACTIVE` por padrão

**Relaciona com:** UC-002, UC-003, UC-004, UC-005

---

### UC-002 — Cadastrar filial

**Endpoint:** `POST /v1/companies`
**Permissão:** `CREATE_COMPANY`
**Cache:** invalida `SCOS_ORGANIZATION_COMPANY`
**Idempotência:** `SCOS_ORGANIZATION_IDP_COMPANY`

**Descrição:** Registra uma filial vinculada a uma empresa existente. A hierarquia pode ter múltiplos níveis — uma filial pode ser empresa mãe de outra filial.

**Regras de negócio:**
- `parentCompanyId` é obrigatório e deve referenciar uma empresa existente
- A empresa referenciada deve estar com status `ACTIVE` — retorna `422` caso contrário
- CNPJ deve ser único no sistema — retorna `409` se já cadastrado
- Data de fundação não pode ser futura
- Criada com status `ACTIVE` por padrão

**Relaciona com:** UC-001, UC-003, UC-009, UC-010

---

### UC-003 — Buscar empresa por ID

**Endpoint:** `GET /v1/companies/{id}`
**Permissão:** `GET_COMPANY`
**Cache:** `SCOS_ORGANIZATION_COMPANY`

**Descrição:** Retorna os dados completos de uma empresa ou filial. Quando aplicável, inclui os dados resumidos da empresa mãe (`parentCompany`). Não inclui filiais filhas — use UC-009.

**Regras de negócio:**
- Retorna `404` se a empresa não existir

**Relaciona com:** UC-001, UC-002, UC-009

---

### UC-004 — Listar empresas 📄

**Endpoint:** `GET /v1/companies`
**Permissão:** `GET_COMPANY`
**Cache:** `SCOS_ORGANIZATION_COMPANY`

**Descrição:** Lista todas as empresas e filiais cadastradas com paginação. Usado para seleção em formulários, relatórios e visão geral da organização.

**Regras de negócio:**
- Paginação obrigatória
- Filtros disponíveis via query params: `status`, `name`

**Relaciona com:** UC-001, UC-002

---

### UC-005 — Atualizar empresa

**Endpoint:** `PUT /v1/companies/{id}`
**Permissão:** `UPDATE_COMPANY`
**Cache:** invalida `SCOS_ORGANIZATION_COMPANY`
**Idempotência:** `SCOS_ORGANIZATION_IDP_COMPANY`

**Descrição:** Atualiza os dados cadastrais de uma empresa ou filial. Não altera o vínculo com a empresa mãe — mudanças hierárquicas não são suportadas neste endpoint.

**Regras de negócio:**
- CNPJ atualizado deve continuar único — retorna `409` se em conflito
- Só pode atualizar empresa com status `ACTIVE` ou `INACTIVE` — retorna `422` se `DISABLED`
- Data de fundação não pode ser futura

**Relaciona com:** UC-003

---

### UC-006 — Ativar empresa

**Endpoint:** `PUT /v1/companies/{id}/enable`
**Permissão:** `ENABLE_COMPANY`
**Cache:** invalida `SCOS_ORGANIZATION_COMPANY`

**Descrição:** Reativa uma empresa com status `INACTIVE`. A empresa volta a aceitar novos funcionários e logins.

**Regras de negócio:**
- Só pode ativar empresa com status `INACTIVE` — retorna `422` para qualquer outro status
- Empresa `DISABLED` não pode ser reativada
- A empresa mãe deve estar `ACTIVE` para que uma filial seja ativada

**Relaciona com:** UC-007

---

### UC-007 — Inativar empresa

**Endpoint:** `PUT /v1/companies/{id}/disable`
**Permissão:** `DISABLE_COMPANY`
**Cache:** invalida `SCOS_ORGANIZATION_COMPANY`

**Descrição:** Inativa uma empresa ou filial. Funcionários vinculados ficam impedidos de receber novos logins enquanto a empresa permanecer inativa.

**Regras de negócio:**
- Só pode inativar empresa com status `ACTIVE` — retorna `422` para qualquer outro status
- Inativar a matriz não inativa automaticamente as filiais — cada unidade deve ser inativada individualmente

**Relaciona com:** UC-006

---

### UC-008 — Excluir empresa

**Endpoint:** `DELETE /v1/companies/{id}`
**Permissão:** `DELETE_COMPANY`
**Cache:** invalida `SCOS_ORGANIZATION_COMPANY`

**Descrição:** Realiza a exclusão lógica da empresa — o registro permanece no banco com status `DISABLED` para preservar o histórico.

**Regras de negócio:**
- Só pode excluir empresa com status `INACTIVE` — deve ser inativada antes
- Não é possível excluir empresa que possua filiais vinculadas — as filiais devem ser excluídas primeiro
- Não é possível excluir empresa que possua funcionários com status `ACTIVE` ou `INACTIVE`

**Relaciona com:** UC-007

---

### UC-009 — Consultar hierarquia de empresas

**Endpoint:** `GET /v1/companies/{id}/hierarchy`
**Permissão:** `GET_COMPANY`
**Cache:** `SCOS_ORGANIZATION_COMPANY`

**Descrição:** Retorna a hierarquia completa em formato de árvore recursiva — a empresa informada como raiz e todas as filiais em todos os níveis.

**Regras de negócio:**
- Retorna `404` se a empresa não existir
- Inclui filiais de todos os status na árvore

**Relaciona com:** UC-003, UC-010

---

### UC-010 — Listar filiais de uma empresa 📄

**Endpoint:** `GET /v1/companies/{id}/branches`
**Permissão:** `GET_COMPANY`
**Cache:** `SCOS_ORGANIZATION_COMPANY`

**Descrição:** Lista as filiais diretamente vinculadas à empresa informada com paginação. Retorna apenas o primeiro nível, sem recursividade.

**Regras de negócio:**
- Paginação obrigatória
- Retorna `404` se a empresa não existir
- Filtros disponíveis via query params: `status`

**Relaciona com:** UC-009

---

### UC-011 — Listar contatos da empresa 📄

**Endpoint:** `GET /v1/companies/{companyId}/contacts`
**Permissão:** `GET_COMPANY_CONTACT`
**Cache:** `SCOS_ORGANIZATION_COMPANY_CONTACT`

**Descrição:** Lista todos os contatos cadastrados para uma empresa com paginação.

**Regras de negócio:**
- Paginação obrigatória
- Retorna `404` se a empresa não existir

**Relaciona com:** UC-012

---

### UC-012 — Adicionar contato à empresa

**Endpoint:** `POST /v1/companies/{companyId}/contacts`
**Permissão:** `CREATE_COMPANY_CONTACT`
**Cache:** invalida `SCOS_ORGANIZATION_COMPANY_CONTACT`, `SCOS_ORGANIZATION_COMPANY`
**Idempotência:** `SCOS_ORGANIZATION_IDP_COMPANY_CONTACT`

**Descrição:** Adiciona um contato a uma empresa. Uma empresa pode ter múltiplos contatos de tipos diferentes — ex: `COMMERCIAL`, `FINANCIAL`, `SUPPORT`.

**Regras de negócio:**
- Não é permitido duplicar `phone` ou `email` para a mesma empresa — retorna `409`
- A empresa deve estar com status `ACTIVE`
- O tipo do contato deve ser um valor válido do enum

**Relaciona com:** UC-011

---

### UC-013 — Buscar contato da empresa por ID

**Endpoint:** `GET /v1/companies/{companyId}/contacts/{id}`
**Permissão:** `GET_COMPANY_CONTACT`
**Cache:** `SCOS_ORGANIZATION_COMPANY_CONTACT`

**Descrição:** Retorna os dados de um contato específico de uma empresa.

**Regras de negócio:**
- Retorna `404` se o contato não pertencer à empresa informada

---

### UC-014 — Atualizar contato da empresa

**Endpoint:** `PUT /v1/companies/{companyId}/contacts/{id}`
**Permissão:** `UPDATE_COMPANY_CONTACT`
**Cache:** invalida `SCOS_ORGANIZATION_COMPANY_CONTACT`, `SCOS_ORGANIZATION_COMPANY`

**Descrição:** Atualiza os dados de um contato existente de uma empresa.

**Regras de negócio:**
- O contato deve pertencer à empresa informada — retorna `404` caso contrário
- Não é permitido duplicar `phone` ou `email` para a mesma empresa — retorna `409`

**Relaciona com:** UC-013

---

### UC-015 — Remover contato da empresa

**Endpoint:** `DELETE /v1/companies/{companyId}/contacts/{id}`
**Permissão:** `DELETE_COMPANY_CONTACT`
**Cache:** invalida `SCOS_ORGANIZATION_COMPANY_CONTACT`, `SCOS_ORGANIZATION_COMPANY`

**Descrição:** Remove definitivamente um contato de uma empresa.

**Regras de negócio:**
- O contato deve pertencer à empresa informada — retorna `404` caso contrário

**Relaciona com:** UC-013

---

### UC-016 — Listar endereços da empresa 📄

**Endpoint:** `GET /v1/companies/{companyId}/addresses`
**Permissão:** `GET_COMPANY_ADDRESS`
**Cache:** `SCOS_ORGANIZATION_COMPANY_ADDRESS`

**Descrição:** Lista todos os endereços vinculados a uma empresa com paginação.

**Regras de negócio:**
- Paginação obrigatória
- Retorna `404` se a empresa não existir

**Relaciona com:** UC-017

---

### UC-017 — Vincular endereço à empresa

**Endpoint:** `POST /v1/companies/{companyId}/addresses`
**Permissão:** `CREATE_COMPANY_ADDRESS`
**Cache:** invalida `SCOS_ORGANIZATION_COMPANY_ADDRESS`, `SCOS_ORGANIZATION_COMPANY`
**Idempotência:** `SCOS_ORGANIZATION_IDP_COMPANY_ADDRESS`

**Descrição:** Vincula um endereço gerenciado por um sistema externo à empresa. O SCOS armazena apenas o `addressId` de referência, o tipo e as coordenadas geográficas.

**Regras de negócio:**
- O mesmo `addressId` não pode ser vinculado duas vezes à mesma empresa — chave composta, retorna `409`
- A empresa deve estar com status `ACTIVE`
- Coordenadas geográficas são obrigatórias
- A integridade do `addressId` é responsabilidade da aplicação — não há FK no banco

**Relaciona com:** UC-016

---

### UC-018 — Buscar endereço da empresa por ID

**Endpoint:** `GET /v1/companies/{companyId}/addresses/{id}`
**Permissão:** `GET_COMPANY_ADDRESS`
**Cache:** `SCOS_ORGANIZATION_COMPANY_ADDRESS`

**Descrição:** Retorna os dados do vínculo de endereço — tipo, número, complemento e coordenadas. Os dados completos do endereço devem ser consultados no sistema externo usando o `addressId`.

**Regras de negócio:**
- Retorna `404` se o endereço não pertencer à empresa informada

---

### UC-019 — Atualizar endereço da empresa

**Endpoint:** `PUT /v1/companies/{companyId}/addresses/{id}`
**Permissão:** `UPDATE_COMPANY_ADDRESS`
**Cache:** invalida `SCOS_ORGANIZATION_COMPANY_ADDRESS`, `SCOS_ORGANIZATION_COMPANY`

**Descrição:** Atualiza os dados do vínculo — tipo, número, complemento e coordenadas. Não altera o `addressId` de referência externa.

**Regras de negócio:**
- O endereço deve pertencer à empresa informada — retorna `404` caso contrário

**Relaciona com:** UC-018

---

### UC-020 — Desvincular endereço da empresa

**Endpoint:** `DELETE /v1/companies/{companyId}/addresses/{id}`
**Permissão:** `DELETE_COMPANY_ADDRESS`
**Cache:** invalida `SCOS_ORGANIZATION_COMPANY_ADDRESS`, `SCOS_ORGANIZATION_COMPANY`

**Descrição:** Remove o vínculo entre o endereço externo e a empresa. O registro no sistema externo não é afetado.

**Regras de negócio:**
- O endereço deve pertencer à empresa informada — retorna `404` caso contrário

**Relaciona com:** UC-018

---

## Department API — Fase 1

---

### UC-021 — Criar departamento

**Endpoint:** `POST /v1/departments`
**Permissão:** `CREATE_DEPARTMENT`
**Cache:** invalida `SCOS_ORGANIZATION_DEPARTMENT`
**Idempotência:** `SCOS_ORGANIZATION_IDP_DEPARTMENT`

**Descrição:** Cria um novo departamento da organização. Um departamento precisa existir antes que cargos possam ser cadastrados.

**Regras de negócio:**
- O código do departamento deve ser único — retorna `409` se duplicado
- Criado com status `ACTIVE` por padrão

**Relaciona com:** UC-028

---

### UC-022 — Listar departamentos 📄

**Endpoint:** `GET /v1/departments`
**Permissão:** `GET_DEPARTMENT`
**Cache:** `SCOS_ORGANIZATION_DEPARTMENT`

**Descrição:** Lista todos os departamentos com paginação. Usado para seleção ao cadastrar cargos e filtrar funcionários por área.

**Regras de negócio:**
- Paginação obrigatória
- Filtros disponíveis via query params: `active`

---

### UC-023 — Buscar departamento por ID

**Endpoint:** `GET /v1/departments/{id}`
**Permissão:** `GET_DEPARTMENT`
**Cache:** `SCOS_ORGANIZATION_DEPARTMENT`

**Descrição:** Retorna os dados de um departamento específico.

**Regras de negócio:**
- Retorna `404` se o departamento não existir

---

### UC-024 — Atualizar departamento

**Endpoint:** `PUT /v1/departments/{id}`
**Permissão:** `UPDATE_DEPARTMENT`
**Cache:** invalida `SCOS_ORGANIZATION_DEPARTMENT`
**Idempotência:** `SCOS_ORGANIZATION_IDP_DEPARTMENT`

**Descrição:** Atualiza o código ou descrição de um departamento.

**Regras de negócio:**
- O código atualizado deve continuar único — retorna `409` se em conflito
- Não é possível atualizar departamento `DISABLED`

**Relaciona com:** UC-023

---

### UC-025 — Ativar departamento

**Endpoint:** `PUT /v1/departments/{id}/enable`
**Permissão:** `ENABLE_DEPARTMENT`
**Cache:** invalida `SCOS_ORGANIZATION_DEPARTMENT`

**Descrição:** Reativa um departamento com status `INACTIVE`. Os cargos vinculados voltam a estar disponíveis para atribuição.

**Regras de negócio:**
- Só pode ativar departamento com status `INACTIVE` — retorna `422` para qualquer outro status

**Relaciona com:** UC-026

---

### UC-026 — Inativar departamento

**Endpoint:** `PUT /v1/departments/{id}/disable`
**Permissão:** `DISABLE_DEPARTMENT`
**Cache:** invalida `SCOS_ORGANIZATION_DEPARTMENT`

**Descrição:** Inativa um departamento. Os cargos vinculados não podem ser atribuídos a novos funcionários enquanto o departamento estiver inativo.

**Regras de negócio:**
- Só pode inativar departamento com status `ACTIVE` — retorna `422` para qualquer outro status
- Não inativa automaticamente os cargos vinculados

**Relaciona com:** UC-025

---

### UC-027 — Excluir departamento

**Endpoint:** `DELETE /v1/departments/{id}`
**Permissão:** `DELETE_DEPARTMENT`
**Cache:** invalida `SCOS_ORGANIZATION_DEPARTMENT`

**Descrição:** Realiza a exclusão lógica do departamento — registro permanece com status `DISABLED`.

**Regras de negócio:**
- Só pode excluir departamento com status `INACTIVE`
- Não é possível excluir departamento que possua cargos com status `ACTIVE` ou `INACTIVE`

**Relaciona com:** UC-026

---

## Position API — Fase 1

---

### UC-028 — Criar cargo

**Endpoint:** `POST /v1/positions`
**Permissão:** `CREATE_POSITION`
**Cache:** invalida `SCOS_ORGANIZATION_POSITION`
**Idempotência:** `SCOS_ORGANIZATION_IDP_POSITION`

**Descrição:** Cria um cargo vinculado a um departamento. Um cargo precisa existir antes que funcionários possam ser registrados com ele.

**Regras de negócio:**
- O código do cargo deve ser único — retorna `409` se duplicado
- O departamento informado deve estar com status `ACTIVE`
- Criado com status `ACTIVE` por padrão

**Relaciona com:** UC-021, UC-035

---

### UC-029 — Listar cargos 📄

**Endpoint:** `GET /v1/positions`
**Permissão:** `GET_POSITION`
**Cache:** `SCOS_ORGANIZATION_POSITION`

**Descrição:** Lista todos os cargos com paginação. Pode ser filtrado por departamento.

**Regras de negócio:**
- Paginação obrigatória
- Filtros disponíveis via query params: `departmentId`, `active`

---

### UC-030 — Buscar cargo por ID

**Endpoint:** `GET /v1/positions/{id}`
**Permissão:** `GET_POSITION`
**Cache:** `SCOS_ORGANIZATION_POSITION`

**Descrição:** Retorna os dados de um cargo incluindo o departamento ao qual pertence.

**Regras de negócio:**
- Retorna `404` se o cargo não existir

---

### UC-031 — Atualizar cargo

**Endpoint:** `PUT /v1/positions/{id}`
**Permissão:** `UPDATE_POSITION`
**Cache:** invalida `SCOS_ORGANIZATION_POSITION`
**Idempotência:** `SCOS_ORGANIZATION_IDP_POSITION`

**Descrição:** Atualiza o código, descrição ou departamento de um cargo.

**Regras de negócio:**
- O código atualizado deve continuar único — retorna `409` se em conflito
- O novo departamento deve estar com status `ACTIVE`
- Não é possível atualizar cargo `DISABLED`

**Relaciona com:** UC-030

---

### UC-032 — Ativar cargo

**Endpoint:** `PUT /v1/positions/{id}/enable`
**Permissão:** `ENABLE_POSITION`
**Cache:** invalida `SCOS_ORGANIZATION_POSITION`

**Descrição:** Reativa um cargo com status `INACTIVE`.

**Regras de negócio:**
- Só pode ativar cargo com status `INACTIVE` — retorna `422` para qualquer outro status
- O departamento do cargo deve estar com status `ACTIVE`

**Relaciona com:** UC-033

---

### UC-033 — Inativar cargo

**Endpoint:** `PUT /v1/positions/{id}/disable`
**Permissão:** `DISABLE_POSITION`
**Cache:** invalida `SCOS_ORGANIZATION_POSITION`

**Descrição:** Inativa um cargo. Funcionários que já possuem o cargo não são afetados — apenas novas atribuições são bloqueadas.

**Regras de negócio:**
- Só pode inativar cargo com status `ACTIVE` — retorna `422` para qualquer outro status

**Relaciona com:** UC-032

---

### UC-034 — Excluir cargo

**Endpoint:** `DELETE /v1/positions/{id}`
**Permissão:** `DELETE_POSITION`
**Cache:** invalida `SCOS_ORGANIZATION_POSITION`

**Descrição:** Realiza a exclusão lógica do cargo — registro permanece com status `DISABLED`.

**Regras de negócio:**
- Só pode excluir cargo com status `INACTIVE`
- Não é possível excluir cargo que possua funcionários com status `ACTIVE` ou `INACTIVE`

**Relaciona com:** UC-033

---

## Employee API — Fase 3

---

### UC-035 — Registrar funcionário

**Endpoint:** `POST /v1/employees`
**Permissão:** `CREATE_EMPLOYEE`
**Cache:** invalida `SCOS_ORGANIZATION_EMPLOYEE`
**Idempotência:** `SCOS_ORGANIZATION_IDP_EMPLOYEE`

**Descrição:** Registra um novo funcionário vinculado a um cargo e empresa ou filial. CPF e e-mail são identificadores únicos no sistema.

**Regras de negócio:**
- CPF deve ser único e válido — retorna `409` se duplicado
- E-mail deve ser único — retorna `409` se duplicado
- O cargo informado deve estar com status `ACTIVE`
- A empresa informada deve estar com status `ACTIVE`
- `supervisorId` é opcional — funcionários no topo da hierarquia não possuem supervisor
- Quando informado, o supervisor deve ser funcionário com status `ACTIVE`
- Data de nascimento e data de contratação não podem ser futuras
- Data de nascimento deve indicar idade mínima de 14 anos na data de contratação (menor aprendiz)
- Criado com status `ACTIVE` por padrão

**Relaciona com:** UC-028, UC-001, UC-002, UC-054

---

### UC-036 — Listar funcionários 📄

**Endpoint:** `GET /v1/employees`
**Permissão:** `GET_EMPLOYEE`
**Cache:** `SCOS_ORGANIZATION_EMPLOYEE`

**Descrição:** Lista todos os funcionários com paginação. Usado para gestão de equipes e seleção em formulários administrativos.

**Regras de negócio:**
- Paginação obrigatória
- Filtros disponíveis via query params: `companyId`, `positionId`, `status`

---

### UC-037 — Buscar funcionário por ID

**Endpoint:** `GET /v1/employees/{id}`
**Permissão:** `GET_EMPLOYEE`
**Cache:** `SCOS_ORGANIZATION_EMPLOYEE`

**Descrição:** Retorna os dados completos de um funcionário incluindo cargo com departamento, empresa ou filial e supervisor direto.

**Regras de negócio:**
- Retorna `404` se o funcionário não existir

---

### UC-038 — Atualizar funcionário

**Endpoint:** `PUT /v1/employees/{id}`
**Permissão:** `UPDATE_EMPLOYEE`
**Cache:** invalida `SCOS_ORGANIZATION_EMPLOYEE`
**Idempotência:** `SCOS_ORGANIZATION_IDP_EMPLOYEE`

**Descrição:** Atualiza os dados pessoais de um funcionário — nome, e-mail, datas e observação. Para mudanças de empresa, cargo ou supervisor use UC-041. O e-mail do funcionário é independente do e-mail de login — alterá-lo não afeta o acesso ao sistema (o login é atualizado via UC-062).

**Regras de negócio:**
- E-mail atualizado deve continuar único — retorna `409` se em conflito
- Não é possível atualizar funcionário `DISABLED`
- Datas de nascimento e contratação não podem ser futuras

**Relaciona com:** UC-037

---

### UC-039 — Ativar funcionário

**Endpoint:** `PUT /v1/employees/{id}/enable`
**Permissão:** `ENABLE_EMPLOYEE`
**Cache:** invalida `SCOS_ORGANIZATION_EMPLOYEE`

**Descrição:** Reativa um funcionário com status `INACTIVE`. O funcionário volta a poder receber novos logins e ser atribuído como supervisor.

**Regras de negócio:**
- Só pode ativar funcionário com status `INACTIVE` — retorna `422` para qualquer outro status
- A empresa vinculada deve estar com status `ACTIVE`

**Relaciona com:** UC-040

---

### UC-040 — Inativar funcionário ⚡

**Endpoint:** `PUT /v1/employees/{id}/disable`
**Permissão:** `DISABLE_EMPLOYEE`
**Cache:** invalida `SCOS_ORGANIZATION_EMPLOYEE`, `SCOS_ORGANIZATION_LOGIN`, `SCOS_ORGANIZATION_LOGIN_INFO`

**Descrição:** Inativa um funcionário administrativamente. Os logins vinculados são automaticamente inativados (status `INACTIVE`) e desabilitados no Keycloak via Saga — diferente de bloqueio de segurança, esta é uma ação administrativa.

**Regras de negócio:**
- Só pode inativar funcionário com status `ACTIVE` — retorna `422` para qualquer outro status
- Os logins vinculados devem ser automaticamente inativados (status `INACTIVE`)
- Para cada login inativado, dispara `SCOS_INTEGRATION_KEYCLOAK` com `TYPE=UPDATE` desabilitando o usuário no Keycloak (`enabled=false`)
- Logins inativados por este fluxo podem ser reativados ao reativar o funcionário via UC-039

**Relaciona com:** UC-039, UC-063

---

### UC-041 — Transferir funcionário ⚡

**Endpoint:** `PATCH /v1/employees/{id}/transfer`
**Permissão:** `TRANSFER_EMPLOYEE`
**Cache:** invalida `SCOS_ORGANIZATION_EMPLOYEE`, `SCOS_ORGANIZATION_LOGIN_INFO`

**Descrição:** Move o funcionário para outra empresa, filial, cargo ou supervisor. Todos os campos são opcionais — informar apenas o que mudou. Quando a empresa ou filial é alterada, dispara a Saga Keycloak para atualizar os atributos do token.

**Regras de negócio:**
- Pelo menos um campo deve ser informado — retorna `400` se o body estiver vazio
- A nova empresa, o novo cargo e o novo supervisor devem estar com status `ACTIVE`
- O supervisor não pode ser o próprio funcionário nem um de seus subordinados diretos ou indiretos — evita ciclos na hierarquia
- O funcionário deve estar com status `ACTIVE` para ser transferido
- Quando empresa ou filial muda — dispara `SCOS_INTEGRATION_KEYCLOAK` com `TYPE=UPDATE` para atualizar `company_id` e `branch_id` no token

**Relaciona com:** UC-035, UC-037, UC-054

---

### UC-042 — Consultar hierarquia do funcionário

**Endpoint:** `GET /v1/employees/{id}/hierarchy`
**Permissão:** `GET_EMPLOYEE`
**Cache:** `SCOS_ORGANIZATION_EMPLOYEE`

**Descrição:** Retorna a cadeia completa de supervisores do funcionário em formato de árvore ascendente — do funcionário informado até o topo da hierarquia.

**Regras de negócio:**
- Retorna `404` se o funcionário não existir
- Funcionários sem supervisor retornam nó único sem ascendentes

**Relaciona com:** UC-035, UC-037

---

### UC-043 — Excluir funcionário

**Endpoint:** `DELETE /v1/employees/{id}`
**Permissão:** `DELETE_EMPLOYEE`
**Cache:** invalida `SCOS_ORGANIZATION_EMPLOYEE`

**Descrição:** Realiza a exclusão lógica do funcionário — registro permanece com status `DISABLED`.

**Regras de negócio:**
- Só pode excluir funcionário com status `INACTIVE`
- Não é possível excluir funcionário que possua logins com status `ACTIVE`, `INACTIVE` ou `BLOCKED` — devem ser excluídos antes via UC-066
- Não é possível excluir funcionário que seja supervisor de outros funcionários ativos

**Relaciona com:** UC-040, UC-066

---

### UC-044 — Listar contatos do funcionário 📄

**Endpoint:** `GET /v1/employees/{employeeId}/contacts`
**Permissão:** `GET_EMPLOYEE_CONTACT`
**Cache:** `SCOS_ORGANIZATION_EMPLOYEE_CONTACT`

**Descrição:** Lista todos os contatos cadastrados para um funcionário com paginação.

**Regras de negócio:**
- Paginação obrigatória
- Retorna `404` se o funcionário não existir

**Relaciona com:** UC-045

---

### UC-045 — Adicionar contato ao funcionário

**Endpoint:** `POST /v1/employees/{employeeId}/contacts`
**Permissão:** `CREATE_EMPLOYEE_CONTACT`
**Cache:** invalida `SCOS_ORGANIZATION_EMPLOYEE_CONTACT`, `SCOS_ORGANIZATION_EMPLOYEE`
**Idempotência:** `SCOS_ORGANIZATION_IDP_EMPLOYEE_CONTACT`

**Descrição:** Adiciona um contato telefônico ao funcionário. Tipos diferentes — ex: `MOBILE`, `WORK`, `HOME`.

**Regras de negócio:**
- Não é permitido duplicar o mesmo `phone` com o mesmo `type` para o mesmo funcionário — retorna `409`
- O funcionário deve estar com status `ACTIVE`

**Relaciona com:** UC-044

---

### UC-046 — Buscar contato do funcionário por ID

**Endpoint:** `GET /v1/employees/{employeeId}/contacts/{id}`
**Permissão:** `GET_EMPLOYEE_CONTACT`
**Cache:** `SCOS_ORGANIZATION_EMPLOYEE_CONTACT`

**Descrição:** Retorna os dados de um contato específico de um funcionário.

**Regras de negócio:**
- Retorna `404` se o contato não pertencer ao funcionário informado

---

### UC-047 — Atualizar contato do funcionário

**Endpoint:** `PUT /v1/employees/{employeeId}/contacts/{id}`
**Permissão:** `UPDATE_EMPLOYEE_CONTACT`
**Cache:** invalida `SCOS_ORGANIZATION_EMPLOYEE_CONTACT`, `SCOS_ORGANIZATION_EMPLOYEE`

**Descrição:** Atualiza o tipo ou telefone de um contato de um funcionário.

**Regras de negócio:**
- O contato deve pertencer ao funcionário informado — retorna `404` caso contrário
- Não é permitido duplicar `phone` e `type` — retorna `409`

**Relaciona com:** UC-046

---

### UC-048 — Remover contato do funcionário

**Endpoint:** `DELETE /v1/employees/{employeeId}/contacts/{id}`
**Permissão:** `DELETE_EMPLOYEE_CONTACT`
**Cache:** invalida `SCOS_ORGANIZATION_EMPLOYEE_CONTACT`, `SCOS_ORGANIZATION_EMPLOYEE`

**Descrição:** Remove definitivamente um contato de um funcionário.

**Regras de negócio:**
- O contato deve pertencer ao funcionário informado — retorna `404` caso contrário

**Relaciona com:** UC-046

---

### UC-049 — Listar endereços do funcionário 📄

**Endpoint:** `GET /v1/employees/{employeeId}/addresses`
**Permissão:** `GET_EMPLOYEE_ADDRESS`
**Cache:** `SCOS_ORGANIZATION_EMPLOYEE_ADDRESS`

**Descrição:** Lista todos os endereços vinculados a um funcionário com paginação.

**Regras de negócio:**
- Paginação obrigatória
- Retorna `404` se o funcionário não existir

**Relaciona com:** UC-050

---

### UC-050 — Vincular endereço ao funcionário

**Endpoint:** `POST /v1/employees/{employeeId}/addresses`
**Permissão:** `CREATE_EMPLOYEE_ADDRESS`
**Cache:** invalida `SCOS_ORGANIZATION_EMPLOYEE_ADDRESS`, `SCOS_ORGANIZATION_EMPLOYEE`
**Idempotência:** `SCOS_ORGANIZATION_IDP_EMPLOYEE_ADDRESS`

**Descrição:** Vincula um endereço gerenciado por sistema externo ao funcionário. O SCOS armazena apenas o `addressId`, o tipo e as coordenadas.

**Regras de negócio:**
- O mesmo `addressId` não pode ser vinculado duas vezes ao mesmo funcionário — chave composta, retorna `409`
- O funcionário deve estar com status `ACTIVE`
- Coordenadas geográficas são obrigatórias
- A integridade do `addressId` é responsabilidade da aplicação

**Relaciona com:** UC-049

---

### UC-051 — Buscar endereço do funcionário por ID

**Endpoint:** `GET /v1/employees/{employeeId}/addresses/{id}`
**Permissão:** `GET_EMPLOYEE_ADDRESS`
**Cache:** `SCOS_ORGANIZATION_EMPLOYEE_ADDRESS`

**Descrição:** Retorna os dados do vínculo de endereço de um funcionário.

**Regras de negócio:**
- Retorna `404` se o endereço não pertencer ao funcionário informado

---

### UC-052 — Atualizar endereço do funcionário

**Endpoint:** `PUT /v1/employees/{employeeId}/addresses/{id}`
**Permissão:** `UPDATE_EMPLOYEE_ADDRESS`
**Cache:** invalida `SCOS_ORGANIZATION_EMPLOYEE_ADDRESS`, `SCOS_ORGANIZATION_EMPLOYEE`

**Descrição:** Atualiza os dados do vínculo — tipo, número, complemento e coordenadas.

**Regras de negócio:**
- O endereço deve pertencer ao funcionário informado — retorna `404` caso contrário

**Relaciona com:** UC-051

---

### UC-053 — Desvincular endereço do funcionário

**Endpoint:** `DELETE /v1/employees/{employeeId}/addresses/{id}`
**Permissão:** `DELETE_EMPLOYEE_ADDRESS`
**Cache:** invalida `SCOS_ORGANIZATION_EMPLOYEE_ADDRESS`, `SCOS_ORGANIZATION_EMPLOYEE`

**Descrição:** Remove o vínculo entre o endereço externo e o funcionário.

**Regras de negócio:**
- O endereço deve pertencer ao funcionário informado — retorna `404` caso contrário

**Relaciona com:** UC-051

---

### UC-080 — Listar subordinados diretos 📄 *(novo)*

**Endpoint:** `GET /v1/employees/{id}/subordinates`
**Permissão:** `GET_EMPLOYEE`
**Cache:** `SCOS_ORGANIZATION_EMPLOYEE`

**Descrição:** Lista os funcionários que reportam diretamente ao funcionário informado, com paginação. Complementa o UC-042 — enquanto a hierarquia sobe (cadeia de supervisores), este desce um nível (equipe direta). Usado por gestores e em fluxos de aprovação.

**Regras de negócio:**
- Paginação obrigatória
- Retorna `404` se o funcionário não existir
- Filtros disponíveis via query params: `status`

**Relaciona com:** UC-042, UC-037

---

## Login API — Fase 4

---

### UC-054 — Criar login de funcionário ⚡

**Endpoint:** `POST /v1/employees/{employeeId}/logins`
**Permissão:** `CREATE_LOGIN`
**Cache:** invalida `SCOS_ORGANIZATION_LOGIN`
**Idempotência:** `SCOS_ORGANIZATION_IDP_LOGIN`

**Descrição:** Cria um acesso para um funcionário já cadastrado. O tipo é automaticamente `EMPLOYEE`. Inicia a Saga Keycloak — o usuário é criado assincronamente com os atributos da empresa e filial injetados no token.

**Regras de negócio:**
- O funcionário deve estar com status `ACTIVE`
- O e-mail de login deve ter formato válido e ser único — retorna `409` se duplicado
- O perfil informado deve estar com status `ACTIVE`
- Um funcionário pode ter apenas um login `ACTIVE` ou `PENDING` por vez — retorna `422` se já existir
- Criado com status `PENDING` até a Saga concluir
- Dispara `SCOS_INTEGRATION_KEYCLOAK` com `TYPE=CREATE`
- Os atributos `company_id`, `branch_id` e `employee_id` são incluídos no payload Keycloak

**Relaciona com:** UC-035, UC-057, UC-068

---

### UC-055 — Listar logins de um funcionário 📄

**Endpoint:** `GET /v1/employees/{employeeId}/logins`
**Permissão:** `GET_LOGIN`
**Cache:** `SCOS_ORGANIZATION_LOGIN`

**Descrição:** Lista os logins vinculados a um funcionário com paginação. Inclui todos os status para histórico e auditoria.

**Regras de negócio:**
- Paginação obrigatória
- Retorna `404` se o funcionário não existir

**Relaciona com:** UC-035, UC-054

---

### UC-056 — Buscar login de funcionário por ID

**Endpoint:** `GET /v1/employees/{employeeId}/logins/{id}`
**Permissão:** `GET_LOGIN`
**Cache:** `SCOS_ORGANIZATION_LOGIN`

**Descrição:** Retorna os dados de um login específico de um funcionário incluindo perfil e status da integração Keycloak.

**Regras de negócio:**
- Retorna `404` se o login não pertencer ao funcionário informado

**Relaciona com:** UC-054

---

### UC-057 — Criar login standalone ⚡

**Endpoint:** `POST /v1/logins`
**Permissão:** `CREATE_LOGIN`
**Cache:** invalida `SCOS_ORGANIZATION_LOGIN`
**Idempotência:** `SCOS_ORGANIZATION_IDP_LOGIN`

**Descrição:** Cria um login sem vínculo com funcionário — para usuários externos (`EXTERNAL`) ou contas de serviço (`SERVICE`). Inicia a Saga Keycloak.

**Regras de negócio:**
- O tipo deve ser `EXTERNAL` ou `SERVICE` — tipo `EMPLOYEE` retorna `422`, use UC-054
- O e-mail de login deve ter formato válido e ser único — retorna `409` se duplicado
- O perfil informado deve estar com status `ACTIVE`
- Criado com status `PENDING` até a Saga concluir
- Para tipo `SERVICE`: atributos de empresa e filial não são incluídos no payload Keycloak

**Relaciona com:** UC-054, UC-068

---

### UC-058 — Listar todos os logins 📄

**Endpoint:** `GET /v1/logins`
**Permissão:** `GET_LOGIN`
**Cache:** `SCOS_ORGANIZATION_LOGIN`

**Descrição:** Lista todos os logins do sistema com paginação — funcionários, externos e serviços.

**Regras de negócio:**
- Paginação obrigatória
- Filtros disponíveis via query params: `type`, `status`, `employeeId`

---

### UC-059 — Consultar contexto do usuário autenticado

**Endpoint:** `GET /v1/logins/me`
**Permissão:** `GET_LOGIN_INFO`
**Cache:** `SCOS_ORGANIZATION_LOGIN_INFO`

**Descrição:** Retorna o contexto completo do usuário autenticado: dados pessoais, empresa, filial, cargo, perfil e lista de permissões. Usado na inicialização de interfaces.

**Regras de negócio:**
- O token deve ser válido e não expirado — retorna `401` caso contrário
- Retorna `403` se o login estiver `BLOCKED` ou `INACTIVE`
- Dados de empresa e filial são extraídos dos atributos do token
- Dados de perfil e permissões são buscados no banco em tempo real
- Se o `KEYCLOAK_ID` for nulo (Saga em processamento), retorna os dados disponíveis sem as permissões
- A permissão `GET_LOGIN_INFO` deve constar em todos os perfis por padrão — sem ela o usuário não consegue inicializar a própria interface

**Relaciona com:** UC-054, UC-057, UC-065, UC-075

---

### UC-060 — Buscar login por ID

**Endpoint:** `GET /v1/logins/{id}`
**Permissão:** `GET_LOGIN`
**Cache:** `SCOS_ORGANIZATION_LOGIN`

**Descrição:** Retorna os dados de um login pelo identificador interno incluindo perfil, status e `KEYCLOAK_ID`.

**Regras de negócio:**
- Retorna `404` se o login não existir

---

### UC-061 — Buscar login por Keycloak ID

**Endpoint:** `GET /v1/logins/keycloak/{keycloakId}`
**Permissão:** `GET_LOGIN`
**Cache:** `SCOS_ORGANIZATION_LOGIN`

**Descrição:** Busca um login pelo `KEYCLOAK_ID`. Utilizado para reconciliação entre SCOS e Keycloak e para identificar o usuário a partir de eventos do Keycloak.

**Regras de negócio:**
- Retorna `404` se nenhum login possuir o `keycloakId` informado
- Logins com `KEYCLOAK_ID` nulo (Saga em andamento) não são retornados

**Relaciona com:** UC-054, UC-057

---

### UC-062 — Atualizar login ⚡

**Endpoint:** `PUT /v1/logins/{id}`
**Permissão:** `UPDATE_LOGIN`
**Cache:** invalida `SCOS_ORGANIZATION_LOGIN`, `SCOS_ORGANIZATION_LOGIN_INFO`
**Idempotência:** `SCOS_ORGANIZATION_IDP_LOGIN`

**Descrição:** Atualiza o e-mail de login. Dispara atualização do username no Keycloak via Saga.

**Regras de negócio:**
- O novo e-mail deve ser único — retorna `409` se em conflito
- Só pode atualizar login com status `ACTIVE` — retorna `422` para qualquer outro status
- Dispara `SCOS_INTEGRATION_KEYCLOAK` com `TYPE=UPDATE`

**Relaciona com:** UC-060

---

### UC-063 — Bloquear login ⚡

**Endpoint:** `PUT /v1/logins/{id}/block`
**Permissão:** `UPDATE_LOGIN_STATUS`
**Cache:** invalida `SCOS_ORGANIZATION_LOGIN`, `SCOS_ORGANIZATION_LOGIN_INFO`

**Descrição:** Bloqueia o acesso por razão de segurança. O status muda para `BLOCKED` e o usuário é desabilitado no Keycloak via Saga — novos tokens deixam de ser emitidos na origem. O token atual expira pelo TTL configurado.

**Regras de negócio:**
- Só pode bloquear login com status `ACTIVE` — retorna `422` para qualquer outro status
- O bloqueio é refletido no `scos-registry` imediatamente — validações retornam acesso negado mesmo antes da Saga concluir
- Dispara `SCOS_INTEGRATION_KEYCLOAK` com `TYPE=UPDATE` desabilitando o usuário no Keycloak (`enabled=false`)
- Diferença de UC-040: bloqueio é ação de segurança pontual, inativação é ação administrativa

**Relaciona com:** UC-064, UC-040

---

### UC-064 — Desbloquear login ⚡

**Endpoint:** `PUT /v1/logins/{id}/unblock`
**Permissão:** `UPDATE_LOGIN_STATUS`
**Cache:** invalida `SCOS_ORGANIZATION_LOGIN`, `SCOS_ORGANIZATION_LOGIN_INFO`

**Descrição:** Restaura o acesso de um login `BLOCKED`. O login volta ao status `ACTIVE` e o usuário é reabilitado no Keycloak via Saga.

**Regras de negócio:**
- Só pode desbloquear login com status `BLOCKED` — retorna `422` para qualquer outro status
- O funcionário vinculado deve estar com status `ACTIVE`
- A empresa vinculada deve estar com status `ACTIVE`
- Dispara `SCOS_INTEGRATION_KEYCLOAK` com `TYPE=UPDATE` reabilitando o usuário no Keycloak (`enabled=true`)

**Relaciona com:** UC-063

---

### UC-065 — Atribuir perfil ao login

**Endpoint:** `PUT /v1/logins/{id}/profile/{profileId}`
**Permissão:** `UPDATE_LOGIN`
**Cache:** invalida `SCOS_ORGANIZATION_LOGIN`, `SCOS_ORGANIZATION_LOGIN_INFO`

**Descrição:** Substitui o perfil atual do login. As novas permissões entram em vigor na próxima consulta ao `/me` ou validação via `scos-registry` — o token atual não é invalidado.

**Regras de negócio:**
- O novo perfil deve estar com status `ACTIVE` — retorna `422` caso contrário
- O login deve estar com status `ACTIVE` ou `INACTIVE` — retorna `422` se `BLOCKED` ou `PENDING`
- O perfil anterior é substituído completamente — não é possível ter múltiplos perfis

**Relaciona com:** UC-068, UC-075

---

### UC-066 — Excluir login ⚡

**Endpoint:** `DELETE /v1/logins/{id}`
**Permissão:** `DELETE_LOGIN`
**Cache:** invalida `SCOS_ORGANIZATION_LOGIN`, `SCOS_ORGANIZATION_LOGIN_INFO`

**Descrição:** Realiza a exclusão definitiva de um login. Dispara remoção do usuário no Keycloak via Saga.

**Regras de negócio:**
- Só pode excluir login com status `INACTIVE` ou `BLOCKED` — retorna `422` se `ACTIVE` ou `PENDING`
- Dispara `SCOS_INTEGRATION_KEYCLOAK` com `TYPE=DELETE`
- Após exclusão bem-sucedida o `KEYCLOAK_ID` é invalidado
- Se a Saga falhar após `MAX_RETRIES`, a operação fica com status `ERROR` e pode ser reprocessada via UC-083

**Relaciona com:** UC-063, UC-040

---

## Profile API — Fases 4 e 5

---

### UC-067 — Criar perfil

**Endpoint:** `POST /v1/profiles`
**Permissão:** `CREATE_PROFILE`
**Cache:** invalida `SCOS_ORGANIZATION_PROFILE`
**Idempotência:** `SCOS_ORGANIZATION_IDP_PROFILE`

**Descrição:** Cria um novo perfil de acesso. Um perfil recém criado não possui recursos atribuídos — use UC-075. Sem recursos, o perfil não concede nenhum acesso.

**Regras de negócio:**
- O código do perfil deve ser único — retorna `409` se duplicado
- Criado com status `ACTIVE` por padrão

**Relaciona com:** UC-075

---

### UC-068 — Listar perfis 📄

**Endpoint:** `GET /v1/profiles`
**Permissão:** `GET_PROFILE`
**Cache:** `SCOS_ORGANIZATION_PROFILE`

**Descrição:** Lista todos os perfis com paginação. Usado para seleção ao criar ou atualizar logins.

**Regras de negócio:**
- Paginação obrigatória
- Filtros disponíveis via query params: `active`

---

### UC-069 — Buscar perfil por ID

**Endpoint:** `GET /v1/profiles/{id}`
**Permissão:** `GET_PROFILE`
**Cache:** `SCOS_ORGANIZATION_PROFILE`

**Descrição:** Retorna os dados completos de um perfil incluindo a lista de recursos atribuídos e a quantidade de logins que possuem este perfil.

**Regras de negócio:**
- Retorna `404` se o perfil não existir

---

### UC-070 — Atualizar perfil

**Endpoint:** `PUT /v1/profiles/{id}`
**Permissão:** `UPDATE_PROFILE`
**Cache:** invalida `SCOS_ORGANIZATION_PROFILE`
**Idempotência:** `SCOS_ORGANIZATION_IDP_PROFILE`

**Descrição:** Atualiza o código ou descrição de um perfil. Para os recursos use UC-075.

**Regras de negócio:**
- O código atualizado deve continuar único — retorna `409` se em conflito
- Não é possível atualizar perfil `DISABLED`

**Relaciona com:** UC-069

---

### UC-071 — Ativar perfil

**Endpoint:** `PUT /v1/profiles/{id}/enable`
**Permissão:** `ENABLE_PROFILE`
**Cache:** invalida `SCOS_ORGANIZATION_PROFILE`

**Descrição:** Reativa um perfil com status `INACTIVE`. Volta a estar disponível para atribuição.

**Regras de negócio:**
- Só pode ativar perfil com status `INACTIVE` — retorna `422` para qualquer outro status

**Relaciona com:** UC-072

---

### UC-072 — Inativar perfil

**Endpoint:** `PUT /v1/profiles/{id}/disable`
**Permissão:** `DISABLE_PROFILE`
**Cache:** invalida `SCOS_ORGANIZATION_PROFILE`

**Descrição:** Inativa um perfil. Logins que já possuem o perfil não são afetados — a inativação impede apenas novas atribuições.

**Regras de negócio:**
- Só pode inativar perfil com status `ACTIVE` — retorna `422` para qualquer outro status
- Não remove automaticamente o perfil dos logins existentes

**Relaciona com:** UC-071

---

### UC-073 — Excluir perfil

**Endpoint:** `DELETE /v1/profiles/{id}`
**Permissão:** `DELETE_PROFILE`
**Cache:** invalida `SCOS_ORGANIZATION_PROFILE`

**Descrição:** Realiza a exclusão lógica do perfil — registro permanece com status `DISABLED`.

**Regras de negócio:**
- Só pode excluir perfil com status `INACTIVE`
- Não é possível excluir perfil atribuído a logins com status `ACTIVE`, `INACTIVE` ou `BLOCKED`

**Relaciona com:** UC-072

---

### UC-074 — Listar recursos do perfil 📄 *(Fase 5)*

**Endpoint:** `GET /v1/profiles/{id}/resources`
**Permissão:** `GET_PROFILE`
**Cache:** `SCOS_ORGANIZATION_PROFILE`

**Descrição:** Lista todos os recursos atribuídos a um perfil com paginação. Usado para auditoria de permissões e revisão de acessos.

**Regras de negócio:**
- Paginação obrigatória
- Retorna `404` se o perfil não existir

**Relaciona com:** UC-069, UC-076

---

### UC-075 — Atualizar recursos do perfil *(Fase 5)*

**Endpoint:** `PUT /v1/profiles/{id}/resources`
**Permissão:** `UPDATE_PROFILE`
**Cache:** invalida `SCOS_ORGANIZATION_PROFILE`, `SCOS_ORGANIZATION_LOGIN_INFO`

**Descrição:** Substitui completamente a lista de recursos de um perfil. A operação é idempotente por natureza. Logins com o perfil refletem as novas permissões na próxima validação — sem novo login.

**Regras de negócio:**
- Todos os `resourceId` devem existir e estar com status `ACTIVE` — retorna `422` caso contrário
- A lista pode ser enviada vazia — remove todas as permissões do perfil
- O perfil deve estar com status `ACTIVE`

**Relaciona com:** UC-069, UC-074, UC-076

---

## Resource API — Fase 5

---

### UC-076 — Listar recursos disponíveis 📄

**Endpoint:** `GET /v1/resources`
**Permissão:** `GET_RESOURCE`
**Cache:** `SCOS_ORGANIZATION_RESOURCE`

**Descrição:** Lista todos os recursos cadastrados nos sistemas externos com paginação. Pode ser filtrado por `systemId`. Usado na configuração de perfis.

**Regras de negócio:**
- Paginação obrigatória
- Filtros disponíveis via query params: `systemId`, `active`
- Endpoint somente leitura — o cadastro de recursos é feito pelos sistemas externos via `scos-registry` (gRPC)

**Relaciona com:** UC-075

---

## Configuration API — Fase 1

---

### UC-077 — Listar configurações 📄

**Endpoint:** `GET /v1/configurations`
**Permissão:** `GET_CONFIGURATION`
**Cache:** `SCOS_ORGANIZATION_CONFIGURATION`

**Descrição:** Lista todas as configurações do sistema com identificadores, valores e tipos.

**Regras de negócio:**
- Paginação obrigatória
- Configurações marcadas como sensíveis devem ter seus valores mascarados

---

### UC-078 — Buscar configuração por ID

**Endpoint:** `GET /v1/configurations/{id}`
**Permissão:** `GET_CONFIGURATION`
**Cache:** `SCOS_ORGANIZATION_CONFIGURATION`

**Descrição:** Retorna uma configuração pelo identificador textual — ex: `TOKEN_EXPIRY_MINUTES`.

**Regras de negócio:**
- Retorna `404` se o identificador não existir
- Configurações sensíveis devem ter seus valores mascarados

**Relaciona com:** UC-079

---

### UC-079 — Atualizar configuração

**Endpoint:** `PUT /v1/configurations/{id}`
**Permissão:** `UPDATE_CONFIGURATION`
**Cache:** invalida `SCOS_ORGANIZATION_CONFIGURATION`

**Descrição:** Atualiza o valor de uma configuração existente em tempo real, sem reimplantação.

**Regras de negócio:**
- A configuração deve existir — não é possível criar via API, retorna `404`
- O valor deve ser compatível com o tipo declarado: `STRING`, `INTEGER`, `BOOLEAN`, `JSON` — retorna `422` se incompatível
- Configurações críticas devem exigir dupla autorização a nível de aplicação

**Relaciona com:** UC-078

---

## Integration API — Fase 4 *(nova)*

> APIs operacionais para o administrador acompanhar e agir sobre a fila de integração com o Keycloak (Outbox + dead letter). Sem essas APIs, operações com status `ERROR` ficariam órfãs — sem visibilidade e sem caminho de recuperação.

---

### UC-081 — Listar integrações Keycloak 📄 *(novo)*

**Endpoint:** `GET /v1/integrations/keycloak`
**Permissão:** `GET_INTEGRATION`
**Cache:** sem cache — dados operacionais em tempo real

**Descrição:** Lista as operações da fila de integração (Outbox) com paginação. Painel operacional para acompanhar o que está pendente, em processamento, concluído ou com erro.

**Regras de negócio:**
- Paginação obrigatória
- Filtros disponíveis via query params: `status`, `type`, `startDate`, `endDate`
- O campo `REQUEST` (payload) só é retornado na consulta individual (UC-082), não na listagem

**Relaciona com:** UC-082, UC-083

---

### UC-082 — Buscar integração Keycloak por ID *(novo)*

**Endpoint:** `GET /v1/integrations/keycloak/{id}`
**Permissão:** `GET_INTEGRATION`
**Cache:** sem cache — dados operacionais em tempo real

**Descrição:** Retorna os detalhes de uma operação de integração incluindo o payload enviado e o histórico completo de tentativas (`SCOS_INTEGRATION_KEYCLOAK_LOG`) ordenado da mais recente para a mais antiga.

**Regras de negócio:**
- Retorna `404` se a operação não existir
- Dados sensíveis do payload devem ser mascarados na resposta

**Relaciona com:** UC-081, UC-083

---

### UC-083 — Reprocessar integração com erro ⚡ *(novo)*

**Endpoint:** `POST /v1/integrations/keycloak/{id}/retry`
**Permissão:** `RETRY_INTEGRATION`
**Cache:** sem cache — dados operacionais em tempo real

**Descrição:** Reenfileira uma operação que esgotou as tentativas automáticas. A operação volta ao status `PENDING` e é reprocessada respeitando a idempotência — se o `KEYCLOAK_ID` já existe, a chamada ao Keycloak é pulada e apenas o passo local pendente é concluído.

**Regras de negócio:**
- Só pode reprocessar operação com status `ERROR` — retorna `422` para qualquer outro status
- Zera o `RETRY_COUNT` e retorna o status para `PENDING`
- Registra no log quem solicitou o reprocessamento (`USER_AT`)
- O processador verifica o `KEYCLOAK_ID` antes de chamar o Keycloak — garantia de idempotência

**Relaciona com:** UC-081, UC-082, UC-066

---

### UC-084 — Listar mensagens inválidas 📄 *(novo)*

**Endpoint:** `GET /v1/integrations/messages-invalid`
**Permissão:** `GET_INTEGRATION`
**Cache:** sem cache — dados operacionais em tempo real

**Descrição:** Consulta a fila de mensagens mortas (dead letter) — mensagens que chegaram ao sistema mas não puderam ser processadas por dados inválidos ou formato incorreto. Usado para análise de falhas e correção manual.

**Regras de negócio:**
- Paginação obrigatória
- Filtros disponíveis via query params: `startDate`, `endDate`

**Relaciona com:** UC-081

---

## Resumo dos Use Cases

| API | Fase | Use Cases | Paginados | Assíncronos |
|---|---|---|---|---|
| Configuration | 1 | UC-077 a UC-079 | 1 | 0 |
| Department | 1 | UC-021 a UC-027 | 1 | 0 |
| Position | 1 | UC-028 a UC-034 | 1 | 0 |
| Company | 2 | UC-001 a UC-020 | 4 | 0 |
| Employee | 3 | UC-035 a 053, UC-080 | 4 | 2 |
| Login | 4 | UC-054 a UC-066 | 2 | 6 |
| Integration | 4 | UC-081 a UC-084 | 2 | 1 |
| Profile | 4 e 5 | UC-067 a UC-075 | 2 | 0 |
| Resource | 5 | UC-076 | 1 | 0 |
| **Total** | — | **84** | **18** | **9** |

## Catálogo de Permissões (recursos do SCOS no scos-registry)

```
Company:           GET_COMPANY, CREATE_COMPANY, UPDATE_COMPANY, DELETE_COMPANY, ENABLE_COMPANY, DISABLE_COMPANY
Company Contact:   GET_COMPANY_CONTACT, CREATE_COMPANY_CONTACT, UPDATE_COMPANY_CONTACT, DELETE_COMPANY_CONTACT
Company Address:   GET_COMPANY_ADDRESS, CREATE_COMPANY_ADDRESS, UPDATE_COMPANY_ADDRESS, DELETE_COMPANY_ADDRESS
Department:        GET_DEPARTMENT, CREATE_DEPARTMENT, UPDATE_DEPARTMENT, DELETE_DEPARTMENT, ENABLE_DEPARTMENT, DISABLE_DEPARTMENT
Position:          GET_POSITION, CREATE_POSITION, UPDATE_POSITION, DELETE_POSITION, ENABLE_POSITION, DISABLE_POSITION
Employee:          GET_EMPLOYEE, CREATE_EMPLOYEE, UPDATE_EMPLOYEE, DELETE_EMPLOYEE, ENABLE_EMPLOYEE, DISABLE_EMPLOYEE, TRANSFER_EMPLOYEE
Employee Contact:  GET_EMPLOYEE_CONTACT, CREATE_EMPLOYEE_CONTACT, UPDATE_EMPLOYEE_CONTACT, DELETE_EMPLOYEE_CONTACT
Employee Address:  GET_EMPLOYEE_ADDRESS, CREATE_EMPLOYEE_ADDRESS, UPDATE_EMPLOYEE_ADDRESS, DELETE_EMPLOYEE_ADDRESS
Login:             GET_LOGIN, GET_LOGIN_INFO, CREATE_LOGIN, UPDATE_LOGIN, UPDATE_LOGIN_STATUS, DELETE_LOGIN
Profile:           GET_PROFILE, CREATE_PROFILE, UPDATE_PROFILE, DELETE_PROFILE, ENABLE_PROFILE, DISABLE_PROFILE
Resource:          GET_RESOURCE
Configuration:     GET_CONFIGURATION, UPDATE_CONFIGURATION
Integration:       GET_INTEGRATION, RETRY_INTEGRATION
```

## Catálogo de Caches

| Cache | Lido por | Invalidado por |
|---|---|---|
| SCOS_ORGANIZATION_COMPANY | UC-003, 004, 009, 010 | UC-001, 002, 005 a 008, 012, 014, 015, 017, 019, 020 |
| SCOS_ORGANIZATION_COMPANY_CONTACT | UC-011, 013 | UC-012, 014, 015 |
| SCOS_ORGANIZATION_COMPANY_ADDRESS | UC-016, 018 | UC-017, 019, 020 |
| SCOS_ORGANIZATION_DEPARTMENT | UC-022, 023 | UC-021, 024 a 027 |
| SCOS_ORGANIZATION_POSITION | UC-029, 030 | UC-028, 031 a 034 |
| SCOS_ORGANIZATION_EMPLOYEE | UC-036, 037, 042, 080 | UC-035, 038 a 041, 043, 045, 047, 048, 050, 052, 053 |
| SCOS_ORGANIZATION_EMPLOYEE_CONTACT | UC-044, 046 | UC-045, 047, 048 |
| SCOS_ORGANIZATION_EMPLOYEE_ADDRESS | UC-049, 051 | UC-050, 052, 053 |
| SCOS_ORGANIZATION_LOGIN | UC-055, 056, 058, 060, 061 | UC-040, 054, 057, 062 a 066 |
| SCOS_ORGANIZATION_LOGIN_INFO | UC-059 | UC-040, 041, 062 a 066, 075 |
| SCOS_ORGANIZATION_PROFILE | UC-068, 069, 074 | UC-067, 070 a 073, 075 |
| SCOS_ORGANIZATION_RESOURCE | UC-076 | scos-registry (gRPC) |
| SCOS_ORGANIZATION_CONFIGURATION | UC-077, 078 | UC-079 |

> **Nota:** os endpoints da Integration API (UC-081 a UC-084) não utilizam cache — são dados operacionais que mudam constantemente e exigem leitura em tempo real.
