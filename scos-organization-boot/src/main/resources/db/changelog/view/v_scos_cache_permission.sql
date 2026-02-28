CREATE OR REPLACE VIEW scos.v_scos_cache_permission AS
SELECT
    feature,
    array_agg(permission ORDER BY permission) AS permissions
FROM
    scos.scos_permission,
    unnest(features) AS feature
GROUP BY
    feature;
