CREATE OR REPLACE TRIGGER trg_before_insert_company_status_history
BEFORE INSERT
ON scos.SCOS_COMPANY_STATUS_HISTORY
FOR EACH ROW
EXECUTE FUNCTION scos.fn_before_insert_company_status_history();
