CREATE OR REPLACE FUNCTION fn_validate_login_profile_not_primary()
RETURNS TRIGGER AS $$
DECLARE
  v_primary_profile_id BIGINT;
BEGIN
  SELECT profile_id INTO v_primary_profile_id
  FROM scos_login
  WHERE login_id = NEW.login_id;

  IF NEW.profile_id = v_primary_profile_id THEN
    RAISE EXCEPTION 'profile_id % já é o perfil principal do login % (SCOS_LOGIN.PROFILE_ID); não deve ser duplicado em SCOS_LOGIN_PROFILE', NEW.profile_id, NEW.login_id;
  END IF;

  RETURN NEW;
END;
$$
LANGUAGE plpgsql;
