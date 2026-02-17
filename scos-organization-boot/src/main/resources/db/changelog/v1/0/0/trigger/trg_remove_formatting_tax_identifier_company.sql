CREATE TRIGGER trg_remove_formatting_tax_identifier_company
BEFORE INSERT OR UPDATE
ON SCOS_COMPANY
FOR EACH ROW
EXECUTE FUNCTION remove_formatting_tax_identifier();
