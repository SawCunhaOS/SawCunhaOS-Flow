-- =============================================================================
-- VW_LOGIN_CONTEXT (MATERIALIZED)
-- =============================================================================
-- Uma linha por (login, permissão).
-- Base para vw_authority_response e consultas granulares.
--
-- Útil para:
--   - Verificar se um login possui uma permissão específica
--   - Auditoria de acessos por recurso
--   - Debug de configurações de perfil
--
-- Resolução de empresa e filial:
--   Funcionário em filial  → company = matriz (parent), branch = filial direta
--   Funcionário na matriz  → company = matriz,          branch = matriz (mesma)
--   Login EXTERNAL/SERVICE → company, branch e employee retornam NULL
--
-- Filtros aplicados na view:
--   - Apenas recursos com active = true contribuem para permissões
--   - Logins sem recursos aparecem com permission = NULL (perfil vazio)
--   - Status do login NÃO é filtrado — responsabilidade da query consumidora
--
-- Refresh: pg_cron a cada 30 minutos via refresh_authority_views()
--
-- Permissão agrega Perfil principal (SCOS_LOGIN.PROFILE_ID) E Perfis adicionais
-- (SCOS_LOGIN_PROFILE, N:N) — profile_id/profile_code continuam se referindo só
-- ao principal (Story 3.4).
-- =============================================================================

DROP MATERIALIZED VIEW IF EXISTS vw_login_context CASCADE;

CREATE MATERIALIZED VIEW vw_login_context AS
WITH login_profiles AS (
    -- Perfil principal (1:1, SCOS_LOGIN.PROFILE_ID) + Perfis adicionais (N:N, SCOS_LOGIN_PROFILE)
    SELECT l.login_id, l.profile_id FROM scos_login l
    UNION
    SELECT lp.login_id, lp.profile_id FROM scos_login_profile lp
)
SELECT DISTINCT
    -- Login
    l.login_id,
    l.login,
    l.type,
    l.status,
    l.external_id,

    -- Funcionário (NULL para EXTERNAL e SERVICE)
    e.employee_id,
    COALESCE(e.name, l.login) AS name,
    e.email,

    -- Empresa e filial resolvidos
    COALESCE(ec.parent_company_id, ec.company_id) AS company_id,
    COALESCE(pc.name,              ec.name)        AS company_name,
    ec.company_id                                  AS branch_id,
    ec.name                                        AS branch_name,

    -- Perfil principal
    pp.profile_id,
    pp.code AS profile_code,

    -- Permissão (principal + adicionais; NULL quando nenhum perfil tem recursos atribuídos)
    COALESCE(r.code, '') AS permission

FROM       scos_login            l
    JOIN       scos_profile          pp   ON  pp.profile_id = l.profile_id
    LEFT JOIN  scos_employee         e    ON  e.employee_id = l.employee_id
    LEFT JOIN  scos_company          ec   ON  ec.company_id = e.company_id
    LEFT JOIN  scos_company          pc   ON  pc.company_id = ec.parent_company_id
    LEFT JOIN  login_profiles             allp ON  allp.login_id = l.login_id
    LEFT JOIN  scos_profile_resource pr   ON  pr.profile_id = allp.profile_id
    LEFT JOIN  scos_resource         r    ON  r.resource_id = pr.resource_id
                                             AND r.active = true;

CREATE UNIQUE INDEX uidx_vw_login_context_login_permission
    ON vw_login_context (login_id, permission);
