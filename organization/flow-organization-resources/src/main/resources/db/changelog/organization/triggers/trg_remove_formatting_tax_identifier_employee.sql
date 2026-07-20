CREATE OR REPLACE TRIGGER trg_remove_formatting_tax_identifier_employee
BEFORE INSERT OR UPDATE
ON scos.SCOS_EMPLOYEE
FOR EACH ROW
EXECUTE FUNCTION scos.remove_formatting_tax_identifier();
