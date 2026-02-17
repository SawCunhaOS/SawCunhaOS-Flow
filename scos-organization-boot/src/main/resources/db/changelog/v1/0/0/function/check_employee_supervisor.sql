CREATE OR REPLACE FUNCTION check_employee_supervisor()
RETURNS TRIGGER AS $$
BEGIN
    -- Verifica se o SUPERVISOR_ID é igual ao EMPLOYEE_ID
    IF NEW.SUPERVISOR_ID = NEW.EMPLOYEE_ID THEN
        RAISE EXCEPTION 'EMPLOYEE_ID (%), não pode ser igual ao SUPERVISOR_ID (%)',
            NEW.EMPLOYEE_ID, NEW.SUPERVISOR_ID;
    END IF;
    RETURN NEW;
END;
$$
LANGUAGE plpgsql;
