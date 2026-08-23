CREATE OR REPLACE TRIGGER trg_validate_employee_contact_type
BEFORE INSERT OR UPDATE OF contact_type_id
ON SCOS_EMPLOYEE_CONTACT
FOR EACH ROW
EXECUTE FUNCTION fn_validate_employee_contact_type();
