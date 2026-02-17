CREATE OR REPLACE TRIGGER trg_check_company_parent_company
BEFORE INSERT OR UPDATE
ON SCOS_COMPANY
FOR EACH ROW
EXECUTE FUNCTION check_company_parent_company();
