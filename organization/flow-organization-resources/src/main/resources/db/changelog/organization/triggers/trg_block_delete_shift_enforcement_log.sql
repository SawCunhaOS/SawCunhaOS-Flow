CREATE OR REPLACE TRIGGER trg_block_delete_shift_enforcement_log
BEFORE DELETE
ON scos.SCOS_SHIFT_ENFORCEMENT_LOG
FOR EACH ROW
EXECUTE FUNCTION scos.fn_block_delete();
