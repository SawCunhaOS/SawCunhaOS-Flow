CREATE OR REPLACE VIEW v_scos_cache_login AS
SELECT il.login,
       il.profile_id,
       il.status,
       ie.active,
       ip.features
FROM   scos_login    il
JOIN   scos_profile  ip ON ip.profile_id  = il.profile_id
JOIN   scos_employee ie ON il.employee_id = ie.employee_id;
