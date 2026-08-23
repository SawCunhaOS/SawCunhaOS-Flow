DO $$
DECLARE
    v_job_id INT8;
BEGIN
    -- Busca o ID exato associado ao nome do job
    SELECT jobid INTO v_job_id
    FROM cron.job
    WHERE jobname = 'VW_AUTHORITY_JOB';
    --
    -- Se o job existir, realiza a remoção segura
    IF v_job_id IS NOT NULL THEN
            PERFORM cron.unschedule(v_job_id);
            RAISE NOTICE 'Job VW_AUTHORITY_JOB removido com sucesso. ID: %', v_job_id;
    ELSE
            RAISE NOTICE 'O Job VW_AUTHORITY_JOB nao foi encontrado ou ja foi removido.';
    END IF;
    --
END $$;


