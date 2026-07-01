CREATE OR REPLACE TRIGGER trg_validate_employee_contact_type
BEFORE INSERT OR UPDATE OF contact_type_id
ON scos.SCOS_EMPLOYEE_CONTACT
FOR EACH ROW
EXECUTE FUNCTION scos.fn_validate_employee_contact_type();
