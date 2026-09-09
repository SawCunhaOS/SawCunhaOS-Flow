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
-- REASON_INACTIVATE_ID gerados: 1=COMPANY, 2=EMPLOYEE, 3=LOGIN

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

-- ============================================================
-- SCOS_OUTBOX_TOPIC — tópico Keycloak (Saga de sincronização de Login, Story 3.2)
-- BACKEND=DIRECT_API é placeholder - ainda não existe dispatcher real (fora de escopo).
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
-- ============================================================
INSERT INTO scos.SCOS_LOGIN (
    PROFILE_ID, EMPLOYEE_ID, EXTERNAL_ID, LOGIN, STATUS, TYPE, UPDATED_AT, USER_AT
)
VALUES
    (1, 1,    '01000000-0000-0000-0000-000000000001', 'scos-admin', 'ACTIVE', 'EMPLOYEE', NOW(), 'seed'),
    (2, NULL, '02000000-0000-0000-0000-000000000002', 'scos-api',   'ACTIVE', 'SERVICE',  NOW(), 'seed')
ON CONFLICT DO NOTHING;
-- LOGIN_ID: 1 = scos-admin, 2 = scos-api

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
