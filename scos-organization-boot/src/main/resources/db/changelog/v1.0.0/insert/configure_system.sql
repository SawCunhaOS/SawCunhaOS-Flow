-- =============================================================================
-- CONFIGURAÇÃO INICIAL DO SISTEMA SCOS
-- Utiliza CTEs encadeadas para garantir integridade referencial
--
-- Alterações em relação à versão anterior:
--   scos_company:
--     - Removida coluna active (modelo usa apenas status)
--     - Corrigido tax_identifier para formato CNPJ (14 dígitos)
--
--   scos_employee:
--     - Removida coluna active, adicionado status = 'ACTIVE'
--     - Corrigido birth_date para 1990-01-01 (regra: idade mínima de 14 anos)
--
--   scos_profile:
--     - Removida coluna features (permissões migradas para scos_profile_resource)
--
--   scos_login:
--     - Removidas colunas password, salt e date_last_change_password
--       (autenticação gerenciada pelo Keycloak)
--     - Adicionados keycloak_id (NULL até Saga concluir) e type = 'EMPLOYEE'
--     - Status corrigido para PENDING (aguarda integração com Keycloak)
--
--   Novos:
--     - scos_system          cadastro do sistema SCOS_ORGANIZATION
--     - scos_resource        58 recursos do catálogo de permissões
--     - scos_profile_resource vínculo do perfil ADMIN com todos os recursos
--     - scos_integration_keycloak disparo da Saga de criação do usuário
-- =============================================================================

WITH company_insert AS (
INSERT INTO scos.scos_company
(
    company_id,
    name,
    name_treatment,
    tax_identifier,
    foundation_date,
    sector_of_activity,
    parent_company_id,
    observation,
    status,
    created_at,
    updated_at,
    user_at
)
VALUES
    (
    nextval('scos.SEQ_COMPANY_ID'),
    'Scos Flow',
    'Scos Softwares Flow System',
    '00000000000000',  -- CNPJ placeholder (14 dígitos)
    '2025-01-01',
    'INFO',
    NULL,
    'Cadastro inicial do sistema',
    'ACTIVE',
    now(),
    now(),
    'Migration'
    )
    RETURNING company_id, name
    ),
    department_insert AS (
INSERT INTO scos.scos_department
(
    department_id,
    code,
    description,
    active,
    created_at,
    updated_at,
    user_at
)
VALUES
    (
    nextval('scos.SEQ_DEPARTMENT_ID'),
    'ADMIN',
    'Administrador do Sistema',
    true,
    now(),
    now(),
    'Migration'
    )
    RETURNING department_id
    ),
    position_insert AS (
INSERT INTO scos.scos_position
(
    position_id,
    department_id,
    code,
    description,
    active,
    created_at,
    updated_at,
    user_at
)
SELECT
    nextval('scos.SEQ_POSITION_ID'),
    d.department_id,
    'ADMIN',
    'Administrador MASTER',
    true,
    now(),
    now(),
    'Migration'
FROM department_insert d
    RETURNING position_id
    ),
    employee_insert AS (
INSERT INTO scos.scos_employee
(
    employee_id,
    company_id,
    position_id,
    name,
    name_treatment,
    tax_identifier,
    email,
    birth_date,
    date_of_hiring,
    observation,
    status,
    supervisor_id,
    created_at,
    updated_at,
    user_at
)
SELECT
    nextval('scos.SEQ_EMPLOYEE_ID'),
    c.company_id,
    p.position_id,
    'Scos Softwares',
    'Flow',
    '00000000000',  -- CPF placeholder (11 dígitos)
    'scos_flow@scos.com',
    '1990-01-01',   -- Corrigido: birth_date deve garantir idade mínima de 14 anos
    '2025-01-01',
    'Administrador do Sistema',
    'ACTIVE',
    NULL,
    now(),
    now(),
    'Migration'
FROM company_insert c, position_insert p
    RETURNING employee_id
    ),
-- -------------------------------------------------------------------------
-- Cadastra o sistema SCOS_ORGANIZATION no registry de sistemas
-- Sistema interno não necessita de secret_key
-- -------------------------------------------------------------------------
    system_insert AS (
INSERT INTO scos.scos_system
(
    system_id,
    code,
    description,
    secret_key,
    status,
    created_at,
    updated_at,
    user_at
)
VALUES
    (
    gen_random_uuid(),
    'SCOS_ORGANIZATION',
    'Organization and company management system for SCOS',
    gen_random_uuid(),
    'ACTIVE',
    now(),
    now(),
    'Migration'
    )
    RETURNING system_id
    ),
    profile_insert AS (
INSERT INTO scos.scos_profile
(
    profile_id,
    code,
    description,
    active,
    created_at,
    updated_at,
    user_at
)
VALUES
    (
    nextval('scos.SEQ_PROFILE_ID'),
    'ADMIN',
    'Administrador',
    true,
    now(),
    now(),
    'Migration'
    )
    RETURNING profile_id
    ),
    login_insert AS (
INSERT INTO scos.scos_login
(
    login_id,
    profile_id,
    employee_id,
    keycloak_id,
    login,
    type,
    status,
    created_at,
    updated_at,
    user_at
)
SELECT
    nextval('scos.SEQ_LOGIN_ID'),
    pr.profile_id,
    e.employee_id,
    NULL,           -- Preenchido pela Saga após integração com Keycloak
    'scosadmin',
    'EMPLOYEE',
    'PENDING',      -- Aguarda Saga Keycloak concluir com TYPE=CREATE
    now(),
    now(),
    'Migration'
FROM profile_insert pr, employee_insert e
    RETURNING login_id
    ),
-- -------------------------------------------------------------------------
-- Cadastra os 58 recursos do catálogo de permissões do SCOS_ORGANIZATION
-- Cada recurso representa uma permissão que pode ser atribuída a perfis
-- -------------------------------------------------------------------------
    resource_insert AS (
INSERT INTO scos.scos_resource
(
    resource_id,
    system_id,
    code,
    description,
    active,
    created_at,
    updated_at,
    user_at
)
SELECT
    gen_random_uuid(),
    s.system_id,
    v.code,
    v.description,
    true,
    now(),
    now(),
    'Migration'
FROM system_insert s,
    (VALUES
    -- Company
    ('GET_COMPANY',             'Visualizar empresa'),
    ('CREATE_COMPANY',          'Cadastrar empresa'),
    ('UPDATE_COMPANY',          'Atualizar empresa'),
    ('DELETE_COMPANY',          'Excluir empresa'),
    ('ENABLE_COMPANY',          'Ativar empresa'),
    ('DISABLE_COMPANY',         'Inativar empresa'),
    -- Company Contact
    ('GET_COMPANY_CONTACT',     'Visualizar contato de empresa'),
    ('CREATE_COMPANY_CONTACT',  'Cadastrar contato de empresa'),
    ('UPDATE_COMPANY_CONTACT',  'Atualizar contato de empresa'),
    ('DELETE_COMPANY_CONTACT',  'Excluir contato de empresa'),
    -- Company Address
    ('GET_COMPANY_ADDRESS',     'Visualizar endereço de empresa'),
    ('CREATE_COMPANY_ADDRESS',  'Cadastrar endereço de empresa'),
    ('UPDATE_COMPANY_ADDRESS',  'Atualizar endereço de empresa'),
    ('DELETE_COMPANY_ADDRESS',  'Excluir endereço de empresa'),
    -- Department
    ('GET_DEPARTMENT',          'Visualizar departamento'),
    ('CREATE_DEPARTMENT',       'Cadastrar departamento'),
    ('UPDATE_DEPARTMENT',       'Atualizar departamento'),
    ('DELETE_DEPARTMENT',       'Excluir departamento'),
    ('ENABLE_DEPARTMENT',       'Ativar departamento'),
    ('DISABLE_DEPARTMENT',      'Inativar departamento'),
    -- Position
    ('GET_POSITION',            'Visualizar cargo'),
    ('CREATE_POSITION',         'Cadastrar cargo'),
    ('UPDATE_POSITION',         'Atualizar cargo'),
    ('DELETE_POSITION',         'Excluir cargo'),
    ('ENABLE_POSITION',         'Ativar cargo'),
    ('DISABLE_POSITION',        'Inativar cargo'),
    -- Employee
    ('GET_EMPLOYEE',            'Visualizar funcionário'),
    ('CREATE_EMPLOYEE',         'Cadastrar funcionário'),
    ('UPDATE_EMPLOYEE',         'Atualizar funcionário'),
    ('DELETE_EMPLOYEE',         'Excluir funcionário'),
    ('ENABLE_EMPLOYEE',         'Ativar funcionário'),
    ('DISABLE_EMPLOYEE',        'Inativar funcionário'),
    ('TRANSFER_EMPLOYEE',       'Transferir funcionário'),
    -- Employee Contact
    ('GET_EMPLOYEE_CONTACT',    'Visualizar contato de funcionário'),
    ('CREATE_EMPLOYEE_CONTACT', 'Cadastrar contato de funcionário'),
    ('UPDATE_EMPLOYEE_CONTACT', 'Atualizar contato de funcionário'),
    ('DELETE_EMPLOYEE_CONTACT', 'Excluir contato de funcionário'),
    -- Employee Address
    ('GET_EMPLOYEE_ADDRESS',    'Visualizar endereço de funcionário'),
    ('CREATE_EMPLOYEE_ADDRESS', 'Cadastrar endereço de funcionário'),
    ('UPDATE_EMPLOYEE_ADDRESS', 'Atualizar endereço de funcionário'),
    ('DELETE_EMPLOYEE_ADDRESS', 'Excluir endereço de funcionário'),
    -- Login
    ('GET_LOGIN',               'Visualizar login'),
    ('GET_LOGIN_INFO',          'Visualizar informações do login autenticado'),
    ('CREATE_LOGIN',            'Cadastrar login'),
    ('UPDATE_LOGIN',            'Atualizar login'),
    ('UPDATE_LOGIN_STATUS',     'Bloquear e desbloquear login'),
    ('DELETE_LOGIN',            'Excluir login'),
    -- Profile
    ('GET_PROFILE',             'Visualizar perfil'),
    ('CREATE_PROFILE',          'Cadastrar perfil'),
    ('UPDATE_PROFILE',          'Atualizar perfil'),
    ('DELETE_PROFILE',          'Excluir perfil'),
    ('ENABLE_PROFILE',          'Ativar perfil'),
    ('DISABLE_PROFILE',         'Inativar perfil'),
    -- Resource
    ('GET_RESOURCE',            'Visualizar recursos disponíveis'),
    -- Configuration
    ('GET_CONFIGURATION',       'Visualizar configuração'),
    ('UPDATE_CONFIGURATION',    'Atualizar configuração'),
    -- Integration
    ('GET_INTEGRATION',         'Visualizar fila de integração'),
    ('RETRY_INTEGRATION',       'Reprocessar integração com erro')
    ) AS v(code, description)
    RETURNING resource_id
    ),
-- -------------------------------------------------------------------------
-- Vincula todos os 58 recursos ao perfil ADMIN
-- Cross join entre 1 perfil × N recursos = N registros em profile_resource
-- -------------------------------------------------------------------------
    profile_resource_insert AS (
INSERT INTO scos.scos_profile_resource
(
    profile_id,
    resource_id,
    created_at,
    user_at
)
SELECT
    pr.profile_id,
    r.resource_id,
    now(),
    'Migration'
FROM profile_insert pr, resource_insert r
    RETURNING profile_id
    )
-- -------------------------------------------------------------------------
-- Dispara a Saga de integração com o Keycloak para criação do usuário admin
-- Status PENDING: aguarda o processador da Saga executar a chamada
-- O KEYCLOAK_ID será preenchido em scos_login após a Saga concluir com sucesso
-- -------------------------------------------------------------------------
INSERT INTO scos.scos_integration_keycloak
(
    integration_keycloak_id,
    keycloak_id,
    realm,
    email,
    username,
    requesting,
    type,
    status,
    request,
    retry_count,
    max_retries,
    created_at,
    updated_at,
    user_at
)
SELECT
    gen_random_uuid(),
    NULL,
    'scos',
    'scos_flow@scos.com',
    'scosadmin',
    'Migration',
    'CREATE',
    'PENDING',
    json_build_object(
            'username',  'scosadmin',
            'email',     'scos_flow@scos.com',
            'firstName', 'Scos Softwares',
            'lastName',  'Flow',
            'enabled',   true,
            'attributes', json_build_object(
                    'company_id',   json_build_array(c.company_id::text),
                    'company_name', json_build_array(c.name),
                    'branch_id',    json_build_array(c.company_id::text),
                    'branch_name',  json_build_array(c.name),
                    'employee_id',  json_build_array(e.employee_id::text)
                          )
    ),
    0,
    3,
    now(),
    now(),
    'Migration'
FROM company_insert c, employee_insert e, login_insert l;
