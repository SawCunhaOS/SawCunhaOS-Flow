CREATE OR REPLACE FUNCTION scos.fn_sync_login_status()
RETURNS TRIGGER AS $$
BEGIN
  UPDATE scos.scos_login
  SET status = NEW.status,
      updated_at = NEW.created_at
  WHERE login_id = NEW.login_id;

  RETURN NEW;
END;
$$
LANGUAGE plpgsql;
