CREATE OR REPLACE FUNCTION remove_formatting_tax_identifier()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.TAX_IDENTIFIER IS NOT NULL THEN
        NEW.TAX_IDENTIFIER := regexp_replace(NEW.TAX_IDENTIFIER, '[^a-zA-Z0-9]', '', 'g');
    END IF;
    RETURN NEW;
END;
$$
LANGUAGE plpgsql;
