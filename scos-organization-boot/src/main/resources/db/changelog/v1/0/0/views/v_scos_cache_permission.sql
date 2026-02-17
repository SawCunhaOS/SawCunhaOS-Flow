CREATE OR REPLACE VIEW v_scos_cache_permission AS
SELECT
    feature,
    array_agg(permission ORDER BY permission) AS permissions
FROM
    scos_permission,
    unnest(features) AS feature
GROUP BY
    feature
ORDER BY
    feature;
