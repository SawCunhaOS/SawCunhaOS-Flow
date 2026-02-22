-- =========================================================================
-- CONFIGURAÇÃO INICIAL DO SISTEMA SCOS
-- Utiliza sequences para gerar IDs automáticos e CTEs para integridade
-- referencial entre as tabelas
-- =========================================================================

WITH company_insert AS (
    INSERT INTO scos_company
    (
        company_id,
        "name",
        name_treatment,
        tax_identifier,
        foundation_date,
        sector_of_activity,
        parent_company_id,
        observation,
        date_created,
        active,
        status,
        created_at,
        updated_at,
        user_at
    )
    VALUES
    (
        nextval('SEQ_COMPANY_ID'),
        'Scos Flow',
        'Scos Softwares Flow System',
        'ULX45AEH000133',
        '2025-01-01',
        'INFO',
        NULL,
        'Cadastro de Teste',
        '2025-01-01 00:00:00.000',
        true,
        'ACTIVE',
        now(),
        now(),
        'Migration'
    )
    RETURNING company_id
),
department_insert AS (
    INSERT INTO scos_department
    (
        department_id,
        code,
        description,
        created_at,
        updated_at,
        user_at
    )
    VALUES
    (
        nextval('SEQ_DEPARTMENT_ID'),
        'ADMIN',
        'Administrador do Sistema',
        now(),
        now(),
        'Migration'
    )
    RETURNING department_id
),
position_insert AS (
    INSERT INTO scos_position
    (
        position_id,
        department_id,
        code,
        description,
        created_at,
        updated_at,
        user_at
    )
    SELECT
        nextval('SEQ_POSITION_ID'),
        d.department_id,
        'ADMIN',
        'Administrador MASTER',
        now(),
        now(),
        'Migration'
    FROM department_insert d
    RETURNING position_id
),
employee_insert AS (
    INSERT INTO scos_employee
    (
        employee_id,
        company_id,
        position_id,
        "name",
        name_treatment,
        tax_identifier,
        email,
        birth_date,
        observation,
        date_of_hiring,
        date_created,
        active,
        supervisor_id,
        created_at,
        updated_at,
        user_at
    )
    SELECT
        nextval('SEQ_EMPLOYEE_ID'),
        c.company_id,
        p.position_id,
        'Scos Softwares',
        'Flow',
        '00000000000',
        'Scos_flow@Scos.com',
        '2025-01-01',
        'Administrador do Sistema',
        '2025-01-01',
        '2025-01-01',
        true,
        NULL,
        now(),
        now(),
        'Migration'
    FROM company_insert c, position_insert p
    RETURNING employee_id
),
profile_insert AS (
    INSERT INTO scos_profile
    (
        profile_id,
        code,
        description,
        features,
        created_at,
        updated_at,
        user_at
    )
    VALUES
    (
        nextval('SEQ_PROFILE_ID'),
        'ADMIN',
        'Administrador',
        ARRAY['ORGANIZATION_MANAGEMENT'],
        now(),
        now(),
        'Migration'
    )
    RETURNING profile_id
)
INSERT INTO scos_login
(
    login_id,
    profile_id,
    employee_id,
    login,
    "password",
    salt,
    status,
    date_created,
    date_last_change_password,
    created_at,
    updated_at,
    user_at
)
SELECT
    nextval('SEQ_LOGIN_ID'),
    pr.profile_id,
    e.employee_id,
    'scosadmin',
    'jQ9uhi98g/WQaCHjl4t1wfTZ9RI+92Feyj+MowimUliLgD7b969AbIpM6Sw8gtHY1Zk8MmK8bULb8QvyuhLOSQ==',
    'Wb/TpxcbveJDtozW4U0kE8HWcSYGENNk',
    'ENABLE',
    '2025-01-01',
    '2025-01-01 00:00:00.000',
    now(),
    now(),
    'Migration'
FROM profile_insert pr, employee_insert e;
