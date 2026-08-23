CREATE OR REPLACE FUNCTION fn_close_previous_position()
RETURNS TRIGGER AS $$
BEGIN
  UPDATE scos_employee_position_history
  SET end_date = NEW.start_date
  WHERE employee_id = NEW.employee_id
    AND end_date IS NULL;

  RETURN NEW;
END;
$$
LANGUAGE plpgsql;
