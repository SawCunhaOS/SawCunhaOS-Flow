CREATE OR REPLACE FUNCTION fn_sync_employee_position()
RETURNS TRIGGER AS $$
BEGIN
  UPDATE scos_employee
  SET position_id = NEW.position_id,
      updated_at = NEW.created_at
  WHERE employee_id = NEW.employee_id;

  RETURN NEW;
END;
$$
LANGUAGE plpgsql;
