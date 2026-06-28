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
-- Refresh: pg_cron a cada 30 minutos via scos.refresh_authority_views()
-- =============================================================================

DROP MATERIALIZED VIEW IF EXISTS scos.vw_login_context CASCADE;

CREATE MATERIALIZED VIEW scos.vw_login_context AS
SELECT
    -- Login
    l.login_id,
    l.login,
    l.type,
    l.status,
    l.keycloak_id,

    -- Funcionário (NULL para EXTERNAL e SERVICE)
    e.employee_id,
    COALESCE(e.name, l.login) AS name,
    e.email,

    -- Empresa e filial resolvidos
    COALESCE(ec.parent_company_id, ec.company_id) AS company_id,
    COALESCE(pc.name,              ec.name)        AS company_name,
    ec.company_id                                  AS branch_id,
    ec.name                                        AS branch_name,

    -- Perfil
    p.profile_id,
    p.code AS profile_code,

    -- Permissão (NULL quando o perfil não tem recursos atribuídos)
    r.code AS permission

FROM       scos.scos_login            l
    JOIN       scos.scos_profile          p   ON  p.profile_id  = l.profile_id
    LEFT JOIN  scos.scos_employee         e   ON  e.employee_id = l.employee_id
    LEFT JOIN  scos.scos_company          ec  ON  ec.company_id = e.company_id
    LEFT JOIN  scos.scos_company          pc  ON  pc.company_id = ec.parent_company_id
    LEFT JOIN  scos.scos_profile_resource pr  ON  pr.profile_id = p.profile_id
    LEFT JOIN  scos.scos_resource         r   ON  r.resource_id = pr.resource_id
                                            AND r.active = true;

CREATE UNIQUE INDEX uidx_vw_login_context_login_permission
    ON scos.vw_login_context (login_id, COALESCE(permission, ''));
