CREATE OR REPLACE TRIGGER trg_block_delete_reason_position_change
BEFORE DELETE
ON scos.SCOS_REASON_POSITION_CHANGE
FOR EACH ROW
EXECUTE FUNCTION scos.fn_block_delete();
