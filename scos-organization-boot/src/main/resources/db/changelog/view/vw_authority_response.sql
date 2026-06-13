-- =============================================================================
-- VW_AUTHORITY_RESPONSE
-- =============================================================================
-- Uma linha por login com permissões agregadas em array.
-- Mapeamento direto para o proto AuthorityResponse:
--
--   string         login        = 1  → login
--   string         name         = 2  → name
--   string         email        = 3  → email
--   int64          company_id   = 4  → company_id
--   string         company_name = 5  → company_name
--   int64          branch_id    = 6  → branch_id
--   string         branch_name  = 7  → branch_name
--   int64          employee_id  = 8  → employee_id
--   repeated string permissions = 9  → permissions (array ordenado)
--
-- Login sem permissões retorna permissions = '{}'.
-- Login EXTERNAL/SERVICE retorna company_id, branch_id, employee_id = NULL.
--
-- Exemplo de uso no scos-registry:
--   SELECT * FROM scos.vw_authority_response
--   WHERE  login  = $1
--   AND    status = 'ACTIVE';
-- =============================================================================
CREATE VIEW scos.vw_authority_response AS
SELECT
    login_id,
    login,
    type,
    status,
    keycloak_id,
    employee_id,
    name,
    email,
    company_id,
    company_name,
    branch_id,
    branch_name,
    profile_id,
    profile_code,

    -- ARRAY_REMOVE elimina NULL gerado pelo LEFT JOIN quando o perfil está vazio
    ARRAY_REMOVE(
            ARRAY_AGG(permission ORDER BY permission),
            NULL
    ) AS permissions

FROM scos.vw_login_context

GROUP BY
    login_id,
    login,
    type,
    status,
    keycloak_id,
    employee_id,
    name,
    email,
    company_id,
    company_name,
    branch_id,
    branch_name,
    profile_id,
    profile_code;
