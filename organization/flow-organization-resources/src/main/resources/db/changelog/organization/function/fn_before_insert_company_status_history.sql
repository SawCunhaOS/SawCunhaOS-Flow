CREATE OR REPLACE FUNCTION fn_before_insert_company_status_history()
RETURNS TRIGGER AS $$
DECLARE
  v_entity_type VARCHAR(20);
BEGIN
  -- (1) deriva o status anterior a partir do último registro real
  SELECT status INTO NEW.previous_status
  FROM scos_company_status_history
  WHERE company_id = NEW.company_id
  ORDER BY created_at DESC
  LIMIT 1;

  -- (2) valida que cada motivo informado pertence a ENTITY_TYPE = 'COMPANY'
  IF NEW.reason_activate_id IS NOT NULL THEN
    SELECT entity_type INTO v_entity_type FROM scos_reason_activate WHERE reason_activate_id = NEW.reason_activate_id;
    IF v_entity_type IS DISTINCT FROM 'COMPANY' THEN
      RAISE EXCEPTION 'reason_activate_id % não pertence a entity_type COMPANY', NEW.reason_activate_id;
    END IF;
  END IF;

  IF NEW.reason_inactivate_id IS NOT NULL THEN
    SELECT entity_type INTO v_entity_type FROM scos_reason_inactivate WHERE reason_inactivate_id = NEW.reason_inactivate_id;
    IF v_entity_type IS DISTINCT FROM 'COMPANY' THEN
      RAISE EXCEPTION 'reason_inactivate_id % não pertence a entity_type COMPANY', NEW.reason_inactivate_id;
    END IF;
  END IF;

  IF NEW.reason_disable_id IS NOT NULL THEN
    SELECT entity_type INTO v_entity_type FROM scos_reason_disable WHERE reason_disable_id = NEW.reason_disable_id;
    IF v_entity_type IS DISTINCT FROM 'COMPANY' THEN
      RAISE EXCEPTION 'reason_disable_id % não pertence a entity_type COMPANY', NEW.reason_disable_id;
    END IF;
  END IF;

  IF NEW.reason_enable_id IS NOT NULL THEN
    SELECT entity_type INTO v_entity_type FROM scos_reason_enable WHERE reason_enable_id = NEW.reason_enable_id;
    IF v_entity_type IS DISTINCT FROM 'COMPANY' THEN
      RAISE EXCEPTION 'reason_enable_id % não pertence a entity_type COMPANY', NEW.reason_enable_id;
    END IF;
  END IF;

  RETURN NEW;
END;
$$
LANGUAGE plpgsql;
