CREATE OR REPLACE FUNCTION scos.fn_sync_employee_status()
RETURNS TRIGGER AS $$
BEGIN
  UPDATE scos.scos_employee
  SET status = NEW.status,
      updated_at = NEW.created_at
  WHERE employee_id = NEW.employee_id;

  RETURN NEW;
END;
$$
LANGUAGE plpgsql;
