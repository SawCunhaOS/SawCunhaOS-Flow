CREATE TRIGGER trg_remove_formatting_tax_identifier_employee
BEFORE INSERT OR UPDATE
ON SCOS_EMPLOYEE
FOR EACH ROW
EXECUTE FUNCTION remove_formatting_tax_identifier();
