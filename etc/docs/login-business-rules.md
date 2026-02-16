# Regras de Negócio — Login & Profile API

Fonte: `etc/api/organization/ScosOrganization_Login.yml`

## Visão Geral
Regras para gerenciamento de `Login`, `Profile` e `Features` (criação, alteração, bloqueio, alteração de perfil e senha).

---

## Entidades principais
- Login (id, login, status, dateCreated, dateLastChangePassword, profile)
- Profile (id, code, description, features)
- Features / Permissions

---

## Regras extraídas do OpenAPI (obrigatórias)
- `CreateEmployeeLoginRequest` requer `login`, `profileId`, `password`.
- `UpdateEmployeeLoginDTO` requer `login` e `profileId`.
- Endpoints para block/unblock, change password e obter informações de login estão disponíveis.
- Status possíveis (`LoginStatus`): `ENABLE`, `BLOCKED`, `INACTIVE`, `PENDING`, `PENDING_PASSWORD_CHANGE`.
- Operações importantes: `block`, `unblock`, `change password`, `update profile`.

---

## Regras de negócio propostas / recomendadas
- Unicidade: `login` deve ser único por tenant.
- Política de senha: definir requisitos mínimos (tamanho, complexidade, histórico) e expiração (sugerido).
- Fluxo de bloqueio:
  - `block` altera `status = BLOCKED`; sessões ativas devem ser invalidadas e Keycloak deve refletir bloqueio.
  - `unblock` restaura para `ENABLE` (ou `PENDING` conforme regras de negócio).
- Alteração de perfil:
  - Atualizar permissões/roles no Keycloak e propagar alterações para sessões ativas se necessário.
- Etiqueta de segurança:
  - Ao `change password`, verificar `passwordOld`, atualizar `dateLastChangePassword` e invalidar tokens antigos.
- Exclusão de `Profile`: proibida se houver `Login` associado; retornar 409/422.
- Criação/Alteração de `Login`: sincronizar com Keycloak (criar usuário, atribuir perfil/roles, set password).

---

## Validações de campo (resumo)
- `login`: obrigatório, formato alfanumérico e/ou e-mail (definir padrão), unicidade obrigatória.
- `password`: obrigatório na criação; validar conforme política de senha.
- `profileId`: referenciar `Profile` existente.

---

## Eventos de domínio sugeridos
- `login.created`, `login.updated`, `login.blocked`, `login.unblocked`, `login.password.changed`
- `profile.updated` (disparar re-atribuição/propagação para logins afetados)

---

## Perguntas / decisões a confirmar
- Política de senha (força, expiração, histórico)?
- Revalidação/forçar logout de sessões ativas após mudança de perfil/senha?
- Mapear `Profile` → roles no Keycloak (tabela de permissões)?

---

## Observações técnicas
- Toda operação que altera estado de autenticação deve ser refletida em Keycloak via Port/Adapter.
- Use `x-authorize` para controlar acesso a endpoints administrativos.
- Mantenha trilha de auditoria e publique eventos para consumidores de features/permissions.
