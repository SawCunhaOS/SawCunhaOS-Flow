## 1. Remoção e Criação de Arquivos

- [x] 1.1 Remover `scos_permission.yml` do diretório de tables
- [x] 1.2 Criar `scos_system.yml` — UUID PK (`gen_random_uuid()`), CODE UK, SECRET_KEY, STATUS, auditoria NOT NULL
- [x] 1.3 Criar `scos_resource.yml` — UUID PK, FK SYSTEM_ID, CODE UK, ACTIVE DEFAULT TRUE, `IDX_SYSTEM_ID_SCOS_RESOURCE`
- [x] 1.4 Criar `scos_profile_resource.yml` — PK composta (PROFILE_ID, RESOURCE_ID), sem UPDATED_AT, `IDX_PROFILE_ID_*` e `IDX_RESOURCE_ID_*`

## 2. Correções de Schema — Organização

- [x] 2.1 Reescrever `scos_department.yml` — `DESCRIPTION VARCHAR(30)`, `UPDATED_AT NOT NULL`, `USER_AT VARCHAR(255) NOT NULL`, PK com `GENERATED ALWAYS AS IDENTITY`, sem sequence
- [x] 2.2 Reescrever `scos_position.yml` — `DESCRIPTION VARCHAR(30)`, `UPDATED_AT NOT NULL`, `USER_AT VARCHAR(255) NOT NULL`, PK com `GENERATED ALWAYS AS IDENTITY`, sem sequence
- [x] 2.3 Reescrever `scos_company.yml` — remover `ACTIVE`, `NAME VARCHAR(250)`, `NAME_TREATMENT VARCHAR(100)`, `STATUS NOT NULL`, `UPDATED_AT NOT NULL`, `USER_AT VARCHAR(255) NOT NULL`, PK com `GENERATED ALWAYS AS IDENTITY`, sem sequence; adicionar `IDX_STATUS_SCOS_COMPANY`
- [x] 2.4 Reescrever `scos_company_address.yml` — PK composta (`COMPANY_ID_ADDRESS`, `COMPANY_ID`), sem surrogate, sem sequence, `GEOLOCATION NOT NULL`, `UPDATED_AT NOT NULL`, `USER_AT VARCHAR(255) NOT NULL`; UK em `TYPE`
- [x] 2.5 Reescrever `scos_company_contact.yml` — renomear PK para `COMPANY_ID_CONTACT`, remover `RESPONSIBLE_PERSON`, adicionar UK em `PHONE`/`EMAIL`/`TYPE`, `UPDATED_AT NOT NULL`, `USER_AT VARCHAR(255) NOT NULL`, PK com `GENERATED ALWAYS AS IDENTITY`, sem sequence
- [x] 2.6 Reescrever `scos_employee.yml` — `COMPANY_ID NOT NULL`, `POSITION_ID NOT NULL`, adicionar `STATUS VARCHAR(20) NOT NULL`, remover `ACTIVE`, `NAME VARCHAR(250)`, `NAME_TREATMENT VARCHAR(100)`, `EMAIL VARCHAR(255)`, adicionar UK em `EMAIL`, `UPDATED_AT NOT NULL`, `USER_AT VARCHAR(255) NOT NULL`, PK com `GENERATED ALWAYS AS IDENTITY`, sem sequence; adicionar `IDX_STATUS_SCOS_EMPLOYEE`
- [x] 2.7 Reescrever `scos_employee_address.yml` — renomear `ADDRESS_ID` → `EMPLOYEE_ID_ADDRESS`, PK composta (`EMPLOYEE_ID_ADDRESS`, `EMPLOYEE_ID`), `GEOLOCATION NOT NULL`, `UPDATED_AT NOT NULL`, `USER_AT VARCHAR(255) NOT NULL`
- [x] 2.8 Reescrever `scos_employee_contact.yml` — renomear PK para `EMPLOYEE_ID_CONTACT`, adicionar UK em `PHONE` e `TYPE`, `UPDATED_AT NOT NULL`, `USER_AT VARCHAR(255) NOT NULL`, PK com `GENERATED ALWAYS AS IDENTITY`, sem sequence

## 3. Correções de Schema — Acesso e Permissões

- [x] 3.1 Reescrever `scos_profile.yml` — remover `FEATURES TEXT[]` e índice GIN, `CODE VARCHAR(30)`, `DESCRIPTION VARCHAR(30)`, `UPDATED_AT NOT NULL`, `USER_AT VARCHAR(255) NOT NULL`, PK com `GENERATED ALWAYS AS IDENTITY`, sem sequence
- [x] 3.2 Reescrever `scos_login.yml` — `EMPLOYEE_ID FK NULL`, remover `PASSWORD`/`SALT`/`DATE_LAST_CHANGE_PASSWORD`, adicionar `TYPE VARCHAR(50) NOT NULL`, `LOGIN VARCHAR(255)`, `UPDATED_AT NOT NULL`, `USER_AT VARCHAR(255) NOT NULL`, PK com `GENERATED ALWAYS AS IDENTITY`, sem sequence; adicionar `IDX_KEYCLOAK_ID_SCOS_LOGIN`

## 4. Correções de Schema — Configuração

- [x] 4.1 Reescrever `scos_configuration.yml` — PK textual `CONFIGURATION_ID VARCHAR(50)` (sem auto-increment, sem sequence), remover colunas antigas, adicionar `VALUE TEXT NOT NULL` e `TYPE VARCHAR(50) NOT NULL`, `UPDATED_AT NOT NULL`, `USER_AT VARCHAR(255) NOT NULL`

## 5. Correções de Schema — Integração Keycloak

- [x] 5.1 Reescrever `scos_integration_keycloak.yml` — adicionar `RETRY_COUNT INT NOT NULL DEFAULT 0` e `MAX_RETRIES INT NOT NULL DEFAULT 3`; adicionar `IDX_STATUS_SCOS_INTEGRATION_KEYCLOAK` e `IDX_KEYCLOAK_ID_SCOS_INTEGRATION_KEYCLOAK`
- [x] 5.2 Reescrever `scos_integration_keycloak_log.yml` — remover `UPDATED_AT` (tabela imutável)
- [x] 5.3 Reescrever `scos_integration_message_invalid.yml` — `MESSAGE_INVALID NOT NULL`, remover `UPDATED_AT` (tabela imutável)

## 6. Reorganização do Changelog

- [x] 6.1 Atualizar `tables.yml` — remover `scos_permission`, adicionar `scos_system`, `scos_resource`, `scos_profile_resource` na ordem correta de dependências FK:
  ```
  scos_department → scos_position → scos_company → scos_company_address
  → scos_company_contact → scos_employee → scos_employee_address
  → scos_employee_contact → scos_profile → scos_system → scos_resource
  → scos_profile_resource → scos_login → scos_configuration
  → scos_integration_keycloak → scos_integration_keycloak_log
  → scos_integration_message_invalid
  ```

## 7. Validação

- [ ] 7.1 Executar `liquibase update` em banco limpo — confirmar que todas as 17 tabelas são criadas sem erros
- [ ] 7.2 Verificar que `SCOS_PERMISSION` não existe no banco após a migration
- [ ] 7.3 Verificar que nenhuma sequence `SEQ_*` existe no schema `scos`
- [ ] 7.4 Executar `liquibase rollback` — confirmar que todas as tabelas são removidas sem erros
