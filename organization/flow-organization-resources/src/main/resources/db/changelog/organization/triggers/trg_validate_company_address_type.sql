CREATE OR REPLACE TRIGGER trg_validate_company_address_type
BEFORE INSERT OR UPDATE OF address_type_id
ON SCOS_COMPANY_ADDRESS
FOR EACH ROW
EXECUTE FUNCTION fn_validate_company_address_type();
