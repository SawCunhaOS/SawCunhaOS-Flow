CREATE OR REPLACE FUNCTION scos.fn_validate_company_address_type()
RETURNS TRIGGER AS $$
DECLARE
  v_entity_type VARCHAR(20);
BEGIN
  SELECT entity_type INTO v_entity_type
  FROM scos.scos_address_type
  WHERE address_type_id = NEW.address_type_id;

  IF v_entity_type IS DISTINCT FROM 'COMPANY' THEN
    RAISE EXCEPTION 'address_type_id % não pertence a entity_type COMPANY', NEW.address_type_id;
  END IF;

  RETURN NEW;
END;
$$
LANGUAGE plpgsql;
