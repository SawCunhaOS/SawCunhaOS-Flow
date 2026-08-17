-- =============================================================================
-- SCOS — Limpeza total (testes integrados / Testcontainers)
--
-- Executado por @Sql AFTER_TEST_METHOD para isolar cada método de teste.
--
-- Usa TRUNCATE (não DELETE) por dois motivos:
--   1. As tabelas de referência (SCOS_REASON_*, SCOS_*_TYPE, SCOS_OUTBOX_TOPIC)
--      têm trigger BEFORE DELETE (fn_block_delete) que PROÍBE DELETE. TRUNCATE
--      não dispara triggers de linha, então não é bloqueado.
--   2. RESTART IDENTITY zera as sequences → os IDs gerados (DEPARTMENT_ID=1,
--      COMPANY_ID=1, etc.) voltam a ser determinísticos no próximo setsup.
--
-- CASCADE resolve automaticamente a ordem de dependência das FKs.
-- =============================================================================

SET search_path TO scos, public;

TRUNCATE TABLE
    scos.SCOS_PROFILE_RESOURCE,
    scos.SCOS_LOGIN_PROFILE,
    scos.SCOS_LOGIN_STATUS_HISTORY,
    scos.SCOS_LOGIN,
    scos.SCOS_PROFILE,
    scos.SCOS_RESOURCE,
    scos.SCOS_SYSTEM,
    scos.SCOS_EMPLOYEE_STATUS_HISTORY,
    scos.SCOS_EMPLOYEE_POSITION_HISTORY,
    scos.SCOS_EMPLOYEE_WORK_SCHEDULE,
    scos.SCOS_EMPLOYEE_ADDRESS,
    scos.SCOS_EMPLOYEE_CONTACT,
    scos.SCOS_EMPLOYEE,
    scos.SCOS_POSITION_WORK_SCHEDULE,
    scos.SCOS_POSITION,
    scos.SCOS_DEPARTMENT,
    scos.SCOS_COMPANY_STATUS_HISTORY,
    scos.SCOS_COMPANY_ADDRESS,
    scos.SCOS_COMPANY_CONTACT,
    scos.SCOS_COMPANY_CNAE_SECONDARY,
    scos.SCOS_COMPANY,
    scos.SCOS_CNAE,
    scos.SCOS_LEGAL_NATURE,
    scos.SCOS_ADDRESS_TYPE,
    scos.SCOS_CONTACT_TYPE,
    scos.SCOS_REASON_ACTIVATE,
    scos.SCOS_REASON_INACTIVATE,
    scos.SCOS_REASON_DISABLE,
    scos.SCOS_REASON_ENABLE,
    scos.SCOS_REASON_POSITION_CHANGE,
    scos.SCOS_CONFIGURATION,
    scos.SCOS_OUTBOX_EVENT_LOG,
    scos.SCOS_OUTBOX_EVENT_DEAD_LETTER,
    scos.SCOS_OUTBOX_EVENT,
    scos.SCOS_OUTBOX_TOPIC
RESTART IDENTITY CASCADE;
-- =============================================================================
-- SCOS — Dados de semente para desenvolvimento
--
-- Executar MANUALMENTE após o Liquibase ter criado o schema e as tabelas.
-- Script é idempotente:
--   - Tabelas com UK real (CODE, CODE+ENTITY_TYPE, TAX_IDENTIFIER, etc.) usam ON CONFLICT DO NOTHING.
--   - SCOS_EMPLOYEE_POSITION_HISTORY usa ON CONFLICT DO NOTHING apoiado no índice
--     único parcial (EMPLOYEE_ID) WHERE END_DATE IS NULL.
--   - SCOS_*_STATUS_HISTORY não tem UK (tabela de auditoria, múltiplas linhas por
--     entidade são esperadas) — usa INSERT ... WHERE NOT EXISTS para não duplicar
--     a linha de criação em re-execuções.
--
-- Pré-requisitos:
--   - Keycloak com realm Scos importado (etc/infra/keycloak/Scos_Realm.json)
--   - Banco scos com schema scos criado pelo Liquibase
--
-- Usuários Keycloak referenciados:
--   scos-admin  → EXTERNAL_ID = 01000000-0000-0000-0000-000000000001
--   scos-api    → EXTERNAL_ID = 02000000-0000-0000-0000-000000000002
--
-- SCOS_SYSTEM e SCOS_RESOURCE:
--   Em produção a própria aplicação os popula ao subir (registro via gRPC no
--   scos-registry). Aqui são semeados com IDENTIDADE FIXA para que os testes
--   integrados (Testcontainers) tenham dados determinísticos SEM depender do
--   registro em runtime. As linhas de SCOS_RESOURCE espelham 1:1 as constantes
--   de ScosOrganizationPermission (grupo, subgrupo, versão, data e descrições
--   pt/en dos bundles messages_permission*).
--
-- Identidade fixa do sistema:
--   SYSTEM_ID  = 03000000-0000-0000-0000-000000000003
--   SECRET_KEY = 03000000-0000-0000-0000-0000000000AA
--   CODE       = SCOS_ORGANIZATION
--
-- Ordem de execução (script único e autossuficiente):
--   1. Tabelas de referência (tipos, motivos, natureza jurídica, CNAE)
--   2. SCOS_SYSTEM + SCOS_RESOURCE (sistema e permissões)
--   3. Empresa, funcionário, perfis, logins, histórico inicial de cargo/status
--      e configurações
--   4. SCOS_PROFILE_RESOURCE → vincula todos os recursos a todos os perfis
-- =============================================================================

SET search_path TO scos, public;

-- ============================================================
-- SCOS_LEGAL_NATURE — natureza jurídica (tabela de referência IBGE)
-- ============================================================
INSERT INTO scos.SCOS_LEGAL_NATURE (CODE, DESCRIPTION, CREATED_AT)
VALUES ('206-2', 'Sociedade Empresária Limitada', NOW())
ON CONFLICT DO NOTHING;
-- LEGAL_NATURE_ID gerado: 1

-- ============================================================
-- SCOS_CNAE — CNAE principal (tabela de referência IBGE)
-- ============================================================
INSERT INTO scos.SCOS_CNAE (CODE, DESCRIPTION, CREATED_AT)
VALUES ('6201-5/01', 'Desenvolvimento de programas de computador sob encomenda', NOW())
ON CONFLICT DO NOTHING;
-- CNAE_ID gerado: 1

-- ============================================================
-- SCOS_ADDRESS_TYPE — tipos de endereço (compartilhado EMPLOYEE/COMPANY)
-- ============================================================
INSERT INTO scos.SCOS_ADDRESS_TYPE (CODE, DESCRIPTION, ENTITY_TYPE, ACTIVE, UPDATED_AT, USER_AT)
VALUES
    ('HOME',       'Residencial',        'EMPLOYEE', true, NOW(), 'seed'),
    ('WORK',       'Comercial/trabalho', 'EMPLOYEE', true, NOW(), 'seed'),
    ('COMMERCIAL', 'Comercial',          'COMPANY',  true, NOW(), 'seed'),
    ('BILLING',    'Cobrança',           'COMPANY',  true, NOW(), 'seed'),
    ('BRANCH',     'Filial',             'COMPANY',  true, NOW(), 'seed')
ON CONFLICT DO NOTHING;
-- ADDRESS_TYPE_ID gerados: 1=HOME/EMPLOYEE, 2=WORK/EMPLOYEE, 3=COMMERCIAL/COMPANY, 4=BILLING/COMPANY, 5=BRANCH/COMPANY

-- Registro adicional INATIVO — usado nos ITs de "disable já inativo". O disable é
-- @JdempotentResource keyed pelo id do path; re-disable de um id recém-criado devolveria a
-- resposta cacheada (204) sem exercitar a regra _004. Partindo de um id JÁ inativo no seed, o
-- disable executa de fato e a regra "já inativo" é validada. ADDRESS_TYPE_ID gerado: 6.
INSERT INTO scos.SCOS_ADDRESS_TYPE (CODE, DESCRIPTION, ENTITY_TYPE, ACTIVE, UPDATED_AT, USER_AT)
VALUES ('ARCHIVED', 'Tipo de endereço arquivado (inativo p/ testes)', 'EMPLOYEE', false, NOW(), 'seed')
ON CONFLICT DO NOTHING;

-- ============================================================
-- SCOS_CONTACT_TYPE — tipos de contato (compartilhado EMPLOYEE/COMPANY)
-- ============================================================
INSERT INTO scos.SCOS_CONTACT_TYPE (CODE, DESCRIPTION, ENTITY_TYPE, ACTIVE, UPDATED_AT, USER_AT)
VALUES
    ('MOBILE',     'Celular',            'EMPLOYEE', true, NOW(), 'seed'),
    ('WORK',       'Telefone comercial', 'EMPLOYEE', true, NOW(), 'seed'),
    ('COMMERCIAL', 'Comercial',          'COMPANY',  true, NOW(), 'seed'),
    ('SUPPORT',    'Suporte',            'COMPANY',  true, NOW(), 'seed')
ON CONFLICT DO NOTHING;
-- CONTACT_TYPE_ID gerados: 1=MOBILE/EMPLOYEE, 2=WORK/EMPLOYEE, 3=COMMERCIAL/COMPANY, 4=SUPPORT/COMPANY

-- Registro adicional INATIVO — usado no IT de "disable já inativo" (ver nota em SCOS_ADDRESS_TYPE).
-- CONTACT_TYPE_ID gerado: 5.
INSERT INTO scos.SCOS_CONTACT_TYPE (CODE, DESCRIPTION, ENTITY_TYPE, ACTIVE, UPDATED_AT, USER_AT)
VALUES ('ARCHIVED', 'Tipo de contato arquivado (inativo p/ testes)', 'EMPLOYEE', false, NOW(), 'seed')
ON CONFLICT DO NOTHING;

-- ============================================================
-- SCOS_REASON_ACTIVATE — motivos de ativação/reativação (COMPANY/EMPLOYEE/LOGIN)
-- ============================================================
INSERT INTO scos.SCOS_REASON_ACTIVATE (CODE, DESCRIPTION, ENTITY_TYPE, ACTIVE, UPDATED_AT, USER_AT)
VALUES
    ('INITIAL_REGISTRATION', 'Cadastro inicial da empresa',  'COMPANY',  true, NOW(), 'seed'),
    ('NEW_HIRE',             'Nova contratação',             'EMPLOYEE', true, NOW(), 'seed'),
    ('REINSTATEMENT',        'Recontratação (rehire)',       'EMPLOYEE', true, NOW(), 'seed'),
    ('NEW_HIRE',             'Criação inicial do login',     'LOGIN',    true, NOW(), 'seed')
ON CONFLICT DO NOTHING;
-- REASON_ACTIVATE_ID gerados: 1=COMPANY/INITIAL_REGISTRATION, 2=EMPLOYEE/NEW_HIRE, 3=EMPLOYEE/REINSTATEMENT, 4=LOGIN/NEW_HIRE

-- Registro adicional INATIVO — usado no IT de "disable já inativo" (ver nota em SCOS_ADDRESS_TYPE).
-- REASON_ACTIVATE_ID gerado: 5.
INSERT INTO scos.SCOS_REASON_ACTIVATE (CODE, DESCRIPTION, ENTITY_TYPE, ACTIVE, UPDATED_AT, USER_AT)
VALUES ('ARCHIVED_REASON', 'Motivo de ativação arquivado (inativo p/ testes)', 'COMPANY', false, NOW(), 'seed')
ON CONFLICT DO NOTHING;

-- Registro adicional INATIVO/EMPLOYEE — usado no IT de "enable com motivo inativo/incompatível" (Story 2.2).
-- REASON_ACTIVATE_ID gerado: 6.
INSERT INTO scos.SCOS_REASON_ACTIVATE (CODE, DESCRIPTION, ENTITY_TYPE, ACTIVE, UPDATED_AT, USER_AT)
VALUES ('ARCHIVED_REASON', 'Motivo de ativação arquivado (inativo p/ testes)', 'EMPLOYEE', false, NOW(), 'seed')
ON CONFLICT DO NOTHING;

-- ============================================================
-- SCOS_REASON_INACTIVATE — motivos de encerramento definitivo
-- ============================================================
INSERT INTO scos.SCOS_REASON_INACTIVATE (CODE, DESCRIPTION, ENTITY_TYPE, ACTIVE, UPDATED_AT, USER_AT)
VALUES
    ('COMPANY_CLOSED', 'Encerramento da empresa',          'COMPANY',  true, NOW(), 'seed'),
    ('RESIGNATION',    'Desligamento/demissão',            'EMPLOYEE', true, NOW(), 'seed'),
    ('ACCOUNT_CLOSED', 'Encerramento definitivo da conta', 'LOGIN',    true, NOW(), 'seed'),
    ('VACATION',       'Férias',                           'EMPLOYEE', true, NOW(), 'seed'),
    ('MEDICAL_LEAVE',  'Licença médica',                   'EMPLOYEE', true, NOW(), 'seed')
ON CONFLICT DO NOTHING;
-- REASON_INACTIVATE_ID gerados: 1=COMPANY, 2=EMPLOYEE, 3=LOGIN, 4=VACATION/EMPLOYEE, 5=MEDICAL_LEAVE/EMPLOYEE

-- Registro adicional INATIVO — usado no IT de "disable já inativo" (ver nota em SCOS_ADDRESS_TYPE).
-- REASON_INACTIVATE_ID gerado: 6.
INSERT INTO scos.SCOS_REASON_INACTIVATE (CODE, DESCRIPTION, ENTITY_TYPE, ACTIVE, UPDATED_AT, USER_AT)
VALUES ('ARCHIVED_REASON', 'Motivo de inativação arquivado (inativo p/ testes)', 'COMPANY', false, NOW(), 'seed')
ON CONFLICT DO NOTHING;

-- Registro adicional INATIVO/EMPLOYEE — usado no IT de "disable com motivo inativo/incompatível" (Story 2.2).
-- REASON_INACTIVATE_ID gerado: 7.
INSERT INTO scos.SCOS_REASON_INACTIVATE (CODE, DESCRIPTION, ENTITY_TYPE, ACTIVE, UPDATED_AT, USER_AT)
VALUES ('ARCHIVED_REASON', 'Motivo de inativação arquivado (inativo p/ testes)', 'EMPLOYEE', false, NOW(), 'seed')
ON CONFLICT DO NOTHING;

-- ============================================================
-- SCOS_REASON_DISABLE — motivos de bloqueio temporário
-- ============================================================
INSERT INTO scos.SCOS_REASON_DISABLE (CODE, DESCRIPTION, ENTITY_TYPE, ACTIVE, UPDATED_AT, USER_AT)
VALUES
    ('UNDER_AUDIT',             'Suspensão em auditoria',                  'COMPANY',  true, NOW(), 'seed'),
    ('UNDER_AUDIT',             'Suspensão em auditoria',                  'EMPLOYEE', true, NOW(), 'seed'),
    ('INVALID_LOGIN_ATTEMPTS',  'Tentativas de login inválidas excedidas', 'LOGIN',    true, NOW(), 'seed')
ON CONFLICT DO NOTHING;
-- REASON_DISABLE_ID gerados: 1=COMPANY, 2=EMPLOYEE, 3=LOGIN

-- Registro adicional INATIVO — usado no IT de "disable já inativo" (ver nota em SCOS_ADDRESS_TYPE).
-- REASON_DISABLE_ID gerado: 4.
INSERT INTO scos.SCOS_REASON_DISABLE (CODE, DESCRIPTION, ENTITY_TYPE, ACTIVE, UPDATED_AT, USER_AT)
VALUES ('ARCHIVED_REASON', 'Motivo de bloqueio arquivado (inativo p/ testes)', 'COMPANY', false, NOW(), 'seed')
ON CONFLICT DO NOTHING;

-- Registro adicional INATIVO/EMPLOYEE — usado no IT de "block com motivo inativo/incompatível" (Story 2.2).
-- REASON_DISABLE_ID gerado: 5.
INSERT INTO scos.SCOS_REASON_DISABLE (CODE, DESCRIPTION, ENTITY_TYPE, ACTIVE, UPDATED_AT, USER_AT)
VALUES ('ARCHIVED_REASON', 'Motivo de bloqueio arquivado (inativo p/ testes)', 'EMPLOYEE', false, NOW(), 'seed')
ON CONFLICT DO NOTHING;

-- ============================================================
-- SCOS_REASON_ENABLE — motivos de desbloqueio
-- ============================================================
INSERT INTO scos.SCOS_REASON_ENABLE (CODE, DESCRIPTION, ENTITY_TYPE, ACTIVE, UPDATED_AT, USER_AT)
VALUES
    ('AUDIT_CLEARED',        'Revisão de auditoria concluída',   'COMPANY',  true, NOW(), 'seed'),
    ('AUDIT_CLEARED',        'Revisão de auditoria concluída',   'EMPLOYEE', true, NOW(), 'seed'),
    ('UNBLOCKED_BY_SUPPORT', 'Desbloqueio realizado pelo suporte','LOGIN',    true, NOW(), 'seed')
ON CONFLICT DO NOTHING;
-- REASON_ENABLE_ID gerados: 1=COMPANY, 2=EMPLOYEE, 3=LOGIN

-- Registro adicional INATIVO — usado no IT de "disable já inativo" (ver nota em SCOS_ADDRESS_TYPE).
-- REASON_ENABLE_ID gerado: 4.
INSERT INTO scos.SCOS_REASON_ENABLE (CODE, DESCRIPTION, ENTITY_TYPE, ACTIVE, UPDATED_AT, USER_AT)
VALUES ('ARCHIVED_REASON', 'Motivo de desbloqueio arquivado (inativo p/ testes)', 'COMPANY', false, NOW(), 'seed')
ON CONFLICT DO NOTHING;

-- Registro adicional INATIVO/EMPLOYEE — usado no IT de "unblock com motivo inativo/incompatível" (Story 2.2).
-- REASON_ENABLE_ID gerado: 5.
INSERT INTO scos.SCOS_REASON_ENABLE (CODE, DESCRIPTION, ENTITY_TYPE, ACTIVE, UPDATED_AT, USER_AT)
VALUES ('ARCHIVED_REASON', 'Motivo de desbloqueio arquivado (inativo p/ testes)', 'EMPLOYEE', false, NOW(), 'seed')
ON CONFLICT DO NOTHING;

-- ============================================================
-- SCOS_REASON_POSITION_CHANGE — motivos de mudança de cargo (só EMPLOYEE)
-- ============================================================
INSERT INTO scos.SCOS_REASON_POSITION_CHANGE (CODE, DESCRIPTION, ACTIVE, UPDATED_AT, USER_AT)
VALUES
    ('NEW_HIRE',  'Atribuição inicial de cargo na contratação', true, NOW(), 'seed'),
    ('PROMOTION', 'Promoção de cargo',                          true, NOW(), 'seed'),
    ('TRANSFER',  'Transferência de cargo/departamento',        true, NOW(), 'seed')
ON CONFLICT DO NOTHING;
-- REASON_POSITION_CHANGE_ID gerados: 1=NEW_HIRE, 2=PROMOTION, 3=TRANSFER

-- Registro adicional INATIVO — usado no IT de "rehire com motivo de mudança de cargo inativo" (Story 2.3).
-- Não existe endpoint de disable para este catálogo ainda (só entidade/repositório) — seed direto, mesmo padrão dos demais catálogos.
-- REASON_POSITION_CHANGE_ID gerado: 4.
INSERT INTO scos.SCOS_REASON_POSITION_CHANGE (CODE, DESCRIPTION, ACTIVE, UPDATED_AT, USER_AT)
VALUES ('ARCHIVED_REASON', 'Motivo de mudança de cargo arquivado (inativo p/ testes)', false, NOW(), 'seed')
ON CONFLICT DO NOTHING;

-- ============================================================
-- SCOS_OUTBOX_TOPIC — tópico Keycloak (Saga de sincronização de Login, Story 3.2)
-- ============================================================
INSERT INTO scos.SCOS_OUTBOX_TOPIC (TOPIC, BACKEND, TARGET_SYSTEM, DEFAULT_MAX_RETRIES, ACTIVE, UPDATED_AT, USER_AT)
VALUES ('KEYCLOAK_LOGIN_SYNC', 'DIRECT_API', 'keycloak', 3, true, NOW(), 'seed')
ON CONFLICT DO NOTHING;

-- ============================================================
-- SCOS_COMPANY — matriz SawCunhaOS
-- ============================================================
INSERT INTO scos.SCOS_COMPANY (
    PARENT_COMPANY_ID, NAME, NAME_TREATMENT, TAX_IDENTIFIER,
    LEGAL_NATURE_ID, CNAE_PRINCIPAL_ID, STATE_REGISTRATION, MUNICIPAL_REGISTRATION,
    FOUNDATION_DATE, SECTOR_OF_ACTIVITY, OBSERVATION,
    STATUS, UPDATED_AT, USER_AT
)
VALUES (
    NULL,
    'SawCunhaOS Tecnologia LTDA',
    'SawCunhaOS',
    '12345678000100',
    1,
    1,
    'ISENTO',
    NULL,
    '2020-01-01',
    'Tecnologia da Informação',
    NULL,
    'ACTIVE',
    NOW(),
    'seed'
)
ON CONFLICT DO NOTHING;
-- COMPANY_ID gerado: 1

-- ============================================================
-- SCOS_DEPARTMENT
-- ============================================================
INSERT INTO scos.SCOS_DEPARTMENT (CODE, DESCRIPTION, ACTIVE, UPDATED_AT, USER_AT)
VALUES ('TI', 'Tecnologia da Informação', true, NOW(), 'seed')
ON CONFLICT DO NOTHING;
-- DEPARTMENT_ID gerado: 1

-- ============================================================
-- SCOS_POSITION — cargo de admin de sistema no dept TI
-- ============================================================
INSERT INTO scos.SCOS_POSITION (DEPARTMENT_ID, CODE, DESCRIPTION, ACTIVE, UPDATED_AT, USER_AT)
VALUES (1, 'ADMIN_SISTEMA', 'Admin. de Sistema', true, NOW(), 'seed')
ON CONFLICT DO NOTHING;
-- POSITION_ID gerado: 1

-- ============================================================
-- SCOS_EMPLOYEE — funcionário administrador
-- Nome vem de firstName + lastName do Keycloak: "Scos Admin"
-- Email corresponde ao email do usuário scos-admin no Keycloak
-- ============================================================
INSERT INTO scos.SCOS_EMPLOYEE (
    SUPERVISOR_ID, POSITION_ID, COMPANY_ID,
    NAME, NAME_TREATMENT, TAX_IDENTIFIER, EMAIL,
    BIRTH_DATE, DATE_OF_HIRING, CONTRACT_TYPE, PROBATION_END_DATE, OBSERVATION,
    STATUS, UPDATED_AT, USER_AT
)
VALUES (
    NULL, 1, 1,
    'Scos Admin',
    'Admin',
    '00000000000',
    'admin@scos.local',
    '1990-01-01',
    '2020-01-01',
    'CLT',
    NULL,
    NULL,
    'ACTIVE',
    NOW(),
    'seed'
)
ON CONFLICT DO NOTHING;
-- EMPLOYEE_ID gerado: 1

-- ============================================================
-- SCOS_EMPLOYEE_CONTACT
-- Atenção: UK composta (PHONE, EMPLOYEE_ID) e (CONTACT_TYPE_ID, EMPLOYEE_ID)
-- ============================================================
INSERT INTO scos.SCOS_EMPLOYEE_CONTACT (EMPLOYEE_ID, PHONE, CONTACT_TYPE_ID, UPDATED_AT, USER_AT)
VALUES (1, '11999990000', 1, NOW(), 'seed')
ON CONFLICT DO NOTHING;

-- ============================================================
-- SCOS_EMPLOYEE_ADDRESS
-- GEOLOCATION é tipo POINT básico do PostgreSQL (não PostGIS)
-- Coordenadas: São Paulo, SP (-46.6333, -23.5505) → (long, lat)
-- Atenção: UK composta (ADDRESS_TYPE_ID, EMPLOYEE_ID)
-- ============================================================
INSERT INTO scos.SCOS_EMPLOYEE_ADDRESS (
    EMPLOYEE_ID_ADDRESS, EMPLOYEE_ID, ADDRESS_TYPE_ID, NUMBER, COMPLEMENT, GEOLOCATION, UPDATED_AT, USER_AT
)
VALUES (1, 1, 1, 100, NULL, '(-46.6333,-23.5505)'::point, NOW(), 'seed')
ON CONFLICT DO NOTHING;

-- ============================================================
-- SCOS_COMPANY_CONTACT
-- Atenção: UK composta (PHONE, COMPANY_ID), (EMAIL, COMPANY_ID) e (CONTACT_TYPE_ID, COMPANY_ID)
-- ============================================================
INSERT INTO scos.SCOS_COMPANY_CONTACT (
    COMPANY_ID, PHONE, EMAIL, CONTACT_TYPE_ID, RESPONSIBLE_PERSON, UPDATED_AT, USER_AT
)
VALUES (
    1,
    '1133330000',
    'contato@sawcunhaos.com.br',
    3,
    'Scos Admin',
    NOW(),
    'seed'
)
ON CONFLICT DO NOTHING;

-- ============================================================
-- SCOS_COMPANY_ADDRESS
-- Atenção: UK composta (ADDRESS_TYPE_ID, COMPANY_ID)
-- ============================================================
INSERT INTO scos.SCOS_COMPANY_ADDRESS (
    COMPANY_ID_ADDRESS, COMPANY_ID, ADDRESS_TYPE_ID, NUMBER, COMPLEMENT, GEOLOCATION, UPDATED_AT, USER_AT
)
VALUES (1, 1, 3, 1000, NULL, '(-46.6333,-23.5505)'::point, NOW(), 'seed')
ON CONFLICT DO NOTHING;

-- ============================================================
-- SCOS_SYSTEM — este sistema (organization) com identidade FIXA
-- Em produção o valor de SECRET_KEY é um UUID gerado em runtime pelo registro;
-- aqui é fixo para determinismo nos testes integrados.
-- CODE/NAME/DESCRIPTION conforme scos.registry.* do bootstrap.yml.
-- ============================================================
INSERT INTO scos.SCOS_SYSTEM (
    SYSTEM_ID, NAME, CODE, DESCRIPTION, SECRET_KEY, STATUS, VERSION, UPDATED_AT, USER_AT
)
VALUES (
    '03000000-0000-0000-0000-000000000003',
    'Scos Organization',
    'SCOS_ORGANIZATION',
    'Sistema de Gestão de Empresas e Funcionarios',
    '03000000-0000-0000-0000-0000000000AA',
    'ACTIVE',
    '1.0.0',
    NOW(),
    'seed'
)
ON CONFLICT DO NOTHING;
-- SYSTEM_ID fixo: 03000000-0000-0000-0000-000000000003

-- ============================================================
-- SCOS_RESOURCE — uma linha por permissão de ScosOrganizationPermission
-- (grupo/subgrupo/versão/data e descrições pt/en espelham o enum + bundles
-- messages_permission*). RESOURCE_ID é gerado (uuid_function default).
-- Todos vinculados ao SYSTEM_ID fixo acima.
-- ============================================================
INSERT INTO scos.SCOS_RESOURCE (
    SYSTEM_ID, CODE, DESCRIPTION_PT, DESCRIPTION_EN, ACTIVE,
    RESOURCE_GROUP, SUB_GROUP, VERSION, DEFINITION_UPDATED_AT, UPDATED_AT, USER_AT
)
VALUES
    ('03000000-0000-0000-0000-000000000003', 'GET_DEPARTMENT', 'Consultar departamento', 'Get department', true, 'Corporate', 'Department', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'CREATE_DEPARTMENT', 'Criar departamento', 'Create department', true, 'Corporate', 'Department', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'UPDATE_DEPARTMENT', 'Atualizar departamento', 'Update department', true, 'Corporate', 'Department', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'DELETE_DEPARTMENT', 'Excluir departamento', 'Delete department', true, 'Corporate', 'Department', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'ENABLE_DEPARTMENT', 'Habilitar departamento', 'Enable department', true, 'Corporate', 'Department', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'DISABLE_DEPARTMENT', 'Desabilitar departamento', 'Disable department', true, 'Corporate', 'Department', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'GET_POSITION', 'Consultar cargo', 'Get position', true, 'Corporate', 'Position', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'CREATE_POSITION', 'Criar cargo', 'Create position', true, 'Corporate', 'Position', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'UPDATE_POSITION', 'Atualizar cargo', 'Update position', true, 'Corporate', 'Position', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'DELETE_POSITION', 'Excluir cargo', 'Delete position', true, 'Corporate', 'Position', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'ENABLE_POSITION', 'Habilitar cargo', 'Enable position', true, 'Corporate', 'Position', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'DISABLE_POSITION', 'Desabilitar cargo', 'Disable position', true, 'Corporate', 'Position', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'GET_LOGIN', 'Consultar login', 'Get login', true, 'Access', 'Login', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'GET_LOGIN_INFO', 'Consultar informações de login', 'Get login info', true, 'Access', 'Login', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'CREATE_LOGIN', 'Criar login', 'Create login', true, 'Access', 'Login', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'UPDATE_LOGIN', 'Atualizar login', 'Update login', true, 'Access', 'Login', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'UPDATE_LOGIN_STATUS', 'Atualizar status do login', 'Update login status', true, 'Access', 'Login', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'DELETE_LOGIN', 'Excluir login', 'Delete login', true, 'Access', 'Login', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'GET_PROFILE', 'Consultar perfil', 'Get profile', true, 'Access', 'Profile', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'CREATE_PROFILE', 'Criar perfil', 'Create profile', true, 'Access', 'Profile', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'UPDATE_PROFILE', 'Atualizar perfil', 'Update profile', true, 'Access', 'Profile', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'DELETE_PROFILE', 'Excluir perfil', 'Delete profile', true, 'Access', 'Profile', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'ENABLE_PROFILE', 'Habilitar perfil', 'Enable profile', true, 'Access', 'Profile', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'DISABLE_PROFILE', 'Desabilitar perfil', 'Disable profile', true, 'Access', 'Profile', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'GET_RESOURCE', 'Consultar recurso', 'Get resource', true, 'Access', 'Resource', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'GET_OUTBOX_EVENT', 'Consultar evento do outbox', 'Get outbox event', true, 'Outbox', 'Outbox', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'RETRY_OUTBOX_EVENT', 'Reprocessar evento do outbox', 'Retry outbox event', true, 'Outbox', 'Outbox', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'GET_OUTBOX_EVENT_DEAD_LETTER', 'Consultar dead letter do outbox', 'Get outbox event dead letter', true, 'Outbox', 'Outbox', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'GET_COMPANY', 'Consultar empresa', 'Get company', true, 'Corporate', 'Company', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'CREATE_COMPANY', 'Criar empresa', 'Create company', true, 'Corporate', 'Company', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'UPDATE_COMPANY', 'Atualizar empresa', 'Update company', true, 'Corporate', 'Company', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'DELETE_COMPANY', 'Excluir empresa', 'Delete company', true, 'Corporate', 'Company', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'ENABLE_COMPANY', 'Habilitar empresa', 'Enable company', true, 'Corporate', 'Company', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'DISABLE_COMPANY', 'Desabilitar empresa', 'Disable company', true, 'Corporate', 'Company', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'GET_COMPANY_CONTACT', 'Consultar contato da empresa', 'Get company contact', true, 'Corporate', 'Company Contact', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'CREATE_COMPANY_CONTACT', 'Criar contato da empresa', 'Create company contact', true, 'Corporate', 'Company Contact', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'UPDATE_COMPANY_CONTACT', 'Atualizar contato da empresa', 'Update company contact', true, 'Corporate', 'Company Contact', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'DELETE_COMPANY_CONTACT', 'Excluir contato da empresa', 'Delete company contact', true, 'Corporate', 'Company Contact', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'GET_COMPANY_ADDRESS', 'Consultar endereço da empresa', 'Get company address', true, 'Corporate', 'Company Address', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'CREATE_COMPANY_ADDRESS', 'Criar endereço da empresa', 'Create company address', true, 'Corporate', 'Company Address', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'UPDATE_COMPANY_ADDRESS', 'Atualizar endereço da empresa', 'Update company address', true, 'Corporate', 'Company Address', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'DELETE_COMPANY_ADDRESS', 'Excluir endereço da empresa', 'Delete company address', true, 'Corporate', 'Company Address', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'GET_EMPLOYEE', 'Consultar colaborador', 'Get employee', true, 'Corporate', 'Employee', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'CREATE_EMPLOYEE', 'Criar colaborador', 'Create employee', true, 'Corporate', 'Employee', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'UPDATE_EMPLOYEE', 'Atualizar colaborador', 'Update employee', true, 'Corporate', 'Employee', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'DELETE_EMPLOYEE', 'Excluir colaborador', 'Delete employee', true, 'Corporate', 'Employee', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'ENABLE_EMPLOYEE', 'Habilitar colaborador', 'Enable employee', true, 'Corporate', 'Employee', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'DISABLE_EMPLOYEE', 'Desabilitar colaborador', 'Disable employee', true, 'Corporate', 'Employee', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'TRANSFER_EMPLOYEE', 'Transferir colaborador', 'Transfer employee', true, 'Corporate', 'Employee', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'GET_EMPLOYEE_CONTACT', 'Consultar contato do colaborador', 'Get employee contact', true, 'Corporate', 'Employee Contact', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'CREATE_EMPLOYEE_CONTACT', 'Criar contato do colaborador', 'Create employee contact', true, 'Corporate', 'Employee Contact', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'UPDATE_EMPLOYEE_CONTACT', 'Atualizar contato do colaborador', 'Update employee contact', true, 'Corporate', 'Employee Contact', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'DELETE_EMPLOYEE_CONTACT', 'Excluir contato do colaborador', 'Delete employee contact', true, 'Corporate', 'Employee Contact', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'GET_EMPLOYEE_ADDRESS', 'Consultar endereço do colaborador', 'Get employee address', true, 'Corporate', 'Employee Address', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'CREATE_EMPLOYEE_ADDRESS', 'Criar endereço do colaborador', 'Create employee address', true, 'Corporate', 'Employee Address', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'UPDATE_EMPLOYEE_ADDRESS', 'Atualizar endereço do colaborador', 'Update employee address', true, 'Corporate', 'Employee Address', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'DELETE_EMPLOYEE_ADDRESS', 'Excluir endereço do colaborador', 'Delete employee address', true, 'Corporate', 'Employee Address', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'GET_CONFIGURATION', 'Consultar configuração', 'Get configuration', true, 'Configuration', 'Configuration', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'UPDATE_CONFIGURATION', 'Atualizar configuração', 'Update configuration', true, 'Configuration', 'Configuration', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'GET_ADDRESS_TYPE', 'Consultar tipo de endereço', 'Get address type', true, 'Corporate', 'Address Type', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'CREATE_ADDRESS_TYPE', 'Criar tipo de endereço', 'Create address type', true, 'Corporate', 'Address Type', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'UPDATE_ADDRESS_TYPE', 'Atualizar tipo de endereço', 'Update address type', true, 'Corporate', 'Address Type', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'ENABLE_ADDRESS_TYPE', 'Habilitar tipo de endereço', 'Enable address type', true, 'Corporate', 'Address Type', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'DISABLE_ADDRESS_TYPE', 'Desabilitar tipo de endereço', 'Disable address type', true, 'Corporate', 'Address Type', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'GET_CONTACT_TYPE', 'Consultar tipo de contato', 'Get contact type', true, 'Corporate', 'Contact Type', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'CREATE_CONTACT_TYPE', 'Criar tipo de contato', 'Create contact type', true, 'Corporate', 'Contact Type', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'UPDATE_CONTACT_TYPE', 'Atualizar tipo de contato', 'Update contact type', true, 'Corporate', 'Contact Type', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'ENABLE_CONTACT_TYPE', 'Habilitar tipo de contato', 'Enable contact type', true, 'Corporate', 'Contact Type', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'DISABLE_CONTACT_TYPE', 'Desabilitar tipo de contato', 'Disable contact type', true, 'Corporate', 'Contact Type', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'GET_CNAE', 'Consultar CNAE', 'Get CNAE', true, 'Corporate', 'Cnae', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'CREATE_CNAE', 'Criar CNAE', 'Create CNAE', true, 'Corporate', 'Cnae', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'UPDATE_CNAE', 'Atualizar CNAE', 'Update CNAE', true, 'Corporate', 'Cnae', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'DELETE_CNAE', 'Excluir CNAE', 'Delete CNAE', true, 'Corporate', 'Cnae', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'GET_LEGAL_NATURE', 'Consultar natureza jurídica', 'Get legal nature', true, 'Corporate', 'Legal Nature', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'CREATE_LEGAL_NATURE', 'Criar natureza jurídica', 'Create legal nature', true, 'Corporate', 'Legal Nature', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'UPDATE_LEGAL_NATURE', 'Atualizar natureza jurídica', 'Update legal nature', true, 'Corporate', 'Legal Nature', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'DELETE_LEGAL_NATURE', 'Excluir natureza jurídica', 'Delete legal nature', true, 'Corporate', 'Legal Nature', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'GET_REASON_ACTIVATE', 'Consultar motivo de ativação', 'Get activation reason', true, 'Access', 'Reason Activate', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'CREATE_REASON_ACTIVATE', 'Criar motivo de ativação', 'Create activation reason', true, 'Access', 'Reason Activate', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'UPDATE_REASON_ACTIVATE', 'Atualizar motivo de ativação', 'Update activation reason', true, 'Access', 'Reason Activate', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'ENABLE_REASON_ACTIVATE', 'Habilitar motivo de ativação', 'Enable activation reason', true, 'Access', 'Reason Activate', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'DISABLE_REASON_ACTIVATE', 'Desabilitar motivo de ativação', 'Disable activation reason', true, 'Access', 'Reason Activate', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'GET_REASON_INACTIVATE', 'Consultar motivo de inativação', 'Get inactivation reason', true, 'Access', 'Reason Inactivate', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'CREATE_REASON_INACTIVATE', 'Criar motivo de inativação', 'Create inactivation reason', true, 'Access', 'Reason Inactivate', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'UPDATE_REASON_INACTIVATE', 'Atualizar motivo de inativação', 'Update inactivation reason', true, 'Access', 'Reason Inactivate', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'ENABLE_REASON_INACTIVATE', 'Habilitar motivo de inativação', 'Enable inactivation reason', true, 'Access', 'Reason Inactivate', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'DISABLE_REASON_INACTIVATE', 'Desabilitar motivo de inativação', 'Disable inactivation reason', true, 'Access', 'Reason Inactivate', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'GET_REASON_DISABLE', 'Consultar motivo de bloqueio', 'Get block reason', true, 'Access', 'Reason Disable', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'CREATE_REASON_DISABLE', 'Criar motivo de bloqueio', 'Create block reason', true, 'Access', 'Reason Disable', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'UPDATE_REASON_DISABLE', 'Atualizar motivo de bloqueio', 'Update block reason', true, 'Access', 'Reason Disable', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'ENABLE_REASON_DISABLE', 'Habilitar motivo de bloqueio', 'Enable block reason', true, 'Access', 'Reason Disable', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'DISABLE_REASON_DISABLE', 'Desabilitar motivo de bloqueio', 'Disable block reason', true, 'Access', 'Reason Disable', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'GET_REASON_ENABLE', 'Consultar motivo de desbloqueio', 'Get unblock reason', true, 'Access', 'Reason Enable', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'CREATE_REASON_ENABLE', 'Criar motivo de desbloqueio', 'Create unblock reason', true, 'Access', 'Reason Enable', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'UPDATE_REASON_ENABLE', 'Atualizar motivo de desbloqueio', 'Update unblock reason', true, 'Access', 'Reason Enable', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'ENABLE_REASON_ENABLE', 'Habilitar motivo de desbloqueio', 'Enable unblock reason', true, 'Access', 'Reason Enable', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'DISABLE_REASON_ENABLE', 'Desabilitar motivo de desbloqueio', 'Disable unblock reason', true, 'Access', 'Reason Enable', '1.0.0', '2026-07-09', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'GET_LOGIN_APPROVAL_REQUEST', 'Consultar solicitação de aprovação de login', 'Get login approval request', true, 'Access', 'Login Approval Request', '1.0.0', '2026-08-16', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'DECIDE_LOGIN_APPROVAL_REQUEST', 'Aprovar ou rejeitar solicitação de aprovação de login', 'Approve or reject login approval request', true, 'Access', 'Login Approval Request', '1.0.0', '2026-08-16', NOW(), 'seed'),
    ('03000000-0000-0000-0000-000000000003', 'APPROVE_SYSTEM_ACCESS', 'Decidir solicitações no nível grupo de acesso ao sistema (válvula de última instância)', 'Decide requests at the system access group level (last-resort valve)', true, 'Access', 'Login Approval Request', '1.0.0', '2026-08-16', NOW(), 'seed')
ON CONFLICT DO NOTHING;

-- ============================================================
-- SCOS_PROFILE
--   1 — ADMIN       : acesso total (funcionário administrador)
--   2 — INTEGRATION : conta de serviço sem vínculo com funcionário
-- ============================================================
INSERT INTO scos.SCOS_PROFILE (CODE, DESCRIPTION, ACTIVE, UPDATED_AT, USER_AT)
VALUES
    ('ADMIN',       'Administrador com acesso total ao sistema',  true, NOW(), 'seed'),
    ('INTEGRATION', 'Perfil para integrações entre sistemas',     true, NOW(), 'seed')
ON CONFLICT DO NOTHING;
-- PROFILE_ID: 1 = ADMIN, 2 = INTEGRATION

-- ============================================================
-- SCOS_LOGIN
--
--   scos-admin → EMPLOYEE (vinculado ao funcionário 1)
--     EXTERNAL_ID = 01000000-0000-0000-0000-000000000001
--
--   scos-api → SERVICE (sem funcionário — conta de integração)
--     EXTERNAL_ID = 02000000-0000-0000-0000-000000000002
--
--   inside.admin → EMPLOYEE (vinculado ao funcionário 1) - identidade fixa do JWT de teste
--     (ScosJwtTestSupport.bearer/BEAR_TOKEN_VALID, preferred_username="inside.admin"). O gRPC
--     ValidateAuthorityService mockado sempre devolve login="inside.admin" (fullAdminAuthority),
--     então esta linha precisa existir para LoginRepository.findByLogin("inside.admin") resolver
--     o Login de quem está autenticado nos testes (Story 3.2 - abertura da LoginApprovalRequest
--     na criação, decisão de aprovação). Só no seed de teste - não existe em produção.
--     EXTERNAL_ID = 04000000-0000-0000-0000-000000000004
-- ============================================================
INSERT INTO scos.SCOS_LOGIN (
    PROFILE_ID, EMPLOYEE_ID, EXTERNAL_ID, LOGIN, STATUS, TYPE, UPDATED_AT, USER_AT
)
VALUES
    (1, 1,    '01000000-0000-0000-0000-000000000001', 'scos-admin',   'ACTIVE', 'EMPLOYEE', NOW(), 'seed'),
    (2, NULL, '02000000-0000-0000-0000-000000000002', 'scos-api',     'ACTIVE', 'SERVICE',  NOW(), 'seed'),
    (1, 1,    '04000000-0000-0000-0000-000000000004', 'inside.admin', 'ACTIVE', 'EMPLOYEE', NOW(), 'seed')
ON CONFLICT DO NOTHING;
-- LOGIN_ID: 1 = scos-admin, 2 = scos-api, 3 = inside.admin

-- ============================================================
-- SCOS_EMPLOYEE_POSITION_HISTORY — cargo inicial (bootstrap)
-- SCOS_EMPLOYEE.POSITION_ID é cache sincronizado por trigger a partir daqui;
-- a linha principal já sobe com POSITION_ID=1 e esta linha só confirma/audita
-- a atribuição. Idempotente via índice único parcial (EMPLOYEE_ID) WHERE END_DATE IS NULL.
-- ============================================================
INSERT INTO scos.SCOS_EMPLOYEE_POSITION_HISTORY (
    EMPLOYEE_ID, POSITION_ID, START_DATE, END_DATE, REASON_POSITION_CHANGE_ID, USER_AT
)
VALUES (1, 1, '2020-01-01', NULL, 1, 'seed')
ON CONFLICT DO NOTHING;

-- ============================================================
-- Histórico inicial de status (bootstrap)
-- SCOS_COMPANY/EMPLOYEE/LOGIN.STATUS são cache sincronizados por trigger a partir
-- das tabelas *_STATUS_HISTORY; as linhas principais já sobem com STATUS='ACTIVE'
-- e estas linhas só confirmam/auditam a criação. Sem UK própria — idempotência
-- via WHERE NOT EXISTS (não é seguro repetir com ON CONFLICT DO NOTHING aqui).
-- ============================================================
INSERT INTO scos.SCOS_COMPANY_STATUS_HISTORY (COMPANY_ID, STATUS, REASON_ACTIVATE_ID, USER_AT)
SELECT 1, 'ACTIVE', 1, 'seed'
WHERE NOT EXISTS (SELECT 1 FROM scos.SCOS_COMPANY_STATUS_HISTORY WHERE COMPANY_ID = 1);

INSERT INTO scos.SCOS_EMPLOYEE_STATUS_HISTORY (EMPLOYEE_ID, STATUS, REASON_ACTIVATE_ID, USER_AT)
SELECT 1, 'ACTIVE', 2, 'seed'
WHERE NOT EXISTS (SELECT 1 FROM scos.SCOS_EMPLOYEE_STATUS_HISTORY WHERE EMPLOYEE_ID = 1);

INSERT INTO scos.SCOS_LOGIN_STATUS_HISTORY (LOGIN_ID, STATUS, REASON_ACTIVATE_ID, USER_AT)
SELECT 1, 'ACTIVE', 4, 'seed'
WHERE NOT EXISTS (SELECT 1 FROM scos.SCOS_LOGIN_STATUS_HISTORY WHERE LOGIN_ID = 1);

INSERT INTO scos.SCOS_LOGIN_STATUS_HISTORY (LOGIN_ID, STATUS, REASON_ACTIVATE_ID, USER_AT)
SELECT 2, 'ACTIVE', 4, 'seed'
WHERE NOT EXISTS (SELECT 1 FROM scos.SCOS_LOGIN_STATUS_HISTORY WHERE LOGIN_ID = 2);

-- ============================================================
-- SCOS_CONFIGURATION — parâmetros básicos do sistema
-- Chaves conforme br.com.sawcunhaos.organization.domain.configuration.internal.ConfigurationKey
-- ============================================================
INSERT INTO scos.SCOS_CONFIGURATION (CONFIGURATION_ID, VALUE, TYPE, UPDATED_AT, USER_AT)
VALUES
    ('EMPLOYEE_MIN_AGE',              '18',                 'INTEGER', NOW(), 'seed'),
    ('COMPANY_HIERARCHY_MAX_DEPTH',   '10',                 'INTEGER', NOW(), 'seed'),
    ('LOGIN_INACTIVITY_TIMEOUT_DAYS', '90',                 'INTEGER', NOW(), 'seed'),
    ('EMPLOYEE_EMAIL_DOMAIN',         'sawcunhaos.com.br',  'STRING',  NOW(), 'seed'),
    ('DEFAULT_COMPANY_ID',            '1',                  'INTEGER', NOW(), 'seed')
ON CONFLICT DO NOTHING;

-- ============================================================
-- SCOS_PROFILE_RESOURCE — vincula todos os recursos a todos os perfis
--
-- Os recursos já foram semeados acima (seção SCOS_RESOURCE), então este
-- CROSS JOIN roda no mesmo script — sem depender de a aplicação ter subido.
--
-- Lógica: CROSS JOIN perfis × recursos ativos → todas as combinações.
-- ON CONFLICT DO NOTHING garante idempotência em re-execuções.
-- ============================================================
INSERT INTO scos.SCOS_PROFILE_RESOURCE (PROFILE_ID, RESOURCE_ID, CREATED_AT, USER_AT)
SELECT
    p.PROFILE_ID,
    r.RESOURCE_ID,
    NOW(),
    'seed'
FROM scos.SCOS_PROFILE p
CROSS JOIN scos.SCOS_RESOURCE r
WHERE r.ACTIVE = true
ON CONFLICT DO NOTHING;

-- ============================================================
-- Refresh das views materializadas de autoridade (vw_login_context, vw_authority_response)
-- Em produção o refresh é feito pelo pg_cron (scos.refresh_authority_views(), a cada 30 min) -
-- nos testes precisa ser explícito, senão as views ficam com o snapshot da última migration,
-- sem os Logins/permissões recém-semeados (usado por GET /v1/login-approval-requests/system-access-approvers).
-- Ordem importa: vw_authority_response é agregada a partir de vw_login_context.
-- ============================================================
REFRESH MATERIALIZED VIEW scos.vw_login_context;
REFRESH MATERIALIZED VIEW scos.vw_authority_response;
