CREATE OR REPLACE TRIGGER trg_validate_company_contact_type
BEFORE INSERT OR UPDATE OF contact_type_id
ON SCOS_COMPANY_CONTACT
FOR EACH ROW
EXECUTE FUNCTION fn_validate_company_contact_type();
