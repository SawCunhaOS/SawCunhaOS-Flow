INSERT INTO scos_company
(company_id, "name", name_treatment, tax_identifier, foundation_date, sector_of_activity, parent_company_id, observation, date_created, active, status)
VALUES(1, 'Scos Flow', 'Scos Softwares Flow System', 'ULX45AEH000133', '2025-01-01', 'INFO', NULL, 'Cadastro de Teste', '2025-01-01 00:00:00.000', true, 'ACTIVE');

INSERT INTO scos_department
(department_id, code, description)
VALUES(1, 'ADMIN', 'Administrador do Sistema');

INSERT INTO scos_position
(position_id, department_id, code, description)
VALUES(1, 1, 'ADMIN', 'Administrador MASTER');

INSERT INTO scos_employee
(employee_id, company_id, position_id, "name", name_treatment, tax_identifier, email, birth_date, observation, date_of_hiring, date_created, active, supervisor_id)
VALUES(1, 1, 1, 'Scos Softwares', 'Flow', '00000000000', 'Scos_flow@Scos.com', '2025-01-01', 'Administrador do Sistema', '2025-01-01', '2025-01-01', true, NULL);

INSERT INTO scos_profile
(profile_id, code, description, features)
VALUES(1, 'ADMIN', 'Administrador', ARRAY['ORGANIZATION_ADMINISTRATION', 'ADDRESS_ADMINISTRATION']);

INSERT INTO scos_login
(login_id, profile_id, employee_id, login, "password", salt, status, date_created, date_last_change_password)
VALUES(1, 1, 1, 'scos_admin', 'jQ9uhi98g/WQaCHjl4t1wfTZ9RI+92Feyj+MowimUliLgD7b969AbIpM6Sw8gtHY1Zk8MmK8bULb8QvyuhLOSQ==', 'Wb/TpxcbveJDtozW4U0kE8HWcSYGENNk', 'ENABLE', '2025-01-01', '2025-01-01 00:00:00.000');
