CREATE OR REPLACE TRIGGER trg_check_employee_supervisor
BEFORE INSERT OR UPDATE
ON scos.SCOS_EMPLOYEE
FOR EACH ROW
EXECUTE FUNCTION scos.check_employee_supervisor();
