-- =========================================================================
-- CONFIGURAÇÃO INICIAL DO SISTEMA SCOS
-- Utiliza CTEs encadeadas para garantir integridade referencial
-- =========================================================================

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
        active,
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
        'ULX45AEH000133',
        '2025-01-01',
        'INFO',
        NULL,
        'Cadastro inicial do sistema',
        true,
        'ACTIVE',
        now(),
        NULL,
        'Migration'
    )
    RETURNING company_id
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
        NULL,
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
        NULL,
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
        active,
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
        '00000000000',
        'scos_flow@scos.com',
        '2025-01-01',
        '2025-01-01',
        'Administrador do Sistema',
        true,
        NULL,
        now(),
        NULL,
        'Migration'
    FROM company_insert c, position_insert p
    RETURNING employee_id
),
profile_insert AS (
    INSERT INTO scos.scos_profile
    (
        profile_id,
        code,
        description,
        active,
        features,
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
        ARRAY[
            'ORGANIZATION_ADMINISTRATION',
            'ORGANIZATION_VIEW',
            'ORGANIZATION_MANAGEMENT',
            'ORGANIZATION_COMPANY_VIEW',
            'ORGANIZATION_COMPANY_MANAGEMENT',
            'ORGANIZATION_EMPLOYEE_VIEW',
            'ORGANIZATION_EMPLOYEE_MANAGEMENT',
            'ORGANIZATION_EMPLOYEE_LOGIN_MANAGEMENT',
            'ORGANIZATION_DEPARTMENT_VIEW',
            'ORGANIZATION_DEPARTMENT_MANAGEMENT',
            'ORGANIZATION_POSITION_VIEW',
            'ORGANIZATION_POSITION_MANAGEMENT',
            'ORGANIZATION_PROFILE_VIEW',
            'ORGANIZATION_PROFILE_MANAGEMENT'
        ],
        now(),
        NULL,
        'Migration'
    )
    RETURNING profile_id
)
INSERT INTO scos.scos_login
(
    login_id,
    profile_id,
    employee_id,
    login,
    password,
    salt,
    status,
    date_last_change_password,
    created_at,
    updated_at,
    user_at
)
SELECT
    nextval('scos.SEQ_LOGIN_ID'),
    pr.profile_id,
    e.employee_id,
    'scosadmin',
    'jQ9uhi98g/WQaCHjl4t1wfTZ9RI+92Feyj+MowimUliLgD7b969AbIpM6Sw8gtHY1Zk8MmK8bULb8QvyuhLOSQ==',
    'Wb/TpxcbveJDtozW4U0kE8HWcSYGENNk',
    'PENDING_PASSWORD_CHANGE',
    now(),
    now(),
    NULL,
    'Migration'
FROM profile_insert pr, employee_insert e;
