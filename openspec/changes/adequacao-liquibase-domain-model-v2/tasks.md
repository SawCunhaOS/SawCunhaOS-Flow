## 1. Tabelas de catálogo/referência (sem dependências novas)

- [x] 1.1 Criar `scos_address_type.yml` (`SCOS_ADDRESS_TYPE`) — schema-type-catalog
- [x] 1.2 Criar `scos_contact_type.yml` (`SCOS_CONTACT_TYPE`) — schema-type-catalog
- [x] 1.3 Criar `scos_legal_nature.yml` (`SCOS_LEGAL_NATURE`, vazia) — schema-fiscal-data
- [x] 1.4 Criar `scos_cnae.yml` (`SCOS_CNAE`, vazia) — schema-fiscal-data
- [x] 1.5 Criar `scos_outbox_topic.yml` (`SCOS_OUTBOX_TOPIC`) — schema-outbox
- [x] 1.6 Criar `scos_reason_activate.yml` (`SCOS_REASON_ACTIVATE`) — schema-status-history
- [x] 1.7 Criar `scos_reason_inactivate.yml` (`SCOS_REASON_INACTIVATE`) — schema-status-history
- [x] 1.8 Criar `scos_reason_disable.yml` (`SCOS_REASON_DISABLE`) — schema-status-history
- [x] 1.9 Criar `scos_reason_enable.yml` (`SCOS_REASON_ENABLE`) — schema-status-history

## 2. Alterações em tabelas existentes

- [x] 2.1 Corrigir `scos_department.yml`: `DESCRIPTION` 30→255 (já estava 255 no arquivo — nenhuma mudança necessária)
- [x] 2.2 Corrigir `scos_position.yml`: `DESCRIPTION` 30→255, adicionar `IS_TRUST_POSITION`, trocar UK de `CODE` para composta com `DEPARTMENT_ID`
- [x] 2.3 Corrigir `scos_employee.yml`: adicionar `CONTRACT_TYPE`, `PROBATION_END_DATE`
- [x] 2.4 Corrigir `scos_company.yml`: adicionar `LEGAL_NATURE_ID` FK, `CNAE_PRINCIPAL_ID` FK, `STATE_REGISTRATION`, `MUNICIPAL_REGISTRATION` + índices em `LEGAL_NATURE_ID`/`CNAE_PRINCIPAL_ID`
- [x] 2.5 Corrigir `scos_login.yml`: renomear `KEYCLOAK_ID` → `EXTERNAL_ID`, adicionar `TYPE` (se ainda ausente), renomear índice `IDX_KEYCLOAK_ID_SCOS_LOGIN` → `IDX_EXTERNAL_ID_SCOS_LOGIN`
- [x] 2.6 Corrigir `scos_employee_address.yml`: trocar coluna `TYPE` por `ADDRESS_TYPE_ID` FK → `SCOS_ADDRESS_TYPE` + índice
- [x] 2.7 Corrigir `scos_company_address.yml`: trocar coluna `TYPE` por `ADDRESS_TYPE_ID` FK → `SCOS_ADDRESS_TYPE` + índice
- [x] 2.8 Corrigir `scos_employee_contact.yml`: trocar coluna `TYPE` por `CONTACT_TYPE_ID` FK → `SCOS_CONTACT_TYPE` + índice
- [x] 2.9 Corrigir `scos_company_contact.yml`: trocar coluna `TYPE` por `CONTACT_TYPE_ID` FK → `SCOS_CONTACT_TYPE` + índice

## 3. Tabelas dependentes de 1º grau

- [x] 3.1 Criar `scos_company_cnae_secondary.yml` (N:N `SCOS_COMPANY`/`SCOS_CNAE`, índice reverso) — schema-fiscal-data
- [x] 3.2 Criar `scos_position_work_schedule.yml` — schema-organization
- [x] 3.3 Criar `scos_employee_work_schedule.yml` — schema-organization
- [x] 3.4 Criar `scos_reason_position_change.yml` — schema-organization
- [x] 3.5 Criar `scos_employee_position_history.yml` (com índice único parcial `WHERE END_DATE IS NULL`) — schema-organization (índice parcial adicionado na tarefa 8)
- [x] 3.6 Criar `scos_login_profile.yml` (PK composta, índice reverso em `PROFILE_ID`) — schema-access
- [x] 3.7 Criar `scos_outbox_event.yml` (com índice parcial `WHERE STATUS = 'PENDING'`) — schema-outbox (índice parcial adicionado na tarefa 8)
- [x] 3.8 Criar `scos_outbox_event_log.yml` — schema-outbox
- [x] 3.9 Criar `scos_outbox_event_dead_letter.yml` — schema-outbox
- [x] 3.10 Criar `scos_company_status_history.yml` — schema-status-history
- [x] 3.11 Criar `scos_employee_status_history.yml` — schema-status-history
- [x] 3.12 Criar `scos_login_status_history.yml` — schema-status-history

## 4. Remoção das tabelas de integração Keycloak legadas

- [x] 4.1 Remover `scos_integration_keycloak.yml`
- [x] 4.2 Remover `scos_integration_keycloak_log.yml`
- [x] 4.3 Remover `scos_integration_message_invalid.yml`

## 5. Reordenar tables.yml

- [x] 5.1 Atualizar `v1.0.0/tables/tables.yml`: remover as 3 referências da seção 4, adicionar as 21 novas referências (seções 1 e 3), respeitando ordem de dependência de FK (catálogos/motivos → tabelas que os referenciam → tabelas de histórico/outbox)

## 6. CHECK constraints de vocabulário fechado

- [x] 6.1 Criar changeSet(s) com os ~20 `ALTER TABLE ... ADD CONSTRAINT CHK_* CHECK (...)` listados na seção "Checks de Domínio" do domain model (SQL já pronto — copiar literalmente, ajustando apenas nomes de changeSet/rollback) — novo arquivo `checks/checks.yml`, registrado em `db.changelog-master.yml`
- [x] 6.2 Adicionar `rollback` (`DROP CONSTRAINT`) para cada `CHECK` criado

## 7. Functions e triggers

- [x] 7.1 Criar `fn_before_insert_company_status_history` + `trg_before_insert_company_status_history` (deriva `PREVIOUS_STATUS`, valida `ENTITY_TYPE` do motivo)
- [x] 7.2 Criar `fn_before_insert_employee_status_history` + `trg_before_insert_employee_status_history` (mesma lógica, `EMPLOYEE_ID`/`EMPLOYEE`)
- [x] 7.3 Criar `fn_before_insert_login_status_history` + `trg_before_insert_login_status_history` (mesma lógica, `LOGIN_ID`/`LOGIN`)
- [x] 7.4 Criar `fn_sync_company_status` + `trg_sync_company_status` (AFTER INSERT, propaga `STATUS` pra `SCOS_COMPANY`)
- [x] 7.5 Criar `fn_sync_employee_status` + `trg_sync_employee_status` (idem `SCOS_EMPLOYEE`)
- [x] 7.6 Criar `fn_sync_login_status` + `trg_sync_login_status` (idem `SCOS_LOGIN`)
- [x] 7.7 Criar `fn_block_delete` compartilhada + 8 triggers `trg_block_delete_*` (`address_type`, `contact_type`, `outbox_topic`, `reason_activate`, `reason_inactivate`, `reason_disable`, `reason_enable`, `reason_position_change`)
- [x] 7.8 Criar `fn_validate_employee_contact_type` + `trg_validate_employee_contact_type`
- [x] 7.9 Criar `fn_validate_company_contact_type` + `trg_validate_company_contact_type`
- [x] 7.10 Criar `fn_validate_employee_address_type` + `trg_validate_employee_address_type`
- [x] 7.11 Criar `fn_validate_company_address_type` + `trg_validate_company_address_type`
- [x] 7.12 Criar `fn_close_previous_position` + `trg_close_previous_position` (BEFORE INSERT em `SCOS_EMPLOYEE_POSITION_HISTORY`)
- [x] 7.13 Criar `fn_sync_employee_position` + `trg_sync_employee_position` (AFTER INSERT, propaga `POSITION_ID` pra `SCOS_EMPLOYEE`)
- [x] 7.14 Criar `fn_validate_login_profile_not_primary` + `trg_validate_login_profile_not_primary`
- [x] 7.15 Registrar todas as functions em `function/function.yml` e todos os triggers em `triggers/triggers.yml`, com `rollback` (`DROP FUNCTION`/`DROP TRIGGER`) em cada changeSet

## 8. Índices de performance

- [x] 8.1 Adicionar em `indexes.yml` os ~20 índices listados na seção "Índices de Performance" do domain model. Índices simples de FK foram inline nas próprias tabelas (tarefas 1-3); os compostos/parciais ganharam 5 novos arquivos: `status_history.yml` (3 composto + 4 parcial), `type_catalog.yml` (2 parcial), `outbox.yml` (1 parcial + 1 composto), `fiscal.yml` (1 composto), `position_history.yml` (1 único parcial)

## 9. Views

- [x] 9.1 Revisar `vw_login_context.sql` contra `KEYCLOAK_ID`→`EXTERNAL_ID` e demais colunas renomeadas; ajustar se necessário — `l.keycloak_id` → `l.external_id`
- [x] 9.2 Revisar `vw_authority_response.sql` contra colunas renomeadas/removidas; ajustar se necessário — `keycloak_id` → `external_id` no SELECT e no GROUP BY

## 10. Validação

- [x] 10.1 Rodar `liquibase update` em banco limpo — sem erros, todas as ~34 tabelas do domain model presentes
- [x] 10.2 Rodar `liquibase rollback` de ponta a ponta — sem erros
- [x] 10.3 Conferir manualmente que nenhuma tabela/coluna fora do domain model sobrou (`SCOS_INTEGRATION_*`, `TYPE` texto em address/contact, `KEYCLOAK_ID`)
