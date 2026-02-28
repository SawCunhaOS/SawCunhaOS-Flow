CREATE OR REPLACE VIEW scos.v_scos_cache_login AS
SELECT
    il.login,
    il.profile_id,
    il.status,
    ie.active,
    ip.features
FROM   scos.scos_login    il
JOIN   scos.scos_profile  ip ON ip.profile_id  = il.profile_id
JOIN   scos.scos_employee ie ON ie.employee_id = il.employee_id;
