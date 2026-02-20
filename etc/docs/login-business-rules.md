# Regras de Negócio — Login & Profile API

Fonte: `etc/api/organization/ScosOrganization_Login.yml`

---

## 🎯 Visão Estratégica
Este documento define a visão, regras de negócio, contrato de API e guia de implementação para o **domínio Login & Profile** — responsável por gerenciar credenciais, autenticação, autorização e sincronização com IdP (Keycloak).

**Escopo**: `Login`, `Profile` e `Features` com suas operações CRUD, validações de segurança, gerenciamento de estado, mudanças de senha e sincronização Keycloak.

**Decisões arquiteturais críticas**:
- **Unicidade**: Username/login é único no sistema
- **Autenticação Centralizada**: Keycloak como IdP (Identity Provider)
- **Política de Senha**: Requisitos mínimos (tamanho, complexidade), histórico, expiração
- **Bloqueio de Conta**: Invalidação de sessões ativas ao bloquear
- **Auditoria Completa**: Todas as operações geram eventos + trilha de auditoria

---

## Quick Reference
- **Endpoints principais**: `POST /v1/logins` (201), `PUT /v1/logins/{id}` (204), `GET /v1/logins/{id}` (200), `DELETE /v1/logins/{id}` (204), `PUT /v1/logins/{id}/block` (204), `PUT /v1/logins/{id}/password` (204)
- **Regras críticas**: 
  - Username único no sistema → erro `SCOS_LOGIN_002` (409)
  - Senha com política mínima (mín 12 chars; pelo menos 1 maiúscula, 1 minúscula, 1 número e 1 símbolo; proibir substrings do username/email; checar lista de senhas comprometidas; rejeitar sequências/repetições óbvias) → `SCOS-010` (400)
  - Status não pode ser DELETED via PUT (apenas DELETE endpoint)
  - Bloqueio invalida sessões ativas no Keycloak
- **Artifacts chave**: 
  - Contrato: `etc/api/organization/ScosOrganization_Login.yml`
  - Modelo: `scos-organization-domain/.../login/Login.java`
  - UseCases: `scos-organization-application/.../usecase/login/`
  - Integration: `scos-organization-infrastructure/.../keycloak/KeycloakPort.java`

---

## 1. Objetivo
Este documento serve como **especificação técnica** e **guia de implementação** para o domínio Login & Profile. Define o contrato de API (OpenAPI), regras de negócio, validações em cada camada, sincronização com Keycloak e diretrizes para testes automatizados.

## 2. Entidades e Conceitos

### 2.1 Login (Entidade Raiz)
Representa uma credencial de autenticação associada a um Employee.

| Campo | Tipo | Constraints | Descrição |
|-------|------|-------------|----------|
| `id` | Long (PK) | AUTO_INCREMENT | Identificador único |
| `username` | String | NOT NULL, UNIQUE | Login/username (3-100 chars, alfanumérico + underscore) — **único e imutável após criação** |
| `password` | String (hash) | NOT NULL | Senha hasheada (BCrypt, PBKDF2, etc.) |
| `email` | String | NOT NULL, email format | Email de recuperação (único) |
| `profileId` | Long (FK) | NOT NULL | Referência a `Profile` |
| `employeeId` | Long (FK) | NOT NULL | Referência a `Employee` (mutuamente exclusivo com Company logins) |
| `status` | Enum | NOT NULL, default='PENDING' | `PENDING`, `ACTIVE`, `BLOCKED`, `INACTIVE`, `DELETED` |
| `lastPasswordChangeAt` | Timestamp | NULLABLE | Última mudança de senha |
| `lastLoginAt` | Timestamp | NULLABLE | Último login bem-sucedido |
| `failedLoginAttempts` | Integer | NOT NULL, default=0 | Contador de tentativas falhadas |
| `blockedUntil` | Timestamp | NULLABLE | Até quando está temporariamente bloqueado |
| `keycloakUserId` | String (UUID) | NULLABLE | ID do usuário no Keycloak (sincronização) |
| `createdAt` | Timestamp | NOT NULL, AUTO | Data/hora de criação |
| `updatedAt` | Timestamp | NOT NULL, AUTO | Data/hora de última alteração |
| `createdBy` | String | NOT NULL | Usuário que criou |
| `updatedBy` | String | NULL | Usuário que atualizou |

**Relacionamentos**:
- **N:1 com Profile**: Múltiplos Logins podem ter mesmo Profile
- **1:1 com Employee**: Cada Login está vinculado a exatamente um Employee

### 2.2 Profile (Entidade de Autorização)
Representa um perfil de acesso (função, role) que agrega permissions.

| Campo | Tipo | Constraints | Descrição |
|-------|------|-------------|----------|
| `id` | Long (PK) | AUTO_INCREMENT | |
| `code` | String | NOT NULL, UNIQUE globally | Código do perfil (ex: ADMIN, MANAGER, USER) |
| `name` | String | NOT NULL | Nome legível |
| `description` | String | NULLABLE, max 500 | Descrição do perfil |
| `features` | JSON/Array | NOT NULL | Lista de features/permissões habilitadas |
| `keycloakRoleId` | String (UUID) | NULLABLE | ID correspondente no Keycloak |
| `active` | Boolean | NOT NULL, default=true | Perfil ativo/inativo |
| `createdAt` | Timestamp | NOT NULL | |
| `updatedAt` | Timestamp | NOT NULL | |

### 2.3 Features (Enum/Catalog)
Catálogo de features/permissões disponíveis no sistema.

Exemplo:
```
- CREATE_COMPANY
- UPDATE_COMPANY
- DELETE_COMPANY
- CREATE_EMPLOYEE
- MANAGE_LOGINS
- MANAGE_PROFILES
- VIEW_REPORTS
- EXPORT_DATA
```

---

## 3. Contrato OpenAPI — Pontos Essenciais
- **Spec**: `etc/api/organization/ScosOrganization_Login.yml` (schemas, `x-*`, exemplos)
- **Extensões relevantes**: 
  - `x-authorize`: controle de acesso baseado em permissões
  - `x-required-message`: mensagem customizada para campos obrigatórios
  - `x-jdempotentresource`: marca endpoints como idempotentes
  - `x-password-policy`: política de força de senha

---

## 4. Regras de Negócio Base

- **Escopo**: `Login` é um recurso no escopo do sistema; `Profile` é global
- **Unicidade**:
  - `username` — único no sistema → erro `SCOS_LOGIN_002` (409)
  - `email` — único no sistema → erro `SCOS_LOGIN_003` (409)
  - `Profile.code` — único globalmente → erro `SCOS_PROFILE_002` (409)
- **Ciclo de vida de Login**:
  - `PENDING` (criado, aguarda primeira senha) → `ACTIVE` (credencial válida) → opcional: `BLOCKED` (por segurança) → `INACTIVE` (suspenso) → `DELETED` (soft-delete)
- **Política de Senha**:
  - Comprimento mínimo: **12** caracteres, máximo 128
  - Deve conter: pelo menos uma **maiúscula**, uma **minúscula**, um **número** e um **símbolo**
  - Proibir substrings do `username` e do `email` (case‑insensitive)
  - Rejeitar senhas com sequências óbvias (ex.: `1234`, `abcd`) ou caracteres repetidos (>2 consecutivos)
  - Verificar contra lista de senhas comprometidas / banned‑passwords (ex.: HaveIBeenPwned) e rejeitar se encontrada
  - (Opcional) aplicar verificação de entropia/score (ex.: zxcvbn >= 3)
  - Histórico: não permitir reutilizar as últimas **3** senhas
  - Expiração: 90 dias (sugerido)

  **Nota:** A política de senha é **configurável** e pode ser atualizada conforme necessidade. Alterações devem ser versionadas, testadas e comunicadas aos usuários — por padrão aplicam‑se **prospectivamente** (validadas para novas senhas e ao alterar a senha). Para imposições retroativas (ex.: obrigar troca imediata), fornecer um processo/endpoint administrativo de rotação forçada e um plano de comunicação.

- **Bloqueio de Conta**:
  - Após 5 tentativas falhas, bloquear por 15 minutos
  - Admin pode bloquear manualmente → `status = BLOCKED`, sessões ativas invalidadas
- **Sincronização Keycloak**:
  - Criar Login → criar usuário + atribuir grupo/roles em Keycloak
  - Atualizar perfil → sincronizar roles
  - Bloquear → desabilitar usuário em Keycloak
  - Deletar → remover do Keycloak (soft-delete)
  - **Employee deleted**: quando um Employee for excluído (soft-delete), o(s) Login(s) associados devem ser marcados como `DELETED` e sincronizados com Keycloak conforme a política de Login (desabilitar/remover usuário).
- **Auditoria**:
  - Cada tentativa de login (sucesso/falha) registrada
  - Mudanças de senha, perfil, status geram eventos

---

## 5. Validações e Mensagens Detalhadas

- `username`: obrigatório (SCOS-003), 3-100 chars (SCOS-001/SCOS-004), formato `^[a-zA-Z0-9_.-]{3,100}$` (SCOS-010), unicidade (SCOS_LOGIN_002)
- `email`: obrigatório (SCOS-003), formato email válido (SCOS-010), unicidade (SCOS_LOGIN_003)
- `password`: obrigatório na criação (SCOS-003); **política de força estrita**: mínimo 12 caracteres; pelo menos 1 maiúscula, 1 minúscula, 1 número e 1 símbolo; proibir substrings do `username`/`email`; rejeitar sequências/repetições óbvias; checar banned‑passwords; histórico de **3** últimas (SCOS_LOGIN_004); expiração sugerida: 90 dias. Violação → `SCOS-010`.
- `profileId`: obrigatório (SCOS-003), deve existir (SCOS-012), não pode ser deletado se tem Logins (SCOS_PROFILE_003)
- `status`: enum permitido (`PENDING`, `ACTIVE`, `BLOCKED`, `INACTIVE`, `DELETED`)

| Códigoação | HTTP | Significado |
|--------|------|------------|
| SCOS-001 | 400 | Campo vazio |
| SCOS-003 | 400 | Campo obrigatório |
| SCOS-004 | 400 | Valor acima do máximo |
| SCOS-010 | 400 | Formato inválido (senha fraca, username, email) |
| SCOS-012 | 404 | Recurso referenciado não encontrado |
| SCOS_LOGIN_001 | 404 | Login não encontrado |
| SCOS_LOGIN_002 | 409 | Username duplicado |
| SCOS_LOGIN_003 | 409 | Email duplicado |
| SCOS_LOGIN_004 | 400 | Senha foi usada recentemente (histórico) |
| SCOS_LOGIN_005 | 429 | Muitas tentativas de login (rate limiting) |
| SCOS_PROFILE_001 | 404 | Profile não encontrado |
| SCOS_PROFILE_002 | 409 | Código de profile duplicado |
| SCOS_PROFILE_003 | 409 | Profile tem logins associados (não pode deletar) |

---

## 6. Use Cases — Especificação Detalhada

### Login — UseCases

#### UseCase: CreateLoginUseCase (POST `/v1/logins`)

**Verbo HTTP**: `POST`
**Endpoint**: `/v1/logins`
**Autorização**: `x-authorize: CREATE_LOGIN`

**Descrição**:
Cria um novo Login para um Employee. O username e email devem ser únicos no sistema. Status inicial é `PENDING` (aguarda confirmação/primeira senha).

**Request Headers**:
```
Content-Type: application/json
Authorization: Bearer {token}
```

**Request Body (JSON)**:
```json
{
  "username": "string (3-100 chars, required)",
  "email": "string (email format, required)",
  "profileId": "number (required, must exist)",
  "employeeId": "number (required, must exist)",
  "password": "string (min 8, with policy, required)",
  "sendWelcomeEmail": "boolean (optional, default: true)"
}
```

**Validações de Entrada (Field-Level)**:
- `username`: obrigatório (SCOS-003), 3-100 chars (SCOS-001/SCOS-004), alfanumérico + _.- (SCOS-010)
- `email`: obrigatório (SCOS-003), formato email (SCOS-010)
- `password`: obrigatório (SCOS-003); **política de força estrita**: mínimo 12 caracteres; pelo menos 1 maiúscula, 1 minúscula, 1 número e 1 símbolo; proibir substrings do `username`/`email`; rejeitar sequências/repetições óbvias; checar lista de senhas comprometidas. Violação → `SCOS-010`.
- `profileId`: obrigatório (SCOS-003), tipo Long
- `employeeId`: obrigatório (SCOS-003), tipo Long

**Validações de Domínio (Business Rules)**:
- ✅ **Unicidade username**: `username` não existe no sistema → `SCOS_LOGIN_002` (409)
- ✅ **Unicidade email**: `email` não existe no sistema → `SCOS_LOGIN_003` (409)
- ✅ **Profile válido**: Profile com `profileId` existe e está `active` → `SCOS_PROFILE_001` (404)
- ✅ **Employee válido**: Employee com `employeeId` existe → `SCOS-012` (404)
- ✅ **Employee sem Login**: Employee não pode ter múltiplos Logins → erro customizado (400)
- ✅ **Histórico senha**: validar que password não está no histórico das últimas **3** senhas → `SCOS_LOGIN_004` (400)

**Fluxo de Execução**:
1. **Validar entrada** (field-level)
2. **Validar domínio** (unicidade, existência, política)
3. **Hashear senha** (BCrypt, PBKDF2)
4. **Persistir** em `SCOS_LOGIN` com `status = PENDING`
5. **Emitir evento Kafka**: `login.created` — um consumidor responsável fará a sincronização com Keycloak (criar usuário, atribuir roles)
6. **Opcionalmente enviar email**: bem-vindo com instruções
7. **Retornar** 201 Created + Location header

**Resposta de Sucesso (201 Created)**:
```json
{
  "data": {
    "id": 500,
    "username": "jdoe",
    "email": "jdoe@company.com",
    "profileId": 10,
    "employeeId": 100,
    "status": "PENDING",
    "keycloakUserId": "550e8400-e29b-41d4-a716-446655440000",
    "createdAt": "2026-02-17T10:30:00Z",
    "createdBy": "admin@org.com"
  }
}
```

**Respostas de Erro**:

| HTTP | Código | Cenário | Exemplo |
|------|--------|---------|---------|
| 400 | SCOS-003 | Campo obrigatório | `{ "data": { "message": "Field required: username", "codeError": "SCOS-003" } }` |
| 400 | SCOS-010 | Senha fraca | `{ "data": { "message": "Password must contain uppercase, lowercase, number, symbol", "codeError": "SCOS-010" } }` |
| 400 | SCOS_LOGIN_004 | Senha usada recentemente | `{ "data": { "message": "Password was used recently", "codeError": "SCOS_LOGIN_004" } }` |
| 404 | SCOS_PROFILE_001 | Profile não existe | `{ "data": { "message": "Profile not found", "codeError": "SCOS_PROFILE_001" } }` |
| 409 | SCOS_LOGIN_002 | Username duplicado | `{ "data": { "message": "Username already exists", "codeError": "SCOS_LOGIN_002" } }` |
| 409 | SCOS_LOGIN_003 | Email duplicado | `{ "data": { "message": "Email already exists", "codeError": "SCOS_LOGIN_003" } }` |

**Efeitos Colaterais**:
- ✅ Evento `login.created` emitido (Kafka) — Keycloak será sincronizado por um consumer (criação/atribuição de roles)
- ✅ Auditoria registrada
- ✅ Email de boas-vindas enviado (se `sendWelcomeEmail=true`) 

---

#### UseCase: UpdateLoginUseCase (PUT `/v1/logins/{id}`)

**Verbo HTTP**: `PUT`
**Endpoint**: `/v1/logins/{id}`
**Autorização**: `x-authorize: UPDATE_LOGIN`

**Descrição**:
Atualiza informações não-sensíveis de um Login (perfil, email). Senha é alterada via endpoint separado. Username **não pode ser alterado**.

**Request Body (JSON)**:
```json
{
  "email": "newemail@company.com (optional)",
  "profileId": "number (optional, must exist)",
  "status": "enum (PENDING|ACTIVE|BLOCKED|INACTIVE, optional)"
}
```

**Validações de Entrada**:
- Mesmas do Create, exceto `username` e `password` (não permitidos neste endpoint)

**Validações de Domínio**:
- ✅ **Login existe**: `id` deve referenciar um Login válido → `SCOS_LOGIN_001` (404)
- ✅ **Email único**: se alterado, verificar unicidade → `SCOS_LOGIN_003` (409)
- ✅ **Profile válido**: se alterado, verificar existência e se está ativo → `SCOS_PROFILE_001` (404)
- ✅ **Status válido**: transição permitida conforme máquina de estados

**Fluxo**:
1. Validar entrada
2. Buscar Login por ID → 404 se não existe
3. Validar domínio
4. Atualizar campos em BD
5. Atualizar campos em BD (perfil, email) — **NÃO** emitir evento Kafka nem sincronizar com Keycloak.
6. (Observação) Alterações de `status` devem ser feitas via endpoints/UseCases específicos que emitem eventos (ex.: `/v1/logins/{id}/block`, `PUT /v1/logins/{id}/status`).
7. Retornar 204 No Content

**Resposta de Sucesso (204 No Content)**

**Efeitos Colaterais**:
- ✅ Atualização persistida em BD e auditada
- ⚠️ **Não emitir evento Kafka nem sincronizar Keycloak para updates genéricos (perfil/email)**

---

#### UseCase: GetLoginUseCase (GET `/v1/logins/{id}`)

**Verbo HTTP**: `GET`
**Endpoint**: `/v1/logins/{id}`
**Autorização**: `x-authorize: READ_LOGIN`

**Descrição**:
Retorna detalhes de um Login (sem senha).

**Resposta de Sucesso (200 OK)**:
```json
{
  "data": {
    "id": 500,
    "username": "jdoe",
    "email": "jdoe@company.com",
    "profileId": 10,
    "employeeId": 100,
    "status": "ACTIVE",
    "lastPasswordChangeAt": "2026-02-10T10:00:00Z",
    "lastLoginAt": "2026-02-17T09:15:00Z",
    "failedLoginAttempts": 0,
    "createdAt": "2026-02-17T10:30:00Z",
    "createdBy": "admin@org.com"
  }
}
```

**Resposta de Erro**:
- 404 `SCOS_LOGIN_001`: Login não encontrado

---

#### UseCase: BlockLoginUseCase (PUT `/v1/logins/{id}/block`)

**Verbo HTTP**: `PUT`
**Endpoint**: `/v1/logins/{id}/block`
**Autorização**: `x-authorize: BLOCK_LOGIN`

**Descrição**:
Bloqueia um Login (status = BLOCKED). Invalida sessões ativas.

**Request Body (JSON)** (opcional):
```json
{
  "reason": "string (optional, ex: Security incident, Too many attempts)",
  "unblockAt": "timestamp (optional, ex: 2026-02-18T10:00:00Z)"
}
```

**Validações**:
- Login existe → 404
- Status permite bloqueio (não se já deletado)

**Fluxo**:
1. Buscar Login
2. Validar
3. Atualizar `status = BLOCKED`, `blockedUntil` (se informado)
4. **Emitir evento Kafka** `login.blocked` — Keycloak será desabilitado por um consumer
5. Invalidar tokens ativos
6. Retornar 204 No Content

**Efeitos Colaterais**:
- ✅ Evento `login.blocked` emitido (Kafka) — Keycloak será desabilitado por consumer
- ✅ Sessões ativas terminadas

---

#### UseCase: ChangePasswordUseCase (PUT `/v1/logins/{id}/password`)

**Verbo HTTP**: `PUT`
**Endpoint**: `/v1/logins/{id}/password`
**Autorização**: `x-authorize: CHANGE_PASSWORD` (auto-service para próprio usuário, admin para outros)

**Descrição**:
Altera a senha de um Login. Valida política de força, histórico.

**Request Body (JSON)**:
```json
{
  "oldPassword": "string (required if self-service, omit if admin-force)",
  "newPassword": "string (required, must follow policy)",
  "requireChangeOnNextLogin": "boolean (optional, default: false)"
}
```

**Validações de Entrada**:
- `newPassword`: deve seguir **política de força estrita** (mín 12 caracteres; pelo menos 1 maiúscula, 1 minúscula, 1 número e 1 símbolo; proibir substrings do `username`/`email`; rejeitar sequências/repetições; checar banned‑passwords)

**Validações de Domínio**:
- ✅ **Login existe** → `SCOS_LOGIN_001` (404)
- ✅ **OldPassword correto** (se self-service): validar contra hash em BD → erro customizado (401/403)
- ✅ **Senha não usada**: verificar histórico das últimas **3** senhas → `SCOS_LOGIN_004` (400)
- ✅ **Diferentes**: new ≠ old

**Fluxo**:
1. Validar entrada
2. Buscar Login
3. Se self-service: validar `oldPassword`
4. Validar política + histórico
5. Hashear `newPassword`
6. Persistir em BD: atualizar `password`, `lastPasswordChangeAt`
7. **Emitir evento Kafka** `login.password.changed` — Keycloak será atualizado por um consumer
8. Se `requireChangeOnNextLogin=true`: marcar em BD
9. Invalidar tokens antigos (forçar novo login)
10. Retornar 204 No Content

**Resposta de Sucesso (204 No Content)**

**Respostas de Erro**:
- 400 `SCOS-010`: Senha fraca
- 400 `SCOS_LOGIN_004`: Senha usada recentemente
- 401/403: Senha antiga incorreta (self-service)
- 404 `SCOS_LOGIN_001`: Login não encontrado

**Efeitos Colaterais**:
- ✅ Evento `login.password.changed` emitido (Kafka) — Keycloak será atualizado por consumer
- ✅ Tokens antigos invalidados
- ✅ Auditoria registrada

---

#### UseCase: UnblockLoginUseCase (PUT `/v1/logins/{id}/unblock`)

**Verbo HTTP**: `PUT`
**Endpoint**: `/v1/logins/{id}/unblock`
**Autorização**: `x-authorize: MANAGE_LOGIN_STATUS`

**Descrição**:
Desbloqueia um Login (status BLOCKED → ACTIVE).

**Request Body**: vazio

**Fluxo**:
1. Buscar Login
2. Validar se está BLOCKED
3. Atualizar `status = ACTIVE`, `blockedUntil = NULL`, `failedLoginAttempts = 0`
4. **Emitir evento Kafka** `login.unblocked` — Keycloak será habilitado por consumer
5. Retornar 204 No Content

**Efeitos Colaterais**:
- ✅ Evento `login.unblocked` emitido (Kafka) — Keycloak será habilitado por consumer

---

#### UseCase: DeleteLoginUseCase (DELETE `/v1/logins/{id}`)

**Verbo HTTP**: `DELETE`
**Endpoint**: `/v1/logins/{id}`
**Autorização**: `x-authorize: DELETE_LOGIN`

**Descrição**:
Deleta um Login (soft-delete: status = DELETED).

**Pré-condições**:
- Login não associado a sessões ativas (ou terminá-las)

**Fluxo**:
1. Buscar Login
2. Terminar sessões ativas (se houver)
3. Soft-delete: `status = DELETED`
4. **Emitir evento Kafka** `login.deleted` — Keycloak será removido/desabilitado por consumer
5. Retornar 204 No Content

---

#### UseCase: ListLoginsUseCase (GET `/v1/logins`)

**Verbo HTTP**: `GET`
**Endpoint**: `/v1/logins`
**Query Parameters**:
```
?page=0&size=20&sort=username:asc
&status=ACTIVE&username=jdoe&profileId=10&employeeId=100
```

**Autorização**: `x-authorize: LIST_LOGIN`

**Resposta de Sucesso (200 OK)**:
```json
{
  "data": [
    {
      "id": 500,
      "username": "jdoe",
      "email": "jdoe@company.com",
      "profileId": 10,
      "status": "ACTIVE",
      "lastLoginAt": "2026-02-17T09:15:00Z"
    }
  ],
  "pagination": {
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8,
    "hasNext": true
  }
}
```

---

### Profile — UseCases

#### UseCase: CreateProfileUseCase (POST `/v1/profiles`)

**Endpoint**: `POST /v1/profiles`
**Autorização**: `x-authorize: MANAGE_PROFILES`

**Request Body**:
```json
{
  "code": "string (required, unique globally, ex: ADMIN, MANAGER)",
  "name": "string (required, 2-200 chars)",
  "description": "string (optional, max 500)",
  "features": ["array of feature codes", "CREATE_COMPANY", "UPDATE_EMPLOYEE"]
}
```

**Validações**:
- `code`: obrigatório, único globalmente, formato `^[A-Z0-9_]{2,50}$`
- `name`: obrigatório, 2-200 chars
- `features`: array de strings válidas (enum permitidas)

**Fluxo**:
1. Validar entrada
2. Verificar unicidade de `code` → `SCOS_PROFILE_002` (409)
3. Persistir em BD
4. Criar role em Keycloak (POST `/admin/realms/{realm}/roles`)
5. Emitir `profile.created`
6. Retornar 201 Created

---

#### UseCase: UpdateProfileUseCase (PUT `/v1/profiles/{id}`)

**Endpoint**: `PUT /v1/profiles/{id}`
**Autorização**: `x-authorize: MANAGE_PROFILES`

**Request Body**: mesmo do Create

**Validações**:
- Profile existe → `SCOS_PROFILE_001` (404)
- Código único (excluindo current) → `SCOS_PROFILE_002` (409)

**Fluxo**:
1. Buscar Profile
2. Validar
3. Persistir mudanças
4. Sincronizar com Keycloak (atualizar role)
5. Se features alteradas: propagar para todos os Logins com este Profile → re-sincronizar permissões
6. Emitir `profile.updated`
7. Retornar 204 No Content

---

#### UseCase: ListProfilesUseCase (GET `/v1/profiles`)

**Query Parameters**: `?page=0&size=20&active=true`

**Resposta (200 OK)**:
```json
{
  "data": [
    {
      "id": 10,
      "code": "ADMIN",
      "name": "Administrator",
      "description": "Full system access",
      "features": ["CREATE_COMPANY", "UPDATE_COMPANY", "DELETE_COMPANY", "MANAGE_LOGINS"],
      "active": true
    }
  ],
  "pagination": { "page": 0, "size": 20, "totalElements": 5 }
}
```

---

## 7. Exemplos JSON de Request / Response por UseCase

### Login — CreateLoginUseCase
**Request**:
```json
{
  "username": "jdoe",
  "email": "jdoe@company.com",
  "profileId": 10,
  "employeeId": 100,
  "password": "SecurePass123!@"
}
```

**Success (201)**:
```json
{ "data": { "id": 500, "username": "jdoe", "status": "PENDING" } }
```

**Validation Error (400)**:
```json
{
  "data": {
    "message": "Validation failed",
    "codeError": "SCOS-010",
    "validationErrors": [ { "attribute": "password", "message": "Password must contain uppercase, lowercase, number, symbol" } ]
  }
}
```

**Conflict (409) — Username duplicado**:
```json
{ "data": { "message": "Username already exists", "codeError": "SCOS_LOGIN_002" } }
```

---

## 8. Error-Code Mapping (Login / Profile)

| Código | Significado |
|--------|-----------|
| `SCOS_LOGIN_001` | Login não encontrado (GET/UPDATE/DELETE) |
| `SCOS_LOGIN_002` | Username duplicado |
| `SCOS_LOGIN_003` | Email duplicado |
| `SCOS_LOGIN_004` | Senha foi usada recentemente (histórico) |
| `SCOS_LOGIN_005` | Muitas tentativas de login (rate limiting) |
| `SCOS_PROFILE_001` | Profile não encontrado |
| `SCOS_PROFILE_002` | Código de profile duplicado |
| `SCOS_PROFILE_003` | Profile tem logins associados (não pode deletar) |

---

## 9. Arquitetura e Relacionamentos

### 9.1 Sincronização com Keycloak

```
CreateLoginUseCase
    ↓
[Domain validation]
    ↓
[Persist to DB: Login record]
    ↓
[Emit: login.created event]
    ↓
[Keycloak Adapter (Port)]
    ├─ POST /admin/realms/{realm}/users (criar usuário)
    ├─ Set temporary password
    └─ POST /admin/realms/{realm}/users/{userId}/groups/{groupId}
           (associar ao grupo de Profile)
    ↓
[Keycloak confirmação]
    ↓
[Update Login.keycloakUserId]
```

### 9.2 Fluxo de Autenticação (não coberto neste documento, mas referência)

```
Client
  ↓ POST /auth/login (username, password)
  ↓
[LoginService]
  ├─ Buscar Login por username
  ├─ Validar status (não BLOCKED/INACTIVE/DELETED)
  ├─ Validar senha (BCrypt compare)
  └─ Se válido: gerar JWT + enviar para Keycloak
  ↓
[Retornar token + refresh]
  ↓
[Atualizar lastLoginAt, reset failedLoginAttempts]
```

### 9.3 Máquina de Estados de Login

```
     ┌──────────────────────────────────────────────────────┐
     │ Início                                               │
     └──────────────────────────────────────────────────────┘
            ↓
         PENDING (criado, sem primeira senha)
            ↓
      [ChangePassword →]
            ↓
         ACTIVE ←────────────────────┐
            ↓                         │
      [Block →]                      │
            ↓                         │
         BLOCKED (temporário ou manual)
            ├─ Tempo expirado ou [Unblock →]
            ↓
         ACTIVE ────────→ (volta)   │
            ↓                         │
      [Disable →]                    │
            ↓                         │
         INACTIVE (suspenso)         │
            └─ [Enable →] ──────────┘
            ↓
         DELETED (soft-delete)
```

---

## 10. Invariantes de Negócio

1. **Username único**: Nunca há dois Logins com mesmo username no sistema
2. **Email único**: Nunca há dois Logins com mesmo email no sistema
3. **Profile válido**: Todo Login referencia um Profile ativo
4. **Employee válido**: Todo Login referencia um Employee existente
5. **Sincronização Keycloak**: A sincronização com Keycloak ocorre via eventos Kafka emitidos para operações específicas: **criação** (`login.created`), **alteração de status** (`login.blocked` / `login.unblocked`), **alteração de senha** (`login.password.changed`) e **exclusão** (`login.deleted`). Outras alterações (ex.: email, profile) **NÃO** emitem eventos nem sincronizam Keycloak.
6. **Bloqueio refletido**: Se Login status=BLOCKED, o sistema deve emitir evento Kafka para que o Keycloak seja desabilitado pelo consumer
7. **Política de senha**: Toda senha validada conforme requisitos mínimos
8. **Histórico senha**: Últimas **3** senhas não repetíveis no mesmo usuário

---

## 11. Árvore de Erros e Recuperação

```
CreateLoginUseCase
    │
    ├─ Validação de Entrada
    │   ├─ username vazio? ──→ 400 SCOS-003
    │   ├─ email inválido? ──→ 400 SCOS-010
    │   └─ senha fraca? ──→ 400 SCOS-010
    │
    ├─ Validação de Domínio
    │   ├─ username duplicado? ──→ 409 SCOS_LOGIN_002
    │   ├─ email duplicado? ──→ 409 SCOS_LOGIN_003
    │   ├─ profileId não existe? ──→ 404 SCOS_PROFILE_001
    │   └─ employeeId não existe? ──→ 404 SCOS-012
    │
    └─ Sucesso: 201 Created + Kafka event
```

| Erro | Status | Recuperação |
|------|--------|------------|
| SCOS-010 (senha fraca) | 400 | Usar senha com maiús., minús., número, símbolo (8+ chars) |
| SCOS_LOGIN_002 (username duplicado) | 409 | Escolher outro username |
| SCOS_LOGIN_003 (email duplicado) | 409 | Usar outro email ou recuperação de conta |
| SCOS_LOGIN_004 (senha recente) | 400 | Usar senha completamente nova |
| SCOS_LOGIN_005 (rate limit) | 429 | Aguardar antes de nova tentativa |

---

## 12. Implementação Recomendada

### Fase 1: Camada de Domínio (1-2 dias)
1. Criar `Login` entity com invariantes
   - File: `scos-organization-domain/.../login/Login.java`
   - Usar records (Java 17+)
   - Value object: `Username`, `Email`, `PasswordPolicy`

2. Criar `LoginDomainService` para validações
   - `validateUniqueUsername()`, `validateUniqueEmail()`
   - `validatePasswordPolicy()` — implementar validação estrita (mín 12 chars; proibir substrings username/email; checagem de banned‑passwords/HIBP; rejeitar sequências/repetições; aplicar zxcvbn/entropia opcional)
   - `validatePasswordHistory()` — manter histórico e proibir reutilização das últimas **3** senhas

3. Criar `Profile` entity
   - File: `scos-organization-domain/.../login/Profile.java`

4. Testes unitários
   - Invariantes de Login
   - Validações de domínio

### Fase 2: Camada de Aplicação (1-2 dias)
1. Criar UseCases (Command Handlers)
   - `CreateLoginUseCase`, `UpdateLoginUseCase`, `BlockLoginUseCase`
   - `ChangePasswordUseCase`, `UnblockLoginUseCase`, `DeleteLoginUseCase`
   - `CreateProfileUseCase`, `UpdateProfileUseCase`

2. Criar DTOs
   - `CreateLoginRequest`, `LoginResponse`, etc.
   - Expor política de senha via configuração (`application.yml` / feature flags) e painel administrativo; implementar endpoint administrativo para rotação/forçamento de reset; versionar políticas e registrar data de vigência; adicionar auditoria e métricas de conformidade.

3. Criar Event Producers

4. Testes de integração

### Fase 3: Integração Keycloak (1 dia)
1. Implementar `KeycloakPort` (interface de integração)
   - Criar usuário, atribuir grupo/roles, desabilitar, deletar

2. Implementar adaptador (Keycloak REST client)

3. Sincronização bidirecional

### Fase 4: Camada de Apresentação (1 dia)
1. Criar controllers REST
2. Mapear endpoints OpenAPI
3. Testes E2E

---

## 13. Exemplos de Fluxo (Cenários)

### Cenário 1: Criar Login e primeiro acesso
1. POST `/v1/logins` (admin cria) → 201, status=PENDING
2. Email enviado ao usuário com link de confirmação
3. PUT `/v1/logins/{id}/password` (usuário define primeira senha) → 204, status=ACTIVE
4. Usuário consegue fazer login

### Cenário 2: Bloqueio por segurança
1. GET `/v1/logins/{id}` → status=ACTIVE, failedLoginAttempts=0
2. Múltiplas tentativas falhas de login → failedLoginAttempts incrementa
3. Após 5 falhas: PUT `/v1/logins/{id}/block` (automático ou manual) → 204
4. Login status=BLOCKED, usuário desabil itado em Keycloak
5. Usuário não consegue fazer login até desbloquear

### Cenário 3: Mudança de perfil (aplicar novas permissões)
1. PUT `/v1/logins/{id}` com `profileId: new_profile_id` → 204
2. Keycloak sincronizado: remover roles antigos, adicionar novos
3. Sessões ativas: permissões atualizadas após próximo refresh token

### Cenário 4: Alteração de senha (força reiniciar)
1. PUT `/v1/logins/{id}/password` (admin força mudança) → 204
2. Tokens antigos invalidados
3. Usuário precisa fazer novo login com nova senha

---

## 14. Referências Técnicas

### 14.1 Paths e Arquivos Principais
- OpenAPI: `etc/api/organization/ScosOrganization_Login.yml`
- Domain Model:
  - `scos-organization-domain/.../login/Login.java`
  - `scos-organization-domain/.../login/LoginDomainService.java`
  - `scos-organization-domain/.../login/Profile.java`
  - `scos-organization-domain/.../login/Features.java` (enum)
- Repositories: `scos-organization-domain/.../repository/login/`
- UseCases: `scos-organization-application/.../usecase/login/`
- Controllers: `scos-organization-api/.../login/LoginController.java`
- Keycloak Adapter: `scos-organization-infrastructure/.../keycloak/KeycloakPort.java`
- Migrations: `scos-organization-boot/src/main/resources/db/changelog/.../login/`
- Permissões: `scos-organization-infrastructure/.../ScosOrganizationPermission.java`

### 14.2 Integração Keycloak
- REST endpoint: `http://keycloak:8080/admin/realms/{realm}`
- Operações: criar usuário, atribuir grupo, atualizar role, desabilitar
- Autenticação: usar service account ou bearer token

### 14.3 Estrutura de Testes
```
scos-organization-domain/src/test/java/.../login/
├── LoginTest.java (invariantes)
├── LoginDomainServiceTest.java (validações)
├── PasswordPolicyTest.java (política de senha)

scos-organization-application/src/test/.../login/
├── CreateLoginUseCaseIntegrationTest.java
├── BlockLoginUseCaseIntegrationTest.java

scos-organization-api/src/test/.../login/
├── LoginControllerTest.java (E2E com MockMvc)
```

---

## 15. Histórico de Versão

| Versão | Data | Autor | Alteração |
|--------:|:-----:|:-----|:---------|
| 1.1 | 2026-02-17 | GitHub Copilot | **Reestruturação completa v2**: Visão Estratégica, Índice, Entidades com schema BD, UseCases detalhados (Create, Update, Get, Block, ChangePassword, Unblock, Delete, List, Profile CRUD), Exemplos JSON, Error Mapping, Arquitetura Keycloak, Invariantes, Árvore de Erros, Implementação 4 fases, Cenários, Referências Técnicas expandidas. **Documento expandido de 65 para ~850 linhas com padrão Company**. |
| 1.0 | inicial | — | Documento base (origem a partir do OpenAPI) |
