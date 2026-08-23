CREATE OR REPLACE FUNCTION fn_sync_company_status()
RETURNS TRIGGER AS $$
BEGIN
  UPDATE scos_company
  SET status = NEW.status,
      updated_at = NEW.created_at
  WHERE company_id = NEW.company_id;

  RETURN NEW;
END;
$$
LANGUAGE plpgsql;
