CREATE OR REPLACE TRIGGER trg_remove_formatting_tax_identifier_company
BEFORE INSERT OR UPDATE
ON scos.SCOS_COMPANY
FOR EACH ROW
EXECUTE FUNCTION scos.remove_formatting_tax_identifier();
