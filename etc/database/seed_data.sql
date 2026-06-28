-- =============================================================================
-- SCOS — Dados de semente para desenvolvimento
--
-- Executar MANUALMENTE após o Liquibase ter criado o schema e as tabelas.
-- Script é idempotente (ON CONFLICT DO NOTHING).
--
-- Pré-requisitos:
--   - Keycloak com realm Scos importado (etc/infra/keycloak/Scos_Realm.json)
--   - Banco scos com schema scos criado pelo Liquibase
--
-- Usuários Keycloak referenciados:
--   scos-admin  → KEYCLOAK_ID = 01000000-0000-0000-0000-000000000001
--   scos-api    → KEYCLOAK_ID = 02000000-0000-0000-0000-000000000002
--
-- NÃO inserir: SCOS_SYSTEM e SCOS_RESOURCE (populados pela aplicação ao subir)
--
-- Ordem de execução:
--   1. Este script completo  → cria empresa, funcionário, perfis, logins, configurações
--   2. Subir a aplicação     → ela popula SCOS_SYSTEM e SCOS_RESOURCE
--   3. Seção SCOS_PROFILE_RESOURCE no final deste script → vincula recursos aos perfis
-- =============================================================================

SET search_path TO scos, public;

-- ============================================================
-- SCOS_COMPANY — matriz SawCunhaOS
-- ============================================================
INSERT INTO scos.SCOS_COMPANY (
    PARENT_COMPANY_ID, NAME, NAME_TREATMENT, TAX_IDENTIFIER,
    FOUNDATION_DATE, SECTOR_OF_ACTIVITY, OBSERVATION,
    STATUS, UPDATED_AT, USER_AT
)
VALUES (
    NULL,
    'SawCunhaOS Tecnologia LTDA',
    'SawCunhaOS',
    '12345678000100',
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
    BIRTH_DATE, DATE_OF_HIRING, OBSERVATION,
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
    NULL,
    'ACTIVE',
    NOW(),
    'seed'
)
ON CONFLICT DO NOTHING;
-- EMPLOYEE_ID gerado: 1

-- ============================================================
-- SCOS_EMPLOYEE_CONTACT
-- Atenção: UK global em PHONE e TYPE — apenas um por tabela
-- ============================================================
INSERT INTO scos.SCOS_EMPLOYEE_CONTACT (EMPLOYEE_ID, PHONE, TYPE, UPDATED_AT, USER_AT)
VALUES (1, '11999990000', 'MOBILE', NOW(), 'seed')
ON CONFLICT DO NOTHING;

-- ============================================================
-- SCOS_EMPLOYEE_ADDRESS
-- GEOLOCATION é tipo POINT básico do PostgreSQL (não PostGIS)
-- Coordenadas: São Paulo, SP (-46.6333, -23.5505) → (long, lat)
-- Atenção: UK global em TYPE — apenas um HOME por tabela
-- ============================================================
INSERT INTO scos.SCOS_EMPLOYEE_ADDRESS (
    EMPLOYEE_ID_ADDRESS, EMPLOYEE_ID, TYPE, NUMBER, COMPLEMENT, GEOLOCATION, UPDATED_AT, USER_AT
)
VALUES (1, 1, 'HOME', 100, NULL, '(-46.6333,-23.5505)'::point, NOW(), 'seed')
ON CONFLICT DO NOTHING;

-- ============================================================
-- SCOS_COMPANY_CONTACT
-- Atenção: UK global em PHONE, EMAIL e TYPE
-- ============================================================
INSERT INTO scos.SCOS_COMPANY_CONTACT (
    COMPANY_ID, PHONE, EMAIL, TYPE, RESPONSIBLE_PERSON, UPDATED_AT, USER_AT
)
VALUES (
    1,
    '1133330000',
    'contato@sawcunhaos.com.br',
    'COMMERCIAL',
    'Scos Admin',
    NOW(),
    'seed'
)
ON CONFLICT DO NOTHING;

-- ============================================================
-- SCOS_COMPANY_ADDRESS
-- Atenção: UK global em TYPE
-- ============================================================
INSERT INTO scos.SCOS_COMPANY_ADDRESS (
    COMPANY_ID_ADDRESS, COMPANY_ID, TYPE, NUMBER, COMPLEMENT, GEOLOCATION, UPDATED_AT, USER_AT
)
VALUES (1, 1, 'COMMERCIAL', 1000, NULL, '(-46.6333,-23.5505)'::point, NOW(), 'seed')
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
--     KEYCLOAK_ID = 01000000-0000-0000-0000-000000000001
--
--   scos-api → SERVICE (sem funcionário — conta de integração)
--     KEYCLOAK_ID = 02000000-0000-0000-0000-000000000002
-- ============================================================
INSERT INTO scos.SCOS_LOGIN (
    PROFILE_ID, EMPLOYEE_ID, KEYCLOAK_ID, LOGIN, STATUS, TYPE, UPDATED_AT, USER_AT
)
VALUES
    (1, 1,    '01000000-0000-0000-0000-000000000001', 'scos-admin', 'ACTIVE', 'EMPLOYEE', NOW(), 'seed'),
    (2, NULL, '02000000-0000-0000-0000-000000000002', 'scos-api',   'ACTIVE', 'SERVICE',  NOW(), 'seed')
ON CONFLICT DO NOTHING;

-- ============================================================
-- SCOS_CONFIGURATION — parâmetros básicos do sistema
-- ============================================================
INSERT INTO scos.SCOS_CONFIGURATION (CONFIGURATION_ID, VALUE, TYPE, UPDATED_AT, USER_AT)
VALUES
    ('KEYCLOAK_REALM',       'Scos',  'STRING',  NOW(), 'seed'),
    ('TOKEN_EXPIRY_MINUTES', '480',   'INTEGER', NOW(), 'seed'),
    ('MAX_LOGIN_ATTEMPTS',   '5',     'INTEGER', NOW(), 'seed')
ON CONFLICT DO NOTHING;

-- ============================================================
-- SCOS_PROFILE_RESOURCE — vincula todos os recursos a todos os perfis
--
-- EXECUTAR SOMENTE APÓS a aplicação ter subido ao menos uma vez
-- e populado a tabela SCOS_RESOURCE.
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
-- Verificação rápida pós-insert
-- ============================================================
SELECT
    l.login_id,
    l.login,
    l.type,
    l.status,
    l.keycloak_id,
    e.name   AS employee_name,
    p.code   AS profile,
    c.name   AS company,
    COUNT(pr.resource_id) AS total_permissoes
FROM scos.SCOS_LOGIN l
         JOIN       scos.SCOS_PROFILE          p  ON p.profile_id  = l.profile_id
         LEFT JOIN  scos.SCOS_EMPLOYEE         e  ON e.employee_id = l.employee_id
         LEFT JOIN  scos.SCOS_COMPANY          c  ON c.company_id  = e.company_id
         LEFT JOIN  scos.SCOS_PROFILE_RESOURCE pr ON pr.profile_id = p.profile_id
GROUP BY l.login_id, l.login, l.type, l.status, l.keycloak_id,
         e.name, p.code, c.name
ORDER BY l.login_id;
