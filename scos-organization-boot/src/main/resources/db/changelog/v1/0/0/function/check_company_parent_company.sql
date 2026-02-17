CREATE OR REPLACE FUNCTION scos.check_company_parent_company()
RETURNS TRIGGER AS $$
BEGIN
    -- Verifica se o PARENT_COMPANY é igual ao COMPANY_ID
    IF NEW.PARENT_COMPANY_ID = NEW.COMPANY_ID THEN
        RAISE EXCEPTION 'COMPANY_ID (%), não pode ser igual ao PARENT_COMPANY_ID (%)',
            NEW.COMPANY_ID, NEW.PARENT_COMPANY_ID;
    END IF;
    RETURN NEW;
END;
$$
LANGUAGE plpgsql;
