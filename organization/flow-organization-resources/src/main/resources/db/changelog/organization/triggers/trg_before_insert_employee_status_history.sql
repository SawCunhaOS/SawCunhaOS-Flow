CREATE OR REPLACE TRIGGER trg_before_insert_employee_status_history
BEFORE INSERT
ON scos.SCOS_EMPLOYEE_STATUS_HISTORY
FOR EACH ROW
EXECUTE FUNCTION scos.fn_before_insert_employee_status_history();
