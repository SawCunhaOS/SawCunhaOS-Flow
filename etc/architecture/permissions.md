# Mapa de Features ↔ Permissões (Organization)

## Objetivo
Documento resumo que descreve as `Features` e as `Permissões (ScosOrganizationPermission)` do módulo Organization, onde persistir essas permissões em runtime e como manter a consistência entre OpenAPI (x-authorize), enums Java e Keycloak.

---

## Convenções adotadas
- Nome das Features: `ORGANIZATION_<DOMÍNIO>_<SCOPE>` (ex.: `ORGANIZATION_COMPANY_MANAGEMENT`).
- Nome das permissões: `ACTION_RESOURCE` (ex.: `CREATE_COMPANY`, `ENABLE_EMPLOYEE`).
- Cache / x-cache: `SCOS_ORGANIZATION_<TAG>` (já aplicado nos YAMLs).

---

## Resumo (Feature → responsabilidades / exemplos de permissões)
- ORGANIZATION_ADMINISTRATION
  - Finalidade: administração global do sistema Organization. (aplicada apenas a operações administrativas)
  - Ex.: `GET_FEATURES`, gerenciamento de `Profile` (CREATE/UPDATE/DELETE_PROFILE), sincronização de papéis/roles, auditoria e operações infra/segurança.
- ORGANIZATION_VIEW
  - Finalidade: visão/consulta global.
  - Ex.: `GET_COMPANY`, `GET_EMPLOYEE`, `GET_FEATURES`.
- ORGANIZATION_MANAGEMENT
  - Finalidade: permissão genérica de gerenciamento.
  - Ex.: `CREATE_*`, `UPDATE_*`, `DELETE_*` quando aplicável.

- ORGANIZATION_COMPANY_VIEW / ORGANIZATION_COMPANY_MANAGEMENT
  - Ex.: `GET_COMPANY`, `CREATE_COMPANY`, `ENABLE_COMPANY`, `DOWNLOAD_COMPANY_DOCUMENT`.

- ORGANIZATION_EMPLOYEE_VIEW / ORGANIZATION_EMPLOYEE_MANAGEMENT / ORGANIZATION_EMPLOYEE_LOGIN_MANAGEMENT
  - Ex.: `GET_EMPLOYEE`, `CREATE_EMPLOYEE`, `ENABLE_EMPLOYEE`, `GET_EMPLOYEE_LOGIN`, `UPDATE_EMPLOYEE_LOGIN_PASSWORD`.

- ORGANIZATION_DEPARTMENT_VIEW / ORGANIZATION_DEPARTMENT_MANAGEMENT
  - Ex.: `GET_DEPARTMENT`, `CREATE_DEPARTMENT`.

- ORGANIZATION_POSITION_VIEW / ORGANIZATION_POSITION_MANAGEMENT
  - Ex.: `GET_POSITION`, `CREATE_POSITION`.

- ORGANIZATION_PROFILE_VIEW / ORGANIZATION_PROFILE_MANAGEMENT
  - Ex.: `GET_PROFILE`, `CREATE_PROFILE`.

---

## Onde salvar / persistir permissões em runtime
- Banco de dados (seed): `scos_profile.features` — veja `scos-organization-boot/src/main/resources/db/changelog/v1/0/0/insert/configure_system.sql` (ex.: `ORGANIZATION_ADMINISTRATION`).

---

## Regras de manutenção / sincronização
1. Sempre que adicionar um novo `x-authorize` no OpenAPI:
   - Criar a constante em `ScosOrganizationPermission.java` com o mesmo nome.
   - Atualizar `ScosOrganizationFeature.java` se precisar de nova feature.
   - Atualizar Keycloak realm / seed DB conforme necessário.
   - Rodar o teste `PermissionsConsistencyTest` (módulo `scos-organization-infrastructure`) para validar consistência.

2. Padrão de mapeamento sugerido:
   - Endpoints de leitura → `*_VIEW` + `ORGANIZATION_VIEW`
   - Endpoints de escrita/estado → `*_MANAGEMENT` + `ORGANIZATION_MANAGEMENT`
   - Endpoints sensíveis (senha, bloqueio) → `ORGANIZATION_EMPLOYEE_LOGIN_MANAGEMENT` + `ORGANIZATION_MANAGEMENT`

3. Onde armazenar novos papéis/roles:
   - Primeira opção: Keycloak (roles realm + mapper que traduz roles → authorities).
   - Segunda opção: arquivo YAML em `etc/security/permissions.yml` seguido de um job que sincronize com Keycloak/DB.

---

## Automação / validação disponível
- `PermissionsConsistencyTest` (módulo `scos-organization-infrastructure`): valida que todo `x-authorize` nos OpenAPI YAMLs possui constante em `ScosOrganizationPermission`.
- Recomenda-se adicionar CI step que execute esse teste para evitar drift.

---

## Exemplo prático (trecho)
- `x-authorize: [ ENABLE_COMPANY ]`  
  → deve existir `ScosOrganizationPermission.ENABLE_COMPANY` e a feature `ORGANIZATION_COMPANY_MANAGEMENT` deve cobrir essa permissão.

---

## Observações finais
- As `Features` foram renomeadas de `PARTNERS_*` para `ORGANIZATION_*` para refletir o escopo real do domínio.
- Alterações de nome podem requerer atualização do `Scos_Realm.json` (Keycloak) e do seed DB (arquivo SQL já atualizado).

Se desejar, crio também o arquivo `etc/security/permissions.yml` contendo todas permissões atuais como fonte canônica e atualizo o `Scos_Realm.json` automaticamente. (Responda "Sim" para prosseguir.)
