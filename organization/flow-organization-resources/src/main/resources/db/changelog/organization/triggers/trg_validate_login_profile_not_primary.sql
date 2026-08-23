CREATE OR REPLACE TRIGGER trg_validate_login_profile_not_primary
BEFORE INSERT OR UPDATE
ON SCOS_LOGIN_PROFILE
FOR EACH ROW
EXECUTE FUNCTION fn_validate_login_profile_not_primary();
