CREATE OR REPLACE FUNCTION fn_block_delete()
RETURNS TRIGGER AS $$
BEGIN
  RAISE EXCEPTION 'DELETE não permitido em %; utilize UPDATE ... SET active = false', TG_TABLE_NAME;
END;
$$
LANGUAGE plpgsql;
