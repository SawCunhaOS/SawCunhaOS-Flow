CREATE OR REPLACE FUNCTION fn_validate_employee_contact_type()
RETURNS TRIGGER AS $$
DECLARE
  v_entity_type VARCHAR(20);
BEGIN
  SELECT entity_type INTO v_entity_type
  FROM scos_contact_type
  WHERE contact_type_id = NEW.contact_type_id;

  IF v_entity_type IS DISTINCT FROM 'EMPLOYEE' THEN
    RAISE EXCEPTION 'contact_type_id % não pertence a entity_type EMPLOYEE', NEW.contact_type_id;
  END IF;

  RETURN NEW;
END;
$$
LANGUAGE plpgsql;
