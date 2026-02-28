CREATE OR REPLACE TRIGGER trg_check_company_parent_company
BEFORE INSERT OR UPDATE
ON scos.SCOS_COMPANY
FOR EACH ROW
EXECUTE FUNCTION scos.check_company_parent_company();
