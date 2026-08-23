CREATE OR REPLACE FUNCTION fn_validate_employee_address_type()
RETURNS TRIGGER AS $$
DECLARE
  v_entity_type VARCHAR(20);
BEGIN
  SELECT entity_type INTO v_entity_type
  FROM scos_address_type
  WHERE address_type_id = NEW.address_type_id;

  IF v_entity_type IS DISTINCT FROM 'EMPLOYEE' THEN
    RAISE EXCEPTION 'address_type_id % não pertence a entity_type EMPLOYEE', NEW.address_type_id;
  END IF;

  RETURN NEW;
END;
$$
LANGUAGE plpgsql;
