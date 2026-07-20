CREATE OR REPLACE FUNCTION scos.fn_validate_company_contact_type()
RETURNS TRIGGER AS $$
DECLARE
  v_entity_type VARCHAR(20);
BEGIN
  SELECT entity_type INTO v_entity_type
  FROM scos.scos_contact_type
  WHERE contact_type_id = NEW.contact_type_id;

  IF v_entity_type IS DISTINCT FROM 'COMPANY' THEN
    RAISE EXCEPTION 'contact_type_id % não pertence a entity_type COMPANY', NEW.contact_type_id;
  END IF;

  RETURN NEW;
END;
$$
LANGUAGE plpgsql;
