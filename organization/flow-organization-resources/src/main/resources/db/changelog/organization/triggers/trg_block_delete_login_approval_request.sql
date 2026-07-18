CREATE OR REPLACE TRIGGER trg_block_delete_login_approval_request
BEFORE DELETE
ON scos.SCOS_LOGIN_APPROVAL_REQUEST
FOR EACH ROW
EXECUTE FUNCTION scos.fn_block_delete();
